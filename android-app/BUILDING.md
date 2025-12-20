# How This App Was Built

**A story of human-AI collaboration, architectural evolution, and the pursuit of on-device intelligence.**

---

## The Collaboration

This app was built through an iterative dialogue between a human developer and Claude, Anthropic's AI assistant. The human brought domain expertise (cannabis science, product vision) and real-world constraints (device compatibility, privacy requirements). Claude brought software engineering, algorithm design, and the ability to rapidly prototype across languages and frameworks.

Every commit in this repository was pair-programmed. The human described what they wanted. Claude wrote the code, explained trade-offs, caught edge cases, and suggested improvements. When something broke, we debugged together. When requirements changed, we refactored together.

This document chronicles that journey—from a Python web app to a privacy-first Android application with on-device AI.

---

## Timeline

### Phase 1: The Web Era (September 2025)

**September 17, 2025 — Genesis**

The project began as a Python/FastAPI web application. The core idea was simple: help users find cannabis strains that match their preferences based on terpene chemistry.

```
Initial commit: Cannabis strain recommendation system
with personalized web interface
```

The first version had:
- A terpene database with strain profiles
- Google OAuth for user authentication
- HTML templates rendered server-side
- SQLAlchemy for user data persistence

**The First Refactor — Simplicity**

Within hours, the OAuth complexity proved unnecessary for the core use case. Claude suggested stripping it out:

```
Create clean, simple cannabis strain analyzer
- Remove all unnecessary features and complexity
- Implement ONLY the requested functionality
- No authentication, no location features
- Focus on core terpene analysis
```

This became a pattern: start with ambition, discover what's essential, cut ruthlessly.

**September 19, 2025 — The Algorithm Deepens**

The similarity calculation evolved significantly. The original used basic cosine similarity, but users reported unexpected results. A strain that was in your favorites showing low similarity to itself? That's a bug.

The problem: raw terpene percentages have vastly different scales. Myrcene can be 0.85% while bisabolol is 0.02%. Simple cosine similarity was being dominated by high-concentration terpenes.

Solution: **Z-score normalization** before comparison.

```kotlin
fun zScore(vector: List<Double>): List<Double> {
    val mean = vector.average()
    val std = sqrt(vector.map { (it - mean).pow(2) }.average())
    return vector.map { (it - mean) / std }
}
```

But z-scoring alone wasn't enough. We added an ensemble:
- Z-scored cosine similarity (50% weight)
- Euclidean distance (25% weight)
- Pearson correlation (25% weight)

```
Implement ranked favorites and z-score comparison system
- Add ranked favorites system for enhanced similarity analysis
- Implement z-score comparison using multiple profiles
- Update similarity algorithms to support ensemble metrics
```

**The Tabbed Interface**

User testing revealed a flow problem. Users wanted to:
1. First, tell us what they like
2. Then, search for new strains

Two distinct tasks. Two tabs.

```
Implement tabbed interface with Configuration and Compare tabs
- Configuration tab for selecting favorite strains
- Compare tab for analyzing strains against ideal profile
- File-based persistence for user profiles
```

### Phase 2: The Android Pivot (December 2025)

**December 19, 2025 — The Big Rewrite**

Three months later, the human returned with a new vision: **mobile-first, privacy-first**.

The web app required a running server. Users' preferences were stored on that server. For cannabis data, that's a privacy concern. The solution: move everything to the device.

```
Refactor to Android-first architecture with on-device analysis

Major changes:
- Add complete Android app with Jetpack Compose UI
- Implement LocalAnalysisEngine for on-device similarity calculations
- Embed strain database in app assets
- Move Python/FastAPI backend to legacy/ folder
```

This was a complete rewrite. Different language (Python → Kotlin), different framework (FastAPI → Jetpack Compose), different architecture (client-server → local-only).

Claude ported:
- The terpene database (JSON assets)
- The similarity algorithms (Z-score, cosine, euclidean, correlation)
- The profile management (SharedPreferences)
- The UI patterns (tabbed interface, strain cards, progress indicators)

**The LLM Integration**

The embedded database had ~50 strains. Users would search for strains not in the database. Options:

1. ❌ Say "not found" — bad UX
2. ❌ Require a backend — defeats privacy goal
3. ✅ Generate on-demand using LLM, then cache forever

Claude designed a multi-provider LLM abstraction:

```kotlin
interface LlmProvider {
    suspend fun generateStrainProfile(strainName: String): StrainData?
    suspend fun analyzeStrain(strain: String, context: Map<String, String>): String?
}
```

Implementations for:
- OpenAI (GPT-4, GPT-3.5)
- Anthropic (Claude)
- Google (Gemini)
- Groq (Llama)
- OpenRouter (various)
- Ollama (local models)

The user provides their own API key. The app makes one request per unknown strain, then stores the result locally forever. Subsequent lookups are instant and offline.

**Gemini Nano — True On-Device AI**

Then came the breakthrough: Google's Gemini Nano.

Pixel 8 Pro, Pixel 9, and newer devices have a Neural Processing Unit (NPU) that can run small language models directly on the device. No network request. No API key. Complete privacy.

```
Add Gemini Nano on-device AI support for Pixel 9+ devices
- Add ML Kit GenAI Prompt API dependency
- Create GeminiNanoService to detect and use Gemini Nano
- Update LlmService to prefer on-device AI over cloud APIs
- Add download button for devices that support Gemini Nano
```

The priority order became:
1. **Gemini Nano** (if available) — completely on-device
2. **Cloud API** (if configured) — user's own key
3. **Embedded database only** — works offline, limited strains

**Dark Mode — The Final Polish**

The human tested the app at night and found the UI unreadable. Dark mode wasn't about aesthetics—it was accessibility.

```
Fix dark mode compatibility across all screens
- Use MaterialTheme.colorScheme for backgrounds, surfaces, and text
- Add isSystemInDarkTheme() checks for accent colors
- Ensure all cards and text are visible in both light and dark modes
```

Every card, every chip, every text field was audited. Colors were chosen deliberately:
- Light mode: subtle greens and blues on white
- Dark mode: deeper jewel tones that don't strain eyes

---

## Technical Evolution

### Architecture Progression

```
Phase 1: Web (Python)
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Browser   │────▶│   FastAPI   │────▶│   SQLite    │
│   (HTML)    │     │   Server    │     │  Database   │
└─────────────┘     └─────────────┘     └─────────────┘

Phase 2: Android (Kotlin) — Current
┌─────────────────────────────────────────────────────┐
│                    Android Device                    │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  │
│  │  Compose UI │  │   ViewModel │  │  Local DB   │  │
│  │   (Tabs)    │──│  (StateFlow)│──│  (Prefs)    │  │
│  └─────────────┘  └──────┬──────┘  └─────────────┘  │
│                          │                           │
│           ┌──────────────┴──────────────┐           │
│           ▼                             ▼           │
│  ┌─────────────────┐         ┌─────────────────┐   │
│  │ LocalAnalysisEng│         │   LlmService    │   │
│  │ (Z-score,Cosine)│         │ ┌─────────────┐ │   │
│  └─────────────────┘         │ │ GeminiNano  │ │   │
│                              │ │ (On-Device) │ │   │
│                              │ └─────────────┘ │   │
│                              │ ┌─────────────┐ │   │
│                              │ │ Cloud APIs  │◀┼───┼─── Optional
│                              │ └─────────────┘ │   │
│                              └─────────────────┘   │
└─────────────────────────────────────────────────────┘
```

### Algorithm Evolution

| Version | Similarity Method | Problem Solved |
|---------|------------------|----------------|
| v0.1 | Raw cosine similarity | Baseline |
| v0.2 | Z-scored cosine | High-concentration terpene dominance |
| v0.3 | Ensemble (cosine + euclidean + correlation) | Edge cases missed by single metric |
| v0.4 | Weighted ensemble (50/25/25) | Empirical tuning for user satisfaction |

### Data Strategy Evolution

| Version | Strain Data Source | Trade-off |
|---------|-------------------|-----------|
| v0.1 | Hardcoded in Python | Fast but inflexible |
| v0.2 | JSON file on server | Editable but requires server |
| v0.3 | Embedded JSON in APK | Works offline, ships with app |
| v0.4 | Embedded + LLM generation | Unlimited strains, cached locally |
| v0.5 | Embedded + Gemini Nano | Unlimited strains, fully offline |

### UI Framework Evolution

| Phase | Technology | Paradigm |
|-------|------------|----------|
| Web v1 | Jinja2 templates | Server-rendered HTML |
| Web v2 | Jinja2 + JavaScript | Progressive enhancement |
| Android | Jetpack Compose | Declarative UI |

---

## Models & Techniques

### AI Models Used

**During Development**
- **Claude Opus 4.5** — Primary development partner. Wrote code, debugged, refactored. Every commit includes `Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>`.

**In Production (User-Configurable)**
- **Gemini Nano** — On-device, no internet, maximum privacy
- **Gemini Pro** — Google's cloud API
- **GPT-4 / GPT-3.5** — OpenAI's models
- **Claude Sonnet/Opus** — Anthropic's models
- **Llama** (via Groq/Ollama) — Open-source option

### Prompt Engineering

For strain generation, the prompt is structured to return parseable JSON:

```
Generate a detailed terpene profile for the cannabis strain "{name}".
Return JSON with:
- type: indica/sativa/hybrid
- thc_range: e.g., "18-24%"
- cbd_range: e.g., "0.1-0.5%"
- effects: [list of 3-5 effects]
- medical_effects: [list of 3-4 medical uses]
- flavors: [list of 3-4 flavors]
- description: one sentence
- terpenes: {myrcene: 0.0-1.0, limonene: 0.0-1.0, ...}
```

The prompt includes the expected terpene range (0.0-1.0) to ensure consistency with the embedded database format.

### Development Techniques

**Pair Programming with AI**
Every feature followed this pattern:
1. Human describes intent
2. Claude proposes implementation
3. Human reviews, asks questions
4. Claude refines
5. Human tests on device
6. Both debug together

**Incremental Refactoring**
Large changes were broken into commits:
- First: Add new architecture alongside old
- Then: Migrate functionality
- Finally: Remove old code

**Test-in-Context**
Without a formal test suite, testing happened through:
- Build → Install → Run → Report
- Claude interpreting stack traces
- Iterating until "it works"

---

## Lessons Learned

### What Worked

1. **Starting with the algorithm, not the UI**
   - The similarity calculation was the core value. Getting it right first made everything else easier.

2. **Privacy as architecture, not afterthought**
   - Deciding early that "no data leaves the device" shaped every technical choice.

3. **Multi-model abstraction**
   - The `LlmProvider` interface meant adding Gemini Nano was a single file change.

4. **Aggressive simplification**
   - OAuth was cool. It wasn't necessary. Cutting it made the app better.

### What Was Hard

1. **Dark mode consistency**
   - Jetpack Compose makes dark mode "easy" but still requires auditing every hardcoded color.

2. **Gemini Nano availability**
   - Device support is limited. The abstraction handles fallback gracefully.

3. **Terpene data quality**
   - Different sources report different percentages. We normalized to 0-1 scale.

### What's Next

- **Strain journal**: Log your experiences, track effects over time
- **Menu scanner**: OCR dispensary menus, batch analyze
- **Profile export**: Share your terpene preferences
- **Watch app**: Quick lookups from your wrist

---

## The Commits Tell the Story

Every commit in this repo is co-authored by human and AI. Read them chronologically to see the thinking evolve:

```bash
git log --oneline --reverse
```

From `8745274 Initial commit` to `a604377 Fix dark mode compatibility`, each message documents not just what changed, but why.

---

## Acknowledgments

- **The Human**: Vision, domain expertise, testing, and saying "this doesn't feel right"
- **Claude (Anthropic)**: Code, algorithms, architecture, and "have you considered..."
- **Google**: Gemini Nano for making true on-device AI possible
- **The Jetpack Compose Team**: For a UI framework that makes Android development joyful

---

**This is what human-AI collaboration looks like. Not replacement. Partnership.**

*Built with Claude Code — December 2025*
