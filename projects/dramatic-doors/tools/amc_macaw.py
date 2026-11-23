"""Compose the local AMC tall-door packs from verified reference JARs. Requires Pillow."""
import csv
import hashlib
import json
import re
import sys
from collections import defaultdict
from contextlib import ExitStack
from io import BytesIO
from pathlib import Path
from zipfile import ZipFile

from PIL import Image

DD = "dramaticdoors"
AMC = "architectural_material_closure"
MCW = "mcwdoors"
PEERS = "acacia bamboo birch cherry crimson dark_oak jungle mangrove oak pale_oak spruce warped".split()
FIXTURES = {(33, 25, 11), (65, 48, 20), (75, 55, 24), (79, 63, 38)}
HASHES = ["ad0ed399c6dcb7e0f6659865fb820169bd174be2c9cd4556ea61e0c82cf2f7f2",
          "f0b4a7f464026a1c021d7004d81a18b9896cfd428018887e2bf69f1fdbe43462",
          "00e431d662489fe4a145f0e471837e1cd2bc9bcce5abe3f9f9941230765b2ded"]


def asset(ref, kind, ext="json"):
    ns, path = ref.split(":")
    return f"assets/{ns}/{kind}/{path}.{ext}"


def read(jar, path):
    return json.loads(jar.read(path))


def write(root, path, data):
    file = root / path
    file.parent.mkdir(parents=True, exist_ok=True)
    file.write_text(json.dumps(data, ensure_ascii=False, separators=(",", ":")) + "\n", encoding="utf8", newline="\n")


def image(jar, ref, prefix=""):
    with Image.open(BytesIO(jar.read(prefix + asset(ref, "textures", "png")))) as im:
        assert im.size == (16, 16), ref
        return im.convert("RGBA")


def pixels(im):
    return list(im.getdata())


def luma(rgb):
    return 299 * rgb[0] + 587 * rgb[1] + 114 * rgb[2]


def ramp(colors):
    return sorted(set(colors), key=lambda c: (luma(c), c))


def palette(im):
    return ramp(p[:3] for p in pixels(im) if p[3] == 255)


def ranked(colors, target):
    colors = ramp(colors)
    n = max(1, len(colors) - 1)
    return {c: target[(2 * i * (len(target) - 1) + n) // (2 * n)] for i, c in enumerate(colors)}


def rename_wood(ref, wood, new):
    return re.sub(r"(?<![a-z])" + wood + r"(?=_)", new, ref)


def mask(jar, ref, wood):
    tile = image(jar, ref)
    peers = [image(jar, other) for w in PEERS if w != wood and
             asset(other := rename_wood(ref, wood, w), "textures", "png") in jar.NameToInfo]
    assert peers, ref
    keep = set()
    for i, (r, g, b, a) in enumerate(pixels(tile)):
        xy = (i % 16, i // 16)
        if a != 255:
            continue
        if "stem_glass" in ref:
            wooden = (r, g, b) not in {(208, 234, 233), (168, 168, 168)}
        else:
            wooden = r > g + 8 and g > b + 10 and (":item/" not in ref or (r, g, b) not in FIXTURES)
            wooden &= sum(p.getpixel(xy) == (r, g, b, a) for p in peers) * 2 < len(peers)
        if wooden:
            keep.add(xy)
    return tile, keep


def whisper_palette(mc, macaw):
    # Establish exact design identity before using DD's tall Pale Oak composition.
    lookup = {}
    for half, name in (("top", "upper"), ("bottom", "lower")):
        pale = image(mc, "minecraft:block/pale_oak_door_" + half)
        oak = image(macaw, "mcwdoors:block/oak_whispering_door_" + name)
        for src, dest in zip(pixels(pale), pixels(oak)):
            assert lookup.setdefault(src, dest) == dest
    assert len(set(lookup.values())) == len(lookup) == 9
    return lookup


def whisper_tile(dd, ref, lookup):
    tile = image(dd, ref)
    result = tile.copy()
    wooden = set()
    for i, rgba in enumerate(pixels(tile)):
        if not rgba[3]:
            continue
        xy = (i % 16, i // 16)
        # The dark outline is peculiar to DD's small inventory illustration.
        src = lookup.get(rgba)
        if src is None:
            assert rgba[:3] == (136, 119, 119), (ref, rgba)
            src = (81, 61, 36, 255)
        result.putpixel(xy, src)
        if rgba[0] != rgba[1]:
            wooden.add(xy)
    return result, wooden


def western_board(oak, plank):
    # The original join spans z=1..16. Carry the slatted field between its two stiles.
    tile = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(1, 16):
            if x in (1, 2, 14, 15):
                shade = oak.getpixel((x, (y + x) % 9))
            elif y in (0, 15):
                shade = oak.getpixel((x, 0))
            elif y in (4, 9, 13):
                shade = oak.getpixel((x, 2))     # Recess under each louver.
            elif y in (5, 10, 14):
                shade = oak.getpixel((x, 3))
            else:
                shade = plank.getpixel(((x + y // 5 * 2) % 16, y))
            tile.putpixel((x, y), shade)
    return tile, {(x, y) for x in range(1, 16) for y in range(16)}


def western_icon(src):
    # Keep the high horn, stepped cap, both hinge pins, and bottom rail at native icon density.
    item = Image.new("RGBA", (16, 16))
    for dx, x in enumerate((2, 3, 4, 6, 8, 10, 11, 12), 4):
        for y in range(2, 14):
            item.putpixel((dx, y - 2 if y < 9 else y + 2), src.getpixel((x, y)))
        for y in range(7, 11):
            item.putpixel((dx, y), src.getpixel((x, (8, 9, 10, 9)[y - 7])))
    return item


class Material:
    def __init__(self, jars, wood, root):
        self.dd, self.amc, self.macaw, self.mc, end, rib, trees, new_amc = jars
        self.wood = wood
        self.ns = "ribbits" if wood == "mossy_oak" else "enderscape"
        if wood in ("silver_birch","wisteria"):
            self.amc, self.ns, end = new_amc, "mynx_trees", trees
        self.prefix = f"resourcepacks/{self.ns}_doors/"
        self.root = root / "resourcepacks" / ("amc_" + wood + "_doors")
        self.base = image(self.mc, "minecraft:block/oak_planks")
        self.moss = image(rib, "ribbits:block/mossy_oak_planks")
        self.plank = self.base if self.ns == "ribbits" else image(end, self.ns+":block/" + wood + "_planks")
        suffix = "_stem" if wood in ("celestial", "murublight") else "_log"
        self.bark_ref = (AMC if self.ns == "ribbits" else self.ns) + ":block/" + wood + suffix
        self.bark = image(self.mc, "minecraft:block/oak_log") if self.ns == "ribbits" else image(end, self.bark_ref)
        self.lookup = whisper_palette(self.mc, self.macaw)
        self.labels = {locale: {} for locale in ("en_us", "ru_ru", "zh_cn")}
        self.dd_zh = read(self.dd, "assets/dramaticdoors/lang/zh_cn.json")
        self.macaw_ru = read(self.macaw, "assets/mcwdoors/lang/ru_ru.json")
        self.macaw_zh = read(self.macaw, "assets/mcwdoors/lang/zh_cn.json")
        self.tiles = {}
        self.names = []
        self.items = read(self.amc, self.prefix + f"assets/{AMC}/lang/en_us.json")
        meta = read(self.dd, "resourcepacks/veiled_door/pack.mcmeta")
        meta["pack"]["description"] = "Dramatic Doors / AMC / " + wood
        write(self.root, "pack.mcmeta", meta)

    def bind(self, ref, wood, whisper=False):
        if ref == "minecraft:block/glass":
            return ref
        if ref in ("minecraft:block/oak_log", "minecraft:block/warped_stem"):
            return self.bark_ref
        # C11 routes the Barn Glass lower third to Barn. Its upper parent declares an unused
        # lower slot with no PNG; bind it to the very same lower tile the state actually renders.
        if ref == "dramaticdoors:block/macaw/tall_oak_barn_glass_door_lower":
            ref = ref.replace("_barn_glass_", "_barn_")
        assert ref.startswith("dramaticdoors:"), ref
        dest = ref.replace("/macaw/", "/amc_macaw/")
        if whisper:
            dest = ref.replace("/tall_pale_oak_door", "/amc_macaw/tall_" + self.wood + "_whispering_door")
        else:
            dest = rename_wood(dest, wood, self.wood)
        if dest not in self.tiles:
            tile, wooden = whisper_tile(self.dd, ref, self.lookup) if whisper else mask(self.dd, ref, wood)
            self.tiles[dest] = (tile, wooden, wood)
        return dest

    def standard(self, row, design):
        template = row["template"].split(":")[1]
        whisper = design == "whispering"
        wood = {"classic": "spruce", "stem_glass": "warped", "whispering": "pale_oak"}.get(design, "oak")
        states = read(self.dd, asset(DD + ":" + template, "blockstates"))
        for ref in sorted({v["model"] for v in states["variants"].values()}):
            model = read(self.dd, asset(ref, "models"))
            model["textures"] = {k: self.bind(t, wood, whisper) for k, t in model["textures"].items()}
            dest = rename_wood(ref, wood, self.wood).replace("/macaw/", "/amc_macaw/")
            if whisper:
                dest = ref.replace("/tall_pale_oak_door", "/amc_macaw/tall_" + self.wood + "_whispering_door")
            write(self.root, asset(dest, "models"), model)
            for v in states["variants"].values():
                if v["model"] == ref:
                    v["model"] = dest
        item = read(self.dd, asset(DD + ":item/" + template, "models"))
        item["textures"]["layer0"] = self.bind(item["textures"]["layer0"], wood, whisper)
        return states, item

    def western(self, row):
        # Outer sections inherit the exact AMC leaf models and their Macaw parents. The inserted
        # middle follows the seam footprint, orientation and mirroring of each particular lower model.
        old = read(self.amc, self.prefix + asset(row["source"], "blockstates"))["variants"]
        variants = {}
        middle = DD + ":block/amc_macaw/tall_" + self.wood + "_western_door_middle"
        oak = image(self.macaw, "mcwdoors:block/western/oak_western_door_lower")
        tile, wooden = western_board(oak, self.base)
        self.tiles[middle] = (tile, wooden, "oak")
        for key, branch in old.items():
            lower = "half=lower" in key
            third = "lower" if lower else "upper"
            variants[key.replace("half=" + third, "third=" + third)] = branch
            if not lower:
                continue
            source = read(self.amc, self.prefix + asset(branch["model"], "models"))
            geo = read(self.macaw, asset(source["parent"], "models"))
            elements = []
            for e in geo["elements"]:
                if e["to"][1] != 16:
                    continue
                # Treat each seam strip separately; this preserves both winding and side depth.
                e["from"][1] = 0
                for side in list(e["faces"]):
                    if side in ("up", "down"):
                        del e["faces"][side]
                        continue
                    face = e["faces"][side]
                    u0, _, u1, _ = face["uv"]
                    if side in ("north", "south"):
                        u0, u1 = (2, 4) if side == "north" else (13, 15)
                    face.update(uv=[u0, 0, u1, 16], texture="#board")
                    face.pop("rotation", None)
                elements.append(e)
            assert len(elements) == 2
            model = DD + ":block/amc_macaw/" + branch["model"].split("/")[-1].replace("_bot", "_middle")
            write(self.root, asset(model, "models"), {"ambientocclusion": False, "textures": {"board": middle, "particle": middle}, "elements": elements})
            variants[key.replace("half=lower", "third=middle")] = dict(branch, model=model)
        icon = image(self.amc, AMC + ":item/" + row["source"].split(":")[1], self.prefix)
        layer = DD + ":item/amc_macaw/tall_" + self.wood + "_western_door"
        self.save(layer, western_icon(icon))
        return {"variants": variants}, {"parent": "minecraft:item/generated", "textures": {"layer0": layer}}

    def save(self, ref, tile):
        path = self.root / asset(ref, "textures", "png")
        path.parent.mkdir(parents=True, exist_ok=True)
        tile.save(path)

    def door(self, row):
        source, target = row["source"], row["tall"]
        name = source.split(":")[1]
        design = name[len(self.wood) + 1:-5]
        states, item = self.western(row) if design == "western" else self.standard(row, design)
        assert len(states["variants"]) == 48, name
        write(self.root, asset(target, "blockstates"), states)
        item_model = DD + ":item/" + target.split(":")[1]
        write(self.root, asset(item_model, "models"), item)
        write(self.root, asset(target, "items"), {"model": {"type": "minecraft:model", "model": item_model}})
        loot = self.dd.read("resourcepacks/veiled_door/data/dramaticdoors/loot_table/blocks/tall_veiled_door.json")
        loot = json.loads(loot.decode().replace(DD + ":tall_veiled_door", target))
        loot["fabric:load_conditions"] = [{"condition": "fabric:registry_contains", "registry": "minecraft:block", "values": [target]}]
        write(self.root, f"data/dramaticdoors/loot_table/blocks/{target.split(':')[1]}.json", loot)
        key = "block." + target.replace(":", ".")
        self.labels["en_us"][key] = "Tall " + self.items["block." + source.replace(":", ".")]
        refwood = {"classic": "spruce", "stem_glass": "warped"}.get(design, "oak")
        suffix = f"{refwood}_{design}_door"
        # Use the source mod's Russian design names with DD's existing material terminology.
        ru = self.macaw_ru["item.mcwdoors." + suffix].lower()
        if design in ("stem_glass", "bark_glass"):
            ru = "дверь со стеклом и " + ("стеблем" if design == "stem_glass" else "корой")
        else:
            assert ru.startswith(("дубовая ", "еловая ")), ru
            ru = ru.split(" ", 1)[1]
        adjective = {"veiled": "вуальная ", "celestial": "поднебесная ", "murublight": "мурутравная ", "mossy_oak": "", "silver_birch": "", "wisteria": ""}[self.wood]
        self.labels["ru_ru"][key] = "Высокая " + adjective + ru + {"mossy_oak":" из замшелого дуба","silver_birch":" из серебристой берёзы","wisteria":" из глицинии"}.get(self.wood,"")
        native = self.wood if self.wood != "murublight" else "murushroom"
        chinese = "高银桦木" if self.wood=="silver_birch" else self.dd_zh["block.dramaticdoors.tall_" + native + "_door"].removesuffix("门")
        if design in ("western", "whispering"):
            zh = self.macaw_zh["block.mcwdoors." + suffix].removeprefix("橡木")
        else:
            template_name = self.dd_zh["block." + row["template"].replace(":", ".")]
            prefix = {"oak": "高橡木", "spruce": "高云杉木", "warped": "高诡异木"}[refwood]
            assert template_name.startswith(prefix), template_name
            zh = template_name[len(prefix):]
        self.labels["zh_cn"][key] = chinese + zh
        self.names.append(name)

    def finish(self):
        expected = {Path(n).stem for n in self.amc.namelist()
                    if n.startswith(self.prefix + f"assets/{AMC}/blockstates/{self.wood}_") and n.endswith("_door.json")}
        assert set(self.names) == expected and len(self.names) == 22
        for locale, labels in self.labels.items():
            write(self.root, f"assets/dramaticdoors/lang/{locale}.json", labels)
        values = [{"id": DD + ":tall_macaw_" + n, "required": False} for n in self.names]
        for tag in ("block/tall_wooden_doors", "item/tall_wooden_doors", "item/categories/tall_macaw_doors"):
            write(self.root, "data/dramaticdoors/tags/" + tag + ".json", {"replace": False, "values": values})
        groups = defaultdict(set)
        def group(ref):
            return re.sub("_(upper|middle|lower|top|bottom)$", "", ref)
        for ref, (tile, wooden, _) in self.tiles.items():
            groups[group(ref)].update(tile.getpixel(xy)[:3] for xy in wooden)
        for ref, (tile, wooden, wood) in self.tiles.items():
            target = self.bark if "_bark_glass_" in ref or "_stem_glass_" in ref else self.plank
            mapping = ranked(groups[group(ref)], palette(target))
            out = tile.copy()
            for x, y in wooden:
                p = tile.getpixel((x, y))
                color = mapping[p[:3]]
                if self.wood == "mossy_oak":
                    color = color if wood == "spruce" else p[:3]
                    # Inventory texels occupy DD's narrower tall silhouette, independently of AMC's icon.
                    if ":item/" in ref:
                        left, top, right, bottom = tile.getbbox()
                        uv = ((x-left)*16//(right-left), (y-top)*48//(bottom-top) % 16)
                    else:
                        uv = x, y
                    before, after = self.base.getpixel(uv), self.moss.getpixel(uv)
                    if before != after and after[0] - after[1] <= 13:
                        color = tuple(min(255, round(c*luma(color)/luma(before))) for c in after[:3])
                out.putpixel((x, y), (*color, p[3]))
            self.save(ref, out)


def entries(path):
    with Path(path).open(encoding="utf8") as stream:
        rows = list(csv.DictReader(stream, delimiter="\t"))
    assert len(rows) == len({r["source"] for r in rows}) == len({r["tall"] for r in rows}) == 132
    return rows


def generate(mapping, paths, root):
    if sys.platform == "win32":
        root = Path("//?/" + root.resolve().as_posix())
    for path, expected in zip(paths, HASHES):
        with Path(path).open("rb") as stream:
            assert hashlib.file_digest(stream, "sha256").hexdigest() == expected, path
    rows = entries(mapping)
    with ExitStack() as stack:
        jars = [stack.enter_context(ZipFile(p)) for p in paths]
        for wood in dict.fromkeys(r["wood"] for r in rows):
            material = Material(jars, wood, root)
            for row in rows:
                if row["wood"] == wood:
                    material.door(row)
            material.finish()


if __name__ == "__main__":
    generate(sys.argv[1], sys.argv[2:10], Path(sys.argv[10]))
