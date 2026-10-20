"""Report sight-like bones of every gun geo model (dev tool, not shipped).

The aim anchor of a weapon is the rear sight (or the eyepiece of a scope):
that is the point that has to end up on the screen centre while aiming.
"""
import json
import math
from pathlib import Path

root = Path(__file__).resolve().parents[1]
models = root / "src/main/resources/assets/techguns3/geckolib/models/item"

CANDIDATES = ("is", "sight", "scope", "optic", "iron")


def is_candidate(name: str) -> bool:
    low = name.lower()
    return any(c in low for c in CANDIDATES)


for path in sorted(models.glob("*.geo.json")):
    stem = path.name[: -len(".geo.json")]
    geo = json.loads(path.read_text(encoding="utf-8"))
    bones = geo["minecraft:geometry"][0].get("bones", [])
    found = []
    for bone in bones:
        name = bone.get("name", "")
        if not is_candidate(name):
            continue
        piv = bone.get("pivot", [0, 0, 0])
        cubes = bone.get("cubes", [])
        extent = ""
        if cubes:
            xs = [c["origin"][0] for c in cubes] + [c["origin"][0] + c["size"][0] for c in cubes]
            ys = [c["origin"][1] for c in cubes] + [c["origin"][1] + c["size"][1] for c in cubes]
            zs = [c["origin"][2] for c in cubes] + [c["origin"][2] + c["size"][2] for c in cubes]
            extent = (f" x[{min(xs):.1f},{max(xs):.1f}] y[{min(ys):.1f},{max(ys):.1f}]"
                      f" z[{min(zs):.1f},{max(zs):.1f}]")
        found.append((name, piv, extent))
    print(f"### {stem}")
    for name, piv, extent in found:
        print(f"    {name:<16} pivot={piv}{extent}")
    if not found:
        print("    (no sight bone)")
