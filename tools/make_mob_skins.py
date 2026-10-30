"""Generate the 10 new armed-NPC skins as palette remaps of bandit.png.

The bandit base is a clean 70-colour 64x64 biped skin: greys = uniform/cloth,
browns = skin/leather, near-blacks = outline/boots/hair. Every new mob is the
same geometry with the three material classes re-tinted (shading preserved by
scaling each pixel's original luminance), which keeps the family look of the
CE textures without shipping their assets.

Run from the repo root:  python tools/make_mob_skins.py
"""

from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
ENTITY_DIR = ROOT / "src/main/resources/assets/techguns3/textures/entity"
BASE = ENTITY_DIR / "bandit.png"

# Classification thresholds on the bandit palette.
GREY_MAX_HUE_DELTA = 14   # |r-g|,|g-b|,|r-b| below this = neutral (cloth)
SKIN_MIN_EXCESS = 18      # r-g above this on a non-dark pixel = skin/leather
DARK_LUMA = 60            # below = outline/boots/hair

# Per-mob tints: (cloth RGB, skin RGB, dark RGB).
VARIANTS = {
    "army_soldier": ((74, 90, 48), (128, 102, 87), (20, 22, 14)),      # olive, tan
    "commando": ((58, 65, 80), (128, 102, 87), (16, 18, 24)),          # slate blue
    "outcast": ((74, 48, 96), (160, 158, 168), (18, 12, 26)),          # purple, pale
    "zombie_policeman": ((42, 58, 90), (74, 122, 58), (12, 16, 26)),   # navy, zombie green
    "zombie_miner": ((192, 112, 24), (74, 122, 58), (30, 20, 8)),      # hi-vis orange
    "psycho_steve": ((138, 24, 21), (128, 102, 87), (22, 8, 8)),       # blood red
    "skeleton_soldier": ((58, 58, 58), (216, 216, 200), (12, 12, 12)), # charcoal, bone
    "zombie_pigman_soldier": ((138, 32, 32), (232, 160, 160), (24, 10, 10)),  # crimson, pink
    "super_mutant": ((42, 74, 42), (168, 176, 32), (10, 20, 10)),      # green armour, mutant
    "zombie_farmer": ((58, 106, 138), (74, 122, 58), (16, 24, 32)),    # denim, zombie green
}

# Bandit palette anchors used to normalise luminance per class.
CLOTH_ANCHOR = 174.0   # dominant uniform grey
SKIN_ANCHOR = 110.0    # dominant skin brown luminance
DARK_ANCHOR = 18.0


def luminance(rgb):
    return 0.299 * rgb[0] + 0.587 * rgb[1] + 0.114 * rgb[2]


def classify(rgb):
    r, g, b, a = rgb
    if a == 0:
        return None
    if luminance(rgb) < DARK_LUMA:
        return "dark"
    if abs(r - g) < GREY_MAX_HUE_DELTA and abs(g - b) < GREY_MAX_HUE_DELTA:
        return "cloth"
    if r - g > SKIN_MIN_EXCESS:
        return "skin"
    return "cloth"


def shade(target, lum, anchor):
    factor = max(0.35, min(1.35, lum / anchor))
    return tuple(max(0, min(255, int(round(c * factor)))) for c in target) + (255,)


def remap(base, cloth, skin, dark):
    out = Image.new("RGBA", base.size)
    src = base.load()
    dst = out.load()
    for y in range(base.height):
        for x in range(base.width):
            px = src[x, y]
            kind = classify(px)
            if kind is None:
                dst[x, y] = px
                continue
            lum = luminance(px)
            if kind == "cloth":
                dst[x, y] = shade(cloth, lum, CLOTH_ANCHOR)
            elif kind == "skin":
                dst[x, y] = shade(skin, lum, SKIN_ANCHOR)
            else:
                dst[x, y] = shade(dark, lum, DARK_ANCHOR)
    return out


def main():
    base = Image.open(BASE).convert("RGBA")
    for name, (cloth, skin, dark) in VARIANTS.items():
        skin_img = remap(base, cloth, skin, dark)
        target = ENTITY_DIR / f"{name}.png"
        skin_img.save(target)
        print(f"wrote {target.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
