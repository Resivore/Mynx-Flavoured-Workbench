package com.crispytwig.naturalist.server.block;

import com.crispytwig.naturalist.registry.NaturalistTags;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.TurtleEggBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * Shared 26.2 compatibility layer for Naturalist's turtle-egg-derived blocks.
 */
public abstract class NaturalistEggBlock extends TurtleEggBlock {
    protected NaturalistEggBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void randomTick(@NotNull BlockState state, @NotNull ServerLevel level, @NotNull BlockPos pos,
                           @NotNull RandomSource random) {
        if (!this.shouldUpdateHatchLevel(level, pos, random)) {
            return;
        }

        int hatchStage = state.getValue(HATCH);
        if (hatchStage < 2) {
            level.playSound(null, pos, this.crackSound(), SoundSource.BLOCKS,
                    0.7F, 0.9F + random.nextFloat() * 0.2F);
            level.setBlock(pos, state.setValue(HATCH, hatchStage + 1), 2);
            return;
        }

        level.playSound(null, pos, this.hatchSound(), SoundSource.BLOCKS,
                0.7F, 0.9F + random.nextFloat() * 0.2F);
        level.removeBlock(pos, false);
        this.spawnHatchlings(state, level, pos, random);
    }

    @Override
    public void onPlace(@NotNull BlockState state, Level level, @NotNull BlockPos pos,
                        @NotNull BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide()) {
            level.levelEvent(2005, pos, 0);
        }
    }

    @Override
    public void stepOn(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state,
                       @NotNull Entity entity) {
        if (!entity.isSteppingCarefully()) {
            this.destroyEgg(level, state, pos, entity, 100);
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    public void fallOn(@NotNull Level level, @NotNull BlockState state, @NotNull BlockPos pos,
                       @NotNull Entity entity, double fallDistance) {
        if (!(entity instanceof Zombie)) {
            this.destroyEgg(level, state, pos, entity, 3);
        }
        super.fallOn(level, state, pos, entity, fallDistance);
    }

    protected abstract SoundEvent breakSound();

    protected abstract SoundEvent crackSound();

    protected abstract SoundEvent hatchSound();

    protected abstract boolean isParentSpecies(Entity entity);

    protected abstract boolean shouldUpdateHatchLevel(ServerLevel level, BlockPos pos, RandomSource random);

    protected abstract void spawnHatchlings(BlockState state, ServerLevel level, BlockPos pos, RandomSource random);

    protected boolean supportsEggClusters() {
        return true;
    }

    protected void afterEggDestroyed(ServerLevel level, BlockPos pos, Entity entity) {
    }

    protected static boolean shouldUpdateAtNaturalistDawn(ServerLevel level, BlockPos pos) {
        float hatchChance = level.environmentAttributes()
                .getValue(EnvironmentAttributes.TURTLE_EGG_HATCH_CHANCE, pos);
        return hatchChance > 0.0F && level.getRandom().nextFloat() < hatchChance;
    }

    protected static <T extends AgeableMob> void spawnBaby(ServerLevel level, EntityType<T> entityType,
                                                            BlockPos pos, double xOffset, double zOffset,
                                                            float yRotation, Consumer<T> configure) {
        T baby = entityType.create(level, EntitySpawnReason.BREEDING);
        if (baby == null) {
            return;
        }

        configure.accept(baby);
        baby.setAge(-24000);
        baby.snapTo(pos.getX() + xOffset, pos.getY(), pos.getZ() + zOffset, yRotation, 0.0F);
        level.addFreshEntity(baby);
    }

    private void destroyEgg(Level level, BlockState state, BlockPos pos, Entity entity, int chance) {
        if (!(level instanceof ServerLevel serverLevel)
                || !state.is(this)
                || !this.canDestroyEgg(serverLevel, entity)
                || serverLevel.getRandom().nextInt(chance) != 0) {
            return;
        }

        serverLevel.playSound(null, pos, this.breakSound(), SoundSource.BLOCKS,
                0.7F, 0.9F + serverLevel.getRandom().nextFloat() * 0.2F);
        int eggCount = state.getValue(EGGS);
        if (this.supportsEggClusters() && eggCount > 1) {
            serverLevel.setBlock(pos, state.setValue(EGGS, eggCount - 1), 2);
            serverLevel.gameEvent(GameEvent.BLOCK_DESTROY, pos, GameEvent.Context.of(state));
            serverLevel.levelEvent(2001, pos, Block.getId(state));
        } else {
            serverLevel.destroyBlock(pos, false);
        }
        this.afterEggDestroyed(serverLevel, pos, entity);
    }

    private boolean canDestroyEgg(ServerLevel level, Entity entity) {
        if (this.isParentSpecies(entity) || entity.is(NaturalistTags.EntityTypes.SAFE_EGG_WALKERS)) {
            return false;
        }
        return entity instanceof LivingEntity
                && (entity instanceof Player || level.getGameRules().get(GameRules.MOB_GRIEFING));
    }
}
