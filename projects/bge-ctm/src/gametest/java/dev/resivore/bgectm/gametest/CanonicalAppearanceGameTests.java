package dev.resivore.bgectm.gametest;

import dev.aero.cnmterraincompat.BgeGeometryRole;
import dev.aero.cnmterraincompat.BgeLayerBlock;
import dev.aero.cnmterraincompat.GlazedPatternState;
import dev.aero.cnmterraincompat.NibaruProviderAdapter;
import dev.resivore.bgectm.CanonicalAppearanceResolver;
import dev.resivore.bgectm.CanonicalAppearanceResolver.Policy;
import dev.tazer.clutternomore.common.blocks.StepBlock;
import dev.tazer.clutternomore.common.blocks.VerticalSlabBlock;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfiles;
import net.fabricmc.fabric.api.block.v1.FabricBlockState;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;

import java.lang.reflect.Method;

/** State identity and eligibility proofs; these are not visual Continuity runtime evidence. */
public final class CanonicalAppearanceGameTests implements CustomTestMethodInvoker {
    private static final BlockPos QUERY_POS = new BlockPos(2, 2, 2);

    @GameTest(maxTicks = 40)
    public void ordinaryAndDistinctMaterialFamiliesProjectThroughTypedBindings(GameTestHelper helper) {
        BlockState stoneLayer = layerState(Blocks.STONE, 4);
        assertPolicy(helper, stoneLayer, Policy.ELIGIBLE_FULL_LAYER);
        assertCanonical(helper, appearance(helper, stoneLayer), Blocks.STONE,
                "ordinary stone Layer");

        BlockState stoneVertical = verticalState(Blocks.STONE, true);
        assertPolicy(helper, stoneVertical, Policy.ELIGIBLE_DOUBLE_VERTICAL_SLAB);
        assertCanonical(helper, appearance(helper, stoneVertical), Blocks.STONE,
                "double stone Vertical Slab");

        BlockState clear = appearance(helper, layerState(Blocks.GLASS, 4));
        Block whiteStainedGlass = Blocks.STAINED_GLASS.pick(DyeColor.WHITE);
        BlockState white = appearance(helper, layerState(whiteStainedGlass, 4));
        assertCanonical(helper, clear, Blocks.GLASS, "clear glass family");
        assertCanonical(helper, white, whiteStainedGlass, "white stained glass family");
        helper.assertTrue(clear.getBlock() != white.getBlock(),
                "Distinct canonical glass families collapsed into one appearance");

        NibaruProviderAdapter.RuntimeBinding binding = NibaruProviderAdapter
                .runtimeBinding(stoneLayer.getBlock()).orElseThrow();
        helper.assertTrue(binding.role() == BgeGeometryRole.LAYER
                        && appearance(helper, stoneLayer).getBlock() == binding.profile().canonicalParent(),
                "Resolver did not follow BGE's typed runtime binding");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void axisProjectionCopiesOnlyTheMaterialAxis(GameTestHelper helper) {
        BlockState derived = layerState(Blocks.OAK_LOG, 4)
                .setValue(BgeLayerBlock.FACING, Direction.EAST)
                .setValue(BlockStateProperties.AXIS, Direction.Axis.X)
                .setValue(BgeLayerBlock.DOUBLE, true);
        BlockState canonical = appearance(helper, derived);

        assertCanonical(helper, canonical, Blocks.OAK_LOG, "axis Layer");
        helper.assertTrue(canonical.getValue(BlockStateProperties.AXIS) == Direction.Axis.X,
                "Canonical material axis was not projected");
        helper.assertTrue(!canonical.hasProperty(BgeLayerBlock.LAYERS)
                        && !canonical.hasProperty(BgeLayerBlock.DOUBLE),
                "Layer depth/double geometry leaked into canonical state");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void glazedPatternMapsIndependentlyFromGeometryFacing(GameTestHelper helper) {
        Block whiteGlazedTerracotta = Blocks.GLAZED_TERRACOTTA.pick(DyeColor.WHITE);
        BlockState derived = layerState(whiteGlazedTerracotta, 4)
                .setValue(BgeLayerBlock.FACING, Direction.EAST)
                .setValue(GlazedPatternState.PATTERN_FACING, Direction.SOUTH);
        BlockState canonical = appearance(helper, derived);

        assertCanonical(helper, canonical, whiteGlazedTerracotta,
                "glazed pattern Layer");
        helper.assertTrue(canonical.getValue(HorizontalDirectionalBlock.FACING) == Direction.SOUTH,
                "Canonical glazed facing followed geometry instead of pattern orientation");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void leavesPreserveLeafStateButNotGeometryWaterlogging(GameTestHelper helper) {
        BlockState derived = verticalState(Blocks.OAK_LEAVES, true)
                .setValue(BlockStateProperties.DISTANCE, 3)
                .setValue(BlockStateProperties.PERSISTENT, true)
                .setValue(BlockStateProperties.WATERLOGGED, true)
                .setValue(VerticalSlabBlock.FACING, Direction.WEST);
        BlockState canonical = appearance(helper, derived);

        assertCanonical(helper, canonical, Blocks.OAK_LEAVES, "leaf Vertical Slab");
        helper.assertTrue(canonical.getValue(BlockStateProperties.DISTANCE) == 3,
                "Leaf distance was not projected");
        helper.assertTrue(canonical.getValue(BlockStateProperties.PERSISTENT),
                "Leaf persistence was not projected");
        helper.assertTrue(!canonical.getValue(BlockStateProperties.WATERLOGGED),
                "Derived waterlogging leaked into canonical leaf appearance");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void geometryPolicyRejectsPartialLayerSingleVerticalAndEveryStep(GameTestHelper helper) {
        for (int depth = 1; depth <= 3; depth++) {
            assertPolicy(helper, layerState(Blocks.STONE, depth), Policy.PARTIAL_LAYER);
        }
        assertPolicy(helper, layerState(Blocks.STONE, 4), Policy.ELIGIBLE_FULL_LAYER);

        assertPolicy(helper, verticalState(Blocks.STONE, false), Policy.SINGLE_VERTICAL_SLAB);
        assertPolicy(helper, verticalState(Blocks.STONE, true),
                Policy.ELIGIBLE_DOUBLE_VERTICAL_SLAB);

        Block step = derived(Blocks.STONE, BgeGeometryRole.STEP);
        assertPolicy(helper, step.defaultBlockState().setValue(StepBlock.SLAB_TYPE, SlabType.BOTTOM),
                Policy.STEP_GEOMETRY);
        assertPolicy(helper, step.defaultBlockState().setValue(StepBlock.SLAB_TYPE, SlabType.DOUBLE),
                Policy.STEP_GEOMETRY);

        assertPolicy(helper, derived(Blocks.STONE, BgeGeometryRole.CORNER).defaultBlockState(),
                Policy.CORNER_GEOMETRY);
        assertPolicy(helper, derived(Blocks.STONE, BgeGeometryRole.QUARTER_COLUMN).defaultBlockState(),
                Policy.QUARTER_COLUMN_GEOMETRY);
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void unsupportedVisualAndCanonicalStateFailClosed(GameTestHelper helper) {
        BlockState grass = layerState(Blocks.GRASS_BLOCK, 4);
        assertPolicy(helper, grass, Policy.UNMAPPABLE_CANONICAL_STATE);
        helper.assertTrue(appearance(helper, grass) == grass,
                "Unmappable canonical SNOWY state did not retain its derived appearance");

        BlockState honey = layerState(Blocks.HONEY_BLOCK, 4);
        assertPolicy(helper, honey, Policy.UNSUPPORTED_VISUAL_PROFILE);
        helper.assertTrue(appearance(helper, honey) == honey,
                "Inset honey visual unexpectedly inherited canonical appearance");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void appearanceDispatchLeavesNonBgeAndUnboundLookalikeUntouched(GameTestHelper helper) {
        BlockState vanilla = Blocks.STONE.defaultBlockState();
        assertPolicy(helper, vanilla, Policy.NON_BGE);
        helper.assertTrue(appearance(helper, vanilla) == vanilla,
                "Ordinary non-BGE block changed appearance");

        BlockState decoy = BgeCtmFixtureInitializer.UNBOUND_BGE_LOOKING_VERTICAL.defaultBlockState()
                .setValue(VerticalSlabBlock.DOUBLE, true)
                .setValue(VerticalSlabBlock.FACING, Direction.EAST);
        helper.assertTrue(NibaruProviderAdapter.runtimeBinding(decoy.getBlock()).isEmpty(),
                "Registry-name decoy unexpectedly gained a BGE runtime binding");
        assertPolicy(helper, decoy, Policy.NON_BGE);
        helper.assertTrue(appearance(helper, decoy) == decoy,
                "Broad Vertical Slab mixin inferred canonical identity from a BGE-looking registry ID");

        BlockState rejectedStep = derived(Blocks.STONE, BgeGeometryRole.STEP).defaultBlockState()
                .setValue(StepBlock.SLAB_TYPE, SlabType.DOUBLE);
        helper.assertTrue(appearance(helper, rejectedStep) == rejectedStep,
                "Step mixin bypassed the typed ineligibility decision");
        helper.succeed();
    }

    private static BlockState appearance(GameTestHelper helper, BlockState state) {
        return ((FabricBlockState) (Object) state).getAppearance(
                helper.getLevel(), QUERY_POS, Direction.DOWN, state, QUERY_POS);
    }

    private static void assertPolicy(GameTestHelper helper, BlockState state, Policy expected) {
        Policy actual = CanonicalAppearanceResolver.inspect(state).policy();
        helper.assertTrue(actual == expected,
                "Expected policy " + expected + " for " + state + ", got " + actual);
    }

    private static void assertCanonical(GameTestHelper helper, BlockState state,
            Block canonical, String label) {
        helper.assertTrue(state.getBlock() == canonical,
                label + " did not resolve to its canonical parent: " + state);
    }

    private static BlockState layerState(Block canonical, int layers) {
        return derived(canonical, BgeGeometryRole.LAYER).defaultBlockState()
                .setValue(BgeLayerBlock.LAYERS, layers);
    }

    private static BlockState verticalState(Block canonical, boolean doubled) {
        return derived(canonical, BgeGeometryRole.VERTICAL_SLAB).defaultBlockState()
                .setValue(VerticalSlabBlock.DOUBLE, doubled);
    }

    private static Block derived(Block canonical, BgeGeometryRole role) {
        NibaruMaterialProfile profile = NibaruMaterialProfiles.fromBlock(canonical)
                .orElseThrow(() -> new IllegalStateException("Missing BGE profile for " + canonical));
        return NibaruProviderAdapter.derived(profile, role)
                .orElseThrow(() -> new IllegalStateException("Missing " + role + " for "
                        + profile.canonicalParentId()));
    }

    @Override
    public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
        method.invoke(this, helper);
    }
}
