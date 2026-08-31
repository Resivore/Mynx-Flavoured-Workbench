#!/usr/bin/env python3
"""Assemble and statically inspect Ribbits' uncommitted private resources.

The output of this tool contains All Rights Reserved Ribbits assets. Keep it
under an ignored private/test-build directory and never commit or distribute it.
"""

from __future__ import annotations

import argparse
import copy
import gzip
import hashlib
import json
import platform
import re
import shutil
import struct
import sys
import tempfile
import zipfile
import zlib
from collections import Counter
from datetime import datetime, timezone
from pathlib import Path, PurePosixPath
from typing import Any


EXPECTED_PRISTINE_SHA256 = (
    "4cf86564aed393410fb1dbca3a9ce2425382307655e92bb6b43f3ddcee5bf731"
)
SOURCE_FILE_COUNT = 287  # 285 assets/data files plus icon.png and logo.png
OUTPUT_FILE_COUNT = 308
SOURCE_EXTENSION_COUNTS = {
    ".json": 201,
    ".nbt": 29,
    ".ogg": 18,
    ".png": 39,
}

CONFIGURED_FEATURE_MIGRATION_PATHS = (
    "data/ribbits/worldgen/configured_feature/giant_lilypad_patch.json",
    "data/ribbits/worldgen/configured_feature/swamp_daisy_patch.json",
    "data/ribbits/worldgen/configured_feature/toadstool_patch.json",
    "data/ribbits/worldgen/configured_feature/umbrella_leaf_patch.json",
    "data/ribbits/worldgen/configured_feature/veg_patch.json",
)
CONFIGURED_FEATURE_MIGRATION_IDS = tuple(
    f"ribbits:{PurePosixPath(path).stem}" for path in CONFIGURED_FEATURE_MIGRATION_PATHS
)
LEGACY_RANDOM_PATCH_FEATURE_TYPE = "minecraft:random_patch"
MIGRATED_RANDOM_PATCH_FEATURE_TYPE = "minecraft:sequence"
RIBBITS_VEGETATION_FEATURE_TYPE = "ribbits:vegetation_block_feature"
RIBBITS_VEGETATION_CONFIG_FIELDS = frozenset(
    {"on_solid_state_provider", "on_liquid_state_provider", "cannot_place_on"}
)
LEGACY_RANDOM_PATCH_PLACEMENT = {
    "type": "minecraft:block_predicate_filter",
    "predicate": {
        "type": "minecraft:matching_blocks",
        "blocks": "minecraft:air",
    },
}

OLD_CONFIG_PREFIX = "text.autoconfig.ribbits-fabric-1_21_1"
NEW_CONFIG_PREFIX = "text.autoconfig.ribbits-26_2"

BLOCK_ITEM_IDS = (
    "brown_toadstool",
    "red_toadstool",
    "toadstool_stem",
    "swamp_lantern",
    "giant_lilypad",
    "swamp_daisy",
    "toadstool",
    "mossy_oak_planks",
    "mossy_oak_planks_stairs",
    "mossy_oak_planks_slab",
    "mossy_oak_planks_fence",
    "mossy_oak_planks_fence_gate",
    "mossy_oak_door",
    "umbrella_leaf",
)
SPAWN_EGG_IDS = (
    "ribbit_nitwit_spawn_egg",
    "ribbit_fisherman_spawn_egg",
    "ribbit_gardener_spawn_egg",
    "ribbit_merchant_spawn_egg",
    "ribbit_sorcerer_spawn_egg",
)
REGISTERED_ITEM_IDS = BLOCK_ITEM_IDS + ("maraca",) + SPAWN_EGG_IDS

CUTOUT_MODEL_FILES = (
    "swamp_lantern.json",
    "swamp_lantern_hanging.json",
    "giant_lilypad.json",
    "giant_lilypad_2.json",
    "giant_lilypad_3.json",
    "giant_lilypad_4.json",
    "swamp_daisy.json",
    "swamp_daisy_2.json",
    "swamp_daisy_3.json",
    "swamp_daisy_4.json",
    "toadstool.json",
    "toadstool_2.json",
    "toadstool_3.json",
    "toadstool_4.json",
    "umbrella_leaf.json",
    "umbrella_leaf_2.json",
    "umbrella_leaf_3.json",
    "umbrella_leaf_4.json",
    "mossy_oak_door_bottom_left.json",
    "mossy_oak_door_bottom_left_open.json",
    "mossy_oak_door_bottom_right.json",
    "mossy_oak_door_bottom_right_open.json",
    "mossy_oak_door_top_left.json",
    "mossy_oak_door_top_left_open.json",
    "mossy_oak_door_top_right.json",
    "mossy_oak_door_top_right_open.json",
)

GECKO_MODEL_IDS = {
    "bass_ribbit",
    "bongo_ribbit",
    "fisherman_ribbit",
    "flute_ribbit",
    "gardener_ribbit",
    "guitar_ribbit",
    "merchant_ribbit",
    "nitwit_ribbit",
    "pride_ribbit",
    "sorcerer_ribbit",
    *{
        f"umbrella/{profession}/umbrella_{variant}"
        for profession in ("fisherman", "gardener", "merchant", "nitwit", "sorcerer")
        for variant in (1, 2, 3)
    },
}

EN_US_CONFIG_ADDITIONS = {
    f"{NEW_CONFIG_PREFIX}.option.general": "General Settings",
    f"{NEW_CONFIG_PREFIX}.option.general.disablePrideFlagCN":
        "Disable Pride Ribbits in China",
    f"{NEW_CONFIG_PREFIX}.option.general.disablePrideFlagCN.@Tooltip":
        "When enabled, Pride Ribbits are hidden when the GeoIP check identifies China.",
    f"{NEW_CONFIG_PREFIX}.option.network": "Supporter List Network",
    f"{NEW_CONFIG_PREFIX}.option.network.@Tooltip":
        "Controls the connection used to retrieve supporter-list data.",
    f"{NEW_CONFIG_PREFIX}.option.network.proxyHost": "Proxy Address",
    f"{NEW_CONFIG_PREFIX}.option.network.proxyHost.@Tooltip":
        "Hostname or IP address of the proxy. Leave blank to connect directly.",
    f"{NEW_CONFIG_PREFIX}.option.network.proxyPort": "Proxy Port",
    f"{NEW_CONFIG_PREFIX}.option.network.proxyPort.@Tooltip":
        "Port used by the proxy connection.",
    f"{NEW_CONFIG_PREFIX}.option.network.proxyUsername": "Proxy Username",
    f"{NEW_CONFIG_PREFIX}.option.network.proxyUsername.@Tooltip":
        "Optional username for proxy authentication.",
    f"{NEW_CONFIG_PREFIX}.option.network.proxyPassword": "Proxy Password",
    f"{NEW_CONFIG_PREFIX}.option.network.proxyPassword.@Tooltip":
        "Optional password for proxy authentication.",
}

MINECRAFT_FROG_SPAWN_EGG_ENTRY = "assets/minecraft/textures/item/frog_spawn_egg.png"
RIBBIT_SPAWN_EGG_TEXTURE_ENTRY = "assets/ribbits/textures/item/ribbit_spawn_egg.png"
EXPECTED_MINECRAFT_FROG_SPAWN_EGG_SHA256 = (
    "23962914851db6e2f7f346e80bf412d8f6e1435e6cd54b73dd5ccbec1a50eccf"
)
EXPECTED_MINECRAFT_CLIENT_SIZE = 37_396_380
EXPECTED_MINECRAFT_CLIENT_SHA256 = (
    "200d673e028d27ddb22bd2d365fbcb98b55be4b2043ee52681a8f30812c12cfe"
)
EXPECTED_RIBBIT_SPAWN_EGG_SHA256 = (
    "53eb7f9d59457b2cd38a5bf65c10c78ac0721da2123f37ce2f48c4d3e1dc1ed1"
)
RIBBIT_GREEN_SPAWN_EGG_PALETTE = bytes.fromhex(
    "000000b3c35b96bc38769b4f94ae516fa0306380335286333b6d342d4c2e"
)
SPAWN_EGG_MODEL = {
    "parent": "minecraft:item/generated",
    "textures": {"layer0": "ribbits:item/ribbit_spawn_egg"},
}
SPAWN_EGG_SUBSTITUTION_NOTICE = (
    "Private Canary 2 uses one temporary palette-only green recolor of Minecraft "
    "26.2's vanilla frog spawn-egg artwork for all five Ribbits profession eggs. "
    "This is explicitly authorized for the private Workbench and is not exact "
    "Ribbits 4.1.6 spawn-egg visual parity."
)


class ValidationError(RuntimeError):
    pass


def sha256_bytes(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def png_dimensions(data: bytes, label: str) -> tuple[int, int]:
    if len(data) < 24 or data[:8] != b"\x89PNG\r\n\x1a\n" or data[12:16] != b"IHDR":
        raise ValidationError(f"Invalid PNG header: {label}")
    width, height = struct.unpack(">II", data[16:24])
    if width <= 0 or height <= 0:
        raise ValidationError(f"Invalid PNG dimensions: {label}")
    return width, height


def load_authorized_vanilla_frog_spawn_egg(
    minecraft_client: Path,
) -> tuple[bytes, str]:
    if not minecraft_client.is_file():
        raise ValidationError(f"Minecraft client JAR does not exist: {minecraft_client}")
    client_size = minecraft_client.stat().st_size
    if client_size != EXPECTED_MINECRAFT_CLIENT_SIZE:
        raise ValidationError(
            "Minecraft 26.2 client JAR size differs: "
            f"expected {EXPECTED_MINECRAFT_CLIENT_SIZE}, got {client_size}"
        )
    client_sha256 = sha256_file(minecraft_client)
    if client_sha256 != EXPECTED_MINECRAFT_CLIENT_SHA256:
        raise ValidationError(
            "Minecraft 26.2 client JAR SHA-256 differs: "
            f"expected {EXPECTED_MINECRAFT_CLIENT_SHA256}, got {client_sha256}"
        )
    with zipfile.ZipFile(minecraft_client) as archive:
        try:
            version = json.loads(archive.read("version.json").decode("utf-8"))
            source = archive.read(MINECRAFT_FROG_SPAWN_EGG_ENTRY)
        except (KeyError, UnicodeDecodeError, json.JSONDecodeError) as exc:
            raise ValidationError(
                f"Minecraft client JAR lacks exact 26.2 frog spawn-egg provenance: {exc}"
            ) from exc
    if version.get("id") != "26.2" or version.get("name") != "26.2":
        raise ValidationError(
            f"Minecraft client identity differs: id={version.get('id')!r}, "
            f"name={version.get('name')!r}"
        )
    source_sha256 = sha256_bytes(source)
    if source_sha256 != EXPECTED_MINECRAFT_FROG_SPAWN_EGG_SHA256:
        raise ValidationError(
            "Minecraft 26.2 frog spawn-egg SHA-256 differs: "
            f"expected {EXPECTED_MINECRAFT_FROG_SPAWN_EGG_SHA256}, got {source_sha256}"
        )
    if png_dimensions(source, MINECRAFT_FROG_SPAWN_EGG_ENTRY) != (16, 16):
        raise ValidationError("Minecraft 26.2 frog spawn-egg must be exactly 16x16")
    return source, client_sha256


def recolor_vanilla_frog_spawn_egg(source: bytes) -> tuple[bytes, str]:
    if sha256_bytes(source) != EXPECTED_MINECRAFT_FROG_SPAWN_EGG_SHA256:
        raise ValidationError("Refusing to recolor an unapproved frog spawn-egg source")

    output = [source[:8]]
    offset = 8
    palette_chunks = 0
    while offset < len(source):
        if offset + 12 > len(source):
            raise ValidationError("Truncated Minecraft frog spawn-egg PNG chunk")
        length = struct.unpack(">I", source[offset : offset + 4])[0]
        chunk_end = offset + 12 + length
        if chunk_end > len(source):
            raise ValidationError("Invalid Minecraft frog spawn-egg PNG chunk length")
        chunk_type = source[offset + 4 : offset + 8]
        chunk_data = source[offset + 8 : offset + 8 + length]
        if chunk_type == b"PLTE":
            replacement = RIBBIT_GREEN_SPAWN_EGG_PALETTE
            if len(chunk_data) != len(replacement):
                raise ValidationError(
                    "Minecraft frog spawn-egg palette length differs: "
                    f"expected {len(replacement)}, got {len(chunk_data)}"
                )
            source_palette_hex = chunk_data.hex()
            crc = zlib.crc32(chunk_type + replacement) & 0xFFFFFFFF
            output.extend(
                (
                    struct.pack(">I", len(replacement)),
                    chunk_type,
                    replacement,
                    struct.pack(">I", crc),
                )
            )
            palette_chunks += 1
        else:
            output.append(source[offset:chunk_end])
        offset = chunk_end

    if offset != len(source) or palette_chunks != 1:
        raise ValidationError(
            f"Expected one Minecraft frog spawn-egg palette, found {palette_chunks}"
        )
    recolored = b"".join(output)
    if png_dimensions(recolored, RIBBIT_SPAWN_EGG_TEXTURE_ENTRY) != (16, 16):
        raise ValidationError("Recolored Ribbit spawn egg must remain exactly 16x16")
    actual_sha256 = sha256_bytes(recolored)
    if actual_sha256 != EXPECTED_RIBBIT_SPAWN_EGG_SHA256:
        raise ValidationError(
            "Authorized Ribbit spawn-egg recolor SHA-256 differs: "
            f"expected {EXPECTED_RIBBIT_SPAWN_EGG_SHA256}, got {actual_sha256}"
        )
    return recolored, source_palette_hex


def safe_zip_name(name: str) -> PurePosixPath:
    path = PurePosixPath(name)
    if path.is_absolute() or not path.parts or any(part in ("", ".", "..") for part in path.parts):
        raise ValidationError(f"Unsafe ZIP entry: {name!r}")
    return path


def require_private_path(path: Path, private_root: Path, label: str) -> None:
    root = private_root.resolve()
    candidate = path.resolve()
    if not any(
        root.parts[index : index + 2] == ("test-builds", "private")
        for index in range(len(root.parts) - 1)
    ):
        raise ValidationError(
            f"Private root must be under test-builds/private, got: {private_root}"
        )
    try:
        relative = candidate.relative_to(root)
    except ValueError as exc:
        raise ValidationError(f"{label} must stay inside private root {root}: {candidate}") from exc
    if not relative.parts:
        raise ValidationError(f"{label} must not be the private root itself: {candidate}")
    if any(part.casefold() == "originals" for part in candidate.parts):
        raise ValidationError(f"{label} must never be inside originals/: {candidate}")


def require_outside_tree(path: Path, tree: Path, label: str) -> None:
    candidate = path.resolve()
    root = tree.resolve()
    try:
        candidate.relative_to(root)
    except ValueError:
        return
    raise ValidationError(f"{label} must stay outside resource tree {root}: {candidate}")


def is_private_source_entry(name: str) -> bool:
    return name.startswith("assets/") or name.startswith("data/") or name in {
        "icon.png",
        "logo.png",
    }


def write_json(path: Path, value: Any) -> None:
    path.write_text(
        json.dumps(value, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
        newline="\n",
    )


def load_json(path: Path) -> Any:
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except (UnicodeDecodeError, json.JSONDecodeError) as exc:
        raise ValidationError(f"Invalid JSON {path}: {exc}") from exc


def relative_files(root: Path) -> list[str]:
    return sorted(path.relative_to(root).as_posix() for path in root.rglob("*") if path.is_file())


def configured_feature_migration_record() -> dict[str, Any]:
    return {
        "count": len(CONFIGURED_FEATURE_MIGRATION_PATHS),
        "from_feature_type": LEGACY_RANDOM_PATCH_FEATURE_TYPE,
        "to_feature_type": MIGRATED_RANDOM_PATCH_FEATURE_TYPE,
        "targets": list(CONFIGURED_FEATURE_MIGRATION_IDS),
    }


def require_exact_configured_feature_paths(paths: list[str], context: str) -> None:
    if not all(isinstance(path, str) for path in paths):
        raise ValidationError(f"{context} configured-feature paths must all be strings")

    folded_counts = Counter(path.casefold() for path in paths)
    duplicates = sorted(path for path, count in folded_counts.items() if count > 1)
    if duplicates:
        raise ValidationError(
            f"{context} contains duplicate configured-feature targets: {duplicates}"
        )

    expected = set(CONFIGURED_FEATURE_MIGRATION_PATHS)
    actual = set(paths)
    missing = sorted(expected - actual)
    unexpected = sorted(actual - expected)
    if missing or unexpected:
        raise ValidationError(
            f"{context} configured-feature target set differs: "
            f"missing={missing}, unexpected={unexpected}"
        )


def require_exact_keys(value: Any, expected: set[str], context: str) -> dict[str, Any]:
    if not isinstance(value, dict):
        raise ValidationError(f"{context} must be a JSON object")
    actual = set(value)
    if actual != expected:
        raise ValidationError(
            f"{context} fields differ: missing={sorted(expected - actual)}, "
            f"unexpected={sorted(actual - expected)}"
        )
    return value


def require_bounded_int(value: Any, minimum: int, maximum: int, context: str) -> int:
    if isinstance(value, bool) or not isinstance(value, int):
        raise ValidationError(f"{context} must be an integer")
    if not minimum <= value <= maximum:
        raise ValidationError(
            f"{context} must be between {minimum} and {maximum}, got {value}"
        )
    return value


def require_ribbits_vegetation_feature(value: Any, context: str) -> dict[str, Any]:
    feature = require_exact_keys(value, {"type", "config"}, context)
    if feature["type"] != RIBBITS_VEGETATION_FEATURE_TYPE:
        raise ValidationError(
            f"{context} must use {RIBBITS_VEGETATION_FEATURE_TYPE}, "
            f"got {feature['type']!r}"
        )
    config = feature["config"]
    if not isinstance(config, dict):
        raise ValidationError(f"{context}.config must be a JSON object")
    config_fields = set(config)
    if not config_fields or not config_fields <= RIBBITS_VEGETATION_CONFIG_FIELDS:
        raise ValidationError(
            f"{context}.config fields are not the understood Ribbits vegetation schema: "
            f"{sorted(config_fields)}"
        )
    for provider_field in ("on_solid_state_provider", "on_liquid_state_provider"):
        if provider_field not in config:
            continue
        provider = config[provider_field]
        if not isinstance(provider, dict) or not isinstance(provider.get("type"), str):
            raise ValidationError(
                f"{context}.config.{provider_field} must be a typed state-provider object"
            )
    if "cannot_place_on" in config:
        block_states = config["cannot_place_on"]
        if not isinstance(block_states, list) or not all(
            isinstance(state, dict) and isinstance(state.get("Name"), str)
            for state in block_states
        ):
            raise ValidationError(
                f"{context}.config.cannot_place_on must be an array of named block states"
            )
    return feature


def migrate_legacy_random_patch_document(path: str, value: Any) -> dict[str, Any]:
    document = require_exact_keys(value, {"type", "config"}, path)
    if document["type"] != LEGACY_RANDOM_PATCH_FEATURE_TYPE:
        raise ValidationError(
            f"{path} must use pristine feature type {LEGACY_RANDOM_PATCH_FEATURE_TYPE}, "
            f"got {document['type']!r}"
        )

    config = require_exact_keys(
        document["config"], {"feature", "tries", "xz_spread", "y_spread"}, f"{path}.config"
    )
    tries = require_bounded_int(config["tries"], 1, 4096, f"{path}.config.tries")
    xz_spread = require_bounded_int(config["xz_spread"], 0, 16, f"{path}.config.xz_spread")
    y_spread = require_bounded_int(config["y_spread"], 0, 16, f"{path}.config.y_spread")

    legacy_placed_feature = require_exact_keys(
        config["feature"], {"feature", "placement"}, f"{path}.config.feature"
    )
    require_ribbits_vegetation_feature(
        legacy_placed_feature["feature"], f"{path}.config.feature.feature"
    )
    legacy_placements = legacy_placed_feature["placement"]
    if legacy_placements != [LEGACY_RANDOM_PATCH_PLACEMENT]:
        raise ValidationError(
            f"{path}.config.feature.placement must contain the one understood legacy air filter"
        )

    return {
        "type": MIGRATED_RANDOM_PATCH_FEATURE_TYPE,
        "config": {
            "features": [
                {
                    "feature": copy.deepcopy(legacy_placed_feature["feature"]),
                    "placement": [
                        {"type": "minecraft:count", "count": tries},
                        {
                            "type": "minecraft:random_offset",
                            "xz_spread": {
                                "type": "minecraft:trapezoid",
                                "min": -xz_spread,
                                "max": xz_spread,
                                "plateau": 0,
                            },
                            "y_spread": {
                                "type": "minecraft:trapezoid",
                                "min": -y_spread,
                                "max": y_spread,
                                "plateau": 0,
                            },
                        },
                        *copy.deepcopy(legacy_placements),
                    ],
                }
            ]
        },
    }


def json_contains_value(value: Any, expected: str) -> bool:
    if isinstance(value, dict):
        return any(json_contains_value(item, expected) for item in value.values())
    if isinstance(value, list):
        return any(json_contains_value(item, expected) for item in value)
    return value == expected


def require_triangle_provider(value: Any, context: str) -> None:
    provider = require_exact_keys(value, {"type", "min", "max", "plateau"}, context)
    if provider["type"] != "minecraft:trapezoid":
        raise ValidationError(f"{context} must use minecraft:trapezoid")
    minimum = require_bounded_int(provider["min"], -16, 0, f"{context}.min")
    maximum = require_bounded_int(provider["max"], 0, 16, f"{context}.max")
    plateau = require_bounded_int(provider["plateau"], 0, 0, f"{context}.plateau")
    if minimum != -maximum or plateau != 0:
        raise ValidationError(f"{context} must be a symmetric zero-plateau triangle")


def require_migrated_random_patch_document(path: str, value: Any) -> None:
    document = require_exact_keys(value, {"type", "config"}, path)
    if document["type"] != MIGRATED_RANDOM_PATCH_FEATURE_TYPE:
        raise ValidationError(
            f"{path} must use migrated feature type {MIGRATED_RANDOM_PATCH_FEATURE_TYPE}, "
            f"got {document['type']!r}"
        )
    if json_contains_value(document, LEGACY_RANDOM_PATCH_FEATURE_TYPE):
        raise ValidationError(f"{path} still contains {LEGACY_RANDOM_PATCH_FEATURE_TYPE}")

    config = require_exact_keys(document["config"], {"features"}, f"{path}.config")
    features = config["features"]
    if not isinstance(features, list) or len(features) != 1:
        raise ValidationError(f"{path}.config.features must contain exactly one placed feature")
    placed_feature = require_exact_keys(
        features[0], {"feature", "placement"}, f"{path}.config.features[0]"
    )
    require_ribbits_vegetation_feature(
        placed_feature["feature"], f"{path}.config.features[0].feature"
    )
    placements = placed_feature["placement"]
    if not isinstance(placements, list) or len(placements) != 3:
        raise ValidationError(
            f"{path}.config.features[0].placement must contain count, random_offset, and air filter"
        )

    count = require_exact_keys(
        placements[0], {"type", "count"}, f"{path}.config.features[0].placement[0]"
    )
    if count["type"] != "minecraft:count":
        raise ValidationError(f"{path} migrated placement must begin with minecraft:count")
    require_bounded_int(count["count"], 1, 4096, f"{path}.config.features[0].placement[0].count")

    offset = require_exact_keys(
        placements[1],
        {"type", "xz_spread", "y_spread"},
        f"{path}.config.features[0].placement[1]",
    )
    if offset["type"] != "minecraft:random_offset":
        raise ValidationError(f"{path} second migrated placement must be minecraft:random_offset")
    require_triangle_provider(
        offset["xz_spread"], f"{path}.config.features[0].placement[1].xz_spread"
    )
    require_triangle_provider(
        offset["y_spread"], f"{path}.config.features[0].placement[1].y_spread"
    )
    if placements[2] != LEGACY_RANDOM_PATCH_PLACEMENT:
        raise ValidationError(f"{path} migrated placement must retain the pristine air filter last")


def migrate_configured_feature_documents(
    documents: list[tuple[str, Any]],
) -> tuple[dict[str, Any], dict[str, Any]]:
    paths = [path for path, _ in documents]
    require_exact_configured_feature_paths(paths, "Configured-feature migration input")
    by_path = {path: value for path, value in documents}

    # Build and validate every output in memory before rewriting any protected file.
    migrated = {
        path: migrate_legacy_random_patch_document(path, by_path[path])
        for path in CONFIGURED_FEATURE_MIGRATION_PATHS
    }
    validate_migrated_configured_feature_documents(list(migrated.items()))
    return migrated, configured_feature_migration_record()


def validate_migrated_configured_feature_documents(
    documents: list[tuple[str, Any]],
) -> None:
    paths = [path for path, _ in documents]
    require_exact_configured_feature_paths(paths, "Migrated configured-feature output")
    by_path = {path: value for path, value in documents}
    for path in CONFIGURED_FEATURE_MIGRATION_PATHS:
        require_migrated_random_patch_document(path, by_path[path])


def load_configured_feature_documents(root: Path) -> list[tuple[str, Any]]:
    relative_root = PurePosixPath(CONFIGURED_FEATURE_MIGRATION_PATHS[0]).parent
    configured_root = root.joinpath(*relative_root.parts)
    paths = (
        sorted(
            path.relative_to(root).as_posix()
            for path in configured_root.rglob("*")
            if path.is_file()
        )
        if configured_root.is_dir()
        else []
    )
    require_exact_configured_feature_paths(paths, "Private resource tree")
    return [(path, load_json(root.joinpath(*PurePosixPath(path).parts))) for path in paths]


def migrate_configured_features(root: Path) -> dict[str, Any]:
    documents = load_configured_feature_documents(root)
    migrated, record = migrate_configured_feature_documents(documents)
    for path in CONFIGURED_FEATURE_MIGRATION_PATHS:
        write_json(root.joinpath(*PurePosixPath(path).parts), migrated[path])
    return record


def validate_configured_features(root: Path, errors: list[str]) -> None:
    try:
        documents = load_configured_feature_documents(root)
        validate_migrated_configured_feature_documents(documents)
    except (OSError, KeyError, ValueError, ValidationError) as exc:
        errors.append(f"Configured-feature migration is invalid: {exc}")


def repair_pristine_zh_json(path: Path) -> None:
    text = path.read_text(encoding="utf-8")
    pattern = re.compile(
        r'("text\.autoconfig\.ribbits-fabric-1_21_1\.option\.general\.'
        r'prideFlagAllYear\.@Tooltip"\s*:\s*"(?:[^"\\]|\\.)*")'
        r'(\r?\n\s*)("ribbits\.options\.supporter_hat_button")'
    )
    repaired, count = pattern.subn(r"\1,\2\3", text, count=1)
    if count != 1:
        raise ValidationError("Could not apply the one known pristine zh_cn comma repair")
    path.write_text(repaired, encoding="utf-8", newline="\n")


def migrate_languages(root: Path) -> None:
    lang_dir = root / "assets/ribbits/lang"
    lang_files = sorted(lang_dir.glob("*.json"))
    if len(lang_files) != 11:
        raise ValidationError(f"Expected 11 pristine languages, found {len(lang_files)}")

    repair_pristine_zh_json(lang_dir / "zh_cn.json")
    suffixes = (
        ".title",
        ".option.general.prideFlagAllYear",
        ".option.general.prideFlagAllYear.@Tooltip",
    )

    for path in lang_files:
        values = load_json(path)
        for suffix in suffixes:
            old_key = OLD_CONFIG_PREFIX + suffix
            new_key = NEW_CONFIG_PREFIX + suffix
            if old_key not in values or new_key in values:
                raise ValidationError(f"Unexpected config translations in {path.name}: {old_key}")
            values[new_key] = values.pop(old_key)

        for item_id in BLOCK_ITEM_IDS:
            block_key = f"block.ribbits.{item_id}"
            item_key = f"item.ribbits.{item_id}"
            if block_key not in values:
                raise ValidationError(f"Missing {block_key} in {path.name}")
            if item_key in values:
                raise ValidationError(f"Unexpected pre-existing {item_key} in {path.name}")
            values[item_key] = values[block_key]

        if path.name == "en_us.json":
            overlap = set(values).intersection(EN_US_CONFIG_ADDITIONS)
            if overlap:
                raise ValidationError(f"Unexpected pre-existing en_us config keys: {sorted(overlap)}")
            values.update(EN_US_CONFIG_ADDITIONS)

        write_json(path, values)


def migrate_geckolib_paths(root: Path) -> None:
    old_models = root / "assets/ribbits/geo"
    new_models = root / "assets/ribbits/geckolib/models"
    old_animations = root / "assets/ribbits/animations"
    new_animations = root / "assets/ribbits/geckolib/animations"
    if not old_models.is_dir() or not old_animations.is_dir():
        raise ValidationError("Pristine GeckoLib resource roots are missing")
    new_models.parent.mkdir(parents=True, exist_ok=True)
    shutil.move(str(old_models), str(new_models))
    new_animations.parent.mkdir(parents=True, exist_ok=True)
    shutil.move(str(old_animations), str(new_animations))


def migrate_cutout_models(root: Path) -> None:
    model_dir = root / "assets/ribbits/models/block"
    for filename in CUTOUT_MODEL_FILES:
        path = model_dir / filename
        model = load_json(path)
        if "render_type" in model:
            raise ValidationError(f"Unexpected existing render_type in {filename}")
        model["render_type"] = "cutout"
        write_json(path, model)


def ingredient_string(value: Any, context: str) -> str:
    if not isinstance(value, dict) or len(value) != 1:
        raise ValidationError(f"Unexpected legacy ingredient in {context}: {value!r}")
    if "item" in value and isinstance(value["item"], str):
        return value["item"]
    if "tag" in value and isinstance(value["tag"], str):
        return "#" + value["tag"]
    raise ValidationError(f"Unexpected legacy ingredient in {context}: {value!r}")


def migrate_recipes(root: Path) -> None:
    recipe_dir = root / "data/ribbits/recipe"
    paths = sorted(recipe_dir.glob("*.json"))
    if len(paths) != 11:
        raise ValidationError(f"Expected 11 recipes, found {len(paths)}")
    for path in paths:
        recipe = load_json(path)
        if recipe["type"] == "minecraft:crafting_shaped":
            recipe["key"] = {
                symbol: ingredient_string(value, path.name)
                for symbol, value in recipe["key"].items()
            }
        elif recipe["type"] == "minecraft:crafting_shapeless":
            recipe["ingredients"] = [
                ingredient_string(value, path.name) for value in recipe["ingredients"]
            ]
        else:
            raise ValidationError(f"Unexpected recipe type in {path.name}: {recipe['type']}")
        write_json(path, recipe)


def migrate_recipe_advancements(root: Path) -> None:
    advancement_dir = root / "data/ribbits/advancement/recipes"
    paths = sorted(advancement_dir.glob("*.json"))
    if len(paths) != 6:
        raise ValidationError(f"Expected 6 recipe advancements, found {len(paths)}")
    for path in paths:
        advancement = load_json(path)
        converted = 0
        for criterion in advancement["criteria"].values():
            if criterion.get("trigger") != "minecraft:inventory_changed":
                continue
            for predicate in criterion["conditions"]["items"]:
                items = predicate.get("items")
                if not isinstance(items, list) or len(items) != 1 or not isinstance(items[0], str):
                    raise ValidationError(f"Unexpected item predicate in {path.name}: {items!r}")
                predicate["items"] = items[0]
                converted += 1
        if converted != 1:
            raise ValidationError(f"Expected one item predicate conversion in {path.name}, got {converted}")
        write_json(path, advancement)


def migrate_sorcerer_loot(root: Path) -> None:
    path = root / "data/ribbits/loot_table/chests/sorcerer.json"
    loot = load_json(path)
    converted = 0
    for pool in loot["pools"]:
        for entry in pool["entries"]:
            functions = entry.get("functions", [])
            for index, function in enumerate(functions):
                if function == {
                    "function": "minecraft:set_components",
                    "components": {"potion_contents": "minecraft:strong_leaping"},
                }:
                    functions[index] = {
                        "function": "minecraft:set_potion",
                        "id": "minecraft:strong_leaping",
                    }
                    converted += 1
    if converted != 1:
        raise ValidationError(f"Expected one sorcerer potion conversion, got {converted}")
    write_json(path, loot)


def write_item_definitions(root: Path) -> None:
    item_dir = root / "assets/ribbits/items"
    item_dir.mkdir(parents=True, exist_ok=False)
    for item_id in REGISTERED_ITEM_IDS:
        if item_id == "maraca":
            value = {
                "model": {
                    "type": "minecraft:condition",
                    "property": "ribbits:maraca_using",
                    "on_false": {"type": "minecraft:model", "model": "ribbits:item/maraca"},
                    "on_true": {
                        "type": "minecraft:model",
                        "model": "ribbits:item/maraca_in_hand",
                    },
                }
            }
        else:
            value = {
                "model": {
                    "type": "minecraft:model",
                    "model": f"ribbits:item/{item_id}",
                }
            }
        write_json(item_dir / f"{item_id}.json", value)


def migrate_spawn_egg_models(root: Path, minecraft_client: Path) -> dict[str, Any]:
    source, client_sha256 = load_authorized_vanilla_frog_spawn_egg(minecraft_client)
    recolored, source_palette_hex = recolor_vanilla_frog_spawn_egg(source)

    texture_path = root / PurePosixPath(RIBBIT_SPAWN_EGG_TEXTURE_ENTRY)
    if texture_path.exists():
        raise ValidationError(
            f"Unexpected pre-existing shared spawn-egg texture: {RIBBIT_SPAWN_EGG_TEXTURE_ENTRY}"
        )
    texture_path.parent.mkdir(parents=True, exist_ok=True)
    texture_path.write_bytes(recolored)

    for spawn_egg_id in SPAWN_EGG_IDS:
        model_path = root / f"assets/ribbits/models/item/{spawn_egg_id}.json"
        pristine_model = load_json(model_path)
        if pristine_model != {"parent": "minecraft:item/template_spawn_egg"}:
            raise ValidationError(
                f"Unexpected pristine spawn-egg model for {spawn_egg_id}: {pristine_model!r}"
            )
        write_json(model_path, SPAWN_EGG_MODEL)

    return {
        "authorization": (
            "Explicitly authorized by the Workbench owner for private Ribbits Canary 2"
        ),
        "temporary": True,
        "exact_ribbits_4_1_6_visual_parity": False,
        "behavior_changed": False,
        "source": {
            "description": "Minecraft Java 26.2 vanilla frog spawn-egg artwork",
            "minecraft_version": "26.2",
            "client_jar_path": str(minecraft_client.resolve()),
            "client_jar_size": minecraft_client.stat().st_size,
            "client_jar_sha256": client_sha256,
            "entry": MINECRAFT_FROG_SPAWN_EGG_ENTRY,
            "sha256": EXPECTED_MINECRAFT_FROG_SPAWN_EGG_SHA256,
            "dimensions": [16, 16],
        },
        "transformation": {
            "kind": "palette-only recolor",
            "source_palette_hex": source_palette_hex,
            "ribbit_green_palette_hex": RIBBIT_GREEN_SPAWN_EGG_PALETTE.hex(),
            "pixel_indices_and_alpha": "byte-identical to the vanilla source",
        },
        "output": {
            "entry": RIBBIT_SPAWN_EGG_TEXTURE_ENTRY,
            "sha256": EXPECTED_RIBBIT_SPAWN_EGG_SHA256,
            "dimensions": [16, 16],
        },
        "model": SPAWN_EGG_MODEL,
        "consumers": list(SPAWN_EGG_IDS),
        "notice": SPAWN_EGG_SUBSTITUTION_NOTICE,
    }


def build_manifest(
    pristine: Path,
    output: Path,
    source_hashes: dict[str, str],
    spawn_egg_substitution: dict[str, Any],
    configured_feature_migration: dict[str, Any],
) -> dict[str, Any]:
    output_hashes = {
        name: sha256_file(output / PurePosixPath(name)) for name in relative_files(output)
    }
    return {
        "classification": "PRIVATE ASSEMBLY STAGED / AUTHORIZED SUBSTITUTION",
        "created_utc": datetime.now(timezone.utc).isoformat(),
        "tool": "projects/ribbits-26.2/tools/private_resource_tools.py",
        "python": platform.python_version(),
        "pristine": {
            "path": str(pristine.resolve()),
            "sha256": sha256_file(pristine),
            "selected_file_count": len(source_hashes),
            "files": source_hashes,
        },
        "output": {
            "path": str(output.resolve()),
            "file_count": len(output_hashes),
            "files": output_hashes,
        },
        "migrations": {
            "geckolib_models_moved": 25,
            "geckolib_animations_moved": 1,
            "item_definitions_added": 20,
            "cutout_logical_blocks": 6,
            "cutout_concrete_models": len(CUTOUT_MODEL_FILES),
            "recipes_migrated": 11,
            "recipe_advancements_migrated": 6,
            "sorcerer_potion_loot_functions_migrated": 1,
            "language_files_migrated": 11,
            "config_prefix_renames": 33,
            "block_item_translation_keys_added": 154,
            "en_us_config_keys_added": len(EN_US_CONFIG_ADDITIONS),
            "zh_cn_syntax_repairs": 1,
            "spawn_egg_models_migrated": len(SPAWN_EGG_IDS),
            "spawn_egg_textures_added": 1,
            "configured_feature_random_patch_to_sequence": configured_feature_migration,
        },
        "authorized_spawn_egg_substitution": spawn_egg_substitution,
        "blockers": [],
    }


def _assemble_impl(
    pristine: Path,
    minecraft_client: Path,
    output: Path,
    manifest_path: Path,
) -> None:
    if not pristine.is_file():
        raise ValidationError(f"Pristine JAR does not exist: {pristine}")
    actual_hash = sha256_file(pristine)
    if actual_hash != EXPECTED_PRISTINE_SHA256:
        raise ValidationError(
            f"Pristine SHA-256 mismatch: expected {EXPECTED_PRISTINE_SHA256}, got {actual_hash}"
        )
    if output.exists():
        raise ValidationError(f"Refusing to overwrite existing output: {output}")
    if manifest_path.exists():
        raise ValidationError(f"Refusing to overwrite existing manifest: {manifest_path}")

    output.mkdir(parents=True)
    source_hashes: dict[str, str] = {}
    seen_casefold: set[str] = set()
    try:
        with zipfile.ZipFile(pristine) as archive:
            selected = [
                entry
                for entry in archive.infolist()
                if not entry.is_dir() and is_private_source_entry(entry.filename)
            ]
            selected_paths = [safe_zip_name(entry.filename).as_posix() for entry in selected]
            configured_prefix = (
                PurePosixPath(CONFIGURED_FEATURE_MIGRATION_PATHS[0]).parent.as_posix() + "/"
            )
            configured_source_paths = [
                path for path in selected_paths if path.startswith(configured_prefix)
            ]
            require_exact_configured_feature_paths(
                configured_source_paths, "Pristine Ribbits JAR"
            )
            if len(selected) != SOURCE_FILE_COUNT:
                raise ValidationError(
                    f"Expected {SOURCE_FILE_COUNT} protected source files, found {len(selected)}"
                )
            extension_counts = Counter(PurePosixPath(entry.filename).suffix for entry in selected)
            for extension, expected in SOURCE_EXTENSION_COUNTS.items():
                if extension_counts[extension] != expected:
                    raise ValidationError(
                        f"Expected {expected} {extension} files, found {extension_counts[extension]}"
                    )

            for entry in selected:
                relative = safe_zip_name(entry.filename)
                folded = relative.as_posix().casefold()
                if folded in seen_casefold:
                    raise ValidationError(f"Case-insensitive duplicate source entry: {entry.filename}")
                seen_casefold.add(folded)
                data = archive.read(entry)
                destination = output.joinpath(*relative.parts)
                destination.parent.mkdir(parents=True, exist_ok=True)
                destination.write_bytes(data)
                source_hashes[relative.as_posix()] = sha256_bytes(data)

        migrate_geckolib_paths(output)
        migrate_languages(output)
        migrate_cutout_models(output)
        migrate_recipes(output)
        migrate_recipe_advancements(output)
        migrate_sorcerer_loot(output)
        configured_feature_migration = migrate_configured_features(output)
        write_item_definitions(output)
        spawn_egg_substitution = migrate_spawn_egg_models(output, minecraft_client)

        output_file_count = len(relative_files(output))
        if output_file_count != OUTPUT_FILE_COUNT:
            raise ValidationError(
                f"Expected {OUTPUT_FILE_COUNT} assembled files, found {output_file_count}"
            )

        manifest = build_manifest(
            pristine,
            output,
            source_hashes,
            spawn_egg_substitution,
            configured_feature_migration,
        )
        manifest_path.parent.mkdir(parents=True, exist_ok=True)
        write_json(manifest_path, manifest)
    except Exception:
        # Leave the ignored staging tree intact for forensic inspection; never silently
        # delete a partially assembled protected-resource tree.
        raise


def assemble(
    pristine: Path,
    minecraft_client: Path,
    output: Path,
    manifest_path: Path,
    private_root: Path,
) -> None:
    require_private_path(output, private_root, "Assembly output")
    require_private_path(manifest_path, private_root, "Assembly manifest")
    require_outside_tree(manifest_path, output, "Assembly manifest")
    _assemble_impl(pristine, minecraft_client, output, manifest_path)


def validate_binary_formats(root: Path, errors: list[str]) -> None:
    for path in root.rglob("*.png"):
        data = path.read_bytes()
        if len(data) < 24 or data[:8] != b"\x89PNG\r\n\x1a\n" or data[12:16] != b"IHDR":
            errors.append(f"Invalid PNG header: {path.relative_to(root).as_posix()}")
            continue
        width, height = struct.unpack(">II", data[16:24])
        if width <= 0 or height <= 0:
            errors.append(f"Invalid PNG dimensions: {path.relative_to(root).as_posix()}")
    for path in root.rglob("*.ogg"):
        if not path.read_bytes().startswith(b"OggS"):
            errors.append(f"Invalid OGG header: {path.relative_to(root).as_posix()}")
    for path in root.rglob("*.nbt"):
        data = path.read_bytes()
        try:
            decoded = gzip.decompress(data) if data.startswith(b"\x1f\x8b") else data
        except gzip.BadGzipFile as exc:
            errors.append(f"Invalid compressed NBT {path.relative_to(root).as_posix()}: {exc}")
            continue
        if not decoded or decoded[0] != 10:
            errors.append(f"NBT root is not a compound: {path.relative_to(root).as_posix()}")


def collect_json_errors(root: Path, errors: list[str]) -> int:
    count = 0
    for path in root.rglob("*.json"):
        count += 1
        try:
            load_json(path)
        except ValidationError as exc:
            errors.append(str(exc))
    return count


def validate_pristine_provenance(
    pristine: Path,
    minecraft_client: Path,
    root: Path,
    private_root: Path,
    errors: list[str],
) -> None:
    if not pristine.is_file():
        errors.append(f"Pristine JAR does not exist: {pristine}")
        return
    actual_hash = sha256_file(pristine)
    if actual_hash != EXPECTED_PRISTINE_SHA256:
        errors.append(
            f"Pristine SHA-256 mismatch: expected {EXPECTED_PRISTINE_SHA256}, got {actual_hash}"
        )
        return

    with tempfile.TemporaryDirectory(
        prefix="ribbits-private-validation-", dir=private_root.resolve()
    ) as temp_dir:
        temp_root = Path(temp_dir)
        expected_root = temp_root / "resources"
        expected_manifest = temp_root / "manifest.json"
        try:
            _assemble_impl(pristine, minecraft_client, expected_root, expected_manifest)
        except (OSError, KeyError, ValueError, ValidationError, zipfile.BadZipFile) as exc:
            errors.append(f"Could not reconstruct deterministic expected assembly: {exc}")
            return

        expected_names = set(relative_files(expected_root))
        actual_names = set(relative_files(root))
        missing = expected_names - actual_names
        unexpected = actual_names - expected_names
        if missing:
            errors.append(f"Files missing from deterministic private assembly: {sorted(missing)}")
        if unexpected:
            errors.append(f"Unexpected private assembly files: {sorted(unexpected)}")
        for name in sorted(expected_names & actual_names):
            expected_bytes = (expected_root / PurePosixPath(name)).read_bytes()
            actual_bytes = (root / PurePosixPath(name)).read_bytes()
            if actual_bytes != expected_bytes:
                errors.append(f"Private assembly differs from deterministic migration: {name}")


def validate_transforms(root: Path, errors: list[str]) -> None:
    validate_configured_features(root, errors)

    if (root / "assets/ribbits/geo").exists() or (root / "assets/ribbits/animations").exists():
        errors.append("Legacy GeckoLib resource roots remain")
    model_root = root / "assets/ribbits/geckolib/models"
    model_ids = {
        path.relative_to(model_root).as_posix().removesuffix(".geo.json")
        for path in model_root.rglob("*.geo.json")
    }
    if model_ids != GECKO_MODEL_IDS:
        errors.append(
            f"GeckoLib model IDs differ: missing={sorted(GECKO_MODEL_IDS - model_ids)}, "
            f"extra={sorted(model_ids - GECKO_MODEL_IDS)}"
        )
    if not (root / "assets/ribbits/geckolib/animations/ribbit.animation.json").is_file():
        errors.append("Missing migrated ribbit animation")

    block_model_dir = root / "assets/ribbits/models/block"
    cutout_paths = []
    for path in block_model_dir.glob("*.json"):
        model = load_json(path)
        if model.get("render_type") == "cutout":
            cutout_paths.append(path.name)
        elif "render_type" in model:
            errors.append(f"Unexpected render_type value in {path.name}: {model['render_type']!r}")
    if set(cutout_paths) != set(CUTOUT_MODEL_FILES):
        errors.append(
            f"Cutout concrete model set differs: expected={sorted(CUTOUT_MODEL_FILES)}, "
            f"actual={sorted(cutout_paths)}"
        )

    item_dir = root / "assets/ribbits/items"
    actual_item_defs = {path.stem for path in item_dir.glob("*.json")}
    if actual_item_defs != set(REGISTERED_ITEM_IDS):
        errors.append(
            f"Item definition set differs: expected={sorted(REGISTERED_ITEM_IDS)}, "
            f"actual={sorted(actual_item_defs)}"
        )

    for path in (root / "assets/ribbits/lang").glob("*.json"):
        values = load_json(path)
        stale = [key for key in values if "ribbits-fabric-1_21_1" in key or "ribbits-1_21_10" in key]
        if stale:
            errors.append(f"Stale config translations in {path.name}: {stale}")
        for item_id in BLOCK_ITEM_IDS:
            block_value = values.get(f"block.ribbits.{item_id}")
            item_value = values.get(f"item.ribbits.{item_id}")
            if block_value is None or item_value != block_value:
                errors.append(f"Block/item translation mismatch for {item_id} in {path.name}")
    en_values = load_json(root / "assets/ribbits/lang/en_us.json")
    expected_config_keys = {
        NEW_CONFIG_PREFIX + suffix
        for suffix in (
            ".title",
            ".option.general",
            ".option.general.prideFlagAllYear",
            ".option.general.prideFlagAllYear.@Tooltip",
            ".option.general.disablePrideFlagCN",
            ".option.general.disablePrideFlagCN.@Tooltip",
            ".option.network",
            ".option.network.@Tooltip",
            ".option.network.proxyHost",
            ".option.network.proxyHost.@Tooltip",
            ".option.network.proxyPort",
            ".option.network.proxyPort.@Tooltip",
            ".option.network.proxyUsername",
            ".option.network.proxyUsername.@Tooltip",
            ".option.network.proxyPassword",
            ".option.network.proxyPassword.@Tooltip",
        )
    }
    missing_config = expected_config_keys - set(en_values)
    if missing_config:
        errors.append(f"Missing en_us config translations: {sorted(missing_config)}")

    for path in (root / "data/ribbits/recipe").glob("*.json"):
        recipe = load_json(path)
        ingredients = (
            list(recipe["key"].values())
            if recipe["type"] == "minecraft:crafting_shaped"
            else recipe["ingredients"]
        )
        if not ingredients or not all(isinstance(value, str) for value in ingredients):
            errors.append(f"Recipe ingredients are not 26.2 strings: {path.name}")

    for path in (root / "data/ribbits/advancement/recipes").glob("*.json"):
        advancement = load_json(path)
        for criterion in advancement["criteria"].values():
            if criterion.get("trigger") == "minecraft:inventory_changed":
                for predicate in criterion["conditions"]["items"]:
                    if not isinstance(predicate.get("items"), str):
                        errors.append(f"Advancement item predicate is not a 26.2 scalar: {path.name}")

    sorcerer = load_json(root / "data/ribbits/loot_table/chests/sorcerer.json")
    serialized_sorcerer = json.dumps(sorcerer, separators=(",", ":"))
    if "minecraft:set_potion" not in serialized_sorcerer or "potion_contents" in serialized_sorcerer:
        errors.append("Sorcerer potion loot migration is missing")

    processor = load_json(root / "data/ribbits/worldgen/processor_list/main.json")
    processor_text = json.dumps(processor, separators=(",", ":"))
    if '"randomize_facing":true' in processor_text:
        errors.append("Protected processor data unexpectedly activates randomize_facing")

    spawn_egg_texture = root / PurePosixPath(RIBBIT_SPAWN_EGG_TEXTURE_ENTRY)
    if not spawn_egg_texture.is_file():
        errors.append(f"Missing authorized shared spawn-egg texture: {RIBBIT_SPAWN_EGG_TEXTURE_ENTRY}")
    else:
        spawn_egg_bytes = spawn_egg_texture.read_bytes()
        if png_dimensions(spawn_egg_bytes, RIBBIT_SPAWN_EGG_TEXTURE_ENTRY) != (16, 16):
            errors.append("Authorized shared spawn-egg texture is not exactly 16x16")
        actual_spawn_egg_hash = sha256_bytes(spawn_egg_bytes)
        if actual_spawn_egg_hash != EXPECTED_RIBBIT_SPAWN_EGG_SHA256:
            errors.append(
                "Authorized shared spawn-egg texture SHA-256 differs: "
                f"expected {EXPECTED_RIBBIT_SPAWN_EGG_SHA256}, got {actual_spawn_egg_hash}"
            )

    for spawn_egg_id in SPAWN_EGG_IDS:
        model = load_json(root / f"assets/ribbits/models/item/{spawn_egg_id}.json")
        if model != SPAWN_EGG_MODEL:
            errors.append(
                f"{spawn_egg_id} does not use the authorized shared spawn-egg texture: "
                f"{model!r}"
            )


def validate_resource_references(root: Path, minecraft_entries: set[str], errors: list[str]) -> None:
    def model_exists(identifier: str) -> bool:
        namespace, sep, path = identifier.partition(":")
        if not sep:
            namespace, path = "minecraft", namespace
        entry = f"assets/{namespace}/models/{path}.json"
        return (root / entry).is_file() if namespace != "minecraft" else entry in minecraft_entries

    for path in (root / "assets/ribbits/blockstates").glob("*.json"):
        value = load_json(path)
        serialized = json.dumps(value)
        for identifier in re.findall(r'"model"\s*:\s*"([a-z0-9_.-]+:[a-z0-9_./-]+)"', serialized):
            if not model_exists(identifier):
                errors.append(f"Missing blockstate model {identifier} referenced by {path.name}")

    for path in (root / "assets/ribbits/models").rglob("*.json"):
        value = load_json(path)
        parent = value.get("parent")
        if isinstance(parent, str) and not model_exists(parent):
            errors.append(f"Missing model parent {parent} referenced by {path.relative_to(root).as_posix()}")

    for path in (root / "assets/ribbits/items").glob("*.json"):
        serialized = json.dumps(load_json(path))
        for identifier in re.findall(r'"model"\s*:\s*"([a-z0-9_.-]+:[a-z0-9_./-]+)"', serialized):
            if not model_exists(identifier):
                errors.append(f"Missing item model {identifier} referenced by {path.name}")


def load_minecraft_entries(path: Path) -> set[str]:
    if not path.is_file():
        raise ValidationError(f"Minecraft client JAR does not exist: {path}")
    with zipfile.ZipFile(path) as archive:
        return {entry.filename for entry in archive.infolist() if not entry.is_dir()}


def validation_report(
    resources: Path, pristine: Path, minecraft_client: Path, private_root: Path
) -> dict[str, Any]:
    if not resources.is_dir():
        raise ValidationError(f"Resource directory does not exist: {resources}")
    errors: list[str] = []
    blockers: list[str] = []
    json_count = collect_json_errors(resources, errors)
    validate_binary_formats(resources, errors)
    validate_pristine_provenance(
        pristine, minecraft_client, resources, private_root, errors
    )
    if not errors:
        validate_transforms(resources, errors)
        validate_resource_references(resources, load_minecraft_entries(minecraft_client), errors)
    return {
        "classification": "STATIC GATE BLOCKED" if errors or blockers else "STATIC TREE PASS",
        "validated_utc": datetime.now(timezone.utc).isoformat(),
        "resources": str(resources.resolve()),
        "resource_file_count": len(relative_files(resources)),
        "strict_json_files": json_count,
        "errors": errors,
        "blockers": blockers,
    }


def validate_jar(
    resources: Path,
    pristine: Path,
    jar_path: Path,
    minecraft_client: Path,
    private_root: Path,
) -> dict[str, Any]:
    report = validation_report(resources, pristine, minecraft_client, private_root)
    errors: list[str] = report["errors"]
    if not jar_path.is_file():
        errors.append(f"Built JAR does not exist: {jar_path}")
        report["classification"] = "STATIC GATE BLOCKED"
        return report

    with zipfile.ZipFile(jar_path) as archive:
        names = [entry.filename for entry in archive.infolist() if not entry.is_dir()]
        duplicate_names = sorted(name for name, count in Counter(names).items() if count > 1)
        if duplicate_names:
            errors.append(f"Duplicate JAR entries: {duplicate_names}")
        name_set = set(names)
        for relative in relative_files(resources):
            if relative not in name_set:
                errors.append(f"Private resource omitted from JAR: {relative}")
                continue
            if archive.read(relative) != (resources / PurePosixPath(relative)).read_bytes():
                errors.append(f"Private resource changed during packaging: {relative}")

        for required in (
            "fabric.mod.json",
            "ribbits.mixins.json",
            "ribbits.fabric.mixins.json",
            "pack.mcmeta",
            "com/yungnickyoung/minecraft/ribbits/fabric/RibbitsFabric.class",
            "com/yungnickyoung/minecraft/ribbits/fabric/client/RibbitsFabricClient.class",
        ):
            if required not in name_set:
                errors.append(f"Required JAR entry missing: {required}")

        def archive_json(name: str) -> Any:
            try:
                return json.loads(archive.read(name).decode("utf-8"))
            except (KeyError, UnicodeDecodeError, json.JSONDecodeError) as exc:
                errors.append(f"Invalid or missing packaged JSON {name}: {exc}")
                return {}

        configured_prefix = (
            PurePosixPath(CONFIGURED_FEATURE_MIGRATION_PATHS[0]).parent.as_posix() + "/"
        )
        packaged_configured_paths = [name for name in names if name.startswith(configured_prefix)]
        try:
            require_exact_configured_feature_paths(
                packaged_configured_paths, "Packaged private JAR"
            )
            validate_migrated_configured_feature_documents(
                [(name, archive_json(name)) for name in packaged_configured_paths]
            )
        except (KeyError, ValueError, ValidationError) as exc:
            errors.append(f"Packaged configured-feature migration is invalid: {exc}")

        metadata = archive_json("fabric.mod.json")
        if metadata.get("id") != "ribbits":
            errors.append(f"Unexpected mod ID: {metadata.get('id')!r}")
        if metadata.get("version") != "4.1.6+26.2-port-canary2":
            errors.append(f"Unexpected packaged version: {metadata.get('version')!r}")
        if metadata.get("environment") != "*":
            errors.append(f"Unexpected Fabric environment: {metadata.get('environment')!r}")
        if "accessWidener" in metadata:
            errors.append("Unexpected access widener declaration remains")
        expected_dependencies = {
            "minecraft": ">=26.2",
            "java": ">=25",
            "yungsapi": ">=26.2-Fabric-6.1.1-compat.1",
            "fabric-api": ">=0.157.0",
            "geckolib": ">=5.5.1",
            "cloth-config2": ">=26.2.155",
        }
        actual_dependencies = metadata.get("depends", {})
        for dependency, expected in expected_dependencies.items():
            if actual_dependencies.get(dependency) != expected:
                errors.append(
                    f"Dependency {dependency} differs: expected {expected!r}, "
                    f"got {actual_dependencies.get(dependency)!r}"
                )

        entrypoints = metadata.get("entrypoints", {})
        expected_entrypoints = {
            "main": ["com.yungnickyoung.minecraft.ribbits.fabric.RibbitsFabric"],
            "client": [
                "com.yungnickyoung.minecraft.ribbits.fabric.client.RibbitsFabricClient"
            ],
            "modmenu": ["com.yungnickyoung.minecraft.ribbits.fabric.compat.ModMenuCompat"],
        }
        if entrypoints != expected_entrypoints:
            errors.append(f"Entrypoint mapping differs: {entrypoints!r}")
        for values in entrypoints.values():
            for class_name in values:
                class_entry = class_name.replace(".", "/") + ".class"
                if class_entry not in name_set:
                    errors.append(f"Entrypoint class missing: {class_name}")

        expected_mixins = ["ribbits.mixins.json", "ribbits.fabric.mixins.json"]
        actual_mixins = metadata.get("mixins")
        if actual_mixins != expected_mixins:
            errors.append(f"Mixin config mapping differs: {actual_mixins!r}")
        for mixin_name in expected_mixins:
            mixin_config = archive_json(mixin_name)
            if mixin_config.get("compatibilityLevel") != "JAVA_25":
                errors.append(f"Unexpected compatibility level in {mixin_name}")
            if "refmap" in mixin_config:
                errors.append(f"Unexpected refmap remains in {mixin_name}")
            package = mixin_config.get("package", "")
            for side in ("mixins", "client"):
                for relative_class in mixin_config.get(side, []):
                    class_name = f"{package}.{relative_class}"
                    class_entry = class_name.replace(".", "/") + ".class"
                    if class_entry not in name_set:
                        errors.append(
                            f"Declared {side} mixin class missing from {mixin_name}: {class_name}"
                        )
                    if side == "mixins" and ".client." in class_name:
                        errors.append(
                            f"Client-only mixin declared in common array of {mixin_name}: {class_name}"
                        )

        pack = archive_json("pack.mcmeta").get("pack", {})
        if pack.get("min_format") != [88, 0] or pack.get("max_format") != [107, 1]:
            errors.append(
                f"Unexpected pack format bounds: min={pack.get('min_format')!r}, "
                f"max={pack.get('max_format')!r}"
            )

        nested = sorted(name for name in names if name.startswith("META-INF/jars/") and name.endswith(".jar"))
        expected_nested = {
            "META-INF/jars/javassist-3.29.2-GA.jar",
            "META-INF/jars/reflections-0.10.2.jar",
        }
        if set(nested) != expected_nested:
            errors.append(f"Unexpected nested JAR set: {nested}")
        if any("yung" in name.casefold() for name in nested):
            errors.append("YUNG's API was unexpectedly nested in the Ribbits JAR")

        all_bytes = b"\n".join(archive.read(name) for name in names if name.endswith((".class", ".json")))
        for stale in (b"ribbits-1_21_10", b"ribbits-fabric-1_21_1", b"ribbit_master"):
            if stale in all_bytes:
                errors.append(f"Stale identity/resource marker remains in JAR: {stale.decode()}")
        if b"ribbits-26_2" not in all_bytes:
            errors.append("Resolved ribbits-26_2 config identity is absent from packaged code/resources")

        asset_namespaces = {
            PurePosixPath(name).parts[1]
            for name in names
            if name.startswith("assets/") and len(PurePosixPath(name).parts) > 2
        }
        data_namespaces = {
            PurePosixPath(name).parts[1]
            for name in names
            if name.startswith("data/") and len(PurePosixPath(name).parts) > 2
        }
        if asset_namespaces != {"ribbits"}:
            errors.append(f"Unexpected packaged asset namespaces: {sorted(asset_namespaces)}")
        if data_namespaces != {"alexsmobs", "minecraft", "ribbits"}:
            errors.append(f"Unexpected packaged data namespaces: {sorted(data_namespaces)}")

        for main_safe_class in (
            "com/yungnickyoung/minecraft/ribbits/RibbitsCommon.class",
            "com/yungnickyoung/minecraft/ribbits/fabric/RibbitsFabric.class",
            "com/yungnickyoung/minecraft/ribbits/fabric/FabricPlatformHelper.class",
        ):
            if main_safe_class in name_set and b"net/minecraft/client/" in archive.read(main_safe_class):
                errors.append(f"Client class reference leaked into main initialization: {main_safe_class}")

    report["jar"] = str(jar_path.resolve())
    report["jar_sha256"] = sha256_file(jar_path)
    report["jar_size"] = jar_path.stat().st_size
    report["classification"] = "STATIC GATE BLOCKED" if errors or report["blockers"] else "STATIC JAR PASS"
    return report


def write_report(path: Path | None, report: dict[str, Any]) -> None:
    if path is not None:
        if path.exists():
            raise ValidationError(f"Refusing to overwrite existing report: {path}")
        path.parent.mkdir(parents=True, exist_ok=True)
        write_json(path, report)
    print(json.dumps(report, indent=2))


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    subparsers = parser.add_subparsers(dest="command", required=True)

    assemble_parser = subparsers.add_parser("assemble")
    assemble_parser.add_argument("--private-root", type=Path, required=True)
    assemble_parser.add_argument("--pristine", type=Path, required=True)
    assemble_parser.add_argument("--minecraft-client", type=Path, required=True)
    assemble_parser.add_argument("--output", type=Path, required=True)
    assemble_parser.add_argument("--manifest", type=Path, required=True)

    tree_parser = subparsers.add_parser("validate-tree")
    tree_parser.add_argument("--private-root", type=Path, required=True)
    tree_parser.add_argument("--resources", type=Path, required=True)
    tree_parser.add_argument("--pristine", type=Path, required=True)
    tree_parser.add_argument("--minecraft-client", type=Path, required=True)
    tree_parser.add_argument("--report", type=Path)

    jar_parser = subparsers.add_parser("validate-jar")
    jar_parser.add_argument("--private-root", type=Path, required=True)
    jar_parser.add_argument("--resources", type=Path, required=True)
    jar_parser.add_argument("--pristine", type=Path, required=True)
    jar_parser.add_argument("--jar", type=Path, required=True)
    jar_parser.add_argument("--minecraft-client", type=Path, required=True)
    jar_parser.add_argument("--report", type=Path)
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    try:
        if args.command == "assemble":
            assemble(
                args.pristine,
                args.minecraft_client,
                args.output,
                args.manifest,
                args.private_root,
            )
            print(f"Private resources staged at {args.output.resolve()}")
            print(f"Manifest written to {args.manifest.resolve()}")
            print(f"AUTHORIZED SUBSTITUTION: {SPAWN_EGG_SUBSTITUTION_NOTICE}")
            return 0
        if args.command == "validate-tree":
            require_private_path(args.resources, args.private_root, "Validated resource tree")
            if args.report is not None:
                require_private_path(args.report, args.private_root, "Validation report")
                require_outside_tree(args.report, args.resources, "Validation report")
            report = validation_report(
                args.resources, args.pristine, args.minecraft_client, args.private_root
            )
            write_report(args.report, report)
            return 1 if report["errors"] or report["blockers"] else 0
        if args.command == "validate-jar":
            require_private_path(args.resources, args.private_root, "Validated resource tree")
            if args.report is not None:
                require_private_path(args.report, args.private_root, "Validation report")
                require_outside_tree(args.report, args.resources, "Validation report")
            report = validate_jar(
                args.resources,
                args.pristine,
                args.jar,
                args.minecraft_client,
                args.private_root,
            )
            write_report(args.report, report)
            return 1 if report["errors"] or report["blockers"] else 0
    except (OSError, KeyError, ValueError, ValidationError, zipfile.BadZipFile) as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        return 2
    raise AssertionError(args.command)


if __name__ == "__main__":
    raise SystemExit(main())
