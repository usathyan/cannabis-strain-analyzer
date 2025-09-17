"""
Authentication configuration with Google OAuth
"""

import os
from fastapi_users import FastAPIUsers
from fastapi_users.authentication import AuthenticationBackend, BearerTransport, JWTStrategy
from httpx_oauth.clients.google import GoogleOAuth2
from database import User, get_user_manager

# OAuth configuration
GOOGLE_CLIENT_ID = os.getenv("GOOGLE_CLIENT_ID", "your-google-client-id")
GOOGLE_CLIENT_SECRET = os.getenv("GOOGLE_CLIENT_SECRET", "your-google-client-secret")
SECRET_KEY = os.getenv("SECRET_KEY", "your-secret-key-here")

# Google OAuth client
google_oauth_client = GoogleOAuth2(GOOGLE_CLIENT_ID, GOOGLE_CLIENT_SECRET)

# JWT Strategy
def get_jwt_strategy() -> JWTStrategy:
    return JWTStrategy(secret=SECRET_KEY, lifetime_seconds=3600)

# Bearer transport
bearer_transport = BearerTransport(tokenUrl="auth/jwt/login")

# Authentication backend
auth_backend = AuthenticationBackend(
    name="jwt",
    transport=bearer_transport,
    get_strategy=get_jwt_strategy,
)

# FastAPI Users
fastapi_users = FastAPIUsers[User, int](get_user_manager, [auth_backend])

# Current user dependency
current_active_user = fastapi_users.current_user(active=True)
current_superuser = fastapi_users.current_user(active=True, superuser=True)

