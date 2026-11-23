"""Audit a C12 JAR against C11 and C30 and render all 88 material/design comparisons locally."""
import argparse
from contextlib import ExitStack
from functools import lru_cache
from itertools import product
from math import ceil, floor
from pathlib import Path
from zipfile import ZipFile
import json

from PIL import Image, ImageDraw
from amc_macaw import AMC, DD, MCW, asset, entries, image, mask, read, western_board, western_icon, whisper_palette, whisper_tile
from preview_native_doors import font


class Graph:
    def __init__(self, jars, wood):
        self.jars = jars
        provider = "ribbits" if wood == "mossy_oak" else "enderscape"
        self.packs = {DD: [f"resourcepacks/amc_{wood}_doors/", ""],
                      AMC: [f"resourcepacks/{provider}_doors/", f"resourcepacks/{provider}/", ""]}

    def locate(self, ref, kind, ext="json"):
        ns = ref.split(":")[0]
        path = asset(ref, kind, ext)
        jar = self.jars[ns]
        for prefix in self.packs.get(ns, [""]):
            if prefix + path in jar.NameToInfo:
                return jar, prefix + path
        raise ValueError(f"Unresolved {path}")

    @lru_cache(None)
    def data(self, ref, kind):
        return read(*self.locate(ref, kind))

    @lru_cache(None)
    def sprite(self, ref):
        from io import BytesIO
        jar, name = self.locate(ref, "textures", "png")
        with Image.open(BytesIO(jar.read(name))) as im:
            assert im.width == 16 and im.height % 16 == 0, name
            frame = 0
            if im.height > 16:
                frames = read(jar, name + ".mcmeta")["animation"].get("frames", [0])
                frame = frames[0] if isinstance(frames[0], int) else frames[0]["index"]
            return im.crop((0, frame*16, 16, frame*16+16)).convert("RGBA")

    @lru_cache(None)
    def model(self, ref):
        textures, elements, seen = {}, [], set()
        while ref:
            if ":" not in ref:
                ref = "minecraft:" + ref
            if ref.startswith("minecraft:builtin/"):
                break
            assert ref not in seen, ref
            seen.add(ref)
            model = self.data(ref, "models")
            for k, v in model.get("textures", {}).items():
                textures.setdefault(k, v)
            if not elements:
                elements = model.get("elements", [])
            ref = model.get("parent")
        def resolve(t):
            chain = set()
            while t.startswith("#"):
                assert t not in chain, t
                chain.add(t)
                t = textures[t[1:]]
            self.sprite(t)
            return t
        # Resolve sampled faces, particle and generated item. Caller also checks every candidate binding.
        bindings = {face["texture"]: resolve(face["texture"]) for e in elements for face in e["faces"].values()}
        if "particle" in textures:
            resolve("#particle")
        if "layer0" in textures:
            bindings["#layer0"] = resolve("#layer0")
        return elements, bindings

    def face(self, branch, scale=3):
        elements, bindings = self.model(branch["model"])
        # Local west elevation. Each listed template is authored through its thickness on X.
        out = Image.new("RGBA", (16 * scale, 16 * scale))
        for e in sorted(elements, key=lambda el: el["from"][0], reverse=True):
            face = e["faces"].get("west")
            if not face:
                continue
            assert not e.get("rotation", {}).get("angle", 0)
            x0, x1 = e["from"][2], e["to"][2]
            y0, y1 = 16 - e["to"][1], 16 - e["from"][1]
            if x1 <= x0 or y1 <= y0:
                continue
            u0, v0, u1, v1 = face.get("uv", [x0, y0, x1, y1])
            tex = self.sprite(bindings[face["texture"]])
            for y in range(max(0, ceil(y0*scale-.5)), min(16*scale, ceil(y1*scale-.5))):
                for x in range(max(0, ceil(x0*scale-.5)), min(16*scale, ceil(x1*scale-.5))):
                    u, v = ((x+.5)/scale-x0)/(x1-x0), ((y+.5)/scale-y0)/(y1-y0)
                    for _ in range(face.get("rotation", 0)//90):
                        u, v = v, 1-u
                    color = tex.getpixel((floor(u0+u*(u1-u0))%16, floor(v0+v*(v1-v0))%16))
                    if color[3]:
                        out.putpixel((x, y), color)
        return out

    def elevation(self, ref, tall):
        states = variants(self.data(ref, "blockstates"))
        parts = ("upper", "middle", "lower") if tall else ("upper", "lower")
        out = Image.new("RGBA", (48, 48 * len(parts)))
        for i, part in enumerate(parts):
            branch = select(states, dict(facing="east", open="false", hinge="left", **{("third" if tall else "half"): part}))
            out.paste(self.face(branch), (0, i*48))
        return out


def variants(state):
    return [(dict(s.split("=") for s in key.split(",")), branch) for key, branch in state["variants"].items()]


def select(variants, state):
    matches = [branch for props, branch in variants if props.items() <= state.items()]
    assert len(matches) == 1, state
    return matches[0]


def archive_diff(candidate, old):
    files = lambda z: {n for n in z.namelist() if not n.endswith("/")}
    new, prev = files(candidate), files(old)
    assert len(new) == len([i for i in candidate.infolist() if not i.is_dir()])
    assert not prev - new
    changed = {n for n in prev if candidate.read(n) != old.read(n)}
    assert changed == {"fabric.mod.json", "com/fizzware/dramaticdoors/fabric/DramaticDoorsFabric.class"}, changed
    a, b = (read(z, "fabric.mod.json") for z in (candidate, old))
    assert a.pop("version").endswith("canary12") and b.pop("version").endswith("canary11") and a == b
    for n in new - prev:
        assert (n.startswith("resourcepacks/amc_") or n == "dramaticdoors/amc_macaw_doors.tsv" or
                n.startswith("com/fizzware/dramaticdoors/fabric/AmcMacawDoors") and n.endswith(".class")), n
    print(f"C11 comparison: {len(prev)-len(changed):,} identical, {len(changed)} changed, {len(new-prev):,} added.")


def verify_art(graph, wood, jars):
    # Compare actual output pixels to the pre-transfer tall masks in their own UV space.
    from amc_macaw import luma, palette, ranked, rename_wood
    dd, amc, macaw, mc, end, rib = jars
    count = fixed = mossy = 0
    tones = {}
    prefix = f"resourcepacks/amc_{wood}_doors/assets/dramaticdoors/textures/"
    oak = image(mc, "minecraft:block/oak_planks")
    moss = image(rib, "ribbits:block/mossy_oak_planks")
    lookup = whisper_palette(mc, macaw)
    classic = {}
    for kind, suffixes in (("item", [""]), ("block", ["_lower", "_middle", "_upper"])):
        colors = set()
        for suffix in suffixes:
            tile, coords = mask(dd, f"dramaticdoors:{kind}/macaw/tall_spruce_classic_door{suffix}", "spruce")
            colors.update(tile.getpixel(xy)[:3] for xy in coords)
        classic[kind] = ranked(colors, palette(oak))
    for n in graph.jars[DD].namelist():
        if not n.startswith(prefix) or not n.endswith(".png"):
            continue
        path = n[len(prefix):-4]
        ref = DD + ":" + path
        actual = graph.sprite(ref)
        srcwood = "spruce" if "_classic_door" in ref else "warped" if "_stem_glass_door" in ref else "oak"
        if "_western_" in path:
            if path.startswith("item/"):
                source = image(amc, AMC + ":item/" + wood + "_western_door", graph.packs[AMC][0])
                assert actual.tobytes() == western_icon(source).tobytes()
                continue
            original, wooden = western_board(image(macaw, "mcwdoors:block/western/oak_western_door_lower"), oak)
        elif "_whispering_" in path:
            original, wooden = whisper_tile(dd, DD + ":" + path.replace("/amc_macaw/", "/").replace(wood+"_whispering", "pale_oak"), lookup)
        else:
            donor = rename_wood(ref.replace("/amc_macaw/", "/macaw/"), wood, srcwood)
            original, wooden = mask(dd, donor, srcwood)
        assert original.getchannel("A").tobytes() == actual.getchannel("A").tobytes()
        left, top, right, bottom = original.getbbox()
        field = path.rsplit("_", 1)[0] if path.endswith(("_lower","_middle","_upper","_bottom","_top")) else path
        shades = tones.setdefault(field, {})
        if wood != "mossy_oak":
            bark = "_stem_glass_" in path or "_bark_glass_" in path
            surface = wood + ("_stem" if wood in ("celestial","murublight") else "_log") if bark else wood + "_planks"
            eligible = set(palette(image(end, "enderscape:block/" + surface)))
        for y, x in product(range(16), repeat=2):
            xy = x, y
            before, after = original.getpixel(xy), actual.getpixel(xy)
            if xy not in wooden:
                assert before == after, (ref, xy, "protected component")
                fixed += 1
            elif wood != "mossy_oak":
                assert after[:3] in eligible, (ref, xy, "wood palette")
                assert shades.setdefault(before[:3], after[:3]) == after[:3], (ref, xy, "cross-section ramp")
            else:
                uv = ((x-left)*16//(right-left), (y-top)*48//(bottom-top)%16) if path.startswith("item/") else xy
                clean, green = oak.getpixel(uv), moss.getpixel(uv)
                expected = classic[path.split("/")[0]][before[:3]] if srcwood == "spruce" else before[:3]
                if green != clean and green[0]-green[1] <= 13:
                    expected = tuple(min(255, round(c*luma(expected)/luma(clean))) for c in green[:3])
                    mossy += 1
                assert after[:3] == expected, (ref, xy)
            count += 1
    for field, shades in tones.items():
        ordered = [luma(shades[c]) for c in sorted(shades, key=lambda c:(luma(c),c))]
        assert ordered == sorted(ordered), (field, "luminance order")
    print(f"{wood}: {count:,} template texels, {fixed:,} protected, {mossy:,} selective moss samples.")


def verify(mapping, candidate, input_paths, out):
    rows = entries(mapping)
    with ExitStack() as stack:
        dd, amc, macaw, mc, end, rib, new = [stack.enter_context(ZipFile(p)) for p in [*input_paths, candidate]]
        archive_diff(new, dd)
        assert new.read("dramaticdoors/amc_macaw_doors.tsv") == Path(mapping).read_bytes()
        for wood in dict.fromkeys(r["wood"] for r in rows):
            jars = {DD: new, AMC: amc, MCW: macaw, "minecraft": mc, "enderscape": end, "ribbits": rib}
            g = Graph(jars, wood)
            ref = Graph(dict(jars, dramaticdoors=dd), wood)
            selected = [r for r in rows if r["wood"] == wood]
            keys = {"block." + row["tall"].replace(":", ".") for row in selected}
            for locale in ("en_us", "ru_ru", "zh_cn"):
                labels = read(new, g.packs[DD][0] + f"assets/dramaticdoors/lang/{locale}.json")
                assert set(labels) == keys and len(set(labels.values())) == len(keys), locale
            sheet = Image.new("RGB", (1020, 1760), "#f6f3ec")
            draw = ImageDraw.Draw(sheet)
            draw.text((20, 10), "Dramatic Doors C12   /   " + wood.replace("_", " ").title(), font=font(28, True), fill="#24323a")
            draw.text((20, 52), "Left to right: structural reference, AMC two-block door, C12 tall door, C12 inventory sprite.", font=font(17), fill="#3e5057")
            for idx, row in enumerate(selected):
                src, dst, template = (row[k] for k in ("source", "tall", "template"))
                design = src.split(":")[1][len(wood)+1:-5]
                variants_out = variants(g.data(dst, "blockstates"))
                template_states = variants(ref.data(template, "blockstates"))
                assert len(variants_out) == 48
                for facing, hinge, opened, third in product(("north","east","south","west"), ("left","right"), ("true","false"), ("lower","middle","upper")):
                    state = dict(facing=facing, hinge=hinge, open=opened, third=third)
                    branch = select(variants_out, state)
                    elements, _ = g.model(branch["model"])
                    assert elements
                    model = g.data(branch["model"], "models")
                    for value in model["textures"].values():
                        if not value.startswith("#"):
                            g.sprite(value)
                    if design != "western":
                        original = select(template_states, state)
                        assert {k:v for k,v in branch.items() if k!="model"} == {k:v for k,v in original.items() if k!="model"}
                        assert ref.model(original["model"])[0] == elements
                    else:
                        upper = third == "upper"
                        old_branch = select(variants(g.data(src,"blockstates")), dict(facing=facing,hinge=hinge,open=opened,half="upper" if upper else "lower"))
                        if third != "middle":
                            assert branch == old_branch
                        else:
                            assert branch.get("y",0) == old_branch.get("y",0)
                            foot = g.model(old_branch["model"])[0]
                            seam = lambda es: sorted((e["from"][0],e["from"][2],e["to"][0],e["to"][2]) for e in es)
                            assert seam(elements) == seam([e for e in foot if e["to"][1]==16])
                            assert all(e["from"][1]==0 and e["to"][1]==16 for e in elements)
                model = g.data(dst, "items")["model"]["model"]
                icon = g.sprite(g.model(model)[1]["#layer0"])
                assert icon.getbbox() and g.data(model,"models")["parent"].split(":")[-1]=="item/generated"
                card_x, card_y = 10 + idx%3*338, 91 + idx//3*207
                draw.rounded_rectangle((card_x,card_y,card_x+328,card_y+198),9,fill="#dfe2de")
                title = design.replace("_"," ").title()
                draw.text((card_x+12,card_y+5),title,font=font(17, True),fill="#23383b")
                is_tall = design != "western"
                panels = [ref.elevation(template, is_tall), g.elevation(src, False), g.elevation(dst, True), icon.resize((48,48),Image.Resampling.NEAREST)]
                for j, im in enumerate(panels):
                    x, y = card_x+24+j*79, card_y+172-im.height
                    sheet.paste(im,(x,y),im)
                for j,label in enumerate(("Macaw 2" if not is_tall else "Pale Oak 3" if design=="whispering" else "DD ref 3","AMC 2","C12 3","Item")):
                    draw.text((card_x+47+j*79,card_y+184),label,anchor="mm",font=font(12),fill="#41535a")
            out.mkdir(parents=True,exist_ok=True)
            sheet.save(out/(wood+".png"))
            verify_art(g, wood, (dd, amc, macaw, mc, end, rib))
    print("88 mappings, 4,224 placed state selections, 88 flat item graphs and four comparison sheets passed.")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("mapping")
    parser.add_argument("inputs", nargs=6, help="DD C11, AMC C30, Macaw, Minecraft client, Enderscape, Ribbits")
    parser.add_argument("candidate")
    parser.add_argument("output", type=Path)
    a = parser.parse_args()
    verify(a.mapping, a.candidate, a.inputs, a.output)
