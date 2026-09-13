"""Check the narrow Matcha trade overlay against the accepted source pack.

Run from any directory: python tools/validate_trades.py [--write-report]
The comparison report is deterministic and intentionally lists every requested row.
"""

from __future__ import annotations

import argparse
import copy
import hashlib
import io
import json
import os
import zipfile
from pathlib import Path


PROJECT = Path(__file__).resolve().parents[1]
ROOT = PROJECT.parents[1]


def native_path(path: Path) -> Path:
    # This worktree's descriptive path pushes some trade filenames past the
    # Windows legacy 260-character limit. The extended path form preserves IO.
    return Path("\\\\?\\" + str(path)) if os.name == "nt" else path


BASE = native_path(ROOT / "projects/matcha-flavoured-data/src/main/resources/resourcepacks/matcha_flavoured_1_12")
OVERLAY = native_path(PROJECT / "src/main/resources/resourcepacks/mynx_matcha_trade_tweaks")
REPORT = PROJECT / "tools/TRADE_COMPARISON.md"
TRADE = Path("data/minecraft/villager_trade")
TAG = Path("data/minecraft/tags/villager_trade")
POOL = Path("data/minecraft/trade_set")

# A spec changes only the named fields. All other source fields, including custom
# components, XP, predicates, reputation discounts, and use limits, must survive.
# Stack count 1 is normalized below because JSON may omit the default count.
SPECS: dict[str, dict] = {}


def change(profession: str, level: int, name: str, *, cost=None, result=None,
           no_second_cost=False, max_uses=None, no_result_modifiers=False) -> None:
    trade_id = f"minecraft:{profession}/{level}/{name}"
    assert trade_id not in SPECS
    SPECS[trade_id] = {
        "cost": cost, "result": result, "no_second_cost": no_second_cost,
        "max_uses": max_uses, "no_result_modifiers": no_result_modifiers,
    }


def catalog() -> None:
    for name, quantity in {
        "comparator": 16, "redstone_block": 16, "repeater": 32,
        "redstone_lamp": 16, "waxed_copper_bulb": 16,
        "dispenser": 8, "dropper": 16, "hopper": 8, "observer": 16,
        "crafter": 8, "piston": 8, "sticky_piston": 8,
    }.items():
        level = (1 if name in {"comparator", "redstone_block", "repeater"}
                 else 2 if name in {"redstone_lamp", "waxed_copper_bulb"}
                 else 3 if name in {"dispenser", "dropper", "hopper", "observer"}
                 else 4)
        change("armorer", level, name, result=quantity)
    change("armorer", 1, "noteblock", result=("simple_copper_pipes:waxed_copper_pipe", 32))
    change("armorer", 2, "target", result=("simple_copper_pipes:waxed_copper_fitting", 8))
    change("armorer", 5, "lava_kit", result=("copperhopper:copper_hopper", 8))

    for level, name, quantity in ((1, "sweet_berry_toast", 2),
                                  (2, "warped_stroganoff", 1),
                                  (3, "chorus_mochi", 2)):
        change("butcher", level, name, result=quantity)
    change("butcher", 4, "butcher_knife", cost=8)

    for name in ("cheerful_clay_statue", "mournful_clay_statue"):
        change("cartographer", 5, name, result=("minecraft:emerald", 16))

    change("farmer", 1, "bonemeal", result=8)
    for name, item in (("exotic_seed_bundle", "minecraft:wheat_seeds"),
                       ("floral_bundle", "minecraft:pumpkin_seeds"),
                       ("mushy_bundle", "minecraft:melon_seeds"),
                       ("seed_bundle", "minecraft:beetroot_seeds")):
        change("farmer", 1, name, result=(item, 8),
               no_second_cost=name == "exotic_seed_bundle")
    for name in ("blue_egg", "brown_egg", "white_egg"):
        change("farmer", 2, name, cost=8, max_uses=1)
    for name in ("baby_cold_cow", "baby_cold_pig", "baby_temperate_cow",
                 "baby_temperate_pig", "baby_warm_cow", "baby_warm_pig"):
        change("farmer", 3, name, cost=16, max_uses=1)

    fish = {
        1: "alaska_blackfish anchovy bluegill bujurqui cod crappie freshwater_pufferfish guppy humpback_whitefish mediterranean_killifish pufferfish rainbow_wrasse salmon shad striped_perch tropical_fish",
        2: "black_seabass carp flying_fish gurnard herring lamprey mahi_mahi piranha spoonhead_sculpin walleye",
        3: "armoured_catfish catfish flounder gar monkfish northern_pike painted_moray sturgeon tunisian_barb wolffish",
    }
    for level, names in fish.items():
        for name in names.split():
            change("fisherman", level, name, cost={1: 6, 2: 4, 3: 2}[level])

    for name in ("birch_log", "oak_log", "spruce_log"):
        change("leatherworker", 1, name, result=16)
    for name in ("acacia_log", "cherry_log", "dark_oak_log", "jungle_log",
                 "mangrove_log"):
        change("leatherworker", 2, name, result=16)
    for name in ("crimson_log", "mushroom_stem", "pale_oak_log", "warped_log"):
        change("leatherworker", 3, name, result=16)
    change("leatherworker", 5, "filler", cost=("minecraft:emerald", 1),
           result=("minecraft:brown_mushroom_block", 16),
           no_result_modifiers=True)

    for name in ("divine_comedy", "paradise_lost"):
        change("librarian", 1, name, result=("minecraft:emerald", 6))
    change("librarian", 1, "ender_pearl", result=1)

    mason = {
        1: {"andesite": ("minecraft:andesite", 64),
            "diorite": ("minecraft:diorite", 64),
            "granite": ("minecraft:granite", 64),
            "stone": ("minecraft:stone", 64)},
        2: {"deepslate": ("minecraft:deepslate", 32),
            "glass": ("minecraft:glass", 32),
            "smooth_red_sandstone": ("minecraft:smooth_red_sandstone", 32),
            "smooth_sandstone": ("minecraft:smooth_sandstone", 32),
            "terracotta": ("minecraft:terracotta", 16),
            "tuff": ("minecraft:tuff", 32)},
        3: {"calcite": ("minecraft:calcite", 32),
            "flowstone": ("minecraft:dripstone_block", 32),
            "malachite": ("minecraft:prismarine", 16)},
        4: {"blackstone": ("minecraft:blackstone", 16),
            "smooth_basalt": ("minecraft:smooth_basalt", 16),
            "smooth_quartz": ("minecraft:smooth_quartz", 16)},
    }
    for level, names in mason.items():
        for name, result in names.items():
            change("mason", level, name, result=result)

    colors = "black blue brown cyan gray green light_blue light_gray lime magenta orange pink purple red white yellow"
    for color in colors.split():
        change("shepherd", 1, f"{color}_wool", result=16)
    for name in ("baby_cold_sheep", "baby_temperate_sheep", "baby_warm_sheep"):
        change("shepherd", 2, name, cost=16, max_uses=1)
    change("shepherd", 2, "shepherds_shears", cost=8)
    change("shepherd", 3, "crook", cost=8)


catalog()
REMOVED = {
    "minecraft:armorer/1/composter",
    "minecraft:butcher/1/sweet_berry_toast_recipe",
    "minecraft:butcher/2/warped_stroganoff_recipe",
    "minecraft:butcher/3/chorus_mochi_recipe",
}
ADDED = "minecraft:leatherworker/5/red_mushroom_block"
assert len(SPECS) == 124 and len(REMOVED) == 4 and len(SPECS) + len(REMOVED) + 1 == 129


def path_for(directory: Path, resource_id: str) -> Path:
    namespace, path = resource_id.split(":", 1)
    return directory / f"data/{namespace}/{path}.json"


def trade_path(directory: Path, resource_id: str) -> Path:
    return path_for(directory, resource_id.replace(":", ":villager_trade/", 1))


def load(path: Path):
    return json.loads(path.read_text(encoding="utf-8"))


def canonical_trade(trade: dict) -> dict:
    trade = copy.deepcopy(trade)
    for key in ("wants", "additional_wants", "gives"):
        if isinstance(trade.get(key), dict):
            trade[key].setdefault("count", 1)
    return trade


def set_stack(stack: dict, update) -> None:
    if update is None:
        return
    if isinstance(update, tuple):
        item_id, count = update
        if stack["id"] != item_id:
            stack.pop("components", None)
        stack["id"] = item_id
    else:
        count = update
    stack["count"] = count


def expected_trade(source: dict, spec: dict) -> dict:
    result = canonical_trade(source)
    set_stack(result["wants"], spec["cost"])
    set_stack(result["gives"], spec["result"])
    if spec["no_second_cost"]:
        result.pop("additional_wants", None)
    if spec["max_uses"] is not None:
        result["max_uses"] = spec["max_uses"]
    if spec["no_result_modifiers"]:
        result.pop("given_item_modifiers", None)
    return result


def effective_tag(resource_id: str) -> tuple[list[str], dict | None]:
    source_path = path_for(BASE, resource_id.replace(":", ":tags/villager_trade/", 1))
    override_path = path_for(OVERLAY, resource_id.replace(":", ":tags/villager_trade/", 1))
    source = load(source_path)
    override = load(override_path) if override_path.exists() else None
    values = list(source["values"])
    if override is not None:
        values = ([] if override.get("replace") is True else values) + list(override["values"])
    return values, override


def describe_stack(stack: dict | None) -> str:
    if stack is None:
        return ""
    count = stack.get("count", 1)
    description = f"{count} {stack['id']}"
    components = stack.get("components") or {}
    model = components.get("minecraft:item_model")
    if model:
        description += f" [model: {model}]"
    if "minecraft:bundle_contents" in components:
        description += " [bundle contents]"
    entity = components.get("minecraft:entity_data") or {}
    if entity.get("id") == "minecraft:item":
        inner = entity.get("Item") or {}
        description += f" [contains: {inner.get('count', 1)} {inner.get('id', '?')}]"
    return description


def describe(trade: dict | None) -> str:
    if trade is None:
        return "REMOVED"
    wants = describe_stack(trade["wants"])
    if trade.get("additional_wants"):
        wants += " + " + describe_stack(trade["additional_wants"])
    output = f"{wants} → {describe_stack(trade['gives'])}"
    output += f"; max uses {trade.get('max_uses', 'default')}"
    return output


def comparison_report(old: dict[str, dict], new: dict[str, dict | None]) -> str:
    lines = ["# Matcha trade overlay comparison", "",
             "Source: accepted Matcha Flavoured 1.12 embedded data tree. Counts and item IDs below are effective trade stacks; model and contents notes identify custom stacks.", "",
             "| Trade ID | Old effective trade | New effective trade |",
             "| --- | --- | --- |"]
    for resource_id in sorted(old.keys() | new.keys()):
        before = describe(old.get(resource_id)) if resource_id in old else "NEW"
        after = describe(new.get(resource_id))
        lines.append(f"| `{resource_id}` | {before} | {after} |")
    lines.extend(["", f"{len(SPECS)} changed, {len(REMOVED)} removed, 1 added; 129 actionable sheet rows.", ""])
    return "\n".join(lines)


def verify_dependency_snapshot(snapshot: Path, errors: list[str]) -> None:
    """Prove non-vanilla result IDs against a local, read-only dependency ZIP.

    The snapshot is optional because it is untracked. Hashes identify validation
    inputs only; they are not runtime version requirements.
    """
    expected = {
        "mods/SimpleCopperPipes-mc26.2-2.1.7.jar": (
            "222b7e4116d52784c1f495b8ff86ff4849fcbef55cf9ddd6bf87ffe4c4c30484",
            {
                "simple_copper_pipes:waxed_copper_pipe": "data/simple_copper_pipes/recipe/waxed_copper_pipe_from_honeycomb.json",
                "simple_copper_pipes:waxed_copper_fitting": "data/simple_copper_pipes/recipe/waxed_copper_fitting_from_honeycomb.json",
            },
        ),
        "mods/copperhopper-fabric-0.24.0+26.2.jar": (
            "462b87d7821ca9c4771c071a0e8750d544cc27d9cc02d944cbbc9949a6ed6e93",
            {"copperhopper:copper_hopper": "data/copperhopper/recipe/copper_hopper.json"},
        ),
    }
    with zipfile.ZipFile(snapshot) as archive:
        for jar_name, (expected_hash, recipes) in expected.items():
            try:
                jar_bytes = archive.read(jar_name)
            except KeyError:
                errors.append(f"dependency snapshot lacks {jar_name}")
                continue
            actual_hash = hashlib.sha256(jar_bytes).hexdigest()
            if actual_hash != expected_hash:
                errors.append(f"dependency snapshot hash changed for {jar_name}: {actual_hash}")
            with zipfile.ZipFile(io.BytesIO(jar_bytes)) as jar:
                for item_id, recipe_path in recipes.items():
                    namespace, path = item_id.split(":", 1)
                    asset_path = f"assets/{namespace}/items/{path}.json"
                    if asset_path not in jar.namelist():
                        errors.append(f"dependency item asset absent: {item_id}")
                    try:
                        recipe = json.loads(jar.read(recipe_path))
                    except (KeyError, ValueError) as exc:
                        errors.append(f"dependency item recipe invalid for {item_id}: {exc}")
                        continue
                    if recipe.get("result", {}).get("id") != item_id:
                        errors.append(f"dependency recipe does not produce {item_id}")


def validate(write_report: bool, dependency_zip: Path | None) -> None:
    errors: list[str] = []
    def check(condition: bool, message: str) -> None:
        if not condition:
            errors.append(message)

    check(BASE.is_dir(), f"missing accepted Matcha pack: {BASE}")
    check(OVERLAY.is_dir(), f"missing overlay pack: {OVERLAY}")
    if errors:
        raise SystemExit("\n".join(errors))

    # Matcha's tomato-seed name is an asset alias over the beetroot-seed item.
    # Its accepted crafting recipe establishes the item identity in tracked data.
    tomato_recipe = load(BASE / "data/crafting/recipe/tomato_seeds_from_tomato.json")
    check(tomato_recipe.get("result", {}).get("id") == "minecraft:beetroot_seeds",
          "accepted Matcha tomato-seed recipe no longer resolves to minecraft:beetroot_seeds")
    if dependency_zip is not None:
        try:
            verify_dependency_snapshot(dependency_zip, errors)
        except (OSError, zipfile.BadZipFile) as exc:
            errors.append(f"cannot validate dependency snapshot {dependency_zip}: {exc}")

    # Parsing every generated JSON also catches malformed resources outside the
    # trade files. The allowed file set catches accidental copying of baseline.
    all_overlay_json = {p.relative_to(OVERLAY).as_posix(): p for p in OVERLAY.rglob("*.json")}
    for relative, path in sorted(all_overlay_json.items()):
        try:
            load(path)
        except (ValueError, OSError) as exc:
            errors.append(f"invalid JSON {relative}: {exc}")
    if errors:
        raise SystemExit("\n".join(errors))

    expected_trade_paths = {trade_path(OVERLAY, rid).relative_to(OVERLAY).as_posix()
                            for rid in SPECS | {ADDED: {}}}
    actual_trade_paths = {p for p in all_overlay_json if p.startswith(TRADE.as_posix() + "/")}
    check(actual_trade_paths == expected_trade_paths,
          f"trade override path mismatch: missing {sorted(expected_trade_paths - actual_trade_paths)}, extra {sorted(actual_trade_paths - expected_trade_paths)}")

    changed_tags = {"minecraft:armorer/custom_level_1",
                    "minecraft:butcher/custom_level_1",
                    "minecraft:butcher/custom_level_2",
                    "minecraft:butcher/custom_level_3",
                    "minecraft:leatherworker/custom_level_5"}
    expected_tag_paths = {path_for(OVERLAY, rid.replace(":", ":tags/villager_trade/", 1)).relative_to(OVERLAY).as_posix()
                          for rid in changed_tags}
    actual_tag_paths = {p for p in all_overlay_json if p.startswith(TAG.as_posix() + "/")}
    check(actual_tag_paths == expected_tag_paths,
          f"tag path mismatch: missing {sorted(expected_tag_paths - actual_tag_paths)}, extra {sorted(actual_tag_paths - expected_tag_paths)}")
    expected_pool_paths = {path_for(OVERLAY, rid.replace("custom_level_", "level_").replace(":", ":trade_set/", 1)).relative_to(OVERLAY).as_posix()
                           for rid in changed_tags}
    actual_pool_paths = {p for p in all_overlay_json if p.startswith(POOL.as_posix() + "/")}
    check(actual_pool_paths == expected_pool_paths,
          f"pool path mismatch: missing {sorted(expected_pool_paths - actual_pool_paths)}, extra {sorted(actual_pool_paths - expected_pool_paths)}")
    check(set(all_overlay_json) == actual_trade_paths | actual_tag_paths | actual_pool_paths,
          "overlay contains unrelated JSON")

    old_report: dict[str, dict] = {}
    new_report: dict[str, dict | None] = {}
    for rid, spec in sorted(SPECS.items()):
        source_path = trade_path(BASE, rid)
        overlay_path = trade_path(OVERLAY, rid)
        check(source_path.is_file(), f"missing baseline trade {rid}")
        check(overlay_path.is_file(), f"missing overlay trade {rid}")
        if not source_path.is_file() or not overlay_path.is_file():
            continue
        source = load(source_path)
        actual = canonical_trade(load(overlay_path))
        expected = expected_trade(source, spec)
        check(actual == expected, f"trade delta or metadata differs: {rid}\nexpected {json.dumps(expected, sort_keys=True)}\nactual   {json.dumps(actual, sort_keys=True)}")
        old_report[rid] = source
        new_report[rid] = actual

    for rid in sorted(REMOVED):
        check(trade_path(BASE, rid).is_file(), f"missing baseline removal target {rid}")
        check(not trade_path(OVERLAY, rid).exists(), f"removed trade has overlay definition {rid}")
        old_report[rid] = load(trade_path(BASE, rid))
        new_report[rid] = None

    brown_id = "minecraft:leatherworker/5/filler"
    red_path = trade_path(OVERLAY, ADDED)
    if red_path.is_file() and brown_id in new_report:
        red = canonical_trade(load(red_path))
        expected_red = copy.deepcopy(new_report[brown_id])
        expected_red["gives"]["id"] = "minecraft:red_mushroom_block"
        check(red == expected_red, "new red mushroom trade must match brown trade except result item")
        new_report[ADDED] = red
    else:
        check(False, "new red mushroom trade is missing")

    for tag_id in sorted(changed_tags):
        base_tag_path = path_for(BASE, tag_id.replace(":", ":tags/villager_trade/", 1))
        overlay_tag_path = path_for(OVERLAY, tag_id.replace(":", ":tags/villager_trade/", 1))
        if not base_tag_path.is_file() or not overlay_tag_path.is_file():
            continue
        original = load(base_tag_path)["values"]
        effective, override = effective_tag(tag_id)
        check(override.get("replace") is True, f"{tag_id} must replace the lower-priority tag")
        expected = [rid for rid in original if rid not in REMOVED]
        if tag_id == "minecraft:leatherworker/custom_level_5":
            expected.append(ADDED)
        check(effective == expected, f"effective tag differs: {tag_id}: {effective} != {expected}")
        check(len(effective) == len(set(effective)), f"duplicate tag entries: {tag_id}")
        pool_id = tag_id.replace("custom_level_", "level_")
        base_pool_path = path_for(BASE, pool_id.replace(":", ":trade_set/", 1))
        overlay_pool_path = path_for(OVERLAY, pool_id.replace(":", ":trade_set/", 1))
        original_pool = load(base_pool_path)
        actual_pool = load(overlay_pool_path)
        expected_pool = copy.deepcopy(original_pool)
        expected_pool["amount"] = float(len(effective))
        check(actual_pool == expected_pool, f"pool metadata/cardinality differs: {pool_id}")
        check(0 < actual_pool.get("amount", 0) <= len(effective), f"invalid pool selection count: {pool_id}")

    retained_fillers = 0
    for filler_path in (BASE / TRADE).rglob("filler.json"):
        parts = filler_path.relative_to(BASE / TRADE).parts
        if len(parts) != 3:
            continue
        profession, level, _ = parts
        filler_id = f"minecraft:{profession}/{level}/filler"
        filler = load(filler_path)
        if filler.get("wants", {}).get("id") != "minecraft:black_wool" or filler.get("gives", {}).get("id") != "minecraft:emerald":
            continue
        if filler_id == brown_id:
            continue  # This specific row is intentionally repurposed.
        retained_fillers += 1
        check(not trade_path(OVERLAY, filler_id).exists(), f"black-wool filler overridden: {filler_id}")
        tag_id = f"minecraft:{profession}/custom_level_{level}"
        members, _ = effective_tag(tag_id)
        check(filler_id in members, f"black-wool filler absent from effective pool: {filler_id}")
    check(retained_fillers == 18, f"expected 18 retained tentative-removal black-wool fillers, found {retained_fillers}")

    # Every other Matcha trade is inherited directly. No other tag or pool may
    # change, so its placement and behavior stay identical to the accepted pack.
    for path in BASE.rglob("data/*/villager_trade/**/*.json"):
        rid = path.relative_to(BASE).as_posix()
        if rid.startswith(TRADE.as_posix() + "/"):
            candidate = OVERLAY / rid
            if candidate.is_file() and candidate.relative_to(OVERLAY).as_posix() not in expected_trade_paths:
                errors.append(f"unchanged trade shadowed by overlay: {rid}")

    report = comparison_report(old_report, new_report)
    if len(old_report) == 128 and len(new_report) == 129:
        if write_report:
            REPORT.write_text(report, encoding="utf-8", newline="\n")
        else:
            check(REPORT.is_file() and REPORT.read_text(encoding="utf-8") == report,
                  "comparison report missing or stale; run with --write-report")
    else:
        check(False, f"incomplete comparison: {len(old_report)} old, {len(new_report)} new")

    if errors:
        raise SystemExit("\n".join(errors))
    print("PASS: 124 changed trades, 4 removals, 1 addition; 5 effective tags and pools; 18 black-wool fillers retained")
    if dependency_zip is not None:
        print("PASS: three external result IDs resolve in the exact local dependency snapshot")
    print(f"PASS: deterministic comparison report {REPORT}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--write-report", action="store_true")
    parser.add_argument("--dependency-zip", type=Path,
                        help="optional read-only local modpacks/mods.zip for external item ID proof")
    args = parser.parse_args()
    validate(args.write_report, args.dependency_zip)
