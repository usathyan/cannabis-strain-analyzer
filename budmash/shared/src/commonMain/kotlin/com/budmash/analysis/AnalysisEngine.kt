package com.budmash.analysis

import com.budmash.data.SimilarityResult
import com.budmash.data.StrainData
import kotlin.math.pow
import kotlin.math.sqrt

class AnalysisEngine {

    companion object {
        private const val COSINE_WEIGHT = 0.50
        private const val EUCLIDEAN_WEIGHT = 0.25
        private const val PEARSON_WEIGHT = 0.25
    }

    fun calculateMatch(userProfile: List<Double>, strain: StrainData): SimilarityResult {
        val strainProfile = strain.terpeneProfile()

        val userZ = zScore(userProfile)
        val strainZ = zScore(strainProfile)

        val cosine = cosineSimilarity(userZ, strainZ)
        val euclidean = euclideanSimilarity(userZ, strainZ)
        val pearson = pearsonCorrelation(userZ, strainZ)

        val overall = (cosine * COSINE_WEIGHT) +
                      (euclidean * EUCLIDEAN_WEIGHT) +
                      (pearson * PEARSON_WEIGHT)

        return SimilarityResult(
            strain = strain,
            overallScore = overall.coerceIn(0.0, 1.0),
            cosineScore = cosine.coerceIn(0.0, 1.0),
            euclideanScore = euclidean.coerceIn(0.0, 1.0),
            pearsonScore = pearson.coerceIn(0.0, 1.0)
        )
    }

    fun buildIdealProfile(strains: List<StrainData>): List<Double> {
        if (strains.isEmpty()) return List(11) { 0.0 }

        // MAX pooling across all strains
        return (0 until 11).map { i ->
            strains.maxOf { it.terpeneProfile()[i] }
        }
    }

    fun zScore(vector: List<Double>): List<Double> {
        val mean = vector.average()
        val variance = vector.map { (it - mean).pow(2) }.average()
        val std = sqrt(variance)

        return if (std < 0.0001) {
            vector.map { 0.0 }
        } else {
            vector.map { (it - mean) / std }
        }
    }

    fun cosineSimilarity(v1: List<Double>, v2: List<Double>): Double {
        val dotProduct = v1.zip(v2).sumOf { it.first * it.second }
        val mag1 = sqrt(v1.sumOf { it.pow(2) })
        val mag2 = sqrt(v2.sumOf { it.pow(2) })

        return if (mag1 < 0.0001 || mag2 < 0.0001) {
            0.0
        } else {
            ((dotProduct / (mag1 * mag2)) + 1) / 2 // Normalize to 0-1
        }
    }

    fun euclideanSimilarity(v1: List<Double>, v2: List<Double>): Double {
        val distance = sqrt(v1.zip(v2).sumOf { (it.first - it.second).pow(2) })
        val maxDistance = sqrt(v1.size.toDouble() * 4) // Max possible for z-scores
        return 1 - (distance / maxDistance).coerceIn(0.0, 1.0)
    }

    fun pearsonCorrelation(v1: List<Double>, v2: List<Double>): Double {
        val mean1 = v1.average()
        val mean2 = v2.average()

        val numerator = v1.zip(v2).sumOf { (it.first - mean1) * (it.second - mean2) }
        val denom1 = sqrt(v1.sumOf { (it - mean1).pow(2) })
        val denom2 = sqrt(v2.sumOf { (it - mean2).pow(2) })

        return if (denom1 < 0.0001 || denom2 < 0.0001) {
            0.0
        } else {
            ((numerator / (denom1 * denom2)) + 1) / 2 // Normalize to 0-1
        }
    }
}
