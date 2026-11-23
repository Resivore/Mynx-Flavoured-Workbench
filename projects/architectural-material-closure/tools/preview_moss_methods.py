"""Render the owner-local Mossy Oak comparison from the retained November 23 inputs."""
from io import BytesIO
from pathlib import Path
from zipfile import ZipFile

from PIL import Image, ImageDraw, ImageFont
from moss import light

PROJECTS = Path(__file__).resolve().parents[2]
OUTPUT = PROJECTS.parent / "test-builds/mossy-oak-method"


def read(path, suffix):
    with ZipFile(path) as archive:
        member, = [name for name in archive.namelist() if name.endswith("/textures/" + suffix + ".png")]
        return Image.open(BytesIO(archive.read(member))).convert("RGBA")


def main():
    OUTPUT.mkdir(parents=True, exist_ok=True)
    client = Path.home() / ".gradle/caches/fabric-loom/26.2/minecraft-client.jar"
    def artifact(project, pattern):
        path, = (PROJECTS / project / "artifacts").glob(pattern)
        return path

    trees = artifact("mynx-trees", "*canary10.jar")
    amc = artifact("architectural-material-closure", "AMC C36.jar")
    beam = artifact("block-geometry-extensions", "BGE C111.jar")
    bbb = artifact("building-but-better", "*dev.7.jar")
    ribbits = artifact("ribbits-26.2", "*mynx-canary27.jar")
    oak = read(client, "block/oak_planks")
    moss = read(ribbits, "block/mossy_oak_planks")
    mask = {}
    for y in range(16):
        for x in range(16):
            uv = x, y
            shade = moss.getpixel(uv)
            if shade != oak.getpixel(uv) and shade[0] - shade[1] <= 13:
                mask[uv] = shade[:3]
    ramp = sorted(set(mask.values()), key=light)
    samples = [(client, "block/" + name, trees, "block/" + name.replace("oak", "mossy_oak"))
               for name in ("oak_log", "oak_log_top", "stripped_oak_log", "stripped_oak_log_top")]
    samples += [(bbb, "block/beam/oak", beam, "block/private/ribbits/mossy_oak_beam"),
                (client, "block/oak_shelf", amc, "block/mossy_oak_shelf"),
                (client, "entity/boat/oak", amc, "entity/boat/mossy_oak")]
    print("Reference footprint", len(mask), "palette", ramp)
    for page, batch in enumerate((samples[:4], samples[4:]), 1):
        sheet = Image.new("RGB", (840, 80 + 180*len(batch)), "#232723")
        draw = ImageDraw.Draw(sheet)
        font = ImageFont.truetype("arial.ttf", 18)
        for col, title in enumerate(("Oak input", "Shipped", "Darken only", "Bounded ramp")):
            draw.text((20+col*205, 17), title, fill="white", font=font)
        for row, (source, path, release, dest) in enumerate(batch):
            base, current = read(source, path), read(release, dest)
            replay, clamp, bounded = [base.copy() for _ in range(3)]
            hits = bright = shadow = floor = 0
            for y in range(base.height):
                for x in range(base.width):
                    uv = x % 16, y % 16
                    pixel = base.getpixel((x,y))
                    if not pixel[3] or uv not in mask:
                        continue
                    if path.endswith("shelf") and (pixel[0]-pixel[1]<10 or pixel[1]-pixel[2]<8):
                        continue
                    shade = mask[uv]
                    gain = light(pixel) / light(oak.getpixel(uv))
                    desired = min(1, gain) * light(shade)
                    selected = min((c for c in ramp if light(c)<=light(shade)),
                                   key=lambda c: abs(light(c)-desired))
                    for im, rgb in ((replay, [min(255,round(c*gain)) for c in shade]),
                                    (clamp, [round(c*min(1,gain)) for c in shade]),
                                    (bounded, selected)):
                        im.putpixel((x,y), (*rgb,pixel[3]))
                    hits += 1
                    bright += light(replay.getpixel((x,y))) > light(shade)
                    shadow += gain < 1
                    floor += desired < light(ramp[0])
            matches = replay.tobytes() == current.tobytes()
            assert matches or path == "block/stripped_oak_log_top", dest
            print(dest, base.size, hits, bright, shadow, floor, matches)
            label = dest.split("/")[-1] if "boat/" not in dest else "mossy_oak_boat (entity atlas)"
            y0 = 51 + row*180
            draw.text((20,y0), label, fill="#e1dbb5", font=font)
            for col, im in enumerate((base,current,clamp,bounded)):
                scale = min(8, 182//im.width, 135//im.height)
                enlarged = im.resize((im.width*scale, im.height*scale), Image.Resampling.NEAREST)
                sheet.paste(enlarged,(20+col*205,y0+29),enlarged)
            if path == "entity/boat/oak":
                # Enlarge the same occupied wood region from all four atlas columns.
                detail = Image.new("RGB", (840, 195), "#232723")
                labels = ImageDraw.Draw(detail)
                for col, (title, im) in enumerate(zip(
                        ("Oak input", "Shipped", "Darken only", "Bounded ramp"),
                        (base, current, clamp, bounded), strict=True)):
                    labels.text((20+col*205, 10), title, fill="white", font=font)
                    crop = im.crop((8, 8, 32, 24)).resize((192, 128), Image.Resampling.NEAREST)
                    detail.paste(crop, (12+col*205, 48), crop)
                detail.save(OUTPUT / "boat-detail.png")
        sheet.save(OUTPUT / f"comparison-{page}.png")


if __name__ == "__main__":
    main()

