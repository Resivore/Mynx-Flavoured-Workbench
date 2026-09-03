package dev.resivore.slabdecorations;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.attribute.EnvironmentAttributeReader;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.BlockAndLightGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

/**
 * Read-only world projection that substitutes exactly one support state for ordinary
 * {@link BlockGetter}/{@link LevelReader} reads and delegates every other observable to the real
 * level. Raw {@link ChunkAccess} access remains delegated: compatibility is intentionally bounded
 * to standard placement/survival contracts rather than arbitrary custom chunk inspection.
 */
final class CanonicalSupportLevelReader implements LevelReader {
    private final LevelReader delegate;
    private final BlockGetter blockView;
    private final BlockPos supportPos;
    private final BlockState canonicalState;

    CanonicalSupportLevelReader(LevelReader delegate, BlockPos supportPos, BlockState canonicalState) {
        this(delegate, delegate, supportPos, canonicalState);
    }

    CanonicalSupportLevelReader(
            LevelReader delegate,
            BlockGetter blockView,
            BlockPos supportPos,
            BlockState canonicalState) {
        this.delegate = delegate;
        this.blockView = blockView;
        this.supportPos = supportPos.immutable();
        this.canonicalState = canonicalState;
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return supportPos.equals(pos) ? canonicalState : blockView.getBlockState(pos);
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return blockView.getFluidState(pos);
    }

    @Override
    public BlockEntity getBlockEntity(BlockPos pos) {
        return blockView.getBlockEntity(pos);
    }

    @Override
    public LevelLightEngine getLightEngine() {
        return blockView instanceof BlockAndLightGetter lightView
                ? lightView.getLightEngine()
                : delegate.getLightEngine();
    }

    @Override
    public ChunkAccess getChunk(int x, int z, ChunkStatus status, boolean create) {
        return delegate.getChunk(x, z, status, create);
    }

    @Override
    public boolean hasChunk(int x, int z) {
        return delegate.hasChunk(x, z);
    }

    @Override
    public int getHeight(Heightmap.Types type, int x, int z) {
        return delegate.getHeight(type, x, z);
    }

    @Override
    public int getSkyDarken() {
        return delegate.getSkyDarken();
    }

    @Override
    public BiomeManager getBiomeManager() {
        return delegate.getBiomeManager();
    }

    @Override
    public Holder<Biome> getUncachedNoiseBiome(int x, int y, int z) {
        return delegate.getUncachedNoiseBiome(x, y, z);
    }

    @Override
    public boolean isClientSide() {
        return delegate.isClientSide();
    }

    @Override
    public int getSeaLevel() {
        return delegate.getSeaLevel();
    }

    @Override
    public DimensionType dimensionType() {
        return delegate.dimensionType();
    }

    @Override
    public RegistryAccess registryAccess() {
        return delegate.registryAccess();
    }

    @Override
    public FeatureFlagSet enabledFeatures() {
        return delegate.enabledFeatures();
    }

    @Override
    public EnvironmentAttributeReader environmentAttributes() {
        return delegate.environmentAttributes();
    }

    @Override
    public WorldBorder getWorldBorder() {
        return delegate.getWorldBorder();
    }

    @Override
    public BlockGetter getChunkForCollisions(int chunkX, int chunkZ) {
        BlockGetter chunk = delegate.getChunkForCollisions(chunkX, chunkZ);
        if (chunk == null) return null;
        return supportPos.getX() >> 4 == chunkX && supportPos.getZ() >> 4 == chunkZ
                ? new ProjectedBlockGetter(chunk, supportPos, canonicalState)
                : chunk;
    }

    @Override
    public List<VoxelShape> getEntityCollisions(Entity entity, AABB bounds) {
        return delegate.getEntityCollisions(entity, bounds);
    }

    @Override
    public int getHeight() {
        return blockView.getHeight();
    }

    @Override
    public int getMinY() {
        return blockView.getMinY();
    }

    private record ProjectedBlockGetter(
            BlockGetter delegate,
            BlockPos supportPos,
            BlockState canonicalState) implements BlockGetter {

        @Override
        public BlockEntity getBlockEntity(BlockPos pos) {
            return delegate.getBlockEntity(pos);
        }

        @Override
        public BlockState getBlockState(BlockPos pos) {
            return supportPos.equals(pos) ? canonicalState : delegate.getBlockState(pos);
        }

        @Override
        public FluidState getFluidState(BlockPos pos) {
            return delegate.getFluidState(pos);
        }

        @Override
        public int getHeight() {
            return delegate.getHeight();
        }

        @Override
        public int getMinY() {
            return delegate.getMinY();
        }
    }
}
