package com.strainanalyzer.app.data

import com.google.gson.annotations.SerializedName

data class UserProfile(
    val id: String,
    val name: String,
    val address: String,
    val radius: Int,
    @SerializedName("favorite_strains")
    val favoriteStrains: List<String> = emptyList(),
    @SerializedName("ranked_favorites")
    val rankedFavorites: List<RankedFavorite> = emptyList(),
    @SerializedName("ideal_profile")
    val idealProfile: IdealProfile? = null
)

data class RankedFavorite(
    val name: String,
    val terpenes: Map<String, Double>,
    val cannabinoids: Map<String, Double>,
    val effects: List<String>,
    val flavors: List<String>,
    val type: String,
    @SerializedName("thc_range")
    val thcRange: String,
    @SerializedName("cbd_range")
    val cbdRange: String
)

data class IdealProfile(
    @SerializedName("aggregate_terpenes")
    val aggregateTerpenes: Map<String, Double>,
    @SerializedName("aggregate_cannabinoids")
    val aggregateCannabinoids: Map<String, Double>,
    @SerializedName("terpene_analysis")
    val terpeneAnalysis: List<TerpeneAnalysis>,
    @SerializedName("top_effects")
    val topEffects: List<String>,
    @SerializedName("top_medical_effects")
    val topMedicalEffects: List<String>?,
    @SerializedName("top_flavors")
    val topFlavors: List<String>?,
    @SerializedName("top_aromas")
    val topAromas: List<String>?,
    @SerializedName("strain_count")
    val strainCount: Int,
    @SerializedName("dominant_terpenes")
    val dominantTerpenes: List<String>,
    @SerializedName("dominant_cannabinoids")
    val dominantCannabinoids: List<String>
)

data class TerpeneAnalysis(
    val name: String,
    val level: String,
    val description: String,
    val effects: List<String>,
    val aroma: String
)

data class StrainData(
    val name: String,
    val type: String,
    @SerializedName("thc_range")
    val thcRange: String,
    @SerializedName("cbd_range")
    val cbdRange: String,
    val description: String,
    val effects: List<String>,
    val flavors: List<String>,
    val terpenes: Map<String, Double>,
    val cannabinoids: Map<String, Double>
)

data class ComparisonResult(
    @SerializedName("strain_analysis")
    val strainAnalysis: StrainAnalysis,
    @SerializedName("ideal_profile")
    val idealProfile: IdealProfile?,
    @SerializedName("ranked_favorites")
    val rankedFavorites: List<RankedFavorite>?,
    val comparison: Comparison,
    @SerializedName("comparison_type")
    val comparisonType: String,
    @SerializedName("llm_analysis")
    val llmAnalysis: String
)

data class StrainAnalysis(
    val name: String,
    val type: String,
    @SerializedName("thc_range")
    val thcRange: String,
    @SerializedName("cbd_range")
    val cbdRange: String,
    val description: String,
    val effects: List<String>,
    val flavors: List<String>,
    val terpenes: List<TerpeneAnalysis>
)

data class Comparison(
    @SerializedName("overall_similarity")
    val overallSimilarity: Double,
    @SerializedName("similarity_percentage")
    val similarityPercentage: Double,
    @SerializedName("match_rating")
    val matchRating: String,
    @SerializedName("individual_similarities")
    val individualSimilarities: List<IndividualSimilarity>? = null,
    val method: String,
    @SerializedName("profiles_used")
    val profilesUsed: Int? = null
)

data class IndividualSimilarity(
    @SerializedName("strain_name")
    val strainName: String,
    val similarity: Double,
    @SerializedName("similarity_percentage")
    val similarityPercentage: Double,
    val rank: Int
)

data class AvailableStrainsResponse(
    val strains: List<String>
)

data class CreateProfileRequest(
    val strains: List<String>
)

data class CreateProfileResponse(
    @SerializedName("ideal_profile")
    val idealProfile: IdealProfile,
    @SerializedName("selected_strains")
    val selectedStrains: List<String>,
    val message: String
)

data class CompareStrainRequest(
    @SerializedName("strain_name")
    val strainName: String,
    @SerializedName("use_zscore")
    val useZScore: Boolean = false
)

data class AddStrainRequest(
    @SerializedName("strain_name")
    val strainName: String
)

data class RemoveStrainRequest(
    @SerializedName("strain_name")
    val strainName: String
)

data class StrainOperationResponse(
    val success: Boolean,
    val message: String,
    @SerializedName("ideal_profile")
    val idealProfile: IdealProfile?,
    @SerializedName("favorite_strains")
    val favoriteStrains: List<String>
)
