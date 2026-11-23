"""BBB model graph rebinding and genuine Mossy Oak boat acquisition."""
from doors import read, resource, wood_tile, write, palette
from masonry import NS, rank_mapping
from mossy_oak import FORMS, moss_sheet
from wood_matrix import copy_tags, remap


def stage_details(root, vanilla, bbb, trees, ribbits, unlock):
    pack=root/"resourcepacks/mynx_trees"
    labels={}
    for wood in ("silver_birch","wisteria"):
        plank=wood_tile(trees,"mynx_trees:block/"+wood+"_planks")
        pairs={}
        def bind(ref):
            if not isinstance(ref,str) or "oak" not in ref or "/lattice/plants/" in ref:
                return ref
            if ref.startswith("block/") or ref.startswith("minecraft:") or ref.startswith("#minecraft:"):
                return ("mynx_trees:"+ref if ref.startswith("block/") else ref.replace("minecraft:","mynx_trees:")).replace("oak",wood)
            if ref.startswith("bbb:"):
                return ref.replace("bbb:",NS+":").replace("oak",wood)
            return ref
        def graph(obj):
            if isinstance(obj,list):
                return [graph(v) for v in obj]
            if isinstance(obj,dict):
                return {k:graph(v) for k,v in obj.items()}
            return bind(obj)
        seen=set()
        def stage_model(ref):
            if bind(ref)==ref or ref in seen:
                return
            seen.add(ref)
            model=read(bbb,resource(ref,"models","json"))
            if "parent" in model:
                stage_model(model["parent"])
            for tex in model.get("textures",{}).values():
                if not tex.startswith("bbb:") or tex==bind(tex):
                    continue
                tile=wood_tile(bbb,tex)
                mask={(x,y) for y in range(tile.height) for x in range(tile.width) if tile.getpixel((x,y))[3]}
                if "/lantern/" in tex:
                    mask={xy for xy in mask if xy[1]<2 and xy[0]<20 or xy[0]<10 and xy[1]<10}
                mapping=rank_mapping({tile.getpixel(xy)[:3] for xy in mask},palette(plank))
                for xy in mask:
                    p=tile.getpixel(xy);tile.putpixel(xy,(*mapping[p[:3]],p[3]))
                dest=pack/resource(bind(tex),"textures","png");dest.parent.mkdir(parents=True,exist_ok=True);tile.save(dest)
            write(pack,resource(bind(ref),"models","json"),graph(model))
        def visit(obj):
            if isinstance(obj,list):
                for v in obj:visit(v)
            elif isinstance(obj,dict):
                for k,v in obj.items():
                    if k=="model" and isinstance(v,str):stage_model(v)
                    else:visit(v)
        for form in (*FORMS,"lantern"):
            name="oak_"+form; ref="bbb:"+name; target=bind(ref); pairs[ref]=target
            for folder in ("items","blockstates"):
                data=read(bbb,resource(ref,folder,"json"));visit(data);write(pack,resource(target,folder,"json"),graph(data))
            for folder in ("recipe","loot_table/blocks"):
                data=read(bbb,f"data/bbb/{folder}/{name}.json")
                write(pack,f"data/{NS}/{folder}/{wood}_{form}.json",graph(data))
            unlock(pack,wood+"_"+form,wood+"_planks","building_blocks",namespace="mynx_trees")
            labels[f"block.{NS}.{wood}_{form}"]=(wood+"_"+form).replace("_"," ").title()
        copy_tags(pack,bbb,pairs)
    write(pack,f"assets/{NS}/lang/en_us.json",labels)

    pack=root/"resourcepacks/ribbits"
    oak=wood_tile(vanilla,"minecraft:block/oak_planks")
    moss=wood_tile(ribbits,"ribbits:block/mossy_oak_planks")
    pairs={"minecraft:oak_planks":"ribbits:mossy_oak_planks"}
    for kind in ("boat","chest_boat"):
        old="minecraft:oak_"+kind; new=NS+":mossy_oak_"+kind
        pairs.update({old:new,"minecraft:item/oak_"+kind:NS+":item/mossy_oak_"+kind})
    labels_path=pack/f"assets/{NS}/lang/en_us.json"
    import json
    labels=json.loads(labels_path.read_text())
    for kind in ("boat","chest_boat"):
        name="mossy_oak_"+kind; old="oak_"+kind
        for folder in ("items","models/item"):
            write(pack,f"assets/{NS}/{folder}/{name}.json",remap(read(vanilla,f"assets/minecraft/{folder}/{old}.json"),pairs))
        write(pack,f"data/{NS}/recipe/{name}.json",remap(read(vanilla,f"data/minecraft/recipe/{old}.json"),pairs))
        unlock(pack,name,"mossy_oak_planks","transportation",namespace="ribbits")
        for prefix in ("entity","item"):
            labels[f"{prefix}.{NS}.{name}"]=name.replace("_"," ").title()
        for src,dest in (("item/"+old,"item/"+name),("entity/"+kind+"/oak","entity/"+kind+"/mossy_oak")):
            art=wood_tile(vanilla,"minecraft:"+src)
            # The chest occupies the lower half of the chest boat sheet. Keep its UV islands intact.
            for y in range(0, min(art.height,64),16):
                for x in range(0,art.width,16):
                    tile=art.crop((x,y,x+16,y+16))
                    treated=moss_sheet(tile,oak,moss,())
                    if kind=="chest_boat" and src.startswith("item/"):
                        for xy in ((xx,yy) for yy in range(16) for xx in range(16)):
                            if tile.getpixel(xy) != wood_tile(vanilla,"minecraft:item/oak_boat").getpixel(xy):
                                treated.putpixel(xy,tile.getpixel(xy))
                    art.paste(treated,(x,y))
            f=pack/resource(NS+":"+dest,"textures","png");f.parent.mkdir(parents=True,exist_ok=True);art.save(f)
    copy_tags(pack,vanilla,{k:v for k,v in pairs.items() if k.endswith("_boat") and "/" not in k}, kinds=("/tags/item/","/tags/entity_type/"))
    write(pack,labels_path.relative_to(pack).as_posix(),labels)



def stage_mossy_recipes(root):
    """Use the same actual provider recipe and log slots as the corresponding Silver Birch form."""
    import json
    pack=root/"resourcepacks/mossy_oak_timber"
    legacy=root/"resourcepacks/mossy_oak_legacy_recipes"
    def relocate(old, path, data, optional=None):
        previous=json.loads(old.read_text())
        if optional:
            condition=[{"condition":"fabric:all_mods_loaded","values":[optional]}]
            data["fabric:load_conditions"]=condition
            previous["fabric:load_conditions"]=condition
        write(legacy,path,previous)
        write(pack,path,data)
        old.unlink()
    for suffix, optional in (("",None),("_doors","mcwdoors"),("_trapdoors","mcwtrpdoors")):
        recipes=f"data/{NS}/recipe"
        folder=root/"resourcepacks"/("mynx_trees"+suffix)/recipes
        for src in folder.glob("*silver_birch*.json"):
            own=json.loads(src.read_text())
            if not any(wood in json.dumps(own) for wood in ("silver_birch_log","silver_birch_wood")):
                continue
            name=src.name.replace("silver_birch","mossy_oak")
            old=root/"resourcepacks"/("ribbits"+suffix)/recipes/name
            if not old.exists():
                continue
            data=json.loads(json.dumps(own).replace("silver_birch","mossy_oak"))
            relocate(old,recipes+"/"+name,data,optional)
    for form in ("shelf","hanging_sign"):
        path=f"data/{NS}/recipe/mossy_oak_{form}.json"
        data=json.loads((root/"resourcepacks/ribbits"/path).read_text())
        data["key"]["#"]="mynx_trees:stripped_mossy_oak_log"
        relocate(root/"resourcepacks/ribbits"/path,path,data)
