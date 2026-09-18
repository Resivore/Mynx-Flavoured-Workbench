package dev.resivore.bgectm.continuity;

import dev.resivore.bgectm.SurfaceContactResolver;
import dev.resivore.bgectm.SurfaceContactResolver.Decision;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Objects;
import java.util.function.Supplier;

/** Veto-only core applied to Continuity's per-candidate Standard Overlay decision. */
public final class OverlayContactFilter {
    private OverlayContactFilter() {}

    /**
     * Returns an upstream overlay result unchanged unless managed geometry lacks valid contact.
     * A false upstream result is terminal and geometry can never turn it into true.
     */
    public static boolean retainAfterUpstream(boolean upstreamApplies, Decision stateDecision,
            ContinuityQuadContext.Capture capture, Supplier<Decision> exactDecision) {
        Objects.requireNonNull(exactDecision, "exactDecision");
        if (!upstreamApplies) return false;
        return ContactFilteringConnectionPredicate.allowsByContactPolicy(
                stateDecision, capture, exactDecision);
    }

    /** Exact receiver/inducing-state mapping shared by the mixin and focused GameTests. */
    public static boolean retainAfterUpstream(boolean upstreamApplies,
            BlockState receiverState, BlockPos receiverPos,
            BlockState inducingState, BlockPos inducingPos, Direction face,
            ContinuityQuadContext.Capture capture) {
        if (!upstreamApplies) return false;
        Objects.requireNonNull(receiverState, "receiverState");
        Objects.requireNonNull(receiverPos, "receiverPos");
        Objects.requireNonNull(inducingState, "inducingState");
        Objects.requireNonNull(inducingPos, "inducingPos");
        Objects.requireNonNull(face, "face");
        Decision stateDecision = SurfaceContactResolver.inspect(
                receiverState, receiverPos, inducingState, inducingPos, face);
        if (stateDecision == Decision.CONNECT && (capture == null || !capture.valid())
                && SurfaceContactResolver.stateDerivedFallbackSafe(receiverState, inducingState)) {
            return true;
        }
        return retainAfterUpstream(true, stateDecision, capture,
                () -> SurfaceContactResolver.inspectWithSourceSurface(
                        receiverState, receiverPos, inducingState, inducingPos,
                        face, capture.surface()));
    }
}
