"""Bind the 26.2 Oak display graphs and Macaw 2.4.2 wood windows in their provider packs."""
from copy import deepcopy
from PIL import Image

from bamboo import existing
from doors import MATERIALS, STEMS, read, resource, wood_tile, write
from masonry import NS
from mossy_oak import moss_sheet

FORMS = ("window", "window2", "four_window", "pane_window")
WOODS = {"bamboo": "minecraft", **MATERIALS}
FIXTURES = ("sign", "wall_sign", "hanging_sign", "wall_hanging_sign", "shelf", "lantern")


def entries(wood):
    timber = wood + ("_stem" if wood in STEMS else "_block" if wood == "bamboo" else "_log")
    for prefix, source, sprite in ((wood, "oak", timber), ("stripped_" + wood, "stripped_oak_log", "stripped_" + timber),
                                    (wood + "_plank", "oak_plank", wood + "_planks")):
        for form in FORMS:
            oak = "stripped_oak" if source == "stripped_oak_log" and form == "pane_window" else source
            yield prefix + "_" + form, oak + "_" + form, sprite, form


def remap(node, strings):
    if isinstance(node, dict):
        return {k: remap(v, strings) for k, v in node.items()}
    if isinstance(node, list):
        return [remap(v, strings) for v in node]
    return strings.get(node, node)


def nodes(state):
    yield from state.get("variants", {}).values()
    yield from (part["apply"] for part in state.get("multipart", []))


def clone_graph(pack, jar, old, new, model_name, bind):
    for kind in ("blockstates", "items"):
        path = resource(old, kind, "json")
        if path not in jar.NameToInfo:
            continue
        graph = read(jar, path)
        for branch in (nodes(graph) if kind == "blockstates" else [graph["model"]]):
            ref = branch["model"]
            target = model_name(ref)
            if target != ref:
                model = bind(read(jar, resource(ref, "models", "json")), ref)
                write(pack, resource(target, "models", "json"), model)
            branch["model"] = target
        write(pack, resource(new, kind, "json"), graph)


def copy_tags(pack, jar, pairs):
    for path in jar.namelist():
        if not path.endswith(".json") or not any(k in path for k in ("/tags/block/", "/tags/item/")):
            continue
        add = [pairs[v] for v in read(jar, path)["values"] if isinstance(v, str) and v in pairs]
        if add:
            tag = existing(pack, path, {"replace": False, "values": []})
            tag["values"].extend(value for value in add if value not in tag["values"])
            write(pack, path, tag)


def loot(pack, jar, old, new):
    ns, name = old.split(":")
    target = new.split(":")[1]
    path = f"data/{ns}/loot_table/blocks/{name}.json"
    if path in jar.NameToInfo:
        write(pack, f"data/{NS}/loot_table/blocks/{target}.json",
              remap(read(jar, path), {old: new, ns + ":blocks/" + name: NS + ":blocks/" + target}))


def bamboo_frame(model, windows, timber, frame):
    # Keep the provider UV islands. Use the long local axis before element rotation to
    # assign longitudinal grain and crosscuts only to frame faces. Glass stays inherited.
    parent = read(windows, resource(model["parent"], "models", "json"))
    model["elements"] = deepcopy(parent["elements"])
    for e in model["elements"]:
        lengths = [abs(b-a) for a, b in zip(e["from"], e["to"])]
        axis = lengths.index(max(lengths))
        for direction, face in e["faces"].items():
            slot = face["texture"][1:]
            if model["textures"].get(slot) != frame:
                continue
            normal = {"east": 0, "west": 0, "up": 1, "down": 1, "north": 2, "south": 2}[direction]
            if normal == axis:
                suffix, tex = "_end", frame + "_top"
            else:
                across_u = axis == (2 if normal == 0 else 0)
                if face.get("rotation", 0) in (90, 270):
                    across_u = not across_u
                if not across_u:
                    continue
                suffix, tex = "_horizontal", NS + ":block/" + timber + "_horizontal"
            model["textures"][slot + suffix] = tex
            face["texture"] = "#" + slot + suffix


def windows_for(pack, wood, material_ns, windows, client, unlock, labels):
    pairs = {}
    for name, source, timber, form in entries(wood):
        old, new = "mcwwindows:" + source, NS + ":" + name
        pairs[old] = new
        native = NS if wood == "mossy_oak" and not timber.endswith("_planks") else material_ns
        frame = native + ":block/" + timber
        def bind(model, ref):
            for slot, src in model["textures"].items():
                if src in ("minecraft:block/oak_planks", "minecraft:block/oak_log", "minecraft:block/stripped_oak_log"):
                    model["textures"][slot] = frame
            if wood == "bamboo" and not timber.endswith("_planks"):
                bamboo_frame(model, windows, timber, frame)
            return model
        def rename(ref):
            if "/parent/" in ref:
                return ref
            return ref.replace("mcwwindows:", NS + ":").replace("oak", wood)
        clone_graph(pack, windows, old, new, rename, bind)
        loot(pack, windows, old, new)
        title = name.replace("window2", "window_2").replace("_", " ").title()
        for kind in ("block", "item"):
            labels[f"{kind}.{NS}.{name}"] = title
        if form == "window":
            recipe = read(windows, f"data/mcwwindows/recipe/{source}.json")
            recipe["result"]["id"] = new
            ingredient = material_ns + ":" + timber
            if wood == "mossy_oak" and not timber.endswith("_planks"):
                ingredient = "minecraft:" + timber.replace("mossy_oak", "oak")
                recipe["pattern"] = ["MA ", "ABA", " A "]
                recipe["key"]["M"] = "ribbits:mossy_oak_planks"
            recipe["key"]["B"] = ingredient
            write(pack, f"data/{NS}/recipe/{name}.json", recipe)
            unlock(pack, name, wood + "_planks", "building_blocks", namespace=material_ns)
    copy_tags(pack, windows, pairs)


def stage(root, client, windows, bbb, rib, unlock):
    for wood, material_ns in WOODS.items():
        pack = root if material_ns == "minecraft" else root / "resourcepacks" / material_ns
        labels_path = f"assets/{NS}/lang/en_us.json"
        labels = existing(pack, labels_path, {})
        windows_for(pack, wood, material_ns, windows, client, unlock, labels)
        if wood == "bamboo":
            dest = pack / resource(NS + ":block/stripped_bamboo_block_horizontal", "textures", "png")
            wood_tile(client, "minecraft:block/stripped_bamboo_block").transpose(Image.Transpose.ROTATE_90).save(dest)
        if wood == "mossy_oak":
            fixtures(pack, client, bbb, rib, labels, unlock)
        write(pack, labels_path, labels)


def fixtures(pack, client, bbb, rib, labels, unlock):
    oak = wood_tile(client, "minecraft:block/oak_planks")
    moss = wood_tile(rib, "ribbits:block/mossy_oak_planks")
    sprites = {"minecraft:block/oak_planks": "ribbits:block/mossy_oak_planks",
               "minecraft:block/stripped_oak_log": NS + ":block/stripped_mossy_oak_log"}
    for category in ("block/oak_sign", "block/oak_hanging_sign", "block/oak_shelf", "item/oak_sign",
                     "item/oak_hanging_sign", "gui/signs/oak", "gui/hanging_signs/oak", "block/lantern/oak"):
        lantern = category == "block/lantern/oak"
        ref = ("bbb:" if lantern else "minecraft:") + category
        sheet = wood_tile(bbb if lantern else client, ref)
        dest = NS + ":" + category.replace("oak", "mossy_oak")
        sprites[ref] = dest
        # Lantern top and foot share UV islands. Hardware and the light chamber have
        # separate islands on the 32px sheet. Keep every pixel outside the wood islands.
        if lantern:
            art = moss_sheet(sheet, oak, moss, [(0, 0, 10, 10), (10, 0, 20, 2)])
        else:
            art = sheet.copy()
            for y in range(0, sheet.height, 16):
                for x in range(0, sheet.width, 16):
                    tile = sheet.crop((x, y, x+16, y+16))
                    treated = moss_sheet(tile, oak, moss, ())
                    for yy in range(16):
                        for xx in range(16):
                            r, g, b, alpha = tile.getpixel((xx, yy))
                            if not alpha or r-g < 10 or g-b < 8:
                                treated.putpixel((xx, yy), tile.getpixel((xx, yy)))
                    art.paste(treated, (x, y))
        path = pack / resource(dest, "textures", "png")
        path.parent.mkdir(parents=True, exist_ok=True)
        art.save(path)
    tags = {"minecraft": {}, "bbb": {}}
    for form in FIXTURES:
        ns = "bbb" if form == "lantern" else "minecraft"
        jar = bbb if form == "lantern" else client
        source, name = "oak_" + form, "mossy_oak_" + form
        old, new = ns + ":" + source, NS + ":" + name
        tags[ns][old] = new
        def rename(ref):
            return ref.replace(ns + ":", NS + ":").replace("oak", "mossy_oak")
        def bind(model, ref):
            if model.get("parent") == "bbb:block/lantern/oak":
                model["parent"] = rename(model["parent"])
            return remap(model, sprites)
        clone_graph(pack, jar, old, new, rename, bind)
        labels[f"block.{NS}.{name}"] = name.replace("_", " ").title()
        if form.startswith("wall_"):
            continue
        loot(pack, jar, old, new)
        recipe = remap(read(jar, f"data/{ns}/recipe/{source}.json"),
                       {old: new, "minecraft:oak_planks": "ribbits:mossy_oak_planks",
                        "minecraft:stripped_oak_log": "ribbits:mossy_oak_planks",
                        "minecraft:oak_slab": "ribbits:mossy_oak_planks_slab"})
        write(pack, f"data/{NS}/recipe/{name}.json", recipe)
        unlock(pack, name, "mossy_oak_planks", "building_blocks", namespace="ribbits")
    for ns, pairs in tags.items():
        copy_tags(pack, bbb if ns == "bbb" else client, pairs)

