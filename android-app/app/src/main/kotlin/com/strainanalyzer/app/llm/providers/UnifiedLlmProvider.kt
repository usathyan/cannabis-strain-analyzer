package com.strainanalyzer.app.llm.providers

import com.strainanalyzer.app.llm.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Unified LLM Provider - handles multiple API formats with a single implementation
 * Similar to LiteLLM's approach of normalizing different provider APIs
 */
class UnifiedLlmProvider : LlmProvider {

    override val providerType: LlmProviderType = LlmProviderType.OPENAI // Default

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    override suspend fun complete(messages: List<LlmMessage>, config: LlmConfig): LlmResponse {
        return withContext(Dispatchers.IO) {
            try {
                when (config.provider) {
                    LlmProviderType.ANTHROPIC -> completeAnthropic(messages, config)
                    LlmProviderType.GOOGLE -> completeGoogle(messages, config)
                    LlmProviderType.NONE -> completeTemplate(messages, config)
                    else -> completeOpenAIFormat(messages, config) // OpenAI, OpenRouter, Groq, Ollama
                }
            } catch (e: Exception) {
                LlmResponse(
                    content = "",
                    model = config.model,
                    error = e.message ?: "Unknown error"
                )
            }
        }
    }

    /**
     * OpenAI-compatible format (OpenAI, OpenRouter, Groq, Ollama, etc.)
     */
    private fun completeOpenAIFormat(messages: List<LlmMessage>, config: LlmConfig): LlmResponse {
        val baseUrl = config.baseUrl ?: getBaseUrl(config.provider)
        val endpoint = "$baseUrl/chat/completions"

        val messagesArray = JSONArray().apply {
            messages.forEach { msg ->
                put(JSONObject().apply {
                    put("role", msg.role)
                    put("content", msg.content)
                })
            }
        }

        val requestBody = JSONObject().apply {
            put("model", config.model)
            put("messages", messagesArray)
            put("max_tokens", config.maxTokens)
            put("temperature", config.temperature)
        }

        val request = Request.Builder()
            .url(endpoint)
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer ${config.apiKey}")
            .apply {
                // OpenRouter requires additional headers
                if (config.provider == LlmProviderType.OPENROUTER) {
                    addHeader("HTTP-Referer", "https://strainanalyzer.app")
                    addHeader("X-Title", "Strain Analyzer")
                }
            }
            .post(requestBody.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            return LlmResponse(
                content = "",
                model = config.model,
                error = "API error ${response.code}: $responseBody"
            )
        }

        val json = JSONObject(responseBody)
        val content = json
            .getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")

        val tokensUsed = json.optJSONObject("usage")?.optInt("total_tokens")

        return LlmResponse(
            content = content,
            model = config.model,
            tokensUsed = tokensUsed
        )
    }

    /**
     * Anthropic Claude format
     */
    private fun completeAnthropic(messages: List<LlmMessage>, config: LlmConfig): LlmResponse {
        val baseUrl = config.baseUrl ?: "https://api.anthropic.com/v1"
        val endpoint = "$baseUrl/messages"

        // Separate system message from others
        val systemMessage = messages.find { it.role == "system" }?.content ?: ""
        val otherMessages = messages.filter { it.role != "system" }

        val messagesArray = JSONArray().apply {
            otherMessages.forEach { msg ->
                put(JSONObject().apply {
                    put("role", msg.role)
                    put("content", msg.content)
                })
            }
        }

        val requestBody = JSONObject().apply {
            put("model", config.model)
            put("max_tokens", config.maxTokens)
            put("messages", messagesArray)
            if (systemMessage.isNotEmpty()) {
                put("system", systemMessage)
            }
        }

        val request = Request.Builder()
            .url(endpoint)
            .addHeader("Content-Type", "application/json")
            .addHeader("x-api-key", config.apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .post(requestBody.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            return LlmResponse(
                content = "",
                model = config.model,
                error = "API error ${response.code}: $responseBody"
            )
        }

        val json = JSONObject(responseBody)
        val content = json
            .getJSONArray("content")
            .getJSONObject(0)
            .getString("text")

        val inputTokens = json.optJSONObject("usage")?.optInt("input_tokens") ?: 0
        val outputTokens = json.optJSONObject("usage")?.optInt("output_tokens") ?: 0

        return LlmResponse(
            content = content,
            model = config.model,
            tokensUsed = inputTokens + outputTokens
        )
    }

    /**
     * Google Gemini format
     */
    private fun completeGoogle(messages: List<LlmMessage>, config: LlmConfig): LlmResponse {
        val baseUrl = config.baseUrl ?: "https://generativelanguage.googleapis.com/v1beta"
        val endpoint = "$baseUrl/models/${config.model}:generateContent?key=${config.apiKey}"

        val contentsArray = JSONArray().apply {
            messages.forEach { msg ->
                put(JSONObject().apply {
                    put("role", if (msg.role == "assistant") "model" else "user")
                    put("parts", JSONArray().put(JSONObject().put("text", msg.content)))
                })
            }
        }

        val requestBody = JSONObject().apply {
            put("contents", contentsArray)
            put("generationConfig", JSONObject().apply {
                put("maxOutputTokens", config.maxTokens)
                put("temperature", config.temperature)
            })
        }

        val request = Request.Builder()
            .url(endpoint)
            .addHeader("Content-Type", "application/json")
            .post(requestBody.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            return LlmResponse(
                content = "",
                model = config.model,
                error = "API error ${response.code}: $responseBody"
            )
        }

        val json = JSONObject(responseBody)
        val content = json
            .getJSONArray("candidates")
            .getJSONObject(0)
            .getJSONObject("content")
            .getJSONArray("parts")
            .getJSONObject(0)
            .getString("text")

        return LlmResponse(
            content = content,
            model = config.model
        )
    }

    /**
     * Template-based response (no API call)
     */
    private fun completeTemplate(messages: List<LlmMessage>, config: LlmConfig): LlmResponse {
        return LlmResponse(
            content = "Template-based analysis. Configure an LLM provider for AI-powered insights.",
            model = "template",
            tokensUsed = 0
        )
    }

    private fun getBaseUrl(provider: LlmProviderType): String {
        return when (provider) {
            LlmProviderType.OPENAI -> "https://api.openai.com/v1"
            LlmProviderType.OPENROUTER -> "https://openrouter.ai/api/v1"
            LlmProviderType.GROQ -> "https://api.groq.com/openai/v1"
            LlmProviderType.OLLAMA -> "http://localhost:11434/v1"
            else -> "https://api.openai.com/v1"
        }
    }
}
