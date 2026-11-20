"""Check the moss footprint, local shading, alpha and untouched art in the packaged textures."""
from fractions import Fraction

from masonry import image
from verify_masonry import NS


def check_moss(jar, bbb, client, expected):
    def tile(archive, ns, name):
        return image(archive, f"assets/{ns}/textures/block/{name}.png")

    clean = list(tile(client, "minecraft", "stone_bricks").getdata())
    moss = list(tile(client, "minecraft", "mossy_stone_bricks").getdata())
    # Independently locate the five chromatic greens. Both vanilla gray ramps
    # include a slightly green gray; its green/blue separation is just 3.
    mask = {i for i, p in enumerate(moss) if p != clean[i] and p[1] - p[2] > 3}
    assert len(mask) == 87 and len({moss[i] for i in mask}) == 5

    def value(rgb):
        return sum(c * w for c, w in zip(rgb, (299, 587, 114)))

    shade = Fraction(sum(value(moss[i]) for i in mask), sum(value(clean[i]) for i in mask))
    sheets = ["column/stone_" + part for part in ("lower", "middle", "none", "top", "upper")]
    sheets += ["polished_stone", "urn/stone", "moulding/stone", "fence/stone_fence", "frame/stone/stone"]
    pairs = [(bbb, "bbb", name, name.replace("stone", "mossy_stone_bricks")) for name in sheets]
    pairs += [(client, "minecraft", "stone", "mossed_stone"), (client, "minecraft", "stone_bricks", "mossed_stone_bricks"),
              (jar, NS, "stone_louvered_shutter", "mossy_stone_bricks_louvered_shutter")]
    for archive, ns, name, output in pairs:
        source = tile(archive, ns, name)
        result = tile(jar, NS, output)
        expected.add(f"assets/{NS}/textures/block/{output}.png")
        assert source.size == result.size
        changed = 0
        tones = []
        for y in range(source.height):
            for x in range(source.width):
                i = (y % 16) * 16 + x % 16
                a, b = source.getpixel((x, y)), result.getpixel((x, y))
                if i not in mask or not a[3] or not value(a):
                    assert b == a, (name, x, y, "unmossed artwork")
                    continue
                moss_rgb = moss[i][:3]
                local_value = value(a) * shade
                for actual, channel in zip(b[:3], moss_rgb):
                    ideal = local_value * channel / value(moss_rgb)
                    assert abs(actual - ideal) <= Fraction(1, 2), (name, x, y, "moss hue or brightness")
                assert a[3] == b[3] and a != b
                assert abs(value(b) - local_value) <= 500  # At most half an RGB level from rounding.
                changed += 1
                tones.append((value(a), value(b)))
        assert 0 < changed < source.width * source.height and min(tones)[1] < max(tones)[1]
        print(f"{output}: {changed}/{source.width * source.height} moss pixels, local shading and all other RGBA verified.")
