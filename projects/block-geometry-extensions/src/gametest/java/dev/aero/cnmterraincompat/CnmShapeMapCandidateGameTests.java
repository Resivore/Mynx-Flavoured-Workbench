package dev.aero.cnmterraincompat;

import dev.tazer.clutternomore.common.shape_map.ShapeMap;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfiles;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

/** Focused C80 contract tests for real CNM admission followed by CNM-owned ShapeMap resolution. */
public final class CnmShapeMapCandidateGameTests implements CustomTestMethodInvoker {
    private static final Identifier TEST_SOURCE = Identifier.fromNamespaceAndPath(
            CnmTerrainCompat.MOD_ID, "c80_mapping_fixture");

    @GameTest(maxTicks = 40)
    public void cnmAdmissionBindsOneResolvedTypedFamilyWithoutDuplicateLocalRoles(GameTestHelper helper) {
        CnmShapeMapCandidateBridge.Snapshot candidate = CnmShapeMapCandidateBridge.snapshots().stream()
                .filter(snapshot -> snapshot.phase() == CnmShapeMapCandidateBridge.Phase.BOUND)
                .filter(snapshot -> !snapshot.cnmRoles().isEmpty())
                .findFirst().orElseThrow(() -> new IllegalStateException(
                        "No real CNM-admitted BGE candidate reached authoritative ShapeMap binding"));

        Block anchor = BuiltInRegistries.BLOCK.getValue(candidate.anchor());
        NibaruMaterialProfile profile = NibaruMaterialProfiles.fromBlock(anchor).orElseThrow();
        Item canonical = profile.canonicalParent().asItem();
        List<Item> component = ShapeMap.getShapes(anchor.asItem());

        helper.assertTrue(candidate.canonicalBindingDeferred() && candidate.resolvedParent().isPresent()
                        && candidate.resolvedParent().orElseThrow().equals(BuiltInRegistries.ITEM.getKey(canonical)),
                "C80 claimed a canonical parent before CNM resolved it for " + candidate.anchor());
        helper.assertTrue(candidate.cnmRoles().entrySet().stream().allMatch(entry -> {
                    Block generated = BuiltInRegistries.BLOCK.getValue(entry.getValue());
                    return entry.getValue().equals(BuiltInRegistries.BLOCK.getKey(generated));
                }), "C80 did not retain CNM's exact generated role identities for " + candidate.anchor());
        helper.assertTrue(candidate.injectedRoles().size() == 3
                        && candidate.injectedRoles().stream().allMatch(id ->
                                java.util.Collections.frequency(component,
                                        BuiltInRegistries.BLOCK.getValue(id).asItem()) == 1),
                "C80 local roles were missing or duplicated in CNM's resolved family: " + candidate);

        Map<BgeGeometryRole, Block> local = Map.of(
                BgeGeometryRole.LAYER, NibaruProviderAdapter.derived(profile, BgeGeometryRole.LAYER).orElseThrow(),
                BgeGeometryRole.CORNER, NibaruProviderAdapter.derived(profile, BgeGeometryRole.CORNER).orElseThrow(),
                BgeGeometryRole.QUARTER_COLUMN,
                NibaruProviderAdapter.derived(profile, BgeGeometryRole.QUARTER_COLUMN).orElseThrow());
        for (Map.Entry<BgeGeometryRole, Block> role : local.entrySet()) {
            helper.assertTrue(java.util.Collections.frequency(component, role.getValue().asItem()) == 1
                            && BgeMaterialBindings.fromBlock(role.getValue()).orElseThrow().canonicalMaterial()
                                    == profile.canonicalParent(),
                    "C80 local role did not bind to CNM's exact canonical family: " + role);
        }
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

    @Override
    public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
        method.invoke(this, helper);
    }
}
