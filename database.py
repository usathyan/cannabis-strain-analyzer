"""
Database configuration and models for user authentication and profiles
"""

from sqlalchemy import Column, Integer, String, Boolean, DateTime, Text, JSON, Float
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import Session
from sqlalchemy import create_engine
from fastapi_users.db import SQLAlchemyBaseUserTable, SQLAlchemyUserDatabase
from fastapi_users import BaseUserManager
from fastapi import Depends
from typing import Optional
import os
from datetime import datetime

# Database URL
DATABASE_URL = os.getenv("DATABASE_URL", "sqlite:///./cannabis_app.db")

# Create database engine
engine = create_engine(DATABASE_URL, connect_args={"check_same_thread": False} if "sqlite" in DATABASE_URL else {})

# Base class for models
Base = declarative_base()

# User model
class User(SQLAlchemyBaseUserTable[int], Base):
    __tablename__ = "users"
    
    id = Column(Integer, primary_key=True, index=True)
    email = Column(String, unique=True, index=True, nullable=False)
    hashed_password = Column(String, nullable=False)
    is_active = Column(Boolean, default=True)
    is_superuser = Column(Boolean, default=False)
    is_verified = Column(Boolean, default=False)
    
    # Profile information
    name = Column(String, nullable=True)
    address = Column(String, nullable=True)
    radius = Column(Integer, default=25)
    favorite_strains = Column(JSON, default=list)  # List of strain names
    terpene_profiles = Column(JSON, default=dict)  # Computed terpene profiles for favorites
    created_at = Column(DateTime, default=datetime.utcnow)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)

# Create tables
Base.metadata.create_all(bind=engine)

# User database dependency
def get_user_db():
    with Session(engine) as session:
        yield SQLAlchemyUserDatabase(session, User)

# User manager
class UserManager(BaseUserManager[User, int]):
    reset_password_token_secret = os.getenv("SECRET_KEY", "your-secret-key-here")
    verification_token_secret = os.getenv("SECRET_KEY", "your-secret-key-here")

def get_user_manager(user_db=Depends(get_user_db)):
    yield UserManager(user_db)

