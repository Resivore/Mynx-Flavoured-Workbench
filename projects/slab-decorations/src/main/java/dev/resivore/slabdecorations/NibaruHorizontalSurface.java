package dev.resivore.slabdecorations;

import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfiles;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.MossyCarpetBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;

import java.util.Optional;

/** Resolves only canonical Nibaru-owned horizontal slabs; material and geometry stay independent. */
public final class NibaruHorizontalSurface {
    public static final double BOTTOM_OFFSET = -0.5D;

    private NibaruHorizontalSurface() {
    }

    /** Returns an exact native horizontal candidate before plant/material/water policy is applied. */
    public static Optional<Surface> candidate(BlockState plantState, BlockGetter level, BlockPos plantPos) {
        if (!PlantFamilyEligibility.isEligible(plantState.getBlock())) return Optional.empty();

        BlockPos rootPos = rootPosition(plantState, level, plantPos).orElse(null);
        if (rootPos == null) return Optional.empty();
        BlockPos supportPos = rootPos.below();
        BlockState supportState = level.getBlockState(supportPos);
        if (!(supportState.getBlock() instanceof SlabBlock)
                || !supportState.hasProperty(BlockStateProperties.SLAB_TYPE)) {
            return Optional.empty();
        }

        NibaruMaterialProfile profile = NibaruMaterialProfiles.fromBlock(supportState.getBlock()).orElse(null);
        if (profile == null || profile.nativeSlab().orElse(null) != supportState.getBlock()) {
            return Optional.empty();
        }

        return Optional.of(new Surface(profile, supportState, supportPos,
                supportState.getValue(BlockStateProperties.SLAB_TYPE)));
    }

    public static Optional<Surface> supporting(BlockState plantState, BlockGetter level, BlockPos plantPos) {
        Surface surface = candidate(plantState, level, plantPos).orElse(null);
        if (surface == null) return Optional.empty();
        if (!PlantFamilyEligibility.acceptsCanonicalParent(
                plantState.getBlock(), surface.canonicalParentState())) {
            return Optional.empty();
        }

        // Canary 1 keeps terrestrial plants out of every waterlogged support state. This avoids
        // reproducing Countered's inconsistent family-specific overlap behavior.
        if (surface.waterlogged()) {
            return Optional.empty();
        }

        return Optional.of(surface);
    }

    public static double visibleOffset(BlockState plantState, BlockGetter level, BlockPos plantPos) {
        Surface surface = supporting(plantState, level, plantPos).orElse(null);
        if (surface == null || surface.type() != SlabType.BOTTOM) return 0.0D;

        BlockPos rootPos = rootPosition(plantState, level, plantPos).orElse(null);
        if (rootPos == null) return 0.0D;
        BlockState rootState = level.getBlockState(rootPos);
        if (level instanceof LevelReader reader && !rootState.canSurvive(reader, rootPos)) return 0.0D;
        return BOTTOM_OFFSET;
    }

    public static double surfaceHeight(SlabType type) {
        return type == SlabType.BOTTOM ? 0.5D : 1.0D;
    }

    private static Optional<BlockPos> rootPosition(BlockState state, BlockGetter level, BlockPos pos) {
        if (PlantFamilyEligibility.isPaleMossCarpetCompanion(state.getBlock())) {
            if (!state.hasProperty(MossyCarpetBlock.BASE)) return Optional.empty();
            if (state.getValue(MossyCarpetBlock.BASE)) return Optional.of(pos);

            BlockPos lowerPos = pos.below();
            BlockState lower = level.getBlockState(lowerPos);
            if (lower.is(Blocks.PALE_MOSS_CARPET)
                    && lower.hasProperty(MossyCarpetBlock.BASE)
                    && lower.getValue(MossyCarpetBlock.BASE)) {
                return Optional.of(lowerPos);
            }
            return Optional.empty();
        }
        if (!PlantFamilyEligibility.isDoubleGrassCompanion(state.getBlock())) return Optional.of(pos);
        if (!state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) return Optional.empty();
        if (state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER) {
            return Optional.of(pos);
        }

        BlockPos lowerPos = pos.below();
        BlockState lower = level.getBlockState(lowerPos);
        if (lower.is(state.getBlock())
                && lower.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
                && lower.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER) {
            return Optional.of(lowerPos);
        }
        return Optional.empty();
    }

    public record Surface(
            NibaruMaterialProfile profile,
            BlockState supportState,
            BlockPos supportPos,
            SlabType type) {

        public BlockState canonicalParentState() {
            return profile.canonicalParent().defaultBlockState();
        }

        public double height() {
            return surfaceHeight(type);
        }

        public double offset() {
            return type == SlabType.BOTTOM ? BOTTOM_OFFSET : 0.0D;
        }

        public boolean waterlogged() {
            return supportState.hasProperty(BlockStateProperties.WATERLOGGED)
                    && supportState.getValue(BlockStateProperties.WATERLOGGED);
        }
    }
}
