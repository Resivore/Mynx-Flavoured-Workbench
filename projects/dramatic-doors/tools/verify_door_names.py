"""Check packaged names against the registration inventory from Get-RegisteredItemIds.ps1."""

import argparse
import csv
import json
import os
from pathlib import Path
import re
import subprocess
import sys
from zipfile import ZipFile

ASSETS = "assets/dramaticdoors/"
LOCALES = ("en_us", "ru_ru", "zh_cn")


def factory_prefixes(jar):
    """Inspect the named production methods, including both public legacy overloads."""
    java_home = os.environ.get("JAVA_HOME")
    javap = str(Path(java_home, "bin", "javap")) if java_home else "javap"
    prefixes = {}
    for cls, factories in (
        ("registry.DDRegistry", {"queueNativeDoor": "native", "createDoorItem": "legacy"}),
        ("fabric.AmcMacawDoors", {"register": "amc"}),
    ):
        dump = subprocess.check_output(
            [javap, "-classpath", str(jar), "-c", "-p", "com.fizzware.dramaticdoors." + cls],
            encoding="utf-8", errors="replace",
        )
        for name, group in factories.items():
            methods = re.findall(r"^  [^\n]+ " + name + r"\([^\n]*\);\n(.*?)(?=^  \S|^\})",
                                 dump, re.MULTILINE | re.DOTALL)
            expected_count = 2 if group == "legacy" else 1
            if len(methods) != expected_count:
                raise ValueError(f"Review changed factory signatures in {cls}.{name}")
            direct = set()
            for body in methods:
                if "class net/minecraft/world/item/BlockItem" in body:
                    direct.add("block" if ".useBlockDescriptionPrefix:" in body else "item")
                elif group != "legacy" or "// Method createDoorItem:" not in body:
                    raise ValueError(f"Unrecognized item construction in {cls}.{name}")
            if len(direct) != 1:
                raise ValueError(f"Inconsistent description prefixes in {cls}.{name}")
            prefixes[group] = direct.pop()
    return prefixes


def read_catalog(jar, groups):
    prefixes = factory_prefixes(jar)
    with ZipFile(jar) as archive:
        paths = set(archive.namelist())
        locales = {path: json.loads(archive.read(path)) for path in paths if
                   re.fullmatch(r"(?:resourcepacks/[^/]+/)?assets/dramaticdoors/lang/[^/]+\.json", path)}
        doors = {}
        for path in sorted(paths):
            match = re.fullmatch(r"(resourcepacks/[^/]+/)?" + ASSETS + r"items/([^/]+)\.json", path)
            if not match:
                continue
            pack, door = match.groups()
            if door in doors or door not in groups:
                raise ValueError(f"Unexpected item definition {path}")
            folder = (pack or "") + ASSETS
            if folder + "blockstates/" + door + ".json" not in paths:
                raise ValueError(f"Missing blockstates for {path}")
            block_key = "block.dramaticdoors." + door
            item_key = prefixes[groups[door]] + ".dramaticdoors." + door
            names = {}
            for locale in LOCALES:
                language = "lang/" + locale + ".json"
                strings = locales[ASSETS + language]
                pack_strings = locales.get(folder + language, {})
                names[locale] = pack_strings.get(block_key, strings.get(block_key, ""))
            doors[door] = (item_key, block_key, names)
    if doors.keys() != groups.keys():
        raise ValueError(f"Missing packaged definitions: {sorted(groups.keys() - doors.keys())}")
    return doors


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("jar", type=Path)
    parser.add_argument("--previous", type=Path)
    parser.add_argument("--report", type=Path)
    args = parser.parse_args()
    groups = dict(line.strip().split("\t") for line in sys.stdin if line.strip())
    if not groups:
        parser.error("Supply the registration inventory on stdin")
    doors = read_catalog(args.jar, groups)
    previous = read_catalog(args.previous, groups) if args.previous else {}
    rows = []
    for door in sorted(doors):
        item, block, names = doors[door]
        if item != block or any(not s.strip() or s.startswith(("block.", "item.")) for s in names.values()):
            raise ValueError(f"Unresolved name for {door}: item={item}, block={block}, translations={names}")
        old_item, _, old_names = previous.get(door, ("", "", {}))
        added = ",".join(k for k in LOCALES if previous and not old_names[k])
        rows.append(("dramaticdoors:" + door, groups[door], old_item, item, block, *names.values(), added))
    if args.report:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        with args.report.open("w", encoding="utf-8", newline="") as out:
            writer = csv.writer(out, delimiter="\t", lineterminator="\n")
            writer.writerow(("registry_id", "factory", "previous_item_key", "item_key", "block_key", *LOCALES, "locales_added"))
            writer.writerows(rows)
    print(f"Verified packaged item and block names for {len(doors)} registrations in {', '.join(LOCALES)}.")


if __name__ == "__main__":
    main()
