#!/usr/bin/env python3
"""
Build strain database from Cannlytics API + fallback config

Usage:
    python3 build_strain_database.py [--add-strain "Strain Name"]

This script:
1. Reads strain list from strain_config.json
2. Fetches each from Cannlytics API
3. Falls back to config data for strains not found
4. Outputs to app/src/main/assets/strains.json
"""

import argparse
import json
import requests
import sys
import time
from pathlib import Path

SCRIPT_DIR = Path(__file__).parent
CONFIG_FILE = SCRIPT_DIR / "strain_config.json"
OUTPUT_FILE = SCRIPT_DIR.parent / "app/src/main/assets/strains.json"

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
    "beta_pinene": "pinene",
    "geraniol": "geraniol",
    "camphene": "camphene",
    "carene": "carene",
}


def load_config() -> dict:
    """Load strain configuration"""
    with open(CONFIG_FILE) as f:
        return json.load(f)


def save_config(config: dict):
    """Save strain configuration"""
    with open(CONFIG_FILE, "w") as f:
        json.dump(config, f, indent=2)


def fetch_from_api(strain_name: str, api_base: str) -> dict | None:
    """Fetch strain data from Cannlytics API"""
    url_name = strain_name.strip().replace(" ", "-").replace("'", "").lower()

    try:
        # Try direct endpoint
        response = requests.get(f"{api_base}/{url_name}", timeout=10)
        if response.status_code == 200:
            data = response.json()
            if data.get("success") and data.get("data"):
                strain_data = data["data"]
                if isinstance(strain_data, dict) and strain_data:
                    return parse_api_response(strain_name, strain_data)

        # Fall back to search
        response = requests.get(f"{api_base}?limit=100", timeout=15)
        if response.status_code == 200:
            data = response.json()
            if data.get("success") and data.get("data"):
                normalized = strain_name.lower().strip()
                words = normalized.replace("'", "").split()

                for strain in data["data"]:
                    api_name = strain.get("strain_name", "").lower()

                    # Check exact match
                    if normalized == api_name:
                        return parse_api_response(strain_name, strain)

                    # Check all words present
                    if all(w in api_name for w in words):
                        return parse_api_response(strain_name, strain)

                    # Check partial match
                    if normalized in api_name or api_name in normalized:
                        return parse_api_response(strain_name, strain)

        return None
    except Exception as e:
        print(f"    API error: {e}")
        return None


def parse_api_response(name: str, data: dict) -> dict:
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
        clean = effect.replace("effect_", "").lower()
        if clean:
            effects.append(clean)

    # Extract aromas
    aromas = []
    for aroma in data.get("potential_aromas", []):
        clean = aroma.replace("aroma_", "").lower()
        if clean:
            aromas.append(clean)

    # Cannabinoids
    thc = data.get("delta_9_thc", 0) or data.get("total_thc", 0) or 0
    cbd = data.get("cbd", 0) or data.get("total_cbd", 0) or 0

    thc_range = f"{int(thc)}-{int(thc+3)}%" if thc > 0 else "Unknown"
    cbd_range = f"{cbd:.1f}-{cbd+0.5:.1f}%" if cbd > 0.1 else "<1%"

    # Infer type
    description = data.get("description", "")
    strain_type = infer_type(description, effects, name)

    # Infer medical effects
    medical = infer_medical_effects(effects, strain_type)

    # Time and activity
    best_time = "evening" if strain_type == "indica" else "daytime" if strain_type == "sativa" else "any"
    activity = "relaxation" if strain_type == "indica" else "focus" if strain_type == "sativa" else "mixed"

    return {
        "terpenes": terpenes if terpenes else {"myrcene": 0.3, "caryophyllene": 0.2, "limonene": 0.15},
        "effects": effects if effects else ["relaxed", "happy"],
        "medical_effects": medical,
        "type": strain_type,
        "thc_range": thc_range,
        "cbd_range": cbd_range,
        "description": description or f"A {strain_type} cannabis strain",
        "flavors": aromas if aromas else ["earthy"],
        "aromas": aromas if aromas else ["earthy"],
        "best_time": best_time,
        "activity": activity,
    }


def infer_type(description: str, effects: list, name: str) -> str:
    """Infer strain type from description and effects"""
    desc_lower = description.lower()

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

    return list(dict.fromkeys(medical))[:4]


def build_database(config: dict) -> dict:
    """Build strain database from config and API"""
    api_base = config.get("api_base", "https://cannlytics.com/api/data/strains")
    strains = config.get("strains", [])
    fallbacks = config.get("fallback_strains", {})

    database = {}
    stats = {"api": 0, "fallback": 0, "missing": 0}

    print(f"Building database with {len(strains)} strains...\n")

    for strain_name in strains:
        normalized = strain_name.lower().strip()
        print(f"  {strain_name}...", end=" ", flush=True)

        # Try API first
        api_data = fetch_from_api(strain_name, api_base)

        if api_data and api_data.get("terpenes"):
            print(f"✓ API ({len(api_data['terpenes'])} terpenes)")
            database[normalized] = api_data
            stats["api"] += 1
        elif normalized in fallbacks:
            # Use fallback
            fallback = fallbacks[normalized]
            print("◐ Fallback")
            database[normalized] = {
                "terpenes": fallback.get("terpenes", {}),
                "effects": fallback.get("effects", ["relaxed", "happy"]),
                "medical_effects": fallback.get("medical_effects", ["stress relief"]),
                "type": fallback.get("type", "hybrid"),
                "thc_range": fallback.get("thc_range", "Unknown"),
                "cbd_range": fallback.get("cbd_range", "<1%"),
                "description": fallback.get("description", ""),
                "flavors": fallback.get("flavors", ["earthy"]),
                "aromas": fallback.get("flavors", ["earthy"]),
                "best_time": "evening" if fallback.get("type") == "indica" else "daytime",
                "activity": "relaxation" if fallback.get("type") == "indica" else "focus",
            }
            stats["fallback"] += 1
        else:
            print("✗ Not found")
            stats["missing"] += 1

        time.sleep(0.3)  # Rate limiting

    return database, stats


def add_strain_to_config(strain_name: str):
    """Add a new strain to the config file"""
    config = load_config()

    if strain_name not in config["strains"]:
        config["strains"].append(strain_name)
        config["strains"].sort()
        save_config(config)
        print(f"Added '{strain_name}' to config. Run without args to rebuild database.")
    else:
        print(f"'{strain_name}' already in config.")


def main():
    parser = argparse.ArgumentParser(description="Build strain database from API")
    parser.add_argument("--add-strain", help="Add a strain to the config")
    parser.add_argument("--list", action="store_true", help="List strains in config")
    args = parser.parse_args()

    if args.add_strain:
        add_strain_to_config(args.add_strain)
        return

    if args.list:
        config = load_config()
        print("Strains in config:")
        for s in sorted(config.get("strains", [])):
            print(f"  - {s}")
        return

    # Build database
    config = load_config()
    database, stats = build_database(config)

    # Write output
    with open(OUTPUT_FILE, "w") as f:
        json.dump(database, f, indent=2)

    print(f"\n{'='*50}")
    print(f"Database built: {len(database)} strains")
    print(f"  From API:     {stats['api']}")
    print(f"  From fallback:{stats['fallback']}")
    print(f"  Missing:      {stats['missing']}")
    print(f"\nOutput: {OUTPUT_FILE}")


if __name__ == "__main__":
    main()
