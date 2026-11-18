#!/usr/bin/env python3
"""Writes the part system's data files under src/main/resources/data/bsp_weapons/weapons/: patterns, materials, bands,
part types, augments and catalysts (owner decisions D5 to D36). Edit this script, never the output. The materials come
from the vault's materials survey (2026-11-16); a material's numbers are its band's defaults unless listed here.

  python3 tools/gen_weapon_data.py
"""
import json
import shutil
from pathlib import Path

OUT = Path(__file__).resolve().parent.parent / "src/main/resources/data/bsp_weapons/weapons"

KINDS = ["BARREL", "CELL", "GRIP", "OPTIC"]
METAL_KINDS = KINDS
GEM_KINDS = ["OPTIC"]  # D35: gems for optics (and cores and chips, which are not material parts)


def pct(**stats):
    return {k.upper(): {"percent": round(v, 2)} for k, v in stats.items()}


def flat(**stats):
    return {k.upper(): {"flat": v} for k, v in stats.items()}


def merge(*mods):
    out = {}
    for m in mods:
        for k, v in m.items():
            out.setdefault(k, {}).update(v)
    return out


# ---- bands: what a material of each band gives a part of each kind when it lists nothing of its own (D36) -----------------
BAND_PCT = {1: 0, 2: 8, 3: 16, 4: 26, 5: 38}
BAND_DURABILITY = {1: 0, 2: 60, 3: 140, 4: 260, 5: 420}
BAND_HEAT = {1: 0, 2: 6, 3: 12, 4: 20, 5: 30}


def band(n):
    p = BAND_PCT[n]
    return {
        "band": n,
        "modifiers": {
            "BARREL": pct(damage=p, range=p),
            "CELL": merge(pct(capacity=p), pct(recharge=-p * 0.6)),
            "GRIP": merge(pct(fire_rate=p * 0.5), pct(spread=-p * 0.6)),
            "OPTIC": merge(pct(zoom=p * 0.5), pct(spread=-p * 0.4)),
            "ANY": merge(flat(durability=BAND_DURABILITY[n]), flat(heat_cap=BAND_HEAT[n])),
        },
    }


# ---- materials: (name, tag, band, colour, lean modifiers, trait) ----------------------------------------------------------
# lean modifiers are per kind and come on top of the band's defaults; "trait" is a one-word id for later tasks.
def m(name, tag, band_, colour, kinds=METAL_KINDS, mods=None, trait=""):
    d = {"tag": tag, "band": band_, "colour": colour, "kinds": kinds}
    if mods:
        d["modifiers"] = mods
    if trait:
        d["trait"] = trait
    return name, d


MATERIALS = [
    # vanilla
    m("iron", "forge:ingots/iron", 1, "#B8B8B8", mods={"BARREL": pct(damage=0), "GRIP": flat(durability=40)}),
    m("copper", "forge:ingots/copper", 1, "#C47A4A", mods={"CELL": pct(recharge=-8)}),
    m("gold", "forge:ingots/gold", 1, "#F2D14A", mods={"GRIP": pct(fire_rate=8), "BARREL": flat(durability=-40)}, trait="soft"),
    m("netherite", "forge:ingots/netherite", 4, "#4A3F44", mods={"BARREL": pct(piercing=10), "GRIP": flat(durability=200)}, trait="fireproof"),
    # BSP-Core
    m("tetrium", "forge:ingots/tetrium", 2, "#6B4FA3", mods={"BARREL": pct(damage=6)}),
    m("magnatite", "forge:ingots/magnatite", 3, "#5C7FB8", mods={"BARREL": pct(range=8, piercing=6)}, trait="magnetic"),
    m("charged_magnatite", "forge:ingots/charged_magnatite", 4, "#6E7CFF", mods={"CELL": pct(capacity=12)}, trait="charged"),
    m("illyrium", "forge:ingots/illyrium", 5, "#19D3B0", mods={"BARREL": pct(damage=8), "CELL": pct(capacity=8), "GRIP": pct(fire_rate=4), "OPTIC": pct(zoom=8)}),
    # common tech metals
    m("tin", "forge:ingots/tin", 1, "#D5D9DD", mods={"GRIP": pct(spread=-6)}),
    m("zinc", "forge:ingots/zinc", 1, "#C9CFC4", mods={"GRIP": pct(fire_rate=6)}),
    m("lead", "forge:ingots/lead", 2, "#5B5F73", mods={"BARREL": flat(heat_cap=10), "GRIP": pct(fire_rate=-4)}),
    m("nickel", "forge:ingots/nickel", 2, "#C8C2A2", mods={"GRIP": flat(durability=60)}),
    m("silver", "forge:ingots/silver", 2, "#DDE6EE", mods={"CELL": pct(recharge=-8), "BARREL": pct(range=6)}),
    m("aluminum", "forge:ingots/aluminum", 2, "#CFD6D9", mods={"GRIP": pct(spread=-8)}),
    m("osmium", "forge:ingots/osmium", 2, "#8FA7BF", mods={"BARREL": pct(damage=6), "GRIP": flat(durability=40)}),
    m("uranium", "forge:ingots/uranium", 3, "#7FB84A", mods={"BARREL": pct(damage=10)}, trait="irradiates"),
    m("platinum", "forge:ingots/platinum", 3, "#D9E6F2", mods={"CELL": pct(recharge=-12)}),
    m("iridium", "forge:ingots/iridium", 4, "#E6E6F2", mods={"BARREL": pct(piercing=12), "GRIP": flat(durability=160)}),
    m("titanium", "forge:ingots/titanium", 3, "#A9B3B8", mods={"GRIP": merge(pct(spread=-8), flat(durability=100))}),
    m("chromium", "forge:ingots/chromium", 2, "#C4D0D8"),
    m("vanadium", "forge:ingots/vanadium", 3, "#9AA6A0"),
    m("molybdenum", "forge:ingots/molybdenum", 3, "#8A8F99", mods={"BARREL": flat(heat_cap=14)}),
    m("lithium", "forge:ingots/lithium", 2, "#E8E8D0", mods={"CELL": pct(capacity=10)}),
    # alloys
    m("bronze", "forge:ingots/bronze", 1, "#B87333", mods={"GRIP": flat(durability=40)}),
    m("brass", "forge:ingots/brass", 2, "#D4A84B", mods={"GRIP": pct(fire_rate=6, spread=-4)}),
    m("steel", "forge:ingots/steel", 2, "#8C949F", mods={"BARREL": pct(damage=4), "GRIP": flat(durability=80)}),
    m("invar", "forge:ingots/invar", 2, "#AEB6A6", mods={"BARREL": flat(heat_cap=12)}),
    m("electrum", "forge:ingots/electrum", 2, "#E8D27A", mods={"CELL": pct(recharge=-10)}),
    m("constantan", "forge:ingots/constantan", 2, "#C98A5E", mods={"BARREL": flat(heat_cap=12), "CELL": flat(heat_cap=8)}),
    m("signalum", "forge:ingots/signalum", 3, "#E0552E", mods={"GRIP": pct(fire_rate=10)}, trait="redstone_pulse"),
    m("lumium", "forge:ingots/lumium", 3, "#F2E88A", mods={"OPTIC": pct(zoom=12)}, trait="lights_target"),
    m("enderium", "forge:ingots/enderium", 4, "#1F8A7A", mods={"BARREL": pct(range=16)}),
    m("refined_obsidian", "forge:ingots/refined_obsidian", 4, "#4A3B6B", mods={"BARREL": pct(piercing=10), "GRIP": flat(durability=200)}),
    m("refined_glowstone", "forge:ingots/refined_glowstone", 3, "#F0C84A", mods={"OPTIC": pct(zoom=10), "CELL": pct(recharge=-8)}),
    m("cast_iron", "forge:ingots/cast_iron", 1, "#4D4F52", mods={"BARREL": flat(heat_cap=8)}),
    m("nethersteel", "forge:ingots/nethersteel", 3, "#6B3A3A", mods={"BARREL": merge(flat(heat_cap=16), pct(damage=4))}),
    m("compressed_iron", "forge:ingots/compressed_iron", 2, "#9CA3AD", mods={"GRIP": flat(durability=100)}),
    m("hslasteel", "forge:ingots/hslasteel", 3, "#7F8790", mods={"GRIP": flat(durability=140)}),
    m("stainlesssteel", "forge:ingots/stainlesssteel", 3, "#C7CDD3", mods={"BARREL": flat(heat_cap=12)}),
    m("vanadiumsteel", "forge:ingots/vanadiumsteel", 4, "#8E9AA0", mods={"GRIP": flat(durability=220)}),
    m("titaniumcarbide", "forge:ingots/titaniumcarbide", 4, "#5C6B74", mods={"BARREL": pct(piercing=8), "GRIP": flat(durability=240)}),
    m("superconductive", "forge:ingots/superconductive", 4, "#4AD6FF", mods={"CELL": pct(recharge=-18)}),
    m("rose_gold", "forge:ingots/rose_gold", 2, "#E8A0A0", mods={"GRIP": pct(fire_rate=6)}),
    m("pig_iron", "forge:ingots/pig_iron", 2, "#E0A8A0"),
    m("amethyst_bronze", "forge:ingots/amethyst_bronze", 3, "#B97AC8", mods={"OPTIC": pct(zoom=8)}),
    m("hepatizon", "forge:ingots/hepatizon", 3, "#6B4A6E", mods={"BARREL": pct(damage=8)}),
    m("manyullyn", "forge:ingots/manyullyn", 4, "#8A4FB0", mods={"BARREL": pct(damage=12)}),
    m("cobalt", "forge:ingots/cobalt", 3, "#3A6FD8", mods={"GRIP": pct(fire_rate=10)}),
    m("slimesteel", "forge:ingots/slimesteel", 2, "#6EC8C8", mods={"GRIP": pct(spread=-6)}, trait="bouncy"),
    m("knightslime", "forge:ingots/knightslime", 3, "#E08AC8", trait="bouncy"),
    m("queens_slime", "forge:ingots/queens_slime", 3, "#C8B04A", trait="bouncy"),
    m("cinderslime", "forge:ingots/cinderslime", 3, "#E06A3A", mods={"BARREL": flat(heat_cap=14)}),
    m("soulsteel", "forge:ingots/soulsteel", 4, "#3A5A6B", mods={"BARREL": pct(piercing=12)}),
    m("knightmetal", "forge:ingots/knightmetal", 4, "#7A9A4A", mods={"BARREL": pct(piercing=10), "GRIP": flat(durability=180)}),
    m("copper_alloy", "forge:ingots/copper_alloy", 2, "#C98A4A", mods={"CELL": pct(recharge=-6)}),
    m("conductive_alloy", "forge:ingots/conductive_alloy", 2, "#B0B0B8", mods={"CELL": pct(recharge=-10)}),
    m("energetic_alloy", "forge:ingots/energetic_alloy", 3, "#E88A2A", mods={"CELL": pct(capacity=14)}),
    m("vibrant_alloy", "forge:ingots/vibrant_alloy", 4, "#A8D83A", mods={"CELL": pct(capacity=16, recharge=-10)}),
    m("pulsating_alloy", "forge:ingots/pulsating_alloy", 3, "#6ADA8A", mods={"CELL": pct(recharge=-12)}),
    m("redstone_alloy", "forge:ingots/redstone_alloy", 2, "#C83A3A", mods={"GRIP": pct(fire_rate=8)}),
    m("dark_steel", "forge:ingots/dark_steel", 3, "#3A3A44", mods={"GRIP": flat(durability=160), "BARREL": pct(piercing=6)}),
    m("end_steel", "forge:ingots/end_steel", 5, "#D8D8A8", mods={"BARREL": pct(piercing=12), "GRIP": flat(durability=300)}),
    m("soularium", "forge:ingots/soularium", 3, "#7A5A3A", trait="mob_bane"),
    m("sky_steel", "forge:ingots/sky_steel", 3, "#A8C8E8", mods={"GRIP": pct(spread=-10)}),
    # exotic and endgame
    m("allthemodium", "forge:ingots/allthemodium", 4, "#F2C94A", mods={"BARREL": pct(damage=12), "GRIP": flat(durability=240)}),
    m("vibranium", "forge:ingots/vibranium", 5, "#3AD8C8", mods={"CELL": pct(capacity=16), "BARREL": pct(range=12)}),
    m("unobtainium", "forge:ingots/unobtainium", 5, "#C86AE8", mods={"BARREL": pct(piercing=14), "GRIP": flat(durability=400)}),
    m("unobtainium_allthemodium_alloy", "forge:ingots/unobtainium_allthemodium_alloy", 5, "#E0A0E8", mods={"BARREL": pct(damage=10, piercing=10)}),
    m("unobtainium_vibranium_alloy", "forge:ingots/unobtainium_vibranium_alloy", 5, "#8AE8E0", mods={"CELL": pct(capacity=14, recharge=-10)}),
    m("vibranium_allthemodium_alloy", "forge:ingots/vibranium_allthemodium_alloy", 5, "#A8E8A0", mods={"BARREL": pct(damage=8, range=10)}),
    m("gobber", "forge:ingots/gobber", 3, "#A8D83A", trait="regenerating"),
    m("gobber_nether", "forge:ingots/gobber_nether", 4, "#E86A3A", trait="regenerating"),
    m("gobber_end", "forge:ingots/gobber_end", 5, "#C8A8F0", trait="regenerating"),
    m("manasteel", "forge:ingots/manasteel", 2, "#4AA8E8", mods={"CELL": pct(recharge=-8)}),
    m("elementium", "forge:ingots/elementium", 3, "#E88AD8", mods={"GRIP": pct(spread=-10)}),
    m("terrasteel", "forge:ingots/terrasteel", 4, "#5AD86A", mods={"BARREL": pct(damage=12)}),
    m("yellorium", "forge:ingots/yellorium", 2, "#D8D83A", mods={"CELL": pct(capacity=8)}),
    m("cyanite", "forge:ingots/cyanite", 3, "#3AB8E8", mods={"BARREL": flat(heat_cap=14)}),
    m("blutonium", "forge:ingots/blutonium", 4, "#3A5AE8", mods={"CELL": pct(capacity=14)}),
    m("magentite", "forge:ingots/magentite", 4, "#E83AB8", mods={"CELL": pct(recharge=-14)}),
    m("graphite", "forge:ingots/graphite", 2, "#4A4A4A", mods={"BARREL": flat(heat_cap=12)}),
    m("inanite", "forge:ingots/inanite", 3, "#8A3A3A"),
    m("insanite", "forge:ingots/insanite", 4, "#B83A8A"),
    m("ridiculite", "forge:ingots/ridiculite", 5, "#E83A3A", mods={"BARREL": pct(damage=10)}),
    m("ludicrite", "forge:ingots/ludicrite", 5, "#E8E83A", mods={"CELL": pct(capacity=16, recharge=-12)}),
    m("plutonium", "forge:ingots/plutonium", 4, "#8AE83A", mods={"BARREL": pct(damage=14)}, trait="irradiates_strongly"),
    # gems (D35: optics only among the material parts)
    m("diamond", "forge:gems/diamond", 3, "#6AF0E8", GEM_KINDS, {"OPTIC": pct(spread=-12)}),
    m("emerald", "forge:gems/emerald", 3, "#3AD86A", GEM_KINDS, {"OPTIC": pct(zoom=8)}),
    m("lapis", "forge:gems/lapis", 1, "#2A4AC8", GEM_KINDS, {"OPTIC": pct(zoom=4)}),
    m("amethyst", "forge:gems/amethyst", 2, "#B07AE8", GEM_KINDS, {"OPTIC": pct(zoom=10)}),
    m("quartz", "forge:gems/quartz", 1, "#F0E8E0", GEM_KINDS, {"OPTIC": pct(zoom=6)}),
    m("prismarine", "forge:gems/prismarine", 2, "#6AC8B8", GEM_KINDS),
    m("certus_quartz", "forge:gems/certus_quartz", 2, "#C8E8F0", GEM_KINDS, {"OPTIC": pct(spread=-6)}),
    m("fluix", "forge:gems/fluix", 3, "#A05AE8", GEM_KINDS, {"OPTIC": pct(zoom=8, spread=-6)}),
    m("ruby", "forge:gems/ruby", 3, "#E83A4A", GEM_KINDS, {"OPTIC": pct(zoom=6)}),
    m("sapphire", "forge:gems/sapphire", 3, "#3A6AE8", GEM_KINDS, {"OPTIC": pct(zoom=12)}),
    m("peridot", "forge:gems/peridot", 3, "#9AD83A", GEM_KINDS),
    m("fluorite", "forge:gems/fluorite", 2, "#C8F0C8", GEM_KINDS),
    m("mana_diamond", "forge:gems/mana_diamond", 4, "#5AD8F0", GEM_KINDS, {"OPTIC": pct(zoom=14)}),
    m("dragonstone", "forge:gems/dragonstone", 5, "#E85AC8", GEM_KINDS, {"OPTIC": pct(zoom=18, spread=-12)}),
    m("ender_crystal", "forge:gems/ender_crystal", 3, "#3AD8A8", GEM_KINDS),
    m("vibrant_crystal", "forge:gems/vibrant_crystal", 4, "#A8E83A", GEM_KINDS, {"OPTIC": pct(zoom=12)}),
    m("pulsating_crystal", "forge:gems/pulsating_crystal", 3, "#6AE88A", GEM_KINDS),
    m("xychorium", "forge:gems/xychorium", 3, "#8AC8E8", GEM_KINDS),
]

# ---- part types (D6): the shape and behaviour chosen at the parts machine ---------------------------------------------------
PART_TYPES = {
    "focused_barrel": {"kind": "BARREL", "modifiers": merge(pct(range=20), pct(spread=-30))},
    "scatter_barrel": {"kind": "BARREL", "modifiers": merge(pct(damage=15, spread=80), pct(range=-35))},
    "thermal_barrel": {"kind": "BARREL", "modifiers": merge(pct(damage=5), flat(heat_cap=-10))},
    "pulse_barrel": {"kind": "BARREL", "modifiers": merge(pct(fire_rate=25), pct(damage=-10))},
    "energy_cell": {"kind": "CELL", "modifiers": {}, "ammo": "energy"},
    "overcharge_cell": {"kind": "CELL", "modifiers": merge(pct(damage=12), pct(recharge=30, capacity=-25)), "ammo": "energy"},
    "magazine": {"kind": "CELL", "modifiers": merge(pct(capacity=20), pct(recharge=15)), "ammo": "rounds"},
    "emp_cell": {"kind": "CELL", "modifiers": merge(pct(damage=-30), pct(capacity=10)), "ammo": "energy"},
    "standard_grip": {"kind": "GRIP", "modifiers": {}},
    "light_grip": {"kind": "GRIP", "modifiers": merge(pct(fire_rate=12, spread=10), flat(durability=-60))},
    "heavy_grip": {"kind": "GRIP", "modifiers": merge(pct(spread=-20, fire_rate=-8), flat(durability=120))},
    "iron_sight": {"kind": "OPTIC", "modifiers": {}},
    "scope": {"kind": "OPTIC", "modifiers": merge(flat(zoom=3), pct(spread=-25), pct(fire_rate=-10))},
    "holo_sight": {"kind": "OPTIC", "modifiers": merge(flat(zoom=1), pct(spread=-10))},
}

# ---- patterns (D5, D17): the first class; the others come with their tasks ------------------------------------------------
PATTERNS = {
    "laser_pistol": {
        "kind": "ENERGY",
        "slots": ["BARREL", "CELL", "GRIP", "OPTIC"],
        "base": {"DAMAGE": 4.0, "PIERCING": 0, "RANGE": 24, "FIRE_RATE": 2.5, "CHARGE_TIME": 0, "CAPACITY": 24, "RECHARGE": 60,
                 "SPREAD": 2.0, "ZOOM": 1, "HEAT_CAP": 50, "BLOCK_DAMAGE": 0, "RICOCHETS": 0, "DURABILITY": 300},
    },
}

# ---- augments (D9, D25) ----------------------------------------------------------------------------------------------------
AUGMENTS = {
    "piercing": {"modifiers": merge(flat(piercing=10), pct(piercing=15))},
    "knockback": {"modifiers": pct(damage=-5)},  # the push itself is an effect for a later task
    "quick_draw": {"modifiers": pct(fire_rate=15)},
    "stabiliser": {"modifiers": pct(spread=-30)},
    "overcharge": {"modifiers": merge(pct(damage=20), pct(heat_cap=-20))},
    "coolant": {"modifiers": pct(heat_cap=35)},
    "extended_cell": {"modifiers": pct(capacity=30)},
    "breacher": {"modifiers": flat(block_damage=4), "kinds": ["ENERGY"]},  # D25: classes are set when the rifle, sniper and rail exist
    "ricochet": {"modifiers": flat(ricochets=1), "kinds": ["ENERGY"]},
}

# ---- catalysts (D21, D30): lapis plus one ingredient; odds Normal / Rare / Epic / S -----------------------------------------
# colour: the ingredient's, which tints the vial's motes (D62)
CATALYSTS = {
    "lapis_catalyst": {"ingredient": "forge:dusts/redstone", "odds": [70, 25, 5, 0], "colour": "#E03A3A"},
    "magnatite_catalyst": {"ingredient": "forge:ingots/magnatite", "odds": [20, 55, 25, 0], "colour": "#5C7FB8"},
    "illyrium_catalyst": {"ingredient": "forge:ingots/illyrium", "odds": [0, 0, 85, 15], "colour": "#19D3B0"},
    "allthemodium_catalyst": {"ingredient": "forge:ingots/allthemodium", "odds": [0, 0, 40, 60], "colour": "#F2C94A"},
}


def write(folder, items):
    d = OUT / folder
    if d.exists():
        shutil.rmtree(d)
    d.mkdir(parents=True)
    for name, data in sorted(items):
        (d / f"{name}.json").write_text(json.dumps(data, indent=2) + "\n")
    print(f"{folder}: {len(items)}")


LOOT = Path(__file__).resolve().parent.parent / "src/main/resources/data/bsp_weapons/loot_tables/blocks"


def main():
    LOOT.mkdir(parents=True, exist_ok=True)
    (LOOT / "weapon_assembler.json").write_text(json.dumps({
        "type": "minecraft:block",
        "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "bsp_weapons:weapon_assembler"}], "conditions": [{"condition": "minecraft:survives_explosion"}]}],
    }, indent=2) + "\n")
    print("loot_tables: 1")
    dt = OUT.parent / "damage_type"
    dt.mkdir(parents=True, exist_ok=True)
    (dt / "laser.json").write_text(json.dumps({"message_id": "bsp_weapons.laser", "exhaustion": 0.1, "scaling": "when_caused_by_living_non_player"}, indent=2) + "\n")
    # the pierced share skips armour by the maths in Damage, so the type itself bypasses armour; Projectile Protection applies
    tags = Path(__file__).resolve().parent.parent / "src/main/resources/data/minecraft/tags/damage_type"
    tags.mkdir(parents=True, exist_ok=True)
    for tag in ("bypasses_armor", "is_projectile", "bypasses_cooldown"):
        (tags / f"{tag}.json").write_text(json.dumps({"replace": False, "values": ["bsp_weapons:laser"]}, indent=2) + "\n")
    print("damage_type: 1 (+3 tags)")
    # BSP-Core declares no forge ingot tags for its metals (request WP-R3 asks it to); until then this mod declares them
    ingot_tags = Path(__file__).resolve().parent.parent / "src/main/resources/data/forge/tags/items/ingots"
    ingot_tags.mkdir(parents=True, exist_ok=True)
    for name in ("tetrium", "magnatite", "charged_magnatite", "illyrium"):
        (ingot_tags / f"{name}.json").write_text(json.dumps({"replace": False, "values": [f"bsp_core:{name}_ingot"]}, indent=2) + "\n")
    print("forge ingot tags for BSP-Core's metals: 4")
    write("bands", [(f"band_{n}", band(n)) for n in range(1, 6)])
    write("materials", MATERIALS)
    write("part_types", list(PART_TYPES.items()))
    write("patterns", list(PATTERNS.items()))
    write("augments", list(AUGMENTS.items()))
    write("catalysts", list(CATALYSTS.items()))


if __name__ == "__main__":
    main()
