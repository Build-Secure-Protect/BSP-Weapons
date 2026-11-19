#!/usr/bin/env python3
"""Measures the screens' strings against the boxes they are drawn in, with Minecraft's own glyph widths (D123, Bug Log B1).
The mock-ups were drawn in a narrower pixel font, so a string that fitted there can overflow in the game; this fails the build
instead. Glyph widths come from the game's ascii.png in the Forge Gradle cache (space 4, every other glyph its last filled
column plus two, as the game measures); when the cache is missing the check is skipped with a note.

  python3 tools/gen_lang.py && python3 tools/check_text_widths.py
"""
import glob
import json
import os
import struct
import sys
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
LANG = ROOT / "src/main/resources/assets/bsp_weapons/lang/en_us.json"
MOD = "bsp_weapons"
FONT_JAR = os.path.expanduser("~/.gradle/caches/forge_gradle/minecraft_repo/versions/1.20.1/client-extra.jar")

# (lang key, sample arguments, the box's width in px, how many rows the screen gives it)
# the Rail (WeaponAssemblerScreen, 300 wide): title 150, keys w - 8, tabs 34, slot labels 40, one status or odds line of 160
# the Board (WeaponEnhancerScreen): title 140, keys w - 8, slot labels 36, one status line of 286, name 124
# the Catalogue (WeaponFabricatorScreen): title 123, tabs 36, slot labels 46, MAKE 34, price 68, need names 84, odds 162, status 162 x 2
BOXES = [
    ("gui.assembler.title", (), 150, 1), ("gui.assembler.assemble", (), 52, 1), ("gui.assembler.take_apart", (), 122, 1),
    ("gui.assembler.build", (), 34, 1), ("gui.assembler.plan", (), 34, 1),
    ("gui.assembler.odds", ("70", "25", "5", "0"), 160, 1), ("gui.assembler.no_catalyst", (), 160, 1),
    ("slot.engram", (), 40, 1), ("slot.core", (), 40, 1), ("slot.barrel", (), 40, 1), ("slot.cell", (), 40, 1), ("slot.grip", (), 40, 1), ("slot.optic", (), 40, 1), ("slot.roll", (), 40, 1), ("slot.weapon", (), 36, 1), ("slot.part", (), 40, 1),
    ("gui.enhancer.title", (), 140, 1), ("gui.enhancer.fit", (), 30, 1), ("gui.enhancer.swap", (), 30, 1), ("gui.enhancer.remove", (), 44, 1), ("gui.enhancer.upgrade", (), 46, 1), ("gui.enhancer.repair", (), 40, 1),
    ("slot.chip_in", (), 36, 1), ("slot.repair", (), 36, 1),
    ("gui.enhancer.fit_cost", ("SWAP", "400"), 286, 1), ("gui.enhancer.upgrade_cost", ("UPGRADE", "2,000"), 286, 1), ("gui.enhancer.chosen", ("4", "Extended Cell Chip"), 286, 1), ("gui.enhancer.idle", (), 286, 1),
    ("enhance.not_a_chip", (), 286, 1), ("enhance.unknown_chip", (), 286, 1), ("enhance.invalid_weapon", (), 286, 1), ("enhance.not_yet", ("Extended Cell Chip",), 286, 1), ("enhance.wrong_weapon", ("Extended Cell Chip",), 286, 1),
    ("enhance.already_fitted", ("Extended Cell Chip",), 286, 1), ("enhance.full", ("4", "4"), 286, 1), ("enhance.worn_out", ("Extended Cell Chip",), 286, 1), ("enhance.no_chip", (), 286, 1), ("enhance.no_weapon", (), 286, 1),
    ("enhance.no_chip_in", (), 286, 1), ("enhance.no_power", ("19,999", "20,000"), 286, 1), ("enhance.chip_in_full", (), 286, 1), ("enhance.one_at_a_time", (), 286, 1), ("enhance.no_core", (), 286, 1),
    ("enhance.one_core_at_a_time", (), 286, 1), ("enhance.core_not_higher", ("III",), 286, 1), ("enhance.no_ingot", (), 286, 1), ("enhance.wrong_material", ("Unobtainium-Allthemodium Alloy Ingot",), 286, 1), ("enhance.nothing_to_repair", (), 286, 1),
    ("gui.fabricator.title", (), 123, 1), ("gui.fabricator.tab.parts", (), 36, 1), ("gui.fabricator.tab.chips", (), 36, 1), ("gui.fabricator.tab.engram", (), 36, 1), ("gui.fabricator.tab.more", (), 36, 1),
    ("gui.fabricator.make", (), 34, 1), ("gui.fabricator.price", ("2,000", "4"), 68, 1), ("gui.fabricator.optional_roll", (), 84, 1), ("gui.fabricator.needs", (), 30, 1), ("gui.fabricator.nothing_chosen", (), 150, 1),
    ("slot.material", (), 46, 1), ("slot.extra", (), 46, 1), ("slot.blank", (), 46, 1), ("slot.out", (), 46, 1),
    ("gui.fabricator.odds", ("70", "25", "5", "0"), 162, 1), ("gui.fabricator.no_job", (), 162, 2), ("gui.fabricator.working", ("Allthemodium Catalyst", "3.2"), 162, 2),
    ("gui.fabricator.cost", ("2,500", "4 Illyrium Ingot, 1 Unobtainium Ingot"), 162, 2), ("gui.fabricator.cost", ("1,200", "1 Prismarine Crystals, 2 Redstone Dust"), 162, 2),
    ("fabricate.no_job", (), 162, 2), ("fabricate.busy", (), 162, 2), ("fabricate.out_full", (), 162, 2), ("fabricate.no_material", ("3",), 162, 2), ("fabricate.not_a_material", ("Unobtainium-Allthemodium Alloy Ingot",), 162, 2),
    ("fabricate.wrong_kind", ("Unobtainium-Allthemodium Alloy", "barrel"), 162, 2), ("fabricate.too_few", ("3", "Unobtainium-Allthemodium Alloy Ingot", "2"), 162, 2), ("fabricate.need_material", ("4", "Prismarine Crystals"), 162, 2),
    ("fabricate.need_extra", ("2", "Redstone Dust"), 162, 2), ("fabricate.no_power", ("19,999", "20,000"), 162, 2),
]


def read_png(data):
    assert data[:8] == b'\x89PNG\r\n\x1a\n'
    pos, idat, pal, trns = 8, b'', None, None
    while pos < len(data):
        n, = struct.unpack('>I', data[pos:pos + 4]); kind = data[pos + 4:pos + 8]; body = data[pos + 8:pos + 8 + n]; pos += 12 + n
        if kind == b'IHDR': w, h, depth, ctype, _, _, _ = struct.unpack('>IIBBBBB', body)
        elif kind == b'IDAT': idat += body
        elif kind == b'PLTE': pal = body
        elif kind == b'tRNS': trns = body
    bpp = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}[ctype]
    raw = zlib.decompress(idat); stride = w * bpp; rows = []; prev = bytearray(stride); p = 0
    for _ in range(h):
        f = raw[p]; line = bytearray(raw[p + 1:p + 1 + stride]); p += 1 + stride
        for i in range(stride):
            a = line[i - bpp] if i >= bpp else 0; b = prev[i]; c = prev[i - bpp] if i >= bpp else 0
            if f == 1: line[i] = (line[i] + a) & 255
            elif f == 2: line[i] = (line[i] + b) & 255
            elif f == 3: line[i] = (line[i] + (a + b) // 2) & 255
            elif f == 4:
                pa, pb, pc = abs(b - c), abs(a - c), abs(a + b - 2 * c)
                line[i] = (line[i] + (a if pa <= pb and pa <= pc else (b if pb <= pc else c))) & 255
        rows.append(bytes(line)); prev = line

    def alpha(x, y):
        px = rows[y][x * bpp:(x + 1) * bpp]
        if ctype == 6: return px[3]
        if ctype == 4: return px[1]
        if ctype == 3: return trns[px[0]] if trns and px[0] < len(trns) else 255
        return 255 if any(px) else 0
    return w, h, alpha


def advances():
    import zipfile
    with zipfile.ZipFile(FONT_JAR) as z:
        w, h, alpha = read_png(z.read("assets/minecraft/textures/font/ascii.png"))
    cell = w // 16
    adv = {}
    for code in range(32, 127):
        cx, cy = (code % 16) * cell, (code // 16) * cell
        last = max((x for x in range(cell) if any(alpha(cx + x, cy + y) > 0 for y in range(cell))), default=-1)
        adv[chr(code)] = 4 if code == 32 else (last + 2 if last >= 0 else 0)
    adv["×"] = adv["x"]
    adv["·"] = 2
    return adv


def main():
    if not os.path.exists(FONT_JAR):
        print(f"check_text_widths: no font sheet at {FONT_JAR}; skipped")
        return 0
    adv = advances()
    lang = json.loads(LANG.read_text())
    bad = 0
    for key, args, box, rows in BOXES:
        full = key.split(".", 1)
        full = f"{full[0]}.{MOD}.{full[1]}"
        if full not in lang:
            print("MISSING ", full); bad += 1; continue
        text = lang[full]
        for a in args: text = text.replace("%s", str(a), 1)
        text = text.replace("%%", "%")
        width = sum(adv.get(ch, 6) for ch in text)
        if rows == 1 and width > box or rows > 1 and width > box * rows * 0.9:  # wrapping loses a little at the breaks
            print(f"TOO WIDE {full}: {width} px for {box} x {rows}: {text!r}"); bad += 1
    print(f"{len(BOXES)} strings measured, {bad} too wide")
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main())
