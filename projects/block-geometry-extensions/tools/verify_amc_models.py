"""Verify the production writer output captured by the AMC Fabric GameTest against the actual packs."""
import argparse
import hashlib
import itertools
import json
from collections import Counter
from contextlib import ExitStack
from pathlib import Path
from zipfile import ZipFile

AMC = "architectural_material_closure"
MATERIALS = ("prismarine_bricks", "quartz", "nether_bricks", "end_stone_bricks",
             "tuff", "cinnabar", "sulfur", "smooth_basalt")
PATTERNS = ("running_bond", "flagstone", "windmill_weave", "crystal_floor")
ROLES = ("block", "slab", "stairs", "wall", "vertical_slab", "step", "corner", "quarter_column", "layer")


def asset(identifier, folder, extension="json"):
    namespace, path = identifier.split(":") if ":" in identifier else ("minecraft", identifier)
    return namespace + ":" + folder + "/" + path + "." + extension


def selectors(node):
    if isinstance(node, list):
        for child in node:
            yield from selectors(child)
    elif isinstance(node, dict):
        if isinstance(node.get("model"), str):
            yield node
        else:
            for child in node.values():
                yield from selectors(child)


def block_uv(direction, bounds):
    (x, y, z), (a, b, c) = bounds
    return {"up": [x, z, a, c], "down": [x, 16-c, a, 16-z],
            "north": [16-a, 16-b, 16-x, 16-y], "south": [x, 16-b, a, 16-y],
            "west": [z, 16-b, c, 16-y], "east": [16-c, 16-b, 16-z, 16-y]}[direction]


def verify(audit, jars, version, enderscape):
    provider_pack = enderscape and version in ("24", "25", "26")
    def read(key):
        if key in audit["resources"]:
            return audit["resources"][key]
        namespace, path = key.split(":")
        entry = f"assets/{namespace}/{path}"
        for jar in reversed(jars):
            nested = "resourcepacks/enderscape/" + entry
            if provider_pack and Path(jar.filename).name == f"AMC C{version}.jar" and nested in jar.NameToInfo:
                return json.loads(jar.read(nested)) if entry.endswith(".json") else jar.read(nested)
            if entry in jar.NameToInfo:
                return json.loads(jar.read(entry)) if entry.endswith(".json") else jar.read(entry)
        raise KeyError(key)

    def model(key):
        data = read(asset(key, "models"))
        inherited = model(data["parent"]) if "parent" in data else {}
        return {**inherited, **data, "textures": {**inherited.get("textures", {}), **data.get("textures", {})}}

    def resolve(value, textures):
        seen = set()
        while value.startswith("#"):
            assert value not in seen, value
            seen.add(value)
            value = textures[value[1:]]
        return value

    materials = () if version == "none" else MATERIALS
    if provider_pack:
        materials += ("veradite", "mirestone", "kurodite")
    if version == "26":
        materials += ("purpur", "resin")
        if provider_pack:
            materials += ("dusk_purpur",)
    expected = {f"{AMC}:{m}_{p}" for m, p in itertools.product(materials, PATTERNS)}
    assert set(audit["membership"]) == expected
    if not expected:
        assert not audit["resources"]
    counts, sprite_hashes = Counter(), set()
    selector_counts = dict(zip(ROLES, (2, 3, 40, 9, 8, 12, 4, 6, 24), strict=True))
    for root, roles in audit["membership"].items():
        assert tuple(roles) == ROLES and roles["block"] == root
        name = root.split(":")[1]
        for role in ROLES[1:]:
            if role == "vertical_slab":
                expected_id = f"clutternomore:cnm_terrain_slabs_compat/vertical_{AMC}/{name}_slab"
            elif role == "step":
                expected_id = f"clutternomore:cnm_terrain_slabs_compat/{AMC}/{name}_step"
            else:
                expected_id = f"cnm_terrain_slabs_compat:{AMC}/{name}_{role}"
            assert roles[role] == expected_id, (root, role, roles[role], expected_id)
        canonical = read(asset(root, "blockstates"))["variants"]["engraved=false"]["model"]
        parent = model(canonical)
        sprite = resolve(parent["textures"]["particle"], parent["textures"])
        assert sprite == f"{AMC}:block/{name}", (root, sprite)
        png = read(asset(sprite, "textures", "png"))
        assert png[:8] == bytes.fromhex("89504e470d0a1a0a") and png[16:24] == bytes.fromhex("0000001000000010")
        sprite_hashes.add(hashlib.sha256(png).digest())
        for role, block in roles.items():
            state = read(asset(block, "blockstates"))
            item = read(asset(block, "items"))
            if role == "block":
                assert set(state["variants"]) == {"engraved=false", "engraved=true"}
            if role == "wall":
                wall = read("minecraft:blockstates/cobblestone_wall.json")
                expected_state = json.loads(json.dumps(wall).replace(
                    "minecraft:block/cobblestone_wall", block.replace(":", ":block/")))
                assert state == expected_state, block
            if role == "stairs":
                # Exact production reference tests every facing, half, inner/outer shape and UV lock.
                reference = read("mcwpaths:blockstates/dark_prismarine_flagstone_stairs.json")
                assert set(reference["variants"]) == set(state["variants"])
                for key, value in state["variants"].items():
                    ref = reference["variants"][key]
                    assert {k:v for k,v in ref.items() if k != "model"} == {k:v for k,v in value.items() if k != "model"}
                    assert value["model"].split("_stairs")[-1] == ref["model"].split("_stairs")[-1]
            placed = list(selectors(state))
            inventory = list(selectors(item))
            assert len(placed) == selector_counts[role] and len(inventory) == 1, (root, role)
            for selection in placed:
                if selection.get("x") or selection.get("y"):
                    assert selection.get("uvlock") is True, (block, selection)
            counts["placed selectors"] += len(placed)
            counts["previews"] += 1
            for key in {s["model"] for s in placed + inventory}:
                data = model(key)
                textures = data["textures"]
                assert resolve(textures["particle"], textures) == sprite, key
                assert data["elements"], key
                for elem in data["elements"]:
                    for direction, face in elem["faces"].items():
                        assert resolve(face["texture"], textures) == sprite, (key, direction)
                        expected_uv = block_uv(direction, (elem["from"], elem["to"]))
                        uv = face.get("uv", expected_uv)
                        assert face.get("rotation", 0) == 0
                        if role == "quarter_column" and key == inventory[0]["model"]:
                            # The centred preview retains the exact crop of the southeast quarter.
                            assert elem["from"] == [4, 0, 4] and elem["to"] == [12, 16, 12]
                            expected_uv = block_uv(direction, ([8, 0, 8], [16, 16, 16]))
                        assert uv == expected_uv, (key, direction, uv, expected_uv)
                        counts["model faces"] += 1
                counts["models"] += 1
    assert len(sprite_hashes) == len(expected)
    print(f"Verified {len(expected)} independent AMC patterned families with {dict(counts)}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--amc-version", default="23", choices=("none", "23", "24", "25", "26"))
    parser.add_argument("--enderscape", action="store_true")
    parser.add_argument("audit", type=Path)
    parser.add_argument("jars", nargs="+")
    args = parser.parse_args()
    audit = json.loads(args.audit.read_text(encoding="utf-8"))
    with ExitStack() as stack:
        verify(audit, [stack.enter_context(ZipFile(p)) for p in args.jars], args.amc_version, args.enderscape)
    report_name = ("amc-pattern-families.json" if args.amc_version == "26" and args.enderscape
                   else args.audit.stem + "-members.json")
    report = args.audit.parent / "reports" / report_name
    report.parent.mkdir(parents=True, exist_ok=True)
    report.write_text(json.dumps(audit["membership"], indent=2) + "\n", encoding="utf-8")
