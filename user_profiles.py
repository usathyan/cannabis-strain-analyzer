"""
User profile management and terpene analysis
"""

from typing import List, Dict, Any, Optional
from sqlalchemy.orm import Session
from database import User, engine
from enhanced_strain_database import EnhancedStrainDatabase
import numpy as np
from sklearn.metrics.pairwise import cosine_similarity

class UserProfileService:
    def __init__(self):
        self.strain_db = EnhancedStrainDatabase()
    
    def update_user_profile(self, user_id: int, name: str, address: str, radius: int, favorite_strains: List[str]) -> Dict[str, Any]:
        """Update user profile and compute terpene profiles for favorite strains"""
        with Session(engine) as session:
            user = session.query(User).filter(User.id == user_id).first()
            if not user:
                raise ValueError("User not found")
            
            # Update basic profile
            user.name = name
            user.address = address
            user.radius = radius
            user.favorite_strains = favorite_strains
            
            # Compute terpene profiles for favorite strains
            terpene_profiles = self._compute_terpene_profiles(favorite_strains)
            user.terpene_profiles = terpene_profiles
            
            session.commit()
            session.refresh(user)
            
            return {
                "name": user.name,
                "address": user.address,
                "radius": user.radius,
                "favorite_strains": user.favorite_strains,
                "terpene_profiles": user.terpene_profiles
            }
    
    def _compute_terpene_profiles(self, favorite_strains: List[str]) -> Dict[str, Any]:
        """Compute terpene profiles for user's favorite strains"""
        profiles = {}
        
        for strain_name in favorite_strains:
            strain_data = self.strain_db.get_strain(strain_name)
            if strain_data and "terpenes" in strain_data:
                # Get detailed terpene analysis
                terpenes = strain_data.get("terpenes", {})
                terpene_analysis = self._build_terpene_analysis(terpenes)
                
                profiles[strain_name] = {
                    "terpenes": terpenes,
                    "terpene_analysis": terpene_analysis,
                    "effects": strain_data.get("effects", []),
                    "type": strain_data.get("type", "unknown"),
                    "thc_range": strain_data.get("thc_range", "unknown"),
                    "description": strain_data.get("description", "No description available")
                }
        
        return profiles
    
    def _build_terpene_analysis(self, terpenes: Dict[str, float]) -> List[Dict[str, Any]]:
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
    
    def get_user_profile(self, user_id: int) -> Optional[Dict[str, Any]]:
        """Get user profile with terpene analysis"""
        with Session(engine) as session:
            user = session.query(User).filter(User.id == user_id).first()
            if not user:
                return None
            
            return {
                "id": user.id,
                "email": user.email,
                "name": user.name,
                "address": user.address,
                "radius": user.radius,
                "favorite_strains": user.favorite_strains or [],
                "terpene_profiles": user.terpene_profiles or {},
                "created_at": user.created_at,
                "updated_at": user.updated_at
            }
    
    def compare_strain_to_favorites(self, user_id: int, strain_name: str, strain_terpenes: Dict[str, float]) -> Dict[str, Any]:
        """Compare a strain to user's favorite strains and return similarity analysis"""
        with Session(engine) as session:
            user = session.query(User).filter(User.id == user_id).first()
            if not user or not user.terpene_profiles:
                return {"similarity_scores": [], "average_similarity": 0.0, "best_match": None}
            
            # Get all terpene vectors for comparison
            all_terpenes = set(strain_terpenes.keys())
            for profile in user.terpene_profiles.values():
                all_terpenes.update(profile.get("terpenes", {}).keys())
            
            all_terpenes = sorted(list(all_terpenes))
            
            # Create terpene vector for the query strain
            query_vector = np.array([strain_terpenes.get(terpene, 0.0) for terpene in all_terpenes]).reshape(1, -1)
            
            similarity_scores = []
            
            # Compare with each favorite strain
            for fav_strain, profile in user.terpene_profiles.items():
                fav_terpenes = profile.get("terpenes", {})
                fav_vector = np.array([fav_terpenes.get(terpene, 0.0) for terpene in all_terpenes]).reshape(1, -1)
                
                # Calculate cosine similarity
                similarity = cosine_similarity(query_vector, fav_vector)[0][0]
                
                similarity_scores.append({
                    "strain_name": fav_strain,
                    "similarity": float(similarity),
                    "similarity_percentage": float(similarity * 100),
                    "profile": profile
                })
            
            # Sort by similarity
            similarity_scores.sort(key=lambda x: x["similarity"], reverse=True)
            
            # Calculate average similarity
            average_similarity = sum(score["similarity"] for score in similarity_scores) / len(similarity_scores) if similarity_scores else 0.0
            
            return {
                "similarity_scores": similarity_scores,
                "average_similarity": float(average_similarity),
                "average_similarity_percentage": float(average_similarity * 100),
                "best_match": similarity_scores[0] if similarity_scores else None,
                "personal_rating": self._calculate_personal_rating(average_similarity)
            }
    
    def _calculate_personal_rating(self, similarity: float) -> str:
        """Calculate personal rating based on similarity score"""
        if similarity >= 0.8:
            return "Excellent Match"
        elif similarity >= 0.6:
            return "Good Match"
        elif similarity >= 0.4:
            return "Moderate Match"
        elif similarity >= 0.2:
            return "Poor Match"
        else:
            return "Very Different"

