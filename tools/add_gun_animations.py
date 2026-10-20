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


def body_kick(length, pitch, back, axis="z"):
    """The whole weapon kicks up and back, then settles.

    ``axis`` is the bore axis of the model: ``z`` for the weapons whose barrel
    points along -Z, ``x`` for the ones whose barrel points along +X. The
    weapon is pushed against the bore, the same way the already ported guns
    do it.
    """
    if axis == "x":
        push = [-back, 0.1, 0]
    else:
        push = [0, 0.1, -back]
    return {
        "rotation": track([
            (0.0, [0, 0, 0], "easeoutcubic"),
            (round(length * 0.2, 3), [pitch, 0, 0], "easeoutquad"),
            (round(length * 0.5, 3), [round(pitch * 0.55, 3), 0, 0], "easeinoutcubic"),
            (round(length * 0.75, 3), [round(pitch * 0.2, 3), 0, 0], "easeinoutcubic"),
            (length, [0, 0, 0], "easeoutcubic"),
        ]),
        "position": track([
            (0.0, [0, 0, 0], "easeoutcubic"),
            (round(length * 0.2, 3), push, "easeoutquad"),
            (round(length * 0.5, 3), [v * 0.5 for v in push], "easeinoutcubic"),
            (length, [0, 0, 0], "easeoutcubic"),
        ]),
    }


def reload_pose(length, pitch=-20.0, roll=10.0, drop=0.35, axis="z"):
    """The weapon is tipped over, held, and brought back up.

    ``axis`` is the bore axis of the model, so the muzzle always tips down and
    the weapon always moves towards the shooter.
    """
    if axis == "x":
        rotation = [0.0, 0.0, -pitch]
        position = [0.0, -drop, 0.0]
    else:
        rotation = [pitch, 0.0, roll]
        position = [0.0, -drop, 0.0]
    return {
        "rotation": track([
            (0.0, [0, 0, 0], "easeinoutcubic"),
            (round(length * 0.18, 3), rotation, "easeinoutcubic"),
            (round(length * 0.82, 3), rotation, "easeinoutcubic"),
            (length, [0, 0, 0], "easeinoutcubic"),
        ]),
        "position": track([
            (0.0, [0, 0, 0], "easeinoutcubic"),
            (round(length * 0.5, 3), position, "easeinoutcubic"),
            (length, [0, 0, 0], "easeinoutcubic"),
        ]),
    }


ANIMATIONS = {
    # Revolver: heavy single shot, the cylinder is part of the body.
    "revolver": {
        "fire": {"animation_length": 0.3, "bones": {"gun": body_kick(0.3, 7.0, 1.2, "x")}},
        "reload": {"animation_length": 2.25, "bones": {"gun": reload_pose(2.25, -22.0, 12.0, 0.4)}},
    },
    # Thompson: hose, fast rounds.
    "thompson": {
        "fire": {"animation_length": 0.15, "bones": {"gun": body_kick(0.15, 3.0, 0.7, "x")}},
        "reload": {"animation_length": 2.0, "bones": {"gun": reload_pose(2.0, -20.0, 10.0, 0.35)}},
    },
    # M4: charging handle cycles after every shot, the magazine drops on reload.
    "m4": {
        "fire": {
            "animation_length": 0.15,
            "bones": {
                "gun": body_kick(0.15, 4.5, 0.8, "z"),
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
    # Bolt action: the bolt is cycled after the shot, the lever swings with it.
    "bolt_action": {
        "fire": {
            "animation_length": 1.25,
            "bones": {
                "gun": body_kick(1.25, 6.0, 1.0, "x"),
                "Bolt": {
                    "position": track([
                        (0.0, [0, 0, 0], "linear"),
                        (0.35, [-4.0, 0, 0], "easeinoutcubic"),
                        (0.75, [-4.0, 0, 0], "easeinoutcubic"),
                        (1.15, [0, 0, 0], "easeoutcubic"),
                        (1.25, [0, 0, 0], "linear"),
                    ]),
                },
                "Lever": {
                    "rotation": track([
                        (0.0, [0, 0, 0], "linear"),
                        (0.3, [-35.0, 0, 0], "easeinoutcubic"),
                        (0.75, [-35.0, 0, 0], "easeinoutcubic"),
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
                        (0.6, [-4.0, 0, 0], "easeinoutcubic"),
                        (1.9, [-4.0, 0, 0], "easeinoutcubic"),
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
