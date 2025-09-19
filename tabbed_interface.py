"""
Tabbed Cannabis Strain Recommendation Interface
"""

from fastapi import FastAPI, Request, Depends, HTTPException
from fastapi.responses import HTMLResponse
from fastapi.templating import Jinja2Templates
from typing import Optional, Dict, Any, List
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
app = FastAPI(title="Tabbed Cannabis Strain Recommendations")

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
    """Serve the main tabbed interface"""
    return templates.TemplateResponse("tabbed_interface.html", {
        "request": request,
        "is_authenticated": current_user_id is not None,
        "user_id": current_user_id
    })

@app.post("/api/set-user")
async def set_user(user_id: int):
    """Set the current user ID (for demo purposes)"""
    global current_user_id
    current_user_id = user_id
    return {"message": f"User ID set to {user_id}"}

@app.get("/api/available-strains")
async def get_available_strains():
    """Get list of all available strains in the database"""
    strains = list(strain_db.strains.keys())
    return {"strains": sorted(strains)}

@app.post("/api/create-ideal-profile")
async def create_ideal_profile(selected_strains: List[str]):
    """Create ideal terpene profile from selected strains"""
    if not current_user_id:
        raise HTTPException(status_code=401, detail="No user ID set")
    
    if not selected_strains:
        raise HTTPException(status_code=400, detail="No strains selected")
    
    try:
        # Get strain data for all selected strains
        strain_data_list = []
        for strain_name in selected_strains:
            strain_data = strain_db.get_strain(strain_name)
            if strain_data:
                strain_data_list.append(strain_data)
            else:
                # Try to generate strain data using LLM
                print(f"🤖 Generating strain data for: {strain_name}")
                try:
                    loop = asyncio.new_event_loop()
                    asyncio.set_event_loop(loop)
                    strain_data = loop.run_until_complete(generate_strain_data(strain_name))
                    loop.close()
                    if strain_data:
                        strain_db.add_custom_strain(strain_name, strain_data)
                        strain_data_list.append(strain_data)
                        print(f"✅ Added new strain to database: {strain_name}")
                except Exception as e:
                    print(f"❌ Error generating strain data for {strain_name}: {e}")
        
        if not strain_data_list:
            raise HTTPException(status_code=400, detail="No valid strain data found")
        
        # Calculate aggregate terpene profile
        ideal_profile = calculate_aggregate_terpene_profile(strain_data_list)
        
        # Save the ideal profile to user's profile
        profile_service.update_user_profile(
            current_user_id,
            f"User {current_user_id}",
            "Default Location",
            25,
            selected_strains
        )
        
        # Update the user's terpene profiles with the ideal profile
        with profile_service.strain_db.strains as session:
            user = session.query(profile_service.User).filter(profile_service.User.id == current_user_id).first()
            if user:
                user.terpene_profiles = {"ideal_profile": ideal_profile}
                session.commit()
        
        return {
            "ideal_profile": ideal_profile,
            "selected_strains": selected_strains,
            "message": "Ideal terpene profile created successfully"
        }
        
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/api/ideal-profile")
async def get_ideal_profile():
    """Get the user's ideal terpene profile"""
    if not current_user_id:
        raise HTTPException(status_code=401, detail="No user ID set")
    
    profile = profile_service.get_user_profile(current_user_id)
    if not profile or not profile.get("terpene_profiles"):
        raise HTTPException(status_code=404, detail="No ideal profile found")
    
    ideal_profile = profile["terpene_profiles"].get("ideal_profile")
    if not ideal_profile:
        raise HTTPException(status_code=404, detail="No ideal profile found")
    
    return ideal_profile

@app.post("/api/compare-strain")
async def compare_strain(strain_name: str):
    """Compare a strain against the user's ideal profile"""
    if not current_user_id:
        raise HTTPException(status_code=401, detail="No user ID set")
    
    try:
        # Get strain data
        strain_data = strain_db.get_strain(strain_name)
        if not strain_data:
            # Try to generate strain data using LLM
            print(f"🤖 Generating strain data for: {strain_name}")
            try:
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
        
        # Get ideal profile
        ideal_profile = await get_ideal_profile()
        
        # Compare strain against ideal profile
        comparison = compare_against_ideal_profile(strain_data, ideal_profile)
        
        # Generate LLM-based analysis
        llm_analysis = await generate_llm_analysis(strain_data, ideal_profile, comparison)
        
        return {
            "strain_analysis": {
                "name": strain_data.get("name", strain_name),
                "type": strain_data.get("type", "unknown"),
                "thc_range": strain_data.get("thc_range", "unknown"),
                "cbd_range": strain_data.get("cbd_range", "unknown"),
                "description": strain_data.get("description", "No description available"),
                "effects": strain_data.get("effects", []),
                "flavors": strain_data.get("flavors", []),
                "terpenes": profile_service._build_terpene_analysis(strain_data.get("terpenes", {}))
            },
            "ideal_profile": ideal_profile,
            "comparison": comparison,
            "llm_analysis": llm_analysis
        }
        
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

def calculate_aggregate_terpene_profile(strain_data_list: List[Dict[str, Any]]) -> Dict[str, Any]:
    """Calculate aggregate terpene profile from multiple strains"""
    all_terpenes = {}
    total_strains = len(strain_data_list)
    
    # Collect all terpenes from all strains
    for strain_data in strain_data_list:
        terpenes = strain_data.get("terpenes", {})
        for terpene, value in terpenes.items():
            if terpene not in all_terpenes:
                all_terpenes[terpene] = []
            all_terpenes[terpene].append(value)
    
    # Calculate average for each terpene
    aggregate_terpenes = {}
    for terpene, values in all_terpenes.items():
        aggregate_terpenes[terpene] = sum(values) / len(values)
    
    # Sort by average value (highest first)
    sorted_terpenes = sorted(aggregate_terpenes.items(), key=lambda x: x[1], reverse=True)
    
    # Build detailed analysis
    terpene_analysis = profile_service._build_terpene_analysis(aggregate_terpenes)
    
    # Calculate aggregate effects
    all_effects = []
    for strain_data in strain_data_list:
        all_effects.extend(strain_data.get("effects", []))
    
    # Count effect frequency
    effect_counts = {}
    for effect in all_effects:
        effect_counts[effect] = effect_counts.get(effect, 0) + 1
    
    # Sort effects by frequency
    sorted_effects = sorted(effect_counts.items(), key=lambda x: x[1], reverse=True)
    top_effects = [effect for effect, count in sorted_effects[:10]]  # Top 10 effects
    
    return {
        "aggregate_terpenes": aggregate_terpenes,
        "terpene_analysis": terpene_analysis,
        "top_effects": top_effects,
        "strain_count": total_strains,
        "dominant_terpenes": [terpene for terpene, value in sorted_terpenes[:5]],
        "terpene_diversity": len(aggregate_terpenes)
    }

def compare_against_ideal_profile(strain_data: Dict[str, Any], ideal_profile: Dict[str, Any]) -> Dict[str, Any]:
    """Compare a strain against the ideal profile"""
    strain_terpenes = strain_data.get("terpenes", {})
    ideal_terpenes = ideal_profile.get("aggregate_terpenes", {})
    
    # Get all terpenes for comparison
    all_terpenes = set(strain_terpenes.keys()) | set(ideal_terpenes.keys())
    
    # Create vectors for comparison
    strain_vector = [strain_terpenes.get(terpene, 0.0) for terpene in all_terpenes]
    ideal_vector = [ideal_terpenes.get(terpene, 0.0) for terpene in all_terpenes]
    
    # Calculate cosine similarity
    import numpy as np
    from sklearn.metrics.pairwise import cosine_similarity
    
    strain_array = np.array(strain_vector).reshape(1, -1)
    ideal_array = np.array(ideal_vector).reshape(1, -1)
    
    similarity = cosine_similarity(strain_array, ideal_array)[0][0]
    
    # Calculate individual terpene differences
    terpene_differences = {}
    for terpene in all_terpenes:
        strain_value = strain_terpenes.get(terpene, 0.0)
        ideal_value = ideal_terpenes.get(terpene, 0.0)
        difference = strain_value - ideal_value
        terpene_differences[terpene] = {
            "strain_value": strain_value,
            "ideal_value": ideal_value,
            "difference": difference,
            "percentage_diff": (difference / ideal_value * 100) if ideal_value > 0 else 0
        }
    
    # Sort by absolute difference
    sorted_differences = sorted(
        terpene_differences.items(),
        key=lambda x: abs(x[1]["difference"]),
        reverse=True
    )
    
    return {
        "overall_similarity": float(similarity),
        "similarity_percentage": float(similarity * 100),
        "terpene_differences": dict(sorted_differences),
        "match_rating": get_match_rating(similarity)
    }

def get_match_rating(similarity: float) -> str:
    """Get match rating based on similarity score"""
    if similarity >= 0.9:
        return "Perfect Match"
    elif similarity >= 0.8:
        return "Excellent Match"
    elif similarity >= 0.7:
        return "Very Good Match"
    elif similarity >= 0.6:
        return "Good Match"
    elif similarity >= 0.5:
        return "Moderate Match"
    elif similarity >= 0.3:
        return "Poor Match"
    else:
        return "Very Different"

async def generate_llm_analysis(strain_data: Dict[str, Any], ideal_profile: Dict[str, Any], comparison: Dict[str, Any]) -> str:
    """Generate LLM-based analysis and recommendations"""
    try:
        from langchain_ollama import ChatOllama
        
        llm = ChatOllama(
            model="llama3.2:latest",
            base_url="http://localhost:11434",
            temperature=0.7
        )
        
        strain_name = strain_data.get("name", "Unknown Strain")
        strain_type = strain_data.get("type", "unknown")
        strain_effects = strain_data.get("effects", [])
        strain_terpenes = strain_data.get("terpenes", {})
        
        ideal_effects = ideal_profile.get("top_effects", [])
        ideal_terpenes = ideal_profile.get("aggregate_terpenes", {})
        
        similarity = comparison["similarity_percentage"]
        match_rating = comparison["match_rating"]
        
        prompt = f"""
        Analyze this cannabis strain comparison and provide detailed insights:
        
        STRAIN: {strain_name} ({strain_type})
        Effects: {', '.join(strain_effects)}
        Top Terpenes: {', '.join([f"{t}: {v:.2f}" for t, v in sorted(strain_terpenes.items(), key=lambda x: x[1], reverse=True)[:3]])}
        
        IDEAL PROFILE (based on user's favorite strains):
        Expected Effects: {', '.join(ideal_effects[:5])}
        Expected Terpenes: {', '.join([f"{t}: {v:.2f}" for t, v in sorted(ideal_terpenes.items(), key=lambda x: x[1], reverse=True)[:3]])}
        
        COMPARISON RESULTS:
        Similarity: {similarity:.1f}%
        Match Rating: {match_rating}
        
        Please provide:
        1. A brief analysis of how well this strain matches the user's preferences
        2. What effects they can expect compared to their usual strains
        3. Specific terpene differences and their implications
        4. A recommendation on whether this strain would be good for them
        5. Any potential benefits or drawbacks compared to their ideal profile
        
        Keep the response concise but informative (2-3 paragraphs).
        """
        
        response = await llm.ainvoke(prompt)
        return response.content.strip()
        
    except Exception as e:
        print(f"❌ Error generating LLM analysis: {e}")
        return f"Analysis unavailable due to LLM error: {str(e)}"

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
