"""Provider-bound wood accessories. All derived art stays in the local AMC stage."""
import json
from pathlib import Path
from shutil import copyfile
from PIL import Image
from bamboo import existing
from doors import MATERIALS, STEMS, read, resource, transfer, wood_tile, write
from masonry import NS
from mossy_oak import moss_sheet

FORMS = ("planks_path","log_parapet","plank_parapet","blinds","curtain_rod","shutter","louvered_shutter")
SHUTTER_ART = Path(__file__).with_name("shutter_art")
# C33 top-edge sampling for the two owner panels. Both broad faces use
# columns 0..10 and rows 1..15; the two-column side strip is separate.
TOP_EDGE_COLUMNS = {
    "celestial": (0, 1, 2, 3, 4, 6, 8, 10, 12, 13, 15),
    "murublight": (0, 2, 4, 5, 6, 7, 8, 9, 10, 11, 14),
}


def write_regular_shutter(path, wood, template, provider, ns, originals):
    if ns == "mynx_trees":
        if wood == "wisteria":
            copyfile(originals / "assets/wisteria assets/wisteria shutter.png", path)
        else:
            from zipfile import ZipFile
            with ZipFile(originals.parent / "resourcepacks/foundation/artifacts/Foundation v10.zip") as goop:
                path.write_bytes(goop.read("assets/mcwwindows/textures/block/birch_shutter.png"))
        return
    if wood in ("veiled", "mossy_oak"):
        copyfile(SHUTTER_ART / f"{wood}_full_texture.png", path)
        return
    tile = wood_tile(provider, f"{ns}:block/{wood}_trapdoor")
    atlas = template.copy()
    for x, source_x in enumerate(TOP_EDGE_COLUMNS[wood]):
        atlas.putpixel((x, 0), tile.getpixel((source_x, 0)))
    atlas.paste(tile.crop((14, 0, 16, 16)), (14, 0))
    with Image.open(SHUTTER_ART / f"{wood}_front_panel.png") as panel:
        assert panel.size == (11, 15)
        atlas.paste(panel.convert("RGBA"), (0, 1))
    atlas.save(path)


def stage(root, vanilla, windows, paths, end, ribbits, trees, originals, unlock):
    oak=wood_tile(vanilla,"minecraft:block/oak_planks")
    moss=wood_tile(ribbits,"ribbits:block/mossy_oak_planks")
    for wood,ns in MATERIALS.items():
        rib=ns=="ribbits"
        provider={"ribbits":ribbits,"enderscape":end,"mynx_trees":trees}[ns]
        pack=root/"resourcepacks"/ns
        log=wood+("_stem" if wood in STEMS else "_log")
        plank=wood+"_planks"
        slab=plank+"_slab" if rib else wood+"_slab"
        def native(name):
            return ns+":block/"+name
        roles={"oak_log":(NS if rib else ns)+":block/"+log,
               "stripped_oak_log":(NS if rib else ns)+":block/stripped_"+log,
               "oak_planks":native(plank),"warped_stem":native(log)}
        if rib:
            tile=moss_sheet(wood_tile(vanilla,"minecraft:block/stripped_oak_log"),oak,moss,())
            dest=pack/resource(roles["stripped_oak_log"],"textures","png")
            dest.parent.mkdir(parents=True,exist_ok=True)
            tile.save(dest)
        labels_path=f"assets/{NS}/lang/en_us.json"
        labels=existing(pack,labels_path,{})
        for form in (("button","pressure_plate") if rib else ())+FORMS:
            control=form in ("button","pressure_plate")
            if form=="planks_path":
                jar,source_ns=paths,"mcwpaths"
            elif control:
                jar,source_ns=vanilla,"minecraft"
            else:
                jar,source_ns=windows,"mcwwindows"
            stem=form=="log_parapet" and wood in STEMS
            srcwood="warped" if stem else "oak"
            suffix="stem_parapet" if stem else form
            src,dst=srcwood+"_"+suffix,wood+"_"+suffix
            old,target=source_ns+":"+src,NS+":"+dst
            for authority,owner in ((jar,source_ns),(provider,ns)):
                assert resource(owner+":"+dst,"items","json") not in authority.NameToInfo
            def rename(ref):
                return ref.replace(source_ns+":",NS+":").replace("/"+srcwood+"_","/"+wood+"_")
            def sprite(ref):
                if ref.startswith("minecraft:block/"):
                    return roles[ref.split("/")[-1]]
                assert ref==f"{source_ns}:block/{src}",ref
                dest=NS+":block/"+dst
                path=pack/resource(dest,"textures","png")
                tile=wood_tile(jar,ref)
                path.parent.mkdir(parents=True,exist_ok=True)
                if form=="shutter":
                    write_regular_shutter(path, wood, tile, provider, ns, originals)
                    return dest
                assert form in ("planks_path","louvered_shutter")
                mask={(x,y) for y in range(16) for x in range(16) if tile.getpixel((x,y))[3]==255}
                palette=wood_tile(provider,native(plank))
                art=transfer({ref:(tile,mask)},wood,oak,moss,palette,palette)[ref]
                art.save(path)
                return dest
            models=set()
            def bind(node):
                ref=node["model"]
                if ref=="minecraft:block/air":return
                new=rename(ref)
                node["model"]=new
                if new in models:return
                model=read(jar,resource(ref,"models","json"))
                model["textures"]={k:sprite(v) for k,v in model["textures"].items()}
                write(pack,resource(new,"models","json"),model)
                models.add(new)
            state=read(jar,resource(old,"blockstates","json"))
            for branch in state.get("variants",{}).values():bind(branch)
            for part in state.get("multipart",[]):bind(part["apply"])
            write(pack,resource(target,"blockstates","json"),state)
            item=read(jar,resource(old,"items","json"))
            bind(item["model"])
            write(pack,resource(target,"items","json"),item)
            recipe=read(jar,f"data/{source_ns}/recipe/{src}.json")
            ingredients={"minecraft:oak_planks":ns+":"+plank,"minecraft:oak_slab":ns+":"+slab,
                         "minecraft:"+srcwood+("_stem" if stem else "_log"):ns+":"+(slab if rib else log),
                         "minecraft:oak_trapdoor":NS+":mossy_oak_classic_trapdoor" if rib else ns+":"+wood+"_trapdoor"}
            if "key" in recipe:
                recipe["key"]={k:ingredients.get(v,v) for k,v in recipe["key"].items()}
            else:
                recipe["ingredients"]=[ingredients[v] for v in recipe["ingredients"]]
            recipe["result"]["id"]=target
            recipe_pack=root/"resourcepacks/ribbits_trapdoors" if rib and form=="shutter" else pack
            write(recipe_pack,f"data/{NS}/recipe/{dst}.json",recipe)
            unlock(recipe_pack,dst,plank,"redstone" if control else "building_blocks",namespace=ns)
            loot=jar.read(f"data/{source_ns}/loot_table/blocks/{src}.json").decode().replace(old,target)
            loot=loot.replace("minecraft:blocks/"+src,NS+":blocks/"+dst)
            write(pack,f"data/{NS}/loot_table/blocks/{dst}.json",json.loads(loot))
            en=read(jar,f"assets/{source_ns}/lang/en_us.json")
            name=en[("block" if control else "item")+"."+source_ns+"."+src].replace(srcwood.title(),wood.replace("_"," ").title(),1)
            for kind in ("item","block"):labels[f"{kind}.{NS}.{dst}"]=name
            for path in jar.namelist():
                if path.endswith(".json") and any(t in path for t in ("/tags/block/","/tags/item/")) and old in read(jar,path)["values"]:
                    tag=existing(pack,path,{"replace":False,"values":[]})
                    if target not in tag["values"]:tag["values"].append(target)
                    write(pack,path,tag)
        write(pack,labels_path,labels)
