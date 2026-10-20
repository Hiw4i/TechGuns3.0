"""Writes the fire and reload animation tracks of the newly ported guns.

The tracks follow the ones of the already ported weapons: the body kick on
the "gun" bone plus the moving part of the weapon (the bolt of the bolt
action, the charging handle and the magazine of the M4).

Run from the project root:  python tools/add_gun_animations.py
"""

import json
from pathlib import Path

root = Path(__file__).resolve().parents[1]
anim_dir = root / "src/main/resources/assets/techguns3/geckolib/animations/item"


def kf(vector, easing):
    return {"vector": vector, "easing": easing}


def track(keys):
    return {str(time): kf(vector, easing) for time, vector, easing in keys}


def body_kick(length, pitch, back, ease="easeoutcubic"):
    """The whole weapon kicks up and back, then settles."""
    return {
        "rotation": track([
            (0.0, [0, 0, 0], ease),
            (round(length * 0.2, 3), [pitch, 0, 0], "easeoutquad"),
            (round(length * 0.5, 3), [round(pitch * 0.55, 3), 0, 0], "easeinoutcubic"),
            (round(length * 0.75, 3), [round(pitch * 0.2, 3), 0, 0], "easeinoutcubic"),
            (length, [0, 0, 0], "easeoutcubic"),
        ]),
        "position": track([
            (0.0, [0, 0, 0], ease),
            (round(length * 0.2, 3), [0, 0.1, -back], "easeoutquad"),
            (round(length * 0.5, 3), [0, 0.05, -round(back * 0.5, 3)], "easeinoutcubic"),
            (length, [0, 0, 0], "easeoutcubic"),
        ]),
    }


def reload_pose(length, pitch=-20.0, roll=10.0, drop=0.35):
    """The weapon is tipped over, held, and brought back up."""
    return {
        "rotation": track([
            (0.0, [0, 0, 0], "easeinoutcubic"),
            (round(length * 0.18, 3), [pitch, 0, roll], "easeinoutcubic"),
            (round(length * 0.82, 3), [pitch, 0, roll], "easeinoutcubic"),
            (length, [0, 0, 0], "easeinoutcubic"),
        ]),
        "position": track([
            (0.0, [0, 0, 0], "easeinoutcubic"),
            (round(length * 0.5, 3), [0, -drop, drop], "easeinoutcubic"),
            (length, [0, 0, 0], "easeinoutcubic"),
        ]),
    }


ANIMATIONS = {
    # Revolver: heavy single shot, the cylinder is part of the body.
    "revolver": {
        "fire": {"animation_length": 0.3, "bones": {"gun": body_kick(0.3, 7.0, 0.9)}},
        "reload": {"animation_length": 2.25, "bones": {"gun": reload_pose(2.25, -22.0, 12.0, 0.4)}},
    },
    # Thompson: hose, fast rounds.
    "thompson": {
        "fire": {"animation_length": 0.15, "bones": {"gun": body_kick(0.15, 3.0, 0.45)}},
        "reload": {"animation_length": 2.0, "bones": {"gun": reload_pose(2.0, -20.0, 10.0, 0.35)}},
    },
    # M4: charging handle cycles after every shot, the magazine drops on reload.
    "m4": {
        "fire": {
            "animation_length": 0.15,
            "bones": {
                "gun": body_kick(0.15, 4.5, 0.6),
                "Bolt03": {
                    "position": track([
                        (0.0, [0, 0, 0], "linear"),
                        (0.05, [0, 0, 1.2], "easeoutquad"),
                        (0.09, [0, 0, 1.2], "linear"),
                        (0.14, [0, 0, 0], "easeoutcubic"),
                    ]),
                },
            },
        },
        "reload": {
            "animation_length": 2.25,
            "bones": {
                "gun": reload_pose(2.25, -20.0, 10.0, 0.35),
                "Magazine02": {
                    "position": track([
                        (0.0, [0, 0, 0], "linear"),
                        (0.5, [0, -4, 0], "linear"),
                        (1.3, [0, -4, 0], "linear"),
                        (1.5, [0, 0, 0], "linear"),
                        (2.25, [0, 0, 0], "linear"),
                    ]),
                },
                "Bolt03": {
                    "position": track([
                        (0.0, [0, 0, 0], "linear"),
                        (1.2, [0, 0, 1.2], "easeoutquad"),
                        (1.5, [0, 0, 1.2], "linear"),
                        (1.7, [0, 0, 0], "easeoutcubic"),
                        (2.25, [0, 0, 0], "linear"),
                    ]),
                },
            },
        },
    },
    # Bolt action: the bolt is cycled after the shot.
    "bolt_action": {
        "fire": {
            "animation_length": 1.25,
            "bones": {
                "gun": body_kick(1.25, 6.0, 0.8),
                "Bolt": {
                    "position": track([
                        (0.0, [0, 0, 0], "linear"),
                        (0.35, [0, 0, 4.0], "easeinoutcubic"),
                        (0.75, [0, 0, 4.0], "easeinoutcubic"),
                        (1.15, [0, 0, 0], "easeoutcubic"),
                        (1.25, [0, 0, 0], "linear"),
                    ]),
                },
                "Lever": {
                    "rotation": track([
                        (0.0, [0, 0, 0], "linear"),
                        (0.3, [0, 0, 35.0], "easeinoutcubic"),
                        (0.75, [0, 0, 35.0], "easeinoutcubic"),
                        (1.15, [0, 0, 0], "easeoutcubic"),
                        (1.25, [0, 0, 0], "linear"),
                    ]),
                },
            },
        },
        "reload": {
            "animation_length": 2.5,
            "bones": {
                "gun": reload_pose(2.5, -24.0, 12.0, 0.4),
                "Bolt": {
                    "position": track([
                        (0.0, [0, 0, 0], "linear"),
                        (0.6, [0, 0, 4.0], "easeinoutcubic"),
                        (1.9, [0, 0, 4.0], "easeinoutcubic"),
                        (2.2, [0, 0, 0], "easeoutcubic"),
                        (2.5, [0, 0, 0], "linear"),
                    ]),
                },
            },
        },
    },
}

for gun, animations in ANIMATIONS.items():
    data = {"format_version": "1.8.0", "animations": animations}
    path = anim_dir / f"{gun}.animation.json"
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")
    print(f"{gun}: {', '.join(animations)}")
