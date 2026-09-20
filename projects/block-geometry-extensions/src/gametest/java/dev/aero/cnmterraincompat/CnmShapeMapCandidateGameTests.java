package dev.aero.cnmterraincompat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.tazer.clutternomore.ClutterNoMore;
import dev.tazer.clutternomore.common.shape_map.ShapeMap;
import dev.aero.cnmterraincompat.client.ResolvedCnmCandidateResources;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfiles;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.GameType;

import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Focused C81 integration tests for real CNM resolution, not a synthetic mapping substitute. */
public final class CnmShapeMapCandidateGameTests implements CustomTestMethodInvoker {
    private static final Identifier TEST_SOURCE = Identifier.fromNamespaceAndPath(
            CnmTerrainCompat.MOD_ID, "c81_mapping_fixture");
    private static final Identifier MOSS_SLAB_CNM_ADMISSION = Identifier.fromNamespaceAndPath(
            "more_slabs_stairs_and_walls", "moss_block_slab");

    @GameTest(maxTicks = 40)
    public void untypedCnmAdmissionBindsOneResolvedCanonicalFamilyWithoutTemporaryDuplicates(
            GameTestHelper helper) {
        CnmShapeMapCandidateBridge.Snapshot candidate = CnmShapeMapCandidateBridge.snapshots().stream()
                .filter(snapshot -> snapshot.phase() == CnmShapeMapCandidateBridge.Phase.BOUND)
                .filter(snapshot -> !snapshot.deferredMaterial())
                .filter(snapshot -> snapshot.anchor().equals(MOSS_SLAB_CNM_ADMISSION))
                .filter(snapshot -> !snapshot.cnmRoles().isEmpty())
                .findFirst().orElseThrow(() -> new IllegalStateException(
                        "Moss CNM admission did not reach authoritative ShapeMap binding"));

        Item canonical = BuiltInRegistries.ITEM.getValue(candidate.resolvedParent().orElseThrow());
        NibaruMaterialProfile profile = NibaruMaterialProfiles.fromBlock(Block.byItem(canonical)).orElseThrow();
        List<Item> component = ShapeMap.getShapes(canonical);

        helper.assertTrue(candidate.canonicalBindingDeferred() && candidate.resolvedParent().isPresent()
                        && candidate.resolvedParent().orElseThrow().equals(BuiltInRegistries.ITEM.getKey(canonical)),
                "C81 claimed a canonical parent before CNM resolved it for " + candidate.anchor());
        helper.assertTrue(candidate.cnmRoles().entrySet().stream().allMatch(entry -> {
                    Block generated = BuiltInRegistries.BLOCK.getValue(entry.getValue());
                    return entry.getValue().equals(BuiltInRegistries.BLOCK.getKey(generated));
                }), "C81 did not retain CNM's exact generated role identities for " + candidate.anchor());
        Map<BgeGeometryRole, Block> local = Map.of(
                BgeGeometryRole.LAYER, NibaruProviderAdapter.derived(profile, BgeGeometryRole.LAYER).orElseThrow(),
                BgeGeometryRole.CORNER, NibaruProviderAdapter.derived(profile, BgeGeometryRole.CORNER).orElseThrow(),
                BgeGeometryRole.QUARTER_COLUMN,
                NibaruProviderAdapter.derived(profile, BgeGeometryRole.QUARTER_COLUMN).orElseThrow());
        helper.assertTrue(candidate.injectedRoles().size() == 3
                        && candidate.injectedRoles().equals(local.values().stream()
                                .map(BuiltInRegistries.BLOCK::getKey).collect(java.util.stream.Collectors.toSet())),
                "C87 diagnostics did not describe this resolved mapping pass: " + candidate);
        for (Map.Entry<BgeGeometryRole, Block> role : local.entrySet()) {
            helper.assertTrue(java.util.Collections.frequency(component, role.getValue().asItem()) == 1
                            && BgeMaterialBindings.fromBlock(role.getValue()).orElseThrow().canonicalMaterial()
                                    == profile.canonicalParent(),
                    "C81 local role did not bind to CNM's exact canonical family: " + role);
        }
        assertResolvedCandidateResourcesAndDrops(helper, local, profile.canonicalParent());
        helper.succeed();
    }

    /**
     * This is the user-visible C80 regression: stock CNM resolves Moss, and the query consumers
     * actually use must return exactly the nine normal family roles. C80 returned only six.
     */
    @GameTest(maxTicks = 40)
    public void realCnmMossFamilyHasExactlyOneOfAllNineRoles(GameTestHelper helper) {
        assertNineRoleFamily(helper, Blocks.MOSS_BLOCK, "Moss Block");
        helper.succeed();
    }

    /** Covers a normal brick family, an axis log family, and a non-Minecraft provider fixture. */
    @GameTest(maxTicks = 40)
    public void resolvedFamiliesShareTheOneCompletionRule(GameTestHelper helper) {
        assertNineRoleFamily(helper, Blocks.STONE_BRICKS, "Stone Bricks");
        assertNineRoleFamily(helper, Blocks.OAK_LOG, "Oak Log");
        Block provider = BuiltInRegistries.BLOCK.getValue(Identifier.parse("ribbits:mossy_oak_planks"));
        helper.assertTrue(provider != Blocks.AIR, "Missing real non-Minecraft CNM/provider fixture");
        assertNineRoleFamily(helper, provider, "Ribbits Mossy Oak Planks");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void mappingPresenceIsExplicitAndAbsentOrRemovedAnchorsFailClosed(GameTestHelper helper) {
        List<ShapeMap.Mapping> present = List.of(new ShapeMap.Mapping(Items.STONE, Items.STONE_SLAB,
                100, TEST_SOURCE));
        List<ShapeMap.Mapping> absentAfterRemoval = List.of();
        helper.assertTrue(CnmShapeMapCandidateBridge.mappingMentions(present, Items.STONE_SLAB),
                "Actual Mapping object containing an admitted anchor was not detected");
        helper.assertTrue(!CnmShapeMapCandidateBridge.mappingMentions(absentAfterRemoval, Items.STONE_SLAB),
                "Absent/remove-filtered candidate anchor would be injected into a ShapeMap family");
        helper.succeed();
    }

    /** C87 generic Wall fixture: slab/stair admission has no Wall until BGE closes the component. */
    @GameTest(maxTicks = 40)
    public void genericSlabStairComponentWithoutWallGetsOneNormalWall(GameTestHelper helper) {
        List<ShapeMap.Mapping> slabAndStairs = List.of(
                new ShapeMap.Mapping(Items.STONE, Items.STONE_SLAB, 100, TEST_SOURCE),
                new ShapeMap.Mapping(Items.STONE, Items.STONE_STAIRS, 100, TEST_SOURCE));
        helper.assertTrue(!CnmShapeMapCandidateBridge.componentHasWall(slabAndStairs, Items.STONE),
                "Generic slab/stair fixture unexpectedly supplied a provider Wall");
        Block provider = BuiltInRegistries.BLOCK.getValue(Identifier.parse("ribbits:mossy_oak_planks"));
        List<Item> resolved = ShapeMap.getShapes(provider.asItem());
        long walls = resolved.stream().filter(item -> Block.byItem(item)
                instanceof net.minecraft.world.level.block.WallBlock).count();
        helper.assertTrue(walls == 1 && resolved.stream().filter(item -> Block.byItem(item)
                        instanceof net.minecraft.world.level.block.WallBlock).allMatch(item ->
                        CnmTerrainCompat.MOD_ID.equals(BuiltInRegistries.ITEM.getKey(item).getNamespace())),
                "Generic CNM slab/stair family did not resolve exactly one BGE-owned normal Wall: "
                        + resolved.stream().map(BuiltInRegistries.ITEM::getKey).toList());
        helper.succeed();
    }

    /**
     * C87 regression: CNM gives setMappings a new list on reload/reconstruction.  Preserve the
     * live graph, execute two independent fresh-list passes, and prove the second pass receives
     * the same BGE closure rather than stale roles retained from the first graph.
     */
    @GameTest(maxTicks = 80)
    public void twoFreshShapeMapBuildsRetainEveryAutomaticBgeTail(GameTestHelper helper) {
        Map<Item, List<Item>> savedShapes = new LinkedHashMap<>();
        ShapeMap.shapesView().forEach((parent, component) -> savedShapes.put(parent, new ArrayList<>(component)));
        Map<Item, Item> savedInverse = new LinkedHashMap<>(ShapeMap.inverseView());
        try {
            List<ShapeMap.Mapping> fresh = freshMappings(savedShapes);
            for (int pass = 1; pass <= 2; pass++) {
                ShapeMap.setMappings(new ArrayList<>(fresh), false);
                assertEveryResolvedTail(helper, "fresh ShapeMap pass " + pass);
            }
        } finally {
            ShapeMap.setShapeMaps(savedShapes, savedInverse);
        }
        helper.succeed();
    }

    /** First-bake resources use a real admitted visual source and are byte-stable on regeneration. */
    @GameTest(maxTicks = 80)
    public void provisionalGenericResourcesExistBeforeAndAfterResolvedGeneration(GameTestHelper helper) {
        var manager = ExternalMaterialFamilyGameTests.clientFixtureManagerForCnmRegression();
        var visualSources = CnmShapeMapCandidateBridge.provisionalGenericFamilies().stream()
                .map(CnmShapeMapCandidateBridge.ProvisionalGenericFamily::visualSource)
                .collect(java.util.stream.Collectors.toSet());
        var first = ResolvedCnmCandidateResources.generateProvisionalForValidation(manager,
                visualSources);
        helper.assertTrue(first.familyCount() == visualSources.size() && first.familyCount() > 0,
                "First-bake generic resource plan lost admitted CNM samples: " + first);
        Map<Identifier, String> before = genericResourceBytes();
        var second = ResolvedCnmCandidateResources.generateProvisionalForValidation(manager,
                visualSources);
        helper.assertTrue(second.equals(first) && genericResourceBytes().equals(before),
                "Repeated provisional CNM resource generation was not deterministic");
        helper.succeed();
    }

    /**
     * BBB's standard beam stairs are valid CNM visual sources, but their block ID is not a model
     * ID: the blockstate selects {@code block/beam/<material>_beam_stairs[_inner|_outer]}.
     * Exercise the identical first-bake writer for every beam material so this remains a
     * resource-structure rule rather than an Acacia exception.
     */
    @GameTest(maxTicks = 80)
    public void bbbBeamStairBlockstateModelsCloseGenericResourceGeneration(GameTestHelper helper) {
        var manager = ExternalMaterialFamilyGameTests.clientFixtureManagerForBbbBeamResourceRegression();
        Identifier acacia = Identifier.parse("bbb:acacia_beam_stairs");
        helper.assertTrue(manager.getResource(Identifier.parse("bbb:models/block/acacia_beam_stairs.json")).isEmpty()
                        && manager.getResource(Identifier.parse("bbb:blockstates/acacia_beam_stairs.json")).isPresent()
                        && manager.getResource(Identifier.parse(
                                "bbb:models/block/beam/acacia_beam_stairs_inner.json")).isPresent(),
                "BBB fixture no longer proves its blockstate-to-beam-model indirection for " + acacia);
        for (String material : BBB_BEAM_MATERIALS) {
            Identifier visualSource = Identifier.fromNamespaceAndPath("bbb", material + "_beam_stairs");
            Map<BgeGeometryRole, Identifier> roles = validationRoles(material);
            var first = ResolvedCnmCandidateResources.generateVisualSourceForValidation(manager,
                    visualSource, roles);
            helper.assertTrue(first.familyCount() == 1,
                    "First bake omitted valid BBB beam-stair visual source " + visualSource + ": " + first);
            Map<Identifier, String> bytes = validationResourceBytes(roles);
            var repeated = ResolvedCnmCandidateResources.generateVisualSourceForValidation(manager,
                    visualSource, roles);
            helper.assertTrue(repeated.equals(first) && validationResourceBytes(roles).equals(bytes),
                    "Repeated BBB beam-stair resource generation was not deterministic: " + visualSource);
            assertBbbBeamStairResourceClosure(helper, roles, material);
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void bgeGeneratedGeometryCannotRecursivelySeedAnotherCandidate(GameTestHelper helper) {
        NibaruMaterialProfile profile = NibaruMaterialProfiles.all().getFirst();
        Block generatedBgeRole = NibaruProviderAdapter.derived(profile, BgeGeometryRole.LAYER).orElseThrow();
        CnmShapeMapCandidateBridge.admit(generatedBgeRole, BgeGeometryRole.VERTICAL_SLAB,
                generatedBgeRole, BuiltInRegistries.BLOCK.getKey(generatedBgeRole));
        helper.assertTrue(CnmShapeMapCandidateBridge.rejectedRecursiveSource(generatedBgeRole),
                "A BGE-generated role became a new Phase-A candidate");
        helper.succeed();
    }

    private static void assertNineRoleFamily(GameTestHelper helper, Block source, String label) {
        NibaruMaterialProfile profile = NibaruMaterialProfiles.fromBlock(source).orElseThrow(() ->
                new IllegalStateException("Missing BGE material profile for " + label));
        List<Item> expected = List.of(
                profile.canonicalParent().asItem(),
                profile.effectiveSlabSource().orElseThrow().asItem(),
                profile.effectiveStairSource().orElseThrow().asItem(),
                profile.nativeWall().orElseThrow().asItem(),
                NibaruProviderAdapter.derived(profile, BgeGeometryRole.VERTICAL_SLAB).orElseThrow().asItem(),
                NibaruProviderAdapter.derived(profile, BgeGeometryRole.STEP).orElseThrow().asItem(),
                NibaruProviderAdapter.derived(profile, BgeGeometryRole.CORNER).orElseThrow().asItem(),
                NibaruProviderAdapter.derived(profile, BgeGeometryRole.QUARTER_COLUMN).orElseThrow().asItem(),
                NibaruProviderAdapter.derived(profile, BgeGeometryRole.LAYER).orElseThrow().asItem());
        List<Item> actual = ShapeMap.getShapes(source.asItem());
        helper.assertTrue(new LinkedHashSet<>(expected).size() == 9,
                label + " test fixture has duplicate expected role ownership: " + expected);
        boolean exactSingleVariantMenu = source == Blocks.MOSS_BLOCK;
        helper.assertTrue((!exactSingleVariantMenu || actual.size() == 9)
                        && actual.containsAll(expected)
                        && expected.stream().allMatch(item -> java.util.Collections.frequency(actual, item) == 1),
                label + " CNM menu is not one exact nine-role family: actual=" + actual
                        + ", expected=" + expected);
        System.out.println("REAL_CNM_FAMILY|material=" + BuiltInRegistries.BLOCK.getKey(source)
                + "|roles=" + actual.stream().map(BuiltInRegistries.ITEM::getKey).toList());
    }

    private static List<ShapeMap.Mapping> freshMappings(Map<Item, List<Item>> shapes) {
        List<ShapeMap.Mapping> result = new ArrayList<>();
        for (Map.Entry<Item, List<Item>> entry : shapes.entrySet()) {
            for (Item member : entry.getValue()) if (member != entry.getKey()) {
                result.add(new ShapeMap.Mapping(entry.getKey(), member, 100, TEST_SOURCE));
            }
        }
        return result;
    }

    private static void assertEveryResolvedTail(GameTestHelper helper, String pass) {
        for (CnmShapeMapCandidateBridge.Snapshot candidate : CnmShapeMapCandidateBridge.snapshots()) {
            if (candidate.phase() != CnmShapeMapCandidateBridge.Phase.BOUND) continue;
            Item parent = BuiltInRegistries.ITEM.getValue(candidate.resolvedParent().orElseThrow());
            List<Item> component = ShapeMap.getShapes(parent);
            NibaruMaterialProfile profile = NibaruMaterialProfiles.fromBlock(Block.byItem(parent)).orElse(null);
            if (profile != null) {
                for (BgeGeometryRole role : List.of(BgeGeometryRole.CORNER,
                        BgeGeometryRole.QUARTER_COLUMN, BgeGeometryRole.LAYER)) {
                    Item expected = NibaruProviderAdapter.derived(profile, role).orElseThrow().asItem();
                    helper.assertTrue(java.util.Collections.frequency(component, expected) == 1,
                            pass + " lost or duplicated " + role + " for " + candidate.resolvedParent());
                }
            }
        }
        for (CnmShapeMapCandidateBridge.ResolvedGenericFamily family
                : CnmShapeMapCandidateBridge.resolvedGenericFamilies()) {
            List<Item> component = ShapeMap.getShapes(BuiltInRegistries.ITEM.getValue(family.canonicalParent()));
            for (Identifier role : family.roles().values()) helper.assertTrue(
                    java.util.Collections.frequency(component, BuiltInRegistries.BLOCK.getValue(role).asItem()) == 1,
                    pass + " lost or duplicated generic BGE role " + role + " for " + family.canonicalParent());
            family.wall().ifPresent(wall -> helper.assertTrue(java.util.Collections.frequency(component,
                    BuiltInRegistries.BLOCK.getValue(wall).asItem()) == 1,
                    pass + " lost or duplicated generic BGE Wall " + wall + " for " + family.canonicalParent()));
        }
    }

    private static Map<Identifier, String> genericResourceBytes() {
        Map<Identifier, String> result = new LinkedHashMap<>();
        for (CnmShapeMapCandidateBridge.ProvisionalGenericFamily family
                : CnmShapeMapCandidateBridge.provisionalGenericFamilies()) {
            family.roles().values().forEach(id -> result.put(id, generated(
                    net.minecraft.server.packs.PackType.CLIENT_RESOURCES,
                    Identifier.fromNamespaceAndPath(id.getNamespace(), "blockstates/" + id.getPath() + ".json")).toString()));
            family.wall().ifPresent(id -> result.put(id, generated(net.minecraft.server.packs.PackType.CLIENT_RESOURCES,
                    Identifier.fromNamespaceAndPath(id.getNamespace(), "blockstates/" + id.getPath() + ".json")).toString()));
        }
        return result;
    }

    private static void assertBbbBeamStairResourceClosure(GameTestHelper helper,
            Map<BgeGeometryRole, Identifier> roles, String material) {
        for (Identifier role : roles.values()) {
            Identifier blockState = Identifier.fromNamespaceAndPath(role.getNamespace(),
                    "blockstates/" + role.getPath() + ".json");
            generated(net.minecraft.server.packs.PackType.CLIENT_RESOURCES, blockState);
        }
        Identifier layer = roles.get(BgeGeometryRole.LAYER);
        JsonObject model = generated(net.minecraft.server.packs.PackType.CLIENT_RESOURCES,
                Identifier.fromNamespaceAndPath(layer.getNamespace(), "models/block/" + layer.getPath()
                        + "_up_1.json"));
        JsonObject textures = model.getAsJsonObject("textures");
        String side = "bbb:block/beam/" + material;
        String end = side + "_top";
        helper.assertTrue(textures.get("side").getAsString().equals(side)
                        && textures.get("top").getAsString().equals(end)
                        && textures.get("bottom").getAsString().equals(end),
                "BBB beam-stair resource indirection did not retain the beam side/end texture contract: "
                        + material + " " + textures);
    }

    private static Map<BgeGeometryRole, Identifier> validationRoles(String material) {
        String prefix = "validation/bbb/" + material + "_beam_stairs_";
        return Map.of(BgeGeometryRole.LAYER, Identifier.fromNamespaceAndPath(CnmTerrainCompat.MOD_ID,
                        prefix + "layer"),
                BgeGeometryRole.CORNER, Identifier.fromNamespaceAndPath(CnmTerrainCompat.MOD_ID,
                        prefix + "corner"),
                BgeGeometryRole.QUARTER_COLUMN, Identifier.fromNamespaceAndPath(CnmTerrainCompat.MOD_ID,
                        prefix + "quarter_column"));
    }

    private static Map<Identifier, String> validationResourceBytes(Map<BgeGeometryRole, Identifier> roles) {
        Map<Identifier, String> result = new LinkedHashMap<>();
        for (Identifier role : roles.values()) {
            Identifier blockState = Identifier.fromNamespaceAndPath(role.getNamespace(),
                    "blockstates/" + role.getPath() + ".json");
            result.put(blockState, generated(net.minecraft.server.packs.PackType.CLIENT_RESOURCES, blockState)
                    .toString());
        }
        return result;
    }

    private static final List<String> BBB_BEAM_MATERIALS = List.of("oak", "spruce", "birch", "jungle",
            "acacia", "dark_oak", "crimson", "warped", "mangrove", "bamboo", "cherry", "pale_oak");

    /**
     * The fixture was untyped during registry admission. Once CNM selected its actual profile,
     * every surviving role must have a real server loot entry, client model closure, and the
     * one-source canonical economy—not merely a ShapeMap edge.
     */
    private static void assertResolvedCandidateResourcesAndDrops(GameTestHelper helper,
            Map<BgeGeometryRole, Block> roles, Block canonical) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        int index = 0;
        for (Map.Entry<BgeGeometryRole, Block> entry : roles.entrySet()) {
            Block block = entry.getValue();
            Identifier id = BuiltInRegistries.BLOCK.getKey(block);
            JsonObject loot = generated(net.minecraft.server.packs.PackType.SERVER_DATA, Identifier.fromNamespaceAndPath(
                    id.getNamespace(), "loot_table/blocks/" + id.getPath() + ".json"));
            helper.assertTrue(loot.getAsJsonArray("pools").get(0).getAsJsonObject()
                            .getAsJsonArray("entries").get(0).getAsJsonObject().get("name").getAsString()
                            .equals(BuiltInRegistries.BLOCK.getKey(canonical).toString()),
                    "Resolved untyped candidate has no canonical loot target: " + entry.getKey());
            BlockPos pos = new BlockPos(1 + index++, 2, 1);
            helper.setBlock(pos, block.defaultBlockState());
            var drops = Block.getDrops(block.defaultBlockState(), helper.getLevel(), helper.absolutePos(pos),
                    null, player, ItemStack.EMPTY);
            helper.assertTrue(drops.size() == 1 && drops.getFirst().is(canonical.asItem())
                            && drops.getFirst().getCount() == 1,
                    "Resolved untyped candidate did not return its canonical material: " + entry.getKey());
        }
    }

    private static JsonObject generated(net.minecraft.server.packs.PackType type, Identifier id) {
        try {
            var supplier = ClutterNoMore.RESOURCES.getResource(type, id);
            if (supplier == null) throw new IllegalStateException("Missing generated resource " + id);
            try (var input = supplier.get(); var reader = new InputStreamReader(input)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot inspect generated resource " + id, exception);
        }
    }


    @Override
    public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
        method.invoke(this, helper);
    }
}
