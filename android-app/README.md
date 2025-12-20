# Terpene Profile Analyzer

**Find your perfect strain through the science of terpenes.**

A privacy-first Android app that uses advanced similarity algorithms to match cannabis strains to your personal preference profile. All analysis runs on-device—no data leaves your phone.

---

## The Science Behind the Experience

Cannabis effects aren't random. They're orchestrated by **terpenes**—aromatic compounds that shape each strain's character. The same terpene that makes lavender calming (linalool) creates relaxation in certain strains. The citrus burst in some sativas? That's limonene at work.

This app doesn't guess. It calculates.

When you tell us your favorite strains, we extract their terpene fingerprints and build a mathematical model of what you actually enjoy. Then we compare any new strain against your profile using the same similarity metrics used in machine learning and data science.

### The Terpene Wheel

| Terpene | Aroma | Common Effect | Found In |
|---------|-------|---------------|----------|
| **Myrcene** | Earthy, musky | Relaxation, sedation | Mangoes, hops, lemongrass |
| **Limonene** | Citrus, lemon | Mood elevation, stress relief | Citrus rinds, juniper |
| **Caryophyllene** | Spicy, peppery | Anti-inflammatory, calm | Black pepper, cloves |
| **Pinene** | Pine, forest | Alertness, memory | Pine needles, rosemary |
| **Linalool** | Floral, lavender | Relaxation, anti-anxiety | Lavender, coriander |
| **Humulene** | Hoppy, earthy | Appetite suppression | Hops, coriander |
| **Terpinolene** | Herbal, floral | Uplifting, creative | Nutmeg, tea tree |
| **Ocimene** | Sweet, herbal | Energizing | Mint, parsley, orchids |
| **Nerolidol** | Woody, floral | Sedative, relaxing | Jasmine, ginger |
| **Bisabolol** | Floral, sweet | Soothing, anti-irritant | Chamomile |
| **Eucalyptol** | Minty, cool | Cooling, clearing | Eucalyptus, bay leaves |

Your preference for "relaxing indicas" or "energizing sativas" is really a preference for specific terpene ratios. We make that visible.

---

## How It Works

### 1. Build Your Profile

Add strains you know and love. Each strain you add contributes its terpene fingerprint to your ideal profile. We use **MAX pooling**—taking the highest value of each terpene across all your favorites—to capture what you're drawn to.

```
Your Favorites:
├── Granddaddy Purple  → High myrcene (0.85), moderate caryophyllene (0.65)
├── Blue Dream         → Balanced myrcene (0.55), high limonene (0.60)
└── OG Kush            → High myrcene (0.75), moderate limonene (0.45)

Your Ideal Profile (MAX pooling):
├── Myrcene:       0.85  ████████████████░░░░
├── Caryophyllene: 0.65  █████████████░░░░░░░
├── Limonene:      0.60  ████████████░░░░░░░░
├── Pinene:        0.45  █████████░░░░░░░░░░░
└── Linalool:      0.30  ██████░░░░░░░░░░░░░░
```

### 2. Search Any Strain

Type any strain name. If it's in our curated database of 50+ strains with lab-verified terpene data, we analyze instantly. If not, the app can generate the profile using on-device AI (Gemini Nano) or cloud APIs—then saves it locally for future use.

### 3. Get Your Match Score

We don't just say "good match." We show you exactly why:

```
Gelato vs Your Profile: 78% Match (Excellent)

Similarity Breakdown:
├── Z-Scored Cosine:     82%  ████████████████░░░░
├── Euclidean Distance:  71%  ██████████████░░░░░░
└── Pearson Correlation: 79%  ████████████████░░░░

Terpene Comparison:
├── Myrcene:      +12% above your profile  ↑
├── Limonene:     -8% below your profile   ↓
├── Caryophyllene: +3% match               ≈
└── Linalool:     +15% above your profile  ↑
```

---

## The Algorithm

Most "strain matching" apps use simple keyword matching or user ratings. We use the same mathematical foundations as recommendation engines and scientific research.

### Multi-Metric Similarity Analysis

**Z-Score Normalization**
Raw terpene percentages are misleading—myrcene naturally occurs in higher concentrations than bisabolol. We normalize each terpene to its z-score (standard deviations from mean), ensuring fair comparison across all compounds.

```kotlin
fun zScore(vector: List<Double>): List<Double> {
    val mean = vector.average()
    val std = sqrt(vector.map { (it - mean).pow(2) }.average())
    return vector.map { (it - mean) / std }
}
```

**Cosine Similarity (50% weight)**
Measures the angle between two terpene vectors. Two strains with identical *ratios* of terpenes score 100%, even if absolute concentrations differ. Perfect for comparing profiles regardless of potency.

**Euclidean Distance (25% weight)**
The straight-line distance in 11-dimensional terpene space. Catches cases where cosine similarity misses—strains with similar ratios but vastly different intensities.

**Pearson Correlation (25% weight)**
Measures how terpenes move together. If your favorites all have high myrcene when they have high caryophyllene, a strain matching that pattern scores higher.

### Combined Score Formula

```
Overall Score = (0.5 × Cosine) + (0.25 × Euclidean) + (0.25 × Correlation)
```

This weighted ensemble reduces the blind spots of any single metric.

---

## Privacy First Architecture

**Your preferences stay on your device.**

| Data | Location | Leaves Device? |
|------|----------|----------------|
| Favorite strains | Local SharedPreferences | Never |
| Terpene calculations | On-device | Never |
| Custom strain data | Local database | Never |
| AI-generated strains | Saved locally after generation | Only during generation* |

*When using cloud APIs for unknown strains. Gemini Nano runs entirely on-device.

### On-Device AI with Gemini Nano

On supported devices (Pixel 8+, select Samsung), the app uses **Google's Gemini Nano**—a language model that runs entirely on your phone's NPU. Search for obscure strains without any network request.

When Gemini Nano isn't available, you can optionally configure:
- **Google Gemini** (cloud)
- **OpenAI GPT-4** (cloud)
- **Anthropic Claude** (cloud)
- **Ollama** (local server)

Or use the app with just the embedded database—no AI required for core functionality.

---

## Features

### Home Dashboard
- Profile statistics at a glance
- Favorite strain chips
- AI status indicator (Nano ready, downloading, or cloud configured)
- Quick navigation to build profile or search strains

### Profile Builder
- **Add any strain** by name—known or unknown
- **Quick-add grid** from our curated database
- **Terpene visualization** showing your combined profile
- **Individual strain charts** to see each favorite's contribution
- Toggle between combined MAX profile and per-strain breakdown

### Strain Analyzer
- Real-time search with source indication (local/cached/API fetched/generated)
- **Match percentage** with breakdown by algorithm
- **Similarity metrics** with progress bars
- **Strain details**: type, THC/CBD ranges, effects, flavors
- **Local analysis summary** generated from templates
- **AI-powered insights** with rich HTML formatting (bold, italic, structured sections)
- Searched strains automatically saved to local database for future use

### Settings
- Configure cloud API providers with your own keys
- Download Gemini Nano when available
- View database statistics
- Manage saved strains

### Dark Mode
Full dark theme support with carefully selected colors for readability in low-light environments—because sometimes you're researching at night.

---

## Technical Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      UI Layer (Compose)                      │
├─────────────────────────────────────────────────────────────┤
│  HomeScreen │ ConfigureScreen │ CompareScreen │ Settings    │
│             │   TerpeneChart  │               │             │
└──────┬──────┴────────┬────────┴───────┬───────┴─────────────┘
       │               │                │
       ▼               ▼                ▼
┌─────────────────────────────────────────────────────────────┐
│                    StrainViewModel                           │
│            (StateFlow, Coroutines, MVVM)                    │
└──────┬──────────────┬─────────────────┬─────────────────────┘
       │              │                 │
       ▼              ▼                 ▼
┌──────────────┐ ┌─────────────┐ ┌─────────────────────────────┐
│ LocalAnalysis │ │StrainData   │ │      LlmService             │
│   Engine      │ │  Service    │ │  ┌─────────────────────────┐│
│               │ │             │ │  │    GeminiNanoService    ││
│ • Z-Score     │ │ • Local DB  │ │  │    (On-Device NPU)      ││
│ • Cosine      │ │ • Caching   │ │  └─────────────────────────┘│
│ • Euclidean   │ │ • LLM Gen   │ │  ┌─────────────────────────┐│
│ • Correlation │ │             │ │  │  UnifiedLlmProvider     ││
└───────────────┘ └─────────────┘ │  │  • Gemini (Cloud)       ││
                                   │  │  • OpenAI               ││
                                   │  │  • Anthropic            ││
                                   │  │  • Ollama               ││
                                   │  └─────────────────────────┘│
                                   └─────────────────────────────┘
```

### Tech Stack

| Layer | Technology |
|-------|------------|
| Language | Kotlin 1.9 |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + StateFlow |
| Async | Kotlin Coroutines |
| Local Storage | SharedPreferences + JSON |
| On-Device AI | Google AI Edge SDK (Gemini Nano) |
| Cloud AI | Retrofit + OkHttp |
| Min SDK | 24 (Android 7.0) |
| Target SDK | 34 (Android 14) |

---

## Getting Started

### Prerequisites

- Android device or emulator (Android 7.0+)
- Java 17+ for building
- (Optional) API keys for cloud AI providers

### Build & Install

```bash
# Clone and navigate
cd android-app

# Build debug APK
JAVA_HOME=/path/to/jdk17 ./gradlew assembleDebug

# Install via ADB
adb install app/build/outputs/apk/debug/app-debug.apk
```

Or open in Android Studio and click Run.

### First Launch

1. **Home screen** shows your empty profile and AI status
2. Navigate to **Profile** tab
3. **Add your favorite strains** using the text field or quick-add grid
4. Return to **Search** tab
5. **Search any strain** to see your match score

No account required. No backend server. Just you and your preferences.

---

## The Database

The app ships with 50+ carefully curated strains with lab-verified terpene profiles:

**Indicas**: Granddaddy Purple, Northern Lights, Purple Punch, Bubba Kush, Hindu Kush...

**Sativas**: Jack Herer, Durban Poison, Green Crack, Sour Diesel, Super Lemon Haze...

**Hybrids**: Blue Dream, OG Kush, Gelato, Wedding Cake, GSC, Zkittlez, Runtz...

Each strain includes:
- Complete terpene profile (11 compounds)
- Effect descriptors
- Medical use cases
- THC/CBD ranges
- Flavor and aroma notes
- Optimal time of day
- Suggested activities

Unknown strains can be generated via AI and automatically saved to your local database for future searches.

---

## Strain Data Sources

The app uses a multi-tier approach to fetch strain data, prioritizing reliability and accuracy:

### Data Source Hierarchy

| Priority | Source | Description |
|----------|--------|-------------|
| 1 | **Embedded Database** | 30+ strains with curated terpene profiles, ships with the app |
| 2 | **Local Cache** | Previously fetched strains stored locally |
| 3 | **Cannlytics API** | Primary external API for real strain data |
| 4 | **Fallback Config** | 22+ curated strains for when API is unavailable |
| 5 | **LLM Generation** | AI-generated profiles as last resort |

### Cannlytics API

The app integrates with the [Cannlytics](https://cannlytics.com) public strain database API:

```
GET https://cannlytics.com/api/data/strains/{strain-name}
GET https://cannlytics.com/api/data/strains?limit=20
```

The API provides:
- Terpene profiles (myrcene, limonene, caryophyllene, pinene, etc.)
- THC/CBD content ranges
- Effects and aromas
- Strain descriptions

### Database Maintenance Scripts

Scripts in the `scripts/` directory manage the embedded strain database:

```bash
# List configured strains
python3 scripts/build_strain_database.py --list

# Add a new strain to config
python3 scripts/build_strain_database.py --add-strain "Strain Name"

# Rebuild database from API + fallbacks
python3 scripts/build_strain_database.py
```

Configuration: `scripts/strain_config.json`
- `strains`: List of strain names to include
- `fallback_strains`: Curated data for strains not in API

Output: `app/src/main/assets/strains.json`

### Fallback Strategy

When the Cannlytics API doesn't have data for a strain:
1. **Build script**: Uses fallback data from `strain_config.json`
2. **Runtime**: Uses embedded fallback strains in `StrainApiService.kt`
3. **Last resort**: LLM generates the profile (if configured)

This ensures the app works reliably even when external APIs are unavailable.

---

## For the Technically Curious

### Why These Specific Algorithms?

**Cosine similarity** is standard for high-dimensional sparse vectors (think: TF-IDF in search engines). Terpene profiles are exactly that—11 dimensions where many values are near zero.

**Z-score normalization** prevents dominant terpenes (myrcene, caryophyllene) from overshadowing trace compounds that may still influence your experience.

**Ensemble weighting** (50/25/25) was empirically tuned—cosine captures the "shape" of preferences, while Euclidean and correlation catch edge cases the cosine misses.

### Why MAX Pooling for Profile Creation?

Average pooling would dilute strong preferences. If you love one strain with 0.85 myrcene and one with 0.20, averaging to 0.52 doesn't represent what you seek.

MAX pooling says: "You've shown you can enjoy high myrcene. Keep that ceiling." It captures the upper bound of what works for you.

### Why On-Device First?

1. **Privacy**: Cannabis preferences are sensitive. They shouldn't live on someone else's server.
2. **Speed**: No network latency for calculations. Analysis is instant.
3. **Reliability**: Works offline, on planes, in areas with poor connectivity.
4. **Cost**: No API fees for daily use.

---

## CI/CD with GitHub Actions

The project includes GitHub Actions workflows for automated builds.

### Workflows

| Workflow | Trigger | Output |
|----------|---------|--------|
| **CI Build** | Push to `main`, PRs | Debug APK + Lint report |
| **Release Build** | Git tags (`v*`), Manual | Signed AAB for Google Play |

### Setting Up Release Builds

To enable signed release builds, add these secrets to your GitHub repository:

1. **Generate a keystore** (if you don't have one):
   ```bash
   keytool -genkey -v -keystore release-keystore.jks -keyalg RSA \
     -keysize 2048 -validity 10000 -alias release
   ```

2. **Encode keystore to base64**:
   ```bash
   base64 -i release-keystore.jks | pbcopy  # macOS
   base64 -w 0 release-keystore.jks         # Linux
   ```

3. **Add GitHub Secrets** (Settings → Secrets → Actions):
   | Secret | Description |
   |--------|-------------|
   | `KEYSTORE_BASE64` | Base64-encoded keystore file |
   | `KEYSTORE_PASSWORD` | Keystore password |
   | `KEY_ALIAS` | Key alias (e.g., `release`) |
   | `KEY_PASSWORD` | Key password |

### Creating a Release

```bash
# Tag and push to trigger release build
git tag v1.0.0
git push origin v1.0.0
```

Or use **Actions → Build Release AAB → Run workflow** for manual builds.

---

## Roadmap

- [ ] Strain journal (log your experiences with ratings)
- [ ] Dispensary menu scanner (OCR + batch analysis)
- [ ] Terpene tolerance tracking over time
- [ ] Export profile for sharing or backup
- [ ] Widget for quick strain lookup
- [ ] Wear OS companion

---

## Contributing

Issues and PRs welcome. The core algorithm lives in `LocalAnalysisEngine.kt` if you want to experiment with different similarity metrics.

---

## Disclaimer

This app is for educational and informational purposes. Cannabis laws vary by jurisdiction. Always comply with local regulations. Consult healthcare professionals for medical advice.

The terpene data and effects are based on general research and may vary by batch, grower, and individual physiology. Your experience may differ.

---

## How This Was Built

Curious about the development process? Read **[BUILDING.md](BUILDING.md)** — a chronicle of human-AI collaboration that took this project from a Python web app to a privacy-first Android application with on-device AI.

Topics covered:
- The evolution from server-dependent to fully offline
- Algorithm iterations and why we chose ensemble similarity
- Architectural decisions and trade-offs
- What it's like to pair-program with Claude

---

**Built with Kotlin, Compose, and a deep appreciation for the entourage effect.**
