#!/usr/bin/env python3
"""Source-safe regression tests for the tracked private-resource assembler."""

from __future__ import annotations

import copy
import json
import tempfile
import unittest
from pathlib import Path, PurePosixPath

import private_resource_tools as tools


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


if __name__ == "__main__":
    unittest.main()
