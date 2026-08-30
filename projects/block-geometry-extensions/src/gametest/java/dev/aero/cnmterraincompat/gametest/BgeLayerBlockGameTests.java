package dev.aero.cnmterraincompat.gametest;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import dev.aero.cnmterraincompat.BgeLayerBlock;
import dev.aero.cnmterraincompat.NibaruProviderAdapter;
import games.twinhead.moreslabsstairsandwalls.api.material.DerivedGeometrySupport;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfiles;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.lang.reflect.Method;

/** Focused physical/state regression coverage for the BGE-owned Layer geometry. */
public final class BgeLayerBlockGameTests implements CustomTestMethodInvoker {
    private static final BlockPos TEST_POS = new BlockPos(1, 1, 1);

    @GameTest(maxTicks = 40)
    public void exactStateSpaceOutlineAndCollision(GameTestHelper helper) {
        BgeLayerBlock layer = ordinaryLayer();
        BlockState defaultState = layer.defaultBlockState();
        helper.assertTrue(defaultState.getValue(BgeLayerBlock.FACING) == Direction.UP
                        && defaultState.getValue(BgeLayerBlock.LAYERS) == 1
                        && !defaultState.getValue(BgeLayerBlock.DOUBLE)
                        && !defaultState.getValue(BgeLayerBlock.WATERLOGGED),
                "Layer default state is not facing=up,layers=1,double=false,waterlogged=false: "
                        + defaultState);
        helper.assertTrue(layer.getStateDefinition().getProperties().size() == 4
                        && layer.getStateDefinition().getPossibleStates().size() == 96,
                "Ordinary Layer state space must include the exact CNM combined-geometry marker");

        BlockPos absolute = helper.absolutePos(TEST_POS);
        CollisionContext context = CollisionContext.empty();
        int checked = 0;
        for (Direction facing : Direction.values()) {
            for (int layers = 1; layers <= 4; layers++) {
                BlockState state = defaultState
                        .setValue(BgeLayerBlock.FACING, facing)
                        .setValue(BgeLayerBlock.LAYERS, layers)
                        .setValue(BgeLayerBlock.DOUBLE, layers > 1);
                AABB expected = expectedBounds(facing, layers);
                assertBounds(helper, expected,
                        state.getShape(helper.getLevel(), absolute, context).bounds(),
                        "outline " + facing + " layers=" + layers);
                assertBounds(helper, expected,
                        state.getCollisionShape(helper.getLevel(), absolute, context).bounds(),
                        "collision " + facing + " layers=" + layers);
                checked++;
            }
        }

        helper.assertTrue(checked == 24, "Expected 24 Layer geometry states, checked " + checked);
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void rotationWaterAndFullStateAreStable(GameTestHelper helper) {
        BgeLayerBlock layer = ordinaryLayer();
        BlockState partial = layer.defaultBlockState()
                .setValue(BgeLayerBlock.FACING, Direction.NORTH)
                .setValue(BgeLayerBlock.LAYERS, 3)
                .setValue(BgeLayerBlock.DOUBLE, true)
                .setValue(BgeLayerBlock.WATERLOGGED, true);

        BlockState rotated = layer.rotate(partial, Rotation.CLOCKWISE_90);
        helper.assertTrue(rotated.getValue(BgeLayerBlock.FACING) == Direction.EAST
                        && rotated.getValue(BgeLayerBlock.LAYERS) == 3
                        && rotated.getValue(BgeLayerBlock.DOUBLE)
                        && rotated.getValue(BgeLayerBlock.WATERLOGGED),
                "Layer rotation changed non-facing state: " + rotated);
        BlockState mirrored = rotated.mirror(Mirror.FRONT_BACK);
        helper.assertTrue(mirrored.getValue(BgeLayerBlock.FACING) == Direction.WEST
                        && mirrored.getValue(BgeLayerBlock.LAYERS) == 3
                        && mirrored.getValue(BgeLayerBlock.DOUBLE)
                        && mirrored.getValue(BgeLayerBlock.WATERLOGGED),
                "Layer mirror changed non-facing state: " + mirrored);
        helper.assertTrue(partial.getFluidState().getType() == Fluids.WATER,
                "Waterlogged partial Layer does not expose source water");

        BlockPos partialPos = new BlockPos(1, 1, 1);
        BlockPos partialAbsolute = helper.absolutePos(partialPos);
        BlockState dryPartial = partial.setValue(BgeLayerBlock.WATERLOGGED, false);
        helper.setBlock(partialPos, dryPartial);
        helper.assertTrue(layer.canPlaceLiquid(null, helper.getLevel(), partialAbsolute,
                        dryPartial, Fluids.WATER),
                "Partial Layer rejected water");
        helper.assertTrue(layer.placeLiquid(helper.getLevel(), partialAbsolute,
                        dryPartial, Fluids.WATER.getSource(false)),
                "Partial Layer failed to accept water");
        helper.assertTrue(helper.getBlockState(partialPos).getValue(BgeLayerBlock.WATERLOGGED),
                "Accepted water did not persist in the partial Layer state");

        BlockPos fullPos = new BlockPos(2, 1, 1);
        BlockPos fullAbsolute = helper.absolutePos(fullPos);
        BlockState full = layer.defaultBlockState()
                .setValue(BgeLayerBlock.FACING, Direction.WEST)
                .setValue(BgeLayerBlock.LAYERS, 4)
                .setValue(BgeLayerBlock.DOUBLE, true)
                .setValue(BgeLayerBlock.WATERLOGGED, false);
        helper.setBlock(fullPos, full);
        helper.assertTrue(!layer.canPlaceLiquid(null, helper.getLevel(), fullAbsolute, full, Fluids.WATER)
                        && !layer.placeLiquid(helper.getLevel(), fullAbsolute,
                                full, Fluids.WATER.getSource(false)),
                "Four-layer state accepted water");
        helper.assertTrue(helper.getBlockState(fullPos).equals(full)
                        && helper.getBlockState(fullPos).getBlock() == layer,
                "Four-layer state collapsed to another block or changed properties");

        assertCodecRoundTrip(helper, partial, "waterlogged three-layer state");
        assertCodecRoundTrip(helper, full, "dry four-layer state");
        helper.succeed();
    }

    private static BgeLayerBlock ordinaryLayer() {
        NibaruMaterialProfile stone = NibaruMaterialProfiles.all().stream()
                .filter(profile -> profile.canonicalParentId().equals(Identifier.parse("minecraft:stone")))
                .findFirst().orElseThrow(() -> new IllegalStateException("Missing stone material profile"));
        return (BgeLayerBlock) NibaruProviderAdapter.derived(stone, DerivedGeometrySupport.Geometry.LAYER)
                .orElseThrow(() -> new IllegalStateException("Missing generated stone Layer"));
    }

    private static AABB expectedBounds(Direction facing, int layers) {
        double depth = layers / 4.0;
        return switch (facing) {
            case UP -> new AABB(0, 0, 0, 1, depth, 1);
            case DOWN -> new AABB(0, 1 - depth, 0, 1, 1, 1);
            case NORTH -> new AABB(0, 0, 1 - depth, 1, 1, 1);
            case SOUTH -> new AABB(0, 0, 0, 1, 1, depth);
            case EAST -> new AABB(0, 0, 0, depth, 1, 1);
            case WEST -> new AABB(1 - depth, 0, 0, 1, 1, 1);
        };
    }

    private static void assertBounds(GameTestHelper helper, AABB expected, AABB actual, String label) {
        helper.assertTrue(expected.minX == actual.minX && expected.minY == actual.minY
                        && expected.minZ == actual.minZ && expected.maxX == actual.maxX
                        && expected.maxY == actual.maxY && expected.maxZ == actual.maxZ,
                "Wrong Layer " + label + ": expected=" + expected + ", actual=" + actual);
    }

    private static void assertCodecRoundTrip(GameTestHelper helper, BlockState state, String label) {
        JsonElement encoded = BlockState.CODEC.encodeStart(JsonOps.INSTANCE, state).result()
                .orElseThrow(() -> new IllegalStateException("Could not encode " + label));
        BlockState decoded = BlockState.CODEC.parse(JsonOps.INSTANCE, encoded).result()
                .orElseThrow(() -> new IllegalStateException("Could not decode " + label));
        helper.assertTrue(decoded.equals(state),
                "Layer state changed across codec persistence for " + label
                        + ": encoded=" + encoded + ", decoded=" + decoded);
    }

    @Override
    public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
        method.invoke(this, helper);
    }
}
