package com.budmash.parser

import com.budmash.data.DispensaryMenu
import com.budmash.data.ParseError
import com.budmash.llm.LlmConfig
import com.budmash.llm.LlmProvider
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock

class DefaultMenuParser(
    private val llmProvider: LlmProvider,
    private val config: LlmConfig
) : MenuParser {

    private val extractor = LlmMenuExtractor(llmProvider)
    private val terpeneResolver = DefaultTerpeneResolver(llmProvider, config)
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
        println("[BudMash] HTML length: ${html.length} chars")
        println("[BudMash] HTML preview (first 500 chars): ${html.take(500)}")
        println("[BudMash] HTML contains 'product': ${html.lowercase().contains("product")}")
        println("[BudMash] HTML contains 'flower': ${html.lowercase().contains("flower")}")
        println("[BudMash] HTML contains 'strain': ${html.lowercase().contains("strain")}")
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
            validateAndExtract(response)
        } catch (e: Exception) {
            // Retry with exponential backoff
            var lastException = e
            for (attempt in 1..3) {
                delay((1000L shl (attempt - 1)).coerceAtMost(4000))
                try {
                    val response: HttpResponse = httpClient.get(url)
                    return validateAndExtract(response)
                } catch (retryError: Exception) {
                    lastException = retryError
                }
            }
            Result.failure(lastException)
        }
    }

    private suspend fun validateAndExtract(response: HttpResponse): Result<String> {
        return if (response.status.isSuccess()) {
            Result.success(response.bodyAsText())
        } else {
            Result.failure(Exception("HTTP ${response.status.value}: ${response.status.description}"))
        }
    }
}
