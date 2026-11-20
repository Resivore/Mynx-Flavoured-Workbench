"""Check the packaged AMC release against the real Macaw and Minecraft resources."""
import hashlib
import io
import json
import sys
from pathlib import Path
from zipfile import ZipFile

from PIL import Image

from verify_masonry import COVERAGE, check_masonry

NS = "architectural_material_closure"
FORMS = ("stone_parapet", "stone_louvered_shutter")


def read_json(jar, path):
    return json.loads(jar.read(path))


def reference_id(value):
    return value.replace(NS + ":", "mcwwindows:").replace("stone_", "andesite_")


def compare_display(jar, provider, form, models):
    template = form.replace("stone_", "andesite_")
    for folder in ("blockstates", "items"):
        actual = read_json(jar, f"assets/{NS}/{folder}/{form}.json")
        expected = read_json(provider, f"assets/mcwwindows/{folder}/{template}.json")
        # All predicates, transforms and model choices must survive the ID change.
        assert json.loads(reference_id(json.dumps(actual))) == expected, (form, folder)
        if folder == "blockstates":
            entries = (actual["variants"].values() if "variants" in actual
                       else [part["apply"] for part in actual["multipart"]])
        else:
            entries = [actual["model"]]
        for entry in entries:
            namespace, name = entry["model"].split(":")
            assert namespace == NS
            models.add(name)


def compare_model(jar, provider, client, name):
    model = read_json(jar, f"assets/{NS}/models/{name}.json")
    reference = read_json(provider, f"assets/mcwwindows/models/{reference_id(name)}.json")
    assert model.keys() == reference.keys() == {"parent", "textures"}
    assert model["parent"] == reference["parent"]  # Identical inherited geometry, UVs and display transforms.
    namespace, parent_name = model["parent"].split(":")
    assert namespace == "mcwwindows"
    parent = read_json(provider, f"assets/mcwwindows/models/{parent_name}.json")
    assert parent["elements"] and "parent" not in parent
    assert model["textures"].keys() == reference["textures"].keys()
    for element in parent["elements"]:
        for face in element["faces"].values():
            assert len(face["uv"]) == 4 and face["texture"].startswith("#")
            assert face["texture"][1:] in model["textures"]
    for slot, sprite in model["textures"].items():
        if "/parapet/" in name:
            assert sprite == "minecraft:block/stone" and reference["textures"][slot] == "minecraft:block/andesite"
            archive = client
        else:
            assert sprite == NS + ":block/stone_louvered_shutter"
            assert reference["textures"][slot] == reference_id(sprite)
            archive = jar
        ns, path = sprite.split(":")
        archive.getinfo(f"assets/{ns}/textures/{path}.png")


def check_texture(jar, provider, client):
    def image(archive, ns, name):
        path = f"assets/{ns}/textures/block/{name}.png"
        return Image.open(io.BytesIO(archive.read(path))).convert("RGBA")

    source = image(provider, "mcwwindows", "andesite_louvered_shutter")
    result = image(jar, NS, "stone_louvered_shutter")
    stone = image(client, "minecraft", "stone")
    assert source.size == result.size == (16, 16)
    assert source.getchannel("A").tobytes() == result.getchannel("A").tobytes()
    assert source.size == stone.size
    # Every visible pixel is either the co-located Stone pixel or that same
    # pixel under one consistent lighting gain per structural region.
    gains = {}
    unshaded = 0
    for y in range(16):
        for x in range(16):
            pixel = result.getpixel((x, y))
            if not pixel[3]:
                assert pixel == source.getpixel((x, y))
                continue
            grey = stone.getpixel((x, y))[0]
            assert pixel[0] == pixel[1] == pixel[2]
            region = None
            if y >= 1 and 2 <= x <= 9:
                if y % 3 == 1:
                    region = "raised"
                elif y % 3 == 0:
                    region = "lip" if x == 2 else "recess"
                elif x == 9:
                    region = "return"
            if region is None:
                assert pixel == stone.getpixel((x, y)), (x, y, "Stone grain changed")
                unshaded += 1
            else:
                # Intersect the gain intervals permitted by integer rounding.
                lo, hi = gains.get(region, (0, float("inf")))
                gains[region] = (max(lo, (pixel[0] - 0.5) / grey), min(hi, (pixel[0] + 0.5) / grey))
    assert unshaded == 122
    for region, (low, high) in gains.items():
        assert low <= high, f"Source mottling leaked into {region}"
    assert 0 < gains["return"][1] < gains["lip"][0] < 1 < gains["raised"][0]
    assert 0 < gains["recess"][1] < gains["lip"][0]

    # Levels observed in the two retained shutter sprites on 127-grey Stone.
    levels_at_127 = {"return": (72, 99), "recess": (78, 103), "lip": (100, 114), "raised": (146, 146)}
    for region, (c7, c8) in levels_at_127.items():
        target = (c7 + c8) / 2
        low, high = gains[region]
        assert low * 127 <= target + 0.5 and high * 127 >= target - 0.5, region


def check_andesite(jar, provider, client, expected):
    controls = {"button": ("stone_button", 3), "pressure_plate": ("stone_pressure_plate", 2)}
    masonry = {"gothic_window": ("stone_brick_gothic", 4), "arrow_slit": ("stone_brick_arrow_slit", 4)}
    for form, (source, model_count) in (controls | masonry).items():
        archive, namespace = (client, "minecraft") if form in controls else (provider, "mcwwindows")
        name = "andesite_" + form
        own, ref = f"{NS}:{name}", f"{namespace}:{source}"
        provider_name = "andesite_gothic" if form == "gothic_window" else name
        for alias in {name, provider_name}:
            assert f"assets/mcwwindows/blockstates/{alias}.json" not in provider.namelist()
        assert f"assets/minecraft/blockstates/{name}.json" not in client.namelist()
        models = set()

        def resource(path):
            expected.add(path)
            return read_json(jar, path)

        for folder in ("items", "blockstates"):
            actual = resource(f"assets/{NS}/{folder}/{name}.json")
            reference = read_json(archive, f"assets/{namespace}/{folder}/{source}.json")
            if folder == "items":
                entries = [actual["model"]]
            elif "variants" in actual:
                entries = actual["variants"].values()
            else:
                entries = [part["apply"] for part in actual["multipart"]]
            for entry in entries:
                model = entry["model"]
                if model == "mcwwindows:block/gothic/iron_bars":
                    assert form == "gothic_window"
                    bars = read_json(provider, "assets/mcwwindows/models/block/gothic/iron_bars.json")
                    assert bars["elements"] and bars["textures"] == {"3": "minecraft:block/iron_bars"}
                    client.getinfo("assets/minecraft/textures/block/iron_bars.png")
                else:
                    prefix, path = model.split(":")
                    assert prefix == NS and name in path
                    models.add(path)
                    entry["model"] = f"{namespace}:{path.replace(name, source)}"
            assert actual == reference, (name, folder, "state predicates, transforms or item differ")
        assert len(models) == model_count
        for path in models:
            child = resource(f"assets/{NS}/models/{path}.json")
            reference = read_json(archive, f"assets/{namespace}/models/{path.replace(name, source)}.json")
            slots = {"texture": "minecraft:block/andesite"} if form in controls else {
                "0": "minecraft:block/andesite", "particle": "minecraft:block/andesite"}
            if form == "gothic_window":
                slots["3"] = "minecraft:block/glass"
            assert child == {"parent": reference["parent"], "textures": slots}
            assert reference["textures"].keys() == slots.keys()
            # Follow the original geometry and display ancestry all the way to its root.
            parent = child["parent"]
            while parent:
                ns, _, model = parent.partition(":")
                if not model:
                    ns, model = "minecraft", ns
                parent_archive = {"minecraft": client, "mcwwindows": provider}[ns]
                inherited = read_json(parent_archive, f"assets/{ns}/models/{model}.json")
                for e in inherited.get("elements", []):
                    for face in e["faces"].values():
                        assert face["texture"].removeprefix("#") in slots
                parent = inherited.get("parent")
            for texture in slots.values():
                client.getinfo("assets/minecraft/textures/" + texture.split(":")[1] + ".png")

        recipe = resource(f"data/{NS}/recipe/{name}.json")
        original = read_json(archive, f"data/{namespace}/recipe/{source}.json")
        # Compare the entire original recipe and loot structure with just the identity/material swapped.
        adapted = json.dumps(original).replace(ref, own).replace('"minecraft:stone"', '"minecraft:andesite"')
        adapted = adapted.replace("minecraft:stone_brick_slab", "minecraft:andesite_slab")
        assert recipe == json.loads(adapted.replace("minecraft:stone_bricks", "minecraft:andesite"))
        loot = resource(f"data/{NS}/loot_table/blocks/{name}.json")
        original = read_json(archive, f"data/{namespace}/loot_table/blocks/{source}.json")
        assert loot == json.loads(json.dumps(original).replace(ref, own)
                                  .replace(f"minecraft:blocks/{source}", f"{NS}:blocks/{name}"))
        category = "redstone" if form in controls else "building_blocks"
        unlock = resource(f"data/{NS}/advancement/recipes/{category}/{name}.json")
        assert unlock["rewards"]["recipes"] == [own] and unlock["criteria"]["recipe"]["conditions"]["recipe"] == own
        assert unlock["criteria"]["material"]["conditions"]["items"] == [{"items": "minecraft:andesite"}]


def verify(jar, provider, client, bbb):
    files = [e.filename for e in jar.infolist() if not e.is_dir()]
    assert len(files) == len(set(files))
    expected = {"META-INF/MANIFEST.MF", "fabric.mod.json", "dev/resivore/amc/StoneBlocks.class",
                f"assets/{NS}/lang/en_us.json", f"assets/{NS}/textures/block/stone_louvered_shutter.png"}
    expected.add("dev/resivore/amc/AndesiteBlocks.class")
    expected.add("dev/resivore/amc/MasonryBlocks.class")
    names = read_json(jar, f"assets/{NS}/lang/en_us.json")
    new_forms = ("andesite_button", "andesite_pressure_plate", "andesite_gothic_window", "andesite_arrow_slit")
    assert names == {f"block.{NS}.{form}": form.replace("_", " ").title() for form in (*FORMS, *new_forms, *("andesite_" + f for f in COVERAGE))}
    models = set()
    for form in FORMS:
        own = f"{NS}:{form}"
        assert names[f"block.{NS}.{form}"] == form.replace("_", " ").title()
        assert f"assets/mcwwindows/blockstates/{form}.json" not in provider.namelist()
        compare_display(jar, provider, form, models)
        for prefix in (f"assets/{NS}/blockstates", f"assets/{NS}/items", f"data/{NS}/loot_table/blocks",
                       f"data/{NS}/recipe", f"data/{NS}/advancement/recipes/building_blocks"):
            expected.add(f"{prefix}/{form}.json")
        recipe = read_json(jar, f"data/{NS}/recipe/{form}.json")
        pattern, keys, count = ((["AAA", "ABA"], {"A": "minecraft:stick", "B": "minecraft:stone"}, 5)
                                if form == "stone_parapet" else (["B"] * 3, {"B": "minecraft:stone_slab"}, 3))
        assert recipe["type"] == "minecraft:crafting_shaped" and recipe["pattern"] == pattern
        assert recipe["key"] == keys and recipe["result"] == {"count": count, "id": own}
        unlock = read_json(jar, f"data/{NS}/advancement/recipes/building_blocks/{form}.json")
        assert unlock["rewards"]["recipes"] == [own]
        loot = read_json(jar, f"data/{NS}/loot_table/blocks/{form}.json")
        assert loot["pools"] == [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": own}]}]
    assert len(models) == 6
    for model in models:
        compare_model(jar, provider, client, model)
        expected.add(f"assets/{NS}/models/{model}.json")
    check_andesite(jar, provider, client, expected)
    for tag, forms in (("minecraft/tags/block/mineable/pickaxe", FORMS + new_forms[1:]),
                       ("minecraft/tags/block/stone_buttons", new_forms[:1]),
                       ("minecraft/tags/item/stone_buttons", new_forms[:1]),
                       ("minecraft/tags/block/stone_pressure_plates", new_forms[1:2]),
                       ("minecraft/tags/block/walls", (*new_forms[2:], "andesite_fence")),
                       ("mcwwindows/tags/block/gothic", new_forms[2:3]),
                       ("mcwwindows/tags/block/arrow_slit", new_forms[3:]),
                       ("mcwwindows/tags/block/parapets", FORMS[:1]), ("mcwwindows/tags/block/shutters", FORMS[1:])):
        path = f"data/{tag}.json"
        expected.add(path)
        assert read_json(jar, path) == {"replace": False, "values": [f"{NS}:{form}" for form in forms]}
    check_masonry(jar, bbb, client, expected)
    assert set(files) == expected, set(files).symmetric_difference(expected)
    check_texture(jar, provider, client)
    sprite = jar.read(f"assets/{NS}/textures/block/stone_louvered_shutter.png")
    assert hashlib.sha256(sprite).hexdigest() == "c67d4b297dd2b312c0732dc38dc607cdae9937d493028000a33f4b2e4e959878"
    print(f"Verified 11 block/item sets, state and sprite closure, recipes, drops, tags, "
          f"the exact C9 shutter sprite and all {len(files)} production entries.")


if __name__ == "__main__":
    if not __debug__:
        sys.exit("Run resource checks without Python optimization")
    with ZipFile(Path(sys.argv[1])) as jar, ZipFile(sys.argv[2]) as provider, ZipFile(sys.argv[3]) as client, ZipFile(sys.argv[4]) as bbb:
        verify(jar, provider, client, bbb)
