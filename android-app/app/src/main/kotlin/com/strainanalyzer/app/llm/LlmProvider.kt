package com.strainanalyzer.app.llm

/**
 * Lightweight LLM abstraction layer - similar to LiteLLM but for Android/Kotlin
 * Supports multiple providers with a unified interface
 */

enum class LlmProviderType {
    OPENAI,
    ANTHROPIC,
    OPENROUTER,
    GOOGLE,
    GROQ,
    OLLAMA,  // For local/self-hosted
    NONE     // Template-based only (no API calls)
}

data class LlmConfig(
    val provider: LlmProviderType,
    val apiKey: String,
    val model: String,
    val baseUrl: String? = null,  // For custom endpoints
    val maxTokens: Int = 1024,
    val temperature: Float = 0.7f
)

data class LlmMessage(
    val role: String,  // "system", "user", "assistant"
    val content: String
)

data class LlmResponse(
    val content: String,
    val model: String,
    val tokensUsed: Int? = null,
    val error: String? = null
)

interface LlmProvider {
    val providerType: LlmProviderType
    suspend fun complete(messages: List<LlmMessage>, config: LlmConfig): LlmResponse
    suspend fun complete(prompt: String, config: LlmConfig): LlmResponse {
        return complete(listOf(LlmMessage("user", prompt)), config)
    }
}

/**
 * Default models for each provider
 */
object DefaultModels {
    val defaults = mapOf(
        LlmProviderType.OPENAI to "gpt-4o-mini",
        LlmProviderType.ANTHROPIC to "claude-3-haiku-20240307",
        LlmProviderType.OPENROUTER to "openai/gpt-4o-mini",
        LlmProviderType.GOOGLE to "gemini-1.5-flash",
        LlmProviderType.GROQ to "llama-3.1-8b-instant",
        LlmProviderType.OLLAMA to "llama3.2",
        LlmProviderType.NONE to ""
    )

    fun getDefault(provider: LlmProviderType): String {
        return defaults[provider] ?: ""
    }
}
