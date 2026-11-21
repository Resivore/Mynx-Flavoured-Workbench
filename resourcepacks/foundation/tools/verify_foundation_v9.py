"""Verify the v8 archive delta and trace each native provider state and item."""
import csv
import hashlib
import io
import json
from collections import Counter
from fractions import Fraction
from pathlib import PurePosixPath

from PIL import Image

from build_foundation_v9 import BOTTOM, PRIVATE_BOTTOM, URN, VERTICAL, light, pixels, resource, sprite

STATE_COUNTS = {
    "bbb": {"column": 60, "fence": 9, "frame": 48, "moulding": 80, "urn": 2},
    "mcwwindows": {"window": 32, "window2": 16, "four_window": 16, "pane_window": 64},
}
BASE = "minecraft:block/red_sandstone"
DECORATIONS = {"minecraft:block/dirt", "minecraft:block/glass", "mcwwindows:block/glass"}


def catalog(archives):
    for ns, forms in STATE_COUNTS.items():
        z = archives[ns]
        ids = {"red_sandstone_" + suffix for suffix in forms}
        for folder in ("blockstates", "items"):
            prefix = f"assets/{ns}/{folder}/"
            found = {n[len(prefix):-5] for n in z.namelist() if n.startswith(prefix)
                     and "red_sandstone" in n and n.endswith(".json")}
            assert found == ids, (folder, found)
        lang = json.loads(z.read(f"assets/{ns}/lang/en_us.json"))
        assert {key.split(".")[-1] for key, val in lang.items() if "red sandstone" in val.lower()} == ids
        for block in sorted(ids):
            yield ns, block


class Resources:
    def __init__(self, layers):
        self.layers = layers
        self.model_cache = {}

    def source(self, path):
        for label, archive in self.layers:
            if path in archive.NameToInfo:
                return label, archive
        raise KeyError(f"Unresolved asset {path}")

    def model(self, key):
        if key in self.model_cache:
            return self.model_cache[key]
        chain, slots, elements = [], {}, None
        current = key
        while current:
            assert current not in [model for model, _ in chain], chain
            path = resource(current, "models", "json")
            layer, archive = self.source(path)
            obj = json.loads(archive.read(path))
            chain.append((current, layer))
            slots = obj.get("textures", {}) | slots
            if elements is None:
                elements = obj.get("elements")
            current = obj.get("parent")
        assert elements, key

        def resolve(slot):
            visited = set()
            while slot.startswith("#"):
                assert slot not in visited
                visited.add(slot)
                slot = slots[slot[1:]]
            return slot if ":" in slot else "minecraft:" + slot

        faces = [resolve(f["texture"]) for el in elements for f in el["faces"].values()]
        particle = resolve("#particle") if "particle" in slots else None
        result = (chain, elements, faces, particle)
        self.model_cache[key] = result
        return result


def selections(archive, ns, block):
    identifier = f"{ns}:{block}"
    path = resource(identifier, "blockstates", "json")
    obj = json.loads(archive.read(path))
    states = obj.get("variants")
    if states is None:
        choices = [(json.dumps(part.get("when", {}), sort_keys=True), part["apply"]) for part in obj["multipart"]]
    else:
        choices = list(states.items())
    assert len(choices) == STATE_COUNTS[ns][block.removeprefix("red_sandstone_")]
    for condition, choice in choices:
        for variant in choice if isinstance(choice, list) else [choice]:
            yield path, condition, variant
    item = resource(identifier, "items", "json")
    selector = json.loads(archive.read(item))["model"]
    assert selector["type"] == "minecraft:model"
    yield item, "inventory", selector


def trace(archives, final):
    lower = [(name, archives[name]) for name in ("matcha", "bbb", "mcwwindows", "minecraft")]
    before = Resources([("v8", archives["v8"]), *lower])
    after = Resources([("v9", final), *lower])
    traces = [["block_id", "selector", "condition", "model_application", "model_chain_with_source",
               "v8_face_sprites", "v9_face_sprites", "v8_particle", "v9_particle"]]
    assets, used_models, roots = set(), set(), set()
    decorative = set()
    for ns, block in catalog(archives):
        for path, condition, select in selections(archives[ns], ns, block):
            assert before.source(path)[0] == after.source(path)[0] == ns
            key = select["model"]
            roots.add(key)
            old_chain, original, old_faces, old_particle = before.model(key)
            chain, elements, faces, particle = after.model(key)
            assert original == elements and [k for k, _ in chain] == [k for k, _ in old_chain]
            for model, layer in chain:
                if layer == "v9":
                    used_models.add(resource(model, "models", "json"))
            assert bool(old_particle) == bool(particle)
            pairs = list(zip(old_faces, faces, strict=True))
            if particle:
                pairs.append((old_particle, particle))
            for old, new in pairs:
                n = sprite(new)
                prev_src = before.source(sprite(old))[0]
                label, _ = after.source(n)
                if old == BOTTOM:
                    assert new == PRIVATE_BOTTOM and label == "v9" and prev_src == "minecraft"
                elif old == "bbb:block/frame/stone/stone":
                    assert key.endswith("/red_sandstone_vertical") and old == old_particle and new == BASE
                elif old.startswith("bbb:") and "red_sandstone" in old:
                    assert new == old and label == "v9" and prev_src == "bbb"
                elif old == BASE:
                    assert new == old and label == "v9" and prev_src == "v8"
                else:
                    assert old == new and old in DECORATIONS, (key, old, new)
                    assert label == prev_src
                    decorative.add(old)
                    continue
                assert label == "v9"
                if new != BASE:
                    assets.add(n)
            def describe(stack, identifier):
                return f"{identifier} [{stack.source(sprite(identifier))[0]}]" if identifier else ""
            traces.append([f"{ns}:{block}", path, condition, json.dumps(select, sort_keys=True),
                           " > ".join(f"{k} [{src}]" for k,src in chain),
                           ", ".join(describe(before, k) for k in sorted(set(old_faces))),
                           ", ".join(describe(after, k) for k in sorted(set(faces))),
                           describe(before, old_particle), describe(after, particle)])
    assert decorative == DECORATIONS and len(assets) == 10 and len(used_models) == 22
    assert len(traces) == 1 + 327 + 9
    print(f"9 native IDs, 327 blockstate applications, 9 item selectors, {len(roots)} distinct root models")
    return traces, assets, used_models


def verify_models(archives, final, models):
    """Check the entire JSON diff, including parent, display, geometry and every non-material slot."""
    for path in models:
        ns = path.split("/")[1]
        orig = json.loads(archives[ns].read(path))
        if path == VERTICAL:
            assert orig["textures"]["particle"] == "bbb:block/frame/stone/stone"
            orig["textures"]["particle"] = BASE
        elif path == URN:
            assert orig["textures"]["particle"] == "block/red_sandstone_bottom"
            orig["textures"]["particle"] = PRIVATE_BOTTOM
        else:
            assert ns == "mcwwindows"
            slots = [k for k, v in orig["textures"].items() if v == BOTTOM]
            assert len(slots) == 2 and "particle" in slots
            for key in slots:
                orig["textures"][key] = PRIVATE_BOTTOM
        assert json.loads(final.read(path)) == orig
    for ns in STATE_COUNTS:
        z = archives[ns]
        for path in z.namelist():
            if path.startswith(f"assets/{ns}/") and path.endswith(".json") and "red_sandstone" not in path:
                if any(folder in path for folder in ("/models/", "/items/", "/blockstates/")):
                    assert b"red_sandstone" not in z.read(path), path


def check_art(archives, final, paths):
    # Independent measurement from chiseled artwork; verify cut artwork yields precisely the same pairs.
    maps = []
    for name in ("chiseled_red_sandstone", "cut_red_sandstone"):
        path = sprite("minecraft:block/" + name)
        a, b = (pixels(archives[key].read(path)) for key in ("minecraft", "matcha"))
        pairs = {(p[:3], q[:3]) for p,q in zip(a.getdata(), b.getdata(), strict=True)}
        assert len(pairs) == len(dict(pairs)) == 7
        maps.append(dict(pairs))
    assert maps[0] == maps[1]
    measured = maps[0]
    ramp = sorted([((0,)*3, (0,)*3), *measured.items(), ((255,)*3, (255,)*3)], key=lambda p: light(p[0]))
    mappings = {}
    report = [["asset", "source", "size", "material_pixels", "transparent_pixels", "opaque_black_backing",
               "visible_material_palette_source_to_v9"]]
    total = Counter()
    for path in sorted(paths):
        src = "minecraft" if path == sprite(PRIVATE_BOTTOM) else "bbb"
        original = sprite(BOTTOM) if src == "minecraft" else path
        a, b = pixels(archives[src].read(original)), pixels(final.read(path))
        assert a.size == b.size and a.getchannel("A").tobytes() == b.getchannel("A").tobytes()
        counts, colors = Counter(), {}
        for p,q in zip(a.getdata(), b.getdata(), strict=True):
            if p[3] == 0 or p[:3] == (0,0,0):
                assert p == q
                counts["transparent" if p[3] == 0 else "black"] += 1
                continue
            key, value = p[:3], q[:3]
            assert key != value and colors.setdefault(key, value) == value
            assert mappings.setdefault(key, value) == value
            counts["material"] += 1
        # An injective, strictly luminance-ordered transfer preserves the full
        # silhouette of each shade, including the provider's least-used highlights.
        ordered = [colors[k] for k in sorted(colors, key=light)]
        assert all(light(x) < light(y) for x,y in zip(ordered, ordered[1:]))
        if src == "minecraft":
            assert all(measured[k] == v for k,v in colors.items())
        hexcolor = lambda rgb: bytes(rgb).hex()
        pairs = ", ".join(f"{hexcolor(k)}>{hexcolor(colors[k])}" for k in sorted(colors, key=light))
        report.append([path, original, f"{a.width}x{a.height}", counts["material"], counts["transparent"],
                       counts["black"], pairs])
        total.update(counts)
    for key, value in mappings.items():
        for (lo, lcolor), (hi, hcolor) in zip(ramp, ramp[1:]):
            if light(lo) <= light(key) <= light(hi):
                t = Fraction(light(key) - light(lo), light(hi) - light(lo))
                assert value == tuple(round(l + t*(h-l)) for l,h in zip(lcolor, hcolor))
                break
        else:
            raise ValueError(f"Uncalibrated material shade {key}")
    assert len(set(mappings.values())) == len(mappings)
    print(f"Material pixels {total['material']}, preserved transparent pixels {total['transparent']}, "
          f"preserved opaque black backing {total['black']}")
    return report


def check_archive(archives, final, additions):
    base = archives["v8"]
    original, names = set(base.namelist()), set(final.namelist())
    assert len(names) == len(final.namelist()) and final.testzip() is None
    assert original <= names and names - original == additions
    assert {p for p in original if base.read(p) != final.read(p)} == {"pack.mcmeta"}
    meta = json.loads(base.read("pack.mcmeta"))
    meta["pack"]["description"] = "Foundation v9"
    assert json.loads(final.read("pack.mcmeta")) == meta
    assert meta["pack"]["min_format"] == [88, 0] and meta["pack"]["max_format"] == 88
    atlas_path = "assets/minecraft/atlases/blocks.json"
    atlas = json.loads(archives["minecraft"].read(atlas_path))
    assert {"type": "minecraft:directory", "prefix": "block/", "source": "block"} in atlas["sources"]
    assert all(atlas_path not in z.NameToInfo for z in (final, archives["matcha"], archives["bbb"], archives["mcwwindows"]))
    for name in names:
        assert not name.startswith("/") and "\\" not in name and ".." not in PurePosixPath(name).parts
        if name.endswith((".json", ".mcmeta")):
            json.loads(final.read(name))
        elif name.endswith(".png"):
            data = io.BytesIO(final.read(name))
            with Image.open(data) as im:
                assert im.format == "PNG"
                im.verify()
            data.seek(0)
            with Image.open(data) as im:
                im.load()
    report = [["change", "entry", "v8_sha256", "v9_sha256"]]
    for path in sorted(additions | {"pack.mcmeta"}):
        before = hashlib.sha256(base.read(path)).hexdigest() if path in original else ""
        report.append(["modified" if before else "added", path, before, hashlib.sha256(final.read(path)).hexdigest()])
    print(f"Archive diff: {len(additions)} added, 1 modified, 0 removed, {len(original)-1} byte-identical")
    print(f"{sum(n.endswith('.png') for n in names)} PNG and {sum(n.endswith(('.mcmeta','.json')) for n in names)} JSON/metadata files")
    return report


def verify(archives, final, reports):
    if not __debug__:
        raise RuntimeError("Run with Python assertions enabled")
    traces, textures, models = trace(archives, final)
    verify_models(archives, final, models)
    palette = check_art(archives, final, textures)
    delta = check_archive(archives, final, textures | models)
    if reports:
        reports.mkdir(parents=True, exist_ok=True)
        for name, rows in (("model-chains", traces), ("artwork", palette), ("archive-diff", delta)):
            with (reports / (name + ".tsv")).open("w", encoding="utf-8", newline="") as f:
                csv.writer(f, delimiter="\t", lineterminator="\n").writerows(rows)
