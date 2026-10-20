"""Converts the Techguns 1.12 java models into GeckoLib geo models.

The four weapons that were still flat sprites (revolver, m4, thompson,
bolt_action) are built from boxes in the original mod. This script turns those
boxes into GeckoLib cuboids.

Coordinate conversion (calibrated against the AK-47, which is already ported):

* The 1.12 model space has Y pointing down, GeckoLib has Y pointing up, so
  every Y coordinate is mirrored.
* A rotation about X or Z is mirrored with it, a rotation about Y is not.
* The box of a part is placed relative to its rotation point, exactly like in
  the original.

Run from the project root:  python tools/convert_legacy_models.py
"""

import json
import math
import re
import urllib.request
from pathlib import Path

root = Path(__file__).resolve().parents[1]
cache = root / "tools/.legacy-cache"
out_dir = root / "src/main/resources/assets/techguns3/geckolib/models/item"

LEGACY = "https://raw.githubusercontent.com/TheSlize/Techguns2-CE/50f8a02678415f1238f1a1aa164a949b0aab5a46/src/main/java/techguns/client/models/guns/"

# Legacy model class -> gun id.
GUNS = {
    "ModelRevolver": "revolver",
    "ModelM4": "m4",
    "ModelThompson": "thompson",
    "ModelBoltaction": "bolt_action",
}

PART = re.compile(
    r"(?P<name>\w+)\s*=\s*new ModelRenderer\(this,\s*(?P<u>-?\d+),\s*(?P<v>-?\d+)\);"
    r".*?"
    r"(?P=name)\.addBox\((?P<x>-?[\d.]+)F?,\s*(?P<y>-?[\d.]+)F?,\s*(?P<z>-?[\d.]+)F?,"
    r"\s*(?P<w>-?[\d.]+)F?,\s*(?P<h>-?[\d.]+)F?,\s*(?P<d>-?[\d.]+)F?\);"
    r".*?"
    r"(?P=name)\.setRotationPoint\((?P<rx>-?[\d.]+)F?,\s*(?P<ry>-?[\d.]+)F?,\s*(?P<rz>-?[\d.]+)F?\);",
    re.S)
ROT = re.compile(r"setRotation\((?P<name>\w+),\s*(?P<rx>-?[\d.]+)F?,\s*(?P<ry>-?[\d.]+)F?,\s*(?P<rz>-?[\d.]+)F?\);")
SIZE = re.compile(r"textureWidth\s*=\s*(\d+);\s*textureHeight\s*=\s*(\d+);", re.S)


def fetch(name: str) -> str:
    cache.mkdir(parents=True, exist_ok=True)
    path = cache / f"{name}.java"
    if not path.exists():
        url = LEGACY + f"{name}.java"
        print(f"downloading {url}")
        urllib.request.urlretrieve(url, path)
    return path.read_text(encoding="utf-8", errors="replace")


def convert(name: str, source: str):
    size = SIZE.search(source)
    tex_w, tex_h = (int(size.group(1)), int(size.group(2))) if size else (64, 64)
    rotations = {m.group("name"): (float(m.group("rx")), float(m.group("ry")), float(m.group("rz")))
                  for m in ROT.finditer(source)}

    bones = [{"name": "gun", "pivot": [0, 0, 0]}]
    for m in PART.finditer(source):
        part = m.group("name")
        u, v = int(m.group("u")), int(m.group("v"))
        bx, by, bz = (float(m.group(k)) for k in ("x", "y", "z"))
        w, h, d = (float(m.group(k)) for k in ("w", "h", "d"))
        rx, ry, rz = (float(m.group(k)) for k in ("rx", "ry", "rz"))
        # MC allows negative sizes, the box then grows to the other side.
        min_x, min_y, min_z = min(bx, bx + w), min(by, by + h), min(bz, bz + d)
        size_x, size_y, size_z = abs(w), abs(h), abs(d)
        rot = rotations.get(part, (0.0, 0.0, 0.0))
        bone = {
            "name": part,
            "pivot": [rx, -ry, rz],
            "rotation": [round(math.degrees(-rot[0]), 5),
                         round(math.degrees(rot[1]), 5),
                         round(math.degrees(-rot[2]), 5)],
            "cubes": [{
                "origin": [round(rx + min_x, 4), round(-(ry + min_y + size_y), 4), round(rz + min_z, 4)],
                "size": [round(size_x, 4), round(size_y, 4), round(size_z, 4)],
                "uv": [u, v],
            }],
        }
        bones.append(bone)
    return {
        "format_version": "1.12.0",
        "minecraft:geometry": [{
            "description": {
                "identifier": f"geometry.techguns3.{name}",
                "texture_width": tex_w,
                "texture_height": tex_h,
                "visible_bounds_width": 4,
                "visible_bounds_height": 4,
                "visible_bounds_offset": [0, 0, 0],
            },
            "bones": bones,
        }],
    }


for legacy_name, gun_id in GUNS.items():
    geo = convert(gun_id, fetch(legacy_name))
    out = out_dir / f"{gun_id}.geo.json"
    out.write_text(json.dumps(geo, indent=2) + "\n", encoding="utf-8")
    print(f"{gun_id:<12} {len(geo['minecraft:geometry'][0]['bones']) - 1} parts -> {out.name}")
