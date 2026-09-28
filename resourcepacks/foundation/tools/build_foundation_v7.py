#!/usr/bin/env python3
"""Build and statically validate Foundation v7 from the retained v6 artifact."""
from __future__ import annotations

import hashlib
import json
import shutil
import tempfile
import zipfile
from datetime import datetime, timezone
from pathlib import Path


ROOT = Path(__file__).resolve().parents[3]
SOURCE = ROOT / "resourcepacks" / "foundation" / "artifacts" / "Foundation v6.zip"
OUTPUT = ROOT / "resourcepacks" / "foundation" / "artifacts" / "Foundation v7.zip"

SOURCE_SHA256 = "ee2cd0ca5fdade2f66404fcd2f34ae950a1661080b4cc23cf1792a76305bf281"
PACK_META = {"pack": {"description": "Foundation v7", "min_format": [88, 0], "max_format": 88}}

TARGETS = {
    "veiled": {
        "item": "assets/enderscape/items/veiled_leaves.json",
        "model": "assets/enderscape/models/item/veiled_leaves_bushy_inventory.json",
        "model_id": "enderscape:item/veiled_leaves_bushy_inventory",
        "world_model": "assets/enderscape/models/block/veiled_leaves_bushy.json",
        "all": "enderscape:block/veiled_leaves",
        "bushy": "enderscape:block/veiled_leaves_bushy",
        "tints": None,
    },
    "silver_birch": {
        "item": "assets/mynx_trees/items/silver_birch_leaves.json",
        "model": "assets/mynx_trees/models/item/silver_birch_leaves_bushy_inventory.json",
        "model_id": "mynx_trees:item/silver_birch_leaves_bushy_inventory",
        "world_model": "assets/mynx_trees/models/block/silver_birch_leaves_bushy.json",
        "all": "mynx_trees:block/silver_birch_leaves",
        "bushy": "mynx_trees:block/silver_birch_leaves_bushy",
        "tints": [{"type": "minecraft:constant", "value": -8034015}],
    },
    "wisteria": {
        "item": "assets/mynx_trees/items/wisteria_leaves.json",
        "model": "assets/mynx_trees/models/item/wisteria_leaves_bushy_inventory.json",
        "model_id": "mynx_trees:item/wisteria_leaves_bushy_inventory",
        "world_model": "assets/mynx_trees/models/block/wisteria_leaves_bushy.json",
        "all": "mynx_trees:block/wisteria_leaves",
        "bushy": "mynx_trees:block/wisteria_leaves_bushy",
        "tints": None,
    },
}


def encode(value: object) -> bytes:
    return (json.dumps(value, indent=2) + "\n").encode("utf-8")


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for block in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def entries(archive: zipfile.ZipFile) -> dict[str, bytes]:
    result: dict[str, bytes] = {}
    for entry in archive.infolist():
        if entry.is_dir():
            continue
        if entry.filename in result:
            raise ValueError(f"duplicate archive entry: {entry.filename}")
        result[entry.filename] = archive.read(entry)
    return result


def clone(entry: zipfile.ZipInfo, name: str | None = None) -> zipfile.ZipInfo:
    result = zipfile.ZipInfo(name or entry.filename, entry.date_time)
    result.comment, result.extra = entry.comment, entry.extra
    result.internal_attr, result.external_attr = entry.internal_attr, entry.external_attr
    result.create_system, result.create_version = entry.create_system, entry.create_version
    result.extract_version, result.flag_bits = entry.extract_version, entry.flag_bits
    result.volume, result.compress_type = entry.volume, entry.compress_type
    return result


def generated_entry(name: str) -> zipfile.ZipInfo:
    entry = zipfile.ZipInfo(name, (2026, 9, 28, 0, 0, 0))
    entry.compress_type = zipfile.ZIP_DEFLATED
    entry.external_attr = 0o100644 << 16
    return entry


def face(texture: str, tint: bool, **extra: object) -> dict[str, object]:
    result: dict[str, object] = {"texture": texture, "uv": [0, 0, 16, 16]}
    if tint:
        result["tintindex"] = 0
    result.update(extra)
    return result


def inventory_model(target: dict[str, object]) -> dict[str, object]:
    """The exact five-element Matcha cross-leaves topology with target materials."""
    tint = target["tints"] is not None
    all_texture = "#all"
    outer_top = "#outer_top"
    outer_bottom = "#outer_bottom"
    return {
        "parent": "minecraft:block/block",
        "textures": {
            "all": target["all"],
            "outer_top": target["bushy"],
            "outer_bottom": target["bushy"],
            "particle": target["all"],
        },
        "elements": [
            {
                "from": [-8, -8, 8], "to": [24, 24, 8],
                "faces": {"north": face(outer_bottom, tint), "south": face(outer_bottom, tint)},
                "rotation": {"origin": [8, 8, 8], "axis": "y", "angle": 36}, "shade": False,
            },
            {
                "from": [-8, -8, 8], "to": [24, 24, 8],
                "faces": {"north": face(outer_top, tint), "south": face(outer_top, tint)},
                "rotation": {"origin": [8, 8, 8], "axis": "y", "angle": -12}, "shade": False,
            },
            {
                "from": [0, 0, 0], "to": [16, 16, 16],
                "faces": {
                    "down": face(all_texture, tint, uv=[16, 16, 0, 0], cullface="down"),
                    "up": face(all_texture, tint, cullface="up"),
                    "north": face(all_texture, tint, cullface="north"),
                    "east": face(all_texture, tint, cullface="east"),
                    "south": face(all_texture, tint, cullface="south"),
                    "west": face(all_texture, tint, cullface="west"),
                },
            },
            {
                "from": [8, -8, -8], "to": [8, 24, 24],
                "faces": {"east": face(outer_top, tint), "west": face(outer_top, tint)},
                "rotation": {"origin": [8, 8, 8], "axis": "y", "angle": -12}, "shade": False,
            },
            {
                "from": [8, -8, -8], "to": [8, 24, 24],
                "faces": {"east": face(outer_bottom, tint), "west": face(outer_bottom, tint)},
                "rotation": {"origin": [8, 8, 8], "axis": "y", "angle": 36}, "shade": False,
            },
        ],
    }


def item_definition(target: dict[str, object]) -> dict[str, object]:
    model: dict[str, object] = {"type": "minecraft:model", "model": target["model_id"]}
    if target["tints"] is not None:
        model["tints"] = target["tints"]
    return {"model": model}


ADDITIONS = {target["model"]: encode(inventory_model(target)) for target in TARGETS.values()}
REPLACEMENTS = {
    "pack.mcmeta": encode(PACK_META),
    **{target["item"]: encode(item_definition(target)) for target in TARGETS.values()},
}


def validate() -> dict[str, object]:
    if sha256(SOURCE) != SOURCE_SHA256:
        raise ValueError("Foundation v6 source hash does not match its canonical record")
    with zipfile.ZipFile(SOURCE) as before, zipfile.ZipFile(OUTPUT) as final:
        if final.testzip() is not None:
            raise ValueError("ZIP CRC validation failed")
        source_entries, final_entries = entries(before), entries(final)
        added = set(final_entries) - set(source_entries)
        removed = set(source_entries) - set(final_entries)
        changed = {name for name in source_entries if source_entries[name] != final_entries[name]}
        if added != set(ADDITIONS) or removed or changed != set(REPLACEMENTS):
            raise ValueError(f"v7 diff is not item-only; added={sorted(added)}, removed={sorted(removed)}, changed={sorted(changed)}")
        if any(name.startswith(("/", "\\")) or "\\" in name or name.startswith("../") for name in final_entries):
            raise ValueError("invalid archive path")
        if "pack.mcmeta" not in final_entries or not any(name.startswith("assets/") for name in final_entries):
            raise ValueError("invalid resource-pack root layout")
        if any(name.startswith("Foundation v7/") for name in final_entries):
            raise ValueError("resource-pack files are nested below an extra directory")
        if json.loads(final_entries["pack.mcmeta"]) != PACK_META:
            raise ValueError("Foundation v7 pack metadata is incorrect")
        for name, data in final_entries.items():
            if name.endswith((".json", ".mcmeta")):
                json.loads(data)
        for label, target in TARGETS.items():
            item = json.loads(final_entries[target["item"]])
            model = json.loads(final_entries[target["model"]])
            if item != item_definition(target):
                raise ValueError(f"unexpected item definition for {label}")
            if model != inventory_model(target):
                raise ValueError(f"unexpected Matcha-structured item model for {label}")
            namespace, resource_path = item["model"]["model"].split(":", 1)
            resolved = f"assets/{namespace}/models/{resource_path}.json"
            if resolved != target["model"] or resolved not in final_entries:
                raise ValueError(f"unresolved item model for {label}")
            if source_entries[target["world_model"]] != final_entries[target["world_model"]]:
                raise ValueError(f"world model changed for {label}")
            if "assets/" + target["bushy"].replace(":", "/textures/") + ".png" not in final_entries:
                raise ValueError(f"missing target bushy texture for {label}")
            if model["textures"]["all"] != target["all"] or model["textures"]["outer_top"] != target["bushy"] or model["textures"]["outer_bottom"] != target["bushy"]:
                raise ValueError(f"target texture chain changed for {label}")
            if target["tints"] is None:
                if "tints" in item["model"] or any("tintindex" in face_data for element in model["elements"] for face_data in element["faces"].values()):
                    raise ValueError(f"unexpected tint for {label}")
            elif item["model"].get("tints") != target["tints"] or any(face_data.get("tintindex") != 0 for element in model["elements"] for face_data in element["faces"].values()):
                raise ValueError(f"Silver Birch tint chain changed")
        return {"sha256": sha256(OUTPUT), "parsed_json_resources": sum(name.endswith((".json", ".mcmeta")) for name in final_entries), "final_paths": len(final_entries)}


def main() -> int:
    if not SOURCE.is_file():
        raise FileNotFoundError(SOURCE)
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(SOURCE) as before, tempfile.TemporaryDirectory(dir=OUTPUT.parent) as temporary:
        staged = Path(temporary) / OUTPUT.name
        with zipfile.ZipFile(staged, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as final:
            for entry in before.infolist():
                if not entry.is_dir():
                    final.writestr(clone(entry), REPLACEMENTS.get(entry.filename, before.read(entry)))
            for name, data in ADDITIONS.items():
                final.writestr(generated_entry(name), data)
        shutil.move(staged, OUTPUT)
    result = validate()
    result["artifact"] = str(OUTPUT)
    result["built_at"] = datetime.now(timezone.utc).isoformat(timespec="microseconds").replace("+00:00", "Z")
    print(json.dumps(result, indent=2, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
