package com.budmash.database

import com.budmash.data.StrainData
import com.budmash.data.StrainType

object StrainDatabase {
    private val strainsByName: Map<String, StrainData> by lazy { buildStrainMap() }

    fun getAllStrains(): List<StrainData> = strainsByName.values.toList()

    fun getStrainByName(name: String): StrainData? {
        return strainsByName[name.lowercase().trim()]
    }

    fun searchStrains(query: String): List<StrainData> {
        if (query.isBlank()) return getAllStrains()
        val q = query.lowercase().trim()
        return strainsByName.values.filter { strain ->
            strain.name.lowercase().contains(q) ||
            strain.effects.any { it.lowercase().contains(q) } ||
            strain.flavors.any { it.lowercase().contains(q) } ||
            strain.type.name.lowercase().contains(q)
        }
    }

    private fun parseThcRange(range: String): Pair<Double?, Double?> {
        val cleaned = range.replace("%", "").trim()
        return when {
            cleaned.startsWith("<") -> null to cleaned.removePrefix("<").toDoubleOrNull()
            cleaned.contains("-") -> {
                val parts = cleaned.split("-")
                parts[0].toDoubleOrNull() to parts.getOrNull(1)?.toDoubleOrNull()
            }
            else -> cleaned.toDoubleOrNull()?.let { it to it } ?: (null to null)
        }
    }

    private fun buildStrainMap(): Map<String, StrainData> {
        return strainEntries.associate { entry ->
            entry.name.lowercase() to entry
        }
    }

    private val strainEntries = listOf(
        StrainData(
            name = "24K Gold",
            type = StrainType.INDICA,
            description = "24K Gold is a potent hybrid cannabis strain, renowned for its unique blend of flavors. It combines the fruity tangie strain with the earthy indica Kosher Kush, resulting in a rich, citrusy aroma with undertones of spice. Its a popular choice for recreational use.",
            effects = listOf("relaxed"),
            flavors = listOf("earthy", "pine"),
            thcMin = 21.0, thcMax = 24.0,
            myrcene = 0.5486, limonene = 0.2543, caryophyllene = 0.2214,
            pinene = 0.18, linalool = 0.1529, humulene = 0.1275,
            bisabolol = 0.0243
        ),
        StrainData(
            name = "Apple Fritter",
            type = StrainType.HYBRID,
            description = "A potent hybrid known for its sweet apple and vanilla flavors",
            effects = listOf("relaxed", "happy", "euphoric", "uplifted", "creative"),
            flavors = listOf("apple", "vanilla", "sweet", "earthy"),
            thcMin = 22.0, thcMax = 28.0,
            myrcene = 0.35, limonene = 0.28, caryophyllene = 0.22,
            pinene = 0.12, linalool = 0.08
        ),
        StrainData(
            name = "Blue Dream",
            type = StrainType.SATIVA,
            description = "Blue Dream is a popular cannabis strain, renowned for its balanced and full-bodied effects. This sativa-dominant hybrid boasts a sweet, berry aroma, inherited from its Blueberry parent. Its high is smooth, starting with a cerebral rush, leading to motivated energy, before mellowing into a calm euphoria. Ideal for daytime use, Blue Dream is perfect for managing pain, depression, and nausea.",
            effects = listOf("relaxed", "happy"),
            flavors = listOf("earthy"),
            thcMin = 0.0, thcMax = 3.0,
            myrcene = 0.26, limonene = 0.5, caryophyllene = 0.22,
            pinene = 0.09, linalool = 0.36, humulene = 0.03,
            terpinolene = 0.03, bisabolol = 0.1
        ),
        StrainData(
            name = "Blueberry",
            type = StrainType.INDICA,
            description = "Blueberry is a renowned cannabis strain known for its distinctive sweet and fruity aroma reminiscent of fresh blueberries. This indica-dominant hybrid typically features dense, colorful buds with hues of purple and blue, making it visually appealing to consumers.",
            effects = listOf("happy", "uplifted"),
            flavors = listOf("lemon", "blueberry"),
            thcMin = 13.0, thcMax = 16.0,
            myrcene = 0.564, limonene = 0.07, caryophyllene = 0.128,
            pinene = 0.024, linalool = 0.038, humulene = 0.0322,
            terpinolene = 0.172, ocimene = 0.059, nerolidol = 0.003,
            bisabolol = 0.018
        ),
        StrainData(
            name = "Bubba Kush",
            type = StrainType.INDICA,
            description = "Classic indica with coffee and chocolate flavors, known for heavy sedation",
            effects = listOf("relaxed", "sleepy", "happy", "euphoric", "hungry"),
            flavors = listOf("coffee", "chocolate", "earthy", "sweet"),
            thcMin = 14.0, thcMax = 22.0,
            myrcene = 0.45, caryophyllene = 0.28, limonene = 0.15,
            pinene = 0.08, linalool = 0.06
        ),
        StrainData(
            name = "Charlotte's Web",
            type = StrainType.SATIVA,
            description = "High-CBD strain famous for its therapeutic properties with minimal psychoactive effects",
            effects = listOf("calm", "focused", "clear-headed", "relaxed"),
            flavors = listOf("pine", "earthy", "woody", "sage"),
            thcMax = 1.0, cbdMin = 13.0, cbdMax = 20.0,
            myrcene = 0.45, pinene = 0.25, caryophyllene = 0.18,
            limonene = 0.08, linalool = 0.04
        ),
        StrainData(
            name = "Cherry Pie",
            type = StrainType.HYBRID,
            description = "Cherry Pie is a popular cannabis strain known for its enticing aroma and flavor reminiscent of sweet cherries. This hybrid strain is a cross between the flavorful Granddaddy Purple and the potent Durban Poison.",
            effects = listOf("happy", "euphoric"),
            flavors = listOf("sweet", "tree"),
            thcMin = 8.0, thcMax = 11.0,
            myrcene = 0.691, limonene = 0.072, caryophyllene = 0.315,
            pinene = 0.134, linalool = 0.073, humulene = 0.0978,
            terpinolene = 0.004, ocimene = 0.13, nerolidol = 0.008,
            bisabolol = 0.016
        ),
        StrainData(
            name = "Durban Poison",
            type = StrainType.SATIVA,
            description = "Pure African sativa known for its sweet smell and energetic, uplifting effects",
            effects = listOf("energetic", "uplifted", "happy", "creative", "focused"),
            flavors = listOf("sweet", "earthy", "pine", "anise"),
            thcMin = 15.0, thcMax = 25.0,
            terpinolene = 0.4, myrcene = 0.22, pinene = 0.15,
            limonene = 0.12, ocimene = 0.08
        ),
        StrainData(
            name = "Gelato",
            type = StrainType.HYBRID,
            description = "Gelato is a popular cannabis strain known for its enticing aroma and flavorful profile. It is a hybrid strain that combines the genetics of Sunset Sherbet and Thin Mint Girl Scout Cookies.",
            effects = listOf("happy", "relaxed", "uplifted", "creative", "euphoric", "tingly", "hungry", "giggly"),
            flavors = listOf("sweet", "flowery"),
            thcMin = 0.0, thcMax = 3.0,
            myrcene = 0.1067, limonene = 0.4033, caryophyllene = 0.5167,
            pinene = 0.09, linalool = 0.1667, humulene = 0.1567,
            terpinolene = 0.0067, ocimene = 0.0433, nerolidol = 0.0267,
            bisabolol = 0.0067
        ),
        StrainData(
            name = "Girl Scout Cookies",
            type = StrainType.HYBRID,
            description = "Famous hybrid with sweet and earthy flavors, known for full-body relaxation and euphoria",
            effects = listOf("relaxed", "happy", "euphoric", "uplifted", "creative"),
            flavors = listOf("sweet", "earthy", "mint", "chocolate"),
            thcMin = 19.0, thcMax = 28.0,
            caryophyllene = 0.35, limonene = 0.28, myrcene = 0.22,
            humulene = 0.12, linalool = 0.08
        ),
        StrainData(
            name = "Gorilla Glue",
            type = StrainType.HYBRID,
            description = "Extremely potent hybrid known for heavy relaxation and earthy, diesel flavors",
            effects = listOf("relaxed", "euphoric", "happy", "uplifted", "sleepy"),
            flavors = listOf("earthy", "pine", "diesel", "pungent"),
            thcMin = 25.0, thcMax = 30.0,
            caryophyllene = 0.42, myrcene = 0.3, limonene = 0.18,
            pinene = 0.1, humulene = 0.08
        ),
        StrainData(
            name = "Granddaddy Purple",
            type = StrainType.INDICA,
            description = "Classic indica with grape and berry flavors, known for deep relaxation",
            effects = listOf("relaxed", "sleepy", "happy", "euphoric", "hungry"),
            flavors = listOf("grape", "berry", "earthy", "sweet"),
            thcMin = 17.0, thcMax = 24.0,
            myrcene = 0.42, caryophyllene = 0.28, pinene = 0.18,
            limonene = 0.12, linalool = 0.08
        ),
        StrainData(
            name = "Green Crack",
            type = StrainType.SATIVA,
            description = "Intense sativa with sharp energy and focus, featuring mango and citrus notes",
            effects = listOf("energetic", "focused", "happy", "uplifted", "creative"),
            flavors = listOf("mango", "citrus", "sweet", "earthy"),
            thcMin = 15.0, thcMax = 24.0,
            myrcene = 0.4, limonene = 0.25, caryophyllene = 0.15,
            pinene = 0.12, ocimene = 0.08
        ),
        StrainData(
            name = "Harlequin",
            type = StrainType.SATIVA,
            description = "High-CBD sativa providing clear-headed relief without heavy psychoactive effects",
            effects = listOf("relaxed", "focused", "happy", "uplifted", "clear-headed"),
            flavors = listOf("earthy", "mango", "sweet", "woody"),
            thcMin = 4.0, thcMax = 10.0, cbdMin = 8.0, cbdMax = 16.0,
            myrcene = 0.48, pinene = 0.22, caryophyllene = 0.15,
            limonene = 0.1, humulene = 0.05
        ),
        StrainData(
            name = "Jack Herer",
            type = StrainType.SATIVA,
            description = "Legendary sativa named after the cannabis activist, known for creative and energetic effects",
            effects = listOf("energetic", "creative", "focused", "happy", "uplifted"),
            flavors = listOf("pine", "earthy", "woody", "spicy"),
            thcMin = 18.0, thcMax = 24.0,
            terpinolene = 0.35, pinene = 0.25, caryophyllene = 0.18,
            myrcene = 0.12, limonene = 0.1
        ),
        StrainData(
            name = "Lemon Haze",
            type = StrainType.SATIVA,
            description = "Haze is a popular cannabis strain known for its strong sativa effects. It typically has a high THC content and is characterized by its earthy, citrusy, and sweet aroma.",
            effects = listOf("happy", "talkative", "uplifted", "creative", "energetic", "giggly"),
            flavors = listOf("spicytoherbal"),
            thcMin = 14.0, thcMax = 17.0,
            myrcene = 0.19, limonene = 0.07, caryophyllene = 0.08,
            pinene = 0.08, humulene = 0.03, terpinolene = 0.6,
            ocimene = 0.09, nerolidol = 0.01, eucalyptol = 0.01
        ),
        StrainData(
            name = "Northern Lights",
            type = StrainType.INDICA,
            description = "Legendary indica known for its resinous buds and fast flowering, provides blissful relaxation",
            effects = listOf("relaxed", "sleepy", "happy", "euphoric", "hungry"),
            flavors = listOf("sweet", "earthy", "pine", "spicy"),
            thcMin = 16.0, thcMax = 21.0,
            myrcene = 0.5, caryophyllene = 0.22, pinene = 0.15,
            limonene = 0.1, linalool = 0.08
        ),
        StrainData(
            name = "OG Kush",
            type = StrainType.HYBRID,
            description = "Legendary strain with complex earthy, pine, and citrus flavors",
            effects = listOf("relaxed", "happy", "euphoric", "uplifted", "hungry"),
            flavors = listOf("earthy", "pine", "woody", "citrus"),
            thcMin = 19.0, thcMax = 26.0,
            myrcene = 0.38, limonene = 0.28, caryophyllene = 0.22,
            pinene = 0.15, linalool = 0.08
        ),
        StrainData(
            name = "Pineapple Express",
            type = StrainType.HYBRID,
            description = "Tropical hybrid with pineapple and mango flavors, provides energetic and creative buzz",
            effects = listOf("happy", "uplifted", "euphoric", "energetic", "creative"),
            flavors = listOf("pineapple", "mango", "tropical", "citrus"),
            thcMin = 17.0, thcMax = 24.0,
            myrcene = 0.35, limonene = 0.3, caryophyllene = 0.18,
            pinene = 0.12, ocimene = 0.08
        ),
        StrainData(
            name = "Purple Haze",
            type = StrainType.SATIVA,
            description = "Haze is a popular cannabis strain known for its strong sativa effects. It typically has a high THC content and is characterized by its earthy, citrusy, and sweet aroma.",
            effects = listOf("happy", "talkative", "uplifted", "creative", "energetic", "giggly"),
            flavors = listOf("spicytoherbal"),
            thcMin = 14.0, thcMax = 17.0,
            myrcene = 0.19, limonene = 0.07, caryophyllene = 0.08,
            pinene = 0.08, humulene = 0.03, terpinolene = 0.6,
            ocimene = 0.09, nerolidol = 0.01, eucalyptol = 0.01
        ),
        StrainData(
            name = "Purple Punch",
            type = StrainType.INDICA,
            description = "Sweet and sedating indica with grape candy flavors",
            effects = listOf("relaxed", "sleepy", "happy", "euphoric", "hungry"),
            flavors = listOf("grape", "berry", "sweet", "vanilla"),
            thcMin = 18.0, thcMax = 25.0,
            myrcene = 0.38, caryophyllene = 0.25, limonene = 0.18,
            pinene = 0.12, linalool = 0.1
        ),
        StrainData(
            name = "Runtz",
            type = StrainType.HYBRID,
            description = "Trendy hybrid with candy-like sweetness and colorful buds, known for balanced effects",
            effects = listOf("relaxed", "happy", "euphoric", "uplifted", "giggly"),
            flavors = listOf("sweet", "candy", "tropical", "fruity"),
            thcMin = 19.0, thcMax = 29.0,
            limonene = 0.32, caryophyllene = 0.28, myrcene = 0.22,
            linalool = 0.12, pinene = 0.08
        ),
        StrainData(
            name = "Skywalker OG",
            type = StrainType.INDICA,
            description = "Powerful indica cross of Skywalker and OG Kush, known for heavy relaxation and euphoria",
            effects = listOf("relaxed", "sleepy", "happy", "euphoric", "hungry"),
            flavors = listOf("earthy", "spicy", "sweet", "diesel"),
            thcMin = 20.0, thcMax = 26.0,
            myrcene = 0.42, caryophyllene = 0.28, limonene = 0.18,
            pinene = 0.1, linalool = 0.08
        ),
        StrainData(
            name = "Sour Diesel",
            type = StrainType.HYBRID,
            description = "Chemdawg Sour Diesel is a hybrid cannabis strain that combines the genetics of Chemdawg and Sour Diesel. This strain is known for its pungent aroma and potent effects.",
            effects = listOf("relaxed", "creative", "euphoric"),
            flavors = listOf("pine", "pungent"),
            thcMin = 0.0, thcMax = 3.0, cbdMin = 0.1, cbdMax = 0.6,
            myrcene = 0.39, limonene = 0.05, caryophyllene = 0.09,
            pinene = 0.34, linalool = 0.02, humulene = 0.01,
            bisabolol = 0.02
        ),
        StrainData(
            name = "Super Lemon Haze",
            type = StrainType.SATIVA,
            description = "Haze is a popular cannabis strain known for its strong sativa effects. It typically has a high THC content and is characterized by its earthy, citrusy, and sweet aroma.",
            effects = listOf("happy", "talkative", "uplifted", "creative", "energetic", "giggly"),
            flavors = listOf("spicytoherbal"),
            thcMin = 14.0, thcMax = 17.0,
            myrcene = 0.19, limonene = 0.07, caryophyllene = 0.08,
            pinene = 0.08, humulene = 0.03, terpinolene = 0.6,
            ocimene = 0.09, nerolidol = 0.01, eucalyptol = 0.01
        ),
        StrainData(
            name = "Tangie",
            type = StrainType.HYBRID,
            description = "Double Tangie Banana is a unique cannabis strain known for its vibrant flavors and aromas. This hybrid cultivar combines the citrusy and sweet notes of Tangie with the tropical and fruity essence of Banana Kush.",
            effects = listOf("relaxed", "happy"),
            flavors = listOf("citrus", "orange"),
            thcMin = 0.0, thcMax = 3.0,
            myrcene = 0.71, limonene = 0.03, caryophyllene = 0.04,
            pinene = 0.14, linalool = 0.06, terpinolene = 0.01,
            bisabolol = 0.03
        ),
        StrainData(
            name = "Wedding Cake",
            type = StrainType.HYBRID,
            description = "Sweet and tangy strain with relaxing effects and beautiful trichome coverage",
            effects = listOf("relaxed", "happy", "euphoric", "uplifted", "hungry"),
            flavors = listOf("sweet", "vanilla", "earthy", "peppery"),
            thcMin = 22.0, thcMax = 27.0,
            limonene = 0.35, caryophyllene = 0.3, myrcene = 0.22,
            linalool = 0.1, humulene = 0.08
        ),
        StrainData(
            name = "White Widow",
            type = StrainType.HYBRID,
            description = "Dutch classic covered in white crystals, known for balanced euphoria and energy",
            effects = listOf("relaxed", "happy", "euphoric", "uplifted", "creative"),
            flavors = listOf("earthy", "woody", "pungent", "spicy"),
            thcMin = 18.0, thcMax = 25.0,
            myrcene = 0.35, caryophyllene = 0.25, pinene = 0.18,
            limonene = 0.15, humulene = 0.08
        ),
        StrainData(
            name = "Zkittlez",
            type = StrainType.INDICA,
            description = "Award-winning indica with candy-like fruit flavors and calming effects",
            effects = listOf("relaxed", "happy", "euphoric", "sleepy", "focused"),
            flavors = listOf("sweet", "berry", "grape", "tropical"),
            thcMin = 15.0, thcMax = 23.0,
            caryophyllene = 0.32, limonene = 0.25, myrcene = 0.2,
            linalool = 0.12, humulene = 0.08
        )
    )
}
