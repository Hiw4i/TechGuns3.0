"""Add easing curves to the gun animation tracks.

GeckoLib applies a keyframe's easing to the segment that *starts* at that
keyframe, so the curve of a track is described by the easing on its first,
second, ... keyframe. Linear interpolation between keyframes makes every
weapon move like a robot; real recoil rises fast and settles slowly.

Rules used here:

* ``fire``   - the body kick: fast rise (ease out), fast drop off the peak
  (ease out quad), then a soft settle.
* ``reload`` - the weapon is eased into the reload pose and eased back out.
* ``charge`` - a smooth ramp into the charging pose.
* ``spin``   - a constant rotation has to stay linear, otherwise the barrels
  would speed up and slow down.
* Moving parts (slide, bolt, pump, magazine, ...) stay linear: they are
  mechanical and must snap.

Run from the project root:  python tools/add_animation_easing.py
"""

import json
import re
from pathlib import Path

root = Path(__file__).resolve().parents[1]
anim_dir = root / "src/main/resources/assets/techguns3/geckolib/animations/item"

# Easing per keyframe index, for the tracks of a given animation.
CURVES = {
    "fire": ["easeoutcubic", "easeoutquad", "easeinoutcubic", "easeoutcubic"],
    "reload": ["easeinoutcubic", "easeinoutcubic", "easeinoutcubic", "easeinoutcubic"],
    "charge": ["easeinoutcubic", "easeinoutcubic", "easeinoutcubic"],
    "spin": ["linear"],
}
DEFAULT_CURVE = ["easeinoutcubic"]

# Bones that are mechanically moved and must not be smoothed.
MECHANICAL = ("slide", "pump", "magazine", "bolt", "coil", "glow", "shell", "cylinder",
              "handle", "trigger", "charging", "arm")


def is_mechanical(bone: str) -> bool:
    lowered = bone.lower()
    return any(part in lowered for part in MECHANICAL)


def curve_for(animation: str, bone: str, index: int) -> str:
    if is_mechanical(bone):
        return "linear"
    curves = CURVES.get(animation, DEFAULT_CURVE)
    return curves[index % len(curves)]


def add_easing(value, easing: str):
    if isinstance(value, dict):
        if "vector" in value:
            value = dict(value)
            value["easing"] = easing
            return value
        return value
    return {"vector": value, "easing": easing}


for path in sorted(anim_dir.glob("*.animation.json")):
    data = json.loads(path.read_text(encoding="utf-8"))
    changed = False
    for anim_name, anim in data.get("animations", {}).items():
        for bone, tracks in anim.get("bones", {}).items():
            for transform, keyframes in tracks.items():
                if not isinstance(keyframes, dict):
                    continue
                times = sorted(keyframes.keys(), key=lambda t: float(t))
                for index, time in enumerate(times):
                    easing = curve_for(anim_name, bone, index)
                    keyframes[time] = add_easing(keyframes[time], easing)
                    changed = True
    if changed:
        text = json.dumps(data, indent=2) + "\n"
        # Keep the vectors on one line, they are far easier to read that way.
        text = re.sub(r'"vector": \[\n\s*([-\d.]+),\n\s*([-\d.]+),\n\s*([-\d.]+)\n\s*\]',
                      r'"vector": [\1, \2, \3]', text)
        path.write_text(text, encoding="utf-8")
        print(f"{path.name}: eased")
