package com.budmash.parser

import com.budmash.data.DispensaryMenu
import com.budmash.data.StrainData
import com.budmash.data.StrainType
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock

/**
 * Mock parser for UI testing. Returns sample strain data
 * to demonstrate the app flow before LLM integration.
 */
class MockMenuParser : MenuParser {

    override fun parseMenu(url: String): Flow<ParseStatus> = flow {
        println("[BudMash] MockMenuParser starting for URL: $url")

        emit(ParseStatus.Fetching)
        delay(800) // Simulate network delay

        emit(ParseStatus.FetchComplete(45_000))
        delay(500)

        emit(ParseStatus.ProductsFound(24, 18))
        delay(300)

        // Simulate extracting strains
        val sampleStrains = createSampleStrains()
        sampleStrains.forEachIndexed { index, _ ->
            emit(ParseStatus.ResolvingTerpenes(index + 1, sampleStrains.size))
            delay(150)
        }

        val menu = DispensaryMenu(
            url = url,
            fetchedAt = Clock.System.now().toEpochMilliseconds(),
            strains = sampleStrains
        )

        println("[BudMash] MockMenuParser complete with ${sampleStrains.size} strains")
        emit(ParseStatus.Complete(menu))
    }

    private fun createSampleStrains(): List<StrainData> = listOf(
        StrainData(
            name = "Blue Dream",
            type = StrainType.HYBRID,
            thcMin = 19.0,
            thcMax = 24.0,
            price = 45.0,
            description = "A legendary sativa-dominant hybrid with sweet berry notes",
            myrcene = 0.35,
            limonene = 0.28,
            caryophyllene = 0.22,
            pinene = 0.15,
            linalool = 0.12,
            humulene = 0.08,
            terpinolene = 0.05,
            ocimene = 0.03
        ),
        StrainData(
            name = "OG Kush",
            type = StrainType.HYBRID,
            thcMin = 20.0,
            thcMax = 26.0,
            price = 50.0,
            description = "Classic strain with earthy pine and sour lemon scent",
            myrcene = 0.42,
            limonene = 0.35,
            caryophyllene = 0.28,
            pinene = 0.10,
            linalool = 0.18,
            humulene = 0.12,
            terpinolene = 0.02,
            ocimene = 0.01
        ),
        StrainData(
            name = "Girl Scout Cookies",
            type = StrainType.HYBRID,
            thcMin = 22.0,
            thcMax = 28.0,
            price = 55.0,
            description = "Sweet and earthy with hints of mint and chocolate",
            myrcene = 0.30,
            limonene = 0.25,
            caryophyllene = 0.38,
            pinene = 0.08,
            linalool = 0.20,
            humulene = 0.15,
            terpinolene = 0.04,
            ocimene = 0.02
        ),
        StrainData(
            name = "Granddaddy Purple",
            type = StrainType.INDICA,
            thcMin = 17.0,
            thcMax = 23.0,
            price = 48.0,
            description = "Potent indica with grape and berry aroma",
            myrcene = 0.55,
            limonene = 0.12,
            caryophyllene = 0.18,
            pinene = 0.05,
            linalool = 0.28,
            humulene = 0.10,
            terpinolene = 0.01,
            ocimene = 0.01
        ),
        StrainData(
            name = "Sour Diesel",
            type = StrainType.SATIVA,
            thcMin = 19.0,
            thcMax = 25.0,
            price = 52.0,
            description = "Energizing sativa with pungent diesel aroma",
            myrcene = 0.25,
            limonene = 0.42,
            caryophyllene = 0.30,
            pinene = 0.20,
            linalool = 0.08,
            humulene = 0.12,
            terpinolene = 0.15,
            ocimene = 0.05
        ),
        StrainData(
            name = "Northern Lights",
            type = StrainType.INDICA,
            thcMin = 16.0,
            thcMax = 21.0,
            price = 42.0,
            description = "Pure indica with sweet and spicy notes",
            myrcene = 0.60,
            limonene = 0.10,
            caryophyllene = 0.15,
            pinene = 0.08,
            linalool = 0.22,
            humulene = 0.08,
            terpinolene = 0.02,
            ocimene = 0.01
        )
    )
}
