package com.budmash.parser

/**
 * Platform-specific HTML preprocessor that extracts relevant product data
 * from raw HTML before sending to LLM. This reduces token usage and improves
 * extraction quality.
 */
expect class HtmlPreprocessor() {
    /**
     * Preprocess HTML to extract product-relevant content.
     * Removes scripts, styles, navigation, and other non-product content.
     * Returns cleaned HTML with just product information.
     */
    fun preprocess(html: String): String

    /**
     * Extract potential product elements as structured text.
     * Looks for common patterns like product cards, menu items, etc.
     */
    fun extractProductText(html: String): List<ProductCandidate>
}

data class ProductCandidate(
    val name: String,
    val price: String? = null,
    val category: String? = null,
    val details: String? = null
)
