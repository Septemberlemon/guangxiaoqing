from fastapi import FastAPI, Depends, HTTPException, status, BackgroundTasks
from fastapi.middleware.cors import CORSMiddleware
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from fastapi.responses import StreamingResponse
from sqlalchemy.orm import Session
from typing import Optional
import uvicorn
import jwt
import uuid
from datetime import datetime, timedelta
from passlib.context import CryptContext
import requests
import random
import httpx
from contextlib import asynccontextmanager

from database import get_db, engine
from models import Base, User, SMSRecord
from schemas import (
    UserCreate, UserLogin, PasswordChange, PasswordReset,
    SendSMSRequest, Token, TokenData, ChatRequest, ChatResponse,
    SMSResponse
)


# ============================================================
# 应用初始化
# ============================================================

# 创建数据库表
Base.metadata.create_all(bind=engine)

# 创建 FastAPI 应用
app = FastAPI(
    debug=True,
    root_path="/api",
    title="用户管理API",
    description="使用FastAPI和SQLite实现的用户管理接口，支持短信验证码和单点登录控制。"
)

# 添加 CORS 中间件
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# ============================================================
# 全局配置
# ============================================================

# 密码哈希工具
pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")

# JWT 设置
SECRET_KEY = "your_secret_key"
ALGORITHM = "HS256"
ACCESS_TOKEN_EXPIRE_MINUTES = 7 * 24 * 60

# HTTP Bearer
bearer_scheme = HTTPBearer()

# 短信发送配置
SPUG_API_KEY = "LgM8MDJOrdnw"
SPUG_SEND_URL = f"https://push.spug.cc/send/{SPUG_API_KEY}"


# ============================================================
# 工具函数
# ============================================================

def verify_password(plain_password: str, hashed_password: str) -> bool:
    """验证密码是否匹配哈希值

    Args:
        plain_password: 明文密码
        hashed_password: 哈希后的密码

    Returns:
        bool: 密码是否匹配
    """
    return pwd_context.verify(plain_password, hashed_password)


def get_password_hash(password: str) -> str:
    """获取密码的哈希值

    Args:
        password: 明文密码

    Returns:
        str: 哈希后的密码
    """
    return pwd_context.hash(password)


def get_user_by_phone(db: Session, phone: str) -> Optional[User]:
    """根据手机号获取用户

    Args:
        db: 数据库会话
        phone: 手机号

    Returns:
        Optional[User]: 用户对象，如果不存在则返回 None
    """
    return db.query(User).filter(User.phone == phone).first()


def create_access_token(sub: str, expires_delta: Optional[timedelta] = None):
    """生成访问令牌

    Args:
        sub: 令牌主题（通常是用户标识）
        expires_delta: 过期时间增量，默认为 ACCESS_TOKEN_EXPIRE_MINUTES

    Returns:
        tuple: (token, jti) 令牌和令牌ID
    """
    jti = str(uuid.uuid4())
    to_encode = {"sub": sub, "jti": jti}
    expire = datetime.utcnow() + (expires_delta or timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES))
    to_encode.update({"exp": expire})
    encoded_jwt = jwt.encode(to_encode, SECRET_KEY, algorithm=ALGORITHM)
    return encoded_jwt, jti


def get_current_user(credentials: HTTPAuthorizationCredentials = Depends(bearer_scheme),
                     db: Session = Depends(get_db)) -> User:
    """获取当前用户并验证令牌

    Args:
        credentials: HTTP认证凭证
        db: 数据库会话

    Returns:
        User: 当前用户对象

    Raises:
        HTTPException: 如果令牌无效或已过期
    """
    token = credentials.credentials
    try:
        payload = jwt.decode(token, SECRET_KEY, algorithms=[ALGORITHM])
        # 使用 TokenData 类解析令牌数据
        token_data = TokenData(phone=payload.get("sub"), jti=payload.get("jti"))
        if token_data.phone is None or token_data.jti is None:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="无效的认证信息",
                headers={"WWW-Authenticate": "Bearer"},
            )
    except jwt.PyJWTError:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="令牌解析失败",
            headers={"WWW-Authenticate": "Bearer"},
        )
    user = get_user_by_phone(db, token_data.phone)
    if user is None or user.current_jti != token_data.jti:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="认证已失效，请重新登录",
            headers={"WWW-Authenticate": "Bearer"},
        )
    return user


def _send_sms_via_third_party(phone: str, code: str):
    """调用第三方短信API发送验证码

    Args:
        phone: 手机号
        code: 验证码
    """
    try:
        resp = requests.get(
            SPUG_SEND_URL,
            params={"code": code, "targets": phone},
            timeout=5
        )
        data = resp.json()
        if resp.status_code != 200 or data.get("code") != 200:
            # 这里可接入日志系统，记录失败详情
            print(f"[SMS ERROR] phone={phone}, resp={data}")
    except Exception as e:
        print(f"[SMS EXCEPTION] phone={phone}, error={e}")


def _update_password(user: User, new_password: str, db: Session) -> None:
    """更新用户密码

    Args:
        user: 用户对象
        new_password: 新密码
        db: 数据库会话
    """
    user.hashed_password = get_password_hash(new_password)
    db.commit()
    db.refresh(user)


def _create_token(user: User, db: Session) -> dict:
    """为用户创建新的访问令牌

    Args:
        user: 用户对象
        db: 数据库会话

    Returns:
        dict: 包含访问令牌的响应
    """
    token, jti = create_access_token(sub=user.phone)
    user.current_jti = jti
    db.commit()

    return {"access_token": token, "token_type": "bearer"}


# 内部函数，验证短信验证码
def _verify_sms(phone: str, code: str, sms_type: str, db: Session):
    """验证短信验证码

    Args:
        phone: 手机号
        code: 验证码
        sms_type: 验证码类型（"registration" 或 "password_reset"）
        db: 数据库会话

    Raises:
        HTTPException: 如果验证失败
    """
    print(f"验证码验证: phone={phone}, code={code}, type={sms_type}")

    # 获取所有相关验证码记录，排除已使用的，按时间倒序排列
    recent_codes = db.query(SMSRecord).filter(
        SMSRecord.phone == phone,
        SMSRecord.sms_type == sms_type,
        SMSRecord.is_used == False  # 排除已使用的验证码
    ).order_by(SMSRecord.send_time.desc()).limit(5).all()

    print(f"找到验证码记录数: {len(recent_codes)}")

    if not recent_codes:
        print(f"验证失败: 未找到验证码记录 phone={phone}")
        raise HTTPException(status_code=400, detail="未找到有效的验证码记录，请重新获取验证码")

    # 遍历所有验证码记录
    for sms_record in recent_codes:
        print(f"检查验证码: code={sms_record.code}, is_used={sms_record.is_used}, send_time={sms_record.send_time}")

        # 检查验证码是否过期 (10分钟内有效)
        now = datetime.now()
        time_diff = (now - sms_record.send_time).total_seconds()
        if time_diff > 600:
            print(f"验证码已过期: code={sms_record.code}")
            continue

        # 检查验证码是否匹配
        if code == sms_record.code:
            print(f"验证成功: code={sms_record.code}")

            sms_record.is_used = True
            db.commit()
            print(f"验证码已标记为已使用: code={sms_record.code}")
            return  # 验证成功，返回

    # 如果所有验证码都不匹配或已过期
    print(f"验证失败: 所有验证码都不匹配或已过期 phone={phone}, code={code}")
    raise HTTPException(status_code=400, detail="验证码错误或已过期，请重新输入")


# 异步流式转发函数
async def stream_rag(req: ChatRequest) -> StreamingResponse:
    payload = {
        "question": req.content,
        "history": [
            {"role": m.role, "content": m.content}
            for m in (req.history or [])
        ]
    }
    # 用 httpx 异步客户端，stream=True 保持连接不关闭
    async with httpx.AsyncClient() as client:
        try:
            resp = await client.post(
                "http://localhost:6001/ask/stream",
                json=payload,
                timeout=None,  # 关闭超时限制
                stream=True
            )
            resp.raise_for_status()
        except httpx.HTTPError as e:
            raise HTTPException(status_code=502, detail=f"RAG 服务调用失败：{e}")

        # 把 downstream 流式响应直接透传
        return StreamingResponse(
            resp.aiter_bytes(),
            media_type="text/event-stream"
        )


# ============================================================
# 短信验证码相关接口
# ============================================================

@app.post("/sms/send", response_model=SMSResponse, status_code=status.HTTP_202_ACCEPTED)
def send_sms(req: SendSMSRequest, background_tasks: BackgroundTasks, db: Session = Depends(get_db)) -> SMSResponse:
    """发送短信验证码

    Args:
        req: 发送短信请求
        background_tasks: 后台任务
        db: 数据库会话

    Returns:
        SMSResponse: 包含发送状态和剩余次数的响应

    Raises:
        HTTPException: 如果手机号不符合要求或超过发送限制
    """
    # 检查手机号在注册/重置场景下的合法性
    user = get_user_by_phone(db, req.phone)
    if req.type == "registration" and user:
        raise HTTPException(status_code=400, detail="该手机号已注册")
    if req.type == "password_reset" and not user:
        raise HTTPException(status_code=404, detail="该手机号未注册")

    # 检查该手机号在过去30天内的短信发送次数
    monthly_sms_count = SMSRecord.count_monthly_sms(db, req.phone)
    if monthly_sms_count >= 3:  # 每月最多发送3次
        raise HTTPException(
            status_code=status.HTTP_429_TOO_MANY_REQUESTS,
            detail=f"该手机号在过去30天内已发送{monthly_sms_count}次验证码，超过限制"
        )

    # 生成一个 6 位数字验证码
    code = f"{random.randint(0, 999999):06d}"

    # 记录短信发送
    sms_record = SMSRecord(phone=req.phone, sms_type=req.type, code=code, is_used=False)
    db.add(sms_record)
    db.commit()

    # 异步调用第三方接口发送短信
    background_tasks.add_task(_send_sms_via_third_party, req.phone, code)

    # 返回给前端发送成功信息和剩余可发送次数
    return SMSResponse(
        message="验证码发送成功",
        remaining=3 - monthly_sms_count - 1
    )


# ============================================================
# 用户管理相关接口
# ============================================================

@app.post("/users", response_model=Token, status_code=status.HTTP_201_CREATED)
def create_user(user: UserCreate, db: Session = Depends(get_db)):
    """创建新用户

    Args:
        user: 用户创建请求
        db: 数据库会话

    Returns:
        Token: 包含访问令牌的响应

    Raises:
        HTTPException: 如果手机号已注册或验证码无效
    """
    # 验证验证码
    _verify_sms(user.phone, user.code, "registration", db)

    # 创建新用户
    hashed = get_password_hash(user.password)
    new_user = User(phone=user.phone, hashed_password=hashed)
    db.add(new_user)
    db.commit()
    db.refresh(new_user)

    # 生成访问令牌
    return _create_token(new_user, db)


@app.post("/login", response_model=Token)
def login(user_login: UserLogin, db: Session = Depends(get_db)):
    """用户登录

    Args:
        user_login: 用户登录请求
        db: 数据库会话

    Returns:
        Token: 包含访问令牌的响应

    Raises:
        HTTPException: 如果手机号或密码错误
    """
    user = get_user_by_phone(db, user_login.phone)
    if not user or not verify_password(user_login.password, user.hashed_password):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="手机号或密码错误",
            headers={"WWW-Authenticate": "Bearer"},
        )

    # 生成新的访问令牌
    return _create_token(user, db)


@app.put("/users/password", response_model=Token)
def change_password(data: PasswordChange, current_user: User = Depends(get_current_user),
                    db: Session = Depends(get_db)):
    """修改密码（需要登录）

    Args:
        data: 密码修改请求，包含手机号、原密码和新密码
        current_user: 当前登录用户
        db: 数据库会话

    Returns:
        Token: 包含新访问令牌的响应

    Raises:
        HTTPException: 如果无权修改密码或原密码错误
    """
    # 仅允许用户修改自己的密码
    if current_user.phone != data.phone:
        raise HTTPException(status_code=403, detail="无权限修改此用户密码")

    # 验证原密码是否正确
    if not verify_password(data.old_password, current_user.hashed_password):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="原密码错误",
            headers={"WWW-Authenticate": "Bearer"},
        )

    # 更新密码
    _update_password(current_user, data.new_password, db)

    # 创建新的访问令牌
    return _create_token(current_user, db)


@app.post("/users/reset-password", response_model=Token)
def reset_password(data: PasswordReset, db: Session = Depends(get_db)):
    """重置密码（通过验证码）
    Args:
        data: 密码重置请求
        db: 数据库会话

    Returns:
        Token: 包含新访问令牌的响应

    Raises:
        HTTPException: 如果用户不存在或验证码无效
    """
    # 查找用户
    user = get_user_by_phone(db, data.phone)
    if not user:
        raise HTTPException(status_code=404, detail="用户不存在")

    # 验证验证码
    _verify_sms(data.phone, data.code, "password_reset", db)

    # 更新密码
    _update_password(user, data.new_password, db)

    # 创建新的访问令牌
    return _create_token(user, db)


# ============================================================
# 聊天相关接口
# ============================================================

client = httpx.AsyncClient(
    timeout=httpx.Timeout(60.0, read=60.0),
)
# RAG 服务地址和超时设置
RAG_URL = "http://localhost:6000/ask/stream"
TIMEOUT = httpx.Timeout(60.0, read=60.0)


async def stream_from_rag(payload: dict):
    try:
        print("-----------", flush=True)
        async with client.stream("POST", RAG_URL, json=payload) as resp:
            print("===========", flush=True)
            if resp.status_code != 200:
                detail = await resp.text()
                yield f"RAG服务错误: {resp.status_code}, {detail}\\n".encode()
                return
            async for line in resp.aiter_lines():
                if line:
                    yield (line + "\\n").encode()
    except httpx.ConnectTimeout:
        yield "连接RAG服务超时\\n".encode()
    except httpx.RequestError as e:
        yield f"请求RAG服务失败: {e}\\n".encode()
    finally:
        yield b""


@app.post("/chat/stream", response_model=None)
async def chat_stream(chat_req: ChatRequest, current_user: User = Depends(get_current_user)):
    request_id = str(uuid.uuid4())[:8]
    print(f"[{request_id}] 用户{current_user.phone} 请求: {chat_req.content[:30]}...")
    payload = {
        "request_id": request_id,
        "question": chat_req.content,
        "history": [{"role": m.role, "content": m.content} for m in chat_req.history or []],
    }
    return StreamingResponse(stream_from_rag(payload), media_type="text/plain")


# ============================================================
# 应用入口
# ============================================================

if __name__ == "__main__":
    uvicorn.run("main:app", host="0.0.0.0", port=8000)
