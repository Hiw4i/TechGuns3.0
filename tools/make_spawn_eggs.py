"""Generate the armed-NPC spawn egg textures.

26.3 bake spawn egg colours into per-item textures (no tint layers), so each
mob gets its own 16x16 egg: the vanilla egg silhouette, shaded with the mob's
base colour and speckled deterministically (seed = mob id) in its accent.

Run from the repo root:  python tools/make_spawn_eggs.py
"""

import hashlib
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
ENTITY_DIR = ROOT / "src/main/resources/assets/techguns3/textures/item"

# Vanilla egg silhouette (any egg texture shares the same mask).
MASK_SOURCE = Path(__file__).resolve().parents[1] / "build/tmp/allay_spawn_egg.png"

# mob id -> (base RGB, spots RGB), echoing each mob's skin palette.
MOBS = {
    "zombie_soldier": ((70, 120, 60), (40, 60, 35)),
    "bandit": ((104, 78, 56), (60, 44, 32)),
    "army_soldier": ((74, 90, 48), (45, 55, 30)),
    "commando": ((58, 65, 80), (30, 34, 42)),
    "outcast": ((74, 48, 96), (45, 30, 60)),
    "zombie_policeman": ((42, 58, 90), (74, 122, 58)),
    "zombie_miner": ((192, 112, 24), (74, 122, 58)),
    "psycho_steve": ((138, 24, 21), (230, 60, 50)),
    "skeleton_soldier": ((216, 216, 200), (58, 58, 58)),
    "zombie_pigman_soldier": ((232, 160, 160), (138, 32, 32)),
    "super_mutant": ((168, 176, 32), (42, 74, 42)),
    "zombie_farmer": ((58, 106, 138), (74, 122, 58)),
}


def rng(seed: str):
    """Deterministic 0..255 stream from the mob id."""
    digest = hashlib.sha256(seed.encode()).digest()
    while True:
        for byte in digest:
            yield byte
        digest = hashlib.sha256(digest).digest()


def shade(color, factor):
    return tuple(max(0, min(255, int(round(c * factor)))) for c in color)


def main():
    mask = Image.open(MASK_SOURCE).convert("RGBA")
    assert mask.size == (16, 16), f"unexpected vanilla egg size {mask.size}"
    silhouette = [(x, y) for y in range(16) for x in range(16)
                  if mask.getpixel((x, y))[3] > 0]

    for mob, (base, spots) in MOBS.items():
        random = rng(mob)
        egg = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        inside = set(silhouette)
        painted_spots = set()

        # 5-7 speckles of 2-4 adjacent pixels each.
        for _ in range(5 + next(random) % 3):
            seed_pixel = silhouette[next(random) % len(silhouette)]
            cluster = {seed_pixel}
            for _ in range(2 + next(random) % 3):
                px, py = list(cluster)[next(random) % len(cluster)]
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    candidate = (px + dx, py + dy)
                    if candidate in inside:
                        cluster.add(candidate)
            painted_spots |= cluster

        for (x, y) in silhouette:
            # Edge darkening for volume: neighbours outside the silhouette.
            edge = any((x + dx, y + dy) not in inside
                       for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
            if (x, y) in painted_spots:
                color = shade(spots, 0.75 if edge else 1.0)
            else:
                color = shade(base, 0.72 if edge else (1.12 if y < 5 else 1.0))
            egg.putpixel((x, y), color + (255,))

        target = ENTITY_DIR / f"{mob}_spawn_egg.png"
        egg.save(target)
        print(f"wrote {target.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
