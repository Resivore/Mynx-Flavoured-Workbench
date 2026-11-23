"""Build the Cut Red Sandstone side-overlay correction from exact Foundation v9."""
import argparse
import csv
import hashlib
import io
import json
import os
import re
from contextlib import ExitStack
from pathlib import Path, PurePosixPath
from tempfile import TemporaryDirectory
from zipfile import ZipFile

from PIL import Image

from build_foundation_v9 import ARCHIVES, CLIENT_SHA256, PROJECT, build, encode, pixels, publish
from verify_foundation_v9 import Resources
from foundation_v10_seams import verify_seams, comparison_sheet


BASE_SHA = "ecab2e5963df45805947b12a96dce8c586969ec2ba70737dd49b6d6b60946b2d"
CTM = "assets/minecraft/optifine/ctm/"
GROUP = CTM + "_overlays/cut_red_sandstone/"
RULE = GROUP + "0.properties"
BLOCK = "assets/minecraft/textures/block/"
CUT = BLOCK + "cut_red_sandstone.png"
LIVE = {3, 5, 7, 8, 10, 12, 13}
MARKS = {(0, 0), (0, 15)}
CYAN = (101, 245, 227, 3)


def properties(data):
    # The pinned packs use key=value, comments, and Java Properties' escaped
    # spaces. Reject other escape forms instead of silently misreading a selector.
    result = {}
    for raw in data.decode("latin-1").splitlines():
        line = raw.strip()
        if not line or line.startswith(("#", "!")):
            continue
        key, separator, value = line.partition("=")
        assert separator and key and key not in result and "\\" not in value.replace("\\ ", ""), raw
        result[key] = value.replace("\\ ", " ").strip()
    return result


def tiles(rule, spec):
    for term in spec.split():
        assert re.fullmatch(r"\d+(-\d+)?", term), (rule, term)
        ends = term.split("-")
        for index in range(int(ends[0]), int(ends[-1]) + 1):
            yield rule.rsplit("/", 1)[0] + f"/{index}.png"


def audit_rules(base, matcha, client, provider):
    layers = Resources([("Foundation", base), ("Matcha", matcha), ("Minecraft", client)])
    rows = [["rule", "role", "method", "matchBlocks", "matchTiles", "connectBlocks", "connectTiles",
             "connect", "faces", "layer", "priority", "tile_paths", "closure_or_condition"]]
    holes = []
    assert not any("/ctm/" in n and n.endswith(".properties") for n in matcha.namelist())
    for name in base.namelist():
        if not name.endswith(".properties"):
            continue
        prop = properties(base.read(name))
        if "/ctm/" not in name:
            continue
        paths = list(tiles(name, prop["tiles"]))
        missing = [p for p in paths if not any(p in z.NameToInfo for _, z in layers.layers)]
        holes.extend(missing)
        role = "other"
        if any("red_sandstone" in prop.get(k, "") for k in ("connectBlocks", "connectTiles")):
            role = "material"
        elif any("red_sandstone" in prop.get(k, "") for k in ("matchBlocks", "matchTiles")):
            assert prop["method"] == "overlay" and prop.get("connectBlocks", prop.get("connectTiles"))
            role = "receiver only"
        rows.append([name, role, prop["method"], *[prop.get(k, "") for k in
                     ("matchBlocks", "matchTiles", "connectBlocks", "connectTiles", "connect", "faces", "layer", "prioritize")],
                     " ".join(paths), "missing in v9: " + " ".join(missing) if missing else "resolved"])
    assert len(rows) == 152 and [r[0] for r in rows if r[1] == "material"] == [RULE]
    assert sum(r[1] == "receiver only" for r in rows) == 62
    assert set(holes) == {CTM + "chain/0.png", CTM + "chain/1.png"}
    assert properties(base.read(RULE)) == {
        "matchBlocks": "smooth_red_sandstone smooth_red_sandstone_slab", "method": "overlay", "tiles": "0-16",
        "connect": "block", "connectBlocks": "cut_red_sandstone", "layer": "cutout", "faces": "sides",
    }
    # A built-in Continuity rule with resourceCondition is enabled only for
    # its named source pack (vanilla by default). Check the actual winning PNG.
    if provider:
        for name in provider.namelist():
            if not ("/optifine/ctm/" in name and name.endswith(".properties")):
                continue
            rule = properties(provider.read(name)) if "red_sandstone" in name else {}
            if not rule:
                continue
            ref, _, pack = rule["resourceCondition"].partition("@")
            winner, _ = layers.source("assets/minecraft/" + ref)
            assert winner in ("Foundation", "Matcha") and pack in ("", "programmer_art")
            paths = list(tiles(name, rule["tiles"]))
            assert all(path in provider.NameToInfo for path in paths)
            rows.append([name, "provider condition inactive", rule["method"], rule.get("matchBlocks", ""),
                         rule.get("matchTiles", ""), "", "", rule.get("connect", ""), rule.get("faces", ""), "", "", " ".join(paths),
                         f"{ref}: supplied by {winner}; requires {pack or 'vanilla'}"])
        assert len(rows) == 158
    return rows


def audit_other_art(base, matcha, client):
    palette = set()
    for archive in (matcha, client):
        for path in archive.namelist():
            if path.startswith(BLOCK) and "red_sandstone" in path and path.endswith(".png"):
                image = pixels(archive.read(path))
                palette.update(color[:3] for _, color in image.getcolors(image.width * image.height))
    # Red Sand shares some Matcha RGBs with sandstone. Identify its actual
    # artwork at the original UV, including shades unique to sand.
    sand = pixels(matcha.read(BLOCK + "red_sand.png"))
    sand_pixels = 0
    for path in base.namelist():
        if not path.startswith(CTM) or not path.endswith(".png") or path.startswith(GROUP):
            continue
        image = pixels(base.read(path))
        visible = {rgba[:3] for _, rgba in image.getcolors(image.width * image.height) if rgba[3]}
        if not palette.isdisjoint(visible):
            assert path.startswith(CTM + "overlays/red_sand/") and image.size == sand.size
            for y in range(image.height):
                for x in range(image.width):
                    pixel = image.getpixel((x, y))
                    if pixel[3]:
                        assert pixel[:3] == sand.getpixel((x, y))[:3]
                        sand_pixels += 1
    assert sand_pixels == 983


def face_sources(base, matcha, client):
    stack = Resources([("Foundation", base), ("Matcha", matcha), ("Minecraft", client)])
    for name in ("cut_red_sandstone", "smooth_red_sandstone", "smooth_red_sandstone_slab"):
        path = f"assets/minecraft/blockstates/{name}.json"
        _, archive = stack.source(path)
        for state, variant in json.loads(archive.read(path))["variants"].items():
            key = variant["model"]
            _, elements, faces, _ = stack.model(key)
            directions = [direction for el in elements for direction in el["faces"]]
            for direction, sprite in zip(directions, faces, strict=True):
                if name == "cut_red_sandstone":
                    expected = "red_sandstone_top" if direction in ("up", "down") else name
                else:
                    expected = "polished_smooth_red_sandstone" if state == "type=double" else "smooth_red_sandstone"
                assert sprite == "minecraft:block/" + expected
                winner, _ = stack.source(BLOCK + expected + ".png")
                assert winner == ("Foundation" if expected == "red_sandstone_top" else "Matcha")
    return pixels(matcha.read(CUT))


def patch(base, source):
    meta = json.loads(base.read("pack.mcmeta"))
    meta["pack"]["description"] = "Foundation v10"
    changes = {"pack.mcmeta": encode(meta)}
    for index in sorted(LIVE):
        path = GROUP + f"{index}.png"
        old = pixels(base.read(path))
        # The two low-alpha cyan marks also exist in the non-red sibling.
        # Only the opaque, one-pixel-wide carved stone edge carries material.
        mask = old.getchannel("A").point(lambda alpha: 255 if alpha == 255 else 0)
        old.paste(source, mask=mask)
        out = io.BytesIO()
        old.save(out, format="PNG")
        changes[path] = out.getvalue()
    return changes


def verify_tiles(base, final, source, vanilla):
    updated = []
    for index in range(17):
        path = GROUP + f"{index}.png"
        old, new = (pixels(z.read(path)) for z in (base, final))
        assert old.size == new.size == source.size == vanilla.size == (16, 16)
        assert old.getchannel("A").tobytes() == new.getchannel("A").tobytes()
        if index not in LIVE:
            assert not old.getchannel("A").getbbox() and final.read(path) == base.read(path)
        else:
            sibling = pixels(base.read(path.replace("/cut_red_sandstone/", "/cut_sandstone/")))
            for y in range(16):
                for x in range(16):
                    pos = x, y
                    before, after = old.getpixel(pos), new.getpixel(pos)
                    if x == 15:
                        assert before[3] == 255 and after == source.getpixel(pos) and before != after
                        if y >= 2:
                            assert before == vanilla.getpixel(pos)
                    elif pos in MARKS:
                        assert before == after == sibling.getpixel(pos) == CYAN
                    else:
                        assert before == after and after[3] == 0
        updated.append(new)
    return updated


def verify_archive(base, final):
    original = base.namelist()
    assert original == final.namelist() and len(set(original)) == len(original)
    assert final.testzip() is None
    paths = []
    for name in original:
        assert not name.startswith("/") and "\\" not in name and ".." not in PurePosixPath(name).parts
        old, new = base.read(name), final.read(name)
        if old != new:
            paths.append([name, hashlib.sha256(old).hexdigest(), hashlib.sha256(new).hexdigest()])
        if name.endswith((".json", ".mcmeta")):
            json.loads(new)
        elif name.endswith(".properties"):
            properties(new)
        elif name.endswith(".png"):
            with Image.open(io.BytesIO(new)) as parsed:
                assert parsed.format == "PNG"
                parsed.verify()
            pixels(new)
    assert {row[0] for row in paths} == {"pack.mcmeta", *(GROUP + f"{i}.png" for i in LIVE)}
    meta = json.loads(base.read("pack.mcmeta"))
    assert meta["pack"] == {"description": "Foundation v9", "min_format": [88, 0], "max_format": 88}
    meta["pack"]["description"] = "Foundation v10"
    assert json.loads(final.read("pack.mcmeta")) == meta
    return [["modified_path", "v9_sha256", "v10_sha256"], *paths]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--sources", type=Path, default=PROJECT.parents[1])
    gradle = Path(os.environ.get("GRADLE_USER_HOME", Path.home() / ".gradle"))
    parser.add_argument("--client", type=Path, default=gradle / "caches/fabric-loom/26.2/minecraft-client.jar")
    parser.add_argument("--continuity", type=Path, help="include the 3.0.1+26.2 built-in rule exclusions")
    parser.add_argument("--reports", type=Path, help="write the exact rule inventory, ZIP diff and comparison sheet")
    parser.add_argument("--output", type=Path, default=PROJECT / "artifacts/Foundation v10.zip")
    args = parser.parse_args()
    if not __debug__:
        raise RuntimeError("Validation requires Python assertions")
    args.output.parent.mkdir(parents=True, exist_ok=True)
    with ExitStack() as stack, TemporaryDirectory(dir=args.output.parent) as temp:
        def checked(path, digest):
            if hashlib.sha256(path.read_bytes()).hexdigest() != digest:
                raise ValueError(f"Wrong input SHA-256: {path}")
            return stack.enter_context(ZipFile(path))

        base = checked(args.sources / "resourcepacks/foundation/artifacts/Foundation v9.zip", BASE_SHA)
        matcha = checked(args.sources / ARCHIVES["matcha"][0], ARCHIVES["matcha"][1])
        client = checked(args.client, CLIENT_SHA256)
        provider = checked(args.continuity, "2094a52b98e247f664ef2ecdbb41d7dcad7779cc7a8fb87f0ed8a4f8196c0446") if args.continuity else None
        rows = audit_rules(base, matcha, client, provider)
        audit_other_art(base, matcha, client)
        source = face_sources(base, matcha, client)
        staged = Path(temp) / "Foundation v10.zip"
        build(base, patch(base, source), staged)
        with ZipFile(staged) as final:
            diff = verify_archive(base, final)
            new = verify_tiles(base, final, source, pixels(client.read(CUT)))
            smooth = pixels(matcha.read(BLOCK + "smooth_red_sandstone.png"))
            verify_seams(new, source, smooth, LIVE)
            if args.reports:
                args.reports.mkdir(parents=True, exist_ok=True)
                for name, table in (("ctm-rules", rows), ("archive-diff", diff)):
                    with (args.reports / (name + ".tsv")).open("w", newline="", encoding="utf-8") as f:
                        csv.writer(f, delimiter="\t", lineterminator="\n").writerows(table)
                old = [pixels(base.read(GROUP + f"{i}.png")) for i in range(17)]
                comparison_sheet(old, new, source, smooth, args.reports)
        digest = hashlib.sha256(staged.read_bytes()).hexdigest()
        publish(staged, args.output)
        assert hashlib.sha256(args.output.read_bytes()).hexdigest() == digest
        print(f"{args.output.name}  {digest}  {args.output.stat().st_size:,} bytes")
        print(f"7 tiles, 112 material pixels, 14 non-material marks; {len(base.namelist())-8:,} byte-identical entries.")
        print("All target tiles resolve. Inherited missing CTM references: chain/0.png and chain/1.png.")


if __name__ == "__main__":
    main()
