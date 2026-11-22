"""Check the conditional wood family against the retained Oak graphs and native pixel pairs."""
import hashlib
import io
import json
import subprocess
import sys
from collections import Counter
from contextlib import ExitStack
from pathlib import Path
from zipfile import ZipFile

from PIL import Image

NS = "architectural_material_closure"
PACK = "resourcepacks/ribbits/"
FORMS = ("frame", "lattice", "trim", "balustrade", "support", "pallet")
# Packed UV islands have separate grain origins. Fingerprint the occupied moss
# coordinates independently of color, and check RGB gain against genuine moss below.
PACKED = {
    "frame/oak": "4d19ecb09a77f76eb1ce7c5bdbbbcef9b55f9b592efe678eb242a8f6dc5d34a1",
    "frame/oak_sticks": "0b18e26a15c7aae37e545dfc3ff47b3318e2fdc9d758c12bc988bde8008eff56",
    "pallet/oak_pallet": "d3f039d864ba348312c963eb2e8931fe6768b3b8ee534e4f17deffac12cbf48e",
}


def files(archive):
    return {name for name in archive.namelist() if not name.endswith("/")}


def read(archive, name):
    return json.loads(archive.read(name))


def pixel_image(archive, path):
    return Image.open(io.BytesIO(archive.read(path))).convert("RGBA")


def verify_sheets(jar, bbb, vanilla, ribbits, paths):
    oak = pixel_image(vanilla, "assets/minecraft/textures/block/oak_planks.png")
    moss = pixel_image(ribbits, "assets/ribbits/textures/block/mossy_oak_planks.png")
    pairs = list(zip(oak.getdata(), moss.getdata()))
    olive = Counter(after[:3] for before, after in pairs if before != after)
    assert olive == {(157,157,75):21, (134,128,60):19, (172,161,79):19, (118,105,47):4}
    assert all(before[0] - before[1] >= 23 and before[3] == after[3] == 255 for before, after in pairs)
    oak_colors = {before for before, _ in pairs}
    changed_pairs = set(pair for pair in pairs if pair[0] != pair[1])

    def shaded(pixel, pair):
        before, after = pair
        weights = (299, 587, 114)
        brightness = sum(w*c for w,c in zip(weights,pixel)) / sum(w*c for w,c in zip(weights,before))
        return tuple(min(255, round(c*brightness)) for c in after[:3]) + (pixel[3],)

    assert len(paths) == 15
    for path in paths:
        short = path.split("/textures/block/")[1][:-4].replace("mossy_oak","oak")
        provider, namespace = (vanilla,"minecraft") if short in ("oak_log","oak_log_top") else (bbb,"bbb")
        source = pixel_image(provider, f"assets/{namespace}/textures/block/{short}.png")
        result = pixel_image(jar, path)
        assert result.size == source.size
        if namespace == "bbb":
            assert {pixel for pixel in source.getdata() if pixel[3]} <= oak_colors, short
        mask = bytearray()
        for i,(old,new) in enumerate(zip(source.getdata(),result.getdata())):
            mask.append(old != new)
            assert old[3] == new[3] and (old[3] or old == new)
            if short in PACKED:
                assert old == new or any(shaded(old,pair) == new for pair in changed_pairs)
            else:
                before, after = pairs[i]
                assert new == (shaded(old,(before,after)) if old[3] and before != after else old)
        if short in PACKED:
            assert hashlib.sha256(mask).hexdigest() == PACKED[short]
            expected_rgba = {
                "frame/oak": "c1f881078e443bb6c36a4cfb42f0e2c6ad845baa04631897c4e2e65c13627760",
                "frame/oak_sticks": "5bfc6cb894668fb502776a7ddcd20868fc5c1e53a5b7c382ab0b4fcee74fce73",
                "pallet/oak_pallet": "880ffe99a12593e018b639df26842fda5fa91e0e73ad5beb8d79033842f9ae22",
            }
            assert hashlib.sha256(result.tobytes()).hexdigest() == expected_rgba[short]
        assert any(mask), short


def verify_graphs(jar, bbb, vanilla, ribbits, added):
    asset = PACK + f"assets/{NS}/"
    model_paths = {n for n in added if n.startswith(asset+"models/")}
    sprite_paths = {n for n in added if n.startswith(asset+"textures/")}
    visited, used_sprites = set(), set()

    def binding(value):
        if not value.startswith("bbb:") or "/plants/" in value or "/template/" in value:
            return value
        return value.replace("bbb:",NS+":").replace("oak","mossy_oak")

    def check_model(value):
        dest = binding(value)
        ns, path = value.split(":") if ":" in value else ("minecraft",value)
        original_name = f"assets/{ns}/models/{path}.json"
        if value == dest:
            (bbb if ns == "bbb" else vanilla).getinfo(original_name)
            return dest
        target = asset+"models/"+dest.split(":")[1]+".json"
        if target in visited:
            return dest
        visited.add(target)
        model = read(bbb,original_name)
        if "parent" in model:
            model["parent"] = check_model(model["parent"])
        for key, tex in model.get("textures",{}).items():
            if tex.split(":")[-1] == "block/oak_planks":
                ribbits.getinfo("assets/ribbits/textures/block/mossy_oak_planks.png")
                model["textures"][key] = "ribbits:block/mossy_oak_planks"
            else:
                path = tex.split(":")[-1].replace("oak","mossy_oak")
                model["textures"][key] = NS+":"+path
                used_sprites.add(asset+"textures/"+path+".png")
        assert model == read(jar,target), target
        return dest

    for form in FORMS:
        for folder in ("blockstates","items"):
            path = f"{folder}/oak_{form}.json"
            model = read(bbb,"assets/bbb/"+path)
            branches = ([model["model"]] if folder == "items" else model["variants"].values()
                        if "variants" in model else [node["apply"] for node in model["multipart"]])
            for node in branches:
                node["model"] = check_model(node["model"])
            assert model == read(jar,asset+path.replace("oak_","mossy_oak_"))
        own = f"{NS}:mossy_oak_{form}"
        for folder in ("recipe","loot_table/blocks"):
            original = bbb.read(f"data/bbb/{folder}/oak_{form}.json").decode()
            expected = json.loads(original.replace("bbb:oak_"+form,own)
                                  .replace("minecraft:oak_slab","ribbits:mossy_oak_planks_slab")
                                  .replace("minecraft:oak_planks","ribbits:mossy_oak_planks"))
            if folder == "recipe" and form == "balustrade":
                expected["key"]["M"] = "minecraft:moss_block"
                expected["pattern"] = [" A "," AM"," A "]
            assert expected == read(jar,PACK+f"data/{NS}/{folder}/mossy_oak_{form}.json")
        unlock, = (n for n in added if "/advancement/" in n and n.endswith(f"/mossy_oak_{form}.json"))
        assert read(jar,unlock)["rewards"]["recipes"] == [own]
    for path in (n for n in files(bbb) if "/tags/" in n and n.endswith(".json")):
        values = read(bbb,path)["values"]
        members = [f"{NS}:mossy_oak_{form}" for form in FORMS if "bbb:oak_"+form in values]
        if members:
            assert read(jar,PACK+path) == {"replace":False,"values":members}
    assert visited == model_paths and len(visited) == 44
    assert used_sprites == sprite_paths
    verify_sheets(jar,bbb,vanilla,ribbits,sprite_paths)


if __name__ == "__main__":
    if not __debug__:
        sys.exit("Verification requires assertions enabled")
    current, *refs, previous, ribbits_path = map(Path,sys.argv[1:])
    assert hashlib.sha256(previous.read_bytes()).hexdigest() == "c90194fb12306d1c47b22023eab23e62869f8310a56ab2c322c8f635bbce7fa9"
    subprocess.run([sys.executable,"-B",Path(__file__).with_name("verify_c28.py"),previous,*refs],check=True)
    with ExitStack() as stack:
        jar, old, vanilla, bbb, ribbits = [stack.enter_context(ZipFile(p)) for p in
                                           (current,previous,refs[1],refs[2],ribbits_path)]
        assert files(old) <= files(jar)
        assert [n for n in files(old) if old.read(n) != jar.read(n)] == ["fabric.mod.json"]
        added = files(jar)-files(old)
        assert {n for n in added if not n.startswith(PACK)} == {
            "dev/resivore/amc/MossyOakBlocks.class","dev/resivore/amc/MossyOakClient.class",
            "dev/resivore/amc/mixin/RibbitsInitializationMixin.class","amc.ribbits.mixins.json",
            "dev/resivore/amc/mixin/BbbInitializationMixin.class"}
        assert len(added) == 108
        verify_graphs(jar,bbb,vanilla,ribbits,added)
        print("C29 matched C28 payloads, six Oak block/item graphs, 44 model definitions and 15 wood sheets.")
