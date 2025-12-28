package com.budmash.parser

import com.budmash.data.DispensaryMenu
import com.budmash.data.ParseError
import com.budmash.data.StrainData
import kotlinx.coroutines.flow.Flow

sealed class ParseStatus {
    data object Fetching : ParseStatus()
    data class FetchComplete(val sizeBytes: Int) : ParseStatus()
    data class ProductsFound(val total: Int, val flowerCount: Int) : ParseStatus()
    data class ExtractingStrains(val current: Int, val total: Int) : ParseStatus()
    data class ResolvingTerpenes(val current: Int, val total: Int) : ParseStatus()
    data class Complete(val menu: DispensaryMenu) : ParseStatus()
    data class Error(val error: ParseError) : ParseStatus()
}

interface MenuParser {
    fun parseMenu(url: String): Flow<ParseStatus>
}

interface LlmMenuExtractor {
    suspend fun extractStrainsFromHtml(html: String): List<ExtractedStrain>
}

data class ExtractedStrain(
    val name: String,
    val type: String?,
    val thcMin: Double?,
    val thcMax: Double?,
    val cbdMin: Double?,
    val cbdMax: Double?,
    val price: Double?,
    val description: String?
)
