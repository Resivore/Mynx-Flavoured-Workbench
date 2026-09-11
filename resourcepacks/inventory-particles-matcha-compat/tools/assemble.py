"""Build C2 from declarative resources and the immutable audited upstream JAR."""
from __future__ import annotations

import hashlib
import json
import subprocess
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "artifacts/inventory-particles-matcha-compat-c2.zip"
CUSTOM_MODELS = {"minecraft:green_curry", "minecraft:ramen", "minecraft:heart_container"}
OVERRIDES = (
    "assets/inventory_particles/iparticles/vanilla/generic/sand/dirt_sand.json",
    "assets/inventory_particles/iparticles/vanilla/generic/sand/dirt_sand_additional_small.json",
    "assets/inventory_particles/iparticles/vanilla/potion/potion.json",
    "assets/inventory_particles/iparticles/vanilla/potion/potion_additional_small.json",
    "assets/inventory_particles/ifamilies/fallback/standard.json",
)


def canonical_root() -> Path:
    """Use the primary checkout's ignored immutable-reference library in a worktree."""
    result = subprocess.run(
        ["git", "rev-parse", "--path-format=absolute", "--git-common-dir"],
        cwd=ROOT,
        check=True,
        capture_output=True,
        text=True,
    )
    return Path(result.stdout.strip()).resolve().parent


UPSTREAM = canonical_root() / "originals/mods/InventoryParticles-2.6.0+26.2+fabric.jar"


def model_predicate(models: set[str]) -> dict:
    return {"this_name": "components", "this_type": "object", "next": {"this_name": "minecraft:item_model", "this_type": "string", "check_value": sorted(models)}}


def village_map_predicate() -> dict:
    return {
        "this_name": "components",
        "this_type": "object",
        "next": {
            "this_name": "minecraft:custom_data",
            "this_type": "object",
            "next": {
                "this_name": "ribbits:ribbit_village_explorer_map",
                "this_type": "number",
                "check_value": "1",
            },
        },
    }

def patched_upstream(name: str) -> bytes:
    with zipfile.ZipFile(UPSTREAM) as source:
        data = json.loads(source.read(name))
    if "/iparticles/" in name:
        for holder in data["holders"]:
            if holder.get("item") == "minecraft:poisonous_potato":
                holder["nbt_conditions_match"] = "none"
                holder["nbt_conditions"] = [model_predicate(CUSTOM_MODELS)]
    else:
        # Family fallback is accumulated in addition to literal holders.  Exclude
        # only the two literal Ribbits IDs and the exact successful explorer-map
        # component marker; all other automatic/fallback behavior remains intact.
        data["keywords"] = {"blacklist": ["@glowcap", "@toadstool_heart"], "whitelist": []}
        particle = data["particles"][0]
        particle["nbt_conditions_match"] = "none"
        particle["nbt_conditions"] = [village_map_predicate()]
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
