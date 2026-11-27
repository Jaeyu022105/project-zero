"""Original 16x16 textures for our nature blocks. Run from the repo root: python tools/textures/nature.py"""
import random
from PIL import Image

OUT = "src/main/resources/assets/projectzero/textures/block/"

def clamp(v): return max(0, min(255, int(v)))

def glowcap_gills():
    r = random.Random(7)
    im = Image.new("RGBA", (16, 16))
    for x in range(16):
        ridge = x % 3 == 1
        for y in range(16):
            base = (40, 150, 170) if not ridge else (120, 240, 235)
            n = r.randint(-18, 18)
            fade = 1.0 - abs(y - 7.5) / 22
            im.putpixel((x, y), (clamp(base[0] * fade + n), clamp(base[1] * fade + n), clamp(base[2] * fade + n), 255))
    for _ in range(10):
        im.putpixel((r.randrange(16), r.randrange(16)), (210, 255, 250, 255))
    im.save(OUT + "glowcap_gills.png")

def violet_cap():
    r = random.Random(11)
    im = Image.new("RGBA", (16, 16))
    for x in range(16):
        for y in range(16):
            n = r.randint(-14, 14)
            im.putpixel((x, y), (clamp(118 + n), clamp(62 + n // 2), clamp(170 + n), 255))
    spots = [(3, 3), (10, 2), (6, 8), (13, 9), (2, 12), (9, 13)]
    for sx, sy in spots:
        for dx, dy in [(0, 0), (1, 0), (0, 1), (1, 1)]:
            x, y = (sx + dx) % 16, (sy + dy) % 16
            im.putpixel((x, y), (225, 205, 245, 255) if (dx + dy) < 2 else (190, 160, 225, 255))
    im.save(OUT + "violet_cap.png")

if __name__ == "__main__":
    glowcap_gills(); violet_cap()
