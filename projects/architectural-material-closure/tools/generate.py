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
import paths

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


def remap_stone_shutter(source, client, dest, material):
    tile = masonry.image(client, f"assets/minecraft/textures/block/{material}.png")
    target = sorted({p[:3] for p in tile.getdata() if p[3] == 255}, key=masonry.rank)
    mapping = masonry.rank_mapping({p[:3] for p in source.getdata() if p[3] == 255}, target)
    result = source.copy()
    result.putdata([(*mapping[p[:3]], p[3]) if p[3] == 255 else p for p in source.getdata()])
    dest.parent.mkdir(parents=True, exist_ok=True)
    result.save(dest)


def shade_shutter(provider, client, dest, substrate):
    def image(archive, path):
        return Image.open(io.BytesIO(archive.read(path))).convert("RGBA")

    shutter = image(provider, "assets/mcwwindows/textures/block/andesite_louvered_shutter.png")
    base = image(client, f"assets/minecraft/textures/block/{substrate}.png")
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

    result = base.copy()
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


def bind_sandstone_caps(model, archive, material, material_slot):
    if material not in ("sandstone", "red_sandstone"):
        return
    ns, path = model["parent"].split(":")
    parent = masonry.load_json(archive, f"assets/{ns}/models/{path}.json")
    # These parents share one material slot across directions. Retain the parent for
    # transforms and lighting, and override its elements with the original UVs intact.
    model["elements"] = parent["elements"]
    for direction, slot in (("up", "top"), ("down", "bottom")):
        model["textures"][slot] = f"minecraft:block/{material}_{slot}"
        for element in model["elements"]:
            face = element["faces"].get(direction)
            if face and face["texture"] == material_slot:
                face["texture"] = "#" + slot


def generate_direct_form(root, archive, namespace, source, form, material, *, substrate=None):
    substrate = substrate or material
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
                model["textures"][slot] = f"minecraft:block/{substrate}"
            else:
                assert slot == "3" and sprite == "minecraft:block/glass"
        bind_sandstone_caps(model, archive, material, "#texture" if namespace == "minecraft" else "#0")
        write_json(root, f"assets/{NS}/models/{dest}.json", model)

    recipe = load(f"data/{namespace}/recipe/{source}.json")
    materials = {"minecraft:stone": f"minecraft:{substrate}", "minecraft:stone_bricks": f"minecraft:{substrate}",
                 "minecraft:stone_brick_slab": f"minecraft:{material}_slab", "minecraft:glass_pane": "minecraft:glass_pane"}
    if "key" in recipe:
        recipe["key"] = {key: materials[value] for key, value in recipe["key"].items()}
    else:
        recipe["ingredients"] = [materials[value] for value in recipe["ingredients"]]
    assert recipe["result"]["id"] == source_id
    recipe["result"]["id"] = own_id
    write(f"data/{NS}/recipe", recipe)
    category = "redstone" if namespace == "minecraft" else "building_blocks"
    recipe_unlock(root, name, substrate, category)
    loot = load(f"data/{namespace}/loot_table/blocks/{source}.json")
    entry, = loot["pools"][0]["entries"]
    assert entry["name"] == source_id
    entry["name"] = own_id
    if "random_sequence" in loot:
        loot["random_sequence"] = f"{NS}:blocks/{name}"
    write(f"data/{NS}/loot_table/blocks", loot)


def generate_macaw(root, provider, client, tags, material, substrate, slab, forms, *, stone_artwork=None):
    sandstone = material in ("sandstone", "red_sandstone")
    ids = {f"mcwwindows:andesite_{form}": f"{NS}:{material}_{form}" for form in forms}
    for source_id, own_id in ids.items():
        source = source_id.split(":")[1]
        name = own_id.split(":")[1]
        states = masonry.load_json(provider, f"assets/mcwwindows/blockstates/{source}.json")
        item = masonry.load_json(provider, f"assets/mcwwindows/items/{source}.json")
        choices = (states["variants"].values() if "variants" in states
                   else [part["apply"] for part in states["multipart"]])
        models = {}
        for choice in [*choices, item["model"]]:
            reference = choice["model"]
            if reference == "mcwwindows:block/parent/resizeable/mid_m":
                continue  # Glass-only center shared by the connected windows.
            assert reference.startswith("mcwwindows:block/") and "/andesite_" in reference
            path = reference.split(":")[1]
            models[path] = path.replace("/andesite_", f"/{material}_")
            choice["model"] = f"{NS}:{models[path]}"
        write_json(root, f"assets/{NS}/blockstates/{name}.json", states)
        write_json(root, f"assets/{NS}/items/{name}.json", item)

        for path, dest in models.items():
            model = masonry.load_json(provider, f"assets/mcwwindows/models/{path}.json")
            for slot, sprite in model["textures"].items():
                if sprite in ("minecraft:block/andesite", "minecraft:block/polished_andesite"):
                    model["textures"][slot] = f"minecraft:block/{substrate}"
                elif sprite == "mcwwindows:block/andesite_louvered_shutter":
                    model["textures"][slot] = f"{NS}:block/{material}_louvered_shutter"
                else:
                    assert sprite in ("minecraft:block/glass", "mcwwindows:block/glass")
            bind_sandstone_caps(model, provider, material, "#1" if source.endswith("_parapet") else "#0")
            if sandstone and source == "andesite_louvered_shutter":
                model["textures"]["bottom"] = model["textures"]["top"]
            write_json(root, f"assets/{NS}/models/{dest}.json", model)

        recipe = masonry.load_json(provider, f"data/mcwwindows/recipe/{source}.json")
        materials = {"minecraft:andesite": f"minecraft:{substrate}", "minecraft:andesite_slab": f"minecraft:{slab}"}
        recipe["key"] = {k: materials.get(v, v) for k, v in recipe["key"].items()}
        assert recipe["result"]["id"] == source_id
        recipe["result"]["id"] = own_id
        write_json(root, f"data/{NS}/recipe/{name}.json", recipe)
        ingredient = slab if name.endswith("_shutter") else substrate
        recipe_unlock(root, name, ingredient, "building_blocks")
        loot = masonry.load_json(provider, f"data/mcwwindows/loot_table/blocks/{source}.json")
        entry, = loot["pools"][0]["entries"]
        assert entry["name"] == source_id
        entry["name"] = own_id
        write_json(root, f"data/{NS}/loot_table/blocks/{name}.json", loot)

    for path in provider.namelist():
        if path.startswith("data/") and "/tags/" in path and path.endswith(".json"):
            members = masonry.load_json(provider, path)["values"]
            additions = [own for ref, own in ids.items() if ref in members]
            if additions:
                tags.setdefault(path[5:-5], []).extend(additions)
    dest = root / f"assets/{NS}/textures/block/{material}_louvered_shutter.png"
    if stone_artwork is None:
        shade_shutter(provider, client, dest, material + "_top" if sandstone else substrate)
    else:
        remap_stone_shutter(stone_artwork, client, dest, material)


def generate_stone_forms(root, provider, client, tags, names, material):
    for archive, namespace, source, form in (
        (client, "minecraft", "stone_button", "button"),
        (client, "minecraft", "stone_pressure_plate", "pressure_plate"),
        (provider, "mcwwindows", "stone_brick_gothic", "gothic_window"),
        (provider, "mcwwindows", "stone_brick_arrow_slit", "arrow_slit"),
    ):
        generate_direct_form(root, archive, namespace, source, form, material)
        names[f"block.{NS}.{material}_{form}"] = f"{material}_{form}".replace("_", " ").title()
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


def generate(provider, client, bbb, path_provider, stone_shutter, root):
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

    shade_shutter(provider, client, root / f"assets/{NS}/textures/block/stone_louvered_shutter.png", "stone")
    names = {
        f"block.{NS}.stone_parapet": "Stone Parapet",
        f"block.{NS}.stone_louvered_shutter": "Stone Louvered Shutter",
    }
    stone_ids = [f"{NS}:stone_{form}" for form in FORMS]
    tags = {"mcwwindows/tags/block/parapets": stone_ids[:1], "mcwwindows/tags/block/shutters": stone_ids[1:],
            "minecraft/tags/block/mineable/pickaxe": stone_ids.copy()}
    for material in ("andesite", "diorite", "granite"):
        generate_stone_forms(root, provider, client, tags, names, material)
        for form in masonry.FORMS:
            names[f"block.{NS}.{material}_{form}"] = f"{material} {form}".title()
        masonry.generate(root, bbb, client, material, tags, write_json, recipe_unlock)

    for material, substrate, slab in (("mud_bricks", "packed_mud", "mud_brick_slab"),
                                      ("prismarine_bricks", "prismarine_bricks", "prismarine_brick_slab"),
                                      ("dark_prismarine", "dark_prismarine", "dark_prismarine_slab")):
        for form in ("button", "pressure_plate"):
            generate_direct_form(root, client, "minecraft", "stone_" + form, form, material, substrate=substrate)
        for tag in ("minecraft/tags/block/stone_buttons", "minecraft/tags/item/stone_buttons"):
            tags[tag].append(f"{NS}:{material}_button")
        for tag in ("minecraft/tags/block/mineable/pickaxe", "minecraft/tags/block/stone_pressure_plates"):
            tags[tag].append(f"{NS}:{material}_pressure_plate")
        windows = ("louvered_shutter",)
        if material == "mud_bricks":
            windows = ("window", "window2", "four_window", "pane_window", "parapet", *windows)
        generate_macaw(root, provider, client, tags, material, substrate, slab, windows,
                       stone_artwork=stone_shutter if material in ("prismarine_bricks", "dark_prismarine") else None)
        masonry.generate(root, bbb, client, material, tags, write_json, recipe_unlock, substrate=substrate, slab=slab)
        for form in ("button", "pressure_plate", *windows, *masonry.FORMS):
            name = f"{material}_{form}"
            names[f"block.{NS}.{name}"] = name.replace("window2", "window 2").replace("_", " ").title()
    paths.generate(root, path_provider, client, tags, names, write_json, recipe_unlock)
    for material in ("sandstone", "red_sandstone"):
        generate_stone_forms(root, provider, client, tags, names, material)
        generate_macaw(root, provider, client, tags, material, material, material + "_slab", FORMS)
        for form in FORMS:
            name = f"{material}_{form}"
            names[f"block.{NS}.{name}"] = name.replace("_", " ").title()
    write_json(root, f"assets/{NS}/lang/en_us.json", names)
    for tag, values in tags.items():
        write_json(root, f"data/{tag}.json", {"replace": False, "values": values})


if __name__ == "__main__":
    provider_path, client_path, bbb_path, paths_path, stone_path, root = map(Path, sys.argv[1:])
    if not root.resolve().is_relative_to(Path(__file__).resolve().parents[1] / "build"):
        sys.exit("Private derived assets must be generated under AMC's ignored build directory")
    if hashlib.sha256(provider_path.read_bytes()).hexdigest() != PROVIDER_SHA256:
        sys.exit("Unexpected Macaw's Windows reference bytes")
    if hashlib.sha1(client_path.read_bytes()).hexdigest() != CLIENT_SHA1:
        sys.exit("Expected the official Minecraft 26.2 client JAR")
    if hashlib.sha256(bbb_path.read_bytes()).hexdigest() != masonry.REFERENCE_SHA256:
        sys.exit("Unexpected BBB 26.2 reference bytes")
    if hashlib.sha256(paths_path.read_bytes()).hexdigest() != paths.REFERENCE_SHA256:
        sys.exit("Unexpected Macaw's Paths reference bytes")
    if hashlib.sha256(stone_path.read_bytes()).hexdigest() != "f32b382563252e8a73a0bfd0b9ca0abacb02dd3c81a2a074b9d200fb5f147e8b":
        sys.exit("Expected the retained AMC C9 artifact")
    with ZipFile(stone_path) as c9:
        stone_shutter = masonry.image(c9, f"assets/{NS}/textures/block/stone_louvered_shutter.png")
    with ZipFile(provider_path) as provider, ZipFile(client_path) as client, ZipFile(bbb_path) as bbb, ZipFile(paths_path) as path_provider:
        generate(provider, client, bbb, path_provider, stone_shutter, root)
