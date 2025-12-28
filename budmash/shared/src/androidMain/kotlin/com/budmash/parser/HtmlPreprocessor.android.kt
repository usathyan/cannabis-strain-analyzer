package com.budmash.parser

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

actual class HtmlPreprocessor {

    actual fun preprocess(html: String): String {
        val doc = Jsoup.parse(html)

        // Remove non-content elements
        doc.select("script, style, noscript, iframe, svg, path, meta, link, head").remove()
        doc.select("nav, footer, header, aside").remove()
        doc.select("[class*='nav'], [class*='footer'], [class*='header'], [class*='sidebar']").remove()
        doc.select("[id*='nav'], [id*='footer'], [id*='header'], [id*='sidebar']").remove()

        // Focus on product-like content
        val productSelectors = listOf(
            "[class*='product']",
            "[class*='menu-item']",
            "[class*='item-card']",
            "[class*='strain']",
            "[class*='flower']",
            "[data-product]",
            "[data-item]",
            ".card",
            ".item",
            "article"
        )

        val productElements = mutableListOf<Element>()
        for (selector in productSelectors) {
            productElements.addAll(doc.select(selector))
        }

        return if (productElements.isNotEmpty()) {
            // Return just product elements
            productElements.joinToString("\n") { it.text() }
        } else {
            // Fallback: return cleaned body text
            doc.body()?.text() ?: html
        }
    }

    actual fun extractProductText(html: String): List<ProductCandidate> {
        val doc = Jsoup.parse(html)
        val candidates = mutableListOf<ProductCandidate>()

        // Common product container selectors
        val productSelectors = listOf(
            "[class*='product']",
            "[class*='menu-item']",
            "[class*='item-card']",
            "[class*='strain']",
            "[class*='flower']",
            "[data-product]",
            ".card",
            "article"
        )

        val seen = mutableSetOf<String>()

        for (selector in productSelectors) {
            for (element in doc.select(selector)) {
                val name = extractName(element)
                if (name.isNotBlank() && name !in seen && name.length > 2) {
                    seen.add(name)
                    candidates.add(
                        ProductCandidate(
                            name = name,
                            price = extractPrice(element),
                            category = extractCategory(element),
                            details = extractDetails(element)
                        )
                    )
                }
            }
        }

        println("[BudMash] HtmlPreprocessor found ${candidates.size} product candidates")
        return candidates
    }

    private fun extractName(element: Element): String {
        // Try common name selectors
        val nameSelectors = listOf(
            "[class*='name']",
            "[class*='title']",
            "h1", "h2", "h3", "h4",
            ".product-name",
            ".item-name",
            ".strain-name"
        )

        for (selector in nameSelectors) {
            val found = element.select(selector).firstOrNull()
            if (found != null) {
                val text = found.text().trim()
                if (text.isNotBlank() && text.length < 100) {
                    return text
                }
            }
        }

        // Fallback: first text node
        return element.ownText().take(100).trim()
    }

    private fun extractPrice(element: Element): String? {
        val priceSelectors = listOf(
            "[class*='price']",
            "[data-price]",
            ".price"
        )

        for (selector in priceSelectors) {
            val found = element.select(selector).firstOrNull()
            if (found != null) {
                return found.text().trim()
            }
        }

        // Try to find price pattern in text
        val pricePattern = Regex("""\$\d+(?:\.\d{2})?""")
        val match = pricePattern.find(element.text())
        return match?.value
    }

    private fun extractCategory(element: Element): String? {
        val categorySelectors = listOf(
            "[class*='category']",
            "[class*='type']",
            "[data-category]"
        )

        for (selector in categorySelectors) {
            val found = element.select(selector).firstOrNull()
            if (found != null) {
                val text = found.text().trim()
                if (text.isNotBlank() && text.length < 50) {
                    return text
                }
            }
        }

        // Check for indica/sativa/hybrid keywords
        val text = element.text().lowercase()
        return when {
            "indica" in text -> "Indica"
            "sativa" in text -> "Sativa"
            "hybrid" in text -> "Hybrid"
            else -> null
        }
    }

    private fun extractDetails(element: Element): String? {
        val detailSelectors = listOf(
            "[class*='description']",
            "[class*='detail']",
            "[class*='thc']",
            "[class*='potency']",
            "p"
        )

        val details = mutableListOf<String>()

        for (selector in detailSelectors) {
            val found = element.select(selector).firstOrNull()
            if (found != null) {
                val text = found.text().trim()
                if (text.isNotBlank() && text.length < 200) {
                    details.add(text)
                }
            }
        }

        // Look for THC percentage
        val thcPattern = Regex("""(\d+(?:\.\d+)?)\s*%?\s*THC""", RegexOption.IGNORE_CASE)
        val thcMatch = thcPattern.find(element.text())
        if (thcMatch != null) {
            details.add("THC: ${thcMatch.groupValues[1]}%")
        }

        return details.distinct().take(3).joinToString("; ").takeIf { it.isNotBlank() }
    }
}
