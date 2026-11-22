"""Verify C11's packaged model graph against the actual 26.2 Minecraft and provider JARs."""
import argparse
from contextlib import ExitStack
import json
from zipfile import ZipFile


def check(material, provider, packs):
    candidate = packs["dramaticdoors"]
    optional = material in {"veiled", "mossy_oak"}
    prefix = f"resourcepacks/{material}_door/" if optional else ""

    def read(namespace, path):
        archive = packs[namespace]
        name = f"assets/{namespace}/{path}"
        if namespace == "dramaticdoors" and prefix + name in archive.NameToInfo:
            name = prefix + name
        return archive.read(name)

    def location(value):
        namespace, _, path = value.rpartition(":")
        return namespace or "minecraft", path

    def resolve(model_id):
        textures = {}
        elements = []
        visited = set()
        while model_id:
            ns, path = location(model_id)
            if path.startswith("builtin/"):
                break
            assert model_id not in visited, model_id
            visited.add(model_id)
            model = json.loads(read(ns, f"models/{path}.json"))
            for key, value in model.get("textures", {}).items():
                textures.setdefault(key, value)
            if not elements:
                elements = model.get("elements", [])
            model_id = model.get("parent")
        actual = set()
        def sprite(value):
            aliases = set()
            while value.startswith("#"):
                assert value not in aliases, value
                aliases.add(value)
                value = textures[value[1:]]
            ns, path = location(value)
            assert read(ns, f"textures/{path}.png").startswith(b"\x89PNG"), value
            actual.add(value)
            return value
        for value in textures.values():
            sprite(value)
        sampled = {sprite(face["texture"]) for element in elements for face in element["faces"].values()}
        return sampled or actual, elements

    language = json.loads(read("dramaticdoors", "lang/en_us.json"))
    for height, states in (("short", 16), ("tall", 48)):
        name = f"{height}_{material}_door"
        blockstate = json.loads(read("dramaticdoors", f"blockstates/{name}.json"))["variants"]
        assert len(blockstate) == states, name
        models = {v["model"] for v in blockstate.values()}
        assert len(models) == states // 4, name
        definition = json.loads(read("dramaticdoors", f"items/{name}.json"))["model"]
        assert definition == {"type": "minecraft:model", "model": "dramaticdoors:item/" + name}
        assert language[f"block.dramaticdoors.{name}"] == (
            f"{height} {'murublight' if material == 'murushroom' else material} door".replace("_", " ").title())
        for locale in ("ru_ru", "zh_cn"):
            assert f"block.dramaticdoors.{name}" in json.loads(read("dramaticdoors", f"lang/{locale}.json"))
        category = {"minecraft": "vanilla", "ribbits": "ribbits", "enderscape": "enderscape"}[provider]
        tag_path = f"data/dramaticdoors/tags/item/categories/{height}_{category}_doors.json"
        tag = json.loads(candidate.read((prefix if provider == "ribbits" else "") + tag_path))
        assert "dramaticdoors:" + name in [v["id"] if isinstance(v, dict) else v for v in tag["values"]]
        for kind in ("block", "item"):
            tag = json.loads(candidate.read(f"data/dramaticdoors/tags/{kind}/{height}_wooden_doors.json"))
            assert {"id": "dramaticdoors:" + name, "required": False} in tag["values"]
        item_sprites, _ = resolve(definition["model"])
        for model in models:
            sprites, elements = resolve(model)
            assert elements, model
            if optional:
                path = model.split(":")[1]
                baseline = "assets/dramaticdoors/models/" + path.replace(material, "celestial") + ".json"
                expected = json.loads(candidate.read(baseline).decode().replace("celestial", material))
                assert expected == json.loads(read("dramaticdoors", "models/" + path + ".json")), model
                section = next((part for part in ("top", "middle", "bottom") if f"_{part}_" in path), None)
                texture = "dramaticdoors:block/" + name + ("_" + section if section else "")
                assert sprites == {texture}, model
                assert len(elements) == 1, model
                for side in ("west", "east"):
                    u, v, end_u, end_v = elements[0]["faces"][side]["uv"]
                    assert abs(end_u - u) == abs(end_v - v) == 16, (model, side)
        if optional:
            assert item_sprites == {"dramaticdoors:item/" + name}
            item = json.loads(read("dramaticdoors", f"models/item/{name}.json"))
            assert location(item.pop("parent")) == ("minecraft", "item/generated"), name
            assert item == {"textures": {"layer0": "dramaticdoors:item/" + name}}, name
            baseline = f"assets/dramaticdoors/blockstates/{height}_celestial_door.json"
            expected = json.loads(candidate.read(baseline).decode().replace("celestial", material))["variants"]
            def normalized(variants):
                return {tuple(sorted(k.split(","))): v for k, v in variants.items()}
            assert normalized(blockstate) == normalized(expected), name
            for half in ("top", "bottom"):
                source = json.loads(read(provider, f"models/block/{material}_door_{half}_left.json"))
                assert source["textures"] == {k: f"{provider}:block/{material}_door_{k}" for k in ("top", "bottom")}
            assert f"assets/dramaticdoors/items/{name}.json" not in candidate.NameToInfo
            assert f"{prefix}data/dramaticdoors/loot_table/blocks/{name}.json" in candidate.NameToInfo


def compare_archives(candidate, previous):
    def files(jar):
        entries = [entry.filename for entry in jar.infolist() if not entry.is_dir()]
        assert len(entries) == len(set(entries)), "Duplicate archive entry"
        return set(entries)

    new = files(candidate)
    old = files(previous)
    added, removed, changed = set(), set(), {"fabric.mod.json"}
    for height in ("short", "tall"):
        removed.add(f"assets/dramaticdoors/models/item/native_{height}_door.json")
    for section in ("short", "top", "middle", "bottom"):
        for hinge in ("left", "right"):
            for opened in ("", "_open"):
                removed.add(f"assets/dramaticdoors/models/block/native_door/{section}_{hinge}{opened}.json")
    for material in ("veiled", "mossy_oak"):
        root = f"resourcepacks/{material}_door/assets/dramaticdoors/"
        for height in ("short", "tall"):
            name = f"{height}_{material}_door"
            changed.add(root + f"models/item/{name}.json")
            added.add(root + f"textures/item/{name}.png")
            for suffix in (("",) if height == "short" else ("_top", "_middle", "_bottom")):
                added.add(root + f"textures/block/{name}{suffix}.png")
                for hinge in ("left", "right"):
                    for opened in ("", "_open"):
                        changed.add(root + f"models/block/{name}{suffix}_{hinge}{opened}.json")
    actual = {n for n in new & old if previous.read(n) != candidate.read(n)}
    assert actual == changed, sorted(actual ^ changed)
    assert new - old == added, sorted((new - old) ^ added)
    assert old - new == removed, sorted((old - new) ^ removed)
    before, after = (json.loads(jar.read("fabric.mod.json")) for jar in (previous, candidate))
    assert before.pop("version").endswith("canary10") and after.pop("version").endswith("canary11")
    assert before == after
    print(f"{len(new & old) - len(actual):,} identical payloads; 36 model replacements, "
          "12 new sprites, 18 retired model templates and the Fabric version change.")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for key in ("dramaticdoors", "minecraft", "enderscape", "ribbits", "previous"):
        parser.add_argument(key)
    args = vars(parser.parse_args())
    with ExitStack() as stack:
        packs = {key: stack.enter_context(ZipFile(path)) for key, path in args.items()}
        candidate = packs["dramaticdoors"]
        previous = packs["previous"]
        names = set(candidate.namelist())
        assert not any(n.startswith(("assets/ribbits/", "assets/enderscape/")) for n in names)
        compare_archives(candidate, previous)
        for material, provider in (("pale_oak", "minecraft"), ("veiled", "enderscape"),
                                   ("celestial", "enderscape"), ("murushroom", "enderscape"),
                                   ("mossy_oak", "ribbits")):
            check(material, provider, packs)
    print("10 item models, 320 state selections, 80 block models and all referenced textures resolve; all four corrected state graphs match Celestial geometry and face UVs.")


if __name__ == "__main__":
    main()
