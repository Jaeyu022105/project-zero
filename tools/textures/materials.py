"""Draw original 16x16 monster material icons. Run from the repository root."""
import random
from pathlib import Path

from PIL import Image, ImageDraw

OUT = Path("src/main/resources/assets/projectzero/textures/item")


def icon(name, base, light, shadow, seed, shape):
    rng = random.Random(seed)
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    for y in range(3, 14):
        for x in range(3, 14):
            if shape(x, y) and rng.random() > 0.08:
                color = light if rng.random() < 0.25 else base
                if rng.random() < 0.18:
                    color = shadow
                image.putpixel((x, y), (*color, 255))
    image.save(OUT / f"{name}.png")


def blob(x, y):
    return (x - 8) ** 2 + (y - 8) ** 2 <= 26


def fang(x, y):
    return 5 <= x <= 10 and 3 <= y <= 12 and x >= 8 - (y - 3) // 2


def claw(x, y):
    return (5 <= x <= 10 and 5 <= y <= 12) or (x in (4, 11) and 3 <= y <= 7)


def pelt(x, y):
    return 4 <= x <= 11 and 4 <= y <= 12 and not (x in (4, 11) and y in (4, 12))


def meat(x, y):
    return (x - 8) ** 2 + (y - 8) ** 2 <= 25


def plate(x, y):
    return 4 <= x <= 11 and 4 <= y <= 11 and (x + y) % 4 != 0


def gland(x, y):
    return (x - 8) ** 2 + (y - 8) ** 2 <= 16 or (6 <= x <= 9 and 3 <= y <= 6)


def shard(x, y):
    return 7 <= x <= 9 and 3 <= y <= 12 or 5 <= x <= 10 and 7 <= y <= 10


def sinew(x, y):
    return (5 <= x <= 6 and 4 <= y <= 11) or (9 <= x <= 10 and 4 <= y <= 11) or (6 <= x <= 9 and y in (4, 11))


def crystal(x, y):
    return 5 <= x <= 10 and 4 <= y <= 11 and x >= 8 - (y - 4) // 2 and x <= 8 + (y - 4) // 2


if __name__ == "__main__":
    OUT.mkdir(parents=True, exist_ok=True)
    icon("beast_fang", (220, 211, 180), (250, 241, 216), (150, 138, 115), 1, fang)
    icon("beast_claw", (190, 177, 145), (235, 220, 183), (116, 103, 82), 2, claw)
    icon("beast_pelt", (129, 91, 57), (177, 132, 82), (82, 57, 42), 3, pelt)
    icon("raw_beast_meat", (176, 63, 67), (225, 105, 102), (112, 42, 50), 4, meat)
    icon("cooked_beast_meat", (143, 80, 43), (204, 134, 65), (87, 49, 35), 5, meat)
    icon("chitin_plate", (91, 111, 99), (149, 176, 143), (48, 65, 66), 6, plate)
    icon("venom_gland", (99, 155, 75), (185, 218, 93), (53, 92, 59), 7, gland)
    icon("bone_shard", (196, 188, 160), (239, 231, 200), (128, 118, 101), 8, shard)
    icon("rotten_sinew", (111, 105, 69), (164, 145, 84), (62, 69, 51), 9, sinew)
