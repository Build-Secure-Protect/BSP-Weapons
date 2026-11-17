#!/usr/bin/env python3
"""Writes BSP-Weapons' language file. Edit this script, never the output. Item and material names follow the data the
generators write (tools/gen_weapon_data.py), so a new material or part type gets its name here.

  python3 tools/gen_lang.py
"""
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import gen_weapon_data  # noqa: E402

LANG = Path(__file__).resolve().parent.parent / "src/main/resources/assets/bsp_weapons/lang/en_us.json"
MOD = "bsp_weapons"

# names that a plain title-case of the id gets wrong
MATERIAL_NAMES = {
    "hslasteel": "HSLA Steel", "stainlesssteel": "Stainless Steel", "vanadiumsteel": "Vanadium Steel", "titaniumcarbide": "Titanium Carbide",
    "superconductive": "Superconductive Alloy", "gobber_nether": "Nether Gobber", "gobber_end": "End Gobber", "pink_slime": "Pink Slime",
    "unobtainium_allthemodium_alloy": "Unobtainium-Allthemodium Alloy", "unobtainium_vibranium_alloy": "Unobtainium-Vibranium Alloy",
    "vibranium_allthemodium_alloy": "Vibranium-Allthemodium Alloy", "aluminum": "Aluminium", "emp_cell": "EMP Cell", "holo_sight": "Holo Sight",
}
STAT_NAMES = {
    "damage": "Damage", "piercing": "Armour piercing", "range": "Range", "fire_rate": "Fire rate", "charge_time": "Charge time", "capacity": "Capacity",
    "recharge": "Recharge", "spread": "Spread", "zoom": "Zoom", "heat_cap": "Heat capacity", "block_damage": "Block damage", "ricochets": "Ricochets",
    "durability": "Durability",
}


def title(id_):
    return MATERIAL_NAMES.get(id_, " ".join(w.capitalize() for w in id_.split("_")))


KEYS = {
    f"{MOD}.name": "BSP-Weapons",
    f"itemGroup.{MOD}": "BSP-Weapons",
    f"item.{MOD}.weapon_core": "Weapon Core",
    f"tooltip.{MOD}.part": "%s · %s",
    f"tooltip.{MOD}.unknown_material": "Unknown material",
    f"tooltip.{MOD}.core_tier": "Tier %s",
    f"tooltip.{MOD}.core_slot": "%s augment slot",
    f"tooltip.{MOD}.core_slots": "%s augment slots",
    f"rarity.{MOD}.normal": "Normal",
    f"rarity.{MOD}.rare": "Rare",
    f"rarity.{MOD}.epic": "Epic",
    f"rarity.{MOD}.s": "S Tier",
}
for name in gen_weapon_data.PART_TYPES:
    KEYS[f"item.{MOD}.{name}"] = title(name)
for name, _ in gen_weapon_data.MATERIALS:
    KEYS[f"material.{MOD}.{name}"] = title(name)
for stat, text in STAT_NAMES.items():
    KEYS[f"stat.{MOD}.{stat}"] = text


def main():
    LANG.parent.mkdir(parents=True, exist_ok=True)
    LANG.write_text(json.dumps(KEYS, indent=2, ensure_ascii=False) + "\n")
    print(f"{LANG.relative_to(LANG.parents[6])}: {len(KEYS)} lines")


if __name__ == "__main__":
    main()
