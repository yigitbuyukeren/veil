#!/usr/bin/env python3
"""Generates Veil's own raster brand assets (original artwork, no upstream assets). Run from the repo root: python3 -I scripts/branding/generate_assets.py"""
from PIL import Image, ImageDraw, ImageFont
import os
R = "apps/multiplatform/"
def vmark(d, ox, oy, s, fill):
    # "V" as draped cloth, same geometry as drawable/veil_icon_foreground.xml (108-unit box)
    k = s / 108.0
    P = lambda pts: [(ox + x * k, oy + y * k) for x, y in pts]
    d.polygon(P([(30,34),(42,34),(54,66),(66,34),(78,34),(60,80),(48,80)]), fill=fill)
    d.polygon(P([(36,28),(54,38),(72,28),(72,31),(54,41),(36,31)]), fill=fill[:3] + (153,))
def font(size):
    for p in ("/System/Library/Fonts/Helvetica.ttc", "/System/Library/Fonts/SFNS.ttf", "/Library/Fonts/Arial.ttf"):
        if os.path.exists(p):
            return ImageFont.truetype(p, size)
    return ImageFont.load_default()
def wordmark(path, size, color):
    w, h = size
    im = Image.new("RGBA", size, (0, 0, 0, 0)); d = ImageDraw.Draw(im)
    vmark(d, 0, -h * 0.08, h * 1.1, color)
    f = font(int(h * 0.8)); d.text((h * 1.0, h * 0.08), "Veil", font=f, fill=color)
    im.save(path)
def square(path, n, color, bg=None):
    im = Image.new("RGBA", (n, n), bg or (0, 0, 0, 0)); d = ImageDraw.Draw(im)
    vmark(d, 0, 0, n, color); im.save(path)
dark, light = (31, 58, 77, 255), (255, 255, 255, 255)
wordmark(R + "common/src/commonMain/resources/MR/images/logo@4x.png", (768, 189), dark)
wordmark(R + "common/src/commonMain/resources/MR/images/logo_light@4x.png", (768, 190), light)
wordmark(R + "android/src/main/res/drawable/logo.png", (768, 189), dark)
wordmark(R + "android/src/main/res/drawable/logo_light.png", (768, 190), light)
square(R + "common/src/commonMain/resources/MR/images/ic_simplex_dark@4x.png", 180, dark)
square(R + "common/src/commonMain/resources/MR/images/ic_simplex_light@4x.png", 300, light)
square(R + "common/src/commonMain/resources/MR/images/icon_foreground_common@4x.png", 432, dark)
square(R + "android/src/main/res/drawable-hdpi/icon.png", 784, dark)
for d, n in (("mdpi", 24), ("hdpi", 36), ("xhdpi", 48), ("xxhdpi", 72), ("xxxhdpi", 96)):
    for name in ("ntf_icon.png", "ntf_service_icon.png"):
        square(R + f"android/src/main/res/drawable-{d}/{name}", n, light)
print("assets written")
