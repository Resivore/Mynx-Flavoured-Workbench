"""Verify the release delta and the actual material, model and state graph of each new window."""
import hashlib
import json
import subprocess
import sys
from contextlib import ExitStack
from pathlib import Path
from shutil import copyfile
from tempfile import TemporaryDirectory
from zipfile import ZipFile

from doors import resource, read
from trapdoor_previews import Graph
from wood_matrix import WOODS, FIXTURES, NS, entries, nodes


def check(current, baseline, windows, client, bbb, end, rib):
    ids = {NS + ":" + name for wood in WOODS for name, *_ in entries(wood)}
    ids.update(NS + ":mossy_oak_" + form for form in FIXTURES)
    allowed = {"fabric.mod.json", *("dev/resivore/amc/" + name + ".class"
               for name in ("BambooBlocks", "MossyOakBlocks", "WoodAccessories"))}
    for path in baseline.namelist():
        before, after = baseline.read(path), current.read(path)
        if before == after or path in allowed:
            continue
        assert path.endswith(".json"), path
        old, new = json.loads(before), json.loads(after)
        if "/tags/" in path:
            assert old == dict(new, values=[v for v in new["values"] if v not in ids]), path
        else:
            assert "/lang/" in path and old == {k:v for k,v in new.items() if NS + ":" + k.split(".")[-1] not in ids}, path

    count = selections = 0
    forms = ("window", "window2", "four_window", "pane_window")
    for wood in ("oak","spruce","birch","jungle","acacia","dark_oak","mangrove","cherry","pale_oak","crimson","warped"):
        stem = wood in ("crimson","warped")
        for finish in ("raw","stripped","plank"):
            for form in forms:
                prefix = wood + ("_stem" if stem and form != "pane_window" else "")
                if finish == "stripped":
                    prefix = "stripped_" + wood + ("" if form == "pane_window" else "_stem" if stem else "_log")
                if finish == "plank":
                    prefix = wood + ("_planks" if stem and form != "pane_window" else "_plank")
                name = prefix + "_" + form
                for kind in ("items","blockstates"):
                    windows.getinfo(resource("mcwwindows:"+name, kind, "json"))
                windows.getinfo(f"data/mcwwindows/recipe/{name}.json")
                count += 1
    assert count == 132
    expected_items = set()
    for wood, owner in WOODS.items():
        pack = "" if owner == "minecraft" else "resourcepacks/" + owner + "/"
        graph = Graph([(current,pack),(current,""),(windows,""),(client,""),(bbb,""),(end,""),(rib,"")])
        for name, source, timber, form in entries(wood):
            expected_items.add(pack + resource(NS + ":" + name, "items", "json"))
            old, new = "mcwwindows:" + source, NS + ":" + name
            original = graph.get(old, "blockstates", "json")
            actual = graph.get(new, "blockstates", "json")
            assert original["variants"].keys() == actual["variants"].keys()
            branches = list(zip(nodes(original), nodes(actual)))
            branches.append((graph.get(old, "items", "json")["model"], graph.get(new, "items", "json")["model"]))
            for a, b in branches:
                assert a == dict(b, model=a["model"]), (name,a,b)
                source_model, own_model = (graph.resolve(node["model"]) for node in (a,b))
                frame_ns = NS if wood == "mossy_oak" and not timber.endswith("_planks") else owner
                for slot, tex in source_model["textures"].items():
                    wanted = frame_ns + ":block/" + timber if "oak" in tex else tex
                    assert own_model["textures"][slot] == wanted, (name,slot,tex)
                for tex in own_model["textures"].values():
                    if not tex.startswith("#"):
                        graph.get(tex,"textures","png")
                if "elements" in source_model:
                    a_elements, b_elements = source_model["elements"], own_model["elements"]
                    assert len(a_elements) == len(b_elements)
                    for e,f in zip(a_elements,b_elements):
                        assert dict(e,faces=f["faces"]) == f
                        for side, face in e["faces"].items():
                            bound = f["faces"][side]
                            assert face == dict(bound,texture=face["texture"])
                            slot = face["texture"][1:]
                            if "glass" in source_model["textures"].get(slot,"") or wood != "bamboo" or timber.endswith("_planks"):
                                assert face == bound
                selections += 1
            assert read(current,pack+f"data/{NS}/loot_table/blocks/{name}.json")["pools"][0]["entries"][0]["name"] == new
            if form == "window":
                assert read(current,pack+f"data/{NS}/recipe/{name}.json")["result"]["id"] == new
    ribpack = "resourcepacks/ribbits/"
    for form in FIXTURES:
        name = "mossy_oak_" + form
        current.getinfo(ribpack+resource(NS+":"+name,"blockstates","json"))
        if not form.startswith("wall_"):
            expected_items.add(ribpack+resource(NS+":"+name,"items","json"))
            for folder in ("recipe","loot_table/blocks"):
                current.getinfo(ribpack+f"data/{NS}/{folder}/{name}.json")
    actual = {p for p in set(current.namelist()) - set(baseline.namelist()) if "/items/" in p and p.endswith(".json")}
    assert actual == expected_items and len(actual) == 64
    print(f"64 new items, 60 window graphs, {selections} model selections and 132 original Macaw windows verified.")


if __name__ == "__main__":
    if not __debug__:
        sys.exit("Assertions required")
    current, previous, *history = map(Path, sys.argv[1:])
    assert hashlib.sha256(previous.read_bytes()).hexdigest() == "bae05199d1d9b6d8fd187b4f1f50b72db30d046a05038496b8ffce998095185c"
    with TemporaryDirectory(dir=current.parent.parent) as tmp:
        staged = Path(tmp)/"libs"/previous.name
        staged.parent.mkdir()
        copyfile(previous, staged)
        subprocess.run([sys.executable, "-B", Path(__file__).with_name("verify_accessories.py"), staged, *history], check=True)
    with ExitStack() as stack:
        check(*(stack.enter_context(ZipFile(p)) for p in (current,previous,history[0],history[1],history[2],history[6],history[12])))

