"""Adds the generic pose tracks to every gun animation.

The tracks that every weapon shares: the weapon is drawn, it is put away, it
can be looked over and it clicks when the magazine is empty. They all work on
the root bone of the model, so they fit every weapon without a per gun table.

The tracks that belong to a single weapon - the slide of a pistol, the pump of
a shotgun, the bolt of a rifle - stay in the tracks that are already there.

Run from the project root:  python tools/add_gun_poses.py
"""

import json
import re
from pathlib import Path

root = Path(__file__).resolve().parents[1]
anim_dir = root / "src/main/resources/assets/techguns3/geckolib/animations/item"

# The name of the root bone of the models.
ROOT_BONE = "gun"


def kf(vector, easing):
    return {"vector": vector, "easing": easing}


def track(keys):
    return {str(time): kf(vector, easing) for time, vector, easing in keys}


def pose(length, keys, rotation, position):
    """A body pose: the weapon is turned and moved, then held, then back."""
    return {
        "animation_length": length,
        "bones": {
            ROOT_BONE: {
                "rotation": track([(t, r, e) for t, r, e in rotation]),
                "position": track([(t, p, e) for t, p, e in position]),
            }
        },
    }


# The weapon is looked over: tilted towards the player, held, and back.
INSPECT = pose(
    2.6,
    None,
    [
        (0.0, [0, 0, 0], "easeinoutcubic"),
        (0.45, [-16, 38, 22], "easeinoutcubic"),
        (1.70, [-16, 38, 22], "easeinoutcubic"),
        (2.15, [0, 0, 0], "easeinoutcubic"),
        (2.60, [0, 0, 0], "easeinoutcubic"),
    ],
    [
        (0.0, [0, 0, 0], "easeinoutcubic"),
        (0.45, [1.5, -2.0, -2.0], "easeinoutcubic"),
        (1.70, [1.5, -2.0, -2.0], "easeinoutcubic"),
        (2.15, [0, 0, 0], "easeinoutcubic"),
        (2.60, [0, 0, 0], "easeinoutcubic"),
    ],
)

# The weapon comes up: it is drawn from below and snaps into the hand.
DRAW = pose(
    0.4,
    None,
    [
        (0.0, [58, 0, 0], "easeoutcubic"),
        (0.30, [-4, 0, 0], "easeoutcubic"),
        (0.40, [0, 0, 0], "linear"),
    ],
    [
        (0.0, [0, -9.0, 4.0], "easeoutcubic"),
        (0.30, [0, 0.4, -0.6], "easeoutcubic"),
        (0.40, [0, 0, 0], "linear"),
    ],
)

# The weapon is put away: the mirror image of the draw.
HOLSTER = None

# The trigger is pulled but there is nothing in the magazine: a dry click.
DRY_FIRE = pose(
    0.28,
    None,
    [
        (0.0, [0, 0, 0], "easeoutquad"),
        (0.05, [-5, 0, 0], "easeoutquad"),
        (0.16, [2, 0, 0], "easeoutcubic"),
        (0.28, [0, 0, 0], "linear"),
    ],
    [
        (0.0, [0, 0, 0], "easeoutquad"),
        (0.05, [0, 0, -0.4], "easeoutquad"),
        (0.16, [0, 0, 0.2], "easeoutcubic"),
        (0.28, [0, 0, 0], "linear"),
    ],
)

NEW = {
    "inspect": INSPECT,
    "draw": DRAW,
    "dryfire": DRY_FIRE,
}
NEW = {name: data for name, data in NEW.items() if data is not None}

for path in sorted(anim_dir.glob("*.animation.json")):
    data = json.loads(path.read_text(encoding="utf-8"))
    animations = data.setdefault("animations", {})
    changed = []
    for name, track_data in NEW.items():
        if name in animations:
            continue
        animations[name] = track_data
        changed.append(name)
    if changed:
        text = json.dumps(data, indent=2) + "\n"
        # Keep the vectors on one line, they are far easier to read that way.
        text = re.sub(r'"vector": \[\n\s*([-\d.]+),\n\s*([-\d.]+),\n\s*([-\d.]+)\n\s*\]',
                      r'"vector": [\1, \2, \3]', text)
        path.write_text(text, encoding="utf-8")
        print(f"{path.name}: {', '.join(changed)}")
