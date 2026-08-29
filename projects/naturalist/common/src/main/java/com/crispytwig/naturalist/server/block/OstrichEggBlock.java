package com.crispytwig.naturalist.server.block;

import com.crispytwig.naturalist.server.entity.mob.Ostrich;
import com.crispytwig.naturalist.registry.NaturalistEntityTypes;
import com.crispytwig.naturalist.registry.NaturalistSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class OstrichEggBlock extends NaturalistEggBlock {
    private static final VoxelShape EGG_AABB = Block.box(5.0, 0.0, 5.0, 11.0, 8.0, 11.0);

    public OstrichEggBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected SoundEvent breakSound() {
        return NaturalistSoundEvents.OSTRICH_EGG_BREAK.get();
    }

    @Override
    protected SoundEvent crackSound() {
        return NaturalistSoundEvents.OSTRICH_EGG_CRACK.get();
    }

    @Override
    protected SoundEvent hatchSound() {
        return NaturalistSoundEvents.OSTRICH_EGG_HATCH.get();
    }

    @Override
    protected boolean isParentSpecies(Entity entity) {
        return entity instanceof Ostrich;
    }

    @Override
    protected boolean shouldUpdateHatchLevel(ServerLevel level, BlockPos pos, RandomSource random) {
        return random.nextInt(3) == 0;
    }

    @Override
    protected void spawnHatchlings(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        level.levelEvent(2001, pos, Block.getId(state));
        spawnBaby(level, NaturalistEntityTypes.OSTRICH.get(), pos, 0.3D, 0.3D, 0.0F, baby -> {
        });
    }

    @Override
    public @NotNull BlockState playerWillDestroy(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull Player player) {
        if (!level.isClientSide() && !player.getAbilities().instabuild) {
            this.angerNearbyOstriches(level, pos, player);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected boolean supportsEggClusters() {
        return false;
    }

    @Override
    protected void afterEggDestroyed(ServerLevel level, BlockPos pos, Entity entity) {
        if (entity instanceof Player player) {
            this.angerNearbyOstriches(level, pos, player);
        }
    }

    private void angerNearbyOstriches(@NotNull Level level, BlockPos pos, Player player) {
        for (Ostrich ostrich : level.getEntitiesOfClass(Ostrich.class, new AABB(pos).inflate(16.0, 8.0, 16.0),
                entity -> !entity.isBaby() && !entity.isTame() && entity.owns(pos))) {
            ostrich.onOwnedEggDestroyed(pos, player);
        }
    }

    @Override
    public boolean canBeReplaced(@NotNull BlockState state, @NotNull BlockPlaceContext useContext) {
        return false;
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return EGG_AABB;
    }
}
