package com.budmash.data

import kotlin.test.Test
import kotlin.test.assertEquals

class ModelsTest {
    @Test
    fun strainData_terpeneProfile_returnsList() {
        val strain = StrainData(
            name = "Blue Dream",
            type = StrainType.HYBRID,
            myrcene = 0.6,
            limonene = 0.4,
            caryophyllene = 0.3,
            pinene = 0.5,
            linalool = 0.2,
            humulene = 0.1,
            terpinolene = 0.15,
            ocimene = 0.05,
            nerolidol = 0.02,
            bisabolol = 0.01,
            eucalyptol = 0.03
        )

        val profile = strain.terpeneProfile()
        assertEquals(11, profile.size)
        assertEquals(0.6, profile[0]) // myrcene first
    }
}
