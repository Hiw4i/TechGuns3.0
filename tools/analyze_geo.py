"""Report the model-space extents of every gun geo model (dev tool, not shipped).

Blockbench/GeckoLib bone convention: a bone's cubes live in the bone's local
space; the bone transform relative to the parent is
T(pivot) * R(rotation) * T(-pivot).

The forward axis of a model follows from its item display transform:
  models/item/<gun>.json firstperson rotation [0,0,0]   -> barrel at -Z
  models/item/<gun>.json firstperson rotation [0,270,0] -> barrel at +X
"""
import json
import math
from pathlib import Path

root = Path(__file__).resolve().parents[1]
models = root / "src/main/resources/assets/techguns3/geckolib/models/item"

IDENT = [[1, 0, 0, 0], [0, 1, 0, 0], [0, 0, 1, 0], [0, 0, 0, 1]]


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


def per_bone_bbox(pts):
    bb = {}
    for p, name in pts:
        if name not in bb:
            bb[name] = [p[0], p[1], p[2], p[0], p[1], p[2]]
        b = bb[name]
        b[0] = min(b[0], p[0]); b[1] = min(b[1], p[1]); b[2] = min(b[2], p[2])
        b[3] = max(b[3], p[0]); b[4] = max(b[4], p[1]); b[5] = max(b[5], p[2])
    return bb


for path in sorted(models.glob("*.geo.json")):
    stem = path.name[: -len(".geo.json")]
    disp = json.loads((root / "src/main/resources/assets/techguns3/models/item" / f"{stem}.json")
                      .read_text(encoding="utf-8"))
    rot_y = disp["display"]["firstperson_righthand"]["rotation"][1]
    axis = "Z" if rot_y == 0 else "X"
    data = json.loads(path.read_text(encoding="utf-8"))
    bones = data["minecraft:geometry"][0].get("bones", [])
    pts = []
    collect(bones, None, IDENT, pts)
    bb = per_bone_bbox(pts)
    key = 2 if axis == "Z" else 0
    print(f"### {path.stem}  displayRotY={rot_y}  forward=+{axis}  "
          f"(muzzle at {'max' if axis == 'Z' else 'max'})")
    ordered = sorted(bb.items(), key=lambda kv: kv[1][3 + key], reverse=True)
    for name, b in ordered[:5]:
        print(f"    front {name:<22} x[{b[0]:7.2f},{b[3]:7.2f}] y[{b[1]:7.2f},{b[4]:7.2f}] z[{b[2]:7.2f},{b[5]:7.2f}]")
    for name, b in ordered[-3:]:
        print(f"    rear  {name:<22} x[{b[0]:7.2f},{b[3]:7.2f}] y[{b[1]:7.2f},{b[4]:7.2f}] z[{b[2]:7.2f},{b[5]:7.2f}]")
