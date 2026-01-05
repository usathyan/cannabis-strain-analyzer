# BudMash Developer Guide

Technical documentation for developers working on BudMash.

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        Compose UI Layer                         │
│  ┌───────────┐ ┌───────────┐ ┌───────────┐ ┌───────────────────┐│
│  │ HomeScreen│ │SearchScreen│ │StrainDetail│ │ SettingsScreen  ││
│  └─────┬─────┘ └─────┬─────┘ └─────┬─────┘ └────────┬──────────┘│
└────────┼─────────────┼─────────────┼────────────────┼───────────┘
         │             │             │                │
┌────────▼─────────────▼─────────────▼────────────────▼───────────┐
│                      Shared Business Logic                      │
│  ┌─────────────┐ ┌──────────────┐ ┌────────────────────────────┐│
│  │ProfileStorage│ │SimilarityCalc│ │    VisionMenuExtractor    ││
│  └──────┬──────┘ └──────┬───────┘ └────────────┬───────────────┘│
│         │               │                      │                │
│  ┌──────▼───────────────▼──────────────────────▼───────────────┐│
│  │                    Data Models                              ││
│  │  StrainData, TerpeneProfile, SimilarityResult, LlmConfig   ││
│  └─────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────┘
         │                                       │
┌────────▼───────────────────────────────────────▼────────────────┐
│                    Platform Implementations                     │
│  ┌─────────────────────┐          ┌────────────────────────────┐│
│  │   Android (actual)  │          │      iOS (actual)          ││
│  │  - ProfileStorage   │          │  - ProfileStorage          ││
│  │  - ScreenshotCapture│          │  - ScreenshotCapture       ││
│  │  - ImageChunker     │          │  - ImageChunker            ││
│  │  - SecureStorage    │          │  - SecureStorage           ││
│  └─────────────────────┘          └────────────────────────────┘│
└─────────────────────────────────────────────────────────────────┘
         │
┌────────▼────────────────────────────────────────────────────────┐
│                       External Services                         │
│  ┌──────────────┐    ┌──────────────┐    ┌───────────────────┐ │
│  │  OpenRouter  │    │  Gemini Flash│    │  Claude Haiku     │ │
│  │  (Gateway)   │───▶│  (Vision)    │    │  (Analysis)       │ │
│  └──────────────┘    └──────────────┘    └───────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

## Project Structure

```
budmash/
├── composeApp/                 # Compose Multiplatform UI
│   └── src/
│       ├── commonMain/         # Shared UI code
│       │   └── kotlin/com/budmash/ui/
│       │       ├── App.kt              # Navigation & app state
│       │       ├── screens/            # Screen composables
│       │       ├── components/         # Reusable UI components
│       │       └── theme/              # Colors, typography
│       ├── androidMain/        # Android-specific UI
│       └── iosMain/            # iOS-specific UI
│
├── shared/                     # Kotlin Multiplatform business logic
│   └── src/
│       ├── commonMain/         # Shared logic (expect declarations)
│       │   └── kotlin/com/budmash/
│       │       ├── data/               # Data models
│       │       ├── parser/             # Menu parsing & vision extraction
│       │       ├── similarity/         # Terpene matching algorithms
│       │       ├── llm/                # LLM provider abstraction
│       │       ├── capture/            # Screenshot & image handling
│       │       └── storage/            # Profile persistence
│       ├── androidMain/        # Android implementations (actual)
│       └── iosMain/            # iOS implementations (actual)
│
├── screenshots/                # App screenshots for README
├── README.md                   # End-user documentation
└── DEVELOPER.md                # This file
```

## Key Components

### Vision Menu Extraction

`VisionMenuExtractor.kt` handles extracting strain data from dispensary menu screenshots:

```kotlin
class VisionMenuExtractor(private val llmProvider: LlmProvider) {
    suspend fun extractFromScreenshot(
        base64Image: String,
        config: LlmConfig,
        visionModel: String
    ): Result<List<StrainData>>
}
```

**Pipeline:**
1. Check image dimensions
2. If height > 4000px, chunk into ~3000px segments with 200px overlap
3. Send each chunk to vision model with extraction prompt
4. Parse JSON response with multi-strategy recovery for truncated responses
5. Deduplicate strains across chunks by normalized name

**Image Chunking** (`ImageChunker.kt`):

Vision APIs downsample large images, losing text detail. For tall scroll screenshots (common when capturing full dispensary menus), chunking preserves OCR accuracy.

```kotlin
expect class ImageChunker() {
    fun getImageDimensions(base64Image: String): Pair<Int, Int>?
    fun chunkImage(base64Image: String, maxChunkHeight: Int = 3000): List<String>
}

const val MAX_CHUNK_HEIGHT = 3000    // Pixels per chunk
const val OVERLAP = 200              // Overlap to catch boundary strains
const val CHUNK_THRESHOLD = 4000     // Only chunk images taller than this
```

Platform implementations use:
- **Android:** `BitmapFactory` + `Bitmap.createBitmap()`
- **iOS:** `UIImage` + `CGImageCreateWithImageInRect()`

### Similarity Calculation

`SimilarityCalculator.kt` scores strains against user profile:

```kotlin
data class SimilarityResult(
    val score: Double,                    // Combined 0-100
    val cosineSimilarity: Double,         // Z-normalized cosine
    val euclideanSimilarity: Double,      // Distance-based
    val pearsonCorrelation: Double        // Correlation coefficient
)
```

**Algorithm Details:**

1. **Z-Score Normalization:**
   ```kotlin
   fun zScoreNormalize(profile: Map<String, Double>): Map<String, Double> {
       val values = profile.values.toList()
       val mean = values.average()
       val std = sqrt(values.map { (it - mean).pow(2) }.average())
       return profile.mapValues { (it.value - mean) / std }
   }
   ```

2. **Cosine Similarity:**
   ```kotlin
   // dot(a, b) / (||a|| * ||b||)
   val dotProduct = keys.sumOf { zUser[it]!! * zStrain[it]!! }
   val normUser = sqrt(keys.sumOf { zUser[it]!!.pow(2) })
   val normStrain = sqrt(keys.sumOf { zStrain[it]!!.pow(2) })
   val cosine = dotProduct / (normUser * normStrain)
   ```

3. **Euclidean Distance:**
   ```kotlin
   val distance = sqrt(keys.sumOf { (user[it]!! - strain[it]!!).pow(2) })
   val similarity = 1.0 / (1.0 + distance)
   ```

4. **Pearson Correlation:**
   ```kotlin
   // Standard Pearson formula with mean-centering
   ```

### Profile Storage

`ProfileStorage.kt` handles persistence using platform-specific implementations:

```kotlin
expect class ProfileStorage {
    fun saveLikedStrains(strains: List<StrainData>)
    fun loadLikedStrains(): List<StrainData>
    fun saveDislikedStrains(strains: List<StrainData>)
    fun loadDislikedStrains(): List<StrainData>
}
```

- **Android:** SharedPreferences with JSON serialization
- **iOS:** NSUserDefaults with JSON serialization

### LLM Integration

`LlmProvider.kt` abstracts LLM calls via OpenRouter:

```kotlin
class OpenRouterLlmProvider : LlmProvider {
    override suspend fun complete(messages: List<LlmMessage>, config: LlmConfig): LlmResponse
    override suspend fun completeVision(messages: List<MultimodalMessage>, config: LlmConfig): LlmResponse
}
```

**Supported Models:**
- Vision: `google/gemini-2.0-flash-001` (default), `google/gemini-flash-1.5`
- Analysis: `anthropic/claude-3-haiku`

### Navigation

Single-activity architecture with Compose state management:

```kotlin
sealed class SubScreen {
    data object Search : SubScreen()
    data class Results(val strains: List<StrainData>, ...) : SubScreen()
    data class StrainDetail(
        val strain: StrainData,
        val similarity: SimilarityResult?,
        val returnTo: SubScreen? = null  // Back navigation target
    ) : SubScreen()
}
```

## Build & Run

### Prerequisites

- JDK 17+
- Android Studio Hedgehog or later
- Xcode 15+ (for iOS)
- Kotlin 2.0+

### Android

```bash
./gradlew :composeApp:assembleDebug
# Or run from Android Studio
```

### iOS

```bash
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
# Or open iosApp/iosApp.xcodeproj in Xcode
```

### Release Builds

**Android (AAB for Play Store):**
```bash
./gradlew :composeApp:bundleRelease
# Output: composeApp/build/outputs/bundle/release/composeApp-release.aab
```

**Android (APK for direct install):**
```bash
./gradlew :composeApp:assembleRelease
# Output: composeApp/build/outputs/apk/release/composeApp-release.apk
```

**iOS (Archive for TestFlight):**
```bash
./gradlew :composeApp:linkReleaseFrameworkIosArm64
# Then in Xcode: Product → Archive → Distribute App
```

### Environment Setup

Set `JAVA_HOME` if needed:
```bash
export JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.17/libexec/openjdk.jdk/Contents/Home
```

## Data Models

### StrainData

```kotlin
data class StrainData(
    val name: String,
    val type: StrainType,           // INDICA, SATIVA, HYBRID
    val thcMin: Double,
    val thcMax: Double,
    val cbdMin: Double = 0.0,
    val cbdMax: Double = 0.0,
    val price: Double,
    val description: String,
    val terpenes: Map<String, Double> = emptyMap(),
    val effects: List<String> = emptyList(),
    val flavors: List<String> = emptyList()
)
```

### TerpeneProfile

```kotlin
typealias TerpeneProfile = Map<String, Double>
// e.g., {"myrcene": 0.45, "limonene": 0.32, "caryophyllene": 0.28}
```

## Error Handling

### JSON Truncation Recovery

Vision models sometimes return truncated JSON. `VisionMenuExtractor` uses multi-strategy recovery:

1. **Strategy 1:** Full object regex with all fields
2. **Strategy 2:** Name + type only
3. **Strategy 3:** Names only (last resort)
4. **AI Cleanup:** If regex fails, use text model to extract from raw response

### Image Processing Failures

`ImageChunker` returns original image on any failure, ensuring graceful degradation.

## Testing

```bash
./gradlew :shared:testDebugUnitTest
```

Key test areas:
- Similarity calculation accuracy
- JSON parsing edge cases
- Profile aggregation (MAX pooling)

## API Keys

OpenRouter API key is stored in platform secure storage:
- **Android:** EncryptedSharedPreferences
- **iOS:** Keychain

## Contributing

1. Follow existing patterns for expect/actual declarations
2. Keep UI in `composeApp`, logic in `shared`
3. Test on both platforms before PR
4. Update screenshots if UI changes

## License

See [LICENSE](LICENSE) for full terms. Personal use is free; commercial use requires a license.
