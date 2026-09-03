package dev.resivore.slabdecorations;

import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfiles;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BigDripleafBlock;
import net.minecraft.world.level.block.BigDripleafStemBlock;
import net.minecraft.world.level.block.MossyCarpetBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;

import java.util.Optional;

/** Resolves only canonical BGE-owned native horizontal slabs and rooted foliage geometry. */
public final class NibaruHorizontalSurface {
    public static final double BOTTOM_OFFSET = -0.5D;

    private NibaruHorizontalSurface() {
    }

    /** Returns an exact native horizontal candidate before projected survival is evaluated. */
    public static Optional<Surface> candidate(BlockState plantState, BlockGetter level, BlockPos plantPos) {
        Root root = root(plantState, level, plantPos).orElse(null);
        if (root == null) return Optional.empty();

        BlockPos supportPos = root.pos().below();
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
                supportState.getValue(BlockStateProperties.SLAB_TYPE), root));
    }

    /**
     * Returns a usable surface only when the complete projected vanilla result can be evaluated.
     * Client render snapshots are paired with their ClientLevel by the model wrapper.
     */
    public static Optional<Surface> supporting(BlockState plantState, BlockGetter level, BlockPos plantPos) {
        Surface surface = candidate(plantState, level, plantPos).orElse(null);
        if (surface == null || surface.waterlogged()) return Optional.empty();
        if (!(level instanceof LevelReader reader)
                || !CanonicalSurvivalProjection.evaluate(plantState, reader, plantPos, surface)) {
            return Optional.empty();
        }
        return Optional.of(surface);
    }

    /** Uses a render snapshot for block data and its ClientLevel only for LevelReader services. */
    public static Optional<Surface> supporting(
            BlockState plantState,
            BlockGetter blockView,
            LevelReader environment,
            BlockPos plantPos) {
        Surface surface = candidate(plantState, blockView, plantPos).orElse(null);
        if (surface == null || surface.waterlogged()) return Optional.empty();
        return CanonicalSurvivalProjection.evaluate(
                plantState, environment, blockView, plantPos, surface)
                ? Optional.of(surface)
                : Optional.empty();
    }

    public static double visibleOffset(BlockState plantState, BlockGetter level, BlockPos plantPos) {
        Surface surface = supporting(plantState, level, plantPos).orElse(null);
        return surface != null && surface.type() == SlabType.BOTTOM ? BOTTOM_OFFSET : 0.0D;
    }

    public static double visibleOffset(
            BlockState plantState,
            BlockGetter blockView,
            LevelReader environment,
            BlockPos plantPos) {
        Surface surface = supporting(plantState, blockView, environment, plantPos).orElse(null);
        return surface != null && surface.type() == SlabType.BOTTOM ? BOTTOM_OFFSET : 0.0D;
    }

    public static double surfaceHeight(SlabType type) {
        return type == SlabType.BOTTOM ? 0.5D : 1.0D;
    }

    static Optional<Root> root(BlockState state, BlockGetter level, BlockPos pos) {
        PlantFamilyEligibility.Family family = PlantFamilyEligibility.family(state).orElse(null);
        if (family == null) return Optional.empty();

        return switch (family) {
            case UPWARD_VEGETATION, SURFACE_FOLIAGE -> mossyCarpetRoot(state, level, pos);
            case DOUBLE_HEIGHT_VEGETATION -> doublePlantRoot(state, level, pos);
            case DRIPLEAF_COLUMN -> dripleafRoot(state, level, pos);
        };
    }

    private static Optional<Root> doublePlantRoot(BlockState state, BlockGetter level, BlockPos pos) {
        if (!state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) return Optional.empty();
        if (state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER) {
            return Optional.of(new Root(pos, state));
        }

        BlockPos lowerPos = pos.below();
        BlockState lower = level.getBlockState(lowerPos);
        if (lower.is(state.getBlock())
                && lower.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
                && lower.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER) {
            return Optional.of(new Root(lowerPos, lower));
        }
        return Optional.empty();
    }

    private static Optional<Root> mossyCarpetRoot(BlockState state, BlockGetter level, BlockPos pos) {
        if (!(state.getBlock() instanceof MossyCarpetBlock)) {
            return Optional.of(new Root(pos, state));
        }
        if (!state.hasProperty(MossyCarpetBlock.BASE)) return Optional.empty();
        if (state.getValue(MossyCarpetBlock.BASE)) return Optional.of(new Root(pos, state));

        BlockPos lowerPos = pos.below();
        BlockState lower = level.getBlockState(lowerPos);
        if (lower.getBlock() instanceof MossyCarpetBlock
                && lower.is(state.getBlock())
                && lower.hasProperty(MossyCarpetBlock.BASE)
                && lower.getValue(MossyCarpetBlock.BASE)) {
            return Optional.of(new Root(lowerPos, lower));
        }
        return Optional.empty();
    }

    private static Optional<Root> dripleafRoot(BlockState state, BlockGetter level, BlockPos pos) {
        if (!(state.getBlock() instanceof BigDripleafBlock)
                && !(state.getBlock() instanceof BigDripleafStemBlock)) {
            return Optional.empty();
        }

        BlockPos rootPos = pos;
        BlockState rootState = state;
        while (!level.isOutsideBuildHeight(rootPos.getY() - 1)) {
            BlockPos belowPos = rootPos.below();
            BlockState below = level.getBlockState(belowPos);
            if (!(below.getBlock() instanceof BigDripleafBlock)
                    && !(below.getBlock() instanceof BigDripleafStemBlock)) break;
            rootPos = belowPos;
            rootState = below;
        }
        return Optional.of(new Root(rootPos, rootState));
    }

    record Root(BlockPos pos, BlockState state) {
    }

    public record Surface(
            NibaruMaterialProfile profile,
            BlockState supportState,
            BlockPos supportPos,
            SlabType type,
            Root root) {

        public BlockState canonicalParentState() {
            return profile.canonicalParent().withPropertiesOf(supportState);
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
