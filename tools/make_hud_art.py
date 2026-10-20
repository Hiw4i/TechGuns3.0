"""Generates the Techguns HUD panel texture (dev tool, run once, output is committed).

The panel is the frame of the ammo readout in the bottom right: a dark gun
metal plate with an orange accent, in the style of the Techguns 2 HUD.

Run from the project root:  python tools/make_hud_art.py
"""
from pathlib import Path

from PIL import Image, ImageDraw

root = Path(__file__).resolve().parents[1]
out = root / "src/main/resources/assets/techguns3/textures/gui/hud_panel.png"

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
