"""Verify the six Bamboo graphs against the retained C31 and real Oak models."""
from contextlib import ExitStack
from copy import deepcopy
import hashlib
import json
from pathlib import Path
from shutil import copyfile
import subprocess
import sys
from tempfile import TemporaryDirectory
from zipfile import ZipFile

from PIL import Image, ImageDraw

from doors import NS, read, resource
from trapdoor_previews import Graph

FORMS = ("ranch_trapdoor","log_parapet","plank_parapet","blinds","curtain_rod","louvered_shutter")
PACK = "resourcepacks/bamboo_trapdoors/"
C31 = "b1d7cb016e806b32bee2c299df1af57239130c040c7bdd3b203991448d492fdb"


def files(jar):
    return {n for n in jar.namelist() if not n.endswith("/")}


def material(graph,model,face):
    ref = face["texture"]
    while ref.startswith("#"):
        ref = model["textures"][ref[1:]]
    graph.get(ref,"textures","png")
    return ref


def geometry(graph,src,dst,form):
    first,last = (graph.resolve(k) for k in (src,dst))
    assert first["parent"]==last["parent"] and first["display"]==last["display"]
    assert len(first["elements"])==len(last["elements"])
    for i,(old,new) in enumerate(zip(first["elements"],last["elements"])):
        normalized = deepcopy(new)
        for side,face in normalized["faces"].items():
            before = old["faces"][side]
            texture = material(graph,last,face)
            original = material(graph,first,before)
            face["texture"] = before["texture"]
            if form in ("log_parapet","ranch_trapdoor"):
                # Physical member axes in the unrotated template coordinate system.
                if form=="log_parapet":
                    axis = "z" if "support" in src else "x"
                else:
                    axis = "xxzzxxxxx"[i]
                assert original in ("minecraft:block/oak_log","minecraft:block/stripped_oak_log")
                name = "bamboo_block"
                normal = {"up":"y","down":"y","north":"z","south":"z","east":"x","west":"x"}[side]
                if normal==axis:
                    expected = "minecraft:block/"+name+"_top"
                else:
                    u_axis = "z" if normal=="x" else "x"
                    runs_u = axis==u_axis
                    if before.get("rotation",0)%180==90:
                        runs_u = not runs_u
                    expected = NS+":block/"+name+"_horizontal" if runs_u else "minecraft:block/"+name
            else:
                expected = NS+":block/bamboo_louvered_shutter" if form=="louvered_shutter" else "minecraft:block/bamboo_planks"
            assert expected==texture,(dst,i,side,texture,expected)
        assert normalized==old,(src,i)
    return sum(len(e["faces"]) for e in last["elements"])


def texture_checks(graph):
    vertical = graph.get("minecraft:block/bamboo_block","textures","png")
    horizontal = graph.get(NS+":block/bamboo_block_horizontal","textures","png")
    for y in range(16):
        for x in range(16):
            assert horizontal.getpixel((x,y))==vertical.getpixel((15-y,x))
    original = graph.get("mcwwindows:block/oak_louvered_shutter","textures","png")
    dest = graph.get(NS+":block/bamboo_louvered_shutter","textures","png")
    substrate = graph.get("minecraft:block/bamboo_planks","textures","png")
    assert original.size==dest.size==substrate.size==(16,16)
    luminance = original.convert("L")
    neutral, = {luminance.getpixel((x,y)) for y in (2,5,8,11,14) for x in range(3,9)}
    for y in range(16):
        for x in range(16):
            p = original.getpixel((x,y))
            if p[3]:
                factor = luminance.getpixel((x,y))/neutral
                expected = tuple(min(255,round(c*factor)) for c in substrate.getpixel((x,y))[:3])+p[3:]
            else:
                expected = p
            assert dest.getpixel((x,y))==expected,(x,y)
    for top in (1,4,7,10,13):
        assert all(luminance.getpixel((x,top))>neutral>luminance.getpixel((x,top+2)) for x in range(3,10))


def verify(jar,old,windows,traps,vanilla,output):
    before,after = files(old),files(jar)
    assert before<=after
    changed = {n for n in before if old.read(n)!=jar.read(n)}
    assert changed=={"fabric.mod.json","dev/resivore/amc/TrapdoorIntegration.class",f"assets/{NS}/lang/en_us.json",
                     "data/mcwwindows/tags/block/parapets.json","data/mcwwindows/tags/block/shutters.json"},changed
    meta = read(old,"fabric.mod.json")
    meta["version"]="0.1.0-canary32"
    meta["entrypoints"]["main"].append("dev.resivore.amc.BambooBlocks")
    assert meta==read(jar,"fabric.mod.json")
    allowed = {NS+":bamboo_"+f for f in FORMS}
    for path in changed:
        if "/tags/" in path:
            previous,current=read(old,path),read(jar,path)
            current["values"]=[v for v in current["values"] if v not in allowed]
            assert current==previous
        if "/lang/" in path:
            previous,current=read(old,path),read(jar,path)
            for k in list(current.keys()-previous.keys()):
                assert k.split(".")[-1] in {v.split(":")[1] for v in allowed}
                del current[k]
            assert current==previous
    graph = Graph([(jar,PACK),(jar,""),(windows,""),(traps,""),(vanilla,"")])
    texture_checks(graph)
    chart = Image.new("RGB",(1010,690),(34,39,44))
    draw = ImageDraw.Draw(chart)
    draw.text((12,8),"AMC C32   /   Bamboo and the Macaw Oak references",fill="white")
    for i,label in enumerate(("Oak reference","Bamboo")):
        draw.text((155+432*i,32),label,fill="white")
    models,branches,faces=0,0,0
    additions=set()
    for i,form in enumerate(FORMS):
        ranch = form=="ranch_trapdoor"
        ns,archive,prefix = ("mcwtrpdoors",traps,PACK) if ranch else ("mcwwindows",windows,"")
        src,dst = "oak_"+form,"bamboo_"+form
        original,target = ns+":"+src,NS+":"+dst
        for kind in ("items","blockstates"):
            assert resource(ns+":"+dst,kind,"json") not in files(archive)
            additions.add(prefix+resource(target,kind,"json"))
        state = graph.get(original,"blockstates","json")
        revised = graph.get(target,"blockstates","json")
        def renamed(ref):
            return ref.replace(ns+":",NS+":").replace("/oak_","/bamboo_")
        assert json.loads(json.dumps(state).replace(ns+":",NS+":").replace("/oak_","/bamboo_"))==revised
        old_item,new_item=(graph.get(k,"items","json")["model"] for k in (original,target))
        assert new_item==dict(old_item,model=renamed(old_item["model"]))
        variants = list(state["variants"].values()) if "variants" in state else [p["apply"] for p in state["multipart"]]
        branches+=len(variants)
        for ref in {b["model"] for b in variants+[old_item]}:
            if ref=="minecraft:block/air":
                graph.resolve(ref)
                continue
            faces+=geometry(graph,ref,renamed(ref),form)
            additions.add(prefix+resource(renamed(ref),"models","json"))
            models+=1
        for folder in ("recipe","loot_table/blocks"):
            path = f"data/{ns}/{folder}/{src}.json"
            expected = archive.read(path).decode().replace(original,target)
            for oak,bamboo in (("oak_log","bamboo_block"),("oak_slab","bamboo_slab"),("oak_planks","bamboo_planks")):
                expected = expected.replace("minecraft:"+oak,"minecraft:"+bamboo)
            path = prefix+f"data/{NS}/{folder}/{dst}.json"
            assert json.loads(expected)==read(jar,path)
            additions.add(path)
        adv = prefix+f"data/{NS}/advancement/recipes/building_blocks/{dst}.json"
        assert read(jar,adv)["rewards"]["recipes"]==[target]
        additions.add(adv)
        labels=read(jar,prefix+f"assets/{NS}/lang/en_us.json")
        for kind in ("block","item"):
            assert labels[f"{kind}.{NS}.{dst}"]==read(archive,f"assets/{ns}/lang/en_us.json")[f"item.{ns}.{src}"].replace("Oak","Bamboo")
        for path in files(archive):
            if ("/tags/block/" in path or "/tags/item/" in path) and path.endswith(".json") and original in read(archive,path)["values"]:
                assert read(jar,prefix+path)["values"].count(target)==1
                additions.add(prefix+path)
        preview_row(graph,chart,draw,i,form,state,revised,old_item,new_item)
    additions.update({"dev/resivore/amc/BambooBlocks.class",PACK+"pack.mcmeta",PACK+f"assets/{NS}/lang/en_us.json",
                      resource(NS+":block/bamboo_block_horizontal","textures","png"),
                      resource(NS+":block/bamboo_louvered_shutter","textures","png")})
    assert after-before==additions-before,(after-before)^(additions-before)
    output.parent.mkdir(exist_ok=True,parents=True)
    chart.save(output)
    print(f"C32: six pairs, {branches} state branches, {models} material models, {faces} faces, 512 verified RGBA pixels; "
          f"{len(before)-len(changed)} unchanged predecessor entries, {len(changed)} changed, {len(after-before)} added.")


def preview_row(graph,chart,draw,row,form,old,new,old_item,new_item):
    y = 58+row*104
    draw.text((12,y+34),form.replace("_"," ").title(),fill="white")
    for j,(state,item) in enumerate(((old,old_item),(new,new_item))):
        if "multipart" in state:
            parts = [p["apply"] for p in state["multipart"] if p["when"]["AND"][0]["facing"]=="east"]
            views = [("Shelf",[parts[0]]),("With support",parts)]
        else:
            variants = state["variants"]
            if form=="ranch_trapdoor":
                keys=[("Lower","facing=north,half=bottom,open=false"),("Open","facing=north,half=bottom,open=true"),("Upper","facing=north,half=top,open=false")]
            elif form=="blinds":
                keys=[(m.title(),"facing=south,part=top,blindsstate="+m) for m in ("open","closed","raised")]
            elif form=="curtain_rod":
                keys=[(m.title(),"facing="+m) for m in ("south","west")]
            else:
                keys=[("Open","facing=south,open=true,hinge=left"),("Closed","facing=south,open=false,hinge=left"),("Right hinge","facing=south,open=false,hinge=right")]
            views=[(label,[variants[k]]) for label,k in keys]
        views.append(("Inventory",[item]))
        for i,(label,parts) in enumerate(views):
            x=150+432*j+98*i
            for p in parts:
                img=graph.view(p,gui=label=="Inventory")
                chart.paste(img,(x,y),img)
            draw.text((x+5,y+77),label,fill=(210,218,226))


if __name__=="__main__":
    if not __debug__:
        raise SystemExit("Assertions required")
    current,*history,previous=map(Path,sys.argv[1:])
    assert hashlib.sha256(previous.read_bytes()).hexdigest()==C31
    with TemporaryDirectory(dir=current.parent.parent) as temp:
        baseline=Path(temp)/"libs"/previous.name
        baseline.parent.mkdir()
        copyfile(previous,baseline)
        subprocess.run([sys.executable,"-B",Path(__file__).with_name("verify_trapdoors.py"),baseline,*history],check=True)
    with ExitStack() as stack:
        jars=[stack.enter_context(ZipFile(p)) for p in (current,previous,history[0],history[-1],history[1])]
        verify(*jars,current.parent.parent/"previews/bamboo.png")
