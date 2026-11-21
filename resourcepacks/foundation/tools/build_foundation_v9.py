"""Build Foundation v9 from v8 and the read-only native Red Sandstone providers."""
import argparse
import hashlib
import io
import json
import os
from bisect import bisect_left
from contextlib import ExitStack
from copy import copy
from pathlib import Path
from tempfile import TemporaryDirectory
from zipfile import ZIP_DEFLATED, ZipFile, ZipInfo

from PIL import Image

PROJECT = Path(__file__).resolve().parents[1]
ARCHIVES = {
    "v8": ("resourcepacks/foundation/artifacts/Foundation v8.zip", "6ff2d732ab196204e0cfe7d1057953b5ce0d0ac09b10d476a234966913f36f92"),
    "matcha": ("originals/resourcepacks/Matcha_Flavoured_1_12.zip", "6209783021c358044abedabacee471faff5bd4080437d4e3b5e51963f1804248"),
    "bbb": ("projects/building-but-better/artifacts/bbb-fabric-26.2-2.0pre4+26.2-enderscape-dev.7.jar", "57ddb5dfe62f2eb9f4a2ce22fbeeb5cce4386bbd93aab3f7df0dd8e6d19ddaf0"),
    "mcwwindows": ("originals/mods/mcw-windows-2.4.2-mc26.2fabric.jar", "40ba9c55f191f8bd4758293683ed2bcf6d0c76c8f54f9fc8a0dccabdbcfd36ad"),
}
CLIENT_SHA256 = "40896ee9f1e2bec3c934daac7e93d41e9e3d9c2f8ae0ca366d52ffbfd1afa290"
BOTTOM = "minecraft:block/red_sandstone_bottom"
PRIVATE_BOTTOM = "foundation:block/red_sandstone_bottom"
VERTICAL = "assets/bbb/models/block/frame/stone/red_sandstone_vertical.json"
URN = "assets/bbb/models/block/urn/red_sandstone.json"


def resource(identifier, folder, extension):
    namespace, sep, name = identifier.partition(":")
    if not sep:
        namespace, name = "minecraft", namespace
    return f"assets/{namespace}/{folder}/{name}.{extension}"


def sprite(identifier):
    return resource(identifier, "textures", "png")


def encode(document):
    return (json.dumps(document, indent=2) + "\n").encode("utf-8")


def pixels(data):
    with Image.open(io.BytesIO(data)) as image:
        return image.convert("RGBA")


def light(rgb):
    return 299 * rgb[0] + 587 * rgb[1] + 114 * rgb[2]


def calibration(vanilla, matcha):
    # Both carvings preserve the vanilla pixel layout. Each original shade has
    # exactly one Matcha counterpart at every occurrence, in both references.
    lookup = {}
    for carving in ("cut_red_sandstone", "chiseled_red_sandstone"):
        path = sprite("minecraft:block/" + carving)
        original, target = (pixels(z.read(path)) for z in (vanilla, matcha))
        for before, after in zip(original.getdata(), target.getdata(), strict=True):
            key, value = before[:3], after[:3]
            if lookup.setdefault(key, value) != value:
                raise ValueError(f"Inconsistent Matcha shade in {path}")
    if len(lookup) != 7:
        raise ValueError("Expected seven measured Red Sandstone shades")
    return lookup


def transfer(rgb, anchors):
    # Extend the measured curve to black and white for the urn's deep recesses
    # and the column's raised highlights. Use one curve across seams and forms.
    if rgb in anchors:
        return anchors[rgb]
    ramp = sorted(anchors.items(), key=lambda pair: light(pair[0]))
    level = light(rgb)
    upper = bisect_left([light(key) for key, _ in ramp], level)
    lo, low_color = ramp[upper - 1]
    hi, high_color = ramp[upper]
    fraction = (level - light(lo)) / (light(hi) - light(lo))
    return tuple(round(a + (b - a) * fraction) for a, b in zip(low_color, high_color))


def recolor(data, anchors):
    image = pixels(data)
    palette = {color[:3] for color in image.getdata() if color[3]}
    lut = {rgb: transfer(rgb, anchors) for rgb in palette}
    image.putdata([(*lut[p[:3]], p[3]) if p[3] else p for p in image.getdata()])
    output = io.BytesIO()
    image.save(output, format="PNG")
    return output.getvalue()


def overrides(archives):
    base, bbb, windows = (archives[k] for k in ("v8", "bbb", "mcwwindows"))
    meta = json.loads(base.read("pack.mcmeta"))
    meta["pack"]["description"] = "Foundation v9"
    changes = {"pack.mcmeta": encode(meta)}
    anchors = calibration(archives["minecraft"], archives["matcha"])
    anchors.update({(0, 0, 0): (0, 0, 0), (255, 255, 255): (255, 255, 255)})
    for path in bbb.namelist():
        if path.startswith("assets/bbb/textures/") and "red_sandstone" in path and path.endswith(".png"):
            changes[path] = recolor(bbb.read(path), anchors)

    changes[sprite(PRIVATE_BOTTOM)] = recolor(archives["minecraft"].read(sprite(BOTTOM)), anchors)
    for path in windows.namelist():
        if path.startswith("assets/mcwwindows/models/") and "red_sandstone" in path and path.endswith(".json"):
            model = json.loads(windows.read(path))
            for slot, value in model["textures"].items():
                if value == BOTTOM:
                    model["textures"][slot] = PRIVATE_BOTTOM
            changes[path] = encode(model)

    # Other frame pieces already use this material's base particle. Rebind
    # only the vertical piece and the urn's orange bottom particle.
    for path, particle in ((VERTICAL, "minecraft:block/red_sandstone"), (URN, PRIVATE_BOTTOM)):
        model = json.loads(bbb.read(path))
        model["textures"]["particle"] = particle
        changes[path] = encode(model)
    return changes


def build(base, changes, destination):
    with ZipFile(destination, "w", ZIP_DEFLATED) as out:
        out.comment = base.comment
        for entry in base.infolist():
            data = changes.get(entry.filename)
            out.writestr(copy(entry), base.read(entry) if data is None else data)
        for path in sorted(changes.keys() - base.NameToInfo.keys()):
            entry = ZipInfo(path, (2026, 11, 21, 0, 0, 0))
            entry.create_system = 3
            entry.external_attr = 0o100644 << 16
            out.writestr(entry, changes[path], compress_type=ZIP_DEFLATED)


def publish(staged, destination):
    try:
        if os.name == "nt":
            staged.rename(destination)  # Windows refuses an existing destination.
        else:
            os.link(staged, destination)
    except FileExistsError:
        if staged.read_bytes() != destination.read_bytes():
            raise ValueError(f"Different artifact already exists at {destination}")


def main():
    from verify_foundation_v9 import verify

    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--sources", type=Path, default=PROJECT.parents[1], help="checkout retaining the private inputs")
    gradle = Path(os.environ.get("GRADLE_USER_HOME", Path.home() / ".gradle"))
    parser.add_argument("--client", type=Path, default=gradle / "caches/fabric-loom/26.2/minecraft-client.jar")
    parser.add_argument("--output", type=Path, default=PROJECT / "artifacts/Foundation v9.zip")
    parser.add_argument("--reports", type=Path, help="write complete model chains, palette checks and archive diff as TSV")
    args = parser.parse_args()
    inputs = {key: (args.sources / path, digest) for key, (path, digest) in ARCHIVES.items()}
    inputs["minecraft"] = (args.client, CLIENT_SHA256)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    with ExitStack() as stack, TemporaryDirectory(dir=args.output.parent) as temp:
        archives = {}
        for label, (path, digest) in inputs.items():
            if hashlib.sha256(path.read_bytes()).hexdigest() != digest:
                raise ValueError(f"Wrong source digest for {path}")
            archives[label] = stack.enter_context(ZipFile(path))
        staged = Path(temp) / "Foundation v9.zip"
        build(archives["v8"], overrides(archives), staged)
        with ZipFile(staged) as result:
            verify(archives, result, args.reports)
        digest = hashlib.sha256(staged.read_bytes()).hexdigest()
        publish(staged, args.output)
        if hashlib.sha256(args.output.read_bytes()).hexdigest() != digest:
            raise ValueError("Final artifact hash mismatch")
        print(f"{args.output.name}  {digest}  {args.output.stat().st_size:,} bytes")


if __name__ == "__main__":
    main()
