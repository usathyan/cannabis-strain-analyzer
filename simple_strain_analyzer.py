"""
Simple Cannabis Strain Analyzer
- No authentication required
- Select favorite strains
- Analyze any strain against your preferences
"""

import os
import json
from typing import List, Dict, Any, Optional
from pathlib import Path

from fastapi import FastAPI, Request, Form, HTTPException
from fastapi.responses import HTMLResponse, RedirectResponse
from fastapi.templating import Jinja2Templates
from fastapi.staticfiles import StaticFiles

from enhanced_strain_database import EnhancedStrainDatabase

# Initialize FastAPI app
app = FastAPI(title="Simple Cannabis Strain Analyzer", version="1.0.0")

# Templates
templates = Jinja2Templates(directory="templates")

# Initialize strain database
strain_db = EnhancedStrainDatabase()

# Simple in-memory storage for demo (in production, use a database)
user_profiles = {}

@app.get("/", response_class=HTMLResponse)
async def home(request: Request):
    """Home page"""
    return templates.TemplateResponse("simple_home.html", {"request": request})

@app.get("/select-favorites", response_class=HTMLResponse)
async def select_favorites(request: Request):
    """Page for selecting favorite strains"""
    # Get all available strains
    all_strains = list(strain_db.strains.keys())
    return templates.TemplateResponse("simple_favorites.html", {
        "request": request,
        "all_strains": all_strains
    })

@app.post("/save-favorites")
async def save_favorites(
    request: Request,
    favorite_strains: List[str] = Form(...)
):
    """Save user's favorite strains and compute terpene profile"""
    try:
        # For demo purposes, use a simple session ID
        session_id = "demo_user"
        
        # Compute aggregated terpene profile
        if favorite_strains:
            terpene_profiles = strain_db.compute_aggregated_terpene_profile(favorite_strains)
            user_profiles[session_id] = {
                "favorite_strains": favorite_strains,
                "terpene_profiles": terpene_profiles
            }
        else:
            user_profiles[session_id] = {
                "favorite_strains": [],
                "terpene_profiles": {}
            }
        
        return RedirectResponse("/dashboard", status_code=303)
        
    except Exception as e:
        raise HTTPException(status_code=400, detail=f"Failed to save favorites: {str(e)}")

@app.get("/dashboard", response_class=HTMLResponse)
async def dashboard(request: Request):
    """User dashboard - shows favorite strains and analysis"""
    session_id = "demo_user"
    user_profile = user_profiles.get(session_id, {"favorite_strains": [], "terpene_profiles": {}})
    
    return templates.TemplateResponse("simple_dashboard.html", {
        "request": request,
        "favorite_strains": user_profile.get("favorite_strains", []),
        "terpene_profiles": user_profile.get("terpene_profiles", {})
    })

@app.get("/analyze", response_class=HTMLResponse)
async def analyze_page(request: Request):
    """Simple strain analysis page"""
    return templates.TemplateResponse("simple_analyze.html", {"request": request})

@app.post("/analyze-strain")
async def analyze_strain(
    request: Request,
    strain_name: str = Form(...)
):
    """Analyze a strain against user's favorite profile"""
    try:
        # Get strain data
        strain_data = strain_db.get_strain(strain_name)
        if not strain_data:
            raise HTTPException(status_code=404, detail=f"Strain '{strain_name}' not found")
        
        # Get user's aggregated terpene profile
        session_id = "demo_user"
        user_profile = user_profiles.get(session_id, {"terpene_profiles": {}})
        user_terpene_profile = user_profile.get("terpene_profiles", {})
        
        # Perform analysis
        analysis = strain_db.analyze_strain_against_profile(strain_name, user_terpene_profile)
        
        return templates.TemplateResponse("simple_results.html", {
            "request": request,
            "strain_name": strain_name,
            "strain_data": strain_data,
            "analysis": analysis,
            "user_profile": user_terpene_profile
        })
        
    except Exception as e:
        raise HTTPException(status_code=400, detail=f"Analysis failed: {str(e)}")

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
