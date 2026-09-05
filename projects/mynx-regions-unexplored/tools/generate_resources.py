"""Reproducible private resource assembler; originals remain immutable and untracked."""
import hashlib, json, os, pathlib, shutil, subprocess, zipfile

ROOT = pathlib.Path(__file__).resolve().parents[1]
OUT = ROOT / "build/generated/resources"
COMMON = pathlib.Path(subprocess.check_output(["git", "rev-parse", "--path-format=absolute", "--git-common-dir"], text=True).strip()).parent
INPUTS = json.loads((ROOT / "build-inputs.json").read_text(encoding="utf-8"))
RU = COMMON / INPUTS["reference"]["path"]
MC = pathlib.Path(os.environ.get("MYNX_MINECRAFT_JAR", pathlib.Path.home() / ".gradle/caches/fabric-loom/26.2/minecraft-merged.jar"))
NS = "mynx_regions_unexplored"
OBTAINABLE = INPUTS["scope"]["obtainable"]
COMPANIONS = INPUTS["scope"]["companions"]
ALL_BLOCKS = OBTAINABLE + COMPANIONS

def write(path, value):
    target = OUT / path
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_bytes(value if isinstance(value, bytes) else (json.dumps(value, indent=2) + "\n").encode())

def rename(value):
    if isinstance(value, str):
        return value.replace("regions_unexplored:", NS + ":")
    if isinstance(value, list):
        return [rename(v) for v in value]
    if isinstance(value, dict):
        return {k: rename(v) for k, v in value.items()}
    return value

def references(value, key):
    found = []
    if isinstance(value, dict):
        for k, v in value.items():
            if k == key and isinstance(v, str): found.append(v)
            found.extend(references(v, key))
    elif isinstance(value, list):
        for v in value: found.extend(references(v, key))
    return found

def texture_values(value):
    found = []
    if isinstance(value, dict):
        for key, child in value.items():
            if key == "textures" and isinstance(child, dict):
                found.extend(v for v in child.values() if isinstance(v, str))
            found.extend(texture_values(child))
    elif isinstance(value, list):
        for child in value: found.extend(texture_values(child))
    return found

def main():
    if OUT.exists(): shutil.rmtree(OUT)
    if RU.stat().st_size != INPUTS["reference"]["size"] or hashlib.sha256(RU.read_bytes()).hexdigest() != INPUTS["reference"]["sha256"]:
        raise ValueError("Regions Unexplored reference identity changed")
    if hashlib.sha256(MC.read_bytes()).hexdigest() != INPUTS["minecraft_baseline"]["sha256"]:
        raise ValueError("Minecraft 26.2 baseline changed")

    with zipfile.ZipFile(RU) as ru, zipfile.ZipFile(MC) as mc:
        model_queue, seen_models, textures = [], set(), set()
        for name in ALL_BLOCKS:
            old = json.loads(ru.read(f"assets/regions_unexplored/blockstates/{name}.json"))
            transformed = rename(old)
            write(f"assets/{NS}/blockstates/{name}.json", transformed)
            model_queue.extend(references(transformed, "model"))
        for name in OBTAINABLE:
            old = rename(json.loads(ru.read(f"assets/regions_unexplored/models/item/{name}.json")))
            write(f"assets/{NS}/models/item/{name}.json", old)
            model_queue.extend(references(old, "parent")); textures.update(texture_values(old))
        while model_queue:
            ident = model_queue.pop()
            if ident.startswith("minecraft:") or ident.startswith("#"): continue
            if ":" not in ident: ident = NS + ":" + ident
            namespace, path = ident.split(":", 1)
            if namespace != NS or not path.startswith("block/") or ident in seen_models: continue
            seen_models.add(ident)
            old_path = f"assets/regions_unexplored/models/{path}.json"
            # RU's packaged potted models point at an omitted generated copy of this vanilla template.
            # Reconstruct only that template from the exact 26.2 baseline instead of importing unrelated output.
            vanilla_path = f"assets/minecraft/models/{path}.json"
            if old_path not in ru.namelist() and vanilla_path in mc.namelist():
                model = json.loads(mc.read(vanilla_path))
            else:
                model = rename(json.loads(ru.read(old_path)))
            write(f"assets/{NS}/models/{path}.json", model)
            model_queue.extend(references(model, "parent")); textures.update(texture_values(model))
        for ident in sorted(textures):
            if ident.startswith("#") or ident.startswith("minecraft:"): continue
            if ":" not in ident:
                # Texture variables inherited from vanilla templates are not asset identities.
                continue
            namespace, path = ident.split(":", 1)
            if namespace != NS: continue
            write(f"assets/{NS}/textures/{path}.png", ru.read(f"assets/regions_unexplored/textures/{path}.png"))

        grass_tint = {"type": "minecraft:grass", "temperature": .5, "downfall": 1.0}
        for name in OBTAINABLE:
            model = {"type": "minecraft:model", "model": f"{NS}:item/{name}"}
            if name in {"stone_bud", "clover"}: model["tints"] = [grass_tint]
            elif name == "windswept_grass": model["tints"] = [{"type": "minecraft:constant", "value": -12012264}]
            write(f"assets/{NS}/items/{name}.json", {"model": model})
        write(f"assets/{NS}/lang/en_us.json", {f"block.{NS}.{n}": n.replace("_", " ").title() for n in OBTAINABLE})

        def vanilla(path): return json.loads(mc.read(path))
        def replaced(obj, pairs):
            text = json.dumps(obj)
            for before, after in pairs.items(): text = text.replace(before, after)
            return json.loads(text)
        def loot(name, obj):
            obj["random_sequence"] = f"{NS}:blocks/{name}"
            write(f"data/{NS}/loot_table/blocks/{name}.json", obj)

        simple = lambda name: {"type":"minecraft:block","pools":[{"conditions":[{"condition":"minecraft:survives_explosion"}],"entries":[{"type":"minecraft:item","name":f"{NS}:{name}"}],"rolls":1.0}]}
        tall_names = ["barley", "mycotoxic_daisy", "cattail", "tassel", "meadow_sage"]
        for name in tall_names:
            loot(name, replaced(vanilla("data/minecraft/loot_table/blocks/sunflower.json"), {"minecraft:sunflower": f"{NS}:{name}", "minecraft:blocks/sunflower": f"{NS}:blocks/{name}"}))
        for name in ["hyssop", "blue_lupine", "pink_lupine", "purple_lupine", "red_lupine", "yellow_lupine", "duckweed"]:
            loot(name, simple(name))
        loot("stone_bud", replaced(vanilla("data/minecraft/loot_table/blocks/short_grass.json"), {"minecraft:short_grass": f"{NS}:stone_bud", "minecraft:blocks/short_grass": f"{NS}:blocks/stone_bud"}))
        loot("windswept_grass", replaced(vanilla("data/minecraft/loot_table/blocks/tall_grass.json"), {"minecraft:tall_grass": f"{NS}:windswept_grass", "minecraft:short_grass": f"{NS}:windswept_grass", "minecraft:blocks/tall_grass": f"{NS}:blocks/windswept_grass"}))
        loot("dropleaf", replaced(vanilla("data/minecraft/loot_table/blocks/weeping_vines.json"), {"minecraft:weeping_vines": f"{NS}:dropleaf", "minecraft:blocks/weeping_vines": f"{NS}:blocks/dropleaf"}))
        loot("dropleaf_plant", {"type":"minecraft:block", "pools":[]})
        clover = {"type":"minecraft:block","pools":[{"entries":[{"type":"minecraft:item","name":f"{NS}:clover","functions":[*[
            {"function":"minecraft:set_count","count":i,"conditions":[{"condition":"minecraft:block_state_property","block":f"{NS}:clover","properties":{"flower_amount":str(i)}}]} for i in range(1,5)
        ],{"function":"minecraft:explosion_decay"}]}],"rolls":1.0}]}
        loot("clover", clover)
        pot_loot = vanilla("data/minecraft/loot_table/blocks/flower_pot.json")
        for name in COMPANIONS:
            if name.startswith("potted_"): loot(name, json.loads(json.dumps(pot_loot)))

        recipes = {
            "blue_dye_from_blue_lupine": ("blue_lupine", "blue_dye"),
            "pink_dye_from_pink_lupine": ("pink_lupine", "pink_dye"),
            "purple_dye_from_purple_lupine": ("purple_lupine", "purple_dye"),
            "red_dye_from_red_lupine": ("red_lupine", "red_dye"),
            "yellow_dye_from_yellow_lupine": ("yellow_lupine", "yellow_dye"),
            "purple_dye_from_hyssop": ("hyssop", "purple_dye"),
            "brown_dye_from_cattail": ("cattail", "brown_dye"),
            "light_gray_dye_from_tassel": ("tassel", "light_gray_dye"),
            "blue_dye_from_meadow_sage": ("meadow_sage", "blue_dye")
        }
        for name, (ingredient, result) in recipes.items():
            write(f"data/{NS}/recipe/{name}.json", {"type":"minecraft:crafting_shapeless","group":result,"ingredients":[f"{NS}:{ingredient}"],"result":{"id":f"minecraft:{result}"}})
        for kind, time in [("smelting",200),("smoking",100)]:
            name=f"barley_{kind}"
            write(f"data/{NS}/recipe/{name}.json", {"type":f"minecraft:{kind}","category":"food","cookingtime":time,"experience":.35,"ingredient":f"{NS}:barley","result":{"id":"minecraft:bread"}})
        all_recipes = {**recipes, "barley_smelting":("barley", "bread"), "barley_smoking":("barley", "bread")}
        for name, (ingredient, _) in all_recipes.items():
            write(f"data/{NS}/advancement/recipes/{name}.json", {"parent":"minecraft:recipes/root","criteria":{"has_material":{"trigger":"minecraft:inventory_changed","conditions":{"items":[{"items":f"{NS}:{ingredient}"}]}},"has_recipe":{"trigger":"minecraft:recipe_unlocked","conditions":{"recipe":f"{NS}:{name}"}}},"requirements":[["has_material","has_recipe"]],"rewards":{"recipes":[f"{NS}:{name}"]}})

    def tag(namespace, kind, name, values): write(f"data/{namespace}/tags/{kind}/{name}.json", {"replace":False,"values":values})
    stone = ["#minecraft:terracotta", "#minecraft:base_stone_overworld", "#minecraft:base_stone_nether",
             {"id":"#c:ores","required":False}, "minecraft:calcite", "minecraft:dripstone_block", "minecraft:gravel",
             {"id":"#c:gravel","required":False}, {"id":"#c:stone","required":False}, "minecraft:gilded_blackstone"]
    tag(NS, "block", "stone_bud_supports", stone)
    tag(NS, "block", "cattail_supports", ["#minecraft:dirt", "#minecraft:sand", "minecraft:clay", "minecraft:gravel"])
    small = [f"{NS}:{n}" for n in ["hyssop","blue_lupine","pink_lupine","purple_lupine","red_lupine","yellow_lupine"]]
    flowers = small + [f"{NS}:{n}" for n in ["mycotoxic_daisy","tassel","meadow_sage"]]
    tag("minecraft", "block", "small_flowers", small)
    tag("minecraft", "item", "small_flowers", small)
    tag("minecraft", "item", "flowers", flowers)
    tag("minecraft", "block", "bee_attractive", flowers)
    tag("minecraft", "item", "bee_food", flowers)
    tag("minecraft", "block", "climbable", [f"{NS}:dropleaf", f"{NS}:dropleaf_plant"])
    tag(NS, "block", "wind_short_foliage", [f"{NS}:{n}" for n in ["stone_bud","clover","hyssop","blue_lupine","pink_lupine","purple_lupine","red_lupine","yellow_lupine"]])
    tag(NS, "block", "wind_tall_foliage", [f"{NS}:{n}" for n in ["barley","windswept_grass","mycotoxic_daisy","cattail","tassel","meadow_sage"]])
    tag(NS, "block", "wind_hanging_foliage", [f"{NS}:dropleaf", f"{NS}:dropleaf_plant"])
    tag(NS, "block", "wind_floating_foliage", [f"{NS}:duckweed"])
    print(f"Generated {len(ALL_BLOCKS)} focused blocks from verified private RU input; no worldgen or foreign namespace output.")

if __name__ == "__main__": main()
