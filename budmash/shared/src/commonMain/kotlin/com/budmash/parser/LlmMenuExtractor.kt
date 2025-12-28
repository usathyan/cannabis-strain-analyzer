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
    val type: String? = null,
    val thcMin: Double? = null,
    val thcMax: Double? = null,
    val price: Double? = null,
    val description: String? = null
) {
    fun toStrainData(): StrainData = StrainData(
        name = name,
        type = when (type?.uppercase()) {
            "INDICA" -> StrainType.INDICA
            "SATIVA" -> StrainType.SATIVA
            else -> StrainType.HYBRID
        },
        thcMin = thcMin ?: 0.0,
        thcMax = thcMax ?: 0.0,
        price = price ?: 0.0,
        description = description ?: ""
    )
}
