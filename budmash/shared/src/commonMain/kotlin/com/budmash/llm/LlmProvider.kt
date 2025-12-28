package com.budmash.llm

import kotlinx.serialization.Serializable

enum class LlmProviderType {
    OPENAI, ANTHROPIC, OPENROUTER, GOOGLE, GROQ, OLLAMA
}

@Serializable
data class LlmConfig(
    val provider: LlmProviderType = LlmProviderType.OPENROUTER,
    val apiKey: String,
    val model: String = "anthropic/claude-3-haiku",
    val baseUrl: String? = null,
    val maxTokens: Int = 4096,
    val temperature: Float = 0.3f
)

@Serializable
data class LlmMessage(
    val role: String,  // "system", "user", "assistant"
    val content: String
)

data class LlmResponse(
    val content: String,
    val tokensUsed: Int
)

interface LlmProvider {
    suspend fun complete(messages: List<LlmMessage>, config: LlmConfig): LlmResponse
}
