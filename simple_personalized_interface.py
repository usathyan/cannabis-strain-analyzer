"""
Simplified Personalized Cannabis Strain Recommendation Web Interface
"""

from fastapi import FastAPI, Request, Depends, HTTPException
from fastapi.responses import HTMLResponse
from fastapi.templating import Jinja2Templates
from typing import Optional, Dict, Any
import asyncio
import os
from dotenv import load_dotenv

# Load environment variables
load_dotenv()

# Import our modules
from enhanced_strain_database import EnhancedStrainDatabase
from google_maps_integration import LocationService
from user_profiles import UserProfileService
from models import (
    UserProfileUpdate, UserProfileResponse, StrainRecommendationRequest,
    PersonalizedRecommendationResponse, StrainAnalysis, TerpeneAnalysis,
    PersonalRating, SimilarityScore, StrainRecommendation, DispensaryInfo
)

# Initialize FastAPI app
app = FastAPI(title="Personalized Cannabis Strain Recommendations")

# Initialize services
strain_db = EnhancedStrainDatabase()
location_service = LocationService()
profile_service = UserProfileService()

# Templates
templates = Jinja2Templates(directory="templates")

# Simple user session management (in production, use proper authentication)
current_user_id = None

# Routes

@app.get("/", response_class=HTMLResponse)
async def root(request: Request):
    """Serve the main personalized interface"""
    return templates.TemplateResponse("simple_personalized_index.html", {
        "request": request,
        "is_authenticated": current_user_id is not None,
        "user_id": current_user_id
    })

@app.get("/config", response_class=HTMLResponse)
async def config_page(request: Request):
    """Serve the user configuration page"""
    if not current_user_id:
        return templates.TemplateResponse("simple_personalized_index.html", {
            "request": request,
            "is_authenticated": False,
            "user_id": None,
            "message": "Please set a user ID first"
        })
    
    profile = profile_service.get_user_profile(current_user_id)
    return templates.TemplateResponse("simple_config.html", {
        "request": request,
        "user_id": current_user_id,
        "profile": profile
    })

@app.post("/api/set-user")
async def set_user(user_id: int):
    """Set the current user ID (for demo purposes)"""
    global current_user_id
    current_user_id = user_id
    return {"message": f"User ID set to {user_id}"}

@app.post("/api/profile", response_model=UserProfileResponse)
async def update_profile(profile_data: UserProfileUpdate):
    """Update user profile and compute terpene profiles"""
    if not current_user_id:
        raise HTTPException(status_code=401, detail="No user ID set")
    
    try:
        updated_profile = profile_service.update_user_profile(
            current_user_id,
            profile_data.name,
            profile_data.address,
            profile_data.radius,
            profile_data.favorite_strains
        )
        return UserProfileResponse(
            id=current_user_id,
            email=f"user{current_user_id}@example.com",
            **updated_profile
        )
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))

@app.get("/api/profile", response_model=UserProfileResponse)
async def get_profile():
    """Get user profile"""
    if not current_user_id:
        raise HTTPException(status_code=401, detail="No user ID set")
    
    profile = profile_service.get_user_profile(current_user_id)
    if not profile:
        raise HTTPException(status_code=404, detail="Profile not found")
    return UserProfileResponse(**profile)

@app.get("/api/recommendations", response_model=PersonalizedRecommendationResponse)
async def get_personalized_recommendations(strain_name: str):
    """Get personalized strain recommendations based on user's favorite strains"""
    if not current_user_id:
        raise HTTPException(status_code=401, detail="No user ID set")
    
    # Get strain data
    strain_data = strain_db.get_strain(strain_name)
    if not strain_data:
        # Try to generate strain data using LLM
        print(f"🤖 NO STRAIN DATA FOUND - Generating strain data for: {strain_name}")
        try:
            # Create event loop for async call
            loop = asyncio.new_event_loop()
            asyncio.set_event_loop(loop)
            strain_data = loop.run_until_complete(generate_strain_data(strain_name))
            loop.close()
            if strain_data:
                strain_db.add_custom_strain(strain_name, strain_data)
                print(f"✅ Added new strain to database: {strain_name}")
            else:
                raise HTTPException(
                    status_code=404,
                    detail=f"Could not generate data for '{strain_name}'"
                )
        except Exception as e:
            print(f"❌ Error generating strain data: {e}")
            raise HTTPException(
                status_code=500,
                detail=f"Error generating strain data: {str(e)}"
            )
    
    # Build strain analysis
    terpenes = strain_data.get("terpenes", {})
    terpene_analysis = profile_service._build_terpene_analysis(terpenes)
    
    strain_analysis = StrainAnalysis(
        name=strain_data.get("name", strain_name),
        type=strain_data.get("type", "unknown"),
        thc_range=strain_data.get("thc_range", "unknown"),
        cbd_range=strain_data.get("cbd_range", "unknown"),
        description=strain_data.get("description", "No description available"),
        effects=strain_data.get("effects", []),
        flavors=strain_data.get("flavors", []),
        terpenes=[TerpeneAnalysis(**terpene) for terpene in terpene_analysis]
    )
    
    # Get personal rating comparison
    personal_rating_data = profile_service.compare_strain_to_favorites(
        current_user_id, strain_name, terpenes
    )
    
    personal_rating = PersonalRating(
        similarity_scores=[SimilarityScore(**score) for score in personal_rating_data["similarity_scores"]],
        average_similarity=personal_rating_data["average_similarity"],
        average_similarity_percentage=personal_rating_data["average_similarity_percentage"],
        best_match=SimilarityScore(**personal_rating_data["best_match"]) if personal_rating_data["best_match"] else None,
        personal_rating=personal_rating_data["personal_rating"]
    )
    
    # Get similar strains
    similar_strains = strain_db.get_similar_strains(strain_name, limit=3)
    recommendations = [
        StrainRecommendation(
            strain_name=strain,
            similarity=similarity,
            strain_data=strain_db.get_strain(strain)
        )
        for strain, similarity in similar_strains
    ]
    
    # Get dispensaries
    user_profile = profile_service.get_user_profile(current_user_id)
    dispensaries = []
    if user_profile and user_profile.get("address"):
        try:
            dispensary_data = location_service.find_dispensaries_near_location(
                user_profile["address"], user_profile.get("radius", 25)
            )
            dispensaries = [
                DispensaryInfo(
                    name=disp["name"],
                    address=disp["address"],
                    distance_miles=disp["distance_miles"],
                    rating=disp["rating"],
                    phone=disp.get("phone"),
                    opening_hours=disp.get("opening_hours"),
                    website=disp.get("website")
                )
                for disp in dispensary_data
            ]
        except Exception as e:
            print(f"⚠️ Dispensary search failed: {e}")
    
    return PersonalizedRecommendationResponse(
        strain_analysis=strain_analysis,
        personal_rating=personal_rating,
        recommendations=recommendations,
        dispensaries=dispensaries,
        user_location=user_profile.get("address") if user_profile else None
    )

# Helper function for generating strain data (copied from original web_interface.py)
async def generate_strain_data(strain_name: str) -> Optional[Dict[str, Any]]:
    """Generate strain data using LLM"""
    try:
        from langchain_ollama import ChatOllama
        
        llm = ChatOllama(
            model="llama3.2:latest",
            base_url="http://localhost:11434",
            temperature=0.7
        )
        
        prompt = f"""
        Generate detailed cannabis strain information for "{strain_name}" in JSON format.
        Include: terpenes (as decimal percentages), effects, type (indica/sativa/hybrid), 
        thc_range, cbd_range, description, flavors.
        
        Example format:
        {{
            "terpenes": {{"myrcene": 0.5, "limonene": 0.3, "caryophyllene": 0.2}},
            "effects": ["relaxed", "happy", "uplifted"],
            "type": "hybrid",
            "thc_range": "18-24%",
            "cbd_range": "0.1-0.5%",
            "description": "A balanced hybrid with...",
            "flavors": ["sweet", "citrus", "earthy"]
        }}
        """
        
        response = await llm.ainvoke(prompt)
        content = response.content
        
        # Clean up response
        if content.startswith("```json"):
            content = content[7:]
        if content.startswith("```"):
            content = content[3:]
        if content.endswith("```"):
            content = content[:-3]
        
        content = content.strip()
        
        # Parse JSON
        import json
        strain_data = json.loads(content)
        
        # Validate required fields
        required_fields = ["terpenes", "effects", "type", "thc_range", "description"]
        for field in required_fields:
            if field not in strain_data:
                print(f"Missing required field: {field}")
                return None
        
        # Add the strain name to the data
        strain_data["name"] = strain_name
        
        return strain_data
        
    except Exception as e:
        print(f"Error generating strain data for {strain_name}: {e}")
        return None

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="127.0.0.1", port=8000)

