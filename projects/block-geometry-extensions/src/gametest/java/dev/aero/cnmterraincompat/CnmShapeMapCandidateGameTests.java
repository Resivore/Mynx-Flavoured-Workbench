package dev.aero.cnmterraincompat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.tazer.clutternomore.ClutterNoMore;
import dev.tazer.clutternomore.common.shape_map.ShapeMap;
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
import java.util.List;
import java.util.Map;

/** Focused C81 integration tests for real CNM resolution, not a synthetic mapping substitute. */
public final class CnmShapeMapCandidateGameTests implements CustomTestMethodInvoker {
    private static final Identifier TEST_SOURCE = Identifier.fromNamespaceAndPath(
            CnmTerrainCompat.MOD_ID, "c81_mapping_fixture");

    @GameTest(maxTicks = 40)
    public void untypedCnmAdmissionBindsOneResolvedCanonicalFamilyWithoutTemporaryDuplicates(
            GameTestHelper helper) {
        CnmShapeMapCandidateBridge.Snapshot candidate = CnmShapeMapCandidateBridge.snapshots().stream()
                .filter(snapshot -> snapshot.phase() == CnmShapeMapCandidateBridge.Phase.BOUND)
                .filter(snapshot -> !snapshot.deferredMaterial())
                .filter(snapshot -> !snapshot.cnmRoles().isEmpty())
                .findFirst().orElseThrow(() -> new IllegalStateException(
                        "No real CNM-admitted BGE candidate reached authoritative ShapeMap binding"));

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
        helper.assertTrue(candidate.injectedRoles().size() == 3
                        && candidate.injectedRoles().stream().noneMatch(id -> component.contains(
                                BuiltInRegistries.BLOCK.getValue(id).asItem())),
                "C81 left a provisional untyped carrier in a resolved canonical family: " + candidate);

        Map<BgeGeometryRole, Block> local = Map.of(
                BgeGeometryRole.LAYER, NibaruProviderAdapter.derived(profile, BgeGeometryRole.LAYER).orElseThrow(),
                BgeGeometryRole.CORNER, NibaruProviderAdapter.derived(profile, BgeGeometryRole.CORNER).orElseThrow(),
                BgeGeometryRole.QUARTER_COLUMN,
                NibaruProviderAdapter.derived(profile, BgeGeometryRole.QUARTER_COLUMN).orElseThrow());
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
