from __future__ import annotations

import json
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ARTIFACT = ROOT / "artifacts/inventory-particles-matcha-compat-c1.zip"
MODELS = {"minecraft:green_curry", "minecraft:ramen", "minecraft:heart_container"}

def predicate_values(holder: dict) -> set[str]:
    node = holder["nbt_conditions"][0]
    value = node["next"]["check_value"]
    return set(value if isinstance(value, list) else [value])

def main() -> None:
    assert ARTIFACT.is_file(), "assemble C1 first"
    with zipfile.ZipFile(ARTIFACT) as archive:
        names = set(archive.namelist())
        assert "pack.mcmeta" in names
        for name in names:
            if name.endswith(".json"):
                json.loads(archive.read(name))
        for name in ("assets/inventory_particles/iparticles/vanilla/generic/sand/dirt_sand.json", "assets/inventory_particles/iparticles/vanilla/potion/potion.json"):
            configs = json.loads(archive.read(name))
            potato = next(x for x in configs["holders"] if x.get("item") == "minecraft:poisonous_potato")
            assert potato["nbt_conditions_match"] == "none"
            assert predicate_values(potato) == MODELS
        custom = json.loads(archive.read("assets/inventory_particles/iparticles/mynx/matcha_components.json"))
        assert {predicate_values(x).pop() for x in custom["holders"]} == {"minecraft:green_curry", "minecraft:ramen"}
        ribbits = json.loads(archive.read("assets/inventory_particles/iparticles/mynx/ribbits.json"))
        assert {x["item"] for x in ribbits["holders"]} == {"ribbits:glowcap", "ribbits:toadstool_heart", "ribbits:ribbit_village_explorer_map"}
    print("PASS: C1 JSON, selector routing, upstream suppression, and archive structure")

if __name__ == "__main__":
    main()
