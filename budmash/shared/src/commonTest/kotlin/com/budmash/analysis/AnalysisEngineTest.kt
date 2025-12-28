package com.budmash.analysis

import com.budmash.data.StrainData
import com.budmash.data.StrainType
import kotlin.test.Test
import kotlin.test.assertTrue

class AnalysisEngineTest {
    private val engine = AnalysisEngine()

    @Test
    fun cosineSimilarity_identicalVectors_returns1() {
        val v1 = listOf(0.5, 0.3, 0.2)
        val v2 = listOf(0.5, 0.3, 0.2)
        val result = engine.cosineSimilarity(v1, v2)
        assertTrue(result > 0.99)
    }

    @Test
    fun calculateMatch_similarStrains_highScore() {
        val userProfile = listOf(0.8, 0.6, 0.5, 0.4, 0.3, 0.2, 0.1, 0.1, 0.1, 0.1, 0.1)
        val strain = StrainData(
            name = "Test Strain",
            type = StrainType.HYBRID,
            myrcene = 0.75,
            limonene = 0.55,
            caryophyllene = 0.45,
            pinene = 0.35,
            linalool = 0.25
        )
        val result = engine.calculateMatch(userProfile, strain)
        assertTrue(result.overallScore > 0.7)
    }
}
