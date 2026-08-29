package dev.resivore.bedrockifynemosbonemealcompat.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Makes the audited Nemo flower-duplication policy deterministic when both
 * upstream mods contribute the same {@link BonemealableBlock} methods.
 *
 * <p>Priority 1100 makes this method owner authoritative over both
 * default-priority-1000 upstream mixins through Mixin's merged-method
 * conflict rule.</p>
 */
@Mixin(value = FlowerBlock.class, priority = 1100)
public abstract class FlowerBlockBonemealMixin implements BonemealableBlock {
    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        FlowerBlock.popResource(level, pos, new ItemStack(state.getBlock()));
    }
}
