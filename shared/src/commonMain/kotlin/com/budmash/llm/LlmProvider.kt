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

// Legacy text-only message
@Serializable
data class LlmMessage(
    val role: String,  // "system", "user", "assistant"
    val content: String
)

// Multimodal content types for vision
sealed class MessageContent {
    data class Text(val text: String) : MessageContent()
    data class ImageBase64(val base64: String, val mediaType: String = "image/png") : MessageContent()
    data class ImageUrl(val url: String) : MessageContent()
}

// Multimodal message for vision-capable models
data class MultimodalMessage(
    val role: String,
    val content: List<MessageContent>
)

data class LlmResponse(
    val content: String,
    val tokensUsed: Int
)

interface LlmProvider {
    suspend fun complete(messages: List<LlmMessage>, config: LlmConfig): LlmResponse

    // Vision-capable completion
    suspend fun completeVision(messages: List<MultimodalMessage>, config: LlmConfig): LlmResponse
}
