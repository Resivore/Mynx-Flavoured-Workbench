#!/usr/bin/env python3
"""Assemble and statically inspect Ribbits' uncommitted private resources.

The output of this tool contains All Rights Reserved Ribbits assets. Keep it
under an ignored private/test-build directory and never commit or distribute it.
"""

from __future__ import annotations

import argparse
import base64
import copy
import gzip
import hashlib
import json
import os
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
CANDIDATE_VERSION = "4.1.6+26.2-mynx-canary18"
CANDIDATE_CANARY = 18
PRIVATE_MANIFEST_SCHEMA = "mynx-ribbits-private-resource-manifest/v1"
PRIVATE_MANIFEST_CLASSIFICATION = (
    "PRIVATE MYNX ASSEMBLY STAGED / NONREDISTRIBUTABLE DONOR ASSETS"
)
PRIVATE_ARTIFACT_FILENAME = (
    "ribbits-private-reconstruction-4.1.6+26.2-mynx-canary18.jar"
)
SOURCE_ONLY_ARTIFACT_FILENAME = "ribbits-source-only-4.1.6+26.2-mynx-canary18.jar"
SOURCE_SAFE_PUBLIC_RESOURCE_PATHS = frozenset(
    {
        "assets/ribbits/items/glowcap.json",
        "assets/ribbits/items/toadstool_heart.json",
        "assets/ribbits/items/ribbit_village_explorer_map.json",
        "assets/ribbits/models/item/glowcap.json",
        "assets/ribbits/models/item/toadstool_heart.json",
        "assets/ribbits/models/item/ribbit_village_explorer_map.json",
        "assets/ribbits/textures/item/glowcap.png",
        "assets/ribbits/textures/item/toadstool_heart.png",
        "assets/ribbits/textures/item/ribbit_village_explorer_map.png",
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
)
EXPECTED_PRIVATE_DATA_NAMESPACES = frozenset(
    {"alexsmobs", "minecraft", "ribbits", "trinkets"}
)
FINAL_ITEM_SPRITE_SPECS: dict[str, dict[str, Any]] = {
    "assets/ribbits/textures/item/glowcap.png": {
        "item": "ribbits:glowcap",
        "source_filename": "glowcap_16x16_down1.png",
        "size": 221,
        "sha256": "9f79b36007a4a4e5e0b5683264c335d76c0f7e00572116b74a33b9e26c0d5abe",
        "dimensions": (16, 16),
        "transparent_pixels": 178,
        "opaque_pixels": 78,
    },
    "assets/ribbits/textures/item/toadstool_heart.png": {
        "item": "ribbits:toadstool_heart",
        "source_filename": "4e8e067e-3969-49ea-be28-fb8d91ea932b.png",
        "size": 881,
        "sha256": "024773d1cccfbe15ba4378b53b09d8522e6157c6ef7cb6e693a99ac8ae36ecb0",
        "dimensions": (16, 16),
        "transparent_pixels": 152,
        "opaque_pixels": 104,
    },
    "assets/ribbits/textures/item/ribbit_village_explorer_map.png": {
        "item": "ribbits:ribbit_village_explorer_map",
        "source_filename": "610cbaa3-e4e8-4d55-abb9-377f0545e672.png",
        "size": 506,
        "sha256": "6065e126da4d3d70725cc3adca725e2ce2812ba8a0155a07c1e510b373aa38f5",
        "dimensions": (16, 16),
        "transparent_pixels": 33,
        "opaque_pixels": 223,
    },
    "assets/ribbits/textures/map/decorations/ribbit_village.png": {
        "item": "ribbits:ribbit_village_explorer_map",
        "source_filename": "ribbit_village_marker_8x8.png",
        "size": 122,
        "sha256": "bd05ccfbe6ade532d20fc42f05c665074a0a733a9398069d8d766b515aba861d",
        "dimensions": (8, 8),
        "transparent_pixels": 30,
        "opaque_pixels": 34,
    },
}
FINAL_ITEM_RESOURCE_MODELS = {
    "assets/ribbits/items/glowcap.json": {
        "model": {"type": "minecraft:model", "model": "ribbits:item/glowcap"}
    },
    "assets/ribbits/items/toadstool_heart.json": {
        "model": {"type": "minecraft:model", "model": "ribbits:item/toadstool_heart"}
    },
    "assets/ribbits/items/ribbit_village_explorer_map.json": {
        "model": {
            "type": "minecraft:model",
            "model": "ribbits:item/ribbit_village_explorer_map",
        }
    },
    "assets/ribbits/models/item/glowcap.json": {
        "parent": "minecraft:item/generated",
        "textures": {"layer0": "ribbits:item/glowcap"},
    },
    "assets/ribbits/models/item/toadstool_heart.json": {
        "parent": "minecraft:item/generated",
        "textures": {"layer0": "ribbits:item/toadstool_heart"},
    },
    "assets/ribbits/models/item/ribbit_village_explorer_map.json": {
        "parent": "minecraft:item/generated",
        "textures": {"layer0": "ribbits:item/ribbit_village_explorer_map"},
    },
}
REQUIRED_FABRIC_DEPENDENCIES = {
    "minecraft": ">=26.2",
    "java": ">=25",
    "yungsapi": ">=26.2-Fabric-6.1.1-compat.2",
    "fabric-api": ">=0.157.0",
    "trinkets_updated": ">=4.1.0-beta.3",
    "geckolib": ">=5.5.1",
    "cloth-config2": ">=26.2.155",
    "customportals": ">=4.0.0",
    "matcha_heart_death_compat": ">=0.1.10-0",
}
SOURCE_FILE_COUNT = 287  # 285 assets/data files plus icon.png and logo.png
OUTPUT_FILE_COUNT = 349
# Exact deterministic Canary 12 private staging inventory.
OUTPUT_TOTAL_SIZE = 2_735_161
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
PRISTINE_SPAWN_EGG_IDS = (
    "ribbit_nitwit_spawn_egg",
    "ribbit_fisherman_spawn_egg",
    "ribbit_gardener_spawn_egg",
    "ribbit_merchant_spawn_egg",
    "ribbit_sorcerer_spawn_egg",
)
NEW_SPAWN_EGG_IDS = (
    "ribbit_chef_spawn_egg",
    "ribbit_farmer_spawn_egg",
    "ribbit_prospector_spawn_egg",
    "ribbit_guard_spawn_egg",
    "wandering_ribbit_spawn_egg",
)
SPAWN_EGG_IDS = PRISTINE_SPAWN_EGG_IDS + NEW_SPAWN_EGG_IDS
REGISTERED_ITEM_IDS = BLOCK_ITEM_IDS + ("maraca", "chute_leaf") + SPAWN_EGG_IDS
AUXILIARY_ITEM_DEFINITION_IDS = frozenset({"chute_leaf_open", "chute_leaf_closed"})

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

NEW_PROFESSIONS = ("chef", "farmer", "prospector", "guard")
UMBRELLA_VARIANTS = (1, 2, 3)

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
    "wandering_ribbit",
    *{
        f"umbrella/{profession}/umbrella_{variant}"
        for profession in ("fisherman", "gardener", "merchant", "nitwit", "sorcerer")
        for variant in UMBRELLA_VARIANTS
    },
    *{f"{profession}_ribbit" for profession in NEW_PROFESSIONS},
    *{
        f"umbrella/{profession}/umbrella_{variant}"
        for profession in NEW_PROFESSIONS
        for variant in UMBRELLA_VARIANTS
    },
}

COMPOSITE_TEXTURE_WIDTH = 256
COMPOSITE_TEXTURE_HEIGHT = 128
COMPOSITE_SHARED_TEXTURE_X = 128
SHARED_RIBBIT_TEXTURE_ENTRY = "assets/ribbits/textures/entity/ribbit.png"

DONOR_INPUT_SPECS: dict[str, dict[str, Any]] = {
    "guard": {
        "filename": "GuardRibbits-1.20.1-Fabric-1.0.4.jar",
        "size": 166_896,
        "sha256": "52f1e184dc12cf1e29bc224ab5a640b8ea5875aa9f46067c0907c6a45b7d1869",
        "members": {
            "assets/guardribbits/geo/guard_ribbit.geo.json": {
                "size": 2_411,
                "sha256": "fe1d3f1df26c4cd3d9107c77008208d5baab3581d897b0c3a2f53b032985a021",
            },
            "assets/guardribbits/textures/entity/guard_ribbit.png": {
                "size": 6_490,
                "sha256": "21bd9bc749885db28ac5d451ebd6cd70e47ce7d3e58bc7ab766cc2d1ba689394",
            },
        },
    },
    "useful": {
        "filename": "useful_ribbits-1.0.2-forge-1.20.1.jar",
        "size": 416_439,
        "sha256": "2b56007a985b162477113bb2ea1776d9ce2ce602886ea21a88d8b2500d2ded5d",
        "members": {
            "assets/useful_ribbits/geo/chef_ribbit.geo.json": {
                "size": 2_061,
                "sha256": "d3a217da7ff960963ed5b7d7372c18f0ff1186b4629e07f091b75cc585e45ff8",
            },
            "assets/useful_ribbits/textures/entities/chef_ribbit.png": {
                "size": 1_538,
                "sha256": "7648086fa2cfcda4483f6a3bf03348e35a54d267e008991ad54b099dd5a514b9",
            },
            "assets/useful_ribbits/geo/farmer_ribbit.geo.json": {
                "size": 2_405,
                "sha256": "e1e579469aafa8c5c44e3dd0dcfe73b25881ade91a5bc74cf2b327c9ce4e9822",
            },
            "assets/useful_ribbits/textures/entities/farmer_ribbit.png": {
                "size": 1_542,
                "sha256": "376f1a12cd16ffc536fd9d217dd987ec43dfcf0a7cff819fd37aa74af211e0cd",
            },
            "assets/useful_ribbits/geo/miner_ribbit.geo.json": {
                "size": 2_704,
                "sha256": "63c4f8391d0261119497555388995b9140bce386dd1b3d3d0fb5447a4c5ffcfd",
            },
            "assets/useful_ribbits/textures/entities/miner_ribbit.png": {
                "size": 1_448,
                "sha256": "429593b57ad873fea108c27983eec7c64fd9413d18afb837884b0c0a34b91330",
            },
        },
    },
    "wandering": {
        "filename": "wandering_ribbit-4.0-forge.jar",
        "size": 98_047,
        "sha256": "5bfd24a88c84d6948f72dc19153155ce99ca5bb4baef90acadda0ab0ca529e4a",
        "members": {
            "assets/wandering_ribbit/geo/wandering_ribbit.geo.json": {
                "size": 3_277,
                "sha256": "c86ea5f798f12c4af172a2df1939e318900b349463d6ca9e26a250134f6c8d08",
            },
            "assets/wandering_ribbit/textures/entity/wandering_ribbit.png": {
                "size": 6_233,
                "sha256": "8e481fa8b4e4458adb52d60ab3123801e612835b65cd362264c00801d84d681a",
            },
            "assets/wandering_ribbit/models/custom/umbrella_leaf.json": {
                "size": 3_841,
                "sha256": "59506a4a35e71f5630a22f45332fb0813df1bff4a985096328e206df9a5f5027",
            },
            "assets/wandering_ribbit/textures/item/umbrella_leaf_item.png": {
                "size": 305,
                "sha256": "d595d40e69a838c7ff3bdc5d59de92bc9c84b39a4a3a433d84590ec4fd1c04ad",
            },
            "assets/wandering_ribbit/textures/item/umbrella_leaf_texture.png": {
                "size": 548,
                "sha256": "3505383ee2da238b600c3a445f676209066307f5a2a690252179b767b50f99ff",
            },
        },
    },
}

DONOR_ARCHIVE_FILENAMES = frozenset(
    spec["filename"].casefold() for spec in DONOR_INPUT_SPECS.values()
)
DONOR_PACKAGE_PREFIXES = (
    "assets/guardribbits/",
    "assets/useful_ribbits/",
    "sunbatheproductions28/guardribbits/",
    "me/rogue_one/useful_ribbits/",
    "guardribbits/",
    "assets/wandering_ribbit/",
    "data/wandering_ribbit/",
    "com/cosmicbarri/wandering_ribbit/",
)

PROFESSION_DONOR_ASSETS = {
    "chef": {
        "donor": "useful",
        "model": "assets/useful_ribbits/geo/chef_ribbit.geo.json",
        "texture": "assets/useful_ribbits/textures/entities/chef_ribbit.png",
    },
    "farmer": {
        "donor": "useful",
        "model": "assets/useful_ribbits/geo/farmer_ribbit.geo.json",
        "texture": "assets/useful_ribbits/textures/entities/farmer_ribbit.png",
    },
    "prospector": {
        "donor": "useful",
        "model": "assets/useful_ribbits/geo/miner_ribbit.geo.json",
        "texture": "assets/useful_ribbits/textures/entities/miner_ribbit.png",
    },
    "guard": {
        "donor": "guard",
        "model": "assets/guardribbits/geo/guard_ribbit.geo.json",
        "texture": "assets/guardribbits/textures/entity/guard_ribbit.png",
    },
}

PROFESSION_DONOR_DERIVED_OUTPUTS = frozenset(
    {
        *{
            f"assets/ribbits/geckolib/models/{profession}_ribbit.geo.json"
            for profession in NEW_PROFESSIONS
        },
        *{
            f"assets/ribbits/geckolib/models/umbrella/{profession}/umbrella_{variant}.geo.json"
            for profession in NEW_PROFESSIONS
            for variant in UMBRELLA_VARIANTS
        },
        *{
            f"assets/ribbits/textures/entity/{profession}_ribbit.png"
            for profession in NEW_PROFESSIONS
        },
    }
)

WANDERING_DONOR_DERIVED_OUTPUTS = frozenset(
    {
        "assets/ribbits/geckolib/models/wandering_ribbit.geo.json",
        "assets/ribbits/textures/entity/wandering_ribbit.png",
        "assets/ribbits/models/item/chute_leaf.json",
        "assets/ribbits/models/item/chute_leaf_closed.json",
        "assets/ribbits/items/chute_leaf_closed.json",
        "assets/ribbits/textures/item/drop_leaf_inventory.png",
        "assets/ribbits/textures/item/chute_leaf.png",
    }
)

DONOR_DERIVED_OUTPUTS = frozenset(
    PROFESSION_DONOR_DERIVED_OUTPUTS | WANDERING_DONOR_DERIVED_OUTPUTS
)
USER_AUTHORED_CHUTE_INPUTS = {
    "bbmodel": {
        "filename": "chute_leaf_open.bbmodel",
        "size": 5_953,
        "sha256": "1d2332100daef279fd9bd1ea442714fe82b572a1662e05f40360cdeaa7680444",
    },
    "png": {
        "filename": "chute_leaf_open.png",
        "size": 743,
        "sha256": "c9dcc9db447c84e69306810aaef1818b52525e8df5205432c8eb454743e059b2",
    },
}
USER_AUTHORED_CHUTE_OUTPUTS = frozenset(
    {
        "assets/ribbits/models/item/chute_leaf_open.json",
        "assets/ribbits/items/chute_leaf_open.json",
        "assets/ribbits/textures/item/chute_leaf_open.png",
    }
)
PRIVATE_ASSEMBLED_DERIVED_OUTPUTS = frozenset(DONOR_DERIVED_OUTPUTS | USER_AUTHORED_CHUTE_OUTPUTS)

VILLAGE_RIBBIT_TEMPLATE_PROFESSIONS = {
    "data/ribbits/structure/ribbits/ribbit_nitwit.nbt": "ribbits:nitwit",
    "data/ribbits/structure/ribbits/ribbit_gardener.nbt": "ribbits:gardener",
    "data/ribbits/structure/ribbits/ribbit_fisherman.nbt": "ribbits:fisherman",
    "data/ribbits/structure/ribbits/ribbit_merchant.nbt": "ribbits:merchant",
    "data/ribbits/structure/ribbits/ribbit_sorcerer.nbt": "ribbits:sorcerer",
}
VILLAGE_RIBBIT_TEMPLATE_DATA = {
    "data/ribbits/structure/ribbits/ribbit_fisherman.nbt": {
        "profession": "ribbits:fisherman",
        "umbrella": "ribbits:umbrella_3",
        "instrument": "ribbits:none",
    },
    "data/ribbits/structure/ribbits/ribbit_gardener.nbt": {
        "profession": "ribbits:gardener",
        "umbrella": "ribbits:umbrella_2",
        "instrument": "ribbits:none",
    },
    "data/ribbits/structure/ribbits/ribbit_merchant.nbt": {
        "profession": "ribbits:merchant",
        "umbrella": "ribbits:umbrella_2",
        "instrument": "ribbits:none",
    },
    "data/ribbits/structure/ribbits/ribbit_nitwit.nbt": {
        "profession": "ribbits:nitwit",
        "umbrella": "ribbits:umbrella_3",
        "instrument": "ribbits:bongo",
    },
    "data/ribbits/structure/ribbits/ribbit_sorcerer.nbt": {
        "profession": "ribbits:sorcerer",
        "umbrella": "ribbits:umbrella_1",
        "instrument": "ribbits:none",
    },
}

# The immutable Ribbits 4.1.6 archive stores building templates under
# structure/houses; structure/ribbits contains only the five resident entities.
# Every input hash below is the compressed NBT member hash from the pristine JAR.
PRIVATE_VILLAGE_TEMPLATE_COUNT = 29
PRIVATE_VILLAGE_TEMPLATE_TREE_BEFORE_SHA256 = (
    "9c2f704975baf2fe7e1c530c85a82cc1a69116be609ee769615c422cfb8d0499"
)
PRIVATE_VILLAGE_TEMPLATE_TREE_AFTER_SHA256 = (
    "cb1cac748727cc4387092cee0d77426816204d0986866d44e9995d6948468de0"
)
PRIVATE_VILLAGE_UTILITY_TRANSFORMS: dict[str, dict[str, Any]] = {
    "data/ribbits/structure/houses/brown_sorcerer_house.nbt": {
        "before_sha256": "502dc904d293d411a5ebafed2c7f71b8eed8e36ab2123553ae8ae3295a56aa76",
        "after_sha256": "d6878d280ec391a7ffd48fcd442fbdd33f6b341031e7efe753f98812270d1b63",
        "coordinate": (6, 2, 5),
        "source_state": {
            "Name": "minecraft:brewing_stand",
            "Properties": {
                "has_bottle_0": "false",
                "has_bottle_1": "false",
                "has_bottle_2": "false",
            },
        },
        "replacement_state": {"Name": "minecraft:air"},
        "block_entity_id": "minecraft:brewing_stand",
    },
    "data/ribbits/structure/houses/red_sorcerer_house.nbt": {
        "before_sha256": "ef66d580570c81657500f714b76eb761de910285571ff0ce36442b14e4bc8948",
        "after_sha256": "57dcf47cdece4e459522cea74b69215269a45c81028a2c42f2f3ebfaee43d516",
        "coordinate": (6, 2, 5),
        "source_state": {
            "Name": "minecraft:brewing_stand",
            "Properties": {
                "has_bottle_0": "false",
                "has_bottle_1": "false",
                "has_bottle_2": "false",
            },
        },
        "replacement_state": {"Name": "minecraft:air"},
        "block_entity_id": "minecraft:brewing_stand",
    },
    "data/ribbits/structure/houses/small_house_brown_3.nbt": {
        "before_sha256": "329dd885fd3a26fbf809cc37b74696799bea2e5397a6dd645810261bfbb1055a",
        "after_sha256": "b54530ffebc2284ab4397796b8bbad411193fb318e3dfcf93f61558b78327b87",
        "coordinate": (3, 1, 5),
        "source_state": {
            "Name": "minecraft:damaged_anvil",
            "Properties": {"facing": "west"},
        },
        "replacement_state": {"Name": "minecraft:air"},
        "block_entity_id": None,
    },
    "data/ribbits/structure/houses/small_house_red_3.nbt": {
        "before_sha256": "4652d7c9fa1481b9d210a32140eedc751a797c0d2deb6b6e12f53d4c60955e70",
        "after_sha256": "2c5a76cf50f5993ed0f6f089aefabe3b04feab2e75962e80b8cfa6f5788ccb3e",
        "coordinate": (3, 1, 5),
        "source_state": {
            "Name": "minecraft:damaged_anvil",
            "Properties": {"facing": "west"},
        },
        "replacement_state": {"Name": "minecraft:air"},
        "block_entity_id": None,
    },
}
PRIVATE_VILLAGE_RESTORED_UTILITY_SPECS: dict[str, dict[str, Any]] = {
    "data/ribbits/structure/houses/small_house_brown_2.nbt": {
        "sha256": "7e7ca64fe02c9953b6e3ccf3bf2a2393c3274bbbb3848ae874b4ff8c9c6b1676",
        "size": 2_810,
        "coordinate": (5, 1, 6),
        "state": {
            "Name": "minecraft:smoker",
            "Properties": {"lit": "false", "facing": "north"},
        },
        "block_entity_id": "minecraft:smoker",
        "block_entity_nbt_sha256": (
            "65c78f8c0d18fbe8de274adf10d4d3d7e5365e37e7f7a8f95295719d0265b536"
        ),
    },
    "data/ribbits/structure/houses/small_house_red_2.nbt": {
        "sha256": "a91945113b28214f5be8935efdbb4c42f6ec469bf9ca9bae5074a0579023d20a",
        "size": 2_881,
        "coordinate": (4, 1, 2),
        "state": {
            "Name": "minecraft:blast_furnace",
            "Properties": {"lit": "false", "facing": "south"},
        },
        "block_entity_id": "minecraft:blast_furnace",
        "block_entity_nbt_sha256": (
            "f0f5c4e4e4987767031407cd66b704e739a8c092bb08df7269888f172440ad25"
        ),
    },
}
PRIVATE_VILLAGE_REMOVED_UTILITY_COUNTS = {
    "minecraft:brewing_stand": 2,
    "minecraft:damaged_anvil": 2,
}
PRIVATE_VILLAGE_PRESERVED_BLOCK_COUNTS = {
    "minecraft:barrel": 60,
    "minecraft:chest": 11,
    "minecraft:crafting_table": 6,
    "minecraft:water_cauldron": 8,
    "minecraft:campfire": 2,
    "minecraft:blue_bed": 6,
    "minecraft:gray_bed": 2,
    "minecraft:green_bed": 10,
    "minecraft:light_gray_bed": 10,
    "minecraft:purple_bed": 4,
    "minecraft:red_bed": 6,
    "minecraft:yellow_bed": 4,
    "minecraft:cake": 2,
    "minecraft:smoker": 1,
    "minecraft:blast_furnace": 1,
    "minecraft:furnace": 0,
}
PRIVATE_VILLAGE_PRESERVED_BLOCK_ENTITY_COUNTS = {
    "minecraft:smoker": 1,
    "minecraft:blast_furnace": 1,
    "minecraft:furnace": 0,
}
PRIVATE_VILLAGE_PROCESSOR_SENTINEL_COUNTS = {
    "minecraft:lapis_block": 287,
    "minecraft:end_stone": 78,
    "minecraft:orange_stained_glass": 112,
    "minecraft:red_nether_bricks": 28,
    "minecraft:warped_planks": 152,
    "minecraft:crimson_planks": 1,
    "minecraft:stripped_crimson_stem": 34,
    "minecraft:crimson_fence": 4,
}
PRIVATE_VILLAGE_LOOT_BINDING_COUNTS = {
    "ribbits:chests/fisherman_main": 11,
    "ribbits:chests/fisherman_storage": 25,
    "ribbits:chests/gardener": 3,
    "ribbits:chests/merchant": 4,
    "ribbits:chests/nitwit": 10,
    "ribbits:chests/sorcerer": 6,
}

LOOT_TABLE_PATHS = (
    "data/ribbits/loot_table/chests/fisherman_main.json",
    "data/ribbits/loot_table/chests/sorcerer.json",
)
LOOT_POOL_ENTRY_COUNTS = {
    LOOT_TABLE_PATHS[0]: (4, 4),
    LOOT_TABLE_PATHS[1]: (6, 2),
}
LOOT_EMPTY_WEIGHTS = {
    LOOT_TABLE_PATHS[0]: 15,
    LOOT_TABLE_PATHS[1]: 1,
}
SORCERER_FORBIDDEN_LOOT_ITEM_IDS = frozenset(
    {
        "minecraft:glass_bottle",
        "minecraft:potion",
        "minecraft:splash_potion",
        "minecraft:lingering_potion",
    }
)
SORCERER_REMOVED_LOOT_ENTRY_SPECS = (
    {
        "pool": 0,
        "entry": 2,
        "source": {
            "type": "minecraft:item",
            "weight": 5,
            "functions": [
                {
                    "function": "minecraft:set_count",
                    "count": {
                        "type": "minecraft:uniform",
                        "min": 2.0,
                        "max": 4.0,
                    },
                    "add": False,
                }
            ],
            "name": "minecraft:glass_bottle",
        },
        "replacement": {"type": "minecraft:empty", "weight": 5},
    },
    {
        "pool": 1,
        "entry": 1,
        "source": {
            "type": "minecraft:item",
            "weight": 1,
            "functions": [
                {
                    "function": "minecraft:set_components",
                    "components": {"potion_contents": "minecraft:strong_leaping"},
                }
            ],
            "name": "minecraft:potion",
        },
        "replacement": {"type": "minecraft:empty", "weight": 1},
    },
)
SORCERER_LOOT_OUTPUT_SIZE = 2_563
SORCERER_LOOT_OUTPUT_SHA256 = (
    "5b06e06502bf11f661161e89bf34e329d8f23268b7b0104371038c38ad9b378d"
)

VILLAGE_CHEST_LOOT_TABLE_PATHS = (
    "data/ribbits/loot_table/chests/fisherman_main.json",
    "data/ribbits/loot_table/chests/fisherman_storage.json",
    "data/ribbits/loot_table/chests/gardener.json",
    "data/ribbits/loot_table/chests/merchant.json",
    "data/ribbits/loot_table/chests/nitwit.json",
    "data/ribbits/loot_table/chests/sorcerer.json",
)
VILLAGE_LOOT_POOL_ENTRY_COUNTS = {
    VILLAGE_CHEST_LOOT_TABLE_PATHS[0]: (4, 4),
    VILLAGE_CHEST_LOOT_TABLE_PATHS[1]: (8,),
    VILLAGE_CHEST_LOOT_TABLE_PATHS[2]: (4,),
    VILLAGE_CHEST_LOOT_TABLE_PATHS[3]: (2,),
    VILLAGE_CHEST_LOOT_TABLE_PATHS[4]: (4,),
    VILLAGE_CHEST_LOOT_TABLE_PATHS[5]: (6, 2),
}
GLOWCAP_CURRENCY_SOURCE_ID = "minecraft:amethyst_shard"
GLOWCAP_CURRENCY_TARGET_ID = "ribbits:glowcap"
SOURCE_SAFE_PUBLIC_LOOT_ITEM_IDS = frozenset({GLOWCAP_CURRENCY_TARGET_ID})
GLOWCAP_CURRENCY_ENTRY_SPECS = {
    VILLAGE_CHEST_LOOT_TABLE_PATHS[1]: {
        "pool": 0,
        "entry": 7,
        "weight": 5,
        "count_min": 1.0,
        "count_max": 3.0,
    },
    VILLAGE_CHEST_LOOT_TABLE_PATHS[2]: {
        "pool": 0,
        "entry": 3,
        "weight": 3,
        "count_min": 1.0,
        "count_max": 2.0,
    },
    VILLAGE_CHEST_LOOT_TABLE_PATHS[3]: {
        "pool": 0,
        "entry": 0,
        "weight": 8,
        "count_min": 2.0,
        "count_max": 4.0,
    },
    VILLAGE_CHEST_LOOT_TABLE_PATHS[4]: {
        "pool": 0,
        "entry": 3,
        "weight": 3,
        "count_min": 1.0,
        "count_max": 3.0,
    },
    VILLAGE_CHEST_LOOT_TABLE_PATHS[5]: {
        "pool": 0,
        "entry": 4,
        "weight": 5,
        "count_min": 2.0,
        "count_max": 6.0,
    },
}
UNRELATED_AMETHYST_BLOCK_SPEC = {
    "table": VILLAGE_CHEST_LOOT_TABLE_PATHS[3],
    "pool": 0,
    "entry": 1,
    "value": {
        "type": "minecraft:item",
        "weight": 1,
        "functions": [
            {
                "function": "minecraft:set_count",
                "count": 1,
                "add": False,
            }
        ],
        "name": "minecraft:amethyst_block",
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

PHASE_C_MAP_TRANSLATIONS = {
    "item.ribbits.ribbit_village_explorer_map": "Ribbit Village Explorer Map",
    "item.ribbits.uncharted_ribbit_map": "Uncharted Ribbit Map",
    "item.ribbits.uncharted_ribbit_map.lore": "No Ribbit village could be charted.",
}

EN_US_MYNX_PROFESSION_TRANSLATIONS = {
    "item.ribbits.ribbit_nitwit_spawn_egg": "Musician Ribbit Spawn Egg",
    "item.ribbits.ribbit_chef_spawn_egg": "Chef Ribbit Spawn Egg",
    "item.ribbits.ribbit_farmer_spawn_egg": "Farmer Ribbit Spawn Egg",
    "item.ribbits.ribbit_prospector_spawn_egg": "Prospector Ribbit Spawn Egg",
    "item.ribbits.ribbit_guard_spawn_egg": "Guard Ribbit Spawn Egg",
    "item.ribbits.wandering_ribbit_spawn_egg": "Wandering Ribbit Spawn Egg",
    "item.ribbits.glowcap": "Glowcap",
    "item.ribbits.toadstool_heart": "Toadstool Heart",
    "item.ribbits.chute_leaf": "Drop Leaf",
    "item.ribbits.chute_leaf.tooltip": "Jump again while airborne to deploy.",
    "entity.ribbits.wandering_ribbit": "Wandering Ribbit",
    **PHASE_C_MAP_TRANSLATIONS,
    "entity.ribbits.merchant.gardener.tier_1": "Sprout Tender",
    "entity.ribbits.merchant.gardener.tier_2": "Toadstool Keeper",
    "entity.ribbits.merchant.farmer.tier_1": "Vine Puller",
    "entity.ribbits.merchant.farmer.tier_2": "Root Wrangler",
    "entity.ribbits.merchant.farmer.tier_3": "Mudfield Steward",
    "entity.ribbits.merchant.fisherman.tier_1": "Pond Forager",
    "entity.ribbits.merchant.fisherman.tier_2": "Coral Keeper",
    "entity.ribbits.merchant.fisherman.tier_3": "Amphibian Attendant",
    "entity.ribbits.merchant.fisherman.tier_4": "Opal Angler",
    "entity.ribbits.merchant.fisherman.tier_5": "Monument Mariner",
    "entity.ribbits.merchant.merchant.tier_1": "Moss Peddler",
    "entity.ribbits.merchant.merchant.tier_2": "Lantern Trader",
    "entity.ribbits.merchant.merchant.tier_3": "Glowgoods Baron",
    "entity.ribbits.merchant.chef.tier_1": "Tadpole Cook",
    "entity.ribbits.merchant.chef.tier_2": "Pond Cook",
    "entity.ribbits.merchant.chef.tier_3": "Swamp Chef",
    "entity.ribbits.merchant.chef.tier_4": "Grand Chef",
    "entity.ribbits.merchant.chef.tier_5": "Master of the Feast",
    "entity.ribbits.merchant.sorcerer.tier_1": "Wart Whisperer",
    "entity.ribbits.merchant.sorcerer.tier_2": "Gatecaller",
    "entity.ribbits.merchant.sorcerer.tier_3": "Flask Sage",
    "entity.ribbits.merchant.sorcerer.tier_4": "Deep-Pond Oracle",
    "entity.ribbits.merchant.prospector.tier_1": "Pebble Picker",
    "entity.ribbits.merchant.prospector.tier_2": "Vein-Seeker",
    "entity.ribbits.merchant.prospector.tier_3": "Deep Delver",
    "entity.ribbits.merchant.guard.tier_1": "Pond Sentry",
    "entity.ribbits.merchant.guard.tier_2": "Lily Warden",
    "entity.ribbits.merchant.guard.tier_3": "Marsh Marshal",
    "entity.ribbits.merchant.guard.tier_4": "Bulwark of the Bog",
    "entity.ribbits.merchant.nitwit.musician": "Musician",
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
    "The private Mynx release preserves the palette-only green recolor of Minecraft "
    "26.2's vanilla frog spawn-egg artwork for all ten Ribbits spawn eggs. "
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


def require_exact_originals_member_path(
    path: Path, originals_root: Path, expected_filename: str, label: str
) -> Path:
    root = originals_root.resolve(strict=True)
    if not root.is_dir() or root.name.casefold() != "originals":
        raise ValidationError(f"Originals root must be the resolved originals directory: {root}")
    expected = (root / "mods" / expected_filename).resolve(strict=False)
    candidate = path.resolve(strict=False)
    if candidate != expected:
        raise ValidationError(
            f"{label} must be the exact originals/mods member {expected_filename}: {candidate}"
        )
    if path.is_symlink() or not candidate.is_file():
        raise ValidationError(f"{label} must be an existing regular non-symlink file: {candidate}")
    return candidate


def load_user_authored_chute_inputs(user_assets_root: Path) -> tuple[dict[str, Any], bytes, list[dict[str, Any]]]:
    """Load the two C18 inputs exactly, without ever writing beneath originals/."""
    root = user_assets_root.resolve(strict=True)
    if not root.is_dir() or root.name.casefold() != "originals":
        raise ValidationError(f"Originals root must be the resolved originals directory: {root}")

    loaded: dict[str, bytes] = {}
    paths: dict[str, Path] = {}
    for key, spec in USER_AUTHORED_CHUTE_INPUTS.items():
        candidate = root / "assets" / spec["filename"]
        resolved = candidate.resolve(strict=True)
        if resolved != candidate or candidate.is_symlink() or not candidate.is_file():
            raise ValidationError(f"C18 user-authored Chute input must be an exact regular originals/assets file: {candidate}")
        data = candidate.read_bytes()
        if len(data) != spec["size"] or sha256_bytes(data) != spec["sha256"]:
            raise ValidationError(
                f"C18 user-authored Chute input identity differs for {candidate}: "
                f"expected {spec['size']} bytes/{spec['sha256']}, got {len(data)}/{sha256_bytes(data)}"
            )
        loaded[key] = data
        paths[key] = candidate

    try:
        model = json.loads(loaded["bbmodel"].decode("utf-8"))
    except (UnicodeDecodeError, json.JSONDecodeError) as exc:
        raise ValidationError("C18 user-authored Chute BBModel is not valid UTF-8 JSON") from exc
    if not isinstance(model, dict) or model.get("resolution") != {"width": 32, "height": 32}:
        raise ValidationError("C18 user-authored Chute BBModel texture resolution differs")
    if png_dimensions(loaded["png"], str(paths["png"])) != (32, 32):
        raise ValidationError("C18 user-authored Chute PNG is not 32x32")
    texture = model.get("textures")
    texture = texture[0] if isinstance(texture, list) and len(texture) == 1 else texture
    if not isinstance(texture, dict) or not isinstance(texture.get("source"), str):
        raise ValidationError("C18 user-authored Chute BBModel has no embedded texture")
    prefix = "data:image/png;base64,"
    if not texture["source"].startswith(prefix):
        raise ValidationError("C18 user-authored Chute BBModel embedded texture is not PNG base64")
    try:
        embedded = base64.b64decode(texture["source"][len(prefix):], validate=True)
    except ValueError as exc:
        raise ValidationError("C18 user-authored Chute BBModel embedded texture is invalid base64") from exc
    if embedded != loaded["png"]:
        raise ValidationError("C18 user-authored Chute BBModel embedded PNG differs from the standalone PNG")

    records = [
        {
            "logical_path": f"originals/assets/{USER_AUTHORED_CHUTE_INPUTS[key]['filename']}",
            "size": len(loaded[key]),
            "sha256": sha256_bytes(loaded[key]),
            "unchanged_after_assembly": True,
        }
        for key in ("bbmodel", "png")
    ]
    return model, loaded["png"], records


def load_exact_donor(
    path: Path, originals_root: Path, donor_key: str
) -> tuple[dict[str, bytes], dict[str, Any]]:
    spec = DONOR_INPUT_SPECS[donor_key]
    resolved = require_exact_originals_member_path(
        path, originals_root, spec["filename"], f"{donor_key.title()} donor JAR"
    )
    actual_size = resolved.stat().st_size
    if actual_size != spec["size"]:
        raise ValidationError(
            f"{spec['filename']} size differs: expected {spec['size']}, got {actual_size}"
        )
    actual_hash = sha256_file(resolved)
    if actual_hash != spec["sha256"]:
        raise ValidationError(
            f"{spec['filename']} SHA-256 differs: expected {spec['sha256']}, got {actual_hash}"
        )

    with resolved.open("rb") as donor_stream, zipfile.ZipFile(donor_stream, mode="r") as archive:
        names = [entry.filename for entry in archive.infolist() if not entry.is_dir()]
        for name in names:
            safe_zip_name(name)
        duplicates = sorted(name for name, count in Counter(names).items() if count > 1)
        if duplicates:
            raise ValidationError(f"Duplicate entries in {spec['filename']}: {duplicates}")
        member_bytes: dict[str, bytes] = {}
        for name, member_spec in spec["members"].items():
            matches = [entry for entry in archive.infolist() if entry.filename == name]
            if len(matches) != 1 or matches[0].is_dir():
                raise ValidationError(
                    f"{spec['filename']} must contain exactly one approved member {name}"
                )
            data = archive.read(matches[0])
            if len(data) != member_spec["size"]:
                raise ValidationError(
                    f"Approved donor member size differs for {name}: "
                    f"expected {member_spec['size']}, got {len(data)}"
                )
            member_hash = sha256_bytes(data)
            if member_hash != member_spec["sha256"]:
                raise ValidationError(
                    f"Approved donor member SHA-256 differs for {name}: "
                    f"expected {member_spec['sha256']}, got {member_hash}"
                )
            member_bytes[name] = data

    if set(member_bytes) != set(spec["members"]):
        raise ValidationError(f"Approved donor allowlist accounting differs for {spec['filename']}")
    identity = {
        "filename": spec["filename"],
        "size": actual_size,
        "sha256": actual_hash,
        "approved_members": {
            name: {
                "size": len(data),
                "sha256": sha256_bytes(data),
            }
            for name, data in member_bytes.items()
        },
    }
    return member_bytes, identity


def require_donor_unchanged(path: Path, identity: dict[str, Any], label: str) -> None:
    if not path.is_file():
        raise ValidationError(f"{label} disappeared during private assembly: {path}")
    actual_size = path.stat().st_size
    actual_hash = sha256_file(path)
    if actual_size != identity["size"] or actual_hash != identity["sha256"]:
        raise ValidationError(
            f"{label} changed during private assembly: expected "
            f"{identity['size']} bytes/{identity['sha256']}, got {actual_size}/{actual_hash}"
        )


def png_dimensions(data: bytes, label: str) -> tuple[int, int]:
    if len(data) < 24 or data[:8] != b"\x89PNG\r\n\x1a\n" or data[12:16] != b"IHDR":
        raise ValidationError(f"Invalid PNG header: {label}")
    width, height = struct.unpack(">II", data[16:24])
    if width <= 0 or height <= 0:
        raise ValidationError(f"Invalid PNG dimensions: {label}")
    return width, height


def _png_chunks(data: bytes, label: str) -> list[tuple[bytes, bytes]]:
    if not data.startswith(b"\x89PNG\r\n\x1a\n"):
        raise ValidationError(f"Invalid PNG signature: {label}")
    chunks: list[tuple[bytes, bytes]] = []
    offset = 8
    while offset < len(data):
        if offset + 12 > len(data):
            raise ValidationError(f"Truncated PNG chunk: {label}")
        length = struct.unpack(">I", data[offset : offset + 4])[0]
        end = offset + 12 + length
        if end > len(data):
            raise ValidationError(f"Invalid PNG chunk length: {label}")
        chunk_type = data[offset + 4 : offset + 8]
        chunk_data = data[offset + 8 : offset + 8 + length]
        expected_crc = struct.unpack(">I", data[offset + 8 + length : end])[0]
        actual_crc = zlib.crc32(chunk_type + chunk_data) & 0xFFFFFFFF
        if actual_crc != expected_crc:
            raise ValidationError(f"PNG CRC differs for {label} chunk {chunk_type!r}")
        chunks.append((chunk_type, chunk_data))
        offset = end
        if chunk_type == b"IEND":
            break
    if offset != len(data) or not chunks or chunks[-1][0] != b"IEND":
        raise ValidationError(f"PNG has trailing data or no IEND: {label}")
    return chunks


def _paeth(left: int, above: int, upper_left: int) -> int:
    estimate = left + above - upper_left
    left_distance = abs(estimate - left)
    above_distance = abs(estimate - above)
    upper_left_distance = abs(estimate - upper_left)
    if left_distance <= above_distance and left_distance <= upper_left_distance:
        return left
    return above if above_distance <= upper_left_distance else upper_left


def decode_rgba_png(data: bytes, label: str) -> tuple[int, int, bytes]:
    chunks = _png_chunks(data, label)
    ihdr_values = [chunk for chunk_type, chunk in chunks if chunk_type == b"IHDR"]
    if len(ihdr_values) != 1 or len(ihdr_values[0]) != 13:
        raise ValidationError(f"PNG must contain one 13-byte IHDR: {label}")
    width, height, depth, color_type, compression, filtering, interlace = struct.unpack(
        ">IIBBBBB", ihdr_values[0]
    )
    if (depth, color_type, compression, filtering, interlace) != (8, 6, 0, 0, 0):
        raise ValidationError(
            f"Approved PNG must be non-interlaced 8-bit RGBA: {label}; got "
            f"depth={depth}, color={color_type}, compression={compression}, "
            f"filter={filtering}, interlace={interlace}"
        )
    compressed = b"".join(chunk for chunk_type, chunk in chunks if chunk_type == b"IDAT")
    try:
        scanlines = zlib.decompress(compressed)
    except zlib.error as exc:
        raise ValidationError(f"Could not decompress PNG {label}: {exc}") from exc
    stride = width * 4
    if len(scanlines) != height * (stride + 1):
        raise ValidationError(f"Unexpected decoded scanline length for {label}")

    output = bytearray(height * stride)
    source_offset = 0
    for row_index in range(height):
        filter_type = scanlines[source_offset]
        source_offset += 1
        filtered = scanlines[source_offset : source_offset + stride]
        source_offset += stride
        row_offset = row_index * stride
        for column in range(stride):
            value = filtered[column]
            left = output[row_offset + column - 4] if column >= 4 else 0
            above = output[row_offset - stride + column] if row_index else 0
            upper_left = (
                output[row_offset - stride + column - 4]
                if row_index and column >= 4
                else 0
            )
            if filter_type == 0:
                decoded = value
            elif filter_type == 1:
                decoded = value + left
            elif filter_type == 2:
                decoded = value + above
            elif filter_type == 3:
                decoded = value + ((left + above) // 2)
            elif filter_type == 4:
                decoded = value + _paeth(left, above, upper_left)
            else:
                raise ValidationError(f"Unsupported PNG filter {filter_type} in {label}")
            output[row_offset + column] = decoded & 0xFF
    return width, height, bytes(output)


def final_item_sprite_manifest_records() -> list[dict[str, Any]]:
    return [
        {
            "item": spec["item"],
            "source_filename": spec["source_filename"],
            "path": relative,
            "size": spec["size"],
            "sha256": spec["sha256"],
            "dimensions": list(spec["dimensions"]),
            "format": "non-interlaced 8-bit RGBA PNG",
            "alpha": {
                "transparent_pixels": spec["transparent_pixels"],
                "opaque_pixels": spec["opaque_pixels"],
                "partial_alpha_pixels": 0,
            },
            "packaged_bytes": "exact tracked sprite bytes",
        }
        for relative, spec in sorted(FINAL_ITEM_SPRITE_SPECS.items())
    ]


def validate_final_item_public_resources(root: Path, errors: list[str]) -> None:
    for relative, expected in FINAL_ITEM_RESOURCE_MODELS.items():
        path = root.joinpath(*PurePosixPath(relative).parts)
        if not path.is_file():
            errors.append(f"Final item model resource is missing: {relative}")
            continue
        try:
            actual = json.loads(path.read_text(encoding="utf-8"))
        except (OSError, UnicodeDecodeError, json.JSONDecodeError) as exc:
            errors.append(f"Final item model resource is invalid: {relative}: {exc}")
            continue
        if actual != expected:
            errors.append(
                f"Final item model resource differs: {relative}: "
                f"expected {expected!r}, got {actual!r}"
            )

    for relative, expected in FINAL_ITEM_SPRITE_SPECS.items():
        path = root.joinpath(*PurePosixPath(relative).parts)
        if not path.is_file():
            errors.append(f"Final item sprite is missing: {relative}")
            continue
        try:
            data = path.read_bytes()
            width, height, pixels = decode_rgba_png(data, relative)
        except (OSError, ValidationError) as exc:
            errors.append(f"Final item sprite is invalid: {relative}: {exc}")
            continue
        actual_hash = sha256_bytes(data)
        if len(data) != expected["size"] or actual_hash != expected["sha256"]:
            errors.append(
                f"Final item sprite identity differs for {relative}: expected "
                f"{expected['size']} bytes/{expected['sha256']}, got {len(data)}/{actual_hash}"
            )
        if (width, height) != expected["dimensions"]:
            errors.append(
                f"Final item sprite dimensions differ for {relative}: "
                f"expected {expected['dimensions']}, got {(width, height)}"
            )
        alpha = pixels[3::4]
        transparent = alpha.count(0)
        opaque = alpha.count(255)
        partial = len(alpha) - transparent - opaque
        if (
            transparent != expected["transparent_pixels"]
            or opaque != expected["opaque_pixels"]
            or partial != 0
        ):
            errors.append(
                f"Final item sprite alpha differs for {relative}: expected "
                f"transparent={expected['transparent_pixels']}, "
                f"opaque={expected['opaque_pixels']}, partial=0; got "
                f"transparent={transparent}, opaque={opaque}, partial={partial}"
            )


def _png_chunk(chunk_type: bytes, data: bytes) -> bytes:
    return (
        struct.pack(">I", len(data))
        + chunk_type
        + data
        + struct.pack(">I", zlib.crc32(chunk_type + data) & 0xFFFFFFFF)
    )


def encode_rgba_png(width: int, height: int, pixels: bytes) -> bytes:
    if width <= 0 or height <= 0 or len(pixels) != width * height * 4:
        raise ValidationError("RGBA pixel buffer dimensions differ")
    stride = width * 4
    scanlines = b"".join(
        b"\x00" + pixels[row * stride : (row + 1) * stride] for row in range(height)
    )
    ihdr = struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0)
    return (
        b"\x89PNG\r\n\x1a\n"
        + _png_chunk(b"IHDR", ihdr)
        + _png_chunk(b"IDAT", zlib.compress(scanlines, level=9))
        + _png_chunk(b"IEND", b"")
    )


def build_composite_texture(
    donor_texture: bytes, donor_label: str, shared_texture: bytes
) -> bytes:
    donor_width, donor_height, donor_pixels = decode_rgba_png(donor_texture, donor_label)
    shared_width, shared_height, shared_pixels = decode_rgba_png(
        shared_texture, SHARED_RIBBIT_TEXTURE_ENTRY
    )
    if donor_width not in (64, 128) or donor_height not in (64, 128):
        raise ValidationError(
            f"Donor texture dimensions are outside the approved 64/128 atlas boundary: "
            f"{donor_label}={donor_width}x{donor_height}"
        )
    if (shared_width, shared_height) != (128, 128):
        raise ValidationError("Pristine shared Ribbits texture must be exactly 128x128")
    pixels = bytearray(COMPOSITE_TEXTURE_WIDTH * COMPOSITE_TEXTURE_HEIGHT * 4)
    output_stride = COMPOSITE_TEXTURE_WIDTH * 4
    for row in range(donor_height):
        source = row * donor_width * 4
        destination = row * output_stride
        pixels[destination : destination + donor_width * 4] = donor_pixels[
            source : source + donor_width * 4
        ]
    for row in range(shared_height):
        source = row * shared_width * 4
        destination = row * output_stride + COMPOSITE_SHARED_TEXTURE_X * 4
        pixels[destination : destination + shared_width * 4] = shared_pixels[
            source : source + shared_width * 4
        ]
    result = encode_rgba_png(COMPOSITE_TEXTURE_WIDTH, COMPOSITE_TEXTURE_HEIGHT, bytes(pixels))
    if decode_rgba_png(result, f"composite {donor_label}")[2] != bytes(pixels):
        raise ValidationError(f"Deterministic composite PNG round-trip differs: {donor_label}")
    return result


def load_json_bytes(data: bytes, label: str) -> Any:
    try:
        return json.loads(data.decode("utf-8"))
    except (UnicodeDecodeError, json.JSONDecodeError) as exc:
        raise ValidationError(f"Invalid donor JSON {label}: {exc}") from exc


def require_geometry_document(value: Any, label: str) -> dict[str, Any]:
    if not isinstance(value, dict) or set(value) != {"format_version", "minecraft:geometry"}:
        raise ValidationError(f"Unexpected GeckoLib document fields in {label}")
    geometries = value.get("minecraft:geometry")
    if not isinstance(geometries, list) or len(geometries) != 1:
        raise ValidationError(f"Expected one geometry in {label}")
    geometry = geometries[0]
    if not isinstance(geometry, dict) or set(geometry) != {"description", "bones"}:
        raise ValidationError(f"Unexpected geometry fields in {label}")
    description = geometry.get("description")
    bones = geometry.get("bones")
    if not isinstance(description, dict) or not isinstance(bones, list) or not bones:
        raise ValidationError(f"Invalid geometry description/bones in {label}")
    names = [bone.get("name") for bone in bones if isinstance(bone, dict)]
    if len(names) != len(bones) or any(not isinstance(name, str) for name in names):
        raise ValidationError(f"Every bone must have a string name in {label}")
    duplicates = sorted(name for name, count in Counter(names).items() if count > 1)
    if duplicates:
        raise ValidationError(f"Duplicate bone names in {label}: {duplicates}")
    if "main" not in names or "body" not in names:
        raise ValidationError(f"Required main/body bones are missing in {label}")
    width = description.get("texture_width")
    height = description.get("texture_height")
    if not isinstance(width, int) or not isinstance(height, int) or width <= 0 or height <= 0:
        raise ValidationError(f"Invalid geometry texture dimensions in {label}")
    return geometry


def cube_uv_rectangles(cube: dict[str, Any], context: str) -> list[tuple[float, float, float, float]]:
    size = cube.get("size")
    uv = cube.get("uv")
    if (
        not isinstance(size, list)
        or len(size) != 3
        or any(not isinstance(value, (int, float)) or isinstance(value, bool) for value in size)
    ):
        raise ValidationError(f"Invalid cube size in {context}: {size!r}")
    x_size, y_size, z_size = (float(value) for value in size)
    if min(x_size, y_size, z_size) < 0:
        raise ValidationError(f"Negative cube size in {context}: {size!r}")
    rectangles: list[tuple[float, float, float, float]] = []
    if isinstance(uv, list):
        if (
            len(uv) != 2
            or any(not isinstance(value, (int, float)) or isinstance(value, bool) for value in uv)
        ):
            raise ValidationError(f"Invalid box UV in {context}: {uv!r}")
        u, v = (float(value) for value in uv)
        candidates = (
            (u, v + z_size, z_size, y_size),
            (u + z_size, v + z_size, x_size, y_size),
            (u + z_size + x_size, v + z_size, z_size, y_size),
            (u + 2 * z_size + x_size, v + z_size, x_size, y_size),
            (u + z_size, v, x_size, z_size),
            (u + z_size + x_size, v, x_size, z_size),
        )
        rectangles.extend(rectangle for rectangle in candidates if rectangle[2] and rectangle[3])
    elif isinstance(uv, dict):
        for face, face_data in uv.items():
            if not isinstance(face_data, dict):
                raise ValidationError(f"Invalid per-face UV in {context}/{face}")
            face_uv = face_data.get("uv")
            face_size = face_data.get("uv_size")
            if (
                not isinstance(face_uv, list)
                or len(face_uv) != 2
                or not isinstance(face_size, list)
                or len(face_size) != 2
                or any(
                    not isinstance(value, (int, float)) or isinstance(value, bool)
                    for value in (*face_uv, *face_size)
                )
            ):
                raise ValidationError(f"Invalid per-face UV coordinates in {context}/{face}")
            width, height = abs(float(face_size[0])), abs(float(face_size[1]))
            if width and height:
                rectangles.append((float(face_uv[0]), float(face_uv[1]), width, height))
    else:
        raise ValidationError(f"Cube lacks supported UV data in {context}")
    return rectangles


def validate_model_uv_bounds(
    value: Any,
    label: str,
    minimum_u: float,
    maximum_u: float,
    maximum_v: float = COMPOSITE_TEXTURE_HEIGHT,
    bone_names: set[str] | None = None,
) -> None:
    geometry = require_geometry_document(value, label)
    checked = 0
    for bone in geometry["bones"]:
        if bone_names is not None and bone["name"] not in bone_names:
            continue
        cubes = bone.get("cubes", [])
        if not isinstance(cubes, list):
            raise ValidationError(f"Bone cubes must be a list in {label}/{bone['name']}")
        for index, cube in enumerate(cubes):
            if not isinstance(cube, dict):
                raise ValidationError(f"Invalid cube in {label}/{bone['name']}[{index}]")
            for u, v, width, height in cube_uv_rectangles(
                cube, f"{label}/{bone['name']}[{index}]"
            ):
                if u < minimum_u or v < 0 or u + width > maximum_u or v + height > maximum_v:
                    raise ValidationError(
                        f"Cube UV escapes approved atlas region in {label}/{bone['name']}[{index}]: "
                        f"({u}, {v}, {width}, {height}) not within "
                        f"[{minimum_u}, {maximum_u}]x[0, {maximum_v}]"
                    )
                checked += 1
    if checked == 0:
        raise ValidationError(f"No textured cube faces were validated in {label}")


def offset_cube_uv(cube: dict[str, Any], u_offset: int, context: str) -> None:
    uv = cube.get("uv")
    if isinstance(uv, list) and len(uv) == 2:
        uv[0] += u_offset
        return
    if isinstance(uv, dict):
        for face, face_data in uv.items():
            if not isinstance(face_data, dict) or not isinstance(face_data.get("uv"), list):
                raise ValidationError(f"Invalid per-face UV while offsetting {context}/{face}")
            face_data["uv"][0] += u_offset
        return
    raise ValidationError(f"Unsupported cube UV while offsetting {context}")


def build_profession_model(donor_model: Any, label: str) -> dict[str, Any]:
    result = copy.deepcopy(donor_model)
    geometry = require_geometry_document(result, label)
    donor_width = geometry["description"]["texture_width"]
    donor_height = geometry["description"]["texture_height"]
    if donor_width not in (64, 128) or donor_height not in (64, 128):
        raise ValidationError(f"Unexpected donor model texture dimensions in {label}")
    validate_model_uv_bounds(result, label, 0, COMPOSITE_SHARED_TEXTURE_X)
    geometry["description"]["texture_width"] = COMPOSITE_TEXTURE_WIDTH
    geometry["description"]["texture_height"] = COMPOSITE_TEXTURE_HEIGHT
    return result


def build_umbrella_composite_model(
    profession_model: Any, umbrella_model: Any, profession: str, variant: int
) -> dict[str, Any]:
    label = f"umbrella/{profession}/umbrella_{variant}"
    result = copy.deepcopy(profession_model)
    profession_geometry = require_geometry_document(result, f"{profession} donor model")
    umbrella_geometry = require_geometry_document(umbrella_model, f"pristine {label}")
    expected_bone_name = "umbrella" if variant == 1 else f"umbrella{variant}"
    matching_bones = [
        bone for bone in umbrella_geometry["bones"] if bone["name"] == expected_bone_name
    ]
    if len(matching_bones) != 1 or matching_bones[0].get("parent") != "body":
        raise ValidationError(f"Pristine {label} must have one body-parented umbrella bone")
    umbrella_bone = copy.deepcopy(matching_bones[0])
    for index, cube in enumerate(umbrella_bone.get("cubes", [])):
        offset_cube_uv(cube, COMPOSITE_SHARED_TEXTURE_X, f"{label}[{index}]")
    if any(bone["name"] == expected_bone_name for bone in profession_geometry["bones"]):
        raise ValidationError(f"Donor profession model already defines {expected_bone_name}: {profession}")
    profession_geometry["bones"].append(umbrella_bone)

    source_description = umbrella_geometry["description"]
    target_description = profession_geometry["description"]
    target_description["identifier"] = source_description["identifier"]
    target_description["texture_width"] = COMPOSITE_TEXTURE_WIDTH
    target_description["texture_height"] = COMPOSITE_TEXTURE_HEIGHT
    target_description["visible_bounds_width"] = max(
        target_description.get("visible_bounds_width", 0),
        source_description.get("visible_bounds_width", 0),
    )
    target_description["visible_bounds_height"] = max(
        target_description.get("visible_bounds_height", 0),
        source_description.get("visible_bounds_height", 0),
    )
    validate_model_uv_bounds(
        result,
        label,
        COMPOSITE_SHARED_TEXTURE_X,
        COMPOSITE_TEXTURE_WIDTH,
        bone_names={expected_bone_name},
    )
    donor_bones = {bone["name"] for bone in profession_geometry["bones"]} - {expected_bone_name}
    validate_model_uv_bounds(
        result,
        label,
        0,
        COMPOSITE_SHARED_TEXTURE_X,
        bone_names=donor_bones,
    )
    return result


class NbtScanner:
    """Bounded NBT scanner for exact, byte-preserving private structure transforms."""

    def __init__(self, data: bytes, label: str):
        self.data = data
        self.label = label
        self.offset = 0
        self.strings: dict[tuple[str, ...], str] = {}
        self.ints: dict[tuple[str, ...], int] = {}
        self.int_payload_ranges: dict[tuple[str, ...], tuple[int, int]] = {}
        self.lists: dict[tuple[str, ...], tuple[int, int]] = {}
        self.list_details: dict[tuple[str, ...], dict[str, int]] = {}
        self.list_items: dict[tuple[str, ...], dict[str, Any]] = {}
        self.compound_fields: dict[tuple[str, ...], list[tuple[str, int]]] = {}
        self.named_tags: list[dict[str, Any]] = []
        self.named_compounds: list[dict[str, Any]] = []

    def read(self, length: int) -> bytes:
        if length < 0 or self.offset + length > len(self.data):
            raise ValidationError(f"Truncated NBT while reading {self.label}")
        result = self.data[self.offset : self.offset + length]
        self.offset += length
        return result

    def read_u8(self) -> int:
        return self.read(1)[0]

    def read_i32(self) -> int:
        return struct.unpack(">i", self.read(4))[0]

    def read_string(self) -> str:
        length = struct.unpack(">H", self.read(2))[0]
        try:
            return self.read(length).decode("utf-8")
        except UnicodeDecodeError as exc:
            raise ValidationError(f"Invalid UTF-8 NBT string in {self.label}") from exc

    def scan_payload(self, tag_type: int, path: tuple[str, ...]) -> None:
        if tag_type == 0:
            raise ValidationError(f"Unexpected standalone TAG_End in {self.label}")
        if tag_type in {1, 2, 3, 4, 5, 6}:
            payload_start = self.offset
            payload = self.read({1: 1, 2: 2, 3: 4, 4: 8, 5: 4, 6: 8}[tag_type])
            if tag_type == 3:
                self.ints[path] = struct.unpack(">i", payload)[0]
                self.int_payload_ranges[path] = (payload_start, self.offset)
            return
        if tag_type == 7:
            length = self.read_i32()
            if length < 0:
                raise ValidationError(f"Negative TAG_Byte_Array length in {self.label}")
            self.read(length)
            return
        if tag_type == 8:
            self.strings[path] = self.read_string()
            return
        if tag_type == 9:
            element_type = self.read_u8()
            length_start = self.offset
            length = self.read_i32()
            if length < 0 or (element_type == 0 and length != 0):
                raise ValidationError(f"Invalid TAG_List header in {self.label}")
            self.lists[path] = (element_type, length)
            detail = {
                "element_type": element_type,
                "length": length,
                "length_start": length_start,
                "items_start": self.offset,
            }
            self.list_details[path] = detail
            for index in range(length):
                item_path = path + (f"[{index}]",)
                item_start = self.offset
                self.scan_payload(element_type, item_path)
                self.list_items[item_path] = {
                    "tag_type": element_type,
                    "start": item_start,
                    "end": self.offset,
                }
            detail["items_end"] = self.offset
            return
        if tag_type == 10:
            fields: list[tuple[str, int]] = []
            self.compound_fields[path] = fields
            while True:
                tag_start = self.offset
                child_type = self.read_u8()
                if child_type == 0:
                    break
                if child_type > 12:
                    raise ValidationError(f"Unknown NBT tag type {child_type} in {self.label}")
                name = self.read_string()
                fields.append((name, child_type))
                payload_start = self.offset
                child_path = path + (name,)
                self.scan_payload(child_type, child_path)
                named_tag = {
                    "path": child_path,
                    "tag_type": child_type,
                    "start": tag_start,
                    "payload_start": payload_start,
                    "end": self.offset,
                }
                self.named_tags.append(named_tag)
                if child_type == 10:
                    self.named_compounds.append(named_tag)
            return
        if tag_type in {11, 12}:
            length = self.read_i32()
            if length < 0:
                raise ValidationError(f"Negative NBT array length in {self.label}")
            self.read(length * (4 if tag_type == 11 else 8))
            return
        raise ValidationError(f"Unknown NBT tag type {tag_type} in {self.label}")

    def scan_root(self) -> None:
        root_type = self.read_u8()
        if root_type != 10:
            raise ValidationError(f"NBT root is not a compound in {self.label}")
        self.read_string()  # Root name is semantically irrelevant but length-checked.
        self.scan_payload(root_type, ())
        if self.offset != len(self.data):
            raise ValidationError(f"Trailing bytes after NBT root in {self.label}")


def decode_nbt_bytes(data: bytes, label: str) -> bytes:
    if data.startswith(b"\x1f\x8b"):
        try:
            return gzip.decompress(data)
        except gzip.BadGzipFile as exc:
            raise ValidationError(f"Invalid compressed NBT {label}: {exc}") from exc
    return data


def deterministic_gzip(data: bytes) -> bytes:
    compressor = zlib.compressobj(level=9, method=zlib.DEFLATED, wbits=-15)
    compressed = compressor.compress(data) + compressor.flush()
    return (
        b"\x1f\x8b\x08\x00\x00\x00\x00\x00\x02\xff"
        + compressed
        + struct.pack("<II", zlib.crc32(data) & 0xFFFFFFFF, len(data) & 0xFFFFFFFF)
    )


def scan_nbt(data: bytes, label: str) -> NbtScanner:
    scanner = NbtScanner(data, label)
    scanner.scan_root()
    return scanner


def remove_exact_village_ribbit_data(data: bytes, path: str) -> bytes:
    if path not in VILLAGE_RIBBIT_TEMPLATE_DATA:
        raise ValidationError(f"Unexpected village resident template: {path}")
    if not data.startswith(b"\x1f\x8b"):
        raise ValidationError(f"Village resident template must remain gzip-compressed: {path}")
    decoded = decode_nbt_bytes(data, path)
    scanner = scan_nbt(decoded, path)
    if scanner.lists.get(("entities",)) != (10, 1):
        raise ValidationError(f"{path} must contain exactly one compound resident entity")
    id_path = ("entities", "[0]", "nbt", "id")
    if scanner.strings.get(id_path) != "ribbits:ribbit":
        raise ValidationError(f"{path} resident must be exactly ribbits:ribbit")
    ribbit_data_path = ("entities", "[0]", "nbt", "RibbitData")
    matches = [item for item in scanner.named_compounds if item["path"] == ribbit_data_path]
    if len(matches) != 1:
        raise ValidationError(f"{path} must contain exactly one entities[0].nbt.RibbitData tag")
    expected = VILLAGE_RIBBIT_TEMPLATE_DATA[path]
    fields = scanner.compound_fields.get(ribbit_data_path)
    if fields is None or set(fields) != {(name, 8) for name in expected} or len(fields) != len(expected):
        raise ValidationError(f"{path} RibbitData must have exactly the pinned string keys")
    actual = {
        name: scanner.strings.get(ribbit_data_path + (name,)) for name in expected
    }
    if actual != expected:
        raise ValidationError(f"{path} pinned RibbitData differs: expected {expected}, got {actual}")

    match = matches[0]
    transformed = decoded[: match["start"]] + decoded[match["end"] :]
    transformed_scanner = scan_nbt(transformed, f"transformed {path}")
    if any(item["path"] == ribbit_data_path for item in transformed_scanner.named_compounds):
        raise ValidationError(f"RibbitData remained after exact splice in {path}")
    if transformed_scanner.strings.get(id_path) != "ribbits:ribbit":
        raise ValidationError(f"Resident identity changed while splicing {path}")
    if transformed != decoded[: match["start"]] + decoded[match["end"] :]:
        raise AssertionError("NBT splice changed bytes outside the named RibbitData tag")
    return deterministic_gzip(transformed)


def remove_village_profession_assignments(root: Path) -> dict[str, Any]:
    structure_root = root / "data/ribbits/structure"
    actual_templates = {
        path.relative_to(root).as_posix()
        for path in (structure_root / "ribbits").glob("*.nbt")
    }
    expected_templates = set(VILLAGE_RIBBIT_TEMPLATE_DATA)
    if actual_templates != expected_templates:
        raise ValidationError(
            "Private resident template set differs: "
            f"missing={sorted(expected_templates - actual_templates)}, "
            f"extra={sorted(actual_templates - expected_templates)}"
        )
    records: list[dict[str, Any]] = []
    for relative in sorted(expected_templates):
        path = root / PurePosixPath(relative)
        before = path.read_bytes()
        after = remove_exact_village_ribbit_data(before, relative)
        path.write_bytes(after)
        records.append(
            {
                "template": relative,
                "removed_tag": "entities[0].nbt.RibbitData",
                "previous": VILLAGE_RIBBIT_TEMPLATE_DATA[relative],
                "before_sha256": sha256_bytes(before),
                "after_sha256": sha256_bytes(after),
            }
        )
    return {
        "count": len(records),
        "policy": "caller-random explicit Java village profession pool",
        "templates": records,
    }


def validate_village_profession_assignments(root: Path, errors: list[str]) -> None:
    expected_templates = set(VILLAGE_RIBBIT_TEMPLATE_DATA)
    actual_templates = {
        path.relative_to(root).as_posix()
        for path in (root / "data/ribbits/structure/ribbits").glob("*.nbt")
    }
    if actual_templates != expected_templates:
        errors.append(
            "Private resident template set differs after assembly: "
            f"missing={sorted(expected_templates - actual_templates)}, "
            f"extra={sorted(actual_templates - expected_templates)}"
        )
        return
    for path in sorted((root / "data/ribbits/structure").rglob("*.nbt")):
        relative = path.relative_to(root).as_posix()
        try:
            scanner = scan_nbt(decode_nbt_bytes(path.read_bytes(), relative), relative)
        except ValidationError as exc:
            errors.append(str(exc))
            continue
        retained = [
            item["path"] for item in scanner.named_compounds if item["path"][-1:] == ("RibbitData",)
        ]
        if retained:
            errors.append(f"Private village NBT retains explicit RibbitData in {relative}: {retained}")
        retained_fields = [
            path
            for path in scanner.strings
            if path[-1:] in {("profession",), ("instrument",), ("umbrella",)}
            and "entities" in path
        ]
        if retained_fields:
            errors.append(
                f"Private village resident NBT retains explicit variant fields in "
                f"{relative}: {retained_fields}"
            )
        if relative in expected_templates:
            if scanner.lists.get(("entities",)) != (10, 1):
                errors.append(f"{relative} no longer has exactly one resident entity")
            if scanner.strings.get(("entities", "[0]", "nbt", "id")) != "ribbits:ribbit":
                errors.append(f"{relative} resident identity is no longer ribbits:ribbit")


def _require_unique_nbt_fields(
    scanner: NbtScanner,
    path: tuple[str, ...],
    required: set[tuple[str, int]],
    optional: set[tuple[str, int]],
    label: str,
) -> list[tuple[str, int]]:
    fields = scanner.compound_fields.get(path)
    if fields is None:
        raise ValidationError(f"Missing NBT compound {'.'.join(path)} in {label}")
    if len({name for name, _tag_type in fields}) != len(fields):
        raise ValidationError(f"Duplicate NBT compound field in {'.'.join(path)} of {label}")
    actual = set(fields)
    if not required.issubset(actual) or not actual.issubset(required | optional):
        raise ValidationError(
            f"Unexpected NBT fields in {'.'.join(path)} of {label}: {fields}"
        )
    return fields


def _structure_palette(scanner: NbtScanner, label: str) -> list[dict[str, Any]]:
    palette_header = scanner.lists.get(("palette",))
    if palette_header is None or palette_header[0] != 10 or palette_header[1] <= 0:
        raise ValidationError(f"{label} must contain a nonempty compound palette")
    states: list[dict[str, Any]] = []
    for index in range(palette_header[1]):
        path = ("palette", f"[{index}]")
        fields = _require_unique_nbt_fields(
            scanner,
            path,
            {("Name", 8)},
            {("Properties", 10)},
            label,
        )
        name = scanner.strings.get(path + ("Name",))
        if not isinstance(name, str) or not name:
            raise ValidationError(f"Palette state {index} lacks a valid Name in {label}")
        state: dict[str, Any] = {"Name": name}
        if ("Properties", 10) in fields:
            property_path = path + ("Properties",)
            property_fields = scanner.compound_fields.get(property_path)
            if property_fields is None or not property_fields:
                raise ValidationError(f"Empty or missing Properties in palette state {index} of {label}")
            if (
                len({key for key, _tag_type in property_fields}) != len(property_fields)
                or any(tag_type != 8 for _key, tag_type in property_fields)
            ):
                raise ValidationError(
                    f"Palette state {index} has non-string or duplicate Properties in {label}"
                )
            properties = {
                key: scanner.strings.get(property_path + (key,))
                for key, _tag_type in property_fields
            }
            if any(not isinstance(value, str) for value in properties.values()):
                raise ValidationError(f"Palette state {index} has missing Properties in {label}")
            state["Properties"] = properties
        states.append(state)
    return states


def inspect_structure_template(data: bytes, label: str) -> dict[str, Any]:
    if not data.startswith(b"\x1f\x8b"):
        raise ValidationError(f"Private structure template must remain gzip-compressed: {label}")
    decoded = decode_nbt_bytes(data, label)
    scanner = scan_nbt(decoded, label)
    palette = _structure_palette(scanner, label)
    blocks_header = scanner.lists.get(("blocks",))
    if blocks_header is None or blocks_header[0] != 10:
        raise ValidationError(f"{label} must contain a compound blocks list")

    blocks: list[dict[str, Any]] = []
    positions: set[tuple[int, int, int]] = set()
    for index in range(blocks_header[1]):
        path = ("blocks", f"[{index}]")
        fields = _require_unique_nbt_fields(
            scanner,
            path,
            {("pos", 9), ("state", 3)},
            {("nbt", 10)},
            label,
        )
        if scanner.lists.get(path + ("pos",)) != (3, 3):
            raise ValidationError(f"Block {index} position is not exactly three TAG_Ints in {label}")
        try:
            position = tuple(
                scanner.ints[path + ("pos", f"[{axis}]")] for axis in range(3)
            )
            state_index = scanner.ints[path + ("state",)]
            state_range = scanner.int_payload_ranges[path + ("state",)]
        except KeyError as exc:
            raise ValidationError(f"Block {index} lacks exact position/state data in {label}") from exc
        if position in positions:
            raise ValidationError(f"Duplicate block coordinate {position} in {label}")
        positions.add(position)
        if state_index < 0 or state_index >= len(palette):
            raise ValidationError(f"Block {index} has invalid palette index {state_index} in {label}")
        item_range = scanner.list_items.get(path)
        if item_range is None or item_range["tag_type"] != 10:
            raise ValidationError(f"Block {index} lacks an exact compound payload range in {label}")
        nbt_matches = [tag for tag in scanner.named_tags if tag["path"] == path + ("nbt",)]
        expects_nbt = ("nbt", 10) in fields
        if len(nbt_matches) != (1 if expects_nbt else 0):
            raise ValidationError(f"Block {index} has inconsistent block-entity NBT in {label}")
        block_entity_id = None
        loot_table = None
        nbt_range = None
        if nbt_matches:
            nbt_range = (nbt_matches[0]["start"], nbt_matches[0]["end"])
            block_entity_id = scanner.strings.get(path + ("nbt", "id"))
            if not isinstance(block_entity_id, str) or not block_entity_id:
                raise ValidationError(f"Block {index} block entity lacks a valid id in {label}")
            loot_table = scanner.strings.get(path + ("nbt", "LootTable"))
            if loot_table is not None and not isinstance(loot_table, str):
                raise ValidationError(f"Block {index} has invalid LootTable data in {label}")
        blocks.append(
            {
                "index": index,
                "path": path,
                "position": position,
                "state_index": state_index,
                "state": palette[state_index],
                "state_payload_range": state_range,
                "payload_range": (item_range["start"], item_range["end"]),
                "nbt_range": nbt_range,
                "block_entity_id": block_entity_id,
                "loot_table": loot_table,
            }
        )
    return {
        "decoded": decoded,
        "scanner": scanner,
        "palette": palette,
        "blocks": blocks,
    }


def _encode_nbt_string(value: str) -> bytes:
    encoded = value.encode("utf-8")
    if len(encoded) > 0xFFFF:
        raise ValidationError("NBT string is too long")
    return struct.pack(">H", len(encoded)) + encoded


def _encode_palette_state_payload(state: dict[str, Any]) -> bytes:
    if set(state) not in ({"Name"}, {"Name", "Properties"}):
        raise ValidationError(f"Unsupported appended palette state: {state!r}")
    name = state.get("Name")
    if not isinstance(name, str) or not name:
        raise ValidationError(f"Invalid appended palette state Name: {state!r}")
    payload = b"\x08" + _encode_nbt_string("Name") + _encode_nbt_string(name)
    properties = state.get("Properties")
    if properties is not None:
        if not isinstance(properties, dict) or not properties:
            raise ValidationError(f"Invalid appended palette Properties: {state!r}")
        payload += b"\x0a" + _encode_nbt_string("Properties")
        for key in sorted(properties):
            value = properties[key]
            if not isinstance(key, str) or not isinstance(value, str):
                raise ValidationError(f"Invalid appended palette property: {key!r}={value!r}")
            payload += b"\x08" + _encode_nbt_string(key) + _encode_nbt_string(value)
        payload += b"\x00"
    return payload + b"\x00"


def _apply_exact_byte_patches(
    data: bytes, patches: list[tuple[int, int, bytes, str]], label: str
) -> bytes:
    result = bytearray()
    previous_end = 0
    for start, end, replacement, description in sorted(patches, key=lambda patch: patch[0]):
        if start < previous_end or start < 0 or end < start or end > len(data):
            raise ValidationError(
                f"Overlapping or invalid {description} byte patch [{start}, {end}) in {label}"
            )
        result.extend(data[previous_end:start])
        result.extend(replacement)
        previous_end = end
    result.extend(data[previous_end:])
    return bytes(result)


def transform_exact_private_village_utility(
    data: bytes, relative: str, spec: dict[str, Any]
) -> tuple[bytes, dict[str, Any]]:
    before_sha256 = sha256_bytes(data)
    if before_sha256 != spec["before_sha256"]:
        raise ValidationError(
            f"Private utility template SHA-256 differs for {relative}: "
            f"expected {spec['before_sha256']}, got {before_sha256}"
        )
    inspected = inspect_structure_template(data, relative)
    source_state = spec["source_state"]
    source_name = source_state["Name"]
    coordinate = tuple(spec["coordinate"])
    source_matches = [
        block for block in inspected["blocks"] if block["state"]["Name"] == source_name
    ]
    if len(source_matches) != 1:
        raise ValidationError(
            f"{relative} must contain exactly one {source_name}, found {len(source_matches)}"
        )
    target = source_matches[0]
    if target["position"] != coordinate:
        raise ValidationError(
            f"{relative} {source_name} shifted: expected {coordinate}, got {target['position']}"
        )
    if target["state"] != source_state:
        raise ValidationError(
            f"{relative} source state differs at {coordinate}: "
            f"expected {source_state}, got {target['state']}"
        )
    blocks_at_coordinate = [
        block for block in inspected["blocks"] if block["position"] == coordinate
    ]
    if blocks_at_coordinate != [target]:
        raise ValidationError(f"{relative} has a duplicate or ambiguous target at {coordinate}")

    expected_block_entity_id = spec["block_entity_id"]
    if target["block_entity_id"] != expected_block_entity_id:
        raise ValidationError(
            f"{relative} block entity differs at {coordinate}: "
            f"expected {expected_block_entity_id!r}, got {target['block_entity_id']!r}"
        )
    if expected_block_entity_id is None and target["nbt_range"] is not None:
        raise ValidationError(f"{relative} unexpectedly has block-entity NBT at {coordinate}")
    if expected_block_entity_id is not None and target["nbt_range"] is None:
        raise ValidationError(f"{relative} lacks required block-entity NBT at {coordinate}")

    replacement_state = spec["replacement_state"]
    replacement_indices = [
        index for index, state in enumerate(inspected["palette"]) if state == replacement_state
    ]
    if len(replacement_indices) > 1:
        raise ValidationError(
            f"{relative} has duplicate exact replacement palette states: {replacement_indices}"
        )
    patches: list[tuple[int, int, bytes, str]] = []
    palette_appended = not replacement_indices
    if replacement_indices:
        replacement_index = replacement_indices[0]
    else:
        replacement_index = len(inspected["palette"])
        palette_detail = inspected["scanner"].list_details.get(("palette",))
        if palette_detail is None:
            raise ValidationError(f"{relative} lacks palette byte-range metadata")
        patches.extend(
            [
                (
                    palette_detail["length_start"],
                    palette_detail["length_start"] + 4,
                    struct.pack(">i", replacement_index + 1),
                    "palette length",
                ),
                (
                    palette_detail["items_end"],
                    palette_detail["items_end"],
                    _encode_palette_state_payload(replacement_state),
                    "appended palette state",
                ),
            ]
        )
    state_start, state_end = target["state_payload_range"]
    patches.append(
        (state_start, state_end, struct.pack(">i", replacement_index), "target state index")
    )
    if target["nbt_range"] is not None:
        nbt_start, nbt_end = target["nbt_range"]
        patches.append((nbt_start, nbt_end, b"", "target block-entity NBT removal"))

    transformed_decoded = _apply_exact_byte_patches(
        inspected["decoded"], patches, relative
    )
    transformed = deterministic_gzip(transformed_decoded)
    if transformed != deterministic_gzip(transformed_decoded):
        raise AssertionError("Deterministic NBT encoder produced inconsistent bytes")
    after_sha256 = sha256_bytes(transformed)
    if after_sha256 != spec["after_sha256"]:
        raise ValidationError(
            f"Private utility template output SHA-256 differs for {relative}: "
            f"expected {spec['after_sha256']}, got {after_sha256}"
        )

    after_inspected = inspect_structure_template(transformed, f"transformed {relative}")
    after_by_position = {block["position"]: block for block in after_inspected["blocks"]}
    if set(after_by_position) != {block["position"] for block in inspected["blocks"]}:
        raise ValidationError(f"{relative} block coordinates changed during utility transform")
    after_target = after_by_position[coordinate]
    if after_target["state"] != replacement_state or after_target["nbt_range"] is not None:
        raise ValidationError(
            f"{relative} target did not become clean {replacement_state} at {coordinate}"
        )
    before_by_position = {block["position"]: block for block in inspected["blocks"]}
    for position, after_block in after_by_position.items():
        if position == coordinate:
            continue
        before_block = before_by_position[position]
        before_start, before_end = before_block["payload_range"]
        after_start, after_end = after_block["payload_range"]
        if (
            before_block["state"] != after_block["state"]
            or inspected["decoded"][before_start:before_end]
            != after_inspected["decoded"][after_start:after_end]
        ):
            raise ValidationError(
                f"{relative} changed unrelated block record at {position}"
            )

    return transformed, {
        "template": relative,
        "coordinate": list(coordinate),
        "source_state": source_state,
        "replacement_state": replacement_state,
        "removed_block_entity_id": expected_block_entity_id,
        "removed_block_entity_nbt": target["nbt_range"] is not None,
        "processor_generated_brewing_contents_eliminated": source_name
        == "minecraft:brewing_stand",
        "replacement_palette_index": replacement_index,
        "replacement_palette_state_appended": palette_appended,
        "before_sha256": before_sha256,
        "after_sha256": after_sha256,
        "non_target_block_records_byte_identical": True,
    }


def inspect_restored_private_village_utility(
    data: bytes, relative: str, spec: dict[str, Any]
) -> dict[str, Any]:
    actual_hash = sha256_bytes(data)
    if len(data) != spec["size"] or actual_hash != spec["sha256"]:
        raise ValidationError(
            f"Restored private utility template identity differs for {relative}: expected "
            f"{spec['size']} bytes/{spec['sha256']}, got {len(data)}/{actual_hash}"
        )
    inspected = inspect_structure_template(data, relative)
    coordinate = tuple(spec["coordinate"])
    targets = [
        block for block in inspected["blocks"] if block["position"] == coordinate
    ]
    if len(targets) != 1:
        raise ValidationError(
            f"{relative} must contain exactly one restored target at {coordinate}"
        )
    target = targets[0]
    if target["state"] != spec["state"]:
        raise ValidationError(
            f"{relative} restored state differs at {coordinate}: "
            f"expected {spec['state']}, got {target['state']}"
        )
    if target["block_entity_id"] != spec["block_entity_id"] or target["nbt_range"] is None:
        raise ValidationError(
            f"{relative} restored block entity differs at {coordinate}: "
            f"expected {spec['block_entity_id']!r}, got {target['block_entity_id']!r}"
        )
    nbt_start, nbt_end = target["nbt_range"]
    nbt_hash = sha256_bytes(inspected["decoded"][nbt_start:nbt_end])
    if nbt_hash != spec["block_entity_nbt_sha256"]:
        raise ValidationError(
            f"{relative} restored block-entity NBT differs at {coordinate}: "
            f"expected {spec['block_entity_nbt_sha256']}, got {nbt_hash}"
        )
    return {
        "template": relative,
        "coordinate": list(coordinate),
        "state": spec["state"],
        "block_entity_id": spec["block_entity_id"],
        "block_entity_nbt_sha256": nbt_hash,
        "before_sha256": actual_hash,
        "after_sha256": actual_hash,
        "compressed_template_byte_identical_to_pristine": True,
        "block_entity_nbt_byte_identical_to_pristine": True,
    }


def _private_village_structure_inventory(payloads: dict[str, bytes]) -> dict[str, Any]:
    block_counts: Counter[str] = Counter()
    block_entity_counts: Counter[str] = Counter()
    loot_bindings: Counter[str] = Counter()
    inspected: dict[str, dict[str, Any]] = {}
    for relative in sorted(payloads):
        template = inspect_structure_template(payloads[relative], relative)
        inspected[relative] = template
        for block in template["blocks"]:
            block_counts[block["state"]["Name"]] += 1
            if block["block_entity_id"] is not None:
                block_entity_counts[block["block_entity_id"]] += 1
            if block["loot_table"] is not None:
                loot_bindings[block["loot_table"]] += 1
    return {
        "block_counts": block_counts,
        "block_entity_counts": block_entity_counts,
        "loot_bindings": loot_bindings,
        "inspected": inspected,
    }


def _require_pinned_inventory_counts(
    actual: Counter[str], expected: dict[str, int], category: str
) -> None:
    differences = {
        name: {"expected": count, "actual": actual[name]}
        for name, count in expected.items()
        if actual[name] != count
    }
    if differences:
        raise ValidationError(f"Private village {category} counts differ: {differences}")


def _private_village_template_tree_hash(payloads: dict[str, bytes]) -> str:
    digest = hashlib.sha256()
    for relative in sorted(payloads):
        encoded_name = relative.encode("utf-8")
        digest.update(struct.pack(">I", len(encoded_name)))
        digest.update(encoded_name)
        digest.update(struct.pack(">Q", len(payloads[relative])))
        digest.update(payloads[relative])
    return digest.hexdigest()


def transform_private_village_utilities(root: Path) -> dict[str, Any]:
    structure_root = root / "data/ribbits/structure"
    paths = sorted(structure_root.rglob("*.nbt"))
    payloads = {path.relative_to(root).as_posix(): path.read_bytes() for path in paths}
    if len(payloads) != PRIVATE_VILLAGE_TEMPLATE_COUNT:
        raise ValidationError(
            f"Expected {PRIVATE_VILLAGE_TEMPLATE_COUNT} private village templates, "
            f"found {len(payloads)}"
        )
    expected_targets = set(PRIVATE_VILLAGE_UTILITY_TRANSFORMS)
    missing_targets = expected_targets - set(payloads)
    if missing_targets:
        raise ValidationError(f"Private village utility templates are missing: {sorted(missing_targets)}")
    for relative, spec in PRIVATE_VILLAGE_UTILITY_TRANSFORMS.items():
        actual_hash = sha256_bytes(payloads[relative])
        if actual_hash != spec["before_sha256"]:
            raise ValidationError(
                f"Private utility template SHA-256 differs for {relative}: "
                f"expected {spec['before_sha256']}, got {actual_hash}"
            )
    restored_records = [
        inspect_restored_private_village_utility(payloads[relative], relative, spec)
        for relative, spec in sorted(PRIVATE_VILLAGE_RESTORED_UTILITY_SPECS.items())
    ]

    before = _private_village_structure_inventory(payloads)
    _require_pinned_inventory_counts(
        before["block_counts"], PRIVATE_VILLAGE_REMOVED_UTILITY_COUNTS, "removed utility"
    )
    _require_pinned_inventory_counts(
        before["block_counts"], PRIVATE_VILLAGE_PRESERVED_BLOCK_COUNTS, "retained block"
    )
    _require_pinned_inventory_counts(
        before["block_entity_counts"],
        PRIVATE_VILLAGE_PRESERVED_BLOCK_ENTITY_COUNTS,
        "retained block-entity",
    )
    _require_pinned_inventory_counts(
        before["block_counts"],
        PRIVATE_VILLAGE_PROCESSOR_SENTINEL_COUNTS,
        "raw processor sentinel",
    )
    _require_pinned_inventory_counts(
        before["loot_bindings"], PRIVATE_VILLAGE_LOOT_BINDING_COUNTS, "loot binding"
    )

    transformed_payloads = dict(payloads)
    records: list[dict[str, Any]] = []
    for relative in sorted(PRIVATE_VILLAGE_UTILITY_TRANSFORMS):
        transformed, record = transform_exact_private_village_utility(
            payloads[relative], relative, PRIVATE_VILLAGE_UTILITY_TRANSFORMS[relative]
        )
        transformed_again, _record_again = transform_exact_private_village_utility(
            payloads[relative], relative, PRIVATE_VILLAGE_UTILITY_TRANSFORMS[relative]
        )
        if transformed_again != transformed:
            raise AssertionError(f"Nondeterministic private utility transform for {relative}")
        transformed_payloads[relative] = transformed
        records.append(record)

    after = _private_village_structure_inventory(transformed_payloads)
    expected_after_blocks = before["block_counts"].copy()
    for spec in PRIVATE_VILLAGE_UTILITY_TRANSFORMS.values():
        source_name = spec["source_state"]["Name"]
        replacement_name = spec["replacement_state"]["Name"]
        expected_after_blocks[source_name] -= 1
        if expected_after_blocks[source_name] == 0:
            del expected_after_blocks[source_name]
        expected_after_blocks[replacement_name] += 1
    if after["block_counts"] != expected_after_blocks:
        raise ValidationError("Private village block inventory changed outside four exact utilities")

    expected_after_block_entities = before["block_entity_counts"].copy()
    for spec in PRIVATE_VILLAGE_UTILITY_TRANSFORMS.values():
        name = spec["block_entity_id"]
        if name is not None:
            expected_after_block_entities[name] -= 1
            if expected_after_block_entities[name] == 0:
                del expected_after_block_entities[name]
    if after["block_entity_counts"] != expected_after_block_entities:
        raise ValidationError("Private village block-entity inventory changed outside exact removals")
    if after["loot_bindings"] != before["loot_bindings"]:
        raise ValidationError("Private village loot bindings changed during utility transform")
    _require_pinned_inventory_counts(
        after["block_counts"], PRIVATE_VILLAGE_PRESERVED_BLOCK_COUNTS, "retained block"
    )
    _require_pinned_inventory_counts(
        after["block_entity_counts"],
        PRIVATE_VILLAGE_PRESERVED_BLOCK_ENTITY_COUNTS,
        "retained block-entity",
    )
    _require_pinned_inventory_counts(
        after["block_counts"],
        PRIVATE_VILLAGE_PROCESSOR_SENTINEL_COUNTS,
        "raw processor sentinel",
    )
    _require_pinned_inventory_counts(
        after["loot_bindings"], PRIVATE_VILLAGE_LOOT_BINDING_COUNTS, "loot binding"
    )
    for name in PRIVATE_VILLAGE_REMOVED_UTILITY_COUNTS:
        if after["block_counts"][name] != 0:
            raise ValidationError(f"Private village still contains removed utility {name}")
    for relative, spec in PRIVATE_VILLAGE_RESTORED_UTILITY_SPECS.items():
        if transformed_payloads[relative] != payloads[relative]:
            raise ValidationError(
                f"Restored private utility template changed after pristine extraction: {relative}"
            )
        inspect_restored_private_village_utility(
            transformed_payloads[relative], relative, spec
        )

    for relative in sorted(PRIVATE_VILLAGE_UTILITY_TRANSFORMS):
        root.joinpath(*PurePosixPath(relative).parts).write_bytes(transformed_payloads[relative])

    return {
        "count": len(records),
        "policy": "future placements only; exact coordinate and hash-pinned compressed NBT rewrite",
        "canonical_template_count": len(payloads),
        "templates": records,
        "restored_count": len(restored_records),
        "restored_templates": restored_records,
        "template_tree_before_sha256": _private_village_template_tree_hash(payloads),
        "template_tree_after_sha256": _private_village_template_tree_hash(transformed_payloads),
        "removed_utility_counts_before": dict(PRIVATE_VILLAGE_REMOVED_UTILITY_COUNTS),
        "removed_utility_counts_after": {
            name: after["block_counts"][name]
            for name in PRIVATE_VILLAGE_REMOVED_UTILITY_COUNTS
        },
        "retained_block_counts": dict(PRIVATE_VILLAGE_PRESERVED_BLOCK_COUNTS),
        "retained_block_entity_counts": dict(
            PRIVATE_VILLAGE_PRESERVED_BLOCK_ENTITY_COUNTS
        ),
        "raw_processor_sentinel_counts": dict(PRIVATE_VILLAGE_PROCESSOR_SENTINEL_COUNTS),
        "loot_binding_counts": dict(PRIVATE_VILLAGE_LOOT_BINDING_COUNTS),
        "all_non_target_block_counts_unchanged": True,
        "all_non_target_block_records_byte_identical": True,
        "all_loot_bindings_unchanged": True,
        "deterministic_compressed_nbt": True,
    }


def validate_private_village_utility_transform(root: Path, errors: list[str]) -> None:
    try:
        structure_root = root / "data/ribbits/structure"
        paths = sorted(structure_root.rglob("*.nbt"))
        payloads = {path.relative_to(root).as_posix(): path.read_bytes() for path in paths}
        if len(payloads) != PRIVATE_VILLAGE_TEMPLATE_COUNT:
            raise ValidationError(
                f"Expected {PRIVATE_VILLAGE_TEMPLATE_COUNT} private village templates after "
                f"assembly, found {len(payloads)}"
            )
        if not set(PRIVATE_VILLAGE_UTILITY_TRANSFORMS).issubset(payloads):
            raise ValidationError("Private village utility output templates are missing")
        inventory = _private_village_structure_inventory(payloads)
        for relative, spec in PRIVATE_VILLAGE_UTILITY_TRANSFORMS.items():
            actual_hash = sha256_bytes(payloads[relative])
            if actual_hash != spec["after_sha256"]:
                raise ValidationError(
                    f"Private utility output SHA-256 differs for {relative}: "
                    f"expected {spec['after_sha256']}, got {actual_hash}"
                )
            expected_coordinate = tuple(spec["coordinate"])
            target = [
                block
                for block in inventory["inspected"][relative]["blocks"]
                if block["position"] == expected_coordinate
            ]
            if (
                len(target) != 1
                or target[0]["state"] != spec["replacement_state"]
                or target[0]["nbt_range"] is not None
            ):
                raise ValidationError(
                    f"Private utility output target differs in {relative} at {expected_coordinate}"
                )
        for relative, spec in PRIVATE_VILLAGE_RESTORED_UTILITY_SPECS.items():
            if relative not in payloads:
                raise ValidationError(
                    f"Restored private village utility template is missing: {relative}"
                )
            inspect_restored_private_village_utility(payloads[relative], relative, spec)
        for name in PRIVATE_VILLAGE_REMOVED_UTILITY_COUNTS:
            if inventory["block_counts"][name] != 0:
                raise ValidationError(f"Private village still contains removed utility {name}")
        _require_pinned_inventory_counts(
            inventory["block_counts"],
            PRIVATE_VILLAGE_PRESERVED_BLOCK_COUNTS,
            "retained block",
        )
        _require_pinned_inventory_counts(
            inventory["block_entity_counts"],
            PRIVATE_VILLAGE_PRESERVED_BLOCK_ENTITY_COUNTS,
            "retained block-entity",
        )
        _require_pinned_inventory_counts(
            inventory["block_counts"],
            PRIVATE_VILLAGE_PROCESSOR_SENTINEL_COUNTS,
            "raw processor sentinel",
        )
        _require_pinned_inventory_counts(
            inventory["loot_bindings"],
            PRIVATE_VILLAGE_LOOT_BINDING_COUNTS,
            "loot binding",
        )
        for removed_block_entity in ("minecraft:brewing_stand",):
            if inventory["block_entity_counts"][removed_block_entity] != 0:
                raise ValidationError(
                    f"Private village still contains block-entity NBT for {removed_block_entity}"
                )
    except (KeyError, OSError, ValueError, ValidationError) as exc:
        errors.append(f"Private village utility transform is invalid: {exc}")


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
    if "\\" in name or re.match(r"^[A-Za-z]:", name):
        raise ValidationError(f"Unsafe ZIP entry: {name!r}")
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


def require_descendant_path(path: Path, root: Path, label: str) -> None:
    candidate = path.resolve()
    resolved_root = root.resolve()
    try:
        relative = candidate.relative_to(resolved_root)
    except ValueError as exc:
        raise ValidationError(
            f"{label} must stay inside {resolved_root}: {candidate}"
        ) from exc
    if not relative.parts:
        raise ValidationError(f"{label} must not be the root itself: {candidate}")


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


def write_manifest_after_donor_verification(
    manifest_path: Path,
    manifest: dict[str, Any],
    donor_checks: list[tuple[Path, dict[str, Any], str]],
) -> None:
    if manifest_path.exists():
        raise ValidationError(f"Refusing to overwrite existing manifest: {manifest_path}")
    for path, identity, label in donor_checks:
        require_donor_unchanged(path, identity, label)
    manifest_path.parent.mkdir(parents=True, exist_ok=True)
    descriptor, temporary_name = tempfile.mkstemp(
        dir=manifest_path.parent,
        prefix=f".{manifest_path.name}.",
        suffix=".tmp",
    )
    os.close(descriptor)
    temporary_path = Path(temporary_name)
    try:
        write_json(temporary_path, manifest)
        for path, identity, label in donor_checks:
            require_donor_unchanged(path, identity, label)
        if manifest_path.exists():
            raise ValidationError(f"Refusing to overwrite existing manifest: {manifest_path}")
        temporary_path.rename(manifest_path)
    finally:
        temporary_path.unlink(missing_ok=True)


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
            nitwit_key = "item.ribbits.ribbit_nitwit_spawn_egg"
            if values.get(nitwit_key) != "Nitwit Ribbit Spawn Egg":
                raise ValidationError("Pristine Nitwit spawn-egg translation differs")
            new_keys = set(EN_US_MYNX_PROFESSION_TRANSLATIONS) - {nitwit_key}
            overlap = set(values).intersection(new_keys)
            if overlap:
                raise ValidationError(
                    f"Unexpected pre-existing Mynx profession translations: {sorted(overlap)}"
                )
            values.update(EN_US_MYNX_PROFESSION_TRANSLATIONS)

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


def import_donor_profession_resources(
    root: Path,
    donor_members: dict[str, dict[str, bytes]],
    donor_identities: dict[str, dict[str, Any]],
) -> list[dict[str, Any]]:
    shared_texture_path = root / PurePosixPath(SHARED_RIBBIT_TEXTURE_ENTRY)
    shared_texture = shared_texture_path.read_bytes()
    if png_dimensions(shared_texture, SHARED_RIBBIT_TEXTURE_ENTRY) != (128, 128):
        raise ValidationError("Pristine Ribbits shared entity texture must be exactly 128x128")

    records: list[dict[str, Any]] = []
    for profession in NEW_PROFESSIONS:
        asset = PROFESSION_DONOR_ASSETS[profession]
        donor_key = asset["donor"]
        model_member = asset["model"]
        texture_member = asset["texture"]
        members = donor_members[donor_key]
        if set(members) != set(DONOR_INPUT_SPECS[donor_key]["members"]):
            raise ValidationError(f"Loaded donor member accounting differs for {donor_key}")

        source_model = load_json_bytes(members[model_member], model_member)
        profession_model = build_profession_model(source_model, model_member)
        model_output = (
            root
            / f"assets/ribbits/geckolib/models/{profession}_ribbit.geo.json"
        )
        if model_output.exists():
            raise ValidationError(f"Refusing to overwrite donor-derived model: {model_output}")
        model_output.parent.mkdir(parents=True, exist_ok=True)
        write_json(model_output, profession_model)
        model_relative = model_output.relative_to(root).as_posix()
        records.append(
            {
                "output": model_relative,
                "sources": [
                    {
                        "archive": donor_identities[donor_key]["filename"],
                        "member": model_member,
                    }
                ],
                "transformation": "schema-preserving GeckoLib relocation; 256x128 atlas dimensions",
            }
        )

        texture_output = root / f"assets/ribbits/textures/entity/{profession}_ribbit.png"
        if texture_output.exists():
            raise ValidationError(f"Refusing to overwrite donor-derived texture: {texture_output}")
        texture_output.parent.mkdir(parents=True, exist_ok=True)
        composite_texture = build_composite_texture(
            members[texture_member], texture_member, shared_texture
        )
        texture_output.write_bytes(composite_texture)
        texture_relative = texture_output.relative_to(root).as_posix()
        records.append(
            {
                "output": texture_relative,
                "sources": [
                    {
                        "archive": donor_identities[donor_key]["filename"],
                        "member": texture_member,
                    },
                    {
                        "archive": "Ribbits-1.21.1-Fabric-4.1.6.jar",
                        "member": SHARED_RIBBIT_TEXTURE_ENTRY,
                    },
                ],
                "transformation": (
                    "deterministic 256x128 RGBA atlas; donor pixels at x=0, "
                    "pristine shared Ribbits pixels at x=128"
                ),
            }
        )

        for variant in UMBRELLA_VARIANTS:
            umbrella_source_relative = (
                f"assets/ribbits/geckolib/models/umbrella/nitwit/"
                f"umbrella_{variant}.geo.json"
            )
            umbrella_source = load_json(root / PurePosixPath(umbrella_source_relative))
            umbrella_model = build_umbrella_composite_model(
                profession_model, umbrella_source, profession, variant
            )
            umbrella_output = (
                root
                / f"assets/ribbits/geckolib/models/umbrella/{profession}/"
                f"umbrella_{variant}.geo.json"
            )
            if umbrella_output.exists():
                raise ValidationError(
                    f"Refusing to overwrite donor-derived umbrella model: {umbrella_output}"
                )
            umbrella_output.parent.mkdir(parents=True, exist_ok=True)
            write_json(umbrella_output, umbrella_model)
            records.append(
                {
                    "output": umbrella_output.relative_to(root).as_posix(),
                    "sources": [
                        {
                            "archive": donor_identities[donor_key]["filename"],
                            "member": model_member,
                        },
                        {
                            "archive": "Ribbits-1.21.1-Fabric-4.1.6.jar",
                            "member": umbrella_source_relative.replace(
                                "assets/ribbits/geckolib/models/", "assets/ribbits/geo/"
                            ),
                        },
                    ],
                    "transformation": (
                        "donor profession geometry plus exact pristine umbrella bone; "
                        "umbrella U coordinates offset by 128"
                    ),
                }
            )

    actual_outputs = {record["output"] for record in records}
    if (
        actual_outputs != PROFESSION_DONOR_DERIVED_OUTPUTS
        or len(records) != len(PROFESSION_DONOR_DERIVED_OUTPUTS)
    ):
        raise ValidationError(
            "Profession donor-derived output accounting differs: "
            f"missing={sorted(PROFESSION_DONOR_DERIVED_OUTPUTS - actual_outputs)}, "
            f"extra={sorted(actual_outputs - PROFESSION_DONOR_DERIVED_OUTPUTS)}"
        )
    for record in records:
        if any(
            source["member"].endswith((".class", ".java"))
            for source in record["sources"]
        ):
            raise ValidationError("Executable donor content entered the approved output record")
    return records


def import_wandering_visual_resources(
    root: Path,
    members: dict[str, bytes],
    identity: dict[str, Any],
    user_chute_model: dict[str, Any],
    user_chute_png: bytes,
) -> tuple[list[dict[str, Any]], list[dict[str, Any]]]:
    expected_members = DONOR_INPUT_SPECS["wandering"]["members"]
    if set(members) != set(expected_members):
        raise ValidationError("Loaded Wandering Ribbit donor member accounting differs")

    archive = identity["filename"]
    model_member = "assets/wandering_ribbit/geo/wandering_ribbit.geo.json"
    texture_member = "assets/wandering_ribbit/textures/entity/wandering_ribbit.png"
    chute_closed_member = "assets/wandering_ribbit/textures/item/umbrella_leaf_item.png"
    records: list[dict[str, Any]] = []

    def write_exact(member: str, relative: str, transformation: str) -> None:
        destination = root / PurePosixPath(relative)
        if destination.exists():
            raise ValidationError(f"Refusing to overwrite Wandering donor output: {destination}")
        destination.parent.mkdir(parents=True, exist_ok=True)
        destination.write_bytes(members[member])
        records.append(
            {
                "output": relative,
                "sources": [{"archive": archive, "member": member}],
                "transformation": transformation,
            }
        )

    write_exact(
        model_member,
        "assets/ribbits/geckolib/models/wandering_ribbit.geo.json",
        "exact approved bytes under the canonical Ribbits GeckoLib model path",
    )
    model_path = root / "assets/ribbits/geckolib/models/wandering_ribbit.geo.json"
    model = load_json(model_path)
    for geometry in model["minecraft:geometry"]:
        bones = geometry["bones"]
        removed = {"umbrella_leaf", "grip", "leaf2"}
        if {bone["name"] for bone in bones} & removed != removed:
            raise ValidationError("Wandering held-leaf bones differ from the approved donor")
        geometry["bones"] = [bone for bone in bones if bone["name"] not in removed]
    write_json(model_path, model)
    records[-1]["transformation"] = "remove only held umbrella_leaf/grip/leaf2 bones; preserve all other geometry"
    write_exact(
        texture_member,
        "assets/ribbits/textures/entity/wandering_ribbit.png",
        "exact approved bytes under the canonical Ribbits entity-texture path",
    )
    write_exact(
        chute_closed_member,
        "assets/ribbits/textures/item/chute_leaf.png",
        "exact approved closed Drop Leaf item sprite bytes",
    )
    replacement = Path(__file__).resolve().parent / "assets/chute_leaf.png"
    replacement_bytes = replacement.read_bytes()
    if hashlib.sha256(replacement_bytes).hexdigest() != "816e4d4edc23542afeb2f2f90a5af8a2076ae61829f1acb016711e05fec0191d":
        raise ValidationError("User Drop Leaf attachment identity differs")
    (root / "assets/ribbits/textures/item/chute_leaf.png").write_bytes(replacement_bytes)
    records[-1]["sources"] = [{"archive": "tracked-project", "member": "tools/assets/chute_leaf.png"}]
    records[-1]["transformation"] = "exact user-supplied 16x16 PNG replacement; no pixel or byte conversion"
    closed_model_relative = "assets/ribbits/models/item/chute_leaf.json"
    closed_model_path = root / PurePosixPath(closed_model_relative)
    if closed_model_path.exists():
        raise ValidationError(f"Refusing to overwrite Wandering donor output: {closed_model_path}")
    closed_model_path.parent.mkdir(parents=True, exist_ok=True)
    write_json(
        closed_model_path,
        {
            "parent": "minecraft:item/generated",
            "textures": {"layer0": "ribbits:item/chute_leaf"},
        },
    )
    records.append(
        {
            "output": closed_model_relative,
            "sources": [{"archive": archive, "member": chute_closed_member}],
            "transformation": "Minecraft 26.2 generated-item model bound to the exact closed sprite",
        }
    )

    # Preserve the earlier back sprite/model; the inventory receives its own exact asset.
    closed_back = "assets/ribbits/models/item/chute_leaf_closed.json"
    write_json(root / closed_back, load_json(closed_model_path))
    closed_item = "assets/ribbits/items/chute_leaf_closed.json"
    (root / closed_item).parent.mkdir(parents=True, exist_ok=True)
    write_json(root / closed_item, {"model": {"type": "minecraft:model", "model": "ribbits:item/chute_leaf_closed"}})
    inventory_relative = "assets/ribbits/textures/item/drop_leaf_inventory.png"
    inventory_bytes = (Path(__file__).resolve().parent / "assets/drop_leaf_inventory.png").read_bytes()
    if sha256_bytes(inventory_bytes) != "5acbe4afc118b2ec1a04ec5a2dcfd91f7db05bd61937019a75300756cc257a30":
        raise ValidationError("User Drop Leaf inventory attachment identity differs")
    (root / inventory_relative).write_bytes(inventory_bytes)
    write_json(closed_model_path, {"parent": "minecraft:item/generated", "textures": {"layer0": "ribbits:item/drop_leaf_inventory"}})
    records[-1]["sources"] = [{"archive": "tracked-project", "member": "tools/assets/drop_leaf_inventory.png"}]
    records[-1]["transformation"] = "generated inventory model bound to the exact latest user sprite"
    for relative in (closed_back, closed_item, inventory_relative):
        records.append({"output": relative, "sources": [{"archive": "tracked-project", "member": "tools/assets/" + ("drop_leaf_inventory.png" if relative == inventory_relative else "chute_leaf.png")}], "transformation": "separate exact inventory artwork from preserved closed-back presentation"})

    # C18 intentionally does not consult donor umbrella geometry or texture for
    # the deployed leaf. Convert the supplied Blockbench cubes directly to Java
    # item-model elements; the supplied model has no rotations that Java's single-
    # axis element rotation cannot faithfully represent.
    source_elements = user_chute_model.get("elements")
    if not isinstance(source_elements, list) or not source_elements:
        raise ValidationError("C18 user-authored Chute BBModel has no elements")
    open_elements: list[dict[str, Any]] = []
    for index, element in enumerate(source_elements):
        if not isinstance(element, dict) or element.get("type") != "cube" or element.get("export") is False:
            raise ValidationError(f"C18 user-authored Chute element {index} is not an exported cube")
        bounds = (element.get("from"), element.get("to"))
        if any(not isinstance(value, list) or len(value) != 3 for value in bounds):
            raise ValidationError(f"C18 user-authored Chute element {index} has invalid bounds")
        faces = element.get("faces")
        if not isinstance(faces, dict) or not faces:
            raise ValidationError(f"C18 user-authored Chute element {index} has no faces")
        converted_faces: dict[str, Any] = {}
        for direction, face in faces.items():
            if direction not in {"north", "east", "south", "west", "up", "down"} or not isinstance(face, dict):
                raise ValidationError(f"C18 user-authored Chute element {index} has invalid face")
            uv = face.get("uv")
            if not isinstance(uv, list) or len(uv) != 4:
                raise ValidationError(f"C18 user-authored Chute element {index} has invalid face UV")
            converted_faces[direction] = {"uv": [float(value) for value in uv], "texture": "#0"}
        converted: dict[str, Any] = {
            "from": [float(value) for value in element["from"]],
            "to": [float(value) for value in element["to"]],
            "faces": converted_faces,
        }
        if element.get("shade") is False:
            converted["shade"] = False
        rotation = element.get("rotation")
        if rotation is not None:
            if not isinstance(rotation, list) or len(rotation) != 3 or sum(value != 0 for value in rotation) > 1:
                raise ValidationError("C18 BBModel uses a rotation Java item models cannot represent faithfully")
            if any(rotation):
                axis = ("x", "y", "z")[next(index for index, value in enumerate(rotation) if value != 0)]
                origin = element.get("origin")
                if not isinstance(origin, list) or len(origin) != 3:
                    raise ValidationError("C18 BBModel rotated cube lacks a valid pivot")
                converted["rotation"] = {"origin": [float(value) for value in origin], "axis": axis,
                                         "angle": float(next(value for value in rotation if value != 0)),
                                         "rescale": bool(element.get("rescale", False))}
        open_elements.append(converted)
    display = user_chute_model.get("display")
    if not isinstance(display, dict):
        raise ValidationError("C18 user-authored Chute BBModel has no display transforms")
    open_model = {
        "ambientocclusion": bool(user_chute_model.get("ambientocclusion", True)),
        "textures": {"0": "ribbits:item/chute_leaf_open", "particle": "ribbits:item/chute_leaf_open"},
        "elements": open_elements,
        "display": copy.deepcopy(display),
    }
    open_model_relative = "assets/ribbits/models/item/chute_leaf_open.json"
    open_model_path = root / PurePosixPath(open_model_relative)
    if open_model_path.exists():
        raise ValidationError(f"Refusing to overwrite Wandering donor output: {open_model_path}")
    write_json(open_model_path, open_model)
    user_records = [
        {
            "output": open_model_relative,
            "sources": [{"logical_path": "originals/assets/chute_leaf_open.bbmodel"}],
            "transformation": "deterministic direct Blockbench cube/UV/display conversion; no donor geometry",
        }
    ]
    open_texture_relative = "assets/ribbits/textures/item/chute_leaf_open.png"
    (root / open_texture_relative).parent.mkdir(parents=True, exist_ok=True)
    (root / open_texture_relative).write_bytes(user_chute_png)
    user_records.append({
        "output": open_texture_relative,
        "sources": [{"logical_path": "originals/assets/chute_leaf_open.png"}],
        "transformation": "exact standalone user-authored PNG bytes; no pixel or byte conversion",
    })

    open_item_relative = "assets/ribbits/items/chute_leaf_open.json"
    open_item_path = root / PurePosixPath(open_item_relative)
    if open_item_path.exists():
        raise ValidationError(f"Refusing to overwrite Wandering donor output: {open_item_path}")
    open_item_path.parent.mkdir(parents=True, exist_ok=True)
    write_json(
        open_item_path,
        {"model": {"type": "minecraft:model", "model": "ribbits:item/chute_leaf_open"}},
    )
    user_records.append(
        {
            "output": open_item_relative,
            "sources": [{"logical_path": "originals/assets/chute_leaf_open.bbmodel"}],
            "transformation": "Minecraft 26.2 item-definition bridge for the user-authored open Chute model",
        }
    )

    actual_outputs = {record["output"] for record in records}
    if (
        actual_outputs != WANDERING_DONOR_DERIVED_OUTPUTS
        or len(records) != len(WANDERING_DONOR_DERIVED_OUTPUTS)
    ):
        raise ValidationError(
            "Wandering donor-derived output accounting differs: "
            f"missing={sorted(WANDERING_DONOR_DERIVED_OUTPUTS - actual_outputs)}, "
            f"extra={sorted(actual_outputs - WANDERING_DONOR_DERIVED_OUTPUTS)}"
        )
    if {record["output"] for record in user_records} != USER_AUTHORED_CHUTE_OUTPUTS:
        raise ValidationError("C18 user-authored Chute output accounting differs")
    return records, user_records


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


def require_loot_pool_shape(loot: Any, relative: str, empty_type: str) -> None:
    if not isinstance(loot, dict) or set(loot) != {"type", "pools"}:
        raise ValidationError(f"Unexpected top-level loot-table fields in {relative}")
    if loot.get("type") != "minecraft:chest" or not isinstance(loot.get("pools"), list):
        raise ValidationError(f"{relative} must remain a chest loot table")
    expected_counts = LOOT_POOL_ENTRY_COUNTS[relative]
    if len(loot["pools"]) != len(expected_counts):
        raise ValidationError(f"{relative} pool count differs")
    for index, (pool, expected_count) in enumerate(zip(loot["pools"], expected_counts)):
        if not isinstance(pool, dict) or set(pool) != {"rolls", "bonus_rolls", "entries"}:
            raise ValidationError(f"Unexpected pool fields in {relative} pool {index}")
        if pool.get("bonus_rolls") != 0.0 or not isinstance(pool.get("entries"), list):
            raise ValidationError(f"Unexpected pool structure in {relative} pool {index}")
        if len(pool["entries"]) != expected_count:
            raise ValidationError(f"{relative} pool {index} entry count differs")
    special = loot["pools"][1]["entries"][0]
    expected_special = {"type": empty_type, "weight": LOOT_EMPTY_WEIGHTS[relative]}
    if empty_type == "minecraft:item":
        expected_special["name"] = "minecraft:air"
    if special != expected_special:
        raise ValidationError(
            f"{relative} empty-chance entry differs: expected {expected_special}, got {special}"
        )


def repair_air_loot_entry(loot: Any, relative: str) -> dict[str, Any]:
    result = copy.deepcopy(loot)
    require_loot_pool_shape(result, relative, "minecraft:item")
    special = result["pools"][1]["entries"][0]
    if special.get("name") != "minecraft:air":
        raise ValidationError(f"{relative} must contain the exact legacy minecraft:air entry")
    air_entries = [
        entry
        for pool in result["pools"]
        for entry in pool["entries"]
        if isinstance(entry, dict)
        and entry.get("type") == "minecraft:item"
        and entry.get("name") == "minecraft:air"
    ]
    if len(air_entries) != 1:
        raise ValidationError(f"{relative} must contain exactly one legacy minecraft:air item entry")
    special["type"] = "minecraft:empty"
    del special["name"]
    require_loot_pool_shape(result, relative, "minecraft:empty")
    return result


def require_sorcerer_removed_loot_state(loot: Any, relative: str) -> None:
    require_loot_pool_shape(loot, relative, "minecraft:empty")
    for spec in SORCERER_REMOVED_LOOT_ENTRY_SPECS:
        try:
            actual = loot["pools"][spec["pool"]]["entries"][spec["entry"]]
        except (IndexError, KeyError, TypeError) as exc:
            raise ValidationError(
                f"Pinned removed Sorcerer-loot entry is absent in {relative}"
            ) from exc
        if actual != spec["replacement"]:
            raise ValidationError(
                f"Removed Sorcerer-loot entry differs in {relative} at "
                f"pool {spec['pool']} entry {spec['entry']}: expected "
                f"{spec['replacement']}, got {actual}"
            )

    remaining_forbidden = sorted(
        entry.get("name")
        for entry in iter_loot_entries(loot)
        if entry.get("type") == "minecraft:item"
        and entry.get("name") in SORCERER_FORBIDDEN_LOOT_ITEM_IDS
    )
    if remaining_forbidden:
        raise ValidationError(
            f"Forbidden bottle or potion items remain in {relative}: {remaining_forbidden}"
        )
    serialized = json.dumps(loot, separators=(",", ":"))
    if "minecraft:set_potion" in serialized or "potion_contents" in serialized:
        raise ValidationError(f"Potion-specific loot functions remain in {relative}")


def require_exact_sorcerer_loot_replacement(
    before: Any, after: Any, relative: str
) -> None:
    expected = copy.deepcopy(before)
    for spec in SORCERER_REMOVED_LOOT_ENTRY_SPECS:
        expected["pools"][spec["pool"]]["entries"][spec["entry"]] = copy.deepcopy(
            spec["replacement"]
        )
    if after != expected:
        raise ValidationError(
            f"Sorcerer loot replacement changed data outside the two exact entries in {relative}"
        )


def replace_sorcerer_bottle_and_potion_loot(
    loot: Any, relative: str
) -> tuple[dict[str, Any], list[dict[str, Any]]]:
    result = copy.deepcopy(loot)
    require_loot_pool_shape(result, relative, "minecraft:empty")
    source_forbidden = [
        entry.get("name")
        for entry in iter_loot_entries(result)
        if entry.get("type") == "minecraft:item"
        and entry.get("name") in SORCERER_FORBIDDEN_LOOT_ITEM_IDS
    ]
    expected_forbidden = [spec["source"]["name"] for spec in SORCERER_REMOVED_LOOT_ENTRY_SPECS]
    if sorted(source_forbidden) != sorted(expected_forbidden):
        raise ValidationError(
            f"Sorcerer bottle/potion source inventory differs in {relative}: expected "
            f"{sorted(expected_forbidden)}, got {sorted(source_forbidden)}"
        )

    before = copy.deepcopy(result)
    records: list[dict[str, Any]] = []
    for spec in SORCERER_REMOVED_LOOT_ENTRY_SPECS:
        pool_index = spec["pool"]
        entry_index = spec["entry"]
        try:
            actual = result["pools"][pool_index]["entries"][entry_index]
        except (IndexError, KeyError, TypeError) as exc:
            raise ValidationError(
                f"Pinned Sorcerer bottle/potion entry is absent in {relative}"
            ) from exc
        if actual != spec["source"]:
            raise ValidationError(
                f"Pinned Sorcerer bottle/potion entry differs in {relative} at "
                f"pool {pool_index} entry {entry_index}: expected {spec['source']}, got {actual}"
            )
        replacement = copy.deepcopy(spec["replacement"])
        result["pools"][pool_index]["entries"][entry_index] = replacement
        records.append(
            {
                "table": relative,
                "pool": pool_index,
                "entry": entry_index,
                "source_item": spec["source"]["name"],
                "weight": spec["source"]["weight"],
                "removed_functions": copy.deepcopy(spec["source"].get("functions", [])),
                "replacement": replacement,
                "unrelated_entry_data_unchanged": True,
            }
        )

    require_exact_sorcerer_loot_replacement(before, result, relative)
    require_sorcerer_removed_loot_state(result, relative)
    return result, records


def require_village_chest_loot_inventory(root: Path) -> None:
    chest_root = root / "data/ribbits/loot_table/chests"
    if not chest_root.is_dir():
        raise ValidationError(f"Missing private village chest loot directory: {chest_root}")
    actual = {
        f"data/ribbits/loot_table/chests/{relative}"
        for relative in relative_files(chest_root)
    }
    expected = set(VILLAGE_CHEST_LOOT_TABLE_PATHS)
    if actual != expected:
        raise ValidationError(
            "Private village chest loot-table inventory differs: "
            f"missing={sorted(expected - actual)}, unexpected={sorted(actual - expected)}"
        )


def require_village_loot_shape(loot: Any, relative: str) -> None:
    if not isinstance(loot, dict) or set(loot) != {"type", "pools"}:
        raise ValidationError(f"Unexpected top-level loot-table fields in {relative}")
    if loot.get("type") != "minecraft:chest" or not isinstance(loot.get("pools"), list):
        raise ValidationError(f"{relative} must remain a chest loot table")
    expected_counts = VILLAGE_LOOT_POOL_ENTRY_COUNTS[relative]
    if len(loot["pools"]) != len(expected_counts):
        raise ValidationError(f"{relative} pool count differs")
    for index, (pool, expected_count) in enumerate(zip(loot["pools"], expected_counts)):
        if not isinstance(pool, dict) or set(pool) != {"rolls", "bonus_rolls", "entries"}:
            raise ValidationError(f"Unexpected pool fields in {relative} pool {index}")
        if pool.get("bonus_rolls") != 0.0 or not isinstance(pool.get("entries"), list):
            raise ValidationError(f"Unexpected pool structure in {relative} pool {index}")
        if len(pool["entries"]) != expected_count:
            raise ValidationError(f"{relative} pool {index} entry count differs")


def expected_glowcap_currency_entry(spec: dict[str, Any], item_id: str) -> dict[str, Any]:
    return {
        "type": "minecraft:item",
        "weight": spec["weight"],
        "functions": [
            {
                "function": "minecraft:set_count",
                "count": {
                    "type": "minecraft:uniform",
                    "min": spec["count_min"],
                    "max": spec["count_max"],
                },
                "add": False,
            }
        ],
        "name": item_id,
    }


def require_glowcap_currency_state(loot: Any, relative: str, item_id: str) -> None:
    require_village_loot_shape(loot, relative)
    if item_id not in {GLOWCAP_CURRENCY_SOURCE_ID, GLOWCAP_CURRENCY_TARGET_ID}:
        raise ValidationError(f"Unsupported private currency state: {item_id}")

    spec = GLOWCAP_CURRENCY_ENTRY_SPECS.get(relative)
    item_entries = [
        entry for entry in iter_loot_entries(loot) if entry.get("type") == "minecraft:item"
    ]
    source_entries = [
        entry for entry in item_entries if entry.get("name") == GLOWCAP_CURRENCY_SOURCE_ID
    ]
    target_entries = [
        entry for entry in item_entries if entry.get("name") == GLOWCAP_CURRENCY_TARGET_ID
    ]
    expected_count = 1 if spec is not None else 0
    active_entries = source_entries if item_id == GLOWCAP_CURRENCY_SOURCE_ID else target_entries
    inactive_entries = target_entries if item_id == GLOWCAP_CURRENCY_SOURCE_ID else source_entries
    if len(active_entries) != expected_count or inactive_entries:
        raise ValidationError(
            f"{relative} has an unexpected private currency inventory: "
            f"source={len(source_entries)}, target={len(target_entries)}, "
            f"expected_active={expected_count}"
        )
    if spec is not None:
        try:
            actual = loot["pools"][spec["pool"]]["entries"][spec["entry"]]
        except (IndexError, KeyError, TypeError) as exc:
            raise ValidationError(f"Pinned private currency location is absent in {relative}") from exc
        expected = expected_glowcap_currency_entry(spec, item_id)
        if actual != expected:
            raise ValidationError(
                f"Pinned private currency entry differs in {relative}: "
                f"expected {expected}, got {actual}"
            )

    if relative == UNRELATED_AMETHYST_BLOCK_SPEC["table"]:
        block_spec = UNRELATED_AMETHYST_BLOCK_SPEC
        try:
            actual_block = loot["pools"][block_spec["pool"]]["entries"][block_spec["entry"]]
        except (IndexError, KeyError, TypeError) as exc:
            raise ValidationError(
                f"Pinned unrelated amethyst-block entry is absent in {relative}"
            ) from exc
        if actual_block != block_spec["value"]:
            raise ValidationError(
                f"Unrelated amethyst-block entry differs in {relative}: {actual_block}"
            )


def replace_private_glowcap_currency_entries(root: Path) -> dict[str, Any]:
    """Replace only the five pinned village-currency entries, preserving all other bytes."""
    require_village_chest_loot_inventory(root)
    source_token = json.dumps(GLOWCAP_CURRENCY_SOURCE_ID).encode("utf-8")
    target_token = json.dumps(GLOWCAP_CURRENCY_TARGET_ID).encode("utf-8")
    planned: list[tuple[Path, bytes]] = []
    records: list[dict[str, Any]] = []

    # Validate and compute every output before writing any file. A drifted private input
    # therefore fails closed without leaving a partially substituted tree.
    for relative in VILLAGE_CHEST_LOOT_TABLE_PATHS:
        path = root / PurePosixPath(relative)
        before_bytes = path.read_bytes()
        try:
            before = json.loads(before_bytes.decode("utf-8"))
        except (UnicodeDecodeError, json.JSONDecodeError) as exc:
            raise ValidationError(f"Invalid JSON {path}: {exc}") from exc
        require_glowcap_currency_state(before, relative, GLOWCAP_CURRENCY_SOURCE_ID)

        expected_count = 1 if relative in GLOWCAP_CURRENCY_ENTRY_SPECS else 0
        if before_bytes.count(source_token) != expected_count or target_token in before_bytes:
            raise ValidationError(
                f"{relative} does not contain the exact pinned serialized currency token count"
            )
        after_bytes = before_bytes.replace(source_token, target_token)
        try:
            after = json.loads(after_bytes.decode("utf-8"))
        except (UnicodeDecodeError, json.JSONDecodeError) as exc:  # pragma: no cover - replacement is ASCII
            raise ValidationError(f"Currency substitution produced invalid JSON {path}: {exc}") from exc
        require_glowcap_currency_state(after, relative, GLOWCAP_CURRENCY_TARGET_ID)

        expected_after = copy.deepcopy(before)
        if expected_count:
            spec = GLOWCAP_CURRENCY_ENTRY_SPECS[relative]
            expected_after["pools"][spec["pool"]]["entries"][spec["entry"]]["name"] = (
                GLOWCAP_CURRENCY_TARGET_ID
            )
        if after != expected_after:
            raise ValidationError(f"Currency substitution changed unrelated loot data in {relative}")

        planned.append((path, after_bytes))
        if expected_count:
            records.append(
                {
                    "table": relative,
                    "pool": spec["pool"],
                    "entry": spec["entry"],
                    "weight": spec["weight"],
                    "count": {
                        "type": "minecraft:uniform",
                        "min": spec["count_min"],
                        "max": spec["count_max"],
                    },
                    "add": False,
                    "before_sha256": sha256_bytes(before_bytes),
                    "after_sha256": sha256_bytes(after_bytes),
                }
            )

    for path, after_bytes in planned:
        if path.read_bytes() != after_bytes:
            path.write_bytes(after_bytes)

    return {
        "tables_inspected": list(VILLAGE_CHEST_LOOT_TABLE_PATHS),
        "source_item": GLOWCAP_CURRENCY_SOURCE_ID,
        "replacement_item": GLOWCAP_CURRENCY_TARGET_ID,
        "entries_replaced": len(records),
        "targets": records,
        "unrelated_amethyst_preserved": {
            "table": UNRELATED_AMETHYST_BLOCK_SPEC["table"],
            "pool": UNRELATED_AMETHYST_BLOCK_SPEC["pool"],
            "entry": UNRELATED_AMETHYST_BLOCK_SPEC["entry"],
            "item": "minecraft:amethyst_block",
        },
    }


def migrate_private_loot_tables(root: Path) -> dict[str, Any]:
    migrated: dict[str, Any] = {}
    for relative in LOOT_TABLE_PATHS:
        path = root / PurePosixPath(relative)
        migrated[relative] = repair_air_loot_entry(load_json(path), relative)

    sorcerer_relative = LOOT_TABLE_PATHS[1]
    migrated[sorcerer_relative], removed_entries = (
        replace_sorcerer_bottle_and_potion_loot(
            migrated[sorcerer_relative], sorcerer_relative
        )
    )
    for relative, value in migrated.items():
        write_json(root / PurePosixPath(relative), value)
    currency_migration = replace_private_glowcap_currency_entries(root)
    sorcerer_bytes = root.joinpath(*PurePosixPath(sorcerer_relative).parts).read_bytes()
    return {
        "tables": list(LOOT_TABLE_PATHS),
        "air_item_entries_replaced_with_empty": len(LOOT_TABLE_PATHS),
        "replacement": {
            "invalid_item_id": "minecraft:air",
            "faithful_entry_type": "minecraft:empty",
        },
        "sorcerer_bottle_and_potion_entries_replaced_with_empty": len(removed_entries),
        "sorcerer_bottle_and_potion_targets": removed_entries,
        "all_unrelated_sorcerer_loot_unchanged": True,
        "sorcerer_output": {
            "path": sorcerer_relative,
            "size": len(sorcerer_bytes),
            "sha256": sha256_bytes(sorcerer_bytes),
        },
        "glowcap_currency_substitution": currency_migration,
    }


def iter_loot_entries(value: Any) -> list[dict[str, Any]]:
    entries: list[dict[str, Any]] = []
    if isinstance(value, dict):
        if isinstance(value.get("type"), str) and value["type"].startswith("minecraft:"):
            if value["type"] in {
                "minecraft:item",
                "minecraft:empty",
                "minecraft:alternatives",
                "minecraft:group",
                "minecraft:sequence",
            }:
                entries.append(value)
        for child in value.values():
            entries.extend(iter_loot_entries(child))
    elif isinstance(value, list):
        for child in value:
            entries.extend(iter_loot_entries(child))
    return entries


def validate_private_loot_tables(
    root: Path, minecraft_entries: set[str], errors: list[str]
) -> None:
    try:
        require_village_chest_loot_inventory(root)
    except ValidationError as exc:
        errors.append(str(exc))
        return
    for relative in VILLAGE_CHEST_LOOT_TABLE_PATHS:
        path = root / PurePosixPath(relative)
        if not path.is_file():
            errors.append(f"Missing private village chest loot table: {relative}")
            continue
        try:
            loot = load_json(path)
            require_village_loot_shape(loot, relative)
            if relative in LOOT_TABLE_PATHS:
                require_loot_pool_shape(loot, relative, "minecraft:empty")
            require_glowcap_currency_state(loot, relative, GLOWCAP_CURRENCY_TARGET_ID)
        except ValidationError as exc:
            errors.append(str(exc))
            continue
        for entry in iter_loot_entries(loot):
            if entry.get("type") != "minecraft:item":
                continue
            item_id = entry.get("name")
            if not isinstance(item_id, str) or ":" not in item_id:
                errors.append(f"Invalid item ID in {relative}: {item_id!r}")
                continue
            if item_id == "minecraft:air":
                errors.append(f"Item must not be minecraft:air in {relative}")
                continue
            namespace, item_path = item_id.split(":", 1)
            if namespace == "minecraft":
                evidence = f"assets/minecraft/items/{item_path}.json"
                if evidence not in minecraft_entries:
                    errors.append(
                        f"Minecraft 26.2 item registry evidence is absent for {item_id} in {relative}"
                    )
            elif namespace == "ribbits":
                if (
                    item_id not in SOURCE_SAFE_PUBLIC_LOOT_ITEM_IDS
                    and not (root / f"assets/ribbits/items/{item_path}.json").is_file()
                ):
                    errors.append(f"Ribbits item definition is absent for {item_id} in {relative}")
            else:
                errors.append(f"Unapproved item namespace {namespace!r} in {relative}")

    sorcerer_path = root / PurePosixPath(LOOT_TABLE_PATHS[1])
    if sorcerer_path.is_file():
        try:
            require_sorcerer_removed_loot_state(
                load_json(sorcerer_path), LOOT_TABLE_PATHS[1]
            )
        except ValidationError as exc:
            errors.append(str(exc))


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

    for spawn_egg_id in PRISTINE_SPAWN_EGG_IDS:
        model_path = root / f"assets/ribbits/models/item/{spawn_egg_id}.json"
        pristine_model = load_json(model_path)
        if pristine_model != {"parent": "minecraft:item/template_spawn_egg"}:
            raise ValidationError(
                f"Unexpected pristine spawn-egg model for {spawn_egg_id}: {pristine_model!r}"
            )
        write_json(model_path, SPAWN_EGG_MODEL)
    for spawn_egg_id in NEW_SPAWN_EGG_IDS:
        model_path = root / f"assets/ribbits/models/item/{spawn_egg_id}.json"
        if model_path.exists():
            raise ValidationError(f"Unexpected pre-existing new spawn-egg model: {spawn_egg_id}")
        write_json(model_path, SPAWN_EGG_MODEL)

    return {
        "authorization": (
            "First authorized by the Workbench owner for private Mynx Ribbits Canary 6; "
            "preserved unchanged in Canary 8"
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
    donor_identities: dict[str, dict[str, Any]],
    donor_outputs: list[dict[str, Any]],
    user_chute_inputs: list[dict[str, Any]],
    user_chute_outputs: list[dict[str, Any]],
    loot_migration: dict[str, Any],
    village_nbt_migration: dict[str, Any],
    village_utility_migration: dict[str, Any],
) -> dict[str, Any]:
    output_hashes = {
        name: sha256_file(output / PurePosixPath(name)) for name in relative_files(output)
    }
    return {
        "schema": PRIVATE_MANIFEST_SCHEMA,
        "classification": PRIVATE_MANIFEST_CLASSIFICATION,
        "candidate": {
            "version": CANDIDATE_VERSION,
            "canary": CANDIDATE_CANARY,
            "private_artifact_filename": PRIVATE_ARTIFACT_FILENAME,
            "source_only_artifact_filename": SOURCE_ONLY_ARTIFACT_FILENAME,
        },
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
            "total_size": sum(
                (output / PurePosixPath(name)).stat().st_size for name in output_hashes
            ),
            "files": output_hashes,
        },
        "read_only_donors": {
            "logical_root": "originals/mods",
            "private_use_only": True,
            "redistribution_authorized": False,
            "inputs": [
                {
                    **donor_identities[key],
                    "logical_path": f"originals/mods/{donor_identities[key]['filename']}",
                    "unchanged_after_assembly": True,
                }
                for key in ("guard", "useful", "wandering")
            ],
            "derived_outputs": donor_outputs,
            "derived_output_count": len(donor_outputs),
        },
        "user_authored_external_inputs": {
            "logical_root": "originals/assets",
            "private_use_only": True,
            "inputs": user_chute_inputs,
            "embedded_png_matches_standalone_png": True,
            "derived_outputs": user_chute_outputs,
            "derived_output_count": len(user_chute_outputs),
        },
        "source_safe_final_item_sprites": final_item_sprite_manifest_records(),
        "migrations": {
            "geckolib_models_moved": 25,
            "geckolib_animations_moved": 1,
            "donor_profession_models_added": 4,
            "donor_profession_umbrella_models_added": 12,
            "donor_profession_composite_textures_added": 4,
            "wandering_ribbit_models_added": 1,
            "wandering_ribbit_textures_added": 1,
            "chute_leaf_models_added": 3,
            "chute_leaf_item_definitions_added": 3,
            "chute_leaf_textures_added": 3,
            "item_definitions_added": len(REGISTERED_ITEM_IDS),
            "cutout_logical_blocks": 6,
            "cutout_concrete_models": len(CUTOUT_MODEL_FILES),
            "recipes_migrated": 11,
            "recipe_advancements_migrated": 6,
            "private_loot_tables_repaired": loot_migration,
            "language_files_migrated": 11,
            "config_prefix_renames": 33,
            "block_item_translation_keys_added": 154,
            "en_us_config_keys_added": len(EN_US_CONFIG_ADDITIONS),
            "en_us_mynx_profession_keys_added_or_changed": len(
                EN_US_MYNX_PROFESSION_TRANSLATIONS
            ),
            "zh_cn_syntax_repairs": 1,
            "spawn_egg_models_migrated": len(SPAWN_EGG_IDS),
            "spawn_egg_textures_added": 1,
            "configured_feature_random_patch_to_sequence": configured_feature_migration,
            "village_resident_profession_assignments_removed": village_nbt_migration,
            "private_village_progression_utilities_removed": village_utility_migration,
        },
        "authorized_spawn_egg_substitution": spawn_egg_substitution,
        "blockers": [],
    }


def _assemble_impl(
    pristine: Path,
    minecraft_client: Path,
    originals_root: Path,
    user_assets_root: Path,
    guard_donor: Path,
    useful_donor: Path,
    wandering_donor: Path,
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

    guard_members, guard_identity = load_exact_donor(
        guard_donor, originals_root, "guard"
    )
    useful_members, useful_identity = load_exact_donor(
        useful_donor, originals_root, "useful"
    )
    wandering_members, wandering_identity = load_exact_donor(
        wandering_donor, originals_root, "wandering"
    )
    donor_members = {
        "guard": guard_members,
        "useful": useful_members,
        "wandering": wandering_members,
    }
    donor_identities = {
        "guard": guard_identity,
        "useful": useful_identity,
        "wandering": wandering_identity,
    }
    donor_checks = [
        (guard_donor, guard_identity, "Guard donor JAR"),
        (useful_donor, useful_identity, "Useful donor JAR"),
        (wandering_donor, wandering_identity, "Wandering Ribbit donor JAR"),
    ]
    user_chute_model, user_chute_png, user_chute_inputs = load_user_authored_chute_inputs(user_assets_root)

    output.mkdir(parents=True)
    source_hashes: dict[str, str] = {}
    seen_casefold: set[str] = set()
    try:
        with zipfile.ZipFile(pristine) as archive:
            source_entries = [entry for entry in archive.infolist() if not entry.is_dir()]
            for entry in source_entries:
                safe_zip_name(entry.filename)
            selected = [
                entry
                for entry in source_entries
                if is_private_source_entry(entry.filename)
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
                require_descendant_path(destination, output, "Private resource output")
                destination.parent.mkdir(parents=True, exist_ok=True)
                destination.write_bytes(data)
                source_hashes[relative.as_posix()] = sha256_bytes(data)

        migrate_geckolib_paths(output)
        donor_outputs = import_donor_profession_resources(
            output, donor_members, donor_identities
        )
        migrate_languages(output)
        migrate_cutout_models(output)
        migrate_recipes(output)
        migrate_recipe_advancements(output)
        loot_migration = migrate_private_loot_tables(output)
        expected_sorcerer_output = {
            "path": LOOT_TABLE_PATHS[1],
            "size": SORCERER_LOOT_OUTPUT_SIZE,
            "sha256": SORCERER_LOOT_OUTPUT_SHA256,
        }
        if loot_migration["sorcerer_output"] != expected_sorcerer_output:
            raise ValidationError(
                "Final Sorcerer loot-table identity differs: expected "
                f"{expected_sorcerer_output}, got {loot_migration['sorcerer_output']}"
            )
        village_nbt_migration = remove_village_profession_assignments(output)
        village_utility_migration = transform_private_village_utilities(output)
        if (
            village_utility_migration["template_tree_before_sha256"]
            != PRIVATE_VILLAGE_TEMPLATE_TREE_BEFORE_SHA256
            or village_utility_migration["template_tree_after_sha256"]
            != PRIVATE_VILLAGE_TEMPLATE_TREE_AFTER_SHA256
        ):
            raise ValidationError(
                "Private village template-tree identity differs from the preserved Canary 6 contract"
            )
        configured_feature_migration = migrate_configured_features(output)
        write_item_definitions(output)
        wandering_donor_outputs, user_chute_outputs = import_wandering_visual_resources(
            output,
            donor_members["wandering"],
            donor_identities["wandering"],
            user_chute_model,
            user_chute_png,
        )
        donor_outputs.extend(wandering_donor_outputs)
        spawn_egg_substitution = migrate_spawn_egg_models(output, minecraft_client)

        output_file_count = len(relative_files(output))
        if output_file_count != OUTPUT_FILE_COUNT:
            raise ValidationError(
                f"Expected {OUTPUT_FILE_COUNT} assembled files, found {output_file_count}"
            )
        output_total_size = sum(path.stat().st_size for path in output.rglob("*") if path.is_file())
        if output_total_size != OUTPUT_TOTAL_SIZE:
            raise ValidationError(
                f"Expected {OUTPUT_TOTAL_SIZE} assembled bytes, found {output_total_size}"
            )

        manifest = build_manifest(
            pristine,
            output,
            source_hashes,
            spawn_egg_substitution,
            configured_feature_migration,
            donor_identities,
            donor_outputs,
            user_chute_inputs,
            user_chute_outputs,
            loot_migration,
            village_nbt_migration,
            village_utility_migration,
        )
        write_manifest_after_donor_verification(manifest_path, manifest, donor_checks)
    except Exception:
        # Leave the ignored staging tree intact for forensic inspection; never silently
        # delete a partially assembled protected-resource tree. A manifest is different:
        # it must never remain eligible after any failed assembly or donor rehash.
        manifest_path.unlink(missing_ok=True)
        raise
    finally:
        try:
            for donor_path, identity, label in donor_checks:
                require_donor_unchanged(donor_path, identity, label)
        except Exception:
            manifest_path.unlink(missing_ok=True)
            raise


def assemble(
    pristine: Path,
    minecraft_client: Path,
    originals_root: Path,
    user_assets_root: Path,
    guard_donor: Path,
    useful_donor: Path,
    wandering_donor: Path,
    output: Path,
    manifest_path: Path,
    private_root: Path,
) -> None:
    require_private_path(output, private_root, "Assembly output")
    require_private_path(manifest_path, private_root, "Assembly manifest")
    require_outside_tree(manifest_path, output, "Assembly manifest")
    _assemble_impl(
        pristine,
        minecraft_client,
        originals_root,
        user_assets_root,
        guard_donor,
        useful_donor,
        wandering_donor,
        output,
        manifest_path,
    )


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
    originals_root: Path,
    user_assets_root: Path,
    guard_donor: Path,
    useful_donor: Path,
    wandering_donor: Path,
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
            _assemble_impl(
                pristine,
                minecraft_client,
                originals_root,
                user_assets_root,
                guard_donor,
                useful_donor,
                wandering_donor,
                expected_root,
                expected_manifest,
            )
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


def nonallowlisted_donor_archive_violations(names: list[str] | set[str]) -> list[str]:
    violations: list[str] = []
    for name in sorted(set(names)):
        normalized = safe_zip_name(name).as_posix()
        folded = normalized.casefold()
        if folded.startswith(DONOR_PACKAGE_PREFIXES):
            violations.append(normalized)
        elif PurePosixPath(normalized).name.casefold() in DONOR_ARCHIVE_FILENAMES:
            violations.append(normalized)
    return violations


def source_only_donor_violations(names: list[str] | set[str]) -> list[str]:
    violations = nonallowlisted_donor_archive_violations(names)
    for name in sorted(set(names)):
        normalized = safe_zip_name(name).as_posix()
        if normalized in PRIVATE_ASSEMBLED_DERIVED_OUTPUTS and normalized not in violations:
            violations.append(normalized)
    return sorted(violations)


def private_domain_entries(names: list[str] | set[str]) -> set[str]:
    return {
        normalized
        for name in names
        if (
            (normalized := safe_zip_name(name).as_posix()).startswith(("assets/", "data/"))
            or normalized in {"icon.png", "logo.png"}
        )
    }


def private_domain_inventory_difference(
    staged_names: list[str] | set[str], packaged_names: list[str] | set[str]
) -> tuple[set[str], set[str]]:
    staged = set(staged_names)
    packaged = private_domain_entries(packaged_names)
    expected = staged | SOURCE_SAFE_PUBLIC_RESOURCE_PATHS
    return expected - packaged, packaged - expected


def validate_source_safe_public_resource_boundary(
    archive: zipfile.ZipFile,
    staged_private_entries: list[str] | set[str],
    errors: list[str],
    source_root: Path | None = None,
) -> None:
    staged_public_entries = set(staged_private_entries) & SOURCE_SAFE_PUBLIC_RESOURCE_PATHS
    if staged_public_entries:
        errors.append(
            "Source-safe public resources entered private staging: "
            f"{sorted(staged_public_entries)}"
        )

    tracked_root = (
        source_root
        if source_root is not None
        else Path(__file__).resolve().parent.parent / "common" / "src" / "main" / "resources"
    )
    validate_final_item_public_resources(tracked_root, errors)
    for relative in sorted(SOURCE_SAFE_PUBLIC_RESOURCE_PATHS):
        tracked_path = tracked_root.joinpath(*PurePosixPath(relative).parts)
        if not tracked_path.is_file():
            errors.append(f"Tracked source-safe public resource is missing: {relative}")
            continue
        try:
            packaged_bytes = archive.read(relative)
        except KeyError:
            # The exact inventory comparison reports the missing archive member.
            continue
        if packaged_bytes != tracked_path.read_bytes():
            errors.append(f"Source-safe public resource changed during packaging: {relative}")


def validate_required_fabric_dependencies(metadata: Any, errors: list[str]) -> None:
    actual_dependencies = metadata.get("depends", {}) if isinstance(metadata, dict) else {}
    if not isinstance(actual_dependencies, dict):
        errors.append("Packaged Fabric dependency map is not an object")
        return
    for dependency, expected in REQUIRED_FABRIC_DEPENDENCIES.items():
        if actual_dependencies.get(dependency) != expected:
            errors.append(
                f"Dependency {dependency} differs: expected {expected!r}, "
                f"got {actual_dependencies.get(dependency)!r}"
            )


def validate_donor_resource_boundary(root: Path, errors: list[str]) -> None:
    relative = set(relative_files(root))
    missing = PRIVATE_ASSEMBLED_DERIVED_OUTPUTS - relative
    if missing:
        errors.append(f"Private derived outputs are missing: {sorted(missing)}")
        return
    leaked_names = nonallowlisted_donor_archive_violations(relative)
    leaked_names.extend(
        name for name in relative if name.casefold().endswith((".class", ".java"))
    )
    if leaked_names:
        errors.append(f"Non-allowlisted donor payload entered private resources: {sorted(leaked_names)}")

    for profession in NEW_PROFESSIONS:
        normal_path = root / f"assets/ribbits/geckolib/models/{profession}_ribbit.geo.json"
        texture_path = root / f"assets/ribbits/textures/entity/{profession}_ribbit.png"
        try:
            normal = load_json(normal_path)
            description = require_geometry_document(normal, normal_path.name)["description"]
            if (
                description.get("texture_width") != COMPOSITE_TEXTURE_WIDTH
                or description.get("texture_height") != COMPOSITE_TEXTURE_HEIGHT
            ):
                errors.append(f"Composite atlas dimensions differ in {normal_path.name}")
            validate_model_uv_bounds(
                normal, normal_path.name, 0, COMPOSITE_SHARED_TEXTURE_X
            )
            if png_dimensions(texture_path.read_bytes(), texture_path.name) != (
                COMPOSITE_TEXTURE_WIDTH,
                COMPOSITE_TEXTURE_HEIGHT,
            ):
                errors.append(f"Composite profession texture dimensions differ: {texture_path.name}")
            decode_rgba_png(texture_path.read_bytes(), texture_path.name)
        except (OSError, ValidationError) as exc:
            errors.append(f"Invalid donor-derived normal resources for {profession}: {exc}")

        for variant in UMBRELLA_VARIANTS:
            umbrella_path = (
                root
                / f"assets/ribbits/geckolib/models/umbrella/{profession}/"
                f"umbrella_{variant}.geo.json"
            )
            expected_bone = "umbrella" if variant == 1 else f"umbrella{variant}"
            try:
                umbrella = load_json(umbrella_path)
                geometry = require_geometry_document(umbrella, umbrella_path.name)
                names = {bone["name"] for bone in geometry["bones"]}
                if expected_bone not in names:
                    errors.append(f"Missing {expected_bone} bone in {umbrella_path.name}")
                    continue
                validate_model_uv_bounds(
                    umbrella,
                    umbrella_path.name,
                    COMPOSITE_SHARED_TEXTURE_X,
                    COMPOSITE_TEXTURE_WIDTH,
                    bone_names={expected_bone},
                )
                validate_model_uv_bounds(
                    umbrella,
                    umbrella_path.name,
                    0,
                    COMPOSITE_SHARED_TEXTURE_X,
                    bone_names=names - {expected_bone},
                )
            except (OSError, ValidationError) as exc:
                errors.append(f"Invalid umbrella resources for {profession}/{variant}: {exc}")

    # Release-specific outputs intentionally differ from the immutable donor.
    for relative, expected in {
        "assets/ribbits/geckolib/models/wandering_ribbit.geo.json":
            "f7ee351ae54cb90c86e36a5faf991f22a25a11a7d755e4b4234d096c2d71a920",
        "assets/ribbits/textures/item/drop_leaf_inventory.png": "5acbe4afc118b2ec1a04ec5a2dcfd91f7db05bd61937019a75300756cc257a30",
        "assets/ribbits/textures/item/chute_leaf.png":
            "816e4d4edc23542afeb2f2f90a5af8a2076ae61829f1acb016711e05fec0191d",
    }.items():
        if sha256_file(root / relative) != expected:
            errors.append(f"Polished Wandering resource identity differs: {relative}")

    wandering_exact_outputs = {
        "assets/ribbits/textures/entity/wandering_ribbit.png": (
            "assets/wandering_ribbit/textures/entity/wandering_ribbit.png"
        )
    }
    for output, member in wandering_exact_outputs.items():
        path = root / PurePosixPath(output)
        spec = DONOR_INPUT_SPECS["wandering"]["members"][member]
        try:
            actual_size = path.stat().st_size
            actual_hash = sha256_file(path)
        except OSError as exc:
            errors.append(f"Could not inspect exact Wandering donor output {output}: {exc}")
            continue
        if actual_size != spec["size"] or actual_hash != spec["sha256"]:
            errors.append(
                f"Exact Wandering donor output differs for {output}: expected "
                f"{spec['size']} bytes/{spec['sha256']}, got {actual_size}/{actual_hash}"
            )

    try:
        require_geometry_document(
            load_json(root / "assets/ribbits/geckolib/models/wandering_ribbit.geo.json"),
            "wandering_ribbit.geo.json",
        )
        inventory_model = load_json(root / "assets/ribbits/models/item/chute_leaf.json")
        if inventory_model != {"parent": "minecraft:item/generated", "textures": {"layer0": "ribbits:item/drop_leaf_inventory"}}:
            errors.append("Drop Leaf inventory model differs")
        if load_json(root / "assets/ribbits/items/chute_leaf_closed.json") != {"model": {"type": "minecraft:model", "model": "ribbits:item/chute_leaf_closed"}}:
            errors.append("Drop Leaf closed-back item definition differs")
        closed_model = load_json(root / "assets/ribbits/models/item/chute_leaf_closed.json")
        if closed_model != {
            "parent": "minecraft:item/generated",
            "textures": {"layer0": "ribbits:item/chute_leaf"},
        }:
            errors.append(f"Closed Drop Leaf model differs: {closed_model!r}")
        open_model = load_json(root / "assets/ribbits/models/item/chute_leaf_open.json")
        if not isinstance(open_model, dict) or open_model.get("textures") != {
            "0": "ribbits:item/chute_leaf_open",
            "particle": "ribbits:item/chute_leaf_open",
        }:
            errors.append("Open Drop Leaf rain model lacks exact item-atlas-safe texture bindings")
        open_texture = root / "assets/ribbits/textures/item/chute_leaf_open.png"
        if not open_texture.is_file():
            errors.append("Open Drop Leaf item-atlas texture reference does not resolve")
        elif (open_texture.stat().st_size != USER_AUTHORED_CHUTE_INPUTS["png"]["size"]
              or sha256_file(open_texture) != USER_AUTHORED_CHUTE_INPUTS["png"]["sha256"]
              or png_dimensions(open_texture.read_bytes(), str(open_texture)) != (32, 32)):
            errors.append("Open Drop Leaf item-atlas texture differs from the exact user-authored 32x32 PNG")
        open_elements = open_model.get("elements") if isinstance(open_model, dict) else None
        if not isinstance(open_elements, list) or len(open_elements) != 2:
            errors.append("Open Drop Leaf does not contain exactly the two user-authored cubes")
        else:
            handle_element, canopy_element = open_elements
            if (
                handle_element.get("from") != [4.5, 0.0, 7.75]
                or handle_element.get("to") != [5.0, 22.0, 8.25]
                or set(handle_element.get("faces", {}))
                != {"north", "east", "south", "west", "up", "down"}
            ):
                errors.append("Open Drop Leaf user-authored handle geometry differs")
            if (
                canopy_element.get("from") != [0.25, 22.0, -2.5]
                or canopy_element.get("to") != [9.25, 22.0, 8.5]
                or set(canopy_element.get("faces", {})) != {"north", "east", "south", "west", "up", "down"}
                or canopy_element.get("faces", {}).get("up", {}).get("uv")
                != [20.0, 30.0, 11.0, 19.0]
                or canopy_element.get("faces", {}).get("down", {}).get("uv")
                != [20.0, 19.0, 11.0, 30.0]
                or any(
                    face.get("texture") != "#0"
                    for face in canopy_element.get("faces", {}).values()
                )
            ):
                errors.append("Open Drop Leaf user-authored canopy geometry/UV contract differs")
        if open_model.get("display") != {
            "thirdperson_righthand": {"rotation": [-106.53, -30.8, 174.15], "translation": [0, 0.75, 7]},
            "thirdperson_lefthand": {"rotation": [-106.53, -30.8, 174.15], "translation": [0, 0.75, 7]},
            "gui": {"rotation": [0, -23, -49.5], "translation": [-2.25, -3, 0], "scale": [1, 0.77, 1]},
            "fixed": {"rotation": [0, 0, 42.75], "translation": [4, -3.5, 0]},
        }:
            errors.append("Open Drop Leaf user-authored display transforms differ")
        open_item = load_json(root / "assets/ribbits/items/chute_leaf_open.json")
        if open_item != {
            "model": {"type": "minecraft:model", "model": "ribbits:item/chute_leaf_open"}
        }:
            errors.append(f"Open Drop Leaf item definition differs: {open_item!r}")
        for output in (
            "assets/ribbits/textures/entity/wandering_ribbit.png",
            "assets/ribbits/textures/item/chute_leaf.png",
            "assets/ribbits/textures/item/chute_leaf_open.png",
        ):
            data = (root / PurePosixPath(output)).read_bytes()
            decode_rgba_png(data, output)
    except (OSError, ValidationError) as exc:
        errors.append(f"Invalid Wandering Ribbit/Chute visual resources: {exc}")


def validate_transforms(root: Path, errors: list[str]) -> None:
    validate_configured_features(root, errors)
    validate_donor_resource_boundary(root, errors)

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
    expected_item_defs = set(REGISTERED_ITEM_IDS) | AUXILIARY_ITEM_DEFINITION_IDS
    if actual_item_defs != expected_item_defs:
        errors.append(
            f"Item definition set differs: expected={sorted(expected_item_defs)}, "
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
    for key, expected in EN_US_MYNX_PROFESSION_TRANSLATIONS.items():
        if en_values.get(key) != expected:
            errors.append(
                f"Mynx profession translation differs for {key}: "
                f"expected {expected!r}, got {en_values.get(key)!r}"
            )

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
    try:
        require_sorcerer_removed_loot_state(sorcerer, LOOT_TABLE_PATHS[1])
    except ValidationError as exc:
        errors.append(str(exc))

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
        names = {entry.filename for entry in archive.infolist() if not entry.is_dir()}
        for name in names:
            safe_zip_name(name)
        return names


def validation_report(
    resources: Path,
    pristine: Path,
    minecraft_client: Path,
    originals_root: Path,
    user_assets_root: Path,
    guard_donor: Path,
    useful_donor: Path,
    wandering_donor: Path,
    private_root: Path,
) -> dict[str, Any]:
    if not resources.is_dir():
        raise ValidationError(f"Resource directory does not exist: {resources}")
    errors: list[str] = []
    blockers: list[str] = []
    json_count = collect_json_errors(resources, errors)
    validate_binary_formats(resources, errors)
    validate_pristine_provenance(
        pristine,
        minecraft_client,
        originals_root,
        user_assets_root,
        guard_donor,
        useful_donor,
        wandering_donor,
        resources,
        private_root,
        errors,
    )
    if not errors:
        minecraft_entries = load_minecraft_entries(minecraft_client)
        validate_transforms(resources, errors)
        validate_private_loot_tables(resources, minecraft_entries, errors)
        validate_village_profession_assignments(resources, errors)
        validate_private_village_utility_transform(resources, errors)
        validate_resource_references(resources, minecraft_entries, errors)
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
    originals_root: Path,
    user_assets_root: Path,
    guard_donor: Path,
    useful_donor: Path,
    wandering_donor: Path,
    private_root: Path,
) -> dict[str, Any]:
    report = validation_report(
        resources,
        pristine,
        minecraft_client,
        originals_root,
        user_assets_root,
        guard_donor,
        useful_donor,
        wandering_donor,
        private_root,
    )
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
        staged_private_entries = set(relative_files(resources))
        missing_private_entries, unexpected_private_entries = (
            private_domain_inventory_difference(staged_private_entries, name_set)
        )
        if missing_private_entries:
            errors.append(
                "Packaged private-domain entries are missing from the staged inventory: "
                f"{sorted(missing_private_entries)}"
            )
        if unexpected_private_entries:
            errors.append(
                "Packaged private-domain entries exceed the staged inventory: "
                f"{sorted(unexpected_private_entries)}"
            )
        validate_source_safe_public_resource_boundary(
            archive, staged_private_entries, errors
        )
        missing_private_outputs = PRIVATE_ASSEMBLED_DERIVED_OUTPUTS - name_set
        if missing_private_outputs:
            errors.append(
                f"Private derived resources omitted from JAR: {sorted(missing_private_outputs)}"
            )
        forbidden_donor_entries = nonallowlisted_donor_archive_violations(name_set)
        if forbidden_donor_entries:
            errors.append(
                "Non-allowlisted donor namespaces/classes entered private JAR: "
                f"{sorted(forbidden_donor_entries)}"
            )
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
        if metadata.get("version") != CANDIDATE_VERSION:
            errors.append(f"Unexpected packaged version: {metadata.get('version')!r}")
        if metadata.get("environment") != "*":
            errors.append(f"Unexpected Fabric environment: {metadata.get('environment')!r}")
        if "accessWidener" in metadata:
            errors.append("Unexpected access widener declaration remains")
        validate_required_fabric_dependencies(metadata, errors)

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
        if data_namespaces != EXPECTED_PRIVATE_DATA_NAMESPACES:
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
    assemble_parser.add_argument("--originals-root", type=Path, required=True)
    assemble_parser.add_argument("--user-assets-root", type=Path, required=True)
    assemble_parser.add_argument("--guard-donor", type=Path, required=True)
    assemble_parser.add_argument("--useful-donor", type=Path, required=True)
    assemble_parser.add_argument("--wandering-donor", type=Path, required=True)
    assemble_parser.add_argument("--output", type=Path, required=True)
    assemble_parser.add_argument("--manifest", type=Path, required=True)

    tree_parser = subparsers.add_parser("validate-tree")
    tree_parser.add_argument("--private-root", type=Path, required=True)
    tree_parser.add_argument("--resources", type=Path, required=True)
    tree_parser.add_argument("--pristine", type=Path, required=True)
    tree_parser.add_argument("--minecraft-client", type=Path, required=True)
    tree_parser.add_argument("--originals-root", type=Path, required=True)
    tree_parser.add_argument("--user-assets-root", type=Path, required=True)
    tree_parser.add_argument("--guard-donor", type=Path, required=True)
    tree_parser.add_argument("--useful-donor", type=Path, required=True)
    tree_parser.add_argument("--wandering-donor", type=Path, required=True)
    tree_parser.add_argument("--report", type=Path)

    jar_parser = subparsers.add_parser("validate-jar")
    jar_parser.add_argument("--private-root", type=Path, required=True)
    jar_parser.add_argument("--resources", type=Path, required=True)
    jar_parser.add_argument("--pristine", type=Path, required=True)
    jar_parser.add_argument("--jar", type=Path, required=True)
    jar_parser.add_argument("--minecraft-client", type=Path, required=True)
    jar_parser.add_argument("--originals-root", type=Path, required=True)
    jar_parser.add_argument("--user-assets-root", type=Path, required=True)
    jar_parser.add_argument("--guard-donor", type=Path, required=True)
    jar_parser.add_argument("--useful-donor", type=Path, required=True)
    jar_parser.add_argument("--wandering-donor", type=Path, required=True)
    jar_parser.add_argument("--report", type=Path)
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    try:
        if args.command == "assemble":
            assemble(
                args.pristine,
                args.minecraft_client,
                args.originals_root,
                args.user_assets_root,
                args.guard_donor,
                args.useful_donor,
                args.wandering_donor,
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
                args.resources,
                args.pristine,
                args.minecraft_client,
                args.originals_root,
                args.user_assets_root,
                args.guard_donor,
                args.useful_donor,
                args.wandering_donor,
                args.private_root,
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
                args.originals_root,
                args.user_assets_root,
                args.guard_donor,
                args.useful_donor,
                args.wandering_donor,
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
