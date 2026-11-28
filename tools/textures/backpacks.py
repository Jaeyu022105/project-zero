"""TODO(claude-art): replace original 16x16 placeholder backpack art. Run with: python tools/textures/backpacks.py"""
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path("src/main/resources/assets/projectzero/textures")


def draw_pack(name, leather, shadow, trim, emblem):
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    # Straps arch over the square pack body.
    draw.rectangle((4, 1, 5, 4), fill=shadow)
    draw.rectangle((10, 1, 11, 4), fill=shadow)
    draw.rectangle((5, 3, 10, 4), fill=trim)
    draw.rectangle((3, 4, 12, 14), fill=shadow)
    draw.rectangle((4, 5, 11, 13), fill=leather)
    draw.rectangle((5, 6, 10, 9), fill=trim)
    draw.rectangle((6, 7, 9, 8), fill=emblem)
    draw.rectangle((5, 10, 10, 12), fill=shadow)
    draw.rectangle((7, 10, 8, 11), fill=trim)
    draw.point((5, 5), fill=(255, 231, 170, 255))
    draw.point((10, 5), fill=(255, 231, 170, 255))
    image.save(ROOT / "item" / f"{name}.png")


def slot_icon():
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    draw.rectangle((5, 2, 6, 5), fill=(58, 88, 108, 255))
    draw.rectangle((9, 2, 10, 5), fill=(58, 88, 108, 255))
    draw.rectangle((4, 4, 11, 13), fill=(65, 112, 143, 255))
    draw.rectangle((5, 5, 10, 11), fill=(91, 174, 199, 255))
    draw.rectangle((6, 7, 9, 9), fill=(28, 60, 82, 255))
    draw.point((7, 8), fill=(125, 231, 248, 255))
    image.save(ROOT / "gui" / "slots" / "backpack.png")


if __name__ == "__main__":
    (ROOT / "item").mkdir(parents=True, exist_ok=True)
    (ROOT / "gui" / "slots").mkdir(parents=True, exist_ok=True)
    draw_pack("leather_satchel", (132, 82, 48, 255), (77, 43, 29, 255), (186, 132, 68, 255), (218, 181, 103, 255))
    draw_pack("hunters_pack", (66, 111, 88, 255), (34, 62, 51, 255), (181, 143, 75, 255), (221, 207, 151, 255))
    draw_pack("rank_pack", (67, 82, 130, 255), (35, 43, 77, 255), (106, 201, 221, 255), (212, 248, 255, 255))
    slot_icon()
