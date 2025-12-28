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
    private val preprocessor = HtmlPreprocessor()

    suspend fun extractStrains(html: String, config: LlmConfig): Result<List<ExtractedStrain>> {
        // Step 1: Try preprocessor extraction first (fast, no LLM cost)
        val preprocessedCandidates = preprocessor.extractProductText(html)
        println("[BudMash] Preprocessor found ${preprocessedCandidates.size} candidates")

        // If preprocessor found enough products, convert and use them directly
        if (preprocessedCandidates.size >= 5) {
            println("[BudMash] Using preprocessor results directly")
            val flowers = preprocessedCandidates.map { candidate ->
                MenuProduct(
                    name = candidate.name,
                    price = candidate.price,
                    details = listOfNotNull(candidate.category, candidate.details).joinToString("; ")
                )
            }
            return extractDetailedStrains(flowers, config)
        }

        // Step 2: Preprocess HTML to reduce size and focus on content
        val cleanedHtml = preprocessor.preprocess(html)
        println("[BudMash] Cleaned HTML size: ${cleanedHtml.length} (from ${html.length})")

        // Step 3: Use LLM to categorize and extract
        val categorizedResult = categorizeMenu(cleanedHtml, config)
        if (categorizedResult.isFailure) {
            return Result.failure(categorizedResult.exceptionOrNull()!!)
        }

        val categorized = categorizedResult.getOrThrow()

        // Log categories received from LLM
        println("[BudMash] LLM returned categories: ${categorized.categories.keys}")

        // Case-insensitive lookup for flower category
        val flowerKey = categorized.categories.keys.find {
            it.lowercase() in listOf("flower", "flowers", "cannabis", "weed", "bud", "buds")
        }
        val flowers = if (flowerKey != null) categorized.categories[flowerKey]!! else emptyList()

        println("[BudMash] Found ${flowers.size} flowers under key: $flowerKey")

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
                content = """Parse this dispensary menu HTML and extract ALL products into categorized JSON.

CRITICAL RULES:
1. ONLY extract product names that appear EXACTLY in the HTML text
2. Do NOT invent, hallucinate, or guess product names
3. If no products are visible in the HTML, return {"categories": {}}
4. Extract EVERY product you can find - do not limit or summarize

Group products by category (flower, edibles, vapes, concentrates, pre-rolls, tinctures, topicals, etc.)
For each product capture: name (exact text from HTML), category, price, any visible details.

Output valid JSON only:
{"categories": {"flower": [{"name": "...", "price": "...", "details": "..."}], ...}}

If the HTML appears to be a JavaScript app without visible product data, return empty categories."""
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
                content = """Extract detailed strain data for ALL these flower products.
IMPORTANT: Process EVERY flower in the list - do not skip any.

For each strain provide:
- name: exact product name
- type: INDICA, SATIVA, or HYBRID (infer from description if not stated)
- thcMin: minimum THC percentage (number, or null if unknown)
- thcMax: maximum THC percentage (number, or null if unknown)
- price: numeric price in dollars (or null if unknown)
- description: brief description if available

Output valid JSON only: {"strains": [...]}
Include ALL flowers from input, not just a subset."""
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
