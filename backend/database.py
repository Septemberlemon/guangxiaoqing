from sqlalchemy import create_engine
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker
import os


# 获取当前文件所在目录
BASEDIR = os.path.dirname(os.path.abspath(__file__))

# 创建SQLite数据库URL
SQLALCHEMY_DATABASE_URL = f"sqlite:///{os.path.join(BASEDIR, 'users.db')}"

# 创建SQLAlchemy引擎
engine = create_engine(
    SQLALCHEMY_DATABASE_URL, connect_args={"check_same_thread": False}
)

# 创建会话工厂
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

# 创建基类
Base = declarative_base()


# 获取数据库会话
def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
