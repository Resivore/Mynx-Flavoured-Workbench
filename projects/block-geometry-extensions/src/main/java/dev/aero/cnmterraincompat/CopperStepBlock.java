package dev.aero.cnmterraincompat;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.ChangeOverTimeBlock;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

final class CopperStepBlock extends ProviderStepBlock
        implements ChangeOverTimeBlock<WeatheringCopper.WeatherState> {
    CopperStepBlock(BlockBehaviour.Properties properties) { super(properties.randomTicks()); }

    @Override public WeatheringCopper.WeatherState getAge() { return NibaruProviderAdapter.copperAge(this); }
    @Override public Optional<BlockState> getNext(BlockState state) { return NibaruProviderAdapter.nextOxidation(this, state); }
    @Override public float getChanceModifier() { return getAge() == WeatheringCopper.WeatherState.UNAFFECTED ? 0.75F : 1.0F; }
    @Override protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        changeOverTime(state, level, pos, random);
    }
    @Override public boolean isRandomlyTicking(BlockState state) {
        return getAge() != WeatheringCopper.WeatherState.OXIDIZED;
    }
}
