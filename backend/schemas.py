from pydantic import BaseModel, Field
from typing import Optional, Literal, Annotated, List


# ============================================================
# 基础字段定义
# ============================================================

# 手机号字段：11位数字，以 1 开头，第二位是 3-9
Phone = Annotated[
    str,
    Field(..., min_length=11, max_length=11, pattern=r'^1[3-9]\d{9}$')
]

# 密码字段：6-20位，允许字母、数字和特殊字符
Password = Annotated[
    str,
    Field(..., min_length=6, max_length=20, pattern=r'^[A-Za-z0-9_!@#$%^&*]{6,20}$')
]


# ============================================================
# 用户身份验证相关模型
# ============================================================

# 用户注册请求模型
class UserCreate(BaseModel):
    phone: Phone  # 手机号
    password: Password  # 密码
    code: str  # 验证码


# 用户登录请求模型
class UserLogin(BaseModel):
    phone: Phone  # 手机号
    password: Password  # 密码


# 修改密码请求模型
class PasswordChange(BaseModel):
    phone: Phone  # 手机号
    old_password: Password  # 原密码
    new_password: Password  # 新密码


# 重置密码请求模型
class PasswordReset(BaseModel):
    phone: Phone  # 手机号
    code: str  # 验证码
    new_password: Password  # 新密码


# 访问令牌响应模型
class Token(BaseModel):
    access_token: str  # 访问令牌
    token_type: str  # 令牌类型，通常为 "bearer"


# 令牌数据模型（用于解析令牌）
class TokenData(BaseModel):
    phone: Optional[str] = None  # 手机号
    jti: Optional[str] = None  # JWT ID


# ============================================================
# 短信验证码相关模型
# ============================================================

# 发送短信验证码请求模型
class SendSMSRequest(BaseModel):
    phone: Phone  # 手机号
    type: Literal["registration", "password_reset"]  # 用途：注册或密码重置


# ============================================================
# 聊天相关模型
# ============================================================

# 聊天消息模型
class ChatMessage(BaseModel):
    role: Literal["user", "assistant"]  # 角色：用户或助手
    content: str  # 消息内容


# 聊天请求模型
class ChatRequest(BaseModel):
    content: str  # 当前消息内容
    history: Optional[List[ChatMessage]] = None  # 历史消息列表（可选）


# 聊天响应模型
class ChatResponse(BaseModel):
    content: str  # AI 助手的回复内容


# 发送短信验证码响应模型
class SMSResponse(BaseModel):
    message: str  # 发送状态消息
    remaining: int  # 剩余发送次数
