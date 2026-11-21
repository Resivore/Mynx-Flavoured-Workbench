"""Compare the packaged model graphs and pixel coordinates to the retained BBB JAR."""
import io
import json
from fractions import Fraction
from math import floor

from PIL import Image

NS = "architectural_material_closure"
# State branches, total graph nodes (including shared parents/soil and the inventory wrapper).
COVERAGE = {"column": (60, 22), "urn": (2, 3), "moulding": (80, 13), "fence": (9, 5), "frame": (48, 26)}


def read(archive, path):
    return json.loads(archive.read(path))


def amc(reference, material):
    return reference.replace("bbb:", NS + ":").replace("stone", material)


def check_geometry(jar, bbb, client, material, substrate, form, expected, sheets):
    visited = set()
    local_models = 0

    def model_pair(ref, actual):
        nonlocal local_models
        shared = ref.startswith("bbb:block/template/") or ref == "bbb:block/urn/soiled"
        assert actual == (ref if shared else amc(ref, material)), (form, ref, actual)
        if ref in visited:
            return
        visited.add(ref)
        path = "assets/{}/models/{}.json"
        source = read(bbb, path.format(*ref.split(":")))
        output = path.format(*actual.split(":"))
        result = read(bbb if shared else jar, output)
        if not shared:
            expected.add(output)
            local_models += 1
        if "parent" in source:
            model_pair(source.pop("parent"), result.pop("parent"))
        # Compare all geometry (including element rotations, UVs, faces, cullface,
        # display and groups) after removing ONLY the independently checked bindings.
        slots = source.pop("textures", {})
        bound = result.pop("textures", {})
        assert slots.keys() == bound.keys()
        assert result == source, (form, ref, "model geometry or transforms")
        for slot, sprite in slots.items():
            if shared:
                assert bound[slot] == sprite
                # Template defaults can be shadowed by the material child.
                if ref == "bbb:block/urn/soiled":
                    assert sprite == "block/dirt"
                    client.getinfo("assets/minecraft/textures/block/dirt.png")
            elif sprite == "block/stone":
                particle = f"{NS}:block/mossed_stone" if material == "mossy_stone_bricks" else f"minecraft:block/{substrate}"
                assert slot == "particle" and bound[slot] == particle
            else:
                assert sprite.startswith("bbb:block/") and bound[slot] == amc(sprite, material)
                sheets.add(sprite.split(":")[1])

    def check_visible_faces(model):
        slots = {}
        elements = None
        while model:
            ns, name = model.split(":")
            node = read({NS: jar, "bbb": bbb}[ns], f"assets/{ns}/models/{name}.json")
            for slot, sprite in node.get("textures", {}).items():
                slots.setdefault(slot, sprite)
            if elements is None:
                elements = node.get("elements")
            model = node.get("parent")
        assert elements
        for element in elements:
            for face in element["faces"].values():
                sprite = face["texture"]
                while sprite.startswith("#"):
                    sprite = slots[sprite[1:]]
                if sprite == "block/dirt":
                    assert form == "urn"
                else:
                    assert sprite.startswith(NS + ":block/"), (form, sprite)
                    jar.getinfo(f"assets/{NS}/textures/{sprite.split(':')[1]}.png")

    rendered = set()
    for folder in ("blockstates", "items"):
        ref = read(bbb, f"assets/bbb/{folder}/stone_{form}.json")
        path = f"assets/{NS}/{folder}/{material}_{form}.json"
        expected.add(path)
        result = read(jar, path)

        def choices(obj):
            if folder == "items":
                return [obj["model"]]
            if "variants" in obj:
                return obj["variants"].values()
            return [part["apply"] for part in obj["multipart"]]

        if folder == "blockstates":
            assert len(choices(ref)) == COVERAGE[form][0]
        for original, generated in zip(choices(ref), choices(result), strict=True):
            model_pair(original["model"], generated["model"])
            rendered.add(generated["model"])
            generated["model"] = original["model"]
        assert result == ref, (form, folder, "predicates, rotations or model selection")
    for model in rendered:
        check_visible_faces(model)
    assert len(visited) == COVERAGE[form][1], (form, visited)
    print(f"{material} {form}: {COVERAGE[form][0]} state entries, {local_models} AMC and "
          f"{len(visited) - local_models} shared BBB model nodes")


def check_sheets(jar, bbb, client, material, substrate, expected, sheets, material_ns="minecraft"):
    def pixels(archive, path):
        im = Image.open(io.BytesIO(archive.read(path))).convert("RGBA")
        return im.size, list(im.getdata())

    def palette(colors):
        return sorted(set(colors), key=lambda c: (sum(x * y for x, y in zip(c, (299, 587, 114))), c))

    _, tile = pixels(client, f"assets/{material_ns}/textures/block/{substrate}.png")
    target = palette(pixel[:3] for pixel in tile if pixel[3] == 255)
    if material == "granite":
        darkest, *target, lightest = target
        assert len(target) == 8 and darkest not in target and lightest not in target
    groups = {}
    for name in sheets:
        group = "column" if name.startswith("block/column/") else name
        groups.setdefault(group, []).append(name)
    assert len(sheets) == 10 and len(groups) == 6
    for group, names in groups.items():
        source = {}
        for name in names:
            source[name] = pixels(bbb, f"assets/bbb/textures/{name}.png")
        keep = {(0, 0, 0, 255)} if group == "block/moulding/stone" else set()
        if keep:
            _, other = pixels(bbb, "assets/bbb/textures/block/moulding/sandstone.png")
            backing = [i for i, p in enumerate(source[group][1]) if p in keep]
            assert len(backing) == 884 and all(other[i] in keep for i in backing)
        ramp = palette(p[:3] for _, data in source.values() for p in data if p[3] and p not in keep)
        for name, (size, original) in source.items():
            path = f"assets/{NS}/textures/{amc(name, material)}.png"
            expected.add(path)
            result_size, result = pixels(jar, path)
            assert size == result_size
            for i, (before, after) in enumerate(zip(original, result, strict=True)):
                if before[3] == 0 or before in keep:
                    assert before == after, (name, i)
                else:
                    rank = Fraction(ramp.index(before[:3]), len(ramp) - 1)
                    index = floor(rank * (len(target) - 1) + Fraction(1, 2))
                    assert after == (*target[index], before[3]), (name, i)
    print(f"Verified every pixel of {len(sheets)} sheets against {len(target)} vanilla {substrate} colors.")


def check_masonry(jar, bbb, client, material, expected):
    substrate = "packed_mud" if material == "mud_bricks" else material
    slab = {"smooth_basalt": "smooth_basalt", "mud_bricks": "mud_brick_slab", "prismarine_bricks": "prismarine_brick_slab",
           "mossy_stone_bricks": "mossy_stone_brick_slab", "end_stone_bricks": "end_stone_brick_slab"}.get(material, material + "_slab")
    sheets = set()
    for form in COVERAGE:
        check_geometry(jar, bbb, client, material, substrate, form, expected, sheets)
        name, source = material + "_" + form, "stone_" + form
        own, ref = NS + ":" + name, "bbb:" + source
        assert f"assets/bbb/blockstates/{name}.json" not in bbb.namelist()
        for folder, suffix in [("loot_table/blocks", ""), ("recipe", ""), ("recipe", "_from_stonecutting")]:
            original = f"data/bbb/{folder}/{source}{suffix}.json"
            if form == "frame" and suffix:
                assert original not in bbb.namelist()
                continue
            path = f"data/{NS}/{folder}/{name}{suffix}.json"
            expected.add(path)
            content = json.dumps(read(bbb, original)).replace(ref, own)
            assert read(jar, path) == json.loads(content.replace('"minecraft:stone"', f'"minecraft:{substrate}"')
                                                 .replace('"minecraft:stone_slab"', f'"minecraft:{slab}"'))
            if folder == "recipe":
                path = f"data/{NS}/advancement/recipes/building_blocks/{name}{suffix}.json"
                expected.add(path)
                unlock = read(jar, path)
                ingredient = slab if form == "frame" else substrate
                assert unlock["rewards"]["recipes"] == [own + suffix]
                assert unlock["criteria"]["recipe"]["conditions"]["recipe"] == own + suffix
                assert unlock["criteria"]["material"]["conditions"]["items"] == [{"items": "minecraft:" + ingredient}]
    if material == "mossy_stone_bricks":
        assert len(sheets) == 10  # check_moss audits these ten atlases texel by texel.
    else:
        check_sheets(jar, bbb, client, material, substrate, expected, sheets)
