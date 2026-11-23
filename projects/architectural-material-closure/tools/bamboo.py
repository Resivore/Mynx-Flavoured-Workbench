"""Stage the six missing Bamboo designs in AMC's local resource output."""
import json
from PIL import Image

from doors import read, resource, wood_tile, write
from masonry import NS

WINDOW_FORMS = ("log_parapet", "plank_parapet", "blinds", "curtain_rod", "louvered_shutter")
PLANKS = "minecraft:block/bamboo_planks"


def existing(root, path, default):
    file = root / path
    return json.loads(file.read_text(encoding="utf8")) if file.exists() else default


def bind_timbers(model, archive, form):
    # The provider points every face at side grain. Identify the timber's local
    # length before the provider's element rotation, including the Ranch joint.
    model["elements"] = read(archive, resource(model["parent"], "models", "json"))["elements"]
    for index, element in enumerate(model["elements"]):
        if form == "ranch_trapdoor":
            along_x = index not in (2, 3)
        else:
            along_x = model["parent"].endswith("/parapet")
        ends = ("east","west") if along_x else ("north","south")
        for direction, face in element["faces"].items():
            original = face["texture"][1:]
            side = model["textures"][original]
            if direction in ends:
                slot = original + "_end"
                model["textures"][slot] = side + "_top"
                face["texture"] = "#" + slot
            else:
                # On vertical faces U runs along the member. On broad faces the
                # Z rails already run along V. Retain the authored UV rotations.
                across_u = direction not in ("up","down") or along_x
                if face.get("rotation",0) in (90,270):
                    across_u = not across_u
                if across_u:
                    slot = original + "_horizontal"
                    dest = side.replace("minecraft:", NS+":") + "_horizontal"
                    model["textures"][slot] = dest
                    face["texture"] = "#" + slot


def stage(root, client, windows, traps, shade_shutter, unlock):
    # Both packs use this lossless rotation to keep longitudinal grain in the
    # provider's original (often non-square) UV islands.
    horizontal = root / resource(NS+":block/bamboo_block_horizontal","textures","png")
    horizontal.parent.mkdir(parents=True,exist_ok=True)
    wood_tile(client,"minecraft:block/bamboo_block").transpose(Image.Transpose.ROTATE_90).save(horizontal)
    for form in (*WINDOW_FORMS,"ranch_trapdoor"):
        ranch = form == "ranch_trapdoor"
        pack = root/"resourcepacks/bamboo_trapdoors" if ranch else root
        ns, archive = ("mcwtrpdoors",traps) if ranch else ("mcwwindows",windows)
        src, dst = "oak_"+form, "bamboo_"+form
        old, new = ns+":"+src, NS+":"+dst
        assert resource(ns+":"+dst,"items","json") not in archive.NameToInfo
        def rename(ref):
            return ref.replace(ns+":",NS+":").replace("/oak_","/bamboo_")
        models = set()
        def bind(branch):
            ref = branch["model"]
            if ref == "minecraft:block/air":
                return
            assert ref.startswith(ns+":") and "/oak_" in ref
            dest = rename(ref)
            branch["model"] = dest
            if dest in models:
                return
            model = read(archive,resource(ref,"models","json"))
            for slot, tex in model["textures"].items():
                if form in ("log_parapet","ranch_trapdoor"):
                    assert tex in ("minecraft:block/oak_log","minecraft:block/stripped_oak_log")
                    value = "minecraft:block/bamboo_block"
                elif form == "louvered_shutter":
                    assert tex == "mcwwindows:block/oak_louvered_shutter"
                    value = NS+":block/bamboo_louvered_shutter"
                else:
                    assert tex in ("minecraft:block/oak_planks","minecraft:block/oak_log","minecraft:block/stripped_oak_log")
                    value = PLANKS
                model["textures"][slot] = value
            if form in ("log_parapet","ranch_trapdoor"):
                bind_timbers(model,archive,form)
            write(pack,resource(dest,"models","json"),model)
            models.add(dest)

        state = read(archive,resource(old,"blockstates","json"))
        for branch in state.get("variants",{}).values():
            bind(branch)
        for part in state.get("multipart",[]):
            bind(part["apply"])
        write(pack,resource(new,"blockstates","json"),state)
        item = read(archive,resource(old,"items","json"))
        bind(item["model"])
        write(pack,resource(new,"items","json"),item)
        if form == "louvered_shutter":
            shade_shutter(windows,client,pack/resource(NS+":block/"+dst,"textures","png"),"bamboo_planks",reference="oak")
        recipe = read(archive,f"data/{ns}/recipe/{src}.json")
        replacements = {"minecraft:oak_planks":"minecraft:bamboo_planks","minecraft:oak_slab":"minecraft:bamboo_slab",
                        "minecraft:oak_log":"minecraft:bamboo_block"}
        material, = [replacements[v] for v in recipe["key"].values() if v in replacements]
        recipe["key"] = {k:replacements.get(v,v) for k,v in recipe["key"].items()}
        recipe["result"]["id"] = new
        write(pack,f"data/{NS}/recipe/{dst}.json",recipe)
        unlock(pack,dst,material.split(":")[1],"building_blocks")
        loot = archive.read(f"data/{ns}/loot_table/blocks/{src}.json").decode().replace(old,new)
        write(pack,f"data/{NS}/loot_table/blocks/{dst}.json",json.loads(loot))
        lang = f"assets/{NS}/lang/en_us.json"
        labels = existing(pack,lang,{})
        label = read(archive,f"assets/{ns}/lang/en_us.json")["item."+ns+"."+src].replace("Oak","Bamboo")
        for prefix in ("block","item"):
            labels[f"{prefix}.{NS}.{dst}"] = label
        write(pack,lang,labels)
        for path in archive.namelist():
            if not path.endswith(".json") or not any(t in path for t in ("/tags/block/","/tags/item/")):
                continue
            if old in read(archive,path)["values"]:
                tag = existing(pack,path,{"replace":False,"values":[]})
                if new not in tag["values"]:
                    tag["values"].append(new)
                write(pack,path,tag)
