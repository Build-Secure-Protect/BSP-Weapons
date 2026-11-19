#!/usr/bin/env python3
"""Writes the Weapon Assembler's block model, blockstates and item model (owner decisions D51, D55 to D60). The machine is
a 32 x 24 x 16 voxel table, the same shape as tools/preview/assembler_refined.html with the arms folded, merged into cubes
and written as one block model that spans the two block spaces (x 0 to 32) from the left space. Its front is the model's
south face (+z), so a player facing the machine sees the pod on their left; the blockstate turns it for each facing. The
companion block has an empty model. Textures come from tools/gen_part_models.py (run it first). Edit this script, never
the output.

  python3 tools/gen_block_models.py
"""
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from gen_part_models import Part, GUNMETAL, GOLD, DARK, TEAL, RED, LIGHT, VIOLET, GLASS, ROLE_TEXTURE, face_uv  # noqa: E402

ASSETS = Path(__file__).resolve().parent.parent / "src/main/resources/assets/bsp_weapons"
GLOW = "machine_glow"  # the display grid's brighter pixels, unlit: drawn with the teal texture here
ROLE_TEXTURE[GLOW] = "part_teal"


def machine():
    """Front is +z (south). x 0..31 across both spaces, y 0..23, z 0..15."""
    b = Part("weapon_assembler")
    # the base: feet, vents, a gold seam at the top
    b.fill(0, 0, 0, 31, 7, 15, GUNMETAL).fill(0, 0, 0, 31, 0, 15, DARK).fill(0, 7, 0, 31, 7, 15, GOLD).fill(1, 1, 1, 30, 6, 14, GUNMETAL)
    for z in (2, 5, 8, 11, 13):
        b.fill(0, 2, z, 0, 5, z, DARK).fill(31, 2, z, 31, 5, z, DARK)
    for x in (2, 5, 8, 11, 13):
        b.fill(x, 2, 15, x, 5, 15, DARK)                                                        # front vents under the pod
    b.fill(18, 2, 15, 29, 5, 15, DARK).fill(19, 3, 15, 28, 4, 15, LIGHT)                        # the service hatch under the console
    # the pod, left half: the lit plate, the back housing, the glass shell over the front and top
    b.fill(2, 8, 6, 13, 8, 13, TEAL).fill(3, 8, 7, 12, 8, 12, DARK).fill(4, 8, 8, 11, 8, 11, TEAL)
    b.fill(0, 8, 0, 15, 19, 5, GUNMETAL).fill(1, 9, 1, 14, 18, 4, DARK).fill(0, 19, 0, 15, 19, 5, LIGHT)
    b.fill(2, 10, 6, 13, 17, 6, DARK).fill(3, 11, 6, 12, 16, 6, GUNMETAL).fill(4, 12, 6, 11, 15, 6, TEAL).fill(5, 13, 6, 10, 14, 6, DARK)
    b.fill(0, 8, 6, 15, 19, 15, GLASS).cut(1, 8, 6, 14, 18, 14)                                   # the shell
    b.fill(0, 8, 15, 15, 8, 15, GOLD).fill(0, 8, 6, 0, 8, 15, GOLD).fill(15, 8, 6, 15, 8, 15, GOLD)   # its foot
    b.fill(0, 19, 15, 15, 19, 15, GOLD).fill(0, 19, 6, 0, 19, 15, GOLD).fill(15, 19, 6, 15, 19, 15, GOLD)  # and crown
    b.fill(0, 8, 15, 0, 19, 15, GOLD).fill(15, 8, 15, 15, 19, 15, GOLD)                            # corner posts
    # the console, right half: a stepped desk, the display grid, the readout, buttons, dials, the lever, the lamp
    b.fill(16, 8, 0, 31, 10, 15, GUNMETAL).fill(16, 11, 0, 31, 12, 11, GUNMETAL).fill(16, 13, 0, 31, 14, 7, GUNMETAL).fill(16, 15, 0, 31, 15, 3, GUNMETAL)
    b.fill(17, 11, 8, 30, 11, 11, LIGHT).fill(17, 13, 4, 30, 13, 7, LIGHT)
    b.fill(18, 12, 4, 29, 12, 7, DARK).fill(18, 14, 0, 29, 14, 3, DARK)
    for gx in range(18, 30):
        for gz in range(4, 8):
            b.fill(gx, 12, gz, gx, 12, gz, TEAL if (gx + gz) % 2 == 0 else GLOW)                    # the display grid
    b.fill(19, 15, 0, 28, 15, 3, TEAL).fill(20, 15, 1, 27, 15, 2, DARK).fill(21, 15, 2, 26, 15, 2, GLOW)  # the upper readout
    for bx in range(17, 31, 2):
        b.fill(bx, 11, 14, bx, 11, 14, GOLD).fill(bx, 11, 13, bx, 11, 13, RED if bx % 4 == 1 else GOLD)   # two button banks
    b.fill(17, 11, 15, 30, 11, 15, LIGHT)
    for dx in (19, 23, 27):
        b.fill(dx - 1, 12, 9, dx + 1, 12, 11, DARK).fill(dx, 12, 10, dx, 12, 10, GOLD).fill(dx, 13, 10, dx, 13, 10, TEAL)  # dials
    b.fill(30, 11, 9, 30, 14, 9, GOLD).fill(30, 15, 9, 30, 15, 9, RED)                               # the lever
    b.fill(16, 16, 0, 31, 16, 2, GUNMETAL).fill(28, 17, 1, 30, 18, 1, DARK).fill(29, 17, 2, 29, 18, 2, TEAL)  # the lamp housing
    # the hologram panel above the console: a checker of teal, tilted back; static here (the live hologram is a later pass)
    for px in range(18, 30):
        for py in range(17, 23):
            if (px + py) % 2 == 0:
                b.fill(px, py, 3 + (py - 17) // 2, px, py, 3 + (py - 17) // 2, GLOW)
    # the two arms, folded: posts, gold shoulder rings, upper arms back, forearms down
    for ax in (2, 12):
        b.fill(ax, 19, 2, ax + 1, 20, 3, GUNMETAL).fill(ax - 1, 21, 1, ax + 2, 21, 4, GOLD).cut(ax, 21, 2, ax + 1, 21, 3)
        b.fill(ax, 22, 2, ax + 1, 23, 3, GUNMETAL).fill(ax, 22, 4, ax + 1, 23, 9, GUNMETAL).fill(ax, 21, 9, ax + 1, 21, 9, GOLD)
        b.fill(ax, 15, 9, ax + 1, 20, 9, GUNMETAL).fill(ax, 14, 9, ax + 1, 14, 9, TEAL)
    return b


def enhancer():
    """The Weapon Enhancer, the Spire (D84; tools/preview/enhancer_refined.html): 16 x 32 x 16 from the lower space, the ring
    parked under the arm, the drum still, the gauge lit to eleven of sixteen. Drawn with the front at z 0 and mirrored at
    the end so the front is the model's south face (+z) like the assembler's."""
    b = Part("weapon_enhancer")
    b.fill(0, 0, 0, 15, 6, 15, GUNMETAL).fill(0, 0, 0, 15, 0, 15, DARK).fill(0, 6, 0, 15, 6, 15, GOLD).fill(1, 6, 1, 14, 6, 14, LIGHT)   # the base, gold seam
    b.fill(4, 2, 0, 11, 4, 0, DARK).fill(5, 3, 0, 10, 3, 0, DARK).fill(5, 3, 0, 8, 3, 0, GOLD)                                          # the charge window
    for z in (2, 13):
        b.fill(0, 1, z, 0, 4, z, DARK).fill(15, 1, z, 15, 4, z, DARK)                                                                  # vents
    b.fill(0, 1, 5, 0, 5, 10, GOLD).fill(0, 2, 6, 0, 4, 9, DARK).fill(0, 3, 7, 0, 3, 8, VIOLET)                                          # the core hatch, left
    b.fill(15, 1, 5, 15, 5, 10, GOLD).fill(15, 2, 6, 15, 4, 9, DARK).fill(15, 2, 6, 15, 2, 9, LIGHT)                                     # the repair hopper, right
    b.fill(3, 7, 10, 12, 25, 15, GUNMETAL).fill(3, 7, 10, 12, 7, 15, LIGHT).fill(4, 8, 9, 11, 24, 9, DARK)                               # the spine and its face plate
    for y in range(9, 25):
        b.fill(6, y, 9, 9, y, 9, GOLD if y <= 19 else DARK)                                                                              # the energy gauge
    b.fill(2, 9, 11, 2, 24, 11, GOLD).fill(13, 9, 11, 13, 24, 11, GOLD)                                                                  # gold stripes
    for vy in range(10, 25, 4):
        b.fill(2, vy, 13, 2, vy + 1, 13, DARK).fill(13, vy, 13, 13, vy + 1, 13, DARK)                                                    # side vents
    b.fill(4, 25, 9, 11, 25, 9, GOLD)                                                                                                     # the lip over the plate
    b.fill(3, 26, 2, 12, 27, 15, GUNMETAL).fill(3, 27, 2, 12, 27, 2, GOLD).fill(3, 26, 2, 12, 26, 2, LIGHT)                              # the top arm
    b.fill(7, 23, 4, 8, 25, 5, GUNMETAL).fill(6, 21, 3, 9, 22, 6, GOLD).cut(7, 21, 4, 8, 22, 5)                                          # the clamp and its jaws
    b.fill(5, 7, 2, 10, 8, 7, GOLD).cut(6, 7, 3, 9, 8, 6).fill(6, 7, 3, 9, 7, 6, DARK).fill(7, 7, 4, 8, 7, 5, TEAL)                      # the socket, teal seat lamp
    b.fill(3, 24, 0, 12, 24, 9, GOLD).cut(4, 24, 1, 11, 24, 8).fill(7, 24, 0, 8, 24, 0, TEAL).fill(7, 24, 9, 8, 24, 9, TEAL).fill(3, 24, 4, 3, 24, 5, TEAL).fill(12, 24, 4, 12, 24, 5, TEAL)  # the ring, parked
    b.fill(5, 28, 2, 10, 30, 7, GUNMETAL)                                                                                                 # the drum
    for q in ((5, 2), (10, 2), (5, 7), (10, 7)):
        b.cut(q[0], 28, q[1], q[0], 30, q[1])
    b.fill(6, 31, 3, 9, 31, 6, GOLD).fill(7, 31, 4, 8, 31, 5, DARK)                                                                       # its cap
    b.cut(7, 28, 2, 8, 30, 2).fill(7, 28, 2, 8, 28, 2, GUNMETAL).fill(7, 29, 2, 8, 29, 2, GOLD).fill(7, 30, 2, 7, 30, 2, GOLD)            # front bay: a Piercing shard
    b.cut(10, 28, 4, 10, 30, 5).fill(10, 28, 4, 10, 28, 5, GUNMETAL).fill(10, 29, 4, 10, 29, 5, RED).fill(10, 30, 5, 10, 30, 5, RED)      # right bay: Overcharge
    b.cut(5, 28, 4, 5, 30, 5).fill(5, 28, 4, 5, 28, 5, GUNMETAL).fill(5, 29, 4, 5, 29, 5, TEAL).fill(5, 30, 4, 5, 30, 4, TEAL)            # left bay: Coolant
    b.cut(7, 28, 7, 8, 30, 7).fill(7, 28, 7, 8, 28, 7, DARK)                                                                             # back bay: empty
    b.cubes = {(x, y, 15 - z): r for (x, y, z), r in b.cubes.items()}
    return b


def fabricator():
    """The Weapon Fabricator, the Gantry Printer (D108; tools/preview/fabricator_refined.html): 32 x 16 x 16 from the tower
    space, the bridge and head parked at the back, the bed bare, the gauge lit to four of six. Drawn with the front at z 0
    as the page draws it and mirrored at the end so the front is the model's south face (+z) like the other machines'."""
    b = Part("weapon_fabricator")
    b.fill(0, 0, 0, 31, 5, 15, GUNMETAL).fill(0, 0, 0, 31, 0, 15, DARK).fill(0, 5, 0, 31, 5, 15, GOLD).fill(1, 5, 1, 30, 5, 14, LIGHT)   # the base, gold seam
    for z in range(2, 14, 3):
        b.fill(0, 1, z, 0, 4, z, DARK).fill(31, 1, z, 31, 4, z, DARK)                                                                    # side vents
    for x in range(8, 30, 3):
        b.fill(x, 1, 0, x, 4, 0, DARK)                                                                                                    # front vents
    b.fill(6, 6, 3, 25, 6, 12, DARK)                                                                                                       # the bed
    for gx in range(7, 25, 3):
        for gz in range(4, 12, 3):
            b.fill(gx, 6, gz, gx, 6, gz, TEAL)                                                                                            # its teal grid
    for cx, cz in ((5, 2), (26, 2), (5, 13), (26, 13)):
        b.fill(cx, 6, cz, cx, 15, cz, GUNMETAL).fill(cx, 15, cz, cx, 15, cz, GOLD)                                                      # the four posts
    b.fill(5, 15, 2, 5, 15, 13, GOLD).fill(26, 15, 2, 26, 15, 13, GOLD).fill(5, 14, 3, 5, 14, 12, LIGHT).fill(26, 14, 3, 26, 14, 12, LIGHT)  # the rails
    b.fill(0, 6, 4, 4, 15, 15, GUNMETAL).fill(0, 6, 4, 4, 6, 15, LIGHT)                                                                   # the tower
    b.fill(1, 7, 3, 3, 14, 3, DARK)
    for y in range(8, 14):
        b.fill(1, y, 3, 3, y, 3, GOLD if y <= 11 else DARK)                                                                               # the energy gauge on its front
    b.fill(0, 15, 5, 4, 15, 10, GOLD).cut(1, 15, 6, 3, 15, 9).fill(1, 15, 6, 3, 15, 9, DARK).fill(1, 15, 7, 3, 15, 8, VIOLET)             # the hopper on top, an ingot in it
    b.fill(0, 8, 7, 0, 13, 12, DARK).fill(0, 9, 8, 0, 12, 11, TEAL).fill(0, 10, 9, 0, 11, 10, GUNMETAL)                                   # the spool on the left side
    b.fill(0, 11, 13, 0, 11, 14, GUNMETAL).fill(1, 11, 13, 4, 11, 13, GUNMETAL)                                                           # its feed tube
    b.fill(27, 6, 0, 31, 7, 6, GUNMETAL).fill(27, 8, 3, 31, 8, 6, GUNMETAL).fill(28, 8, 0, 30, 8, 2, LIGHT).fill(28, 9, 3, 30, 9, 6, DARK).fill(29, 9, 4, 29, 9, 5, TEAL)  # the console
    b.fill(28, 9, 1, 28, 9, 1, GOLD).fill(30, 9, 1, 30, 9, 1, RED)                                                                        # its two buttons
    b.fill(31, 8, 1, 31, 10, 1, GOLD).fill(31, 11, 1, 31, 11, 1, RED).fill(27, 9, 1, 27, 9, 1, GOLD)                                     # the lever and the job lamp
    b.fill(27, 6, 11, 31, 9, 15, GUNMETAL).fill(27, 10, 11, 31, 10, 15, GOLD).cut(28, 10, 12, 30, 10, 14).fill(28, 10, 12, 30, 10, 14, DARK)  # the tray, back right
    b.fill(5, 13, 12, 26, 13, 13, GUNMETAL).fill(5, 13, 12, 26, 13, 12, GOLD).fill(5, 12, 12, 5, 12, 13, GUNMETAL).fill(26, 12, 12, 26, 12, 13, GUNMETAL)  # the bridge, parked at the back
    b.fill(23, 10, 12, 24, 12, 13, GUNMETAL).fill(23, 13, 12, 24, 13, 13, GOLD).fill(23, 9, 12, 24, 9, 13, TEAL)                          # the head on it, nozzle down
    b.cubes = {(x, y, 15 - z): r for (x, y, z), r in b.cubes.items()}
    return b


def gui_transform(part, fill=15.0):
    """The item model's gui transform (D124): the machine seen as every block item is (30 degrees down, 225 degrees round),
    scaled so its projection fills `fill` of the slot's 16 px and moved so the projection's centre sits on the slot's. The game
    scales, then turns (X then Y, as its rotationXYZ does), then moves by the translation, then the slot flips y; so the
    translation is minus the turned, scaled offset of the model's centre from (8, 8, 8), and the flip changes no extent."""
    import math
    xs = [k[0] for k in part.cubes] + [k[0] + 1 for k in part.cubes]
    ys = [k[1] for k in part.cubes] + [k[1] + 1 for k in part.cubes]
    zs = [k[2] for k in part.cubes] + [k[2] + 1 for k in part.cubes]
    ax, ay = math.radians(30), math.radians(225)

    def turn(v):
        x, y, z = v
        x, z = x * math.cos(ay) + z * math.sin(ay), -x * math.sin(ay) + z * math.cos(ay)   # round Y
        y, z = y * math.cos(ax) - z * math.sin(ax), y * math.sin(ax) + z * math.cos(ax)    # then down about X
        return x, y, z

    corners = [turn((x - 8, y - 8, z - 8)) for x in (min(xs), max(xs)) for y in (min(ys), max(ys)) for z in (min(zs), max(zs))]
    w = max(c[0] for c in corners) - min(c[0] for c in corners)
    h = max(c[1] for c in corners) - min(c[1] for c in corners)
    scale = round(fill / max(w, h), 3)
    centre = turn(((min(xs) + max(xs)) / 2 - 8, (min(ys) + max(ys)) / 2 - 8, (min(zs) + max(zs)) / 2 - 8))
    return {"rotation": [30, 225, 0], "translation": [round(-centre[0] * scale, 2), round(-centre[1] * scale, 2), 0], "scale": [scale, scale, scale]}


def block_model(part, glass):
    """glass=False: every opaque cube, drawn solid; glass=True: the GLASS cubes alone, drawn translucent."""
    boxes = [b for b in part.boxes() if (b[6] == GLASS) == glass]
    elements, textures = [], {}
    for (x0, y0, z0, x1, y1, z1, role) in boxes:
        tex = ROLE_TEXTURE[role]
        textures[tex] = f"bsp_weapons:item/{tex}"
        faces = {}
        for face in ("north", "south", "east", "west", "up", "down"):
            faces[face] = {"uv": face_uv(face, x0, y0, z0, x1 + 1, y1 + 1, z1 + 1), "texture": f"#{tex}"}
        elements.append({"from": [x0, y0, z0], "to": [x1 + 1, y1 + 1, z1 + 1], "faces": faces})
    textures["particle"] = "bsp_weapons:item/part_gunmetal"
    return {"credit": "BSP-Weapons, tools/gen_block_models.py", "render_type": "minecraft:translucent" if glass else "minecraft:solid", "ambientocclusion": False, "textures": textures, "elements": elements}, len(boxes)


def main():
    models = ASSETS / "models/block"
    states = ASSETS / "blockstates"
    item_models = ASSETS / "models/item"
    for d in (models, states, item_models):
        d.mkdir(parents=True, exist_ok=True)
    m = machine()
    data, n = block_model(m, False)
    glass, ng = block_model(m, True)
    (models / "weapon_assembler.json").write_text(json.dumps(data, indent=2) + "\n")
    (models / "weapon_assembler_glass.json").write_text(json.dumps(glass, indent=2) + "\n")
    (models / "weapon_assembler_part.json").write_text(json.dumps({"textures": {"particle": "bsp_weapons:item/part_gunmetal"}, "elements": []}, indent=2) + "\n")
    # the front is the model's south face: south needs no turn, west a quarter, north a half, east three quarters; the glass is a second part
    turns = {"south": 0, "west": 90, "north": 180, "east": 270}
    multipart = []
    for facing, y in turns.items():
        for model in ("weapon_assembler", "weapon_assembler_glass"):
            apply = {"model": f"bsp_weapons:block/{model}"}
            if y:
                apply["y"] = y
            multipart.append({"when": {"facing": facing}, "apply": apply})
    (states / "weapon_assembler.json").write_text(json.dumps({"multipart": multipart}, indent=2) + "\n")
    n += ng
    (states / "weapon_assembler_part.json").write_text(json.dumps({"variants": {"": {"model": "bsp_weapons:block/weapon_assembler_part"}}}, indent=2) + "\n")
    (item_models / "weapon_assembler.json").write_text(json.dumps({
        "parent": "bsp_weapons:block/weapon_assembler",
        "display": {
            "gui": gui_transform(m),
            "ground": {"scale": [0.15, 0.15, 0.15], "translation": [-1, 2, 0]},
            "fixed": {"rotation": [0, 180, 0], "scale": [0.3, 0.3, 0.3], "translation": [-3, -2, 0]},
            "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.2, 0.2, 0.2]},
            "thirdperson_lefthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.2, 0.2, 0.2]},
            "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [-2, 0, 0], "scale": [0.25, 0.25, 0.25]},
            "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [-2, 0, 0], "scale": [0.25, 0.25, 0.25]},
        },
    }, indent=2) + "\n")
    print(f"weapon_assembler: {len(m.cubes)} voxels -> {n} cubes; blockstates 2; item model 1")
    e = enhancer()
    data, ne = block_model(e, False)
    (models / "weapon_enhancer.json").write_text(json.dumps(data, indent=2) + "\n")
    (models / "weapon_enhancer_top.json").write_text(json.dumps({"textures": {"particle": "bsp_weapons:item/part_gunmetal"}, "elements": []}, indent=2) + "\n")
    variants = {}
    for facing, y in turns.items():
        v = {"model": "bsp_weapons:block/weapon_enhancer"}
        if y:
            v["y"] = y
        variants[f"facing={facing}"] = v
    (states / "weapon_enhancer.json").write_text(json.dumps({"variants": variants}, indent=2) + "\n")
    (states / "weapon_enhancer_top.json").write_text(json.dumps({"variants": {"": {"model": "bsp_weapons:block/weapon_enhancer_top"}}}, indent=2) + "\n")
    (item_models / "weapon_enhancer.json").write_text(json.dumps({
        "parent": "bsp_weapons:block/weapon_enhancer",
        "display": {
            "gui": gui_transform(e),
            "ground": {"scale": [0.15, 0.15, 0.15], "translation": [0, 2, 0]},
            "fixed": {"rotation": [0, 180, 0], "scale": [0.28, 0.28, 0.28], "translation": [0, -3, 0]},
            "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.2, 0.2, 0.2]},
            "thirdperson_lefthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.2, 0.2, 0.2]},
            "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, -1, 0], "scale": [0.2, 0.2, 0.2]},
            "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, -1, 0], "scale": [0.2, 0.2, 0.2]},
        },
    }, indent=2) + "\n")
    print(f"weapon_enhancer: {len(e.cubes)} voxels -> {ne} cubes; blockstates 2; item model 1")
    f = fabricator()
    data, nf = block_model(f, False)
    (models / "weapon_fabricator.json").write_text(json.dumps(data, indent=2) + "\n")
    (models / "weapon_fabricator_console.json").write_text(json.dumps({"textures": {"particle": "bsp_weapons:item/part_gunmetal"}, "elements": []}, indent=2) + "\n")
    variants = {}
    for facing, y in turns.items():
        v = {"model": "bsp_weapons:block/weapon_fabricator"}
        if y:
            v["y"] = y
        variants[f"facing={facing}"] = v
    (states / "weapon_fabricator.json").write_text(json.dumps({"variants": variants}, indent=2) + "\n")
    (states / "weapon_fabricator_console.json").write_text(json.dumps({"variants": {"": {"model": "bsp_weapons:block/weapon_fabricator_console"}}}, indent=2) + "\n")
    (item_models / "weapon_fabricator.json").write_text(json.dumps({
        "parent": "bsp_weapons:block/weapon_fabricator",
        "display": {
            "gui": gui_transform(f),
            "ground": {"scale": [0.15, 0.15, 0.15], "translation": [-1, 2, 0]},
            "fixed": {"rotation": [0, 180, 0], "scale": [0.3, 0.3, 0.3], "translation": [-3, 0, 0]},
            "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.2, 0.2, 0.2]},
            "thirdperson_lefthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.2, 0.2, 0.2]},
            "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [-2, 0, 0], "scale": [0.25, 0.25, 0.25]},
            "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [-2, 0, 0], "scale": [0.25, 0.25, 0.25]},
        },
    }, indent=2) + "\n")
    print(f"weapon_fabricator: {len(f.cubes)} voxels -> {nf} cubes; blockstates 2; item model 1")


if __name__ == "__main__":
    main()
