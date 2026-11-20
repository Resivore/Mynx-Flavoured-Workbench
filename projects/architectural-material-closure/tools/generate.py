"""Stage AMC resources under the project's ignored build directory."""
import hashlib
import io
import json
import sys
from pathlib import Path
from statistics import median
from zipfile import ZipFile

from PIL import Image

import masonry

NS = "architectural_material_closure"
FORMS = ("parapet", "louvered_shutter")
PROVIDER_SHA256 = "40ba9c55f191f8bd4758293683ed2bcf6d0c76c8f54f9fc8a0dccabdbcfd36ad"
CLIENT_SHA1 = "2dc72797acbc1b63fc16a11c4ac393605f453754"


def write_json(root, path, obj):
    dest = root / path
    dest.parent.mkdir(parents=True, exist_ok=True)
    dest.write_text(json.dumps(obj, indent=2) + "\n", encoding="utf-8")


def stone_reference(value):
    if value.startswith("minecraft:andesite"):
        return value.replace("andesite", "stone")
    if value == "minecraft:block/andesite":
        return "minecraft:block/stone"
    if value.startswith("mcwwindows:") and "andesite_" in value:
        return value.replace("mcwwindows:", NS + ":").replace("andesite_", "stone_")
    return value


def adapt(value):
    if isinstance(value, dict):
        return {k: adapt(v) for k, v in value.items()}
    if isinstance(value, list):
        return [adapt(v) for v in value]
    return stone_reference(value) if isinstance(value, str) else value


def shade_stone_shutter(provider, client, dest):
    def image(archive, path):
        return Image.open(io.BytesIO(archive.read(path))).convert("RGBA")

    shutter = image(provider, "assets/mcwwindows/textures/block/andesite_louvered_shutter.png")
    stone = image(client, "assets/minecraft/textures/block/stone.png")
    light = shutter.convert("L")

    # The broad-face UV is [0,1,11,16]. Measure equivalent regions across
    # its five slats so isolated mineral flecks cannot become surface detail.
    def region_light(columns, first_row):
        return median(light.getpixel((x, y)) for y in range(first_row, 16, 3) for x in columns)

    face = region_light(range(3, 9), 2)

    def gain(columns, row):
        full_gain = (region_light(columns, row) / face) ** 2.25
        # C7 used the full shadow depth and C8 used half. Keep their midpoint.
        return 1.0 - 0.75 * (1.0 - full_gain) if full_gain < 1.0 else full_gain

    raised = gain(range(2, 10), 1)
    lip = gain([2], 3)
    recess = gain(range(3, 10), 3)
    end_return = gain([9], 2)

    result = stone.copy()
    result.putalpha(shutter.getchannel("A"))
    for top in range(1, 16, 3):
        for row, columns, factor in (
            (top, range(2, 10), raised),
            (top + 1, [9], end_return),
            (top + 2, [2], lip),
            (top + 2, range(3, 10), recess),
        ):
            for x in columns:
                r, g, b, a = result.getpixel((x, row))
                result.putpixel((x, row), (*[min(255, round(v * factor)) for v in (r, g, b)], a))

    # Preserve the provider's fully transparent padding as well as the alpha mask.
    result.paste(shutter, mask=shutter.getchannel("A").point(lambda a: 255 if a == 0 else 0))
    dest.parent.mkdir(parents=True, exist_ok=True)
    result.save(dest)


def recipe_unlock(root, name, material, category):
    recipe = f"{NS}:{name}"
    write_json(root, f"data/{NS}/advancement/recipes/{category}/{name}.json", {
        "parent": "minecraft:recipes/root",
        "criteria": {
            "material": {"trigger": "minecraft:inventory_changed", "conditions": {
                "items": [{"items": "minecraft:" + material}]}},
            "recipe": {"trigger": "minecraft:recipe_unlocked", "conditions": {"recipe": recipe}},
        },
        "requirements": [["material", "recipe"]],
        "rewards": {"recipes": [recipe]},
    })


def generate_direct_form(root, archive, namespace, source, form, material):
    name = f"{material}_{form}"

    def load(path):
        return json.loads(archive.read(path))

    def write(folder, value):
        write_json(root, f"{folder}/{name}.json", value)

    source_id = f"{namespace}:{source}"
    own_id = f"{NS}:{name}"
    models = {}

    def bind(entry):
        model = entry["model"]
        # Gothic's optional iron bars are an independent provider model.
        if model == "mcwwindows:block/gothic/iron_bars":
            return
        prefix, path = model.split(":")
        assert prefix == namespace and path.rsplit("/", 1)[-1].startswith(source)
        dest = path.replace(source, name)
        models[path] = dest
        entry["model"] = f"{NS}:{dest}"

    states = load(f"assets/{namespace}/blockstates/{source}.json")
    entries = states["variants"].values() if "variants" in states else (p["apply"] for p in states["multipart"])
    for entry in entries:
        bind(entry)
    item = load(f"assets/{namespace}/items/{source}.json")
    bind(item["model"])
    write(f"assets/{NS}/blockstates", states)
    write(f"assets/{NS}/items", item)
    for path, dest in models.items():
        model = load(f"assets/{namespace}/models/{path}.json")
        assert model.keys() == {"parent", "textures"}
        for slot, sprite in model["textures"].items():
            if sprite in ("minecraft:block/stone", "minecraft:block/stone_bricks"):
                model["textures"][slot] = f"minecraft:block/{material}"
            else:
                assert slot == "3" and sprite == "minecraft:block/glass"
        write_json(root, f"assets/{NS}/models/{dest}.json", model)

    recipe = load(f"data/{namespace}/recipe/{source}.json")
    materials = {"minecraft:stone": f"minecraft:{material}", "minecraft:stone_bricks": f"minecraft:{material}",
                 "minecraft:stone_brick_slab": f"minecraft:{material}_slab", "minecraft:glass_pane": "minecraft:glass_pane"}
    if "key" in recipe:
        recipe["key"] = {key: materials[value] for key, value in recipe["key"].items()}
    else:
        recipe["ingredients"] = [materials[value] for value in recipe["ingredients"]]
    assert recipe["result"]["id"] == source_id
    recipe["result"]["id"] = own_id
    write(f"data/{NS}/recipe", recipe)
    category = "redstone" if namespace == "minecraft" else "building_blocks"
    recipe_unlock(root, name, material, category)
    loot = load(f"data/{namespace}/loot_table/blocks/{source}.json")
    entry, = loot["pools"][0]["entries"]
    assert entry["name"] == source_id
    entry["name"] = own_id
    if "random_sequence" in loot:
        loot["random_sequence"] = f"{NS}:blocks/{name}"
    write(f"data/{NS}/loot_table/blocks", loot)


def generate(provider, client, bbb, root):
    def adapt_resource(template):
        src = template.format(ns="mcwwindows", material="andesite")
        dst = template.format(ns=NS, material="stone")
        write_json(root, dst, adapt(json.loads(provider.read(src))))

    for form in FORMS:
        for template in ("assets/{ns}/blockstates/", "assets/{ns}/items/",
                         "data/{ns}/recipe/", "data/{ns}/loot_table/blocks/"):
            adapt_resource(template + "{material}_" + form + ".json")
        material = "stone" if form == "parapet" else "stone_slab"
        recipe_unlock(root, f"stone_{form}", material, "building_blocks")

    models = ["parapet/{material}_parapet", "parapet/{material}_support"]
    models += ["shutter/{material}_louvered_shutter_" + pose
               for pose in ("left", "right", "closed_left", "closed_right")]
    for model in models:
        adapt_resource("assets/{ns}/models/block/" + model + ".json")

    shade_stone_shutter(provider, client, root / f"assets/{NS}/textures/block/stone_louvered_shutter.png")
    names = {
        f"block.{NS}.stone_parapet": "Stone Parapet",
        f"block.{NS}.stone_louvered_shutter": "Stone Louvered Shutter",
    }
    stone_ids = [f"{NS}:stone_{form}" for form in FORMS]
    tags = {"mcwwindows/tags/block/parapets": stone_ids[:1], "mcwwindows/tags/block/shutters": stone_ids[1:],
            "minecraft/tags/block/mineable/pickaxe": stone_ids.copy()}
    for material in ("andesite", "diorite", "granite"):
        for archive, namespace, source, form in (
            (client, "minecraft", "stone_button", "button"),
            (client, "minecraft", "stone_pressure_plate", "pressure_plate"),
            (provider, "mcwwindows", "stone_brick_gothic", "gothic_window"),
            (provider, "mcwwindows", "stone_brick_arrow_slit", "arrow_slit"),
        ):
            generate_direct_form(root, archive, namespace, source, form, material)
            names[f"block.{NS}.{material}_{form}"] = f"{material}_{form}".replace("_", " ").title()
        for form in masonry.FORMS:
            names[f"block.{NS}.{material}_{form}"] = f"{material} {form}".title()
        button, plate, window, slit = [f"{NS}:{material}_{form}" for form in
                                       ("button", "pressure_plate", "gothic_window", "arrow_slit")]
        for tag, values in (("minecraft/tags/block/mineable/pickaxe", [plate, window, slit]),
                            ("minecraft/tags/block/stone_buttons", [button]),
                            ("minecraft/tags/item/stone_buttons", [button]),
                            ("minecraft/tags/block/stone_pressure_plates", [plate]),
                            ("minecraft/tags/block/walls", [window, slit]),
                            ("mcwwindows/tags/block/gothic", [window]),
                            ("mcwwindows/tags/block/arrow_slit", [slit])):
            tags.setdefault(tag, []).extend(values)
        masonry.generate(root, bbb, client, material, tags, write_json, recipe_unlock)

    write_json(root, f"assets/{NS}/lang/en_us.json", names)
    for tag, values in tags.items():
        write_json(root, f"data/{tag}.json", {"replace": False, "values": values})


if __name__ == "__main__":
    provider_path, client_path, bbb_path, root = map(Path, sys.argv[1:])
    if not root.resolve().is_relative_to(Path(__file__).resolve().parents[1] / "build"):
        sys.exit("Private derived assets must be generated under AMC's ignored build directory")
    if hashlib.sha256(provider_path.read_bytes()).hexdigest() != PROVIDER_SHA256:
        sys.exit("Unexpected Macaw's Windows reference bytes")
    if hashlib.sha1(client_path.read_bytes()).hexdigest() != CLIENT_SHA1:
        sys.exit("Expected the official Minecraft 26.2 client JAR")
    if hashlib.sha256(bbb_path.read_bytes()).hexdigest() != masonry.REFERENCE_SHA256:
        sys.exit("Unexpected BBB 26.2 reference bytes")
    with ZipFile(provider_path) as provider, ZipFile(client_path) as client, ZipFile(bbb_path) as bbb:
        generate(provider, client, bbb, root)
