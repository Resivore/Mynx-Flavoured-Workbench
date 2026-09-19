package dev.resivore.bgectm.continuity;

import dev.resivore.bgectm.SurfaceContactResolver;
import dev.resivore.bgectm.SurfaceContactResolver.Decision;
import dev.resivore.bgectm.SurfaceContactResolver.OverlayContribution;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Objects;
import java.util.function.Supplier;

/** Physical contact filter applied after Continuity has evaluated canonical material semantics. */
public final class OverlayContactFilter {
    private OverlayContactFilter() {}

    /**
     * Returns Continuity's canonical-semantic overlay result unchanged unless managed geometry
     * lacks valid contact. Geometry can never turn a semantic negative into a relationship.
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
        Decision stateDecision = SurfaceContactResolver.inspectOverlay(
                receiverState, receiverPos, inducingState, inducingPos, face);
        if (stateDecision == Decision.CONNECT && (capture == null || !capture.valid())
                && SurfaceContactResolver.stateDerivedFallbackSafe(receiverState, inducingState)) {
            return true;
        }
        if (stateDecision == Decision.BYPASS_UNRELATED) return true;
        if (stateDecision != Decision.CONNECT || capture == null || !capture.valid()) return false;
        OverlayContribution contribution = SurfaceContactResolver.inspectOverlayContribution(
                receiverState, receiverPos, inducingState, inducingPos, face, capture.surface());
        if (contribution.decision() != Decision.CONNECT) return false;
        capture.addOverlayProbe(inducingPos, contribution.footprints());
        return true;
    }
}
