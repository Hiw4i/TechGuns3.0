"""Compute the aim offset of every gun and write it into the gun datapack.

While aiming, the weapon has to move so that its rear sight (or the eyepiece
of a scope) ends up exactly on the screen centre - that is what "aiming down
the sights" means. The offset depends on

  * where the sight sits in the model,
  * the item display transform (models/item/<gun>.json),
  * the first person hand placement of the game.

All three are known here, so the offset is computed once, offline, and stored
in the datapack as ``handling.ads_offset`` (camera space, in blocks). The
client then only has to slide the weapon by that amount.

The sight itself comes from the model's sight bones where they exist and from
the top of the receiver at the back of the weapon otherwise.

Run from the project root:  python tools/add_ads_offsets.py
"""

import json
import math
import re
from pathlib import Path

root = Path(__file__).resolve().parents[1]
models = root / "src/main/resources/assets/techguns3/geckolib/models/item"
item_models = root / "src/main/resources/assets/techguns3/models/item"
guns_dir = root / "src/main/resources/data/techguns3/guns"

IDENT = [[1, 0, 0, 0], [0, 1, 0, 0], [0, 0, 1, 0], [0, 0, 0, 1]]

# First person hand placement of the game (FirstPersonHandsAndItemsRenderer).
HAND_X = 0.56
HAND_Y = -0.52
HAND_Z = -0.72

# GeckoLib re-centres the model after the display transform, which shifted it
# by half a block; the item transform itself also ends with a -0.5 offset.
GECKO_CENTRE = (0.5, 0.51, 0.5)
BLOCK_CENTRE = (0.5, 0.5, 0.5)

# Models with a named sight bone: the aim point is given in model pixels.
# The second value is the height of the notch of the sight, None for the
# centre of the sight (a red dot for example).
SIGHT_BONES = {
    "ak47": ("ISTop", 7.0),
    "as50": ("Scope01", None),
    "gaussrifle": ("Scope", None),
    "pistol": ("IS1", 0.5),
    "m4": ("RedDot", None),
}


def mat_mul(a, b):
    return [[sum(a[i][k] * b[k][j] for k in range(4)) for j in range(4)] for i in range(4)]


def translation(v):
    return [[1, 0, 0, v[0]], [0, 1, 0, v[1]], [0, 0, 1, v[2]], [0, 0, 0, 1]]


def rot_matrix(rx, ry, rz):
    ax, ay, az = math.radians(rx), math.radians(ry), math.radians(rz)
    cx, sx = math.cos(ax), math.sin(ax)
    cy, sy = math.cos(ay), math.sin(ay)
    cz, sz = math.cos(az), math.sin(az)
    mx = [[1, 0, 0, 0], [0, cx, -sx, 0], [0, sx, cx, 0], [0, 0, 0, 1]]
    my = [[cy, 0, sy, 0], [0, 1, 0, 0], [-sy, 0, cy, 0], [0, 0, 0, 1]]
    mz = [[cz, -sz, 0, 0], [sz, cz, 0, 0], [0, 0, 1, 0], [0, 0, 0, 1]]
    return mat_mul(mz, mat_mul(my, mx))


def apply(m, v):
    return (m[0][0] * v[0] + m[0][1] * v[1] + m[0][2] * v[2] + m[0][3],
            m[1][0] * v[0] + m[1][1] * v[1] + m[1][2] * v[2] + m[1][3],
            m[2][0] * v[0] + m[2][1] * v[1] + m[2][2] * v[2] + m[2][3])


def collect(bones, parent_name, parent_matrix, out):
    for bone in bones:
        if bone.get("parent") != parent_name:
            continue
        piv = bone.get("pivot", [0, 0, 0])
        rot = bone.get("rotation", [0, 0, 0])
        world = mat_mul(parent_matrix,
                        mat_mul(translation(piv),
                                mat_mul(rot_matrix(*rot), translation([-p for p in piv]))))
        for cube in bone.get("cubes", []):
            o = cube["origin"]
            s = cube["size"]
            for dx in (0, s[0]):
                for dy in (0, s[1]):
                    for dz in (0, s[2]):
                        out.append((apply(world, (o[0] + dx, o[1] + dy, o[2] + dz)), bone["name"]))
        collect(bones, bone["name"], world, out)


def display_transform(stem):
    """(translation, rotation degrees, scale) of the first person display."""
    data = json.loads((item_models / f"{stem}.json").read_text(encoding="utf-8"))
    disp = data["display"]["firstperson_righthand"]
    return disp["translation"], disp["rotation"], disp["scale"]


def forward_axis(rot_y):
    if rot_y == 0:
        return (0, 0, -1)
    if rot_y == 270:
        return (1, 0, 0)
    raise SystemExit(f"unsupported display rotation {rot_y}")


def aim_point(stem, bones, corners, muzzle, rot_y):
    """Aim point in model pixels: the rear sight, or the top of the receiver."""
    forward = forward_axis(rot_y)
    axis = max(range(3), key=lambda i: abs(forward[i]))
    sign = 1 if forward[axis] > 0 else -1

    if stem in SIGHT_BONES:
        bone_name, top = SIGHT_BONES[stem]
        bone = next((b for b in bones if b.get("name") == bone_name), None)
        if bone is not None:
            piv = bone.get("pivot", [0, 0, 0])
            cubes = bone.get("cubes", [])
            if cubes:
                xs = [c["origin"][0] for c in cubes] + [c["origin"][0] + c["size"][0] for c in cubes]
                ys = [c["origin"][1] for c in cubes] + [c["origin"][1] + c["size"][1] for c in cubes]
                zs = [c["origin"][2] for c in cubes] + [c["origin"][2] + c["size"][2] for c in cubes]
                point = [(min(xs) + max(xs)) / 2.0,
                         max(ys) if top is None else top,
                         (min(zs) + max(zs)) / 2.0]
            else:
                point = list(piv)
            return point, bone_name

    # Fallback: the top of the weapon at the back, on the bore axis.
    depths = [p[axis] * sign for p, _ in corners]
    rear_limit = min(depths) + (max(depths) - min(depths)) * 0.35
    rear = [p for p, _ in corners if p[axis] * sign <= rear_limit]
    top_y = max(p[1] for p in rear)
    # Keep the bore axis lateral position of the muzzle.
    lateral = [i for i in range(3) if i != axis and i != 1]
    point = [0.0, 0.0, 0.0]
    point[1] = top_y
    point[lateral[0]] = muzzle[lateral[0]]
    point[axis] = muzzle[axis]
    return point, "receiver"


def screen_position(point, trans, rot, s, hand_x):
    """Camera space position of a model point, in blocks."""
    p = [-point[0] / 16.0, point[1] / 16.0, point[2] / 16.0]
    r = rot_matrix(*rot)
    centred = [s * apply(r, [p[i] - BLOCK_CENTRE[i] for i in range(3)])[i] + trans[i] + GECKO_CENTRE[i]
               for i in range(3)]
    return [centred[0] + hand_x, centred[1] + HAND_Y, centred[2] + HAND_Z]


def main():
    for geo_path in sorted(models.glob("*.geo.json")):
        stem = geo_path.name[: -len(".geo.json")]
        text = geo_path.read_text(encoding="utf-8")
        compact = "\n" not in text.strip()
        geo = json.loads(text)
        bones = geo["minecraft:geometry"][0].get("bones", [])
        corners = []
        collect(bones, None, IDENT, corners)
        if not corners:
            continue

        muzzle_bone = next((b for b in bones if b.get("name") == "muzzle"), None)
        if muzzle_bone is None:
            print(f"{stem}: no muzzle anchor, skipping")
            continue
        muzzle = muzzle_bone.get("pivot", [0, 0, 0])

        trans, rot, scale = display_transform(stem)
        point, source = aim_point(stem, bones, corners, muzzle, rot[1])
        s = scale[0]

        # Right hand: the display transform as authored.
        right = screen_position(point, trans, rot, s, HAND_X)
        # Left hand: vanilla mirrors the translation on X and the Y/Z rotations.
        left_rot = [rot[0], -rot[1], -rot[2]]
        left_trans = [-trans[0], trans[1], trans[2]]
        left = screen_position(point, left_trans, left_rot, s, -HAND_X)

        offset = [-right[0], -right[1]]
        offset_left = [-left[0], -left[1]]
        if max(abs(v) for v in offset) > 2.2 or max(abs(v) for v in offset_left) > 2.2:
            print(f"{stem}: WARNING implausible offset {offset}")
            continue

        gun_path = guns_dir / f"{stem}.json"
        if not gun_path.exists():
            continue
        data = json.loads(gun_path.read_text(encoding="utf-8"))
        handling = data.setdefault("handling", {})
        handling["ads_offset"] = [round(offset[0], 4), round(offset[1], 4)]
        handling["ads_offset_left"] = [round(offset_left[0], 4), round(offset_left[1], 4)]
        gun_path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")

        # The same point as a cube-less anchor bone: the renderer reads the
        # real screen position of it while the weapon is at the hip, which is
        # exact no matter how the model or the display transform look.
        bones[:] = [b for b in bones if b.get("name") != "aim"]
        root_bone = next(b for b in bones if "parent" not in b)
        bones.append({"name": "aim", "parent": root_bone["name"], "pivot": point})
        geo_path.write_text(json.dumps(geo, separators=(",", ":")) if compact
                            else json.dumps(geo, indent=2) + "\n", encoding="utf-8")

        print(f"{stem:<24} sight={source:<10} point={point} "
              f"right=({right[0]:.3f},{right[1]:.3f}) offset=({offset[0]:.3f},{offset[1]:.3f}) "
              f"left_offset=({offset_left[0]:.3f},{offset_left[1]:.3f})")


main()
