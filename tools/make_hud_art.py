"""Generates the Techguns HUD textures (dev tool, run once, output is committed).

* textures/gui/hud_panel.png  - the frame of the ammo readout in the bottom
  right: a dark gun metal plate with an orange accent, in the style of the
  Techguns 2 HUD.
* textures/gui/scope.png      - the mask of a scope: the glass of the lens with
  a soft vignette that hides everything outside of it, the blue coating of the
  lens edge and a mil dot reticle in the middle.

Run from the project root:  python tools/make_hud_art.py
"""
import math
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

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
# The glass of the lens fills the circle in the middle of the image, everything
# outside of it is the dark wall of the tube. The vignette is soft, the edge of
# the glass carries the blue coating of a real lens and the reticle is a mil
# dot with the range numbers next to it.

S = 1024
C = S / 2.0
# The wall of the tube. It has to be the same colour the overlay paints
# outside the mask, or the edge of the quad shows up as a square.
TUBE = (5, 6, 8, 255)
RETICLE = (12, 14, 18, 240)       # the shadowed reticle of a real scope
RETICLE_HALO = (196, 202, 214, 70)  # the faint edge that keeps it readable at night

# The wall of the tube fills the whole image, the lens is cut out of it.
scope = Image.new("RGBA", (S, S), TUBE)
d = ImageDraw.Draw(scope)

# The vignette: the glass is cut out of the tube. The ellipses are drawn from
# the outside in, so the last one that touches a pixel decides its alpha: the
# middle of the lens ends up clear, the rim is solid.
for i in range(int(C), 0, -1):
    t = i / C
    # A smooth falloff from 0.84 to 0.99 of the radius.
    k = min(1.0, max(0.0, (t - 0.84) / 0.15))
    k = k * k * (3.0 - 2.0 * k)
    d.ellipse([C - i, C - i, C + i, C + i], fill=(5, 6, 8, int(255 * k)))

# The glass: a very faint cool tint over the lens.
glass = Image.new("RGBA", (S, S), (0, 0, 0, 0))
gd = ImageDraw.Draw(glass)
for i in range(int(C), 0, -1):
    t = i / C
    # The tint is the strongest at the edge of the glass, the middle is clear.
    alpha = int(9 * (t ** 2))
    gd.ellipse([C - i, C - i, C + i, C + i], fill=(104, 136, 178, alpha))
scope = Image.alpha_composite(scope, glass)
d = ImageDraw.Draw(scope)

# The blue coating of the lens edge. It is a reflection on the glass, so it
# sits on top of the vignette.
coating = Image.new("RGBA", (S, S), (0, 0, 0, 0))
cd = ImageDraw.Draw(coating)
for i in range(int(C), 0, -1):
    t = i / C
    band = math.exp(-((t - 0.875) / 0.026) ** 2)
    alpha = int(38 * band)
    cd.ellipse([C - i, C - i, C + i, C + i], fill=(138, 148, 226, alpha))
scope = Image.alpha_composite(scope, coating)
d = ImageDraw.Draw(scope)

# The rim of the eyepiece: a thin bright ring where the glass ends.
d.ellipse([C - C * 0.965, C - C * 0.965, C + C * 0.965, C + C * 0.965],
          outline=(132, 140, 154, 130), width=3)

# The reticle: a mil dot. A post below the centre with a dot for every mil and
# the range number next to it, a cross with a gap in the middle and a fine
# horizontal line.
post = 0.60 * C
d.line([(C, C), (C, C + post)], fill=RETICLE_HALO, width=6)
d.line([(C, C), (C, C + post)], fill=RETICLE, width=3)

# The horizontal line with a gap in the middle.
gap = 0.035 * C
arm = 0.34 * C
for sign in (-1, 1):
    d.line([(C + sign * gap, C), (C + sign * arm, C)], fill=RETICLE_HALO, width=6)
    d.line([(C + sign * gap, C), (C + sign * arm, C)], fill=RETICLE, width=3)
    d.line([(C, C + sign * gap), (C, C + sign * arm * 0.55)], fill=RETICLE_HALO, width=6)
    d.line([(C, C + sign * gap), (C, C + sign * arm * 0.55)], fill=RETICLE, width=3)

# The dot in the middle.
d.ellipse([C - 5, C - 5, C + 5, C + 5], fill=RETICLE_HALO)
d.ellipse([C - 3, C - 3, C + 3, C + 3], fill=RETICLE)

# The mil dots and their numbers.
try:
    font = ImageFont.load_default(size=22)
except TypeError:
    font = ImageFont.load_default()

mil = 0.115 * C
for number in range(1, 6):
    y = C + mil * number
    d.ellipse([C - 6, y - 6, C + 6, y + 6], fill=RETICLE_HALO)
    d.ellipse([C - 4, y - 4, C + 4, y + 4], fill=RETICLE)
    d.text((C + 15, y - 11), str(number), font=font, fill=RETICLE)

scope.save(scope_out)
print(f"wrote {scope_out} ({S}x{S})")
