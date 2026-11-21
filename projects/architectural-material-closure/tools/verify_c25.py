"""Limit C25 to the Cinnabar visual bindings and verify each transferred pixel."""
import hashlib
import json
import sys
from contextlib import ExitStack
from pathlib import Path
from zipfile import ZipFile

from masonry import NS, image
from verify_c24 import entries, verify_c24
from verify_masonry import check_sheets
from verify_mud import check_ranked_shutter
from verify_paths import FULL, check_paths


def verify_cinnabar(jar, previous, client, bbb, paths):
    raw = "minecraft:block/cinnabar"
    brick = "minecraft:block/cinnabar_bricks"
    tile_path = "assets/minecraft/textures/block/cinnabar_bricks.png"
    assert hashlib.sha256(client.read(tile_path)).hexdigest() == "0cfb48bd8a01555cccd630d3ae7a404624dec3b9e957b25b8acee4538e5e8c70"
    tile = image(client, tile_path)
    assert tile.size == (16, 16) and tile.getchannel("A").getextrema() == (255, 255)
    assert entries(jar) == entries(previous) and len(entries(jar)) == 3896
    assert len(entries(jar)) == sum(not e.is_dir() for e in jar.infolist())
    root = f"assets/{NS}/"
    models, textures = set(), set()
    for name in entries(previous):
        old, new = previous.read(name), jar.read(name)
        if name == "fabric.mod.json":
            meta = json.loads(old)
            assert meta["version"] == "0.1.0-canary24"
            meta["version"] = "0.1.0-canary25"
            assert json.loads(new) == meta
        elif name.startswith(root + "models/") and b'"minecraft:block/cinnabar"' in old:
            model = json.loads(old)
            slots = model["textures"]
            assert "cinnabar" in name and raw in slots.values()
            model["textures"] = {slot: brick if sprite == raw else sprite for slot, sprite in slots.items()}
            assert json.loads(new) == model, name
            models.add(name)
        elif name.startswith(root + "textures/") and "cinnabar" in name and name.endswith(".png"):
            assert new != old, name
            textures.add(name)
        else:
            assert new == old, name

    sheets = {"block/column/stone_" + part for part in ("lower", "middle", "none", "top", "upper")}
    sheets.update(("block/polished_stone", "block/urn/stone", "block/moulding/stone",
                   "block/fence/stone_fence", "block/frame/stone/stone"))
    checked = set()
    check_sheets(jar, bbb, client, "cinnabar", "cinnabar_bricks", checked, sheets)
    check_paths(jar, paths, client, checked, "cinnabar", palette_tile="cinnabar_bricks")
    base = root + "textures/block/"
    assert len({image(jar, base + "cinnabar_" + form + ".png").tobytes() for form in FULL}) == 4
    stone = image(previous, base + "stone_louvered_shutter.png")
    shutter_name = base + "cinnabar_louvered_shutter.png"
    shutter = image(jar, shutter_name)
    check_ranked_shutter(stone, tile, shutter, "cinnabar_bricks")
    checked.add(shutter_name)
    assert {p for p in checked if p.endswith(".png")} == textures and len(textures) == 22
    print(f"C25: {len(models)} Cinnabar model bindings, {len(textures)} sprites, version metadata; "
          f"{len(entries(jar))-len(models)-len(textures)-1} identical C24 entries.")


if __name__ == "__main__":
    if not __debug__:
        sys.exit("Verification requires assertions enabled")
    current, windows, client, bbb, paths, c22, c23, enderscape, c24 = map(Path, sys.argv[1:])
    assert hashlib.sha256(c24.read_bytes()).hexdigest() == "ddafc326d6e41e98170ad5c059a14774858fb1a6bc28b902d95e4b64206d6018"
    verify_c24(c24, windows, client, bbb, paths, c22, c23, enderscape)
    with ExitStack() as stack:
        verify_cinnabar(*(stack.enter_context(ZipFile(p)) for p in (current, c24, client, bbb, paths)))
