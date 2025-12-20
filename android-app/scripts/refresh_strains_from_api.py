#!/usr/bin/env python3
"""
Refresh strains.json with real data from Cannlytics API
"""

import json
import requests
import time
import sys
from pathlib import Path

API_BASE = "https://cannlytics.com/api/data/strains"

# Terpene field mappings from API to our format
TERPENE_MAPPINGS = {
    "beta_myrcene": "myrcene",
    "d_limonene": "limonene",
    "beta_caryophyllene": "caryophyllene",
    "alpha_pinene": "pinene",
    "linalool": "linalool",
    "humulene": "humulene",
    "terpinolene": "terpinolene",
    "ocimene": "ocimene",
    "nerolidol": "nerolidol",
    "alpha_bisabolol": "bisabolol",
    "eucalyptol": "eucalyptol",
    "beta_pinene": "pinene",  # Combine with alpha
    "geraniol": "geraniol",
    "camphene": "camphene",
    "carene": "carene",
}

def fetch_strain(strain_name: str) -> dict | None:
    """Fetch strain data from Cannlytics API"""
    # Try URL-formatted name first
    url_name = strain_name.strip().replace(" ", "-").lower()

    try:
        # Try direct endpoint first
        response = requests.get(f"{API_BASE}/{url_name}", timeout=10)
        if response.status_code == 200:
            data = response.json()
            if data.get("success") and data.get("data"):
                return parse_strain(strain_name, data["data"])

        # Fall back to search
        response = requests.get(f"{API_BASE}?limit=50", timeout=10)
        if response.status_code == 200:
            data = response.json()
            if data.get("success") and data.get("data"):
                # Find best match
                normalized = strain_name.lower().strip()
                for strain in data["data"]:
                    api_name = strain.get("strain_name", "").lower()
                    if normalized in api_name or api_name in normalized:
                        return parse_strain(strain_name, strain)
                    # Check word matches
                    words = normalized.split()
                    if all(w in api_name for w in words):
                        return parse_strain(strain_name, strain)

        return None
    except Exception as e:
        print(f"  Error fetching {strain_name}: {e}")
        return None

def parse_strain(name: str, data: dict) -> dict:
    """Parse API response into our format"""
    # Extract terpenes
    terpenes = {}
    for api_key, our_key in TERPENE_MAPPINGS.items():
        value = data.get(api_key, 0.0)
        if value and value > 0:
            # Normalize to 0-1 scale if needed
            if data.get("total_terpenes", 0) > 10:
                value = value / 100.0
            if our_key in terpenes:
                terpenes[our_key] = max(terpenes[our_key], value)
            else:
                terpenes[our_key] = round(value, 4)

    # Extract effects
    effects = []
    for effect in data.get("potential_effects", []):
        clean = effect.replace("effect_", "").title()
        if clean:
            effects.append(clean.lower())

    # Extract aromas/flavors
    aromas = []
    for aroma in data.get("potential_aromas", []):
        clean = aroma.replace("aroma_", "").title()
        if clean:
            aromas.append(clean.lower())

    # Get cannabinoid data
    thc = data.get("delta_9_thc", 0) or data.get("total_thc", 0)
    cbd = data.get("cbd", 0) or data.get("total_cbd", 0)

    thc_range = f"{int(thc)}-{int(thc+3)}%" if thc > 0 else "Unknown"
    cbd_range = f"{cbd:.1f}-{cbd+0.5:.1f}%" if cbd > 0.1 else "<1%"

    # Infer type
    description = data.get("description", "")
    strain_type = infer_type(description, effects, name)

    # Infer medical effects
    medical = infer_medical_effects(effects, strain_type)

    # Infer time and activity
    best_time = "evening" if strain_type == "indica" else "daytime" if strain_type == "sativa" else "any"
    activity = "relaxation" if strain_type == "indica" else "focus" if strain_type == "sativa" else "mixed"

    return {
        "terpenes": terpenes,
        "effects": effects if effects else ["relaxed", "happy"],
        "medical_effects": medical,
        "type": strain_type,
        "thc_range": thc_range,
        "cbd_range": cbd_range,
        "description": description or f"A {strain_type} strain with balanced effects",
        "flavors": aromas if aromas else ["earthy"],
        "aromas": aromas if aromas else ["earthy"],
        "best_time": best_time,
        "activity": activity,
    }

def infer_type(description: str, effects: list, name: str) -> str:
    """Infer strain type from description and effects"""
    desc_lower = description.lower()
    name_lower = name.lower()

    if "indica" in desc_lower and "sativa" not in desc_lower:
        return "indica"
    if "sativa" in desc_lower and "indica" not in desc_lower:
        return "sativa"
    if "hybrid" in desc_lower:
        return "hybrid"

    indica_effects = {"sleepy", "relaxed", "sedated", "calm"}
    sativa_effects = {"energetic", "uplifted", "creative", "focused", "euphoric"}

    indica_count = sum(1 for e in effects if e in indica_effects)
    sativa_count = sum(1 for e in effects if e in sativa_effects)

    if indica_count > sativa_count:
        return "indica"
    if sativa_count > indica_count:
        return "sativa"
    return "hybrid"

def infer_medical_effects(effects: list, strain_type: str) -> list:
    """Infer medical effects from recreational effects"""
    medical = []

    effect_map = {
        "relaxed": ["stress relief", "anxiety"],
        "sleepy": ["insomnia", "sleep aid"],
        "happy": ["depression"],
        "euphoric": ["mood enhancement"],
        "hungry": ["appetite loss"],
        "creative": ["focus"],
        "focused": ["ADD/ADHD"],
        "uplifted": ["fatigue"],
        "tingly": ["pain relief"],
    }

    for effect in effects:
        if effect in effect_map:
            medical.extend(effect_map[effect])

    # Add type-based defaults
    if strain_type == "indica":
        if "pain relief" not in medical:
            medical.append("pain relief")
        if "insomnia" not in medical:
            medical.append("insomnia")
    elif strain_type == "sativa":
        if "depression" not in medical:
            medical.append("depression")
        if "fatigue" not in medical:
            medical.append("fatigue")

    return list(dict.fromkeys(medical))[:4]  # Unique, max 4

def main():
    # Read current strains
    strains_path = Path(__file__).parent.parent / "app/src/main/assets/strains.json"

    with open(strains_path) as f:
        current_strains = json.load(f)

    print(f"Found {len(current_strains)} strains to refresh\n")

    updated_strains = {}
    api_found = 0
    api_missing = []

    for strain_name in sorted(current_strains.keys()):
        print(f"Fetching: {strain_name}...", end=" ", flush=True)

        api_data = fetch_strain(strain_name)

        if api_data and api_data["terpenes"]:
            print(f"✓ Found ({len(api_data['terpenes'])} terpenes)")
            updated_strains[strain_name] = api_data
            api_found += 1
        else:
            print("✗ Not found, keeping original")
            updated_strains[strain_name] = current_strains[strain_name]
            api_missing.append(strain_name)

        time.sleep(0.5)  # Rate limiting

    # Write updated strains
    with open(strains_path, "w") as f:
        json.dump(updated_strains, f, indent=2)

    print(f"\n{'='*50}")
    print(f"Done! Updated {api_found}/{len(current_strains)} strains from API")

    if api_missing:
        print(f"\nStrains not found in API (kept original data):")
        for name in api_missing:
            print(f"  - {name}")

    print(f"\nUpdated: {strains_path}")

if __name__ == "__main__":
    main()
