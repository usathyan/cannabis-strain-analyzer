package com.budmash.parser

import com.budmash.data.DispensaryMenu
import com.budmash.data.ParseError
import com.budmash.data.StrainData
import com.budmash.data.StrainType
import com.budmash.llm.LlmConfig
import com.budmash.network.MenuFetcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock

class DefaultMenuParser(
    private val llmExtractor: LlmMenuExtractor,
    private val terpeneResolver: TerpeneResolver,
    private val llmConfig: LlmConfig
) : MenuParser {

    override fun parseMenu(url: String): Flow<ParseStatus> = flow {
        emit(ParseStatus.Fetching)

        val htmlResult = MenuFetcher.fetchMenuHtml(url)
        if (htmlResult.isFailure) {
            emit(ParseStatus.Error(ParseError.NetworkError(htmlResult.exceptionOrNull()?.message ?: "Unknown error")))
            return@flow
        }

        val html = htmlResult.getOrThrow()
        emit(ParseStatus.FetchComplete(html.length))

        // Extract strains via LLM
        val extractResult = llmExtractor.extractStrains(html, llmConfig)
        if (extractResult.isFailure) {
            emit(ParseStatus.Error(ParseError.LlmError(extractResult.exceptionOrNull()?.message ?: "Unknown error")))
            return@flow
        }

        val extracted = extractResult.getOrThrow()
        val flowerStrains = extracted.filter {
            it.type.lowercase() in listOf("indica", "sativa", "hybrid")
        }
        emit(ParseStatus.ProductsFound(extracted.size, flowerStrains.size))

        // Resolve terpene data for each strain
        val strains = mutableListOf<StrainData>()
        flowerStrains.forEachIndexed { index, strain ->
            emit(ParseStatus.ResolvingTerpenes(index + 1, flowerStrains.size))

            val strainData = terpeneResolver.resolve(strain)
            strains.add(strainData)
        }

        val menu = DispensaryMenu(
            url = url,
            fetchedAt = Clock.System.now().toEpochMilliseconds(),
            strains = strains
        )

        emit(ParseStatus.Complete(menu))
    }
}

interface TerpeneResolver {
    suspend fun resolve(extracted: ExtractedStrain): StrainData
}
