#!/usr/bin/env python3
"""Writes the part items' models and textures (owner decision D48: a part's icon is its own voxel model). Every part
type and the Weapon Core is a table of voxel boxes below, the same shapes as tools/preview/parts_system_refined.html;
this script merges the voxels into as few cubes as it can and writes a JSON item model per part under
assets/bsp_weapons/models/item/, plus the handful of 16 x 16 textures the cubes use under textures/item/ (a PNG writer is
built in, so no image library is needed). The material tints the 'mat' faces in code (tint index 0); the Core's glow
takes the tier colour (tint index 1). Edit this script, never the output.

  python3 tools/gen_part_models.py
"""
import json
import shutil
import struct
import zlib
from pathlib import Path

ASSETS = Path(__file__).resolve().parent.parent / "src/main/resources/assets/bsp_weapons"

# ---- colours and roles ------------------------------------------------------------------------------------------------------
GUNMETAL, GOLD, DARK, TEAL, RED, VIOLET = "gunmetal", "gold", "dark", "teal", "red", "violet"
MAT, GLOW = "mat", "glow"  # tinted in code: material colour (0), core tier colour (1)
TEXTURES = {  # name: (base colour, the three pixel shades as factors)
    "part_metal": (0xFFFFFF, (1.0, 0.84, 0.68)),
    "part_glow": (0xFFFFFF, (1.0, 0.8, 1.0)),
    "part_gunmetal": (0x2E343D, (1.0, 0.86, 0.72)),
    "part_gold": (0xD9B23A, (1.0, 0.86, 0.72)),
    "part_dark": (0x14181E, (1.0, 0.9, 0.8)),
    "part_teal": (0x19D3B0, (1.0, 0.85, 1.0)),
    "part_red": (0xFF6B5C, (1.0, 0.85, 1.0)),
    "part_violet": (0xB58CFF, (1.0, 0.85, 1.0)),
}
ROLE_TEXTURE = {MAT: "part_metal", GLOW: "part_glow", GUNMETAL: "part_gunmetal", GOLD: "part_gold", DARK: "part_dark",
                TEAL: "part_teal", RED: "part_red", VIOLET: "part_violet"}
TINT = {MAT: 0, GLOW: 1}


def hash01(x, y, z=0):
    h = (x * 73856093) ^ (y * 19349663) ^ (z * 83492791)
    h = ((h ^ (h >> 13)) * 1274126177) & 0xFFFFFFFF
    return (h % 1000) / 1000


# ---- voxels -----------------------------------------------------------------------------------------------------------------
class Part:
    """Whole voxels (x, y, z) with a role each; forward is +z, up is +y, x is the width."""

    def __init__(self, name):
        self.name = name
        self.cubes = {}

    def fill(self, x0, y0, z0, x1, y1, z1, role):
        for x in range(x0, x1 + 1):
            for y in range(y0, y1 + 1):
                for z in range(z0, z1 + 1):
                    self.cubes[(x, y, z)] = role
        return self

    def cut(self, x0, y0, z0, x1, y1, z1):
        for x in range(x0, x1 + 1):
            for y in range(y0, y1 + 1):
                for z in range(z0, z1 + 1):
                    self.cubes.pop((x, y, z), None)
        return self

    def boxes(self):
        """Greedy merge of same-role voxels into boxes: along x, then y, then z. Returns (x0, y0, z0, x1, y1, z1, role), inclusive."""
        left = dict(self.cubes)
        out = []
        for key in sorted(left):
            if key not in left:
                continue
            role = left[key]
            x0, y0, z0 = key
            x1 = x0
            while (x1 + 1, y0, z0) in left and left[(x1 + 1, y0, z0)] == role:
                x1 += 1
            y1 = y0
            while all((x, y1 + 1, z0) in left and left[(x, y1 + 1, z0)] == role for x in range(x0, x1 + 1)):
                y1 += 1
            z1 = z0
            while all((x, y, z1 + 1) in left and left[(x, y, z1 + 1)] == role for x in range(x0, x1 + 1) for y in range(y0, y1 + 1)):
                z1 += 1
            for x in range(x0, x1 + 1):
                for y in range(y0, y1 + 1):
                    for z in range(z0, z1 + 1):
                        del left[(x, y, z)]
            out.append((x0, y0, z0, x1, y1, z1, role))
        return out


def barrel_tube(p, z0, z1):
    p.fill(-2, 1, z0, 1, 4, z1, MAT)
    p.fill(-1, 2, z1 + 1, 0, 3, z1 + 1, TEAL)                      # the bore at the muzzle
    p.fill(-3, 0, z1 - 1, 2, 5, z1 - 1, GOLD).cut(-2, 1, z1 - 1, 1, 4, z1 - 1)  # muzzle ring
    p.fill(-3, 0, z0, 2, 5, z0 + 1, GOLD).cut(-2, 1, z0, 1, 4, z0 + 1)          # clamp ring
    return p


def grip(p, width, rows, step_every=2):
    x0 = -2
    x1 = x0 + width - 1
    for i in range(rows):
        y = -1 - i
        back = i // step_every
        p.fill(x0, y, -10 - back, x1, y, -6 - back, MAT)
    for groove in (2, 5, 8):
        if groove < rows:
            back = groove // step_every
            p.cut(x0, -1 - groove, -6 - back, x1, -1 - groove, -6 - back).fill(x0, -1 - groove, -7 - back, x1, -1 - groove, -7 - back, DARK)
    base_back = (rows - 1) // step_every
    p.fill(x0, -1 - rows, -14 - base_back + 4, x1, -1 - rows, -10 - base_back + 4, DARK)
    return p


def parts():
    out = []
    core = Part("weapon_core")
    core.fill(-3, 4, -4, 2, 7, -4, GOLD).fill(-3, 4, 1, 2, 7, 1, GOLD).cut(-2, 5, -4, 1, 7, -4).cut(-2, 5, 1, 1, 7, 1)
    core.fill(-2, 4, -3, 1, 7, 0, GLOW).fill(-2, 4, -3, 1, 4, 0, GUNMETAL).fill(-2, 7, -3, 1, 7, 0, GUNMETAL).cut(-1, 7, -2, 0, 7, -1).fill(-1, 7, -2, 0, 7, -1, GLOW)
    out.append(core)

    b = barrel_tube(Part("focused_barrel"), 13, 24)
    b.fill(-3, 1, 16, -3, 4, 16, MAT).fill(-3, 1, 18, -3, 4, 18, MAT).fill(2, 1, 16, 2, 4, 16, MAT).fill(2, 1, 18, 2, 4, 18, MAT).fill(-1, 5, 15, 0, 5, 21, DARK)
    out.append(b)
    b = barrel_tube(Part("scatter_barrel"), 13, 19)
    b.fill(-3, 0, 20, 2, 5, 21, MAT).cut(-2, 1, 20, 1, 4, 21).fill(-2, 1, 21, 1, 4, 21, TEAL)  # the flared mouth, open and lit
    out.append(b)
    b = barrel_tube(Part("thermal_barrel"), 13, 23)
    for z in (15, 17, 19, 21):
        b.fill(-1, 5, z, 0, 5, z, RED).fill(-1, 0, z, 0, 0, z, RED)                             # vents above and below
    out.append(b)
    b = barrel_tube(Part("pulse_barrel"), 13, 23)
    for z in (16, 19, 22):
        b.fill(-1, 5, z, 0, 5, z, TEAL)                                                        # three charge bands on top
    out.append(b)

    c = Part("energy_cell").fill(-2, -6, -8, 1, -1, -3, MAT)
    c.fill(-2, -4, -7, -2, -2, -4, TEAL).fill(1, -4, -7, 1, -2, -4, TEAL).fill(-2, -6, -8, 1, -6, -3, DARK).fill(-2, -1, -3, 1, -1, -3, GOLD)
    out.append(c)
    c = Part("overcharge_cell").fill(-2, -6, -8, 1, -1, -3, MAT)
    c.fill(-2, -4, -7, -2, -2, -4, RED).fill(1, -4, -7, 1, -2, -4, RED).fill(-2, -6, -8, 1, -6, -3, DARK).fill(-2, -1, -8, 1, -1, -3, GOLD)
    out.append(c)
    c = Part("magazine").fill(-2, -6, -8, 1, -1, -3, MAT)
    c.fill(-2, -3, -8, -2, -3, -3, DARK).fill(1, -3, -8, 1, -3, -3, DARK).fill(-2, -5, -8, -2, -5, -3, DARK).fill(1, -5, -8, 1, -5, -3, DARK)
    c.fill(-2, -6, -8, 1, -6, -3, GOLD).fill(-2, -1, -3, 1, -1, -3, GOLD)
    out.append(c)
    c = Part("emp_cell").fill(-2, -6, -8, 1, -1, -3, MAT)
    for i, z in enumerate(range(-7, -3)):
        y = -4 if i % 2 == 0 else -3
        c.fill(-2, y, z, -2, y, z, VIOLET).fill(1, y, z, 1, y, z, VIOLET)                      # a zigzag of violet on each side
    c.fill(-2, -6, -8, 1, -6, -3, DARK).fill(-2, -1, -3, 1, -1, -3, GOLD)
    out.append(c)

    out.append(grip(Part("standard_grip"), 4, 9))
    out.append(grip(Part("light_grip"), 3, 8))
    g = grip(Part("heavy_grip"), 4, 11)
    g.fill(-3, -12, -16, 2, -12, -9, DARK)                                                       # a wider base plate
    out.append(g)

    o = Part("iron_sight").fill(-1, 4, -9, 0, 6, -6, DARK)
    o.fill(-1, 7, -9, 0, 8, -8, MAT).fill(-2, 7, -4, -2, 8, -4, MAT).fill(1, 7, -4, 1, 8, -4, MAT).fill(-1, 7, -4, 0, 7, -4, GOLD)  # rear blade, front posts
    out.append(o)
    o = Part("scope").fill(-1, 4, -9, 0, 7, -6, DARK)
    o.fill(-1, 8, -10, 0, 10, -4, MAT).fill(-1, 8, -3, 0, 9, -3, TEAL).fill(-1, 11, -9, 0, 11, -9, GOLD).fill(-2, 9, -7, -2, 9, -7, GOLD).fill(1, 9, -7, 1, 9, -7, GOLD)
    out.append(o)
    o = Part("holo_sight").fill(-1, 4, -9, 0, 5, -6, DARK)
    o.fill(-2, 6, -6, 1, 9, -6, MAT).cut(-1, 7, -6, 0, 8, -6).fill(-1, 7, -6, 0, 8, -6, TEAL).fill(-1, 6, -8, 0, 6, -7, GOLD)       # a lit square frame
    out.append(o)
    return out


# ---- models ------------------------------------------------------------------------------------------------------------------
def face_uv(face, x0, y0, z0, x1, y1, z1):
    """Texture coordinates for a face, taken from the face's own position so the pixel pattern follows the voxels."""
    if face in ("north", "south"):
        return [x0 % 16, (16 - y1) % 16 if y1 % 16 else 0, x0 % 16 + (x1 - x0), (16 - y1) % 16 + (y1 - y0)]
    if face in ("east", "west"):
        return [z0 % 16, (16 - y1) % 16 if y1 % 16 else 0, z0 % 16 + (z1 - z0), (16 - y1) % 16 + (y1 - y0)]
    return [x0 % 16, z0 % 16, x0 % 16 + (x1 - x0), z0 % 16 + (z1 - z0)]


def model(part):
    boxes = part.boxes()
    xs = [b[0] for b in boxes] + [b[3] + 1 for b in boxes]
    ys = [b[1] for b in boxes] + [b[4] + 1 for b in boxes]
    zs = [b[2] for b in boxes] + [b[5] + 1 for b in boxes]
    # centre the part in the 16-unit model space
    off = (8 - (min(xs) + max(xs)) / 2, 8 - (min(ys) + max(ys)) / 2, 8 - (min(zs) + max(zs)) / 2)
    extent = max(max(xs) - min(xs), max(ys) - min(ys), max(zs) - min(zs))
    elements, textures = [], {}
    for (x0, y0, z0, x1, y1, z1, role) in boxes:
        tex = ROLE_TEXTURE[role]
        textures[tex] = f"bsp_weapons:item/{tex}"
        fx0, fy0, fz0 = x0 + off[0], y0 + off[1], z0 + off[2]
        fx1, fy1, fz1 = x1 + 1 + off[0], y1 + 1 + off[1], z1 + 1 + off[2]
        faces = {}
        for face in ("north", "south", "east", "west", "up", "down"):
            f = {"uv": face_uv(face, int(fx0), int(fy0), int(fz0), int(fx1), int(fy1), int(fz1)), "texture": f"#{tex}"}
            if role in TINT:
                f["tintindex"] = TINT[role]
            faces[face] = f
        elements.append({"from": [fx0, fy0, fz0], "to": [fx1, fy1, fz1], "faces": faces})
    textures["particle"] = textures.get("part_metal", next(iter(textures.values())))
    gui_scale = round(min(1.4, 0.625 * 16 / max(1, extent)), 3)
    return {
        "credit": "BSP-Weapons, tools/gen_part_models.py",
        "gui_light": "side",
        "textures": textures,
        "elements": elements,
        "display": {
            "gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [gui_scale, gui_scale, gui_scale]},
            "ground": {"scale": [0.25, 0.25, 0.25], "translation": [0, 3, 0]},
            "fixed": {"rotation": [0, 180, 0], "scale": [0.5, 0.5, 0.5]},
            "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.375, 0.375, 0.375]},
            "thirdperson_lefthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.375, 0.375, 0.375]},
            "firstperson_righthand": {"rotation": [0, 45, 0], "scale": [0.4, 0.4, 0.4]},
            "firstperson_lefthand": {"rotation": [0, 225, 0], "scale": [0.4, 0.4, 0.4]},
        },
    }, len(boxes)


# ---- textures: 16 x 16 PNGs, three pixel shades of one colour --------------------------------------------------------------------
def png(width, height, rgba_rows):
    def chunk(kind, data):
        c = kind + data
        return struct.pack(">I", len(data)) + c + struct.pack(">I", zlib.crc32(c) & 0xFFFFFFFF)
    raw = b"".join(b"\x00" + bytes(row) for row in rgba_rows)
    return b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0)) + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b"")


def texture(colour, shades):
    rows = []
    r, g, b = (colour >> 16) & 255, (colour >> 8) & 255, colour & 255
    for y in range(16):
        row = []
        for x in range(16):
            h = hash01(x, y)
            f = shades[0] if h < 0.5 else (shades[1] if h < 0.82 else shades[2])
            row += [min(255, int(r * f)), min(255, int(g * f)), min(255, int(b * f)), 255]
        rows.append(row)
    return png(16, 16, rows)


def main():
    models_dir = ASSETS / "models/item"
    textures_dir = ASSETS / "textures/item"
    for d in (models_dir, textures_dir):
        if d.exists():
            shutil.rmtree(d)
        d.mkdir(parents=True)
    for name, (colour, shades) in TEXTURES.items():
        (textures_dir / f"{name}.png").write_bytes(texture(colour, shades))
    print(f"textures: {len(TEXTURES)}")
    for part in parts():
        data, n = model(part)
        (models_dir / f"{part.name}.json").write_text(json.dumps(data, indent=2) + "\n")
        print(f"{part.name}: {len(part.cubes)} voxels -> {n} cubes, gui scale {data['display']['gui']['scale'][0]}")


if __name__ == "__main__":
    main()
