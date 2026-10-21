"""Adds the `aim` anchor bone to every gun geo model.

The aim anchor is the rear sight of the weapon - the point that has to end up
in the middle of the screen while the player is aiming. It is a cube-less bone
parented to the model root, so the renderer can read where it really ends up
on the screen and slide the weapon accordingly.

The sight itself comes from the model's sight bones where they exist and from
the top of the receiver at the back of the weapon otherwise.

Run from the project root:  python tools/add_aim_bones.py
"""

import json
import math
from pathlib import Path

root = Path(__file__).resolve().parents[1]
models = root / "src/main/resources/assets/techguns3/geckolib/models/item"
item_models = root / "src/main/resources/assets/techguns3/models/item"

IDENT = [[1, 0, 0, 0], [0, 1, 0, 0], [0, 0, 1, 0], [0, 0, 0, 1]]

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


def forward_axis(display_rot_y):
    """Unit vector (in the authored model space) that points out of the muzzle."""
    if display_rot_y == 0:
        return (0, 0, -1)
    if display_rot_y == 270:
        return (1, 0, 0)
    raise SystemExit(f"unsupported display rotation {display_rot_y}")


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


for geo_path in sorted(models.glob("*.geo.json")):
    stem = geo_path.name[: -len(".geo.json")]
    text = geo_path.read_text(encoding="utf-8")
    pretty = "\n" in text.strip()

    geo = json.loads(text)
    geometry = geo["minecraft:geometry"][0]
    bones = geometry["bones"]
    bones[:] = [b for b in bones if b.get("name") != "aim"]

    display = json.loads((item_models / f"{stem}.json").read_text(encoding="utf-8"))
    rot_y = display["display"]["firstperson_righthand"]["rotation"][1]

    corners = []
    collect(bones, None, IDENT, corners)
    if not corners:
        raise SystemExit(f"{stem}: model has no cubes")

    muzzle_bone = next((b for b in bones if b.get("name") == "muzzle"), None)
    if muzzle_bone is None:
        print(f"{stem}: no muzzle anchor, skipping")
        continue
    muzzle = muzzle_bone.get("pivot", [0, 0, 0])

    point, source = aim_point(stem, bones, corners, muzzle, rot_y)
    root_bone = next(b for b in bones if "parent" not in b)

    if pretty:
        marker = "\n      ]\n    }\n  ]\n}"
        idx = text.rindex(marker)
        coords = ",\n".join(f"            {v}" for v in point)
        block = (f'        {{\n'
                 f'          "name": "aim",\n'
                 f'          "pivot": [\n{coords}\n'
                 f'          ],\n'
                 f'          "parent": "{root_bone["name"]}"\n'
                 f'        }}')
        text = text[:idx] + ",\n" + block + text[idx:]
    else:
        bones.append({"name": "aim", "parent": root_bone["name"], "pivot": point})
        text = json.dumps(geo, separators=(",", ":"))

    geo_path.write_text(text, encoding="utf-8")
    print(f"{stem:<24} sight={source:<10} point={point}")
