"""Adapt BBB's retained Stone masonry resources in AMC's private staging directory."""
import io
import json

from PIL import Image

NS = "architectural_material_closure"
FORMS = ("column", "urn", "moulding", "fence", "frame")
REFERENCE_SHA256 = "57ddb5dfe62f2eb9f4a2ce22fbeeb5cce4386bbd93aab3f7df0dd8e6d19ddaf0"

# Rank related column surfaces together so that the same shade joins across layers.
# The polished column particle and the four independent atlases have their own ramps.
RAMPS = (
    tuple("column/stone_" + part for part in ("lower", "middle", "none", "top", "upper")),
    ("polished_stone",), ("urn/stone",), ("moulding/stone",),
    ("fence/stone_fence",), ("frame/stone/stone",),
)


def material_id(reference, material):
    return reference.replace("bbb:", NS + ":").replace("stone", material)


def load_json(archive, path):
    return json.loads(archive.read(path))


def image(archive, path):
    return Image.open(io.BytesIO(archive.read(path))).convert("RGBA")


def rank(rgb):
    r, g, b = rgb
    return (299 * r + 587 * g + 114 * b, rgb)


def stage_textures(root, bbb, client, material):
    tile = image(client, f"assets/minecraft/textures/block/{material}.png")
    target = sorted({rgb[:3] for rgb in tile.getdata() if rgb[3] == 255}, key=rank)
    for group in RAMPS:
        sheets = {name: image(bbb, f"assets/bbb/textures/block/{name}.png") for name in group}
        colors = {pixel[:3] for sheet in sheets.values() for pixel in sheet.getdata() if pixel[3]}
        if group == ("moulding/stone",):
            # Opaque black backing is identical in BBB's Stone and Sandstone moulding atlases.
            colors.remove((0, 0, 0))
        source = sorted(colors, key=rank)
        scale = len(source) - 1
        mapping = {rgb: target[(2 * i * (len(target) - 1) + scale) // (2 * scale)]
                   for i, rgb in enumerate(source)}
        for name, sheet in sheets.items():
            sheet.putdata([(*mapping.get(pixel[:3], pixel[:3]), pixel[3]) if pixel[3] else pixel
                           for pixel in sheet.getdata()])
            dest = root / f"assets/{NS}/textures/block/{material_id(name, material)}.png"
            dest.parent.mkdir(parents=True, exist_ok=True)
            sheet.save(dest)


def generate(root, bbb, client, material, tags, write_json, recipe_unlock):
    stage_textures(root, bbb, client, material)
    sprites = {"bbb:block/" + sheet for group in RAMPS for sheet in group}
    staged = set()

    def stage_model(reference):
        if reference == "bbb:block/urn/soiled" or reference.startswith("bbb:block/template/"):
            return reference
        assert reference.startswith("bbb:") and "stone" in reference, reference
        dest = material_id(reference, material)
        if reference not in staged:
            staged.add(reference)
            model = load_json(bbb, f"assets/bbb/models/{reference.split(':')[1]}.json")
            if "parent" in model:
                model["parent"] = stage_model(model["parent"])
            for slot, sprite in model.get("textures", {}).items():
                if sprite in sprites:
                    model["textures"][slot] = material_id(sprite, material)
                elif slot == "particle" and sprite == "block/stone":
                    model["textures"][slot] = f"minecraft:block/{material}"
                else:
                    raise ValueError(f"Unaudited {reference} sprite {slot}: {sprite}")
            write_json(root, f"assets/{NS}/models/{dest.split(':')[1]}.json", model)
        return dest

    for form in FORMS:
        source, name = "stone_" + form, material + "_" + form
        own, ref = NS + ":" + name, "bbb:" + source
        states = load_json(bbb, f"assets/bbb/blockstates/{source}.json")
        item = load_json(bbb, f"assets/bbb/items/{source}.json")
        choices = (states["variants"].values() if "variants" in states
                   else [part["apply"] for part in states["multipart"]])
        for choice in [*choices, item["model"]]:
            choice["model"] = stage_model(choice["model"])
        write_json(root, f"assets/{NS}/blockstates/{name}.json", states)
        write_json(root, f"assets/{NS}/items/{name}.json", item)
        sources = [("loot_table/blocks", ""), ("recipe", "")]
        if form != "frame":
            sources.append(("recipe", "_from_stonecutting"))
        for folder, suffix in sources:
            content = bbb.read(f"data/bbb/{folder}/{source}{suffix}.json").decode()
            adapted = content.replace(f'"{ref}"', f'"{own}"').replace('"minecraft:stone"', f'"minecraft:{material}"')
            adapted = adapted.replace('"minecraft:stone_slab"', f'"minecraft:{material}_slab"')
            write_json(root, f"data/{NS}/{folder}/{name}{suffix}.json", json.loads(adapted))
            if folder == "recipe":
                ingredient = material + "_slab" if form == "frame" else material
                recipe_unlock(root, name + suffix, ingredient, "building_blocks")
        for path in bbb.namelist():
            if path.startswith("data/") and "/tags/" in path and path.endswith(".json"):
                if ref in load_json(bbb, path)["values"]:
                    tag = path.removeprefix("data/").removesuffix(".json")
                    tags.setdefault(tag, []).append(own)
