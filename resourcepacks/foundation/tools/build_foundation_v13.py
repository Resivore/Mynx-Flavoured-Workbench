"""Carry Foundation's existing armor treatments to Matcha 1.12.5 model and equipment lookups."""
import argparse
from copy import copy
from hashlib import sha256
import json
from pathlib import Path
from zipfile import ZipFile


def build(base_path, client_path, output):
    assert sha256(base_path.read_bytes()).hexdigest() == "c45fbc7b8f9b41807c59d0a6addcbf5a8a98ad5361df793cd8cecc8454a71143"
    assert sha256(client_path.read_bytes()).hexdigest() == "6b76122a3db1f299816efb3627e5a28f7f121a4edbd33e4de19ca9427955a8b7"
    with ZipFile(base_path) as base, ZipFile(client_path) as client:
        aliases = {}
        for old, new in (("bronze", "hepatizon"), ("electrum", "electrum"),
                         ("shakudo", "shakudo"), ("steel", "steel"), ("sturdy_leather", "sturdy_leather")):
            model = json.loads(client.read(f"assets/matcha/models/item/{new}_helmet.json"))
            equipment = json.loads(client.read(f"assets/matcha/equipment/{new}.json"))
            for suffix in (("", "_overlay") if new == "sturdy_leather" else ("",)):
                assert f"matcha:item/{new}_helmet{suffix}" in model["textures"].values()
                pairs = [(f"item/{old}_helmet{suffix}", f"item/{new}_helmet{suffix}")]
                for slot in ("humanoid", "humanoid_leggings"):
                    assert any(layer["texture"] == f"matcha:{new}{suffix}" for layer in equipment["layers"][slot])
                    pairs.append((f"entity/equipment/{slot}/{old}{suffix}", f"entity/equipment/{slot}/{new}{suffix}"))
                for source, target in pairs:
                    path = f"assets/matcha/textures/{target}.png"
                    assert path in client.NameToInfo and path not in base.NameToInfo
                    aliases[path] = f"assets/minecraft/textures/{source}.png"
        assert len(aliases) == 18
        output.parent.mkdir(parents=True, exist_ok=True)
        with ZipFile(output, "w") as result:
            for entry in base.infolist():
                data = base.read(entry)
                if entry.filename == "pack.mcmeta":
                    metadata = json.loads(data)
                    metadata["pack"]["description"] = "Foundation v13"
                    data = (json.dumps(metadata, indent=2) + "\n").encode()
                result.writestr(copy(entry), data)
            for target, source in sorted(aliases.items()):
                entry = copy(base.getinfo(source))
                entry.filename = target
                result.writestr(entry, base.read(source))
        with ZipFile(output) as result:
            assert result.testzip() is None and set(result.namelist()) == set(base.namelist()) | aliases.keys()
            assert all(result.read(n) == base.read(n) for n in base.namelist() if n != "pack.mcmeta")
            assert all(result.read(target) == base.read(source) for target, source in aliases.items())


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("base", type=Path)
    parser.add_argument("client", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()
    build(args.base, args.client, args.output)
