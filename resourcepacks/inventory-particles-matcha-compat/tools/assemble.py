"""Build C1 from declarative resources and the immutable audited upstream JAR."""
from __future__ import annotations

import hashlib
import json
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
UPSTREAM = ROOT.parents[1] / "originals/mods/InventoryParticles-2.6.0+26.2+fabric.jar"
OUTPUT = ROOT / "artifacts/inventory-particles-matcha-compat-c1.zip"
CUSTOM_MODELS = {"minecraft:green_curry", "minecraft:ramen", "minecraft:heart_container"}
OVERRIDES = (
    "assets/inventory_particles/iparticles/vanilla/generic/sand/dirt_sand.json",
    "assets/inventory_particles/iparticles/vanilla/potion/potion.json",
)

def model_predicate(models: set[str]) -> dict:
    return {"this_name": "components", "this_type": "object", "next": {"this_name": "minecraft:item_model", "this_type": "string", "check_value": sorted(models)}}

def patched_upstream(name: str) -> bytes:
    with zipfile.ZipFile(UPSTREAM) as source:
        data = json.loads(source.read(name))
    for holder in data["holders"]:
        if holder.get("item") == "minecraft:poisonous_potato":
            holder["nbt_conditions_match"] = "none"
            holder["nbt_conditions"] = [model_predicate(CUSTOM_MODELS)]
    return (json.dumps(data, separators=(",", ":"), ensure_ascii=False) + "\n").encode()

def build() -> str:
    if not UPSTREAM.is_file():
        raise SystemExit(f"Missing immutable audited input: {UPSTREAM}")
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    entries: dict[str, bytes] = {"pack.mcmeta": (ROOT / "pack.mcmeta").read_bytes()}
    for path in (ROOT / "source").rglob("*.json"):
        entries[path.relative_to(ROOT / "source").as_posix()] = path.read_bytes()
    for name in OVERRIDES:
        entries[name] = patched_upstream(name)
    with zipfile.ZipFile(OUTPUT, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as archive:
        for name in sorted(entries):
            info = zipfile.ZipInfo(name, (2026, 9, 11, 0, 0, 0))
            info.compress_type = zipfile.ZIP_DEFLATED
            archive.writestr(info, entries[name])
    return hashlib.sha256(OUTPUT.read_bytes()).hexdigest()

if __name__ == "__main__":
    print(build())
