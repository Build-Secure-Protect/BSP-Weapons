#!/usr/bin/env python3
"""Checks the text: every key the code looks up exists in the language file, every data file that needs a name has one,
and no value breaks Minecraft's formatting. A missing key would show in game as the raw key, so it is caught here.

  python3 tools/gen_lang.py && python3 tools/check_lang.py
"""
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "src/main/java/com/mrgregles/bsp_weapons"
LANG = ROOT / "src/main/resources/assets/bsp_weapons/lang/en_us.json"
DATA = ROOT / "src/main/resources/data/bsp_weapons/weapons"
MOD = "bsp_weapons"


def main():
    lang = json.loads(LANG.read_text())
    every = "\n".join(p.read_text() for p in SRC.rglob("*.java"))
    needed = set()
    # keys written whole: translatable("item.bsp_weapons.x")
    needed |= set(re.findall(r'translatable\("([a-z0-9_.]+)"\s*[,)]', every))
    # keys built from a prefix and an id: "tooltip." + MODID + ".part" -> the literal tail after the mod id
    for prefix, tail in re.findall(r'"([a-z]+)\." \+ BspWeapons\.MODID \+ "\.([a-z0-9_.]+)"', every):
        needed.add(f"{prefix}.{MOD}.{tail}")
    # ids from the data folders: every material, part type and stat needs a name
    for p in (DATA / "materials").glob("*.json"):
        needed.add(f"material.{MOD}.{p.stem}")
    for p in (DATA / "part_types").glob("*.json"):
        needed.add(f"item.{MOD}.{p.stem}")
    for stat in ("damage", "piercing", "range", "fire_rate", "charge_time", "capacity", "recharge", "spread", "zoom", "heat_cap", "block_damage", "ricochets", "durability"):
        needed.add(f"stat.{MOD}.{stat}")
    for rarity in ("normal", "rare", "epic", "s"):
        needed.add(f"rarity.{MOD}.{rarity}")
    for key in ("core_tier", "core_slot", "core_slots", "part", "unknown_material"):
        needed.add(f"tooltip.{MOD}.{key}")
    for p in (DATA / "catalysts").glob("*.json"):
        needed.add(f"item.{MOD}.{p.stem}")
    for p in (DATA / "patterns").glob("*.json"):
        needed.add(f"item.{MOD}.{p.stem}")
        needed.add(f"item.{MOD}.{p.stem}_engram")
    for kind in ("engram", "core", "barrel", "cell", "grip", "optic", "roll", "weapon", "part"):
        needed.add(f"slot.{MOD}.{kind}")
    for block in ("weapon_assembler", "weapon_assembler_part"):
        needed.add(f"block.{MOD}.{block}")
    for key in ("laser", "laser.player"):
        needed.add(f"death.attack.{MOD}.{key}")
    needed.add(f"item.{MOD}.energy_round")
    for key in ("heat", "overheated", "reloading", "no_charge", "no_rounds"):
        needed.add(f"hud.{MOD}.{key}")
    missing = sorted(k for k in needed if k not in lang)
    unused = sorted(k for k in lang if k not in needed and not k.startswith("itemGroup.") and k != f"{MOD}.name" and k != f"item.{MOD}.weapon_core")
    bad = 0
    for k, v in lang.items():
        if re.search(r"(?<!%)%(?!(\d+\$)?s|%)", v):  # %s, %1$s and %% are fine
            print("BAD %   ", k, repr(v), "(write %% for a percent sign)")
            bad += 1
    for k in missing:
        print("MISSING", k)
    for k in unused:
        print("unused ", k)
    print(f"{len(needed)} keys used, {len(missing)} missing, {len(unused)} unused")
    return 1 if missing or bad else 0


if __name__ == "__main__":
    sys.exit(main())
