package com.crispytwig.naturalist.server.block;

import com.crispytwig.naturalist.server.entity.mob.Alligator;
import com.crispytwig.naturalist.registry.NaturalistEntityTypes;
import com.crispytwig.naturalist.registry.NaturalistSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class AlligatorEggBlock extends NaturalistEggBlock {
    public AlligatorEggBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected SoundEvent breakSound() {
        return NaturalistSoundEvents.GATOR_EGG_BREAK.get();
    }

    @Override
    protected SoundEvent crackSound() {
        return NaturalistSoundEvents.GATOR_EGG_CRACK.get();
    }

    @Override
    protected SoundEvent hatchSound() {
        return NaturalistSoundEvents.GATOR_EGG_HATCH.get();
    }

    @Override
    protected boolean isParentSpecies(Entity entity) {
        return entity instanceof Alligator;
    }

    @Override
    protected boolean shouldUpdateHatchLevel(ServerLevel level, BlockPos pos, RandomSource random) {
        return shouldUpdateAtNaturalistDawn(level, pos);
    }

    @Override
    protected void spawnHatchlings(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        for (int index = 0; index < state.getValue(EGGS); ++index) {
            level.levelEvent(2001, pos, Block.getId(state));
            spawnBaby(level, NaturalistEntityTypes.ALLIGATOR.get(), pos,
                    0.3D + index * 0.2D, 0.3D, 0.0F, baby -> {
                    });
        }
    }
}
