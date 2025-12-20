package com.strainanalyzer.app.data

import android.content.Context
import android.util.Log
import com.strainanalyzer.app.analysis.LocalAnalysisEngine
import com.strainanalyzer.app.api.StrainApiService
import com.strainanalyzer.app.llm.LlmService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Service for fetching strain data from multiple sources:
 * 1. Local embedded database (fastest, offline)
 * 2. Local cache (previously fetched/generated strains)
 * 3. Cannlytics API (real strain database)
 * 4. LLM generation (fallback, requires API key)
 */
class StrainDataService(private val context: Context) {

    private val analysisEngine = LocalAnalysisEngine.getInstance(context)
    private val llmService = LlmService.getInstance(context)
    private val strainApi = StrainApiService.getInstance()
    private val cacheFile = "generated_strains.json"

    companion object {
        private const val TAG = "StrainDataService"

        @Volatile
        private var instance: StrainDataService? = null

        fun getInstance(context: Context): StrainDataService {
            return instance ?: synchronized(this) {
                instance ?: StrainDataService(context.applicationContext).also { instance = it }
            }
        }
    }

    data class StrainFetchResult(
        val strain: LocalAnalysisEngine.StrainData?,
        val source: StrainSource,
        val error: String? = null
    )

    enum class StrainSource {
        LOCAL_DATABASE,    // Embedded in app
        LOCAL_CACHE,       // Previously fetched/generated and cached
        API_FETCHED,       // Fetched from Cannlytics API
        LLM_GENERATED,     // Generated via LLM API (fallback)
        NOT_FOUND          // Could not find or generate
    }

    /**
     * Get strain data, trying sources in order:
     * 1. Local database (embedded, fastest)
     * 2. Local cache (previously fetched)
     * 3. Cannlytics API (real strain database)
     * 4. LLM generation (fallback)
     */
    suspend fun getStrainData(strainName: String): StrainFetchResult {
        val normalizedName = strainName.lowercase().trim()
        Log.d(TAG, "Fetching strain data for: $normalizedName")

        // 1. Check local database first (embedded strains)
        val localStrain = analysisEngine.getStrain(normalizedName)
        if (localStrain != null) {
            Log.d(TAG, "Found in local database: $normalizedName")
            return StrainFetchResult(localStrain, StrainSource.LOCAL_DATABASE)
        }

        // 2. Check local cache (previously fetched/generated)
        val cachedStrain = getCachedStrain(normalizedName)
        if (cachedStrain != null) {
            Log.d(TAG, "Found in cache: $normalizedName")
            return StrainFetchResult(cachedStrain, StrainSource.LOCAL_CACHE)
        }

        // 3. Try Cannlytics API (real strain database)
        Log.d(TAG, "Searching Cannlytics API for: $normalizedName")
        val apiResult = fetchFromApi(strainName)
        if (apiResult != null) {
            Log.d(TAG, "Found in Cannlytics API: ${apiResult.name}")
            // Cache the result and add to database
            analysisEngine.addStrain(apiResult)
            cacheStrain(normalizedName, apiResult)
            return StrainFetchResult(apiResult, StrainSource.API_FETCHED)
        }

        // 4. Try to generate via LLM (fallback only)
        Log.d(TAG, "Not found in API, trying LLM for: $normalizedName")
        if (!llmService.isConfigured()) {
            return StrainFetchResult(
                null,
                StrainSource.NOT_FOUND,
                "Strain '$strainName' not found in database or Cannlytics API. Configure an LLM provider in Settings for AI-generated data."
            )
        }

        return try {
            val generatedStrain = generateStrainViaLlm(normalizedName)
            if (generatedStrain != null) {
                Log.d(TAG, "Generated via LLM: $normalizedName")
                // Add to local database permanently
                analysisEngine.addStrain(generatedStrain)
                cacheStrain(normalizedName, generatedStrain)
                StrainFetchResult(generatedStrain, StrainSource.LLM_GENERATED)
            } else {
                StrainFetchResult(
                    null,
                    StrainSource.NOT_FOUND,
                    "Could not find or generate data for '$strainName'. Try a different strain name."
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating strain data", e)
            StrainFetchResult(
                null,
                StrainSource.NOT_FOUND,
                "Error fetching strain data: ${e.message}"
            )
        }
    }

    /**
     * Fetch strain from Cannlytics API and convert to our format
     */
    private suspend fun fetchFromApi(strainName: String): LocalAnalysisEngine.StrainData? {
        return try {
            val apiData = strainApi.searchStrain(strainName) ?: return null

            // Convert API response to our StrainData format
            LocalAnalysisEngine.StrainData(
                name = apiData.name.lowercase(),
                terpenes = apiData.terpenes,
                effects = apiData.effects,
                medicalEffects = inferMedicalEffects(apiData.effects, apiData.type),
                type = apiData.type,
                thcRange = apiData.thcRange,
                cbdRange = apiData.cbdRange,
                description = apiData.description,
                flavors = apiData.aromas
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching from API", e)
            null
        }
    }

    /**
     * Infer medical effects based on recreational effects and strain type
     */
    private fun inferMedicalEffects(effects: List<String>, type: String): List<String> {
        val medicalEffects = mutableListOf<String>()

        effects.forEach { effect ->
            when (effect.lowercase()) {
                "relaxed" -> medicalEffects.addAll(listOf("Stress Relief", "Anxiety"))
                "sleepy" -> medicalEffects.addAll(listOf("Insomnia", "Sleep Aid"))
                "happy" -> medicalEffects.add("Depression")
                "euphoric" -> medicalEffects.add("Mood Enhancement")
                "hungry" -> medicalEffects.add("Appetite Loss")
                "creative" -> medicalEffects.add("Focus")
                "focused" -> medicalEffects.add("ADD/ADHD")
                "uplifted" -> medicalEffects.add("Fatigue")
                "tingly" -> medicalEffects.add("Pain Relief")
            }
        }

        // Add common medical uses based on type
        when (type.lowercase()) {
            "indica" -> {
                if ("Pain Relief" !in medicalEffects) medicalEffects.add("Pain Relief")
                if ("Insomnia" !in medicalEffects) medicalEffects.add("Insomnia")
            }
            "sativa" -> {
                if ("Depression" !in medicalEffects) medicalEffects.add("Depression")
                if ("Fatigue" !in medicalEffects) medicalEffects.add("Fatigue")
            }
        }

        return medicalEffects.distinct().take(4)
    }

    private suspend fun generateStrainViaLlm(strainName: String): LocalAnalysisEngine.StrainData? {
        return withContext(Dispatchers.IO) {
            try {
                val prompt = buildStrainGenerationPrompt(strainName)
                val response = llmService.complete(prompt)

                if (response == null) {
                    return@withContext null
                }

                parseStrainFromLlmResponse(strainName, response)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    private fun buildStrainGenerationPrompt(strainName: String): String {
        return """
Generate detailed cannabis strain information for "$strainName" in JSON format.

You MUST respond with ONLY valid JSON, no other text.

Required format:
{
    "type": "indica" or "sativa" or "hybrid",
    "thc_range": "XX-XX%",
    "cbd_range": "X.X-X.X%",
    "description": "Brief description",
    "effects": ["effect1", "effect2", "effect3"],
    "medical_effects": ["medical1", "medical2"],
    "flavors": ["flavor1", "flavor2"],
    "terpenes": {
        "myrcene": 0.0 to 1.0,
        "limonene": 0.0 to 1.0,
        "caryophyllene": 0.0 to 1.0,
        "pinene": 0.0 to 1.0,
        "linalool": 0.0 to 1.0,
        "humulene": 0.0 to 1.0,
        "terpinolene": 0.0 to 1.0,
        "ocimene": 0.0 to 1.0,
        "nerolidol": 0.0 to 1.0,
        "bisabolol": 0.0 to 1.0,
        "eucalyptol": 0.0 to 1.0
    }
}

Respond with ONLY the JSON object, no markdown, no explanation.
        """.trimIndent()
    }

    private fun parseStrainFromLlmResponse(strainName: String, response: String): LocalAnalysisEngine.StrainData? {
        return try {
            // Extract JSON from response (handle markdown code blocks)
            val jsonStr = response
                .replace("```json", "")
                .replace("```", "")
                .trim()

            val json = JSONObject(jsonStr)

            val terpenes = mutableMapOf<String, Double>()
            json.optJSONObject("terpenes")?.let { terpenesObj ->
                terpenesObj.keys().forEach { key ->
                    terpenes[key] = terpenesObj.optDouble(key, 0.0)
                }
            }

            LocalAnalysisEngine.StrainData(
                name = strainName,
                terpenes = terpenes,
                effects = parseJsonArray(json.optJSONArray("effects")),
                medicalEffects = parseJsonArray(json.optJSONArray("medical_effects")),
                type = json.optString("type", "hybrid"),
                thcRange = json.optString("thc_range", "Unknown"),
                cbdRange = json.optString("cbd_range", "Unknown"),
                description = json.optString("description", ""),
                flavors = parseJsonArray(json.optJSONArray("flavors"))
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun parseJsonArray(arr: org.json.JSONArray?): List<String> {
        if (arr == null) return emptyList()
        return (0 until arr.length()).map { arr.getString(it) }
    }

    private fun getCachedStrain(name: String): LocalAnalysisEngine.StrainData? {
        return try {
            val prefs = context.getSharedPreferences("strain_cache", Context.MODE_PRIVATE)
            val json = prefs.getString(name, null) ?: return null
            val obj = JSONObject(json)
            parseStrainFromCache(name, obj)
        } catch (e: Exception) {
            null
        }
    }

    private fun cacheStrain(name: String, strain: LocalAnalysisEngine.StrainData) {
        try {
            val json = JSONObject().apply {
                put("type", strain.type)
                put("thc_range", strain.thcRange)
                put("cbd_range", strain.cbdRange)
                put("description", strain.description)
                put("effects", org.json.JSONArray(strain.effects))
                put("medical_effects", org.json.JSONArray(strain.medicalEffects))
                put("flavors", org.json.JSONArray(strain.flavors))
                put("terpenes", JSONObject(strain.terpenes))
            }

            val prefs = context.getSharedPreferences("strain_cache", Context.MODE_PRIVATE)
            prefs.edit().putString(name, json.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun parseStrainFromCache(name: String, json: JSONObject): LocalAnalysisEngine.StrainData {
        val terpenes = mutableMapOf<String, Double>()
        json.optJSONObject("terpenes")?.let { terpenesObj ->
            terpenesObj.keys().forEach { key ->
                terpenes[key] = terpenesObj.optDouble(key, 0.0)
            }
        }

        return LocalAnalysisEngine.StrainData(
            name = name,
            terpenes = terpenes,
            effects = parseJsonArray(json.optJSONArray("effects")),
            medicalEffects = parseJsonArray(json.optJSONArray("medical_effects")),
            type = json.optString("type", "hybrid"),
            thcRange = json.optString("thc_range", "Unknown"),
            cbdRange = json.optString("cbd_range", "Unknown"),
            description = json.optString("description", ""),
            flavors = parseJsonArray(json.optJSONArray("flavors"))
        )
    }

    fun getCachedStrainCount(): Int {
        val prefs = context.getSharedPreferences("strain_cache", Context.MODE_PRIVATE)
        return prefs.all.size
    }

    fun clearCache() {
        val prefs = context.getSharedPreferences("strain_cache", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}
