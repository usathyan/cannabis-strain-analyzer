# How BudMash Was Built

*A story of terpenes, technology, and thoughtful iteration*

---

## The Starting Point

It began with an Android app called **Terpene Profile Analyzer**—a Kotlin/Compose application that used terpene chemistry to match cannabis strains against a user's preference profile. The science was solid: z-score normalization, cosine similarity, Euclidean distance, Pearson correlation—the same mathematical foundations used in recommendation engines and machine learning.

The app worked. It had 50+ curated strains with lab-verified terpene data. It supported on-device AI via Gemini Nano. It respected privacy by keeping all calculations local.

But there was a vision for something more.

---

## The Brainstorming Session

The conversation started simply: "Let's build this project I just cloned."

After a successful build and install, the real question emerged: *What's next?*

The initial feature list was ambitious:
- Cross-platform iOS/iPad support
- Web crawling dispensary menus
- AI-powered strain extraction
- Agentic dashboards with progressive analysis

But feature lists are just starting points. The real design emerges through questions.

### Question by Question

**Cross-platform approach?** Three options presented: Flutter (complete rewrite), React Native (JavaScript ecosystem), or Kotlin Multiplatform Mobile. The choice was KMM—it preserved the existing Kotlin investment while enabling iOS support.

**Target devices?** Universal iOS app for both iPhone and iPad. Maximum reach, single codebase.

**How should menu crawling work?** Template-based parsing was considered, but the existing LLM infrastructure made AI-powered universal parsing the natural choice. Why maintain templates when the AI can adapt to any dispensary format?

**Where should crawling run?** On-device via Ktor HTTP. Simple, private, no backend costs.

**MVP scope?** URL-only entry (maps integration later), flower products only (where terpene science shines brightest).

### The Pivot That Changed Everything

Then came a crucial clarification about the dashboard experience:

> "This is one of the reasons I called it agent, because from a user experience, we can keep the UI app interface as-is, but add an agent button... where all the new experience happens."

This sparked a bigger realization. Maybe this wasn't about adding features to the existing app. Maybe it was about building something new.

> "The new agentic UI I have in mind should likely be a new app, that shares the same codebase."

But then an even clearer vision emerged:

> "I am thinking maybe, the new app is a superset app... We do need the old app to create/save profiles, and for search strains, deep analysis etc, but they are all not designed for this casual end user... So, I propose, we redesign this entire flow, and make this a new seamless experience."

One unified app. Primary flow for the new agentic experience. Advanced features accessible but not in the way.

### Knowing Your User

Perhaps the most important refinement came when discussing the onboarding experience. The initial proposal was a quick-start guided flow with preference questions:

> "Do you prefer relaxing or energizing? Fruity or earthy?"

The response was immediate and clear:

> "This is not the right product I have in mind. My consumer is a more educated user, not a beginner. They know about names of strains, and expert level knowledge on each strain."

This changed everything. No hand-holding. No dumbed-down interfaces. The target user is the cannabis connoisseur who currently wastes hours cross-referencing dispensary menus with strain databases. They want scientific precision, data density, and respect for their expertise.

The onboarding became: paste a URL, start working. Profile builds organically as you mark strains you've tried.

---

## The Name

Names matter. "Terpene Profile Analyzer" was accurate but clinical. "Dispensary Scout" emphasized the new use case but felt generic.

Then: **BudMash**.

Short. Catchy. A playful nod to "mashing" data together. It stuck.

---

## The Architecture

With requirements crystallized, the technical architecture fell into place:

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
│  │  • HTTP client (Ktor)                     │  │
│  │  • LLM integration                        │  │
│  └───────────────────────────────────────────┘  │
├─────────────────────────────────────────────────┤
│  Platform-Specific                              │
│  • Local storage                                │
│  • LLM providers                                │
└─────────────────────────────────────────────────┘
```

**Kotlin Multiplatform** for shared business logic. The analysis engine, data models, and HTTP client work identically on Android and iOS.

**Compose Multiplatform** for shared UI. One codebase for both platforms, with the pragmatic acknowledgment that iOS could migrate to SwiftUI later if needed.

**Ktor** for networking. Lightweight, Kotlin-native, with platform-specific engines (CIO for Android, Darwin for iOS).

---

## The Implementation

The plan was structured into 7 phases with 19 bite-sized tasks:

1. **Project Scaffold** - KMM structure, Gradle configuration, version catalog
2. **Core Data Models** - Strain data, user profiles, similarity results
3. **HTTP & Menu Parsing** - Ktor client, parser interface with progressive status
4. **Compose UI** - Theme, home screen, scan screen, dashboard
5. **Android Integration** - MainActivity, manifest, navigation
6. **Build & Test** - Gradle wrapper, APK build, device testing
7. **Documentation** - This very document

Each task followed TDD principles: write the failing test, implement the minimum to pass, commit. Frequent commits meant the history told the story of incremental progress.

The parallel session approach kept implementation focused. One session executed tasks mechanically. The orchestrating session maintained context and could course-correct.

14 commits later, BudMash was running on a physical device.

---

## What We Built (MVP)

The MVP delivers the core value proposition:

- **Home Screen**: Paste a dispensary URL (defaults to Aunt Mary's in Flemington, NJ)
- **Progressive Scan**: Watch the analysis unfold in real-time
- **Dashboard**: See all flower strains with terpene breakdowns
- **Profile Building**: Mark strains as liked/disliked to build your profile
- **Match Scores**: Once you have a profile, see similarity percentages

What it doesn't have yet (intentionally deferred):
- Actual LLM integration for menu extraction
- Terpene data resolution from APIs
- iOS build
- Advanced features menu
- Scan history

These are Phase 2+ features. The MVP proves the architecture and user flow work.

---

## Lessons Learned

### 1. Requirements Emerge Through Dialogue

The final product looked nothing like the initial feature list. It took 14 carefully sequenced questions to uncover what was really needed. Each question narrowed the possibility space while expanding understanding.

### 2. Know Your User (Really Know Them)

The pivot from "casual user onboarding" to "expert-first design" was crucial. A preference quiz would have been patronizing to the target audience. Understanding that the user is an expert who wants to *save time*, not *learn basics*, shaped every subsequent decision.

### 3. Superset Beats Feature Bloat

Rather than cramming new features into an existing app, building a new superset app created space for a fresh UX while preserving powerful tools. The advanced features exist—they're just not in the critical path.

### 4. Progressive Feedback Is Magic

The "agentic" experience isn't about AI autonomy—it's about visibility. Watching the app fetch, parse, extract, and resolve creates engagement. Users feel the work being done on their behalf.

### 5. TDD + Frequent Commits = Confidence

19 tasks, 14 commits. Each commit was a working checkpoint. When something broke, the blast radius was one task. When something worked, it was locked in.

---

## What's Next

The MVP is a foundation. The road ahead includes:

- ~~**LLM Integration**: Connect menu parsing to actual AI extraction~~ ✅ Done
- ~~**Terpene Resolution**: Link to Cannlytics API and local database~~ ✅ Done
- **iOS Build**: Compile and test on iPhone/iPad
  - Create Xcode project in `iosApp/`
  - Add SwiftUI wrapper to host Compose Multiplatform UI
  - Configure Info.plist and app entitlements
  - Link ComposeApp.framework
  - Test on iOS Simulator (no paid dev account needed)
- ~~**Strain Detail Screen**: Deep dive into individual strains~~ ✅ Done
- **Comparison Mode**: Side-by-side analysis
- **Scan History**: Remember previous dispensary scans
- **Maps Integration**: Find dispensaries by location

---

## Phase 2: From MVP to Real App (December 28, 2025)

The MVP proved the architecture. Phase 2 made it actually useful.

### Vision-Powered Menu Extraction

The first real feature: take a screenshot of any dispensary menu and extract strain data using AI vision.

**The Challenge:** Vision APIs (Gemini Flash, GPT-4V) have resolution limits. When you feed them a tall scroll screenshot—common when capturing a full dispensary menu—they downsample the image and lose text detail. Our first test only extracted 24 of 77 visible strains.

**The Solution:** Image chunking. For images taller than 4000px:
1. Split into ~3000px chunks with 200px overlap
2. Send each chunk to the vision model separately
3. Merge results, deduplicating strains that appear in overlap regions

This required platform-specific implementations:
- **Android**: `BitmapFactory` + `Bitmap.createBitmap()`
- **iOS**: `UIImage` + `CGImageCreateWithImageInRect()`

Expect/actual pattern for the win.

### JSON Recovery for Truncated Responses

Vision models sometimes return truncated JSON—they hit token limits mid-response. Rather than fail completely, we implemented multi-strategy recovery:

1. **Strategy 1**: Full object regex with all fields
2. **Strategy 2**: Name + type only
3. **Strategy 3**: Just names (last resort)
4. **AI Cleanup**: Use a text model to extract from the raw response

This ensures partial results are better than no results.

### Similarity Scoring

The core value proposition: given your terpene preferences, how well does a new strain match?

**Three algorithms, weighted:**
- **Z-Score Normalized Cosine Similarity** (50%): Profile shape comparison
- **Euclidean Distance** (25%): Absolute concentration matching
- **Pearson Correlation** (25%): Pattern validation

The z-score normalization was crucial. Raw terpene values vary wildly (Myrcene at 0.5% vs Terpinolene at 0.05%). Without normalization, common terpenes dominate. Z-scoring puts them all on equal footing.

### MAX Pooling for Profile Building

When building a user's ideal profile from liked strains, we use MAX pooling rather than averaging:
- You like Blue Dream (Myrcene: 0.4%, Limonene: 0.3%)
- You like Sour Diesel (Myrcene: 0.2%, Limonene: 0.5%)
- Your profile: Myrcene: 0.4%, Limonene: 0.5% (takes the MAX)

This preserves peak preferences. If you respond to high limonene in one strain and high myrcene in another, both are captured.

### Navigation That Works

A subtle but important fix: the back button from strain detail now returns to the results screen, not a blank search screen. Implemented via a `returnTo` field that tracks navigation history.

### Model Selection

Users can now choose their vision model in Settings:
- Gemini 2.0 Flash (default)
- Gemini 1.5 Flash
- Other OpenRouter-supported vision models

---

## Phase 3: Repository Restructure (December 28, 2025)

The original repo had grown organically:
- `android-app/` - Old Android-only app
- `budmash/` - New KMM project
- `legacy/` - Python backend prototype
- Various docs and scripts

This was confusing. The KMM project *is* the project now.

**The restructure:**
1. Remove `android-app/`, `legacy/`, old docs
2. Move `budmash/` contents to root
3. Update GitHub Actions workflows
4. Clean up `.gitignore`

Now the repo is just the app. Clean.

---

## The Human-AI Partnership

This project was built through collaboration. A human with domain expertise and product vision. An AI with technical knowledge and systematic execution. The brainstorming session wasn't a requirements dump—it was a conversation. Each question refined understanding. Each answer constrained the design space.

The implementation wasn't a prompt-and-pray—it was a structured plan executed in bite-sized, verifiable chunks. Parallel sessions kept concerns separated. Frequent commits created accountability.

This is what building software can look like: thoughtful, iterative, documented, and collaborative.

---

## Technical Details

**Repository**: `cannabis-strain-analyzer` (GitHub)

**Tech Stack**:
- Kotlin 2.0.21
- Kotlin Multiplatform Mobile
- Compose Multiplatform 1.7.1
- Ktor 2.3.12
- Kotlinx.serialization 1.7.3
- Koin 4.0.0 (DI)

**Target Platforms**:
- Android (Min SDK 26, Target SDK 34)
- iOS (arm64, x64, simulator)

**Design Documents**:
- `docs/plans/2025-12-27-budmash-design.md`
- `docs/plans/2025-12-27-budmash-mvp-implementation.md`

---

*Built with curiosity, chemistry, and code.*

*December 27, 2025*
