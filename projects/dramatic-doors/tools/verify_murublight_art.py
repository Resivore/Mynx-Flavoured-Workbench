"""Verify the owner faces, item projections and C12 archive delta. Render the native door comparison."""
import argparse
import json
from hashlib import sha256
from pathlib import Path
from zipfile import ZipFile

from PIL import Image, ImageDraw

from preview_native_doors import sprite, stack
from verify_native_assets import check

ROOT = "assets/dramaticdoors/textures/"
SHORT = ROOT + "block/short_murushroom_door"
TALL = [ROOT + "block/tall_murushroom_door_" + third for third in ("top", "middle", "bottom")]
ICONS = [ROOT + "item/" + height + "_murushroom_door" for height in ("short", "tall")]
# RGBA digests of the supplied short_oak_door_front.png and tall_murushroom_door_front.png.
OWNER_PIXELS = [
    "7a06b5d87c46749b972527dfd29b3a1501acb7ca997b529f15f0a4f4b9cddbfe",
    "89fd7727b522f1607827d290f21be2d186db8b36778b6f428149e1efe86d4367",
]


def verify(previous, current, minecraft, enderscape, output):
    files = {n for n in previous.namelist() if not n.endswith("/")}
    assert files == {n for n in current.namelist() if not n.endswith("/")}
    changed = {n for n in files if previous.read(n) != current.read(n)}
    assert changed == {n + ".png" for n in [SHORT, *TALL, *ICONS]} | {"fabric.mod.json"}, changed
    before, after = (json.loads(j.read("fabric.mod.json")) for j in (previous, current))
    assert before.pop("version").endswith("canary12") and after.pop("version").endswith("canary13")
    assert before == after
    check("murushroom", "enderscape", {"dramaticdoors": current, "minecraft": minecraft, "enderscape": enderscape})

    def faces(jar):
        return [sprite(jar, SHORT), stack([sprite(jar, n) for n in TALL])]

    new_faces = faces(current)
    icons = [sprite(current, path) for path in ICONS]
    for face, icon, path, digest in zip(new_faces, icons, ICONS, OWNER_PIXELS):
        assert sha256(face.tobytes()).hexdigest() == digest
        expected = sprite(previous, path)
        left, top, right, bottom = expected.getbbox()
        expected.paste(face.resize((right - left, bottom - top), Image.Resampling.NEAREST), (left, top))
        assert icon.tobytes() == expected.tobytes()

    native = stack([sprite(enderscape, "assets/enderscape/textures/block/murublight_door_" + part)
                    for part in ("top", "bottom")])
    sheet = Image.new("RGB", (850, 345), "#25292e")
    draw = ImageDraw.Draw(sheet)
    columns = ("Native regular", "C12 short", "C12 tall", "Owner / C13 short", "Owner / C13 tall",
               "C13 short item", "C13 tall item")
    for i, (label, art) in enumerate(zip(columns, [native, *faces(previous), *new_faces, *icons])):
        panel = art.resize((art.width * 6, art.height * 6), Image.Resampling.NEAREST)
        x = 14 + i * 120
        draw.text((x, 12), label, fill="white")
        sheet.paste(panel, (x, 325 - panel.height), panel)
    output.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(output)
    print(f"64 state selections, two item graphs, both owner faces and icons verified; {len(files)-len(changed):,} unchanged C12 files.")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ("previous", "current", "minecraft", "enderscape", "output"):
        parser.add_argument(name, type=Path)
    args = parser.parse_args()
    assert sha256(args.previous.read_bytes()).hexdigest() == "7da8044d0c2ab1eec84227a55bb51dacd7a5af400aee2476c0f45b51dba676cf"
    with ZipFile(args.previous) as previous, ZipFile(args.current) as current, ZipFile(args.minecraft) as minecraft, ZipFile(args.enderscape) as enderscape:
        verify(previous, current, minecraft, enderscape, args.output)
