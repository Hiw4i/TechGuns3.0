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

for name in ("pistol", "revolver", "ak47", "m4", "thompson", "bolt_action", "combat_shotgun", "minigun",
             "teslagun", "tfg", "nucleardeathray", "gaussrifle", "rocketlauncher",
             "guidedmissilelauncher", "grimreaper", "sonicshotgun", "lasergun",
             "laserpistol", "pulserifle", "vector", "pdw", "as50"):
    texture = assets / "textures/item" / f"{name}.png"
    if not texture.exists():
        texture = assets / "textures/item/guns" / f"{name}.png"
    for path in (
        assets / "geckolib/models/item" / f"{name}.geo.json",
        assets / "geckolib/animations/item" / f"{name}.animation.json",
    ):
        if not path.exists():
            errors.append(f"missing gun asset: {path.relative_to(root)}")
    if not texture.exists():
        errors.append(f"missing gun texture: {name}.png")
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
                errors.append(f"missing sound: {name}")

# --- gun datapack definitions (data-driven GunStats) ---
guns_dir = resources / "data/techguns3/guns"
expected_guns = {"pistol","revolver","ak47","m4","thompson","combat_shotgun","minigun",
    "teslagun","bolt_action","biogun","flamethrower","tfg","nucleardeathray","gaussrifle",
    "rocketlauncher","guidedmissilelauncher","grimreaper","sonicshotgun","lasergun",
    "laserpistol","pulserifle","vector","pdw","as50"}
valid_projectiles = {"BALLISTIC","ELECTRIC","BIO","FIRE","EXPLOSIVE","BEAM","LASER",
    "SONIC","PLASMA","GAUSS","ADVANCED"}
if not guns_dir.is_dir():
    errors.append("missing guns datapack dir: data/techguns3/guns")
else:
    found = {p.stem for p in guns_dir.glob("*.json")}
    for name in sorted(expected_guns - found):
        errors.append(f"missing gun datapack: {name}.json")
    for path in sorted(guns_dir.glob("*.json")):
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
        except (UnicodeDecodeError, json.JSONDecodeError) as exc:
            errors.append(f"{path.relative_to(root)}: {exc}")
            continue
        if data.get("schema_version", 1) != 1:
            errors.append(f"{path.name}: unsupported schema_version {data.get('schema_version')}")
        if "ammo" not in data:
            errors.append(f"{path.name}: missing field ammo")
        bal = data.get("ballistic", {})
        for field in ("damage","cooldown_ticks","magazine","reload_ticks"):
            if field not in bal:
                errors.append(f"{path.name}: missing ballistic.{field}")
        if bal.get("magazine", 1) <= 0:
            errors.append(f"{path.name}: magazine must be > 0")
        if bal.get("cooldown_ticks", 1) < 1:
            errors.append(f"{path.name}: cooldown_ticks must be >= 1")
        handling = data.get("handling", {})
        if handling.get("projectile") not in valid_projectiles:
            errors.append(f"{path.name}: unknown projectile {handling.get('projectile')}")
        sounds_obj = data.get("sounds", {})
        for snd in ("fire","reload","extra","action"):
            if snd in sounds_obj and isinstance(sounds_obj[snd], str) and sounds_obj[snd].startswith("techguns3:"):
                key = sounds_obj[snd].split(":",1)[1]
                if key not in sounds:
                    errors.append(f"{path.name}: unknown sound {sounds_obj[snd]}")
        for ammo_key in ("ammo","empty_magazine"):
            if ammo_key in data and isinstance(data[ammo_key], str) and data[ammo_key].startswith("techguns3:"):
                ammo_name = data[ammo_key].split(":",1)[1]
                if ammo_name not in item_ids:
                    errors.append(f"{path.name}: unknown item {data[ammo_key]}")
        extras = data.get("extras", {})
        if "extra_ammo" in extras and isinstance(extras["extra_ammo"], list):
            for ammo_ref in extras["extra_ammo"]:
                if isinstance(ammo_ref, str) and ammo_ref.startswith("techguns3:"):
                    ammo_name = ammo_ref.split(":",1)[1]
                    if ammo_name not in item_ids:
                        errors.append(f"{path.name}: unknown item {ammo_ref}")

# --- GeckoLib geo/animation cross-checks -------------------------------------
# GeckoLib silently ignores animations that reference a bone the model does not
# have, so a typo there is invisible in game: the bone simply never moves.
# These checks turn that into a build failure.

# Easing names accepted by GeckoLib's EasingType registry (see
# com.geckolib.animation.object.EasingType). "linear"/"none" are aliases.
VALID_EASING = {
    "linear", "none", "step",
    "easeinsine", "easeoutsine", "easeinoutsine",
    "easeinquad", "easeoutquad", "easeinoutquad",
    "easeincubic", "easeoutcubic", "easeinoutcubic",
    "easeinquart", "easeoutquart", "easeinoutquart",
    "easeinquint", "easeoutquint", "easeinoutquint",
    "easeinexpo", "easeoutexpo", "easeinoutexpo",
    "easeincirc", "easeoutcirc", "easeinoutcirc",
    "easeinback", "easeoutback", "easeinoutback",
    "easeinelastic", "easeoutelastic", "easeinoutelastic",
    "easeinbounce", "easeoutbounce", "easeinoutbounce",
    "catmullrom",
}
VALID_LERP_MODES = {"linear", "catmullrom", "step"}


def geo_bones(geo: dict) -> set:
    names = set()
    for geometry in geo.get("minecraft:geometry", []):
        for bone in geometry.get("bones", []):
            if "name" in bone:
                names.add(bone["name"])
    return names


def geo_locators(geo: dict) -> set:
    names = set()
    for geometry in geo.get("minecraft:geometry", []):
        names.update(geometry.get("locators", {}).keys())
    return names


def anchor_bones(geo: dict) -> set:
    """Cube-less anchor bones the renderer hangs first-person FX off."""
    names = set()
    for geometry in geo.get("minecraft:geometry", []):
        for bone in geometry.get("bones", []):
            if bone.get("name") and not bone.get("cubes"):
                names.add(bone["name"])
    return names


def walk_keyframes(node, path, out):
    """Collect (bone, time, easing) triples from an animation track tree."""
    if not isinstance(node, dict):
        return
    for key, value in node.items():
        if isinstance(value, dict):
            if "easing" in value and isinstance(value["easing"], str):
                out.append((path, key, value["easing"]))
            if "lerp_mode" in value and isinstance(value["lerp_mode"], str):
                out.append((path, key, value["lerp_mode"]))
            walk_keyframes(value, path, out)


def check_animation(anim_path: Path, geo_path: Path, required_animations, required_locators):
    rel = anim_path.relative_to(root)
    if not geo_path.exists():
        return
    try:
        anim = json.loads(anim_path.read_text(encoding="utf-8"))
        geo = json.loads(geo_path.read_text(encoding="utf-8"))
    except (UnicodeDecodeError, json.JSONDecodeError) as exc:
        errors.append(f"{rel}: {exc}")
        return

    bones = geo_bones(geo)
    anchors = anchor_bones(geo)
    animations = anim.get("animations", {})

    for name in sorted(required_animations):
        if name not in animations:
            errors.append(f"{rel}: missing animation '{name}'")

    for name, track in sorted(animations.items()):
        for bone in sorted(track.get("bones", {}).keys()):
            if bone not in bones:
                errors.append(f"{rel}: animation '{name}' drives unknown bone '{bone}'")
        easing_hits = []
        walk_keyframes(track.get("bones", {}), name, easing_hits)
        for bone, time, value in easing_hits:
            if value.lower() not in VALID_EASING and value.lower() not in VALID_LERP_MODES:
                errors.append(f"{rel}: animation '{name}' bone '{bone}' t={time} "
                              f"uses unknown easing/lerp_mode '{value}'")

    for name in sorted(required_locators):
        if name not in anchors:
            errors.append(f"{geo_path.relative_to(root)}: missing anchor bone '{name}'")


# Every gun that has a GeckoLib model needs a muzzle anchor bone so the
# first-person muzzle flash can be attached to the barrel.
for geo_path in sorted((assets / "geckolib/models/item").glob("*.geo.json")):
    anim_path = assets / "geckolib/animations/item" / geo_path.name.replace(".geo.json", ".animation.json")
    check_animation(anim_path, geo_path, required_animations=("fire", "reload"),
                    required_locators=("muzzle", "aim"))

for kind in ("block", "entity"):
    for geo_path in sorted((assets / "geckolib/models" / kind).glob("*.geo.json")):
        anim_path = assets / "geckolib/animations" / kind / geo_path.name.replace(".geo.json", ".animation.json")
        check_animation(anim_path, geo_path, required_animations=(),
                        required_locators=())

# Reload animation length must match the server-side reload time, otherwise the
# weapon is still moving after the magazine is already in.
for path in sorted(guns_dir.glob("*.json")):
    data = json.loads(path.read_text(encoding="utf-8"))
    name = path.stem
    anim_path = assets / "geckolib/animations/item" / f"{name}.animation.json"
    if not anim_path.exists():
        continue
    anim = json.loads(anim_path.read_text(encoding="utf-8"))
    reload_anim = anim.get("animations", {}).get("reload")
    if not reload_anim:
        continue
    expected = data.get("ballistic", {}).get("reload_ticks", 0) / 20.0
    actual = reload_anim.get("animation_length", 0)
    if expected > 0 and abs(actual - expected) > 0.051:
        errors.append(f"{anim_path.relative_to(root)}: reload length {actual}s does not match "
                      f"{name}.json ballistic.reload_ticks ({expected}s)")

if errors:
    raise SystemExit("\n".join(errors))
print(f"Validated {len(list(resources.rglob('*.json')))} JSON files and checkpoint assets")
