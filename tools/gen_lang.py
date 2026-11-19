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
    f"block.{MOD}.weapon_enhancer": "Weapon Enhancer",
    f"block.{MOD}.weapon_enhancer_top": "Weapon Enhancer",
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
    f"slot.{MOD}.chip_in": "CHIP IN",
    f"slot.{MOD}.repair": "REPAIR",
    f"gui.{MOD}.assembler.title": "WEAPON ASSEMBLER",
    f"gui.{MOD}.assembler.build": "BUILD",
    f"gui.{MOD}.assembler.plan": "PLAN",
    f"gui.{MOD}.assembler.assemble": "ASSEMBLE",
    f"gui.{MOD}.assembler.take_apart": "TAKE APART · 1 catalyst",
    f"gui.{MOD}.assembler.odds": "Normal %s  Rare %s  Epic %s  S %s",
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
    # WP-6b: the Augment Chip (D71, D73, D81, D83) and the enhancer's refusals (R23)
    f"item.{MOD}.augment_chip": "Augment Chip",
    f"item.{MOD}.augment_chip.named": "%s Chip",
    f"tooltip.{MOD}.chip": "%s · effect ×%s",
    f"tooltip.{MOD}.chip_durability": "Points %s / %s",
    f"tooltip.{MOD}.chip_not_yet": "Cannot be fitted yet",
    f"tooltip.{MOD}.chip_push": "Hits throw the target back about %s blocks",
    f"tooltip.{MOD}.chip_fits_any": "Fits any weapon",
    f"tooltip.{MOD}.chip_fits": "Fits: %s",
    f"tooltip.{MOD}.chip_wear": "Spends a point on every fit, upgrade and repair",
    f"tooltip.{MOD}.chip_unknown": "Names no augment this server knows",
    f"tooltip.{MOD}.weapon_chips": "Chips %s / %s",
    f"kind.{MOD}.energy": "energy weapons",
    f"kind.{MOD}.projectile": "projectile weapons",
    f"kind.{MOD}.thrown": "thrown weapons",
    f"enhance.{MOD}.not_a_chip": "That is not an augment chip.",
    f"enhance.{MOD}.unknown_chip": "This server knows no such augment.",
    f"enhance.{MOD}.invalid_weapon": "This weapon is not one the server would make.",
    f"enhance.{MOD}.not_yet": "%s cannot be fitted yet.",
    f"enhance.{MOD}.wrong_weapon": "%s does not fit this weapon.",
    f"enhance.{MOD}.already_fitted": "%s is already fitted.",
    f"enhance.{MOD}.full": "No free socket: %s of %s used.",
    f"enhance.{MOD}.worn_out": "%s is too worn to fit.",
    f"enhance.{MOD}.no_chip": "No chip in that socket.",
    f"enhance.{MOD}.no_weapon": "Put a weapon in the WEAPON slot.",
    f"enhance.{MOD}.no_chip_in": "Put a chip in the CHIP IN slot.",
    f"enhance.{MOD}.no_power": "Not enough charge: %s of %s FE.",
    f"enhance.{MOD}.chip_in_full": "Empty the CHIP IN slot first.",
    f"enhance.{MOD}.one_at_a_time": "Swap one chip at a time.",
    f"enhance.{MOD}.no_such_operation": "The machine has no such operation.",
    f"enhance.{MOD}.no_core": "Put a higher Weapon Core in the CORE slot.",
    f"enhance.{MOD}.one_core_at_a_time": "Upgrade with one Core at a time.",
    f"enhance.{MOD}.core_not_higher": "The Core must be higher than Tier %s.",
    f"enhance.{MOD}.no_ingot": "Put an ingot or gem of a fitted material in REPAIR.",
    f"enhance.{MOD}.wrong_material": "%s: not on this weapon.",
    f"enhance.{MOD}.nothing_to_repair": "The weapon is not worn.",
    f"enhance.{MOD}.burnt_out": "%s burnt out.",
    f"gui.{MOD}.enhancer.upgrade_cost": "%s: %s FE and a point of every chip.",
    f"gui.{MOD}.enhancer.title": "WEAPON ENHANCER",
    f"gui.{MOD}.enhancer.fit": "FIT",
    f"gui.{MOD}.enhancer.remove": "REMOVE",
    f"gui.{MOD}.enhancer.upgrade": "UPGRADE",
    f"gui.{MOD}.enhancer.repair": "REPAIR",
    f"gui.{MOD}.enhancer.swap": "SWAP",
    f"gui.{MOD}.enhancer.chips": "CHIPS %s / %s",
    f"gui.{MOD}.enhancer.empty_socket": "empty socket",
    f"gui.{MOD}.enhancer.no_weapon": "no weapon",
    f"gui.{MOD}.enhancer.fe": "%s / %s FE",
    f"gui.{MOD}.enhancer.fit_cost": "%s: %s FE and a point of the chip.",
    f"gui.{MOD}.enhancer.chosen": "Socket %s chosen: REMOVE takes %s.",
    f"gui.{MOD}.enhancer.idle": "Choose a socket, or put a chip in CHIP IN.",
    f"gui.{MOD}.enhancer.core_line": "Core %s  %s  %s sockets",
    # WP-7b: the Blank Engram (D102, D110) and the fabricator's refusals (D113, D119 to D122)
    f"item.{MOD}.blank_engram": "Blank Engram",
    f"tooltip.{MOD}.blank_engram": "Takes a weapon class's glyph at the Weapon Fabricator",
    f"part_kind.{MOD}.core": "core",
    f"part_kind.{MOD}.barrel": "barrel",
    f"part_kind.{MOD}.cell": "cell",
    f"part_kind.{MOD}.grip": "grip",
    f"part_kind.{MOD}.optic": "optic",
    f"fabricate.{MOD}.no_job": "Choose something to make.",
    f"fabricate.{MOD}.busy": "The machine is working.",
    f"fabricate.{MOD}.out_full": "Take the product out of OUT first.",
    f"fabricate.{MOD}.no_material": "Put %s of a weapon material in MATERIAL.",
    f"fabricate.{MOD}.not_a_material": "%s: no weapon material.",
    f"fabricate.{MOD}.wrong_kind": "%s makes no %s.",
    f"fabricate.{MOD}.too_few": "Needs %s × %s, %s there.",
    f"fabricate.{MOD}.need_material": "Needs %s × %s in MATERIAL.",
    f"fabricate.{MOD}.need_extra": "Also needs %s × %s.",
    f"fabricate.{MOD}.no_power": "Not enough charge: %s of %s FE.",
    # WP-7c: the Weapon Fabricator block (D97, D108, R24) and its stand-in screen
    f"block.{MOD}.weapon_fabricator": "Weapon Fabricator",
    f"block.{MOD}.weapon_fabricator_console": "Weapon Fabricator",
    f"slot.{MOD}.material": "MATERIAL",
    f"slot.{MOD}.extra": "EXTRA",
    f"slot.{MOD}.blank": "BLANK",
    f"slot.{MOD}.out": "OUT",
    f"gui.{MOD}.fabricator.title": "WEAPON FABRICATOR",
    f"gui.{MOD}.fabricator.tab.parts": "PARTS",
    f"gui.{MOD}.fabricator.tab.chips": "CHIPS",
    f"gui.{MOD}.fabricator.tab.engram": "ENGRAM",
    f"gui.{MOD}.fabricator.tab.more": "MORE",
    f"gui.{MOD}.fabricator.make": "MAKE",
    f"gui.{MOD}.fabricator.fe": "FE",
    f"gui.{MOD}.fabricator.nothing_chosen": "nothing chosen",
    f"gui.{MOD}.fabricator.needs": "needs",
    f"gui.{MOD}.fabricator.optional_roll": "optional, the roll",
    f"gui.{MOD}.fabricator.odds": "Normal %s  Rare %s  Epic %s  S %s",
    f"gui.{MOD}.fabricator.price": "%s FE · %s s",
    f"gui.{MOD}.fabricator.no_job": "Choose what to make.",
    f"gui.{MOD}.fabricator.working": "Making %s: %s s left.",
    f"gui.{MOD}.fabricator.cost": "MAKE: %s FE, %s.",
    f"gui.{MOD}.fabricator.count": "%s × %s",
}
AUGMENT_NAMES = {"quick_draw": "Quick-draw", "extended_cell": "Extended Cell"}
for name in gen_weapon_data.AUGMENTS:
    KEYS[f"augment.{MOD}.{name}"] = AUGMENT_NAMES.get(name, title(name))
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
