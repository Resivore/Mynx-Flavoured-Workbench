"""Verify the Stage 1 distribution, slot-update contracts and deferred migration boundaries."""
import argparse
import hashlib
import json
import re
import runpy
import subprocess
from collections import Counter
from pathlib import Path
from zipfile import ZipFile

ROOT = Path(__file__).resolve().parents[1]
PROJECTS = ROOT / "projects"
BASELINE = "c5172fee26245bfb71b768f8dc5a89c1c7c0a1fd"
PROVIDER = PROJECTS / "matcha-flavoured-data"
ARCHIVE = PROVIDER / "upstream/MF_1_12_5_26_2.zip"
INTRINSIC = PROJECTS / "matcha-intrinsic-update-compat/src/main/resources"
CONSUMERS = ("coal-consolidation", "enderscape-integration", "inventory-particles-matcha-compat",
             "matcha-heart-death-compat", "matcha-frost-protection", "matcha-jei-integration",
             "matcha-beacon-kindling-compat")


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def members(pack):
    return {name for name in pack.namelist() if not name.endswith("/")}


def intrinsic_contract(pack):
    counts = Counter()
    smithing_calls = 0
    for path in members(pack):
        if not path.endswith(".mcfunction"):
            continue
        body = pack.read(path).decode()
        for slot in re.findall(r"^function matcha_item:enchants/(\w+) with storage matcha_item:enchants", body, re.M):
            counts[slot] += 1
            target = f"data/matcha_item/function/enchants/{slot}.mcfunction"
            assert target not in pack.namelist()
            slot_name = ("weapon." if slot.endswith("hand") else "armor.") + slot
            writer = (INTRINSIC / target).read_text().splitlines()
            assert writer[0].startswith(f"$item modify entity @s {slot_name} ")
            assert "'minecraft:enchantments': $(held)" in writer[0]
            assert writer[1:] == ["function matcha_intrinsic_update_compat:clear_enchantment_scratch"]
            item = Path(path).stem
            reward = json.loads(pack.read(f"data/matcha_item/advancement/trigger/{item}.json"))["rewards"]
            assert reward["function"] == f"matcha_item:update/{item}"
            if slot.endswith("hand"):
                dispatcher = pack.read(f"data/matcha_item/function/update/{item}.mcfunction").decode()
                assert f"run function matcha_item:{slot}/{item}" in dispatcher
            recipe = f"data/matcha/recipe/smithing/{item}.json"
            if recipe in pack.namelist():
                minima = {key: int(level) for key, level in re.findall(
                    r"held merge value \{'([^']+)': (\d+)\}", body)}
                seeded = json.loads(pack.read(recipe))["result"]["components"]["minecraft:stored_enchantments"]
                assert minima == seeded, path
                smithing_calls += 1
    assert counts == {"mainhand": 54, "offhand": 54, "feet": 9, "chest": 8, "head": 12, "legs": 9}
    assert smithing_calls == 78
    print("146 updater calls resolved. All 78 calls with a same-name smithing recipe agree on minimum levels.")


def check_boundaries(pack, legacy):
    protected = ["projects/mynx-matcha-trade-tweaks", "resourcepacks/foundation", "resourcepacks/enderscape-overlays"]
    subprocess.run(["git", "-c", f"safe.directory={ROOT}", "diff", "--exit-code", BASELINE, "--", *protected],
                   cwd=ROOT, check=True)
    trades = runpy.run_path(str(PROJECTS / "mynx-matcha-trade-tweaks/generate_overlay.py"))
    assert len(trades["CHANGES"]) == 124 and len(trades["REMOVALS"]) == 4
    assert trades["POOL_MEMBERSHIP"][("leatherworker", 5)]["add"] == ["red_mushroom_block"]
    for kind in ("plains", "desert", "savanna", "snowy", "taiga"):
        path = f"data/minecraft/worldgen/structure/village_{kind}.json"
        assert pack.read(path) == legacy.read(path)
    path = "data/minecraft/worldgen/structure_set/villages.json"
    assert json.loads(pack.read(path)) == json.loads(legacy.read(path))
    for project in CONSUMERS:
        for path in (PROJECTS / project / "src/main").rglob("*"):
            if path.suffix in (".java", ".json", ".mcfunction"):
                assert not re.search(r'[" ](?:main|blessings|food|crafting|smithing_table|smoking|smelting):',
                                     path.read_text(encoding="utf-8")), path
    print("129 owner trade changes preserved. Five village definitions and placement set verified.")


def check_built_stack(pack, legacy):
    projects = (*CONSUMERS, "matcha-flavoured-data", "matcha-intrinsic-update-compat", "ribbits-26.2")
    providers = []
    for project in projects:
        jars = [path for path in (PROJECTS / project / "build/libs").glob("*.jar")
                if not path.name.endswith("-sources.jar")]
        assert len(jars) == 1, (project, jars)
        with ZipFile(jars[0]) as jar:
            meta = json.loads(jar.read("fabric.mod.json"))
            assert meta["id"] != "mr_matcha_flavoured"
            if "workbench_matcha_data_1_12_5" in meta.get("provides", []):
                providers.append(project)
                prefix = "resourcepacks/matcha_flavoured_1_12_5/"
                assert {n[len(prefix):] for n in members(jar) if n.startswith(prefix)} == members(pack)
                assert all(jar.read(prefix + n) == pack.read(n) for n in members(pack))
            if project == "matcha-intrinsic-update-compat":
                writers = {n for n in members(jar) if n.startswith("data/matcha_item/")}
                assert len(writers) == 6 and not writers.intersection(members(pack))
            if project == "ribbits-26.2":
                resource = "data/ribbits/trade_item/estus_flask.json"
                assert json.loads(jar.read(resource)) == json.loads(
                    legacy.read("data/crafting/recipe/estus_flask.json"))["result"]
                assert "data/ribbits/recipe/estus_flask.json" not in members(jar)
                assert meta["depends"]["matcha_heart_death_compat"] == ">=0.2.0-0"
            if project == "matcha-heart-death-compat":
                recipe = "data/matcha/recipe/crafting/crystal_heart.json"
                assert jar.read(recipe) == jar.read("resourcepacks/heart_progression/" + recipe)
    assert providers == ["matcha-flavoured-data"], providers
    print("Ten built JARs checked against the exact upstream and retained offer fixtures.")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--official-mod", type=Path, required=True)
    parser.add_argument("--client-pack", type=Path, required=True)
    parser.add_argument("--foundation", type=Path, required=True)
    args = parser.parse_args()
    assert digest(ARCHIVE) == "966843d0aeb3860479721ef34791a631e62bc225fdaa918dd2ad90c49977e5b4"
    assert digest(args.official_mod) == "865ae19b98022775c4bb0995c9e6b10812cc5041fd2a38973f1ed6da2ff64b11"
    assert digest(args.client_pack) == "6b76122a3db1f299816efb3627e5a28f7f121a4edbd33e4de19ca9427955a8b7"
    assert digest(args.foundation) == "c45fbc7b8f9b41807c59d0a6addcbf5a8a98ad5361df793cd8cecc8454a71143"
    with ZipFile(ARCHIVE) as pack, ZipFile(args.official_mod) as mod:
        assert all(mod.read(name) == pack.read(name) for name in members(pack))
        assert json.loads(mod.read("fabric.mod.json"))["id"] == "mr_matcha_flavoured"
        intrinsic_contract(pack)
        with ZipFile(ROOT / "originals/datapacks/Matcha_Flavoured_1_12.zip") as legacy:
            check_boundaries(pack, legacy)
            check_built_stack(pack, legacy)
    with ZipFile(args.client_pack) as client:
        for model in ("heart_container", "beacon_kindling", "green_curry", "ramen"):
            assert f"assets/matcha/items/{model}.json" in members(client)
        for path, expected in ((args.foundation, (165, 153)),
                               (ROOT / "originals/assets/Matcha-Overlays-v37.zip", (56, 56))):
            with ZipFile(path) as overlay:
                common = {n for n in members(client) & members(overlay) if n.startswith("assets/")}
                different = sum(client.read(n) != overlay.read(n) for n in common)
                assert (len(common), different) == expected
                print(f"{path.name}: {len(common)} shared asset paths, {different} differing.")


if __name__ == "__main__":
    main()
