package com.budmash.parser

import com.budmash.capture.CaptureResult
import com.budmash.capture.CaptureStatus
import com.budmash.capture.ScreenshotCapture
import com.budmash.data.DispensaryMenu
import com.budmash.data.ParseError
import com.budmash.llm.LlmConfig
import com.budmash.llm.LlmProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock

class DefaultMenuParser(
    private val llmProvider: LlmProvider,
    private val config: LlmConfig
) : MenuParser {

    private val visionExtractor = VisionMenuExtractor(llmProvider)
    private val terpeneResolver = DefaultTerpeneResolver(llmProvider, config)
    private val screenshotCapture = ScreenshotCapture()

    override fun parseMenu(url: String): Flow<ParseStatus> = flow {
        println("[BudMash] DefaultMenuParser starting for URL: $url")

        emit(ParseStatus.Fetching)

        // Step 1: Capture screenshot of the webpage
        var screenshotResult: CaptureResult? = null
        var captureError: String? = null

        screenshotCapture.capture(url).collect { status ->
            when (status) {
                is CaptureStatus.Loading -> {
                    println("[BudMash] Screenshot capture starting...")
                }
                is CaptureStatus.Progress -> {
                    println("[BudMash] Screenshot capture progress: ${status.percent}%")
                }
                is CaptureStatus.Success -> {
                    screenshotResult = status.result
                    println("[BudMash] Screenshot captured: ${status.result.width}x${status.result.height}")
                }
                is CaptureStatus.Error -> {
                    captureError = status.message
                    println("[BudMash] Screenshot capture error: ${status.message}")
                }
            }
        }

        // Check for capture errors
        if (captureError != null) {
            emit(ParseStatus.Error(ParseError.NetworkError("Screenshot capture failed: $captureError")))
            return@flow
        }

        val screenshot = screenshotResult
        if (screenshot == null) {
            emit(ParseStatus.Error(ParseError.NetworkError("Screenshot capture returned no result")))
            return@flow
        }

        println("[BudMash] Screenshot base64 length: ${screenshot.base64Image.length}")
        emit(ParseStatus.FetchComplete(screenshot.base64Image.length))

        // Step 2: Extract strains via vision LLM
        println("[BudMash] Sending screenshot to vision LLM for extraction...")
        val strainsResult = visionExtractor.extractFromScreenshot(screenshot.base64Image, config)

        if (strainsResult.isFailure) {
            emit(ParseStatus.Error(ParseError.LlmError(strainsResult.exceptionOrNull()?.message ?: "Vision extraction failed")))
            return@flow
        }

        var strains = strainsResult.getOrThrow()
        println("[BudMash] Vision extracted ${strains.size} strains")
        emit(ParseStatus.ProductsFound(strains.size, strains.size))

        if (strains.isEmpty()) {
            emit(ParseStatus.Error(ParseError.NoFlowersFound))
            return@flow
        }

        // Step 3: Resolve terpenes for each strain
        strains = terpeneResolver.resolveAll(strains, config) { current, total ->
            println("[BudMash] Resolving terpenes: $current/$total")
        }

        // Emit terpene progress for UI updates
        strains.forEachIndexed { index, _ ->
            emit(ParseStatus.ResolvingTerpenes(index + 1, strains.size))
            delay(50)
        }

        // Step 4: Build and return menu
        val menu = DispensaryMenu(
            url = url,
            fetchedAt = Clock.System.now().toEpochMilliseconds(),
            strains = strains
        )

        println("[BudMash] DefaultMenuParser complete with ${strains.size} strains")
        emit(ParseStatus.Complete(menu))
    }

    override fun parseFromImage(imageBase64: String): Flow<ParseStatus> = flow {
        println("[BudMash] DefaultMenuParser starting for image, base64 length: ${imageBase64.length}")

        emit(ParseStatus.Fetching)
        emit(ParseStatus.FetchComplete(imageBase64.length))

        // Step 1: Extract strains via vision LLM
        println("[BudMash] Sending image to vision LLM for extraction...")
        val strainsResult = visionExtractor.extractFromScreenshot(imageBase64, config)

        if (strainsResult.isFailure) {
            emit(ParseStatus.Error(ParseError.LlmError(strainsResult.exceptionOrNull()?.message ?: "Vision extraction failed")))
            return@flow
        }

        var strains = strainsResult.getOrThrow()
        println("[BudMash] Vision extracted ${strains.size} strains")
        emit(ParseStatus.ProductsFound(strains.size, strains.size))

        if (strains.isEmpty()) {
            emit(ParseStatus.Error(ParseError.NoFlowersFound))
            return@flow
        }

        // Step 2: Resolve terpenes for each strain
        strains = terpeneResolver.resolveAll(strains, config) { current, total ->
            println("[BudMash] Resolving terpenes: $current/$total")
        }

        strains.forEachIndexed { index, _ ->
            emit(ParseStatus.ResolvingTerpenes(index + 1, strains.size))
            delay(50)
        }

        // Step 3: Build and return menu
        val menu = DispensaryMenu(
            url = "Photo capture",
            fetchedAt = Clock.System.now().toEpochMilliseconds(),
            strains = strains
        )

        println("[BudMash] DefaultMenuParser complete with ${strains.size} strains")
        emit(ParseStatus.Complete(menu))
    }
}
