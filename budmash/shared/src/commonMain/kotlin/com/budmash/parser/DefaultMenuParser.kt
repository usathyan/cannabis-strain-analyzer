package com.budmash.parser

import com.budmash.data.DispensaryMenu
import com.budmash.data.StrainData
import com.budmash.data.StrainType
import com.budmash.network.MenuFetcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock

class DefaultMenuParser(
    private val llmExtractor: LlmMenuExtractor,
    private val terpeneResolver: TerpeneResolver
) : MenuParser {

    override fun parseMenu(url: String): Flow<ParseStatus> = flow {
        emit(ParseStatus.Fetching)

        val htmlResult = MenuFetcher.fetchMenuHtml(url)
        if (htmlResult.isFailure) {
            emit(ParseStatus.Error("Failed to fetch: ${htmlResult.exceptionOrNull()?.message}"))
            return@flow
        }

        val html = htmlResult.getOrThrow()
        emit(ParseStatus.FetchComplete(html.length))

        // Extract strains via LLM
        val extracted = try {
            llmExtractor.extractStrainsFromHtml(html)
        } catch (e: Exception) {
            emit(ParseStatus.Error("LLM extraction failed: ${e.message}"))
            return@flow
        }

        val flowerStrains = extracted.filter {
            it.type?.lowercase() in listOf("indica", "sativa", "hybrid", "flower", null)
        }
        emit(ParseStatus.ProductsFound(extracted.size, flowerStrains.size))

        // Resolve terpene data for each strain
        val strains = mutableListOf<StrainData>()
        flowerStrains.forEachIndexed { index, extracted ->
            emit(ParseStatus.ResolvingTerpenes(index + 1, flowerStrains.size))

            val strainData = terpeneResolver.resolve(extracted)
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
