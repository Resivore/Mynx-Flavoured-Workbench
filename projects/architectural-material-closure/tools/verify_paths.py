"""Verify all fifteen shipped path families against the original model graphs and pixels."""
import json
from fractions import Fraction

from verify_masonry import NS, read
from masonry import image

FULL = ("running_bond", "flagstone", "windmill_weave", "crystal_floor")
FORMS = ("running_bond_path", "strewn_rocky_path", "windmill_weave_path", "flagstone_path", "crystal_floor_path",
         "diamond_paving", "basket_weave_paving", "square_paving", "honeycomb_paving", "clover_paving", "dumble_paving", *FULL)


def check_paths(jar, provider, client, expected, material="prismarine_bricks", *, material_archive=None, material_ns="minecraft"):
    ingredient = "quartz_block" if material == "quartz" else material
    palette_tile = "quartz_block_side" if material == "quartz" else material
    sheets = {}
    models = set()
    for form in FORMS:
        name = f"{material}_{form}"
        ref = f"dark_prismarine_{form}"
        own_id, ref_id = f"{NS}:{name}", "mcwpaths:" + ref
        assert f"assets/mcwpaths/blockstates/{name}.json" not in provider.namelist()
        choices = set()
        for folder in ("blockstates", "items"):
            path = f"assets/{NS}/{folder}/{name}.json"
            expected.add(path)
            original = read(provider, f"assets/mcwpaths/{folder}/{ref}.json")
            actual = read(jar, path)
            entries = actual["variants"].values() if folder == "blockstates" else [actual["model"]]
            for variant in entries:
                for entry in variant if isinstance(variant, list) else [variant]:
                    ns, model = entry["model"].split(":")
                    assert ns == NS and name in model
                    choices.add(model)
                    entry["model"] = "mcwpaths:" + model.replace(name, ref)
            assert actual == original, (name, folder, "predicates, rotations, weights, item selection")
        assert len(choices) == (3 if form == "strewn_rocky_path" else 2 if form in FULL else 1)
        for model in choices:
            path = f"assets/{NS}/models/{model}.json"
            expected.add(path)
            models.add(model)
            child = read(jar, path)
            original = read(provider, f"assets/mcwpaths/models/{model.replace(name, ref)}.json")
            actual_slots = child.pop("textures")
            original_slots = original.pop("textures")
            assert child == original and actual_slots.keys() == original_slots.keys()
            for slot, source in original_slots.items():
                target = actual_slots[slot]
                if form == "strewn_rocky_path":
                    assert source == "minecraft:block/dark_prismarine" and target == f"{NS}:block/{material}_strewn_rocky"
                else:
                    assert source.startswith("mcwpaths:block/dark_prismarine_")
                    assert target == source.replace("mcwpaths:", NS + ":").replace("dark_prismarine_", f"{material}_")
                assert sheets.setdefault(source, target) == target

            # Both graphs now point to the same immutable ancestors. Resolve all visible face slots
            # through those ancestors, including the carved cube and all weighted strewn choices.
            elements = None
            node = child
            while "parent" in node:
                parent = node["parent"]
                ns, ancestor = parent.split(":") if ":" in parent else ("minecraft", parent)
                archive = {"mcwpaths": provider, "minecraft": client}[ns]
                node = read(archive, f"assets/{ns}/models/{ancestor}.json")
                for key, sprite in node.get("textures", {}).items():
                    actual_slots.setdefault(key, sprite)
                if elements is None:
                    elements = node.get("elements")
            assert elements
            for element in elements:
                for face in element["faces"].values():
                    sprite = face["texture"]
                    while sprite.startswith("#"):
                        sprite = actual_slots[sprite[1:]]
                    assert sprite in sheets.values()
        for folder in ("recipe", "loot_table/blocks"):
            path = f"data/{NS}/{folder}/{name}.json"
            expected.add(path)
            reference = json.dumps(read(provider, f"data/mcwpaths/{folder}/{ref}.json"))
            actual = read(jar, path)
            assert actual == json.loads(reference.replace(ref_id, own_id).replace("minecraft:dark_prismarine", f"{material_ns}:{ingredient}"))
            if folder == "recipe":
                assert actual["type"] == "minecraft:stonecutting" and actual["result"]["count"] == (1 if form in FULL else 4)
        unlock = f"data/{NS}/advancement/recipes/building_blocks/{name}.json"
        expected.add(unlock)
        advancement = read(jar, unlock)
        assert advancement["rewards"]["recipes"] == [own_id]
        assert advancement["criteria"]["material"]["conditions"]["items"] == [{"items": f"{material_ns}:{ingredient}"}]

    def palette(im):
        return sorted({p[:3] for p in im.getdata() if p[3]},
                      key=lambda c: (sum(x * y for x, y in zip(c, (299, 587, 114))), c))

    colors = palette(image(material_archive or client, f"assets/{material_ns}/textures/block/{palette_tile}.png"))
    assert len(colors) == {"quartz": 6, "nether_bricks": 7, "prismarine_bricks": 8, "end_stone_bricks": 8,
                           "tuff": 5, "cinnabar": 11, "sulfur": 6, "smooth_basalt": 6, "veradite": 6, "mirestone": 6, "kurodite": 6}[material] and len(sheets) == 11 and len(set(sheets.values())) == 11
    for source, target in sheets.items():
        ns, sprite = source.split(":")
        original = image(provider if ns == "mcwpaths" else client, f"assets/{ns}/textures/{sprite}.png")
        path = f"assets/{NS}/textures/{target.split(':')[1]}.png"
        expected.add(path)
        actual = image(jar, path)
        ramp = palette(original)
        assert original.size == actual.size
        for before, after in zip(original.getdata(), actual.getdata(), strict=True):
            if before[3]:
                idx = int(Fraction(ramp.index(before[:3]) * (len(colors) - 1), len(ramp) - 1) + Fraction(1, 2))
                assert after == (*colors[idx], before[3]), source
            else:
                assert before == after, source
    print(f"Matched all 15 {material} Paths state and item graphs, {len(models)} model bindings and every pixel of 11 pattern sheets.")
