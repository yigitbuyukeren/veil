#!/usr/bin/env python3
"""Generates Veil's own raster brand assets (original artwork, no upstream assets). Run from the repo root: python3 -I scripts/branding/generate_assets.py

Palette "Tül": night #0B0F14, accent gradient #7CF5D3 -> #8A7CFF -> #FF8FB1 (diagonal, top-left to bottom-right), text #EEF2F6.
Optional: set VEIL_WORDMARK_FONT to a Bricolage Grotesque file (Bold instance is used) for the wordmark text.
"""
from PIL import Image, ImageDraw, ImageFont
import os
R = "apps/multiplatform/"
NIGHT = (0x0B, 0x0F, 0x14, 255)
TEXT = (0xEE, 0xF2, 0xF6, 255)
GRAD_STOPS = [(0.0, (0x7C, 0xF5, 0xD3)), (0.5, (0x8A, 0x7C, 0xFF)), (1.0, (0xFF, 0x8F, 0xB1))]
WHITE_60 = (255, 255, 255, 153)  # same as #99FFFFFF in the launcher vector
BODY = [(30,34),(42,34),(54,66),(66,34),(78,34),(60,80),(48,80)]
STROKE = [(36,28),(54,38),(72,28),(72,31),(54,41),(36,31)]
def vmark(d, ox, oy, s, body=None, stroke=None):
    # "V" as draped cloth, same geometry as drawable/veil_icon_foreground.xml (108-unit box)
    k = s / 108.0
    P = lambda pts: [(ox + x * k, oy + y * k) for x, y in pts]
    if body is not None:
        d.polygon(P(BODY), fill=body)
    if stroke is not None:
        d.polygon(P(STROKE), fill=stroke)
def grad_color(t):
    t = min(1.0, max(0.0, t))
    for (t0, c0), (t1, c1) in zip(GRAD_STOPS, GRAD_STOPS[1:]):
        if t <= t1:
            u = (t - t0) / (t1 - t0)
            return tuple(round(a + (b - a) * u) for a, b in zip(c0, c1))
    return GRAD_STOPS[-1][1]
def gradient(size, x0, y0, dx, dy):
    # linear gradient along the vector (x0,y0)->(x0+dx,y0+dy): same mapping as the Android <gradient> on the launcher vector
    w, h = size
    im = Image.new("RGBA", size); px = im.load()
    L = dx * dx + dy * dy
    for y in range(h):
        for x in range(w):
            t = ((x + 0.5 - x0) * dx + (y + 0.5 - y0) * dy) / L
            px[x, y] = grad_color(t) + (255,)
    return im
def mark_layer(size, ox, oy, s):
    # gradient-filled V body, white 60% drape stroke on top
    k = s / 108.0
    body = Image.new("L", size, 0)
    vmark(ImageDraw.Draw(body), ox, oy, s, body=255)
    layer = gradient(size, ox + 30 * k, oy + 28 * k, 48 * k, 52 * k)
    layer.putalpha(body)
    stroke = Image.new("RGBA", size, (0, 0, 0, 0))
    vmark(ImageDraw.Draw(stroke), ox, oy, s, stroke=WHITE_60)
    return Image.alpha_composite(layer, stroke)
def font(size):
    env = os.environ.get("VEIL_WORDMARK_FONT")
    if env and os.path.exists(env):
        f = ImageFont.truetype(env, size)
        try:
            f.set_variation_by_name("Bold")
        except (OSError, ValueError):
            pass
        return f
    for p in ("/System/Library/Fonts/Helvetica.ttc", "/System/Library/Fonts/SFNS.ttf", "/Library/Fonts/Arial.ttf"):
        if os.path.exists(p):
            return ImageFont.truetype(p, size)
    return ImageFont.load_default()
def wordmark(path, size, color):
    w, h = size
    im = mark_layer(size, 0, -h * 0.08, h * 1.1)
    d = ImageDraw.Draw(im)
    f = font(int(h * 0.8)); d.text((h * 1.0, h * 0.08), "Veil", font=f, fill=color)
    im.save(path)
def square(path, n, body_mode, bg=None):
    if body_mode == "gradient":
        im = mark_layer((n, n), 0, 0, n)
        if bg is not None:
            im = Image.alpha_composite(Image.new("RGBA", (n, n), bg), im)
    else:
        im = Image.new("RGBA", (n, n), bg or (0, 0, 0, 0))
        vmark(ImageDraw.Draw(im), 0, 0, n, body=(255, 255, 255, 255), stroke=WHITE_60)
    im.save(path)
wordmark(R + "common/src/commonMain/resources/MR/images/logo@4x.png", (768, 189), NIGHT)
wordmark(R + "common/src/commonMain/resources/MR/images/logo_light@4x.png", (768, 190), TEXT)
wordmark(R + "android/src/main/res/drawable/logo.png", (768, 189), NIGHT)
wordmark(R + "android/src/main/res/drawable/logo_light.png", (768, 190), TEXT)
square(R + "common/src/commonMain/resources/MR/images/ic_simplex_dark@4x.png", 180, "gradient")
square(R + "common/src/commonMain/resources/MR/images/ic_simplex_light@4x.png", 300, "gradient")
square(R + "common/src/commonMain/resources/MR/images/icon_foreground_common@4x.png", 432, "gradient")
square(R + "android/src/main/res/drawable-hdpi/icon.png", 784, "gradient")
for d, n in (("mdpi", 24), ("hdpi", 36), ("xhdpi", 48), ("xxhdpi", 72), ("xxxhdpi", 96)):
    for name in ("ntf_icon.png", "ntf_service_icon.png"):
        square(R + f"android/src/main/res/drawable-{d}/{name}", n, "white")
print("assets written")
