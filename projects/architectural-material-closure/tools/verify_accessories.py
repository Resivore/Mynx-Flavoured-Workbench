"""C33 packaged-graph, artwork and predecessor checks; write private comparison sheets."""
from contextlib import ExitStack
from pathlib import Path
from tempfile import TemporaryDirectory
from shutil import copyfile
from zipfile import ZipFile
import hashlib
import json
import subprocess
import sys
from PIL import Image, ImageDraw
from doors import MATERIALS, NS, STEMS, read, resource
from trapdoor_previews import Graph
from verify_bamboo import files
from wood_accessories import FORMS


def placed(state):
    if "multipart" in state:
        return [p["apply"] for p in state["multipart"]]
    return list(state["variants"].values())


def check(jar,old,windows,paths,vanilla,end,rib,folder):
    prev,curr=files(old),files(jar)
    assert prev<=curr
    delta={n for n in prev if old.read(n)!=jar.read(n)}
    assert len(curr-prev)==260 and len(delta)==8,delta
    ids={NS+":"+w+"_"+("stem_parapet" if w in STEMS and f=="log_parapet" else f)
         for w in MATERIALS for f in FORMS+(("button","pressure_plate") if w=="mossy_oak" else ())}
    for n in delta:
        if n in ("dev/resivore/amc/EnderscapeFamilies.class","dev/resivore/amc/MossyOakBlocks.class","fabric.mod.json"):continue
        before,after=read(old,n),read(jar,n)
        if "/tags/" in n:
            assert before==dict(after,values=[v for v in after["values"] if v not in ids])
        else:
            assert n.endswith("/lang/en_us.json") and before=={k:v for k,v in after.items() if NS+":"+k.split(".")[-1] not in ids}
    for kind in ("blockstates","items","recipe","loot_table/blocks"):
        assert {NS+":"+Path(n).stem for n in curr-prev if "/"+kind+"/" in n}==ids
    folder.mkdir(exist_ok=True,parents=True)
    total=0
    for wood,ns in MATERIALS.items():
        provider=rib if ns=="ribbits" else end
        prefix="resourcepacks/"+ns+"/"
        graph=Graph([(jar,prefix),(jar,""),(provider,""),(windows,""),(paths,""),(vanilla,"")])
        def texture(ref):return graph.get(ref,"textures","png")
        forms=FORMS+(("button","pressure_plate") if ns=="ribbits" else ())
        canvas=Image.new("RGB",(720,100+88*len(forms)),(34,39,44))
        draw=ImageDraw.Draw(canvas)
        draw.text((10,5),wood.replace("_"," ").title()+"   |   Native door halves, trapdoor, Macaw Oak shutter and AMC shutter",fill="white")
        for i,ref in enumerate([ns+":block/"+wood+"_"+t for t in ("door_top","door_bottom","trapdoor")]+["mcwwindows:block/oak_shutter",NS+":block/"+wood+"_shutter"]):
            if ns=="ribbits" and i==2:continue
            im=texture(ref).resize((60,60),Image.Resampling.NEAREST)
            canvas.paste(im,(12+i*75,24),im)
        for i,form in enumerate(forms):
            stem=form=="log_parapet" and wood in STEMS
            suffix="stem_parapet" if stem else form
            control=form in ("button","pressure_plate")
            authority,source_ns=(vanilla,"minecraft") if control else (paths,"mcwpaths") if form=="planks_path" else (windows,"mcwwindows")
            sourcewood="warped" if stem else "oak"
            source=source_ns+":"+sourcewood+"_"+suffix
            target=NS+":"+wood+"_"+suffix
            def rename(ref):return ref.replace(source_ns+":",NS+":").replace("/"+sourcewood+"_","/"+wood+"_")
            state=graph.get(source,"blockstates","json")
            new=graph.get(target,"blockstates","json")
            assert json.loads(rename(json.dumps(state)))==new
            item=graph.get(source,"items","json")["model"]
            own=graph.get(target,"items","json")["model"]
            assert own==dict(item,model=rename(item["model"]))
            for ref in {b["model"] for b in placed(state)+[item]}:
                if ref=="minecraft:block/air":continue
                m,n=(graph.get(k,"models","json") for k in (ref,rename(ref)))
                assert n==dict(m,textures=n["textures"]) and m["textures"].keys()==n["textures"].keys()
                assert graph.resolve(ref)["elements"]==graph.resolve(rename(ref))["elements"]
                for slot,orig in m["textures"].items():
                    val=n["textures"][slot]
                    log=wood+("_stem" if wood in STEMS else "_log")
                    if orig=="minecraft:block/oak_planks":expected=ns+":block/"+wood+"_planks"
                    elif orig in ("minecraft:block/oak_log","minecraft:block/warped_stem"):expected=(NS if ns=="ribbits" else ns)+":block/"+log
                    elif orig=="minecraft:block/stripped_oak_log":expected=(NS if ns=="ribbits" else ns)+":block/stripped_"+log
                    else:expected=NS+":block/"+wood+"_"+form
                    assert val==expected,(ref,slot,val)
                    texture(val)
            if form in ("planks_path","louvered_shutter"):
                art=texture(NS+":block/"+wood+"_"+form)
                original=texture(source_ns+":block/oak_"+form)
                assert art.getchannel("A").tobytes()==original.getchannel("A").tobytes()
                target_tile=texture(ns+":block/"+wood+"_planks")
                def luminance(p):return sum(a*b for a,b in zip(p,(299,587,114)))
                def ranking(im):return sorted({p[:3] for p in im.getdata() if p[3]},key=lambda c:(luminance(c),c))
                ramp=ranking(original);palette=ranking(target_tile)
                for y in range(16):
                    for x in range(16):
                        pixel=original.getpixel((x,y));rgb=pixel[:3]
                        if pixel[3] and ns=="enderscape":
                            rgb=palette[int(ramp.index(rgb)*(len(palette)-1)/(len(ramp)-1)+0.5)]
                        elif pixel[3]:
                            old_oak=texture("minecraft:block/oak_planks").getpixel((x,y))
                            moss=target_tile.getpixel((x,y))
                            if old_oak!=moss:
                                gain=luminance(rgb)/luminance(old_oak)
                                rgb=tuple(min(255,round(c*gain)) for c in moss[:3])
                        assert art.getpixel((x,y))==(*rgb,pixel[3])
            if form=="shutter":
                art=texture(NS+":block/"+wood+"_shutter")
                assert art.crop((11,0,14,16)).tobytes()==texture("mcwwindows:block/oak_shutter").crop((11,0,14,16)).tobytes()
                tile=texture(ns+":block/"+wood+("_trapdoor" if ns=="enderscape" else "_door_top"))
                if ns=="enderscape":
                    assert art.crop((14,0,16,16)).tobytes()==tile.crop((14,0,16,16)).tobytes()
                    assert set(art.crop((0,1,11,16)).getdata())<=set(tile.getdata())
                    openings=sum(p[3]==0 for p in art.crop((0,1,11,16)).getdata())
                    assert (openings>0)==(wood!="celestial")
                else:
                    assert art.crop((1,3,10,12)).tobytes()==tile.crop((4,7,13,16)).tobytes()
                    assert art.crop((8,12,11,15)).tobytes()==texture(ns+":block/mossy_oak_door_bottom").crop((12,5,15,8)).tobytes()
            recipe_prefix="resourcepacks/ribbits_trapdoors/" if wood=="mossy_oak" and form=="shutter" else prefix
            name=target.split(":")[1]
            assert read(jar,recipe_prefix+f"data/{NS}/recipe/{name}.json")["result"]["id"]==target
            y=96+88*i;draw.text((8,y+25),suffix.replace("_"," ").title(),fill="white")
            for col,b in enumerate([item,own]+[b for b in placed(new) if b["model"]!="minecraft:block/air"][:3]):
                img=graph.view(b,gui=col<2);x=150+100*col
                canvas.paste(img,(x,y),img)
                draw.text((x,y+73),["Ref item","AMC item","Placed 1","Placed 2","Placed 3"][col],fill="white")
            total+=len(placed(new))
        canvas.save(folder/(wood+".png"))
    print(f"C33: 30 pairs, {total} state branches; {len(prev)-len(delta)} unchanged C32 files, {len(delta)} changed and {len(curr-prev)} added.")


if __name__=="__main__":
    if not __debug__:sys.exit("Assertions required")
    current,*history,previous=map(Path,sys.argv[1:])
    assert hashlib.sha256(previous.read_bytes()).hexdigest()=="632c4423c016ebeb7057559efa0494f680c93efdf1cd79e7ee25cd4708111f7c"
    with TemporaryDirectory(dir=current.parent.parent) as temp:
        baseline=Path(temp)/"libs"/previous.name
        baseline.parent.mkdir()
        copyfile(previous,baseline)
        subprocess.run([sys.executable,"-B",Path(__file__).with_name("verify_bamboo.py"),baseline,*history],check=True)
    with ExitStack() as stack:
        jars=[stack.enter_context(ZipFile(p)) for p in (current,previous,history[0],history[3],history[1],history[6],history[12])]
        check(*jars,current.parent.parent/"previews")
