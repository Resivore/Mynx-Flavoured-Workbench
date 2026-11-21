"""Prove the three complete families against the shipped C25 Tuff graphs and provider pixels."""
import hashlib
import json
import sys
from collections import defaultdict
from contextlib import ExitStack
from pathlib import Path
from zipfile import ZipFile

from masonry import NS, image
from verify_c24 import PREFIX, Pack, entries, verify_c24
from verify_c25 import verify_cinnabar
from verify_masonry import check_sheets
from verify_mud import check_ranked_shutter
from verify_paths import FULL, check_paths


def verify_families(current, previous, client, enderscape, bbb, paths):
    old, new = entries(previous), entries(current)
    assert len(old) == 3896 and old <= new
    assert len(new) == sum(not member.is_dir() for member in current.infolist())
    lang = f"assets/{NS}/lang/en_us.json"
    labels, groups = defaultdict(dict), defaultdict(list)
    expected = set()
    changed = {"fabric.mod.json", "dev/resivore/amc/BaseStoneFamilies.class", "dev/resivore/amc/EnderscapeFamilies.class"}
    meta = json.loads(previous.read("fabric.mod.json"))
    meta["version"] = "0.1.0-canary26"
    assert meta == json.loads(current.read("fabric.mod.json"))
    stone_path = f"assets/{NS}/textures/block/stone_louvered_shutter.png"
    assert hashlib.sha256(previous.read(stone_path)).hexdigest() == "c67d4b297dd2b312c0732dc38dc607cdae9937d493028000a33f4b2e4e959878"
    stone = image(previous, stone_path)
    sheets = {"block/column/stone_" + part for part in ("lower", "middle", "none", "top", "upper")}
    sheets.update(("block/polished_stone", "block/urn/stone", "block/moulding/stone",
                   "block/fence/stone_fence", "block/frame/stone/stone"))
    for material, slab, sprite, digest in (
            ("purpur", "purpur_slab", "purpur_block", "3e6a2fe3f74a47f110e7870ad2a55814129bd0c1908391c4e5465345a8402dbd"),
            ("resin", "resin_block", "resin_bricks", "ab7b68d7fd987c8d82e9225a33b783419a86ff4a2e77e5b1cc22cd63f546445c"),
            ("dusk_purpur", "dusk_purpur_slab", "dusk_purpur_block", "5f8793fdad0995b213e1091e9ca06b35ee4b0920dde139197c86a45655a60d74")):
        optional = material == "dusk_purpur"
        ns, authority, prefix, pack = ("enderscape", enderscape, PREFIX, Pack(current)) if optional else ("minecraft", client, "", current)
        base = material + "_block"
        tile_path = f"assets/{ns}/textures/block/{sprite}.png"
        assert hashlib.sha256(authority.read(tile_path)).hexdigest() == digest
        for block in (base, slab):
            for folder in ("items", "blockstates"):
                authority.getinfo(f"assets/{ns}/{folder}/{block}.json")
        checked = set()
        for name in old:
            if name.startswith(PREFIX):
                continue
            if "tuff" in name and name.endswith(".json"):
                dest = name.replace("tuff", material)
                checked.add(dest)
                source = previous.read(name).decode()
                ref = json.loads(source.replace("minecraft:block/tuff", f"{ns}:block/{sprite}")
                                 .replace("minecraft:tuff_slab", f"{ns}:{slab}")
                                 .replace("minecraft:tuff", f"{ns}:{base}").replace("tuff", material))
                if material == "resin" and name == f"data/{NS}/recipe/tuff_button.json":
                    ref.pop("ingredients")
                    ref.update(type="minecraft:crafting_shaped", key={"R": "minecraft:resin_block"}, pattern=["R ", " R"])
                if material == "resin" and name == f"data/{NS}/recipe/tuff_louvered_shutter.json":
                    ref["pattern"] = ["B  ", " B ", "  B"]
                assert json.loads(pack.read(dest)) == ref, dest
            if "/tags/" in name:
                members = [v.replace("tuff", material) for v in json.loads(previous.read(name))["values"]
                           if isinstance(v, str) and v.startswith(NS + ":tuff_")]
                if members:
                    groups[prefix + name].extend(members)
        source_labels = json.loads(previous.read(lang))
        additions = {k.replace("tuff", material): v.replace("Tuff", material.replace("_", " ").title())
                     for k, v in source_labels.items() if ".tuff_" in k}
        assert len(additions) == 30
        labels[prefix + lang].update(additions)
        check_sheets(pack, bbb, authority, material, sprite, checked, sheets, ns)
        check_paths(pack, paths, client, checked, material, material_archive=authority, material_ns=ns,
                    palette_tile=sprite, ingredient=base)
        assert len({image(pack, f"assets/{NS}/textures/block/{material}_{form}.png").tobytes() for form in FULL}) == 4
        dest = stone_path.replace("stone_louvered", material + "_louvered")
        check_ranked_shutter(stone, image(authority, tile_path), image(pack, dest), sprite)
        checked.add(dest)
        expected.update(prefix + f for f in checked)
    for name, additions in labels.items():
        assert json.loads(current.read(name)) == json.loads(previous.read(name)) | additions
        changed.add(name)
    for name, additions in groups.items():
        prior = json.loads(previous.read(name)) if name in old else {"replace": False, "values": []}
        tag = json.loads(current.read(name))
        assert tag == {"replace": False, "values": prior["values"] + additions}, name
        (changed if name in old else expected).add(name)
    assert new - old == expected, (new - old) ^ expected
    assert {name for name in old if previous.read(name) != current.read(name)} == changed
    print(f"C26: 90 new pairs, {len(expected)} added entries, {len(old)-len(changed)} identical C25 entries, "
          f"{len(changed)} updated shared entries, {len(new)} total.")


if __name__ == "__main__":
    if not __debug__:
        sys.exit("Verification requires assertions enabled")
    current, windows, client, bbb, paths, c22, c23, end, c24, c25 = map(Path, sys.argv[1:])
    assert hashlib.sha256(c25.read_bytes()).hexdigest() == "dd4f53d297c51c74436b0f240d12943b9de91749669ec7cc0e48107d212e8f81"
    assert hashlib.sha256(c24.read_bytes()).hexdigest() == "ddafc326d6e41e98170ad5c059a14774858fb1a6bc28b902d95e4b64206d6018"
    verify_c24(c24, windows, client, bbb, paths, c22, c23, end)
    with ExitStack() as stack:
        jars = [stack.enter_context(ZipFile(p)) for p in (current, c25, c24, client, end, bbb, paths)]
        release, previous, prior, vanilla, optional, masonry, paving = jars
        verify_cinnabar(previous, prior, vanilla, masonry, paving)
        verify_families(release, previous, vanilla, optional, masonry, paving)
