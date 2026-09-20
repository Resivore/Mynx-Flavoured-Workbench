#!/usr/bin/env python3
"""Source-safe regression tests for the tracked private-resource assembler."""

from __future__ import annotations

import copy
import json
import struct
import tempfile
import unittest
import zipfile
import zlib
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


def named_int(name: str, value: int) -> bytes:
    return b"\x03" + nbt_string(name) + struct.pack(">i", value)


def named_compound(name: str, payload: bytes) -> bytes:
    return b"\x0a" + nbt_string(name) + payload + b"\x00"


def named_list(name: str, element_type: int, payloads: list[bytes]) -> bytes:
    return (
        b"\x09"
        + nbt_string(name)
        + bytes([element_type])
        + struct.pack(">i", len(payloads))
        + b"".join(payloads)
    )


def synthetic_structure_nbt(
    palette: list[dict[str, object]], blocks: list[dict[str, object]]
) -> bytes:
    """Build a vanilla-only structure fixture without protected Ribbits content."""
    palette_payloads = [tools._encode_palette_state_payload(state) for state in palette]
    block_payloads: list[bytes] = []
    for block in blocks:
        payload = b""
        block_entity_id = block.get("block_entity_id")
        if block_entity_id is not None:
            nbt_payload = named_string("id", str(block_entity_id))
            loot_table = block.get("loot_table")
            if loot_table is not None:
                nbt_payload += named_string("LootTable", str(loot_table))
            nbt_payload += named_string("synthetic_sentinel", "preserved")
            payload += named_compound("nbt", nbt_payload)
        position = tuple(block["position"])
        payload += named_list(
            "pos", 3, [struct.pack(">i", int(coordinate)) for coordinate in position]
        )
        payload += named_int("state", int(block["state"]))
        block_payloads.append(payload + b"\x00")
    root = (
        b"\x0a"
        + nbt_string("")
        + named_list("blocks", 10, block_payloads)
        + named_list("palette", 10, palette_payloads)
        + b"\x00"
    )
    return tools.deterministic_gzip(root)


def pin_synthetic_utility_spec(
    data: bytes,
    relative: str,
    coordinate: tuple[int, int, int],
    source_state: dict[str, object],
    replacement_state: dict[str, object],
    block_entity_id: str | None,
) -> dict[str, object]:
    spec: dict[str, object] = {
        "before_sha256": tools.sha256_bytes(data),
        "after_sha256": "0" * 64,
        "coordinate": coordinate,
        "source_state": source_state,
        "replacement_state": replacement_state,
        "block_entity_id": block_entity_id,
    }
    try:
        tools.transform_exact_private_village_utility(data, relative, spec)
    except tools.ValidationError as exc:
        message = str(exc)
        marker = "got "
        if "output SHA-256 differs" not in message or marker not in message:
            raise
        spec["after_sha256"] = message.rsplit(marker, 1)[1]
    else:
        raise AssertionError("Synthetic output hash probe unexpectedly matched zero")
    return spec


def pin_synthetic_restored_utility_spec(
    data: bytes,
    relative: str,
    coordinate: tuple[int, int, int],
    state: dict[str, object],
    block_entity_id: str,
) -> dict[str, object]:
    inspected = tools.inspect_structure_template(data, relative)
    target = next(
        block for block in inspected["blocks"] if block["position"] == coordinate
    )
    nbt_start, nbt_end = target["nbt_range"]
    return {
        "sha256": tools.sha256_bytes(data),
        "size": len(data),
        "coordinate": coordinate,
        "state": state,
        "block_entity_id": block_entity_id,
        "block_entity_nbt_sha256": tools.sha256_bytes(
            inspected["decoded"][nbt_start:nbt_end]
        ),
    }


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


class HugeToadstoolFeatureTest(unittest.TestCase):
    def test_exact_vanilla_shapes_are_crossed_with_ribbits_materials(self) -> None:
        documents = [
            (path, tools.derive_huge_toadstool_document(spec))
            for path, spec in tools.HUGE_TOADSTOOL_FEATURE_SPECS.items()
        ]
        tools.validate_huge_toadstool_documents(documents)
        by_path = dict(documents)

        red = by_path[
            "data/ribbits/worldgen/configured_feature/huge_red_toadstool.json"
        ]
        self.assertEqual("minecraft:huge_brown_mushroom", red["type"])
        self.assertEqual(3, red["config"]["foliage_radius"])
        self.assertEqual(
            "ribbits:red_toadstool",
            red["config"]["cap_provider"]["state"]["Name"],
        )
        self.assertEqual(
            "ribbits:toadstool_stem",
            red["config"]["stem_provider"]["state"]["Name"],
        )

        brown = by_path[
            "data/ribbits/worldgen/configured_feature/huge_brown_toadstool.json"
        ]
        self.assertEqual("minecraft:huge_red_mushroom", brown["type"])
        self.assertNotIn("foliage_radius", brown["config"])
        self.assertEqual(
            2,
            tools.HUGE_TOADSTOOL_FEATURE_SPECS[
                "data/ribbits/worldgen/configured_feature/huge_brown_toadstool.json"
            ]["effective_foliage_radius"],
        )
        self.assertEqual(
            "ribbits:brown_toadstool",
            brown["config"]["cap_provider"]["state"]["Name"],
        )
        self.assertEqual(
            "ribbits:toadstool_stem",
            brown["config"]["stem_provider"]["state"]["Name"],
        )
        serialized = json.dumps(by_path, sort_keys=True)
        for forbidden in tools.VANILLA_MUSHROOM_BLOCK_IDS:
            self.assertNotIn(forbidden, serialized)

    def test_shape_or_block_drift_is_rejected(self) -> None:
        documents = [
            (path, tools.derive_huge_toadstool_document(spec))
            for path, spec in tools.HUGE_TOADSTOOL_FEATURE_SPECS.items()
        ]
        documents[0][1]["type"] = "minecraft:huge_red_mushroom"
        with self.assertRaisesRegex(tools.ValidationError, "not the exact hash-pinned"):
            tools.validate_huge_toadstool_documents(documents)


class VillageToadstoolColorTransformTest(unittest.TestCase):
    @staticmethod
    def _location_to_template(root: Path, location: str) -> Path:
        namespace, relative = location.split(":", 1)
        assert namespace == "ribbits"
        return root / "data/ribbits/structure" / f"{relative}.nbt"

    def _write_fixture(self, root: Path) -> list[str]:
        def write(relative: str, value: object) -> None:
            path = root.joinpath(*PurePosixPath(relative).parts)
            path.parent.mkdir(parents=True, exist_ok=True)
            tools.write_json(path, value)

        exact_houses = [
            "ribbits:" + path.removeprefix("data/ribbits/structure/").removesuffix(".nbt")
            for path in tools.VILLAGE_TOADSTOOL_NBT_PLACEMENTS
        ]
        locations_by_pool = {
            "data/ribbits/worldgen/template_pool/decor.json": [
                "ribbits:decor/synthetic_decor"
            ],
            "data/ribbits/worldgen/template_pool/houses.json": exact_houses
            + [f"ribbits:houses/synthetic_house_{index}" for index in range(9)],
            "data/ribbits/worldgen/template_pool/paths.json": [
                f"ribbits:paths/synthetic_path_{index}" for index in range(7)
            ],
            "data/ribbits/worldgen/template_pool/paths_fallback.json": [
                "ribbits:paths/synthetic_fallback"
            ],
            "data/ribbits/worldgen/template_pool/ribbits.json": [
                f"ribbits:ribbits/synthetic_resident_{index}" for index in range(5)
            ],
            "data/ribbits/worldgen/template_pool/starts.json": [
                "ribbits:paths/synthetic_start"
            ],
        }
        all_locations: list[str] = []
        for relative in tools.VILLAGE_TEMPLATE_POOL_PATHS:
            locations = locations_by_pool[relative]
            all_locations.extend(locations)
            elements = [
                {
                    "weight": 1,
                    "element": {
                        "location": location,
                        "processors": "ribbits:main",
                    },
                }
                for location in locations
            ]
            if relative.endswith("/decor.json"):
                elements.append(
                    {
                        "weight": 1,
                        "element": {"feature": "ribbits:veg_patch"},
                    }
                )
            write(relative, {"name": PurePosixPath(relative).stem, "elements": elements})

        write(tools.VILLAGE_MAIN_PROCESSOR_PATH, {"processors": []})
        legacy_veg = synthetic_legacy_document()
        legacy_veg["config"]["feature"]["feature"]["config"] = {
            "on_solid_state_provider": {
                "type": "minecraft:weighted_state_provider",
                "entries": copy.deepcopy(tools.VILLAGE_VEG_PATCH_SOURCE_ENTRIES),
            },
            "on_liquid_state_provider": {
                "type": "minecraft:simple_state_provider",
                "state": {"Name": "ribbits:giant_lilypad"},
            },
            "cannot_place_on": [
                {"Name": "minecraft:podzol"},
                {"Name": "minecraft:coarse_dirt"},
            ],
        }
        migrated_veg = tools.migrate_legacy_random_patch_document(
            tools.VILLAGE_VEG_PATCH_CONFIGURED_FEATURE_PATH, legacy_veg
        )
        write(tools.VILLAGE_VEG_PATCH_CONFIGURED_FEATURE_PATH, migrated_veg)
        write(
            "data/ribbits/worldgen/placed_feature/veg_patch.json",
            {"feature": "ribbits:veg_patch", "placement": []},
        )

        for location in all_locations:
            path = self._location_to_template(root, location)
            path.parent.mkdir(parents=True, exist_ok=True)
            relative = path.relative_to(root).as_posix()
            positions = tools.VILLAGE_TOADSTOOL_NBT_PLACEMENTS.get(relative)
            state = (
                {"Name": "ribbits:toadstool"}
                if positions
                else {"Name": "minecraft:stone"}
            )
            block_positions = positions or ((0, 0, 0),)
            path.write_bytes(
                synthetic_structure_nbt(
                    [state],
                    [
                        {"position": position, "state": 0}
                        for position in block_positions
                    ],
                )
            )
        self.assertEqual(29, len(all_locations))
        return all_locations

    def test_transform_covers_both_paths_without_recoloring_nbt(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            self._write_fixture(root)
            tools.require_village_toadstool_generation_contract(root, transformed=False)
            nbt_before = {
                path.relative_to(root).as_posix(): path.read_bytes()
                for path in root.rglob("*.nbt")
            }

            record = tools.transform_village_toadstool_colors(root)

            tools.require_village_toadstool_generation_contract(root, transformed=True)
            self.assertEqual(0.5, record["processor"]["brown_probability"])
            self.assertEqual(0.5, record["processor"]["red_probability"])
            self.assertEqual(0.6, record["decoration_vegetation"]["toadstool_probability_preserved"])
            self.assertEqual(
                nbt_before,
                {
                    path.relative_to(root).as_posix(): path.read_bytes()
                    for path in root.rglob("*.nbt")
                },
            )

    def test_static_brown_nbt_or_nonvillage_veg_reference_is_rejected(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            locations = self._write_fixture(root)
            generic = next(
                location
                for location in locations
                if self._location_to_template(root, location).relative_to(root).as_posix()
                not in tools.VILLAGE_TOADSTOOL_NBT_PLACEMENTS
            )
            self._location_to_template(root, generic).write_bytes(
                synthetic_structure_nbt(
                    [{"Name": "ribbits:small_brown_toadstool"}],
                    [{"position": (0, 0, 0), "state": 0}],
                )
            )
            with self.assertRaisesRegex(tools.ValidationError, "statically recolored"):
                tools.require_village_toadstool_generation_contract(root, transformed=False)

        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            self._write_fixture(root)
            leak = root / "data/ribbits/worldgen/biome/leak.json"
            leak.parent.mkdir(parents=True, exist_ok=True)
            tools.write_json(leak, {"feature": "ribbits:veg_patch"})
            with self.assertRaisesRegex(tools.ValidationError, "escaped"):
                tools.require_village_toadstool_generation_contract(root, transformed=False)


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

    def test_tree_migration_replaces_only_exact_sorcerer_bottle_and_potion_entries(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            values = {
                relative: synthetic_loot(relative)
                for relative in tools.VILLAGE_CHEST_LOOT_TABLE_PATHS
            }
            sorcerer = values[tools.LOOT_TABLE_PATHS[1]]
            for spec in tools.SORCERER_REMOVED_LOOT_ENTRY_SPECS:
                sorcerer["pools"][spec["pool"]]["entries"][spec["entry"]] = copy.deepcopy(
                    spec["source"]
                )
            before = copy.deepcopy(values)
            for relative, value in values.items():
                path = root / PurePosixPath(relative)
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_text(json.dumps(value), encoding="utf-8")
            record = tools.migrate_private_loot_tables(root)
            self.assertEqual(2, record["air_item_entries_replaced_with_empty"])
            self.assertEqual(
                2, record["sorcerer_bottle_and_potion_entries_replaced_with_empty"]
            )
            self.assertTrue(record["all_unrelated_sorcerer_loot_unchanged"])
            self.assertEqual(
                ["minecraft:glass_bottle", "minecraft:potion"],
                [
                    target["source_item"]
                    for target in record["sorcerer_bottle_and_potion_targets"]
                ],
            )
            currency = record["glowcap_currency_substitution"]
            self.assertEqual(5, currency["entries_replaced"])
            self.assertEqual(
                tools.GLOWCAP_CURRENCY_TARGET_ID, currency["replacement_item"]
            )
            migrated = json.loads(
                (root / PurePosixPath(tools.LOOT_TABLE_PATHS[1])).read_text(encoding="utf-8")
            )
            serialized = json.dumps(migrated, separators=(",", ":"))
            self.assertNotIn("minecraft:glass_bottle", serialized)
            self.assertNotIn("minecraft:potion", serialized)
            self.assertNotIn("minecraft:splash_potion", serialized)
            self.assertNotIn("minecraft:lingering_potion", serialized)
            self.assertNotIn("minecraft:set_potion", serialized)
            self.assertNotIn("potion_contents", serialized)
            expected_sorcerer = copy.deepcopy(before[tools.LOOT_TABLE_PATHS[1]])
            expected_sorcerer["pools"][1]["entries"][0] = {
                "type": "minecraft:empty",
                "weight": tools.LOOT_EMPTY_WEIGHTS[tools.LOOT_TABLE_PATHS[1]],
            }
            for spec in tools.SORCERER_REMOVED_LOOT_ENTRY_SPECS:
                expected_sorcerer["pools"][spec["pool"]]["entries"][spec["entry"]] = (
                    copy.deepcopy(spec["replacement"])
                )
            currency_spec = tools.GLOWCAP_CURRENCY_ENTRY_SPECS[tools.LOOT_TABLE_PATHS[1]]
            expected_sorcerer["pools"][currency_spec["pool"]]["entries"][
                currency_spec["entry"]
            ]["name"] = tools.GLOWCAP_CURRENCY_TARGET_ID
            self.assertEqual(expected_sorcerer, migrated)
            sorcerer_bytes = (
                root / PurePosixPath(tools.LOOT_TABLE_PATHS[1])
            ).read_bytes()
            self.assertEqual(
                {
                    "path": tools.LOOT_TABLE_PATHS[1],
                    "size": len(sorcerer_bytes),
                    "sha256": tools.sha256_bytes(sorcerer_bytes),
                },
                record["sorcerer_output"],
            )

            errors: list[str] = []
            tools.validate_private_loot_tables(
                root,
                {
                    "assets/minecraft/items/stone.json",
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
                set(),
                missing_registry_errors,
            )
            self.assertTrue(
                any("item registry evidence is absent" in error for error in missing_registry_errors)
            )

    def test_sorcerer_bottle_and_potion_replacement_fails_closed_on_drift(self) -> None:
        relative = tools.LOOT_TABLE_PATHS[1]
        source = synthetic_loot(relative)
        for spec in tools.SORCERER_REMOVED_LOOT_ENTRY_SPECS:
            source["pools"][spec["pool"]]["entries"][spec["entry"]] = copy.deepcopy(
                spec["source"]
            )
        source = tools.repair_air_loot_entry(source, relative)

        for label, mutate in {
            "changed weight": lambda value: value["pools"][0]["entries"][2].update(
                {"weight": 6}
            ),
            "relocated potion": lambda value: value["pools"][1]["entries"].reverse(),
            "extra potion form": lambda value: value["pools"][0]["entries"][0].update(
                {"name": "minecraft:splash_potion"}
            ),
        }.items():
            with self.subTest(label=label):
                drifted = copy.deepcopy(source)
                mutate(drifted)
                with self.assertRaises(tools.ValidationError):
                    tools.replace_sorcerer_bottle_and_potion_loot(drifted, relative)

        source_snapshot = copy.deepcopy(source)
        replaced, _records = tools.replace_sorcerer_bottle_and_potion_loot(
            source, relative
        )
        self.assertEqual(source_snapshot, source, "pure replacement must not mutate its input")
        for forbidden in tools.SORCERER_FORBIDDEN_LOOT_ITEM_IDS:
            self.assertNotIn(forbidden, json.dumps(replaced))
        self.assertNotIn("minecraft:set_potion", json.dumps(replaced))
        unrelated_drift = copy.deepcopy(replaced)
        unrelated_drift["pools"][0]["entries"][0]["weight"] = 99
        with self.assertRaisesRegex(tools.ValidationError, "outside the two exact entries"):
            tools.require_exact_sorcerer_loot_replacement(
                source_snapshot, unrelated_drift, relative
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


class PrivateVillageUtilityTransformTest(unittest.TestCase):
    def test_exact_four_template_transform_and_two_pristine_restorations_are_pinned(self) -> None:
        expected_hashes = {
            "data/ribbits/structure/houses/brown_sorcerer_house.nbt": (
                "502dc904d293d411a5ebafed2c7f71b8eed8e36ab2123553ae8ae3295a56aa76",
                "d6878d280ec391a7ffd48fcd442fbdd33f6b341031e7efe753f98812270d1b63",
            ),
            "data/ribbits/structure/houses/red_sorcerer_house.nbt": (
                "ef66d580570c81657500f714b76eb761de910285571ff0ce36442b14e4bc8948",
                "57dcf47cdece4e459522cea74b69215269a45c81028a2c42f2f3ebfaee43d516",
            ),
            "data/ribbits/structure/houses/small_house_brown_3.nbt": (
                "329dd885fd3a26fbf809cc37b74696799bea2e5397a6dd645810261bfbb1055a",
                "b54530ffebc2284ab4397796b8bbad411193fb318e3dfcf93f61558b78327b87",
            ),
            "data/ribbits/structure/houses/small_house_red_3.nbt": (
                "4652d7c9fa1481b9d210a32140eedc751a797c0d2deb6b6e12f53d4c60955e70",
                "2c5a76cf50f5993ed0f6f089aefabe3b04feab2e75962e80b8cfa6f5788ccb3e",
            ),
        }
        self.assertEqual(29, tools.PRIVATE_VILLAGE_TEMPLATE_COUNT)
        self.assertEqual(set(expected_hashes), set(tools.PRIVATE_VILLAGE_UTILITY_TRANSFORMS))
        for relative, (before, after) in expected_hashes.items():
            with self.subTest(relative=relative):
                spec = tools.PRIVATE_VILLAGE_UTILITY_TRANSFORMS[relative]
                self.assertEqual(before, spec["before_sha256"])
                self.assertEqual(after, spec["after_sha256"])
                self.assertRegex(before, r"^[0-9a-f]{64}$")
                self.assertRegex(after, r"^[0-9a-f]{64}$")
                self.assertTrue(relative.startswith("data/ribbits/structure/houses/"))
        self.assertEqual(
            {
                "minecraft:brewing_stand": 2,
                "minecraft:damaged_anvil": 2,
            },
            tools.PRIVATE_VILLAGE_REMOVED_UTILITY_COUNTS,
        )
        expected_restored = {
            "data/ribbits/structure/houses/small_house_brown_2.nbt": (
                2_810,
                "7e7ca64fe02c9953b6e3ccf3bf2a2393c3274bbbb3848ae874b4ff8c9c6b1676",
                "minecraft:smoker",
                "65c78f8c0d18fbe8de274adf10d4d3d7e5365e37e7f7a8f95295719d0265b536",
            ),
            "data/ribbits/structure/houses/small_house_red_2.nbt": (
                2_881,
                "a91945113b28214f5be8935efdbb4c42f6ec469bf9ca9bae5074a0579023d20a",
                "minecraft:blast_furnace",
                "f0f5c4e4e4987767031407cd66b704e739a8c092bb08df7269888f172440ad25",
            ),
        }
        self.assertEqual(
            set(expected_restored), set(tools.PRIVATE_VILLAGE_RESTORED_UTILITY_SPECS)
        )
        for relative, (size, digest, block_entity, nbt_digest) in expected_restored.items():
            with self.subTest(restored=relative):
                spec = tools.PRIVATE_VILLAGE_RESTORED_UTILITY_SPECS[relative]
                self.assertEqual(size, spec["size"])
                self.assertEqual(digest, spec["sha256"])
                self.assertEqual(block_entity, spec["block_entity_id"])
                self.assertEqual(nbt_digest, spec["block_entity_nbt_sha256"])
        self.assertEqual(60, tools.PRIVATE_VILLAGE_PRESERVED_BLOCK_COUNTS["minecraft:barrel"])
        self.assertEqual(11, tools.PRIVATE_VILLAGE_PRESERVED_BLOCK_COUNTS["minecraft:chest"])
        self.assertEqual(1, tools.PRIVATE_VILLAGE_PRESERVED_BLOCK_COUNTS["minecraft:smoker"])
        self.assertEqual(
            1, tools.PRIVATE_VILLAGE_PRESERVED_BLOCK_COUNTS["minecraft:blast_furnace"]
        )
        self.assertEqual(0, tools.PRIVATE_VILLAGE_PRESERVED_BLOCK_COUNTS["minecraft:furnace"])
        self.assertEqual(
            {
                "minecraft:smoker": 1,
                "minecraft:blast_furnace": 1,
                "minecraft:furnace": 0,
            },
            tools.PRIVATE_VILLAGE_PRESERVED_BLOCK_ENTITY_COUNTS,
        )
        self.assertEqual(
            59, sum(tools.PRIVATE_VILLAGE_LOOT_BINDING_COUNTS.values())
        )

    def test_exact_rewrite_removes_block_entity_and_preserves_unrelated_record(self) -> None:
        source_state = {
            "Name": "minecraft:smoker",
            "Properties": {"lit": "false", "facing": "north"},
        }
        replacement_state = {"Name": "minecraft:stone_bricks"}
        palette = [replacement_state, source_state, {"Name": "minecraft:barrel"}]
        data = synthetic_structure_nbt(
            palette,
            [
                {
                    "position": (5, 1, 6),
                    "state": 1,
                    "block_entity_id": "minecraft:smoker",
                },
                {
                    "position": (0, 0, 0),
                    "state": 2,
                    "block_entity_id": "minecraft:barrel",
                    "loot_table": "ribbits:synthetic",
                },
            ],
        )
        relative = "data/ribbits/structure/houses/synthetic_smoker.nbt"
        spec = pin_synthetic_utility_spec(
            data,
            relative,
            (5, 1, 6),
            source_state,
            replacement_state,
            "minecraft:smoker",
        )
        transformed, record = tools.transform_exact_private_village_utility(
            data, relative, spec
        )
        transformed_again, _ = tools.transform_exact_private_village_utility(
            data, relative, spec
        )
        self.assertEqual(transformed, transformed_again)
        self.assertFalse(record["replacement_palette_state_appended"])
        self.assertTrue(record["removed_block_entity_nbt"])
        self.assertTrue(record["non_target_block_records_byte_identical"])
        inspected = tools.inspect_structure_template(transformed, relative)
        by_position = {block["position"]: block for block in inspected["blocks"]}
        self.assertEqual(replacement_state, by_position[(5, 1, 6)]["state"])
        self.assertIsNone(by_position[(5, 1, 6)]["block_entity_id"])
        self.assertEqual("minecraft:barrel", by_position[(0, 0, 0)]["block_entity_id"])
        self.assertEqual("ribbits:synthetic", by_position[(0, 0, 0)]["loot_table"])

    def test_missing_replacement_state_is_appended_deterministically(self) -> None:
        source_state = {
            "Name": "minecraft:damaged_anvil",
            "Properties": {"facing": "west"},
        }
        replacement_state = {"Name": "minecraft:air"}
        data = synthetic_structure_nbt(
            [source_state, {"Name": "minecraft:stone"}],
            [{"position": (3, 1, 5), "state": 0}],
        )
        relative = "data/ribbits/structure/houses/synthetic_anvil.nbt"
        spec = pin_synthetic_utility_spec(
            data, relative, (3, 1, 5), source_state, replacement_state, None
        )
        transformed, record = tools.transform_exact_private_village_utility(
            data, relative, spec
        )
        self.assertTrue(record["replacement_palette_state_appended"])
        inspected = tools.inspect_structure_template(transformed, relative)
        self.assertEqual(3, len(inspected["palette"]))
        self.assertEqual(replacement_state, inspected["palette"][-1])
        self.assertEqual(replacement_state, inspected["blocks"][0]["state"])

    def test_missing_shifted_duplicate_modified_and_nbt_drift_fail_closed(self) -> None:
        source_state = {
            "Name": "minecraft:damaged_anvil",
            "Properties": {"facing": "west"},
        }
        replacement_state = {"Name": "minecraft:air"}
        relative = "data/ribbits/structure/houses/synthetic_drift.nbt"

        cases = {
            "shifted": (
                synthetic_structure_nbt(
                    [source_state, replacement_state],
                    [{"position": (4, 1, 5), "state": 0}],
                ),
                "shifted",
            ),
            "duplicate": (
                synthetic_structure_nbt(
                    [source_state, replacement_state],
                    [
                        {"position": (3, 1, 5), "state": 0},
                        {"position": (4, 1, 5), "state": 0},
                    ],
                ),
                "exactly one",
            ),
            "already modified or missing": (
                synthetic_structure_nbt(
                    [source_state, replacement_state],
                    [{"position": (3, 1, 5), "state": 1}],
                ),
                "exactly one",
            ),
            "unexpected block entity": (
                synthetic_structure_nbt(
                    [source_state, replacement_state],
                    [
                        {
                            "position": (3, 1, 5),
                            "state": 0,
                            "block_entity_id": "minecraft:chest",
                        }
                    ],
                ),
                "block entity differs",
            ),
        }
        for name, (data, expected_error) in cases.items():
            with self.subTest(name=name):
                spec = {
                    "before_sha256": tools.sha256_bytes(data),
                    "after_sha256": "0" * 64,
                    "coordinate": (3, 1, 5),
                    "source_state": source_state,
                    "replacement_state": replacement_state,
                    "block_entity_id": None,
                }
                with self.assertRaisesRegex(tools.ValidationError, expected_error):
                    tools.transform_exact_private_village_utility(data, relative, spec)

        valid = synthetic_structure_nbt(
            [source_state, replacement_state],
            [{"position": (3, 1, 5), "state": 0}],
        )
        bad_hash_spec = {
            "before_sha256": "f" * 64,
            "after_sha256": "0" * 64,
            "coordinate": (3, 1, 5),
            "source_state": source_state,
            "replacement_state": replacement_state,
            "block_entity_id": None,
        }
        with self.assertRaisesRegex(tools.ValidationError, "SHA-256 differs"):
            tools.transform_exact_private_village_utility(valid, relative, bad_hash_spec)

    def test_complete_29_template_inventory_and_tree_output_are_deterministic(self) -> None:
        target_shapes = [
            ("brown_sorcerer_house.nbt", (6, 2, 5), {
                "Name": "minecraft:brewing_stand",
                "Properties": {
                    "has_bottle_0": "false",
                    "has_bottle_1": "false",
                    "has_bottle_2": "false",
                },
            }, {"Name": "minecraft:air"}, "minecraft:brewing_stand"),
            ("red_sorcerer_house.nbt", (6, 2, 5), {
                "Name": "minecraft:brewing_stand",
                "Properties": {
                    "has_bottle_0": "false",
                    "has_bottle_1": "false",
                    "has_bottle_2": "false",
                },
            }, {"Name": "minecraft:air"}, "minecraft:brewing_stand"),
            ("small_house_brown_3.nbt", (3, 1, 5), {
                "Name": "minecraft:damaged_anvil", "Properties": {"facing": "west"},
            }, {"Name": "minecraft:air"}, None),
            ("small_house_red_3.nbt", (3, 1, 5), {
                "Name": "minecraft:damaged_anvil", "Properties": {"facing": "west"},
            }, {"Name": "minecraft:air"}, None),
        ]
        payloads: dict[str, bytes] = {}
        specs: dict[str, dict[str, object]] = {}
        for filename, coordinate, source, replacement, block_entity in target_shapes:
            relative = f"data/ribbits/structure/houses/{filename}"
            palette = [replacement, source, {"Name": "minecraft:barrel"}]
            data = synthetic_structure_nbt(
                palette,
                [
                    {
                        "position": coordinate,
                        "state": 1,
                        "block_entity_id": block_entity,
                    },
                    {
                        "position": (0, 0, 0),
                        "state": 2,
                        "block_entity_id": "minecraft:barrel",
                        "loot_table": "ribbits:synthetic",
                    },
                ],
            )
            payloads[relative] = data
            specs[relative] = pin_synthetic_utility_spec(
                data, relative, coordinate, source, replacement, block_entity
            )
        restored_shapes = [
            (
                "small_house_brown_2.nbt",
                (5, 1, 6),
                {
                    "Name": "minecraft:smoker",
                    "Properties": {"lit": "false", "facing": "north"},
                },
                "minecraft:smoker",
            ),
            (
                "small_house_red_2.nbt",
                (4, 1, 2),
                {
                    "Name": "minecraft:blast_furnace",
                    "Properties": {"lit": "false", "facing": "south"},
                },
                "minecraft:blast_furnace",
            ),
        ]
        restored_specs: dict[str, dict[str, object]] = {}
        for filename, coordinate, state, block_entity in restored_shapes:
            relative = f"data/ribbits/structure/houses/{filename}"
            data = synthetic_structure_nbt(
                [state, {"Name": "minecraft:barrel"}],
                [
                    {
                        "position": coordinate,
                        "state": 0,
                        "block_entity_id": block_entity,
                    },
                    {
                        "position": (0, 0, 0),
                        "state": 1,
                        "block_entity_id": "minecraft:barrel",
                        "loot_table": "ribbits:synthetic",
                    },
                ],
            )
            payloads[relative] = data
            restored_specs[relative] = pin_synthetic_restored_utility_spec(
                data, relative, coordinate, state, block_entity
            )
        for index in range(23):
            relative = f"data/ribbits/structure/paths/synthetic_{index:02d}.nbt"
            payloads[relative] = synthetic_structure_nbt(
                [{"Name": "minecraft:barrel"}],
                [
                    {
                        "position": (0, 0, 0),
                        "state": 0,
                        "block_entity_id": "minecraft:barrel",
                        "loot_table": "ribbits:synthetic",
                    }
                ],
            )

        def write_tree(root: Path) -> None:
            for relative, payload in payloads.items():
                path = root.joinpath(*PurePosixPath(relative).parts)
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_bytes(payload)

        patches = (
            mock.patch.object(tools, "PRIVATE_VILLAGE_TEMPLATE_COUNT", 29),
            mock.patch.object(tools, "PRIVATE_VILLAGE_UTILITY_TRANSFORMS", specs),
            mock.patch.object(
                tools, "PRIVATE_VILLAGE_RESTORED_UTILITY_SPECS", restored_specs
            ),
            mock.patch.object(
                tools,
                "PRIVATE_VILLAGE_REMOVED_UTILITY_COUNTS",
                {
                    "minecraft:brewing_stand": 2,
                    "minecraft:damaged_anvil": 2,
                },
            ),
            mock.patch.object(
                tools,
                "PRIVATE_VILLAGE_PRESERVED_BLOCK_COUNTS",
                {
                    "minecraft:barrel": 29,
                    "minecraft:smoker": 1,
                    "minecraft:blast_furnace": 1,
                    "minecraft:furnace": 0,
                },
            ),
            mock.patch.object(
                tools,
                "PRIVATE_VILLAGE_PRESERVED_BLOCK_ENTITY_COUNTS",
                {
                    "minecraft:smoker": 1,
                    "minecraft:blast_furnace": 1,
                    "minecraft:furnace": 0,
                },
            ),
            mock.patch.object(tools, "PRIVATE_VILLAGE_PROCESSOR_SENTINEL_COUNTS", {}),
            mock.patch.object(
                tools, "PRIVATE_VILLAGE_LOOT_BINDING_COUNTS", {"ribbits:synthetic": 29}
            ),
        )
        with tempfile.TemporaryDirectory() as first_dir, tempfile.TemporaryDirectory() as second_dir:
            roots = (Path(first_dir), Path(second_dir))
            for root in roots:
                write_tree(root)
            with (
                patches[0],
                patches[1],
                patches[2],
                patches[3],
                patches[4],
                patches[5],
                patches[6],
                patches[7],
            ):
                first_record = tools.transform_private_village_utilities(roots[0])
                second_record = tools.transform_private_village_utilities(roots[1])
                self.assertEqual(
                    first_record["template_tree_before_sha256"],
                    second_record["template_tree_before_sha256"],
                )
                self.assertEqual(
                    first_record["template_tree_after_sha256"],
                    second_record["template_tree_after_sha256"],
                )
                self.assertEqual(4, first_record["count"])
                self.assertEqual(2, first_record["restored_count"])
                self.assertEqual(29, first_record["canonical_template_count"])
                self.assertEqual(
                    {name: 0 for name in tools.PRIVATE_VILLAGE_REMOVED_UTILITY_COUNTS},
                    first_record["removed_utility_counts_after"],
                )
                self.assertTrue(first_record["all_loot_bindings_unchanged"])
                for relative in restored_specs:
                    self.assertEqual(payloads[relative], roots[0].joinpath(
                        *PurePosixPath(relative).parts
                    ).read_bytes())
                for relative in payloads:
                    self.assertEqual(
                        roots[0].joinpath(*PurePosixPath(relative).parts).read_bytes(),
                        roots[1].joinpath(*PurePosixPath(relative).parts).read_bytes(),
                    )
                errors: list[str] = []
                tools.validate_private_village_utility_transform(roots[0], errors)
                self.assertEqual([], errors)


class SmallBrownToadstoolResourceTest(unittest.TestCase):
    @staticmethod
    def synthetic_brown_palette_png() -> bytes:
        pixels: list[tuple[int, int, int, int]] = []
        for color, count in tools.SMALL_BROWN_FULL_DONOR_PALETTE_COUNTS.items():
            pixels.extend([color] * count)
        return tools.encode_rgba_png(16, 16, tools._rgba_bytes(pixels))

    @staticmethod
    def synthetic_block_model(
        element_count: int, particle: str = "ribbits:block/toadstool"
    ) -> dict[str, object]:
        elements = [
            {
                "from": [index, 0, 0],
                "to": [index + 1, 1, 1],
                "rotation": {"angle": 0, "axis": "y", "origin": [8, 8, 8]},
                "faces": {
                    "north": {"uv": [0, 0, 1, 1], "texture": "#0"},
                },
            }
            for index in range(element_count)
        ]
        return {
            "credit": "Made with Blockbench",
            "parent": "block/block",
            "texture_size": [64, 64],
            "textures": {
                "0": "ribbits:block/toadstool",
                "particle": particle,
            },
            "elements": elements,
            "groups": [
                {
                    "name": "main",
                    "origin": [8, 8, 8],
                    "color": 0,
                    "children": [
                        {
                            "name": "mushroom",
                            "origin": [8, 8, 8],
                            "color": 0,
                            "children": list(range(element_count)),
                        }
                    ],
                }
            ],
        }

    def test_indexed_png_decoder_preserves_palette_alpha(self) -> None:
        ihdr = struct.pack(">IIBBBBB", 2, 2, 8, 3, 0, 0, 0)
        palette = bytes((10, 20, 30, 40, 50, 60))
        transparency = bytes((255, 0))
        scanlines = b"\x00\x00\x01\x00\x01\x00"
        png = (
            b"\x89PNG\r\n\x1a\n"
            + tools._png_chunk(b"IHDR", ihdr)
            + tools._png_chunk(b"PLTE", palette)
            + tools._png_chunk(b"tRNS", transparency)
            + tools._png_chunk(b"IDAT", zlib.compress(scanlines))
            + tools._png_chunk(b"IEND", b"")
        )
        self.assertEqual(
            (
                2,
                2,
                bytes(
                    (
                        10, 20, 30, 255,
                        40, 50, 60, 0,
                        40, 50, 60, 0,
                        10, 20, 30, 255,
                    )
                ),
            ),
            tools.decode_indexed_png(png, "synthetic indexed fixture"),
        )

    def test_authoritative_recolor_donor_loader_is_exact_and_hash_guarded(self) -> None:
        atlas = tools.encode_rgba_png(64, 64, bytes((105, 34, 21, 255)) * (64 * 64))
        brown = tools.encode_rgba_png(16, 16, bytes((4, 5, 6, 255)) * 256)
        models: dict[str, tuple[dict[str, object], bytes, str]] = {}
        for index, member in enumerate(tools.SMALL_BROWN_DONOR_MODEL_MEMBERS):
            particle = (
                "ribbits:block/red_toadstool"
                if index == 0
                else "ribbits:block/toadstool"
            )
            model = self.synthetic_block_model(index + 1, particle)
            payload = json.dumps(model, separators=(",", ":")).encode("utf-8")
            models[member] = (model, payload, particle)
        with tempfile.TemporaryDirectory() as temp_dir:
            originals = Path(temp_dir) / "originals"
            assets = originals / "assets"
            assets.mkdir(parents=True)
            donor = assets / "Matcha-Overlays-v37.zip"
            with zipfile.ZipFile(donor, "w") as archive:
                archive.writestr(tools.SMALL_BROWN_DONOR_BLOCK_ATLAS_MEMBER, atlas)
                archive.writestr(tools.SMALL_BROWN_DONOR_PALETTE_MEMBER, brown)
                for member, (_, payload, _) in models.items():
                    archive.writestr(member, payload)
                archive.writestr("assets/ribbits/textures/item/unrelated.png", b"ignored")
            spec = {
                "filename": donor.name,
                "size": donor.stat().st_size,
                "sha256": tools.sha256_file(donor),
                "members": {
                    tools.SMALL_BROWN_DONOR_BLOCK_ATLAS_MEMBER: {
                        "size": len(atlas),
                        "sha256": tools.sha256_bytes(atlas),
                        "dimensions": (64, 64),
                    },
                    tools.SMALL_BROWN_DONOR_PALETTE_MEMBER: {
                        "size": len(brown),
                        "sha256": tools.sha256_bytes(brown),
                        "dimensions": (16, 16),
                    },
                    **{
                        member: {
                            "size": len(payload),
                            "sha256": tools.sha256_bytes(payload),
                            "element_count": len(model["elements"]),
                            "geometry_sha256": tools.small_brown_model_geometry_sha256(
                                model
                            ),
                            "particle": particle,
                        }
                        for member, (model, payload, particle) in models.items()
                    },
                },
            }
            with mock.patch.object(tools, "SMALL_BROWN_RECOLOR_DONOR_SPEC", spec):
                path, members, identity = (
                    tools.load_authoritative_small_brown_recolor_donor(originals)
                )
                self.assertEqual(donor, path)
                self.assertEqual(set(spec["members"]), set(members))
                self.assertEqual(spec["sha256"], identity["sha256"])
                self.assertEqual(6, len(identity["approved_members"]))
                for member in tools.SMALL_BROWN_DONOR_MODEL_MEMBERS:
                    self.assertEqual(
                        len(models[member][0]["elements"]),
                        identity["approved_members"][member]["element_count"],
                    )
                    self.assertEqual(
                        0,
                        identity["approved_members"][member]["flat_element_count"],
                    )
                donor.write_bytes(donor.read_bytes() + b"tamper")
                with self.assertRaisesRegex(tools.ValidationError, "changed during"):
                    tools.require_donor_unchanged(
                        donor, identity, "synthetic Matcha donor"
                    )

    def test_donor_models_allow_only_brown_bindings_and_cutout_migration(self) -> None:
        members: dict[str, dict[str, object]] = {}
        payloads: dict[str, bytes] = {}
        for index, member in enumerate(tools.SMALL_BROWN_DONOR_MODEL_MEMBERS):
            particle = (
                "ribbits:block/red_toadstool"
                if index == 0
                else "ribbits:block/toadstool"
            )
            model = self.synthetic_block_model(index + 1, particle)
            payload = json.dumps(model, separators=(",", ":")).encode("utf-8")
            payloads[member] = payload
            members[member] = {
                "size": len(payload),
                "sha256": tools.sha256_bytes(payload),
                "element_count": index + 1,
                "geometry_sha256": tools.small_brown_model_geometry_sha256(model),
                "particle": particle,
            }
        spec = {"members": members}
        with mock.patch.object(tools, "SMALL_BROWN_RECOLOR_DONOR_SPEC", spec):
            for member in tools.SMALL_BROWN_DONOR_MODEL_MEMBERS:
                donor = json.loads(payloads[member])
                derived, record = tools.derive_small_brown_block_model(
                    member, payloads[member]
                )
                self.assertEqual(
                    {
                        "0": "ribbits:block/small_brown_toadstool",
                        "particle": "ribbits:block/small_brown_toadstool",
                    },
                    derived["textures"],
                )
                self.assertEqual("cutout", derived["render_type"])
                for key in ("credit", "parent", "texture_size", "elements", "groups"):
                    self.assertEqual(donor[key], derived[key])
                self.assertEqual(0, record["flat_element_count"])
                self.assertTrue(
                    record[
                        "geometry_matches_donor_after_permitted_metadata_normalization"
                    ]
                )
            tampered_member = tools.SMALL_BROWN_DONOR_MODEL_MEMBERS[0]
            tampered = json.loads(payloads[tampered_member])
            tampered["elements"][0]["faces"]["north"]["uv"][0] = 0.25
            with self.assertRaisesRegex(tools.ValidationError, "geometry differs"):
                tools.derive_small_brown_block_model(
                    tampered_member,
                    json.dumps(tampered, separators=(",", ":")).encode("utf-8"),
                )

        flat = self.synthetic_block_model(1)
        flat["elements"][0]["to"][0] = flat["elements"][0]["from"][0]
        with self.assertRaisesRegex(tools.ValidationError, "zero-thickness"):
            tools.require_small_brown_block_model(
                flat,
                "synthetic flat donor",
                1,
                {
                    "0": "ribbits:block/toadstool",
                    "particle": "ribbits:block/toadstool",
                },
                None,
            )

    def test_user_item_sprite_is_centered_and_blue_is_transparent(self) -> None:
        blue = tools.SMALL_BROWN_ITEM_DONOR_SPEC["placeholder_blue"]
        pixels = [blue] * (14 * 14)
        pixels[7 * 14 + 7] = (115, 82, 58, 255)
        source_png = tools.encode_rgba_png(14, 14, tools._rgba_bytes(pixels))
        with mock.patch.object(tools, "SMALL_BROWN_ITEM_DONOR_SPEC", {
            **tools.SMALL_BROWN_ITEM_DONOR_SPEC, "dimensions": (14, 14)
        }):
            output_png, metadata = tools.derive_small_brown_item_texture(source_png)
        width, height, output_raw = tools.decode_rgba_png(output_png, "synthetic output")
        self.assertEqual((16, 16), (width, height))
        output = tools._rgba_pixels(output_raw)
        self.assertTrue(all(output[index][3] == 0 for index in range(16)))
        self.assertTrue(all(output[index][3] == 0 for index in range(240, 256)))
        self.assertEqual((115, 82, 58, 255), output[8 * 16 + 8])
        self.assertNotIn(blue, output)
        self.assertTrue(metadata["transparent_one_pixel_border"])

    def test_block_recolor_changes_only_derived_cap_mask(self) -> None:
        coordinates = [(x, y) for y in range(64) for x in range(64)]
        cap = set(coordinates[:960])
        stem = set(coordinates[960:1267])
        pixels = [(0, 0, 0, 0)] * (64 * 64)
        cap_colors: list[tuple[int, int, int, int]] = []
        for color, count in zip(
            tuple(tools.SMALL_BROWN_ITEM_CAP_RECOLOR),
            (185, 234, 279, 262),
        ):
            cap_colors.extend([color] * count)
        for coordinate, color in zip(sorted(cap), cap_colors):
            pixels[coordinate[1] * 64 + coordinate[0]] = color
        for coordinate in stem:
            pixels[coordinate[1] * 64 + coordinate[0]] = (
                (128, 112, 96, 255)
            )
        source_png = tools.encode_rgba_png(64, 64, tools._rgba_bytes(pixels))
        output_png, metadata = tools.derive_small_brown_block_texture(
            source_png, self.synthetic_brown_palette_png()
        )
        _, _, output_raw = tools.decode_rgba_png(output_png, "synthetic block output")
        output = tools._rgba_pixels(output_raw)
        for index, source in enumerate(pixels):
            coordinate = (index % 64, index // 64)
            expected = (
                tools.SMALL_BROWN_ITEM_CAP_RECOLOR[source]
                if coordinate in cap
                else source
            )
            self.assertEqual(expected, output[index])
        self.assertEqual(960, metadata["cap_pixels"])
        self.assertEqual(307, metadata["stem_pixels_preserved"])

    def test_reference_scope_rejects_json_and_nbt_leaks(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            for relative, count in tools.SMALL_BROWN_JSON_REFERENCE_COUNTS.items():
                path = root / PurePosixPath(relative)
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_text(
                    json.dumps(["ribbits:small_brown_toadstool"] * count),
                    encoding="utf-8",
                )
            nbt_paths = []
            for index in range(29):
                path = root / f"data/ribbits/structure/synthetic_{index:02d}.nbt"
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_bytes(
                    synthetic_structure_nbt(
                        [{"Name": "minecraft:stone"}],
                        [{"position": (0, 0, 0), "state": 0}],
                    )
                )
                nbt_paths.append(path)
            tools.require_small_brown_reference_scope(root)

            forbidden_json = root / "data/ribbits/worldgen/biome/forbidden.json"
            forbidden_json.parent.mkdir(parents=True, exist_ok=True)
            forbidden_json.write_text(
                json.dumps({"Name": "ribbits:small_brown_toadstool"}),
                encoding="utf-8",
            )
            with self.assertRaisesRegex(tools.ValidationError, "reference scope differs"):
                tools.require_small_brown_reference_scope(root)
            forbidden_json.unlink()

            leaked_nbt = (
                b"\x0a"
                + nbt_string("")
                + named_string("marker", "ribbits:small_brown_toadstool")
                + b"\x00"
            )
            nbt_paths[0].write_bytes(tools.deterministic_gzip(leaked_nbt))
            with self.assertRaisesRegex(tools.ValidationError, "leaked into protected"):
                tools.require_small_brown_reference_scope(root)

    def test_matcha_archive_basename_is_forbidden_from_packaging(self) -> None:
        candidate = "assets/ribbits/Matcha-Overlays-v37.zip"
        self.assertEqual(
            [candidate], tools.source_only_donor_violations([candidate])
        )
        self.assertEqual(108, 84 + 24)
        self.assertEqual(151, 108 + 43)
        self.assertEqual(
            "89600a4f4212733a03d6634423b7805fc8277e7edaa2dad9f91f6b58ea1f51df",
            tools.SMALL_BROWN_ITEM_MASK_SHA256,
        )
        self.assertEqual(6, len(tools.NATIVE_BROWN_TOADSTOOL_RESOURCE_SPECS))


class DonorBoundaryContractTest(unittest.TestCase):
    def test_exact_accounting_contains_only_approved_visual_members_and_outputs(self) -> None:
        self.assertEqual("4.1.6+26.2-mynx-canary29", tools.CANDIDATE_VERSION)
        self.assertEqual(29, tools.CANDIDATE_CANARY)
        self.assertEqual(
            "mynx-ribbits-private-resource-manifest/v1", tools.PRIVATE_MANIFEST_SCHEMA
        )
        self.assertEqual(
            "PRIVATE MYNX ASSEMBLY STAGED / NONREDISTRIBUTABLE DONOR ASSETS",
            tools.PRIVATE_MANIFEST_CLASSIFICATION,
        )
        self.assertEqual(363, tools.OUTPUT_FILE_COUNT)
        self.assertEqual(2_780_713, tools.OUTPUT_TOTAL_SIZE)
        self.assertEqual(2_563, tools.SORCERER_LOOT_OUTPUT_SIZE)
        self.assertEqual(
            "5b06e06502bf11f661161e89bf34e329d8f23268b7b0104371038c38ad9b378d",
            tools.SORCERER_LOOT_OUTPUT_SHA256,
        )
        self.assertEqual(
            "9c2f704975baf2fe7e1c530c85a82cc1a69116be609ee769615c422cfb8d0499",
            tools.PRIVATE_VILLAGE_TEMPLATE_TREE_BEFORE_SHA256,
        )
        self.assertEqual(
            "cb1cac748727cc4387092cee0d77426816204d0986866d44e9995d6948468de0",
            tools.PRIVATE_VILLAGE_TEMPLATE_TREE_AFTER_SHA256,
        )
        self.assertEqual(42, len(tools.GECKO_MODEL_IDS))
        self.assertEqual(27, len(tools.REGISTERED_ITEM_IDS))
        self.assertEqual({"chute_leaf_open", "chute_leaf_closed"}, tools.AUXILIARY_ITEM_DEFINITION_IDS)
        self.assertNotIn("glowcap", tools.REGISTERED_ITEM_IDS)
        self.assertNotIn("toadstool_heart", tools.REGISTERED_ITEM_IDS)
        self.assertEqual(10, len(tools.SPAWN_EGG_IDS))
        self.assertIn("chute_leaf", tools.REGISTERED_ITEM_IDS)
        self.assertIn("small_brown_toadstool", tools.REGISTERED_ITEM_IDS)
        self.assertEqual(
            {"alexsmobs", "minecraft", "ribbits", "trinkets"},
            tools.EXPECTED_PRIVATE_DATA_NAMESPACES,
        )
        self.assertEqual(27, len(tools.DONOR_DERIVED_OUTPUTS))
        self.assertEqual(3, len(tools.USER_AUTHORED_CHUTE_OUTPUTS))
        self.assertEqual(12, len(tools.SMALL_BROWN_TOADSTOOL_DERIVED_OUTPUTS))
        self.assertEqual(30, len(tools.CUTOUT_MODEL_FILES))
        self.assertEqual(
            13,
            sum(len(spec["members"]) for spec in tools.DONOR_INPUT_SPECS.values()),
        )
        self.assertEqual(
            7, len(tools.SMALL_BROWN_RECOLOR_DONOR_SPEC["members"])
        )
        self.assertEqual(
            [7, 8, 7, 7],
            [
                tools.SMALL_BROWN_RECOLOR_DONOR_SPEC["members"][member][
                    "element_count"
                ]
                for member in tools.SMALL_BROWN_DONOR_MODEL_MEMBERS
            ],
        )
        for spec in tools.DONOR_INPUT_SPECS.values():
            for member in spec["members"]:
                self.assertTrue(member.endswith((".json", ".png")))
                self.assertFalse(member.endswith((".class", ".java")))
                self.assertNotIn("/animations/", member)
        for output in tools.DONOR_DERIVED_OUTPUTS:
            self.assertTrue(output.endswith((".json", ".png")))
            self.assertTrue(output.startswith("assets/ribbits/"))

    def test_wandering_visual_import_is_exact_and_bounded(self) -> None:
        model_member = "assets/wandering_ribbit/geo/wandering_ribbit.geo.json"
        texture_member = "assets/wandering_ribbit/textures/entity/wandering_ribbit.png"
        chute_model_member = "assets/wandering_ribbit/models/custom/umbrella_leaf.json"
        closed_member = "assets/wandering_ribbit/textures/item/umbrella_leaf_item.png"
        open_member = "assets/wandering_ribbit/textures/item/umbrella_leaf_texture.png"
        payloads = {
            model_member: json.dumps({"format_version": "1.12.0", "minecraft:geometry": [{
                "description": {"texture_width": 128, "texture_height": 128},
                "bones": [
                    {"name": "main"},
                    {"name": "body", "cubes": [{"sentinel": "preserve"}]},
                    {"name": "left_arm", "parent": "body"},
                    {"name": "leaf", "parent": "body"},
                    {"name": "umbrella_leaf", "parent": "left_arm"},
                    {"name": "grip", "parent": "umbrella_leaf", "cubes": [
                        {"origin": [4, 5, 2.75], "size": [0.5, 15, 0.5], "uv": [97, 64]}
                    ]},
                    {"name": "leaf2", "parent": "umbrella_leaf", "cubes": [
                        {"origin": [-4.5, 20, -5.5], "size": [17, 0, 17], "uv": [61, 47]}
                    ]},
                ]
            }]}).encode(),
            texture_member: tools.encode_rgba_png(
                128, 128, bytes((35, 95, 38, 255)) * (128 * 128)
            ),
            chute_model_member: json.dumps(
                {
                    "parent": "minecraft:builtin/entity",
                    "display": {"gui": {"rotation": [30, 225, 0]}},
                    "elements": [],
                }
            ).encode("utf-8"),
            closed_member: b"closed-png",
            open_member: b"open-png",
        }
        self.assertEqual(
            set(payloads), set(tools.DONOR_INPUT_SPECS["wandering"]["members"])
        )

        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            user_png = tools.encode_rgba_png(32, 32, bytes((12, 34, 56, 255)) * (32 * 32))
            user_model = {
                "ambientocclusion": True,
                "elements": [
                    {"type": "cube", "export": True, "from": [4.5, 0, 7.75], "to": [5, 22, 8.25],
                     "faces": {face: {"uv": [0, 0, 1, 1]} for face in ("north", "east", "south", "west", "up", "down")}},
                    {"type": "cube", "export": True, "from": [0.25, 22, -2.5], "to": [9.25, 22, 8.5],
                     "faces": {face: {"uv": [20, 30, 11, 19]} for face in ("north", "east", "south", "west", "up", "down")}},
                ],
                "display": {},
            }
            records, user_records = tools.import_wandering_visual_resources(
                root,
                payloads,
                {"filename": tools.DONOR_INPUT_SPECS["wandering"]["filename"]},
                user_model,
                user_png,
            )
            self.assertEqual(
                tools.WANDERING_DONOR_DERIVED_OUTPUTS,
                {record["output"] for record in records},
            )
            self.assertEqual(
                {"format_version": "1.12.0", "minecraft:geometry": [{
                    "description": {"texture_width": 128, "texture_height": 128},
                    "bones": json.loads(payloads[model_member])["minecraft:geometry"][0]["bones"][:-3],
                }]},
                tools.load_json(root / "assets/ribbits/geckolib/models/wandering_ribbit.geo.json"),
            )
            self.assertEqual(
                payloads[texture_member],
                (root / "assets/ribbits/textures/entity/wandering_ribbit.png").read_bytes(),
            )
            self.assertEqual(
                (Path(tools.__file__).parent / "assets/chute_leaf.png").read_bytes(),
                (root / "assets/ribbits/textures/item/chute_leaf.png").read_bytes(),
            )
            self.assertEqual(
                user_png,
                (root / "assets/ribbits/textures/item/chute_leaf_open.png").read_bytes(),
            )
            self.assertEqual(
                (32, 32),
                tools.png_dimensions(
                    (root / "assets/ribbits/textures/item/chute_leaf_open.png").read_bytes(),
                    "assembled open leaf",
                ),
            )
            self.assertEqual(tools.USER_AUTHORED_CHUTE_OUTPUTS, {record["output"] for record in user_records})
            self.assertEqual(
                {
                    "model": {
                        "type": "minecraft:model",
                        "model": "ribbits:item/chute_leaf_open",
                    }
                },
                tools.load_json(root / "assets/ribbits/items/chute_leaf_open.json"),
            )
            self.assertEqual((Path(tools.__file__).parent / "assets/drop_leaf_inventory.png").read_bytes(),
                             (root / "assets/ribbits/textures/item/drop_leaf_inventory.png").read_bytes())
            self.assertEqual({"parent": "minecraft:item/generated", "textures": {"layer0": "ribbits:item/drop_leaf_inventory"}},
                             tools.load_json(root / "assets/ribbits/models/item/chute_leaf.json"))
            self.assertEqual({"parent": "minecraft:item/generated", "textures": {"layer0": "ribbits:item/chute_leaf"}},
                             tools.load_json(root / "assets/ribbits/models/item/chute_leaf_closed.json"))
            self.assertEqual({"model": {"type": "minecraft:model", "model": "ribbits:item/chute_leaf_closed"}},
                             tools.load_json(root / "assets/ribbits/items/chute_leaf_closed.json"))
            open_model = tools.load_json(
                root / "assets/ribbits/models/item/chute_leaf_open.json"
            )
            self.assertEqual(2, len(open_model["elements"]))
            self.assertEqual([4.5, 0.0, 7.75], open_model["elements"][0]["from"])
            self.assertEqual([5.0, 22.0, 8.25], open_model["elements"][0]["to"])
            self.assertEqual([0.25, 22.0, -2.5], open_model["elements"][1]["from"])
            self.assertEqual([9.25, 22.0, 8.5], open_model["elements"][1]["to"])
            self.assertEqual({"north", "east", "south", "west", "up", "down"}, set(open_model["elements"][1]["faces"]))
            self.assertTrue(all("cullface" not in face for face in open_model["elements"][1]["faces"].values()))
            self.assertEqual(
                {
                    "layer0": "ribbits:item/chute_leaf_open",
                    "particle": "ribbits:item/chute_leaf_open",
                },
                open_model["textures"],
            )
            source_elements = user_model["elements"]
            for source, generated in zip(source_elements, open_model["elements"], strict=True):
                self.assertEqual(source["from"], generated["from"])
                self.assertEqual(source["to"], generated["to"])
                for direction, source_face in source["faces"].items():
                    generated_face = generated["faces"][direction]
                    self.assertEqual("#layer0", generated_face["texture"])
                    self.assertEqual(
                        tools.normalize_bbmodel_face_uv(source_face["uv"], (32, 32)),
                        generated_face["uv"],
                    )
            self.assertEqual(
                [10.0, 15.0, 5.5, 9.5],
                open_model["elements"][1]["faces"]["up"]["uv"],
            )
            # Flipped UV ordering and fractional values are semantic and survive scaling.
            self.assertEqual(
                [10.0, 9.5, 5.5, 15.0],
                tools.normalize_bbmodel_face_uv([20, 19, 11, 30], (32, 32)),
            )
            self.assertNotIn("ribbits:entity/wandering_ribbit", json.dumps(open_model))
            self.assertFalse((root / "assets/ribbits/geckolib/animations/wandering.json").exists())

    def test_bbmodel_uv_normalization_uses_each_texture_dimension(self) -> None:
        texture = tools.encode_rgba_png(64, 32, bytes((12, 34, 56, 255)) * (64 * 32))
        model = {"resolution": {"width": 64, "height": 32}}
        dimensions = tools.resolve_bbmodel_texture_dimensions(model, texture, "synthetic texture")
        self.assertEqual((64, 32), dimensions)
        self.assertEqual(
            [5.0, 15.0, 2.75, 9.5],
            tools.normalize_bbmodel_face_uv([20, 30, 11, 19], dimensions),
        )
        with self.assertRaisesRegex(tools.ValidationError, "differs from its exact PNG"):
            tools.resolve_bbmodel_texture_dimensions(
                {"resolution": {"width": 32, "height": 32}}, texture, "synthetic texture"
            )

    def test_polish_pixels_preserve_artwork_exactly(self) -> None:
        source = Path(tools.__file__).resolve().parents[1] / "common/src/main/resources/assets/ribbits/textures"
        w, h, pixels = tools.decode_rgba_png((source / "item/glowcap.png").read_bytes(), "glowcap")
        self.assertEqual((16, 16), (w, h))
        self.assertEqual(bytes(64), pixels[:64])
        # Moving back up recovers every original RGBA byte, including transparent RGB.
        self.assertEqual("b7b9cdcd1dd03452699011e8b4b970bb8b047dba8b807620448e1d5a6d82b0d2",
                         tools.sha256_bytes(pixels[64:] + bytes(64)))
        w, h, pixels = tools.decode_rgba_png((source / "map/decorations/ribbit_village.png").read_bytes(), "marker")
        self.assertEqual((8, 8), (w, h))
        restored = bytearray(16 * 16 * 4)
        for y in range(8):
            restored[((y + 4) * 16 + 4) * 4:((y + 4) * 16 + 12) * 4] = pixels[y * 32:(y + 1) * 32]
        self.assertEqual("ab282bfb3c9099ecdcea98d66c9e382024e0cebc0b04f26589d3bea979598458",
                         tools.sha256_bytes(restored))
        inventory = (Path(tools.__file__).parent / "assets/drop_leaf_inventory.png").read_bytes()
        self.assertEqual((16, 16), tools.png_dimensions(inventory, "inventory leaf"))
        self.assertEqual("5acbe4afc118b2ec1a04ec5a2dcfd91f7db05bd61937019a75300756cc257a30",
                         tools.sha256_bytes(inventory))
        leaf = (Path(tools.__file__).parent / "assets/chute_leaf.png").read_bytes()
        self.assertEqual((16, 16), tools.png_dimensions(leaf, "user leaf"))
        self.assertEqual("816e4d4edc23542afeb2f2f90a5af8a2076ae61829f1acb016711e05fec0191d",
                         tools.sha256_bytes(leaf))

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
            "assets/wandering_ribbit/geo/wandering_ribbit.geo.json",
            "com/cosmicbarri/wandering_ribbit/entity/WanderingRibbitEntity.class",
            "nested/wandering_ribbit-4.0-forge.jar",
        }
        self.assertEqual(9, len(tools.source_only_donor_violations(forbidden)))

    def test_private_jar_donor_boundary_uses_real_guard_prefix_and_jar_basenames(self) -> None:
        violations = tools.nonallowlisted_donor_archive_violations(
            {
                "sunbatheproductions28/guardribbits/entity/GuardRibbitEntity.class",
                "me/rogue_one/useful_ribbits/procedures/RBGUIChefBtnProcedure.class",
                "nested/deeper/GuardRibbits-1.20.1-Fabric-1.0.4.jar",
                "useful_ribbits-1.0.2-forge-1.20.1.jar",
                "assets/wandering_ribbit/models/custom/umbrella_leaf.json",
                "com/cosmicbarri/wandering_ribbit/WanderingRibbit.class",
                "nested/deeper/wandering_ribbit-4.0-forge.jar",
                "com/yungnickyoung/minecraft/ribbits/RibbitsCommon.class",
            }
        )
        self.assertEqual(7, len(violations))
        self.assertNotIn(
            "com/yungnickyoung/minecraft/ribbits/RibbitsCommon.class", violations
        )

    def test_zip_entry_paths_and_staging_destinations_fail_closed(self) -> None:
        for unsafe in (
            r"assets\guardribbits\geo\guard_ribbit.geo.json",
            "C:" + r"\donors\GuardRibbits.jar",
            "C:" + "/donors/GuardRibbits.jar",
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
            "assets/ribbits/items/ribbit_village_explorer_map.json",
            "assets/ribbits/items/toadstool_heart.json",
            "assets/ribbits/models/item/glowcap.json",
            "assets/ribbits/models/item/ribbit_village_explorer_map.json",
            "assets/ribbits/models/item/toadstool_heart.json",
            "assets/ribbits/textures/item/glowcap.png",
            "assets/ribbits/textures/item/ribbit_village_explorer_map.png",
            "assets/ribbits/textures/item/toadstool_heart.png",
            "assets/ribbits/textures/map/decorations/ribbit_village.png",
            "data/ribbits/advancement/recipes/misc/toadstool_heart.json",
            "data/ribbits/item_modifier/ribbit_village_explorer_result.json",
            "data/ribbits/loot_table/chests/swamp_hut_map.json",
            "data/ribbits/recipe/toadstool_heart.json",
            "data/ribbits/tags/worldgen/structure/on_ribbit_village_explorer_maps.json",
            "data/ribbits/tags/worldgen/biome/allows_wandering_ribbit_spawns.json",
            "data/ribbits/tags/worldgen/biome/without_wandering_ribbit_spawns.json",
            "data/trinkets/tags/item/chest/cape.json",
        }
        self.assertEqual(expected_paths, tools.SOURCE_SAFE_PUBLIC_RESOURCE_PATHS)

        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            source_root = root / "tracked-resources"
            archive_path = root / "private.jar"
            canonical_root = (
                Path(__file__).resolve().parent.parent
                / "common"
                / "src"
                / "main"
                / "resources"
            )
            expected_bytes: dict[str, bytes] = {}
            for index, relative in enumerate(sorted(expected_paths)):
                canonical = canonical_root.joinpath(*PurePosixPath(relative).parts)
                payload = (
                    canonical.read_bytes()
                    if relative in tools.FINAL_ITEM_RESOURCE_MODELS
                    or relative in tools.FINAL_ITEM_SPRITE_SPECS
                    else f"source-safe-{index}\n".encode("utf-8")
                )
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

    def test_final_item_sprites_and_models_match_exact_approved_identities(self) -> None:
        source_root = (
            Path(__file__).resolve().parent.parent
            / "common"
            / "src"
            / "main"
            / "resources"
        )
        errors: list[str] = []
        tools.validate_final_item_public_resources(source_root, errors)
        self.assertEqual([], errors)
        self.assertEqual(
            [
                {
                    "item": "ribbits:glowcap",
                    "source_filename": "glowcap_16x16_down1.png",
                    "path": "assets/ribbits/textures/item/glowcap.png",
                    "size": 221,
                    "sha256": "9f79b36007a4a4e5e0b5683264c335d76c0f7e00572116b74a33b9e26c0d5abe",
                    "dimensions": [16, 16],
                    "format": "non-interlaced 8-bit RGBA PNG",
                    "alpha": {
                        "transparent_pixels": 178,
                        "opaque_pixels": 78,
                        "partial_alpha_pixels": 0,
                    },
                    "packaged_bytes": "exact tracked sprite bytes",
                },
                {
                    "item": "ribbits:ribbit_village_explorer_map",
                    "source_filename": "610cbaa3-e4e8-4d55-abb9-377f0545e672.png",
                    "path": "assets/ribbits/textures/item/ribbit_village_explorer_map.png",
                    "size": 506,
                    "sha256": "6065e126da4d3d70725cc3adca725e2ce2812ba8a0155a07c1e510b373aa38f5",
                    "dimensions": [16, 16],
                    "format": "non-interlaced 8-bit RGBA PNG",
                    "alpha": {
                        "transparent_pixels": 33,
                        "opaque_pixels": 223,
                        "partial_alpha_pixels": 0,
                    },
                    "packaged_bytes": "exact tracked sprite bytes",
                },
                {
                    "item": "ribbits:toadstool_heart",
                    "source_filename": "4e8e067e-3969-49ea-be28-fb8d91ea932b.png",
                    "path": "assets/ribbits/textures/item/toadstool_heart.png",
                    "size": 881,
                    "sha256": "024773d1cccfbe15ba4378b53b09d8522e6157c6ef7cb6e693a99ac8ae36ecb0",
                    "dimensions": [16, 16],
                    "format": "non-interlaced 8-bit RGBA PNG",
                    "alpha": {
                        "transparent_pixels": 152,
                        "opaque_pixels": 104,
                        "partial_alpha_pixels": 0,
                    },
                    "packaged_bytes": "exact tracked sprite bytes",
                },
                {
                    "item": "ribbits:ribbit_village_explorer_map",
                    "source_filename": "ribbit_village_marker_8x8.png",
                    "path": "assets/ribbits/textures/map/decorations/ribbit_village.png",
                    "size": 122,
                    "sha256": "bd05ccfbe6ade532d20fc42f05c665074a0a733a9398069d8d766b515aba861d",
                    "dimensions": [8, 8],
                    "format": "non-interlaced 8-bit RGBA PNG",
                    "alpha": {
                        "transparent_pixels": 30,
                        "opaque_pixels": 34,
                        "partial_alpha_pixels": 0,
                    },
                    "packaged_bytes": "exact tracked sprite bytes",
                },
            ],
            tools.final_item_sprite_manifest_records(),
        )

        glowcap = source_root / "assets/ribbits/textures/item/glowcap.png"
        with tempfile.TemporaryDirectory() as temp_dir:
            tampered_root = Path(temp_dir)
            for relative in tools.FINAL_ITEM_RESOURCE_MODELS:
                destination = tampered_root.joinpath(*PurePosixPath(relative).parts)
                destination.parent.mkdir(parents=True, exist_ok=True)
                destination.write_bytes(
                    source_root.joinpath(*PurePosixPath(relative).parts).read_bytes()
                )
            for relative in tools.FINAL_ITEM_SPRITE_SPECS:
                destination = tampered_root.joinpath(*PurePosixPath(relative).parts)
                destination.parent.mkdir(parents=True, exist_ok=True)
                destination.write_bytes(
                    source_root.joinpath(*PurePosixPath(relative).parts).read_bytes()
                )
            tampered = bytearray(glowcap.read_bytes())
            tampered[-1] ^= 1
            tampered_root.joinpath(
                *PurePosixPath("assets/ribbits/textures/item/glowcap.png").parts
            ).write_bytes(tampered)
            tampered_errors: list[str] = []
            tools.validate_final_item_public_resources(tampered_root, tampered_errors)
            self.assertTrue(tampered_errors)

    def test_private_jar_requires_exact_economy_runtime_dependencies(self) -> None:
        self.assertEqual(
            ">=4.1.0-beta.3",
            tools.REQUIRED_FABRIC_DEPENDENCIES["trinkets_updated"],
        )
        self.assertEqual(">=4.0.0", tools.REQUIRED_FABRIC_DEPENDENCIES["customportals"])
        self.assertEqual(
            ">=0.1.10-0",
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

    def test_musician_and_five_new_egg_translations_are_exact(self) -> None:
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
                if key.startswith("item.ribbits.")
                and key.endswith("_spawn_egg")
                and "nitwit" not in key
            },
        )
        self.assertEqual(
            "Wandering Ribbit Spawn Egg",
            tools.EN_US_MYNX_PROFESSION_TRANSLATIONS[
                "item.ribbits.wandering_ribbit_spawn_egg"
            ],
        )

    def test_economy_item_and_rank_title_translations_are_exact(self) -> None:
        expected_items = {
            "item.ribbits.glowcap": "Glowcap",
            "item.ribbits.toadstool_heart": "Toadstool Heart",
            "item.ribbits.ribbit_village_explorer_map": "Ribbit Village Explorer Map",
            "item.ribbits.uncharted_ribbit_map": "Uncharted Ribbit Map",
            "item.ribbits.uncharted_ribbit_map.lore":
                "No Ribbit village could be charted.",
        }
        self.assertEqual(
            expected_items,
            {
                key: tools.EN_US_MYNX_PROFESSION_TRANSLATIONS[key]
                for key in expected_items
            },
        )
        public_language = tools.load_json(
            Path(__file__).resolve().parent.parent
            / "common/src/publicResources/assets/ribbits/lang/en_us.json"
        )
        self.assertEqual(
            tools.PHASE_C_MAP_TRANSLATIONS,
            {
                key: public_language.get(key)
                for key in tools.PHASE_C_MAP_TRANSLATIONS
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

    def test_naturalist_baby_translation_is_public_and_private_assembly_contract(self) -> None:
        public_language = tools.load_json(
            Path(__file__).resolve().parent.parent
            / "common/src/publicResources/assets/ribbits/lang/en_us.json"
        )
        expected = {"trade.ribbits.naturalist_fauna.baby": "Baby %s"}
        self.assertEqual(expected, tools.EN_US_NATURALIST_FAUNA_TRANSLATIONS)
        self.assertEqual(expected, {key: public_language.get(key) for key in expected})


if __name__ == "__main__":
    unittest.main()
