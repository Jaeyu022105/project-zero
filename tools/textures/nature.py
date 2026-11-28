"""Original 16x16 textures for our nature blocks. Run from the repo root: python tools/textures/nature.py"""
import random
from PIL import Image, ImageDraw

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
            c = (152, 126, 102) if not fold else (128, 104, 84)
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

def salt_crust():
    """Smooth white salt crust: soft crystalline grain, faint irregular cracks that wrap across
    block edges (Voronoi on a torus), so the ground reads as one continuous crust, not tiles."""
    r = random.Random(43)
    seeds = [(r.uniform(0, 16), r.uniform(0, 16)) for _ in range(5)]
    def dist(x, y, s):
        dx = min(abs(x - s[0]), 16 - abs(x - s[0])); dy = min(abs(y - s[1]), 16 - abs(y - s[1]))
        return (dx * dx + dy * dy) ** 0.5
    im = Image.new("RGBA", (16, 16))
    for x in range(16):
        for y in range(16):
            d = sorted(dist(x + 0.5, y + 0.5, s) for s in seeds)
            crack = d[1] - d[0] < 0.7
            n = r.randint(-5, 5)
            c = (232, 232, 228)
            if crack: c = (214, 213, 207)
            if r.random() < 0.08: c = (246, 247, 245)   # glints
            im.putpixel((x, y), (clamp(c[0] + n), clamp(c[1] + n), clamp(c[2] + n), 255))
    im.save(OUT + "salt_crust.png")

def mana_crystal_block():
    """Faceted pale-blue crystal with bright cores."""
    r = random.Random(42)
    im = Image.new("RGBA", (16, 16))
    for x in range(16):
        for y in range(16):
            facet = ((x + y) // 4 + (x - y) // 5) % 3
            c = [(92, 170, 230), (130, 205, 245), (70, 140, 214)][facet]
            n = r.randint(-8, 8)
            im.putpixel((x, y), (clamp(c[0] + n), clamp(c[1] + n), clamp(c[2] + n), 255))
    for _ in range(8):
        im.putpixel((r.randrange(16), r.randrange(16)), (224, 248, 255, 255))
    im.save(OUT + "mana_crystal_block.png")

def skywood_stripped_log():
    """Smooth pale wood, retaining the soft blue grain of Skywood."""
    r = random.Random(71)
    im = Image.new("RGBA", (16, 16))
    for x in range(16):
        grain = -8 if x % 6 == 0 else (5 if x % 5 == 2 else 0)
        for y in range(16):
            n = r.randint(-7, 7) + grain
            c = (211, 207, 190) if (x + y // 5) % 7 else (166, 191, 208)
            im.putpixel((x, y), (clamp(c[0] + n), clamp(c[1] + n), clamp(c[2] + n), 255))
    im.save(OUT + "stripped_skywood_log.png")

def skywood_stripped_log_top():
    r = random.Random(72)
    im = Image.new("RGBA", (16, 16))
    for x in range(16):
        for y in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            n = r.randint(-5, 5)
            c = (187, 185, 171) if d > 7 else ((226, 220, 197) if int(d * 0.8) % 2 == 0 else (207, 204, 185))
            im.putpixel((x, y), (clamp(c[0] + n), clamp(c[1] + n), clamp(c[2] + n), 255))
    im.save(OUT + "stripped_skywood_log_top.png")

def skywood_door_textures():
    for part, seed, seam in (("bottom", 73, 14), ("top", 74, 1)):
        r = random.Random(seed)
        im = Image.new("RGBA", (16, 16))
        d = ImageDraw.Draw(im)
        for y in range(16):
            for x in range(16):
                n = r.randint(-6, 6)
                c = (187, 204, 218) if x in (0, 15) else (202, 216, 226)
                im.putpixel((x, y), (clamp(c[0] + n), clamp(c[1] + n), clamp(c[2] + n), 255))
        d.rectangle((2, 2, 12, 13), outline=(139, 167, 189, 255))
        d.line((7, 3, 7, 12), fill=(164, 188, 205, 255))
        d.rectangle((10, seam, 11, seam + 1), fill=(101, 152, 186, 255))
        im.save(OUT + f"skywood_door_{part}.png")

def skywood_trapdoor():
    r = random.Random(75)
    im = Image.new("RGBA", (16, 16))
    d = ImageDraw.Draw(im)
    for y in range(16):
        for x in range(16):
            n = r.randint(-7, 7)
            c = (189, 208, 222)
            if x in (0, 15) or y in (0, 15): c = (136, 164, 187)
            if x in (6, 9) or y in (6, 9): c = (153, 178, 198)
            im.putpixel((x, y), (clamp(c[0] + n), clamp(c[1] + n), clamp(c[2] + n), 255))
    d.rectangle((6, 6, 9, 9), fill=(72, 126, 159, 255))
    im.save(OUT + "skywood_trapdoor.png")

def skywood_sapling():
    r = random.Random(76)
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    d.line((7, 15, 8, 8, 8, 4), fill=(126, 152, 151, 255), width=2)
    for box in ((2, 2, 8, 7), (7, 0, 13, 5), (5, 5, 11, 9)):
        base = (124, 190, 197)
        d.ellipse(box, fill=(base[0] + r.randint(-12, 12), base[1] + r.randint(-12, 12), base[2] + r.randint(-8, 8), 255))
    d.point((5, 3), fill=(229, 245, 255, 255))
    d.point((11, 2), fill=(229, 245, 255, 255))
    im.save(OUT + "skywood_sapling.png")

def skywood_door_item():
    """Tiny 16x16 inventory pictogram for the tall in-world door texture pair."""
    r = random.Random(77)
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    d.rectangle((4, 1, 11, 14), fill=(194, 211, 224, 255), outline=(103, 137, 165, 255))
    d.rectangle((6, 3, 9, 6), fill=(124, 171, 198, 255), outline=(151, 182, 202, 255))
    d.rectangle((6, 9, 9, 12), fill=(124, 171, 198, 255), outline=(151, 182, 202, 255))
    d.point((10, 8), fill=(242, 216, 145, 255))
    for y in range(2, 14):
        n = r.randint(0, 2)
        if n == 0: d.point((5, y), fill=(180, 203, 219, 255))
    item_out = "src/main/resources/assets/projectzero/textures/item/"
    im.save(item_out + "skywood_door.png")

def baobab_stripped_log():
    """Warm, smooth core wood with subtle bottle-tree grain."""
    r = random.Random(81)
    im = Image.new("RGBA", (16, 16))
    for x in range(16):
        fold = -9 if x % 5 == 0 else (5 if x % 4 == 1 else 0)
        for y in range(16):
            n = r.randint(-7, 7) + fold
            c = (201, 173, 137) if (x + y // 4) % 6 else (177, 146, 116)
            im.putpixel((x, y), (clamp(c[0] + n), clamp(c[1] + n), clamp(c[2] + n), 255))
    im.save(OUT + "stripped_baobab_log.png")

def baobab_stripped_log_top():
    r = random.Random(82)
    im = Image.new("RGBA", (16, 16))
    for x in range(16):
        for y in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            n = r.randint(-5, 5)
            c = (136, 116, 99) if d > 7 else ((223, 195, 160) if int(d * 0.8) % 2 == 0 else (204, 174, 143))
            im.putpixel((x, y), (clamp(c[0] + n), clamp(c[1] + n), clamp(c[2] + n), 255))
    im.save(OUT + "stripped_baobab_log_top.png")

def baobab_door_textures():
    for part, seed, seam in (("bottom", 83, 14), ("top", 84, 1)):
        r = random.Random(seed)
        im = Image.new("RGBA", (16, 16))
        d = ImageDraw.Draw(im)
        for y in range(16):
            for x in range(16):
                n = r.randint(-6, 6)
                c = (175, 145, 115) if x not in (0, 15) else (119, 94, 77)
                im.putpixel((x, y), (clamp(c[0] + n), clamp(c[1] + n), clamp(c[2] + n), 255))
        d.rectangle((2, 2, 12, 13), outline=(108, 83, 66, 255))
        d.line((7, 3, 7, 12), fill=(143, 111, 85, 255))
        d.rectangle((10, seam, 11, seam + 1), fill=(93, 123, 77, 255))
        im.save(OUT + f"baobab_door_{part}.png")

def baobab_trapdoor():
    r = random.Random(85)
    im = Image.new("RGBA", (16, 16))
    d = ImageDraw.Draw(im)
    for y in range(16):
        for x in range(16):
            n = r.randint(-7, 7)
            c = (171, 141, 111)
            if x in (0, 15) or y in (0, 15): c = (107, 82, 64)
            if x in (6, 9) or y in (6, 9): c = (136, 106, 82)
            im.putpixel((x, y), (clamp(c[0] + n), clamp(c[1] + n), clamp(c[2] + n), 255))
    d.rectangle((6, 6, 9, 9), fill=(91, 118, 73, 255))
    im.save(OUT + "baobab_trapdoor.png")

def baobab_sapling():
    r = random.Random(86)
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    d.line((7, 15, 8, 8, 8, 3), fill=(125, 103, 76, 255), width=2)
    for box in ((2, 2, 8, 7), (7, 0, 13, 5), (5, 5, 11, 9)):
        base = (130, 161, 86)
        d.ellipse(box, fill=(base[0] + r.randint(-12, 12), base[1] + r.randint(-12, 12), base[2] + r.randint(-8, 8), 255))
    d.point((4, 4), fill=(188, 205, 126, 255))
    d.point((10, 2), fill=(188, 205, 126, 255))
    im.save(OUT + "baobab_sapling.png")

def baobab_door_item():
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    d.rectangle((4, 1, 11, 14), fill=(179, 148, 116, 255), outline=(103, 77, 60, 255))
    d.rectangle((6, 3, 9, 6), fill=(133, 157, 98, 255), outline=(121, 94, 72, 255))
    d.rectangle((6, 9, 9, 12), fill=(133, 157, 98, 255), outline=(121, 94, 72, 255))
    d.point((10, 8), fill=(228, 194, 126, 255))
    im.save("src/main/resources/assets/projectzero/textures/item/baobab_door.png")

if __name__ == "__main__":
    glowcap_gills(); violet_cap()
    skywood_log(); skywood_log_top(); skywood_planks(); skywood_leaves()
    limestone(); baobab_log(); baobab_log_top(); baobab_planks()
    salt_crust(); mana_crystal_block()
    skywood_stripped_log(); skywood_stripped_log_top(); skywood_door_textures()
    skywood_trapdoor(); skywood_sapling(); skywood_door_item()
    baobab_stripped_log(); baobab_stripped_log_top(); baobab_door_textures()
    baobab_trapdoor(); baobab_sapling(); baobab_door_item()
