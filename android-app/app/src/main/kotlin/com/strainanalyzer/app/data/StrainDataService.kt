package com.strainanalyzer.app.data

import android.content.Context
import com.strainanalyzer.app.analysis.LocalAnalysisEngine
import com.strainanalyzer.app.llm.LlmService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Service for fetching strain data from multiple sources:
 * 1. Local embedded database (fastest, offline)
 * 2. Local cache (previously generated strains)
 * 3. LLM generation (requires API key)
 */
class StrainDataService(private val context: Context) {

    private val analysisEngine = LocalAnalysisEngine.getInstance(context)
    private val llmService = LlmService.getInstance(context)
    private val cacheFile = "generated_strains.json"

    data class StrainFetchResult(
        val strain: LocalAnalysisEngine.StrainData?,
        val source: StrainSource,
        val error: String? = null
    )

    enum class StrainSource {
        LOCAL_DATABASE,    // Embedded in app
        LOCAL_CACHE,       // Previously generated and cached
        LLM_GENERATED,     // Just generated via LLM API
        NOT_FOUND          // Could not find or generate
    }

    /**
     * Get strain data, trying sources in order:
     * 1. Local database
     * 2. Local cache
     * 3. LLM generation
     */
    suspend fun getStrainData(strainName: String): StrainFetchResult {
        val normalizedName = strainName.lowercase().trim()

        // 1. Check local database first
        val localStrain = analysisEngine.getStrain(normalizedName)
        if (localStrain != null) {
            return StrainFetchResult(localStrain, StrainSource.LOCAL_DATABASE)
        }

        // 2. Check local cache
        val cachedStrain = getCachedStrain(normalizedName)
        if (cachedStrain != null) {
            return StrainFetchResult(cachedStrain, StrainSource.LOCAL_CACHE)
        }

        // 3. Try to generate via LLM
        if (!llmService.isConfigured()) {
            return StrainFetchResult(
                null,
                StrainSource.NOT_FOUND,
                "Strain not in database. Configure an LLM provider in Settings to generate strain data."
            )
        }

        return try {
            val generatedStrain = generateStrainViaLlm(normalizedName)
            if (generatedStrain != null) {
                // Add to local database permanently
                analysisEngine.addStrain(generatedStrain)
                StrainFetchResult(generatedStrain, StrainSource.LLM_GENERATED)
            } else {
                StrainFetchResult(
                    null,
                    StrainSource.NOT_FOUND,
                    "Could not generate data for '$strainName'. Try a different strain name."
                )
            }
        } catch (e: Exception) {
            StrainFetchResult(
                null,
                StrainSource.NOT_FOUND,
                "Error generating strain data: ${e.message}"
            )
        }
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

    companion object {
        @Volatile
        private var instance: StrainDataService? = null

        fun getInstance(context: Context): StrainDataService {
            return instance ?: synchronized(this) {
                instance ?: StrainDataService(context.applicationContext).also { instance = it }
            }
        }
    }
}
