"""Stage the two Stone resources under AMC's ignored build directory."""
import hashlib
import io
import json
import sys
from pathlib import Path
from zipfile import ZipFile

from PIL import Image

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


def palette(image):
    colors = {pixel[:3] for _, pixel in image.getcolors(image.width * image.height) if pixel[3]}
    return sorted(colors, key=lambda c: (299 * c[0] + 587 * c[1] + 114 * c[2], c))


def recolor(provider, client, dest):
    def image(archive, path):
        return Image.open(io.BytesIO(archive.read(path))).convert("RGBA")

    shutter = image(provider, "assets/mcwwindows/textures/block/andesite_louvered_shutter.png")
    stone = image(client, "assets/minecraft/textures/block/stone.png")
    source, target = palette(shutter), palette(stone)
    # BBB rounds the linearly scaled luminance rank to the nearest palette index.
    scale = len(source) - 1
    mapping = {c: target[(2 * i * (len(target) - 1) + scale) // (2 * scale)]
               for i, c in enumerate(source)}
    for y in range(shutter.height):
        for x in range(shutter.width):
            pixel = shutter.getpixel((x, y))
            if pixel[3]:
                shutter.putpixel((x, y), (*mapping[pixel[:3]], pixel[3]))
    dest.parent.mkdir(parents=True, exist_ok=True)
    shutter.save(dest)


def generate(provider, client, root):
    def adapt_resource(template):
        src = template.format(ns="mcwwindows", material="andesite")
        dst = template.format(ns=NS, material="stone")
        write_json(root, dst, adapt(json.loads(provider.read(src))))

    for form in FORMS:
        for template in ("assets/{ns}/blockstates/", "assets/{ns}/items/",
                         "data/{ns}/recipe/", "data/{ns}/loot_table/blocks/"):
            adapt_resource(template + "{material}_" + form + ".json")
        material = "stone" if form == "parapet" else "stone_slab"
        recipe = f"{NS}:stone_{form}"
        write_json(root, f"data/{NS}/advancement/recipes/building_blocks/stone_{form}.json", {
            "parent": "minecraft:recipes/root",
            "criteria": {
                "material": {"trigger": "minecraft:inventory_changed", "conditions": {
                    "items": [{"items": "minecraft:" + material}]}},
                "recipe": {"trigger": "minecraft:recipe_unlocked", "conditions": {"recipe": recipe}},
            },
            "requirements": [["material", "recipe"]],
            "rewards": {"recipes": [recipe]},
        })

    models = ["parapet/{material}_parapet", "parapet/{material}_support"]
    models += ["shutter/{material}_louvered_shutter_" + pose
               for pose in ("left", "right", "closed_left", "closed_right")]
    for model in models:
        adapt_resource("assets/{ns}/models/block/" + model + ".json")

    recolor(provider, client, root / f"assets/{NS}/textures/block/stone_louvered_shutter.png")
    write_json(root, f"assets/{NS}/lang/en_us.json", {
        f"block.{NS}.stone_parapet": "Stone Parapet",
        f"block.{NS}.stone_louvered_shutter": "Stone Louvered Shutter",
    })
    ids = [f"{NS}:stone_{form}" for form in FORMS]
    for tag, values in (("minecraft/tags/block/mineable/pickaxe", ids),
                        ("mcwwindows/tags/block/parapets", ids[:1]),
                        ("mcwwindows/tags/block/shutters", ids[1:])):
        write_json(root, f"data/{tag}.json", {"replace": False, "values": values})


if __name__ == "__main__":
    provider_path, client_path, root = map(Path, sys.argv[1:])
    if not root.resolve().is_relative_to(Path(__file__).resolve().parents[1] / "build"):
        sys.exit("Private derived assets must be generated under AMC's ignored build directory")
    if hashlib.sha256(provider_path.read_bytes()).hexdigest() != PROVIDER_SHA256:
        sys.exit("Unexpected Macaw's Windows reference bytes")
    if hashlib.sha1(client_path.read_bytes()).hexdigest() != CLIENT_SHA1:
        sys.exit("Expected the official Minecraft 26.2 client JAR")
    with ZipFile(provider_path) as provider, ZipFile(client_path) as client:
        generate(provider, client, root)
