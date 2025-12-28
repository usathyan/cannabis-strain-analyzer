# LLM Menu Parser Design

## Overview

Replace the MockMenuParser with a real LLM-powered parser that extracts cannabis strain data from dispensary menu URLs. The system uses a two-pass LLM extraction pipeline with terpene resolution via Cannlytics API.

## Architecture

```
URL → Fetch HTML → LLM Pass 1 (Categorize) → Filter Flowers → LLM Pass 2 (Extract) → Resolve Terpenes → DispensaryMenu
```

**Key Components:**
- **KtorLlmProvider**: Cross-platform HTTP client for LLM API calls (OpenRouter default)
- **LlmMenuExtractor**: Two-pass extraction pipeline
- **TerpeneResolver**: Cannlytics API + LLM fallback for terpene profiles
- **DefaultMenuParser**: Orchestrates the full pipeline with Flow-based status updates

## LLM Provider Interface

```kotlin
enum class LlmProviderType {
    OPENAI, ANTHROPIC, OPENROUTER, GOOGLE, GROQ, OLLAMA
}

data class LlmConfig(
    val provider: LlmProviderType,
    val apiKey: String,
    val model: String,
    val baseUrl: String? = null,
    val maxTokens: Int = 4096,
    val temperature: Float = 0.3f
)

data class LlmMessage(
    val role: String,  // "system", "user", "assistant"
    val content: String
)

data class LlmResponse(
    val content: String,
    val tokensUsed: Int
)

interface LlmProvider {
    suspend fun complete(messages: List<LlmMessage>, config: LlmConfig): LlmResponse
}
```

## Ktor-Based Provider

Single unified provider handling all API formats:

| Provider | Base URL | Format |
|----------|----------|--------|
| OpenRouter | openrouter.ai/api/v1 | OpenAI-compatible |
| OpenAI | api.openai.com/v1 | OpenAI native |
| Anthropic | api.anthropic.com/v1 | Anthropic format |
| Google | generativelanguage.googleapis.com | Gemini format |
| Groq | api.groq.com/openai/v1 | OpenAI-compatible |
| Ollama | localhost:11434/api | Ollama format |

Uses Ktor with platform-specific engines:
- Android: OkHttp engine
- iOS: Darwin engine

## Two-Pass LLM Extraction Pipeline

### Pass 1: HTML → Categorized Menu JSON

**Prompt:**
```
System: Parse this dispensary menu HTML into categorized JSON.
        Group products by category (flower, edibles, vapes, concentrates,
        pre-rolls, tinctures, topicals, etc.)
        For each product capture: name, category, price, any visible details.
        Output valid JSON only.

User: [Raw HTML content, truncated to ~100KB]
```

**Output:**
```json
{
  "categories": {
    "flower": [
      {"name": "Blue Dream", "price": "$45", "details": "Hybrid, 22% THC"},
      {"name": "OG Kush", "price": "$50", "details": "Indica, 24% THC"}
    ],
    "edibles": [...],
    "vapes": [...]
  }
}
```

### Pass 2: Flower Category → Detailed Strain Data

**Prompt:**
```
System: Extract detailed strain data for these flower products.
        For each strain provide:
        - name: exact product name
        - type: INDICA, SATIVA, or HYBRID
        - thcMin: minimum THC percentage (number)
        - thcMax: maximum THC percentage (number)
        - cbdMin: minimum CBD percentage (number, 0 if not listed)
        - cbdMax: maximum CBD percentage (number, 0 if not listed)
        - price: numeric price in dollars
        - description: brief description if available

        Output valid JSON array only.

User: [Flower array from Pass 1]
```

**Output:**
```json
{
  "strains": [
    {
      "name": "Blue Dream",
      "type": "HYBRID",
      "thcMin": 19.0,
      "thcMax": 24.0,
      "cbdMin": 0.0,
      "cbdMax": 0.1,
      "price": 45.0,
      "description": "Sativa-dominant hybrid with sweet berry notes"
    }
  ]
}
```

## Terpene Resolution

Dispensary menus rarely include terpene data. Resolved after extraction:

**Resolution Order:**
1. **Cannlytics API** (free, no key) - Query by strain name
2. **LLM Fallback** - Generate typical profile for known strains

**Cannlytics Query:**
```
GET https://cannlytics.com/api/strains?name={strainName}
```

**LLM Fallback Prompt:**
```
System: You are a cannabis expert. Provide typical terpene percentages
        for this strain. Use values between 0.0-1.0 representing %.
        Return JSON with: myrcene, limonene, caryophyllene, pinene,
        linalool, humulene, terpinolene, ocimene.

User: Strain: "Blue Dream" (Hybrid)
```

**Batch Processing:** Resolve 5 strains in parallel, emit `ResolvingTerpenes(current, total)` status.

## Error Handling

```kotlin
sealed class ParseError {
    data class NetworkError(val message: String) : ParseError()
    data class LlmError(val message: String) : ParseError()
    data class ParseFailure(val message: String) : ParseError()
    data object NoFlowersFound : ParseError()
}
```

**Retry Policy:**
- Network fetch: 3 retries with exponential backoff (1s, 2s, 4s)
- LLM calls: 2 retries
- Terpene resolution: Silent fallback to null (non-blocking)

**Graceful Degradation:**
- Pass 1 fails → Emit error
- Pass 2 fails → Retry once, then error
- Terpene fails → Continue with null terpenes
- No flowers → Emit NoFlowersFound with helpful message

## File Structure

```
shared/src/commonMain/kotlin/com/budmash/
├── llm/
│   ├── LlmProvider.kt          # Interface + LlmConfig + LlmMessage
│   ├── KtorLlmProvider.kt      # Ktor-based unified provider
│   └── LlmConfigStorage.kt     # expect/actual for API key storage
├── parser/
│   ├── LlmMenuExtractor.kt     # Two-pass extraction logic
│   ├── TerpeneResolver.kt      # Cannlytics + LLM fallback
│   └── DefaultMenuParser.kt    # Wires it all together
└── data/
    └── ParseError.kt           # Error types

shared/src/androidMain/kotlin/com/budmash/llm/
└── LlmConfigStorage.android.kt # SharedPreferences impl

shared/src/iosMain/kotlin/com/budmash/llm/
└── LlmConfigStorage.ios.kt     # NSUserDefaults impl
```

## Dependencies

```kotlin
commonMain {
    implementation("io.ktor:ktor-client-core:2.3.7")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.7")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")
}
androidMain {
    implementation("io.ktor:ktor-client-okhttp:2.3.7")
}
iosMain {
    implementation("io.ktor:ktor-client-darwin:2.3.7")
}
```

## Integration

Modify `App.kt` to use `DefaultMenuParser` instead of `MockMenuParser`:

```kotlin
val parser = remember { DefaultMenuParser(
    llmProvider = KtorLlmProvider(),
    terpeneResolver = TerpeneResolver(KtorLlmProvider()),
    configStorage = LlmConfigStorage()
) }
```
