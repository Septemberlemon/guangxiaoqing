from sqlalchemy import Column, Integer, String, DateTime, Boolean
from sqlalchemy.sql import func
from database import Base
from datetime import datetime, timedelta


# ============================================================
# 数据库模型定义
# ============================================================

class User(Base):
    """用户表，存储用户基本信息和认证信息"""
    __tablename__ = "users"

    id = Column(Integer, primary_key=True, index=True)  # 用户ID
    phone = Column(String, unique=True, index=True)  # 手机号（唯一）
    hashed_password = Column(String)  # 哈希后的密码
    current_jti = Column(String, nullable=True, index=True)  # 当前有效token的JTI，用于防止多处登录
    created_at = Column(DateTime, default=lambda: datetime.now(), nullable=False)  # 创建时间
    updated_at = Column(DateTime, default=lambda: datetime.now(), nullable=False)  # 更新时间


class SMSRecord(Base):
    """短信验证码记录表，用于跟踪短信发送次数、频率和验证码使用状态"""
    __tablename__ = "sms_records"

    id = Column(Integer, primary_key=True, index=True)  # 记录ID
    code = Column(String, index=True)  # 验证码
    phone = Column(String, index=True)  # 手机号
    is_used = Column(Boolean, default=False, index=True)  # 是否已使用
    send_time = Column(DateTime, default=lambda: datetime.now(), nullable=False)  # 发送时间
    sms_type = Column(String)  # 短信类型（注册、重置密码等）

    @classmethod
    def count_monthly_sms(cls, db, phone):
        """统计指定手机号在过去30天内的短信发送次数

        Args:
            db: 数据库会话
            phone: 手机号

        Returns:
            int: 过去30天内的短信发送次数
        """
        thirty_days_ago = datetime.now() - timedelta(days=30)
        return db.query(cls).filter(
            cls.phone == phone,
            cls.send_time >= thirty_days_ago
        ).count()
