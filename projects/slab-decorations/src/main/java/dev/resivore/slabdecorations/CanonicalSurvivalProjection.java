package dev.resivore.slabdecorations;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

/** Executes ordinary state survival against a read-only, one-position canonical support view. */
public final class CanonicalSurvivalProjection {
    private static final ThreadLocal<Integer> EVALUATION_DEPTH = new ThreadLocal<>();

    private CanonicalSurvivalProjection() {
    }

    /** Returns empty when this project does not own the state/support pair. */
    public static Optional<Boolean> evaluate(BlockState state, LevelReader level, BlockPos pos) {
        if (isEvaluating()) return Optional.empty();
        NibaruHorizontalSurface.Surface surface =
                NibaruHorizontalSurface.candidate(state, level, pos).orElse(null);
        if (surface == null) return Optional.empty();
        return Optional.of(evaluate(state, level, pos, surface));
    }

    static boolean evaluate(
            BlockState state,
            LevelReader level,
            BlockPos pos,
            NibaruHorizontalSurface.Surface surface) {
        return evaluate(state, level, level, pos, surface);
    }

    static boolean evaluate(
            BlockState state,
            LevelReader environment,
            BlockGetter currentView,
            BlockPos pos,
            NibaruHorizontalSurface.Surface surface) {
        if (isEvaluating()) {
            // Re-entry is only expected through BlockState.canSurvive. Let the caller's vanilla
            // invocation continue rather than opening a nested projection.
            return state.canSurvive(environment, pos);
        }

        enter();
        try {
            LevelReader projected = new CanonicalSupportLevelReader(
                    environment, pos, currentView.getBlockState(pos), currentView.getFluidState(pos),
                    surface.supportPos(), surface.canonicalParentState());
            NibaruHorizontalSurface.Attachment attachment = surface.attachment();

            // The root/anchor owns the support decision for every connected segment.
            if (!attachment.state().canSurvive(projected, attachment.pos())) return false;
            if (attachment.pos().equals(pos)) return true;

            // Preserve the current segment's remaining vanilla neighbour/state requirements too.
            return state.canSurvive(projected, pos);
        } finally {
            exit();
        }
    }

    public static boolean isEvaluating() {
        Integer depth = EVALUATION_DEPTH.get();
        return depth != null && depth != 0;
    }

    private static void enter() {
        Integer depth = EVALUATION_DEPTH.get();
        EVALUATION_DEPTH.set(depth == null ? 1 : depth + 1);
    }

    private static void exit() {
        Integer depth = EVALUATION_DEPTH.get();
        if (depth == null) {
            throw new IllegalStateException("canonical survival projection guard underflow");
        }
        int next = depth - 1;
        if (next == 0) {
            EVALUATION_DEPTH.remove();
        } else if (next > 0) {
            EVALUATION_DEPTH.set(next);
        } else {
            EVALUATION_DEPTH.remove();
            throw new IllegalStateException("canonical survival projection guard underflow");
        }
    }
}
