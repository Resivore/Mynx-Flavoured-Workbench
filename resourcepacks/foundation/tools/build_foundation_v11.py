"""Restore Matcha Birch and extend its existing plank overlay to the two new woods."""
from collections import Counter
from copy import copy
from contextlib import ExitStack
import hashlib
import json
from io import BytesIO
from pathlib import Path
import subprocess
from zipfile import ZipFile, ZIP_DEFLATED
from PIL import Image

COMMON = Path(subprocess.check_output(["git", "rev-parse", "--path-format=absolute", "--git-common-dir"],text=True).strip()).parent
ROOT = Path(__file__).resolve().parents[1]
MATCHA = "originals/resourcepacks/Matcha_Flavoured_1_12.zip"
BASE = "resourcepacks/foundation/artifacts/Foundation v10.zip"
PLANKS = "assets/minecraft/optifine/ctm/overlays/planks/"
BIRCH = PLANKS + "birch_planks/"
LOG = "assets/minecraft/optifine/ctm/overlays/logs/birch_log/"
MATERIALS = ("silver_birch","wisteria")
PROVIDERS = {"mcwdoors":"doors", "mcwtrpdoors":"trapdoors", "mcwwindows":"windows", "mcwpaths":"paths"}


def load(data):
    return Image.open(BytesIO(data)).convert("RGBA")


def patch(mask, texture):
    shape, tile = load(mask), load(texture)
    if shape.size != tile.size:
        raise ValueError("Mismatched CTM UVs")
    # Preserve authored alpha, including the fully transparent empty tile.
    tile.putalpha(shape.getchannel("A"))
    out = BytesIO()
    tile.save(out,format="PNG")
    return out.getvalue()


def recolor_provider(override, native, native_planks, matcha_planks):
    ref, new = load(native_planks), load(matcha_planks)
    a, old = load(native), load(override)
    if a.size != old.size:
        raise ValueError("Provider reference dimensions changed")
    # Learn the exact per-texel Birch palette from the two plank sheets. Extend it
    # to the provider's shaded wood, leaving neutral hinges, panes and handles alone.
    light = lambda c: .2126*c[0]+.7152*c[1]+.0722*c[2]
    palette = {}
    for before, after in zip(ref.getdata(),new.getdata()):
        palette.setdefault(before[:3],Counter())[after[:3]]+=1
    ramp = {rgb:counts.most_common(1)[0][0] for rgb,counts in palette.items()}
    def shade(p):
        near=min(ramp,key=lambda c:sum((v-w)**2 for v,w in zip(c,p[:3])))
        if p[:3] not in ramp and not (p[0]>p[1] and p[1]>p[2]+10):
            return p
        gain=light(p)/max(1,light(near))
        return (*[max(0,min(255,round(v*gain))) for v in ramp[near]],p[3])
    a.putdata([shade(p) if p[3] else p for p in a.getdata()])
    out=BytesIO(); a.save(out,format="PNG")
    return out.getvalue()


def build(output):
    client=Path.home()/".gradle/caches/fabric-loom/26.2/minecraft-client.jar"
    with ExitStack() as files:
        archive=lambda p:files.enter_context(ZipFile(p))
        for source,digest in ((COMMON/BASE,"ebf0a92e954392544299d762900030f7c4c7fc975574795b19c4aacf987be769"),
                              (COMMON/MATCHA,"6209783021c358044abedabacee471faff5bd4080437d4e3b5e51963f1804248")):
            with source.open("rb") as stream:
                if hashlib.file_digest(stream,"sha256").hexdigest()!=digest: raise ValueError("Unexpected input: "+str(source))
        base, matcha, mc = archive(COMMON/BASE), archive(COMMON/MATCHA), archive(client)
        mods={ns:archive(next((COMMON/"originals/mods").glob(f"mcw-{name}-*mc26.2fabric.jar"))) for ns,name in PROVIDERS.items()}
        plank="assets/minecraft/textures/block/birch_planks.png"
        textures={"birch":matcha.read(plank), "silver_birch":base.read(plank),
                  "wisteria":(COMMON/"originals/assets/wisteria assets/wisteria planks.png").read_bytes()}
        changes={}
        metadata=json.loads(base.read("pack.mcmeta"))
        assert metadata["pack"]["description"]=="Foundation v10"
        metadata["pack"]["description"]="Foundation v11"
        changes["pack.mcmeta"]=(json.dumps(metadata,indent=2)+"\n").encode()
        bbb=archive(COMMON/"projects/building-but-better/artifacts/bbb-fabric-26.2-2.0pre4+26.2-enderscape-dev.7.jar")
        dd=archive(COMMON/"projects/dramatic-doors/artifacts/DD C14.jar")
        for provider in (bbb,dd):
            for n in provider.namelist():
                if not n.startswith("assets/") or "/textures/" not in n or "birch" not in n or not n.endswith(".png"):
                    continue
                if provider is dd and not ("/macaw/" in n or "/short_birch_door" in n or "/tall_birch_door" in n):
                    continue
                if n in matcha.NameToInfo:
                    changes[n]=matcha.read(n)
                else:
                    changes[n]=recolor_provider(provider.read(n),provider.read(n),mc.read(plank),textures["birch"])
        for n in base.namelist():
            if n.endswith("/"):
                continue
            if n.startswith(PLANKS) and n.endswith(".properties"):
                data=base.read(n).decode()
                lines=data.splitlines(keepends=True)
                for i,line in enumerate(lines):
                    if line.startswith("matchBlocks=") and "birch_planks" in line.split("=")[1].split():
                        lines[i]=line.replace("birch_planks","birch_planks "+" ".join("mynx_trees:"+w+"_planks" for w in MATERIALS),1)
                changes[n]="".join(lines).encode()
            if n.startswith(BIRCH):
                if n.endswith(".png"):
                    changes[n]=patch(base.read(n),textures["birch"])
                for wood in MATERIALS:
                    dst=n.replace("/birch_planks/","/"+wood+"_planks/").replace("/birch_planks.properties","/"+wood+"_planks.properties")
                    if n.endswith(".properties"):
                        data=changes[n].decode().splitlines(keepends=True)
                        changes[dst]="".join(line.replace("=birch_planks","=mynx_trees:"+wood+"_planks") if line.startswith(("connectBlocks=","tintBlock=")) else line for line in data).encode()
                    else:
                        changes[dst]=patch(base.read(n),textures[wood])
            elif n.startswith(LOG) and n.endswith(".png"):
                changes[n]=patch(base.read(n),matcha.read("assets/minecraft/textures/block/birch_log.png"))
            elif n.startswith("assets/") and "birch" in n and n.endswith(".png") and "/textures/" in n:
                ns=n.split("/")[1]
                if ns=="minecraft" and not any(item in n for item in ("leaves","sapling")):
                    if n in matcha.NameToInfo:
                        changes[n]=matcha.read(n)
                    elif n in mc.NameToInfo:
                        changes[n]=recolor_provider(base.read(n),mc.read(n),mc.read(plank),textures["birch"])
                elif ns in mods and n in mods[ns].NameToInfo:
                    changes[n]=recolor_provider(base.read(n),mods[ns].read(n),mc.read(plank),textures["birch"])
        output.parent.mkdir(parents=True,exist_ok=True)
        with ZipFile(output,"w",ZIP_DEFLATED) as out:
            for entry in base.infolist():
                out.writestr(copy(entry),changes.pop(entry.filename,base.read(entry)))
            for path,data in sorted(changes.items()):
                entry=copy(base.getinfo(BIRCH+"0.png"))
                entry.filename=path
                out.writestr(entry,data)
        with ZipFile(output) as out:
            assert out.testzip() is None
            assert out.read(plank)==matcha.read(plank)
            for w in ("birch",*MATERIALS):
                for i in range(17):
                    old=load(base.read(BIRCH+str(i)+".png"))
                    tile=load(out.read(PLANKS+f"{w}_planks/{i}.png"))
                    assert tile.getchannel("A").tobytes()==old.getchannel("A").tobytes()
                    assert all(p[:3]==q[:3] for p,q in zip(tile.getdata(),load(textures[w]).getdata()) if p[3])
            for n in base.namelist():
                if n!="pack.mcmeta" and "birch" not in n and not (n.startswith(PLANKS) and n.endswith(".properties")):
                    assert base.read(n)==out.read(n),n
        print(output,hashlib.sha256(output.read_bytes()).hexdigest())


if __name__=="__main__":
    build(ROOT/"artifacts/Foundation v11.zip")
