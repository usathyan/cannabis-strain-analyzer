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
        // Use Gemini Flash for vision (via OpenRouter)
        val visionConfig = config.copy(
            model = "google/gemini-2.0-flash-001"
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
            println("[BudMash] Vision response received, length: ${response.content.length}")
            println("[BudMash] Vision response preview: ${response.content.take(500)}...")

            val extracted = parseResponse(response.content)
            println("[BudMash] Extracted ${extracted.size} strains from screenshot")
            Result.success(extracted)
        } catch (e: Exception) {
            println("[BudMash] Vision extraction error: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private fun parseResponse(content: String): List<StrainData> {
        val jsonContent = extractJson(content)
        println("[BudMash] Extracted JSON: ${jsonContent.take(500)}...")

        return try {
            val parsed = json.decodeFromString<VisionStrainList>(jsonContent)
            parsed.strains.map { it.toStrainData() }
        } catch (e: Exception) {
            println("[BudMash] JSON parse error: ${e.message}")
            // Try to extract individual strain names as fallback
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
Look at this dispensary menu screenshot and extract ALL cannabis flower products visible.

For each flower product, provide:
- name: exact product/strain name as shown (required)
- type: INDICA, SATIVA, or HYBRID (infer from name if not shown)
- thcPercent: THC percentage if visible (number only like 25.5, or null)
- price: price if visible (number only like 45.00, or null)

IMPORTANT RULES:
1. Only extract FLOWER products (not edibles, vapes, concentrates, etc.)
2. Use the EXACT name shown on screen - do not guess or invent names
3. If you cannot clearly read a product name, skip it
4. Extract ALL visible flower products, not just a few

Return ONLY valid JSON (no other text):
{"strains": [{"name": "Blue Dream", "type": "HYBRID", "thcPercent": 24.5, "price": 45.00}]}

If no flower products are visible, return: {"strains": []}
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
