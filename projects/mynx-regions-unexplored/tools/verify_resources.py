"""Focused static/package checks. These are deliberately not Minecraft runtime evidence."""
import hashlib, json, pathlib, subprocess, unittest, zipfile

ROOT = pathlib.Path(__file__).resolve().parents[1]
OUT = ROOT / "build/generated/resources"
NS = "mynx_regions_unexplored"
INPUTS = json.loads((ROOT / "build-inputs.json").read_text(encoding="utf-8"))
COMMON = pathlib.Path(subprocess.check_output(["git", "rev-parse", "--path-format=absolute", "--git-common-dir"], text=True).strip()).parent
RU = COMMON / INPUTS["reference"]["path"]
OBTAINABLE = INPUTS["scope"]["obtainable"]
COMPANIONS = INPUTS["scope"]["companions"]

class Contract(unittest.TestCase):
    def read(self, path): return json.loads((OUT / path).read_text(encoding="utf-8"))

    def test_exact_identity_and_private_asset_bytes(self):
        self.assertEqual(INPUTS["reference"]["size"], RU.stat().st_size)
        self.assertEqual(INPUTS["reference"]["sha256"], hashlib.sha256(RU.read_bytes()).hexdigest())
        with zipfile.ZipFile(RU) as source:
            for target in (OUT / f"assets/{NS}/textures").rglob("*.png"):
                relative = target.relative_to(OUT / f"assets/{NS}/textures").as_posix()
                self.assertEqual(source.read("assets/regions_unexplored/textures/" + relative), target.read_bytes(), relative)

    def test_exact_registry_and_item_surface(self):
        self.assertEqual(16, len(OBTAINABLE))
        self.assertEqual(8, len(COMPANIONS))
        self.assertEqual(set(OBTAINABLE), {p.stem for p in (OUT / f"assets/{NS}/items").glob("*.json")})
        self.assertEqual(set(OBTAINABLE + COMPANIONS), {p.stem for p in (OUT / f"assets/{NS}/blockstates").glob("*.json")})
        source = (ROOT / "src/main/java/dev/resivore/mynxregions/MynxRegionsUnexplored.java").read_text()
        for name in OBTAINABLE + COMPANIONS: self.assertIn(f'"{name}"', source)
        for forbidden in INPUTS["scope"]["explicit_exclusions"][:4]: self.assertNotIn(f'"{forbidden}"', source)
        self.assertIn('registerBlockNoItem("dropleaf_plant"', source)
        self.assertEqual(6, source.count('pot("potted_'))
        self.assertIn('pottedMycotoxicDaisy("potted_mycotoxic_daisy", MYCOTOXIC_DAISY, 14)', source)

    def test_display_only_glowleaf_rename_is_generated(self):
        language = self.read(f"assets/{NS}/lang/en_us.json")
        self.assertEqual("Glowleaf", language[f"block.{NS}.dropleaf"])
        self.assertEqual("Glowleaf Plant", language[f"block.{NS}.dropleaf_plant"])
        self.assertNotIn(f"block.{NS}.glowleaf", language)
        generator = (ROOT / "tools/generate_resources.py").read_text(encoding="utf-8")
        self.assertIn('translations[f"block.{NS}.dropleaf"] = "Glowleaf"', generator)
        self.assertIn('translations[f"block.{NS}.dropleaf_plant"] = "Glowleaf Plant"', generator)

    def test_requested_and_preserved_light_levels(self):
        source = (ROOT / "src/main/java/dev/resivore/mynxregions/MynxRegionsUnexplored.java").read_text(encoding="utf-8")
        self.assertIn('.sound(SoundType.ROOTS).lightLevel(state -> 14));', source)
        self.assertIn('pottedMycotoxicDaisy("potted_mycotoxic_daisy", MYCOTOXIC_DAISY, 14)', source)
        self.assertIn('return head ? p.randomTicks().lightLevel(state -> 14) : p;', source)
        self.assertIn('registerBlockNoItem("dropleaf_plant", DropleafPlantBlock::new, dropleafProperties(false))', source)

    def test_mycotoxic_daisy_particles_are_client_ambient_and_limited(self):
        tall = (ROOT / "src/main/java/dev/resivore/mynxregions/MycotoxicDaisyBlock.java").read_text(encoding="utf-8")
        pot = (ROOT / "src/main/java/dev/resivore/mynxregions/MycotoxicDaisyPotBlock.java").read_text(encoding="utf-8")
        particles = (ROOT / "src/main/java/dev/resivore/mynxregions/MycotoxicDaisyParticles.java").read_text(encoding="utf-8")
        self.assertIn("void animateTick(", tall)
        self.assertIn("state.getValue(HALF) == DoubleBlockHalf.UPPER", tall)
        self.assertEqual(1, tall.count("MycotoxicDaisyParticles.trySpawn("))
        self.assertIn("extends FlowerPotBlock", pot)
        self.assertEqual(1, pot.count("MycotoxicDaisyParticles.trySpawn("))
        self.assertIn("private static final int SPAWN_CHANCE = 4;", particles)
        self.assertIn("level.addParticle(ParticleTypes.END_ROD", particles)
        self.assertNotIn("addAlwaysVisibleParticle", particles)
        for source in (tall, pot, particles):
            self.assertNotIn("randomTick(", source)
            self.assertNotIn("ServerLevel", source)
            self.assertNotIn("Minecraft.getInstance", source)
            self.assertNotIn("ParticleStatus", source)

    def test_resource_references_are_closed(self):
        def walk(value):
            if isinstance(value, dict):
                for key, child in value.items():
                    if key in {"model", "parent"} and isinstance(child, str) and child.startswith(NS + ":"):
                        rel = child.split(":", 1)[1]
                        self.assertTrue((OUT / f"assets/{NS}/models/{rel}.json").exists(), child)
                    if key == "textures" and isinstance(child, dict):
                        for texture in child.values():
                            if isinstance(texture, str) and texture.startswith(NS + ":"):
                                rel = texture.split(":", 1)[1]
                                self.assertTrue((OUT / f"assets/{NS}/textures/{rel}.png").exists(), texture)
                    walk(child)
            elif isinstance(value, list):
                for child in value: walk(child)
        for path in (OUT / f"assets/{NS}").rglob("*.json"): walk(json.loads(path.read_text()))

    def test_clover_preserves_both_grass_tint_indices(self):
        client = (ROOT / "src/client/java/dev/resivore/mynxregions/MynxRegionsUnexploredClient.java").read_text(encoding="utf-8")
        self.assertIn("List.of(BlockTintSources.grass(), BlockTintSources.grass())", client)
        self.assertIn("MynxRegionsUnexplored.CLOVER", client)
        with zipfile.ZipFile(RU) as source:
            for number in range(1, 5):
                model = json.loads(source.read(f"assets/regions_unexplored/models/block/clover_{number}.json"))
                stem_faces = [face for element in model["elements"] for face in element["faces"].values() if face["texture"] == "#stem"]
                self.assertTrue(stem_faces)
                self.assertEqual({1}, {face["tintindex"] for face in stem_faces})

    def test_loot_and_growth_contracts(self):
        stone = self.read(f"data/{NS}/loot_table/blocks/stone_bud.json")
        text = json.dumps(stone)
        self.assertIn("minecraft:shears", text); self.assertIn("minecraft:wheat_seeds", text)
        self.assertIn("minecraft:fortune", text); self.assertIn("0.125", text)
        wind = self.read(f"data/{NS}/loot_table/blocks/windswept_grass.json")
        self.assertEqual(2, len(wind["pools"])); self.assertEqual(2, json.dumps(wind).count('"count": 2.0'))
        clover = self.read(f"data/{NS}/loot_table/blocks/clover.json")
        self.assertEqual([1,2,3,4], [f["count"] for f in clover["pools"][0]["entries"][0]["functions"] if "count" in f])
        self.assertEqual([], self.read(f"data/{NS}/loot_table/blocks/dropleaf_plant.json")["pools"])
        for potted in [n for n in COMPANIONS if n.startswith("potted_")]:
            pot = self.read(f"data/{NS}/loot_table/blocks/{potted}.json")
            item_names = [entry["name"] for pool in pot["pools"] for entry in pool["entries"]]
            self.assertEqual(["minecraft:flower_pot"], item_names)

    def test_recipes_and_no_first_copy_recipe(self):
        recipes = list((OUT / f"data/{NS}/recipe").glob("*.json"))
        self.assertEqual(11, len(recipes)); self.assertEqual(11, len(list((OUT / f"data/{NS}/advancement/recipes").glob("*.json"))))
        for path in recipes:
            recipe = json.loads(path.read_text()); ingredient = recipe["ingredient"] if "ingredient" in recipe else recipe["ingredients"][0]
            self.assertTrue(ingredient.startswith(NS + ":"), path.name)
        for kind, time in [("smelting",200),("smoking",100)]:
            recipe=self.read(f"data/{NS}/recipe/barley_{kind}.json")
            self.assertEqual(time, recipe["cookingtime"]); self.assertEqual(.35, recipe["experience"]); self.assertEqual({"id":"minecraft:bread"}, recipe["result"])

    def test_compost_flower_and_support_tags(self):
        source=(ROOT / "src/main/java/dev/resivore/mynxregions/MynxRegionsUnexplored.java").read_text()
        self.assertIn("COMPOST_CHANCES.forEach", source)
        self.assertIn("block != MYCOTOXIC_DAISY", source)
        self.assertIn("head ? Blocks.WEEPING_VINES : Blocks.WEEPING_VINES_PLANT", source)
        for chance in [".15F", ".6F", ".5F", ".2F", ".3F", ".4F"]: self.assertIn(chance, source)
        bees=set(self.read("data/minecraft/tags/block/bee_attractive.json")["values"])
        self.assertEqual({f"{NS}:{n}" for n in ["hyssop","blue_lupine","pink_lupine","purple_lupine","red_lupine","yellow_lupine","mycotoxic_daisy","tassel","meadow_sage"]}, bees)
        small=set(self.read("data/minecraft/tags/block/small_flowers.json")["values"])
        self.assertNotIn(f"{NS}:mycotoxic_daisy", small); self.assertNotIn(f"{NS}:tassel", small); self.assertNotIn(f"{NS}:stone_bud", small)
        supports=json.dumps(self.read(f"data/{NS}/tags/block/stone_bud_supports.json"))
        self.assertNotIn("dirt", supports); self.assertNotIn("farmland", supports); self.assertNotIn("regions_unexplored", supports)

    def test_no_worldgen_trades_or_optional_serializer(self):
        data = OUT / f"data/{NS}"
        self.assertFalse((data / "worldgen").exists())
        self.assertFalse((OUT / "data/create").exists())
        for path in OUT.rglob("*.json"):
            text=path.read_text(); self.assertNotIn('"regions_unexplored:', text); self.assertNotIn("grass_sprouts", text)
            self.assertNotIn("trade", path.as_posix().lower())

    def test_shader_mapping_categories_and_pot_exclusion(self):
        source=(ROOT / "src/client/java/dev/resivore/mynxregions/mixin/IrisPlantMaterialMixin.java").read_text()
        for counterpart in ["SHORT_GRASS", "TALL_GRASS", "LILY_PAD", "WEEPING_VINES", "WEEPING_VINES_PLANT"]: self.assertIn(counterpart, source)
        self.assertNotIn("10005", source); self.assertNotIn("10489", source); self.assertNotIn("potted", source.lower())
        self.assertIn('@At("RETURN")', source)

    def test_final_archive_is_focused_and_exact(self):
        jars=list((ROOT / "build/libs").glob("mynx-regions-unexplored-private-*.jar")); self.assertEqual(1, len(jars))
        with zipfile.ZipFile(jars[0]) as jar:
            names=jar.namelist()
            for path in OUT.rglob("*"):
                if path.is_file(): self.assertEqual(path.read_bytes(), jar.read(path.relative_to(OUT).as_posix()))
            particle_classes = [
                "dev/resivore/mynxregions/MycotoxicDaisyBlock.class",
                "dev/resivore/mynxregions/MycotoxicDaisyParticles.class",
                "dev/resivore/mynxregions/MycotoxicDaisyPotBlock.class",
            ]
            for name in particle_classes: self.assertIn(name, names)
            particle_bytecode = b"".join(jar.read(name) for name in particle_classes)
            self.assertIn(b"animateTick", particle_bytecode)
            self.assertIn(b"END_ROD", particle_bytecode)
            for forbidden in [b"ServerLevel", b"net/minecraft/network", b"net/minecraft/client/Minecraft", b"ParticleStatus", b"addAlwaysVisibleParticle"]:
                self.assertNotIn(forbidden, particle_bytecode)
            self.assertFalse(any(n.endswith(".jar") for n in names))
            self.assertFalse(any(n.startswith(("assets/regions_unexplored/", "io/github/uhq_games/", "data/create/")) for n in names))

if __name__ == "__main__": unittest.main(verbosity=2)
