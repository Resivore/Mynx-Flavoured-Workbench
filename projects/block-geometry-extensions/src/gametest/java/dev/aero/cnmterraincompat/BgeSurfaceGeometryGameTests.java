package dev.aero.cnmterraincompat;

import dev.aero.cnmterraincompat.BgeMaterialBindings.Binding;
import dev.aero.cnmterraincompat.BgeSurfaceGeometry.PlaneRelation;
import dev.aero.cnmterraincompat.BgeSurfaceGeometry.SurfaceModel;
import dev.aero.cnmterraincompat.BgeSurfaceGeometry.SurfacePatch;
import dev.tazer.clutternomore.common.blocks.StepBlock;
import dev.tazer.clutternomore.common.blocks.VerticalSlabBlock;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfiles;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;

import java.lang.reflect.Method;
import java.util.List;

/** Controlled proofs for the BGE-owned planar-surface contract; not visual runtime evidence. */
public final class BgeSurfaceGeometryGameTests implements CustomTestMethodInvoker {
    @GameTest(maxTicks = 40)
    public void simpleGeometryExposesAllSixCorrespondingCanonicalFaces(GameTestHelper helper) {
        Binding full = BgeMaterialBindings.fromBlock(Blocks.STONE).orElseThrow();
        SurfaceModel fullModel = full.surfaceModel(Blocks.STONE.defaultBlockState());
        helper.assertTrue(fullModel.supported(), "Canonical full block has no surface model");
        for (Direction face : Direction.values()) {
            List<SurfacePatch> patches = fullModel.patches(face);
            helper.assertTrue(patches.size() == 1 && patches.getFirst().canonicalFace() == face
                            && patches.getFirst().planeRelation() == PlaneRelation.EXACT,
                    "Canonical face mapping changed for " + face + ": " + patches);
        }

        Block slab = profile(Blocks.STONE).effectiveSlabSource().orElseThrow();
        BlockState bottom = slab.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
        assertPatch(helper, model(bottom), Direction.UP, 8, 0, 16, 0, 16,
                PlaneRelation.EXACT, "bottom slab top");
        assertPatch(helper, model(bottom), Direction.NORTH, 0, 0, 16, 0, 8,
                PlaneRelation.EXACT, "bottom slab side");

        Block vertical = derived(Blocks.STONE, BgeGeometryRole.VERTICAL_SLAB);
        BlockState east = vertical.defaultBlockState()
                .setValue(VerticalSlabBlock.FACING, Direction.EAST)
                .setValue(VerticalSlabBlock.DOUBLE, false);
        assertPatch(helper, model(east), Direction.UP, 16, 8, 16, 0, 16,
                PlaneRelation.EXACT, "east Vertical Slab top");

        Block layer = derived(Blocks.STONE, BgeGeometryRole.LAYER);
        BlockState westLayer = layer.defaultBlockState()
                .setValue(BgeLayerBlock.FACING, Direction.WEST)
                .setValue(BgeLayerBlock.LAYERS, 2);
        assertPatch(helper, model(westLayer), Direction.UP, 16, 0, 8, 0, 16,
                PlaneRelation.EXACT, "west Layer top");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void compoundGeometryExportsDeterministicPatchCollections(GameTestHelper helper) {
        Block step = derived(Blocks.STONE, BgeGeometryRole.STEP);
        BlockState doubledStep = step.defaultBlockState()
                .setValue(StepBlock.FACING, Direction.NORTH)
                .setValue(StepBlock.SLAB_TYPE, SlabType.DOUBLE);
        SurfaceModel stepModel = model(doubledStep);
        helper.assertTrue(stepModel.supported() && stepModel.patches(Direction.UP).size() == 2,
                "Double Step did not expose two deterministic top regions: "
                        + stepModel.patches(Direction.UP));

        BgeCornerBlock corner = (BgeCornerBlock) derived(Blocks.STONE, BgeGeometryRole.CORNER);
        BlockState cornerState = corner.defaultBlockState()
                .setValue(BgeCornerBlock.FACING,
                        BgeCornerBlock.Orientation.SOUTH_WEST.stateFacing());
        SurfaceModel cornerModel = model(cornerState);
        helper.assertTrue(cornerModel.patches(Direction.UP).size() == 3,
                "Corner union was flattened instead of retaining three exposed quarter patches: "
                        + cornerModel.patches(Direction.UP));

        BgeColumnBlock column = (BgeColumnBlock) derived(
                Blocks.STONE, BgeGeometryRole.QUARTER_COLUMN);
        BlockState diagonal = column.defaultBlockState()
                .setValue(BgeColumnBlock.OCCUPANCY, BgeColumnBlock.Occupancy.NW_SE);
        List<SurfacePatch> first = model(diagonal).patches(Direction.UP);
        List<SurfacePatch> second = model(diagonal).patches(Direction.UP);
        helper.assertTrue(first.size() == 2 && first.equals(second),
                "Quarter Column surface order is not deterministic: " + first + " / " + second);
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void terrainInsetIsTypedOnlyOnIntentionallyLoweredUpSurfaces(GameTestHelper helper) {
        BlockState farmland = CnmTerrainCompat.FARMLAND_SLAB.defaultBlockState()
                .setValue(FarmlandSlabBlock.TYPE, SlabType.BOTTOM);
        assertPatch(helper, model(farmland), Direction.UP, 7, 0, 16, 0, 16,
                PlaneRelation.TERRAIN_HEIGHT_INSET, "Farmland bottom slab top");
        assertPatch(helper, model(farmland), Direction.NORTH, 0, 0, 16, 0, 7,
                PlaneRelation.EXACT, "Farmland bottom slab side");

        SurfaceModel farmlandRoot = model(Blocks.FARMLAND.defaultBlockState());
        assertPatch(helper, farmlandRoot, Direction.UP, 15, 0, 16, 0, 16,
                PlaneRelation.TERRAIN_HEIGHT_INSET, "Farmland root top");

        NibaruMaterialProfile path = profile(Blocks.DIRT_PATH);
        Block pathStepBlock = NibaruProviderAdapter.derived(path, BgeGeometryRole.STEP).orElseThrow();
        BlockState pathStep = pathStepBlock.defaultBlockState()
                .setValue(StepBlock.FACING, Direction.NORTH)
                .setValue(StepBlock.SLAB_TYPE, SlabType.BOTTOM);
        assertPatch(helper, model(pathStep), Direction.UP, 7, 0, 16, 0, 8,
                PlaneRelation.TERRAIN_HEIGHT_INSET, "Path Step top");

        Block pathLayerBlock = NibaruProviderAdapter.derived(path, BgeGeometryRole.LAYER).orElseThrow();
        BlockState partialPathLayer = pathLayerBlock.defaultBlockState()
                .setValue(BgeLayerBlock.FACING, Direction.UP)
                .setValue(BgeLayerBlock.LAYERS, 2);
        assertPatch(helper, model(partialPathLayer), Direction.UP, 8, 0, 16, 0, 16,
                PlaneRelation.EXACT, "non-lowered partial Path Layer top");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void contextualNativeTopologiesRemainExplicitlyFailClosed(GameTestHelper helper) {
        Binding stair = BgeMaterialBindings.fromBlock(
                profile(Blocks.STONE).effectiveStairSource().orElseThrow()).orElseThrow();
        Binding wall = BgeMaterialBindings.fromBlock(
                profile(Blocks.STONE).nativeWall().orElseThrow()).orElseThrow();
        helper.assertTrue(!stair.surfaceModel(stair.physicalBlock().defaultBlockState()).supported()
                        && stair.surfaceModel(stair.physicalBlock().defaultBlockState())
                                .limitation().orElseThrow().contains("contextual"),
                "Native Stair did not retain its explicit contextual limitation");
        helper.assertTrue(!wall.surfaceModel(wall.physicalBlock().defaultBlockState()).supported()
                        && wall.surfaceModel(wall.physicalBlock().defaultBlockState())
                                .limitation().orElseThrow().contains("neighbor-contextual"),
                "Native Wall did not retain its explicit contextual limitation");
        helper.succeed();
    }

    private static SurfaceModel model(BlockState state) {
        return BgeMaterialBindings.fromBlock(state.getBlock()).orElseThrow().surfaceModel(state);
    }

    private static void assertPatch(GameTestHelper helper, SurfaceModel model, Direction face,
            int plane, int uMin, int uMax, int vMin, int vMax, PlaneRelation relation,
            String label) {
        boolean found = model.patches(face).stream().anyMatch(patch -> patch.plane16() == plane
                && patch.uMin16() == uMin && patch.uMax16() == uMax
                && patch.vMin16() == vMin && patch.vMax16() == vMax
                && patch.planeRelation() == relation && patch.canonicalFace() == face);
        helper.assertTrue(found, label + " missing from " + model.patches(face));
    }

    private static Block derived(Block canonical, BgeGeometryRole role) {
        return NibaruProviderAdapter.derived(profile(canonical), role).orElseThrow();
    }

    private static NibaruMaterialProfile profile(Block canonical) {
        Identifier id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(canonical);
        return NibaruMaterialProfiles.all().stream()
                .filter(profile -> profile.canonicalParentId().equals(id))
                .findFirst().orElseThrow(() -> new IllegalStateException("Missing profile " + id));
    }

    @Override
    public void invokeTestMethod(GameTestHelper helper, Method method)
            throws ReflectiveOperationException {
        method.invoke(this, helper);
    }
}
