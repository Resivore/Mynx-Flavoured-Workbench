package games.twinhead.moreslabsstairsandwalls.block.dirt;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.level.Level;

/** Geometry-neutral Dirt Path survival and reversion semantics. */
public final class PathSemantics {
    public static final int CONVERSION_DELAY = 1;

    private PathSemantics() {}

    public static boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (state.getBlock() instanceof PathGeometry geometry
                && !geometry.pathSurfaceRequiresClearAbove(state)) return true;
        BlockState above = level.getBlockState(pos.above());
        return !above.isRedstoneConductor(level, pos) || above.getBlock() instanceof FenceGateBlock;
    }

    public static void scheduleConversionIfNeeded(BlockState state, LevelReader level,
            ScheduledTickAccess ticks, BlockPos pos, Direction changedDirection) {
        if (changedDirection == Direction.UP && !canSurvive(state, level, pos)) {
            ticks.scheduleTick(pos, state.getBlock(), CONVERSION_DELAY);
        }
    }

    public static BlockState placementState(BlockState pathState, BlockState dirtState,
            LevelAccessor level, BlockPos pos) {
        if (canSurvive(pathState, level, pos)) return pathState;
        BlockState target = copySharedProperties(pathState, dirtState);
        return Block.pushEntitiesUp(pathState, target, level, pos);
    }

    public static void convert(ServerLevel level, BlockPos pos, BlockState pathState, BlockState dirtState) {
        BlockState target = copySharedProperties(pathState, dirtState);
        level.setBlockAndUpdate(pos, Block.pushEntitiesUp(pathState, target, level, pos));
    }

    public static void revertIfObstructed(ServerLevel level, BlockPos pos, BlockState pathState,
            BlockState dirtState) {
        if (!canSurvive(pathState, level, pos)) convert(level, pos, pathState, dirtState);
    }

    public static BlockState copySharedProperties(BlockState source, BlockState target) {
        BlockState result = target;
        for (Property<?> property : source.getProperties()) {
            if (target.hasProperty(property)) result = copy(source, result, property);
        }
        return result;
    }

    public static InteractionResult flatten(ItemStack stack, BlockState source, BlockState pathTarget,
            Level level, BlockPos pos, Player player, InteractionHand hand) {
        if (!(stack.getItem() instanceof ShovelItem) || !canSurvive(pathTarget, level, pos)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!level.isClientSide()) {
            level.setBlockAndUpdate(pos, copySharedProperties(source, pathTarget));
            stack.hurtAndBreak(1, player,
                    hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        } else {
            level.playSound(player, pos, SoundEvents.SHOVEL_FLATTEN, SoundSource.BLOCKS, 1.0f, 1.0f);
        }
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
    }

    private static <T extends Comparable<T>> BlockState copy(BlockState source, BlockState target,
            Property<T> property) {
        return target.setValue(property, source.getValue(property));
    }
}
