package dev.resivore.slabdecorations;

import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfiles;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/** Delegates the three admitted bottom-slab substrates to their canonical 26.2 blocks. */
public final class SubstrateBonemeal {
    private static final int RECONCILE_FLAGS = Block.UPDATE_CLIENTS
            | Block.UPDATE_SUPPRESS_DROPS
            | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS
            | Block.UPDATE_SKIP_ON_PLACE;

    private SubstrateBonemeal() {
    }

    /**
     * Returns an empty result when vanilla should retain control, or the canonical result when
     * this project owns the exact bottom-native-slab path.
     */
    public static Optional<Boolean> growCrop(ItemStack stack, Level level, BlockPos pos) {
        Target target = target(level.getBlockState(pos)).orElse(null);
        if (target == null) return Optional.empty();

        BonemealableBlock canonical = (BonemealableBlock) target.family().canonicalBlock();
        BlockState canonicalState = target.family().canonicalBlock().withPropertiesOf(target.slabState());
        if (!canonical.isValidBonemealTarget(level, pos, canonicalState)) {
            return Optional.of(false);
        }

        if (level instanceof ServerLevel serverLevel) {
            try (CanonicalProjection ignored = CanonicalProjection.open(serverLevel, pos, target.family())) {
                if (canonical.isBonemealSuccess(level, level.getRandom(), pos, canonicalState)) {
                    canonical.performBonemeal(serverLevel, level.getRandom(), pos, canonicalState);
                }
            }
            // Vanilla consumes one item for every valid server-side activation, whether or not the
            // probabilistic effect succeeds.
            stack.shrink(1);
        }
        return Optional.of(true);
    }

    /** Resolves only dry, exact, native bottom slabs in the three demonstrated families. */
    public static Optional<Target> target(BlockState state) {
        NativeBottomSlab nativeSlab = nativeBottomSlab(state).orElse(null);
        if (nativeSlab == null) return Optional.empty();

        Family family = Family.fromCanonical(nativeSlab.profile().canonicalParent()).orElse(null);
        if (family == null) return Optional.empty();
        return Optional.of(new Target(family, nativeSlab.profile(), state));
    }

    private static Optional<NativeBottomSlab> nativeBottomSlab(BlockState state) {
        if (!(state.getBlock() instanceof SlabBlock)
                || !state.hasProperty(BlockStateProperties.SLAB_TYPE)
                || state.getValue(BlockStateProperties.SLAB_TYPE) != SlabType.BOTTOM
                || state.hasProperty(BlockStateProperties.WATERLOGGED)
                && state.getValue(BlockStateProperties.WATERLOGGED)) {
            return Optional.empty();
        }

        NibaruMaterialProfile profile = NibaruMaterialProfiles.fromBlock(state.getBlock()).orElse(null);
        if (profile == null || profile.nativeSlab().orElse(null) != state.getBlock()) {
            return Optional.empty();
        }
        return Optional.of(new NativeBottomSlab(profile, state));
    }

    public enum Family {
        GRASS(Blocks.GRASS_BLOCK, 7, 7),
        MOSS(Blocks.MOSS_BLOCK, 3, 5),
        PALE_MOSS(Blocks.PALE_MOSS_BLOCK, 3, 5);

        private final Block canonicalBlock;
        private final int horizontalRadius;
        private final int verticalRadius;

        Family(Block canonicalBlock, int horizontalRadius, int verticalRadius) {
            if (!(canonicalBlock instanceof BonemealableBlock)) {
                throw new IllegalArgumentException(canonicalBlock + " is not canonically bonemealable");
            }
            this.canonicalBlock = canonicalBlock;
            this.horizontalRadius = horizontalRadius;
            this.verticalRadius = verticalRadius;
        }

        public Block canonicalBlock() {
            return canonicalBlock;
        }

        private static Optional<Family> fromCanonical(Block block) {
            for (Family family : values()) {
                if (family.canonicalBlock == block) return Optional.of(family);
            }
            return Optional.empty();
        }
    }

    public record Target(Family family, NibaruMaterialProfile profile, BlockState slabState) {
    }

    private record NativeBottomSlab(NibaruMaterialProfile profile, BlockState state) {
    }

    /**
     * Presents the bounded slab field as canonical full blocks only while vanilla runs. Grass's
     * longest random walk is seven cells; the two vegetation patches use radius three and vertical
     * range five. No top/double slab or non-native geometry can enter this transaction.
     */
    private static final class CanonicalProjection implements AutoCloseable {
        private final ServerLevel level;
        private final List<ProjectedSlab> projected;

        private CanonicalProjection(ServerLevel level, List<ProjectedSlab> projected) {
            this.level = level;
            this.projected = projected;
        }

        static CanonicalProjection open(ServerLevel level, BlockPos origin, Family family) {
            List<ProjectedSlab> projected = new ArrayList<>();
            BlockPos min = origin.offset(-family.horizontalRadius, -family.verticalRadius,
                    -family.horizontalRadius);
            BlockPos max = origin.offset(family.horizontalRadius, family.verticalRadius,
                    family.horizontalRadius);

            for (BlockPos scanned : BlockPos.betweenClosed(min, max)) {
                BlockPos pos = scanned.immutable();
                NativeBottomSlab nativeSlab = nativeBottomSlab(level.getBlockState(pos)).orElse(null);
                if (nativeSlab == null) continue;
                if (family == Family.GRASS
                        && nativeSlab.profile().canonicalParent() != Blocks.GRASS_BLOCK) {
                    continue;
                }
                BlockState canonicalState = nativeSlab.profile().canonicalParent()
                        .withPropertiesOf(nativeSlab.state());
                projected.add(new ProjectedSlab(pos, nativeSlab.state(), canonicalState));
            }

            CanonicalProjection projection = new CanonicalProjection(level, projected);
            int applied = 0;
            try {
                for (; applied < projected.size(); applied++) {
                    ProjectedSlab entry = projected.get(applied);
                    if (!level.setBlock(entry.pos(), entry.canonicalState(),
                            Block.UPDATE_SKIP_ALL_SIDEEFFECTS)) {
                        throw new IllegalStateException("could not project canonical substrate at "
                                + entry.pos());
                    }
                }
                return projection;
            } catch (RuntimeException exception) {
                projection.restorePrefix(applied);
                throw exception;
            }
        }

        @Override
        public void close() {
            restorePrefix(projected.size());
        }

        private void restorePrefix(int count) {
            List<ProjectedSlab> reverse = projected.subList(0, count);
            Collections.reverse(reverse);
            try {
                for (ProjectedSlab entry : reverse) {
                    BlockState current = level.getBlockState(entry.pos());
                    if (current.getBlock() == entry.canonicalState().getBlock()) {
                        level.setBlock(entry.pos(), entry.originalState(),
                                Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
                        continue;
                    }

                    // Moss features may canonically replace a projected dirt/stone/etc. parent.
                    // Reconcile only those demonstrated ground outputs back to native bottom slabs.
                    if (!current.is(Blocks.MOSS_BLOCK) && !current.is(Blocks.PALE_MOSS_BLOCK)) {
                        continue;
                    }
                    NibaruMaterialProfile output = NibaruMaterialProfiles
                            .fromBlock(current.getBlock()).orElse(null);
                    Block nativeOutput = output == null ? null : output.nativeSlab().orElse(null);
                    if (output == null || output.canonicalParent() != current.getBlock()
                            || nativeOutput == null) {
                        throw new IllegalStateException("canonical moss output has no native slab: "
                                + current);
                    }
                    BlockState mapped = nativeOutput.defaultBlockState()
                            .setValue(BlockStateProperties.SLAB_TYPE, SlabType.BOTTOM);
                    if (mapped.hasProperty(BlockStateProperties.WATERLOGGED)) {
                        mapped = mapped.setValue(BlockStateProperties.WATERLOGGED, false);
                    }
                    if (!level.setBlock(entry.pos(), mapped, RECONCILE_FLAGS)) {
                        throw new IllegalStateException("could not reconcile canonical moss output at "
                                + entry.pos());
                    }
                }
            } finally {
                Collections.reverse(reverse);
            }
        }
    }

    private record ProjectedSlab(BlockPos pos, BlockState originalState, BlockState canonicalState) {
    }
}
