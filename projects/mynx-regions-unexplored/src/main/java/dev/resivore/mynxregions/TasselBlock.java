package dev.resivore.mynxregions;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Preserves RU's unusual PASS-then-bonemeal loop without creating dispenser output. */
public final class TasselBlock extends DoublePlantBlock implements BonemealableBlock {
    public static final MapCodec<TasselBlock> CODEC = simpleCodec(TasselBlock::new);
    public TasselBlock(BlockBehaviour.Properties properties) { super(properties); }
    @Override public MapCodec<TasselBlock> codec() { return CODEC; }
    @Override protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                                     Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.is(Items.BONE_MEAL)) {
            if (!level.isClientSide()) Block.popResource(level, pos, new ItemStack(this));
            return InteractionResult.PASS;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }
    @Override protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) { return false; }
    @Override public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) { return true; }
    @Override public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) { return true; }
    @Override public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) { }
}
