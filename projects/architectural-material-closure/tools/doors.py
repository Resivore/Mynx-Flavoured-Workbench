"""Stage the Macaw wood-door variants in local, provider-gated packs."""
import hashlib
import json
import re
from collections import defaultdict
from pathlib import Path

from masonry import NS, image, rank, rank_mapping
from moss import light

MACAW = "mcwdoors"
WOODS = ("acacia", "bamboo", "birch", "cherry", "crimson", "dark_oak", "jungle",
         "mangrove", "oak", "pale_oak", "spruce", "warped")
MATERIALS = {"veiled": "enderscape", "celestial": "enderscape", "murublight": "enderscape",
             "mossy_oak": "ribbits", "silver_birch":"mynx_trees", "wisteria":"mynx_trees"}
STEMS = {"celestial", "murublight"}


def read(archive, name):
    return json.loads(archive.read(name))


def write(root, name, data):
    path = root / name
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, separators=(",", ":")) + "\n", encoding="utf8")


def designs(archive):
    result = set()
    for filename in archive.namelist():
        if filename.startswith("assets/mcwdoors/blockstates/") and filename.endswith("_door.json"):
            for wood in WOODS:
                if Path(filename).stem.startswith(wood + "_"):
                    result.add(Path(filename).stem[len(wood)+1:-5])
                    break
    return sorted(result)


def reference(design):
    return {"classic": "spruce", "stem_glass": "warped"}.get(design, "oak")


def rename(path, wood, material):
    # Prefixes can follow either a folder or "japanese_".
    return re.sub(r"(?<![a-z])" + wood + r"(?=_|$)", material, path)


def resource(ref, kind, ext):
    ns, path = ref.split(":")
    return f"assets/{ns}/{kind}/{path}.{ext}"


def material_mask(archive, ref, wood):
    path = resource(ref, "textures", "png")
    tile = image(archive, path)
    assert tile.size == (16, 16), path
    peers = [image(archive, other) for variant in WOODS if variant != wood
             and (other := rename(path, wood, variant)) in archive.NameToInfo]
    assert peers
    mask = set()
    for y in range(16):
        for x in range(16):
            xy = (x, y)
            pixel = tile.getpixel(xy)
            r, g, b, a = pixel
            if a != 255:
                continue
            if "stem_glass" in ref:
                # The maroon bark base is common to both nether-stem icons.
                # Only the glass border/reflections and right handle are fittings.
                wooden = (r, g, b) not in ((208, 234, 233), (168, 168, 168))
            else:
                # Oak/Spruce wood ramps are warm. Steel changes tint between
                # some provider woods, so cross-wood disagreement alone is unsafe.
                wooden = r > g + 8 and g > b + 10
                agrees = sum(p.getpixel(xy) == pixel for p in peers)
                wooden = wooden and agrees * 2 < len(peers)
                if ":item/" in ref and (r, g, b) in (
                        (33,25,11), (65,48,20), (75,55,24), (79,63,38)):
                    wooden = False  # Authored hinges, pulls and Stable iron bars.
            if wooden:
                mask.add(xy)
    assert mask, ref
    return tile, mask


def wood_tile(archive, ref):
    return image(archive, resource(ref, "textures", "png"))


def palette(tile):
    return sorted({p[:3] for p in tile.getdata() if p[3] == 255}, key=rank)


def moss_uv(x, y, item):
    # Placed halves each occupy a 16px tile. The authored item silhouettes
    # compress a 16x32 front onto columns 3..12 and rows 2..15.
    if item:
        return ((x-3)*16//10 % 16, (y-2)*32//14 % 16)
    return (x, y)


def transfer(sheets, material, oak, moss, planks, bark):
    # Use one ramp for upper/lower sheets of a design, and a separate ramp
    # for its independently drawn inventory icon.
    groups = defaultdict(set)
    for src, (tile, mask) in sheets.items():
        group = re.sub("_(upper|lower)$", "", src)
        groups[group].update(tile.getpixel(xy)[:3] for xy in mask)
    result = {}
    for src, (tile, mask) in sheets.items():
        group = re.sub("_(upper|lower)$", "", src)
        is_bark = "bark_glass" in src or "stem_glass" in src
        target = palette(bark if is_bark else planks)
        mapping = rank_mapping(groups[group], target)
        dest = tile.copy()
        for xy in mask:
            original = tile.getpixel(xy)
            if material == "mossy_oak":
                # Macaw omits Oak Classic. Normalize its Spruce wood ramp to
                # actual Oak planks before transferring moss at matching UVs.
                base = mapping[original[:3]] if "spruce_classic" in src else original[:3]
                uv = moss_uv(*xy, ":item/" in src)
                before, after = oak.getpixel(uv), moss.getpixel(uv)
                if after[0] - after[1] <= 13 and after != before:
                    gain = min(1.0, light(base) / light(before))
                    base = tuple(min(255, round(c * gain)) for c in after[:3])
            else:
                base = mapping[original[:3]]
            dest.putpixel(xy, (*base, original[3]))
        result[src] = dest
    return result


def stage(root, vanilla, macaw, enderscape, ribbits, trees):
    if hashlib.sha256(Path(macaw.filename).read_bytes()).hexdigest() != "00e431d662489fe4a145f0e471837e1cd2bc9bcce5abe3f9f9941230765b2ded":
        raise ValueError("Expected the retained Macaw's Doors 1.1.5 Fabric 26.2 reference")
    oak = wood_tile(vanilla, "minecraft:block/oak_planks")
    moss = wood_tile(ribbits, "ribbits:block/mossy_oak_planks")
    assert sum(a != b for a,b in zip(oak.getdata(), moss.getdata())) == 63
    provider_tags = {n: read(macaw,n)["values"] for n in macaw.namelist()
                     if n.endswith(".json") and "/tags/" in n}
    names = read(macaw, "assets/mcwdoors/lang/en_us.json")
    for provider, archive in (("enderscape", enderscape), ("ribbits", ribbits), ("mynx_trees", trees)):
        pack = root / "resourcepacks" / (provider + "_doors")
        tags, labels = defaultdict(list), {}
        for material, ns in MATERIALS.items():
            if ns != provider:
                continue
            planks = material + "_planks"
            slab = (planks if ns == "ribbits" else material) + "_slab"
            log = material + ("_stem" if material in STEMS else "_log")
            for ingredient in (planks, slab, planks if ns == "ribbits" else log):
                archive.getinfo(f"assets/{ns}/items/{ingredient}.json")
            native_bark = (f"{NS}:block/mossy_oak_log" if ns == "ribbits" else f"{ns}:block/{log}")
            bark = wood_tile(vanilla, "minecraft:block/oak_log") if ns == "ribbits" else wood_tile(archive,native_bark)
            target_planks = oak if ns == "ribbits" else wood_tile(archive, f"{ns}:block/{planks}")
            sheets, destinations, rewards = {}, {}, []
            for design in designs(macaw):
                if design == ("bark_glass" if material in STEMS else "stem_glass"):
                    continue
                wood = reference(design)
                src = f"{wood}_{design}_door"
                name = f"{material}_{design}_door"
                own = NS + ":" + name
                for jar, owner in ((archive,ns),(macaw,MACAW)):
                    assert f"assets/{owner}/items/{name}.json" not in jar.NameToInfo
                rewards.append(own)
                label = names["item.mcwdoors." + src].replace(wood.title(), material.replace("_"," ").title(), 1)
                for prefix in ("item", "block"):
                    labels[f"{prefix}.{NS}.{name}"] = label

                def bind_texture(ref):
                    if ref == "minecraft:block/glass":
                        return ref
                    if ref in ("minecraft:block/oak_log", "minecraft:block/warped_stem"):
                        return native_bark
                    assert ref.startswith("mcwdoors:") and wood in ref, ref
                    if ref not in sheets:
                        sheets[ref] = material_mask(macaw, ref, wood)
                    dest = NS + ":" + rename(ref.split(":")[1], wood, material)
                    destinations[ref] = dest
                    return dest

                def bind_model(ref):
                    model = read(macaw, resource(ref,"models","json"))
                    for key, tex in model["textures"].items():
                        model["textures"][key] = bind_texture(tex)
                    dest = NS + ":" + rename(ref.split(":")[1],wood,material)
                    write(pack,resource(dest,"models","json"),model)
                    return dest

                state = read(macaw, f"assets/mcwdoors/blockstates/{src}.json")
                for branch in state["variants"].values():
                    branch["model"] = bind_model(branch["model"])
                write(pack, f"assets/{NS}/blockstates/{name}.json", state)
                item = read(macaw, f"assets/mcwdoors/items/{src}.json")
                item["model"]["model"] = bind_model(item["model"]["model"])
                write(pack, f"assets/{NS}/items/{name}.json", item)
                recipe = read(macaw, f"data/mcwdoors/recipe/{src}.json")
                ingredients = {f"minecraft:{wood}_planks": ns+":"+planks,
                               f"minecraft:{wood}_slab": ns+":"+slab,
                               f"minecraft:{wood}_log": ns+":"+(planks if ns == "ribbits" else log),
                               f"minecraft:{wood}_stem": ns+":"+log,
                               f"mcwdoors:{wood}_stable_door": f"{NS}:{material}_stable_door"}
                recipe["key"] = {k:ingredients.get(v,v) for k,v in recipe["key"].items()}
                recipe["result"]["id"] = own
                write(pack, f"data/{NS}/recipe/{name}.json", recipe)
                loot = macaw.read(f"data/mcwdoors/loot_table/blocks/{src}.json").decode().replace(MACAW+":"+src,own)
                write(pack, f"data/{NS}/loot_table/blocks/{name}.json", json.loads(loot))
                for path, values in provider_tags.items():
                    if MACAW+":"+src in values:
                        tags[path].append(own)
            for src, tile in transfer(sheets, material, oak, moss, target_planks, bark).items():
                path = pack / resource(destinations[src],"textures","png")
                path.parent.mkdir(parents=True,exist_ok=True)
                tile.save(path)
            unlock = read(macaw, "data/mcwdoors/advancement/recipes/oak.json")
            unlock["rewards"]["recipes"] = rewards
            unlock["criteria"]["has_wood"]["conditions"]["items"][0]["items"] = [ns+":"+planks]
            unlock["criteria"]["has_the_recipe"]["conditions"]["recipe"] = rewards[0]
            write(pack,f"data/{NS}/advancement/recipes/{material}_macaw_doors.json",unlock)
        write(pack, f"assets/{NS}/lang/en_us.json", labels)
        for path, values in tags.items():
            write(pack, path, {"replace":False,"values":values})
