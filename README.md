<div align="center">

# BudMash

### Your Personal Terpene-Based Strain Matcher

*Find cannabis strains that match your chemistry, not just your guesses.*

[![Build](https://github.com/usathyan/cannabis-strain-analyzer/actions/workflows/ci.yml/badge.svg)](https://github.com/usathyan/cannabis-strain-analyzer/actions)
[![Release](https://img.shields.io/github/v/release/usathyan/cannabis-strain-analyzer?include_prereleases)](https://github.com/usathyan/cannabis-strain-analyzer/releases)
[![Platform](https://img.shields.io/badge/platform-Android%20%7C%20iOS-blue)](#)

---

<table>
<tr>
<td><img src="screenshots/01-my-profile.png" width="180" alt="Profile"></td>
<td><img src="screenshots/02-search.png" width="180" alt="Search"></td>
<td><img src="screenshots/04-strain-detail-top.png" width="180" alt="Match"></td>
<td><img src="screenshots/05-strain-detail-terpenes.png" width="180" alt="Details"></td>
</tr>
<tr>
<td align="center"><b>Your Profile</b></td>
<td align="center"><b>Scan Menu</b></td>
<td align="center"><b>Similarity Breakdown</b></td>
<td align="center"><b>Terpene Analysis</b></td>
</tr>
</table>

</div>

---

## The Problem

You walk into a dispensary. The menu has 50+ flower strains. Some you've tried, most you haven't. You know what you like—but how do you find *more* of it?

**BudMash solves this.** It builds a terpene profile from strains you've enjoyed, then scores every strain on the menu against your preferences using real chemistry.

---

## How It Works

### 1. Build Your Profile

Like strains you know you enjoy. BudMash extracts the terpene profile from each one and builds your ideal chemical signature using **MAX pooling**:

```
You like Blue Dream  → Myrcene: 0.4%, Limonene: 0.3%
You like Sour Diesel → Myrcene: 0.2%, Limonene: 0.5%
─────────────────────────────────────────────────────
Your Profile         → Myrcene: 0.4%, Limonene: 0.5%  ← takes the MAX
```

This preserves your peak preferences. If you respond well to high limonene in one strain and high myrcene in another, both are captured.

### 2. Scan Any Dispensary Menu

Take a screenshot of your dispensary's flower menu. BudMash uses AI vision to extract every strain, then fetches terpene data for each one.

### 3. Get Match Scores

Every strain is scored against your profile. A 93% match means the chemistry aligns with what you've liked before.

<div align="center">
<table>
<tr>
<td><img src="screenshots/03-scan-results.png" width="250" alt="Results"></td>
<td width="40"></td>
<td><img src="screenshots/04-strain-detail-top.png" width="250" alt="Details"></td>
</tr>
<tr>
<td align="center"><em>Strains ranked by match %</em></td>
<td></td>
<td align="center"><em>Detailed similarity breakdown</em></td>
</tr>
</table>
</div>

---

## The Science

BudMash uses three complementary algorithms weighted together:

| Algorithm | Weight | What It Measures |
|-----------|--------|------------------|
| **Z-Score Cosine** | 50% | Profile shape—do you emphasize the same terpenes? |
| **Euclidean Distance** | 25% | Absolute concentrations—are the amounts similar? |
| **Pearson Correlation** | 25% | Pattern matching—do the profiles move together? |

### Why Z-Score Normalization?

Raw terpene percentages vary wildly. Myrcene might be 0.5% while Terpinolene is 0.05%. Without normalization, high-concentration terpenes dominate.

**Z-scoring puts all terpenes on equal footing.** Your preference for rare Terpinolene matters as much as common Myrcene.

<details>
<summary><b>Click for the math</b></summary>

<br>

**Z-Score Normalization:**
```
z = (value - mean) / standard_deviation
```

**Cosine Similarity:**
```
similarity = dot(A, B) / (||A|| × ||B||)
```

**Combined Score:**
```
score = (0.50 × cosine) + (0.25 × euclidean) + (0.25 × pearson)
```

A strain scoring 90%+ matches both the *shape* and *intensity* of your preferences.

</details>

---

## Terpene Reference

| Terpene | Aroma | Effects |
|---------|-------|---------|
| **Myrcene** | Earthy, musky | Relaxation, sedation |
| **Limonene** | Citrus | Mood elevation, stress relief |
| **Caryophyllene** | Pepper, spicy | Anti-inflammatory, calming |
| **Pinene** | Pine | Alertness, memory |
| **Linalool** | Lavender | Anxiety relief, relaxation |
| **Humulene** | Hoppy | Appetite suppression |
| **Terpinolene** | Floral, herbal | Uplifting, antioxidant |

---

## Quick Start

<table>
<tr>
<td width="50%" valign="top">

### Step 1: Configure

Go to **Settings** and enter your OpenRouter API key. Select your preferred vision model.

<img src="screenshots/06-settings.png" width="200" alt="Settings">

</td>
<td width="50%" valign="top">

### Step 2: Build Profile

Like 3-5 strains you know you enjoy. Your terpene profile builds automatically.

<img src="screenshots/01-my-profile.png" width="200" alt="Profile">

</td>
</tr>
</table>

<table>
<tr>
<td width="50%" valign="top">

### Step 3: Scan Menu

Take a screenshot of your dispensary's flower menu and select it in BudMash.

<img src="screenshots/02-search.png" width="200" alt="Search">

</td>
<td width="50%" valign="top">

### Step 4: Find Matches

See every strain scored against your profile. Tap for detailed analysis.

<img src="screenshots/05-strain-detail-terpenes.png" width="200" alt="Detail">

</td>
</tr>
</table>

---

## Tips

- **Like 3-5 strains** before scanning for best results
- **Tall screenshots** are handled automatically via image chunking
- **Dislike strains too**—tracking what you avoid helps you remember
- Your profile **evolves** as you like more strains

---

## Privacy

- Terpene profile stored locally on your device
- API keys stored in device secure storage

---

## Download

| Platform | Download |
|----------|----------|
| Android | [Latest APK](https://github.com/usathyan/cannabis-strain-analyzer/releases/latest) |
| macOS | [Latest DMG](https://github.com/usathyan/cannabis-strain-analyzer/releases/latest) |

---

<div align="center">

*Built with terpenes, chemistry, and code.*

**[Developer Guide](DEVELOPER.md)** · **[How It Was Built](HOW_THIS_WAS_BUILT.md)**

</div>
