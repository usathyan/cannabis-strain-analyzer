package com.strainanalyzer.app.llm

import android.content.Context
import android.content.SharedPreferences
import com.strainanalyzer.app.llm.providers.UnifiedLlmProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * LLM Service - manages provider configuration and strain analysis
 */
class LlmService(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("llm_settings", Context.MODE_PRIVATE)
    private val provider = UnifiedLlmProvider()

    private val _config = MutableStateFlow(loadConfig())
    val config: StateFlow<LlmConfig> = _config

    private fun loadConfig(): LlmConfig {
        val providerName = prefs.getString("provider", "NONE") ?: "NONE"
        val providerType = try {
            LlmProviderType.valueOf(providerName)
        } catch (e: Exception) {
            LlmProviderType.NONE
        }

        return LlmConfig(
            provider = providerType,
            apiKey = prefs.getString("api_key", "") ?: "",
            model = prefs.getString("model", DefaultModels.getDefault(providerType)) ?: "",
            baseUrl = prefs.getString("base_url", null),
            maxTokens = prefs.getInt("max_tokens", 1024),
            temperature = prefs.getFloat("temperature", 0.7f)
        )
    }

    fun saveConfig(config: LlmConfig) {
        prefs.edit().apply {
            putString("provider", config.provider.name)
            putString("api_key", config.apiKey)
            putString("model", config.model)
            putString("base_url", config.baseUrl)
            putInt("max_tokens", config.maxTokens)
            putFloat("temperature", config.temperature)
            apply()
        }
        _config.value = config
    }

    fun isConfigured(): Boolean {
        val cfg = _config.value
        return cfg.provider != LlmProviderType.NONE && cfg.apiKey.isNotBlank()
    }

    /**
     * Generate strain analysis using configured LLM
     */
    suspend fun analyzeStrain(
        strainName: String,
        strainData: Map<String, Any>,
        userProfile: Map<String, Any>,
        similarity: Float
    ): String {
        if (!isConfigured()) {
            return generateTemplateAnalysis(strainName, similarity)
        }

        val prompt = buildAnalysisPrompt(strainName, strainData, userProfile, similarity)
        val messages = listOf(
            LlmMessage("system", SYSTEM_PROMPT),
            LlmMessage("user", prompt)
        )

        val response = provider.complete(messages, _config.value)

        return if (response.error != null) {
            "Analysis unavailable: ${response.error}\n\n${generateTemplateAnalysis(strainName, similarity)}"
        } else {
            response.content
        }
    }

    /**
     * Simple completion - send prompt, get response
     */
    suspend fun complete(prompt: String): String? {
        if (!isConfigured()) {
            return null
        }

        val response = provider.complete(prompt, _config.value)
        return if (response.error != null) null else response.content
    }

    /**
     * Generate strain data for unknown strains
     */
    suspend fun generateStrainData(strainName: String): String? {
        if (!isConfigured()) {
            return null
        }

        val prompt = """
            Generate cannabis strain information for "$strainName" in JSON format.
            Include: type (indica/sativa/hybrid), thc_range, cbd_range, effects (list),
            flavors (list), terpenes (object with terpene names and percentages).

            Return ONLY valid JSON, no explanation.
        """.trimIndent()

        val response = provider.complete(prompt, _config.value)

        return if (response.error != null) null else response.content
    }

    private fun buildAnalysisPrompt(
        strainName: String,
        strainData: Map<String, Any>,
        userProfile: Map<String, Any>,
        similarity: Float
    ): String {
        return """
            Analyze this cannabis strain comparison:

            STRAIN: $strainName
            THC: ${strainData["thc_range"] ?: "Unknown"}
            CBD: ${strainData["cbd_range"] ?: "Unknown"}
            Type: ${strainData["type"] ?: "Unknown"}
            Effects: ${strainData["effects"] ?: "Unknown"}

            USER PREFERENCES:
            Favorite strains: ${userProfile["favorite_strains"] ?: "None set"}
            Preferred THC: ${userProfile["preferred_thc"] ?: "Not specified"}

            SIMILARITY SCORE: ${(similarity * 100).toInt()}%

            Provide a brief, helpful analysis (2-3 sentences) explaining:
            1. How well this strain matches their preferences
            2. What they might experience
            3. Any considerations for use
        """.trimIndent()
    }

    private fun generateTemplateAnalysis(strainName: String, similarity: Float): String {
        val percentage = (similarity * 100).toInt()
        return when {
            percentage >= 90 -> "Excellent match! $strainName closely aligns with your preferences. Based on the terpene and cannabinoid profile, this strain should deliver effects very similar to your favorites."
            percentage >= 70 -> "Good match. $strainName shares many characteristics with your preferred strains. You'll likely enjoy the similar effects while experiencing some new nuances."
            percentage >= 50 -> "Moderate match. $strainName has some overlap with your preferences but also brings different characteristics. This could be a good option if you're looking to explore."
            percentage >= 30 -> "This strain differs noticeably from your usual preferences. Consider starting with a smaller amount to see how it affects you."
            else -> "This strain is quite different from your preferred profile. If you try it, approach with an open mind as the effects may vary from what you're used to."
        }
    }

    companion object {
        private const val SYSTEM_PROMPT = """You are a helpful cannabis strain advisor.
Provide brief, accurate, and responsible guidance about cannabis strains.
Focus on terpene profiles, effects, and how strains compare to user preferences.
Always encourage responsible use and consulting healthcare providers for medical questions."""

        @Volatile
        private var instance: LlmService? = null

        fun getInstance(context: Context): LlmService {
            return instance ?: synchronized(this) {
                instance ?: LlmService(context.applicationContext).also { instance = it }
            }
        }
    }
}
