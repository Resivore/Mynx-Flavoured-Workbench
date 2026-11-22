"""Stage the six Oak graphs in the conditional Ribbits pack."""
import hashlib
import json
from collections import defaultdict
from zipfile import ZipFile

from masonry import NS, image
from moss import light

FORMS = ("frame", "lattice", "trim", "balustrade", "support", "pallet")
RIBBITS_SHA = "088c4b7e88c432d6275395e29273597cf42575f59350fe8b4f48c46e6df7f9fd"
PLANKS = "ribbits:mossy_oak_planks"
SLAB = PLANKS + "_slab"


def member(name):
    return name.replace("bbb:", NS + ":").replace("oak", "mossy_oak")


def moss_sheet(sheet, oak, moss, rectangles):
    # Native 16px tiles keep their UVs. The packed Frame and Pallet sheets
    # have independent face islands. Register each island to the closest oak
    # grain by counting exact texel matches at each possible tile offset.
    coords = {}
    if sheet.size == (16, 16):
        coords = {(x, y): (x, y) for y in range(16) for x in range(16)}
    else:
        assert sheet.size == (32, 32) and rectangles
        for x0, y0, x1, y1 in sorted(rectangles, key=lambda r: (-(r[2]-r[0])*(r[3]-r[1]), r)):
            points = [(x,y) for y in range(y0,y1) for x in range(x0,x1) if sheet.getpixel((x,y))[3]]
            if not points:
                continue
            def score(offset):
                dx, dy = offset
                return sum(sheet.getpixel((x,y)) == oak.getpixel(((x+dx)%16,(y+dy)%16)) for x,y in points)
            dx, dy = max(((dx,dy) for dy in range(16) for dx in range(16)), key=score)
            for x,y in points:
                coords.setdefault((x,y), ((x+dx)%16,(y+dy)%16))
    result = sheet.copy()
    for xy, uv in coords.items():
        pixel = sheet.getpixel(xy)
        before, after = oak.getpixel(uv), moss.getpixel(uv)
        # Four olive shades. Even the greenest native oak has R-G >= 23.
        if pixel[3] and after[0]-after[1] <= 13 and before != after:
            gain = light(pixel) / light(before)
            result.putpixel(xy, (*[min(255, round(c*gain)) for c in after[:3]], pixel[3]))
    return result


def stage(root, client, bbb, ribbits_path, write_json, recipe_unlock):
    assert hashlib.sha256(ribbits_path.read_bytes()).hexdigest() == RIBBITS_SHA
    with ZipFile(ribbits_path) as ribbits:
        oak = image(client, "assets/minecraft/textures/block/oak_planks.png")
        moss = image(ribbits, "assets/ribbits/textures/block/mossy_oak_planks.png")
        for name in (PLANKS, SLAB):
            ribbits.getinfo("assets/ribbits/items/" + name.split(":")[1] + ".json")
    models, sprites, regions = {}, {}, defaultdict(set)
    def load(path):
        return json.loads(bbb.read(path))
    def bind_model(ref):
        if "/plants/" in ref or "/template/" in ref or not ref.startswith("bbb:"):
            return ref
        if ref in models:
            return member(ref)
        assert "/oak" in ref
        model = load("assets/bbb/models/" + ref[4:] + ".json")
        models[ref] = model
        slots = model.get("textures", {})
        for slot, tex in list(slots.items()):
            if tex in ("block/oak_planks","minecraft:block/oak_planks"):
                slots[slot] = "ribbits:block/mossy_oak_planks"
            elif tex.startswith("bbb:") or tex in ("block/oak_log", "block/oak_log_top"):
                source = tex if ":" in tex else "minecraft:" + tex
                ns, path = source.split(":")
                sprite = NS + ":" + path.replace("oak","mossy_oak")
                sprites[source] = sprite
                slots[slot] = sprite
            else:
                raise ValueError((ref, tex))
        if "parent" in model:
            model["parent"] = bind_model(model["parent"])
        write_json(root, f"assets/{NS}/models/{member(ref).split(':')[1]}.json", model)
        return member(ref)

    labels, tags = {}, defaultdict(list)
    for form in FORMS:
        name = "oak_" + form
        target = member(name)
        labels[f"block.{NS}.{target}"] = target.replace("_"," ").title()
        for folder in ("blockstates","items"):
            obj = load(f"assets/bbb/{folder}/{name}.json")
            nodes = ([obj["model"]] if folder == "items" else obj["variants"].values() if "variants" in obj
                     else (part["apply"] for part in obj["multipart"]))
            for node in nodes:
                node["model"] = bind_model(node["model"])
            write_json(root,f"assets/{NS}/{folder}/{target}.json",obj)
        own = f"{NS}:{target}"
        for folder in ("recipe","loot_table/blocks","advancement/recipes"):
            path = f"data/bbb/{folder}/{name}.json"
            if path not in bbb.NameToInfo:
                assert folder == "advancement/recipes"
                if form == "balustrade":
                    recipe_unlock(root,target,"moss_block","building_blocks")
                else:
                    recipe_unlock(root,target,"mossy_oak_planks","building_blocks",namespace="ribbits")
                continue
            obj = json.loads(bbb.read(path).decode().replace("bbb:"+name,own)
                             .replace("minecraft:oak_planks",PLANKS).replace("minecraft:oak_slab",SLAB)
                             .replace("#minecraft:oak_logs",SLAB if form == "frame" else PLANKS))
            if folder == "recipe" and form == "balustrade":
                assert obj["key"] == {"A":"minecraft:stripped_oak_log"}
                obj["key"]["M"] = "minecraft:moss_block"
                obj["pattern"] = [" A "," AM"," A "]
            write_json(root,f"data/{NS}/{folder}/{target}.json",obj)
        for path in bbb.namelist():
            if "/tags/" in path and path.endswith(".json") and "bbb:"+name in load(path)["values"]:
                tags[path].append(own)
    for path, values in tags.items():
        write_json(root,path,{"replace":False,"values":values})
    write_json(root,f"assets/{NS}/lang/en_us.json",labels)

    # Resolve the original Oak model inheritance before reading packed UVs.
    for ref in models:
        key = ref
        slots, elements = {}, None
        while key.startswith("bbb:"):
            node = load("assets/bbb/models/"+key[4:]+".json")
            for slot, tex in node.get("textures",{}).items():
                slots.setdefault(slot,tex)
            if elements is None:
                elements = node.get("elements")
            key = node.get("parent","")
        for el in elements or ():
            for face in el["faces"].values():
                tex = face["texture"]
                while tex.startswith("#"):
                    tex = slots[tex[1:]]
                if tex in ("bbb:block/frame/oak","bbb:block/frame/oak_sticks","bbb:block/pallet/oak_pallet"):
                    u,v,s,t = [round(2*c) for c in face["uv"]]
                    regions[tex].add((min(u,s),min(v,t),max(u,s),max(v,t)))
    for source,dest in sprites.items():
        ns,path = source.split(":")
        sheet = image(bbb if ns == "bbb" else client,f"assets/{ns}/textures/{path}.png")
        output = root / f"assets/{NS}/textures/{dest.split(':')[1]}.png"
        output.parent.mkdir(parents=True,exist_ok=True)
        moss_sheet(sheet,oak,moss,regions[source]).save(output)
