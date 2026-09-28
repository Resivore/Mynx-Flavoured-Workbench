#!/usr/bin/env python3
"""Build and statically validate Foundation v6 from Foundation v5 and Matcha Overlays v37."""
from __future__ import annotations

import hashlib
import json
import shutil
import tempfile
import zipfile
from datetime import datetime, timezone
from pathlib import Path


ROOT = Path(__file__).resolve().parents[3]
FOUNDATION_V5 = ROOT / "resourcepacks" / "foundation" / "artifacts" / "Foundation v5.zip"
MATCHA_V37 = ROOT / "originals" / "resourcepacks" / "Matcha-Overlays-v37.zip"
OUTPUT = ROOT / "resourcepacks" / "foundation" / "artifacts" / "Foundation v6.zip"

FOUNDATION_V5_SHA256 = "70178c497995083e6331d6412395d5b89039e60535877d14cc52cf5cf988ba80"
MATCHA_V37_SHA256 = "2642dcea338100f469df905b212423683e83ae7c683c7b9fabfbd2195a2fe802"
PACK_META = {"pack": {"description": "Foundation v6", "min_format": [88, 0], "max_format": 88}}
CHANGELOG_PATH = "FOUNDATION_V6_CHANGELOG.txt"
CHANGELOG = """Foundation v6

Base pack: exact Foundation v5 retained artifact
Base SHA-256: 70178c497995083e6331d6412395d5b89039e60535877d14cc52cf5cf988ba80
Overlay pack: exact Matcha Overlays v37 immutable source archive
Overlay SHA-256: 2642dcea338100f469df905b212423683e83ae7c683c7b9fabfbd2195a2fe802

Merge policy:
- Includes every Foundation v5 resource and every Matcha Overlays v37 resource.
- On every shared asset/resource path, Matcha Overlays v37 bytes win exactly, matching its higher resource-pack precedence in the prior two-pack stack.
- Foundation v6 keeps Foundation branding at the resource-pack root: pack.mcmeta is updated only to identify Foundation v6 and Foundation v5's pack.png is retained. These are the only shared-path exceptions.
- All non-colliding root attribution, license, credit, note, and provenance files from both inputs are retained unchanged.

Validation:
- Entry-by-entry provenance, collision precedence, JSON parsing, ZIP CRC, and resource-pack root-layout checks run during packaging.
- No Minecraft runtime validation was performed.
""".encode("utf-8")
ROOT_EXCEPTIONS = {"pack.mcmeta", "pack.png"}


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
    result.extract_version, result.flag_bits = entry.flag_bits, entry.flag_bits
    result.volume, result.compress_type = entry.volume, entry.compress_type
    return result


def generated_entry(name: str) -> zipfile.ZipInfo:
    entry = zipfile.ZipInfo(name, (2026, 9, 27, 0, 0, 0))
    entry.compress_type = zipfile.ZIP_DEFLATED
    entry.external_attr = 0o100644 << 16
    return entry


def validate() -> dict[str, object]:
    if sha256(FOUNDATION_V5) != FOUNDATION_V5_SHA256:
        raise ValueError("Foundation v5 source hash does not match its canonical record")
    if sha256(MATCHA_V37) != MATCHA_V37_SHA256:
        raise ValueError("Matcha Overlays v37 source hash changed")

    with zipfile.ZipFile(FOUNDATION_V5) as foundation, zipfile.ZipFile(MATCHA_V37) as matcha, zipfile.ZipFile(OUTPUT) as final:
        if final.testzip() is not None:
            raise ValueError("ZIP CRC validation failed")
        before = entries(foundation)
        overlay = entries(matcha)
        merged = entries(final)
        shared = sorted(set(before) & set(overlay))
        expected_names = set(before) | set(overlay) | {CHANGELOG_PATH}
        if set(merged) != expected_names:
            unexpected = sorted(set(merged) - expected_names)
            missing = sorted(expected_names - set(merged))
            raise ValueError(f"unexpected final path set; unexpected={unexpected}, missing={missing}")
        if any(name.startswith(("/", "\\\\")) or "\\\\" in name or name.startswith("../") for name in merged):
            raise ValueError("invalid archive path")
        if "pack.mcmeta" not in merged or not any(name.startswith("assets/") for name in merged):
            raise ValueError("invalid resource-pack root layout")
        if any(name.startswith("Foundation v6/") for name in merged):
            raise ValueError("resource-pack files are nested below an extra directory")

        for name, data in before.items():
            if name not in overlay and merged[name] != data:
                raise ValueError(f"Foundation v5 non-collision changed: {name}")
        for name, data in overlay.items():
            if name not in ROOT_EXCEPTIONS and merged[name] != data:
                raise ValueError(f"Matcha resource changed: {name}")
        for name in shared:
            if name not in ROOT_EXCEPTIONS and merged[name] != overlay[name]:
                raise ValueError(f"Matcha did not win collision: {name}")
        if json.loads(merged["pack.mcmeta"]) != PACK_META:
            raise ValueError("Foundation v6 pack metadata is incorrect")
        if merged["pack.png"] != before["pack.png"]:
            raise ValueError("Foundation branding icon changed")
        if merged[CHANGELOG_PATH] != CHANGELOG:
            raise ValueError("Foundation v6 changelog changed")

        parsed_json = 0
        for name, data in merged.items():
            if name.endswith(".json") or name.endswith(".mcmeta"):
                json.loads(data)
                parsed_json += 1
        return {
            "foundation_paths": len(before),
            "matcha_paths": len(overlay),
            "foundation_only_paths": len(set(before) - set(overlay)),
            "matcha_only_paths": len(set(overlay) - set(before)),
            "collisions": shared,
            "collision_count": len(shared),
            "root_exceptions": sorted(ROOT_EXCEPTIONS),
            "final_paths": len(merged),
            "parsed_json_resources": parsed_json,
            "sha256": sha256(OUTPUT),
        }


def main() -> int:
    if not FOUNDATION_V5.is_file() or not MATCHA_V37.is_file():
        raise FileNotFoundError("required Foundation v5 or Matcha Overlays v37 archive is missing")
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(FOUNDATION_V5) as foundation, zipfile.ZipFile(MATCHA_V37) as matcha:
        foundation_entries = entries(foundation)
        matcha_entries = entries(matcha)
        with tempfile.TemporaryDirectory(dir=OUTPUT.parent) as temporary:
            staged = Path(temporary) / OUTPUT.name
            with zipfile.ZipFile(staged, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as final:
                for entry in foundation.infolist():
                    if entry.is_dir():
                        continue
                    name = entry.filename
                    if name == "pack.mcmeta":
                        final.writestr(clone(entry), json.dumps(PACK_META, indent=2).encode("utf-8") + b"\n")
                    elif name in matcha_entries and name not in ROOT_EXCEPTIONS:
                        final.writestr(clone(entry), matcha_entries[name])
                    else:
                        final.writestr(clone(entry), foundation_entries[name])
                for entry in matcha.infolist():
                    if not entry.is_dir() and entry.filename not in foundation_entries:
                        final.writestr(clone(entry), matcha_entries[entry.filename])
                final.writestr(generated_entry(CHANGELOG_PATH), CHANGELOG)
            shutil.move(staged, OUTPUT)
    result = validate()
    result["artifact"] = str(OUTPUT)
    result["built_at"] = datetime.now(timezone.utc).isoformat(timespec="microseconds").replace("+00:00", "Z")
    print(json.dumps(result, indent=2, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
