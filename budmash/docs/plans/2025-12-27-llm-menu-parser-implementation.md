# LLM Menu Parser Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Replace MockMenuParser with real LLM-powered extraction using OpenRouter API

**Architecture:** Two-pass LLM pipeline (HTML→Categories→Strains) with Cannlytics terpene resolution

**Tech Stack:** Ktor HTTP client, kotlinx.serialization, OpenRouter API, Cannlytics API

---

## Task 1: Add Ktor Dependencies

**Files:**
- Modify: `shared/build.gradle.kts`

**Step 1: Add Ktor dependencies to shared module**

```kotlin
// In shared/build.gradle.kts, find sourceSets block and add:

val commonMain by getting {
    dependencies {
        // ... existing dependencies ...

        // Ktor HTTP client
        implementation("io.ktor:ktor-client-core:2.3.7")
        implementation("io.ktor:ktor-client-content-negotiation:2.3.7")
        implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")
    }
}

val androidMain by getting {
    dependencies {
        // ... existing dependencies ...
        implementation("io.ktor:ktor-client-okhttp:2.3.7")
    }
}

val iosMain by getting {
    dependencies {
        // ... existing dependencies ...
        implementation("io.ktor:ktor-client-darwin:2.3.7")
    }
}
```

**Step 2: Sync Gradle**

Run: `./gradlew :shared:build`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add shared/build.gradle.kts
git commit -m "build: add Ktor HTTP client dependencies for LLM integration"
```

---

## Task 2: Create LLM Provider Interface

**Files:**
- Create: `shared/src/commonMain/kotlin/com/budmash/llm/LlmProvider.kt`

**Step 1: Create llm directory**

```bash
mkdir -p shared/src/commonMain/kotlin/com/budmash/llm
```

**Step 2: Write the LlmProvider interface**

```kotlin
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
```

**Step 3: Verify compilation**

Run: `./gradlew :shared:compileKotlinAndroid`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add shared/src/commonMain/kotlin/com/budmash/llm/
git commit -m "feat: add LlmProvider interface with config and message types"
```

---

## Task 3: Implement KtorLlmProvider

**Files:**
- Create: `shared/src/commonMain/kotlin/com/budmash/llm/KtorLlmProvider.kt`

**Step 1: Write the Ktor-based provider**

```kotlin
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
```

**Step 2: Verify compilation**

Run: `./gradlew :shared:compileKotlinAndroid`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/budmash/llm/KtorLlmProvider.kt
git commit -m "feat: implement KtorLlmProvider with OpenRouter/OpenAI/Anthropic support"
```

---

## Task 4: Create ParseError Types

**Files:**
- Create: `shared/src/commonMain/kotlin/com/budmash/data/ParseError.kt`

**Step 1: Write error types**

```kotlin
package com.budmash.data

sealed class ParseError {
    data class NetworkError(val message: String) : ParseError()
    data class LlmError(val message: String) : ParseError()
    data class ParseFailure(val message: String) : ParseError()
    data object NoFlowersFound : ParseError()

    fun toUserMessage(): String = when (this) {
        is NetworkError -> "Network error: $message"
        is LlmError -> "AI processing error: $message"
        is ParseFailure -> "Failed to parse menu: $message"
        is NoFlowersFound -> "No flower products found on this menu"
    }
}
```

**Step 2: Update ParseStatus to include Error state**

Modify `shared/src/commonMain/kotlin/com/budmash/parser/MenuParser.kt`:

```kotlin
// Add to ParseStatus sealed class:
data class Error(val error: ParseError) : ParseStatus()
```

**Step 3: Verify compilation**

Run: `./gradlew :shared:compileKotlinAndroid`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add shared/src/commonMain/kotlin/com/budmash/data/ParseError.kt
git add shared/src/commonMain/kotlin/com/budmash/parser/MenuParser.kt
git commit -m "feat: add ParseError types and Error status"
```

---

## Task 5: Implement LlmMenuExtractor

**Files:**
- Create: `shared/src/commonMain/kotlin/com/budmash/parser/LlmMenuExtractor.kt`

**Step 1: Write the two-pass extractor**

```kotlin
package com.budmash.parser

import com.budmash.data.ParseError
import com.budmash.data.StrainData
import com.budmash.data.StrainType
import com.budmash.llm.LlmConfig
import com.budmash.llm.LlmMessage
import com.budmash.llm.LlmProvider
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class LlmMenuExtractor(
    private val llmProvider: LlmProvider
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun extractStrains(html: String, config: LlmConfig): Result<List<ExtractedStrain>> {
        // Pass 1: Categorize menu
        val categorizedResult = categorizeMenu(html, config)
        if (categorizedResult.isFailure) {
            return Result.failure(categorizedResult.exceptionOrNull()!!)
        }

        val categorized = categorizedResult.getOrThrow()
        val flowers = categorized.categories["flower"] ?: categorized.categories["flowers"] ?: emptyList()

        if (flowers.isEmpty()) {
            return Result.failure(Exception(ParseError.NoFlowersFound.toUserMessage()))
        }

        // Pass 2: Extract detailed strain data
        return extractDetailedStrains(flowers, config)
    }

    private suspend fun categorizeMenu(html: String, config: LlmConfig): Result<CategorizedMenu> {
        val truncatedHtml = if (html.length > 100_000) html.take(100_000) else html

        val messages = listOf(
            LlmMessage(
                role = "system",
                content = """Parse this dispensary menu HTML into categorized JSON.
Group products by category (flower, edibles, vapes, concentrates, pre-rolls, tinctures, topicals, etc.)
For each product capture: name, category, price, any visible details.
Output valid JSON only with this structure:
{"categories": {"flower": [{"name": "...", "price": "...", "details": "..."}], "edibles": [...], ...}}"""
            ),
            LlmMessage(role = "user", content = truncatedHtml)
        )

        return try {
            val response = llmProvider.complete(messages, config)
            val parsed = json.decodeFromString<CategorizedMenu>(extractJson(response.content))
            Result.success(parsed)
        } catch (e: Exception) {
            Result.failure(Exception(ParseError.LlmError("Failed to categorize menu: ${e.message}").toUserMessage()))
        }
    }

    private suspend fun extractDetailedStrains(
        flowers: List<MenuProduct>,
        config: LlmConfig
    ): Result<List<ExtractedStrain>> {
        val flowersJson = json.encodeToString(MenuProductList.serializer(), MenuProductList(flowers))

        val messages = listOf(
            LlmMessage(
                role = "system",
                content = """Extract detailed strain data for these flower products.
For each strain provide:
- name: exact product name
- type: INDICA, SATIVA, or HYBRID (infer from description if not stated)
- thcMin: minimum THC percentage (number)
- thcMax: maximum THC percentage (number)
- price: numeric price in dollars
- description: brief description if available

Output valid JSON only: {"strains": [...]}"""
            ),
            LlmMessage(role = "user", content = flowersJson)
        )

        return try {
            val response = llmProvider.complete(messages, config)
            val parsed = json.decodeFromString<StrainList>(extractJson(response.content))
            Result.success(parsed.strains)
        } catch (e: Exception) {
            Result.failure(Exception(ParseError.LlmError("Failed to extract strains: ${e.message}").toUserMessage()))
        }
    }

    private fun extractJson(content: String): String {
        // Handle markdown code blocks
        val jsonMatch = Regex("```(?:json)?\\s*([\\s\\S]*?)```").find(content)
        if (jsonMatch != null) {
            return jsonMatch.groupValues[1].trim()
        }
        // Try to find JSON object directly
        val startIdx = content.indexOf('{')
        val endIdx = content.lastIndexOf('}')
        if (startIdx >= 0 && endIdx > startIdx) {
            return content.substring(startIdx, endIdx + 1)
        }
        return content
    }
}

@Serializable
data class CategorizedMenu(
    val categories: Map<String, List<MenuProduct>>
)

@Serializable
data class MenuProduct(
    val name: String,
    val price: String? = null,
    val details: String? = null
)

@Serializable
private data class MenuProductList(val products: List<MenuProduct>)

@Serializable
data class StrainList(val strains: List<ExtractedStrain>)

@Serializable
data class ExtractedStrain(
    val name: String,
    val type: String,
    val thcMin: Double = 0.0,
    val thcMax: Double = 0.0,
    val price: Double = 0.0,
    val description: String? = null
) {
    fun toStrainData(): StrainData = StrainData(
        name = name,
        type = when (type.uppercase()) {
            "INDICA" -> StrainType.INDICA
            "SATIVA" -> StrainType.SATIVA
            else -> StrainType.HYBRID
        },
        thcMin = thcMin,
        thcMax = thcMax,
        price = price,
        description = description ?: ""
    )
}
```

**Step 2: Verify compilation**

Run: `./gradlew :shared:compileKotlinAndroid`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/budmash/parser/LlmMenuExtractor.kt
git commit -m "feat: implement LlmMenuExtractor with two-pass extraction"
```

---

## Task 6: Implement TerpeneResolver

**Files:**
- Create: `shared/src/commonMain/kotlin/com/budmash/parser/TerpeneResolver.kt`

**Step 1: Write the terpene resolver**

```kotlin
package com.budmash.parser

import com.budmash.data.StrainData
import com.budmash.llm.LlmConfig
import com.budmash.llm.LlmMessage
import com.budmash.llm.LlmProvider
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class TerpeneResolver(
    private val llmProvider: LlmProvider
) {
    private val json = Json { ignoreUnknownKeys = true }

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(json)
        }
    }

    suspend fun resolveAll(
        strains: List<StrainData>,
        config: LlmConfig,
        onProgress: (Int, Int) -> Unit
    ): List<StrainData> = coroutineScope {
        strains.chunked(5).flatMapIndexed { chunkIndex, chunk ->
            val resolved = chunk.mapIndexed { index, strain ->
                async {
                    val globalIndex = chunkIndex * 5 + index + 1
                    onProgress(globalIndex, strains.size)
                    resolveTerpenes(strain, config)
                }
            }.awaitAll()
            resolved
        }
    }

    private suspend fun resolveTerpenes(strain: StrainData, config: LlmConfig): StrainData {
        // Try Cannlytics first
        val cannlyticsResult = tryCannlytics(strain.name)
        if (cannlyticsResult != null) {
            return strain.copy(
                myrcene = cannlyticsResult.myrcene,
                limonene = cannlyticsResult.limonene,
                caryophyllene = cannlyticsResult.caryophyllene,
                pinene = cannlyticsResult.pinene,
                linalool = cannlyticsResult.linalool,
                humulene = cannlyticsResult.humulene,
                terpinolene = cannlyticsResult.terpinolene,
                ocimene = cannlyticsResult.ocimene
            )
        }

        // Fallback to LLM
        val llmResult = tryLlmTerpenes(strain, config)
        if (llmResult != null) {
            return strain.copy(
                myrcene = llmResult.myrcene,
                limonene = llmResult.limonene,
                caryophyllene = llmResult.caryophyllene,
                pinene = llmResult.pinene,
                linalool = llmResult.linalool,
                humulene = llmResult.humulene,
                terpinolene = llmResult.terpinolene,
                ocimene = llmResult.ocimene
            )
        }

        // Return unchanged if both fail
        return strain
    }

    private suspend fun tryCannlytics(strainName: String): TerpeneProfile? {
        return try {
            val response: CannlyticsResponse = client.get("https://cannlytics.com/api/strains") {
                parameter("name", strainName)
            }.body()

            response.data.firstOrNull()?.let { strain ->
                TerpeneProfile(
                    myrcene = strain.myrcene ?: 0.0,
                    limonene = strain.limonene ?: 0.0,
                    caryophyllene = strain.caryophyllene ?: 0.0,
                    pinene = strain.pinene ?: 0.0,
                    linalool = strain.linalool ?: 0.0,
                    humulene = strain.humulene ?: 0.0,
                    terpinolene = strain.terpinolene ?: 0.0,
                    ocimene = strain.ocimene ?: 0.0
                )
            }
        } catch (e: Exception) {
            println("[BudMash] Cannlytics lookup failed for $strainName: ${e.message}")
            null
        }
    }

    private suspend fun tryLlmTerpenes(strain: StrainData, config: LlmConfig): TerpeneProfile? {
        val messages = listOf(
            LlmMessage(
                role = "system",
                content = """You are a cannabis expert. Provide typical terpene percentages for this strain.
Use values between 0.0-1.0 representing percentage (e.g., 0.35 = 35%).
Output JSON only: {"myrcene": 0.0, "limonene": 0.0, "caryophyllene": 0.0, "pinene": 0.0, "linalool": 0.0, "humulene": 0.0, "terpinolene": 0.0, "ocimene": 0.0}"""
            ),
            LlmMessage(
                role = "user",
                content = "Strain: \"${strain.name}\" (${strain.type.name})"
            )
        )

        return try {
            val response = llmProvider.complete(messages, config.copy(maxTokens = 256))
            json.decodeFromString<TerpeneProfile>(extractJson(response.content))
        } catch (e: Exception) {
            println("[BudMash] LLM terpene lookup failed for ${strain.name}: ${e.message}")
            null
        }
    }

    private fun extractJson(content: String): String {
        val startIdx = content.indexOf('{')
        val endIdx = content.lastIndexOf('}')
        if (startIdx >= 0 && endIdx > startIdx) {
            return content.substring(startIdx, endIdx + 1)
        }
        return content
    }
}

@Serializable
data class TerpeneProfile(
    val myrcene: Double = 0.0,
    val limonene: Double = 0.0,
    val caryophyllene: Double = 0.0,
    val pinene: Double = 0.0,
    val linalool: Double = 0.0,
    val humulene: Double = 0.0,
    val terpinolene: Double = 0.0,
    val ocimene: Double = 0.0
)

@Serializable
private data class CannlyticsResponse(val data: List<CannlyticsStrain>)

@Serializable
private data class CannlyticsStrain(
    val name: String? = null,
    val myrcene: Double? = null,
    val limonene: Double? = null,
    val caryophyllene: Double? = null,
    val pinene: Double? = null,
    val linalool: Double? = null,
    val humulene: Double? = null,
    val terpinolene: Double? = null,
    val ocimene: Double? = null
)
```

**Step 2: Verify compilation**

Run: `./gradlew :shared:compileKotlinAndroid`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/budmash/parser/TerpeneResolver.kt
git commit -m "feat: implement TerpeneResolver with Cannlytics + LLM fallback"
```

---

## Task 7: Implement DefaultMenuParser

**Files:**
- Create: `shared/src/commonMain/kotlin/com/budmash/parser/DefaultMenuParser.kt`

**Step 1: Write the main parser**

```kotlin
package com.budmash.parser

import com.budmash.data.DispensaryMenu
import com.budmash.data.ParseError
import com.budmash.llm.LlmConfig
import com.budmash.llm.LlmProvider
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock

class DefaultMenuParser(
    private val llmProvider: LlmProvider,
    private val config: LlmConfig
) : MenuParser {

    private val extractor = LlmMenuExtractor(llmProvider)
    private val terpeneResolver = TerpeneResolver(llmProvider)
    private val httpClient = HttpClient()

    override fun parseMenu(url: String): Flow<ParseStatus> = flow {
        println("[BudMash] DefaultMenuParser starting for URL: $url")

        emit(ParseStatus.Fetching)

        // Fetch HTML
        val htmlResult = fetchHtml(url)
        if (htmlResult.isFailure) {
            emit(ParseStatus.Error(ParseError.NetworkError(htmlResult.exceptionOrNull()?.message ?: "Unknown error")))
            return@flow
        }

        val html = htmlResult.getOrThrow()
        emit(ParseStatus.FetchComplete(html.length))

        // Extract strains via LLM
        val strainsResult = extractor.extractStrains(html, config)
        if (strainsResult.isFailure) {
            emit(ParseStatus.Error(ParseError.LlmError(strainsResult.exceptionOrNull()?.message ?: "Extraction failed")))
            return@flow
        }

        val extractedStrains = strainsResult.getOrThrow()
        emit(ParseStatus.ProductsFound(extractedStrains.size, extractedStrains.size))

        // Convert to StrainData
        var strains = extractedStrains.map { it.toStrainData() }

        // Resolve terpenes
        strains = terpeneResolver.resolveAll(strains, config) { current, total ->
            // Note: We can't emit from inside the callback directly
            // The progress is logged instead
            println("[BudMash] Resolving terpenes: $current/$total")
        }

        // For UI updates, emit terpene progress
        strains.forEachIndexed { index, _ ->
            emit(ParseStatus.ResolvingTerpenes(index + 1, strains.size))
            delay(50) // Brief delay for UI update
        }

        val menu = DispensaryMenu(
            url = url,
            fetchedAt = Clock.System.now().toEpochMilliseconds(),
            strains = strains
        )

        println("[BudMash] DefaultMenuParser complete with ${strains.size} strains")
        emit(ParseStatus.Complete(menu))
    }

    private suspend fun fetchHtml(url: String): Result<String> {
        return try {
            val response: HttpResponse = httpClient.get(url)
            Result.success(response.bodyAsText())
        } catch (e: Exception) {
            // Retry with exponential backoff
            var lastException = e
            for (attempt in 1..3) {
                delay((1000L * attempt).coerceAtMost(4000))
                try {
                    val response: HttpResponse = httpClient.get(url)
                    return Result.success(response.bodyAsText())
                } catch (retryError: Exception) {
                    lastException = retryError
                }
            }
            Result.failure(lastException)
        }
    }
}
```

**Step 2: Verify compilation**

Run: `./gradlew :shared:compileKotlinAndroid`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/budmash/parser/DefaultMenuParser.kt
git commit -m "feat: implement DefaultMenuParser orchestrating full extraction pipeline"
```

---

## Task 8: Create LLM Config Storage (expect/actual)

**Files:**
- Create: `shared/src/commonMain/kotlin/com/budmash/llm/LlmConfigStorage.kt`
- Create: `shared/src/androidMain/kotlin/com/budmash/llm/LlmConfigStorage.android.kt`
- Create: `shared/src/iosMain/kotlin/com/budmash/llm/LlmConfigStorage.ios.kt`

**Step 1: Create expect declaration**

```kotlin
// shared/src/commonMain/kotlin/com/budmash/llm/LlmConfigStorage.kt
package com.budmash.llm

expect class LlmConfigStorage() {
    fun getApiKey(): String?
    fun setApiKey(key: String)
    fun getModel(): String
    fun setModel(model: String)
    fun getProvider(): LlmProviderType
    fun setProvider(provider: LlmProviderType)
}
```

**Step 2: Create Android actual implementation**

```kotlin
// shared/src/androidMain/kotlin/com/budmash/llm/LlmConfigStorage.android.kt
package com.budmash.llm

import android.content.Context
import android.content.SharedPreferences

actual class LlmConfigStorage {
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        prefs = context.getSharedPreferences("budmash_llm", Context.MODE_PRIVATE)
    }

    actual fun getApiKey(): String? = prefs?.getString("api_key", null)

    actual fun setApiKey(key: String) {
        prefs?.edit()?.putString("api_key", key)?.apply()
    }

    actual fun getModel(): String = prefs?.getString("model", "anthropic/claude-3-haiku") ?: "anthropic/claude-3-haiku"

    actual fun setModel(model: String) {
        prefs?.edit()?.putString("model", model)?.apply()
    }

    actual fun getProvider(): LlmProviderType {
        val name = prefs?.getString("provider", "OPENROUTER") ?: "OPENROUTER"
        return LlmProviderType.valueOf(name)
    }

    actual fun setProvider(provider: LlmProviderType) {
        prefs?.edit()?.putString("provider", provider.name)?.apply()
    }
}
```

**Step 3: Create iOS actual implementation**

```kotlin
// shared/src/iosMain/kotlin/com/budmash/llm/LlmConfigStorage.ios.kt
package com.budmash.llm

import platform.Foundation.NSUserDefaults

actual class LlmConfigStorage {
    private val defaults = NSUserDefaults.standardUserDefaults

    actual fun getApiKey(): String? = defaults.stringForKey("budmash_api_key")

    actual fun setApiKey(key: String) {
        defaults.setObject(key, "budmash_api_key")
    }

    actual fun getModel(): String = defaults.stringForKey("budmash_model") ?: "anthropic/claude-3-haiku"

    actual fun setModel(model: String) {
        defaults.setObject(model, "budmash_model")
    }

    actual fun getProvider(): LlmProviderType {
        val name = defaults.stringForKey("budmash_provider") ?: "OPENROUTER"
        return LlmProviderType.valueOf(name)
    }

    actual fun setProvider(provider: LlmProviderType) {
        defaults.setObject(provider.name, "budmash_provider")
    }
}
```

**Step 4: Create directories and verify compilation**

```bash
mkdir -p shared/src/androidMain/kotlin/com/budmash/llm
mkdir -p shared/src/iosMain/kotlin/com/budmash/llm
```

Run: `./gradlew :shared:build`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/budmash/llm/LlmConfigStorage.kt
git add shared/src/androidMain/kotlin/com/budmash/llm/
git add shared/src/iosMain/kotlin/com/budmash/llm/
git commit -m "feat: add LlmConfigStorage with expect/actual for Android and iOS"
```

---

## Task 9: Wire Up DefaultMenuParser in App

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/budmash/ui/App.kt`

**Step 1: Update App.kt to use DefaultMenuParser**

```kotlin
// Replace MockMenuParser with DefaultMenuParser
// Add imports:
import com.budmash.llm.KtorLlmProvider
import com.budmash.llm.LlmConfig
import com.budmash.llm.LlmProviderType
import com.budmash.parser.DefaultMenuParser

// In App composable, replace:
// val parser = remember { MockMenuParser() }

// With:
val llmProvider = remember { KtorLlmProvider() }
val config = remember {
    LlmConfig(
        provider = LlmProviderType.OPENROUTER,
        apiKey = "", // TODO: Get from storage or environment
        model = "anthropic/claude-3-haiku"
    )
}
val parser = remember { DefaultMenuParser(llmProvider, config) }
```

**Step 2: Build and test**

Run: `./gradlew :composeApp:assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/budmash/ui/App.kt
git commit -m "feat: wire up DefaultMenuParser replacing MockMenuParser"
```

---

## Task 10: Add API Key Configuration UI

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/budmash/ui/screens/SettingsScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/budmash/ui/App.kt`

**Step 1: Create SettingsScreen**

```kotlin
package com.budmash.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    currentApiKey: String,
    currentModel: String,
    onSave: (apiKey: String, model: String) -> Unit,
    onBack: () -> Unit
) {
    var apiKey by remember { mutableStateOf(currentApiKey) }
    var model by remember { mutableStateOf(currentModel) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            label = { Text("OpenRouter API Key") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = model,
            onValueChange = { model = it },
            label = { Text("Model") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Default: anthropic/claude-3-haiku",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }

            Button(
                onClick = { onSave(apiKey, model) },
                modifier = Modifier.weight(1f),
                enabled = apiKey.isNotBlank()
            ) {
                Text("Save")
            }
        }
    }
}
```

**Step 2: Add Settings screen to navigation in App.kt**

Add to Screen sealed class:
```kotlin
data object Settings : Screen()
```

Add settings icon to HomeScreen and handle navigation.

**Step 3: Build and verify**

Run: `./gradlew :composeApp:assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/budmash/ui/screens/SettingsScreen.kt
git add composeApp/src/commonMain/kotlin/com/budmash/ui/App.kt
git commit -m "feat: add SettingsScreen for API key configuration"
```

---

## Task 11: Test Full Flow on Device

**Step 1: Install on device**

Run: `./gradlew :composeApp:installDebug`

**Step 2: Configure API key**

1. Open app
2. Go to Settings
3. Enter OpenRouter API key
4. Save

**Step 3: Test parsing**

1. Enter dispensary URL
2. Tap "Scan Menu"
3. Verify progress through all states
4. Verify strains appear on Dashboard

**Step 4: Verify error handling**

1. Try with invalid URL
2. Try with non-dispensary URL
3. Verify error messages display correctly

---

## Summary

This plan implements:
1. Ktor HTTP client for cross-platform networking
2. LlmProvider interface with OpenRouter/OpenAI/Anthropic support
3. Two-pass LLM extraction (categorize → extract flowers)
4. Terpene resolution via Cannlytics + LLM fallback
5. DefaultMenuParser orchestrating the full pipeline
6. Settings screen for API key configuration
7. Error handling with user-friendly messages
