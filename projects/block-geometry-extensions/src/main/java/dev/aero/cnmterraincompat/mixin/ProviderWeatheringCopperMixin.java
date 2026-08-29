package dev.aero.cnmterraincompat.mixin;

import dev.aero.cnmterraincompat.NibaruProviderAdapter;
import dev.tazer.clutternomore.common.blocks.WeatheringStepBlock;
import dev.tazer.clutternomore.common.blocks.WeatheringVerticalSlabBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChangeOverTimeBlock;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

import java.util.Optional;

/** Supplies the lifecycle callback missing from CNM's weathering geometry classes. */
@Mixin(value = {WeatheringVerticalSlabBlock.class, WeatheringStepBlock.class}, remap = false)
abstract class ProviderWeatheringCopperMixin implements ChangeOverTimeBlock<WeatheringCopper.WeatherState> {
    @Override
    public Optional<BlockState> getNext(BlockState state) {
        return NibaruProviderAdapter.nextOxidation((Block) (Object) this, state);
    }

    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        changeOverTime(state, level, pos, random);
    }

    public boolean isRandomlyTicking(BlockState state) {
        return NibaruProviderAdapter.copperAge((Block) (Object) this)
                != WeatheringCopper.WeatherState.OXIDIZED;
    }
}
