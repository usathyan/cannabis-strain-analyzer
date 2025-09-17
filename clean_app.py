"""
Clean Cannabis Strain Analyzer - ONLY the requested functionality:
1. User selects favorite strains
2. System analyzes terpene profiles and stores user profile
3. User enters strain name to compare against their profile
4. System shows analysis and recommendation
"""

from fastapi import FastAPI, Request, Form, HTTPException
from fastapi.responses import HTMLResponse, RedirectResponse
from fastapi.templating import Jinja2Templates
from enhanced_strain_database import EnhancedStrainDatabase

app = FastAPI(title="Cannabis Strain Analyzer")
templates = Jinja2Templates(directory="templates")
strain_db = EnhancedStrainDatabase()

# Simple storage for demo
user_profile = {"favorite_strains": [], "terpene_profiles": {}}

@app.get("/", response_class=HTMLResponse)
async def home(request: Request):
    return templates.TemplateResponse("clean_home.html", {"request": request})

@app.get("/select-favorites", response_class=HTMLResponse)
async def select_favorites(request: Request):
    all_strains = list(strain_db.strains.keys())
    return templates.TemplateResponse("clean_favorites.html", {
        "request": request,
        "all_strains": all_strains,
        "current_favorites": user_profile["favorite_strains"]
    })

@app.post("/save-favorites")
async def save_favorites(favorite_strains: list = Form(...)):
    user_profile["favorite_strains"] = favorite_strains
    if favorite_strains:
        user_profile["terpene_profiles"] = strain_db.compute_aggregated_terpene_profile(favorite_strains)
    return RedirectResponse("/dashboard", status_code=303)

@app.get("/dashboard", response_class=HTMLResponse)
async def dashboard(request: Request):
    return templates.TemplateResponse("clean_dashboard.html", {
        "request": request,
        "favorite_strains": user_profile["favorite_strains"],
        "terpene_profiles": user_profile["terpene_profiles"]
    })

@app.get("/analyze", response_class=HTMLResponse)
async def analyze_page(request: Request):
    return templates.TemplateResponse("clean_analyze.html", {"request": request})

@app.post("/analyze-strain")
async def analyze_strain(strain_name: str = Form(...)):
    strain_data = strain_db.get_strain(strain_name)
    if not strain_data:
        raise HTTPException(status_code=404, detail=f"Strain '{strain_name}' not found")
    
    analysis = strain_db.analyze_strain_against_profile(strain_name, user_profile["terpene_profiles"])
    
    return templates.TemplateResponse("clean_results.html", {
        "request": Request,
        "strain_name": strain_name,
        "strain_data": strain_data,
        "analysis": analysis
    })

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
