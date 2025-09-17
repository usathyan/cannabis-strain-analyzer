"""
Enhanced Strain Database with Comprehensive Terpene Profiles
"""

from typing import Dict, List, Optional, Any
import json
from pathlib import Path
import numpy as np
from sklearn.metrics.pairwise import cosine_similarity
import os

class EnhancedStrainDatabase:
    """Enhanced strain database with comprehensive terpene profiles and effects"""
    
    def __init__(self):
        self.strains = self._load_comprehensive_database()
        self.terpene_effects = self._load_terpene_effects()
        self.effect_categories = self._load_effect_categories()
        self.custom_strains_file = Path("custom_strains.json")
        self._load_custom_strains()
    
    def _load_comprehensive_database(self) -> Dict[str, Dict]:
        """Load comprehensive strain database with detailed terpene profiles"""
        return {
            # Indica Dominant Strains
            "granddaddy purple": {
                "terpenes": {
                    "myrcene": 0.85, "caryophyllene": 0.65, "pinene": 0.45, 
                    "limonene": 0.35, "linalool": 0.25, "humulene": 0.20, "terpinolene": 0.10
                },
                "effects": ["relaxed", "sleepy", "happy", "euphoric", "hungry"],
                "medical_effects": ["pain relief", "insomnia", "stress", "anxiety"],
                "type": "indica",
                "thc_range": "17-24%",
                "cbd_range": "0.1-0.5%",
                "description": "A classic indica with grape and berry flavors, known for deep relaxation",
                "flavors": ["grape", "berry", "earthy", "sweet"],
                "aromas": ["fruity", "sweet", "grape", "berry"],
                "best_time": "evening",
                "activity": "relaxation"
            },
            "og kush": {
                "terpenes": {
                    "myrcene": 0.75, "caryophyllene": 0.60, "limonene": 0.45, 
                    "pinene": 0.35, "linalool": 0.30, "humulene": 0.25, "terpinolene": 0.15
                },
                "effects": ["relaxed", "happy", "euphoric", "sleepy", "uplifted"],
                "medical_effects": ["pain relief", "stress", "depression", "insomnia"],
                "type": "indica",
                "thc_range": "18-26%",
                "cbd_range": "0.1-0.8%",
                "description": "Classic indica with earthy pine flavors and strong relaxation effects",
                "flavors": ["earthy", "pine", "woody", "spicy"],
                "aromas": ["pine", "earthy", "woody", "spicy"],
                "best_time": "evening",
                "activity": "relaxation"
            },
            "purple punch": {
                "terpenes": {
                    "myrcene": 0.80, "limonene": 0.50, "caryophyllene": 0.45,
                    "pinene": 0.30, "linalool": 0.35, "humulene": 0.20, "terpinolene": 0.15
                },
                "effects": ["relaxed", "happy", "sleepy", "euphoric", "hungry"],
                "medical_effects": ["pain relief", "insomnia", "stress", "appetite"],
                "type": "indica",
                "thc_range": "15-20%",
                "cbd_range": "0.1-0.3%",
                "description": "Sweet indica with grape and berry flavors, perfect for evening relaxation",
                "flavors": ["grape", "berry", "sweet", "fruity"],
                "aromas": ["sweet", "fruity", "grape", "berry"],
                "best_time": "evening",
                "activity": "relaxation"
            },
            "blueberry": {
                "terpenes": {
                    "myrcene": 0.75, "limonene": 0.55, "caryophyllene": 0.50,
                    "pinene": 0.35, "linalool": 0.30, "humulene": 0.25, "terpinolene": 0.20
                },
                "effects": ["relaxed", "happy", "euphoric", "creative", "hungry"],
                "medical_effects": ["pain relief", "stress", "depression", "insomnia"],
                "type": "indica",
                "thc_range": "16-22%",
                "cbd_range": "0.1-0.5%",
                "description": "Sweet blueberry-flavored indica with relaxing effects, perfect for evening use",
                "flavors": ["blueberry", "sweet", "berry", "fruity"],
                "aromas": ["blueberry", "sweet", "fruity", "berry"],
                "best_time": "evening",
                "activity": "relaxation"
            },
            
            # Sativa Dominant Strains
            "sour diesel": {
                "terpenes": {
                    "limonene": 0.80, "caryophyllene": 0.55, "pinene": 0.45, 
                    "myrcene": 0.35, "terpinolene": 0.40, "linalool": 0.20, "humulene": 0.25
                },
                "effects": ["energetic", "uplifted", "creative", "focused", "happy"],
                "medical_effects": ["depression", "fatigue", "stress", "pain relief"],
                "type": "sativa",
                "thc_range": "20-25%",
                "cbd_range": "0.1-0.5%",
                "description": "Energizing sativa with diesel and citrus notes, great for daytime use",
                "flavors": ["diesel", "citrus", "sour", "earthy"],
                "aromas": ["diesel", "citrus", "sour", "pungent"],
                "best_time": "morning",
                "activity": "creative"
            },
            "jack herer": {
                "terpenes": {
                    "pinene": 0.85, "limonene": 0.65, "caryophyllene": 0.45, 
                    "myrcene": 0.35, "terpinolene": 0.30, "linalool": 0.25, "humulene": 0.20
                },
                "effects": ["energetic", "uplifted", "creative", "focused", "happy"],
                "medical_effects": ["depression", "fatigue", "stress", "pain relief"],
                "type": "sativa",
                "thc_range": "15-24%",
                "cbd_range": "0.1-0.8%",
                "description": "Uplifting sativa with pine and citrus notes, legendary for creativity",
                "flavors": ["pine", "citrus", "earthy", "spicy"],
                "aromas": ["pine", "citrus", "earthy", "spicy"],
                "best_time": "morning",
                "activity": "creative"
            },
            "green crack": {
                "terpenes": {
                    "limonene": 0.75, "pinene": 0.70, "caryophyllene": 0.40, 
                    "myrcene": 0.30, "terpinolene": 0.35, "linalool": 0.20, "humulene": 0.25
                },
                "effects": ["energetic", "uplifted", "focused", "happy", "creative"],
                "medical_effects": ["depression", "fatigue", "stress", "ADHD"],
                "type": "sativa",
                "thc_range": "13-21%",
                "cbd_range": "0.1-0.4%",
                "description": "High-energy sativa with sweet tropical flavors, perfect for productivity",
                "flavors": ["tropical", "sweet", "citrus", "mango"],
                "aromas": ["sweet", "tropical", "citrus", "fruity"],
                "best_time": "morning",
                "activity": "work"
            },
            
            # Hybrid Strains
            "blue dream": {
                "terpenes": {
                    "myrcene": 0.55, "pinene": 0.70, "caryophyllene": 0.45, 
                    "limonene": 0.60, "linalool": 0.30, "humulene": 0.25, "terpinolene": 0.20
                },
                "effects": ["happy", "uplifted", "creative", "relaxed", "focused"],
                "medical_effects": ["pain relief", "depression", "stress", "anxiety"],
                "type": "hybrid",
                "thc_range": "17-24%",
                "cbd_range": "0.1-0.6%",
                "description": "Balanced hybrid with sweet berry aroma, perfect for any time of day",
                "flavors": ["berry", "sweet", "earthy", "blueberry"],
                "aromas": ["sweet", "berry", "earthy", "fruity"],
                "best_time": "any",
                "activity": "balanced"
            },
            "wedding cake": {
                "terpenes": {
                    "limonene": 0.70, "caryophyllene": 0.60, "myrcene": 0.50, 
                    "pinene": 0.40, "linalool": 0.35, "humulene": 0.30, "terpinolene": 0.25
                },
                "effects": ["relaxed", "happy", "euphoric", "uplifted", "creative"],
                "medical_effects": ["pain relief", "stress", "depression", "anxiety"],
                "type": "hybrid",
                "thc_range": "20-28%",
                "cbd_range": "0.1-0.5%",
                "description": "Potent hybrid with sweet vanilla flavors and balanced effects",
                "flavors": ["sweet", "vanilla", "earthy", "spicy"],
                "aromas": ["sweet", "vanilla", "earthy", "spicy"],
                "best_time": "afternoon",
                "activity": "social"
            },
            "gelato": {
                "terpenes": {
                    "limonene": 0.65, "caryophyllene": 0.55, "myrcene": 0.45, 
                    "pinene": 0.35, "linalool": 0.40, "humulene": 0.25, "terpinolene": 0.20
                },
                "effects": ["relaxed", "happy", "euphoric", "uplifted", "creative"],
                "medical_effects": ["pain relief", "stress", "depression", "anxiety"],
                "type": "hybrid",
                "thc_range": "18-25%",
                "cbd_range": "0.1-0.4%",
                "description": "Sweet hybrid with dessert-like flavors and euphoric effects",
                "flavors": ["sweet", "dessert", "vanilla", "berry"],
                "aromas": ["sweet", "dessert", "vanilla", "fruity"],
                "best_time": "evening",
                "activity": "social"
            },
            
            # High CBD Strains
            "charlotte's web": {
                "terpenes": {
                    "myrcene": 0.40, "pinene": 0.35, "caryophyllene": 0.30, 
                    "limonene": 0.25, "linalool": 0.20, "humulene": 0.15, "terpinolene": 0.10
                },
                "effects": ["relaxed", "focused", "clear-headed", "uplifted"],
                "medical_effects": ["epilepsy", "anxiety", "pain relief", "inflammation"],
                "type": "sativa",
                "thc_range": "0.3-0.5%",
                "cbd_range": "17-20%",
                "description": "High-CBD strain developed for epilepsy treatment, minimal psychoactive effects",
                "flavors": ["earthy", "woody", "spicy", "pine"],
                "aromas": ["earthy", "woody", "spicy", "pine"],
                "best_time": "any",
                "activity": "medical"
            },
            "harlequin": {
                "terpenes": {
                    "myrcene": 0.35, "pinene": 0.40, "caryophyllene": 0.30, 
                    "limonene": 0.25, "linalool": 0.20, "humulene": 0.15, "terpinolene": 0.10
                },
                "effects": ["relaxed", "focused", "clear-headed", "uplifted"],
                "medical_effects": ["pain relief", "anxiety", "inflammation", "stress"],
                "type": "sativa",
                "thc_range": "4-7%",
                "cbd_range": "8-12%",
                "description": "Balanced CBD:THC ratio for therapeutic use without strong psychoactive effects",
                "flavors": ["earthy", "woody", "spicy", "mango"],
                "aromas": ["earthy", "woody", "spicy", "fruity"],
                "best_time": "any",
                "activity": "medical"
            }
        }
    
    def _load_terpene_effects(self) -> Dict[str, Dict]:
        """Load terpene effects and properties"""
        return {
            "myrcene": {
                "effects": ["relaxed", "sleepy", "sedated"],
                "medical": ["pain relief", "insomnia", "muscle relaxation"],
                "flavors": ["earthy", "musky", "clove"],
                "aromas": ["earthy", "musky", "clove"],
                "description": "Most common terpene, promotes relaxation and sleep"
            },
            "caryophyllene": {
                "effects": ["relaxed", "euphoric", "uplifted"],
                "medical": ["anti-inflammatory", "pain relief", "anxiety"],
                "flavors": ["pepper", "spicy", "woody"],
                "aromas": ["pepper", "spicy", "woody"],
                "description": "Only terpene that acts as a cannabinoid, anti-inflammatory"
            },
            "pinene": {
                "effects": ["alert", "focused", "energetic"],
                "medical": ["bronchodilator", "anti-inflammatory", "memory"],
                "flavors": ["pine", "woody", "fresh"],
                "aromas": ["pine", "woody", "fresh"],
                "description": "Promotes alertness and memory retention"
            },
            "limonene": {
                "effects": ["uplifted", "happy", "energetic"],
                "medical": ["anti-anxiety", "anti-depressant", "stress relief"],
                "flavors": ["citrus", "lemon", "orange"],
                "aromas": ["citrus", "lemon", "orange"],
                "description": "Mood elevator, stress relief, anti-anxiety"
            },
            "linalool": {
                "effects": ["relaxed", "calm", "sedated"],
                "medical": ["anti-anxiety", "sedative", "anti-convulsant"],
                "flavors": ["floral", "lavender", "spicy"],
                "aromas": ["floral", "lavender", "spicy"],
                "description": "Calming and sedating, anti-anxiety properties"
            },
            "humulene": {
                "effects": ["focused", "appetite suppressant"],
                "medical": ["anti-inflammatory", "anti-bacterial", "appetite suppressant"],
                "flavors": ["hoppy", "woody", "earthy"],
                "aromas": ["hoppy", "woody", "earthy"],
                "description": "Appetite suppressant, anti-inflammatory"
            },
            "terpinolene": {
                "effects": ["uplifted", "energetic", "creative"],
                "medical": ["anti-oxidant", "sedative", "anti-bacterial"],
                "flavors": ["floral", "citrus", "pine"],
                "aromas": ["floral", "citrus", "pine"],
                "description": "Uplifting and energizing, promotes creativity"
            }
        }
    
    def _load_effect_categories(self) -> Dict[str, List[str]]:
        """Load effect categories for filtering"""
        return {
            "relaxation": ["relaxed", "sleepy", "calm", "sedated"],
            "energy": ["energetic", "uplifted", "focused", "alert"],
            "mood": ["happy", "euphoric", "uplifted", "creative"],
            "medical": ["pain relief", "anti-inflammatory", "anti-anxiety", "stress relief"],
            "time_of_day": ["morning", "afternoon", "evening", "night", "any"],
            "activity": ["work", "creative", "social", "relaxation", "medical", "balanced"]
        }
    
    def get_strain(self, strain_name: str) -> Optional[Dict]:
        """Get strain data by name"""
        return self.strains.get(strain_name.lower().strip())
    
    def search_strains(self, 
                      terpene_profile: Optional[Dict[str, float]] = None,
                      effects: Optional[List[str]] = None,
                      strain_type: Optional[str] = None,
                      thc_min: Optional[float] = None,
                      thc_max: Optional[float] = None,
                      cbd_min: Optional[float] = None,
                      cbd_max: Optional[float] = None,
                      best_time: Optional[str] = None,
                      activity: Optional[str] = None,
                      limit: Optional[int] = None) -> List[Dict[str, Any]]:
        """Search strains based on multiple criteria"""
        results = []
        
        for name, data in self.strains.items():
            # Check strain type
            if strain_type and strain_type.lower() != "any" and data["type"] != strain_type.lower():
                continue
            
            # Check THC range
            if thc_min or thc_max:
                thc_range = data["thc_range"]
                thc_min_val = float(thc_range.split("-")[0].replace("%", ""))
                thc_max_val = float(thc_range.split("-")[1].replace("%", ""))
                
                if thc_min and thc_max_val < thc_min:
                    continue
                if thc_max and thc_min_val > thc_max:
                    continue
            
            # Check CBD range
            if cbd_min or cbd_max:
                cbd_range = data["cbd_range"]
                cbd_min_val = float(cbd_range.split("-")[0].replace("%", ""))
                cbd_max_val = float(cbd_range.split("-")[1].replace("%", ""))
                
                if cbd_min and cbd_max_val < cbd_min:
                    continue
                if cbd_max and cbd_min_val > cbd_max:
                    continue
            
            # Check effects
            if effects:
                if not any(effect in data["effects"] for effect in effects):
                    continue
            
            # Check best time
            if best_time and best_time.lower() != "any" and data["best_time"] != best_time.lower():
                continue
            
            # Check activity
            if activity and activity.lower() != "any" and data["activity"] != activity.lower():
                continue
            
            # Calculate terpene similarity if provided
            similarity = 1.0
            if terpene_profile:
                similarity = self._calculate_terpene_similarity(terpene_profile, data["terpenes"])
            
            results.append({
                "name": name,
                "data": data,
                "similarity": similarity
            })
        
        # Sort by similarity if terpene profile provided
        if terpene_profile:
            results.sort(key=lambda x: x["similarity"], reverse=True)
        
        # Apply limit if specified
        if limit:
            results = results[:limit]
        
        return results
    
    def _calculate_terpene_similarity(self, target: Dict[str, float], candidate: Dict[str, float]) -> float:
        """Calculate similarity between terpene profiles"""
        common_terpenes = set(target.keys()) & set(candidate.keys())
        
        if not common_terpenes:
            return 0.0
        
        similarities = []
        for terpene in common_terpenes:
            diff = abs(target[terpene] - candidate[terpene])
            similarity = 1.0 - diff
            similarities.append(similarity)
        
        return sum(similarities) / len(similarities)
    
    def get_terpene_info(self, terpene_name: str) -> Optional[Dict]:
        """Get terpene information"""
        return self.terpene_effects.get(terpene_name.lower())
    
    def get_all_terpenes(self) -> List[str]:
        """Get list of all terpenes"""
        return list(self.terpene_effects.keys())
    
    def get_effect_categories(self) -> Dict[str, List[str]]:
        """Get effect categories"""
        return self.effect_categories

    def get_similar_strains(self, strain_name: str, limit: int = 3) -> List[tuple]:
        """Get similar strains based on terpene profile"""
        target_strain = self.get_strain(strain_name)
        if not target_strain:
            return []

        target_terpenes = target_strain["terpenes"]
        similarities = []

        for name, data in self.strains.items():
            if name.lower() == strain_name.lower():
                continue

            similarity = self._calculate_terpene_similarity(target_terpenes, data["terpenes"])
            similarities.append((name, similarity))

        # Sort by similarity and return top results
        similarities.sort(key=lambda x: x[1], reverse=True)
        return similarities[:limit]

    def add_custom_strain(self, strain_name: str, strain_data: Dict[str, Any]):
        """Add a custom strain to the database and persist it"""
        key = strain_name.lower()
        self.strains[key] = strain_data
        self._save_custom_strains()

    def _load_custom_strains(self):
        """Load custom strains from file"""
        if self.custom_strains_file.exists():
            try:
                with open(self.custom_strains_file, 'r') as f:
                    custom_strains = json.load(f)
                    self.strains.update(custom_strains)
                    print(f"✅ Loaded {len(custom_strains)} custom strains from file")
            except Exception as e:
                print(f"⚠️  Could not load custom strains: {e}")

    def _save_custom_strains(self):
        """Save custom strains to file"""
        # Get only the custom strains (not the built-in ones)
        builtin_strains = set(self._load_comprehensive_database().keys())
        custom_strains = {
            name: data for name, data in self.strains.items()
            if name not in builtin_strains
        }

        try:
            with open(self.custom_strains_file, 'w') as f:
                json.dump(custom_strains, f, indent=2)
                print(f"✅ Saved {len(custom_strains)} custom strains to file")
        except Exception as e:
            print(f"⚠️  Could not save custom strains: {e}")