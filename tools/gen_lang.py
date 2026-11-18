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
    f"block.{MOD}.weapon_assembler": "Weapon Assembler",
    f"block.{MOD}.weapon_assembler_part": "Weapon Assembler",
    f"item.{MOD}.laser_pistol": "Laser Pistol",
    f"item.{MOD}.laser_pistol_engram": "Laser Pistol Engram",
    f"item.{MOD}.lapis_catalyst": "Lapis Catalyst",
    f"item.{MOD}.magnatite_catalyst": "Magnatite Catalyst",
    f"item.{MOD}.illyrium_catalyst": "Illyrium Catalyst",
    f"item.{MOD}.allthemodium_catalyst": "Allthemodium Catalyst",
    f"tooltip.{MOD}.engram": "Takes: %s",
    f"tooltip.{MOD}.engram_unknown": "Names no weapon this server knows",
    f"tooltip.{MOD}.catalyst_odds": "Normal %s%% · Rare %s%% · Epic %s%% · S %s%%",
    f"tooltip.{MOD}.catalyst_use": "Sets the roll in the assembler; one is spent to take a weapon apart",
    f"tooltip.{MOD}.catalyst_unknown": "No odds on this server",
    f"tooltip.{MOD}.weapon": "%s · Core %s",
    f"tooltip.{MOD}.weapon_unassembled": "Not assembled",
    f"slot.{MOD}.engram": "ENGRAM",
    f"slot.{MOD}.core": "CORE",
    f"slot.{MOD}.barrel": "BARREL",
    f"slot.{MOD}.cell": "CELL",
    f"slot.{MOD}.grip": "GRIP",
    f"slot.{MOD}.optic": "OPTIC",
    f"slot.{MOD}.roll": "ROLL",
    f"slot.{MOD}.weapon": "WEAPON",
    f"slot.{MOD}.part": "PART",
    f"gui.{MOD}.assembler.title": "WEAPON ASSEMBLER",
    f"gui.{MOD}.assembler.build": "BUILD",
    f"gui.{MOD}.assembler.plan": "PLAN",
    f"gui.{MOD}.assembler.assemble": "ASSEMBLE",
    f"gui.{MOD}.assembler.take_apart": "TAKE APART · 1 catalyst",
    f"gui.{MOD}.assembler.odds": "roll: Normal %s · Rare %s · Epic %s · S %s",
    f"gui.{MOD}.assembler.no_catalyst": "roll: Normal (no catalyst)",
    f"death.attack.{MOD}.laser": "%1$s was cut down by a laser",
    f"death.attack.{MOD}.laser.player": "%1$s was cut down by %2$s's laser",
    f"hud.{MOD}.heat": "HEAT",
    f"hud.{MOD}.overheated": "OVERHEATED",
    f"hud.{MOD}.reloading": "RELOADING",
    f"hud.{MOD}.no_charge": "NO CHARGE",
    f"tooltip.{MOD}.energy": "Energy %s / %s FE",
    f"item.{MOD}.energy_round": "Energy Round",
    f"tooltip.{MOD}.energy_round": "Loads a weapon built with a Magazine",
    f"tooltip.{MOD}.rounds": "Rounds %s / %s",
    f"hud.{MOD}.no_rounds": "NO ROUNDS",
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
