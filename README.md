# Strain Analyzer

A privacy-focused Android app for analyzing cannabis strain compatibility based on your personal preferences. All similarity calculations run **on-device** - no data leaves your phone.

## What It Does

Strain Analyzer helps you find strains that match your preferences by:

1. **Learning your taste**: You add strains you've enjoyed to your profile
2. **Analyzing new strains**: Enter any strain name to see how well it matches
3. **Explaining the match**: See detailed similarity breakdowns and recommendations

## How Analysis Works

### The Match Percentage

When you analyze a strain, you see a **match percentage (0-100%)**. This score represents how similar the strain's chemical profile is to your "ideal profile" - which is derived from your favorite strains.

**Score Interpretation:**
| Score | Rating | Meaning |
|-------|--------|---------|
| 90-100% | Perfect Match | Very similar to your favorites |
| 75-89% | Excellent Match | Strong alignment with your preferences |
| 60-74% | Good Match | Notable similarities with some differences |
| 45-59% | Moderate Match | Mixed - some overlap, some differences |
| 30-44% | Fair Match | Noticeably different from your usual preferences |
| 0-29% | Different Profile | Quite different from what you typically enjoy |

### What Gets Compared

The analysis compares **terpene profiles** - the aromatic compounds that give each strain its unique effects and flavors:

- **Myrcene**: Sedative, relaxing (earthy, musky)
- **Limonene**: Uplifting, mood-enhancing (citrus)
- **Caryophyllene**: Anti-inflammatory, calming (spicy, peppery)
- **Pinene**: Alertness, focus (pine)
- **Linalool**: Calming, anti-anxiety (floral, lavender)
- **Humulene**: Appetite control (hoppy)
- **Terpinolene**: Uplifting, creative (herbal)
- And more...

### How Scores Are Calculated

The app uses **four mathematical similarity metrics**, all calculated on-device:

#### 1. Z-Scored Cosine Similarity (50% weight)
Measures the angle between terpene profile vectors after normalizing for scale differences. This captures whether two strains have similar *relative* terpene proportions, regardless of overall intensity.

```
Formula: cos(θ) = (A·B) / (|A| × |B|)
Where A and B are Z-score normalized terpene vectors
```

#### 2. Euclidean Similarity (25% weight)
Measures the straight-line distance between profiles in terpene space. Closer = more similar.

```
Formula: 1 / (1 + √Σ(ai - bi)²)
Converted from distance to similarity (0-1 scale)
```

#### 3. Correlation Similarity (25% weight)
Measures whether terpene levels move together - when one strain is high in myrcene and low in limonene, does your profile show the same pattern?

```
Formula: Pearson correlation coefficient, normalized to 0-1
```

#### 4. Combined Score
```
Overall = (Z-Cosine × 0.50) + (Euclidean × 0.25) + (Correlation × 0.25)
```

### Your Ideal Profile

Your "ideal profile" is created from your favorite strains using **MAX pooling**:
- For each terpene, we take the highest value across all your favorites
- This creates a profile representing the best characteristics you enjoy
- More favorites = more refined profile

## App Features

### Home Tab
- Overview of your profile
- Quick access to analysis

### Configure Tab
- Add/remove favorite strains
- Your favorites define your ideal terpene profile

### Compare Tab
- Analyze any strain
- See match score, breakdown, and recommendations
- Option to enhance analysis with AI (if configured)

### Settings Tab
- Configure LLM provider for unknown strains
- Supports: OpenAI, Anthropic, Google, Groq, OpenRouter, Ollama

## Adding New Strains

### If Strain is in Database
1. Enter strain name → Instant analysis
2. All calculations run locally
3. No internet required

### If Strain is NOT in Database

**With API configured (Settings → LLM Provider):**
1. Enter strain name
2. App automatically fetches strain data via your configured LLM
3. Strain is permanently saved to your local database
4. Future lookups are instant (offline)

**Without API:**
- You'll see a message to configure an API provider
- The app ships with 21 common strains built-in

### Flow Example (Pixel phone with Gemini API):

```
1. User enters "Gelato 41"
2. App checks local database → Not found
3. App calls Gemini API → "Generate strain profile for Gelato 41"
4. Gemini returns: type, THC/CBD range, terpene percentages, effects
5. App saves this to local database permanently
6. App runs similarity calculations locally
7. User sees: 78% match, "Good Match"
8. Next time: "Gelato 41" loads instantly from local storage
```

## Privacy & Architecture

### What Runs Locally (Always)
- All similarity calculations (Z-score, cosine, euclidean, correlation)
- Strain database lookups
- User profile storage
- Recommendation generation (template-based)

### What Uses API (Optional)
- Fetching unknown strain data (one-time, then cached forever)
- Enhanced AI recommendations (optional "Enhance with AI" button)

### Data Storage
- Favorites: SharedPreferences
- Generated strains: SharedPreferences (custom_strains)
- LLM config: SharedPreferences (encrypted API keys)

## Building the App

### Requirements
- Android Studio or command line with Gradle
- Java 17
- Android SDK

### Build
```bash
cd android-app
JAVA_HOME=/path/to/java17 ./gradlew assembleDebug
```

### Install
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Project Structure

```
strain-analyzer/
├── android-app/          # Main Android application
│   ├── app/src/main/
│   │   ├── kotlin/       # Kotlin source code
│   │   │   ├── analysis/ # LocalAnalysisEngine (similarity calculations)
│   │   │   ├── data/     # StrainDataService (fetching/caching)
│   │   │   ├── llm/      # LLM providers (OpenAI, Anthropic, etc.)
│   │   │   └── ui/       # Compose UI screens
│   │   └── assets/
│   │       └── strains.json  # Embedded strain database
├── legacy/               # Retired web backend (reference only)
└── screenshots/          # App screenshots
```

## Legacy Web Backend

The original Python/FastAPI backend has been retired and moved to `legacy/web-backend/`. The Android app now contains all functionality:
- Strain database embedded in app
- All calculations run on-device
- LLM integration for unknown strains

The legacy backend is preserved for reference but is no longer required.

## License

MIT License - See LICENSE file

---

**Note**: This app is for informational purposes only. Cannabis laws vary by jurisdiction. Always comply with local regulations.
