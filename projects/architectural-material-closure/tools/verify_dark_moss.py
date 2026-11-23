"""Verify the C36 to C37 production delta against the original Ribbits palette."""
from io import BytesIO
import sys
from pathlib import Path
from zipfile import ZipFile

from PIL import Image
from verify_doors import Resources, check_pixels
from verify_trapdoors import Graph, pixels
from tree_architecture import boat_wood_uvs


def image(archive, path):
    return Image.open(BytesIO(archive.read(path))).convert("RGBA")


def verify(current, previous, client, ribbits):
    oak = image(client, "assets/minecraft/textures/block/oak_planks.png")
    moss = image(ribbits, "assets/ribbits/textures/block/mossy_oak_planks.png")
    palette = {b[:3] for a,b in zip(oak.getdata(),moss.getdata()) if a != b and b[0]-b[1]<=13}
    assert len(palette) == 4
    entries = {n for n in previous.namelist() if not n.endswith("/")}
    assert entries == {n for n in current.namelist() if not n.endswith("/")}
    changed = []
    for n in sorted(entries):
        if previous.read(n) == current.read(n):
            continue
        if n in ("fabric.mod.json", "META-INF/MANIFEST.MF"):
            continue
        assert "/textures/" in n and "mossy_oak" in n and n.endswith(".png"), n
        assert not n.endswith("/mossy_oak_shutter.png"), n
        a,b = image(previous,n),image(current,n)
        assert a.size == b.size
        boat = "/entity/" in n and "boat/" in n
        original = image(client, "assets/minecraft/textures/entity/"+n.split("/entity/")[1].replace("mossy_oak","oak")) if boat else None
        hull = boat_wood_uvs() if boat else set()
        for y in range(a.height):
            for x in range(a.width):
                before, after = a.getpixel((x,y)), b.getpixel((x,y))
                assert before[3] == after[3], (n,x,y)
                if boat and (x,y) not in hull:
                    assert after == original.getpixel((x,y)), (n,x,y)
                elif before != after:
                    assert after[:3] in palette and all(p>=q for p,q in zip(before,after)), (n,x,y)
        changed.append(n)
    print("Verified changed AMC PNGs:",len(changed))
    print("\n".join(changed))


def verify_macaw(current, client, ribbits, doors, traps):
    res = Resources(current,doors,client,ribbits,"ribbits")
    graph = Graph([(current,"resourcepacks/ribbits_trapdoors/"),(client,""),(traps,""),(ribbits,"")])
    bindings = {}
    checked = 0
    for path in current.namelist():
        if "/textures/" not in path or not path.endswith(".png"):
            continue
        key = path.split("/textures/")[1][:-4]
        wood = "spruce" if "_classic_" in key else "oak"
        original = key.replace("mossy_oak", wood)
        dest = "architectural_material_closure:"+key
        if path.startswith("resourcepacks/ribbits_doors/"):
            bindings["mcwdoors:"+original] = dest,wood
        elif path.startswith("resourcepacks/ribbits_trapdoors/"):
            design = "stripped" if key.endswith("stripped_mossy_oak_log") else key.split("/")[1]
            source = ("minecraft:" if design=="stripped" else "mcwtrpdoors:")+original
            checked += pixels(graph,source,dest,"mossy_oak",design,"minecraft:block/oak_planks",darken_only=True)
    checked += check_pixels(res,"mossy_oak",bindings,{},darken_only=True)
    print("Verified",checked,"Macaw source texels against masks, oriented UVs and final pixels.")


if __name__ == "__main__":
    from contextlib import ExitStack
    with ExitStack() as stack:
        current,previous,client,ribbits,doors,traps = [stack.enter_context(ZipFile(p)) for p in sys.argv[1:]]
        verify(current,previous,client,ribbits)
        verify_macaw(current,client,ribbits,doors,traps)
