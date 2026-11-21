"""Generate fifteen material patterns from Macaw's Dark Prismarine family."""
from masonry import NS, image, load_json, rank, rank_mapping, visual_sprite

REFERENCE_SHA256 = "39128a12cb64fd61286b631714da742d7b6ea0fa8d4573c103081d01e4993bc2"
FULL = ("running_bond", "flagstone", "windmill_weave", "crystal_floor")
FORMS = (*FULL, *(name + "_path" for name in FULL), "strewn_rocky_path",
         "diamond_paving", "basket_weave_paving", "square_paving", "honeycomb_paving", "clover_paving", "dumble_paving")


def generate(root, provider, client, tags, names, write_json, recipe_unlock, material="prismarine_bricks", *, material_archive=None, material_ns="minecraft"):
    ingredient = "quartz_block" if material == "quartz" else material
    sprite = "quartz_block_side" if material == "quartz" else visual_sprite(material_ns, material)
    tile = image(material_archive or client, f"assets/{material_ns}/textures/block/{sprite}.png")
    palette = sorted({p[:3] for p in tile.getdata() if p[3] == 255}, key=rank)
    sheets = {}
    models = {}
    ids = {f"mcwpaths:dark_prismarine_{form}": f"{NS}:{material}_{form}" for form in FORMS}

    def bind_model(choice):
        ref = choice["model"]
        assert ref.startswith("mcwpaths:block/") and "/dark_prismarine_" in ref
        path = ref.split(":")[1]
        dest = path.replace("/dark_prismarine_", f"/{material}_")
        models[path] = dest
        choice["model"] = f"{NS}:{dest}"

    for ref, own in ids.items():
        source, name = ref.split(":")[1], own.split(":")[1]
        names[f"block.{NS}.{name}"] = name.replace("_", " ").title()
        states = load_json(provider, f"assets/mcwpaths/blockstates/{source}.json")
        for variant in states["variants"].values():
            for choice in variant if isinstance(variant, list) else [variant]:
                bind_model(choice)
        item = load_json(provider, f"assets/mcwpaths/items/{source}.json")
        bind_model(item["model"])
        write_json(root, f"assets/{NS}/blockstates/{name}.json", states)
        write_json(root, f"assets/{NS}/items/{name}.json", item)
        recipe = load_json(provider, f"data/mcwpaths/recipe/{source}.json")
        assert recipe["ingredient"] == "minecraft:dark_prismarine" and recipe["result"]["id"] == ref
        recipe["ingredient"] = f"{material_ns}:{ingredient}"
        recipe["result"]["id"] = own
        write_json(root, f"data/{NS}/recipe/{name}.json", recipe)
        recipe_unlock(root, name, ingredient, "building_blocks", namespace=material_ns)
        loot = load_json(provider, f"data/mcwpaths/loot_table/blocks/{source}.json")
        entry, = loot["pools"][0]["entries"]
        assert entry["name"] == ref
        entry["name"] = own
        write_json(root, f"data/{NS}/loot_table/blocks/{name}.json", loot)

    for source, dest in models.items():
        model = load_json(provider, f"assets/mcwpaths/models/{source}.json")
        for slot, sprite in model["textures"].items():
            if sprite == "minecraft:block/dark_prismarine":
                # Strewn geometry samples the vanilla source tile on all three models.
                output = f"block/{material}_strewn_rocky"
            else:
                assert sprite.startswith("mcwpaths:block/dark_prismarine_")
                output = sprite.split(":")[1].replace("dark_prismarine_", f"{material}_")
            sheets[sprite] = output
            model["textures"][slot] = f"{NS}:{output}"
        write_json(root, f"assets/{NS}/models/{dest}.json", model)

    # A patterned full block, its engraved state and its thin path reuse one physical sheet.
    for sprite, output in sheets.items():
        ns, path = sprite.split(":")
        sheet = image(client if ns == "minecraft" else provider, f"assets/{ns}/textures/{path}.png")
        mapping = rank_mapping({p[:3] for p in sheet.getdata() if p[3]}, palette)
        sheet.putdata([(*mapping[p[:3]], p[3]) if p[3] else p for p in sheet.getdata()])
        dest = root / f"assets/{NS}/textures/{output}.png"
        dest.parent.mkdir(parents=True, exist_ok=True)
        sheet.save(dest)

    for path in provider.namelist():
        if path.startswith("data/") and "/tags/" in path and path.endswith(".json"):
            additions = [ids[v] for v in load_json(provider, path)["values"] if v in ids]
            if additions:
                tags.setdefault(path[5:-5], []).extend(additions)
