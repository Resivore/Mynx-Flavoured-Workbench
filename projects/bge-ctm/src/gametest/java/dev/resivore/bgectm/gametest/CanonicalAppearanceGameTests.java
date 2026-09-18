package dev.resivore.bgectm.gametest;

import dev.aero.cnmterraincompat.BgeGeometryRole;
import dev.aero.cnmterraincompat.BgeLayerBlock;
import dev.aero.cnmterraincompat.BgeSurfaceGeometry.PlaneRelation;
import dev.aero.cnmterraincompat.CnmTerrainCompat;
import dev.aero.cnmterraincompat.FarmlandSlabBlock;
import dev.aero.cnmterraincompat.GlazedPatternState;
import dev.aero.cnmterraincompat.NibaruProviderAdapter;
import dev.resivore.bgectm.CanonicalAppearanceResolver;
import dev.aero.cnmterraincompat.BgeMaterialBindings.Topology;
import dev.resivore.bgectm.CanonicalAppearanceResolver.Policy;
import dev.resivore.bgectm.SurfaceContactResolver;
import dev.resivore.bgectm.SurfaceContactResolver.Decision;
import dev.resivore.bgectm.SurfaceContactResolver.QuadSurface;
import dev.resivore.bgectm.SurfaceContactResolver.Interval;
import dev.resivore.bgectm.SurfaceContactResolver.SurfaceDescriptor;
import dev.resivore.bgectm.continuity.ContinuityQuadContext;
import dev.resivore.bgectm.continuity.OverlayContactFilter;
import dev.resivore.bgectm.continuity.OverlaySourceEligibility;
import dev.resivore.bgectm.continuity.ContactFilteringConnectionPredicate;
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

/** Controlled material/state/contact proofs; these are not visual Continuity evidence. */
public final class CanonicalAppearanceGameTests implements CustomTestMethodInvoker {
    private static final BlockPos ORIGIN = new BlockPos(0, 0, 0);
    private static final BlockPos EAST = new BlockPos(1, 0, 0);
    private static final BlockPos SOUTH = new BlockPos(0, 0, 1);
    private static final BlockPos QUERY_POS = new BlockPos(2, 2, 2);

    @GameTest(maxTicks = 40)
    public void materialIdentityIsTypedAndNeverInferredFromIds(GameTestHelper helper) {
        BlockState clearTop = slabState(Blocks.GLASS, SlabType.TOP);
        Block whiteStainedGlass = Blocks.STAINED_GLASS.pick(DyeColor.WHITE);
        BlockState whiteTop = slabState(whiteStainedGlass, SlabType.TOP);

        assertDecision(helper, clearTop, ORIGIN, clearTop, EAST, Direction.UP, Decision.CONNECT);
        assertDecision(helper, clearTop, ORIGIN, whiteTop, EAST, Direction.UP, Decision.CONNECT);
        helper.assertTrue(CanonicalAppearanceResolver.materialBinding(clearTop).orElseThrow().materialProfile().orElseThrow()
                        != CanonicalAppearanceResolver.materialBinding(whiteTop).orElseThrow().materialProfile().orElseThrow(),
                "Cross-profile fixture unexpectedly shares one material profile");
        helper.assertTrue(CanonicalAppearanceResolver.materialBinding(clearTop).orElseThrow().topology()
                        == Topology.HORIZONTAL_SLAB,
                "Exact effective slab source was not recognized through BGE's typed profile");

        BlockState decoy = BgeCtmFixtureInitializer.UNBOUND_BGE_LOOKING_VERTICAL.defaultBlockState()
                .setValue(VerticalSlabBlock.FACING, Direction.EAST);
        helper.assertTrue(NibaruProviderAdapter.runtimeBinding(decoy.getBlock()).isEmpty(),
                "Registry-name decoy unexpectedly gained a typed BGE binding");
        assertPolicy(helper, decoy, Policy.NON_BGE_GEOMETRY);
        helper.assertTrue(appearance(helper, decoy) == decoy,
                "Broad geometry mixin inferred material identity from a registry-looking ID");
        assertDecision(helper, decoy, ORIGIN, decoy, SOUTH, Direction.UP,
                Decision.BYPASS_UNRELATED);
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void ordinarySlabsUseCoplanarSurfacesNotStateEquality(GameTestHelper helper) {
        BlockState top = slabState(Blocks.GLASS, SlabType.TOP);
        BlockState bottom = slabState(Blocks.GLASS, SlabType.BOTTOM);
        BlockState doubled = slabState(Blocks.GLASS, SlabType.DOUBLE);
        BlockState full = Blocks.GLASS.defaultBlockState();

        assertDecision(helper, top, ORIGIN, top, EAST, Direction.UP, Decision.CONNECT);
        assertDecision(helper, bottom, ORIGIN, bottom, EAST, Direction.UP, Decision.CONNECT);
        assertDecision(helper, top, ORIGIN, full, EAST, Direction.UP, Decision.CONNECT);
        assertDecision(helper, bottom, ORIGIN, full, EAST, Direction.DOWN, Decision.CONNECT);
        assertDecision(helper, bottom, ORIGIN, full, EAST, Direction.UP, Decision.NON_COPLANAR);
        assertDecision(helper, top, ORIGIN, bottom, EAST, Direction.UP, Decision.NON_COPLANAR);
        assertDecision(helper, doubled, ORIGIN, full, EAST, Direction.UP, Decision.CONNECT);

        BlockState canonical = appearance(helper, top);
        assertCanonical(helper, canonical, Blocks.GLASS, "ordinary glass top slab");
        helper.assertTrue(!canonical.hasProperty(BlockStateProperties.SLAB_TYPE),
                "Ordinary slab form leaked into canonical material state");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void terrainCrossMaterialGeometryIsContactOnly(GameTestHelper helper) {
        BlockState grassTop = slabState(Blocks.GRASS_BLOCK, SlabType.TOP);
        BlockState grassBottom = slabState(Blocks.GRASS_BLOCK, SlabType.BOTTOM);
        BlockState dirtTop = slabState(Blocks.DIRT, SlabType.TOP);

        assertDecision(helper, grassTop, ORIGIN, Blocks.PODZOL.defaultBlockState(), EAST,
                Direction.UP, Decision.CONNECT);
        assertDecision(helper, grassTop, ORIGIN, Blocks.DIRT.defaultBlockState(), EAST,
                Direction.UP, Decision.CONNECT);
        assertDecision(helper, dirtTop, ORIGIN, Blocks.GRASS_BLOCK.defaultBlockState(), EAST,
                Direction.UP, Decision.CONNECT);
        assertDecision(helper, grassTop, ORIGIN, dirtTop, EAST,
                Direction.UP, Decision.CONNECT);
        assertDecision(helper, grassBottom, ORIGIN, dirtTop, EAST,
                Direction.UP, Decision.NON_COPLANAR);

        NibaruMaterialProfile grassProfile = CanonicalAppearanceResolver
                .materialBinding(grassTop).orElseThrow().materialProfile().orElseThrow();
        NibaruMaterialProfile dirtProfile = CanonicalAppearanceResolver
                .materialBinding(dirtTop).orElseThrow().materialProfile().orElseThrow();
        NibaruMaterialProfile podzolProfile = NibaruMaterialProfiles
                .fromBlock(Blocks.PODZOL).orElseThrow();
        helper.assertTrue(grassProfile != dirtProfile && grassProfile != podzolProfile,
                "Terrain cross-material fixture did not use distinct typed profiles");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void overlayUsesTheSameSurfacePlaneModelAsRegularCtm(GameTestHelper helper) {
        BlockState grassTop = slabState(Blocks.GRASS_BLOCK, SlabType.TOP);
        BlockState grassBottom = slabState(Blocks.GRASS_BLOCK, SlabType.BOTTOM);
        BlockState fullDirt = Blocks.DIRT.defaultBlockState();
        var topBoundaryQuad = new ContinuityQuadContext.Capture(new QuadSurface(
                Direction.UP, 16, Direction.Axis.X, 0, 16,
                Direction.Axis.Z, 0, 16));
        var recessedQuad = new ContinuityQuadContext.Capture(new QuadSurface(
                Direction.UP, 8, Direction.Axis.X, 0, 16,
                Direction.Axis.Z, 0, 16));

        helper.assertTrue(OverlayContactFilter.retainAfterUpstream(true,
                        grassTop, ORIGIN, fullDirt, EAST, Direction.UP, topBoundaryQuad),
                "Positive cross-material overlay lost valid coplanar receiver contact");
        helper.assertTrue(!OverlayContactFilter.retainAfterUpstream(true,
                        grassBottom, ORIGIN, fullDirt, EAST, Direction.UP, recessedQuad),
                "Standard Overlay substituted boundary adjacency for incompatible surface planes");
        helper.assertTrue(!OverlayContactFilter.retainAfterUpstream(false,
                        grassTop, ORIGIN, fullDirt, EAST, Direction.UP, topBoundaryQuad),
                "Geometry turned a negative upstream overlay result positive");
        helper.assertTrue(OverlayContactFilter.retainAfterUpstream(true,
                        Blocks.STONE.defaultBlockState(), ORIGIN,
                        Blocks.BRICKS.defaultBlockState(), EAST, Direction.UP, null),
                "Unrelated full-block overlay behavior unexpectedly required quad context");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void farmlandSpecialBindingUsesGenericTypedSurfaceContact(GameTestHelper helper) {
        BlockState grassBottom = slabState(Blocks.GRASS_BLOCK, SlabType.BOTTOM);
        BlockState grassTop = slabState(Blocks.GRASS_BLOCK, SlabType.TOP);
        BlockState farmlandBottom = CnmTerrainCompat.FARMLAND_SLAB.defaultBlockState()
                .setValue(FarmlandSlabBlock.TYPE, SlabType.BOTTOM)
                .setValue(FarmlandSlabBlock.MOISTURE, 5);
        BlockState farmlandTop = farmlandBottom.setValue(FarmlandSlabBlock.TYPE, SlabType.TOP);
        BlockState farmlandDouble = farmlandBottom.setValue(FarmlandSlabBlock.TYPE, SlabType.DOUBLE);

        helper.assertTrue(CanonicalAppearanceResolver.materialBinding(farmlandBottom).orElseThrow()
                        .membership() == dev.aero.cnmterraincompat.BgeMaterialBindings.CatalogMembership.SPECIAL_CANONICAL_BOUND,
                "Farmland Slab was not recognized as BGE's special canonical binding");
        BlockState canonical = appearance(helper, farmlandBottom);
        assertCanonical(helper, canonical, Blocks.FARMLAND, "special farmland slab");
        helper.assertTrue(canonical.getValue(BlockStateProperties.MOISTURE) == 5,
                "Farmland canonical projection lost moisture on the real appearance path");
        helper.assertTrue(SurfaceContactResolver.inspect(grassBottom, ORIGIN, farmlandBottom, EAST,
                        Direction.UP) == Decision.CONNECT,
                "Typed terrain relation did not bridge the intentional 8/16 versus 7/16 top planes");
        assertDecision(helper, grassBottom, ORIGIN, farmlandBottom, EAST,
                Direction.NORTH, Decision.CONNECT);
        var bottomReceiver = new ContinuityQuadContext.Capture(new QuadSurface(Direction.UP, 8,
                Direction.Axis.X, 0, 16, Direction.Axis.Z, 0, 16));
        helper.assertTrue(OverlayContactFilter.retainAfterUpstream(true, grassBottom, ORIGIN,
                        farmlandBottom, EAST, Direction.UP, bottomReceiver),
                "Generic typed overlay contact rejected grass/farmland bottom slabs");
        var topReceiver = new ContinuityQuadContext.Capture(new QuadSurface(Direction.UP, 16,
                Direction.Axis.X, 0, 16, Direction.Axis.Z, 0, 16));
        helper.assertTrue(OverlayContactFilter.retainAfterUpstream(true, grassTop, ORIGIN,
                        farmlandTop, EAST, Direction.UP, topReceiver),
                "Generic typed overlay contact rejected grass/farmland top slabs");
        helper.assertTrue(OverlayContactFilter.retainAfterUpstream(true, Blocks.GRASS_BLOCK.defaultBlockState(), ORIGIN,
                        farmlandDouble, EAST, Direction.UP, topReceiver),
                "Full receiver to special Farmland Slab lost its typed plane relationship");
        assertLocalSurface(helper, farmlandBottom, Direction.UP, 7, 0, 16, 0, 16);
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void ordinaryCtmMatchesCorrespondingOrientationsOnAllSixFaces(GameTestHelper helper) {
        BlockState bottom = slabState(Blocks.GLASS, SlabType.BOTTOM);
        for (Direction face : Direction.values()) {
            BlockPos neighbor = face.getAxis() == Direction.Axis.X ? SOUTH : EAST;
            assertDecision(helper, bottom, ORIGIN, bottom, neighbor, face, Decision.CONNECT);
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void oneUnitToleranceRequiresAnExplicitTerrainRelation(GameTestHelper helper) {
        SurfaceDescriptor exactEight = descriptor(8, PlaneRelation.EXACT);
        SurfaceDescriptor exactSeven = descriptor(7, PlaneRelation.EXACT);
        SurfaceDescriptor terrainSeven = descriptor(7, PlaneRelation.TERRAIN_HEIGHT_INSET);
        helper.assertTrue(!SurfaceContactResolver.planesCompatible(exactEight, exactSeven),
                "An ordinary 1/16 plane mismatch received a generic tolerance");
        helper.assertTrue(SurfaceContactResolver.planesCompatible(exactEight, terrainSeven),
                "An explicitly typed terrain inset lost its one-unit plane relation");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void overlayEmissionFallbackDescribesUnambiguousProviderSurfaces(GameTestHelper helper) {
        BlockState bottom = slabState(Blocks.GRASS_BLOCK, SlabType.BOTTOM);
        BlockState top = slabState(Blocks.GRASS_BLOCK, SlabType.TOP);
        BlockState doubled = slabState(Blocks.GRASS_BLOCK, SlabType.DOUBLE);
        BlockState layer = layerState(Blocks.GRASS_BLOCK, Direction.UP, 2);
        BlockState vertical = verticalState(Blocks.GRASS_BLOCK, Direction.EAST, false);

        assertLocalSurface(helper, bottom, Direction.UP, 8, 0, 16, 0, 16);
        assertLocalSurface(helper, bottom, Direction.NORTH, 0, 0, 16, 0, 8);
        assertLocalSurface(helper, top, Direction.NORTH, 0, 0, 16, 8, 16);
        assertLocalSurface(helper, doubled, Direction.UP, 16, 0, 16, 0, 16);
        assertLocalSurface(helper, layer, Direction.UP, 8, 0, 16, 0, 16);
        assertLocalSurface(helper, vertical, Direction.UP, 16, 8, 16, 0, 16);

        BlockState step = derived(Blocks.GRASS_BLOCK, BgeGeometryRole.STEP).defaultBlockState();
        helper.assertTrue(SurfaceContactResolver.describeLocalForOverlay(step, Direction.UP).isPresent(),
                "An unambiguous Step top patch was not available to overlay emission");
        BlockState compoundStep = step.setValue(StepBlock.SLAB_TYPE, SlabType.DOUBLE);
        helper.assertTrue(!SurfaceContactResolver.stateDerivedFallbackSafe(
                        compoundStep, Blocks.GRASS_BLOCK.defaultBlockState()),
                "Compound Step topology was flattened into a whole-state simple fallback");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void canaryFourPartialSourceGateAndRegularFallbackStayBounded(GameTestHelper helper) {
        BlockState top = slabState(Blocks.STONE, SlabType.TOP);
        BlockState bottom = slabState(Blocks.STONE, SlabType.BOTTOM);
        BlockState doubled = slabState(Blocks.STONE, SlabType.DOUBLE);
        BlockState layer = layerState(Blocks.STONE, Direction.DOWN, 1);
        BlockState vertical = verticalState(Blocks.STONE, Direction.EAST, false);
        BlockState unrelatedPartial = Blocks.COBBLESTONE_WALL.defaultBlockState();

        helper.assertTrue(OverlaySourceEligibility.mayReachCanonicalSemantics(top)
                        && OverlaySourceEligibility.mayReachCanonicalSemantics(bottom)
                        && OverlaySourceEligibility.mayReachCanonicalSemantics(doubled)
                        && OverlaySourceEligibility.mayReachCanonicalSemantics(layer)
                        && OverlaySourceEligibility.mayReachCanonicalSemantics(vertical),
                "A supported typed BGE carrier did not reach canonical overlay semantics");
        helper.assertTrue(!OverlaySourceEligibility.mayReachCanonicalSemantics(unrelatedPartial),
                "An unmanaged partial block bypassed the native full-source gate");

        helper.assertTrue(ContactFilteringConnectionPredicate.retainRegularAfterUpstream(
                        Decision.CONNECT, null,
                        SurfaceContactResolver.stateDerivedFallbackSafe(top, Blocks.STONE.defaultBlockState()),
                        () -> Decision.CONNECT),
                "Safe simple top-slab CTM did not use the state-derived fallback");
        helper.assertTrue(!ContactFilteringConnectionPredicate.retainRegularAfterUpstream(
                        Decision.CONNECT, null, false, () -> Decision.CONNECT),
                "Unsafe or unknown rendered topology did not remain fail-closed");
        helper.assertTrue(OverlayContactFilter.retainAfterUpstream(true, top, ORIGIN,
                        Blocks.STONE.defaultBlockState(), EAST, Direction.UP, null),
                "Coplanar partial overlay source/receiver lost C4's safe fallback");
        helper.assertTrue(!OverlayContactFilter.retainAfterUpstream(true, bottom, ORIGIN,
                        Blocks.STONE.defaultBlockState(), EAST, Direction.UP, null),
                "Overlay state fallback ignored incompatible bottom-slab/full-block top planes");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void partialLayersConnectOnlyOnCompatiblePlanes(GameTestHelper helper) {
        BlockState full = Blocks.GLASS.defaultBlockState();
        BlockState topBoundaryThin = layerState(Blocks.GLASS, Direction.DOWN, 1);
        BlockState topBoundaryThick = layerState(Blocks.GLASS, Direction.DOWN, 2);
        BlockState bottomBoundary = layerState(Blocks.GLASS, Direction.UP, 1);
        BlockState fullLayer = layerState(Blocks.GLASS, Direction.UP, 4);
        BlockState topSlab = slabState(Blocks.GLASS, SlabType.TOP);

        assertDecision(helper, topBoundaryThin, ORIGIN, full, EAST, Direction.UP,
                Decision.CONNECT);
        assertDecision(helper, bottomBoundary, ORIGIN, full, EAST, Direction.DOWN,
                Decision.CONNECT);
        assertDecision(helper, topBoundaryThin, ORIGIN, topBoundaryThick, EAST, Direction.UP,
                Decision.CONNECT);
        assertDecision(helper, topSlab, ORIGIN, topBoundaryThin, EAST, Direction.UP,
                Decision.CONNECT);
        assertDecision(helper, bottomBoundary, ORIGIN, full, EAST, Direction.UP,
                Decision.NON_COPLANAR);
        assertDecision(helper, bottomBoundary, ORIGIN, topBoundaryThin, EAST, Direction.UP,
                Decision.NON_COPLANAR);
        assertDecision(helper, fullLayer, ORIGIN, full, EAST, Direction.UP, Decision.CONNECT);

        assertPolicy(helper, topBoundaryThin, Policy.ELIGIBLE_BOUND_SURFACE);
        assertCanonical(helper, appearance(helper, topBoundaryThin), Blocks.GLASS,
                "partial glass Layer rule-selection appearance");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void singleVerticalSlabsUseExactBoundaryOccupancy(GameTestHelper helper) {
        BlockState eastHalf = verticalState(Blocks.GLASS, Direction.EAST, false);
        BlockState westHalf = verticalState(Blocks.GLASS, Direction.WEST, false);
        BlockState doubled = verticalState(Blocks.GLASS, Direction.NORTH, true);
        BlockState full = Blocks.GLASS.defaultBlockState();

        assertDecision(helper, eastHalf, ORIGIN, eastHalf, SOUTH, Direction.UP,
                Decision.CONNECT);
        assertDecision(helper, eastHalf, ORIGIN, full, EAST, Direction.UP,
                Decision.CONNECT);
        assertDecision(helper, eastHalf, ORIGIN, full, SOUTH, Direction.EAST,
                Decision.CONNECT);
        assertDecision(helper, westHalf, ORIGIN, full, EAST, Direction.UP,
                Decision.NO_BOUNDARY_CONTACT);
        assertDecision(helper, eastHalf, ORIGIN, westHalf, SOUTH, Direction.UP,
                Decision.NO_BOUNDARY_CONTACT);
        assertDecision(helper, doubled, ORIGIN, full, EAST, Direction.UP, Decision.CONNECT);

        assertPolicy(helper, eastHalf, Policy.ELIGIBLE_BOUND_SURFACE);
        assertCanonical(helper, appearance(helper, eastHalf), Blocks.GLASS,
                "single glass Vertical Slab rule-selection appearance");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void supportedCrossGeometryPairsShareOneContactModel(GameTestHelper helper) {
        BlockState topSlab = slabState(Blocks.GLASS, SlabType.TOP);
        BlockState topLayer = layerState(Blocks.GLASS, Direction.DOWN, 1);
        BlockState eastVertical = verticalState(Blocks.GLASS, Direction.EAST, false);
        BlockState westVertical = verticalState(Blocks.GLASS, Direction.WEST, false);

        assertDecision(helper, topSlab, ORIGIN, topLayer, EAST, Direction.UP,
                Decision.CONNECT);
        assertDecision(helper, topLayer, ORIGIN, westVertical, EAST, Direction.UP,
                Decision.CONNECT);
        assertDecision(helper, eastVertical, ORIGIN, topLayer, SOUTH, Direction.UP,
                Decision.CONNECT);
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void oppositeNormalsAndZeroLengthOverlapNeverConnect(GameTestHelper helper) {
        BlockState top = slabState(Blocks.GLASS, SlabType.TOP);
        BlockState bottom = slabState(Blocks.GLASS, SlabType.BOTTOM);
        var topInner = SurfaceContactResolver.describe(top, ORIGIN, Direction.DOWN).orElseThrow();
        var bottomInner = SurfaceContactResolver.describe(bottom, EAST, Direction.UP).orElseThrow();
        helper.assertTrue(topInner.plane16() == bottomInner.plane16(),
                "Fixture no longer shares the internal y=8 plane");
        helper.assertTrue(!SurfaceContactResolver.meetAlongEvaluatedBoundary(topInner, bottomInner),
                "Opposite face normals manufactured a connection on a shared plane");

        BlockState eastHalf = verticalState(Blocks.GLASS, Direction.EAST, false);
        BlockState westHalf = verticalState(Blocks.GLASS, Direction.WEST, false);
        assertDecision(helper, eastHalf, ORIGIN, westHalf, SOUTH, Direction.UP,
                Decision.NO_BOUNDARY_CONTACT);
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void exactSourceQuadBoundsVetoInsetSplitFaces(GameTestHelper helper) {
        BlockState top = slabState(Blocks.GLASS, SlabType.TOP);
        BlockState full = Blocks.GLASS.defaultBlockState();
        var broadTop = new SurfaceContactResolver.QuadSurface(Direction.UP, 16,
                Direction.Axis.X, 0, 16, Direction.Axis.Z, 0, 16);
        var insetTop = new SurfaceContactResolver.QuadSurface(Direction.UP, 16,
                Direction.Axis.X, 1, 15, Direction.Axis.Z, 1, 15);

        Decision broad = SurfaceContactResolver.inspectWithSourceSurface(
                top, ORIGIN, full, EAST, Direction.UP, broadTop);
        Decision inset = SurfaceContactResolver.inspectWithSourceSurface(
                top, ORIGIN, full, EAST, Direction.UP, insetTop);
        helper.assertTrue(broad == Decision.CONNECT,
                "Full rendered top quad no longer reaches the adjacent full-block surface");
        helper.assertTrue(inset == Decision.NO_BOUNDARY_CONTACT,
                "Inset rendered quad inherited contact from the state-wide slab bounds");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void contactVetoDoesNotDestroyRuleSelection(GameTestHelper helper) {
        BlockState bottom = slabState(Blocks.GLASS, SlabType.BOTTOM);
        BlockState full = Blocks.GLASS.defaultBlockState();
        assertCanonical(helper, appearance(helper, bottom), Blocks.GLASS,
                "bottom slab canonical rule-selection appearance");
        assertDecision(helper, bottom, ORIGIN, full, EAST, Direction.UP,
                Decision.NON_COPLANAR);

        BlockState unrelated = BgeCtmFixtureInitializer.UNBOUND_BGE_LOOKING_VERTICAL
                .defaultBlockState().setValue(VerticalSlabBlock.FACING, Direction.NORTH);
        assertDecision(helper, unrelated, ORIGIN, unrelated, EAST, Direction.UP,
                Decision.BYPASS_UNRELATED);
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void compoundSurfaceContractsParticipateAndContextualNativeFormsFailClosed(GameTestHelper helper) {
        BlockState step = derived(Blocks.STONE, BgeGeometryRole.STEP).defaultBlockState()
                .setValue(StepBlock.SLAB_TYPE, SlabType.BOTTOM);
        assertPolicy(helper, step, Policy.ELIGIBLE_BOUND_SURFACE);
        helper.assertTrue(!SurfaceContactResolver.describeAll(step, ORIGIN, Direction.UP).isEmpty(),
                "Step did not expose its BGE-owned surface patches");

        BlockState corner = derived(Blocks.STONE, BgeGeometryRole.CORNER).defaultBlockState();
        assertPolicy(helper, corner, Policy.ELIGIBLE_BOUND_SURFACE);
        helper.assertTrue(!SurfaceContactResolver.describeAll(corner, ORIGIN, Direction.UP).isEmpty(),
                "Corner did not expose its BGE-owned surface patches");
        BlockState column = derived(Blocks.STONE, BgeGeometryRole.QUARTER_COLUMN).defaultBlockState();
        assertPolicy(helper, column, Policy.ELIGIBLE_BOUND_SURFACE);
        helper.assertTrue(!SurfaceContactResolver.describeAll(column, ORIGIN, Direction.UP).isEmpty(),
                "Quarter Column did not expose its BGE-owned surface patches");

        NibaruMaterialProfile stone = NibaruMaterialProfiles.fromBlock(Blocks.STONE).orElseThrow();
        BlockState stair = stone.effectiveStairSource().orElseThrow().defaultBlockState();
        BlockState wall = stone.nativeWall().orElseThrow().defaultBlockState();
        assertPolicy(helper, stair, Policy.UNSUPPORTED_SURFACE_CONTRACT);
        assertPolicy(helper, wall, Policy.UNSUPPORTED_SURFACE_CONTRACT);
        assertDecision(helper, stair, ORIGIN, Blocks.STONE.defaultBlockState(), EAST,
                Direction.UP, Decision.UNSUPPORTED_GEOMETRY);
        assertDecision(helper, wall, ORIGIN, Blocks.STONE.defaultBlockState(), EAST,
                Direction.UP, Decision.UNSUPPORTED_GEOMETRY);
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void canonicalMaterialPropertiesRemainSeparateFromGeometry(GameTestHelper helper) {
        BlockState axisLayer = layerState(Blocks.OAK_LOG, Direction.EAST, 2)
                .setValue(BlockStateProperties.AXIS, Direction.Axis.X)
                .setValue(BgeLayerBlock.DOUBLE, true);
        BlockState axisCanonical = appearance(helper, axisLayer);
        assertCanonical(helper, axisCanonical, Blocks.OAK_LOG, "axis Layer");
        helper.assertTrue(axisCanonical.getValue(BlockStateProperties.AXIS) == Direction.Axis.X,
                "Canonical material axis was not projected");
        helper.assertTrue(!axisCanonical.hasProperty(BgeLayerBlock.LAYERS)
                        && !axisCanonical.hasProperty(BgeLayerBlock.DOUBLE),
                "Layer form leaked into canonical state");

        Block whiteGlazed = Blocks.GLAZED_TERRACOTTA.pick(DyeColor.WHITE);
        BlockState glazed = layerState(whiteGlazed, Direction.EAST, 1)
                .setValue(GlazedPatternState.PATTERN_FACING, Direction.SOUTH);
        helper.assertTrue(appearance(helper, glazed).getValue(HorizontalDirectionalBlock.FACING)
                        == Direction.SOUTH,
                "Glazed material facing followed geometry rather than pattern orientation");

        BlockState leaves = verticalState(Blocks.OAK_LEAVES, Direction.WEST, false)
                .setValue(BlockStateProperties.DISTANCE, 3)
                .setValue(BlockStateProperties.PERSISTENT, true)
                .setValue(BlockStateProperties.WATERLOGGED, true);
        BlockState leafCanonical = appearance(helper, leaves);
        helper.assertTrue(leafCanonical.getValue(BlockStateProperties.DISTANCE) == 3
                        && leafCanonical.getValue(BlockStateProperties.PERSISTENT)
                        && !leafCanonical.getValue(BlockStateProperties.WATERLOGGED),
                "Leaf material state or geometry waterlogging projection changed");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void grassGeometryUsesCanonicalWorldSnowSemantics(GameTestHelper helper) {
        BlockState slab = slabState(Blocks.GRASS_BLOCK, SlabType.TOP)
                .setValue(BlockStateProperties.SNOWY, true);
        BlockState layer = layerState(Blocks.GRASS_BLOCK, Direction.DOWN, 1);
        BlockState vertical = verticalState(Blocks.GRASS_BLOCK, Direction.EAST, false);

        assertPolicy(helper, slab, Policy.ELIGIBLE_BOUND_SURFACE);
        assertPolicy(helper, layer, Policy.ELIGIBLE_BOUND_SURFACE);
        assertPolicy(helper, vertical, Policy.ELIGIBLE_BOUND_SURFACE);

        helper.setBlock(QUERY_POS.offset(2, 1, 0), Blocks.AIR);
        helper.setBlock(QUERY_POS.above(), Blocks.AIR);
        assertGrassAppearance(helper, appearance(helper, slab), false, "ordinary grass slab");
        assertGrassAppearance(helper, appearance(helper, layer), false, "grass Layer");
        assertGrassAppearance(helper, appearance(helper, vertical), false,
                "grass Vertical Slab");

        helper.setBlock(QUERY_POS.above(), Blocks.SNOW_BLOCK);
        assertGrassAppearance(helper, appearance(helper,
                slab.setValue(BlockStateProperties.SNOWY, false)), true,
                "snow-covered ordinary grass slab");
        assertGrassAppearance(helper, appearance(helper, layer), true,
                "snow-covered grass Layer");
        assertGrassAppearance(helper, appearance(helper, vertical), true,
                "snow-covered grass Vertical Slab");

        BlockState canonical = appearance(helper, layer);
        helper.assertTrue(!canonical.hasProperty(BlockStateProperties.SLAB_TYPE)
                        && !canonical.hasProperty(BgeLayerBlock.FACING)
                        && !canonical.hasProperty(BgeLayerBlock.LAYERS)
                        && !canonical.hasProperty(BgeLayerBlock.DOUBLE)
                        && !canonical.hasProperty(VerticalSlabBlock.FACING)
                        && !canonical.hasProperty(VerticalSlabBlock.DOUBLE)
                        && !canonical.hasProperty(BlockStateProperties.WATERLOGGED),
                "Geometry form leaked into canonical grass state");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void canonicalSnowyDirtProjectionIsProfileIndependent(GameTestHelper helper) {
        BlockState podzolDouble = slabState(Blocks.PODZOL, SlabType.DOUBLE);
        BlockState podzolTop = slabState(Blocks.PODZOL, SlabType.TOP);
        BlockState myceliumDouble = slabState(Blocks.MYCELIUM, SlabType.DOUBLE);

        // This matches the owner-observed physical state: one full-volume ordinary slab
        // carrier, not a pair of TOP/BOTTOM slabs. It must project only canonical podzol.
        assertPolicy(helper, podzolDouble, Policy.ELIGIBLE_BOUND_SURFACE);
        helper.setBlock(QUERY_POS.above(), Blocks.AIR);
        assertSnowyAppearance(helper, appearance(helper, podzolDouble), Blocks.PODZOL, false,
                "dry podzol double slab");
        assertSnowyAppearance(helper, appearance(helper, podzolTop), Blocks.PODZOL, false,
                "dry podzol top slab");
        assertSnowyAppearance(helper, appearance(helper, myceliumDouble), Blocks.MYCELIUM, false,
                "dry mycelium double slab");

        helper.setBlock(QUERY_POS.above(), Blocks.SNOW_BLOCK);
        assertSnowyAppearance(helper, appearance(helper, podzolDouble), Blocks.PODZOL, true,
                "snow-covered podzol double slab");
        assertSnowyAppearance(helper, appearance(helper, myceliumDouble), Blocks.MYCELIUM, true,
                "snow-covered mycelium double slab");

        BlockState canonical = appearance(helper, podzolDouble);
        helper.assertTrue(!canonical.hasProperty(BlockStateProperties.SLAB_TYPE)
                        && !canonical.hasProperty(BlockStateProperties.WATERLOGGED),
                "Double-slab geometry properties leaked into canonical podzol state");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void unrelatedVisualAllowlistStillFailsClosed(GameTestHelper helper) {

        BlockState mycelium = layerState(Blocks.MYCELIUM, Direction.DOWN, 1);
        assertPolicy(helper, mycelium, Policy.ELIGIBLE_BOUND_SURFACE);
        assertCanonical(helper, appearance(helper, mycelium), Blocks.MYCELIUM,
                "Snowy canonical mycelium must project independently of visual profile");

        BlockState honey = layerState(Blocks.HONEY_BLOCK, Direction.DOWN, 1);
        assertPolicy(helper, honey, Policy.UNSUPPORTED_VISUAL_PROFILE);
        helper.assertTrue(appearance(helper, honey) == honey,
                "Inset honey visual unexpectedly inherited canonical appearance");
        assertDecision(helper, honey, ORIGIN, Blocks.HONEY_BLOCK.defaultBlockState(), EAST,
                Direction.UP, Decision.UNSUPPORTED_GEOMETRY);
        helper.succeed();
    }

    private static BlockState appearance(GameTestHelper helper, BlockState state) {
        BlockPos absolute = helper.absolutePos(QUERY_POS);
        BlockPos distinctSource = helper.absolutePos(QUERY_POS.offset(2, 0, 0));
        return ((FabricBlockState) (Object) state).getAppearance(
                helper.getLevel(), absolute, Direction.DOWN, state, distinctSource);
    }

    private static void assertGrassAppearance(GameTestHelper helper, BlockState state,
            boolean snowy, String label) {
        assertCanonical(helper, state, Blocks.GRASS_BLOCK, label);
        helper.assertTrue(state.hasProperty(BlockStateProperties.SNOWY)
                        && state.getValue(BlockStateProperties.SNOWY) == snowy,
                label + " did not project vanilla snowy semantics: " + state);
    }

    private static void assertSnowyAppearance(GameTestHelper helper, BlockState state,
            Block canonical, boolean snowy, String label) {
        assertCanonical(helper, state, canonical, label);
        helper.assertTrue(state.hasProperty(BlockStateProperties.SNOWY)
                        && state.getValue(BlockStateProperties.SNOWY) == snowy,
                label + " did not project positional snowy semantics: " + state);
    }

    private static void assertDecision(GameTestHelper helper, BlockState source, BlockPos sourcePos,
            BlockState other, BlockPos otherPos, Direction face, Decision expected) {
        Decision actual = SurfaceContactResolver.inspect(source, sourcePos, other, otherPos, face);
        helper.assertTrue(actual == expected,
                "Expected " + expected + " for " + source + " -> " + other
                        + " on " + face + ", got " + actual);
    }

    private static SurfaceDescriptor descriptor(long plane, PlaneRelation relation) {
        return new SurfaceDescriptor(Direction.UP, plane, Direction.Axis.X,
                new Interval(0, 16), Direction.Axis.Z, new Interval(0, 16), ORIGIN,
                Direction.UP, relation);
    }

    private static void assertLocalSurface(GameTestHelper helper, BlockState state, Direction face,
            int plane, int uMin, int uMax, int vMin, int vMax) {
        QuadSurface actual = SurfaceContactResolver.describeLocalForOverlay(state, face).orElseThrow();
        helper.assertTrue(actual.normal() == face && actual.plane16() == plane
                        && actual.uMin16() == uMin && actual.uMax16() == uMax
                        && actual.vMin16() == vMin && actual.vMax16() == vMax,
                "Unexpected state-derived overlay surface for " + state + " on " + face + ": " + actual);
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

    private static BlockState slabState(Block canonical, SlabType type) {
        NibaruMaterialProfile profile = NibaruMaterialProfiles.fromBlock(canonical)
                .orElseThrow(() -> new IllegalStateException("Missing BGE profile for " + canonical));
        Block slab = profile.effectiveSlabSource()
                .orElseThrow(() -> new IllegalStateException("Missing effective slab for " + canonical));
        return slab.defaultBlockState().setValue(BlockStateProperties.SLAB_TYPE, type);
    }

    private static BlockState layerState(Block canonical, Direction facing, int layers) {
        return derived(canonical, BgeGeometryRole.LAYER).defaultBlockState()
                .setValue(BgeLayerBlock.FACING, facing)
                .setValue(BgeLayerBlock.LAYERS, layers);
    }

    private static BlockState verticalState(Block canonical, Direction facing, boolean doubled) {
        return derived(canonical, BgeGeometryRole.VERTICAL_SLAB).defaultBlockState()
                .setValue(VerticalSlabBlock.FACING, facing)
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
