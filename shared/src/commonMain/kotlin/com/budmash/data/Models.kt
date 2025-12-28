package com.budmash.data

import kotlinx.serialization.Serializable

@Serializable
enum class StrainType {
    INDICA, SATIVA, HYBRID, UNKNOWN
}

@Serializable
data class StrainData(
    val name: String,
    val type: StrainType = StrainType.UNKNOWN,
    val description: String = "",
    val effects: List<String> = emptyList(),
    val flavors: List<String> = emptyList(),
    val thcMin: Double? = null,
    val thcMax: Double? = null,
    val cbdMin: Double? = null,
    val cbdMax: Double? = null,
    val price: Double? = null,
    // Terpenes (normalized 0-1)
    val myrcene: Double = 0.0,
    val limonene: Double = 0.0,
    val caryophyllene: Double = 0.0,
    val pinene: Double = 0.0,
    val linalool: Double = 0.0,
    val humulene: Double = 0.0,
    val terpinolene: Double = 0.0,
    val ocimene: Double = 0.0,
    val nerolidol: Double = 0.0,
    val bisabolol: Double = 0.0,
    val eucalyptol: Double = 0.0
) {
    fun terpeneProfile(): List<Double> = listOf(
        myrcene, limonene, caryophyllene, pinene, linalool,
        humulene, terpinolene, ocimene, nerolidol, bisabolol, eucalyptol
    )

    companion object {
        val TERPENE_NAMES = listOf(
            "Myrcene", "Limonene", "Caryophyllene", "Pinene", "Linalool",
            "Humulene", "Terpinolene", "Ocimene", "Nerolidol", "Bisabolol", "Eucalyptol"
        )
    }
}

@Serializable
data class UserProfile(
    val favoriteStrains: List<String> = emptyList(),
    val likedStrains: List<String> = emptyList(),
    val dislikedStrains: List<String> = emptyList(),
    val idealProfile: List<Double> = List(11) { 0.0 }
)

@Serializable
data class SimilarityResult(
    val strain: StrainData,
    val overallScore: Double,
    val cosineScore: Double,
    val euclideanScore: Double,
    val pearsonScore: Double
)

@Serializable
data class DispensaryMenu(
    val url: String,
    val name: String = "",
    val fetchedAt: Long = 0,
    val strains: List<StrainData> = emptyList()
)
