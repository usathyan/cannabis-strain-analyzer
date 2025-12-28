# Vision-Based Menu Extraction MVP

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Replace broken HTML extraction with screenshot + Gemini Flash vision extraction that works for JavaScript-heavy dispensary sites like Dutchie.

**Architecture:** Use Android WebView to render dispensary pages, capture screenshots, send to Gemini Flash via OpenRouter for vision-based strain extraction. Also fix like/dislike button click propagation issue.

**Tech Stack:** Kotlin Multiplatform, Android WebView, OpenRouter API (Gemini Flash), Compose Multiplatform

---

## Task 1: Fix Like/Dislike Button Click Propagation

**Problem:** IconButton clicks are consumed by parent Card's clickable modifier.

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/budmash/ui/screens/DashboardScreen.kt:92-180`

**Step 1: Update StrainCard to stop click propagation**

The issue is that `Card(modifier = Modifier.clickable { onClick() })` captures all clicks including those on child IconButtons. Fix by removing clickable from Card and adding a separate clickable area.

```kotlin
@Composable
private fun StrainCard(
    result: SimilarityResult,
    isLiked: Boolean = false,
    isDisliked: Boolean = false,
    hasProfile: Boolean,
    onClick: () -> Unit,
    onLike: () -> Unit,
    onDislike: () -> Unit
) {
    val cardColor = when {
        isLiked -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        isDisliked -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        else -> MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Clickable content area (strain info)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onClick() }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = result.strain.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (isLiked) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Liked",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    } else if (isDisliked) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Disliked",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Text(
                    text = result.strain.type.name,
                    style = MaterialTheme.typography.bodySmall
                )
                val topTerpenes = result.strain.terpeneProfile()
                    .zip(StrainData.TERPENE_NAMES)
                    .sortedByDescending { it.first }
                    .take(3)
                    .filter { it.first > 0 }
                    .joinToString(", ") { "${it.second} ${round(it.first * 100) / 100}" }
                if (topTerpenes.isNotEmpty()) {
                    Text(
                        text = topTerpenes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Match score (if profile exists)
            if (hasProfile) {
                Text(
                    text = "${(result.overallScore * 100).toInt()}%",
                    style = MaterialTheme.typography.headlineSmall,
                    color = when {
                        result.overallScore >= 0.8 -> MaterialTheme.colorScheme.primary
                        result.overallScore >= 0.6 -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            // Like/Dislike buttons - NOT inside clickable area
            Row {
                IconButton(onClick = onLike) {
                    Icon(
                        Icons.Default.Check,
                        "Like",
                        tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDislike) {
                    Icon(
                        Icons.Default.Close,
                        "Dislike",
                        tint = if (isDisliked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
```

**Step 2: Build and test**

Run: `JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./gradlew assembleDebug`
Install and verify like/dislike buttons respond to clicks.

**Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/budmash/ui/screens/DashboardScreen.kt
git commit -m "fix: like/dislike button click propagation"
```

---

## Task 2: Add Vision Message Support to LLM Types

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/budmash/llm/LlmProvider.kt`

**Step 1: Add multimodal message types**

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

// Legacy text-only message
@Serializable
data class LlmMessage(
    val role: String,
    val content: String
)

// Multimodal content types
sealed class MessageContent {
    data class Text(val text: String) : MessageContent()
    data class ImageBase64(val base64: String, val mediaType: String = "image/png") : MessageContent()
    data class ImageUrl(val url: String) : MessageContent()
}

// Multimodal message
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

    // New: Vision-capable completion
    suspend fun completeVision(messages: List<MultimodalMessage>, config: LlmConfig): LlmResponse
}
```

**Step 2: Commit**

```bash
git add shared/src/commonMain/kotlin/com/budmash/llm/LlmProvider.kt
git commit -m "feat: add multimodal message types for vision"
```

---

## Task 3: Implement Vision Completion in KtorLlmProvider

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/budmash/llm/KtorLlmProvider.kt`

**Step 1: Add vision completion method**

Add after the existing `complete` method:

```kotlin
override suspend fun completeVision(messages: List<MultimodalMessage>, config: LlmConfig): LlmResponse {
    return when (config.provider) {
        LlmProviderType.OPENROUTER -> completeVisionOpenRouter(messages, config)
        else -> throw IllegalArgumentException("Vision not supported for ${config.provider}")
    }
}

private suspend fun completeVisionOpenRouter(messages: List<MultimodalMessage>, config: LlmConfig): LlmResponse {
    val baseUrl = config.baseUrl ?: "https://openrouter.ai/api/v1"

    val openAIMessages = messages.map { msg ->
        OpenAIVisionMessage(
            role = msg.role,
            content = msg.content.map { content ->
                when (content) {
                    is MessageContent.Text -> OpenAIContentPart.TextPart(text = content.text)
                    is MessageContent.ImageBase64 -> OpenAIContentPart.ImagePart(
                        image_url = OpenAIImageUrl(
                            url = "data:${content.mediaType};base64,${content.base64}"
                        )
                    )
                    is MessageContent.ImageUrl -> OpenAIContentPart.ImagePart(
                        image_url = OpenAIImageUrl(url = content.url)
                    )
                }
            }
        )
    }

    val request = OpenAIVisionRequest(
        model = config.model,
        messages = openAIMessages,
        max_tokens = config.maxTokens,
        temperature = config.temperature
    )

    val response: OpenAIResponse = client.post("$baseUrl/chat/completions") {
        contentType(ContentType.Application.Json)
        header("Authorization", "Bearer ${config.apiKey}")
        header("HTTP-Referer", "https://budmash.app")
        header("X-Title", "BudMash")
        setBody(request)
    }.body()

    return LlmResponse(
        content = response.choices.firstOrNull()?.message?.content ?: "",
        tokensUsed = response.usage?.total_tokens ?: 0
    )
}
```

**Step 2: Add vision request/response models**

Add these serializable classes at the bottom of the file:

```kotlin
// Vision request models
@Serializable
private data class OpenAIVisionRequest(
    val model: String,
    val messages: List<OpenAIVisionMessage>,
    val max_tokens: Int,
    val temperature: Float
)

@Serializable
private data class OpenAIVisionMessage(
    val role: String,
    val content: List<OpenAIContentPart>
)

@Serializable
private sealed class OpenAIContentPart {
    @Serializable
    @kotlinx.serialization.SerialName("text")
    data class TextPart(
        val type: String = "text",
        val text: String
    ) : OpenAIContentPart()

    @Serializable
    @kotlinx.serialization.SerialName("image_url")
    data class ImagePart(
        val type: String = "image_url",
        val image_url: OpenAIImageUrl
    ) : OpenAIContentPart()
}

@Serializable
private data class OpenAIImageUrl(
    val url: String,
    val detail: String = "auto"
)
```

**Step 3: Build to verify no compile errors**

Run: `JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./gradlew compileKotlinAndroid`

**Step 4: Commit**

```bash
git add shared/src/commonMain/kotlin/com/budmash/llm/KtorLlmProvider.kt
git commit -m "feat: implement vision completion for OpenRouter"
```

---

## Task 4: Create WebView Screenshot Capture (Android)

**Files:**
- Create: `shared/src/commonMain/kotlin/com/budmash/capture/ScreenshotCapture.kt`
- Create: `shared/src/androidMain/kotlin/com/budmash/capture/ScreenshotCapture.android.kt`
- Create: `shared/src/iosMain/kotlin/com/budmash/capture/ScreenshotCapture.ios.kt`

**Step 1: Create expect class**

`shared/src/commonMain/kotlin/com/budmash/capture/ScreenshotCapture.kt`:

```kotlin
package com.budmash.capture

import kotlinx.coroutines.flow.Flow

data class CaptureResult(
    val base64Image: String,
    val width: Int,
    val height: Int
)

sealed class CaptureStatus {
    data object Loading : CaptureStatus()
    data class Progress(val percent: Int) : CaptureStatus()
    data class Success(val result: CaptureResult) : CaptureStatus()
    data class Error(val message: String) : CaptureStatus()
}

expect class ScreenshotCapture {
    fun capture(url: String): Flow<CaptureStatus>
}
```

**Step 2: Create Android implementation**

`shared/src/androidMain/kotlin/com/budmash/capture/ScreenshotCapture.android.kt`:

```kotlin
package com.budmash.capture

import android.graphics.Bitmap
import android.util.Base64
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.io.ByteArrayOutputStream

actual class ScreenshotCapture {

    actual fun capture(url: String): Flow<CaptureStatus> = callbackFlow {
        trySend(CaptureStatus.Loading)

        val context = ScreenshotCaptureContext.applicationContext
            ?: throw IllegalStateException("ScreenshotCapture not initialized")

        val webView = WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            // Set a reasonable viewport size
            layoutParams = android.view.ViewGroup.LayoutParams(1080, 1920)
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, loadedUrl: String?) {
                // Wait a bit for JavaScript to render
                view?.postDelayed({
                    try {
                        trySend(CaptureStatus.Progress(80))

                        // Capture the visible content
                        val bitmap = Bitmap.createBitmap(
                            webView.width,
                            webView.height,
                            Bitmap.Config.ARGB_8888
                        )
                        val canvas = android.graphics.Canvas(bitmap)
                        webView.draw(canvas)

                        // Convert to base64
                        val outputStream = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                        val base64 = Base64.encodeToString(
                            outputStream.toByteArray(),
                            Base64.NO_WRAP
                        )

                        trySend(CaptureStatus.Success(CaptureResult(
                            base64Image = base64,
                            width = bitmap.width,
                            height = bitmap.height
                        )))

                        bitmap.recycle()
                        webView.destroy()
                        close()
                    } catch (e: Exception) {
                        trySend(CaptureStatus.Error("Screenshot failed: ${e.message}"))
                        close()
                    }
                }, 3000) // Wait 3 seconds for JS to render
            }

            override fun onReceivedError(
                view: WebView?,
                errorCode: Int,
                description: String?,
                failingUrl: String?
            ) {
                trySend(CaptureStatus.Error("Failed to load: $description"))
                close()
            }
        }

        trySend(CaptureStatus.Progress(10))
        webView.loadUrl(url)

        awaitClose {
            webView.destroy()
        }
    }
}

object ScreenshotCaptureContext {
    var applicationContext: android.content.Context? = null

    fun init(context: android.content.Context) {
        applicationContext = context.applicationContext
    }
}
```

**Step 3: Create iOS stub**

`shared/src/iosMain/kotlin/com/budmash/capture/ScreenshotCapture.ios.kt`:

```kotlin
package com.budmash.capture

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

actual class ScreenshotCapture {
    actual fun capture(url: String): Flow<CaptureStatus> = flow {
        emit(CaptureStatus.Error("Screenshot capture not yet implemented for iOS"))
    }
}
```

**Step 4: Initialize context in MainActivity**

Add to `composeApp/src/androidMain/kotlin/com/budmash/MainActivity.kt`:

```kotlin
import com.budmash.capture.ScreenshotCaptureContext

// In onCreate, after other inits:
ScreenshotCaptureContext.init(this)
```

**Step 5: Build and verify**

Run: `JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./gradlew compileKotlinAndroid`

**Step 6: Commit**

```bash
git add shared/src/commonMain/kotlin/com/budmash/capture/
git add shared/src/androidMain/kotlin/com/budmash/capture/
git add shared/src/iosMain/kotlin/com/budmash/capture/
git add composeApp/src/androidMain/kotlin/com/budmash/MainActivity.kt
git commit -m "feat: add WebView screenshot capture for Android"
```

---

## Task 5: Create Vision Menu Extractor

**Files:**
- Create: `shared/src/commonMain/kotlin/com/budmash/parser/VisionMenuExtractor.kt`

**Step 1: Create the extractor**

```kotlin
package com.budmash.parser

import com.budmash.data.StrainData
import com.budmash.data.StrainType
import com.budmash.llm.LlmConfig
import com.budmash.llm.LlmProvider
import com.budmash.llm.MessageContent
import com.budmash.llm.MultimodalMessage
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class VisionMenuExtractor(
    private val llmProvider: LlmProvider
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun extractFromScreenshot(
        base64Image: String,
        config: LlmConfig
    ): Result<List<StrainData>> {
        val visionConfig = config.copy(
            model = "google/gemini-2.0-flash-001"  // Use Gemini Flash for vision
        )

        val messages = listOf(
            MultimodalMessage(
                role = "user",
                content = listOf(
                    MessageContent.ImageBase64(base64Image, "image/jpeg"),
                    MessageContent.Text(EXTRACTION_PROMPT)
                )
            )
        )

        return try {
            println("[BudMash] Sending screenshot to Gemini Flash for extraction...")
            val response = llmProvider.completeVision(messages, visionConfig)
            println("[BudMash] Vision response: ${response.content.take(500)}...")

            val extracted = parseResponse(response.content)
            println("[BudMash] Extracted ${extracted.size} strains from screenshot")
            Result.success(extracted)
        } catch (e: Exception) {
            println("[BudMash] Vision extraction error: ${e.message}")
            Result.failure(e)
        }
    }

    private fun parseResponse(content: String): List<StrainData> {
        // Extract JSON from response
        val jsonContent = extractJson(content)

        return try {
            val parsed = json.decodeFromString<VisionStrainList>(jsonContent)
            parsed.strains.map { it.toStrainData() }
        } catch (e: Exception) {
            println("[BudMash] JSON parse error: ${e.message}")
            emptyList()
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

    companion object {
        private val EXTRACTION_PROMPT = """
Look at this dispensary menu screenshot and extract ALL flower/cannabis products you can see.

For each product, provide:
- name: exact product name as shown
- type: INDICA, SATIVA, or HYBRID (infer if not shown)
- thcPercent: THC percentage if visible (number only, e.g., 25.5)
- price: price if visible (number only, e.g., 45.00)

Return ONLY valid JSON in this format:
{"strains": [{"name": "...", "type": "...", "thcPercent": null, "price": null}, ...]}

Rules:
- Extract EVERY flower product visible on screen
- Use exact names as displayed
- If THC or price not visible, use null
- Only include actual cannabis flower products, not edibles/vapes/etc
""".trimIndent()
    }
}

@Serializable
private data class VisionStrainList(
    val strains: List<VisionStrain>
)

@Serializable
private data class VisionStrain(
    val name: String,
    val type: String? = null,
    val thcPercent: Double? = null,
    val price: Double? = null
) {
    fun toStrainData(): StrainData = StrainData(
        name = name,
        type = when (type?.uppercase()) {
            "INDICA" -> StrainType.INDICA
            "SATIVA" -> StrainType.SATIVA
            else -> StrainType.HYBRID
        },
        thcMin = thcPercent ?: 0.0,
        thcMax = thcPercent ?: 0.0,
        price = price ?: 0.0,
        description = ""
    )
}
```

**Step 2: Build and verify**

Run: `JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./gradlew compileKotlinAndroid`

**Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/budmash/parser/VisionMenuExtractor.kt
git commit -m "feat: add vision-based menu extractor using Gemini Flash"
```

---

## Task 6: Integrate Vision Extraction into DefaultMenuParser

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/budmash/parser/DefaultMenuParser.kt`

**Step 1: Update DefaultMenuParser to use screenshot capture**

```kotlin
package com.budmash.parser

import com.budmash.capture.CaptureStatus
import com.budmash.capture.ScreenshotCapture
import com.budmash.data.DispensaryMenu
import com.budmash.data.ParseError
import com.budmash.llm.LlmConfig
import com.budmash.llm.LlmProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock

class DefaultMenuParser(
    private val llmProvider: LlmProvider,
    private val config: LlmConfig
) : MenuParser {

    private val visionExtractor = VisionMenuExtractor(llmProvider)
    private val screenshotCapture = ScreenshotCapture()
    private val terpeneResolver = DefaultTerpeneResolver(llmProvider, config)

    override fun parseMenu(url: String): Flow<ParseStatus> = flow {
        println("[BudMash] DefaultMenuParser starting for URL: $url")

        emit(ParseStatus.Fetching)

        // Step 1: Capture screenshot
        println("[BudMash] Capturing screenshot...")
        var captureResult: com.budmash.capture.CaptureResult? = null

        screenshotCapture.capture(url).collect { status ->
            when (status) {
                is CaptureStatus.Loading -> {
                    println("[BudMash] WebView loading...")
                }
                is CaptureStatus.Progress -> {
                    println("[BudMash] Capture progress: ${status.percent}%")
                }
                is CaptureStatus.Success -> {
                    println("[BudMash] Screenshot captured: ${status.result.width}x${status.result.height}")
                    captureResult = status.result
                }
                is CaptureStatus.Error -> {
                    println("[BudMash] Capture error: ${status.message}")
                    emit(ParseStatus.Error(ParseError.NetworkError(status.message)))
                    return@collect
                }
            }
        }

        val screenshot = captureResult ?: run {
            emit(ParseStatus.Error(ParseError.NetworkError("Failed to capture screenshot")))
            return@flow
        }

        emit(ParseStatus.FetchComplete(screenshot.base64Image.length))

        // Step 2: Extract strains via vision
        println("[BudMash] Extracting strains from screenshot...")
        val strainsResult = visionExtractor.extractFromScreenshot(screenshot.base64Image, config)

        if (strainsResult.isFailure) {
            emit(ParseStatus.Error(ParseError.LlmError(
                strainsResult.exceptionOrNull()?.message ?: "Vision extraction failed"
            )))
            return@flow
        }

        var strains = strainsResult.getOrThrow()
        println("[BudMash] Vision extracted ${strains.size} strains")

        if (strains.isEmpty()) {
            emit(ParseStatus.Error(ParseError.NoFlowersFound))
            return@flow
        }

        emit(ParseStatus.ProductsFound(strains.size, strains.size))

        // Step 3: Resolve terpenes (optional, can skip for MVP)
        // strains = terpeneResolver.resolveAll(strains, config) { current, total ->
        //     println("[BudMash] Resolving terpenes: $current/$total")
        // }

        val menu = DispensaryMenu(
            url = url,
            fetchedAt = Clock.System.now().toEpochMilliseconds(),
            strains = strains
        )

        println("[BudMash] DefaultMenuParser complete with ${strains.size} strains")
        emit(ParseStatus.Complete(menu))
    }
}
```

**Step 2: Build full project**

Run: `JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./gradlew assembleDebug`

**Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/budmash/parser/DefaultMenuParser.kt
git commit -m "feat: integrate vision extraction into menu parser"
```

---

## Task 7: Test End-to-End

**Step 1: Install and run**

```bash
~/Library/Android/sdk/platform-tools/adb install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk
```

**Step 2: Test the flow**

1. Open app
2. Enter `https://auntmarysnj.co/order-online/`
3. Tap "Scan Menu"
4. Wait for screenshot capture and extraction
5. Verify strains are displayed
6. Tap like/dislike buttons and verify they work

**Step 3: Check logs**

```bash
adb logcat | grep -i budmash
```

**Step 4: Commit if working**

```bash
git add -A
git commit -m "feat: vision-based menu extraction MVP complete"
```

---

## Summary

| Task | Description | Status |
|------|-------------|--------|
| 1 | Fix like/dislike click propagation | Pending |
| 2 | Add vision message types | Pending |
| 3 | Implement vision completion | Pending |
| 4 | Create screenshot capture | Pending |
| 5 | Create vision extractor | Pending |
| 6 | Integrate into parser | Pending |
| 7 | Test end-to-end | Pending |
