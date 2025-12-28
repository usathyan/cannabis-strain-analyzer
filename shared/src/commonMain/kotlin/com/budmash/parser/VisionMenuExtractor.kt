package com.budmash.parser

import com.budmash.capture.ImageChunker
import com.budmash.data.StrainData
import com.budmash.data.StrainType
import com.budmash.llm.LlmConfig
import com.budmash.llm.LlmMessage
import com.budmash.llm.LlmProvider
import com.budmash.llm.MessageContent
import com.budmash.llm.MultimodalMessage
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class VisionMenuExtractor(
    private val llmProvider: LlmProvider
) {
    private val imageChunker = ImageChunker()
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun extractFromScreenshot(
        base64Image: String,
        config: LlmConfig,
        visionModel: String = "google/gemini-2.0-flash-001"
    ): Result<List<StrainData>> {
        // Check if we need to chunk the image (for tall scroll screenshots)
        val dimensions = imageChunker.getImageDimensions(base64Image)
        println("[BudMash] Image dimensions: $dimensions")

        val chunks = imageChunker.chunkImage(base64Image)
        println("[BudMash] Processing ${chunks.size} chunk(s)")

        if (chunks.size > 1) {
            // Process multiple chunks and merge results
            return extractFromChunks(chunks, config, visionModel)
        }

        // Single image processing (original logic)
        return extractSingleImage(base64Image, config, visionModel)
    }

    private suspend fun extractFromChunks(
        chunks: List<String>,
        config: LlmConfig,
        visionModel: String
    ): Result<List<StrainData>> {
        val allStrains = mutableListOf<StrainData>()
        val seenNames = mutableSetOf<String>()

        for ((index, chunk) in chunks.withIndex()) {
            println("[BudMash] Processing chunk ${index + 1}/${chunks.size}...")
            val result = extractSingleImage(chunk, config, visionModel)

            if (result.isSuccess) {
                val strains = result.getOrThrow()
                // Deduplicate by name (overlap regions may capture same strain)
                for (strain in strains) {
                    val normalizedName = strain.name.lowercase().trim()
                    if (normalizedName !in seenNames) {
                        seenNames.add(normalizedName)
                        allStrains.add(strain)
                        println("[BudMash] Added strain: ${strain.name}")
                    } else {
                        println("[BudMash] Skipping duplicate: ${strain.name}")
                    }
                }
                println("[BudMash] Chunk ${index + 1} added ${strains.size} strains (${allStrains.size} total unique)")
            } else {
                println("[BudMash] Chunk ${index + 1} failed: ${result.exceptionOrNull()?.message}")
            }
        }

        println("[BudMash] All chunks processed: ${allStrains.size} unique strains total")
        return Result.success(allStrains)
    }

    private suspend fun extractSingleImage(
        base64Image: String,
        config: LlmConfig,
        visionModel: String
    ): Result<List<StrainData>> {
        // Use configured vision model (via OpenRouter)
        // Increase maxTokens to ensure complete JSON response
        val visionConfig = config.copy(
            model = visionModel,
            maxTokens = 8192
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
            println("[BudMash] Sending image to vision model for extraction...")
            val response = llmProvider.completeVision(messages, visionConfig)
            println("[BudMash] Vision response received, length: ${response.content.length}")
            println("[BudMash] Vision response preview: ${response.content.take(500)}...")

            var extracted = parseResponse(response.content)
            println("[BudMash] Extracted ${extracted.size} strains from image")

            // If regex recovery failed but we have content, try AI cleanup
            if (extracted.isEmpty() && response.content.length > 100) {
                println("[BudMash] Regex recovery failed, attempting AI cleanup...")
                extracted = cleanupWithAI(response.content, config)
                println("[BudMash] AI cleanup extracted ${extracted.size} strains")
            }

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
            // Try to recover partial results from truncated JSON using regex
            recoverPartialStrains(jsonContent)
        }
    }

    private suspend fun cleanupWithAI(rawResponse: String, config: LlmConfig): List<StrainData> {
        println("[BudMash] Using AI cleanup for truncated response...")
        val cleanupPrompt = """
The following is a truncated JSON response containing cannabis strain data. Extract all strain names and types you can find.

Response to parse:
$rawResponse

Return ONLY a simple JSON array with name and type for each strain found:
[{"name": "Strain Name", "type": "INDICA/SATIVA/HYBRID"}]

If a type is unclear, use HYBRID. Return [] if no strains found.
""".trimIndent()

        return try {
            val messages = listOf(
                LlmMessage("user", cleanupPrompt)
            )
            val cleanupConfig = config.copy(maxTokens = 2048)
            val response = llmProvider.complete(messages, cleanupConfig)
            println("[BudMash] AI cleanup response: ${response.content.take(300)}")

            val cleanJson = extractJson(response.content)
            val parsed = json.decodeFromString<List<SimpleStrain>>(cleanJson)
            parsed.map { strain ->
                StrainData(
                    name = strain.name,
                    type = when (strain.type.uppercase()) {
                        "INDICA" -> StrainType.INDICA
                        "SATIVA" -> StrainType.SATIVA
                        else -> StrainType.HYBRID
                    },
                    thcMin = 0.0,
                    thcMax = 0.0,
                    price = 0.0,
                    description = ""
                )
            }
        } catch (e: Exception) {
            println("[BudMash] AI cleanup failed: ${e.message}")
            emptyList()
        }
    }

    private fun recoverPartialStrains(jsonContent: String): List<StrainData> {
        println("[BudMash] Attempting to recover partial strains from truncated JSON...")
        val strains = mutableListOf<StrainData>()

        // Strategy 1: Full object pattern with all fields (handles null and numbers)
        val fullPattern = Regex("""\{\s*"name"\s*:\s*"([^"]+)"\s*,\s*"type"\s*:\s*"([^"]+)"\s*,\s*"thcPercent"\s*:\s*([\d.]+|null)\s*,\s*"price"\s*:\s*([\d.]+|null)\s*\}""")
        fullPattern.findAll(jsonContent).forEach { match ->
            try {
                val name = match.groupValues[1]
                val type = match.groupValues[2]
                val thcStr = match.groupValues[3]
                val priceStr = match.groupValues[4]
                strains.add(createStrainData(name, type, thcStr, priceStr))
                println("[BudMash] Strategy 1 recovered: $name")
            } catch (e: Exception) {
                println("[BudMash] Strategy 1 failed for match: ${e.message}")
            }
        }

        if (strains.isNotEmpty()) {
            println("[BudMash] Strategy 1 recovered ${strains.size} strains")
            return strains
        }

        // Strategy 2: Just name and type (more lenient)
        println("[BudMash] Trying Strategy 2: name and type only...")
        val nameTypePattern = Regex(""""name"\s*:\s*"([^"]+)"\s*,\s*"type"\s*:\s*"([^"]+)"""")
        nameTypePattern.findAll(jsonContent).forEach { match ->
            try {
                val name = match.groupValues[1]
                val type = match.groupValues[2]
                strains.add(createStrainData(name, type, null, null))
                println("[BudMash] Strategy 2 recovered: $name")
            } catch (e: Exception) {
                println("[BudMash] Strategy 2 failed: ${e.message}")
            }
        }

        if (strains.isNotEmpty()) {
            println("[BudMash] Strategy 2 recovered ${strains.size} strains")
            return strains
        }

        // Strategy 3: Just names (last resort)
        println("[BudMash] Trying Strategy 3: names only...")
        val nameOnlyPattern = Regex(""""name"\s*:\s*"([^"]+)"""")
        nameOnlyPattern.findAll(jsonContent).forEach { match ->
            try {
                val name = match.groupValues[1]
                // Skip if it looks like a field name
                if (name.length > 2 && !name.contains("strain") && !name.contains("type")) {
                    strains.add(createStrainData(name, "HYBRID", null, null))
                    println("[BudMash] Strategy 3 recovered: $name")
                }
            } catch (e: Exception) {
                println("[BudMash] Strategy 3 failed: ${e.message}")
            }
        }

        println("[BudMash] Final recovery: ${strains.size} strains")
        return strains
    }

    private fun createStrainData(name: String, type: String, thcStr: String?, priceStr: String?): StrainData {
        val thcPercent = thcStr?.let { if (it == "null") null else it.toDoubleOrNull() }
        val price = priceStr?.let { if (it == "null") null else it.toDoubleOrNull() }
        val strainType = when (type.uppercase()) {
            "INDICA" -> StrainType.INDICA
            "SATIVA" -> StrainType.SATIVA
            else -> StrainType.HYBRID
        }
        return StrainData(
            name = name,
            type = strainType,
            thcMin = thcPercent ?: 0.0,
            thcMax = thcPercent ?: 0.0,
            price = price ?: 0.0,
            description = ""
        )
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

@Serializable
private data class SimpleStrain(
    val name: String,
    val type: String = "HYBRID"
)
