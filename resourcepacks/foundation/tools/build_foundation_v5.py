#!/usr/bin/env python3
"""Build and validate Foundation v5 from immutable Foundation v4."""
from __future__ import annotations

import hashlib
import json
import shutil
import tempfile
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
SOURCE = ROOT / "originals" / "resourcepacks" / "Foundation v4.zip"
OUTPUT = ROOT / "resourcepacks" / "foundation" / "artifacts" / "Foundation v5.zip"

PACK_META = {"pack": {"description": "Foundation v5", "min_format": [88, 0], "max_format": 88}}
CHANGELOG = """Foundation v5

Base pack: Foundation v4
Changes:
- Adds Minecraft 26.2 item-model overrides so Veiled, Silver Birch, and Wisteria Leaves use their existing Foundation bushy block models in inventories and creative menus.
- Preserves the existing leaf texture and tint behavior: Silver Birch retains its upstream constant item tint; Veiled Leaves and Wisteria remain untinted.
- Does not alter any blockstate, block model, texture, normal map, material map, or unrelated resource.

Validation:
- JSON/resource references, archive structure, and narrow v4-to-v5 resource diff checked during packaging.
- No Minecraft runtime validation performed.
"""

def encode(value: object) -> bytes:
    return (json.dumps(value, indent=2) + "\n").encode("utf-8")

ITEMS = {
    "assets/enderscape/items/veiled_leaves.json": {"model": {"type": "minecraft:model", "model": "enderscape:block/veiled_leaves_bushy"}},
    "assets/mynx_trees/items/silver_birch_leaves.json": {"model": {"type": "minecraft:model", "model": "mynx_trees:block/silver_birch_leaves_bushy", "tints": [{"type": "minecraft:constant", "value": -8034015}]}},
    "assets/mynx_trees/items/wisteria_leaves.json": {"model": {"type": "minecraft:model", "model": "mynx_trees:block/wisteria_leaves_bushy"}},
}
ADDITIONS = {"FOUNDATION_V5_CHANGELOG.txt": CHANGELOG.encode("utf-8"), **{path: encode(item) for path, item in ITEMS.items()}}
MODELS = {
    "assets/enderscape/models/block/veiled_leaves_bushy.json": "assets/enderscape/textures/block/veiled_leaves_bushy.png",
    "assets/mynx_trees/models/block/silver_birch_leaves_bushy.json": "assets/mynx_trees/textures/block/silver_birch_leaves_bushy.png",
    "assets/mynx_trees/models/block/wisteria_leaves_bushy.json": "assets/mynx_trees/textures/block/wisteria_leaves_bushy.png",
}

def entries(archive: zipfile.ZipFile) -> list[tuple[str, bytes]]:
    return [(entry.filename, archive.read(entry)) for entry in archive.infolist() if not entry.is_dir()]

def clone(entry: zipfile.ZipInfo) -> zipfile.ZipInfo:
    result = zipfile.ZipInfo(entry.filename, entry.date_time)
    result.comment, result.extra = entry.comment, entry.extra
    result.internal_attr, result.external_attr = entry.internal_attr, entry.external_attr
    result.create_system, result.create_version = entry.create_system, entry.create_version
    result.extract_version, result.flag_bits = entry.extract_version, entry.flag_bits
    result.volume, result.compress_type = entry.volume, entry.compress_type
    return result

def validate(output: Path) -> str:
    with zipfile.ZipFile(SOURCE) as before, zipfile.ZipFile(output) as after:
        if after.testzip() is not None:
            raise ValueError("ZIP CRC validation failed")
        before_entries = entries(before)
        after_entries = entries(after)
        expected = [(name, encode(PACK_META) if name == "pack.mcmeta" else data) for name, data in before_entries] + list(ADDITIONS.items())
        if after_entries != expected:
            raise ValueError("v5 differs from v4 outside pack metadata and the four declared additions")
        names = {name for name, _ in after_entries}
        if any(name.startswith("Foundation v5/") for name in names) or "pack.mcmeta" not in names or not any(name.startswith("assets/") for name in names):
            raise ValueError("invalid resource-pack root structure")
        for name, data in after_entries:
            if name.endswith(".json"):
                json.loads(data)
        if json.loads(after.read("pack.mcmeta")) != PACK_META:
            raise ValueError("unexpected pack metadata")
        for path, expected_item in ITEMS.items():
            item = json.loads(after.read(path))
            if item != expected_item:
                raise ValueError(f"unexpected item definition: {path}")
            namespace, model_path = item["model"]["model"].split(":", 1)
            if f"assets/{namespace}/models/{model_path}.json" not in names:
                raise ValueError(f"unresolved item model: {path}")
        for model_path, texture_path in MODELS.items():
            model = json.loads(after.read(model_path))
            if "bushy" not in model["textures"] or texture_path not in names:
                raise ValueError(f"broken bushy texture chain: {model_path}")
        silver = json.loads(after.read("assets/mynx_trees/items/silver_birch_leaves.json"))
        if silver["model"]["tints"] != [{"type": "minecraft:constant", "value": -8034015}]:
            raise ValueError("Silver Birch tint changed")
        for path in ("assets/enderscape/items/veiled_leaves.json", "assets/mynx_trees/items/wisteria_leaves.json"):
            if "tints" in json.loads(after.read(path))["model"]:
                raise ValueError(f"unexpected tint: {path}")
    return hashlib.sha256(output.read_bytes()).hexdigest()

def main() -> int:
    if not SOURCE.is_file():
        raise FileNotFoundError(SOURCE)
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(dir=OUTPUT.parent) as temporary:
        staged = Path(temporary) / OUTPUT.name
        with zipfile.ZipFile(SOURCE) as before, zipfile.ZipFile(staged, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as after:
            for entry in before.infolist():
                if not entry.is_dir():
                    after.writestr(clone(entry), encode(PACK_META) if entry.filename == "pack.mcmeta" else before.read(entry))
            for name, data in ADDITIONS.items():
                entry = zipfile.ZipInfo(name, (2026, 9, 25, 16, 26, 47))
                entry.compress_type, entry.external_attr = zipfile.ZIP_DEFLATED, 0o100644 << 16
                after.writestr(entry, data)
        shutil.move(staged, OUTPUT)
    print(json.dumps({"artifact": str(OUTPUT), "sha256": validate(OUTPUT)}, indent=2))
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
