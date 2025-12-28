# BudMash Design Document

**Date:** 2025-12-27
**Status:** Approved

## Overview

BudMash is a cross-platform mobile app (iOS + Android) that helps expert cannabis users find their ideal strains at local dispensaries. Users paste a dispensary menu URL, and the app progressively analyzes the flower inventory, matching strains against their terpene preference profile.

### Target User

- Expert cannabis users with deep strain knowledge
- Currently wastes time cross-referencing multiple websites
- Wants scientific precision, not hand-holding
- Values data density and terpene chemistry

### Core Value Proposition

> "Stop wasting hours cross-referencing dispensary menus with strain databases. Paste a URL, see terpene science for every flower, find your matches."

---

## Architecture

### High-Level Structure

```
┌─────────────────────────────────────────────────┐
│               BudMash App                       │
│         (Compose Multiplatform UI)              │
├─────────────────────────────────────────────────┤
│  ┌───────────────────────────────────────────┐  │
│  │         Shared KMM Module                 │  │
│  │  • Terpene analysis engine                │  │
│  │  • Similarity algorithms                  │  │
│  │  • Profile management                     │  │
│  │  • Strain data models                     │  │
│  │  • HTTP client (Ktor)                     │  │
│  │  • LLM integration                        │  │
│  └───────────────────────────────────────────┘  │
├─────────────────────────────────────────────────┤
│  Platform-Specific                              │
│  • Local storage (SharedPrefs / UserDefaults)   │
│  • LLM providers (Gemini Nano on Android)       │
└─────────────────────────────────────────────────┘
```

### Technology Choices

| Component | Technology |
|-----------|------------|
| Shared Logic | Kotlin Multiplatform (KMM) |
| Shared UI | Compose Multiplatform |
| HTTP Client | Ktor |
| Menu Parsing | AI-powered (LLM) |
| Local Storage | SharedPreferences (Android) / UserDefaults (iOS) |
| LLM Providers | Gemini Nano (on-device), OpenAI, Anthropic, Ollama |

---

## User Flows

### Flow 1: First Launch

No onboarding quiz. No setup. Expert users get straight to value.

```
┌─────────────────────────────────────────────────┐
│  Welcome to BudMash                             │
│                                                 │
│  Paste a dispensary menu URL to get started.   │
│                                                 │
│  ┌─────────────────────────────────────────┐   │
│  │ https://auntmarysnj.co/order-online/... │   │
│  └─────────────────────────────────────────┘   │
│                                                 │
│  Your profile builds as you mark strains       │
│  you've tried.                                  │
└─────────────────────────────────────────────────┘
```

Default URL: `https://auntmarysnj.co/order-online/?selected_value=rec`

### Flow 2: Dispensary Scan (Progressive Analysis)

```
Step 1: Fetch          "Fetching menu..."
Step 2: Categorize     "Found 47 products: 32 flower, 10 vapes, 5 edibles"
Step 3: Extract        "Extracting flower data..." (flower only for MVP)
Step 4: Analyze        "Building terpene profiles for 32 strains..."
Step 5: Save           "Saved to local database"
Step 6: Display        Full data table with all strains + terpene breakdowns
```

### Flow 3: Building Profile From Scans

- User sees full strain list from dispensary
- Marks strains: ✓ "Tried & Liked" / ✗ "Tried & Disliked" / unmarked
- Liked strains feed into profile (MAX pooling)
- Once profile exists, future scans show match percentages

---

## Dashboard & Data Display

### Main View: Strain Table

```
┌─────────────────────────────────────────────────────────────────────┐
│ Aunt Mary's - Flemington, NJ                    32 flowers found   │
├─────────────────────────────────────────────────────────────────────┤
│ Filter: [All] [Indica] [Sativa] [Hybrid]    Sort: [Match %] [Name] │
├──────────────────┬────────┬────────────────────────────┬───────────┤
│ Strain           │ Match  │ Top Terpenes               │ Tried?    │
├──────────────────┼────────┼────────────────────────────┼───────────┤
│ Gelato           │  87%   │ Myrcene 0.8, Limonene 0.6  │ [✓] [✗]   │
│ Wedding Cake     │  84%   │ Caryophyllene 0.7, Myrc... │ [✓] [✗]   │
│ Blue Dream       │  81%   │ Myrcene 0.6, Pinene 0.5    │ [✓] [✗]   │
│ OG Kush          │  79%   │ Myrcene 0.7, Limonene 0.4  │ [✓] [✗]   │
│ ...              │        │                            │           │
└──────────────────┴────────┴────────────────────────────┴───────────┘
```

### Strain Detail Panel

- Full 11-terpene breakdown with bar chart
- THC/CBD ranges
- Effects, flavors (from data source or AI-generated)
- Scientific similarity breakdown (Cosine/Euclidean/Pearson)
- "Add to Profile" / "Mark as Disliked"

### Top Matches Section (once profile exists)

- Highlighted section showing strains >80% match
- Quick comparison against user's ideal terpene profile
- Visual delta: "Higher in myrcene (+12%), lower in pinene (-8%)"

---

## Menu Parsing Pipeline

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│  User pastes │────▶│  Ktor HTTP   │────▶│  Raw HTML    │
│  URL         │     │  fetch       │     │  content     │
└──────────────┘     └──────────────┘     └──────┬───────┘
                                                 │
                                                 ▼
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│  Strain list │◀────│  LLM parses  │◀────│  Clean HTML  │
│  with data   │     │  to JSON     │     │  (strip nav, │
│              │     │              │     │   ads, etc.) │
└──────────────┘     └──────────────┘     └──────────────┘
```

### LLM Prompt Strategy

```
Extract all flower/bud products from this dispensary menu HTML.
Return JSON array with:
- name: strain name
- type: indica/sativa/hybrid
- thc_range: [min, max] percentage
- cbd_range: [min, max] percentage
- price: price per unit if available
- description: any effects/flavor notes listed

Only include flower products. Ignore edibles, vapes, etc.
```

### Terpene Data Resolution

1. Check local embedded database (50+ strains)
2. Check user's cached strains
3. Query external API (Cannlytics)
4. Fall back to LLM generation for unknown strains

---

## Advanced Features

Accessible via hamburger menu:

| Feature | Description |
|---------|-------------|
| My Profile | View/edit terpene profile, see contributing strains |
| Manual Strain Search | Search any strain by name, deep analysis |
| Saved Strains | Local database management, export/import |
| Compare Strains | Side-by-side comparison (2-4 strains) |
| Settings | LLM provider config, API keys, theme |
| Scan History | Previous dispensary scans, quick re-scan |

---

## Project Structure

```
budmash/
├── shared/                     # KMM shared module
│   └── src/
│       ├── commonMain/         # Shared Kotlin code
│       │   ├── analysis/       # Terpene analysis engine
│       │   ├── data/           # Models, repositories
│       │   ├── network/        # Ktor HTTP client
│       │   ├── llm/            # LLM integration
│       │   └── parser/         # Menu HTML → JSON
│       ├── androidMain/        # Android-specific impl
│       └── iosMain/            # iOS-specific impl
│
├── androidApp/                 # Android application
│   └── src/main/
│       ├── ui/                 # Compose UI
│       └── MainActivity.kt
│
├── iosApp/                     # iOS application
│   └── Sources/
│       └── ContentView.swift   # SwiftUI wrapper (minimal)
│
├── composeApp/                 # Compose Multiplatform UI
│   └── src/
│       └── commonMain/
│           ├── screens/        # Home, Dashboard, Detail, Settings
│           ├── components/     # Shared UI components
│           └── theme/          # Colors, typography
│
└── legacy/                     # Reference to old app (deprecated)
```

### Migration from Current App

| Current Location | New Location |
|-----------------|--------------|
| `LocalAnalysisEngine.kt` | `shared/analysis/` |
| `Models.kt` | `shared/data/` |
| `LlmService.kt` | `shared/llm/` |
| `StrainDataService.kt` | `shared/data/` |
| UI screens | `composeApp/screens/` (rewritten) |

---

## Phased Delivery

### MVP (Phase 1) - Core Value

| Feature | Included |
|---------|----------|
| Paste dispensary URL | ✓ |
| Progressive fetch + parse | ✓ |
| AI-powered menu extraction (flower only) | ✓ |
| Strain table with terpene data | ✓ |
| Mark tried/liked to build profile | ✓ |
| Match % once profile exists | ✓ |
| Strain detail view with terpene chart | ✓ |
| Local strain caching | ✓ |
| LLM provider settings | ✓ |
| Default URL: Aunt Mary's | ✓ |
| iOS + Android builds | ✓ |

### Phase 2 - Enhanced Dashboard

- Side-by-side strain comparison
- Top 10 matches (>80%) summary view
- Scan history with bookmarks
- Export/share profile

### Phase 3 - Discovery

- Dispensary search by zip/address (Google Maps API)
- Auto-detect menu URL from dispensary selection
- Multiple dispensary comparison

### Phase 4 - Intelligence

- AI-generated experience predictions
- "Why this matches" explanations
- Personalized shopping reports (PDF export)

---

## Key Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Cross-platform approach | KMM + Compose Multiplatform | Leverage existing Kotlin codebase, single UI |
| Target devices | Universal (iPhone, iPad, Android) | Maximum reach |
| Menu parsing | AI-powered (LLM) | Handles any dispensary format |
| Crawling location | On-device via Ktor | Privacy, simplicity |
| MVP entry | URL-only | Focus on core value first |
| Product scope | Flower only (MVP) | Where terpene science matters most |
| Target user | Expert cannabis user | Scientific precision over simplification |
| Profile building | Emerges from marking tried strains | No setup friction |
| Current app fate | Deprecated, replaced by BudMash | Clean migration |

---

## Current App Deprecation

The existing "Terpene Profile Analyzer" Android app will be deprecated once BudMash reaches feature parity. Migration path:

1. BudMash MVP ships with core analysis features
2. Users can export profiles from old app (Phase 2)
3. Old app enters maintenance mode
4. Eventually removed from Play Store

---

## Open Questions for Implementation

1. **Compose Multiplatform iOS maturity** - May need to evaluate and fall back to SwiftUI if issues arise
2. **LLM token costs** - Menu HTML can be large; may need HTML preprocessing to reduce tokens
3. **Dispensary bot protection** - Some sites may block direct fetches; may need proxy fallback later
4. **App Store approval** - Cannabis-related apps have restrictions; research iOS guidelines

---

*Design approved: 2025-12-27*
