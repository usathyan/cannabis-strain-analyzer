package com.budmash.llm

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class KtorLlmProvider : LlmProvider {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(json)
        }
    }

    override suspend fun complete(messages: List<LlmMessage>, config: LlmConfig): LlmResponse {
        return when (config.provider) {
            LlmProviderType.OPENROUTER -> completeOpenRouter(messages, config)
            LlmProviderType.OPENAI -> completeOpenAI(messages, config)
            LlmProviderType.ANTHROPIC -> completeAnthropic(messages, config)
            else -> completeOpenAI(messages, config) // Default to OpenAI-compatible
        }
    }

    private suspend fun completeOpenRouter(messages: List<LlmMessage>, config: LlmConfig): LlmResponse {
        val baseUrl = config.baseUrl ?: "https://openrouter.ai/api/v1"
        return completeOpenAIFormat(messages, config, baseUrl, mapOf(
            "HTTP-Referer" to "https://budmash.app",
            "X-Title" to "BudMash"
        ))
    }

    private suspend fun completeOpenAI(messages: List<LlmMessage>, config: LlmConfig): LlmResponse {
        val baseUrl = config.baseUrl ?: "https://api.openai.com/v1"
        return completeOpenAIFormat(messages, config, baseUrl, emptyMap())
    }

    private suspend fun completeOpenAIFormat(
        messages: List<LlmMessage>,
        config: LlmConfig,
        baseUrl: String,
        extraHeaders: Map<String, String>
    ): LlmResponse {
        val request = OpenAIRequest(
            model = config.model,
            messages = messages.map { OpenAIMessage(it.role, it.content) },
            max_tokens = config.maxTokens,
            temperature = config.temperature
        )

        val response: OpenAIResponse = client.post("$baseUrl/chat/completions") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${config.apiKey}")
            extraHeaders.forEach { (key, value) -> header(key, value) }
            setBody(request)
        }.body()

        return LlmResponse(
            content = response.choices.firstOrNull()?.message?.content ?: "",
            tokensUsed = response.usage?.total_tokens ?: 0
        )
    }

    private suspend fun completeAnthropic(messages: List<LlmMessage>, config: LlmConfig): LlmResponse {
        val baseUrl = config.baseUrl ?: "https://api.anthropic.com/v1"

        val systemMessage = messages.find { it.role == "system" }?.content ?: ""
        val nonSystemMessages = messages.filter { it.role != "system" }

        val request = AnthropicRequest(
            model = config.model,
            max_tokens = config.maxTokens,
            system = systemMessage,
            messages = nonSystemMessages.map { AnthropicMessage(it.role, it.content) }
        )

        val response: AnthropicResponse = client.post("$baseUrl/messages") {
            contentType(ContentType.Application.Json)
            header("x-api-key", config.apiKey)
            header("anthropic-version", "2023-06-01")
            setBody(request)
        }.body()

        return LlmResponse(
            content = response.content.firstOrNull()?.text ?: "",
            tokensUsed = (response.usage?.input_tokens ?: 0) + (response.usage?.output_tokens ?: 0)
        )
    }
}

// OpenAI-compatible request/response models
@Serializable
private data class OpenAIRequest(
    val model: String,
    val messages: List<OpenAIMessage>,
    val max_tokens: Int,
    val temperature: Float
)

@Serializable
private data class OpenAIMessage(val role: String, val content: String)

@Serializable
private data class OpenAIResponse(
    val choices: List<OpenAIChoice>,
    val usage: OpenAIUsage? = null
)

@Serializable
private data class OpenAIChoice(val message: OpenAIMessage)

@Serializable
private data class OpenAIUsage(val total_tokens: Int)

// Anthropic request/response models
@Serializable
private data class AnthropicRequest(
    val model: String,
    val max_tokens: Int,
    val system: String,
    val messages: List<AnthropicMessage>
)

@Serializable
private data class AnthropicMessage(val role: String, val content: String)

@Serializable
private data class AnthropicResponse(
    val content: List<AnthropicContent>,
    val usage: AnthropicUsage? = null
)

@Serializable
private data class AnthropicContent(val text: String)

@Serializable
private data class AnthropicUsage(val input_tokens: Int, val output_tokens: Int)
