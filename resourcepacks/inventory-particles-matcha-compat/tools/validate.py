"""Focused C2 contract check, including the exact upstream resources it overrides."""
from __future__ import annotations

import json
import subprocess
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ARTIFACT = ROOT / "artifacts/inventory-particles-matcha-compat-c2.zip"
MODELS = {"minecraft:green_curry", "minecraft:ramen", "minecraft:heart_container"}
POTATO_OVERRIDES = {
    "assets/inventory_particles/iparticles/vanilla/generic/sand/dirt_sand.json",
    "assets/inventory_particles/iparticles/vanilla/generic/sand/dirt_sand_additional_small.json",
    "assets/inventory_particles/iparticles/vanilla/potion/potion.json",
    "assets/inventory_particles/iparticles/vanilla/potion/potion_additional_small.json",
}
FALLBACK = "assets/inventory_particles/ifamilies/fallback/standard.json"


def canonical_root() -> Path:
    result = subprocess.run(["git", "rev-parse", "--path-format=absolute", "--git-common-dir"], cwd=ROOT, check=True, capture_output=True, text=True)
    return Path(result.stdout.strip()).resolve().parent


UPSTREAM = canonical_root() / "originals/mods/InventoryParticles-2.6.0+26.2+fabric.jar"


def models(holder: dict) -> set[str]:
    node = holder["nbt_conditions"][0]
    assert node["this_name"] == "components" and node["this_type"] == "object"
    node = node["next"]
    assert node["this_name"] == "minecraft:item_model" and node["this_type"] == "string"
    value = node["check_value"]
    return set(value if isinstance(value, list) else [value])


def map_predicate(holder: dict) -> dict:
    node = holder["nbt_conditions"][0]
    assert node["this_name"] == "components" and node["this_type"] == "object"
    node = node["next"]
    assert node["this_name"] == "minecraft:custom_data" and node["this_type"] == "object"
    node = node["next"]
    assert node == {"this_name": "ribbits:ribbit_village_explorer_map", "this_type": "number", "check_value": "1"}
    return holder["nbt_conditions"][0]


def matches(node: dict, stack: dict) -> bool:
    value = stack.get(node["this_name"])
    if value is None:
        return False
    if node["this_type"] == "object":
        return isinstance(value, dict) and matches(node["next"], value)
    if node["this_type"] == "string":
        choices = node["check_value"]
        return isinstance(value, str) and value in (choices if isinstance(choices, list) else [choices])
    if node["this_type"] == "number":
        return isinstance(value, int) and str(value) == node["check_value"]
    raise AssertionError(f"unsupported node type: {node['this_type']}")


def main() -> None:
    assert UPSTREAM.is_file(), f"missing immutable audited input: {UPSTREAM}"
    assert ARTIFACT.is_file(), "assemble C2 first"
    with zipfile.ZipFile(UPSTREAM) as upstream, zipfile.ZipFile(ARTIFACT) as archive:
        names = archive.namelist()
        expected = {
            "pack.mcmeta", *POTATO_OVERRIDES, FALLBACK,
            "assets/inventory_particles/iparticles/mynx/matcha_components.json",
            "assets/inventory_particles/iparticles/mynx/matcha_crystal_heart.json",
            "assets/inventory_particles/iparticles/mynx/ribbits_glowcap.json",
            "assets/inventory_particles/iparticles/mynx/ribbits_toadstool_heart.json",
            "assets/inventory_particles/iparticles/mynx/ribbits_village_map.json",
        }
        assert len(names) == len(set(names)) and set(names) == expected
        for name in names:
            if name.endswith(".json"):
                json.loads(archive.read(name))

        for name in POTATO_OVERRIDES:
            patched = json.loads(archive.read(name))
            original = json.loads(upstream.read(name))
            expected_patched = json.loads(json.dumps(original))
            for candidate in expected_patched["holders"]:
                if candidate.get("item") == "minecraft:poisonous_potato":
                    candidate["nbt_conditions_match"] = "none"
                    candidate["nbt_conditions"] = [{"this_name": "components", "this_type": "object", "next": {"this_name": "minecraft:item_model", "this_type": "string", "check_value": sorted(MODELS)}}]
            assert patched == expected_patched, f"unexpected non-potato change in {name}"
            potato = next(x for x in patched["holders"] if x.get("item") == "minecraft:poisonous_potato")
            assert potato["nbt_conditions_match"] == "none" and models(potato) == MODELS
            assert not matches(potato["nbt_conditions"][0], {"components": {}})
            for model in MODELS:
                assert matches(potato["nbt_conditions"][0], {"components": {"minecraft:item_model": model}})

        replacements = set()
        for name in ("assets/inventory_particles/iparticles/mynx/matcha_components.json", "assets/inventory_particles/iparticles/mynx/matcha_crystal_heart.json"):
            for holder in json.loads(archive.read(name))["holders"]:
                assert holder["item"] == "minecraft:poisonous_potato" and holder["nbt_conditions_match"] == "all"
                replacements.update(models(holder))
        assert replacements == MODELS

        for filename, item, texture in (
            ("ribbits_glowcap.json", "ribbits:glowcap", "ribbits:item/glowcap.png"),
            ("ribbits_toadstool_heart.json", "ribbits:toadstool_heart", "ribbits:item/toadstool_heart.png"),
        ):
            config = json.loads(archive.read(f"assets/inventory_particles/iparticles/mynx/{filename}"))
            assert config["textures"] == [texture] and [h["item"] for h in config["holders"]] == [item]
        village = json.loads(archive.read("assets/inventory_particles/iparticles/mynx/ribbits_village_map.json"))
        holder = village["holders"][0]
        assert village["textures"] == ["ribbits:item/ribbit_village_explorer_map.png"]
        assert holder["item"] == "minecraft:filled_map" and holder["nbt_conditions_match"] == "all"
        map_node = map_predicate(holder)
        assert matches(map_node, {"components": {"minecraft:custom_data": {"ribbits:ribbit_village_explorer_map": 1}}})
        assert not matches(map_node, {"components": {"minecraft:custom_data": {}}})

        fallback = json.loads(archive.read(FALLBACK))
        expected_fallback = json.loads(upstream.read(FALLBACK))
        expected_fallback["keywords"] = {"blacklist": ["@glowcap", "@toadstool_heart"], "whitelist": []}
        expected_fallback["particles"][0]["nbt_conditions_match"] = "none"
        expected_fallback["particles"][0]["nbt_conditions"] = [map_node]
        assert fallback == expected_fallback, "fallback override changed unrelated behavior"
    print("PASS: C2 upstream guards, exact Matcha/map identities, isolated Ribbits textures, fallback narrowing, and ZIP structure")


if __name__ == "__main__":
    main()
