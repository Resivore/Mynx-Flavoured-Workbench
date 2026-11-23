"""Native-UV side-face seam checks and the local nearest-neighbor comparison sheet."""
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


# Front elevations. C is a cut donor and S a smooth receiver without an
# overlay. Numbers are the actual standard-overlay tile slots on smooth faces.
# Continuity 3.0.1+26.2 getSprites: right=7, down=1, up=15, right+down=3,
# upper-right diagonal alone=14. Slots 1, 14 and 15 really are blank here.
LAYOUTS = {
    "Straight edge": [("S", 7, "C", "C")] * 3,
    "Inside corner": [("S", 7, "C", "C"), (1, 3, "C", "C"), ("C", "C", "C", "C")],
    "Outside corner": [("S", 7, "C", "C"), ("S", 7, "C", "C"), ("S", 14, 15, 15)],
}


def cutout(background, foreground):
    return Image.composite(foreground, background, foreground.getchannel("A").point(lambda a: 255 if a >= 128 else 0))


def compose(layout, tiles, source, smooth):
    view = Image.new("RGBA", (64, 48))
    for row, cells in enumerate(layout):
        for col, slot in enumerate(cells):
            if slot == "C":
                face = source
            elif slot == "S":
                face = smooth
            else:
                face = cutout(smooth, tiles[slot])
            view.paste(face, (16 * col, 16 * row))
    return view


def verify_seams(tiles, source, smooth, live):
    # Compare both texels straddling each native wrap to independent periodic
    # source art. Neighboring texels can differ legitimately across a carving.
    # Eight choices per side: seven drawn tiles and the unmasked donor.
    faces = {i: cutout(smooth, tiles[i]) for i in live}
    faces["C"] = source
    checks = 0
    for vertical in (False, True):
        for left, first in faces.items():
            for right, second in faces.items():
                for t in range(16):
                    before, after = ((t, 15), (t, 0)) if vertical else ((15, t), (0, t))
                    for key, face, uv in ((left, first, before), (right, second, after)):
                        expected = source if key == "C" or uv[0] == 15 else smooth
                        assert face.getpixel(uv) == expected.getpixel(uv)
                        checks += 1
    for layout in LAYOUTS.values():
        rendered = compose(layout, tiles, source, smooth)
        for y in range(rendered.height):
            for x in range(rendered.width):
                slot = layout[y // 16][x // 16]
                uv = (x % 16, y % 16)
                expected = source if slot == "C" or (slot in live and uv[0] == 15) else smooth
                assert rendered.getpixel((x, y)) == expected.getpixel(uv)
    print(f"128 horizontal/vertical tile pairings, {checks} boundary texels, 3 complete 64x48 fixtures passed.")


def comparison_sheet(old, new, source, smooth, folder):
    sheet = Image.new("RGB", (1140, 855), "#f7f5f0")
    pen = ImageDraw.Draw(sheet)

    def font(size, bold=False):
        for family in (("segoeuib.ttf", "DejaVuSans-Bold.ttf") if bold else ("segoeui.ttf", "DejaVuSans.ttf")):
            try:
                return ImageFont.truetype(family, size)
            except OSError:
                pass
        return ImageFont.load_default(size=size)

    def label(x, y, text, size=18, bold=False):
        pen.text((x, y), text, fill="#392f2b", font=font(size, bold))

    def draw_native(image, x, y, scale):
        sheet.paste(image.convert("RGB").resize((image.width * scale, image.height * scale), Image.Resampling.NEAREST), (x, y))

    label(28, 16, "Cut Red Sandstone connected edges", 30, True)
    label(29, 61, "Foundation v9 and v10   /   Minecraft 26.2 side faces   /   nearest-neighbor pixels", 17)
    checker = Image.new("RGBA", (16, 16))
    checker.putdata([((224, 220, 212, 255) if (x // 4 + y // 4) % 2 else (201, 197, 189, 255))
                     for y in range(16) for x in range(16)])
    for x, image, title, subtitle in (
        (30, cutout(checker, old[7]), "v9 overlay", "Original baked edge"),
        (408, cutout(checker, new[7]), "v10 overlay", "Same connection mask"),
        (786, source, "Matcha source", "Cut Red Sandstone side"),
    ):
        draw_native(image, x, 113, 7)
        label(x + 125, 139, title, 20, True)
        label(x + 125, 172, subtitle, 15)
    label(29, 241, "Tiles 3, 5, 7, 8, 10, 12 and 13 share this edge. The other ten connection masks are transparent.", 17)
    pen.line((28, 283, 1112, 283), fill="#c6bdb3")
    for col, (name, layout) in enumerate(LAYOUTS.items()):
        x = 93 + col * 352
        label(x, 295, name, 20, True)
        for y, version in ((334, old), (586, new)):
            draw_native(compose(layout, version, source, smooth), x, y, 5)
    label(28, 434, "v9", 24, True)
    label(28, 685, "v10", 24, True)
    label(29, 831, "Smooth receiver and cut donor use Matcha in both rows. Blank corner slots retain their original transparency.", 15)
    sheet.save(Path(folder) / "red-sandstone-ctm-comparison.png")
