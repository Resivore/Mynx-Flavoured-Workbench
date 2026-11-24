"""Verify the coordinated Matcha 1.12.5 stack, repaired references and reviewed offers."""
import argparse
import hashlib
import json
import re
import sys
import subprocess
from collections import Counter
from pathlib import Path
from zipfile import ZipFile

ROOT = Path(__file__).resolve().parents[1]
PROJECTS = ROOT / "projects"
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
    subprocess.run([sys.executable, "-B", str(PROJECTS / "mynx-matcha-trade-tweaks/generate_overlay.py")], check=True)
    for kind in ("plains", "desert", "savanna", "snowy", "taiga"):
        path = f"data/minecraft/worldgen/structure/village_{kind}.json"
        assert pack.read(path) == legacy.read(path)
    path = "data/minecraft/worldgen/structure_set/villages.json"
    assert json.loads(pack.read(path)) == json.loads(legacy.read(path))
    for project in CONSUMERS:
        for path in (PROJECTS / project / "src/main").rglob("*"):
            if path.suffix in (".java", ".json", ".mcfunction"):
                source = path.read_text(encoding="utf-8")
                if project == "matcha-frost-protection":
                    # 1.12.5 retains this registry key to upgrade existing enchanted armour.
                    assert "data/main/enchantment/freezing_protection.json" in members(pack)
                    source = source.replace('"main:freezing_protection"', '"saved_frost"')
                assert not re.search(r'[" ](?:main|blessings|food|crafting|smithing_table|smoking|smelting):',
                                     source), path
    print("Five village definitions and placement set verified.")


def check_acquisition_odds(jar, legacy):
    # Effective weights are max(0, floor(weight + quality * luck)). Entries with
    # quality zero can be combined at every luck value. Keep other terms separate.
    def denominator(entries):
        constant = sum(e.get("weight", 1) for e in entries if not e.get("quality"))
        variable = sorted((e.get("weight", 1), e["quality"]) for e in entries if e.get("quality"))
        return constant, variable

    for table, old_target, target in (
        ("ancient_city", "minecraft:echo_shard", "minecraft:echo_shard"),
        ("trial_chambers/reward_ominous_unique", "minecraft:kleis_items/crystal_heart",
         "matcha:treasure/crystal_heart"),
    ):
        path = f"data/minecraft/loot_table/chests/{table}.json"
        before = json.loads(legacy.read(path))["pools"][0]
        after = json.loads(jar.read("resourcepacks/heart_progression/" + path))["pools"][0]
        assert {k: v for k, v in before.items() if k != "entries"} == {
            k: v for k, v in after.items() if k != "entries"}
        assert denominator(before["entries"]) == denominator(after["entries"])
        def selected(pool, name):
            entry, = (e for e in pool["entries"] if name in (e.get("name"), e.get("value")))
            return {k: v for k, v in entry.items() if k not in ("value", "name")}
        assert selected(before, old_target) == selected(after, target)
    print("Roll counts, selected entries and effective Luck-dependent denominators equal the 1.12 baseline.")


def artifact(project):
    directory = PROJECTS / project
    manifest = directory / "WORKBENCH_STATUS.json"
    if manifest.is_file():
        current = json.loads(manifest.read_bytes())["state"]["releases"]["current"]
        if current:
            retained = directory / "artifacts" / current["artifact"]["filename"]
            if retained.is_file():
                assert digest(retained) == current["artifact"]["sha256"], retained
                return retained
    built = [path for path in (directory / "build/libs").glob("*.jar") if not path.name.endswith("-sources.jar")]
    if len(built) != 1:
        raise AssertionError(f"Expected one built JAR or the retained current artifact for {project}")
    return built[0]


def check_built_stack(pack, legacy):
    projects = (*CONSUMERS, "matcha-flavoured-data", "matcha-intrinsic-update-compat", "ribbits-26.2", "mynx-matcha-trade-tweaks")
    providers = []
    for project in projects:
        with ZipFile(artifact(project)) as jar:
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
                check_acquisition_odds(jar, legacy)
                recipe = "data/matcha/recipe/crafting/crystal_heart.json"
                assert jar.read(recipe) == jar.read("resourcepacks/heart_progression/" + recipe)
    assert providers == ["matcha-flavoured-data"], providers
    print("Eleven built JARs checked against the exact upstream and retained offer fixtures.")


def check_effective_data(publisher, minecraft):
    """Resolve the packaged Matcha references against 26.2 and the actual higher-priority resources."""
    # Vanilla files provide the registry reference universe. Only Matcha-owned or
    # coordinated overrides need their bodies read for this verification.
    with ZipFile(minecraft) as vanilla:
        vanilla_paths = {n for n in members(vanilla) if n.startswith("data/") and n.count("/") >= 2}
        village_data = {n: json.loads(vanilla.read(n)) for n in vanilla_paths
                        if re.fullmatch(r"data/minecraft/worldgen/structure/village_(plains|desert|savanna|snowy|taiga)\.json", n)}
    data = {}
    def overlay(jar, prefix):
        meta = prefix + "pack.mcmeta"
        if meta in jar.NameToInfo:
            for rule in json.loads(jar.read(meta)).get("filter", {}).get("block", []):
                ns = re.compile(rule.get("namespace", ".*"))
                path = re.compile(rule.get("path", ".*"))
                def blocked(name):
                    _, namespace, relative = name.split("/", 2)
                    return ns.fullmatch(namespace) and path.fullmatch(relative)
                vanilla_paths.difference_update([n for n in vanilla_paths if blocked(n)])
                for n in list(data):
                    if blocked(n):
                        del data[n]
        for n in members(jar):
            if n.startswith(prefix + "data/"):
                data[n[len(prefix):]] = jar.read(n)

    overlay(publisher, "")
    layers = (
        ("matcha-flavoured-data", "resourcepacks/advancement_repairs/"),
        ("matcha-intrinsic-update-compat", ""),
        ("matcha-frost-protection", ""),
        ("coal-consolidation", "resourcepacks/coal_recipes/"),
        ("enderscape-integration", "resourcepacks/enderscape_integration/"),
        ("matcha-heart-death-compat", "resourcepacks/heart_progression/"),
        ("mynx-matcha-trade-tweaks", "resourcepacks/mynx_matcha_trade_tweaks/"),
        ("matcha-vanilla-village-restoration", "resourcepacks/vanilla_villages/"),
    )
    for project, prefix in layers:
        with ZipFile(artifact(project)) as jar:
            overlay(jar, prefix)
    for name, vanilla_value in village_data.items():
        restored = json.loads(data[name])
        key = Path(name).stem.removeprefix("village_")
        biomes = {"desert": ["desert"], "plains": ["plains", "sunflower_plains"],
                  "savanna": ["savanna"], "snowy": ["snowy_plains"],
                  "taiga": ["taiga", "old_growth_pine_taiga", "old_growth_spruce_taiga"]}
        assert restored == dict(vanilla_value, biomes=["minecraft:" + b for b in biomes[key]]), name
    assert len(village_data) == 5
    available = vanilla_paths | data.keys()
    def reference(kind, key, source):
        ns, path = (key if ":" in key else "minecraft:" + key).split(":", 1)
        extension = ".mcfunction" if kind == "function" else ".json"
        assert f"data/{ns}/{kind}/{path}{extension}" in available, (source, kind, key)

    def loot_references(value, source):
        if isinstance(value, list):
            for element in value:
                loot_references(element, source)
        if isinstance(value, dict):
            if value.get("type") in ("loot_table", "minecraft:loot_table") and isinstance(value.get("value"), str):
                reference("loot_table", value["value"], source)
            if value.get("condition") in ("reference", "minecraft:reference"):
                reference("predicate", value["name"], source)
            for nested in value.values():
                loot_references(nested, source)

    for name, raw in data.items():
        if name.endswith(".mcfunction"):
            for line in raw.decode().splitlines():
                if line.lstrip().startswith("#") or "$(" in line:
                    continue
                for key in re.findall(r"(?:^|run |schedule )function ([\w.-]+:[\w./-]+)", line):
                    reference("function", key, name)
        elif name.endswith(".json"):
            obj = json.loads(raw)
            if "/loot_table/" in name or "/predicate/" in name:
                loot_references(obj, name)
            if name.startswith("data/matcha/advancement/"):
                rewards = obj.get("rewards", {})
                if "parent" in obj:
                    reference("advancement", obj["parent"], name)
                if "function" in rewards:
                    reference("function", rewards["function"], name)
                for key in rewards.get("recipes", []):
                    reference("recipe", key, name)
                for key in rewards.get("loot", []):
                    reference("loot_table", key, name)
                for criterion in obj.get("criteria", {}).values():
                    if criterion.get("trigger") == "minecraft:recipe_unlocked":
                        reference("recipe", criterion["conditions"]["recipe"], name)
    favorite = "data/matcha/function/favorite_food/"
    entry_points = {favorite + n + ".mcfunction" for n in ("favorite_food_setup", "trader_update_init")}
    assert {n for n in data if n.startswith(favorite)} == entry_points
    for name in entry_points:
        assert [l for l in data[name].decode().splitlines() if l and not l.startswith("#")] == ["return 0"]
    assert "data/matcha/villager_trade/librarian/5/crystal_heart.json" not in data
    assert json.loads(data["data/matcha/tags/villager_trade/librarian/custom_level_5.json"])["values"] == []
    # The active tags must resolve to the exact definitions in the reconciliation report.
    import csv
    with (PROJECTS / "mynx-matcha-trade-tweaks/tools/trade_reconciliation.tsv").open(encoding="utf-8") as stream:
        rows = list(csv.DictReader(stream, delimiter="\t"))
    expected = {(row["effective_set"], row["effective_id"]): row["definition_sha256"]
                for row in rows if row["effective_id"]}
    actual = {}
    for name in members(publisher):
        if name.startswith("data/minecraft/trade_set/") and name.endswith(".json"):
            trade_set = json.loads(data[name])
            ns, tag = trade_set["trades"][1:].split(":")
            assert ns == "matcha"
            offers = json.loads(data[f"data/{ns}/tags/villager_trade/{tag}.json"])["values"]
            assert len(offers) == len(set(offers)), name
            definitions = set()
            for offer in offers:
                ns, key = offer.split(":")
                trade = json.loads(data[f"data/{ns}/villager_trade/{key}.json"])
                payload = json.dumps(trade, ensure_ascii=False, separators=(",", ":"), sort_keys=True).encode()
                fingerprint = hashlib.sha256(payload).hexdigest()
                assert fingerprint not in definitions, (name, offer)
                definitions.add(fingerprint)
                actual[name, offer] = fingerprint
    assert actual == expected and len(actual) == 280 and len(rows) == 307
    print("Packaged references, six slot writers, 68 effective pools, 280 offers, 42 filtered food writers and five vanilla villages verified.")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--official-mod", type=Path, required=True)
    parser.add_argument("--client-pack", type=Path, required=True)
    parser.add_argument("--foundation", type=Path, required=True)
    parser.add_argument("--minecraft-jar", type=Path, default=Path.home() / ".gradle/caches/fabric-loom/26.2/minecraft-merged.jar")
    args = parser.parse_args()
    assert digest(ARCHIVE) == "966843d0aeb3860479721ef34791a631e62bc225fdaa918dd2ad90c49977e5b4"
    assert digest(args.official_mod) == "865ae19b98022775c4bb0995c9e6b10812cc5041fd2a38973f1ed6da2ff64b11"
    assert digest(args.client_pack) == "6b76122a3db1f299816efb3627e5a28f7f121a4edbd33e4de19ca9427955a8b7"
    assert json.loads(ZipFile(args.foundation).read("pack.mcmeta"))["pack"]["description"] == "Foundation v13"
    with ZipFile(ARCHIVE) as pack, ZipFile(args.official_mod) as mod:
        assert all(mod.read(name) == pack.read(name) for name in members(pack))
        assert json.loads(mod.read("fabric.mod.json"))["id"] == "mr_matcha_flavoured"
        intrinsic_contract(pack)
        check_effective_data(pack, args.minecraft_jar)
        with ZipFile(ROOT / "originals/datapacks/Matcha_Flavoured_1_12.zip") as legacy:
            check_boundaries(pack, legacy)
            check_built_stack(pack, legacy)
    with ZipFile(args.client_pack) as client:
        for model in ("heart_container", "beacon_kindling", "green_curry", "ramen"):
            assert f"assets/matcha/items/{model}.json" in members(client)
        for path, expected in ((args.foundation, (183, 171)),
                               (ROOT / "originals/assets/Matcha-Overlays-v37.zip", (56, 56))):
            with ZipFile(path) as overlay:
                common = {n for n in members(client) & members(overlay) if n.startswith("assets/")}
                different = sum(client.read(n) != overlay.read(n) for n in common)
                assert (len(common), different) == expected
                print(f"{path.name}: {len(common)} shared asset paths, {different} differing.")


if __name__ == "__main__":
    main()
