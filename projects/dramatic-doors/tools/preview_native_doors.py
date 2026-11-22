"""Inspect packaged door artwork and write a local comparison sheet (requires Pillow)."""
import argparse
from contextlib import ExitStack
from io import BytesIO
import json
from pathlib import Path
from zipfile import ZipFile

from PIL import Image, ImageDraw, ImageFont


def sprite(jar, name):
    with Image.open(BytesIO(jar.read(name + ".png"))) as image:
        assert image.size == (16, 16), name
        return image.convert("RGBA")


def stack(images):
    face = Image.new("RGBA", (16, 16 * len(images)))
    for i, image in enumerate(images):
        face.paste(image, (0, i * 16))
    return face


def old_face(jar, material, source, parts, zoom):
    """Project C10's closed left model face at preview resolution, using its actual UV bounds."""
    face = Image.new("RGBA", (16 * zoom, 16 * zoom * len(parts)))
    for i, part in enumerate(parts):
        name = "short_" + material + "_door" if part == "short" else "tall_" + material + "_door_" + part
        base = f"resourcepacks/{material}_door/assets/dramaticdoors/models/block/{name}_left.json"
        parent = json.loads(jar.read(base))["parent"].split(":")[1]
        model = json.loads(jar.read(f"assets/dramaticdoors/models/{parent}.json"))
        for element in model["elements"]:
            side = element["faces"]["west"]
            texture = source[side["texture"][1:]]
            quad = texture.crop(tuple(side["uv"]))
            bottom, top = element["from"][1], element["to"][1]
            quad = quad.resize((16 * zoom, (top - bottom) * zoom), Image.Resampling.NEAREST)
            face.paste(quad, (0, (16 * i + 16 - top) * zoom))
    return face


def assert_patch(image, xy, source, box):
    a, b, c, d = box
    x, y = xy
    assert image.crop((x, y, x + c - a, y + d - b)).tobytes() == source.crop(box).tobytes(), (xy, box)


def check_art(material, native, short, tall, icons):
    assert [im.getbbox() for im in icons] == [(3, 3, 13, 14), (4, 0, 12, 16)]
    assert len(set(im.tobytes() for im in icons)) == 2
    if material == "celestial":
        return
    if material == "veiled":
        assert_patch(short, (2, 1), native, (2, 2, 14, 15))
        assert_patch(short, (2, 14), native, (2, 28, 14, 29))
        assert_patch(tall, (0, 6), native, (0, 2, 16, 16))      # Grille crosses the upper join.
        assert_patch(tall, (0, 32), native, (0, 16, 16, 32))
        assert_patch(tall, (0, 31), native, (0, 28, 3, 29))     # Stile carries through the waist rail.
    else:
        assert_patch(short, (1, 2), native, (1, 6, 12, 18))
        assert_patch(short, (12, 11), native, (12, 21, 15, 24))
        assert_patch(short, (0, 14), native, (0, 29, 16, 31))
        assert_patch(tall, (0, 6), native, (0, 2, 16, 24))      # Porthole and latch keep their spacing.
        assert_patch(tall, (0, 40), native, (0, 24, 16, 32))
        for x in (5, 8, 9, 12, 14, 15):                         # Vertical grooves cross the lower join.
            assert tall.getpixel((x, 31)) == tall.getpixel((x, 32))
    # Both apertures keep exactly the native number of transparent texels; none leak through the panels.
    for face in (short, tall):
        assert face.getchannel("A").histogram()[0] == native.getchannel("A").histogram()[0]
    for i in (15, 31):
        for y in (i, i + 1):
            assert tall.getpixel((0, y))[3] == tall.getpixel((15, y))[3] == 255
    assert len({tall.crop((0, y, 16, y + 16)).tobytes() for y in (0, 16, 32)}) == 3


def font(size, bold=False):
    for path in (("C:/Windows/Fonts/segoeuib.ttf" if bold else "C:/Windows/Fonts/segoeui.ttf"),
                 ("DejaVuSans-Bold.ttf" if bold else "DejaVuSans.ttf")):
        try:
            return ImageFont.truetype(path, size)
        except OSError:
            pass
    return ImageFont.load_default(size=size)


def render(candidate, previous, enderscape, ribbits, output):
    sheet = Image.new("RGB", (1100, 884), "#f4f1e9")
    draw = ImageDraw.Draw(sheet)
    ink, muted = "#242c33", "#4f5a61"
    draw.text((25, 15), "Dramatic Doors  /  Canary 11", font=font(32, True), fill=ink)
    draw.text((26, 59), "Source artwork, C10 face mappings and finished C11 textures at a common pixel scale.",
              font=font(18), fill=muted)
    centers = [177, 308, 439, 585, 717, 891, 1021]
    for label, center in zip(("Source", "C10 short", "C10 tall", "C11 short", "C11 tall", "Short item", "Tall item"), centers):
        draw.text((center, 107), label, anchor="mm", fill=ink, font=font(16, True))
    zoom = 4
    for i, (material, title, provider, namespace) in enumerate((
        ("veiled", "Veiled", enderscape, "enderscape"),
        ("mossy_oak", "Mossy Oak", ribbits, "ribbits"),
        ("celestial", "Celestial", enderscape, "enderscape"),
    )):
        top = 130 + i * 235
        floor = top + 210
        rect = (13, top, 1087, top + 226)
        draw.rounded_rectangle(rect, radius=10, fill="#dcdedb")
        draw.text((23, top + 10), title, font=font(17, True), fill=ink)
        draw.text((23, top + 37), "Reference" if i == 2 else namespace.title(), font=font(13), fill=muted)
        for x in (243, 511, 802):
            draw.line((x, top + 12, x, top + 210), fill="#b7bfbb")
        source = {h: sprite(provider, f"assets/{namespace}/textures/block/{material}_door_{h}") for h in ("top", "bottom")}
        native = stack([source["top"], source["bottom"]])
        prefix = (f"resourcepacks/{material}_door/" if i != 2 else "") + "assets/dramaticdoors/textures/"
        short = sprite(candidate, prefix + f"block/short_{material}_door")
        tall = stack([sprite(candidate, prefix + f"block/tall_{material}_door_{third}") for third in ("top", "middle", "bottom")])
        icons = [sprite(candidate, prefix + f"item/{h}_{material}_door") for h in ("short", "tall")]
        check_art(material, native, short, tall, icons)

        def place(im, center, scale):
            panel = im.resize((im.width * scale, im.height * scale), Image.Resampling.NEAREST)
            x, y = center - panel.width // 2, floor - panel.height
            sheet.paste(panel, (x, y), panel)

        place(native, centers[0], zoom)
        if i != 2:
            place(old_face(previous, material, source, ["short"], zoom), centers[1], 1)
            place(old_face(previous, material, source, ["top", "middle", "bottom"], zoom), centers[2], 1)
        else:
            draw.multiline_text((278, top + 91), "Existing Dramatic Doors\nlayout reference", fill=muted,
                                font=font(17), spacing=4)
        place(short, centers[3], zoom)
        place(tall, centers[4], zoom)
        for center, icon in zip(centers[-2:], icons):
            side = 16 * 6
            draw.rectangle((center - side // 2, floor - side, center + side // 2, floor), fill="#b9bfbb")
            place(icon, center, 6)
    draw.text((25, 850), "Block faces at 4×. Item canvases at 6×. Light gray shows transparent cutouts.",
              fill=muted, font=font(16))
    output.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(output)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    for key in ("candidate", "previous", "enderscape", "ribbits", "output"):
        parser.add_argument(key, type=Path)
    args = parser.parse_args()
    with ExitStack() as files:
        jars = [files.enter_context(ZipFile(p)) for p in (args.candidate, args.previous, args.enderscape, args.ribbits)]
        render(*jars, args.output)
    print("Verified native landmarks, joins, transparency and icon bounds. Saved", args.output)
