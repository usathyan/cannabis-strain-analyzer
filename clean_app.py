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

@app.post("/lookup-strain")
async def lookup_strain(strain_name: str = Form(...)):
    """Look up a strain and add it to the database if not found"""
    strain_data = strain_db.get_strain(strain_name)
    
    if strain_data:
        # Strain exists, redirect to select favorites
        return RedirectResponse("/select-favorites", status_code=303)
    else:
        # Strain not found, show form to add it
        return templates.TemplateResponse("clean_add_strain.html", {
            "request": Request,
            "strain_name": strain_name
        })

@app.post("/add-custom-strain")
async def add_custom_strain(
    strain_name: str = Form(...),
    strain_type: str = Form(...),
    description: str = Form(...),
    effects: str = Form(...),
    flavors: str = Form(...)
):
    """Add a custom strain to the database"""
    try:
        # Parse effects and flavors
        effects_list = [e.strip() for e in effects.split(",") if e.strip()]
        flavors_list = [f.strip() for f in flavors.split(",") if f.strip()]
        
        # Create basic terpene profile (user can refine later)
        terpene_profile = {
            "myrcene": 0.5,
            "caryophyllene": 0.4,
            "pinene": 0.3,
            "limonene": 0.3,
            "linalool": 0.2,
            "humulene": 0.2,
            "terpinolene": 0.1
        }
        
        # Create strain data
        strain_data = {
            "terpenes": terpene_profile,
            "effects": effects_list,
            "medical_effects": [],
            "type": strain_type.lower(),
            "thc_range": "15-25%",
            "cbd_range": "0.1-0.5%",
            "description": description,
            "flavors": flavors_list,
            "aromas": flavors_list,
            "best_time": "any",
            "activity": "balanced"
        }
        
        # Add to database
        strain_db.add_custom_strain(strain_name, strain_data)
        
        return RedirectResponse("/select-favorites", status_code=303)
        
    except Exception as e:
        raise HTTPException(status_code=400, detail=f"Failed to add strain: {str(e)}")

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
