# 用于生成插入用户的hashed_password
from passlib.context import CryptContext


pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")

plain_password = "wcy047318"
hashed = pwd_context.hash(plain_password)  # e.g. "$2b$12$..."
print(hashed)
