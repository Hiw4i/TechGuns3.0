"""Add the `muzzle` anchor bone to every gun geo model.

The muzzle anchor is what the first-person muzzle flash is attached to. It is a
cube-less bone parented to the model root, so it inherits the root's animated
transform (the recoil kick) and therefore travels with the barrel.

The forward axis of a model is not stored in the model itself - it follows
from the item display transform in models/item/<gun>.json:

    rotation [0,0,0]    -> the barrel points at -Z
    rotation [0,270,0]  -> the barrel points at +X

(the second case also flips X, because GeckoLib mirrors geo models on X).

Run from the project root:  python tools/add_muzzle_bones.py
"""

import json
import math
import re
import statistics
from pathlib import Path

root = Path(__file__).resolve().parents[1]
models = root / "src/main/resources/assets/techguns3/geckolib/models/item"
item_models = root / "src/main/resources/assets/techguns3/models/item"

IDENT = [[1, 0, 0, 0], [0, 1, 0, 0], [0, 0, 1, 0], [0, 0, 0, 1]]

MOZZLE_BLOCK = re.compile(
    r",\n        \{\n          \"name\": \"muzzle\",\n          \"pivot\": \[\n(?:[^\]]*?)\n          \],\n"
    r"          \"parent\": \"[^\"]+\"\n        \}")


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


def forward_axis(display_rot_y):
    """Unit vector (in the authored model space) that points out of the muzzle."""
    if display_rot_y == 0:
        return (0, 0, -1)
    if display_rot_y == 270:
        return (1, 0, 0)
    raise SystemExit(f"unsupported display rotation {display_rot_y}")


def muzzle_anchor(pts, forward):
    """Centre of the front-most face, pulled 1.5 units back into the barrel."""
    axis = max(range(3), key=lambda i: abs(forward[i]))
    sign = 1 if forward[axis] > 0 else -1
    depth = max(p[axis] * sign for p, _ in pts)
    face = [p for p, _ in pts if abs(p[axis] * sign - depth) < 1.01]
    anchor = []
    for i in range(3):
        if i == axis:
            anchor.append(round((depth - 1.5) * sign, 2))
        else:
            anchor.append(round(statistics.median(p[i] for p in face), 2))
    return anchor, depth


def bone_block(name, pivot, parent):
    coords = ",\n".join(f"            {v}" for v in pivot)
    return (f'        {{\n'
            f'          "name": "{name}",\n'
            f'          "pivot": [\n{coords}\n'
            f'          ],\n'
            f'          "parent": "{parent}"\n'
            f'        }}')


for geo_path in sorted(models.glob("*.geo.json")):
    stem = geo_path.name[: -len(".geo.json")]
    text = geo_path.read_text(encoding="utf-8")
    pretty = "\n" in text.strip()

    # Start from a clean slate: drop a previously generated anchor.
    text = MOZZLE_BLOCK.sub("", text)
    geo = json.loads(text)
    geometry = geo["minecraft:geometry"][0]
    bones = geometry["bones"]
    bones[:] = [b for b in bones if b.get("name") != "muzzle"]

    display = json.loads((item_models / f"{stem}.json").read_text(encoding="utf-8"))
    rot_y = display["display"]["firstperson_righthand"]["rotation"][1]
    forward = forward_axis(rot_y)

    pts = []
    collect(bones, None, IDENT, pts)
    if not pts:
        raise SystemExit(f"{stem}: model has no cubes")

    anchor, depth = muzzle_anchor(pts, forward)
    root_bone = next(b for b in bones if "parent" not in b)

    if pretty:
        marker = "\n      ]\n    }\n  ]\n}"
        idx = text.rindex(marker)
        text = text[:idx] + ",\n" + bone_block("muzzle", anchor, root_bone["name"]) + text[idx:]
    else:
        bones.append({"name": "muzzle", "pivot": anchor, "parent": root_bone["name"]})
        text = json.dumps(geo, separators=(",", ":"))

    geo_path.write_text(text, encoding="utf-8")
    print(f"{stem:<24} forward={forward} depth={depth} anchor={anchor}")
