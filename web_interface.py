"""
Simple Cannabis Strain Recommendation Web Interface
Provides a simple webpage for strain analysis and dispensary recommendations
"""

from fastapi import FastAPI, HTTPException, Query
from fastapi.templating import Jinja2Templates
from fastapi.requests import Request
from fastapi.responses import HTMLResponse, JSONResponse
from pydantic import BaseModel, Field
from typing import Dict, List, Optional, Any
import uvicorn
from datetime import datetime
import os
from pathlib import Path

# Import core modules
from enhanced_strain_database import EnhancedStrainDatabase
from google_maps_integration import LocationService
from langgraph_agent import get_strain_recommendations
from langchain_ollama import ChatOllama
import os
from dotenv import load_dotenv

load_dotenv()

# Initialize FastAPI app
app = FastAPI(
    title="Cannabis Strain Recommendation API",
    description="Simple cannabis strain recommendation system",
    version="1.0.0"
)

# Initialize LLM for strain data generation
llm = ChatOllama(
    model=os.getenv("OLLAMA_MODEL", "gemma3:latest"),
    base_url=os.getenv("OLLAMA_BASE_URL", "http://localhost:11434")
)

async def generate_strain_data(strain_name: str) -> Optional[Dict[str, Any]]:
    """Generate strain data using LLM for unknown strains"""
    try:
        prompt = f"""
        Generate detailed information for the cannabis strain "{strain_name}".
        Return a JSON object with exactly this structure:

        {{
            "terpenes": {{
                "myrcene": 0.X,
                "caryophyllene": 0.X,
                "pinene": 0.X,
                "limonene": 0.X,
                "linalool": 0.X,
                "humulene": 0.X,
                "terpinolene": 0.X
            }},
            "effects": ["effect1", "effect2", "effect3", "effect4", "effect5"],
            "medical_effects": ["medical1", "medical2", "medical3"],
            "type": "indica|sativa|hybrid",
            "thc_range": "XX-XX%",
            "cbd_range": "0.X-0.X%",
            "description": "Brief description of the strain",
            "flavors": ["flavor1", "flavor2", "flavor3"],
            "aromas": ["aroma1", "aroma2", "aroma3"],
            "best_time": "morning|afternoon|evening|night",
            "activity": "relaxation|creative|social|medical|work"
        }}

        Use realistic values based on typical cannabis strains. Terpene values should sum to approximately 1.0.
        """

        from langchain_core.messages import HumanMessage
        response = llm.invoke([HumanMessage(content=prompt)])

        # Try to extract JSON from response
        content = response.content.strip()

        # Remove markdown code blocks if present
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

# Initialize core components
strain_db = EnhancedStrainDatabase()
location_service = LocationService()

# Templates
templates = Jinja2Templates(directory="templates")

# Pydantic models
class StrainRecommendationRequest(BaseModel):
    strain_name: str = Field(..., description="Name of the cannabis strain to analyze")
    location: Optional[str] = Field(None, description="User location (city, state)")
    radius: Optional[int] = Field(25, description="Search radius in miles")

# API Routes

@app.get("/", response_class=HTMLResponse)
async def root(request: Request):
    """Serve the main web interface"""
    return templates.TemplateResponse("index.html", {"request": request})

@app.get("/api/recommendations", response_model=Dict[str, Any])
def get_recommendations(
    strain_name: str = Query(..., description="Name of the cannabis strain to analyze"),
    location: Optional[str] = Query(None, description="User location (city, state)"),
    radius: Optional[int] = Query(25, description="Search radius in miles")
):
    """Get strain recommendations using LangGraph agent"""

    try:
        print(f"🔍 Processing request for strain: {strain_name}, location: {location}")
        # First try direct database lookup (case-insensitive)
        strain_name_lower = strain_name.lower().strip()
        strain_data = None

        # Try exact match first
        print(f"Looking for exact match: {strain_name_lower}")
        for name in strain_db.strains.keys():
            if name.lower() == strain_name_lower:
                strain_data = strain_db.get_strain(name)
                print(f"✅ Found exact match: {name}")
                break

        # If no exact match, try partial match - but only if it's a very close match
        if not strain_data:
            print(f"No exact match, trying partial match...")
            for name in strain_db.strains.keys():
                # Only match if the search term is very similar or a substring
                if (strain_name_lower == name.lower() or
                    (len(strain_name_lower.split()) == 1 and strain_name_lower in name.lower()) or
                    (len(strain_name_lower.split()) > 1 and all(word in name.lower() for word in strain_name_lower.split()))):
                    strain_data = strain_db.get_strain(name)
                    print(f"✅ Found partial match: {name}")
                    break

        # If still no match, try fuzzy matching for common terms
        if not strain_data:
            suggestions = []
            search_words = strain_name_lower.split()

            for word in search_words:
                for name in strain_db.strains.keys():
                    if word in name.lower():
                        if name not in suggestions:
                            suggestions.append(name)

            if suggestions:
                strain_data = strain_db.get_strain(suggestions[0])  # Use first suggestion
                # Note: We found a match via suggestion, but we'll proceed with it

        if not strain_data:
            # Try to generate strain data using LLM
            print(f"🤖 NO STRAIN DATA FOUND - Generating strain data for: {strain_name}")
            try:
                # Create event loop for async call
                import asyncio
                loop = asyncio.new_event_loop()
                asyncio.set_event_loop(loop)
                strain_data = loop.run_until_complete(generate_strain_data(strain_name))
                loop.close()
                if strain_data:
                    # Add to database for future use
                    strain_db.add_custom_strain(strain_name, strain_data)
                    print(f"✅ Added new strain to database: {strain_name}")
                    print(f"   Database now has {len(strain_db.strains)} strains")
                    print(f"   Type: {strain_data.get('type')}, THC: {strain_data.get('thc_range')}")
                else:
                    print("❌ Strain data generation returned None")
                    raise HTTPException(
                        status_code=404,
                        detail=f"Could not generate data for '{strain_name}'. Try: blueberry, granddaddy purple, blue dream, og kush, sour diesel, jack herer"
                    )
            except Exception as e:
                print(f"❌ Error generating strain data: {e}")
                raise HTTPException(
                    status_code=500,
                    detail=f"Error generating strain data: {str(e)}"
                )

        # Get similar strains
        strain_name_for_similarity = strain_data.get("name", strain_name)
        print(f"🔍 Getting similar strains for: {strain_name_for_similarity}")

        # Get similar strains - use the strain data from the database if it exists
        similar_strains = strain_db.get_similar_strains(strain_data.get("name", strain_name), limit=3)

        # If no similar strains found and this was a generated strain, provide fallback recommendations
        if not similar_strains and strain_name_for_similarity != strain_name:
            search_base = strain_name.lower()
            if 'blueberry' in search_base:
                # For blueberry strains, suggest similar berry/relaxing strains
                similar_strains = [
                    ('blueberry', 0.95),
                    ('purple punch', 0.85),
                    ('granddaddy purple', 0.80)
                ]
            elif 'thai' in search_base:
                # For thai strains, suggest similar indica-dominant strains
                similar_strains = [
                    ('og kush', 0.88),
                    ('sour diesel', 0.82),
                    ('granddaddy purple', 0.78)
                ]
            else:
                # Generic fallback recommendations
                similar_strains = [
                    ('blue dream', 0.85),
                    ('og kush', 0.80),
                    ('granddaddy purple', 0.75)
                ]

        print(f"✅ Found {len(similar_strains)} similar strains")

        # Get dispensary recommendations
        dispensaries = []
        if location:
            print(f"🏪 Searching for dispensaries near: {location}")
            try:
                dispensaries = location_service.find_dispensaries_near_location(
                    location,
                    radius or 25
                )
                print(f"✅ Found {len(dispensaries)} dispensaries")
            except Exception as e:
                # If dispensary search fails, continue without it
                print(f"⚠️  Dispensary search failed: {e}")
                dispensaries = []

        print("📝 Building response...")
        
        # Build detailed terpene analysis
        terpenes = strain_data.get("terpenes", {})
        print(f"🔬 Terpenes found: {terpenes}")
        terpene_analysis = []
        
        # Terpene information database
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
        
        print(f"🧪 Terpene analysis built: {len(terpene_analysis)} terpenes")
        
        result = {
            "strain_analysis": {
                "name": strain_data.get("name", strain_name),
                "terpene_profile": strain_data.get("terpenes", {}),
                "terpenes": terpene_analysis,
                "effects": strain_data.get("effects", []),
                "flavors": strain_data.get("flavors", []),
                "type": strain_data.get("type", "unknown"),
                "thc_range": strain_data.get("thc_range", "unknown"),
                "cbd_range": strain_data.get("cbd_range", "unknown"),
                "description": strain_data.get("description", "No description available")
            },
            "recommendations": [
                {
                    "strain_name": strain,
                    "similarity": similarity,
                    "strain_data": strain_db.get_strain(strain)
                }
                for strain, similarity in similar_strains
            ],
            "dispensaries": dispensaries
        }

        print("✅ Response built successfully")
        return result

    except HTTPException:
        raise
    except Exception as e:
        print(f"❌ Unexpected error: {e}")
        import traceback
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=f"Error processing request: {str(e)}")

@app.get("/api/dispensaries", response_model=List[Dict[str, Any]])
async def get_dispensaries(
    location: str = Query(..., description="Location to search for dispensaries"),
    radius: int = Query(25, description="Search radius in miles")
):
    """Get dispensaries near a location"""

    # Use Google Maps integration
    dispensaries = location_service.find_dispensaries_near_location(location, radius)

    return dispensaries

@app.get("/api/health")
async def health_check():
    """Health check endpoint"""
    return {
        "status": "healthy",
        "timestamp": datetime.now().isoformat(),
        "version": "1.0.0"
    }

# Error handlers
@app.exception_handler(404)
async def not_found_handler(request: Request, exc: HTTPException):
    return JSONResponse(
        status_code=404,
        content={"detail": "Resource not found"}
    )

@app.exception_handler(500)
async def internal_error_handler(request: Request, exc: Exception):
    return JSONResponse(
        status_code=500,
        content={"detail": "Internal server error"}
    )

# Create templates directory and basic HTML template
def create_templates():
    """Create basic HTML templates"""
    templates_dir = Path("templates")
    templates_dir.mkdir(exist_ok=True)
    
    # Create simple index.html
    index_html = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Cannabis Strain Recommendations</title>
    <script src="https://maps.googleapis.com/maps/api/js?key=AIzaSyCi4wQkDC-7x9nl6_DGXKmT3JqaKwYgPks&libraries=places"></script>
    <style>
        body {
            font-family: Arial, sans-serif;
            margin: 0;
            padding: 0;
            background-color: #f5f5f5;
            height: 100vh;
            overflow: hidden;
        }
        .header {
            text-align: center;
            margin-bottom: 20px;
            background: linear-gradient(135deg, #4CAF50 0%, #45a049 100%);
            color: white;
            padding: 20px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
        }
        .container {
            display: flex;
            height: calc(100vh - 120px);
        }
        .left-panel {
            width: 40%;
            padding: 20px;
            overflow-y: auto;
            background: white;
            box-shadow: 2px 0 10px rgba(0,0,0,0.1);
        }
        .right-panel {
            width: 60%;
            position: relative;
        }
        .form-container {
            background: white;
            padding: 20px;
            border-radius: 10px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
            margin-bottom: 20px;
        }
        .form-group {
            margin-bottom: 15px;
        }
        label {
            display: block;
            margin-bottom: 5px;
            font-weight: bold;
            font-size: 14px;
        }
        input, select {
            width: 100%;
            padding: 8px;
            border: 1px solid #ddd;
            border-radius: 5px;
            font-size: 14px;
        }
        button {
            background: #4CAF50;
            color: white;
            padding: 12px 24px;
            border: none;
            border-radius: 5px;
            font-size: 16px;
            cursor: pointer;
            width: 100%;
        }
        button:hover {
            background: #45a049;
        }
        .results {
            background: white;
            padding: 20px;
            border-radius: 10px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
            margin-top: 20px;
        }
        .strain-card {
            border: 1px solid #ddd;
            border-radius: 8px;
            padding: 15px;
            margin-bottom: 15px;
            background: #f9f9f9;
        }
        .strain-name {
            font-size: 18px;
            font-weight: bold;
            color: #333;
            margin-bottom: 8px;
        }
        .strain-type {
            display: inline-block;
            padding: 4px 8px;
            border-radius: 12px;
            font-size: 11px;
            font-weight: bold;
            text-transform: uppercase;
            margin-bottom: 8px;
        }
        .indica { background-color: #e74c3c; color: white; }
        .sativa { background-color: #27ae60; color: white; }
        .hybrid { background-color: #f39c12; color: white; }
        .effects {
            margin-top: 10px;
        }
        .effect-tag {
            display: inline-block;
            background: #3498db;
            color: white;
            padding: 2px 6px;
            border-radius: 10px;
            font-size: 11px;
            margin-right: 4px;
            margin-bottom: 4px;
        }
        .loading {
            text-align: center;
            padding: 40px;
            font-size: 16px;
            color: #666;
        }
        .error {
            background: #e74c3c;
            color: white;
            padding: 12px;
            border-radius: 5px;
            margin-top: 15px;
            font-size: 14px;
        }
        .dispensary-section {
            margin-top: 25px;
        }
        .dispensary-card {
            border: 1px solid #ddd;
            border-radius: 8px;
            padding: 12px;
            margin-bottom: 10px;
            background: #f9f9f9;
            cursor: pointer;
            transition: background-color 0.2s;
        }
        .dispensary-card:hover {
            background: #f0f8ff;
        }
        .dispensary-name {
            font-weight: bold;
            color: #333;
            margin-bottom: 4px;
        }
        .dispensary-address {
            font-size: 12px;
            color: #666;
            margin-bottom: 2px;
        }
        .dispensary-rating {
            font-size: 12px;
            color: #f39c12;
            margin-bottom: 2px;
        }
        .dispensary-distance {
            font-size: 12px;
            color: #27ae60;
        }
        #map {
            height: 100%;
            width: 100%;
        }
        .recommendation-card {
            border-left: 4px solid #4CAF50;
            background: #f8fff8;
        }
        .aliases {
            margin-top: 10px;
            font-size: 12px;
            color: #666;
            font-style: italic;
        }
    </style>
</head>
<body>
    <div class="header">
        <h1>🌿 Cannabis Strain Recommendations</h1>
        <p>Enter your favorite strain and find similar options with nearby dispensaries on the map</p>
    </div>

    <div class="container">
        <div class="left-panel">
            <div class="form-container">
                <form id="recommendationForm">
                    <div class="form-group">
                        <label for="strainName">Favorite Strain *</label>
                        <input type="text" id="strainName" name="strain_name" required placeholder="e.g., Granddaddy Purple">
                        <small style="color: #666; font-size: 12px;">💡 Try any strain! Popular: Blue Dream, OG Kush, Sour Diesel, Granddaddy Purple, Jack Herer</small>
                    </div>

                    <div class="form-group">
                        <label for="location">Your Location</label>
                        <input type="text" id="location" name="location" placeholder="e.g., San Francisco, CA">
                    </div>

                    <div class="form-group">
                        <label for="radius">Search Radius (miles)</label>
                        <select id="radius" name="radius">
                            <option value="10">10 miles</option>
                            <option value="25" selected>25 miles</option>
                            <option value="50">50 miles</option>
                        </select>
                    </div>

                    <button type="submit">Get Recommendations</button>
                </form>
            </div>

            <div id="results" class="results" style="display: none;">
                <h2>Results</h2>
                <div id="resultsContent"></div>
            </div>
        </div>

        <div class="right-panel">
            <div id="map"></div>
        </div>
    </div>

    <script>
        let map;
        let markers = [];
        let infoWindows = [];

        // Initialize Google Map
        function initMap() {
            // Default location (will be updated based on user input)
            const defaultLocation = { lat: 37.7749, lng: -122.4194 }; // San Francisco

            map = new google.maps.Map(document.getElementById('map'), {
                zoom: 12,
                center: defaultLocation,
                mapTypeControl: false,
                streetViewControl: false,
                fullscreenControl: false
            });
        }

        // Initialize map when page loads
        window.onload = function() {
            initMap();
        };

        document.getElementById('recommendationForm').addEventListener('submit', async function(e) {
            e.preventDefault();

            const formData = new FormData(e.target);
            const data = Object.fromEntries(formData.entries());

            // Show loading
            const resultsDiv = document.getElementById('results');
            const resultsContent = document.getElementById('resultsContent');
            resultsDiv.style.display = 'block';
            resultsContent.innerHTML = '<div class="loading">Analyzing strain and finding recommendations...</div>';

            // Clear existing markers
            clearMapMarkers();

            try {
                const response = await fetch('/api/recommendations', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify(data)
                });

                if (!response.ok) {
                    const errorData = await response.json();
                    throw new Error(errorData.detail || 'Failed to get recommendations');
                }

                const result = await response.json();
                displayResults(result, data.location || 'San Francisco, CA');

            } catch (error) {
                resultsContent.innerHTML = `<div class="error">Error: ${error.message}</div>`;
            }
        });

        function clearMapMarkers() {
            markers.forEach(marker => marker.setMap(null));
            markers = [];
            infoWindows.forEach(infoWindow => infoWindow.close());
            infoWindows = [];
        }

        function addDispensaryToMap(dispensary, index) {
            if (!dispensary.latitude || !dispensary.longitude) return;

            const position = {
                lat: dispensary.latitude,
                lng: dispensary.longitude
            };

            const marker = new google.maps.Marker({
                position: position,
                map: map,
                title: dispensary.name,
                icon: {
                    url: 'data:image/svg+xml;charset=UTF-8,' + encodeURIComponent(`
                        <svg width="40" height="40" viewBox="0 0 40 40" xmlns="http://www.w3.org/2000/svg">
                            <circle cx="20" cy="20" r="18" fill="#4CAF50" stroke="white" stroke-width="3"/>
                            <text x="20" y="25" text-anchor="middle" fill="white" font-family="Arial" font-size="12" font-weight="bold">${index + 1}</text>
                        </svg>
                    `),
                    scaledSize: new google.maps.Size(40, 40)
                }
            });

            const infoWindow = new google.maps.InfoWindow({
                content: `
                    <div style="max-width: 250px;">
                        <h4 style="margin: 0 0 8px 0; color: #4CAF50;">${dispensary.name}</h4>
                        <p style="margin: 4px 0;"><strong>Address:</strong> ${dispensary.address}</p>
                        <p style="margin: 4px 0;"><strong>Distance:</strong> ${dispensary.distance_miles} miles</p>
                        <p style="margin: 4px 0;"><strong>Rating:</strong> ${dispensary.rating}/5.0 ⭐</p>
                        ${dispensary.phone ? `<p style="margin: 4px 0;"><strong>Phone:</strong> ${dispensary.phone}</p>` : ''}
                        ${dispensary.website ? `<p style="margin: 4px 0;"><a href="${dispensary.website}" target="_blank">Visit Website</a></p>` : ''}
                    </div>
                `
            });

            marker.addListener('click', () => {
                infoWindows.forEach(iw => iw.close());
                infoWindow.open(map, marker);
            });

            markers.push(marker);
            infoWindows.push(infoWindow);

            return marker;
        }

        function displayResults(data, userLocation) {
            const resultsContent = document.getElementById('resultsContent');

            // Update map center if we have dispensaries
            if (data.dispensaries && data.dispensaries.length > 0) {
                const firstDispensary = data.dispensaries[0];
                if (firstDispensary.latitude && firstDispensary.longitude) {
                    map.setCenter({
                        lat: firstDispensary.latitude,
                        lng: firstDispensary.longitude
                    });
                    map.setZoom(11);
                }
            }

            let html = `
                <h3>🎯 Your Strain Analysis</h3>
                <div class="strain-card">
                    <div class="strain-name">${data.strain_analysis.name}</div>
                    <span class="strain-type ${data.strain_analysis.type}">${data.strain_analysis.type}</span>
                    <p><strong>THC:</strong> ${data.strain_analysis.thc_range} | <strong>CBD:</strong> ${data.strain_analysis.cbd_range}</p>
                    <p style="margin: 8px 0;">${data.strain_analysis.description}</p>
                    <div class="effects">
                        <strong>Effects:</strong>
                        ${data.strain_analysis.effects.map(effect => `<span class="effect-tag">${effect}</span>`).join('')}
                    </div>
                    <div class="aliases">
                        <strong>Common Aliases:</strong> ${getStrainAliases(data.strain_analysis.name)}
                    </div>
                </div>

                <h3>💚 Recommended Similar Strains</h3>
            `;

            if (data.recommendations && data.recommendations.length > 0) {
                data.recommendations.forEach(rec => {
                    if (rec.strain_data) {
                        html += `
                            <div class="strain-card recommendation-card">
                                <div class="strain-name">${rec.strain_name}</div>
                                <span class="strain-type ${rec.strain_data.type}">${rec.strain_data.type}</span>
                                <p><strong>Similarity:</strong> ${(rec.similarity * 100).toFixed(1)}% match</p>
                                <p><strong>THC:</strong> ${rec.strain_data.thc_range} | <strong>CBD:</strong> ${rec.strain_data.cbd_range}</p>
                                <p style="margin: 6px 0;">${rec.strain_data.description.substring(0, 80)}...</p>
                                <div class="effects">
                                    <strong>Effects:</strong>
                                    ${rec.strain_data.effects.slice(0, 4).map(effect => `<span class="effect-tag">${effect}</span>`).join('')}
                                </div>
                                <div class="aliases">
                                    <strong>Aliases:</strong> ${getStrainAliases(rec.strain_name)}
                                </div>
                            </div>
                        `;
                    }
                });
            } else {
                html += '<p style="color: #666; font-style: italic;">No recommendations found for this strain.</p>';
            }

            if (data.dispensaries && data.dispensaries.length > 0) {
                html += `
                    <div class="dispensary-section">
                        <h3>🏪 Nearby Dispensaries (${data.dispensaries.length} found)</h3>
                        <p style="color: #666; font-size: 14px; margin-bottom: 15px;">
                            Click on dispensary names to see them on the map
                        </p>
                `;

                data.dispensaries.forEach((dispensary, index) => {
                    const marker = addDispensaryToMap(dispensary, index);
                    html += `
                        <div class="dispensary-card" onclick="focusOnDispensary(${index})">
                            <div class="dispensary-name">${dispensary.name}</div>
                            <div class="dispensary-address">📍 ${dispensary.address}</div>
                            <div class="dispensary-rating">⭐ ${dispensary.rating}/5.0</div>
                            <div class="dispensary-distance">📏 ${dispensary.distance_miles} miles away</div>
                            ${dispensary.phone ? `<div style="font-size: 12px; color: #666;">📞 ${dispensary.phone}</div>` : ''}
                            ${dispensary.opening_hours && dispensary.opening_hours.open_now ?
                                '<div style="color: #4CAF50; font-weight: bold; font-size: 12px;">🟢 Currently Open</div>' :
                                '<div style="color: #e74c3c; font-weight: bold; font-size: 12px;">🔴 Currently Closed</div>'
                            }
                        </div>
                    `;
                });

                html += `</div>`;
            } else {
                html += '<p style="color: #666; font-style: italic;">No dispensaries found in this area.</p>';
            }

            resultsContent.innerHTML = html;
        }

        function focusOnDispensary(index) {
            if (markers[index]) {
                map.setCenter(markers[index].getPosition());
                map.setZoom(15);
                google.maps.event.trigger(markers[index], 'click');
            }
        }

        function getStrainAliases(strainName) {
            const aliases = {
                'blue dream': 'Blueberry Dream, BD',
                'og kush': 'OG, Original Gangster',
                'sour diesel': 'Sour D, Sour Deez',
                'granddaddy purple': 'GDP, Grand Daddy Purp',
                'jack herer': 'JH, Jack',
                'green crack': 'Green Crack, GC',
                'purple punch': 'Purple Punch, PP',
                'wedding cake': 'Wedding Cake, WC',
                'gelato': 'Gelato, G',
                'charlotte\'s web': 'Charlotte\'s Web, CW',
                'harlequin': 'Harlequin, Harle',
                'mystic dragon': 'Mystic Dragon, MD'
            };
            return aliases[strainName.toLowerCase()] || 'None known';
        }
    </script>
</body>
</html>
    """
    
    with open(templates_dir / "index.html", "w") as f:
        f.write(index_html)

# CLI Interface removed to keep dependencies simple

def main():
    """Main entry point for running the web server"""
    create_templates()
    print("🚀 Starting web server at http://127.0.0.1:8000")
    print("📖 API documentation available at http://127.0.0.1:8000/docs")
    print("🌐 Open your browser to http://127.0.0.1:8000")

    uvicorn.run(
        "web_interface:app",
        host="127.0.0.1",
        port=8000,
        reload=False
    )

if __name__ == "__main__":
    main()