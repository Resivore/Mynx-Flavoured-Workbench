package com.yungnickyoung.minecraft.ribbits.block;

import com.yungnickyoung.minecraft.ribbits.module.ConfiguredFeatureModule;
import com.yungnickyoung.minecraft.ribbits.module.PlacedFeatureModule;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Optional;

public class ToadstoolBlock extends SwampPlantBlock {
    static final double HUGE_GROWTH_CHANCE = 0.4;
    protected static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 8.0, 16.0);
    private final ResourceKey<ConfiguredFeature<?, ?>> hugeFeature;

    public ToadstoolBlock(Properties properties) {
        this(properties, PlacedFeatureModule.TOADSTOOL_PATCH, ConfiguredFeatureModule.HUGE_RED_TOADSTOOL);
    }

    public ToadstoolBlock(Properties properties, ResourceKey<PlacedFeature> bonemealPatch) {
        this(properties, bonemealPatch, ConfiguredFeatureModule.HUGE_RED_TOADSTOOL);
    }

    public ToadstoolBlock(Properties properties, ResourceKey<PlacedFeature> bonemealPatch,
                          ResourceKey<ConfiguredFeature<?, ?>> hugeFeature) {
        super(properties, bonemealPatch);
        this.hugeFeature = hugeFeature;
    }

    @Override
    public VoxelShape getShape(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, CollisionContext collisionContext) {
        Vec3 offset = blockState.getOffset(blockPos);
        return SHAPE.move(offset.x, offset.y, offset.z);
    }

    @Override
    public void performBonemeal(ServerLevel serverLevel, RandomSource random, BlockPos blockPos, BlockState blockState) {
        if (random.nextFloat() < HUGE_GROWTH_CHANCE
                && this.growHugeToadstool(serverLevel, blockPos, blockState, random)) {
            return;
        }

        super.performBonemeal(serverLevel, random, blockPos, blockState);
    }

    private boolean growHugeToadstool(ServerLevel serverLevel, BlockPos blockPos, BlockState blockState,
                                      RandomSource random) {
        Optional<ConfiguredFeature<?, ?>> configuredFeature = serverLevel.registryAccess()
                .lookupOrThrow(Registries.CONFIGURED_FEATURE)
                .getOptional(this.hugeFeature);
        if (configuredFeature.isEmpty()) {
            return false;
        }

        serverLevel.removeBlock(blockPos, false);
        if (configuredFeature.get().place(
                serverLevel, serverLevel.getChunkSource().getGenerator(), random, blockPos)) {
            return true;
        }

        serverLevel.setBlock(blockPos, blockState, Block.UPDATE_ALL);
        return false;
    }
}
