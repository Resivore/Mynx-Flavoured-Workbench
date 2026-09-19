package dev.resivore.bgeglassculling.gametest;

import dev.aero.cnmterraincompat.BgeLayerBlock;
import dev.aero.cnmterraincompat.BgeMaterialBindings;
import dev.aero.cnmterraincompat.BgeMaterialBindings.Binding;
import dev.aero.cnmterraincompat.BgeMaterialBindings.Role;
import dev.aero.cnmterraincompat.BgeMaterialBindings.Topology;
import dev.aero.cnmterraincompat.BgeSurfaceGeometry.SurfaceModel;
import dev.resivore.bgeglassculling.MaterialCompatibility;
import dev.resivore.bgeglassculling.SurfaceOverlapResolver;
import dev.resivore.bgeglassculling.geometry.Rect16;
import dev.resivore.bgeglassculling.geometry.RectSubtraction;
import dev.tazer.clutternomore.common.blocks.StepBlock;
import dev.tazer.clutternomore.common.blocks.VerticalSlabBlock;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfiles;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.level.block.state.properties.WallSide;

import java.lang.reflect.Method;
import java.util.List;

/** Controlled BGE surface/material proofs; these are not client visual-runtime evidence. */
public final class BgeGlassSurfaceCullingGameTests implements CustomTestMethodInvoker {
    private static final Rect16 FULL = new Rect16(0, 16, 0, 16);

    @GameTest(maxTicks = 40)
    public void canonicalGlassCompatibilityIsIndependentFromGeometry(GameTestHelper helper) {
        BlockState clear = Blocks.GLASS.defaultBlockState();
        BlockState clearSlab = primary(Role.HORIZONTAL_SLAB).physicalBlock().defaultBlockState();
        BlockState white = Blocks.STAINED_GLASS.pick(DyeColor.WHITE).defaultBlockState();
        BlockState ice = Blocks.ICE.defaultBlockState();

        helper.assertTrue(MaterialCompatibility.isGlassState(clear)
                        && MaterialCompatibility.isGlassState(clearSlab),
                "Clear glass states did not resolve through BGE's canonical material profile");
        helper.assertTrue(MaterialCompatibility.mutuallyCullCompatible(clear, clearSlab),
                "Same canonical clear-glass family was not mutually compatible");
        helper.assertTrue(!MaterialCompatibility.mutuallyCullCompatible(clear, white),
                "Distinct clear/stained canonical materials became mutually compatible");
        helper.assertTrue(!MaterialCompatibility.isGlassState(ice)
                        && !MaterialCompatibility.mutuallyCullCompatible(clear, ice),
                "Arbitrary translucent non-glass material entered glass culling");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void fullAndSimpleGeometrySubtractExactContact(GameTestHelper helper) {
        BlockState full = Blocks.GLASS.defaultBlockState();
        Block slab = primary(Role.HORIZONTAL_SLAB).physicalBlock();
        assertRegions(helper, full, slab.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM),
                Direction.EAST, 16, List.of(new Rect16(0, 8, 0, 16)), "bottom slab");
        assertRegions(helper, full, slab.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP),
                Direction.EAST, 16, List.of(new Rect16(8, 16, 0, 16)), "top slab");
        assertRegions(helper, full, full, Direction.EAST, 16, List.of(FULL), "full block");

        BlockState vertical = primary(Role.VERTICAL_SLAB).physicalBlock().defaultBlockState()
                .setValue(VerticalSlabBlock.FACING, Direction.NORTH);
        assertRegions(helper, full, vertical, Direction.EAST, 16,
                List.of(new Rect16(0, 16, 0, 8)), "Vertical Slab");

        BlockState step = primary(Role.STEP).physicalBlock().defaultBlockState()
                .setValue(StepBlock.FACING, Direction.NORTH)
                .setValue(StepBlock.SLAB_TYPE, SlabType.BOTTOM);
        assertRegions(helper, full, step, Direction.EAST, 16,
                List.of(new Rect16(0, 8, 0, 8)), "Step");

        Block layerBlock = primary(Role.LAYER).physicalBlock();
        BlockState oneLayer = layerBlock.defaultBlockState()
                .setValue(BgeLayerBlock.FACING, Direction.UP)
                .setValue(BgeLayerBlock.LAYERS, 1);
        BlockState threeLayers = oneLayer.setValue(BgeLayerBlock.LAYERS, 3);
        assertRegions(helper, full, oneLayer, Direction.EAST, 16,
                List.of(new Rect16(0, 4, 0, 16)), "one Layer");
        assertRegions(helper, full, threeLayers, Direction.EAST, 16,
                List.of(new Rect16(0, 12, 0, 16)), "three Layers");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void compoundAndPartialGeometryUseTheSameIntersectionEngine(GameTestHelper helper) {
        BlockState full = Blocks.GLASS.defaultBlockState();
        for (Role role : List.of(Role.CORNER, Role.QUARTER_COLUMN)) {
            BlockState partial = primary(role).physicalBlock().defaultBlockState();
            boolean strictPartialContact = false;
            for (Direction normal : Direction.Plane.HORIZONTAL) {
                int plane = normal.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 16 : 0;
                List<Rect16> candidate = regions(full, partial, normal, plane, FULL);
                int candidateArea = area(candidate);
                if (candidateArea > 0 && candidateArea < 256) strictPartialContact = true;
            }
            helper.assertTrue(strictPartialContact,
                    role + " did not expose any strict partial boundary contact");
        }

        BlockState bottomSlab = primary(Role.HORIZONTAL_SLAB).physicalBlock().defaultBlockState()
                .setValue(SlabBlock.TYPE, SlabType.BOTTOM);
        BlockState oneLayer = primary(Role.LAYER).physicalBlock().defaultBlockState()
                .setValue(BgeLayerBlock.FACING, Direction.UP)
                .setValue(BgeLayerBlock.LAYERS, 1);
        assertRegions(helper, bottomSlab, oneLayer, Direction.EAST, 16,
                List.of(new Rect16(0, 4, 0, 16)), "partial/partial intersection");

        BlockState insetVertical = primary(Role.VERTICAL_SLAB).physicalBlock().defaultBlockState()
                .setValue(VerticalSlabBlock.FACING, Direction.EAST);
        helper.assertTrue(regions(full, insetVertical, Direction.EAST, 16, FULL).isEmpty(),
                "Adjacent but physically separated Vertical Slab produced a cull region");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void allStairStatesReachTheGenericResolver(GameTestHelper helper) {
        Block stair = primary(Role.STAIR).physicalBlock();
        BlockState full = Blocks.GLASS.defaultBlockState();
        int checked = 0;
        int partialContacts = 0;
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            for (Half half : Half.values()) {
                for (StairsShape shape : StairsShape.values()) {
                    BlockState state = stair.defaultBlockState()
                            .setValue(StairBlock.FACING, facing)
                            .setValue(StairBlock.HALF, half)
                            .setValue(StairBlock.SHAPE, shape);
                    SurfaceModel model = binding(state).surfaceModel(state);
                    helper.assertTrue(model.supported(), "Unsupported glass Stair state " + state);
                    List<Rect16> regions = regions(full, state, Direction.EAST, 16, FULL);
                    helper.assertTrue(regions.stream().allMatch(FULL::covers),
                            "Stair contact escaped its receiver face: " + state + " " + regions);
                    int area = area(regions);
                    if (area > 0 && area < 256) partialContacts++;
                    checked++;
                }
            }
        }
        helper.assertTrue(checked == 40 && partialContacts > 0,
                "Generic Stair matrix was incomplete: " + checked + "/" + partialContacts);
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void wallLowTallPostAndAllStatesUseExactPatches(GameTestHelper helper) {
        Block wall = primary(Role.WALL).physicalBlock();
        BlockState full = Blocks.GLASS.defaultBlockState();
        BlockState lowWest = wallState(wall, true, WallSide.NONE, WallSide.NONE,
                WallSide.NONE, WallSide.LOW);
        BlockState tallWest = lowWest.setValue(WallBlock.WEST, WallSide.TALL);
        assertRegions(helper, full, lowWest, Direction.EAST, 16,
                List.of(new Rect16(0, 14, 5, 11)), "LOW Wall arm");
        assertRegions(helper, full, tallWest, Direction.EAST, 16,
                List.of(new Rect16(0, 16, 5, 11)), "TALL Wall arm");

        BlockState post = wallState(wall, true, WallSide.NONE, WallSide.NONE,
                WallSide.NONE, WallSide.NONE);
        assertRegions(helper, full, post, Direction.DOWN, 0,
                List.of(new Rect16(4, 12, 4, 12)), "Wall post top");

        int checked = 0;
        for (boolean hasPost : new boolean[] {false, true}) {
            for (WallSide north : WallSide.values()) for (WallSide east : WallSide.values())
                for (WallSide south : WallSide.values()) for (WallSide west : WallSide.values()) {
                    if (!hasPost && north == WallSide.NONE && east == WallSide.NONE
                            && south == WallSide.NONE && west == WallSide.NONE) continue;
                    BlockState state = wallState(wall, hasPost, north, east, south, west);
                    helper.assertTrue(binding(state).surfaceModel(state).supported(),
                            "Unsupported glass Wall state " + state);
                    List<Rect16> regions = regions(full, state, Direction.EAST, 16, FULL);
                    helper.assertTrue(regions.stream().allMatch(FULL::covers),
                            "Wall contact escaped its receiver face: " + state + " " + regions);
                    checked++;
                }
        }
        helper.assertTrue(checked == 161, "Expected 161 nonempty Wall states, got " + checked);
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void overlapUnionFeedsExactDeterministicSubtraction(GameTestHelper helper) {
        Block wall = primary(Role.WALL).physicalBlock();
        BlockState multiArm = wallState(wall, true, WallSide.TALL, WallSide.TALL,
                WallSide.TALL, WallSide.TALL);
        List<Rect16> contributions = regions(Blocks.GLASS.defaultBlockState(), multiArm,
                Direction.DOWN, 0, FULL);
        helper.assertTrue(contributions.size() > 1,
                "Compound Wall did not expose multiple independent provider patches: " + contributions);
        List<Rect16> visible = RectSubtraction.subtract(FULL, contributions);
        helper.assertTrue(visible.stream().distinct().count() == visible.size()
                        && area(visible) + unionArea(contributions) == 256,
                "Compound subtraction duplicated or lost face area: " + visible);
        helper.succeed();
    }

    private static List<Rect16> regions(BlockState source, BlockState neighbor,
            Direction normal, int plane, Rect16 bounds) {
        Direction.Axis[] axes = inPlaneAxes(normal.getAxis());
        return SurfaceOverlapResolver.cullRegions(source, neighbor, normal, plane,
                axes[0], axes[1], bounds);
    }

    private static void assertRegions(GameTestHelper helper, BlockState source,
            BlockState neighbor, Direction normal, int plane, List<Rect16> expected, String label) {
        List<Rect16> actual = regions(source, neighbor, normal, plane, FULL);
        helper.assertTrue(actual.equals(expected), label + " regions differ: " + actual);
    }

    private static Binding primary(Role role) {
        NibaruMaterialProfile glass = NibaruMaterialProfiles.fromBlock(Blocks.GLASS).orElseThrow();
        return BgeMaterialBindings.all().stream()
                .filter(binding -> binding.materialProfile().orElse(null) == glass
                        && binding.role() == role
                        && binding.ownership() == BgeMaterialBindings.Ownership.PRIMARY)
                .findFirst().orElseThrow();
    }

    private static Binding binding(BlockState state) {
        return BgeMaterialBindings.fromBlock(state.getBlock()).orElseThrow();
    }

    private static BlockState wallState(Block wall, boolean post, WallSide north, WallSide east,
            WallSide south, WallSide west) {
        return wall.defaultBlockState().setValue(WallBlock.UP, post)
                .setValue(WallBlock.NORTH, north).setValue(WallBlock.EAST, east)
                .setValue(WallBlock.SOUTH, south).setValue(WallBlock.WEST, west);
    }

    private static Direction.Axis[] inPlaneAxes(Direction.Axis normal) {
        return switch (normal) {
            case X -> new Direction.Axis[] {Direction.Axis.Y, Direction.Axis.Z};
            case Y -> new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Z};
            case Z -> new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Y};
        };
    }

    private static int area(List<Rect16> rectangles) {
        return rectangles.stream().mapToInt(rectangle ->
                (rectangle.uMax() - rectangle.uMin())
                        * (rectangle.vMax() - rectangle.vMin())).sum();
    }

    private static int unionArea(List<Rect16> rectangles) {
        return 256 - area(RectSubtraction.subtract(FULL, rectangles));
    }

    @Override
    public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
        method.invoke(this, helper);
    }
}
