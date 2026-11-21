"""Check the Button texture bindings against finalized Column geometry and all C26 bytes."""
import hashlib
import json
import subprocess
import sys
from itertools import product
from pathlib import Path
from zipfile import ZipFile

from masonry import NS, image
from verify_c24 import PREFIX, entries


def check_buttons(current, previous, vanilla, material, prefix, sprite_hash):
    root = prefix + f"assets/{NS}/"
    models = root + "models/block/"
    sprite = f"{NS}:block/column/{material}_top"
    texture_path = root + f"textures/block/column/{material}_top.png"
    assert hashlib.sha256(previous.read(texture_path)).hexdigest() == sprite_hash
    cap = image(previous, texture_path)
    assert cap.size == (16, 16) and cap.getchannel("A").getextrema() == (255, 255)

    column = json.loads(previous.read(root + f"blockstates/{material}_column.json"))["multipart"]
    for kind in ("none", "top"):
        for layer in ("", "_layer4"):
            condition = {"axis": "y", "type": kind}
            if layer:
                condition["layer_4"] = "true"
            entry, = [part["apply"] for part in column if part["when"] == condition]
            assert entry["model"] == f"{NS}:block/column/{material}/{kind}{layer}"
            assert not any(entry.get(k) for k in ("x", "y", "uvlock"))

    for model in ("none", "top", "none_layer4", "top_layer4", "inventory"):
        node = json.loads(previous.read(models + f"column/{material}/{model}.json"))
        element, = node["elements"]
        start, end = element["from"], element["to"]
        inset = 2 if model in ("none", "top") else 0
        assert start == [inset, 12 if model.endswith("_layer4") else 0, inset]
        assert end == [16-inset, 16, 16-inset] and "rotation" not in element
        assert node["textures"]["top"] == sprite
        assert element["faces"]["up"] == {"uv": [start[0], start[2], end[0], end[2]], "texture": "#top"}

    button = material + "_button"
    variants = json.loads(current.read(root + f"blockstates/{button}.json"))["variants"]
    expected_states = product(("wall", "floor", "ceiling"), ("north", "south", "west", "east"), ("true", "false"))
    assert set(variants) == {f"face={face},facing={facing},powered={power}" for face, facing, power in expected_states}
    for state, entry in variants.items():
        suffix = "_pressed" if state.endswith("powered=true") else ""
        assert entry["model"] == f"{NS}:block/{button}{suffix}"
    item = json.loads(current.read(root + f"items/{button}.json"))
    assert item["model"]["model"] == f"{NS}:block/{button}_inventory"

    changed = set()
    for suffix in ("", "_pressed", "_inventory"):
        name = models + button + suffix + ".json"
        node = json.loads(current.read(name))
        old = json.loads(previous.read(name))
        assert old["textures"] != node["textures"]
        assert node == {**old, "textures": {"texture": sprite}}
        assert node["parent"] == "minecraft:block/button" + suffix
        parent = json.loads(vanilla.read(f"assets/minecraft/models/block/button{suffix}.json"))
        assert parent["textures"]["particle"] == "#texture"
        element, = parent["elements"]
        assert set(element["faces"]) == {"up", "down", "north", "east", "south", "west"}
        for face in element["faces"].values():
            assert face["texture"] == "#texture"
            # Respect the inventory parent's reversed V. Every sampled texel comes
            # from the same coordinate on the unrotated full cap.
            u0, v0, u1, v1 = face["uv"]
            left, right = sorted((u0, u1))
            top, bottom = sorted((v0, v1))
            assert 0 <= left < right <= 16 and 0 <= top < bottom <= 16
            dest = root + "textures/" + node["textures"]["texture"].split(":")[1] + ".png"
            assert image(current, dest).crop((left, top, right, bottom)).tobytes() == cap.crop((left, top, right, bottom)).tobytes()
        changed.add(name)
    return changed


if __name__ == "__main__":
    if not __debug__:
        sys.exit("Verification requires assertions enabled")
    current, *references, c26 = map(Path, sys.argv[1:])
    assert hashlib.sha256(c26.read_bytes()).hexdigest() == "06eed8311abd74bc2cc9e522d4c7667ebaca92539ff1289b3f290d1efc326456"
    subprocess.run([sys.executable, "-B", Path(__file__).with_name("verify_c26.py"), c26, *references], check=True)
    with ZipFile(current) as jar, ZipFile(c26) as previous, ZipFile(references[1]) as vanilla:
        files = entries(previous)
        assert entries(jar) == files and len(files) == sum(not e.is_dir() for e in jar.infolist()) == 4769
        changed = {"fabric.mod.json"}
        for material, prefix, digest in (
                ("purpur", "", "b447994778c6baa734822cd695f1cbd1694833bf25099c46135466891a4912b8"),
                ("dusk_purpur", PREFIX, "55dc3a976eafd29ac6033ee401f738fa0af1755d01b88993c0ebc5ebac3bb584")):
            changed.update(check_buttons(jar, previous, vanilla, material, prefix, digest))
        assert json.loads(jar.read("fabric.mod.json")) == {**json.loads(previous.read("fabric.mod.json")), "version": "0.1.0-canary27"}
        assert {p for p in files if jar.read(p) != previous.read(p)} == changed
        print("C27: 48 placed Button states and two item previews verified; six model bindings and release version changed; 4762 entries byte-identical to C26.")
