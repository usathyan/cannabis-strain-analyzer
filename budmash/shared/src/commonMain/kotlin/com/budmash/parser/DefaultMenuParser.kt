package com.budmash.parser

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
    private val config: LlmConfig,
    private val visionModel: String = "google/gemini-2.0-flash-001"
) : MenuParser {

    private val visionExtractor = VisionMenuExtractor(llmProvider)
    private val terpeneResolver = DefaultTerpeneResolver(llmProvider, config)

    override fun parseMenu(url: String): Flow<ParseStatus> = flow {
        // URL-based parsing is deprecated - use parseFromImage instead
        emit(ParseStatus.Error(ParseError.NetworkError("URL parsing is deprecated. Please use photo capture instead.")))
    }

    override fun parseFromImage(imageBase64: String): Flow<ParseStatus> = flow {
        println("[BudMash] DefaultMenuParser starting for image, base64 length: ${imageBase64.length}")

        emit(ParseStatus.Fetching)
        emit(ParseStatus.FetchComplete(imageBase64.length))

        // Step 1: Extract strains via vision LLM
        println("[BudMash] Sending image to vision LLM for extraction using model: $visionModel")
        val strainsResult = visionExtractor.extractFromScreenshot(imageBase64, config, visionModel)

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
