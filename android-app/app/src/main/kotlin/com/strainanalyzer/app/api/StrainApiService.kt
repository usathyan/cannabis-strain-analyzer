package com.strainanalyzer.app.api

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Service for fetching strain data from multiple APIs with fallback support:
 * 1. Primary: Cannlytics API (https://cannlytics.com/api/data/strains)
 * 2. Backup: Embedded fallback strain data
 *
 * Note: LLM fallback is handled separately in StrainDataService
 */
class StrainApiService {

    companion object {
        private const val TAG = "StrainApiService"
        private const val CANNLYTICS_BASE_URL = "https://cannlytics.com/api/data/strains"
        private const val TIMEOUT_MS = 10000

        @Volatile
        private var instance: StrainApiService? = null

        fun getInstance(): StrainApiService {
            return instance ?: synchronized(this) {
                instance ?: StrainApiService().also { instance = it }
            }
        }
    }

    enum class ApiSource {
        CANNLYTICS,
        FALLBACK_CONFIG
    }

    data class ApiResult(
        val data: ApiStrainData?,
        val source: ApiSource
    )

    data class ApiStrainData(
        val name: String,
        val type: String,
        val description: String,
        val thcRange: String,
        val cbdRange: String,
        val effects: List<String>,
        val aromas: List<String>,
        val terpenes: Map<String, Double>,
        val imageUrl: String?
    )

    /**
     * Search for a strain by name with fallback support
     * Data source priority:
     * 1. Cannlytics API (primary)
     * 2. Embedded fallback strains (backup)
     * Returns the best matching strain or null if not found in any source
     */
    suspend fun searchStrain(strainName: String): ApiStrainData? = withContext(Dispatchers.IO) {
        val result = searchStrainWithSource(strainName)
        result.data
    }

    /**
     * Search for a strain and return both the data and its source
     */
    suspend fun searchStrainWithSource(strainName: String): ApiResult = withContext(Dispatchers.IO) {
        // 1. Try Cannlytics API (primary)
        try {
            val cannlyticsResult = fetchFromCannlytics(strainName)
            if (cannlyticsResult != null) {
                Log.d(TAG, "Found in Cannlytics API: ${cannlyticsResult.name}")
                return@withContext ApiResult(cannlyticsResult, ApiSource.CANNLYTICS)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Cannlytics API failed for: $strainName", e)
        }

        // 2. Try fallback config data (backup)
        val fallbackResult = getFallbackStrain(strainName)
        if (fallbackResult != null) {
            Log.d(TAG, "Found in fallback config: ${fallbackResult.name}")
            return@withContext ApiResult(fallbackResult, ApiSource.FALLBACK_CONFIG)
        }

        Log.d(TAG, "No match found in any source for: $strainName")
        ApiResult(null, ApiSource.CANNLYTICS)
    }

    /**
     * Fetch strain from Cannlytics API
     */
    private fun fetchFromCannlytics(strainName: String): ApiStrainData? {
        // First try exact match
        val exactMatch = fetchStrainByName(strainName)
        if (exactMatch != null) {
            return exactMatch
        }

        // Try search with partial matching
        val searchResults = searchStrains(strainName, limit = 10)
        if (searchResults.isNotEmpty()) {
            val normalizedQuery = strainName.lowercase().trim()
            val bestMatch = searchResults.minByOrNull { strain ->
                levenshteinDistance(strain.name.lowercase(), normalizedQuery)
            }

            if (bestMatch != null) {
                val distance = levenshteinDistance(bestMatch.name.lowercase(), normalizedQuery)
                val maxAllowedDistance = (normalizedQuery.length * 0.4).toInt().coerceAtLeast(3)

                if (distance <= maxAllowedDistance) {
                    Log.d(TAG, "Cannlytics fuzzy match: ${bestMatch.name} (distance: $distance)")
                    return bestMatch
                }
            }
        }

        return null
    }

    /**
     * Fetch strain data by exact name from Cannlytics API
     */
    private fun fetchStrainByName(strainName: String): ApiStrainData? {
        return try {
            // Format name for URL (replace spaces with hyphens)
            val urlName = strainName.trim()
                .replace(" ", "-")
                .lowercase()

            val url = URL("$CANNLYTICS_BASE_URL/$urlName")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.setRequestProperty("Accept", "application/json")

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)

                if (json.optBoolean("success", false)) {
                    val data = json.optJSONObject("data")
                    if (data != null && data.length() > 0) {
                        return parseStrainData(data)
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching strain by name: $strainName", e)
            null
        }
    }

    /**
     * Search strains from Cannlytics API
     */
    private fun searchStrains(query: String, limit: Int = 20): List<ApiStrainData> {
        return try {
            val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
            val url = URL("$CANNLYTICS_BASE_URL?limit=$limit")

            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.setRequestProperty("Accept", "application/json")

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)

                if (json.optBoolean("success", false)) {
                    val dataArray = json.optJSONArray("data") ?: return emptyList()
                    val results = mutableListOf<ApiStrainData>()

                    val normalizedQuery = query.lowercase().trim()
                    for (i in 0 until dataArray.length()) {
                        val strainJson = dataArray.optJSONObject(i) ?: continue
                        val strainName = strainJson.optString("strain_name", "").lowercase()

                        // Only include strains that contain the search term
                        if (strainName.contains(normalizedQuery) ||
                            normalizedQuery.split(" ").any { strainName.contains(it) }) {
                            parseStrainData(strainJson)?.let { results.add(it) }
                        }
                    }

                    // If no matches found in first batch, do a broader search
                    if (results.isEmpty()) {
                        for (i in 0 until dataArray.length()) {
                            val strainJson = dataArray.optJSONObject(i) ?: continue
                            parseStrainData(strainJson)?.let { results.add(it) }
                        }
                    }

                    return results
                }
            }
            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error searching strains", e)
            emptyList()
        }
    }

    /**
     * Parse JSON object into ApiStrainData
     */
    private fun parseStrainData(json: JSONObject): ApiStrainData? {
        return try {
            val name = json.optString("strain_name", json.optString("id", ""))
            if (name.isBlank()) return null

            // Extract terpenes
            val terpenes = mutableMapOf<String, Double>()
            TERPENE_MAPPINGS.forEach { (apiKey, ourKey) ->
                val value = json.optDouble(apiKey, 0.0)
                if (value > 0 && !value.isNaN()) {
                    // API returns percentage, normalize to 0-1 scale
                    terpenes[ourKey] = (value / 100.0).coerceIn(0.0, 1.0)
                }
            }

            // If terpene values seem to already be in 0-1 scale (total < 10), don't divide
            val totalTerpenes = json.optDouble("total_terpenes", 0.0)
            if (totalTerpenes < 10) {
                terpenes.clear()
                TERPENE_MAPPINGS.forEach { (apiKey, ourKey) ->
                    val value = json.optDouble(apiKey, 0.0)
                    if (value > 0 && !value.isNaN()) {
                        terpenes[ourKey] = value.coerceIn(0.0, 1.0)
                    }
                }
            }

            // Extract effects
            val effects = mutableListOf<String>()
            json.optJSONArray("potential_effects")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val effect = arr.optString(i, "")
                        .removePrefix("effect_")
                        .replaceFirstChar { it.uppercase() }
                    if (effect.isNotBlank()) effects.add(effect)
                }
            }

            // Extract aromas as flavors
            val aromas = mutableListOf<String>()
            json.optJSONArray("potential_aromas")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val aroma = arr.optString(i, "")
                        .removePrefix("aroma_")
                        .replaceFirstChar { it.uppercase() }
                    if (aroma.isNotBlank()) aromas.add(aroma)
                }
            }

            // Calculate THC/CBD ranges
            val thc = json.optDouble("delta_9_thc", 0.0)
            val totalThc = json.optDouble("total_thc", thc)
            val thcValue = if (totalThc > 0) totalThc else thc

            val cbd = json.optDouble("cbd", 0.0)
            val totalCbd = json.optDouble("total_cbd", cbd)
            val cbdValue = if (totalCbd > 0) totalCbd else cbd

            val thcRange = if (thcValue > 0) "${thcValue.toInt()}-${(thcValue + 3).toInt()}%" else "Unknown"
            val cbdRange = if (cbdValue > 0.1) "${String.format("%.1f", cbdValue)}-${String.format("%.1f", cbdValue + 0.5)}%" else "<1%"

            // Determine type based on effects/description
            val description = json.optString("description", "")
            val type = inferStrainType(description, effects, name)

            ApiStrainData(
                name = name,
                type = type,
                description = description,
                thcRange = thcRange,
                cbdRange = cbdRange,
                effects = effects.ifEmpty { listOf("Relaxed", "Happy") },
                aromas = aromas.ifEmpty { listOf("Earthy") },
                terpenes = terpenes,
                imageUrl = json.optString("image_url", "").ifBlank { null }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing strain data", e)
            null
        }
    }

    /**
     * Infer strain type from description and effects
     */
    private fun inferStrainType(description: String, effects: List<String>, name: String): String {
        val descLower = description.lowercase()
        val nameLower = name.lowercase()

        return when {
            descLower.contains("indica") && !descLower.contains("sativa") -> "indica"
            descLower.contains("sativa") && !descLower.contains("indica") -> "sativa"
            descLower.contains("hybrid") -> "hybrid"
            effects.any { it.lowercase() in listOf("sleepy", "relaxed", "sedated") } -> "indica"
            effects.any { it.lowercase() in listOf("energetic", "uplifted", "creative", "focused") } -> "sativa"
            else -> "hybrid"
        }
    }

    /**
     * Calculate Levenshtein distance for fuzzy matching
     */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val len1 = s1.length
        val len2 = s2.length
        val dp = Array(len1 + 1) { IntArray(len2 + 1) }

        for (i in 0..len1) dp[i][0] = i
        for (j in 0..len2) dp[0][j] = j

        for (i in 1..len1) {
            for (j in 1..len2) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[len1][len2]
    }

    // Mapping from Cannlytics API field names to our terpene names
    private val TERPENE_MAPPINGS = mapOf(
        "beta_myrcene" to "myrcene",
        "d_limonene" to "limonene",
        "beta_caryophyllene" to "caryophyllene",
        "alpha_pinene" to "pinene",
        "linalool" to "linalool",
        "humulene" to "humulene",
        "terpinolene" to "terpinolene",
        "ocimene" to "ocimene",
        "nerolidol" to "nerolidol",
        "alpha_bisabolol" to "bisabolol",
        "eucalyptol" to "eucalyptol",
        "beta_pinene" to "pinene", // Combine with alpha_pinene
        "geraniol" to "geraniol",
        "camphene" to "camphene",
        "carene" to "carene"
    )

    /**
     * Get strain from embedded fallback data
     * These are curated strains with accurate terpene profiles for when API is unavailable
     */
    private fun getFallbackStrain(strainName: String): ApiStrainData? {
        val normalizedName = strainName.lowercase().trim()
        val fallback = FALLBACK_STRAINS[normalizedName] ?: return null
        return fallback
    }

    /**
     * Embedded fallback strain data - curated profiles for popular strains
     * Used when Cannlytics API doesn't have data for a strain
     */
    private val FALLBACK_STRAINS: Map<String, ApiStrainData> = mapOf(
        "apple fritter" to ApiStrainData(
            name = "Apple Fritter",
            type = "hybrid",
            description = "A potent hybrid known for its sweet apple and vanilla flavors",
            thcRange = "22-28%",
            cbdRange = "<1%",
            effects = listOf("Relaxed", "Happy", "Euphoric", "Uplifted", "Creative"),
            aromas = listOf("Apple", "Vanilla", "Sweet", "Earthy"),
            terpenes = mapOf("myrcene" to 0.35, "limonene" to 0.28, "caryophyllene" to 0.22, "pinene" to 0.12, "linalool" to 0.08),
            imageUrl = null
        ),
        "charlotte's web" to ApiStrainData(
            name = "Charlotte's Web",
            type = "sativa",
            description = "High-CBD strain famous for its therapeutic properties with minimal psychoactive effects",
            thcRange = "<1%",
            cbdRange = "13-20%",
            effects = listOf("Calm", "Focused", "Clear-headed", "Relaxed"),
            aromas = listOf("Pine", "Earthy", "Woody", "Sage"),
            terpenes = mapOf("myrcene" to 0.45, "pinene" to 0.25, "caryophyllene" to 0.18, "limonene" to 0.08, "linalool" to 0.04),
            imageUrl = null
        ),
        "gelato" to ApiStrainData(
            name = "Gelato",
            type = "hybrid",
            description = "Sweet dessert strain with a balanced high and beautiful purple hues",
            thcRange = "20-25%",
            cbdRange = "<1%",
            effects = listOf("Relaxed", "Happy", "Euphoric", "Uplifted", "Creative"),
            aromas = listOf("Sweet", "Citrus", "Berry", "Lavender"),
            terpenes = mapOf("limonene" to 0.32, "caryophyllene" to 0.28, "myrcene" to 0.22, "linalool" to 0.15, "humulene" to 0.08),
            imageUrl = null
        ),
        "granddaddy purple" to ApiStrainData(
            name = "Granddaddy Purple",
            type = "indica",
            description = "Classic indica with grape and berry flavors, known for deep relaxation",
            thcRange = "17-24%",
            cbdRange = "<1%",
            effects = listOf("Relaxed", "Sleepy", "Happy", "Euphoric", "Hungry"),
            aromas = listOf("Grape", "Berry", "Earthy", "Sweet"),
            terpenes = mapOf("myrcene" to 0.42, "caryophyllene" to 0.28, "pinene" to 0.18, "limonene" to 0.12, "linalool" to 0.08),
            imageUrl = null
        ),
        "og kush" to ApiStrainData(
            name = "OG Kush",
            type = "hybrid",
            description = "Legendary strain with complex earthy, pine, and citrus flavors",
            thcRange = "19-26%",
            cbdRange = "<1%",
            effects = listOf("Relaxed", "Happy", "Euphoric", "Uplifted", "Hungry"),
            aromas = listOf("Earthy", "Pine", "Woody", "Citrus"),
            terpenes = mapOf("myrcene" to 0.38, "limonene" to 0.28, "caryophyllene" to 0.22, "pinene" to 0.15, "linalool" to 0.08),
            imageUrl = null
        ),
        "wedding cake" to ApiStrainData(
            name = "Wedding Cake",
            type = "hybrid",
            description = "Sweet and tangy strain with relaxing effects and beautiful trichome coverage",
            thcRange = "22-27%",
            cbdRange = "<1%",
            effects = listOf("Relaxed", "Happy", "Euphoric", "Uplifted", "Hungry"),
            aromas = listOf("Sweet", "Vanilla", "Earthy", "Peppery"),
            terpenes = mapOf("limonene" to 0.35, "caryophyllene" to 0.30, "myrcene" to 0.22, "linalool" to 0.10, "humulene" to 0.08),
            imageUrl = null
        ),
        "jack herer" to ApiStrainData(
            name = "Jack Herer",
            type = "sativa",
            description = "Legendary sativa named after the cannabis activist, known for creative and energetic effects",
            thcRange = "18-24%",
            cbdRange = "<1%",
            effects = listOf("Energetic", "Creative", "Focused", "Happy", "Uplifted"),
            aromas = listOf("Pine", "Earthy", "Woody", "Spicy"),
            terpenes = mapOf("terpinolene" to 0.35, "pinene" to 0.25, "caryophyllene" to 0.18, "myrcene" to 0.12, "limonene" to 0.10),
            imageUrl = null
        ),
        "green crack" to ApiStrainData(
            name = "Green Crack",
            type = "sativa",
            description = "Intense sativa with sharp energy and focus, featuring mango and citrus notes",
            thcRange = "15-24%",
            cbdRange = "<1%",
            effects = listOf("Energetic", "Focused", "Happy", "Uplifted", "Creative"),
            aromas = listOf("Mango", "Citrus", "Sweet", "Earthy"),
            terpenes = mapOf("myrcene" to 0.40, "limonene" to 0.25, "caryophyllene" to 0.15, "pinene" to 0.12, "ocimene" to 0.08),
            imageUrl = null
        ),
        "purple punch" to ApiStrainData(
            name = "Purple Punch",
            type = "indica",
            description = "Sweet and sedating indica with grape candy flavors",
            thcRange = "18-25%",
            cbdRange = "<1%",
            effects = listOf("Relaxed", "Sleepy", "Happy", "Euphoric", "Hungry"),
            aromas = listOf("Grape", "Berry", "Sweet", "Vanilla"),
            terpenes = mapOf("myrcene" to 0.38, "caryophyllene" to 0.25, "limonene" to 0.18, "pinene" to 0.12, "linalool" to 0.10),
            imageUrl = null
        ),
        "gorilla glue" to ApiStrainData(
            name = "Gorilla Glue",
            type = "hybrid",
            description = "Extremely potent hybrid known for heavy relaxation and earthy, diesel flavors",
            thcRange = "25-30%",
            cbdRange = "<1%",
            effects = listOf("Relaxed", "Euphoric", "Happy", "Uplifted", "Sleepy"),
            aromas = listOf("Earthy", "Pine", "Diesel", "Pungent"),
            terpenes = mapOf("caryophyllene" to 0.42, "myrcene" to 0.30, "limonene" to 0.18, "pinene" to 0.10, "humulene" to 0.08),
            imageUrl = null
        ),
        "harlequin" to ApiStrainData(
            name = "Harlequin",
            type = "sativa",
            description = "High-CBD sativa providing clear-headed relief without heavy psychoactive effects",
            thcRange = "4-10%",
            cbdRange = "8-16%",
            effects = listOf("Relaxed", "Focused", "Happy", "Uplifted", "Clear-headed"),
            aromas = listOf("Earthy", "Mango", "Sweet", "Woody"),
            terpenes = mapOf("myrcene" to 0.48, "pinene" to 0.22, "caryophyllene" to 0.15, "limonene" to 0.10, "humulene" to 0.05),
            imageUrl = null
        ),
        "bubba kush" to ApiStrainData(
            name = "Bubba Kush",
            type = "indica",
            description = "Classic indica with coffee and chocolate flavors, known for heavy sedation",
            thcRange = "14-22%",
            cbdRange = "<1%",
            effects = listOf("Relaxed", "Sleepy", "Happy", "Euphoric", "Hungry"),
            aromas = listOf("Coffee", "Chocolate", "Earthy", "Sweet"),
            terpenes = mapOf("myrcene" to 0.45, "caryophyllene" to 0.28, "limonene" to 0.15, "pinene" to 0.08, "linalool" to 0.06),
            imageUrl = null
        ),
        "durban poison" to ApiStrainData(
            name = "Durban Poison",
            type = "sativa",
            description = "Pure African sativa known for its sweet smell and energetic, uplifting effects",
            thcRange = "15-25%",
            cbdRange = "<1%",
            effects = listOf("Energetic", "Uplifted", "Happy", "Creative", "Focused"),
            aromas = listOf("Sweet", "Earthy", "Pine", "Anise"),
            terpenes = mapOf("terpinolene" to 0.40, "myrcene" to 0.22, "pinene" to 0.15, "limonene" to 0.12, "ocimene" to 0.08),
            imageUrl = null
        ),
        "girl scout cookies" to ApiStrainData(
            name = "Girl Scout Cookies",
            type = "hybrid",
            description = "Famous hybrid with sweet and earthy flavors, known for full-body relaxation and euphoria",
            thcRange = "19-28%",
            cbdRange = "<1%",
            effects = listOf("Relaxed", "Happy", "Euphoric", "Uplifted", "Creative"),
            aromas = listOf("Sweet", "Earthy", "Mint", "Chocolate"),
            terpenes = mapOf("caryophyllene" to 0.35, "limonene" to 0.28, "myrcene" to 0.22, "humulene" to 0.12, "linalool" to 0.08),
            imageUrl = null
        ),
        "northern lights" to ApiStrainData(
            name = "Northern Lights",
            type = "indica",
            description = "Legendary indica known for its resinous buds and fast flowering, provides blissful relaxation",
            thcRange = "16-21%",
            cbdRange = "<1%",
            effects = listOf("Relaxed", "Sleepy", "Happy", "Euphoric", "Hungry"),
            aromas = listOf("Sweet", "Earthy", "Pine", "Spicy"),
            terpenes = mapOf("myrcene" to 0.50, "caryophyllene" to 0.22, "pinene" to 0.15, "limonene" to 0.10, "linalool" to 0.08),
            imageUrl = null
        ),
        "pineapple express" to ApiStrainData(
            name = "Pineapple Express",
            type = "hybrid",
            description = "Tropical hybrid with pineapple and mango flavors, provides energetic and creative buzz",
            thcRange = "17-24%",
            cbdRange = "<1%",
            effects = listOf("Happy", "Uplifted", "Euphoric", "Energetic", "Creative"),
            aromas = listOf("Pineapple", "Mango", "Tropical", "Citrus"),
            terpenes = mapOf("myrcene" to 0.35, "limonene" to 0.30, "caryophyllene" to 0.18, "pinene" to 0.12, "ocimene" to 0.08),
            imageUrl = null
        ),
        "runtz" to ApiStrainData(
            name = "Runtz",
            type = "hybrid",
            description = "Trendy hybrid with candy-like sweetness and colorful buds, known for balanced effects",
            thcRange = "19-29%",
            cbdRange = "<1%",
            effects = listOf("Relaxed", "Happy", "Euphoric", "Uplifted", "Giggly"),
            aromas = listOf("Sweet", "Candy", "Tropical", "Fruity"),
            terpenes = mapOf("limonene" to 0.32, "caryophyllene" to 0.28, "myrcene" to 0.22, "linalool" to 0.12, "pinene" to 0.08),
            imageUrl = null
        ),
        "skywalker og" to ApiStrainData(
            name = "Skywalker OG",
            type = "indica",
            description = "Powerful indica cross of Skywalker and OG Kush, known for heavy relaxation and euphoria",
            thcRange = "20-26%",
            cbdRange = "<1%",
            effects = listOf("Relaxed", "Sleepy", "Happy", "Euphoric", "Hungry"),
            aromas = listOf("Earthy", "Spicy", "Sweet", "Diesel"),
            terpenes = mapOf("myrcene" to 0.42, "caryophyllene" to 0.28, "limonene" to 0.18, "pinene" to 0.10, "linalool" to 0.08),
            imageUrl = null
        ),
        "white widow" to ApiStrainData(
            name = "White Widow",
            type = "hybrid",
            description = "Dutch classic covered in white crystals, known for balanced euphoria and energy",
            thcRange = "18-25%",
            cbdRange = "<1%",
            effects = listOf("Relaxed", "Happy", "Euphoric", "Uplifted", "Creative"),
            aromas = listOf("Earthy", "Woody", "Pungent", "Spicy"),
            terpenes = mapOf("myrcene" to 0.35, "caryophyllene" to 0.25, "pinene" to 0.18, "limonene" to 0.15, "humulene" to 0.08),
            imageUrl = null
        ),
        "zkittlez" to ApiStrainData(
            name = "Zkittlez",
            type = "indica",
            description = "Award-winning indica with candy-like fruit flavors and calming effects",
            thcRange = "15-23%",
            cbdRange = "<1%",
            effects = listOf("Relaxed", "Happy", "Euphoric", "Sleepy", "Focused"),
            aromas = listOf("Sweet", "Berry", "Grape", "Tropical"),
            terpenes = mapOf("caryophyllene" to 0.32, "limonene" to 0.25, "myrcene" to 0.20, "linalool" to 0.12, "humulene" to 0.08),
            imageUrl = null
        ),
        "blue dream" to ApiStrainData(
            name = "Blue Dream",
            type = "hybrid",
            description = "Popular sativa-dominant hybrid with balanced full-body relaxation and cerebral invigoration",
            thcRange = "17-24%",
            cbdRange = "<1%",
            effects = listOf("Happy", "Relaxed", "Euphoric", "Uplifted", "Creative"),
            aromas = listOf("Blueberry", "Sweet", "Berry", "Earthy"),
            terpenes = mapOf("myrcene" to 0.40, "pinene" to 0.22, "caryophyllene" to 0.18, "limonene" to 0.12, "linalool" to 0.08),
            imageUrl = null
        ),
        "sour diesel" to ApiStrainData(
            name = "Sour Diesel",
            type = "sativa",
            description = "Fast-acting sativa with dreamy, cerebral effects and a pungent diesel aroma",
            thcRange = "19-25%",
            cbdRange = "<1%",
            effects = listOf("Energetic", "Happy", "Uplifted", "Euphoric", "Creative"),
            aromas = listOf("Diesel", "Pungent", "Earthy", "Citrus"),
            terpenes = mapOf("caryophyllene" to 0.35, "myrcene" to 0.25, "limonene" to 0.22, "pinene" to 0.12, "humulene" to 0.08),
            imageUrl = null
        )
    )
}
