"""
Simple Tabbed Cannabis Strain Recommendation Interface
"""

import json
from pathlib import Path
from typing import Any

from fastapi import FastAPI, HTTPException, Request
from fastapi.responses import HTMLResponse
from fastapi.templating import Jinja2Templates

# Import our modules
from enhanced_strain_database import EnhancedStrainDatabase

# Initialize FastAPI app
app = FastAPI(title="Simple Tabbed Cannabis Strain Recommendations")

# Initialize services
strain_db = EnhancedStrainDatabase()

# Templates
templates = Jinja2Templates(directory="templates")

# Simple user session management (in production, use proper authentication)
current_user_id = "1"  # Default user ID for demo

# Fixed schema for comprehensive chemovar analysis
CANNABINOID_SCHEMA = {
    "thc": 0.0,      # THC (0-30%)
    "cbd": 0.0,      # CBD (0-15%)
    "cbg": 0.0,      # CBG (0-2%)
    "cbn": 0.0,      # CBN (0-1%)
    "thcv": 0.0,     # THCV (0-0.5%)
}

TERPENE_SCHEMA = {
    "myrcene": 0.0,        # Most common, sedative
    "limonene": 0.0,       # Citrus, mood elevation
    "caryophyllene": 0.0,  # Spicy, anti-inflammatory
    "pinene": 0.0,         # Pine, alertness
    "linalool": 0.0,       # Floral, calming
    "humulene": 0.0,       # Hoppy, appetite suppressant
    "terpinolene": 0.0,    # Fresh, uplifting
    "ocimene": 0.0,        # Sweet, energizing
    "nerolidol": 0.0,      # Woody, sedative
    "bisabolol": 0.0,      # Floral, anti-inflammatory
    "eucalyptol": 0.0,     # Minty, respiratory
}

# Conservative imputation values for rare features
CONSERVATIVE_IMPUTATION = {
    # Cannabinoids - use very low values for missing ones
    "thc": 0.001,      # 0.1% - minimal THC
    "cbd": 0.001,      # 0.1% - minimal CBD
    "cbg": 0.0005,     # 0.05% - very low CBG
    "cbn": 0.0001,     # 0.01% - very low CBN
    "thcv": 0.00005,   # 0.005% - very low THCV

    # Terpenes - use very low values for missing ones
    "myrcene": 0.001,        # 0.1% - minimal myrcene
    "limonene": 0.001,       # 0.1% - minimal limonene
    "caryophyllene": 0.001,  # 0.1% - minimal caryophyllene
    "pinene": 0.001,         # 0.1% - minimal pinene
    "linalool": 0.001,       # 0.1% - minimal linalool
    "humulene": 0.0005,      # 0.05% - very low humulene
    "terpinolene": 0.0005,   # 0.05% - very low terpinolene
    "ocimene": 0.0005,       # 0.05% - very low ocimene
    "nerolidol": 0.0001,     # 0.01% - very low nerolidol
    "bisabolol": 0.0001,     # 0.01% - very low bisabolol
    "eucalyptol": 0.0001,    # 0.01% - very low eucalyptol
}

# Simple file-based storage for user profiles
PROFILES_FILE = Path("user_profiles.json")

def normalize_to_fixed_schema(strain_data: dict[str, Any]) -> dict[str, Any]:
    """Normalize strain data to fixed schema with conservative imputation"""
    normalized = strain_data.copy()

    # Normalize cannabinoids
    cannabinoids = strain_data.get("cannabinoids", {})
    normalized_cannabinoids = CANNABINOID_SCHEMA.copy()

    for cannabinoid in CANNABINOID_SCHEMA:
        if cannabinoid in cannabinoids:
            normalized_cannabinoids[cannabinoid] = cannabinoids[cannabinoid]
        else:
            # Conservative imputation for missing cannabinoids
            normalized_cannabinoids[cannabinoid] = CONSERVATIVE_IMPUTATION[cannabinoid]

    # Normalize terpenes
    terpenes = strain_data.get("terpenes", {})
    normalized_terpenes = TERPENE_SCHEMA.copy()

    for terpene in TERPENE_SCHEMA:
        if terpene in terpenes:
            normalized_terpenes[terpene] = terpenes[terpene]
        else:
            # Conservative imputation for missing terpenes
            normalized_terpenes[terpene] = CONSERVATIVE_IMPUTATION[terpene]

    normalized["cannabinoids"] = normalized_cannabinoids
    normalized["terpenes"] = normalized_terpenes

    return normalized

def load_user_profiles():
    """Load user profiles from file"""
    if PROFILES_FILE.exists():
        try:
            with open(PROFILES_FILE) as f:
                return json.load(f)
        except Exception as e:
            print(f"Error loading profiles: {e}")
    return {}

def save_user_profiles(profiles):
    """Save user profiles to file"""
    try:
        with open(PROFILES_FILE, 'w') as f:
            json.dump(profiles, f, indent=2)
    except Exception as e:
        print(f"Error saving profiles: {e}")

def get_user_profile(user_id: int) -> dict[str, Any]:
    """Get user profile"""
    profiles = load_user_profiles()
    return profiles.get(str(user_id), {
        "id": user_id,
        "name": f"User {user_id}",
        "address": "Default Location",
        "radius": 25,
        "favorite_strains": [],
        "ranked_favorites": [],  # New: ranked list of favorite strains with their data
        "ideal_profile": None
    })

def save_user_profile(user_id: int, profile: dict[str, Any]):
    """Save user profile"""
    profiles = load_user_profiles()
    profiles[str(user_id)] = profile
    save_user_profiles(profiles)

# Routes

@app.get("/", response_class=HTMLResponse)
async def root(request: Request):
    """Serve the main tabbed interface"""
    return templates.TemplateResponse("index.html", {
        "request": request,
        "is_authenticated": current_user_id is not None,
        "user_id": current_user_id
    })

@app.post("/api/set-user")
async def set_user(request: Request):
    """Set the current user ID (for demo purposes)"""
    global current_user_id
    body = await request.json()
    user_id = body.get("user_id", 1)
    current_user_id = user_id
    return {"message": f"User ID set to {user_id}"}

@app.get("/api/available-strains")
async def get_available_strains():
    """Get list of all available strains in the database"""
    strains = list(strain_db.strains.keys())
    return {"strains": sorted(strains)}

@app.post("/api/create-ideal-profile")
async def create_ideal_profile(selected_strains: list[str]):
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
                    strain_data = await generate_strain_data(strain_name)
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
        user_profile = get_user_profile(current_user_id)
        user_profile["favorite_strains"] = selected_strains
        user_profile["ideal_profile"] = ideal_profile
        save_user_profile(current_user_id, user_profile)

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

    user_profile = get_user_profile(current_user_id)
    ideal_profile = user_profile.get("ideal_profile")

    if not ideal_profile:
        raise HTTPException(status_code=404, detail="No ideal profile found")

    return ideal_profile

@app.post("/api/add-strain-to-profile")
async def add_strain_to_profile(request: dict):
    """Add a single strain to the user's ideal profile"""
    strain_name = request.get("strain_name", "")
    if not strain_name:
        raise HTTPException(status_code=400, detail="Strain name is required")
    if not current_user_id:
        raise HTTPException(status_code=401, detail="No user ID set")

    user_profile = get_user_profile(current_user_id)
    favorite_strains = user_profile.get("favorite_strains", [])

    if strain_name in favorite_strains:
        return {"message": f"Strain '{strain_name}' is already in your profile", "success": False}

    # Add strain to favorites
    favorite_strains.append(strain_name)
    user_profile["favorite_strains"] = favorite_strains

    # Recalculate ideal profile
    try:
        strain_data_list = []
        for strain in favorite_strains:
            strain_data = strain_db.get_strain(strain)
            if strain_data:
                strain_data_list.append(strain_data)
            else:
                # Try to generate strain data using LLM
                try:
                    strain_data = await generate_strain_data(strain)
                    if strain_data:
                        strain_db.add_custom_strain(strain, strain_data)
                        strain_data_list.append(strain_data)
                except Exception as e:
                    print(f"❌ Error generating strain data for {strain}: {e}")

        if strain_data_list:
            ideal_profile = calculate_aggregate_terpene_profile(strain_data_list)
            user_profile["ideal_profile"] = ideal_profile
            save_user_profile(current_user_id, user_profile)

            return {
                "message": f"Strain '{strain_name}' added to profile",
                "success": True,
                "ideal_profile": ideal_profile,
                "favorite_strains": favorite_strains
            }
        else:
            return {"message": "No valid strain data found", "success": False}

    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/api/remove-strain-from-profile")
async def remove_strain_from_profile(request: dict):
    """Remove a single strain from the user's ideal profile"""
    strain_name = request.get("strain_name", "")
    if not strain_name:
        raise HTTPException(status_code=400, detail="Strain name is required")
    if not current_user_id:
        raise HTTPException(status_code=401, detail="No user ID set")

    user_profile = get_user_profile(current_user_id)
    favorite_strains = user_profile.get("favorite_strains", [])

    if strain_name not in favorite_strains:
        return {"message": f"Strain '{strain_name}' not found in your profile", "success": False}

    # Remove strain from favorites
    favorite_strains = [s for s in favorite_strains if s != strain_name]
    user_profile["favorite_strains"] = favorite_strains

    # If no strains left, remove ideal profile
    if not favorite_strains:
        user_profile["ideal_profile"] = None
        save_user_profile(current_user_id, user_profile)
        return {
            "message": f"Strain '{strain_name}' removed. Profile cleared as no strains remain.",
            "success": True,
            "ideal_profile": None,
            "favorite_strains": []
        }

    # Recalculate ideal profile with remaining strains
    try:
        strain_data_list = []
        for strain in favorite_strains:
            strain_data = strain_db.get_strain(strain)
            if strain_data:
                strain_data_list.append(strain_data)

        if strain_data_list:
            ideal_profile = calculate_aggregate_terpene_profile(strain_data_list)
            user_profile["ideal_profile"] = ideal_profile
            save_user_profile(current_user_id, user_profile)

            return {
                "message": f"Strain '{strain_name}' removed from profile",
                "success": True,
                "ideal_profile": ideal_profile,
                "favorite_strains": favorite_strains
            }
        else:
            return {"message": "No valid strain data found", "success": False}

    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/api/user-profile")
async def get_user_profile_endpoint():
    """Get the current user's complete profile"""
    if not current_user_id:
        raise HTTPException(status_code=401, detail="No user ID set")

    user_profile = get_user_profile(current_user_id)
    return user_profile

@app.post("/api/user-profile")
async def update_user_profile_endpoint(request: Request):
    """Update the current user's profile"""
    if not current_user_id:
        raise HTTPException(status_code=401, detail="No user ID set")

    try:
        body = await request.json()
        save_user_profile(current_user_id, body)
        return {"message": "Profile updated successfully"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/api/compare-strain")
async def compare_strain(request: Request):
    """Compare a strain against the user's ideal profile or ranked favorites"""
    body = await request.json()
    strain_name = body.get("strain_name", "")
    use_zscore = body.get("use_zscore", False)  # New: z-score comparison option
    if not current_user_id:
        raise HTTPException(status_code=401, detail="No user ID set")

    try:
        # Get strain data
        strain_data = strain_db.get_strain(strain_name)
        if not strain_data:
            # Try to generate strain data using LLM
            print(f"🤖 Generating strain data for: {strain_name}")
            try:
                strain_data = await generate_strain_data(strain_name)
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

        # Get user profile
        user_profile = get_user_profile(current_user_id)
        ideal_profile = user_profile.get("ideal_profile")
        ranked_favorites = user_profile.get("ranked_favorites", [])

        # Choose comparison method
        if use_zscore and ranked_favorites:
            # Use ranked favorites with z-scoring
            comparison = compare_against_ranked_favorites(strain_data, ranked_favorites, use_zscore=True)
            comparison_type = "ranked_favorites_zscore"
        elif ranked_favorites:
            # Use ranked favorites without z-scoring
            comparison = compare_against_ranked_favorites(strain_data, ranked_favorites, use_zscore=False)
            comparison_type = "ranked_favorites_individual"
        elif ideal_profile:
            # Fall back to ideal profile comparison
            comparison = compare_against_ideal_profile(strain_data, ideal_profile)
            comparison_type = "ideal_profile"
        else:
            raise HTTPException(status_code=404, detail="No ideal profile or ranked favorites found. Please create one first.")

        # Generate LLM-based analysis
        if comparison_type.startswith("ranked_favorites"):
            llm_analysis = await generate_llm_analysis_ranked(strain_data, ranked_favorites, comparison)
        else:
            llm_analysis = await generate_llm_analysis(strain_data, ideal_profile, comparison)

        # Build terpene analysis
        terpene_analysis = build_terpene_analysis(strain_data.get("terpenes", {}))

        return {
            "strain_analysis": {
                "name": strain_data.get("name", strain_name),
                "type": strain_data.get("type", "unknown"),
                "thc_range": strain_data.get("thc_range", "unknown"),
                "cbd_range": strain_data.get("cbd_range", "unknown"),
                "description": strain_data.get("description", "No description available"),
                "effects": strain_data.get("effects", []),
                "flavors": strain_data.get("flavors", []),
                "terpenes": terpene_analysis
            },
            "ideal_profile": ideal_profile,
            "ranked_favorites": ranked_favorites if comparison_type.startswith("ranked_favorites") else None,
            "comparison": comparison,
            "comparison_type": comparison_type,
            "llm_analysis": llm_analysis
        }

    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/api/save-ranked-favorites")
async def save_ranked_favorites():
    """Save ranked favorites by creating individual profiles for each favorite strain"""
    if not current_user_id:
        raise HTTPException(status_code=401, detail="No user ID set")

    try:
        # Get user profile
        user_profile = get_user_profile(current_user_id)
        favorite_strains = user_profile.get("favorite_strains", [])

        if len(favorite_strains) < 2:
            raise HTTPException(status_code=400, detail="Need at least 2 favorite strains to create ranked favorites")

        ranked_favorites = []

        # Create individual profiles for each favorite strain
        for strain_name in favorite_strains:
            # Get or generate strain data
            strain_data = await generate_strain_data(strain_name)
            if strain_data:
                # Normalize to fixed schema
                normalized_strain = normalize_to_fixed_schema(strain_data)
                ranked_favorites.append({
                    "name": strain_name,
                    "terpenes": normalized_strain.get("terpenes", {}),
                    "cannabinoids": normalized_strain.get("cannabinoids", {}),
                    "effects": strain_data.get("effects", []),
                    "flavors": strain_data.get("flavors", []),
                    "type": strain_data.get("type", "unknown"),
                    "thc_range": strain_data.get("thc_range", "unknown"),
                    "cbd_range": strain_data.get("cbd_range", "unknown")
                })

        # Update user profile with ranked favorites
        current_profile = get_user_profile(current_user_id)
        current_profile["ranked_favorites"] = ranked_favorites
        save_user_profile(current_user_id, current_profile)

        return {
            "message": f"Successfully saved {len(ranked_favorites)} ranked favorites",
            "ranked_favorites": ranked_favorites
        }

    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

def build_terpene_analysis(terpenes: dict[str, float]) -> list[dict[str, Any]]:
    """Build detailed terpene analysis from terpene percentages"""
    terpene_info = {
        "myrcene": {
            "name": "Myrcene",
            "description": "The most common terpene in cannabis, known for its sedative and relaxing effects. Found in hops, mango, and lemongrass.",
            "effects": ["relaxation", "sedation", "muscle relaxation", "pain relief"],
            "aroma": "Earthy, musky, clove-like"
        },
        "caryophyllene": {
            "name": "Caryophyllene",
            "description": "A unique terpene that also acts as a cannabinoid, binding to CB2 receptors. Known for anti-inflammatory properties.",
            "effects": ["anti-inflammatory", "pain relief", "stress relief", "anxiety reduction"],
            "aroma": "Spicy, peppery, woody"
        },
        "limonene": {
            "name": "Limonene",
            "description": "A citrusy terpene that promotes mood elevation and stress relief. Also found in citrus fruits and peppermint.",
            "effects": ["mood elevation", "stress relief", "anxiety reduction", "antidepressant"],
            "aroma": "Citrus, lemon, orange"
        },
        "pinene": {
            "name": "Pinene",
            "description": "A pine-scented terpene that promotes alertness and memory retention. Found in pine needles and rosemary.",
            "effects": ["alertness", "memory retention", "bronchodilator", "anti-inflammatory"],
            "aroma": "Pine, fresh, woody"
        },
        "linalool": {
            "name": "Linalool",
            "description": "A floral terpene with calming and sedative properties. Found in lavender and jasmine.",
            "effects": ["calming", "sedation", "anxiety relief", "antidepressant"],
            "aroma": "Floral, lavender, sweet"
        },
        "humulene": {
            "name": "Humulene",
            "description": "An earthy terpene with appetite-suppressing and anti-inflammatory properties. Found in hops and sage.",
            "effects": ["appetite suppression", "anti-inflammatory", "antibacterial", "pain relief"],
            "aroma": "Earthy, woody, hoppy"
        },
        "terpinolene": {
            "name": "Terpinolene",
            "description": "A complex terpene with uplifting and energizing effects. Found in nutmeg, tea tree, and apples.",
            "effects": ["uplifting", "energizing", "antioxidant", "sedative in high doses"],
            "aroma": "Floral, herbal, citrus"
        },
        "ocimene": {
            "name": "Ocimene",
            "description": "A sweet, herbal terpene with uplifting and energizing properties. Found in mint, parsley, and orchids.",
            "effects": ["uplifting", "energizing", "antiviral", "decongestant"],
            "aroma": "Sweet, herbal, woody"
        },
        "terpineol": {
            "name": "Terpineol",
            "description": "A floral terpene with relaxing and sedative effects. Found in lilacs and pine trees.",
            "effects": ["relaxation", "sedation", "antioxidant", "antimicrobial"],
            "aroma": "Floral, lilac, pine"
        }
    }

    # Sort terpenes by percentage (highest first) and build analysis
    sorted_terpenes = sorted(terpenes.items(), key=lambda x: x[1], reverse=True)
    terpene_analysis = []

    for terpene_key, percentage in sorted_terpenes[:5]:  # Top 5 terpenes
        if terpene_key in terpene_info:
            info = terpene_info[terpene_key]
            terpene_analysis.append({
                "name": info["name"],
                "level": f"{percentage * 100:.1f}",
                "description": info["description"],
                "effects": info["effects"],
                "aroma": info["aroma"]
            })

    return terpene_analysis

def calculate_aggregate_terpene_profile(strain_data_list: list[dict[str, Any]]) -> dict[str, Any]:
    """Calculate comprehensive aggregate profile from multiple strains including terpenes, cannabinoids, and effects"""
    all_terpenes = {}
    all_cannabinoids = {}
    all_effects = []
    all_medical_effects = []
    all_flavors = []
    all_aromas = []
    total_strains = len(strain_data_list)

    # Collect all data from all strains
    for strain_data in strain_data_list:
        # Collect terpenes
        terpenes = strain_data.get("terpenes", {})
        for terpene, value in terpenes.items():
            if terpene not in all_terpenes:
                all_terpenes[terpene] = []
            all_terpenes[terpene].append(value)

        # Collect cannabinoids
        cannabinoids = strain_data.get("cannabinoids", {})
        for cannabinoid, value in cannabinoids.items():
            if cannabinoid not in all_cannabinoids:
                all_cannabinoids[cannabinoid] = []
            all_cannabinoids[cannabinoid].append(value)

        # Collect effects and other attributes
        all_effects.extend(strain_data.get("effects", []))
        all_medical_effects.extend(strain_data.get("medical_effects", []))
        all_flavors.extend(strain_data.get("flavors", []))
        all_aromas.extend(strain_data.get("aromas", []))

    # Calculate maximum values for terpenes and cannabinoids (ideal profile should represent peak preferences)
    aggregate_terpenes = {}
    for terpene, values in all_terpenes.items():
        aggregate_terpenes[terpene] = max(values)  # Use maximum instead of average

    aggregate_cannabinoids = {}
    for cannabinoid, values in all_cannabinoids.items():
        aggregate_cannabinoids[cannabinoid] = max(values)  # Use maximum instead of average

    # Sort by average value (highest first)
    sorted_terpenes = sorted(aggregate_terpenes.items(), key=lambda x: x[1], reverse=True)
    sorted_cannabinoids = sorted(aggregate_cannabinoids.items(), key=lambda x: x[1], reverse=True)

    # Build detailed analysis
    terpene_analysis = build_terpene_analysis(aggregate_terpenes)

    # Count effect frequency
    effect_counts = {}
    for effect in all_effects:
        effect_counts[effect] = effect_counts.get(effect, 0) + 1

    medical_effect_counts = {}
    for effect in all_medical_effects:
        medical_effect_counts[effect] = medical_effect_counts.get(effect, 0) + 1

    flavor_counts = {}
    for flavor in all_flavors:
        flavor_counts[flavor] = flavor_counts.get(flavor, 0) + 1

    aroma_counts = {}
    for aroma in all_aromas:
        aroma_counts[aroma] = aroma_counts.get(aroma, 0) + 1

    # Sort by frequency
    sorted_effects = sorted(effect_counts.items(), key=lambda x: x[1], reverse=True)
    sorted_medical_effects = sorted(medical_effect_counts.items(), key=lambda x: x[1], reverse=True)
    sorted_flavors = sorted(flavor_counts.items(), key=lambda x: x[1], reverse=True)
    sorted_aromas = sorted(aroma_counts.items(), key=lambda x: x[1], reverse=True)

    # Get top items
    top_effects = [effect for effect, count in sorted_effects[:10]]
    top_medical_effects = [effect for effect, count in sorted_medical_effects[:8]]
    top_flavors = [flavor for flavor, count in sorted_flavors[:6]]
    top_aromas = [aroma for aroma, count in sorted_aromas[:6]]

    return {
        "aggregate_terpenes": aggregate_terpenes,
        "aggregate_cannabinoids": aggregate_cannabinoids,
        "terpene_analysis": terpene_analysis,
        "top_effects": top_effects,
        "top_medical_effects": top_medical_effects,
        "top_flavors": top_flavors,
        "top_aromas": top_aromas,
        "strain_count": total_strains,
        "dominant_terpenes": [terpene for terpene, value in sorted_terpenes[:5]],
        "dominant_cannabinoids": [cannabinoid for cannabinoid, value in sorted_cannabinoids[:3]],
        "terpene_diversity": len(aggregate_terpenes),
        "cannabinoid_diversity": len(aggregate_cannabinoids)
    }

def compare_against_ranked_favorites(strain_data: dict[str, Any], ranked_favorites: list[dict[str, Any]], use_zscore: bool = False) -> dict[str, Any]:
    """Compare a strain against ranked favorites using z-scoring when enabled"""
    if not ranked_favorites:
        return {"error": "No ranked favorites available for comparison"}

    # Normalize strain data to fixed schema
    normalized_strain = normalize_to_fixed_schema(strain_data)

    # Get normalized strain vector
    strain_terpenes = normalized_strain.get("terpenes", {})
    strain_cannabinoids = normalized_strain.get("cannabinoids", {})

    # Create strain vector in fixed order
    terpene_order = list(TERPENE_SCHEMA.keys())
    cannabinoid_order = list(CANNABINOID_SCHEMA.keys())
    strain_terpene_vector = [strain_terpenes.get(terpene, 0.0) for terpene in terpene_order]
    strain_cannabinoid_vector = [strain_cannabinoids.get(cannabinoid, 0.0) for cannabinoid in cannabinoid_order]
    strain_vector = strain_terpene_vector + strain_cannabinoid_vector

    # Get top 2-3 ranked favorites for comparison
    top_favorites = ranked_favorites[:3] if len(ranked_favorites) >= 3 else ranked_favorites

    if use_zscore and len(top_favorites) >= 2:
        # Use z-scoring approach with multiple profiles
        return compare_with_zscoring(strain_vector, strain_data, top_favorites)
    else:
        # Use individual comparisons against each favorite
        return compare_against_individual_favorites(strain_vector, strain_data, top_favorites)

def compare_with_zscoring(strain_vector: list[float], strain_data: dict[str, Any], ranked_favorites: list[dict[str, Any]]) -> dict[str, Any]:
    """Compare using z-scoring with multiple ranked profiles"""
    import numpy as np
    from sklearn.metrics.pairwise import cosine_similarity
    from sklearn.preprocessing import StandardScaler

    # Prepare all vectors for z-scoring
    all_vectors = [strain_vector]
    favorite_names = [strain_data.get("name", "Unknown")]

    for favorite in ranked_favorites:
        # Normalize each favorite to fixed schema
        normalized_favorite = normalize_to_fixed_schema(favorite)
        favorite_terpenes = normalized_favorite.get("terpenes", {})
        favorite_cannabinoids = normalized_favorite.get("cannabinoids", {})

        # Create favorite vector in fixed order
        terpene_order = list(TERPENE_SCHEMA.keys())
        cannabinoid_order = list(CANNABINOID_SCHEMA.keys())
        favorite_terpene_vector = [favorite_terpenes.get(terpene, 0.0) for terpene in terpene_order]
        favorite_cannabinoid_vector = [favorite_cannabinoids.get(cannabinoid, 0.0) for cannabinoid in cannabinoid_order]
        favorite_vector = favorite_terpene_vector + favorite_cannabinoid_vector

        all_vectors.append(favorite_vector)
        favorite_names.append(favorite.get("name", "Unknown"))

    # Convert to numpy arrays
    all_vectors_array = np.array(all_vectors)

    # Z-score standardization
    scaler = StandardScaler()
    scaled_vectors = scaler.fit_transform(all_vectors_array)

    # Calculate similarities between strain and each favorite
    strain_scaled = scaled_vectors[0].reshape(1, -1)
    similarities = []

    for i in range(1, len(scaled_vectors)):
        favorite_scaled = scaled_vectors[i].reshape(1, -1)
        similarity = cosine_similarity(strain_scaled, favorite_scaled)[0][0]
        similarities.append({
            "strain_name": favorite_names[i],
            "similarity": float(similarity),
            "similarity_percentage": float(similarity * 100),
            "rank": i
        })

    # Sort by similarity (highest first)
    similarities.sort(key=lambda x: x["similarity"], reverse=True)

    # Calculate overall similarity (average of top similarities)
    overall_similarity = sum(s["similarity"] for s in similarities) / len(similarities)

    return {
        "overall_similarity": float(overall_similarity),
        "similarity_percentage": float(overall_similarity * 100),
        "match_rating": get_match_rating(overall_similarity),
        "individual_similarities": similarities,
        "method": "z_scored_multi_profile",
        "profiles_used": len(ranked_favorites)
    }

def compare_against_individual_favorites(strain_vector: list[float], strain_data: dict[str, Any], ranked_favorites: list[dict[str, Any]]) -> dict[str, Any]:
    """Compare against individual favorites using original cosine similarity"""
    import numpy as np
    from sklearn.metrics.pairwise import cosine_similarity

    strain_array = np.array(strain_vector).reshape(1, -1)
    similarities = []

    for i, favorite in enumerate(ranked_favorites):
        # Normalize each favorite to fixed schema
        normalized_favorite = normalize_to_fixed_schema(favorite)
        favorite_terpenes = normalized_favorite.get("terpenes", {})
        favorite_cannabinoids = normalized_favorite.get("cannabinoids", {})

        # Create favorite vector in fixed order
        terpene_order = list(TERPENE_SCHEMA.keys())
        cannabinoid_order = list(CANNABINOID_SCHEMA.keys())
        favorite_terpene_vector = [favorite_terpenes.get(terpene, 0.0) for terpene in terpene_order]
        favorite_cannabinoid_vector = [favorite_cannabinoids.get(cannabinoid, 0.0) for cannabinoid in cannabinoid_order]
        favorite_vector = favorite_terpene_vector + favorite_cannabinoid_vector
        favorite_array = np.array(favorite_vector).reshape(1, -1)

        # Calculate cosine similarity
        similarity = cosine_similarity(strain_array, favorite_array)[0][0]
        similarities.append({
            "strain_name": favorite.get("name", "Unknown"),
            "similarity": float(similarity),
            "similarity_percentage": float(similarity * 100),
            "rank": i + 1
        })

    # Sort by similarity (highest first)
    similarities.sort(key=lambda x: x["similarity"], reverse=True)

    # Calculate overall similarity (average of top similarities)
    overall_similarity = sum(s["similarity"] for s in similarities) / len(similarities)

    return {
        "overall_similarity": float(overall_similarity),
        "similarity_percentage": float(overall_similarity * 100),
        "match_rating": get_match_rating(overall_similarity),
        "individual_similarities": similarities,
        "method": "individual_cosine",
        "profiles_used": len(ranked_favorites)
    }

def compare_against_ideal_profile(strain_data: dict[str, Any], ideal_profile: dict[str, Any]) -> dict[str, Any]:
    """Compare a strain against the ideal profile using fixed schema and z-scored similarity"""
    # Normalize strain data to fixed schema
    normalized_strain = normalize_to_fixed_schema(strain_data)

    # Get normalized data
    strain_terpenes = normalized_strain.get("terpenes", {})
    strain_cannabinoids = normalized_strain.get("cannabinoids", {})
    ideal_terpenes = ideal_profile.get("aggregate_terpenes", {})
    ideal_cannabinoids = ideal_profile.get("aggregate_cannabinoids", {})

    # Use fixed schema order for consistent comparison
    # Terpenes in fixed order
    terpene_order = list(TERPENE_SCHEMA.keys())
    strain_terpene_vector = [strain_terpenes.get(terpene, 0.0) for terpene in terpene_order]
    ideal_terpene_vector = [ideal_terpenes.get(terpene, 0.0) for terpene in terpene_order]

    # Cannabinoids in fixed order
    cannabinoid_order = list(CANNABINOID_SCHEMA.keys())
    strain_cannabinoid_vector = [strain_cannabinoids.get(cannabinoid, 0.0) for cannabinoid in cannabinoid_order]
    ideal_cannabinoid_vector = [ideal_cannabinoids.get(cannabinoid, 0.0) for cannabinoid in cannabinoid_order]

    # Combine terpenes and cannabinoids for overall comparison
    strain_vector = strain_terpene_vector + strain_cannabinoid_vector
    ideal_vector = ideal_terpene_vector + ideal_cannabinoid_vector

    # Calculate multiple similarity metrics
    import numpy as np
    from sklearn.metrics.pairwise import cosine_similarity
    from sklearn.preprocessing import StandardScaler

    strain_array = np.array(strain_vector).reshape(1, -1)
    ideal_array = np.array(ideal_vector).reshape(1, -1)

    # 1. Z-scored Cosine Similarity (RECOMMENDED APPROACH)
    # Standardize both vectors to focus on chemovar shape rather than absolute magnitudes
    scaler = StandardScaler()

    # Combine both vectors for consistent scaling
    combined_vectors = np.vstack([strain_array, ideal_array])
    scaled_vectors = scaler.fit_transform(combined_vectors)

    strain_scaled = scaled_vectors[0].reshape(1, -1)
    ideal_scaled = scaled_vectors[1].reshape(1, -1)

    # Calculate cosine similarity on z-scored vectors
    z_scored_cosine_sim = cosine_similarity(strain_scaled, ideal_scaled)[0][0]

    # 2. Original cosine similarity (for comparison)
    original_cosine_sim = cosine_similarity(strain_array, ideal_array)[0][0]

    # 3. Euclidean distance on z-scored vectors
    euclidean_dist_scaled = np.linalg.norm(strain_scaled - ideal_scaled)
    euclidean_sim_scaled = 1 / (1 + euclidean_dist_scaled)

    # 4. Euclidean distance on original vectors
    euclidean_dist_original = np.linalg.norm(strain_array - ideal_array)
    euclidean_sim_original = 1 / (1 + euclidean_dist_original)

    # 5. Profile shape similarity (correlation on z-scored data)
    if len(strain_vector) > 1 and np.std(strain_vector) > 0 and np.std(ideal_vector) > 0:
        correlation_scaled = np.corrcoef(strain_scaled.flatten(), ideal_scaled.flatten())[0, 1]
        correlation_scaled = max(0, correlation_scaled)  # Only positive correlations

        correlation_original = np.corrcoef(strain_vector, ideal_vector)[0, 1]
        correlation_original = max(0, correlation_original)
    else:
        correlation_scaled = 0
        correlation_original = 0

    # 6. Combined similarity score (weighted average with emphasis on z-scored metrics)
    combined_similarity = (
        0.5 * z_scored_cosine_sim +    # Z-scored cosine similarity (primary metric)
        0.2 * euclidean_sim_scaled +   # Z-scored euclidean similarity
        0.2 * correlation_scaled +     # Z-scored correlation
        0.1 * original_cosine_sim      # Original cosine for reference
    )

    # Use the z-scored enhanced similarity as primary metric
    # But fallback to original cosine if z-scored is negative (indicates z-scoring issues)
    if z_scored_cosine_sim < 0:
        similarity = original_cosine_sim
    else:
        similarity = combined_similarity

    # Calculate individual terpene and cannabinoid differences
    component_differences = {}

    # Terpene differences
    for terpene in terpene_order:
        strain_value = strain_terpenes.get(terpene, 0.0)
        ideal_value = ideal_terpenes.get(terpene, 0.0)
        difference = strain_value - ideal_value
        component_differences[terpene] = {
            "strain_value": strain_value,
            "ideal_value": ideal_value,
            "difference": difference,
            "percentage_diff": (difference / ideal_value * 100) if ideal_value > 0 else 0,
            "type": "terpene"
        }

    # Cannabinoid differences
    for cannabinoid in cannabinoid_order:
        strain_value = strain_cannabinoids.get(cannabinoid, 0.0)
        ideal_value = ideal_cannabinoids.get(cannabinoid, 0.0)
        difference = strain_value - ideal_value
        component_differences[cannabinoid] = {
            "strain_value": strain_value,
            "ideal_value": ideal_value,
            "difference": difference,
            "percentage_diff": (difference / ideal_value * 100) if ideal_value > 0 else 0,
            "type": "cannabinoid"
        }

    # Sort by absolute difference
    sorted_differences = sorted(
        component_differences.items(),
        key=lambda x: abs(x[1]["difference"]),
        reverse=True
    )

    return {
        "overall_similarity": float(similarity),
        "similarity_percentage": float(similarity * 100),
        "component_differences": dict(sorted_differences),
        "match_rating": get_match_rating(similarity),
        "similarity_breakdown": {
            "z_scored_cosine_similarity": float(z_scored_cosine_sim),
            "original_cosine_similarity": float(original_cosine_sim),
            "z_scored_euclidean_similarity": float(euclidean_sim_scaled),
            "original_euclidean_similarity": float(euclidean_sim_original),
            "z_scored_correlation_similarity": float(correlation_scaled),
            "original_correlation_similarity": float(correlation_original),
            "combined_similarity": float(combined_similarity)
        }
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

async def generate_llm_analysis_ranked(strain_data: dict[str, Any], ranked_favorites: list[dict[str, Any]], comparison: dict[str, Any]) -> str:
    """Generate LLM analysis for ranked favorites comparison"""
    try:
        from langchain_ollama import OllamaLLM

        llm = OllamaLLM(model="llama3.2")

        # Prepare ranked favorites summary
        favorites_summary = []
        for i, favorite in enumerate(ranked_favorites[:3]):  # Top 3
            favorites_summary.append(f"{i+1}. {favorite.get('name', 'Unknown')} - {favorite.get('type', 'Unknown')} with {favorite.get('thc_range', 'Unknown')} THC")

        # Prepare individual similarities
        individual_sims = comparison.get("individual_similarities", [])
        similarity_details = []
        for sim in individual_sims:
            similarity_details.append(f"- {sim['strain_name']}: {sim['similarity_percentage']:.1f}% similarity")

        prompt = f"""
        Analyze this cannabis strain comparison against the user's ranked favorite strains:

        **Strain Being Analyzed:**
        - Name: {strain_data.get('name', 'Unknown')}
        - Type: {strain_data.get('type', 'Unknown')}
        - THC Range: {strain_data.get('thc_range', 'Unknown')}
        - CBD Range: {strain_data.get('cbd_range', 'Unknown')}
        - Effects: {', '.join(strain_data.get('effects', []))}
        - Flavors: {', '.join(strain_data.get('flavors', []))}

        **User's Ranked Favorites:**
        {chr(10).join(favorites_summary)}

        **Similarity Results:**
        - Overall Similarity: {comparison.get('similarity_percentage', 0):.1f}%
        - Match Rating: {comparison.get('match_rating', 'Unknown')}
        - Method: {comparison.get('method', 'Unknown')}
        - Individual Similarities:
        {chr(10).join(similarity_details)}

        **Analysis Request:**
        Provide a comprehensive analysis including:
        1. **Overall Assessment**: How well does this strain match the user's preferences?
        2. **Individual Comparisons**: How does it compare to each favorite strain?
        3. **Effects Analysis**: What effects can the user expect compared to their favorites?
        4. **Recommendation**: Should the user try this strain? Why or why not?
        5. **Potential Benefits**: What new experiences might this strain offer?
        6. **Potential Drawbacks**: What might be different or less appealing?

        Format your response with clear sections and be specific about the similarities and differences.
        """

        response = await llm.ainvoke(prompt)
        return response.content if hasattr(response, 'content') else str(response)

    except Exception as e:
        print(f"❌ Error generating LLM analysis: {e}")
        return f"**Analysis of {strain_data.get('name', 'Unknown')} vs Your Ranked Favorites**\n\nOverall Similarity: {comparison.get('similarity_percentage', 0):.1f}%\nMatch Rating: {comparison.get('match_rating', 'Unknown')}\n\nThis strain shows {comparison.get('similarity_percentage', 0):.1f}% similarity to your ranked favorites. The comparison method used was: {comparison.get('method', 'Unknown')}."

async def generate_llm_analysis(strain_data: dict[str, Any], ideal_profile: dict[str, Any], comparison: dict[str, Any]) -> str:
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
async def generate_strain_data(strain_name: str) -> dict[str, Any] | None:
    """Generate strain data using LLM with fallback"""
    try:
        from langchain_ollama import ChatOllama

        llm = ChatOllama(
            model="llama3.2:latest",
            base_url="http://localhost:11434",
            temperature=0.7
        )

        prompt = f"""
Generate detailed cannabis strain information for "{strain_name}" in JSON format.
Use the EXACT fixed schema below for terpenes and cannabinoids.

Required format:
{{
    "terpenes": {{
        "myrcene": 0.0, "limonene": 0.0, "caryophyllene": 0.0, "pinene": 0.0,
        "linalool": 0.0, "humulene": 0.0, "terpinolene": 0.0, "ocimene": 0.0,
        "nerolidol": 0.0, "bisabolol": 0.0, "eucalyptol": 0.0
    }},
    "cannabinoids": {{
        "thc": 0.0, "cbd": 0.0, "cbg": 0.0, "cbn": 0.0, "thcv": 0.0
    }},
    "effects": ["relaxed", "happy", "uplifted"],
    "type": "hybrid",
    "thc_range": "18-24%",
    "cbd_range": "0.1-0.5%",
    "description": "A balanced hybrid with...",
    "flavors": ["sweet", "citrus", "earthy"]
}}

Realistic ranges (as decimals):
Cannabinoids:
- THC: 0.15-0.30 (15-30%)
- CBD: 0.005-0.15 (0.5-15%)
- CBG: 0.005-0.02 (0.5-2%)
- CBN: 0.001-0.01 (0.1-1%)
- THCV: 0.0005-0.005 (0.05-0.5%)

Terpenes (major ones higher, minor ones lower):
- Major: myrcene (0.1-0.8), limonene (0.05-0.6), caryophyllene (0.05-0.5), pinene (0.05-0.4), linalool (0.02-0.3)
- Minor: humulene (0.001-0.2), terpinolene (0.001-0.15), ocimene (0.001-0.1), nerolidol (0.0001-0.05), bisabolol (0.0001-0.05), eucalyptol (0.0001-0.03)
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

        # Check if content is empty
        if not content:
            print(f"Empty response from LLM for {strain_name}, using fallback data")
            return generate_fallback_strain_data(strain_name)

        # Parse JSON
        import json
        strain_data = json.loads(content)

        # Validate required fields
        required_fields = ["terpenes", "cannabinoids", "effects", "type", "thc_range", "description"]
        for field in required_fields:
            if field not in strain_data:
                print(f"Missing required field: {field}, using fallback data")
                return generate_fallback_strain_data(strain_name)

        # Add the strain name to the data
        strain_data["name"] = strain_name

        return strain_data

    except Exception as e:
        print(f"Error generating strain data for {strain_name}: {e}")
        print(f"Using fallback data for {strain_name}")
        return generate_fallback_strain_data(strain_name)

def generate_fallback_strain_data(strain_name: str) -> dict[str, Any]:
    """Generate fallback strain data when LLM fails using fixed schema"""
    import random

    # Generate realistic cannabinoid profile using fixed schema
    cannabinoids = CANNABINOID_SCHEMA.copy()

    # Generate THC (15-30%)
    cannabinoids["thc"] = random.uniform(0.15, 0.30)

    # Generate CBD (0.5-15%)
    cannabinoids["cbd"] = random.uniform(0.005, 0.15)

    # Generate CBG (0.5-2%)
    cannabinoids["cbg"] = random.uniform(0.005, 0.02)

    # Generate CBN (0.1-1%)
    cannabinoids["cbn"] = random.uniform(0.001, 0.01)

    # Generate THCV (0.05-0.5%)
    cannabinoids["thcv"] = random.uniform(0.0005, 0.005)

    # Generate realistic terpene profile using fixed schema
    terpenes = TERPENE_SCHEMA.copy()

    # Major terpenes (higher values)
    terpenes["myrcene"] = random.uniform(0.1, 0.8)
    terpenes["limonene"] = random.uniform(0.05, 0.6)
    terpenes["caryophyllene"] = random.uniform(0.05, 0.5)
    terpenes["pinene"] = random.uniform(0.05, 0.4)
    terpenes["linalool"] = random.uniform(0.02, 0.3)

    # Minor terpenes (lower values)
    terpenes["humulene"] = random.uniform(0.001, 0.2)
    terpenes["terpinolene"] = random.uniform(0.001, 0.15)
    terpenes["ocimene"] = random.uniform(0.001, 0.1)
    terpenes["nerolidol"] = random.uniform(0.0001, 0.05)
    terpenes["bisabolol"] = random.uniform(0.0001, 0.05)
    terpenes["eucalyptol"] = random.uniform(0.0001, 0.03)

    # Common effects
    effect_sets = [
        ["relaxed", "happy", "uplifted"],
        ["euphoric", "creative", "energetic"],
        ["calm", "sleepy", "pain relief"],
        ["focused", "alert", "uplifted"],
        ["happy", "relaxed", "creative"]
    ]

    # Common types
    types = ["indica", "sativa", "hybrid"]

    # Common flavors
    flavor_sets = [
        ["sweet", "citrus", "earthy"],
        ["pine", "woody", "spicy"],
        ["fruity", "sweet", "tropical"],
        ["earthy", "herbal", "spicy"],
        ["citrus", "lemon", "sweet"]
    ]

    # Select random profile
    effects = random.choice(effect_sets)
    strain_type = random.choice(types)
    flavors = random.choice(flavor_sets)

    # Generate THC/CBD ranges based on cannabinoid profile
    thc_val = cannabinoids["thc"]
    cbd_val = cannabinoids["cbd"]
    thc_range = f"{thc_val*100-2:.1f}-{thc_val*100+2:.1f}%"
    cbd_range = f"{cbd_val*100-0.5:.1f}-{cbd_val*100+0.5:.1f}%"

    return {
        "name": strain_name,
        "terpenes": terpenes,
        "cannabinoids": cannabinoids,
        "effects": effects,
        "type": strain_type,
        "thc_range": thc_range,
        "cbd_range": cbd_range,
        "description": f"A {strain_type} strain with {', '.join(flavors[:2])} flavors and {', '.join(effects[:2])} effects.",
        "flavors": flavors
    }

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="127.0.0.1", port=8000)
