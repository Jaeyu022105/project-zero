"""Generate the original 16x16 waypoint stone block textures."""
from pathlib import Path
import random
from PIL import Image, ImageDraw

OUT = Path("src/main/resources/assets/projectzero/textures/block")
OUT.mkdir(parents=True, exist_ok=True)

rng = random.Random(414)
stone = Image.new("RGBA", (16, 16))
for y in range(16):
    for x in range(16):
        shade = rng.randint(-13, 13)
        if y in (0, 15): shade -= 8
        stone.putpixel((x, y), (max(0, 93 + shade), max(0, 105 + shade), max(0, 117 + shade), 255))
draw = ImageDraw.Draw(stone)
draw.line((0, 4, 5, 4), fill=(58, 70, 82, 255))
draw.line((10, 11, 15, 11), fill=(138, 151, 160, 255))
draw.point((3, 13), fill=(51, 63, 74, 255))
draw.point((12, 2), fill=(153, 164, 172, 255))
stone.save(OUT / "waypoint_stone.png")

rune = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
d = ImageDraw.Draw(rune)
cyan = (70, 229, 255, 255)
dim = (34, 139, 173, 210)
# An original, branching location rune: a diamond ring with an ascending stem.
d.line((8, 1, 12, 5, 8, 9, 4, 5, 8, 1), fill=dim, width=1)
d.line((8, 5, 8, 14), fill=cyan, width=2)
d.line((8, 9, 4, 12), fill=cyan, width=1)
d.line((8, 9, 12, 12), fill=cyan, width=1)
d.point((8, 14), fill=(190, 252, 255, 255))
d.point((4, 12), fill=(190, 252, 255, 255))
d.point((12, 12), fill=(190, 252, 255, 255))
rune.save(OUT / "waypoint_stone_rune.png")
