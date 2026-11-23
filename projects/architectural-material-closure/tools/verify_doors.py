"""Compare C30 to the retained C29 and the complete audited Macaw wood-door graph."""
import hashlib
import json
import re
import subprocess
import sys
from collections import defaultdict
from contextlib import ExitStack
from pathlib import Path
from zipfile import ZipFile

from PIL import Image, ImageDraw
from doors import MATERIALS, STEMS, material_mask, read, reference, rename, resource
# This regression suite opens the historical pre-Trees artifacts.
MATERIALS = {k:v for k,v in MATERIALS.items() if v != "mynx_trees"}

from masonry import NS, image

STYLES = ("bamboo", "barn", "barn_glass", "beach", "classic", "cottage", "four_panel",
          "glass", "japanese", "japanese2", "modern", "mystic", "nether", "paper", "stable",
          "stable_head", "swamp", "tropical", "waffle", "western", "whispering")
MASK_DIGEST = "522e7c41846302a026369e46c1f8a625f93a4e7d1b7903ed69758be575bd1722"


def names(archive):
    return {name for name in archive.namelist() if not name.endswith("/")}


def lum(pixel):
    return sum(c*w for c,w in zip(pixel,(299,587,114)))


class Resources:
    def __init__(self, jar, macaw, vanilla, provider, ns):
        self.jar = jar
        self.macaw = macaw
        self.vanilla = vanilla
        self.ns = ns
        self.provider = provider
        self.pack = "resourcepacks/" + ns + "_doors/"

    def locate(self, ref, kind, extension):
        path = resource(ref,kind,extension)
        for archive,prefix in ((self.jar,self.pack),(self.jar,"resourcepacks/"+self.ns+"/"),
                               (self.macaw,""),(self.provider,""),(self.vanilla,"")):
            if prefix+path in archive.NameToInfo:
                return archive,prefix+path
        raise AssertionError(ref)

    def sprite(self, ref):
        return image(*self.locate(ref,"textures","png"))

    def resolved(self, ref):
        textures, elements = {}, None
        while ":" in ref:
            model = read(*self.locate(ref,"models","json"))
            for k,v in model.get("textures",{}).items():
                textures.setdefault(k,v)
            if elements is None:
                elements = model.get("elements")
            ref = model.get("parent","")
        assert elements
        for el in elements:
            for face in el["faces"].values():
                self.locate(textures[face["texture"][1:]],"textures","png")
        return elements,textures

    def front(self, state):
        # All audited closed models face along X before their state rotation.
        # Project the east face of each authored element at its own UVs.
        door = Image.new("RGBA",(32,64))
        for half, y0 in (("upper",0),("lower",32)):
            node = next(v for k,v in state["variants"].items() if "half="+half in k and
                        "open=false" in k and "hinge=right" not in k)
            elements,slots = self.resolved(node["model"])
            for el in sorted(elements,key=lambda elem:elem["to"][0]):
                face = el["faces"].get("east")
                if not face:
                    continue
                assert el.get("rotation",{}).get("angle",0) == face.get("rotation",0) == 0
                a,b = el["from"],el["to"]
                left,top,right,bottom = [round(c*2) for c in (16-b[2],16-b[1],16-a[2],16-a[1])]
                if right <= left or bottom <= top:
                    continue
                tex = self.sprite(slots[face["texture"][1:]])
                u,v,s,t = face["uv"]
                for y in range(top,bottom):
                    for x in range(left,right):
                        px = int(u+(s-u)*(x-left+.5)/(right-left))%16
                        py = int(v+(t-v)*(y-top+.5)/(bottom-top))%16
                        color = tex.getpixel((px,py))
                        if color[3]:
                            door.putpixel((x,y+y0),color)
        assert door.getbbox()
        return door


def check_pixels(res, material, bindings, fingerprints, *, darken_only=False):
    palette_groups = defaultdict(set)
    material_pixels = {}
    for src,(dest,wood) in bindings.items():
        tile,mask = material_mask(res.macaw,src,wood)
        material_pixels[src] = tile,mask
        bitmap = bytes((x,y) in mask for y in range(16) for x in range(16))
        fingerprints[src] = hashlib.sha256(bitmap).hexdigest()
        palette_groups[re.sub("_(upper|lower)$","",src)].update(tile.getpixel(p)[:3] for p in mask)
    oak = res.sprite("minecraft:block/oak_planks")
    mossy = material == "mossy_oak"
    substrate = res.sprite(res.ns+":block/"+material+"_planks")
    if mossy:
        changed = [(p,q) for p,q in zip(oak.getdata(),substrate.getdata()) if p!=q]
        assert len(changed)==63 and len({q for p,q in changed})==4
    count = 0
    for src,(dest,wood) in bindings.items():
        tile,mask = material_pixels[src]
        result = res.sprite(dest)
        assert result.size == tile.size
        palette_source = oak if mossy else substrate
        if not mossy and ("stem_glass" in src or "bark_glass" in src):
            palette_source = res.sprite(res.ns+":block/"+material+("_stem" if material in STEMS else "_log"))
        target = sorted({p[:3] for p in palette_source.getdata() if p[3]==255},key=lambda c:(lum(c),c))
        ranks = sorted(palette_groups[re.sub("_(upper|lower)$","",src)],key=lambda c:(lum(c),c))
        for y in range(16):
            for x in range(16):
                old = tile.getpixel((x,y))
                expected = old
                if (x,y) in mask:
                    fraction = ranks.index(old[:3]) / (len(ranks)-1)
                    base = target[int(fraction*(len(target)-1)+0.5)]
                    if mossy:
                        if wood != "spruce":
                            base = old[:3]
                        uv = ((x-3)*16//10%16,(y-2)*32//14%16) if ":item/" in src else (x,y)
                        before,after = oak.getpixel(uv),substrate.getpixel(uv)
                        if before != after:
                            gain = lum(base)/lum(before)
                            if darken_only:
                                gain = min(1.0, gain)
                            base = tuple(min(255,round(c*gain)) for c in after[:3])
                    expected = (*base,old[3])
                assert result.getpixel((x,y)) == expected,(src,dest,x,y)
                count += 1
    return count


def audit(res, material, fingerprints, preview_dir):
    asset = res.pack + "assets/"+NS+"/"
    data = res.pack + "data/"+NS+"/"
    bindings, members = {},set()
    styles = sorted((*STYLES,"stem_glass" if material in STEMS else "bark_glass"))
    sheet = Image.new("RGB",(990, 6*158+40),(44,49,55))
    draw = ImageDraw.Draw(sheet)
    draw.text((12,9),material.replace("_"," ").title()+"  |  Macaw reference / AMC closed front / AMC item",fill="white")
    labels = read(res.jar,asset+"lang/en_us.json")
    for i,style in enumerate(styles):
        wood = reference(style)
        src = f"{wood}_{style}_door"
        name = f"{material}_{style}_door"
        own = NS+":"+name
        members.add(own)
        for archive,ns in ((res.macaw,"mcwdoors"),(res.provider,res.ns)):
            assert "assets/"+ns+"/items/"+name+".json" not in archive.NameToInfo
        state = read(res.macaw,"assets/mcwdoors/blockstates/"+src+".json")
        actual = read(res.jar,asset+"blockstates/"+name+".json")
        assert state["variants"].keys() == actual["variants"].keys()
        for key,part in state["variants"].items():
            model = part["model"]
            dest = NS+":"+rename(model.split(":")[1],wood,material)
            assert actual["variants"][key] == dict(part,model=dest)
        src_item = read(res.macaw,"assets/mcwdoors/items/"+src+".json")["model"]["model"]
        dest_item = read(res.jar,asset+"items/"+name+".json")["model"]
        assert dest_item == {"type":"minecraft:model","model":NS+":item/"+name}
        for model in {p["model"] for p in state["variants"].values()}|{src_item}:
            dest = NS+":"+rename(model.split(":")[1],wood,material)
            old = read(*res.locate(model,"models","json"))
            new = read(*res.locate(dest,"models","json"))
            assert old.keys()==new.keys() and old["textures"].keys()==new["textures"].keys()
            assert dict(old,textures=new["textures"])==new
            for slot,tex in old["textures"].items():
                actual_tex = new["textures"][slot]
                if tex.startswith("mcwdoors:"):
                    assert actual_tex == NS+":"+rename(tex.split(":")[1],wood,material)
                    bindings[tex] = actual_tex,wood
                elif tex.endswith("/glass"):
                    assert actual_tex == tex
                else:
                    assert actual_tex == (NS+":block/mossy_oak_log" if material=="mossy_oak" else
                                          res.ns+":block/"+material+("_stem" if material in STEMS else "_log"))
                res.locate(actual_tex,"textures","png")
            if model != src_item:
                res.resolved(dest)
        expected_loot = res.macaw.read("data/mcwdoors/loot_table/blocks/"+src+".json").decode().replace("mcwdoors:"+src,own)
        assert read(res.jar,data+"loot_table/blocks/"+name+".json") == json.loads(expected_loot)
        for path in names(res.macaw):
            if "/tags/" in path and path.endswith(".json") and "mcwdoors:"+src in read(res.macaw,path)["values"]:
                assert own in read(res.jar,res.pack+path)["values"]
        x,y = (i%4)*246+10, (i//4)*158+35
        draw.text((x,y),labels["item."+NS+"."+name],fill="white")
        for offset,photo in ((0,res.front(state)),(64,res.front(actual)),(132,res.sprite(NS+":item/"+name))):
            photo = photo.resize((56,112) if offset<132 else (58,58),Image.Resampling.NEAREST)
            sheet.paste(photo,(x+offset,y+21),photo)
    unlock = read(res.jar,data+"advancement/recipes/"+material+"_macaw_doors.json")
    assert set(unlock["rewards"]["recipes"]) == members and len(members)==22
    destination = preview_dir / (material + ".png")
    destination.parent.mkdir(exist_ok=True,parents=True)
    sheet.save(destination)
    return members,check_pixels(res,material,bindings,fingerprints)


if __name__ == "__main__":
    if not __debug__:
        sys.exit("Verification requires assertions")
    current,*history,previous,macaw_path = map(Path,sys.argv[1:])
    assert hashlib.sha256(previous.read_bytes()).hexdigest()=="2a039898f657acd66012b6661866a20b77c24b2a0b595b3a2b138ff133593a76"
    subprocess.run([sys.executable,"-B",Path(__file__).with_name("verify_oak.py"),previous,*history],check=True)
    with ExitStack() as stack:
        jar,old,macaw,vanilla,end,ribbits = [stack.enter_context(ZipFile(p)) for p in
                (current,previous,macaw_path,history[1],history[6],history[-1])]
        changed = {n for n in names(old) if jar.read(n)!=old.read(n)}
        assert changed == {"fabric.mod.json","dev/resivore/amc/mixin/EnderscapeInitializationMixin.class",
                           "dev/resivore/amc/mixin/RibbitsInitializationMixin.class"},changed
        added = names(jar)-names(old)
        assert all(n.startswith(("resourcepacks/ribbits_doors/","resourcepacks/enderscape_doors/")) or
                   n in {"amc.doors.mixins.json","dev/resivore/amc/DoorIntegration.class","dev/resivore/amc/MacawDoorBlocks.class",
                         "dev/resivore/amc/mixin/DoorConstructor.class","dev/resivore/amc/mixin/MacawDoorsInitializationMixin.class"} for n in added)
        assert len([n for n in added if "/blockstates/" in n])==88
        members, fingerprints, pixels = set(),{},0
        for material,ns in MATERIALS.items():
            ids,count = audit(Resources(jar,macaw,vanilla,end if ns=="enderscape" else ribbits,ns),
                              material,fingerprints,current.parent.parent/"previews")
            assert not members.intersection(ids)
            members.update(ids)
            pixels += count
        assert len(members)==88
        assert hashlib.sha256(json.dumps(fingerprints,sort_keys=True).encode()).hexdigest()==MASK_DIGEST
        print(f"C30: {len(members)} door pairs, {len(added)} additions, {len(names(old))-len(changed)} identical C29 files. "
              f"Checked {pixels} RGBA pixels against 63 inspected source masks. Four preview sheets written.")
