"""Apply the moss footprint from vanilla bricks in each Stone artwork's native UVs."""
from masonry import NS, RAMPS, image, material_id

MATERIAL = "mossy_stone_bricks"


def light(rgb):
    return 299 * rgb[0] + 587 * rgb[1] + 114 * rgb[2]


def stage(root, client, bbb, stone_shutter):
    def vanilla(name):
        return image(client, f"assets/minecraft/textures/block/{name}.png")

    bricks, moss = vanilla("stone_bricks"), vanilla(MATERIAL)
    stone_colors = set(bricks.getdata())
    # The mossy tile also edits eleven neutral pixels. Only its new colors are moss.
    pairs = [(before, after) for before, after in zip(bricks.getdata(), moss.getdata(), strict=True)
             if after not in stone_colors]
    assert len(pairs) == 87
    shade = sum(light(after) for _, after in pairs) / sum(light(before) for before, _ in pairs)
    gains = {color: tuple(shade * c / light(color) for c in color[:3]) for _, color in pairs}

    def save(source, name):
        result = source.copy()
        for y in range(source.height):
            for x in range(source.width):
                before = source.getpixel((x, y))
                gain = gains.get(moss.getpixel((x % moss.width, y % moss.height)))
                if before[3] and gain and light(before):
                    value = light(before)
                    result.putpixel((x, y), (*[min(255, round(value * factor)) for factor in gain], before[3]))
        dest = root / f"assets/{NS}/textures/block/{name}.png"
        dest.parent.mkdir(parents=True, exist_ok=True)
        result.save(dest)

    save(vanilla("stone"), "mossed_stone")
    save(bricks, "mossed_stone_bricks")
    save(stone_shutter, MATERIAL + "_louvered_shutter")
    for group in RAMPS:
        for sheet in group:
            save(image(bbb, f"assets/bbb/textures/block/{sheet}.png"), material_id(sheet, MATERIAL))
