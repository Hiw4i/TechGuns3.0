"""Convert cuboid Techguns 1.12 ModelRenderer guns into editable GeckoLib geometry.

Usage: python tools/convert_legacy_guns.py PATH_TO_TECHGUNS2_CE
The converter deliberately stops on an unrecognised cuboid so geometry is never
silently dropped. Animation curves still require a separate visual review.
"""

from __future__ import annotations

import json
import math
import re
import shutil
import sys
from pathlib import Path


GUNS = {
    "pistol": ("ModelPistol", "pistol3.png"),
    "ak47": ("ModelAK", "ak47texture.png"),
    "combat_shotgun": ("ModelCombatShotgun", "combatshotgun.png"),
    "minigun": ("ModelMinigun", "minigun.png"),
    "teslagun": ("ModelTeslaGun", "teslagun.png"),
}
NUMBER = r"[-+]?(?:\d+(?:\.\d*)?|\.\d+)(?:[fFdD])?"


def number(value: str) -> float:
    return float(value.rstrip("fFdD"))


def arguments(text: str) -> list[float]:
    return [number(part.strip()) for part in text.split(",")]


def convert(java: str, item_id: str, item_model: bool = False) -> dict:
    width = int(re.search(r"textureWidth\s*=\s*(\d+)", java).group(1))
    height = int(re.search(r"textureHeight\s*=\s*(\d+)", java).group(1))
    constructors = re.findall(
        r"(\w+)\s*=\s*new ModelRenderer\(this,\s*(\d+),\s*(\d+)\)", java
    )
    bones = []
    for name, u, v in constructors:
        boxes = re.findall(rf"\b{re.escape(name)}\.addBox\(([^)]+)\)", java)
        point = re.search(rf"\b{re.escape(name)}\.setRotationPoint\(([^)]+)\)", java)
        rotation = re.search(rf"setRotation\({re.escape(name)},\s*([^)]+)\)", java)
        if not boxes or not point or not rotation:
            raise ValueError(f"Missing geometry or transform for {item_id}:{name}")
        px, py, pz = arguments(point.group(1))
        rx, ry, rz = arguments(rotation.group(1))
        cubes = []
        for box in boxes:
            vals = arguments(box)
            if len(vals) not in (6, 7):
                raise ValueError(f"Unsupported cuboid for {item_id}:{name}: {box}")
            x, y, z, sx, sy, sz = vals[:6]
            # Legacy ModelRenderer Y grows down; Bedrock/GeckoLib Y grows up.
            cubes.append({
                "origin": [px + x, -(py + y + sy), pz + z],
                "size": [sx, sy, sz],
                "uv": [int(u), int(v)],
            })
        bone = {
            "name": name,
            "pivot": [px, -py, pz],
            "rotation": [
                round(-math.degrees(rx), 5),
                round(math.degrees(ry), 5),
                round(-math.degrees(rz), 5),
            ],
            "cubes": cubes,
        }
        if item_model:
            bone["parent"] = "gun"
        bones.append(bone)
    if len(bones) != len(constructors):
        raise ValueError(f"Lost bones in {item_id}")
    if item_model:
        bones.insert(0, {"name": "gun", "pivot": [0, 0, 0]})
        if item_id == "minigun":
            bones.insert(1, {"name": "barrels", "parent": "gun", "pivot": [0, 0, 0]})
            for bone in bones[2:]:
                if re.fullmatch(r"r\d+", bone["name"]):
                    bone["parent"] = "barrels"
    return {
        "format_version": "1.12.0",
        "minecraft:geometry": [{
            "description": {
                "identifier": f"geometry.techguns3.{item_id}",
                "texture_width": width,
                "texture_height": height,
                "visible_bounds_width": 4,
                "visible_bounds_height": 4,
                "visible_bounds_offset": [0, 0, 0],
            },
            "bones": bones,
        }],
    }


def move_y(geometry: dict, amount: float) -> None:
    """ModelRenderer measured Y down from the top; Gecko entity/block models stand on Y=0."""
    for bone in geometry["minecraft:geometry"][0]["bones"]:
        bone["pivot"][1] += amount
        for cube in bone["cubes"]:
            cube["origin"][1] += amount


def main() -> None:
    upstream = Path(sys.argv[1])
    out = Path(__file__).resolve().parents[1] / "src/main/resources/assets/techguns3"
    for item_id, (class_name, texture_name) in GUNS.items():
        java_path = upstream / f"src/main/java/techguns/client/models/guns/{class_name}.java"
        texture_path = upstream / f"src/main/resources/assets/techguns/textures/guns/{texture_name}"
        geometry = convert(java_path.read_text(encoding="utf-8"), item_id, item_model=True)
        geo_path = out / f"geckolib/models/item/{item_id}.geo.json"
        geo_path.parent.mkdir(parents=True, exist_ok=True)
        geo_path.write_text(json.dumps(geometry, indent=2) + "\n", encoding="utf-8")
        target_texture = out / f"textures/item/{item_id}.png"
        target_texture.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(texture_path, target_texture)
        print(f"{item_id}: {len(geometry['minecraft:geometry'][0]['bones'])} bones")

    turret_source = upstream / "src/main/java/techguns/client/models/npcs/ModelTurret.java"
    turret_geometry = convert(turret_source.read_text(encoding="utf-8"), "turret_head")
    move_y(turret_geometry, 24)
    turret_geo_path = out / "geckolib/models/entity/turret_head.geo.json"
    turret_geo_path.parent.mkdir(parents=True, exist_ok=True)
    turret_geo_path.write_text(json.dumps(turret_geometry, indent=2) + "\n", encoding="utf-8")
    shutil.copyfile(upstream / "src/main/resources/assets/techguns/textures/blocks/turret_base.png",
                    out / "textures/entity/turret_head.png")
    print(f"turret_head: {len(turret_geometry['minecraft:geometry'][0]['bones'])} bones")

    base_source = upstream / "src/main/java/techguns/client/models/machines/ModelTurretBase.java"
    base_geometry = convert(base_source.read_text(encoding="utf-8"), "turret_base")
    move_y(base_geometry, 24)
    base_geo_path = out / "geckolib/models/block/turret_base.geo.json"
    base_geo_path.parent.mkdir(parents=True, exist_ok=True)
    base_geo_path.write_text(json.dumps(base_geometry, indent=2) + "\n", encoding="utf-8")
    print(f"turret_base: {len(base_geometry['minecraft:geometry'][0]['bones'])} bones")


if __name__ == "__main__":
    main()
