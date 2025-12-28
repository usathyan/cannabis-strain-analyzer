package com.budmash.parser

import com.budmash.data.StrainData
import com.budmash.data.StrainType
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

/**
 * Interface for resolving terpene profiles for extracted strains.
 */
interface TerpeneResolver {
    suspend fun resolve(extracted: ExtractedStrain): StrainData
}

/**
 * Default implementation of TerpeneResolver that uses Cannlytics API
 * with LLM fallback for terpene profile resolution.
 */
class DefaultTerpeneResolver(
    private val llmProvider: LlmProvider,
    private val llmConfig: LlmConfig
) : TerpeneResolver {

    private val json = Json { ignoreUnknownKeys = true }

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(json)
        }
    }

    override suspend fun resolve(extracted: ExtractedStrain): StrainData {
        val strainData = extracted.toStrainData()
        return resolveTerpenes(strainData, llmConfig)
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
