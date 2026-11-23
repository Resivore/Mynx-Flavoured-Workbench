"""Check the coordinated AMC delta against C35, then replay its historical archive checks."""
import hashlib
import json
from pathlib import Path
import subprocess
import sys
from zipfile import ZipFile
from contextlib import ExitStack

from doors import resource

NS="architectural_material_closure"
WOODS=("silver_birch","wisteria")


def verify(current,trees,previous,history):
    paths=[current,trees,previous,*history]
    with ExitStack() as stack:
        a,tree,base,_,windows,mc,bbb,*others=[stack.enter_context(ZipFile(p)) for p in paths]
        assert hashlib.sha256(previous.read_bytes()).hexdigest()=="b1d5fd2bb5f1980ac470b8e273216396989330e801f87c7621b80bf16ca790d8"
        changed_classes={"dev/resivore/amc/"+name+".class" for name in
                         ("MacawDoorBlocks","MossyOakBlocks","MossyOakClient","TrapdoorIntegration")}
        for n in base.namelist():
            if n.endswith("/") or n in changed_classes or n in ("fabric.mod.json","amc.ribbits.mixins.json","META-INF/MANIFEST.MF"):continue
            if n.startswith("resourcepacks/ribbits/") and ("/tags/" in n or "/lang/" in n):
                old,new=[json.loads(z.read(n)) for z in (base,a)]
                if "/tags/" in n:
                    assert all(v in new["values"] for v in old["values"]),n
                else:
                    assert all(new[k]==v for k,v in old.items()),n
                continue
            if n not in a.NameToInfo and n.startswith("resourcepacks/ribbits") and "/recipe/" in n:
                legacy="resourcepacks/mossy_oak_legacy_recipes/"+n.split("/",2)[2]
                old=json.loads(a.read(legacy))
                old.pop("fabric:load_conditions",None)
                assert old==json.loads(base.read(n)),n
                continue
            assert a.read(n)==base.read(n),n
        overlays=["resourcepacks/mynx_trees"+suffix+"/" for suffix in ("","_doors","_trapdoors")]
        resources={}
        for z in [mc,bbb,windows,*others,tree,a]:
            for n in z.namelist():
                if n.startswith("assets/"):resources[n]=(z,n)
                for pack in overlays:
                    if n.startswith(pack+"assets/"):resources[n[len(pack):]]=(z,n)
        def checkref(ref,kind,ext):
            if ref.startswith("#"):return
            if ":" not in ref:ref="minecraft:"+ref
            path=resource(ref,kind,ext)
            assert path in resources,path
        def walk(node):
            if isinstance(node,dict):
                for k,v in node.items():
                    if k in ("model","parent") and isinstance(v,str):checkref(v,"models","json")
                    elif k=="textures" and isinstance(v,dict):
                        for tex in v.values():checkref(tex,"textures","png")
                    else:walk(v)
            elif isinstance(node,list):
                for v in node:walk(v)
        checked=0
        for pack in overlays:
            for n in a.namelist():
                if n.startswith(pack+"assets/") and n.endswith(".json") and "/lang/" not in n:
                    walk(json.loads(a.read(n)));checked+=1
        for wood in WOODS:
            for suffix,expected in [("",26),("_doors",22),("_trapdoors",17)]:
                prefix="resourcepacks/mynx_trees"+suffix+"/assets/"+NS+"/items/"
                ids={Path(n).stem for n in a.namelist() if n.startswith(prefix) and n.endswith(".json") and wood in n}
                assert len(ids)==expected,(wood,suffix,ids)
                for name in ids:
                    a.getinfo(prefix.replace("/items/","/blockstates/")+name+".json")
                    assert not ("assets/mynx_trees/items/"+name+".json") in tree.NameToInfo
        shutter="resourcepacks/mynx_trees/assets/"+NS+"/textures/block/wisteria_shutter.png"
        assert hashlib.sha256(a.read(shutter)).hexdigest()=="17035d40bae464354757a7db3a34827f155b7acbca8b308a343bdb9530434161"
        print("Verified 130 architectural block/item pairs and",checked,"published resource graphs.")
    subprocess.run([sys.executable,"-B",Path(__file__).with_name("verify_wood_matrix.py"),previous,*history],check=True)


if __name__=="__main__":
    current,trees,previous,*history=map(Path,sys.argv[1:])
    verify(current,trees,previous,history)
