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
from gen_part_models import Part, GUNMETAL, GOLD, DARK, TEAL, RED, LIGHT, GLASS, ROLE_TEXTURE, face_uv  # noqa: E402

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
            "gui": {"rotation": [30, 225, 0], "translation": [-3, -2, 0], "scale": [0.3, 0.3, 0.3]},
            "ground": {"scale": [0.15, 0.15, 0.15], "translation": [-1, 2, 0]},
            "fixed": {"rotation": [0, 180, 0], "scale": [0.3, 0.3, 0.3], "translation": [-3, -2, 0]},
            "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.2, 0.2, 0.2]},
            "thirdperson_lefthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.2, 0.2, 0.2]},
            "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [-2, 0, 0], "scale": [0.25, 0.25, 0.25]},
            "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [-2, 0, 0], "scale": [0.25, 0.25, 0.25]},
        },
    }, indent=2) + "\n")
    print(f"weapon_assembler: {len(m.cubes)} voxels -> {n} cubes; blockstates 2; item model 1")


if __name__ == "__main__":
    main()
