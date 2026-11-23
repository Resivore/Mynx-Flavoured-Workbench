"""Build native timber resources from the target client and the owner's private artwork."""
from io import BytesIO
import json
from pathlib import Path, PurePosixPath
import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[2] / "architectural-material-closure/tools"))
from mossy_oak import moss_sheet
from zipfile import ZipFile
from PIL import Image

NS = "mynx_trees"
TEXTURES = f"assets/{NS}/textures/"
TIMBER = {"birch_log", "birch_wood", "stripped_birch_log", "stripped_birch_wood"}
NATIVE = TIMBER | {"birch_" + suffix for suffix in (
    "planks", "stairs", "slab", "fence", "fence_gate", "button", "pressure_plate", "door", "trapdoor",
    "sign", "wall_sign", "hanging_sign", "wall_hanging_sign", "shelf", "boat", "chest_boat")}


def remap(value, wood):
    if isinstance(value, str) and "birch" in value and "minecraft:" in value:
        return value.replace("minecraft:", NS + ":").replace("birch", wood)
    if isinstance(value, list):
        return [remap(v, wood) for v in value]
    if isinstance(value, dict):
        return {k: remap(v, wood) for k, v in value.items()}
    return value


def pixels(raw):
    return Image.open(BytesIO(raw)).convert("RGBA")


def png(im):
    data = BytesIO()
    im.save(data, format="PNG")
    return data.getvalue()


def transfer(raw, palette, *, wood_only=False):
    """Map illuminated wood shades along the target ramp, preserving silhouettes and neutral fittings."""
    art = pixels(raw)
    light = lambda c: 0.2126*c[0] + 0.7152*c[1] + 0.0722*c[2]
    ramp = sorted({p[:3] for p in pixels(palette).getdata() if p[3]}, key=light)
    def selected(p):
        return p[3] and (not wood_only or p[0] > p[1] and p[1] > p[2] + 6)
    shades = sorted({p[:3] for p in art.getdata() if selected(p)}, key=light)
    if not shades:
        raise ValueError("Source wood mask is empty")
    low, high = light(shades[0]), light(shades[-1])
    def shade(rgb):
        t = (light(rgb) - low) / max(high-low, 1) * (len(ramp)-1)
        i = int(t)
        return tuple(round(a+(b-a)*(t-i)) for a,b in zip(ramp[i], ramp[min(i+1,len(ramp)-1)]))
    mapping = {rgb: shade(rgb) for rgb in shades}
    art.putdata([(*mapping[p[:3]],p[3]) if selected(p) else p for p in art.getdata()])
    return png(art)


def stage_art(mc, common, write):
    folder = common / "originals/assets/wisteria assets"
    goop = common / "originals/resourcepacks/Foundation v2.zip"
    ribbits = common / "projects/ribbits-26.2/artifacts/ribbits-private-reconstruction-4.1.6+26.2-mynx-canary27.jar"
    def save(name, data):
        write(TEXTURES + name + ".png", data)
    with ZipFile(goop) as pack, ZipFile(folder / "codex generated muted-violet-willow-textures.zip") as willow:
        for wood in ("silver_birch", "wisteria"):
            plank = (folder/"wisteria planks.png").read_bytes() if wood == "wisteria" else pack.read("assets/minecraft/textures/block/birch_planks.png")
            fixed = {"block/birch_planks": plank}
            if wood == "wisteria":
                fixed.update({
                    "block/birch_log_top": (folder/"wisteria top log.png").read_bytes(),
                    "block/stripped_birch_log_top": (folder/"wisteria stripped top log.png").read_bytes(),
                    "block/stripped_birch_log": transfer((folder/"wisteria stripped side - needs luminance recolor.png").read_bytes(), plank),
                })
                for category in ("block/willow_door_bottom", "block/willow_door_top", "block/willow_trapdoor",
                                 "item/willow_door", "item/willow_sign", "item/willow_hanging_sign", "item/willow_boat",
                                 "item/willow_chest_boat", "entity/boat/willow", "entity/chest_boat/willow", "gui/hanging_signs/willow"):
                    fixed[category.replace("willow","birch")] = willow.read("assets/regions_unexplored/textures/" + category + ".png")
            else:
                for name in ("birch_log_top", "stripped_birch_log_top"):
                    fixed["block/" + name] = (common / "private/assets/silver-birch" / (name + ".png")).read_bytes()
            for path in mc.namelist():
                prefix = "assets/minecraft/textures/"
                if not path.startswith(prefix) or not path.endswith(".png"):
                    continue
                key = path[len(prefix):-4]
                if "birch" not in key or any(s in key for s in ("leaves","sapling","potted")) or key == "block/birch_log":
                    continue
                if key not in fixed:
                    raw = mc.read(path)
                    fixed[key] = pack.read(path) if wood == "silver_birch" and path in pack.NameToInfo else transfer(raw, plank, wood_only=True)
                save(key.replace("birch", wood), fixed[key])
    with ZipFile(ribbits) as archive:
        moss = pixels(archive.read("assets/ribbits/textures/block/mossy_oak_planks.png"))
        oak = pixels(mc.read("assets/minecraft/textures/block/oak_planks.png"))
        for name in ("oak_log", "oak_log_top", "stripped_oak_log", "stripped_oak_log_top"):
            target = pixels(mc.read(f"assets/minecraft/textures/block/{name}.png"))
            save("block/" + name.replace("oak", "mossy_oak"), png(moss_sheet(target, oak, moss, ())))


def stage(mc, common, out, write):
    stage_art(mc, common, write)
    lang_path = f"assets/{NS}/lang/en_us.json"
    lang = json.loads((out/lang_path).read_text())
    tags = {}
    for wood in ("silver_birch", "wisteria", "mossy_oak"):
        members = TIMBER if wood == "mossy_oak" else NATIVE
        rename = lambda p: p.replace("/minecraft/", f"/{NS}/",1).replace("birch",wood)
        roots = set()
        for path in mc.namelist():
            if not path.endswith(".json"):
                continue
            leaf = PurePosixPath(path).stem
            if path.startswith(("assets/minecraft/blockstates/", "assets/minecraft/items/", "data/minecraft/loot_table/blocks/")):
                if leaf not in members or (wood != "mossy_oak" and leaf in ("birch_log","birch_wood")):
                    continue
                roots.add(path)
            elif path.startswith(("data/minecraft/recipe/","data/minecraft/advancement/recipes/")):
                if leaf not in members or (wood != "mossy_oak" and leaf in ("birch_planks","birch_wood")):
                    continue
                roots.add(path)
            elif path.startswith("data/minecraft/tags/") and leaf != "birch_logs":
                values = json.loads(mc.read(path)).get("values",[])
                for v in values:
                    if isinstance(v,str) and (v.removeprefix("minecraft:") in members or v == "#minecraft:birch_logs"):
                        tags.setdefault(path,[]).append(remap(v,wood))
        def clone(path):
            if path in written:
                return
            written.add(path)
            obj=json.loads(mc.read(path))
            # Advance through actual state and item model references, leaving shared templates in Minecraft.
            def refs(node):
                if isinstance(node,dict):
                    for k,v in node.items():
                        if k in ("model","parent") and isinstance(v,str) and v.startswith("minecraft:") and "birch" in v:
                            clone("assets/minecraft/models/" + v.split(":",1)[1] + ".json")
                        refs(v)
                elif isinstance(node,list):
                    for child in node:
                        refs(child)
            refs(obj)
            write(rename(path), remap(obj,wood))
        written=set()
        for path in sorted(roots):
            clone(path)
        for kind in ("block","item"):
            write(f"data/{NS}/tags/{kind}/{wood}_logs.json",{"values":[NS+":"+n.replace("birch",wood) for n in sorted(TIMBER)]})
        # Keep the released individual recipes and their ids; add direct stripped variants.
        for base in sorted(TIMBER):
            if wood != "mossy_oak" and not base.startswith("stripped_"):
                continue
            name=base.replace("birch",wood)
            target=("ribbits" if wood=="mossy_oak" else NS)+":"+wood+"_planks"
            condition={"fabric:load_conditions":[{"condition":"fabric:all_mods_loaded","values":["ribbits"]}]} if wood=="mossy_oak" else {}
            recipe=name+"_planks"
            write(f"data/{NS}/recipe/{recipe}.json", {**condition,"type":"minecraft:crafting_shapeless","category":"building","group":"planks",
                                                      "ingredients":[NS+":"+name],"result":{"id":target,"count":4}})
            advancement=remap(json.loads(mc.read("data/minecraft/advancement/recipes/building_blocks/birch_planks.json")),wood)
            for key, criterion in advancement["criteria"].items():
                if criterion["trigger"]=="minecraft:inventory_changed":
                    criterion["conditions"]["items"]=[{"items":NS+":"+name}]
                else:
                    criterion["conditions"]["recipe"]=NS+":"+recipe
            advancement["rewards"]={"recipes":[NS+":"+recipe]}
            write(f"data/{NS}/advancement/recipes/building_blocks/{recipe}.json",{**condition,**advancement})
        for base in sorted(members):
            name=base.replace("birch",wood)
            label=name.replace("_"," ").title()
            lang[f"block.{NS}.{name}"]=label
            if base.endswith("_boat"):
                lang[f"item.{NS}.{name}"]=label
                lang[f"entity.{NS}.{name}"]=label
    for path, adds in tags.items():
        tag=json.loads((out/path).read_text()) if (out/path).is_file() else {"replace":False,"values":[]}
        tag["values"]=list(dict.fromkeys(tag["values"]+adds))
        write(path,tag)
    write(lang_path,lang)
