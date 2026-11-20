"""Check the packaged AMC release against the real Macaw and Minecraft resources."""
import io
import json
import sys
from pathlib import Path
from zipfile import ZipFile

from PIL import Image

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

    # The five dark bands and their raised ends must read against the actual
    # adjacent Stone even where vanilla grain would reverse the contrast.
    for y in (3, 6, 9, 12, 15):
        for x in range(3, 9):
            assert result.getpixel((x, y))[0] + 12 <= result.getpixel((x, y - 1))[0]
        assert result.getpixel((9, y - 1))[0] + 12 <= result.getpixel((10, y - 1))[0]


def verify(jar, provider, client):
    files = [e.filename for e in jar.infolist() if not e.is_dir()]
    assert len(files) == len(set(files))
    expected = {"META-INF/MANIFEST.MF", "fabric.mod.json", "dev/resivore/amc/StoneBlocks.class",
                f"assets/{NS}/lang/en_us.json", f"assets/{NS}/textures/block/stone_louvered_shutter.png"}
    names = read_json(jar, f"assets/{NS}/lang/en_us.json")
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
    for tag, forms in (("minecraft/tags/block/mineable/pickaxe", FORMS),
                       ("mcwwindows/tags/block/parapets", FORMS[:1]), ("mcwwindows/tags/block/shutters", FORMS[1:])):
        path = f"data/{tag}.json"
        expected.add(path)
        assert read_json(jar, path) == {"replace": False, "values": [f"{NS}:{form}" for form in forms]}
    assert set(files) == expected, set(files).symmetric_difference(expected)
    check_texture(jar, provider, client)
    print("Verified two block/item resource sets, six model parents, all state mappings, recipes, drops, tags, "
          "and isolated JAR contents. All 256 shutter pixels and five shaded slat rows passed.")


if __name__ == "__main__":
    if not __debug__:
        sys.exit("Run resource checks without Python optimization")
    with ZipFile(Path(sys.argv[1])) as jar, ZipFile(sys.argv[2]) as provider, ZipFile(sys.argv[3]) as client:
        verify(jar, provider, client)
