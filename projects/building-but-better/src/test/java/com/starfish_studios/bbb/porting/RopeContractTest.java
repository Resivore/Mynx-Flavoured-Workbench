package com.starfish_studios.bbb.porting;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Source-level regression contract for the narrow, state-aware BBB rope feature. */
final class RopeContractTest {
    private static final Path PROJECT = Path.of(System.getProperty("user.dir")).toAbsolutePath();
    private static final Path JAVA = PROJECT.resolve("src/main/java/com/starfish_studios/bbb");

    @Test
    void ropeKeepsItsSingleRegistryIdentityAndChainStateContract() throws IOException {
        String registry = read("registry/BBBContent.java");
        String rope = read("block/RopeBlock.java");

        assertTrue(registry.contains("registerBlock(\"rope\""));
        assertTrue(registry.contains("RopeBlock::new"));
        assertFalse(registry.contains("ChainBlock::new"));
        assertTrue(registry.contains("MUTABLE_BLOCKS.size() != 171 || MUTABLE_ITEMS.size() != 172"));
        assertTrue(rope.contains("extends ChainBlock"));
        assertTrue(rope.contains("state.getValue(AXIS) == Direction.Axis.Y"));
        assertFalse(rope.contains("createBlockStateDefinition"),
                "Rope must inherit ChainBlock's axis and waterlogged state definition unchanged");
        assertFalse(rope.contains("getShape("), "Rope must retain ChainBlock's collision/shape behavior");
    }

    @Test
    void onlyVerticalRopeIsAddedToMinecraftClimbingSemantics() throws IOException {
        String rope = read("block/RopeBlock.java");
        String climbMixin = read("mixin/LivingEntityRopeClimbMixin.java");
        String mixins = Files.readString(PROJECT.resolve("src/main/resources/bbb.mixins.json"));

        assertTrue(rope.contains("public static boolean isVerticalRope"));
        assertTrue(climbMixin.contains("RopeBlock.isVerticalRope(getInBlockState())"));
        assertTrue(climbMixin.contains("lastClimbablePos = Optional.of(blockPosition())"));
        assertFalse(climbMixin.contains("BlockTags.CLIMBABLE"),
                "A global tag would make horizontal rope climbable too");
        assertTrue(mixins.contains("LivingEntityRopeClimbMixin"));
    }

    @Test
    void payoutUsesTheBottomOfOnlyTheContiguousVerticalColumnAndVanillaPlacement() throws IOException {
        String rope = read("block/RopeBlock.java");

        assertTrue(rope.contains("BlockPos target = bottomOfColumn(level, clickedPos).below()"));
        assertTrue(rope.contains("while (bottom.getY() > level.getMinY())"));
        assertTrue(rope.contains("isVerticalRope(level.getBlockState(below))"));
        assertTrue(rope.contains("!level.isInWorldBounds(target)"));
        assertTrue(rope.contains("!level.getWorldBorder().isWithinBounds(target)"));
        assertTrue(rope.contains("!player.mayBuild() || !level.mayInteract(player, target)"));
        assertTrue(rope.contains("!fluid.isEmpty() && !fluid.is(Fluids.WATER)"));
        assertTrue(rope.contains("new BlockHitResult(Vec3.atCenterOf(target), Direction.UP, target, false)"));
        assertTrue(rope.contains("level.getBlockState(target).canBeReplaced(context)"));
        assertTrue(rope.contains("ropeItem.place(context).consumesAction()"));
        assertFalse(rope.contains("setBlock(target,"),
                "Payout must use BlockItem placement rather than force-writing targets");
    }

    @Test
    void mainHandInteractionConsumesOrReturnsExactlyOneAndDoesNotFallThrough() throws IOException {
        String rope = read("block/RopeBlock.java");

        assertTrue(rope.contains("hand != InteractionHand.MAIN_HAND"));
        assertTrue(rope.contains("player.isShiftKeyDown()"));
        assertTrue(rope.contains("stack.is(asItem())"));
        assertTrue(rope.contains("return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER"));
        assertTrue(rope.contains("player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()"));
        assertTrue(rope.contains("BlockPos bottom = bottomOfColumn(level, clickedPos)"));
        assertTrue(rope.contains("Blocks.WATER.defaultBlockState()"));
        assertTrue(rope.contains("Blocks.AIR.defaultBlockState()"));
        assertTrue(rope.contains("if (!player.isCreative())"));
        assertTrue(rope.contains("new ItemStack(asItem())"));
        assertTrue(rope.contains("if (!player.addItem(returned))"));
        assertTrue(rope.contains("player.drop(returned, false)"));
        assertTrue(rope.contains("GameEvent.BLOCK_DESTROY"));
        assertFalse(rope.contains("protected void attack("), "Ordinary mining must not cascade a rope column");
        assertFalse(rope.contains("playerDestroy("), "Ordinary mining must retain normal ChainBlock behavior");
    }

    private static String read(String relative) throws IOException {
        return Files.readString(JAVA.resolve(relative));
    }
}
