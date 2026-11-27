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

def skywood_log():
    """Pale silver bark with soft sky-blue streaks running up it."""
    r = random.Random(21)
    im = Image.new("RGBA", (16, 16))
    streak = [r.random() < 0.3 for _ in range(16)]
    for x in range(16):
        for y in range(16):
            n = r.randint(-10, 10)
            c = (214, 222, 230)
            if streak[x]: c = (150, 190, 228)
            if (x + y * 3) % 11 == 0: c = (176, 186, 200)
            im.putpixel((x, y), (clamp(c[0] + n), clamp(c[1] + n), clamp(c[2] + n), 255))
    for _ in range(4):
        x, y = r.randrange(16), r.randrange(15)
        im.putpixel((x, y), (120, 130, 150, 255)); im.putpixel((x, y + 1), (140, 150, 168, 255))
    im.save(OUT + "skywood_log.png")

def skywood_log_top():
    r = random.Random(22)
    im = Image.new("RGBA", (16, 16))
    for x in range(16):
        for y in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            n = r.randint(-6, 6)
            if d > 7: c = (200, 210, 222)
            elif int(d) % 2 == 0: c = (198, 220, 236)
            else: c = (170, 200, 226)
            im.putpixel((x, y), (clamp(c[0] + n), clamp(c[1] + n), clamp(c[2] + n), 255))
    im.save(OUT + "skywood_log_top.png")

def skywood_planks():
    r = random.Random(23)
    im = Image.new("RGBA", (16, 16))
    for y in range(16):
        row = y // 4
        for x in range(16):
            n = r.randint(-8, 8)
            c = (186, 208, 226) if row % 2 == 0 else (176, 200, 220)
            if y % 4 == 3: c = (138, 160, 184)
            if (x + row * 5) % 16 == 0 and y % 4 != 3: c = (150, 172, 196)
            im.putpixel((x, y), (clamp(c[0] + n), clamp(c[1] + n), clamp(c[2] + n), 255))
    im.save(OUT + "skywood_planks.png")

def skywood_leaves():
    """Pale teal leaves with white-blue blossoms and gaps (cutout)."""
    r = random.Random(24)
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for x in range(16):
        for y in range(16):
            if r.random() < 0.2: continue
            n = r.randint(-16, 16)
            c = (120, 196, 196) if r.random() < 0.6 else (150, 214, 220)
            im.putpixel((x, y), (clamp(c[0] + n), clamp(c[1] + n), clamp(c[2] + n), 255))
    for _ in range(9):
        x, y = r.randrange(16), r.randrange(16)
        im.putpixel((x, y), (236, 246, 255, 255))
    im.save(OUT + "skywood_leaves.png")

def limestone():
    """Pale grey-cream stone with faint vertical dissolution streaks and pits."""
    r = random.Random(31)
    im = Image.new("RGBA", (16, 16))
    for x in range(16):
        streak = r.randint(-12, 4)
        for y in range(16):
            n = r.randint(-9, 9) + streak
            im.putpixel((x, y), (clamp(196 + n), clamp(192 + n), clamp(178 + n), 255))
    for _ in range(7):
        x, y = r.randrange(16), r.randrange(16)
        im.putpixel((x, y), (140, 136, 124, 255))
    im.save(OUT + "limestone.png")

def baobab_log():
    """Smooth grey-brown bark in soft folds."""
    r = random.Random(32)
    im = Image.new("RGBA", (16, 16))
    for x in range(16):
        fold = 1 if x % 5 in (0, 1) else 0
        for y in range(16):
            n = r.randint(-7, 7)
            c = (138, 122, 110) if not fold else (116, 101, 92)
            if (y + x * 2) % 13 == 0: c = (156, 140, 126)
            im.putpixel((x, y), (clamp(c[0] + n), clamp(c[1] + n), clamp(c[2] + n), 255))
    im.save(OUT + "baobab_log.png")

def baobab_log_top():
    r = random.Random(33)
    im = Image.new("RGBA", (16, 16))
    for x in range(16):
        for y in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            n = r.randint(-6, 6)
            if d > 7: c = (126, 110, 100)
            elif int(d * 0.8) % 2 == 0: c = (214, 186, 150)
            else: c = (194, 164, 128)
            im.putpixel((x, y), (clamp(c[0] + n), clamp(c[1] + n), clamp(c[2] + n), 255))
    im.save(OUT + "baobab_log_top.png")

def baobab_planks():
    r = random.Random(34)
    im = Image.new("RGBA", (16, 16))
    for y in range(16):
        row = y // 4
        for x in range(16):
            n = r.randint(-8, 8)
            c = (204, 176, 138) if row % 2 == 0 else (192, 164, 126)
            if y % 4 == 3: c = (146, 120, 92)
            if (x + row * 7) % 16 == 0 and y % 4 != 3: c = (160, 134, 104)
            im.putpixel((x, y), (clamp(c[0] + n), clamp(c[1] + n), clamp(c[2] + n), 255))
    im.save(OUT + "baobab_planks.png")

if __name__ == "__main__":
    glowcap_gills(); violet_cap()
    skywood_log(); skywood_log_top(); skywood_planks(); skywood_leaves()
    limestone(); baobab_log(); baobab_log_top(); baobab_planks()
