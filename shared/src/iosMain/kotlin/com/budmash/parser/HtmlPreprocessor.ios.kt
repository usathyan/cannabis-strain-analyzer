package com.budmash.parser

actual class HtmlPreprocessor {

    actual fun preprocess(html: String): String {
        var cleaned = html

        // Remove script and style tags with content
        cleaned = cleaned.replace(Regex("<script[^>]*>[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), "")
        cleaned = cleaned.replace(Regex("<style[^>]*>[\\s\\S]*?</style>", RegexOption.IGNORE_CASE), "")
        cleaned = cleaned.replace(Regex("<noscript[^>]*>[\\s\\S]*?</noscript>", RegexOption.IGNORE_CASE), "")

        // Remove HTML tags but keep text content
        cleaned = cleaned.replace(Regex("<[^>]+>"), " ")

        // Clean up whitespace
        cleaned = cleaned.replace(Regex("\\s+"), " ").trim()

        return cleaned.take(100_000) // Limit size
    }

    actual fun extractProductText(html: String): List<ProductCandidate> {
        val candidates = mutableListOf<ProductCandidate>()

        // Try to find product-like patterns in the HTML
        // This is a basic implementation - jsoup on Android is more robust

        // Look for product names followed by prices
        val productPattern = Regex(
            """([A-Z][a-zA-Z\s]+(?:OG|Kush|Haze|Diesel|Cookies|Dream|Purple|Blue|White|Green|Zkittlez|Gelato|Runtz))\s*[-–]?\s*(?:\$(\d+(?:\.\d{2})?))?""",
            RegexOption.MULTILINE
        )

        val matches = productPattern.findAll(html)
        val seen = mutableSetOf<String>()

        for (match in matches) {
            val name = match.groupValues[1].trim()
            if (name.isNotBlank() && name !in seen && name.length > 3) {
                seen.add(name)
                candidates.add(
                    ProductCandidate(
                        name = name,
                        price = match.groupValues.getOrNull(2)?.let { "\$$it" },
                        category = detectCategory(name),
                        details = null
                    )
                )
            }
        }

        println("[BudMash] iOS HtmlPreprocessor found ${candidates.size} product candidates")
        return candidates
    }

    private fun detectCategory(name: String): String? {
        val lower = name.lowercase()
        return when {
            "indica" in lower -> "Indica"
            "sativa" in lower -> "Sativa"
            "hybrid" in lower -> "Hybrid"
            else -> null
        }
    }
}
