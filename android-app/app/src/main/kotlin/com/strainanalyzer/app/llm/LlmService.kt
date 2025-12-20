package com.strainanalyzer.app.llm

import android.content.Context
import android.content.SharedPreferences
import com.strainanalyzer.app.llm.providers.UnifiedLlmProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * LLM Service - manages provider configuration and strain analysis
 * Prefers on-device Gemini Nano when available, falls back to cloud APIs
 */
class LlmService(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("llm_settings", Context.MODE_PRIVATE)
    private val provider = UnifiedLlmProvider()
    private val geminiNano = GeminiNanoService.getInstance(context)

    private val _config = MutableStateFlow(loadConfig())
    val config: StateFlow<LlmConfig> = _config

    // Expose Gemini Nano status
    val geminiNanoStatus: StateFlow<GeminiNanoService.NanoStatus> = geminiNano.status
    val geminiNanoDownloadProgress: StateFlow<Float> = geminiNano.downloadProgress

    /**
     * Initialize Gemini Nano (call on app startup)
     */
    suspend fun initializeGeminiNano() {
        geminiNano.initialize()
    }

    /**
     * Download Gemini Nano if available
     */
    suspend fun downloadGeminiNano() {
        geminiNano.download()
    }

    /**
     * Check if Gemini Nano is available on this device
     */
    fun isGeminiNanoAvailable(): Boolean = geminiNano.isAvailable()

    /**
     * Check if Gemini Nano can be downloaded
     */
    fun isGeminiNanoDownloadable(): Boolean = geminiNano.isDownloadable()

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

    /**
     * Check if any AI provider is configured (Gemini Nano or cloud API)
     */
    fun isConfigured(): Boolean {
        // Gemini Nano takes priority
        if (geminiNano.isAvailable()) return true

        // Fall back to cloud API
        val cfg = _config.value
        return cfg.provider != LlmProviderType.NONE && cfg.apiKey.isNotBlank()
    }

    /**
     * Check if only cloud API is configured (not using Gemini Nano)
     */
    fun isCloudApiConfigured(): Boolean {
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
     * Generate comprehensive strain analysis with detailed profile comparison
     */
    suspend fun analyzeStrainComprehensive(
        strainData: Map<String, Any>,
        userProfile: Map<String, Any>,
        similarityData: Map<String, Any>
    ): String {
        val strainName = strainData["name"]?.toString() ?: "Unknown"
        val overallScore = (similarityData["overall_score"] as? Double)?.toFloat() ?: 0f

        if (!isConfigured()) {
            return generateTemplateAnalysis(strainName, overallScore / 100f)
        }

        val prompt = buildComprehensiveAnalysisPrompt(strainData, userProfile, similarityData)
        val messages = listOf(
            LlmMessage("system", COMPREHENSIVE_SYSTEM_PROMPT),
            LlmMessage("user", prompt)
        )

        val response = provider.complete(messages, _config.value)

        return if (response.error != null) {
            "Analysis unavailable: ${response.error}\n\n${generateTemplateAnalysis(strainName, overallScore / 100f)}"
        } else {
            response.content
        }
    }

    private fun buildComprehensiveAnalysisPrompt(
        strainData: Map<String, Any>,
        userProfile: Map<String, Any>,
        similarityData: Map<String, Any>
    ): String {
        return """
            Provide a comprehensive analysis of this cannabis strain comparison.

            === STRAIN BEING ANALYZED ===
            Name: ${strainData["name"] ?: "Unknown"}
            Type: ${strainData["type"] ?: "Unknown"}
            THC: ${strainData["thc_range"] ?: "Unknown"}
            CBD: ${strainData["cbd_range"] ?: "Unknown"}
            Effects: ${strainData["effects"] ?: "Unknown"}
            Medical Uses: ${strainData["medical_effects"] ?: "Unknown"}
            Flavors: ${strainData["flavors"] ?: "Unknown"}
            Terpene Profile: ${strainData["terpene_profile"] ?: "Unknown"}

            === USER'S PREFERENCE PROFILE ===
            Favorite Strains: ${userProfile["favorite_strains"] ?: "None"}
            Favorite Details: ${userProfile["favorite_details"] ?: "None"}
            Ideal Terpene Profile (from favorites): ${userProfile["ideal_terpene_profile"] ?: "Unknown"}

            === SIMILARITY ANALYSIS (On-Device Calculated) ===
            Overall Match: ${similarityData["overall_score"]}% (${similarityData["match_rating"]})
            Z-Scored Cosine Similarity: ${String.format("%.1f", ((similarityData["z_scored_cosine"] as? Double) ?: 0.0) * 100)}%
            Euclidean Similarity: ${String.format("%.1f", ((similarityData["euclidean_similarity"] as? Double) ?: 0.0) * 100)}%
            Correlation: ${String.format("%.1f", ((similarityData["correlation"] as? Double) ?: 0.0) * 100)}%
            Key Terpene Differences: ${similarityData["terpene_comparison"] ?: "None significant"}

            === ANALYSIS REQUESTED ===
            Based on the comprehensive data above, provide a detailed personalized analysis using HTML formatting (no markdown).

            Structure your response with these sections (use <b>SECTION NAME</b> for headers):

            <b>WHAT YOU'LL LIKELY ENJOY</b>
            Based on terpene overlap and effect similarities with your favorites, what aspects of this strain will appeal to you? Be specific about which terpenes and effects align.

            <b>POTENTIAL DIFFERENCES</b>
            What might feel different compared to your usual favorites? Consider terpene levels that are higher or lower than your ideal profile.

            <b>EXPERIENCE PREDICTION</b>
            Given the strain type, THC/CBD levels, and dominant terpenes, describe what the experience might be like for someone with your preferences.

            <b>BEST USE CASE</b>
            When would this strain be ideal? Time of day, activities, or situations based on its profile.

            <b>RECOMMENDATION</b>
            A clear verdict on whether this strain is worth trying, and any tips for the best experience.

            FORMATTING REMINDERS:
            • Use <b>bold</b> for key terms and emphasis
            • Use <i>italic</i> for strain names and terpene names
            • Use <br><br> between paragraphs
            • Use • for bullet points
            • NO markdown (no **, no ##, no ---)

            Keep the analysis conversational but informative. Use the data provided to give specific, personalized insights.
        """.trimIndent()
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
     * Uses Gemini Nano if available, otherwise falls back to cloud API
     */
    suspend fun generateStrainData(strainName: String): String? {
        // Try Gemini Nano first (on-device)
        if (geminiNano.isAvailable()) {
            val result = geminiNano.generateStrainData(strainName)
            if (result != null) return result
        }

        // Fall back to cloud API
        if (!isCloudApiConfigured()) {
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

        private const val COMPREHENSIVE_SYSTEM_PROMPT = """You are an expert cannabis strain analyst with deep knowledge of terpene science and cannabinoid effects.

Your role is to provide personalized, data-driven analysis comparing cannabis strains to a user's established preference profile. You have access to:
- The target strain's complete profile (terpenes, effects, THC/CBD)
- The user's favorite strains and their combined terpene profile
- Mathematical similarity scores calculated on-device

IMPORTANT FORMATTING RULES:
- Format your response using simple HTML tags for rich text display
- Use <b>bold</b> for emphasis and key terms
- Use <i>italic</i> for strain names and terpene names
- Use <br><br> for paragraph breaks
- Section headers should be bold: <b>SECTION NAME</b>
- Keep paragraphs short and scannable
- Do NOT use markdown (no **, no ##, no bullet points with -)
- Use bullet points as: • item (unicode bullet)

Provide specific, actionable insights based on the actual data - not generic advice.
Be conversational but informative. Avoid medical claims; focus on experience and preferences.
When discussing terpenes, explain what they contribute to the experience."""

        @Volatile
        private var instance: LlmService? = null

        fun getInstance(context: Context): LlmService {
            return instance ?: synchronized(this) {
                instance ?: LlmService(context.applicationContext).also { instance = it }
            }
        }
    }
}
