#!/usr/bin/env python3
"""Source-safe regression tests for the tracked private-resource assembler."""

from __future__ import annotations

import copy
import json
import struct
import tempfile
import unittest
import zipfile
from pathlib import Path, PurePosixPath
from unittest import mock

import private_resource_tools as tools


NBT_SENTINEL_BEFORE = ("unrelated_before", "sentinel-before-value")
NBT_SENTINEL_AFTER = ("unrelated_after", "sentinel-after-value")


def synthetic_legacy_document(index: int = 0) -> dict[str, object]:
    """Return vanilla-only synthetic data, never reconstructed Ribbits resource content."""
    return {
        "type": tools.LEGACY_RANDOM_PATCH_FEATURE_TYPE,
        "config": {
            "feature": {
                "feature": {
                    "type": tools.RIBBITS_VEGETATION_FEATURE_TYPE,
                    "config": {
                        "on_solid_state_provider": {
                            "type": "minecraft:simple_state_provider",
                            "state": {"Name": "minecraft:stone"},
                        }
                    },
                },
                "placement": [copy.deepcopy(tools.LEGACY_RANDOM_PATCH_PLACEMENT)],
            },
            "tries": 20 + index,
            "xz_spread": 1 + index,
            "y_spread": index % 3,
        },
    }


def synthetic_documents() -> list[tuple[str, object]]:
    return [
        (path, synthetic_legacy_document(index))
        for index, path in enumerate(tools.CONFIGURED_FEATURE_MIGRATION_PATHS)
    ]


def write_documents(root: Path, documents: list[tuple[str, object]]) -> None:
    for relative, value in documents:
        path = root.joinpath(*PurePosixPath(relative).parts)
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps(value, indent=2) + "\n", encoding="utf-8", newline="\n")


def synthetic_loot(relative: str) -> dict[str, object]:
    counts = tools.VILLAGE_LOOT_POOL_ENTRY_COUNTS[relative]
    pools: list[dict[str, object]] = []
    for pool_index, count in enumerate(counts):
        entries = [
            {"type": "minecraft:item", "weight": 5, "name": "minecraft:stone"}
            for _ in range(count)
        ]
        if relative in tools.LOOT_TABLE_PATHS and pool_index == 1:
            entries[0] = {
                "type": "minecraft:item",
                "weight": tools.LOOT_EMPTY_WEIGHTS[relative],
                "name": "minecraft:air",
            }
        pools.append({"rolls": 1.0, "bonus_rolls": 0.0, "entries": entries})
    result = {"type": "minecraft:chest", "pools": pools}
    currency_spec = tools.GLOWCAP_CURRENCY_ENTRY_SPECS.get(relative)
    if currency_spec is not None:
        result["pools"][currency_spec["pool"]]["entries"][currency_spec["entry"]] = (
            tools.expected_glowcap_currency_entry(
                currency_spec, tools.GLOWCAP_CURRENCY_SOURCE_ID
            )
        )
    block_spec = tools.UNRELATED_AMETHYST_BLOCK_SPEC
    if relative == block_spec["table"]:
        result["pools"][block_spec["pool"]]["entries"][block_spec["entry"]] = copy.deepcopy(
            block_spec["value"]
        )
    return result


def synthetic_model(width: int = 64, uv: list[int] | None = None) -> dict[str, object]:
    return {
        "format_version": "1.12.0",
        "minecraft:geometry": [
            {
                "description": {
                    "identifier": "geometry.synthetic",
                    "texture_width": width,
                    "texture_height": width,
                    "visible_bounds_width": 2,
                    "visible_bounds_height": 2.5,
                    "visible_bounds_offset": [0, 0.75, 0],
                },
                "bones": [
                    {"name": "main", "pivot": [0, 0, 0]},
                    {
                        "name": "body",
                        "parent": "main",
                        "pivot": [0, 0, 0],
                        "cubes": [
                            {
                                "origin": [0, 0, 0],
                                "size": [1, 1, 1],
                                "uv": [0, 0] if uv is None else uv,
                            }
                        ],
                    },
                ],
            }
        ],
    }


def synthetic_umbrella_model(variant: int) -> dict[str, object]:
    result = synthetic_model(128)
    geometry = result["minecraft:geometry"][0]
    geometry["description"]["identifier"] = f"geometry.umbrella_ribbit_{variant}"
    geometry["description"]["visible_bounds_width"] = 3
    name = "umbrella" if variant == 1 else f"umbrella{variant}"
    geometry["bones"].append(
        {
            "name": name,
            "parent": "body",
            "pivot": [0, 0, 0],
            "cubes": [{"origin": [0, 0, 0], "size": [2, 2, 2], "uv": [4, 51]}],
        }
    )
    return result


def nbt_string(value: str) -> bytes:
    encoded = value.encode("utf-8")
    return struct.pack(">H", len(encoded)) + encoded


def named_string(name: str, value: str) -> bytes:
    return b"\x08" + nbt_string(name) + nbt_string(value)


def synthetic_resident_nbt(values: dict[str, str]) -> bytes:
    ribbit_data = b"".join(named_string(name, value) for name, value in values.items()) + b"\x00"
    entity_nbt = (
        named_string("id", "ribbits:ribbit")
        + named_string(*NBT_SENTINEL_BEFORE)
        + b"\x0a"
        + nbt_string("RibbitData")
        + ribbit_data
        + named_string(*NBT_SENTINEL_AFTER)
        + b"\x00"
    )
    entity = b"\x0a" + nbt_string("nbt") + entity_nbt + b"\x00"
    root = (
        b"\x0a"
        + nbt_string("")
        + b"\x09"
        + nbt_string("entities")
        + b"\x0a"
        + struct.pack(">i", 1)
        + entity
        + b"\x00"
    )
    return tools.deterministic_gzip(root)


class ConfiguredFeatureMigrationTest(unittest.TestCase):
    def test_converts_all_five_and_preserves_inline_feature_and_predicate(self) -> None:
        documents = synthetic_documents()
        original = copy.deepcopy(dict(documents))

        migrated, record = tools.migrate_configured_feature_documents(documents)

        self.assertEqual(
            {
                "count": 5,
                "from_feature_type": "minecraft:random_patch",
                "to_feature_type": "minecraft:sequence",
                "targets": [
                    "ribbits:giant_lilypad_patch",
                    "ribbits:swamp_daisy_patch",
                    "ribbits:toadstool_patch",
                    "ribbits:umbrella_leaf_patch",
                    "ribbits:veg_patch",
                ],
            },
            record,
        )
        self.assertEqual(set(tools.CONFIGURED_FEATURE_MIGRATION_PATHS), set(migrated))
        tools.validate_migrated_configured_feature_documents(list(migrated.items()))

        for path in tools.CONFIGURED_FEATURE_MIGRATION_PATHS:
            legacy_config = original[path]["config"]
            output = migrated[path]
            self.assertEqual("minecraft:sequence", output["type"])
            self.assertNotIn("minecraft:random_patch", json.dumps(output))
            placed = output["config"]["features"][0]
            self.assertEqual(legacy_config["feature"]["feature"], placed["feature"])
            self.assertEqual(
                ["minecraft:count", "minecraft:random_offset", "minecraft:block_predicate_filter"],
                [placement["type"] for placement in placed["placement"]],
            )
            self.assertEqual(legacy_config["tries"], placed["placement"][0]["count"])
            offset = placed["placement"][1]
            self.assertEqual(
                {
                    "type": "minecraft:trapezoid",
                    "min": -legacy_config["xz_spread"],
                    "max": legacy_config["xz_spread"],
                    "plateau": 0,
                },
                offset["xz_spread"],
            )
            self.assertEqual(
                {
                    "type": "minecraft:trapezoid",
                    "min": -legacy_config["y_spread"],
                    "max": legacy_config["y_spread"],
                    "plateau": 0,
                },
                offset["y_spread"],
            )
            self.assertEqual(legacy_config["feature"]["placement"][0], placed["placement"][2])

        self.assertEqual(original, dict(documents), "the pure transform must not mutate its input")

    def test_missing_target_fails_closed(self) -> None:
        with self.assertRaisesRegex(tools.ValidationError, "missing="):
            tools.migrate_configured_feature_documents(synthetic_documents()[:-1])

    def test_exact_count_codec_upper_bound_is_accepted(self) -> None:
        documents = synthetic_documents()
        documents[0][1]["config"]["tries"] = 4096
        migrated, _ = tools.migrate_configured_feature_documents(documents)
        first = tools.CONFIGURED_FEATURE_MIGRATION_PATHS[0]
        self.assertEqual(4096, migrated[first]["config"]["features"][0]["placement"][0]["count"])

    def test_duplicate_target_fails_closed(self) -> None:
        documents = synthetic_documents()
        documents.append(copy.deepcopy(documents[0]))
        with self.assertRaisesRegex(tools.ValidationError, "duplicate configured-feature targets"):
            tools.migrate_configured_feature_documents(documents)

    def test_unexpected_extra_target_fails_closed(self) -> None:
        documents = synthetic_documents()
        documents.append(
            (
                "data/ribbits/worldgen/configured_feature/unexpected_patch.json",
                synthetic_legacy_document(),
            )
        )
        with self.assertRaisesRegex(tools.ValidationError, "unexpected="):
            tools.migrate_configured_feature_documents(documents)

    def test_stale_type_anywhere_in_migrated_output_fails_closed(self) -> None:
        migrated, _ = tools.migrate_configured_feature_documents(synthetic_documents())
        first = tools.CONFIGURED_FEATURE_MIGRATION_PATHS[0]
        provider = migrated[first]["config"]["features"][0]["feature"]["config"][
            "on_solid_state_provider"
        ]
        provider["stale_marker"] = tools.LEGACY_RANDOM_PATCH_FEATURE_TYPE

        with self.assertRaisesRegex(tools.ValidationError, "still contains minecraft:random_patch"):
            tools.validate_migrated_configured_feature_documents(list(migrated.items()))

    def test_duplicate_sequence_member_fails_closed(self) -> None:
        migrated, _ = tools.migrate_configured_feature_documents(synthetic_documents())
        first = tools.CONFIGURED_FEATURE_MIGRATION_PATHS[0]
        features = migrated[first]["config"]["features"]
        features.append(copy.deepcopy(features[0]))

        with self.assertRaisesRegex(tools.ValidationError, "exactly one placed feature"):
            tools.validate_migrated_configured_feature_documents(list(migrated.items()))

    def test_unexpected_legacy_structures_fail_closed(self) -> None:
        mutations = {
            "extra top-level field": lambda value: value.update({"unexpected": True}),
            "missing tries": lambda value: value["config"].pop("tries"),
            "boolean tries": lambda value: value["config"].update({"tries": True}),
            "out-of-range tries": lambda value: value["config"].update({"tries": 4097}),
            "out-of-range spread": lambda value: value["config"].update({"xz_spread": 17}),
            "wrong inline feature": lambda value: value["config"]["feature"]["feature"].update(
                {"type": "minecraft:simple_block"}
            ),
            "extra placed-feature field": lambda value: value["config"]["feature"].update(
                {"unexpected": []}
            ),
            "unexpected predicate": lambda value: value["config"]["feature"].update(
                {"placement": [{"type": "minecraft:in_square"}]}
            ),
        }
        for label, mutate in mutations.items():
            with self.subTest(label=label):
                documents = synthetic_documents()
                mutate(documents[0][1])
                with self.assertRaises(tools.ValidationError):
                    tools.migrate_configured_feature_documents(documents)

    def test_partially_migrated_tree_is_rejected_without_rewriting_other_targets(self) -> None:
        documents = synthetic_documents()
        migrated, _ = tools.migrate_configured_feature_documents(copy.deepcopy(documents))
        documents[2] = (documents[2][0], migrated[documents[2][0]])

        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            write_documents(root, documents)
            before = {
                path: root.joinpath(*PurePosixPath(path).parts).read_bytes()
                for path in tools.CONFIGURED_FEATURE_MIGRATION_PATHS
            }

            with self.assertRaisesRegex(tools.ValidationError, "must use pristine feature type"):
                tools.migrate_configured_features(root)

            after = {
                path: root.joinpath(*PurePosixPath(path).parts).read_bytes()
                for path in tools.CONFIGURED_FEATURE_MIGRATION_PATHS
            }
            self.assertEqual(before, after)

    def test_nonlegacy_input_is_not_silently_accepted(self) -> None:
        documents = synthetic_documents()
        documents[0][1]["type"] = "minecraft:simple_block"
        with self.assertRaisesRegex(tools.ValidationError, "must use pristine feature type"):
            tools.migrate_configured_feature_documents(documents)


class LootTableRepairTest(unittest.TestCase):
    def test_exact_air_item_entry_becomes_weighted_empty_without_other_drift(self) -> None:
        for relative in tools.LOOT_TABLE_PATHS:
            with self.subTest(relative=relative):
                source = synthetic_loot(relative)
                before = copy.deepcopy(source)
                repaired = tools.repair_air_loot_entry(source, relative)

                expected = copy.deepcopy(before)
                expected["pools"][1]["entries"][0] = {
                    "type": "minecraft:empty",
                    "weight": tools.LOOT_EMPTY_WEIGHTS[relative],
                }

                self.assertEqual(source, before, "pure loot repair must not mutate its input")
                self.assertEqual(
                    expected,
                    repaired,
                    "the entire loot document must differ only by the exact weighted-empty repair",
                )
                self.assertNotIn("minecraft:air", json.dumps(repaired))

    def test_missing_extra_or_relocated_air_entry_fails_closed(self) -> None:
        relative = tools.LOOT_TABLE_PATHS[0]
        mutations = {
            "missing air": lambda value: value["pools"][1]["entries"][0].update(
                {"name": "minecraft:stone"}
            ),
            "extra function": lambda value: value["pools"][1]["entries"][0].update(
                {"functions": []}
            ),
            "extra pool entry": lambda value: value["pools"][1]["entries"].append(
                {"type": "minecraft:item", "weight": 1, "name": "minecraft:air"}
            ),
        }
        for label, mutate in mutations.items():
            with self.subTest(label=label):
                value = synthetic_loot(relative)
                mutate(value)
                with self.assertRaises(tools.ValidationError):
                    tools.repair_air_loot_entry(value, relative)

    def test_tree_migration_preserves_the_exact_sorcerer_potion_conversion(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            values = {
                relative: synthetic_loot(relative)
                for relative in tools.VILLAGE_CHEST_LOOT_TABLE_PATHS
            }
            sorcerer = values[tools.LOOT_TABLE_PATHS[1]]
            potion_entry = sorcerer["pools"][0]["entries"][0]
            potion_entry["name"] = "minecraft:potion"
            potion_entry["functions"] = [
                {
                    "function": "minecraft:set_components",
                    "components": {"potion_contents": "minecraft:strong_leaping"},
                }
            ]
            for relative, value in values.items():
                path = root / PurePosixPath(relative)
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_text(json.dumps(value), encoding="utf-8")
            record = tools.migrate_private_loot_tables(root)
            self.assertEqual(2, record["air_item_entries_replaced_with_empty"])
            self.assertEqual(1, record["sorcerer_potion_loot_functions_migrated"])
            currency = record["glowcap_currency_substitution"]
            self.assertEqual(5, currency["entries_replaced"])
            self.assertEqual(
                tools.GLOWCAP_CURRENCY_TARGET_ID, currency["replacement_item"]
            )
            migrated = json.loads(
                (root / PurePosixPath(tools.LOOT_TABLE_PATHS[1])).read_text(encoding="utf-8")
            )
            serialized = json.dumps(migrated, separators=(",", ":"))
            self.assertEqual(1, serialized.count("minecraft:set_potion"))
            self.assertNotIn("potion_contents", serialized)

            errors: list[str] = []
            tools.validate_private_loot_tables(
                root,
                {
                    "assets/minecraft/items/stone.json",
                    "assets/minecraft/items/potion.json",
                    "assets/minecraft/items/amethyst_block.json",
                },
                errors,
            )
            self.assertEqual([], errors)
            self.assertFalse((root / "assets/ribbits/items/glowcap.json").exists())
            self.assertEqual(
                frozenset({"ribbits:glowcap"}), tools.SOURCE_SAFE_PUBLIC_LOOT_ITEM_IDS
            )
            missing_registry_errors: list[str] = []
            tools.validate_private_loot_tables(
                root,
                {"assets/minecraft/items/potion.json"},
                missing_registry_errors,
            )
            self.assertTrue(
                any("item registry evidence is absent" in error for error in missing_registry_errors)
            )

    def test_exact_five_currency_entries_change_without_other_byte_drift(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            before_bytes: dict[str, bytes] = {}
            before_values: dict[str, dict[str, object]] = {}
            for relative in tools.VILLAGE_CHEST_LOOT_TABLE_PATHS:
                value = synthetic_loot(relative)
                path = root / PurePosixPath(relative)
                path.parent.mkdir(parents=True, exist_ok=True)
                payload = (json.dumps(value, indent=4) + "\n").encode("utf-8")
                path.write_bytes(payload)
                before_bytes[relative] = payload
                before_values[relative] = copy.deepcopy(value)

            record = tools.replace_private_glowcap_currency_entries(root)

            self.assertEqual(5, record["entries_replaced"])
            self.assertEqual(
                list(tools.VILLAGE_CHEST_LOOT_TABLE_PATHS), record["tables_inspected"]
            )
            self.assertEqual(
                list(tools.GLOWCAP_CURRENCY_ENTRY_SPECS),
                [target["table"] for target in record["targets"]],
            )
            source_token = json.dumps(tools.GLOWCAP_CURRENCY_SOURCE_ID).encode("utf-8")
            target_token = json.dumps(tools.GLOWCAP_CURRENCY_TARGET_ID).encode("utf-8")
            records_by_table = {target["table"]: target for target in record["targets"]}

            for relative in tools.VILLAGE_CHEST_LOOT_TABLE_PATHS:
                with self.subTest(relative=relative):
                    path = root / PurePosixPath(relative)
                    actual_bytes = path.read_bytes()
                    expected_count = int(relative in tools.GLOWCAP_CURRENCY_ENTRY_SPECS)
                    expected_bytes = before_bytes[relative].replace(source_token, target_token)
                    self.assertEqual(expected_bytes, actual_bytes)
                    self.assertEqual(expected_count, actual_bytes.count(target_token))
                    self.assertNotIn(source_token, actual_bytes)

                    expected_value = copy.deepcopy(before_values[relative])
                    spec = tools.GLOWCAP_CURRENCY_ENTRY_SPECS.get(relative)
                    if spec is not None:
                        expected_value["pools"][spec["pool"]]["entries"][spec["entry"]][
                            "name"
                        ] = tools.GLOWCAP_CURRENCY_TARGET_ID
                        target = records_by_table[relative]
                        self.assertEqual(spec["weight"], target["weight"])
                        self.assertEqual(
                            {
                                "type": "minecraft:uniform",
                                "min": spec["count_min"],
                                "max": spec["count_max"],
                            },
                            target["count"],
                        )
                        self.assertFalse(target["add"])
                        self.assertEqual(
                            tools.sha256_bytes(before_bytes[relative]),
                            target["before_sha256"],
                        )
                        self.assertEqual(
                            tools.sha256_bytes(actual_bytes), target["after_sha256"]
                        )
                    self.assertEqual(expected_value, json.loads(actual_bytes))

            fisherman_main = tools.VILLAGE_CHEST_LOOT_TABLE_PATHS[0]
            self.assertEqual(
                before_bytes[fisherman_main],
                (root / PurePosixPath(fisherman_main)).read_bytes(),
            )
            block_spec = tools.UNRELATED_AMETHYST_BLOCK_SPEC
            merchant = json.loads(
                (root / PurePosixPath(block_spec["table"])).read_text(encoding="utf-8")
            )
            self.assertEqual(
                block_spec["value"],
                merchant["pools"][block_spec["pool"]]["entries"][block_spec["entry"]],
            )

    def test_currency_substitution_fails_before_writing_if_any_pinned_input_drifts(self) -> None:
        cases = (
            "missing currency",
            "changed price metadata",
            "extra currency",
            "changed unrelated amethyst",
            "unexpected table",
        )
        for label in cases:
            with self.subTest(label=label), tempfile.TemporaryDirectory() as temp_dir:
                root = Path(temp_dir)
                values = {
                    relative: synthetic_loot(relative)
                    for relative in tools.VILLAGE_CHEST_LOOT_TABLE_PATHS
                }
                if label == "missing currency":
                    relative = tools.VILLAGE_CHEST_LOOT_TABLE_PATHS[2]
                    spec = tools.GLOWCAP_CURRENCY_ENTRY_SPECS[relative]
                    values[relative]["pools"][spec["pool"]]["entries"][spec["entry"]][
                        "name"
                    ] = "minecraft:stone"
                elif label == "changed price metadata":
                    relative = tools.VILLAGE_CHEST_LOOT_TABLE_PATHS[1]
                    spec = tools.GLOWCAP_CURRENCY_ENTRY_SPECS[relative]
                    values[relative]["pools"][spec["pool"]]["entries"][spec["entry"]][
                        "weight"
                    ] = 99
                elif label == "extra currency":
                    relative = tools.VILLAGE_CHEST_LOOT_TABLE_PATHS[0]
                    values[relative]["pools"][0]["entries"][0]["name"] = (
                        tools.GLOWCAP_CURRENCY_SOURCE_ID
                    )
                elif label == "changed unrelated amethyst":
                    block_spec = tools.UNRELATED_AMETHYST_BLOCK_SPEC
                    values[block_spec["table"]]["pools"][block_spec["pool"]]["entries"][
                        block_spec["entry"]
                    ]["name"] = "minecraft:stone"

                for relative, value in values.items():
                    path = root / PurePosixPath(relative)
                    path.parent.mkdir(parents=True, exist_ok=True)
                    path.write_text(json.dumps(value), encoding="utf-8")
                if label == "unexpected table":
                    extra = root / "data/ribbits/loot_table/chests/unexpected.json"
                    extra.write_text(json.dumps(synthetic_loot(tools.LOOT_TABLE_PATHS[0])))

                before = {
                    path: path.read_bytes()
                    for path in (root / "data/ribbits/loot_table/chests").glob("*.json")
                }
                with self.assertRaises(tools.ValidationError):
                    tools.replace_private_glowcap_currency_entries(root)
                self.assertEqual(before, {path: path.read_bytes() for path in before})


class DonorCompositeTest(unittest.TestCase):
    def test_fixed_atlas_preserves_decoded_donor_and_shared_pixels(self) -> None:
        donor_pixel = bytes((10, 20, 30, 255))
        shared_pixel = bytes((40, 50, 60, 255))
        donor = tools.encode_rgba_png(64, 64, donor_pixel * (64 * 64))
        shared = tools.encode_rgba_png(128, 128, shared_pixel * (128 * 128))

        first = tools.build_composite_texture(donor, "synthetic donor", shared)
        second = tools.build_composite_texture(donor, "synthetic donor", shared)
        self.assertEqual(first, second)
        width, height, pixels = tools.decode_rgba_png(first, "synthetic composite")
        self.assertEqual((256, 128), (width, height))

        def pixel(x: int, y: int) -> bytes:
            offset = (y * width + x) * 4
            return pixels[offset : offset + 4]

        self.assertEqual(donor_pixel, pixel(0, 0))
        self.assertEqual(donor_pixel, pixel(63, 63))
        self.assertEqual(bytes(4), pixel(64, 0))
        self.assertEqual(shared_pixel, pixel(128, 0))
        self.assertEqual(shared_pixel, pixel(255, 127))

    def test_normal_and_all_three_umbrella_models_use_disjoint_atlas_halves(self) -> None:
        donor = synthetic_model()
        normal = tools.build_profession_model(donor, "synthetic donor")
        description = normal["minecraft:geometry"][0]["description"]
        self.assertEqual(256, description["texture_width"])
        self.assertEqual(128, description["texture_height"])
        self.assertEqual(64, donor["minecraft:geometry"][0]["description"]["texture_width"])

        for variant in tools.UMBRELLA_VARIANTS:
            with self.subTest(variant=variant):
                composite = tools.build_umbrella_composite_model(
                    normal, synthetic_umbrella_model(variant), "chef", variant
                )
                bone_name = "umbrella" if variant == 1 else f"umbrella{variant}"
                bone = next(
                    bone
                    for bone in composite["minecraft:geometry"][0]["bones"]
                    if bone["name"] == bone_name
                )
                self.assertEqual([132, 51], bone["cubes"][0]["uv"])
                tools.validate_model_uv_bounds(
                    composite, "synthetic composite", 128, 256, bone_names={bone_name}
                )

    def test_donor_cube_crossing_first_half_fails_closed(self) -> None:
        crossing = synthetic_model(128, [126, 0])
        with self.assertRaisesRegex(tools.ValidationError, "escapes approved atlas"):
            tools.build_profession_model(crossing, "crossing donor")


class VillageNbtMigrationTest(unittest.TestCase):
    def test_removes_only_exact_ribbit_data_from_all_five_templates(self) -> None:
        for relative, values in tools.VILLAGE_RIBBIT_TEMPLATE_DATA.items():
            with self.subTest(relative=relative):
                source = synthetic_resident_nbt(values)
                source_decoded = tools.decode_nbt_bytes(source, relative)
                source_scanner = tools.scan_nbt(source_decoded, relative)
                ribbit_data_path = ("entities", "[0]", "nbt", "RibbitData")
                source_matches = [
                    item
                    for item in source_scanner.named_compounds
                    if item["path"] == ribbit_data_path
                ]
                self.assertEqual(1, len(source_matches))
                removed = source_matches[0]
                expected_decoded = (
                    source_decoded[: removed["start"]] + source_decoded[removed["end"] :]
                )
                transformed = tools.remove_exact_village_ribbit_data(source, relative)
                self.assertEqual(
                    transformed,
                    tools.remove_exact_village_ribbit_data(source, relative),
                    "same input must produce byte-identical deterministic NBT",
                )
                decoded = tools.decode_nbt_bytes(transformed, relative)
                self.assertEqual(
                    expected_decoded,
                    decoded,
                    "the NBT payload must be the exact source bytes with only RibbitData spliced",
                )
                scanner = tools.scan_nbt(decoded, relative)
                self.assertEqual((10, 1), scanner.lists[("entities",)])
                self.assertEqual(
                    "ribbits:ribbit",
                    scanner.strings[("entities", "[0]", "nbt", "id")],
                )
                for sentinel_name, sentinel_value in (
                    NBT_SENTINEL_BEFORE,
                    NBT_SENTINEL_AFTER,
                ):
                    sentinel_path = ("entities", "[0]", "nbt", sentinel_name)
                    self.assertEqual(sentinel_value, scanner.strings[sentinel_path])
                    self.assertEqual(
                        1,
                        decoded.count(named_string(sentinel_name, sentinel_value)),
                        f"the exact encoded {sentinel_name} tag bytes must survive once",
                    )
                self.assertFalse(
                    any(item["path"][-1:] == ("RibbitData",) for item in scanner.named_compounds)
                )

    def test_mismatched_pinned_profession_instrument_or_umbrella_fails_closed(self) -> None:
        relative, expected = next(iter(tools.VILLAGE_RIBBIT_TEMPLATE_DATA.items()))
        for key in expected:
            with self.subTest(key=key):
                altered = dict(expected)
                altered[key] = "ribbits:unexpected"
                with self.assertRaisesRegex(tools.ValidationError, "pinned RibbitData differs"):
                    tools.remove_exact_village_ribbit_data(
                        synthetic_resident_nbt(altered), relative
                    )


class DonorBoundaryContractTest(unittest.TestCase):
    def test_exact_accounting_contains_only_approved_visual_members_and_outputs(self) -> None:
        self.assertEqual("4.1.6+26.2-mynx-canary3", tools.CANDIDATE_VERSION)
        self.assertEqual(3, tools.CANDIDATE_CANARY)
        self.assertEqual(
            "mynx-ribbits-private-resource-manifest/v1", tools.PRIVATE_MANIFEST_SCHEMA
        )
        self.assertEqual(
            "PRIVATE MYNX ASSEMBLY STAGED / NONREDISTRIBUTABLE DONOR ASSETS",
            tools.PRIVATE_MANIFEST_CLASSIFICATION,
        )
        self.assertEqual(336, tools.OUTPUT_FILE_COUNT)
        self.assertEqual(41, len(tools.GECKO_MODEL_IDS))
        self.assertEqual(24, len(tools.REGISTERED_ITEM_IDS))
        self.assertNotIn("glowcap", tools.REGISTERED_ITEM_IDS)
        self.assertNotIn("toadstool_heart", tools.REGISTERED_ITEM_IDS)
        self.assertEqual(9, len(tools.SPAWN_EGG_IDS))
        self.assertEqual(20, len(tools.DONOR_DERIVED_OUTPUTS))
        self.assertEqual(
            8,
            sum(len(spec["members"]) for spec in tools.DONOR_INPUT_SPECS.values()),
        )
        for spec in tools.DONOR_INPUT_SPECS.values():
            for member in spec["members"]:
                self.assertTrue(member.endswith((".geo.json", ".png")))
                self.assertFalse(member.endswith((".class", ".java")))
                self.assertNotIn("/animations/", member)
        for output in tools.DONOR_DERIVED_OUTPUTS:
            self.assertTrue(output.endswith((".geo.json", ".png")))
            self.assertTrue(output.startswith("assets/ribbits/"))

    def test_originals_path_guard_rejects_outside_or_renamed_inputs(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            base = Path(temp_dir)
            root = base / "originals"
            mods = root / "mods"
            mods.mkdir(parents=True)
            filename = tools.DONOR_INPUT_SPECS["guard"]["filename"]
            exact = mods / filename
            exact.write_bytes(b"synthetic-not-a-donor")
            self.assertEqual(
                exact.resolve(),
                tools.require_exact_originals_member_path(exact, root, filename, "synthetic"),
            )
            outside = base / filename
            outside.write_bytes(b"synthetic-not-a-donor")
            with self.assertRaisesRegex(tools.ValidationError, "exact originals/mods member"):
                tools.require_exact_originals_member_path(outside, root, filename, "synthetic")
            with self.assertRaisesRegex(tools.ValidationError, "size differs"):
                tools.load_exact_donor(exact, root, "guard")

    def test_source_only_boundary_flags_transformed_assets_and_donor_namespaces(self) -> None:
        allowed = {
            "fabric.mod.json",
            "com/yungnickyoung/minecraft/ribbits/RibbitsCommon.class",
        }
        self.assertEqual([], tools.source_only_donor_violations(allowed))
        forbidden = {
            next(iter(tools.DONOR_DERIVED_OUTPUTS)),
            "assets/guardribbits/geo/guard_ribbit.geo.json",
            "me/rogue_one/useful_ribbits/entity/ChefRibbitEntity.class",
            "sunbatheproductions28/guardribbits/GuardRibbitEntity.class",
            "GuardRibbits-1.20.1-Fabric-1.0.4.jar",
            "META-INF/jars/useful_ribbits-1.0.2-forge-1.20.1.jar",
        }
        self.assertEqual(6, len(tools.source_only_donor_violations(forbidden)))

    def test_private_jar_donor_boundary_uses_real_guard_prefix_and_jar_basenames(self) -> None:
        violations = tools.nonallowlisted_donor_archive_violations(
            {
                "sunbatheproductions28/guardribbits/entity/GuardRibbitEntity.class",
                "me/rogue_one/useful_ribbits/procedures/RBGUIChefBtnProcedure.class",
                "nested/deeper/GuardRibbits-1.20.1-Fabric-1.0.4.jar",
                "useful_ribbits-1.0.2-forge-1.20.1.jar",
                "com/yungnickyoung/minecraft/ribbits/RibbitsCommon.class",
            }
        )
        self.assertEqual(4, len(violations))
        self.assertNotIn(
            "com/yungnickyoung/minecraft/ribbits/RibbitsCommon.class", violations
        )

    def test_zip_entry_paths_and_staging_destinations_fail_closed(self) -> None:
        for unsafe in (
            r"assets\guardribbits\geo\guard_ribbit.geo.json",
            r"C:\donors\GuardRibbits.jar",
            "C:/donors/GuardRibbits.jar",
            "../assets/ribbits/escape.png",
        ):
            with self.subTest(unsafe=unsafe):
                with self.assertRaisesRegex(tools.ValidationError, "Unsafe ZIP entry"):
                    tools.safe_zip_name(unsafe)

        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir) / "resources"
            root.mkdir()
            tools.require_descendant_path(
                root / "assets/ribbits/safe.png", root, "synthetic output"
            )
            with self.assertRaisesRegex(tools.ValidationError, "must stay inside"):
                tools.require_descendant_path(
                    root.parent / "escape.png", root, "synthetic output"
                )

    def test_private_domain_inventory_must_equal_staged_resources_exactly(self) -> None:
        staged = {
            "assets/ribbits/lang/en_us.json",
            "data/ribbits/loot_table/chests/sorcerer.json",
            "icon.png",
            "logo.png",
        }
        packaged = staged | tools.SOURCE_SAFE_PUBLIC_RESOURCE_PATHS | {
            "fabric.mod.json",
            "com/yungnickyoung/minecraft/ribbits/RibbitsCommon.class",
        }
        self.assertEqual((set(), set()), tools.private_domain_inventory_difference(staged, packaged))

        missing, extra = tools.private_domain_inventory_difference(
            staged,
            packaged
            - {"data/ribbits/loot_table/chests/sorcerer.json"}
            | {
                "data/ribbits/worldgen/template_pool/donor_extra.json",
                "assets/ribbits/geckolib/models/donor_extra.geo.json",
            },
        )
        self.assertEqual(
            {"data/ribbits/loot_table/chests/sorcerer.json"}, missing
        )
        self.assertEqual(
            {
                "data/ribbits/worldgen/template_pool/donor_extra.json",
                "assets/ribbits/geckolib/models/donor_extra.geo.json",
            },
            extra,
        )

    def test_private_jar_allows_only_exact_tracked_public_resource_bytes(self) -> None:
        expected_paths = {
            "assets/ribbits/items/glowcap.json",
            "assets/ribbits/items/toadstool_heart.json",
            "data/ribbits/advancement/recipes/misc/toadstool_heart.json",
            "data/ribbits/recipe/toadstool_heart.json",
        }
        self.assertEqual(expected_paths, tools.SOURCE_SAFE_PUBLIC_RESOURCE_PATHS)

        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            source_root = root / "tracked-resources"
            archive_path = root / "private.jar"
            expected_bytes: dict[str, bytes] = {}
            for index, relative in enumerate(sorted(expected_paths)):
                payload = f"source-safe-{index}\n".encode("utf-8")
                expected_bytes[relative] = payload
                tracked = source_root.joinpath(*PurePosixPath(relative).parts)
                tracked.parent.mkdir(parents=True, exist_ok=True)
                tracked.write_bytes(payload)

            with zipfile.ZipFile(archive_path, "w") as archive:
                for relative, payload in expected_bytes.items():
                    archive.writestr(relative, payload)

            errors: list[str] = []
            with zipfile.ZipFile(archive_path) as archive:
                tools.validate_source_safe_public_resource_boundary(
                    archive, set(), errors, source_root
                )
            self.assertEqual([], errors)

            with zipfile.ZipFile(archive_path, "w") as archive:
                for relative, payload in expected_bytes.items():
                    archive.writestr(
                        relative,
                        b"changed\n" if relative.endswith("glowcap.json") else payload,
                    )
            errors = []
            with zipfile.ZipFile(archive_path) as archive:
                tools.validate_source_safe_public_resource_boundary(
                    archive,
                    {"data/ribbits/recipe/toadstool_heart.json"},
                    errors,
                    source_root,
                )
            self.assertTrue(
                any("entered private staging" in error for error in errors), errors
            )
            self.assertTrue(
                any("changed during packaging: assets/ribbits/items/glowcap.json" in error
                    for error in errors),
                errors,
            )

    def test_private_jar_requires_exact_economy_runtime_dependencies(self) -> None:
        self.assertEqual(">=4.0.0", tools.REQUIRED_FABRIC_DEPENDENCIES["customportals"])
        self.assertEqual(
            ">=0.1.10-canary11",
            tools.REQUIRED_FABRIC_DEPENDENCIES["matcha_heart_death_compat"],
        )

        errors: list[str] = []
        tools.validate_required_fabric_dependencies(
            {"depends": dict(tools.REQUIRED_FABRIC_DEPENDENCIES)}, errors
        )
        self.assertEqual([], errors)

        drifted = dict(tools.REQUIRED_FABRIC_DEPENDENCIES)
        drifted.pop("customportals")
        drifted["matcha_heart_death_compat"] = ">=0.1.9"
        errors = []
        tools.validate_required_fabric_dependencies({"depends": drifted}, errors)
        self.assertTrue(any("Dependency customportals differs" in error for error in errors))
        self.assertTrue(
            any("Dependency matcha_heart_death_compat differs" in error for error in errors)
        )

    def test_manifest_is_never_visible_when_final_donor_rehash_fails(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            manifest = Path(temp_dir) / "manifest.json"
            checks = [(Path(temp_dir) / "donor.jar", {}, "synthetic donor")]

            observations: list[bool] = []

            def verify_before_publication(*_args: object) -> None:
                observations.append(manifest.exists())
                if len(observations) == 2:
                    raise tools.ValidationError("post-write donor drift")

            with mock.patch.object(
                tools,
                "require_donor_unchanged",
                side_effect=verify_before_publication,
            ):
                with self.assertRaisesRegex(tools.ValidationError, "post-write donor drift"):
                    tools.write_manifest_after_donor_verification(
                        manifest, {"classification": "eligible"}, checks
                    )
            self.assertEqual([False, False], observations)
            self.assertFalse(manifest.exists())
            self.assertEqual([], list(Path(temp_dir).glob(".manifest.json.*.tmp")))

    def test_manifest_is_published_only_after_both_donor_rehashes_pass(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            manifest = Path(temp_dir) / "manifest.json"
            checks = [(Path(temp_dir) / "donor.jar", {}, "synthetic donor")]
            observations: list[bool] = []

            def verify_before_publication(*_args: object) -> None:
                observations.append(manifest.exists())

            with mock.patch.object(
                tools,
                "require_donor_unchanged",
                side_effect=verify_before_publication,
            ):
                tools.write_manifest_after_donor_verification(
                    manifest, {"classification": "eligible"}, checks
                )

            self.assertEqual([False, False], observations)
            self.assertEqual(
                {"classification": "eligible"}, tools.load_json(manifest)
            )
            self.assertEqual([], list(Path(temp_dir).glob(".manifest.json.*.tmp")))

    def test_manifest_is_never_written_when_prepublication_donor_rehash_fails(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            manifest = Path(temp_dir) / "manifest.json"
            checks = [(Path(temp_dir) / "donor.jar", {}, "synthetic donor")]
            with mock.patch.object(
                tools,
                "require_donor_unchanged",
                side_effect=tools.ValidationError("pre-write donor drift"),
            ):
                with self.assertRaisesRegex(tools.ValidationError, "pre-write donor drift"):
                    tools.write_manifest_after_donor_verification(
                        manifest, {"classification": "eligible"}, checks
                    )
            self.assertFalse(manifest.exists())

    def test_musician_and_four_new_egg_translations_are_exact(self) -> None:
        self.assertEqual(
            "Musician Ribbit Spawn Egg",
            tools.EN_US_MYNX_PROFESSION_TRANSLATIONS[
                "item.ribbits.ribbit_nitwit_spawn_egg"
            ],
        )
        self.assertEqual(
            set(tools.NEW_SPAWN_EGG_IDS),
            {
                key.removeprefix("item.ribbits.")
                for key in tools.EN_US_MYNX_PROFESSION_TRANSLATIONS
                if key.startswith("item.ribbits.ribbit_")
                and key.endswith("_spawn_egg")
                and "nitwit" not in key
            },
        )

    def test_economy_item_and_rank_title_translations_are_exact(self) -> None:
        expected_items = {
            "item.ribbits.glowcap": "Glowcap",
            "item.ribbits.toadstool_heart": "Toadstool Heart",
        }
        self.assertEqual(
            expected_items,
            {
                key: tools.EN_US_MYNX_PROFESSION_TRANSLATIONS[key]
                for key in expected_items
            },
        )

        rank_names = {
            "gardener": ("Sprout Tender", "Toadstool Keeper"),
            "farmer": ("Vine Puller", "Root Wrangler", "Mudfield Steward"),
            "fisherman": (
                "Pond Forager",
                "Coral Keeper",
                "Amphibian Attendant",
                "Opal Angler",
                "Monument Mariner",
            ),
            "merchant": ("Moss Peddler", "Lantern Trader", "Glowgoods Baron"),
            "chef": (
                "Tadpole Cook",
                "Pond Cook",
                "Swamp Chef",
                "Grand Chef",
                "Master of the Feast",
            ),
            "sorcerer": (
                "Wart Whisperer",
                "Gatecaller",
                "Flask Sage",
                "Deep-Pond Oracle",
            ),
            "prospector": ("Pebble Picker", "Vein-Seeker", "Deep Delver"),
            "guard": (
                "Pond Sentry",
                "Lily Warden",
                "Marsh Marshal",
                "Bulwark of the Bog",
            ),
        }
        expected_titles = {
            f"entity.ribbits.merchant.{profession}.tier_{tier}": name
            for profession, names in rank_names.items()
            for tier, name in enumerate(names, start=1)
        }
        expected_titles["entity.ribbits.merchant.nitwit.musician"] = "Musician"
        actual_titles = {
            key: value
            for key, value in tools.EN_US_MYNX_PROFESSION_TRANSLATIONS.items()
            if key.startswith("entity.ribbits.merchant.")
        }
        self.assertEqual(30, len(expected_titles))
        self.assertEqual(expected_titles, actual_titles)


if __name__ == "__main__":
    unittest.main()
