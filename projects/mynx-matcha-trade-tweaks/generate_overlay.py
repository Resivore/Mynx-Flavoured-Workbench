"""Generate Canary 1's explicit deltas against the accepted Matcha data tree.

The source pack is read only. Run this from anywhere with Python 3.11+.
Only changed trade resources and five membership/pool pairs are emitted.
"""

from __future__ import annotations

import json
import os
from copy import deepcopy
from pathlib import Path


PROJECT = Path(__file__).resolve().parent
ROOT = PROJECT.parents[1]
BASE = (
    ROOT
    / "projects/matcha-flavoured-data/src/main/resources/resourcepacks/matcha_flavoured_1_12"
)
OVERLAY = PROJECT / "src/main/resources/resourcepacks/mynx_matcha_trade_tweaks"

# IDs verified against current 26.2 registry/dependency artifacts, not guessed
# from the spreadsheet's display names.
ITEMS = {
    "waxed_copper_pipe": "simple_copper_pipes:waxed_copper_pipe",
    "waxed_copper_fitting": "simple_copper_pipes:waxed_copper_fitting",
    "copper_hopper": "copperhopper:copper_hopper",
    # Matcha's resource pack renames/retextures the vanilla beetroot seeds.
    "tomato_seeds": "minecraft:beetroot_seeds",
}

CHANGES: dict[str, dict] = {}


def add(names: str, *, level: int, profession: str, **changes: object) -> None:
    for name in names.split():
        key = f"{profession}/{level}/{name}"
        if key in CHANGES:
            raise ValueError(f"Duplicate change: {key}")
        CHANGES[key] = dict(changes)


# Armorer
add("comparator redstone_block repeater", level=1, profession="armorer",
    gives_count={"comparator": 16, "redstone_block": 16, "repeater": 32})
add("noteblock", level=1, profession="armorer",
    gives_id="waxed_copper_pipe", gives_count=32)
add("redstone_lamp waxed_copper_bulb", level=2, profession="armorer", gives_count=16)
add("target", level=2, profession="armorer",
    gives_id="waxed_copper_fitting", gives_count=8)
add("dispenser hopper", level=3, profession="armorer", gives_count=8)
add("dropper observer", level=3, profession="armorer", gives_count=16)
add("crafter piston sticky_piston", level=4, profession="armorer", gives_count=8)
add("lava_kit", level=5, profession="armorer",
    gives_id="copper_hopper", gives_count=8)

# Butcher
add("sweet_berry_toast", level=1, profession="butcher", gives_count=2)
add("warped_stroganoff", level=2, profession="butcher", gives_count=1)
add("chorus_mochi", level=3, profession="butcher", gives_count=2)
add("butcher_knife", level=4, profession="butcher",
    wants_count=8, gives_count=1)

# Cartographer: the old statue-specific goat-horn components cannot describe
# the replacement item.
add("cheerful_clay_statue mournful_clay_statue", level=5,
    profession="cartographer", wants_count=1,
    gives_id="minecraft:emerald", gives_count=16)

# Farmer
add("bonemeal", level=1, profession="farmer", gives_count=8)
for name, item in {
    "exotic_seed_bundle": "minecraft:wheat_seeds",
    "floral_bundle": "minecraft:pumpkin_seeds",
    "mushy_bundle": "minecraft:melon_seeds",
    "seed_bundle": "tomato_seeds",
}.items():
    add(name, level=1, profession="farmer", gives_id=item, gives_count=8,
        remove_additional_wants=(name == "exotic_seed_bundle"))
add("blue_egg brown_egg white_egg", level=2, profession="farmer",
    wants_count=8, max_uses=1)
add("baby_cold_cow baby_cold_pig baby_temperate_cow baby_temperate_pig "
    "baby_warm_cow baby_warm_pig", level=3, profession="farmer",
    wants_count=16, max_uses=1)

# Fisherman buybacks; preserve each fish ID and emerald output.
add("alaska_blackfish anchovy bluegill bujurqui cod crappie "
    "freshwater_pufferfish guppy humpback_whitefish mediterranean_killifish "
    "pufferfish rainbow_wrasse salmon shad striped_perch tropical_fish",
    level=1, profession="fisherman", wants_count=6)
add("black_seabass carp flying_fish gurnard herring lamprey mahi_mahi "
    "piranha spoonhead_sculpin walleye", level=2, profession="fisherman",
    wants_count=4)
add("armoured_catfish catfish flounder gar monkfish northern_pike "
    "painted_moray sturgeon tunisian_barb wolffish", level=3,
    profession="fisherman", wants_count=2)

# Leatherworker
add("birch_log oak_log spruce_log", level=1, profession="leatherworker",
    gives_count=16)
add("acacia_log cherry_log dark_oak_log jungle_log mangrove_log", level=2,
    profession="leatherworker", gives_count=16)
add("crimson_log mushroom_stem pale_oak_log warped_log", level=3,
    profession="leatherworker", gives_count=16)
add("filler", level=5, profession="leatherworker",
    wants_id="minecraft:emerald", wants_count=1,
    gives_id="minecraft:brown_mushroom_block", gives_count=16,
    remove_given_item_modifiers=True)

# Librarian: named-book input components stay exactly as Matcha defined them.
add("divine_comedy paradise_lost", level=1, profession="librarian",
    gives_id="minecraft:emerald", gives_count=6)
add("ender_pearl", level=1, profession="librarian", gives_count=1)

# Mason
add("andesite diorite granite stone", level=1, profession="mason",
    gives_count=64)
add("deepslate glass smooth_red_sandstone smooth_sandstone tuff", level=2,
    profession="mason", gives_count=32)
add("terracotta", level=2, profession="mason", gives_count=16)
add("calcite", level=3, profession="mason", gives_count=32)
add("flowstone", level=3, profession="mason",
    gives_id="minecraft:dripstone_block", gives_count=32)
add("malachite", level=3, profession="mason", gives_count=16)
add("blackstone smooth_basalt smooth_quartz", level=4, profession="mason",
    gives_count=16)
# Matcha's bulk sales encode the block in a chicken-spawn-egg entity_data
# component. These requests are direct block stacks, so discard that wrapper.
for level, names in {
    1: "andesite diorite granite stone",
    2: "deepslate glass smooth_red_sandstone smooth_sandstone tuff",
    3: "calcite",
}.items():
    for name in names.split():
        CHANGES[f"mason/{level}/{name}"]["gives_id"] = f"minecraft:{name}"

# Shepherd
add("black_wool blue_wool brown_wool cyan_wool gray_wool green_wool "
    "light_blue_wool light_gray_wool lime_wool magenta_wool orange_wool "
    "pink_wool purple_wool red_wool white_wool yellow_wool", level=1,
    profession="shepherd", gives_count=16)
add("baby_cold_sheep baby_temperate_sheep baby_warm_sheep", level=2,
    profession="shepherd", wants_count=16, max_uses=1)
add("shepherds_shears", level=2, profession="shepherd", wants_count=8)
add("crook", level=3, profession="shepherd", wants_count=8)

REMOVALS = {
    "armorer/1/composter",
    "butcher/1/sweet_berry_toast_recipe",
    "butcher/2/warped_stroganoff_recipe",
    "butcher/3/chorus_mochi_recipe",
}
POOL_MEMBERSHIP = {
    ("armorer", 1): {"remove": {"composter"}},
    ("butcher", 1): {"remove": {"sweet_berry_toast_recipe"}},
    ("butcher", 2): {"remove": {"warped_stroganoff_recipe"}},
    ("butcher", 3): {"remove": {"chorus_mochi_recipe"}},
    ("leatherworker", 5): {"add": ["red_mushroom_block"]},
}


def read(relative: str) -> dict:
    target = BASE / relative
    if os.name == "nt":
        target = Path("\\\\?\\" + str(target))
    return json.loads(target.read_text(encoding="utf-8"))


def write(relative: str, value: dict) -> None:
    target = OVERLAY / relative
    if os.name == "nt":
        target = Path("\\\\?\\" + str(target))
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(value, indent=2, ensure_ascii=False) + "\n",
                      encoding="utf-8", newline="\n")


def resolved(item: str) -> str:
    item = ITEMS.get(item, item)
    if item == "UNRESOLVED" or ":" not in item:
        raise ValueError(f"Unresolved item ID: {item}")
    return item


def scalar(change: object, name: str) -> object:
    return change[name] if isinstance(change, dict) else change


def main() -> None:
    assert len(CHANGES) == 124, len(CHANGES)
    assert len(REMOVALS) == 4
    for key, spec in sorted(CHANGES.items()):
        relative = f"data/minecraft/villager_trade/{key}.json"
        trade = deepcopy(read(relative))
        name = key.rsplit("/", 1)[1]
        if "wants_id" in spec:
            trade["wants"] = {"id": resolved(spec["wants_id"])}
        if "wants_count" in spec:
            trade["wants"]["count"] = scalar(spec["wants_count"], name)
        if "gives_id" in spec:
            old_components = trade["gives"].get("components")
            trade["gives"] = {"id": resolved(spec["gives_id"])}
            if spec.get("preserve_result_components") and old_components:
                trade["gives"]["components"] = old_components
        if "gives_count" in spec:
            trade["gives"]["count"] = scalar(spec["gives_count"], name)
        if "max_uses" in spec:
            trade["max_uses"] = spec["max_uses"]
        if spec.get("remove_additional_wants"):
            trade.pop("additional_wants", None)
        if spec.get("remove_given_item_modifiers"):
            trade.pop("given_item_modifiers", None)
        write(relative, trade)

    brown_path = OVERLAY / "data/minecraft/villager_trade/leatherworker/5/filler.json"
    if os.name == "nt":
        brown_path = Path("\\\\?\\" + str(brown_path))
    brown = json.loads(brown_path.read_text(encoding="utf-8"))
    red = deepcopy(brown)
    red["gives"]["id"] = "minecraft:red_mushroom_block"
    write("data/minecraft/villager_trade/leatherworker/5/red_mushroom_block.json", red)

    for (profession, level), edit in POOL_MEMBERSHIP.items():
        tag_path = f"data/minecraft/tags/villager_trade/{profession}/custom_level_{level}.json"
        tag = read(tag_path)
        before = list(tag["values"])
        removed = {f"minecraft:{profession}/{level}/{name}" for name in edit.get("remove", ())}
        assert removed <= set(before)
        tag["values"] = [value for value in before if value not in removed]
        tag["values"].extend(f"minecraft:{profession}/{level}/{name}"
                             for name in edit.get("add", ()))
        assert len(tag["values"]) == len(set(tag["values"]))
        tag["replace"] = True
        write(tag_path, tag)

        set_path = f"data/minecraft/trade_set/{profession}/level_{level}.json"
        trade_set = read(set_path)
        trade_set["amount"] = float(len(tag["values"]))
        write(set_path, trade_set)

    print(f"Wrote {len(CHANGES)} changed, {len(REMOVALS)} removed via pools, "
          "1 new trade, and 5 tag/trade-set overrides")


if __name__ == "__main__":
    main()
