"""Verify C10's packaged model graph against the actual 26.2 Minecraft and provider JARs."""
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
        for value in textures.values():
            sprite(value)
        for element in elements:
            for face in element["faces"].values():
                sprite(face["texture"])
        return actual, elements

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
                lower = f"{provider}:block/{material}_door_bottom"
                upper = f"{provider}:block/{material}_door_top"
                assert sprites == item_sprites == {lower, upper}, model
                # Read the provider's actual lower and upper model, including its mappings.
                for half in ("bottom", "top"):
                    source = json.loads(read(provider, f"models/block/{material}_door_{half}_left.json"))
                    assert source["textures"] == {"bottom": lower, "top": upper}
                faces = [e["faces"]["west"] for e in elements]
                expected = ([("#bottom", 0, 16), ("#top", 0, 16)] if height == "short" else
                            [("#bottom", 0, 5), ("#top", 11, 16)] if "_middle_" in model else
                            [("#top", 0, 11)] if "_top_" in model else [("#bottom", 5, 16)])
                assert [(f["texture"], *f["uv"][1::2]) for f in faces] == expected, model
        if optional:
            assert f"assets/dramaticdoors/items/{name}.json" not in candidate.NameToInfo
            assert f"{prefix}data/dramaticdoors/loot_table/blocks/{name}.json" in candidate.NameToInfo


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
        assert not any(n.endswith(".png") for n in names - set(previous.namelist()))
        for n in previous.namelist():
            if n.startswith("assets/") and "/lang/" not in n:
                assert candidate.read(n) == previous.read(n), n
        for material, provider in (("pale_oak", "minecraft"), ("veiled", "enderscape"),
                                   ("celestial", "enderscape"), ("murushroom", "enderscape"),
                                   ("mossy_oak", "ribbits")):
            check(material, provider, packs)
    print("10 item models, 320 state selections, 80 block models and all referenced textures resolve; C9 artwork preserved.")


if __name__ == "__main__":
    main()
