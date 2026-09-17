package dev.resivore.slabdecorations;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * A short-lived canonical-parent substitution for vanilla structure features.  It changes only
 * the candidate slab cells, restores the exact preimage on failure, and extends a successful
 * bottom-slab root from the feature's own lowest generated continuation state.
 */
public final class StructureGrowthTransaction {
    private StructureGrowthTransaction() {}

    public static boolean run(ServerLevel level, BlockPos origin, BlockState precursor,
                              Supplier<Boolean> vanillaGrowth) {
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
            reconcile(level, entries);
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

    private static void reconcile(ServerLevel level, List<Entry> entries) {
        for (Entry entry : entries) {
            BlockState generated = level.getBlockState(entry.pos.above());
            if (entry.surface.type() == SlabType.BOTTOM && structuralContinuation(generated)) {
                // Use the feature output as the sole material/state authority. This preserves
                // species, axis and waterlogging when the generated block supports them.
                level.setBlock(entry.pos, generated, Block.UPDATE_CLIENTS);
            } else {
                level.setBlock(entry.pos, entry.surface.supportState(), Block.UPDATE_CLIENTS);
            }
        }
    }

    private static boolean structuralContinuation(BlockState state) {
        return state.is(BlockTags.LOGS) || state.is(Blocks.MUSHROOM_STEM)
                || state.is(Blocks.CRIMSON_STEM) || state.is(Blocks.WARPED_STEM)
                || state.is(Blocks.MANGROVE_ROOTS) || state.is(Blocks.MUDDY_MANGROVE_ROOTS);
    }

    private static void restore(ServerLevel level, List<Entry> entries) {
        for (Entry entry : entries) level.setBlock(entry.pos, entry.surface.supportState(),
                Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
    }

    private record Entry(BlockPos pos, NibaruHorizontalSurface.Surface surface) {}
}
