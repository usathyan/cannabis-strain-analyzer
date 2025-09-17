"""
Streamlined Cannabis Strain Recommendation App
- Google OAuth authentication
- Favorite strains selection
- Terpene profile analysis and storage
- Simple strain comparison interface
"""

import os
import json
from typing import List, Dict, Any, Optional
from datetime import datetime
from pathlib import Path

from fastapi import FastAPI, Depends, HTTPException, Request, Form
from fastapi.responses import HTMLResponse, RedirectResponse
from fastapi.staticfiles import StaticFiles
from fastapi.templating import Jinja2Templates
from fastapi.security import HTTPBearer
from sqlalchemy.orm import Session

from database import get_user_db, User, engine
from auth import fastapi_users, current_active_user, google_oauth_client
from enhanced_strain_database import EnhancedStrainDatabase
from models import UserProfileUpdate, UserProfileResponse, StrainRecommendationRequest

# Initialize FastAPI app
app = FastAPI(title="Cannabis Strain Analyzer", version="2.0.0")

# Templates
templates = Jinja2Templates(directory="templates")

# Initialize strain database
strain_db = EnhancedStrainDatabase()

# Security
security = HTTPBearer()

@app.get("/", response_class=HTMLResponse)
async def home(request: Request):
    """Home page - redirects to login if not authenticated"""
    return templates.TemplateResponse("streamlined_home.html", {"request": request})

@app.get("/auth/google")
async def google_auth():
    """Initiate Google OAuth flow"""
    authorization_url = await google_oauth_client.get_authorization_url(
        redirect_uri="http://localhost:8000/auth/google/callback",
        scope=["openid", "email", "profile"]
    )
    return RedirectResponse(authorization_url)

@app.get("/auth/google/callback")
async def google_callback(request: Request, code: str):
    """Handle Google OAuth callback"""
    try:
        # Exchange code for token
        token = await google_oauth_client.get_access_token(
            code=code,
            redirect_uri="http://localhost:8000/auth/google/callback"
        )
        
        # Get user info
        user_info = await google_oauth_client.get_id_email(token["access_token"])
        
        # Check if user exists, create if not
        db = next(get_user_db())
        user = db.get_by_email(user_info.email)
        
        if not user:
            # Create new user
            user = User(
                email=user_info.email,
                hashed_password="",  # OAuth users don't need password
                is_active=True,
                is_verified=True,
                name=user_info.email.split("@")[0],  # Use email prefix as name
                favorite_strains=[],
                terpene_profiles={}
            )
            db.create(user)
        
        # Redirect to dashboard
        return RedirectResponse("/dashboard")
        
    except Exception as e:
        raise HTTPException(status_code=400, detail=f"Authentication failed: {str(e)}")

@app.get("/dashboard", response_class=HTMLResponse)
async def dashboard(request: Request, user: User = Depends(current_active_user)):
    """User dashboard - shows favorite strains and analysis"""
    return templates.TemplateResponse("streamlined_dashboard.html", {
        "request": request,
        "user": user,
        "favorite_strains": user.favorite_strains or [],
        "terpene_profiles": user.terpene_profiles or {}
    })

@app.get("/select-favorites", response_class=HTMLResponse)
async def select_favorites(request: Request, user: User = Depends(current_active_user)):
    """Page for selecting favorite strains"""
    # Get all available strains
    all_strains = list(strain_db.strains.keys())
    return templates.TemplateResponse("streamlined_favorites.html", {
        "request": request,
        "user": user,
        "all_strains": all_strains,
        "current_favorites": user.favorite_strains or []
    })

@app.post("/save-favorites")
async def save_favorites(
    request: Request,
    user: User = Depends(current_active_user),
    favorite_strains: List[str] = Form(...)
):
    """Save user's favorite strains and compute terpene profile"""
    try:
        # Update user's favorite strains
        db = next(get_user_db())
        user.favorite_strains = favorite_strains
        
        # Compute aggregated terpene profile
        if favorite_strains:
            terpene_profiles = strain_db.compute_aggregated_terpene_profile(favorite_strains)
            user.terpene_profiles = terpene_profiles
        else:
            user.terpene_profiles = {}
        
        # Update database
        db.update(user)
        
        return RedirectResponse("/dashboard", status_code=303)
        
    except Exception as e:
        raise HTTPException(status_code=400, detail=f"Failed to save favorites: {str(e)}")

@app.get("/analyze", response_class=HTMLResponse)
async def analyze_page(request: Request, user: User = Depends(current_active_user)):
    """Simple strain analysis page"""
    return templates.TemplateResponse("streamlined_analyze.html", {
        "request": request,
        "user": user
    })

@app.post("/analyze-strain")
async def analyze_strain(
    request: Request,
    strain_name: str = Form(...),
    user: User = Depends(current_active_user)
):
    """Analyze a strain against user's favorite profile"""
    try:
        # Get strain data
        strain_data = strain_db.get_strain(strain_name)
        if not strain_data:
            raise HTTPException(status_code=404, detail=f"Strain '{strain_name}' not found")
        
        # Get user's aggregated terpene profile
        user_profile = user.terpene_profiles or {}
        
        # Perform analysis
        analysis = strain_db.analyze_strain_against_profile(strain_name, user_profile)
        
        return templates.TemplateResponse("streamlined_results.html", {
            "request": request,
            "user": user,
            "strain_name": strain_name,
            "strain_data": strain_data,
            "analysis": analysis,
            "user_profile": user_profile
        })
        
    except Exception as e:
        raise HTTPException(status_code=400, detail=f"Analysis failed: {str(e)}")

@app.get("/logout")
async def logout():
    """Logout user"""
    return RedirectResponse("/")

# Add the authentication routes
app.include_router(fastapi_users.get_auth_router(auth_backend), prefix="/auth/jwt", tags=["auth"])

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
