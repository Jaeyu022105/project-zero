"""Original 16x16 placeholder art for companion items. Run from the repository root: python tools/textures/companions.py"""
from PIL import Image, ImageDraw

OUT = "src/main/resources/assets/projectzero/textures/item/"


def bond_crystal():
    # TODO(claude-art): replace with the final companion art direction.
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    draw.polygon([(8, 0), (13, 4), (12, 11), (8, 15), (3, 11), (2, 4)], fill=(34, 125, 169, 255))
    draw.polygon([(8, 1), (10, 4), (8, 12), (4, 10), (3, 5)], fill=(89, 215, 239, 255))
    draw.polygon([(8, 1), (12, 5), (10, 10), (8, 12)], fill=(173, 247, 255, 255))
    draw.line([(1, 4), (0, 7)], fill=(113, 230, 255, 180))
    draw.line([(14, 8), (15, 11)], fill=(113, 230, 255, 180))
    image.save(OUT + "bond_crystal.png")


if __name__ == "__main__":
    bond_crystal()
