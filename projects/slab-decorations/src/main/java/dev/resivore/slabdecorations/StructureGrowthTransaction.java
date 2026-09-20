package dev.resivore.slabdecorations;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.function.Predicate;

/**
 * A short-lived canonical-parent substitution for vanilla structure features.  It changes only
 * the candidate slab cells, restores the exact preimage on failure, and extends a successful
 * eligible floor or ceiling slab root from the feature's own generated continuation state.
 */
public final class StructureGrowthTransaction {
    private static final Identifier RIBBITS_TOADSTOOL_STEM =
            Identifier.fromNamespaceAndPath("ribbits", "toadstool_stem");
    private StructureGrowthTransaction() {}

    public static boolean run(ServerLevel level, BlockPos origin, BlockState precursor,
                              Supplier<Boolean> vanillaGrowth) {
        return run(level, origin, precursor, vanillaGrowth,
                StructureGrowthTransaction::structuralContinuation);
    }

    /**
     * Enderscape's tree and chanterelle features produce their continuation as a native
     * RotatedPillarBlock.  This remains scoped to their optional feature seam rather than
     * widening ordinary transaction consumption to arbitrary pillars.
     */
    public static boolean runEnderscapeGrowth(ServerLevel level, BlockPos origin, BlockState precursor,
                                               Supplier<Boolean> growth) {
        return run(level, origin, precursor, growth,
                state -> structuralContinuation(state) || state.getBlock() instanceof RotatedPillarBlock);
    }

    private static boolean run(ServerLevel level, BlockPos origin, BlockState precursor,
                               Supplier<Boolean> vanillaGrowth,
                               Predicate<BlockState> continuation) {
        List<Entry> entries = collect(level, origin, precursor);
        if (entries.isEmpty()) return vanillaGrowth.get();
        try {
            for (Entry entry : entries) level.setBlock(entry.pos, entry.surface.canonicalParentState(),
                    Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
            boolean grown = vanillaGrowth.get();
            if (!grown) {
                restore(level, entries);
                return false;
            }
            reconcile(level, entries, continuation);
            return true;
        } catch (RuntimeException | Error failure) {
            restore(level, entries);
            throw failure;
        }
    }

    private static List<Entry> collect(ServerLevel level, BlockPos origin, BlockState precursor) {
        List<Entry> entries = new ArrayList<>();
        // A vanilla 2x2 grower may begin from any member. Include only its actual same-block
        // footprint; unrelated canopy slabs are never projected or consumed.
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
            BlockPos plantPos = origin.offset(x, 0, z);
            BlockState candidate = level.getBlockState(plantPos);
            if (!candidate.is(precursor.getBlock())) continue;
            NibaruHorizontalSurface.Surface surface = NibaruHorizontalSurface
                    .candidate(candidate, level, plantPos).orElse(null);
            if (surface != null) entries.add(new Entry(surface.supportPos(), surface));
        }
        return entries;
    }

    private static void reconcile(
            ServerLevel level,
            List<Entry> entries,
            Predicate<BlockState> continuation) {
        for (Entry entry : entries) {
            BlockPos continuationPos = continuationPos(entry.surface);
            BlockState generated = continuationPos == null
                    ? Blocks.AIR.defaultBlockState()
                    : level.getBlockState(continuationPos);
            if (continuationPos != null && continuation.test(generated)) {
                // Use the feature output as the sole material/state authority. This preserves
                // species, axis and waterlogging when the generated block supports them.
                level.setBlock(entry.pos, generated, Block.UPDATE_CLIENTS);
            } else {
                level.setBlock(entry.pos, entry.surface.supportState(), Block.UPDATE_CLIENTS);
            }
        }
    }

    private static BlockPos continuationPos(NibaruHorizontalSurface.Surface surface) {
        if (surface.orientation() == NibaruHorizontalSurface.AttachmentOrientation.UPWARD
                && surface.type() == SlabType.BOTTOM) {
            return surface.supportPos().above();
        }
        if (surface.orientation() == NibaruHorizontalSurface.AttachmentOrientation.CEILING
                && surface.type() == SlabType.TOP) {
            return surface.supportPos().below();
        }
        return null;
    }

    private static boolean structuralContinuation(BlockState state) {
        return state.is(BlockTags.LOGS) || state.is(Blocks.MUSHROOM_STEM)
                || state.is(Blocks.CRIMSON_STEM) || state.is(Blocks.WARPED_STEM)
                || state.is(Blocks.MANGROVE_ROOTS) || state.is(Blocks.MUDDY_MANGROVE_ROOTS)
                || RIBBITS_TOADSTOOL_STEM.equals(BuiltInRegistries.BLOCK.getKey(state.getBlock()));
    }

    private static void restore(ServerLevel level, List<Entry> entries) {
        for (Entry entry : entries) level.setBlock(entry.pos, entry.surface.supportState(),
                Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
    }

    private record Entry(BlockPos pos, NibaruHorizontalSurface.Surface surface) {}
}
