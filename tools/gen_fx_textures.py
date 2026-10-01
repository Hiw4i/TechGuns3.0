"""Procedural TG-style FX textures (reproducible asset pipeline, stdlib only).

Generates:
- textures/particle/glow_dot.png  16x16 soft radial spark dot (white, tinted by particle color)
- textures/particle/puff.png      32x32 soft blotchy smoke (white, tinted by particle color)
- textures/particle/flame.png     32x32 teardrop fire blob (white core, tinted by particle color)
- textures/fx/muzzle_flash.png    64x64 baked star (white core + spikes, tinted by flash color)

All textures are white-with-alpha so vertex/particle color provides the hue.
Run:  python tools/gen_fx_textures.py
"""
import math
import os
import struct
import zlib

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources",
                    "assets", "techguns3", "textures")


def write_png(path, w, h, pixels):
    """pixels: list of rows of (r, g, b, a) 0-255 tuples."""
    raw = bytearray()
    for row in pixels:
        raw.append(0)
        for r, g, b, a in row:
            raw += bytes((r, g, b, a))

    def chunk(typ, data):
        out = struct.pack(">I", len(data)) + typ + data
        return out + struct.pack(">I", zlib.crc32(typ + data) & 0xFFFFFFFF)

    png = b"\x89PNG\r\n\x1a\n"
    png += chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(bytes(raw), 9))
    png += chunk(b"IEND", b"")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as f:
        f.write(png)
    print("wrote", path)


def hash_noise(x, y, seed=0):
    h = (x * 374761393 + y * 668265263 + seed * 974634211) & 0xFFFFFFFF
    h = (h ^ (h >> 13)) * 1274126177 & 0xFFFFFFFF
    h ^= h >> 16
    return (h & 0xFFFFFF) / float(0x1000000)


def smooth_noise(x, y):
    xi, yi = int(math.floor(x)), int(math.floor(y))
    xf, yf = x - xi, y - yi
    sx, sy = xf * xf * (3 - 2 * xf), yf * yf * (3 - 2 * yf)
    a = hash_noise(xi, yi)
    b = hash_noise(xi + 1, yi)
    c = hash_noise(xi, yi + 1)
    d = hash_noise(xi + 1, yi + 1)
    return a + (b - a) * sx + (c - a) * sy + (a - b - c + d) * sx * sy


def glow_dot(size=16):
    px = []
    for y in range(size):
        row = []
        for x in range(size):
            dx = (x + 0.5) / size * 2 - 1
            dy = (y + 0.5) / size * 2 - 1
            d = math.sqrt(dx * dx + dy * dy)
            a = max(0.0, 1.0 - d) ** 1.6
            v = 255
            row.append((v, v, v, int(a * 255)))
        px.append(row)
    return px


def puff(size=32):
    px = []
    for y in range(size):
        row = []
        for x in range(size):
            nx = (x + 0.5) / size * 2 - 1
            ny = (y + 0.5) / size * 2 - 1
            d = math.sqrt(nx * nx + ny * ny)
            base = max(0.0, 1.0 - d) ** 1.4
            n = 0.6 * smooth_noise(x / size * 4, y / size * 4) + 0.4 * smooth_noise(x / size * 9 + 7, y / size * 9)
            a = base * (0.45 + 0.55 * n)
            shade = int(225 + 30 * n)
            row.append((shade, shade, shade, int(max(0.0, min(1.0, a)) * 255)))
        px.append(row)
    return px


def flame(size=32):
    px = []
    for y in range(size):
        row = []
        for x in range(size):
            nx = (x + 0.5) / size * 2 - 1
            ny = (y + 0.5) / size * 2 - 1
            # teardrop: wider hot base, tapering tip; core sits below center
            taper = 1.0 - 0.55 * max(0.0, -ny)
            dx = nx / max(0.25, taper)
            dy = (ny - 0.18) / 1.05
            d = math.sqrt(dx * dx + dy * dy)
            flicker = 0.85 + 0.3 * smooth_noise(x / size * 5, y / size * 5 + 3)
            a = max(0.0, 1.0 - d * flicker) ** 1.3
            heat = max(0.0, 1.0 - d * 1.6)
            v = int(200 + 55 * heat)
            row.append((v, int(v * 0.96), int(v * 0.9), int(a * 255)))
        px.append(row)
    return px


def muzzle_flash(size=64):
    px = []
    c = size / 2
    for y in range(size):
        row = []
        for x in range(size):
            dx = (x + 0.5 - c) / c
            dy = (y + 0.5 - c) / c
            ang = math.atan2(dy, dx)
            dist = math.sqrt(dx * dx + dy * dy)
            # core disc
            core = max(0.0, 1.0 - dist / 0.34)
            # spikes: long horizontal, medium vertical, short diagonals
            spike_h = max(0.0, 1.0 - abs(dy) / 0.13) * max(0.0, 1.0 - abs(dx) / 1.0)
            spike_v = max(0.0, 1.0 - abs(dx) / 0.15) * max(0.0, 1.0 - abs(dy) / 0.8)
            diag = abs(abs(ang) - math.pi / 4)
            diag = min(diag, abs(abs(ang) - 3 * math.pi / 4))
            spike_d = max(0.0, 1.0 - diag / 0.16) * max(0.0, 1.0 - dist / 0.62)
            spikes = max(spike_h, spike_v * 0.9, spike_d * 0.75)
            # soft halo binding it together
            halo = max(0.0, 1.0 - dist / 0.85) ** 2 * 0.45
            a = max(min(1.0, core), min(1.0, spikes), halo)
            a = a ** 1.15
            brightness = 255 if core > 0.55 else 245
            row.append((brightness, brightness, brightness, int(a * 255)))
        px.append(row)
    return px


write_png(os.path.join(ROOT, "particle", "glow_dot.png"), 16, 16, glow_dot())
write_png(os.path.join(ROOT, "particle", "puff.png"), 32, 32, puff())
write_png(os.path.join(ROOT, "particle", "flame.png"), 32, 32, flame())
write_png(os.path.join(ROOT, "fx", "muzzle_flash.png"), 64, 64, muzzle_flash())
