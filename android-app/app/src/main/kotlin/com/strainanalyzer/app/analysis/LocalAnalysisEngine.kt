package com.strainanalyzer.app.analysis

import android.content.Context
import org.json.JSONObject
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Local Analysis Engine - performs all strain analysis on-device
 * No LLM required for core functionality
 */
class LocalAnalysisEngine(private val context: Context) {

    // Mutable database that includes both embedded and user-added strains
    private val strainDatabase: MutableMap<String, StrainData> = loadStrainDatabase(context).toMutableMap()

    init {
        // Load any previously saved custom strains
        loadCustomStrains()
    }

    data class StrainData(
        val name: String,
        val terpenes: Map<String, Double>,
        val effects: List<String>,
        val medicalEffects: List<String>,
        val type: String,
        val thcRange: String,
        val cbdRange: String,
        val description: String,
        val flavors: List<String>
    )

    data class AnalysisResult(
        val strain: StrainData?,
        val similarity: SimilarityResult,
        val recommendation: String,
        val isLocalAnalysis: Boolean = true,
        val needsLlmFor: List<String> = emptyList()
    )

    data class SimilarityResult(
        val overallScore: Double,
        val zScoredCosine: Double,
        val euclideanSimilarity: Double,
        val correlationSimilarity: Double,
        val matchRating: String,
        val terpeneComparison: Map<String, TerpeneComparison>
    )

    data class TerpeneComparison(
        val strainValue: Double,
        val profileValue: Double,
        val difference: Double,
        val percentDiff: Double
    )

    // Major terpenes used in analysis
    private val majorTerpenes = listOf(
        "myrcene", "limonene", "caryophyllene", "pinene", "linalool",
        "humulene", "terpinolene", "ocimene", "nerolidol", "bisabolol", "eucalyptol"
    )

    fun getAvailableStrains(): List<String> = strainDatabase.keys.toList().sorted()

    fun getStrain(name: String): StrainData? {
        return strainDatabase[name.lowercase()]
    }

    /**
     * Get the combined ideal terpene profile from favorite strains
     * Uses MAX pooling across all favorites
     */
    fun getIdealProfile(favoriteNames: List<String>): Map<String, Double> {
        val favorites = favoriteNames.mapNotNull { getStrain(it) }
        if (favorites.isEmpty()) return emptyMap()
        return createIdealProfile(favorites)
    }

    /**
     * Get list of major terpenes used in analysis
     */
    fun getMajorTerpenes(): List<String> = majorTerpenes

    /**
     * Get individual strain terpene profiles for favorites
     */
    fun getFavoriteProfiles(favoriteNames: List<String>): Map<String, Map<String, Double>> {
        return favoriteNames.mapNotNull { name ->
            getStrain(name)?.let { strain ->
                strain.name to strain.terpenes.filterKeys { it in majorTerpenes }
            }
        }.toMap()
    }

    fun isStrainKnown(name: String): Boolean {
        return strainDatabase.containsKey(name.lowercase())
    }

    /**
     * Analyze a strain against user's ideal profile
     * Returns complete analysis with similarity scores
     */
    fun analyzeStrain(
        strainName: String,
        userFavorites: List<String>
    ): AnalysisResult {
        val strain = getStrain(strainName)
        val needsLlm = mutableListOf<String>()

        // If strain not in database, we need LLM to generate data
        if (strain == null) {
            needsLlm.add("Unknown strain - LLM needed to generate profile")
            return AnalysisResult(
                strain = null,
                similarity = SimilarityResult(
                    overallScore = 0.0,
                    zScoredCosine = 0.0,
                    euclideanSimilarity = 0.0,
                    correlationSimilarity = 0.0,
                    matchRating = "Unknown",
                    terpeneComparison = emptyMap()
                ),
                recommendation = "Strain '$strainName' not found in database. Enable LLM to generate strain data.",
                isLocalAnalysis = true,
                needsLlmFor = needsLlm
            )
        }

        // Get favorite strains' profiles
        val favoriteStrains = userFavorites.mapNotNull { getStrain(it) }

        if (favoriteStrains.isEmpty()) {
            return AnalysisResult(
                strain = strain,
                similarity = SimilarityResult(
                    overallScore = 50.0,
                    zScoredCosine = 0.5,
                    euclideanSimilarity = 0.5,
                    correlationSimilarity = 0.5,
                    matchRating = "No Profile",
                    terpeneComparison = emptyMap()
                ),
                recommendation = "Add favorite strains in Configure tab to enable personalized matching.",
                isLocalAnalysis = true,
                needsLlmFor = emptyList()
            )
        }

        // Create ideal profile from favorites (using max values)
        val idealProfile = createIdealProfile(favoriteStrains)

        // Calculate similarity
        val similarity = calculateSimilarity(strain, idealProfile)

        // Generate local recommendation (no LLM needed)
        val recommendation = generateLocalRecommendation(strain, similarity, favoriteStrains)

        return AnalysisResult(
            strain = strain,
            similarity = similarity,
            recommendation = recommendation,
            isLocalAnalysis = true,
            needsLlmFor = emptyList()
        )
    }

    /**
     * Create ideal profile from favorite strains using MAX values
     */
    private fun createIdealProfile(favorites: List<StrainData>): Map<String, Double> {
        val profile = mutableMapOf<String, Double>()

        for (terpene in majorTerpenes) {
            val maxValue = favorites.maxOfOrNull { it.terpenes[terpene] ?: 0.0 } ?: 0.0
            profile[terpene] = maxValue
        }

        return profile
    }

    /**
     * Calculate comprehensive similarity using Z-scored metrics
     */
    private fun calculateSimilarity(
        strain: StrainData,
        idealProfile: Map<String, Double>
    ): SimilarityResult {
        val strainVector = majorTerpenes.map { strain.terpenes[it] ?: 0.0 }
        val profileVector = majorTerpenes.map { idealProfile[it] ?: 0.0 }

        // Z-score normalization
        val strainZScored = zScore(strainVector)
        val profileZScored = zScore(profileVector)

        // Calculate metrics
        val zScoredCosine = cosineSimilarity(strainZScored, profileZScored)
        val euclidean = euclideanSimilarity(strainZScored, profileZScored)
        val correlation = correlationSimilarity(strainVector, profileVector)

        // Combined score (weighted average)
        val overall = (zScoredCosine * 0.5 + euclidean * 0.25 + correlation * 0.25) * 100

        // Terpene comparison
        val terpeneComparison = majorTerpenes.associateWith { terpene ->
            val strainVal = strain.terpenes[terpene] ?: 0.0
            val profileVal = idealProfile[terpene] ?: 0.0
            val diff = strainVal - profileVal
            val percentDiff = if (profileVal > 0) (diff / profileVal) * 100 else 0.0
            TerpeneComparison(strainVal, profileVal, diff, percentDiff)
        }

        return SimilarityResult(
            overallScore = overall.coerceIn(0.0, 100.0),
            zScoredCosine = zScoredCosine,
            euclideanSimilarity = euclidean,
            correlationSimilarity = correlation,
            matchRating = getMatchRating(overall),
            terpeneComparison = terpeneComparison
        )
    }

    private fun zScore(vector: List<Double>): List<Double> {
        val mean = vector.average()
        val std = sqrt(vector.map { (it - mean).pow(2) }.average())
        return if (std > 0) vector.map { (it - mean) / std } else vector.map { 0.0 }
    }

    private fun cosineSimilarity(a: List<Double>, b: List<Double>): Double {
        val dotProduct = a.zip(b).sumOf { it.first * it.second }
        val magnitudeA = sqrt(a.sumOf { it.pow(2) })
        val magnitudeB = sqrt(b.sumOf { it.pow(2) })
        return if (magnitudeA > 0 && magnitudeB > 0) {
            (dotProduct / (magnitudeA * magnitudeB) + 1) / 2 // Normalize to 0-1
        } else 0.5
    }

    private fun euclideanSimilarity(a: List<Double>, b: List<Double>): Double {
        val distance = sqrt(a.zip(b).sumOf { (it.first - it.second).pow(2) })
        return 1 / (1 + distance) // Convert distance to similarity
    }

    private fun correlationSimilarity(a: List<Double>, b: List<Double>): Double {
        val meanA = a.average()
        val meanB = b.average()
        val numerator = a.zip(b).sumOf { (it.first - meanA) * (it.second - meanB) }
        val denomA = sqrt(a.sumOf { (it - meanA).pow(2) })
        val denomB = sqrt(b.sumOf { (it - meanB).pow(2) })
        return if (denomA > 0 && denomB > 0) {
            (numerator / (denomA * denomB) + 1) / 2 // Normalize to 0-1
        } else 0.5
    }

    private fun getMatchRating(score: Double): String {
        return when {
            score >= 90 -> "Perfect Match"
            score >= 75 -> "Excellent Match"
            score >= 60 -> "Good Match"
            score >= 45 -> "Moderate Match"
            score >= 30 -> "Fair Match"
            else -> "Different Profile"
        }
    }

    /**
     * Generate recommendation without LLM - uses templates
     */
    private fun generateLocalRecommendation(
        strain: StrainData,
        similarity: SimilarityResult,
        favorites: List<StrainData>
    ): String {
        val score = similarity.overallScore.toInt()
        val topTerpene = strain.terpenes.maxByOrNull { it.value }?.key ?: "unknown"
        val favoriteNames = favorites.take(2).joinToString(" and ") { it.name }

        val matchDescription = when {
            score >= 85 -> "an excellent match for your preferences"
            score >= 70 -> "a good match with your favorite strains"
            score >= 50 -> "moderately similar to your profile"
            score >= 30 -> "somewhat different from your usual preferences"
            else -> "quite different from your preferred strains"
        }

        val effectsMatch = favorites.flatMap { it.effects }.toSet()
            .intersect(strain.effects.toSet())

        val sharedEffects = if (effectsMatch.isNotEmpty()) {
            "Like $favoriteNames, expect ${effectsMatch.take(2).joinToString(" and ")} effects."
        } else {
            "This strain offers different effects than your favorites."
        }

        val terpeneNote = "Dominant terpene is $topTerpene (${((strain.terpenes[topTerpene] ?: 0.0) * 100).toInt()}%)."

        return """
            ${strain.name.replaceFirstChar { it.uppercase() }} is $matchDescription with a ${score}% similarity score.

            $sharedEffects

            $terpeneNote

            Type: ${strain.type.replaceFirstChar { it.uppercase() }} | THC: ${strain.thcRange} | CBD: ${strain.cbdRange}
        """.trimIndent()
    }

    private fun loadStrainDatabase(context: Context): Map<String, StrainData> {
        return try {
            val json = context.assets.open("strains.json").bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(json)
            val strains = mutableMapOf<String, StrainData>()

            jsonObject.keys().forEach { key ->
                val obj = jsonObject.getJSONObject(key)
                strains[key] = StrainData(
                    name = key,
                    terpenes = parseTerpenes(obj.optJSONObject("terpenes")),
                    effects = parseStringArray(obj.optJSONArray("effects")),
                    medicalEffects = parseStringArray(obj.optJSONArray("medical_effects")),
                    type = obj.optString("type", "hybrid"),
                    thcRange = obj.optString("thc_range", "Unknown"),
                    cbdRange = obj.optString("cbd_range", "Unknown"),
                    description = obj.optString("description", ""),
                    flavors = parseStringArray(obj.optJSONArray("flavors"))
                )
            }
            strains
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun parseTerpenes(obj: JSONObject?): Map<String, Double> {
        if (obj == null) return emptyMap()
        val map = mutableMapOf<String, Double>()
        obj.keys().forEach { key ->
            map[key] = obj.optDouble(key, 0.0)
        }
        return map
    }

    private fun parseStringArray(arr: org.json.JSONArray?): List<String> {
        if (arr == null) return emptyList()
        return (0 until arr.length()).map { arr.getString(it) }
    }

    /**
     * Add a new strain to the database and persist it
     */
    fun addStrain(strain: StrainData) {
        val normalizedName = strain.name.lowercase()
        strainDatabase[normalizedName] = strain
        saveCustomStrains()
    }

    /**
     * Check if strain exists (in embedded or custom database)
     */
    fun hasStrain(name: String): Boolean {
        return strainDatabase.containsKey(name.lowercase())
    }

    /**
     * Get count of custom (user-added) strains
     */
    fun getCustomStrainCount(): Int {
        val prefs = context.getSharedPreferences("custom_strains", Context.MODE_PRIVATE)
        val json = prefs.getString("strains", null) ?: return 0
        return try {
            JSONObject(json).length()
        } catch (e: Exception) {
            0
        }
    }

    private fun loadCustomStrains() {
        try {
            val prefs = context.getSharedPreferences("custom_strains", Context.MODE_PRIVATE)
            val json = prefs.getString("strains", null) ?: return
            val jsonObject = JSONObject(json)

            jsonObject.keys().forEach { key ->
                val obj = jsonObject.getJSONObject(key)
                strainDatabase[key] = StrainData(
                    name = key,
                    terpenes = parseTerpenes(obj.optJSONObject("terpenes")),
                    effects = parseStringArray(obj.optJSONArray("effects")),
                    medicalEffects = parseStringArray(obj.optJSONArray("medical_effects")),
                    type = obj.optString("type", "hybrid"),
                    thcRange = obj.optString("thc_range", "Unknown"),
                    cbdRange = obj.optString("cbd_range", "Unknown"),
                    description = obj.optString("description", ""),
                    flavors = parseStringArray(obj.optJSONArray("flavors"))
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveCustomStrains() {
        try {
            // Get the embedded strain names to exclude them
            val embeddedNames = loadEmbeddedStrainNames()

            // Build JSON of custom strains only
            val customStrains = JSONObject()
            strainDatabase.forEach { (name, strain) ->
                if (name !in embeddedNames) {
                    customStrains.put(name, JSONObject().apply {
                        put("type", strain.type)
                        put("thc_range", strain.thcRange)
                        put("cbd_range", strain.cbdRange)
                        put("description", strain.description)
                        put("effects", org.json.JSONArray(strain.effects))
                        put("medical_effects", org.json.JSONArray(strain.medicalEffects))
                        put("flavors", org.json.JSONArray(strain.flavors))
                        put("terpenes", JSONObject(strain.terpenes))
                    })
                }
            }

            val prefs = context.getSharedPreferences("custom_strains", Context.MODE_PRIVATE)
            prefs.edit().putString("strains", customStrains.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadEmbeddedStrainNames(): Set<String> {
        return try {
            val json = context.assets.open("strains.json").bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(json)
            jsonObject.keys().asSequence().toSet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    companion object {
        @Volatile
        private var instance: LocalAnalysisEngine? = null

        fun getInstance(context: Context): LocalAnalysisEngine {
            return instance ?: synchronized(this) {
                instance ?: LocalAnalysisEngine(context.applicationContext).also { instance = it }
            }
        }
    }
}
