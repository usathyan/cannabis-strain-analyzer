"""
Google Maps Integration for Location Services
Provides geocoding, distance calculation, and location-based services
"""

import requests
import json
from typing import Dict, List, Optional, Any, Tuple
import os
from pathlib import Path
import time
from datetime import datetime, timedelta
import math

class GoogleMapsAPI:
    """Integration with Google Maps API for location services"""
    
    def __init__(self, api_key: Optional[str] = None):
        self.api_key = api_key or os.getenv("GOOGLE_MAPS_API_KEY")
        self.base_url = "https://maps.googleapis.com/maps/api"
        
        # Rate limiting
        self.last_request_time = 0
        self.min_request_interval = 0.1  # 100ms between requests
        
        # Cache directory
        self.cache_dir = Path("cache/google_maps")
        self.cache_dir.mkdir(parents=True, exist_ok=True)
    
    def _rate_limit(self):
        """Ensure we don't exceed rate limits"""
        current_time = time.time()
        time_since_last = current_time - self.last_request_time
        
        if time_since_last < self.min_request_interval:
            time.sleep(self.min_request_interval - time_since_last)
        
        self.last_request_time = time.time()
    
    def _get_cache_path(self, endpoint: str, params: Dict[str, Any] = None) -> Path:
        """Get cache file path for an API call"""
        cache_key = f"{endpoint}_{hash(str(sorted(params.items())) if params else '')}"
        return self.cache_dir / f"{cache_key}.json"
    
    def _is_cache_valid(self, cache_path: Path, max_age_days: int = 30) -> bool:
        """Check if cache is still valid (geocoding data doesn't change often)"""
        if not cache_path.exists():
            return False
        
        cache_time = datetime.fromtimestamp(cache_path.stat().st_mtime)
        return datetime.now() - cache_time < timedelta(days=max_age_days)
    
    def _make_request(self, endpoint: str, params: Dict[str, Any] = None, use_cache: bool = True) -> Dict[str, Any]:
        """Make API request with caching and rate limiting"""
        
        # Check cache first
        if use_cache:
            cache_path = self._get_cache_path(endpoint, params)
            if self._is_cache_valid(cache_path):
                with open(cache_path, 'r') as f:
                    return json.load(f)
        
        # Rate limiting
        self._rate_limit()
        
        # Add API key to params
        if params is None:
            params = {}
        params["key"] = self.api_key
        
        # Make request
        url = f"{self.base_url}/{endpoint}"
        
        try:
            response = requests.get(url, params=params, timeout=30)
            response.raise_for_status()
            
            data = response.json()
            
            # Cache the response
            if use_cache:
                cache_path = self._get_cache_path(endpoint, params)
                with open(cache_path, 'w') as f:
                    json.dump(data, f, indent=2)
            
            return data
            
        except requests.exceptions.RequestException as e:
            print(f"Google Maps API request failed: {e}")
            return {}
    
    def geocode_address(self, address: str) -> Optional[Dict[str, Any]]:
        """Convert address to latitude/longitude coordinates"""
        
        params = {
            "address": address,
            "region": "us"  # Bias results to US
        }
        
        response = self._make_request("geocode/json", params)
        
        if response.get("status") == "OK" and response.get("results"):
            result = response["results"][0]
            location = result["geometry"]["location"]
            
            return {
                "address": result["formatted_address"],
                "latitude": location["lat"],
                "longitude": location["lng"],
                "place_id": result["place_id"],
                "types": result.get("types", []),
                "components": result.get("address_components", [])
            }
        
        return None
    
    def reverse_geocode(self, latitude: float, longitude: float) -> Optional[Dict[str, Any]]:
        """Convert latitude/longitude to address"""
        
        params = {
            "latlng": f"{latitude},{longitude}"
        }
        
        response = self._make_request("geocode/json", params)
        
        if response.get("status") == "OK" and response.get("results"):
            result = response["results"][0]
            
            return {
                "address": result["formatted_address"],
                "latitude": latitude,
                "longitude": longitude,
                "place_id": result["place_id"],
                "types": result.get("types", []),
                "components": result.get("address_components", [])
            }
        
        return None
    
    def calculate_distance(self, 
                          origin: Tuple[float, float], 
                          destination: Tuple[float, float],
                          mode: str = "driving") -> Optional[Dict[str, Any]]:
        """Calculate distance and travel time between two points"""
        
        params = {
            "origins": f"{origin[0]},{origin[1]}",
            "destinations": f"{destination[0]},{destination[1]}",
            "mode": mode,
            "units": "imperial"
        }
        
        response = self._make_request("distancematrix/json", params)
        
        if response.get("status") == "OK" and response.get("rows"):
            row = response["rows"][0]
            if row.get("elements"):
                element = row["elements"][0]
                
                if element.get("status") == "OK":
                    return {
                        "distance_text": element["distance"]["text"],
                        "distance_meters": element["distance"]["value"],
                        "duration_text": element["duration"]["text"],
                        "duration_seconds": element["duration"]["value"]
                    }
        
        return None
    
    def find_nearby_places(self, 
                          location: Tuple[float, float], 
                          radius: int = 1000,
                          place_type: str = "store") -> List[Dict[str, Any]]:
        """Find nearby places of a specific type"""
        
        params = {
            "location": f"{location[0]},{location[1]}",
            "radius": radius,
            "type": place_type,
            "keyword": "dispensary cannabis marijuana"
        }
        
        response = self._make_request("place/nearbysearch/json", params)
        
        places = []
        if response.get("status") == "OK" and response.get("results"):
            for result in response["results"]:
                place = {
                    "name": result["name"],
                    "place_id": result["place_id"],
                    "latitude": result["geometry"]["location"]["lat"],
                    "longitude": result["geometry"]["location"]["lng"],
                    "rating": result.get("rating"),
                    "price_level": result.get("price_level"),
                    "types": result.get("types", []),
                    "vicinity": result.get("vicinity", ""),
                    "open_now": result.get("opening_hours", {}).get("open_now")
                }
                places.append(place)
        
        return places
    
    def get_place_details(self, place_id: str) -> Optional[Dict[str, Any]]:
        """Get detailed information about a place"""
        
        params = {
            "place_id": place_id,
            "fields": "name,formatted_address,geometry,rating,price_level,opening_hours,website,formatted_phone_number,reviews"
        }
        
        response = self._make_request("place/details/json", params)
        
        if response.get("status") == "OK" and response.get("result"):
            result = response["result"]
            
            return {
                "name": result["name"],
                "address": result["formatted_address"],
                "latitude": result["geometry"]["location"]["lat"],
                "longitude": result["geometry"]["location"]["lng"],
                "rating": result.get("rating"),
                "price_level": result.get("price_level"),
                "website": result.get("website"),
                "phone": result.get("formatted_phone_number"),
                "opening_hours": result.get("opening_hours", {}),
                "reviews": result.get("reviews", [])
            }
        
        return None
    
    def get_directions(self, 
                      origin: str, 
                      destination: str,
                      mode: str = "driving") -> Optional[Dict[str, Any]]:
        """Get directions between two addresses"""
        
        params = {
            "origin": origin,
            "destination": destination,
            "mode": mode,
            "units": "imperial"
        }
        
        response = self._make_request("directions/json", params)
        
        if response.get("status") == "OK" and response.get("routes"):
            route = response["routes"][0]
            leg = route["legs"][0]
            
            return {
                "distance_text": leg["distance"]["text"],
                "distance_meters": leg["distance"]["value"],
                "duration_text": leg["duration"]["text"],
                "duration_seconds": leg["duration"]["value"],
                "start_address": leg["start_address"],
                "end_address": leg["end_address"],
                "steps": [
                    {
                        "instruction": step["html_instructions"],
                        "distance": step["distance"]["text"],
                        "duration": step["duration"]["text"]
                    }
                    for step in leg["steps"]
                ]
            }
        
        return None

class LocationService:
    """High-level location service using Google Maps API"""
    
    def __init__(self):
        self.maps_api = GoogleMapsAPI()
    
    def geocode_location(self, location: str) -> Optional[Dict[str, Any]]:
        """Geocode a location string to coordinates"""
        return self.maps_api.geocode_address(location)
    
    def find_dispensaries_near_location(self,
                                      location: str,
                                      radius_miles: int = 25) -> List[Dict[str, Any]]:
        """Find dispensaries near a location"""

        # Check if we have a real API key
        if not self.maps_api.api_key:
            print("⚠️  No Google Maps API key found, using mock data")
            return self._get_mock_dispensaries(location, radius_miles)

        try:
            # Try real API first
            # Geocode the location
            geocoded = self.geocode_location(location)
            if not geocoded:
                return self._get_mock_dispensaries(location, radius_miles)

            coordinates = (geocoded["latitude"], geocoded["longitude"])

            # Convert miles to meters
            radius_meters = radius_miles * 1609.34

            # Find nearby places
            nearby_places = self.maps_api.find_nearby_places(coordinates, radius_meters, "store")

            # Filter for dispensaries and get details
            dispensaries = []
            for place in nearby_places:
                # Check if it's likely a dispensary
                if self._is_likely_dispensary(place):
                    details = self.maps_api.get_place_details(place["place_id"])
                    if details:
                        # Calculate distance
                        distance = self._calculate_distance_miles(
                            coordinates,
                            (details["latitude"], details["longitude"])
                        )

                        dispensary = {
                            "id": place["place_id"],
                            "name": details["name"],
                            "address": details["address"],
                            "latitude": details["latitude"],
                            "longitude": details["longitude"],
                            "rating": details.get("rating"),
                            "price_level": details.get("price_level"),
                            "website": details.get("website"),
                            "phone": details.get("phone"),
                            "distance_miles": distance,
                            "opening_hours": details.get("opening_hours", {}),
                            "reviews": details.get("reviews", [])
                        }
                        dispensaries.append(dispensary)

            # Sort by distance
            dispensaries.sort(key=lambda x: x["distance_miles"])

            return dispensaries

        except Exception as e:
            print(f"❌ Real API failed: {e}, falling back to mock data")
            return self._get_mock_dispensaries(location, radius_miles)

    def _get_mock_dispensaries(self, location: str, radius_miles: int = 25) -> List[Dict[str, Any]]:
        """Return mock dispensary data for demonstration"""
        print(f"📍 Generating mock dispensaries near: {location}")

        # Mock dispensaries with realistic data
        mock_dispensaries = [
            {
                "id": "mock_1",
                "name": "Green Valley Dispensary",
                "address": f"123 Main St, {location}",
                "latitude": 40.7128,
                "longitude": -74.0060,
                "rating": 4.5,
                "price_level": 3,
                "website": "https://greenvalleydispensary.com",
                "phone": "(555) 123-4567",
                "distance_miles": 2.3,
                "opening_hours": {
                    "open_now": True,
                    "weekday_text": [
                        "Monday: 9:00 AM – 9:00 PM",
                        "Tuesday: 9:00 AM – 9:00 PM",
                        "Wednesday: 9:00 AM – 9:00 PM",
                        "Thursday: 9:00 AM – 9:00 PM",
                        "Friday: 9:00 AM – 9:00 PM",
                        "Saturday: 9:00 AM – 9:00 PM",
                        "Sunday: 10:00 AM – 7:00 PM"
                    ]
                },
                "reviews": []
            },
            {
                "id": "mock_2",
                "name": "Herbal Remedies Medical",
                "address": f"456 Oak Ave, {location}",
                "latitude": 40.7589,
                "longitude": -73.9851,
                "rating": 4.7,
                "price_level": 2,
                "website": "https://herbalremedies.com",
                "phone": "(555) 987-6543",
                "distance_miles": 3.8,
                "opening_hours": {
                    "open_now": True,
                    "weekday_text": [
                        "Monday: 8:00 AM – 10:00 PM",
                        "Tuesday: 8:00 AM – 10:00 PM",
                        "Wednesday: 8:00 AM – 10:00 PM",
                        "Thursday: 8:00 AM – 10:00 PM",
                        "Friday: 8:00 AM – 10:00 PM",
                        "Saturday: 8:00 AM – 10:00 PM",
                        "Sunday: 9:00 AM – 8:00 PM"
                    ]
                },
                "reviews": []
            },
            {
                "id": "mock_3",
                "name": "Urban Cannabis Collective",
                "address": f"789 Pine St, {location}",
                "latitude": 40.7505,
                "longitude": -73.9934,
                "rating": 4.3,
                "price_level": 2,
                "website": "https://urbancannabis.com",
                "phone": "(555) 456-7890",
                "distance_miles": 4.2,
                "opening_hours": {
                    "open_now": False,
                    "weekday_text": [
                        "Monday: 10:00 AM – 8:00 PM",
                        "Tuesday: 10:00 AM – 8:00 PM",
                        "Wednesday: 10:00 AM – 8:00 PM",
                        "Thursday: 10:00 AM – 8:00 PM",
                        "Friday: 10:00 AM – 8:00 PM",
                        "Saturday: 11:00 AM – 7:00 PM",
                        "Sunday: Closed"
                    ]
                },
                "reviews": []
            }
        ]

        # Filter by radius
        filtered_dispensaries = [
            d for d in mock_dispensaries
            if d["distance_miles"] <= radius_miles
        ]

        print(f"✅ Found {len(filtered_dispensaries)} mock dispensaries within {radius_miles} miles")
        return filtered_dispensaries
    
    def _is_likely_dispensary(self, place: Dict[str, Any]) -> bool:
        """Check if a place is likely a dispensary"""
        name = place["name"].lower()
        types = place.get("types", [])
        
        # Check name for dispensary keywords
        dispensary_keywords = [
            "dispensary", "cannabis", "marijuana", "weed", "green", "herb",
            "medicinal", "medical", "cannabis", "thc", "cbd"
        ]
        
        if any(keyword in name for keyword in dispensary_keywords):
            return True
        
        # Check types
        dispensary_types = ["store", "establishment", "point_of_interest"]
        if any(t in types for t in dispensary_types):
            return True
        
        return False
    
    def _calculate_distance_miles(self, 
                                 point1: Tuple[float, float], 
                                 point2: Tuple[float, float]) -> float:
        """Calculate distance between two points in miles using Haversine formula"""
        
        lat1, lon1 = point1
        lat2, lon2 = point2
        
        # Convert to radians
        lat1, lon1, lat2, lon2 = map(math.radians, [lat1, lon1, lat2, lon2])
        
        # Haversine formula
        dlat = lat2 - lat1
        dlon = lon2 - lon1
        a = math.sin(dlat/2)**2 + math.cos(lat1) * math.cos(lat2) * math.sin(dlon/2)**2
        c = 2 * math.asin(math.sqrt(a))
        
        # Radius of earth in miles
        r = 3959
        
        return c * r
    
    def get_travel_time(self, 
                       origin: str, 
                       destination: str,
                       mode: str = "driving") -> Optional[Dict[str, Any]]:
        """Get travel time between two locations"""
        
        # Geocode both locations
        origin_geocoded = self.geocode_location(origin)
        destination_geocoded = self.geocode_location(destination)
        
        if not origin_geocoded or not destination_geocoded:
            return None
        
        # Calculate distance and time
        origin_coords = (origin_geocoded["latitude"], origin_geocoded["longitude"])
        dest_coords = (destination_geocoded["latitude"], destination_geocoded["longitude"])
        
        return self.maps_api.calculate_distance(origin_coords, dest_coords, mode)
    
    def get_directions_to_dispensary(self, 
                                   user_location: str, 
                                   dispensary: Dict[str, Any]) -> Optional[Dict[str, Any]]:
        """Get directions from user location to dispensary"""
        
        dispensary_address = dispensary.get("address", "")
        if not dispensary_address:
            return None
        
        return self.maps_api.get_directions(user_location, dispensary_address)

# CLI Interface removed to keep dependencies simple





