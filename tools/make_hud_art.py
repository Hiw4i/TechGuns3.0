"""Generates the Techguns HUD textures (dev tool, run once, output is committed).

* textures/gui/hud_panel.png  - the frame of the ammo readout in the bottom
  right: a dark gun metal plate with an orange accent, in the style of the
  Techguns 2 HUD.
* textures/gui/scope.png      - the mask of a scope: a soft vignette that hides
  everything outside of the lens, with the reticle in the middle.

Run from the project root:  python tools/make_hud_art.py
"""
import math
from pathlib import Path

from PIL import Image, ImageDraw

root = Path(__file__).resolve().parents[1]
out = root / "src/main/resources/assets/techguns3/textures/gui/hud_panel.png"
scope_out = root / "src/main/resources/assets/techguns3/textures/gui/scope.png"

W, H = 148, 46
PLATE = (26, 27, 31, 214)
EDGE = (74, 78, 86, 255)
EDGE_DARK = (12, 12, 14, 255)
ACCENT = (232, 138, 42, 255)
ACCENT_DIM = (150, 84, 26, 255)
SCREW = (96, 100, 108, 255)

img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
d = ImageDraw.Draw(img)

# Plate with a soft vertical gradient.
for y in range(H):
    k = y / (H - 1)
    shade = int(PLATE[0] * (1.0 - 0.35 * k))
    shade_g = int(PLATE[1] * (1.0 - 0.35 * k))
    shade_b = int(PLATE[2] * (1.0 - 0.35 * k))
    d.line([(0, y), (W, y)], fill=(shade, shade_g, shade_b, PLATE[3]))

# Frame.
d.rectangle([0, 0, W - 1, H - 1], outline=EDGE)
d.rectangle([1, 1, W - 2, H - 2], outline=EDGE_DARK)

# Accent bar on the left, with a brighter top edge.
d.rectangle([3, 4, 5, H - 5], fill=ACCENT)
d.rectangle([3, 4, 5, 12], fill=ACCENT_DIM)

# Corner screws.
for x, y in ((W - 6, 5), (W - 6, H - 6), (7, H - 6)):
    d.point((x, y), fill=SCREW)
    d.point((x - 1, y), fill=EDGE_DARK)

# Separator line above the reload bar.
d.line([(10, H - 11), (W - 6, H - 11)], fill=EDGE_DARK)
d.line([(10, H - 10), (W - 6, H - 10)], fill=(58, 60, 66, 255))

img.save(out)
print(f"wrote {out} ({W}x{H})")


# --- the scope mask -----------------------------------------------------------
# The lens is a circle in the middle of the image. Everything outside of it is
# covered by the opaque vignette, so the weapon and the world disappear and
# only the view through the lens is left.

S = 256
C = S / 2.0
RETICLE = (18, 20, 24, 255)

scope = Image.new("RGBA", (S, S), (0, 0, 0, 0))
d = ImageDraw.Draw(scope)

# The vignette: opaque at the rim, gone before the middle of the lens.
for i in range(int(C), 0, -1):
    t = i / C
    # Fully opaque from the rim, fading out over the last third.
    k = min(1.0, max(0.0, (t - 0.62) / 0.38))
    alpha = int(255 * (k ** 1.6))
    d.ellipse([C - i, C - i, C + i, C + i], fill=(4, 5, 7, alpha))

# A thin bright ring where the lens ends.
d.ellipse([C - C * 0.995, C - C * 0.995, C + C * 0.995, C + C * 0.995],
          outline=(120, 128, 140, 150), width=2)

# The reticle: a cross with a gap in the middle and small hash marks.
gap = 14
length = 74
width = 1
for sign in (-1, 1):
    d.line([(C + sign * gap, C), (C + sign * length, C)], fill=RETICLE, width=width)
    d.line([(C, C + sign * gap), (C, C + sign * length)], fill=RETICLE, width=width)
# Hash marks on the vertical line.
for i, dist in enumerate((30, 46, 62)):
    w = 8 if i % 2 == 0 else 5
    d.line([(C - w, C + dist), (C + w, C + dist)], fill=RETICLE, width=width)
    d.line([(C - w, C - dist), (C + w, C - dist)], fill=RETICLE, width=width)
# A small dot in the middle.
d.ellipse([C - 1, C - 1, C + 1, C + 1], fill=RETICLE)

scope.save(scope_out)
print(f"wrote {scope_out} ({S}x{S})")
