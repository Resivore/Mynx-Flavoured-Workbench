package com.starfish_studios.bbb.porting;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.lang.reflect.Method;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        assertTrue(climbMixin.contains("LivingEntity self = (LivingEntity) (Object) this"));
        assertTrue(climbMixin.contains("RopeBlock.isVerticalRope(self.getInBlockState())"));
        assertTrue(climbMixin.contains("lastClimbablePos = Optional.of(self.blockPosition())"));
        assertFalse(climbMixin.contains("@Shadow public abstract BlockState getInBlockState()"));
        assertFalse(climbMixin.contains("@Shadow public abstract BlockPos blockPosition()"));
        assertFalse(climbMixin.contains("BlockTags.CLIMBABLE"),
                "A global tag would make horizontal rope climbable too");
        assertTrue(mixins.contains("LivingEntityRopeClimbMixin"));
    }

    @Test
    void climbMixinShadowsOnlyTheActualLivingEntityMemberInMinecraft262() throws Exception {
        String climbMixin = read("mixin/LivingEntityRopeClimbMixin.java");

        // These reflection checks run against the resolved 26.2 development
        // namespace, preventing a source-only contract from accepting an
        // inherited Entity method as a LivingEntity @Shadow again.
        assertEquals(Entity.class, Entity.class.getMethod("getInBlockState").getDeclaringClass());
        assertEquals(Entity.class, Entity.class.getMethod("blockPosition").getDeclaringClass());
        assertEquals(LivingEntity.class,
                LivingEntity.class.getDeclaredMethod("onClimbable").getDeclaringClass());
        assertEquals(LivingEntity.class,
                LivingEntity.class.getDeclaredField("lastClimbablePos").getDeclaringClass());
        assertEquals(Optional.class, LivingEntity.class.getDeclaredField("lastClimbablePos").getType());
        assertEquals(BlockPos.class, Entity.class.getMethod("blockPosition").getReturnType());
        assertFalse(climbMixin.contains("@Shadow public abstract"),
                "Inherited public Entity methods must be called through self, never shadowed on LivingEntity");
        assertTrue(climbMixin.contains("@Shadow private Optional<BlockPos> lastClimbablePos"));
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
        assertTrue(rope.contains("return super.useItemOn(stack, state, level, pos, player, hand, hit)"));
        assertTrue(rope.contains("return super.useWithoutItem(state, level, pos, player, hit)"));
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

    @Test
    void emptyHandItemUseRetainsMinecraft262TryWithEmptyHandRouting() throws Exception {
        String rope = read("block/RopeBlock.java");

        // Minecraft 26.2's resolved BlockBehaviour default is
        // TRY_WITH_EMPTY_HAND. A constructor-free probe keeps this focused
        // JUnit regression independent of the game registry bootstrap.
        Method useItemOn = BlockBehaviour.class.getDeclaredMethod("useItemOn", ItemStack.class,
                net.minecraft.world.level.block.state.BlockState.class,
                net.minecraft.world.level.Level.class, BlockPos.class,
                net.minecraft.world.entity.player.Player.class,
                net.minecraft.world.InteractionHand.class,
                net.minecraft.world.phys.BlockHitResult.class);
        useItemOn.setAccessible(true);
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        Field unsafeField = unsafeClass.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        Object unsafe = unsafeField.get(null);
        BlockBehaviour probe = (BlockBehaviour) unsafeClass.getMethod("allocateInstance", Class.class)
                .invoke(unsafe, InteractionProbe.class);
        assertEquals(InteractionResult.TRY_WITH_EMPTY_HAND,
                useItemOn.invoke(probe, null, null, null, null, null, null, null));
        assertTrue(rope.contains("return super.useItemOn(stack, state, level, pos, player, hand, hit)"));
        assertFalse(rope.contains("return InteractionResult.PASS;"),
                "Rope fallbacks must retain default item-on/empty-hand routing");
    }

    @Test
    void payoutAddsHayBaleSoundOnlyAfterVanillaPlacementSucceeds() throws IOException {
        String rope = read("block/RopeBlock.java");

        assertTrue(rope.contains("if (!ropeItem.place(context).consumesAction())"));
        assertTrue(rope.contains("Blocks.HAY_BLOCK.defaultBlockState().getSoundType()"));
        assertTrue(rope.contains("hayBaleSound.getPlaceSound()"));
        assertTrue(rope.contains("(hayBaleSound.getVolume() + 1.0F) / 2.0F"));
        assertTrue(rope.contains("hayBaleSound.getPitch() * 0.8F"));
        assertTrue(rope.indexOf("if (!ropeItem.place(context).consumesAction())")
                        < rope.indexOf("hayBaleSound.getPlaceSound()"),
                "A failed payout must emit no Hay Bale placement sound");
    }

    private static String read(String relative) throws IOException {
        return Files.readString(JAVA.resolve(relative));
    }

    /** Never constructed: Unsafe allocates it only to invoke BlockBehaviour's default method. */
    private static final class InteractionProbe extends BlockBehaviour {
        private InteractionProbe() {
            super(null);
        }

        @Override
        protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.Block> codec() {
            return null;
        }

        @Override
        protected net.minecraft.world.level.block.Block asBlock() {
            return null;
        }

        @Override
        public net.minecraft.world.item.Item asItem() {
            return null;
        }
    }
}
