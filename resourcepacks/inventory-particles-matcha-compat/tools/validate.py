"""Focused C3 final-ZIP validator using the exact Inventory Particles 2.6.0 schema."""
from __future__ import annotations

import hashlib
import json
import subprocess
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ARTIFACT = ROOT / "artifacts/inventory-particles-matcha-compat-c3.zip"
MODELS = {"minecraft:green_curry", "minecraft:ramen", "minecraft:heart_container"}
POTATO_OVERRIDES = {
    "assets/inventory_particles/iparticles/vanilla/generic/sand/dirt_sand.json",
    "assets/inventory_particles/iparticles/vanilla/generic/sand/dirt_sand_additional_small.json",
    "assets/inventory_particles/iparticles/vanilla/potion/potion.json",
    "assets/inventory_particles/iparticles/vanilla/potion/potion_additional_small.json",
}
FALLBACK = "assets/inventory_particles/ifamilies/fallback/standard.json"
UPSTREAM_SIZE = 1_783_277
UPSTREAM_SHA256 = "b44d808e673805eea4949591abcb3325faa1abf8b9321266d06533ea5a34c802"
NODE_TYPE_CLASS = "net/lopymine/ip/element/predicate/nbt/NbtNodeType.class"
NODE_MATCH_CLASS = "net/lopymine/ip/element/predicate/nbt/NbtNodeMatch.class"
PREDICATE_CLASS = "net/lopymine/ip/element/predicate/nbt/NbtSpawnPredicate.class"
EXPECTED_NODE_TYPES = {"object", "string", "list", "int"}
EXPECTED_NODE_MATCHES = {"any", "all", "none"}


def canonical_root() -> Path:
    result = subprocess.run(
        ["git", "rev-parse", "--path-format=absolute", "--git-common-dir"],
        cwd=ROOT,
        check=True,
        capture_output=True,
        text=True,
    )
    return Path(result.stdout.strip()).resolve().parent


UPSTREAM = canonical_root() / "originals/mods/InventoryParticles-2.6.0+26.2+fabric.jar"


def u1(data: bytes, offset: int) -> tuple[int, int]:
    return data[offset], offset + 1


def u2(data: bytes, offset: int) -> tuple[int, int]:
    return int.from_bytes(data[offset:offset + 2], "big"), offset + 2


def u4(data: bytes, offset: int) -> tuple[int, int]:
    return int.from_bytes(data[offset:offset + 4], "big"), offset + 4


def enum_values(class_file: bytes) -> set[str]:
    """Read ACC_ENUM field names directly from a JVM class file."""
    assert class_file[:4] == b"\xca\xfe\xba\xbe", "not a Java class"
    offset = 8
    count, offset = u2(class_file, offset)
    constant_pool: list[object | None] = [None] * count
    index = 1
    while index < count:
        tag, offset = u1(class_file, offset)
        if tag == 1:  # CONSTANT_Utf8
            length, offset = u2(class_file, offset)
            constant_pool[index] = class_file[offset:offset + length].decode("utf-8")
            offset += length
        elif tag in {3, 4}:  # Integer, Float
            offset += 4
        elif tag in {5, 6}:  # Long, Double; consume two constant-pool slots.
            offset += 8
            index += 1
        elif tag in {7, 8, 16, 19, 20}:  # One u2 index.
            offset += 2
        elif tag in {9, 10, 11, 12, 17, 18}:  # Two u2 indexes.
            offset += 4
        elif tag == 15:  # MethodHandle
            offset += 3
        else:
            raise AssertionError(f"unsupported class-file constant-pool tag {tag}")
        index += 1

    _, offset = u2(class_file, offset)  # class access flags
    _, offset = u2(class_file, offset)  # this_class
    _, offset = u2(class_file, offset)  # super_class
    interfaces, offset = u2(class_file, offset)
    offset += interfaces * 2
    fields, offset = u2(class_file, offset)
    values: set[str] = set()
    for _ in range(fields):
        access, offset = u2(class_file, offset)
        name_index, offset = u2(class_file, offset)
        _, offset = u2(class_file, offset)  # descriptor index
        attributes, offset = u2(class_file, offset)
        if access & 0x4000:  # ACC_ENUM
            name = constant_pool[name_index]
            assert isinstance(name, str)
            values.add(name.lower())
        for _ in range(attributes):
            _, offset = u2(class_file, offset)
            length, offset = u4(class_file, offset)
            offset += length
    return values


def production_predicate_schema(upstream: zipfile.ZipFile) -> tuple[set[str], set[str]]:
    """Bind validation to the audited JAR's production NbtNode codecs."""
    assert UPSTREAM.stat().st_size == UPSTREAM_SIZE
    assert hashlib.sha256(UPSTREAM.read_bytes()).hexdigest() == UPSTREAM_SHA256
    node_types = enum_values(upstream.read(NODE_TYPE_CLASS))
    node_matches = enum_values(upstream.read(NODE_MATCH_CLASS))
    assert node_types == EXPECTED_NODE_TYPES, node_types
    assert node_matches == EXPECTED_NODE_MATCHES, node_matches
    assert "number" not in node_types

    # NbtSpawnPredicate's INT case accepts NumericTag and reads it with asInt;
    # this proves Ribbits' boolean ByteTag marker can use the production int
    # schema value without inventing a separate numeric element name.
    predicate = upstream.read(PREDICATE_CLASS)
    assert b"net/minecraft/nbt/NumericTag" in predicate
    assert b"asInt" in predicate
    return node_types, node_matches


def validate_node(node: object, node_types: set[str], node_matches: set[str]) -> None:
    assert isinstance(node, dict), f"predicate node is not an object: {node!r}"
    assert set(node) <= {"this_name", "this_type", "check_value", "next_match", "next"}
    # The production codec is Codec.STRING, so an empty name is valid for a
    # node that traverses an anonymous entry inside an NBT list.
    assert isinstance(node.get("this_name"), str)
    assert node.get("this_type") in node_types, node
    if "check_value" in node:
        check_value = node["check_value"]
        if isinstance(check_value, list):
            assert check_value and all(isinstance(value, str) for value in check_value)
        else:
            assert isinstance(check_value, str)
    if "next_match" in node:
        assert node["next_match"] in node_matches
    if "next" in node:
        next_nodes = node["next"]
        if isinstance(next_nodes, list):
            assert next_nodes
            for next_node in next_nodes:
                validate_node(next_node, node_types, node_matches)
        else:
            validate_node(next_nodes, node_types, node_matches)


def validate_config_predicates(name: str, config: object, node_types: set[str], node_matches: set[str]) -> None:
    assert isinstance(config, dict), f"{name} root is not an object"
    if "/iparticles/" in name:
        entries = config.get("holders")
    elif "/ifamilies/" in name:
        entries = config.get("particles")
    else:
        raise AssertionError(f"unexpected Inventory Particles resource path {name}")
    assert isinstance(entries, list) and entries, f"{name} has no predicate entries"
    for entry in entries:
        assert isinstance(entry, dict), f"{name} has a non-object predicate entry"
        assert entry.get("nbt_conditions_match") in node_matches, f"{name}: {entry!r}"
        nodes = entry.get("nbt_conditions")
        assert isinstance(nodes, list), f"{name}: nbt_conditions is not a list"
        for node in nodes:
            validate_node(node, node_types, node_matches)


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
    assert node == {
        "this_name": "ribbits:ribbit_village_explorer_map",
        "this_type": "int",
        "check_value": "1",
    }
    return holder["nbt_conditions"][0]


def main() -> None:
    assert UPSTREAM.is_file(), f"missing immutable audited input: {UPSTREAM}"
    assert ARTIFACT.is_file(), "assemble C3 first"
    with zipfile.ZipFile(UPSTREAM) as upstream, zipfile.ZipFile(ARTIFACT) as archive:
        node_types, node_matches = production_predicate_schema(upstream)
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
        assert json.loads(archive.read("pack.mcmeta"))["pack"]["description"].endswith("C3")
        for name in names:
            if name.endswith(".json") and name != "pack.mcmeta":
                validate_config_predicates(name, json.loads(archive.read(name)), node_types, node_matches)

        for name in POTATO_OVERRIDES:
            patched = json.loads(archive.read(name))
            original = json.loads(upstream.read(name))
            expected_patched = json.loads(json.dumps(original))
            for candidate in expected_patched["holders"]:
                if candidate.get("item") == "minecraft:poisonous_potato":
                    candidate["nbt_conditions_match"] = "none"
                    candidate["nbt_conditions"] = [{
                        "this_name": "components",
                        "this_type": "object",
                        "next": {
                            "this_name": "minecraft:item_model",
                            "this_type": "string",
                            "check_value": sorted(MODELS),
                        },
                    }]
            assert patched == expected_patched, f"unexpected non-potato change in {name}"
            potato = next(x for x in patched["holders"] if x.get("item") == "minecraft:poisonous_potato")
            assert potato["nbt_conditions_match"] == "none" and models(potato) == MODELS

        replacements = set()
        for name in (
            "assets/inventory_particles/iparticles/mynx/matcha_components.json",
            "assets/inventory_particles/iparticles/mynx/matcha_crystal_heart.json",
        ):
            for holder in json.loads(archive.read(name))["holders"]:
                assert holder["item"] == "minecraft:poisonous_potato"
                assert holder["nbt_conditions_match"] == "all"
                replacements.update(models(holder))
        assert replacements == MODELS

        for filename, item, texture in (
            ("ribbits_glowcap.json", "ribbits:glowcap", "ribbits:item/glowcap.png"),
            ("ribbits_toadstool_heart.json", "ribbits:toadstool_heart", "ribbits:item/toadstool_heart.png"),
        ):
            config = json.loads(archive.read(f"assets/inventory_particles/iparticles/mynx/{filename}"))
            assert config["textures"] == [texture] and [holder["item"] for holder in config["holders"]] == [item]

        village = json.loads(archive.read("assets/inventory_particles/iparticles/mynx/ribbits_village_map.json"))
        holder = village["holders"][0]
        assert village["textures"] == ["ribbits:item/ribbit_village_explorer_map.png"]
        assert holder["item"] == "minecraft:filled_map" and holder["nbt_conditions_match"] == "all"
        map_node = map_predicate(holder)

        fallback = json.loads(archive.read(FALLBACK))
        expected_fallback = json.loads(upstream.read(FALLBACK))
        expected_fallback["keywords"] = {"blacklist": ["@glowcap", "@toadstool_heart"], "whitelist": []}
        expected_fallback["particles"][0]["nbt_conditions_match"] = "none"
        expected_fallback["particles"][0]["nbt_conditions"] = [map_node]
        assert fallback == expected_fallback, "fallback override changed unrelated behavior"
    print(
        "PASS: C3 final ZIP uses the exact 2.6.0 production NbtNode schema "
        f"(types={sorted(node_types)}, matches={sorted(node_matches)}), rejects number, "
        "preserves C2 routing, and narrowly patches the fallback."
    )


if __name__ == "__main__":
    main()
