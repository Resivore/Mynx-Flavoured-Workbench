"""Compare C31 with C30 and independently inspect all conditional trapdoor graphs."""
from contextlib import ExitStack
from pathlib import Path
from shutil import copyfile
from tempfile import TemporaryDirectory
from zipfile import ZipFile
import hashlib
import json
import subprocess
import sys

from doors import MATERIALS, NS, STEMS, read
from trapdoor_previews import Graph, sheet

MACAW = "mcwtrpdoors"
DESIGNS = "bamboo bark barn barred barrel beach blossom classic cottage four_panel glass mystic paper ranch swamp tropical whispering".split()
# Fixed opaque materials, inspected on the source sheets and their peer woods.
FIXED = {"barrel":"28221c 302c29 453f3b", "beach":"333333 494949 686868",
         "cottage":"353535 3f3f3f 686868", "glass":"a8d0d9 d0eae9",
         "paper":"442e17 5b3e1f 6b4823 eee6d5 eeeade f6f2de fff6e6",
         "tropical":"353535 515151 727272", "whispering":"4a4a4a 6f6f6f"}
COUNTS = dict(zip([d for d in DESIGNS if d not in ("bark","ranch")],
                  [212,256,216,192,208,220,220,232,256,156,201,158,224,184,250],strict=True))


def files(jar):
    return {n for n in jar.namelist() if not n.endswith("/")}


def light(color):
    return sum(a*b for a,b in zip(color,(299,587,114)))


def pixels(graph, original, actual, wood, design, target):
    def get(ref):
        tile = graph.get(ref,"textures","png")
        assert tile.size == (16,16)
        return list(tile.getdata())
    source, result = get(original),get(actual)
    fixed = set(FIXED.get(design,"").split())
    mask = [p[3]==255 and bytes(p[:3]).hex() not in fixed for p in source]
    assert sum(mask)==COUNTS.get(design,256)
    def palette(values):
        return sorted({p[:3] for p in values},key=lambda c:(light(c),c))
    colors = palette(p for p,ok in zip(source,mask) if ok)
    targets = palette(get(target))
    oak,moss = get("minecraft:block/oak_planks"),get("ribbits:block/mossy_oak_planks")
    assert sum(a!=b for a,b in zip(oak,moss))==63
    for i,(p,ok,out) in enumerate(zip(source,mask,result)):
        expected = p[:3]
        if ok:
            rank = colors.index(expected)
            mapped = targets[int(rank*(len(targets)-1)/(len(colors)-1)+0.5)]
            if wood == "mossy_oak":
                if design=="classic":
                    expected = mapped
                uv = i if design=="stripped" else (15-i//16)*16+i%16
                if oak[uv]!=moss[uv]:
                    gain = light(expected)/light(oak[uv])
                    expected = tuple(min(255,round(v*gain)) for v in moss[uv][:3])
            else:
                expected = mapped
        assert out==(*expected,p[3]), (actual,i,out,expected)
    return len(source)


def audit(jar, macaw, graph, wood, provider, folder):
    prefix = f"resourcepacks/{provider}_trapdoors/"
    data = prefix+"data/"+NS+"/"
    flat = []
    count = 0
    lang = read(jar,prefix+f"assets/{NS}/lang/en_us.json")
    for design in DESIGNS:
        stem = wood in STEMS
        ref = "spruce" if design=="classic" else "warped" if stem and design in ("bark","ranch") else "oak"
        src,dst = (w+"_"+design+"_trapdoor" for w in (ref,wood))
        original,own = MACAW+":"+src,NS+":"+dst
        state = graph.get(original,"blockstates","json")["variants"]
        actual = graph.get(own,"blockstates","json")["variants"]
        item = graph.get(own,"items","json")["model"]
        old_item = graph.get(original,"items","json")["model"]
        model = old_item["model"]
        dest = NS+":"+model.split(":")[1].replace("/"+ref+"_","/"+wood+"_")
        assert item == dict(old_item,model=dest)
        assert actual == {k:dict(v,model=dest) for k,v in state.items()} and len(actual)==16
        assert all(v["model"]==model for v in state.values())
        source = graph.get(model,"models","json")
        revised = graph.get(dest,"models","json")
        assert revised == dict(source,textures=revised["textures"]) and source["textures"].keys()==revised["textures"].keys()
        graph.resolve(dest)
        for slot,tex in source["textures"].items():
            value = revised["textures"][slot]
            target = "minecraft:block/oak_planks" if wood=="mossy_oak" else provider+":block/"+wood+"_planks"
            if tex.startswith(MACAW+":"):
                assert value == NS+":"+tex.split(":")[1].replace("/"+ref+"_","/"+wood+"_")
                count += pixels(graph,tex,value,wood,design,target)
            elif tex=="minecraft:block/glass":
                assert value==tex
            else:
                stripped = "stripped_" if "/stripped_" in tex else ""
                assert value == (NS if wood=="mossy_oak" else provider)+":block/"+stripped+wood+("_stem" if stem else "_log")
                if wood=="mossy_oak" and stripped:
                    count += pixels(graph,tex,value,wood,"stripped",target)
            graph.get(value,"textures","png")
        loot = macaw.read(f"data/{MACAW}/loot_table/blocks/{src}.json").decode().replace(original,own)
        assert read(jar,data+"loot_table/blocks/"+dst+".json")==json.loads(loot)
        recipe = read(jar,data+"recipe/"+dst+".json")
        old = read(macaw,f"data/{MACAW}/recipe/{src}.json")
        assert recipe==dict(old,key=recipe["key"],result=dict(old["result"],id=own))
        assert recipe["key"].keys()==old["key"].keys()
        unlock = read(jar,data+"advancement/recipes/"+dst+".json")
        assert unlock["rewards"]["recipes"]==[own] and unlock["criteria"]["has_the_recipe"]["conditions"]["recipe"]==own
        for value in list(recipe["key"].values())+unlock["criteria"]["has_logs"]["conditions"]["items"][0]["items"]:
            graph.get(value,"items","json")
        for k in ("block","item"):
            assert lang[k+"."+NS+"."+dst] == read(macaw,"assets/mcwtrpdoors/lang/en_us.json")["item."+MACAW+"."+src].replace(ref.title(),wood.replace("_"," ").title(),1)
        flat.append((design,state,actual,item))
    sheet(graph,flat,wood,folder)
    return count


if __name__=="__main__":
    if not __debug__:
        sys.exit("Assertions required")
    current,*history,previous,provider = map(Path,sys.argv[1:])
    assert hashlib.sha256(previous.read_bytes()).hexdigest()=="f0b4a7f464026a1c021d7004d81a18b9896cfd428018887e2bf69f1fdbe43462"
    with TemporaryDirectory(dir=current.parent.parent) as temp:
        baseline = Path(temp)/"libs"/previous.name
        baseline.parent.mkdir()
        copyfile(previous,baseline)
        subprocess.run([sys.executable,"-B",Path(__file__).with_name("verify_doors.py"),baseline,*history],check=True)
    with ExitStack() as stack:
        jar,old,macaw,vanilla,end,rib = [stack.enter_context(ZipFile(p)) for p in
                                         (current,previous,provider,history[1],history[6],history[12])]
        existing,new = files(old),files(jar)
        assert existing <= new
        changed = {n for n in existing if old.read(n)!=jar.read(n)}
        assert changed == {"fabric.mod.json",*(f"dev/resivore/amc/mixin/{p}InitializationMixin.class" for p in ("Ribbits","Enderscape"))},changed
        added = new-existing
        outside = {n for n in added if not n.startswith(("resourcepacks/enderscape_trapdoors/","resourcepacks/ribbits_trapdoors/"))}
        assert outside=={"amc.trapdoors.mixins.json","dev/resivore/amc/TrapdoorIntegration.class",
                         "dev/resivore/amc/mixin/MacawTrapdoorsInitializationMixin.class","dev/resivore/amc/mixin/TrapdoorAccess.class"}
        ids = {f"{w}_{d}_trapdoor" for w in MATERIALS for d in DESIGNS}
        assert len(added)==483
        assert len([n for n in added if n.endswith(".png")])==61
        assert len([n for n in added if "/models/" in n])==68
        metadata = read(old,"fabric.mod.json")
        metadata["version"] = "0.1.0-canary31"
        metadata["mixins"].append("amc.trapdoors.mixins.json")
        metadata["suggests"][MACAW] = "*"
        assert metadata == read(jar,"fabric.mod.json")
        for kind in ("blockstates","items","recipe","loot_table/blocks","advancement/recipes"):
            assert {Path(n).stem for n in added if "/"+kind+"/" in n and n.endswith(".json")}==ids
        px = 0
        for wood,ns in MATERIALS.items():
            graph = Graph([(jar,f"resourcepacks/{ns}_trapdoors/"),(jar,f"resourcepacks/{ns}/"),
                           (jar,""),(macaw,""),(vanilla,""),(end,""),(rib,"")])
            px += audit(jar,macaw,graph,wood,ns,current.parent.parent/"previews")
        print(f"C31: 68 trapdoor pairs; {px} generated RGBA texels verified; {len(added)} new files; "
              f"{len(existing)-len(changed)} byte-identical C30 payloads, three expected metadata/initializer changes.")
