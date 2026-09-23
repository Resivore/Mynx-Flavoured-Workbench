"""Audit the existing Bookshelf resource-pack seam without modifying either pack."""

import json
import os
from pathlib import Path
from zipfile import ZipFile


PROJECT = Path(__file__).resolve().parents[2]
ROOT = PROJECT.parents[1]
MATCHA = ROOT / "originals/resourcepacks/Matcha_Flavoured_1_12.zip"
OVERLAY = ROOT / "originals/resourcepacks/Matcha-Overlays-v37.zip"
CNM = ROOT / "originals/mods/clutternomore-2.0.7+26.2-fabric.jar"
CLIENT = Path(os.environ.get("GRADLE_USER_HOME", Path.home() / ".gradle")) / (
    "caches/fabric-loom/26.2/minecraft-client.jar"
)
GENERATED = PROJECT / "build/generated/nibaru-resources/assets/more_slabs_stairs_and_walls/models/block"

PACK_END = "minecraft:block/bookshelf_top"
FALLBACK_END = "minecraft:block/oak_planks"
SIDE = "minecraft:block/bookshelf"


def read_json(archive: ZipFile, path: str) -> dict:
    return json.loads(archive.read(path))


def check_role_model(model: dict, parent: str, end: str, label: str) -> None:
    assert model.get("parent") == parent, f"{label}: parent changed"
    textures = model["textures"]
    assert textures["top"] == textures["bottom"] == end, f"{label}: wooden end changed"
    assert textures["side"] == SIDE, f"{label}: Bookshelf side changed"


with ZipFile(MATCHA) as matcha, ZipFile(OVERLAY) as overlay, ZipFile(CNM) as cnm, ZipFile(CLIENT) as client:
    assert "assets/minecraft/textures/block/bookshelf_top.png" in matcha.namelist(), (
        "The actual Matcha wooden end texture is missing"
    )

    # Canonical Bookshelf uses an authored model, including its internal shelves.
    vanilla = read_json(client, "assets/minecraft/models/block/bookshelf.json")
    assert vanilla["textures"]["end"] == FALLBACK_END
    assert vanilla["textures"]["side"] == SIDE
    matcha_canonical = read_json(matcha, "assets/minecraft/models/block/bookshelf.json")
    assert matcha_canonical["textures"]["end"] == PACK_END
    overlay_canonical = read_json(overlay, "assets/minecraft/models/block/bookshelf.json")
    assert overlay_canonical["textures"]["ends"] == PACK_END
    assert overlay_canonical["textures"]["texture"] in {"block/bookshelf", SIDE}
    exterior = [(direction, face["texture"])
                for element in overlay_canonical["elements"]
                for direction, face in element["faces"].items()
                if direction in {"up", "down"} and face.get("cullface") == direction]
    assert sorted(exterior) == [("down", "#ends"), ("up", "#ends")], (
        "The overlay's canonical exposed wood faces no longer use its end role"
    )

    # Matcha Overlays supplies the six existing forms by model override. The
    # canonical form above is the fifteenth model; these fourteen cover the rest.
    existing = {
        "bookshelf_slab": "minecraft:block/slab",
        "bookshelf_slab_top": "minecraft:block/slab_top",
        "bookshelf_stairs": "minecraft:block/stairs",
        "bookshelf_stairs_inner": "minecraft:block/inner_stairs",
        "bookshelf_stairs_outer": "minecraft:block/outer_stairs",
        "bookshelf_wall_inventory": "more_slabs_stairs_and_walls:block/template_column_wall_inventory",
        "bookshelf_wall_post": "more_slabs_stairs_and_walls:block/template_column_wall_post",
        "bookshelf_wall_side": "more_slabs_stairs_and_walls:block/template_column_wall_side",
        "bookshelf_wall_side_tall": "more_slabs_stairs_and_walls:block/template_column_wall_side_tall",
        "vertical_bookshelf_slab": "clutternomore:block/templates/vertical_slab",
        "vertical_bookshelf_slab_double": "clutternomore:block/templates/vertical_slab_double",
        "bookshelf_step": "clutternomore:block/templates/step",
        "bookshelf_step_top": "clutternomore:block/templates/step_top",
        "bookshelf_step_double": "clutternomore:block/templates/step_double",
    }
    for name, parent in existing.items():
        namespace = "clutternomore" if parent.startswith("clutternomore:") else "more_slabs_stairs_and_walls"
        prefix = "more_slabs_stairs_and_walls/" if namespace == "clutternomore" else ""
        path = f"assets/{namespace}/models/block/{prefix}{name}.json"
        check_role_model(read_json(overlay, path), parent, PACK_END, path)

        if namespace == "more_slabs_stairs_and_walls":
            fallback = json.loads((GENERATED / f"{name}.json").read_text(encoding="utf-8-sig"))
            check_role_model(fallback, parent, FALLBACK_END, f"generated {name}")
        else:
            template = read_json(cnm, "assets/clutternomore/models/block/templates/"
                                 + parent.rsplit("/", 1)[1] + ".json")
            for element in template["elements"]:
                for direction, face in element["faces"].items():
                    expected = "#top" if direction == "up" else (
                        "#bottom" if direction == "down" else "#side"
                    )
                    assert face["texture"] == expected, (
                        f"{name}: CNM {direction} face changed its material role"
                    )

provider = (PROJECT / "src/nibaru/java/games/twinhead/moreslabsstairsandwalls/block/ModBlocks.java").read_text()
bridge = (PROJECT / "src/main/java/dev/aero/cnmterraincompat/mixin/AssetGeneratorMixin.java").read_text()
refresh = (PROJECT / "src/main/java/dev/aero/cnmterraincompat/mixin/ProviderAssetRefreshMixin.java").read_text()
assert 'BOOKSHELF(builder(Blocks.BOOKSHELF).setTextures("bookshelf", "oak_planks")' in provider
assert all(f'textures.addProperty("{role}"' in bridge for role in ("side", "top", "bottom"))
assert 'shape.getPath().startsWith(PROVIDER_PATH) ? Optional.empty() : existing' in refresh, (
    "CNM must regenerate Bookshelf Vertical Slab and Step fallback models after a pack change"
)

print("PASS: canonical plus 14 existing Bookshelf models use Matcha's real end seam; "
      "all six forms retain Bookshelf sides and pack-free fallback roles")
