"""Verify the optional pack against the real provider archives and finalized C23."""
import hashlib
import json
import sys
from collections import Counter
from contextlib import ExitStack
from pathlib import Path
from zipfile import ZipFile

from masonry import NS, image
from verify import verify
from verify_c22 import compare_c22
from verify_masonry import check_sheets
from verify_mud import check_ranked_shutter
from verify_paths import check_paths, FULL

PREFIX = "resourcepacks/enderscape/"
PALETTES = {
    "veradite": "89a498 90af9a 9fbfa3 afceaf c1dbb7 c3e8c3",
    "mirestone": "6f7d62 7c8770 87917b 8d9b82 99a693 aab9a6",
    "kurodite": "30313b 343740 39414a 424c53 4c5558 546560",
}


class Pack:
    def __init__(self, jar):
        self.jar = jar

    def read(self, name):
        return self.jar.read(PREFIX + name)

    def getinfo(self, name):
        return self.jar.getinfo(PREFIX + name)


def entries(jar):
    return {e.filename for e in jar.infolist() if not e.is_dir()}


def adapt(value, material):
    return (value.replace("minecraft:block/tuff", "enderscape:block/" + material)
            .replace("minecraft:tuff", "enderscape:" + material)
            .replace("tuff", material).replace("Tuff", material.title()))


def compare_resources(current, previous, enderscape, bbb, paths, client):
    old, new = entries(previous), entries(current)
    assert len(old) == 3042 and old <= new
    assert len(new) == sum(not e.is_dir() for e in current.infolist())
    assert {f for f in old if previous.read(f) != current.read(f)} == {"fabric.mod.json"}
    meta = json.loads(current.read("fabric.mod.json"))
    assert meta.pop("mixins") == ["amc.enderscape.mixins.json"] and meta.pop("suggests") == {"enderscape": "*"}
    assert meta["version"] == "0.1.0-canary24"
    meta["version"] = "0.1.0-canary23"
    assert meta == json.loads(previous.read("fabric.mod.json"))
    assert {f for f in new-old if not f.startswith(PREFIX)} == {
        "amc.enderscape.mixins.json", "dev/resivore/amc/EnderscapeFamilies.class",
        "dev/resivore/amc/mixin/EnderscapeInitializationMixin.class"}
    pack = Pack(current)
    pack_format = json.loads(pack.read("pack.mcmeta"))["pack"]
    assert pack_format["min_format"] == 88 and pack_format["max_format"] == [107, 1]
    expected = {"pack.mcmeta"}
    lang = f"assets/{NS}/lang/en_us.json"
    labels, groups = {}, {}
    stone = image(previous, f"assets/{NS}/textures/block/stone_louvered_shutter.png")
    sheets = {"block/column/stone_" + part for part in ("lower", "middle", "none", "top", "upper")}
    sheets.update(("block/polished_stone", "block/urn/stone", "block/moulding/stone",
                   "block/fence/stone_fence", "block/frame/stone/stone"))
    for material, shades in PALETTES.items():
        tile = image(enderscape, f"assets/enderscape/textures/block/{material}.png")
        palette = [tuple(bytes.fromhex(color)) for color in shades.split()]
        assert tile.size == (16, 16) and set(tile.getdata()) == {(*c, 255) for c in palette}
        assert json.loads(enderscape.read(f"assets/enderscape/models/block/{material}.json")) == {
            "parent": "minecraft:block/cube_all", "textures": {"all": "enderscape:block/" + material}}
        for form in (material, material+"_slab", "polished_"+material+"_button", "polished_"+material+"_pressure_plate"):
            enderscape.getinfo(f"assets/enderscape/items/{form}.json")
            enderscape.getinfo(f"assets/enderscape/blockstates/{form}.json")
        for f in old:
            if "tuff" in f and f.endswith(".json") and "button" not in f and "pressure_plate" not in f:
                dest = f.replace("tuff", material)
                expected.add(dest)
                assert json.loads(pack.read(dest)) == json.loads(adapt(previous.read(f).decode(), material)), dest
            elif "/tags/" in f:
                original = json.loads(previous.read(f))["values"]
                members = [v.replace("tuff", material) for v in original if v.startswith(NS + ":tuff_")
                           and not v.endswith(("_button", "_pressure_plate"))]
                if members:
                    groups.setdefault(f, []).extend(members)
        labels.update({adapt(k, material): adapt(v, material) for k,v in json.loads(previous.read(lang)).items()
                       if ".tuff_" in k and not k.endswith(("_button", "_pressure_plate"))})
        check_sheets(pack, bbb, enderscape, material, material, expected, sheets, "enderscape")
        check_paths(pack, paths, client, expected, material, material_archive=enderscape, material_ns="enderscape")
        assert len({image(pack, f"assets/{NS}/textures/block/{material}_{f}.png").tobytes() for f in FULL}) == 4
        shutter_path = f"assets/{NS}/textures/block/{material}_louvered_shutter.png"
        expected.add(shutter_path)
        shutter = image(pack, shutter_path)
        check_ranked_shutter(stone, tile, shutter, material)
        colors = Counter(shutter.getdata())
        assert [colors[(*rgb, 255)] for rgb in palette] == [9, 17, 21, 39, 104, 17]
        assert colors[(0, 0, 0, 0)] == 49
    expected.add(lang)
    assert len(labels) == 84 and labels == json.loads(pack.read(lang))
    for f, members in groups.items():
        tag = json.loads(pack.read(f))
        assert tag["replace"] is False and sorted(tag["values"]) == sorted(set(members)), f
    expected.update(groups)
    actual = {p.removeprefix(PREFIX) for p in new if p.startswith(PREFIX)}
    assert expected == actual, expected ^ actual
    print(f"C24: {len(actual)} optional pack entries, 84 new pairs, 3041 byte-identical C23 entries; {len(new)} total.")


def verify_c24(current, windows, client, bbb, paths, c22, c23, enderscape):
    assert hashlib.sha256(enderscape.read_bytes()).hexdigest() == "9fcc4f59ca88e91f90e7c7d18289f2f859f20c810eebcca924764aa15236c40b"
    assert hashlib.sha256(c23.read_bytes()).hexdigest() == "8e850efa8d6e8f17e8bfa0b2da7058a7b1b0cd91e4c42e133fabedf3062589f5"
    with ExitStack() as stack:
        jar, prev, w, v, b, p, e = (stack.enter_context(ZipFile(f)) for f in (current, c23, windows, client, bbb, paths, enderscape))
        verify(prev, w, v, b, p)
        compare_c22(prev, c22)
        compare_resources(jar, prev, e, b, p, v)


if __name__ == "__main__":
    if not __debug__:
        sys.exit("Verification requires assertions enabled")
    verify_c24(*map(Path, sys.argv[1:]))
