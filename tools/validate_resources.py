"""Fast, dependency-free checks for the checkpoint's asset and data pack files."""

import json
import re
from pathlib import Path

root = Path(__file__).resolve().parents[1]
resources = root / "src/main/resources"
assets = resources / "assets/techguns3"
errors = []

for path in sorted(resources.rglob("*.json")):
    try:
        json.loads(path.read_text(encoding="utf-8"))
    except (UnicodeDecodeError, json.JSONDecodeError) as exc:
        errors.append(f"{path.relative_to(root)}: {exc}")

items_java = (root / "src/main/java/com/techguns/techguns3/registry/TGItems.java").read_text(encoding="utf-8")
blocks_java = (root / "src/main/java/com/techguns/techguns3/registry/TGBlocks.java").read_text(encoding="utf-8")
item_ids = set(re.findall(r'ITEMS\.register(?:SimpleItem|Item)\("([a-z0-9_]+)"', items_java))
block_ids = set(re.findall(r'(?:ore|xpOre)\("([a-z0-9_]+)"', blocks_java)) | {"metal_panel", "turret_base"}
item_ids |= block_ids
for name in sorted(item_ids):
    if not (assets / "items" / f"{name}.json").exists():
        errors.append(f"missing item definition: {name}")
for name in sorted(block_ids):
    if not (assets / "blockstates" / f"{name}.json").exists():
        errors.append(f"missing blockstate: {name}")
    if not (resources / "data/techguns3/loot_table/blocks" / f"{name}.json").exists():
        errors.append(f"missing block loot: {name}")

for name in ("pistol", "ak47", "combat_shotgun", "minigun", "teslagun"):
    for path in (
        assets / "geckolib/models/item" / f"{name}.geo.json",
        assets / "geckolib/animations/item" / f"{name}.animation.json",
        assets / "textures/item" / f"{name}.png",
    ):
        if not path.exists():
            errors.append(f"missing gun asset: {path.relative_to(root)}")
for name, subtype in (("turret_base", "block"), ("turret_head", "entity")):
    for path in (
        assets / "geckolib/models" / subtype / f"{name}.geo.json",
        assets / "geckolib/animations" / subtype / f"{name}.animation.json",
        assets / "textures" / subtype / f"{name}.png",
    ):
        if not path.exists():
            errors.append(f"missing turret asset: {path.relative_to(root)}")

if not (assets / "textures/gui/turret_base.png").exists():
    errors.append("missing turret GUI texture")

sounds = json.loads((assets / "sounds.json").read_text(encoding="utf-8"))
for sound in sounds.values():
    for entry in sound.get("sounds", []):
        name = entry if isinstance(entry, str) else entry.get("name", "")
        if name.startswith("techguns3:"):
            path = assets / "sounds" / (name.split(":", 1)[1] + ".ogg")
            if not path.exists():
                errors.append(f"missing sound: {path.relative_to(root)}")

if errors:
    raise SystemExit("\n".join(errors))
print(f"Validated {len(list(resources.rglob('*.json')))} JSON files and checkpoint assets")
