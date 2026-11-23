"""Generate optional wood trapdoors from the retained Macaw resource graphs."""
from collections import defaultdict
import hashlib
from pathlib import Path

from PIL import Image
from doors import MATERIALS, STEMS, WOODS, read, resource, transfer, wood_tile, write
from masonry import NS
from mossy_oak import moss_sheet

MACAW = "mcwtrpdoors"
SHA = "6411cb0ff6c6cc4312deed48c3ca69cdbcbae72e80ff4ed0269f2d0a78acd32d"


def reference(design, wood):
    if design == "classic":
        return "spruce"
    return "warped" if design in ("ranch", "bark") and wood in STEMS else "oak"


def designs(macaw):
    names = set()
    for path in macaw.namelist():
        if path.startswith("assets/mcwtrpdoors/blockstates/") and path.endswith("_trapdoor.json"):
            stem = Path(path).stem
            for wood in WOODS:
                if stem.startswith(wood + "_") and stem != wood + "_trapdoor":
                    names.add(stem[len(wood)+1:-9])
                    break
    return sorted(names)


def mask(macaw, ref, wood, design):
    tile = wood_tile(macaw, ref)
    # Independent warm and cool peer woods identify the same moving wood
    # detail. Glass, metal, paper and air stay fixed at their original UVs.
    peers = ("jungle" if design in ("paper","tropical") else "acacia", "crimson" if design == "mystic" else "warped")
    masks = []
    for peer in peers:
        other = wood_tile(macaw, ref.replace("/"+wood+"_", "/"+peer+"_"))
        masks.append({(x,y) for y in range(16) for x in range(16)
                      if tile.getpixel((x,y))[3] == 255 and tile.getpixel((x,y)) != other.getpixel((x,y))})
    assert tile.size == (16,16) and masks[0] == masks[1] and masks[0], ref
    return tile, masks[0]


def stage(root, vanilla, macaw, enderscape, ribbits):
    assert hashlib.sha256(Path(macaw.filename).read_bytes()).hexdigest() == SHA
    oak = wood_tile(vanilla, "minecraft:block/oak_planks")
    moss = wood_tile(ribbits, "ribbits:block/mossy_oak_planks")
    labels = read(macaw,"assets/mcwtrpdoors/lang/en_us.json")
    tags = {n:read(macaw,n)["values"] for n in macaw.namelist()
            if n.startswith("data/minecraft/tags/") and n.endswith(".json")}
    for provider, archive in (("enderscape",enderscape), ("ribbits",ribbits)):
        pack = root / "resourcepacks" / (provider+"_trapdoors")
        names, members = {}, defaultdict(list)
        for wood, namespace in MATERIALS.items():
            if namespace != provider:
                continue
            rib = provider == "ribbits"
            log = wood + ("_stem" if wood in STEMS else "_log")
            planks = provider + ":" + wood + "_planks"
            palette = oak if rib else wood_tile(archive,planks.replace(":",":block/"))
            for design in designs(macaw):
                ref = reference(design,wood)
                suffix = "_" + design + "_trapdoor"
                src, dst = ref + suffix, wood + suffix
                original, target = MACAW+":"+src, NS+":"+dst
                for jar, ns in ((macaw,MACAW),(archive,provider)):
                    assert f"assets/{ns}/items/{dst}.json" not in jar.NameToInfo
                state = read(macaw,resource(original,"blockstates","json"))
                item = read(macaw,resource(original,"items","json"))
                src_model = item["model"]["model"]
                dst_model = src_model.replace(MACAW+":",NS+":").replace("/"+ref+"_", "/"+wood+"_")
                for branch in state["variants"].values():
                    assert branch["model"] == src_model
                    branch["model"] = dst_model
                item["model"]["model"] = dst_model
                model = read(macaw,resource(src_model,"models","json"))
                for slot, tex in model["textures"].items():
                    if tex.startswith(MACAW+":"):
                        dest = tex.replace(MACAW+":",NS+":").replace("/"+ref+"_", "/"+wood+"_")
                        # The parent's upward face reverses V relative to native planks.
                        flip = Image.Transpose.FLIP_TOP_BOTTOM
                        tile = transfer({tex:mask(macaw,tex,ref,design)},wood,oak.transpose(flip),
                                        moss.transpose(flip),palette,palette)[tex]
                        path = pack / resource(dest,"textures","png")
                        path.parent.mkdir(parents=True,exist_ok=True)
                        tile.save(path)
                    elif tex == "minecraft:block/glass":
                        dest = tex
                    else:
                        stripped = "stripped_" if "/stripped_" in tex else ""
                        assert tex == f"minecraft:block/{stripped}{ref}_" + ("stem" if ref=="warped" else "log")
                        dest = (NS if rib else provider) + ":block/" + stripped + log
                        if not rib:
                            archive.getinfo(resource(dest,"textures","png"))
                        elif stripped:
                            tile = moss_sheet(wood_tile(vanilla,tex),oak,moss,())
                            path = pack / resource(dest,"textures","png")
                            path.parent.mkdir(parents=True,exist_ok=True)
                            tile.save(path)
                    model["textures"][slot] = dest
                write(pack,resource(target,"blockstates","json"),state)
                write(pack,resource(target,"items","json"),item)
                write(pack,resource(dst_model,"models","json"),model)
                for kind in ("item","block"):
                    names[f"{kind}.{NS}.{dst}"] = labels[f"item.{MACAW}.{src}"].replace(ref.title(),wood.replace("_"," ").title(),1)
                recipe = read(macaw,f"data/{MACAW}/recipe/{src}.json")
                timber = provider+":"+log if not rib else planks+("_slab" if design=="bark" else "")
                replacements = {f"minecraft:{ref}_planks":planks, f"minecraft:{ref}_log":timber,
                                f"minecraft:{ref}_stem":timber, f"{MACAW}:{ref}_glass_trapdoor":f"{NS}:{wood}_glass_trapdoor"}
                recipe["key"] = {k:replacements.get(v,v) for k,v in recipe["key"].items()}
                recipe["result"]["id"] = target
                write(pack,f"data/{NS}/recipe/{dst}.json",recipe)
                unlock = read(macaw,f"data/{MACAW}/advancement/recipes/oak/oak_barn_trapdoor.json")
                unlock["rewards"]["recipes"] = [target]
                unlock["criteria"]["has_the_recipe"]["conditions"]["recipe"] = target
                unlock["criteria"]["has_logs"]["conditions"]["items"][0]["items"] = [timber if design in ("bark","ranch") else planks]
                write(pack,f"data/{NS}/advancement/recipes/{dst}.json",unlock)
                loot = read(macaw,f"data/{MACAW}/loot_table/blocks/{src}.json")
                for pool in loot["pools"]:
                    for entry in pool["entries"]:
                        assert entry["name"] == original
                        entry["name"] = target
                write(pack,f"data/{NS}/loot_table/blocks/{dst}.json",loot)
                for path, values in tags.items():
                    if original in values:
                        members[path].append(target)
        write(pack,f"assets/{NS}/lang/en_us.json",names)
        for path, values in members.items():
            write(pack,path,{"replace":False,"values":values})
