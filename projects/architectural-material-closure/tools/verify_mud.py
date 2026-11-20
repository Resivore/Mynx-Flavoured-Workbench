"""Audit AMC's six standard Macaw variants against the 26.2 provider archive."""
from io import BytesIO
import json
from statistics import median

from PIL import Image

NS = "architectural_material_closure"
MACAW_FORMS = {"window": (32, 9), "window2": (16, 2), "four_window": (16, 2),
               "pane_window": (64, 8), "parapet": (8, 2), "louvered_shutter": (16, 4)}


def check_shutter(jar, provider, client, expected, material, substrate):
    def image(archive, path):
        return Image.open(BytesIO(archive.read(path))).convert("RGBA")

    path = f"assets/{NS}/textures/block/{material}_louvered_shutter.png"
    expected.add(path)
    result = image(jar, path)
    base = image(client, f"assets/minecraft/textures/block/{substrate}.png")
    reference = image(provider, "assets/mcwwindows/textures/block/andesite_louvered_shutter.png")
    assert base.size == reference.size == result.size == (16, 16)
    luminance = reference.convert("L")

    # Independently classify the five repeated slats by source UV coordinate.
    def region(x, y):
        if y == 0 or not 2 <= x <= 9:
            return "frame"
        if y % 3 == 1:
            return "bevel"
        if y % 3 == 0:
            return "lip" if x == 2 else "recess"
        return "return" if x == 9 else "flat"

    samples = {}
    for y in range(1, 16):
        for x in range(2, 10):
            part = region(x, y)
            if part != "flat" or 3 <= x < 9:
                samples.setdefault(part, []).append(luminance.getpixel((x, y)))
    gains = {"frame": 1, "flat": 1}
    for part in ("lip", "bevel", "return", "recess"):
        raw = (median(samples[part]) / median(samples["flat"])) ** 2.25
        gains[part] = 1 - 0.75 * (1 - raw) if raw < 1 else raw
    assert gains["return"] < gains["lip"] < 1 < gains["bevel"] and gains["recess"] < gains["lip"]
    for y in range(16):
        for x in range(16):
            src = reference.getpixel((x, y))
            gain = gains[region(x, y)]
            rgb = base.getpixel((x, y))[:3]
            target = (*[min(255, round(c * gain)) for c in rgb], src[3]) if src[3] else src
            assert result.getpixel((x, y)) == target, (x, y, material)
    print(f"Verified all 256 shutter pixels against {substrate} and the five reference slats.")


def check_windows(jar, provider, client, expected, material, substrate, slab):
    shutter = f"{NS}:block/{material}_louvered_shutter"

    def read(archive, path):
        if archive is jar:
            expected.add(path)
        return json.loads(archive.read(path))

    archives = {"mcwwindows": provider, "minecraft": client, NS: jar}

    def check_model(pair, form):
        ref, actual = pair
        original = read(provider, f"assets/mcwwindows/models/{ref.split(':')[1]}.json")
        namespace, model = actual.split(":")
        if ref.endswith("/mid_m"):
            assert form == "window" and actual == ref
            assert original["textures"] == {"0": "mcwwindows:block/glass", "particle": "mcwwindows:block/glass"}
        else:
            assert namespace == NS and actual.replace(NS + ":", "mcwwindows:").replace(material + "_", "andesite_") == ref
        generated = read(archives[namespace], f"assets/{namespace}/models/{model}.json")
        bindings = original.pop("textures")
        slots = generated.pop("textures")
        assert original == generated, (form, ref, "model or parent changed")
        targets = {"minecraft:block/andesite": f"minecraft:block/{substrate}",
                   "minecraft:block/polished_andesite": f"minecraft:block/{substrate}",
                   "mcwwindows:block/andesite_louvered_shutter": shutter}
        assert slots == {k: targets.get(v, v) for k, v in bindings.items()}
        node = generated
        elements = node.get("elements")
        while "parent" in node:
            ns, name = node["parent"].split(":")
            node = read(archives[ns], f"assets/{ns}/models/{name}.json")
            for slot, sprite in node.get("textures", {}).items():
                slots.setdefault(slot, sprite)
            if elements is None:
                elements = node.get("elements")
        assert elements
        visible = set()
        for element in elements:
            for face in element["faces"].values():
                sprite = face["texture"]
                while sprite.startswith("#"):
                    sprite = slots[sprite[1:]]
                ns, path = sprite.split(":")
                archives[ns].getinfo(f"assets/{ns}/textures/{path}.png")
                visible.add(sprite)
        if form == "louvered_shutter":
            assert visible == {shutter}
        elif ref.endswith("/mid_m"):
            assert visible == {"mcwwindows:block/glass"}
        else:
            assert f"minecraft:block/{substrate}" in visible
            assert visible <= {"mcwwindows:block/glass", "minecraft:block/glass", f"minecraft:block/{substrate}"}

    for form, (state_count, model_count) in MACAW_FORMS.items():
        if material != "mud_bricks" and form != "louvered_shutter":
            continue
        src, own = "andesite_" + form, material + "_" + form
        ref_id, own_id = "mcwwindows:" + src, NS + ":" + own
        pairs = set()
        for folder in ("blockstates", "items"):
            ref = read(provider, f"assets/mcwwindows/{folder}/{src}.json")
            actual = read(jar, f"assets/{NS}/{folder}/{own}.json")

            def choices(obj):
                if folder == "items":
                    return [obj["model"]]
                if "variants" in obj:
                    return obj["variants"].values()
                return [part["apply"] for part in obj["multipart"]]

            if folder == "blockstates":
                assert len(choices(ref)) == state_count
            for source, generated in zip(choices(ref), choices(actual), strict=True):
                pairs.add((source["model"], generated["model"]))
                generated["model"] = source["model"]
            assert actual == ref, (form, folder, "state selection or transforms")
        assert len(pairs) == model_count
        for pair in pairs:
            check_model(pair, form)

        for folder in ("recipe", "loot_table/blocks"):
            ref = read(provider, f"data/mcwwindows/{folder}/{src}.json")
            actual = read(jar, f"data/{NS}/{folder}/{own}.json")
            content = json.dumps(ref).replace(ref_id, own_id).replace('"minecraft:andesite"', f'"minecraft:{substrate}"')
            assert actual == json.loads(content.replace("minecraft:andesite_slab", f"minecraft:{slab}"))
        unlock = read(jar, f"data/{NS}/advancement/recipes/building_blocks/{own}.json")
        ingredient = f"minecraft:{slab}" if form == "louvered_shutter" else f"minecraft:{substrate}"
        assert unlock["rewards"]["recipes"] == [own_id] and unlock["criteria"]["recipe"]["conditions"]["recipe"] == own_id
        assert unlock["criteria"]["material"]["conditions"]["items"] == [{"items": ingredient}]
        print(f"{own}: matched {state_count} blockstate entries and {model_count} model choices.")
    check_shutter(jar, provider, client, expected, material, substrate)
