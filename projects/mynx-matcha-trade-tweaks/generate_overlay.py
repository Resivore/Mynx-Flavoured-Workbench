"""Reproduce the reviewed 1.12.5 overlay and its effective matrix from immutable inputs."""
from __future__ import annotations

import argparse
import csv
import hashlib
import io
import json
import os
import re
import subprocess
from collections import Counter, defaultdict
from copy import deepcopy
from pathlib import Path
from zipfile import ZipFile

PROJECT = Path(__file__).resolve().parent
ROOT = PROJECT.parents[1]
PACK = PROJECT / "src/main/resources/resourcepacks/mynx_matcha_trade_tweaks"
if os.name == "nt":
    PACK = Path("\\\\?\\" + str(PACK))
BASELINE = "fc5295af0b46e2c8aa93d3fc5464b1951a5467fe"
ARCHIVE = ROOT / "projects/matcha-flavoured-data/upstream/MF_1_12_5_26_2.zip"
OLD = "projects/matcha-flavoured-data/src/main/resources/resourcepacks/matcha_flavoured_1_12/"
CUSTOM = "projects/mynx-matcha-trade-tweaks/src/main/resources/resourcepacks/mynx_matcha_trade_tweaks/"
KINDS = ("trade_set", "tags/villager_trade", "villager_trade")
STACKS = ("wants", "additional_wants", "gives")
REPORT = PROJECT / "tools/trade_reconciliation.tsv"


def trade_path(key):
    namespace, path = key.split(":")
    return f"data/{namespace}/villager_trade/{path}.json"


def canonical(obj):
    return json.dumps(obj, ensure_ascii=False, sort_keys=True, separators=(",", ":"))


def legacy():
    # Read both exact tracked trees at the last main before this migration.
    args = [prefix + "data/minecraft/" + kind for prefix in (OLD, CUSTOM) for kind in KINDS]
    blob = subprocess.check_output(["git", "-c", f"safe.directory={ROOT.as_posix()}",
                                    "archive", "--format=zip", BASELINE, "--", *args], cwd=ROOT)
    result = {}
    with ZipFile(io.BytesIO(blob)) as archive:
        for prefix in (OLD, CUSTOM):
            for name in archive.namelist():
                if name.startswith(prefix) and name.endswith(".json"):
                    result[name[len(prefix):]] = json.loads(archive.read(name))
    return result


def pools(data):
    result = {}
    for name, definition in data.items():
        if name.startswith("data/minecraft/trade_set/"):
            namespace, pool = definition["trades"][1:].split(":")
            path = f"data/{namespace}/tags/villager_trade/{pool}.json"
            members = data[path]["values"]
            assert len(members) == len(set(members)), path
            result[name] = (path, members, definition["amount"])
    assert len(result) == 68
    return result


def keep(old, new, key):
    # Preserve all economic fields. Use successor components for corresponding
    # semantic items, including gems and equipment whose underlying ID changed.
    result = deepcopy(new)
    for name in ("xp", "max_uses", "reputation_discount"):
        if name in old:
            result[name] = old[name]
        else:
            result.pop(name, None)
    for name in STACKS:
        if name not in old:
            result.pop(name, None)
            continue
        prior = old[name]
        successor = new.get(name, {})
        if not prior.get("components") and prior["id"] != successor.get("id"):
            result[name] = deepcopy(prior)
            if name == "gives":
                result.pop("given_item_modifiers", None)
        else:
            result[name] = deepcopy(successor)
            if prior.get("count", 1) != successor.get("count", 1):
                result[name]["count"] = prior.get("count", 1)
    if "asylum_seeker" in key:
        # Nine fixed age/culture/cause applications. The new random item modifier
        # would replace those choices after the trade is generated.
        result = deepcopy(old)
        components = result["gives"]["components"]
        current = new["gives"]["components"]
        for name in ("minecraft:item_model", "minecraft:item_name", "minecraft:custom_data"):
            components[name] = current[name]
    return result


def modify(trade, key, archive):
    result = deepcopy(trade)
    if key == "librarian/1/paradise_lost":
        result["gives"] = json.loads(archive.read("data/matcha/recipe/crafting/divine_fragment.json"))["result"]
        result["gives"]["count"] = 1
    elif key == "shepherd/2/shepherds_shears":
        result["wants"]["count"] = 8
    elif key == "mason/1/dirt":
        result["gives"]["count"] = 2
    elif key == "mason/3/bulk_cinnabar":
        result["gives"] = result["gives"]["components"]["use_remainder"]
        assert result["gives"] == {"id": "minecraft:cinnabar", "count": 64}
    else:
        raise ValueError(key)
    return result


def reconcile(archive):
    before = legacy()
    upstream = {name: json.loads(archive.read(name)) for name in archive.namelist()
                if name.endswith(".json") and any("/" + kind + "/" in name for kind in KINDS)}
    prior_pools, new_pools = pools(before), pools(upstream)
    old_set = {key: name for name, (_, members, _) in prior_pools.items() for key in members}
    new_set = {key: name for name, (_, members, _) in new_pools.items() for key in members}
    with (PROJECT / "tools/review.tsv").open(encoding="utf-8") as stream:
        rows = list(csv.DictReader(stream, delimiter="\t"))
    assert Counter(r["decision"] for r in rows) == {"Keep mine": 214, "Adopt 1.12.5": 66, "Modify": 4, "Remove": 23}
    assert set(old_set) <= {"minecraft:" + r["legacy_key"] for r in rows}
    assert set(new_set) <= {"matcha:" + r["upstream_key"] for r in rows}
    expected = defaultdict(list)
    output = {}
    report = io.StringIO()
    writer = csv.writer(report, delimiter="\t", lineterminator="\n")
    writer.writerow(("row", "decision", "legacy", "upstream", "effective_set", "effective_id",
                     "wants", "additional_wants", "gives", "max_uses", "xp", "definition_sha256"))
    for row in rows:
        choice, old, new = (row[k] for k in ("decision", "legacy_key", "upstream_key"))
        old_id, new_id = "minecraft:" + old, "matcha:" + new
        key, group, effective = "", "", None
        if choice == "Keep mine" and old_id in old_set:
            group = old_set[old_id]
            prior = before[trade_path(old_id)]
            if new and "asylum_seeker" not in old:
                key = new_id
            else:
                key = "mynx_matcha_trade_tweaks:" + old
            effective = keep(prior, upstream[trade_path(new_id)], old) if new else deepcopy(prior)
            if old.startswith("butcher/") and old.split("/")[-1] in ("sweet_berry_toast", "warped_stroganoff", "chorus_mochi"):
                recipe = "data/matcha/recipe/food/crafting/" + old.split("/")[-1] + ".json"
                effective["gives"] = dict(json.loads(archive.read(recipe))["result"], count=prior["gives"]["count"])
        elif choice in ("Adopt 1.12.5", "Modify"):
            key, group = new_id, new_set[new_id]
            target = upstream[trade_path(new_id)]
            effective = target if choice == "Adopt 1.12.5" else modify(target, new, archive)
        fields = [""] * 5 + ["-"]
        if effective is not None:
            assert key not in expected[group], key
            expected[group].append(key)
            path = trade_path(key)
            if effective != upstream.get(path):
                output[path] = effective
            fields = [canonical(effective[k]) if k in effective else "" for k in STACKS] + [
                effective.get("max_uses", ""), effective.get("xp", ""),
                hashlib.sha256(canonical(effective).encode()).hexdigest()]
        writer.writerow([row["row"], choice, old, new, group, key, *fields])
    for name, (path, members, amount) in new_pools.items():
        wanted = expected[name]
        if set(wanted) != set(members):
            output[path] = {"replace": True, "values": wanted}
        _, previous, selections = prior_pools[name]
        count = len(wanted) if selections >= len(previous) else min(len(wanted), selections)
        # An empty named holder set draws no offers even on codecs requiring positive amount.
        if wanted and min(amount, len(wanted)) != count:
            output[name] = dict(upstream[name], amount=float(count))
    actual = pools(upstream | output)
    for group, (_, members, _) in actual.items():
        assert set(members) == set(expected[group]), group
        for key in members:
            assert trade_path(key) in (output.keys() | upstream.keys()), key
    return output, report.getvalue()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--write", action="store_true")
    args = parser.parse_args()
    assert hashlib.sha256(ARCHIVE.read_bytes()).hexdigest() == "966843d0aeb3860479721ef34791a631e62bc225fdaa918dd2ad90c49977e5b4"
    with ZipFile(ARCHIVE) as archive:
        output, report = reconcile(archive)
    existing = {file.relative_to(PACK).as_posix(): file for file in (PACK / "data").rglob("*.json")
                if any("/" + kind + "/" in file.as_posix() for kind in KINDS)}
    if args.write:
        for name in existing.keys() - output.keys():
            existing[name].unlink()
        for name, value in output.items():
            path = PACK / name
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8", newline="\n")
        REPORT.write_text(report, encoding="utf-8", newline="\n")
    else:
        assert existing.keys() == output.keys()
        for name, path in existing.items():
            assert json.loads(path.read_bytes()) == output[name], name
        assert REPORT.read_text(encoding="utf-8") == report
    print(f"307 decisions verified across all 68 active sets; {len(output)} resource overrides.")


if __name__ == "__main__":
    main()
