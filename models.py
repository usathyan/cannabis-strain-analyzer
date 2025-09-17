"""
Pydantic models for API requests and responses
"""

from pydantic import BaseModel, Field
from typing import List, Dict, Any, Optional
from datetime import datetime

# User profile models
class UserProfileUpdate(BaseModel):
    name: str = Field(..., description="User's full name")
    address: str = Field(..., description="User's address for dispensary search")
    radius: int = Field(25, ge=1, le=100, description="Search radius in miles")
    favorite_strains: List[str] = Field(..., description="List of favorite strain names")

class UserProfileResponse(BaseModel):
    id: int
    email: str
    name: Optional[str] = None
    address: Optional[str] = None
    radius: int = 25
    favorite_strains: List[str] = []
    terpene_profiles: Dict[str, Any] = {}
    created_at: datetime
    updated_at: datetime

# Strain recommendation models
class StrainRecommendationRequest(BaseModel):
    strain_name: str = Field(..., description="Name of the cannabis strain to analyze")

class TerpeneAnalysis(BaseModel):
    name: str
    level: str
    description: str
    effects: List[str]
    aroma: str

class StrainAnalysis(BaseModel):
    name: str
    type: str
    thc_range: str
    cbd_range: str
    description: str
    effects: List[str]
    flavors: List[str]
    terpenes: List[TerpeneAnalysis]

class SimilarityScore(BaseModel):
    strain_name: str
    similarity: float
    similarity_percentage: float
    profile: Dict[str, Any]

class PersonalRating(BaseModel):
    similarity_scores: List[SimilarityScore]
    average_similarity: float
    average_similarity_percentage: float
    best_match: Optional[SimilarityScore] = None
    personal_rating: str

class DispensaryInfo(BaseModel):
    name: str
    address: str
    distance_miles: float
    rating: float
    phone: Optional[str] = None
    opening_hours: Optional[Dict[str, Any]] = None
    website: Optional[str] = None

class StrainRecommendation(BaseModel):
    strain_name: str
    similarity: float
    strain_data: Optional[Dict[str, Any]] = None

class PersonalizedRecommendationResponse(BaseModel):
    strain_analysis: StrainAnalysis
    personal_rating: PersonalRating
    recommendations: List[StrainRecommendation]
    dispensaries: List[DispensaryInfo]
    user_location: Optional[str] = None

