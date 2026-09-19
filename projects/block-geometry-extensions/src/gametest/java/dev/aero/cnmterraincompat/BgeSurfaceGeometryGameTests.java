package dev.aero.cnmterraincompat;

import dev.aero.cnmterraincompat.BgeMaterialBindings.Binding;
import dev.aero.cnmterraincompat.BgeMaterialBindings.Topology;
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
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

        Block pathStairBlock = path.effectiveStairSource().orElseThrow();
        BlockState pathStair = pathStairBlock.defaultBlockState()
                .setValue(StairBlock.FACING, Direction.EAST)
                .setValue(StairBlock.HALF, Half.BOTTOM)
                .setValue(StairBlock.SHAPE, StairsShape.STRAIGHT);
        assertPatch(helper, model(pathStair), Direction.UP, 7, 0, 8, 0, 16,
                PlaneRelation.TERRAIN_HEIGHT_INSET, "Path Stair tread");
        assertPatch(helper, model(pathStair), Direction.UP, 15, 8, 16, 0, 16,
                PlaneRelation.TERRAIN_HEIGHT_INSET, "Path Stair upper tread");

        Block pathWallBlock = path.nativeWall().orElseThrow();
        BlockState pathPost = pathWallBlock.defaultBlockState()
                .setValue(WallBlock.UP, true)
                .setValue(WallBlock.NORTH, WallSide.NONE)
                .setValue(WallBlock.EAST, WallSide.NONE)
                .setValue(WallBlock.SOUTH, WallSide.NONE)
                .setValue(WallBlock.WEST, WallSide.NONE);
        assertPatch(helper, model(pathPost), Direction.UP, 15, 4, 12, 4, 12,
                PlaneRelation.TERRAIN_HEIGHT_INSET, "Path Wall post top");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void stairStateSpaceIsCompleteDeterministicAndUnionTiled(GameTestHelper helper) {
        Block stair = profile(Blocks.STONE).effectiveStairSource().orElseThrow();
        int checked = 0;
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            for (Half half : Half.values()) {
                for (StairsShape shape : StairsShape.values()) {
                    BlockState state = stair.defaultBlockState()
                            .setValue(StairBlock.FACING, facing)
                            .setValue(StairBlock.HALF, half)
                            .setValue(StairBlock.SHAPE, shape);
                    SurfaceModel first = model(state);
                    SurfaceModel second = model(state);
                    helper.assertTrue(first.supported() && !first.patches().isEmpty(),
                            "Unsupported Stair state " + facing + "/" + half + "/" + shape);
                    helper.assertTrue(first.patches().equals(second.patches()),
                            "Non-deterministic Stair patches " + facing + "/" + half + "/" + shape);
                    assertNoCoplanarOverlap(helper, first,
                            "Stair " + facing + "/" + half + "/" + shape);
                    checked++;
                }
            }
        }
        helper.assertTrue(checked == 40, "Expected all 40 Stair topology states, got " + checked);

        BlockState eastStraight = stair.defaultBlockState()
                .setValue(StairBlock.FACING, Direction.EAST)
                .setValue(StairBlock.HALF, Half.BOTTOM)
                .setValue(StairBlock.SHAPE, StairsShape.STRAIGHT);
        SurfaceModel straight = model(eastStraight);
        assertPatch(helper, straight, Direction.UP, 8, 0, 8, 0, 16,
                PlaneRelation.EXACT, "east straight tread");
        assertPatch(helper, straight, Direction.UP, 16, 8, 16, 0, 16,
                PlaneRelation.EXACT, "east straight upper surface");
        assertPatch(helper, straight, Direction.WEST, 8, 8, 16, 0, 16,
                PlaneRelation.EXACT, "east straight riser");
        helper.assertTrue(straight.patches(Direction.DOWN).stream()
                        .noneMatch(patch -> patch.plane16() == 8),
                "Internal Stair member interface escaped as a downward patch: "
                        + straight.patches(Direction.DOWN));

        BlockState inner = eastStraight.setValue(StairBlock.SHAPE, StairsShape.INNER_RIGHT);
        BlockState outer = eastStraight.setValue(StairBlock.SHAPE, StairsShape.OUTER_RIGHT);
        assertPatch(helper, model(inner), Direction.UP, 16, 0, 8, 8, 16,
                PlaneRelation.EXACT, "inner-right added corner");
        assertPatch(helper, model(outer), Direction.UP, 8, 0, 8, 0, 8,
                PlaneRelation.EXACT, "outer-right retained outside tread");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void stairRotationsAndHalfMirrorsTransformWholeSurfaceSets(GameTestHelper helper) {
        Block stair = profile(Blocks.STONE).effectiveStairSource().orElseThrow();
        for (StairsShape shape : StairsShape.values()) {
            BlockState eastBottom = stair.defaultBlockState()
                    .setValue(StairBlock.FACING, Direction.EAST)
                    .setValue(StairBlock.HALF, Half.BOTTOM)
                    .setValue(StairBlock.SHAPE, shape);
            BlockState southBottom = eastBottom.setValue(StairBlock.FACING, Direction.SOUTH);
            helper.assertTrue(transform(model(eastBottom).patches(), Transform.CLOCKWISE)
                            .equals(Set.copyOf(model(southBottom).patches())),
                    "Stair rotation changed authoritative patches for " + shape);

            BlockState eastTop = eastBottom.setValue(StairBlock.HALF, Half.TOP);
            helper.assertTrue(transform(model(eastBottom).patches(), Transform.MIRROR_Y)
                            .equals(Set.copyOf(model(eastTop).patches())),
                    "Stair TOP/BOTTOM mirror changed authoritative patches for " + shape);
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void wallStateSpacePreservesPostsLowTallArmsAndUnions(GameTestHelper helper) {
        Block wall = profile(Blocks.STONE).nativeWall().orElseThrow();
        int checked = 0;
        for (boolean post : new boolean[] {false, true}) {
            for (WallSide north : WallSide.values()) for (WallSide east : WallSide.values())
                for (WallSide south : WallSide.values()) for (WallSide west : WallSide.values()) {
                    if (!post && north == WallSide.NONE && east == WallSide.NONE
                            && south == WallSide.NONE && west == WallSide.NONE) continue;
                    BlockState state = wallState(wall, post, north, east, south, west);
                    SurfaceModel first = model(state);
                    helper.assertTrue(first.supported() && !first.patches().isEmpty(),
                            "Unsupported nonempty Wall state " + state);
                    helper.assertTrue(first.patches().equals(model(state).patches()),
                            "Non-deterministic Wall patches " + state);
                    assertNoCoplanarOverlap(helper, first, "Wall " + state);
                    checked++;
                }
        }
        helper.assertTrue(checked == 161,
                "Expected all 161 nonempty post/connection Wall states, got " + checked);

        BlockState low = wallState(wall, true, WallSide.LOW, WallSide.NONE,
                WallSide.NONE, WallSide.NONE);
        BlockState tall = low.setValue(WallBlock.NORTH, WallSide.TALL);
        assertPatch(helper, model(low), Direction.UP, 14, 5, 11, 0, 4,
                PlaneRelation.EXACT, "LOW Wall arm top");
        helper.assertTrue(model(low).patches(Direction.UP).stream()
                        .noneMatch(patch -> patch.plane16() == 16
                                && patch.uMin16() == 5 && patch.uMax16() == 11
                                && patch.vMin16() == 0 && patch.vMax16() == 4),
                "LOW Wall arm incorrectly reached TALL height");
        assertPatch(helper, model(tall), Direction.UP, 16, 5, 11, 0, 4,
                PlaneRelation.EXACT, "TALL Wall arm top");
        helper.assertTrue(model(low).patches(Direction.SOUTH).stream()
                        .noneMatch(patch -> patch.plane16() == 8
                                && patch.uMin16() < 11 && patch.uMax16() > 5),
                "Internal post/arm junction escaped as an exposed Wall patch");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void wallRotationsTransformWholeSurfaceSets(GameTestHelper helper) {
        Block wall = profile(Blocks.STONE).nativeWall().orElseThrow();
        BlockState original = wallState(wall, true, WallSide.LOW, WallSide.TALL,
                WallSide.NONE, WallSide.LOW);
        BlockState rotated = wallState(wall, true, WallSide.LOW, WallSide.LOW,
                WallSide.TALL, WallSide.NONE);
        helper.assertTrue(transform(model(original).patches(), Transform.CLOCKWISE)
                        .equals(Set.copyOf(model(rotated).patches())),
                "Wall rotation changed authoritative patches");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void everyBoundTopologyIncludingStairsAndWallsHasSurfaceAuthority(GameTestHelper helper) {
        EnumSet<Topology> seen = EnumSet.noneOf(Topology.class);
        int stairs = 0;
        int walls = 0;
        for (Binding binding : BgeMaterialBindings.all()) {
            SurfaceModel surface = binding.surfaceModel(binding.physicalBlock().defaultBlockState());
            helper.assertTrue(surface.supported() && !surface.patches().isEmpty(),
                    "Binding lacks surface authority: " + binding.topology() + " "
                            + binding.physicalBlock());
            assertNoCoplanarOverlap(helper, surface, "Binding " + binding.physicalBlock());
            seen.add(binding.topology());
            if (binding.topology() == Topology.STAIR) stairs++;
            if (binding.topology() == Topology.WALL) walls++;
        }
        helper.assertTrue(seen.equals(EnumSet.allOf(Topology.class)),
                "Not every BGE topology has a bound surface provider: " + seen);
        helper.assertTrue(stairs > 0 && walls > 0,
                "Catalog-wide Stair/Wall binding audit did not run: " + stairs + "/" + walls);
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void canonicalFacesStayInTheWorldMaterialFrame(GameTestHelper helper) {
        for (Block canonical : List.of(Blocks.GRASS_BLOCK, Blocks.OAK_LOG,
                Blocks.GLAZED_TERRACOTTA.pick(DyeColor.BLACK))) {
            NibaruMaterialProfile profile = profile(canonical);
            BlockState stair = profile.effectiveStairSource().orElseThrow().defaultBlockState()
                    .setValue(StairBlock.FACING, Direction.WEST)
                    .setValue(StairBlock.HALF, Half.TOP)
                    .setValue(StairBlock.SHAPE, StairsShape.INNER_LEFT);
            BlockState wall = wallState(profile.nativeWall().orElseThrow(), true,
                    WallSide.LOW, WallSide.TALL, WallSide.NONE, WallSide.LOW);
            assertWorldCanonicalFaces(helper, model(stair), canonical + " Stair");
            assertWorldCanonicalFaces(helper, model(wall), canonical + " Wall");
        }
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

    private static void assertWorldCanonicalFaces(
            GameTestHelper helper, SurfaceModel model, String label) {
        EnumSet<Direction> normals = EnumSet.noneOf(Direction.class);
        for (SurfacePatch patch : model.patches()) {
            normals.add(patch.normal());
            helper.assertTrue(patch.canonicalFace() == patch.normal(),
                    label + " left the canonical world material frame: " + patch);
        }
        helper.assertTrue(normals.contains(Direction.UP) && normals.contains(Direction.DOWN)
                        && normals.stream().anyMatch(direction -> direction.getAxis().isHorizontal()),
                label + " did not exercise TOP/SIDE/BOTTOM semantics: " + normals);
    }

    private static BlockState wallState(Block wall, boolean post, WallSide north, WallSide east,
            WallSide south, WallSide west) {
        return wall.defaultBlockState().setValue(WallBlock.UP, post)
                .setValue(WallBlock.NORTH, north).setValue(WallBlock.EAST, east)
                .setValue(WallBlock.SOUTH, south).setValue(WallBlock.WEST, west);
    }

    private static void assertNoCoplanarOverlap(
            GameTestHelper helper, SurfaceModel model, String label) {
        List<SurfacePatch> patches = model.patches();
        helper.assertTrue(new HashSet<>(patches).size() == patches.size(),
                label + " contains duplicate patches: " + patches);
        for (int first = 0; first < patches.size(); first++) {
            for (int second = first + 1; second < patches.size(); second++) {
                SurfacePatch left = patches.get(first);
                SurfacePatch right = patches.get(second);
                if (left.normal() != right.normal() || left.plane16() != right.plane16()
                        || left.uAxis() != right.uAxis() || left.vAxis() != right.vAxis()) continue;
                boolean overlaps = Math.max(left.uMin16(), right.uMin16())
                        < Math.min(left.uMax16(), right.uMax16())
                        && Math.max(left.vMin16(), right.vMin16())
                        < Math.min(left.vMax16(), right.vMax16());
                helper.assertTrue(!overlaps,
                        label + " contains overlapping coplanar patches: " + left + " / " + right);
            }
        }
    }

    private enum Transform { CLOCKWISE, MIRROR_Y }

    private static Set<SurfacePatch> transform(List<SurfacePatch> patches, Transform transform) {
        Set<SurfacePatch> result = new HashSet<>();
        for (SurfacePatch patch : patches) result.add(transform(patch, transform));
        return Set.copyOf(result);
    }

    private static SurfacePatch transform(SurfacePatch patch, Transform transform) {
        int[] bounds = bounds(patch);
        Direction normal = patch.normal();
        Direction canonical = patch.canonicalFace();
        if (transform == Transform.CLOCKWISE) {
            bounds = new int[] {16 - bounds[5], bounds[1], bounds[0],
                    16 - bounds[2], bounds[4], bounds[3]};
            if (normal.getAxis().isHorizontal()) normal = normal.getClockWise();
            if (canonical.getAxis().isHorizontal()) canonical = canonical.getClockWise();
        } else {
            bounds = new int[] {bounds[0], 16 - bounds[4], bounds[2],
                    bounds[3], 16 - bounds[1], bounds[5]};
            if (normal.getAxis().isVertical()) normal = normal.getOpposite();
            if (canonical.getAxis().isVertical()) canonical = canonical.getOpposite();
        }
        return patch(normal, canonical, patch.planeRelation(), bounds);
    }

    private static int[] bounds(SurfacePatch patch) {
        int[] result = new int[6];
        setInterval(result, patch.uAxis(), patch.uMin16(), patch.uMax16());
        setInterval(result, patch.vAxis(), patch.vMin16(), patch.vMax16());
        setInterval(result, patch.normal().getAxis(), patch.plane16(), patch.plane16());
        return result;
    }

    private static void setInterval(int[] bounds, Direction.Axis axis, int min, int max) {
        int offset = axisIndex(axis);
        bounds[offset] = min;
        bounds[offset + 3] = max;
    }

    private static SurfacePatch patch(Direction normal, Direction canonical,
            PlaneRelation relation, int[] bounds) {
        Direction.Axis u = normal.getAxis() == Direction.Axis.X
                ? Direction.Axis.Y : Direction.Axis.X;
        Direction.Axis v = normal.getAxis() == Direction.Axis.Z
                ? Direction.Axis.Y : Direction.Axis.Z;
        int normalIndex = axisIndex(normal.getAxis());
        int uIndex = axisIndex(u);
        int vIndex = axisIndex(v);
        return new SurfacePatch(normal, bounds[normalIndex], u,
                bounds[uIndex], bounds[uIndex + 3], v,
                bounds[vIndex], bounds[vIndex + 3], canonical, relation);
    }

    private static int axisIndex(Direction.Axis axis) {
        return switch (axis) {
            case X -> 0;
            case Y -> 1;
            case Z -> 2;
        };
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
