# BudMash MVP Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build a cross-platform (iOS + Android) app that parses dispensary menu URLs and matches strains against user's terpene profile.

**Architecture:** KMM shared module contains analysis engine, data models, HTTP client, and LLM integration. Compose Multiplatform provides unified UI. Platform-specific code handles local storage and native LLM (Gemini Nano on Android).

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, Ktor (HTTP), Kotlinx.serialization (JSON), Koin (DI)

---

## Phase 1: Project Scaffold

### Task 1.1: Create KMM Project Structure

**Files:**
- Create: `budmash/settings.gradle.kts`
- Create: `budmash/build.gradle.kts`
- Create: `budmash/gradle.properties`
- Create: `budmash/shared/build.gradle.kts`

**Step 1: Create project directory structure**

```bash
mkdir -p budmash/shared/src/{commonMain,androidMain,iosMain}/kotlin/com/budmash
mkdir -p budmash/androidApp/src/main/kotlin/com/budmash
mkdir -p budmash/iosApp
mkdir -p budmash/composeApp/src/commonMain/kotlin/com/budmash/ui
mkdir -p budmash/gradle/wrapper
```

**Step 2: Create root settings.gradle.kts**

```kotlin
// budmash/settings.gradle.kts
pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "BudMash"
include(":shared")
include(":androidApp")
include(":composeApp")
```

**Step 3: Create root build.gradle.kts**

```kotlin
// budmash/build.gradle.kts
plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
}
```

**Step 4: Create gradle.properties**

```properties
# budmash/gradle.properties
kotlin.code.style=official
android.useAndroidX=true
org.gradle.jvmargs=-Xmx2048M -Dfile.encoding=UTF-8
kotlin.mpp.androidSourceSetLayoutVersion=2
```

**Step 5: Commit scaffold**

```bash
git add budmash/
git commit -m "chore: scaffold BudMash KMM project structure"
```

---

### Task 1.2: Configure Version Catalog

**Files:**
- Create: `budmash/gradle/libs.versions.toml`

**Step 1: Create version catalog**

```toml
# budmash/gradle/libs.versions.toml
[versions]
kotlin = "2.0.21"
agp = "8.2.2"
compose-multiplatform = "1.7.1"
ktor = "2.3.12"
kotlinx-serialization = "1.7.3"
kotlinx-coroutines = "1.9.0"
koin = "4.0.0"

[libraries]
# Ktor
ktor-client-core = { module = "io.ktor:ktor-client-core", version.ref = "ktor" }
ktor-client-cio = { module = "io.ktor:ktor-client-cio", version.ref = "ktor" }
ktor-client-darwin = { module = "io.ktor:ktor-client-darwin", version.ref = "ktor" }
ktor-client-content-negotiation = { module = "io.ktor:ktor-client-content-negotiation", version.ref = "ktor" }
ktor-serialization-json = { module = "io.ktor:ktor-serialization-kotlinx-json", version.ref = "ktor" }

# Serialization
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "kotlinx-serialization" }

# Coroutines
kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "kotlinx-coroutines" }
kotlinx-coroutines-android = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-android", version.ref = "kotlinx-coroutines" }

# Koin DI
koin-core = { module = "io.insert-koin:koin-core", version.ref = "koin" }
koin-compose = { module = "io.insert-koin:koin-compose", version.ref = "koin" }

# Testing
kotlin-test = { module = "org.jetbrains.kotlin:kotlin-test", version.ref = "kotlin" }

[plugins]
androidApplication = { id = "com.android.application", version.ref = "agp" }
androidLibrary = { id = "com.android.library", version.ref = "agp" }
kotlinMultiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
kotlinSerialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
composeMultiplatform = { id = "org.jetbrains.compose", version.ref = "compose-multiplatform" }
composeCompiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
```

**Step 2: Commit**

```bash
git add budmash/gradle/libs.versions.toml
git commit -m "chore: add Gradle version catalog for BudMash"
```

---

### Task 1.3: Configure Shared Module

**Files:**
- Create: `budmash/shared/build.gradle.kts`

**Step 1: Create shared module build config**

```kotlin
// budmash/shared/build.gradle.kts
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.json)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.koin.core)
        }

        androidMain.dependencies {
            implementation(libs.ktor.client.cio)
            implementation(libs.kotlinx.coroutines.android)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "com.budmash.shared"
    compileSdk = 34
    defaultConfig {
        minSdk = 26
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
```

**Step 2: Commit**

```bash
git add budmash/shared/build.gradle.kts
git commit -m "chore: configure shared KMM module"
```

---

## Phase 2: Core Data Models (Migrated from Current App)

### Task 2.1: Create Strain Data Models

**Files:**
- Create: `budmash/shared/src/commonMain/kotlin/com/budmash/data/Models.kt`
- Test: `budmash/shared/src/commonTest/kotlin/com/budmash/data/ModelsTest.kt`

**Step 1: Write the failing test**

```kotlin
// budmash/shared/src/commonTest/kotlin/com/budmash/data/ModelsTest.kt
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
```

**Step 2: Run test to verify it fails**

```bash
cd budmash && ./gradlew :shared:testDebugUnitTest --tests "com.budmash.data.ModelsTest"
```

Expected: FAIL - class not found

**Step 3: Write implementation (migrate from current app)**

```kotlin
// budmash/shared/src/commonMain/kotlin/com/budmash/data/Models.kt
package com.budmash.data

import kotlinx.serialization.Serializable

@Serializable
enum class StrainType {
    INDICA, SATIVA, HYBRID, UNKNOWN
}

@Serializable
data class StrainData(
    val name: String,
    val type: StrainType = StrainType.UNKNOWN,
    val description: String = "",
    val effects: List<String> = emptyList(),
    val flavors: List<String> = emptyList(),
    val thcMin: Double? = null,
    val thcMax: Double? = null,
    val cbdMin: Double? = null,
    val cbdMax: Double? = null,
    val price: Double? = null,
    // Terpenes (normalized 0-1)
    val myrcene: Double = 0.0,
    val limonene: Double = 0.0,
    val caryophyllene: Double = 0.0,
    val pinene: Double = 0.0,
    val linalool: Double = 0.0,
    val humulene: Double = 0.0,
    val terpinolene: Double = 0.0,
    val ocimene: Double = 0.0,
    val nerolidol: Double = 0.0,
    val bisabolol: Double = 0.0,
    val eucalyptol: Double = 0.0
) {
    fun terpeneProfile(): List<Double> = listOf(
        myrcene, limonene, caryophyllene, pinene, linalool,
        humulene, terpinolene, ocimene, nerolidol, bisabolol, eucalyptol
    )

    companion object {
        val TERPENE_NAMES = listOf(
            "Myrcene", "Limonene", "Caryophyllene", "Pinene", "Linalool",
            "Humulene", "Terpinolene", "Ocimene", "Nerolidol", "Bisabolol", "Eucalyptol"
        )
    }
}

@Serializable
data class UserProfile(
    val favoriteStrains: List<String> = emptyList(),
    val likedStrains: List<String> = emptyList(),
    val dislikedStrains: List<String> = emptyList(),
    val idealProfile: List<Double> = List(11) { 0.0 }
)

@Serializable
data class SimilarityResult(
    val strain: StrainData,
    val overallScore: Double,
    val cosineScore: Double,
    val euclideanScore: Double,
    val pearsonScore: Double
)

@Serializable
data class DispensaryMenu(
    val url: String,
    val name: String = "",
    val fetchedAt: Long = 0,
    val strains: List<StrainData> = emptyList()
)
```

**Step 4: Run test to verify it passes**

```bash
cd budmash && ./gradlew :shared:testDebugUnitTest --tests "com.budmash.data.ModelsTest"
```

Expected: PASS

**Step 5: Commit**

```bash
git add budmash/shared/src/
git commit -m "feat(shared): add core data models for strains and profiles"
```

---

### Task 2.2: Create Analysis Engine

**Files:**
- Create: `budmash/shared/src/commonMain/kotlin/com/budmash/analysis/AnalysisEngine.kt`
- Test: `budmash/shared/src/commonTest/kotlin/com/budmash/analysis/AnalysisEngineTest.kt`

**Step 1: Write the failing test**

```kotlin
// budmash/shared/src/commonTest/kotlin/com/budmash/analysis/AnalysisEngineTest.kt
package com.budmash.analysis

import com.budmash.data.StrainData
import com.budmash.data.StrainType
import kotlin.test.Test
import kotlin.test.assertTrue

class AnalysisEngineTest {
    private val engine = AnalysisEngine()

    @Test
    fun cosineSimilarity_identicalVectors_returns1() {
        val v1 = listOf(0.5, 0.3, 0.2)
        val v2 = listOf(0.5, 0.3, 0.2)
        val result = engine.cosineSimilarity(v1, v2)
        assertTrue(result > 0.99)
    }

    @Test
    fun calculateMatch_similarStrains_highScore() {
        val userProfile = listOf(0.8, 0.6, 0.5, 0.4, 0.3, 0.2, 0.1, 0.1, 0.1, 0.1, 0.1)
        val strain = StrainData(
            name = "Test Strain",
            type = StrainType.HYBRID,
            myrcene = 0.75,
            limonene = 0.55,
            caryophyllene = 0.45,
            pinene = 0.35,
            linalool = 0.25
        )
        val result = engine.calculateMatch(userProfile, strain)
        assertTrue(result.overallScore > 0.7)
    }
}
```

**Step 2: Run test to verify it fails**

```bash
cd budmash && ./gradlew :shared:testDebugUnitTest --tests "com.budmash.analysis.AnalysisEngineTest"
```

Expected: FAIL

**Step 3: Write implementation (migrate from LocalAnalysisEngine.kt)**

```kotlin
// budmash/shared/src/commonMain/kotlin/com/budmash/analysis/AnalysisEngine.kt
package com.budmash.analysis

import com.budmash.data.SimilarityResult
import com.budmash.data.StrainData
import kotlin.math.pow
import kotlin.math.sqrt

class AnalysisEngine {

    companion object {
        private const val COSINE_WEIGHT = 0.50
        private const val EUCLIDEAN_WEIGHT = 0.25
        private const val PEARSON_WEIGHT = 0.25
    }

    fun calculateMatch(userProfile: List<Double>, strain: StrainData): SimilarityResult {
        val strainProfile = strain.terpeneProfile()

        val userZ = zScore(userProfile)
        val strainZ = zScore(strainProfile)

        val cosine = cosineSimilarity(userZ, strainZ)
        val euclidean = euclideanSimilarity(userZ, strainZ)
        val pearson = pearsonCorrelation(userZ, strainZ)

        val overall = (cosine * COSINE_WEIGHT) +
                      (euclidean * EUCLIDEAN_WEIGHT) +
                      (pearson * PEARSON_WEIGHT)

        return SimilarityResult(
            strain = strain,
            overallScore = overall.coerceIn(0.0, 1.0),
            cosineScore = cosine.coerceIn(0.0, 1.0),
            euclideanScore = euclidean.coerceIn(0.0, 1.0),
            pearsonScore = pearson.coerceIn(0.0, 1.0)
        )
    }

    fun buildIdealProfile(strains: List<StrainData>): List<Double> {
        if (strains.isEmpty()) return List(11) { 0.0 }

        // MAX pooling across all strains
        return (0 until 11).map { i ->
            strains.maxOf { it.terpeneProfile()[i] }
        }
    }

    fun zScore(vector: List<Double>): List<Double> {
        val mean = vector.average()
        val variance = vector.map { (it - mean).pow(2) }.average()
        val std = sqrt(variance)

        return if (std < 0.0001) {
            vector.map { 0.0 }
        } else {
            vector.map { (it - mean) / std }
        }
    }

    fun cosineSimilarity(v1: List<Double>, v2: List<Double>): Double {
        val dotProduct = v1.zip(v2).sumOf { it.first * it.second }
        val mag1 = sqrt(v1.sumOf { it.pow(2) })
        val mag2 = sqrt(v2.sumOf { it.pow(2) })

        return if (mag1 < 0.0001 || mag2 < 0.0001) {
            0.0
        } else {
            ((dotProduct / (mag1 * mag2)) + 1) / 2 // Normalize to 0-1
        }
    }

    fun euclideanSimilarity(v1: List<Double>, v2: List<Double>): Double {
        val distance = sqrt(v1.zip(v2).sumOf { (it.first - it.second).pow(2) })
        val maxDistance = sqrt(v1.size.toDouble() * 4) // Max possible for z-scores
        return 1 - (distance / maxDistance).coerceIn(0.0, 1.0)
    }

    fun pearsonCorrelation(v1: List<Double>, v2: List<Double>): Double {
        val mean1 = v1.average()
        val mean2 = v2.average()

        val numerator = v1.zip(v2).sumOf { (it.first - mean1) * (it.second - mean2) }
        val denom1 = sqrt(v1.sumOf { (it - mean1).pow(2) })
        val denom2 = sqrt(v2.sumOf { (it - mean2).pow(2) })

        return if (denom1 < 0.0001 || denom2 < 0.0001) {
            0.0
        } else {
            ((numerator / (denom1 * denom2)) + 1) / 2 // Normalize to 0-1
        }
    }
}
```

**Step 4: Run test to verify it passes**

```bash
cd budmash && ./gradlew :shared:testDebugUnitTest --tests "com.budmash.analysis.AnalysisEngineTest"
```

Expected: PASS

**Step 5: Commit**

```bash
git add budmash/shared/src/
git commit -m "feat(shared): add terpene analysis engine with similarity algorithms"
```

---

## Phase 3: HTTP & Menu Parsing

### Task 3.1: Create HTTP Client

**Files:**
- Create: `budmash/shared/src/commonMain/kotlin/com/budmash/network/HttpClient.kt`

**Step 1: Create HTTP client factory**

```kotlin
// budmash/shared/src/commonMain/kotlin/com/budmash/network/HttpClient.kt
package com.budmash.network

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

expect fun createHttpClient(): HttpClient

object MenuFetcher {
    private val client by lazy { createHttpClient() }

    suspend fun fetchMenuHtml(url: String): Result<String> {
        return try {
            val response: HttpResponse = client.get(url)
            Result.success(response.bodyAsText())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

**Step 2: Create Android implementation**

```kotlin
// budmash/shared/src/androidMain/kotlin/com/budmash/network/HttpClient.android.kt
package com.budmash.network

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

actual fun createHttpClient(): HttpClient {
    return HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }
}
```

**Step 3: Create iOS implementation**

```kotlin
// budmash/shared/src/iosMain/kotlin/com/budmash/network/HttpClient.ios.kt
package com.budmash.network

import io.ktor.client.*
import io.ktor.client.engine.darwin.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

actual fun createHttpClient(): HttpClient {
    return HttpClient(Darwin) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }
}
```

**Step 4: Commit**

```bash
git add budmash/shared/src/
git commit -m "feat(shared): add Ktor HTTP client for menu fetching"
```

---

### Task 3.2: Create Menu Parser Interface

**Files:**
- Create: `budmash/shared/src/commonMain/kotlin/com/budmash/parser/MenuParser.kt`

**Step 1: Create parser interface and status events**

```kotlin
// budmash/shared/src/commonMain/kotlin/com/budmash/parser/MenuParser.kt
package com.budmash.parser

import com.budmash.data.DispensaryMenu
import com.budmash.data.StrainData
import kotlinx.coroutines.flow.Flow

sealed class ParseStatus {
    data object Fetching : ParseStatus()
    data class FetchComplete(val sizeBytes: Int) : ParseStatus()
    data class ProductsFound(val total: Int, val flowerCount: Int) : ParseStatus()
    data class ExtractingStrains(val current: Int, val total: Int) : ParseStatus()
    data class ResolvingTerpenes(val current: Int, val total: Int) : ParseStatus()
    data class Complete(val menu: DispensaryMenu) : ParseStatus()
    data class Error(val message: String) : ParseStatus()
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
```

**Step 2: Commit**

```bash
git add budmash/shared/src/
git commit -m "feat(shared): add menu parser interface with status events"
```

---

### Task 3.3: Implement Menu Parser

**Files:**
- Create: `budmash/shared/src/commonMain/kotlin/com/budmash/parser/DefaultMenuParser.kt`

**Step 1: Implement parser with progressive status updates**

```kotlin
// budmash/shared/src/commonMain/kotlin/com/budmash/parser/DefaultMenuParser.kt
package com.budmash.parser

import com.budmash.analysis.AnalysisEngine
import com.budmash.data.DispensaryMenu
import com.budmash.data.StrainData
import com.budmash.data.StrainType
import com.budmash.network.MenuFetcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock

class DefaultMenuParser(
    private val llmExtractor: LlmMenuExtractor,
    private val terpeneResolver: TerpeneResolver
) : MenuParser {

    override fun parseMenu(url: String): Flow<ParseStatus> = flow {
        emit(ParseStatus.Fetching)

        val htmlResult = MenuFetcher.fetchMenuHtml(url)
        if (htmlResult.isFailure) {
            emit(ParseStatus.Error("Failed to fetch: ${htmlResult.exceptionOrNull()?.message}"))
            return@flow
        }

        val html = htmlResult.getOrThrow()
        emit(ParseStatus.FetchComplete(html.length))

        // Extract strains via LLM
        val extracted = try {
            llmExtractor.extractStrainsFromHtml(html)
        } catch (e: Exception) {
            emit(ParseStatus.Error("LLM extraction failed: ${e.message}"))
            return@flow
        }

        val flowerStrains = extracted.filter {
            it.type?.lowercase() in listOf("indica", "sativa", "hybrid", "flower", null)
        }
        emit(ParseStatus.ProductsFound(extracted.size, flowerStrains.size))

        // Resolve terpene data for each strain
        val strains = mutableListOf<StrainData>()
        flowerStrains.forEachIndexed { index, extracted ->
            emit(ParseStatus.ResolvingTerpenes(index + 1, flowerStrains.size))

            val strainData = terpeneResolver.resolve(extracted)
            strains.add(strainData)
        }

        val menu = DispensaryMenu(
            url = url,
            fetchedAt = Clock.System.now().toEpochMilliseconds(),
            strains = strains
        )

        emit(ParseStatus.Complete(menu))
    }
}

interface TerpeneResolver {
    suspend fun resolve(extracted: ExtractedStrain): StrainData
}
```

**Step 2: Commit**

```bash
git add budmash/shared/src/
git commit -m "feat(shared): implement menu parser with progressive status flow"
```

---

## Phase 4: Compose Multiplatform UI

### Task 4.1: Configure Compose App Module

**Files:**
- Create: `budmash/composeApp/build.gradle.kts`

**Step 1: Create Compose app build config**

```kotlin
// budmash/composeApp/build.gradle.kts
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { target ->
        target.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.shared)
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
        }

        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.android)
        }
    }
}

android {
    namespace = "com.budmash.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.budmash.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
```

**Step 2: Commit**

```bash
git add budmash/composeApp/
git commit -m "chore: configure Compose Multiplatform app module"
```

---

### Task 4.2: Create Theme

**Files:**
- Create: `budmash/composeApp/src/commonMain/kotlin/com/budmash/ui/theme/Theme.kt`
- Create: `budmash/composeApp/src/commonMain/kotlin/com/budmash/ui/theme/Color.kt`

**Step 1: Create colors**

```kotlin
// budmash/composeApp/src/commonMain/kotlin/com/budmash/ui/theme/Color.kt
package com.budmash.ui.theme

import androidx.compose.ui.graphics.Color

val Green80 = Color(0xFFA5D6A7)
val GreenGrey80 = Color(0xFFB0BEC5)
val Teal80 = Color(0xFF80CBC4)

val Green40 = Color(0xFF388E3C)
val GreenGrey40 = Color(0xFF546E7A)
val Teal40 = Color(0xFF00897B)

val SurfaceDark = Color(0xFF1C1B1F)
val SurfaceLight = Color(0xFFFFFBFE)
```

**Step 2: Create theme**

```kotlin
// budmash/composeApp/src/commonMain/kotlin/com/budmash/ui/theme/Theme.kt
package com.budmash.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = Green80,
    secondary = Teal80,
    tertiary = GreenGrey80,
    surface = SurfaceDark
)

private val LightColorScheme = lightColorScheme(
    primary = Green40,
    secondary = Teal40,
    tertiary = GreenGrey40,
    surface = SurfaceLight
)

@Composable
fun BudMashTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
```

**Step 3: Commit**

```bash
git add budmash/composeApp/src/
git commit -m "feat(ui): add BudMash theme with green color scheme"
```

---

### Task 4.3: Create Home Screen

**Files:**
- Create: `budmash/composeApp/src/commonMain/kotlin/com/budmash/ui/screens/HomeScreen.kt`

**Step 1: Create home screen with URL input**

```kotlin
// budmash/composeApp/src/commonMain/kotlin/com/budmash/ui/screens/HomeScreen.kt
package com.budmash.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private const val DEFAULT_URL = "https://auntmarysnj.co/order-online/?selected_value=rec"

@Composable
fun HomeScreen(
    onScanClick: (String) -> Unit
) {
    var url by remember { mutableStateOf(DEFAULT_URL) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "BudMash",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Paste a dispensary menu URL to find your matches",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("Dispensary Menu URL") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { onScanClick(url) },
            modifier = Modifier.fillMaxWidth(),
            enabled = url.isNotBlank()
        ) {
            Text("Scan Menu")
        }

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "Your profile builds as you mark strains you've tried.",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
```

**Step 2: Commit**

```bash
git add budmash/composeApp/src/
git commit -m "feat(ui): add home screen with URL input"
```

---

### Task 4.4: Create Scan Progress Screen

**Files:**
- Create: `budmash/composeApp/src/commonMain/kotlin/com/budmash/ui/screens/ScanScreen.kt`

**Step 1: Create scan screen with progressive status**

```kotlin
// budmash/composeApp/src/commonMain/kotlin/com/budmash/ui/screens/ScanScreen.kt
package com.budmash.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.budmash.parser.ParseStatus

@Composable
fun ScanScreen(
    status: ParseStatus,
    onComplete: () -> Unit,
    onError: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (status) {
            is ParseStatus.Fetching -> {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Fetching menu...")
            }

            is ParseStatus.FetchComplete -> {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Fetched ${status.sizeBytes / 1024}kb")
            }

            is ParseStatus.ProductsFound -> {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Found ${status.flowerCount} flower products")
            }

            is ParseStatus.ExtractingStrains -> {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Extracting: ${status.current}/${status.total}")
            }

            is ParseStatus.ResolvingTerpenes -> {
                LinearProgressIndicator(
                    progress = { status.current.toFloat() / status.total },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Resolving terpenes: ${status.current}/${status.total}")
            }

            is ParseStatus.Complete -> {
                LaunchedEffect(Unit) { onComplete() }
                Text("Complete! Found ${status.menu.strains.size} strains")
            }

            is ParseStatus.Error -> {
                LaunchedEffect(Unit) { onError(status.message) }
                Text("Error: ${status.message}", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
```

**Step 2: Commit**

```bash
git add budmash/composeApp/src/
git commit -m "feat(ui): add scan screen with progressive status display"
```

---

### Task 4.5: Create Dashboard Screen

**Files:**
- Create: `budmash/composeApp/src/commonMain/kotlin/com/budmash/ui/screens/DashboardScreen.kt`

**Step 1: Create dashboard with strain table**

```kotlin
// budmash/composeApp/src/commonMain/kotlin/com/budmash/ui/screens/DashboardScreen.kt
package com.budmash.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.budmash.data.SimilarityResult
import com.budmash.data.StrainData

@Composable
fun DashboardScreen(
    strains: List<SimilarityResult>,
    hasProfile: Boolean,
    onStrainClick: (StrainData) -> Unit,
    onLikeClick: (StrainData) -> Unit,
    onDislikeClick: (StrainData) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${strains.size} flowers found",
                    style = MaterialTheme.typography.titleMedium
                )
                if (!hasProfile) {
                    Text(
                        text = "Mark strains to build profile",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // Strain list
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(strains) { result ->
                StrainCard(
                    result = result,
                    hasProfile = hasProfile,
                    onClick = { onStrainClick(result.strain) },
                    onLike = { onLikeClick(result.strain) },
                    onDislike = { onDislikeClick(result.strain) }
                )
            }
        }
    }
}

@Composable
private fun StrainCard(
    result: SimilarityResult,
    hasProfile: Boolean,
    onClick: () -> Unit,
    onLike: () -> Unit,
    onDislike: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.strain.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = result.strain.type.name,
                    style = MaterialTheme.typography.bodySmall
                )
                // Top terpenes
                val topTerpenes = result.strain.terpeneProfile()
                    .zip(StrainData.TERPENE_NAMES)
                    .sortedByDescending { it.first }
                    .take(3)
                    .joinToString(", ") { "${it.second} ${String.format("%.2f", it.first)}" }
                Text(
                    text = topTerpenes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Match score (if profile exists)
            if (hasProfile) {
                Text(
                    text = "${(result.overallScore * 100).toInt()}%",
                    style = MaterialTheme.typography.headlineSmall,
                    color = when {
                        result.overallScore >= 0.8 -> MaterialTheme.colorScheme.primary
                        result.overallScore >= 0.6 -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            // Like/Dislike buttons
            Row {
                IconButton(onClick = onLike) {
                    Icon(Icons.Default.Check, "Like", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDislike) {
                    Icon(Icons.Default.Close, "Dislike", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
```

**Step 2: Commit**

```bash
git add budmash/composeApp/src/
git commit -m "feat(ui): add dashboard screen with strain cards and like/dislike"
```

---

## Phase 5: Android App Integration

### Task 5.1: Create Android MainActivity

**Files:**
- Create: `budmash/composeApp/src/androidMain/kotlin/com/budmash/MainActivity.kt`
- Create: `budmash/composeApp/src/androidMain/AndroidManifest.xml`

**Step 1: Create MainActivity**

```kotlin
// budmash/composeApp/src/androidMain/kotlin/com/budmash/MainActivity.kt
package com.budmash

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.budmash.ui.App

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            App()
        }
    }
}
```

**Step 2: Create AndroidManifest.xml**

```xml
<!-- budmash/composeApp/src/androidMain/AndroidManifest.xml -->
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />

    <application
        android:allowBackup="true"
        android:label="BudMash"
        android:supportsRtl="true"
        android:theme="@android:style/Theme.Material.Light.NoActionBar">

        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

**Step 3: Commit**

```bash
git add budmash/composeApp/src/androidMain/
git commit -m "feat(android): add MainActivity and manifest"
```

---

### Task 5.2: Create App Composable with Navigation

**Files:**
- Create: `budmash/composeApp/src/commonMain/kotlin/com/budmash/ui/App.kt`

**Step 1: Create main App composable**

```kotlin
// budmash/composeApp/src/commonMain/kotlin/com/budmash/ui/App.kt
package com.budmash.ui

import androidx.compose.runtime.*
import com.budmash.data.DispensaryMenu
import com.budmash.data.SimilarityResult
import com.budmash.data.StrainData
import com.budmash.parser.ParseStatus
import com.budmash.ui.screens.DashboardScreen
import com.budmash.ui.screens.HomeScreen
import com.budmash.ui.screens.ScanScreen
import com.budmash.ui.theme.BudMashTheme

sealed class Screen {
    data object Home : Screen()
    data class Scanning(val url: String) : Screen()
    data class Dashboard(val menu: DispensaryMenu) : Screen()
    data class StrainDetail(val strain: StrainData) : Screen()
}

@Composable
fun App() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    var parseStatus by remember { mutableStateOf<ParseStatus>(ParseStatus.Fetching) }
    var likedStrains by remember { mutableStateOf<Set<String>>(emptySet()) }

    BudMashTheme {
        when (val screen = currentScreen) {
            is Screen.Home -> {
                HomeScreen(
                    onScanClick = { url ->
                        currentScreen = Screen.Scanning(url)
                        // TODO: Trigger actual parsing
                    }
                )
            }

            is Screen.Scanning -> {
                ScanScreen(
                    status = parseStatus,
                    onComplete = {
                        // Navigate to dashboard when complete
                        val status = parseStatus
                        if (status is ParseStatus.Complete) {
                            currentScreen = Screen.Dashboard(status.menu)
                        }
                    },
                    onError = {
                        currentScreen = Screen.Home
                    }
                )
            }

            is Screen.Dashboard -> {
                val results = screen.menu.strains.map { strain ->
                    SimilarityResult(
                        strain = strain,
                        overallScore = 0.0, // TODO: Calculate with profile
                        cosineScore = 0.0,
                        euclideanScore = 0.0,
                        pearsonScore = 0.0
                    )
                }

                DashboardScreen(
                    strains = results,
                    hasProfile = likedStrains.isNotEmpty(),
                    onStrainClick = { strain ->
                        currentScreen = Screen.StrainDetail(strain)
                    },
                    onLikeClick = { strain ->
                        likedStrains = likedStrains + strain.name
                    },
                    onDislikeClick = { strain ->
                        likedStrains = likedStrains - strain.name
                    }
                )
            }

            is Screen.StrainDetail -> {
                // TODO: Implement detail screen
            }
        }
    }
}
```

**Step 2: Commit**

```bash
git add budmash/composeApp/src/
git commit -m "feat(ui): add App composable with navigation between screens"
```

---

## Phase 6: Build & Test

### Task 6.1: Copy Gradle Wrapper

**Step 1: Copy gradle wrapper from current app**

```bash
cp -r android-app/gradle/wrapper budmash/gradle/
cp android-app/gradlew budmash/
cp android-app/gradlew.bat budmash/
chmod +x budmash/gradlew
```

**Step 2: Create local.properties**

```bash
echo "sdk.dir=$HOME/Library/Android/sdk" > budmash/local.properties
```

**Step 3: Commit**

```bash
git add budmash/
git commit -m "chore: add gradle wrapper to budmash"
```

---

### Task 6.2: Build Android Debug APK

**Step 1: Sync and build**

```bash
cd budmash
export JAVA_HOME="$(brew --prefix openjdk@17)"
./gradlew :composeApp:assembleDebug
```

**Step 2: Verify APK created**

```bash
ls -la composeApp/build/outputs/apk/debug/
```

Expected: `composeApp-debug.apk` exists

**Step 3: Commit any build fixes**

```bash
git add .
git commit -m "fix: resolve build issues for Android"
```

---

### Task 6.3: Install and Test on Device

**Step 1: Install APK**

```bash
~/Library/Android/sdk/platform-tools/adb install composeApp/build/outputs/apk/debug/composeApp-debug.apk
```

**Step 2: Manual test checklist**
- [ ] App launches
- [ ] Home screen shows with default URL
- [ ] URL field is editable
- [ ] Scan button is clickable

---

## Summary: MVP Tasks

| Phase | Tasks | Description |
|-------|-------|-------------|
| 1 | 1.1-1.3 | Project scaffold & Gradle config |
| 2 | 2.1-2.2 | Data models & Analysis engine |
| 3 | 3.1-3.3 | HTTP client & Menu parser |
| 4 | 4.1-4.5 | Compose UI screens |
| 5 | 5.1-5.2 | Android integration |
| 6 | 6.1-6.3 | Build & test |

**Total: ~18 tasks, ~2-3 hours of focused work**

---

## Phase 7: Documentation

### Task 7.1: Create "How This Was Built" Document

**Files:**
- Create: `budmash/HOW_THIS_WAS_BUILT.md`

**Step 1: Write the story**

A blog-style essay documenting:
- The brainstorming session and key decisions made
- Evolution from Android-only to cross-platform vision
- Target user persona refinement (expert cannabis user)
- Technology choices and rationale
- The collaborative human-AI development process

**Step 2: Commit**

```bash
git add budmash/HOW_THIS_WAS_BUILT.md
git commit -m "docs: add 'How This Was Built' development story"
```

---

## Post-MVP Tasks (Not in this plan)

- LLM integration for menu extraction (requires API key setup)
- Terpene resolver (connect to Cannlytics API + local DB)
- Strain detail screen
- iOS app target
- Advanced menu (profile, search, settings)
- Scan history

---

*Plan created: 2025-12-27*
