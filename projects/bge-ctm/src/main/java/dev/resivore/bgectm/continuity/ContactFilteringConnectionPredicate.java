package dev.resivore.bgectm.continuity;

import dev.resivore.bgectm.SurfaceContactResolver;
import dev.resivore.bgectm.SurfaceContactResolver.Decision;
import dev.resivore.bgectm.BgeCtmDiagnostics;
import dev.resivore.bgectm.CanonicalAppearanceResolver;
import me.pepperbell.continuity.client.processor.ConnectionPredicate;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Objects;
import java.util.function.Supplier;

/** Veto-only decorator for regular Continuity CTM neighbor probes. */
public final class ContactFilteringConnectionPredicate implements ConnectionPredicate {
    private final ConnectionPredicate delegate;
    private final StateContactPolicy stateContactPolicy;

    public ContactFilteringConnectionPredicate(ConnectionPredicate delegate) {
        this(delegate, SurfaceContactResolver::inspect);
    }

    ContactFilteringConnectionPredicate(ConnectionPredicate delegate,
            StateContactPolicy stateContactPolicy) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.stateContactPolicy = Objects.requireNonNull(stateContactPolicy, "stateContactPolicy");
    }

    @Override
    public boolean shouldConnect(BlockAndTintGetter level, BlockPos pos,
            BlockState appearanceState, BlockState state, BlockPos otherPos,
            BlockState otherAppearanceState, BlockState otherState, Direction face,
            TextureAtlasSprite quadSprite) {
        // StandardOverlayQuadProcessor negates this full overload. C4 filters the processor's
        // final positive appliesOverlay result instead, after every native overlay precondition.
        boolean result = delegate.shouldConnect(level, pos, appearanceState, state, otherPos,
                otherAppearanceState, otherState, face, quadSprite);
        return result;
    }

    @Override
    public boolean shouldConnect(BlockAndTintGetter level, BlockPos pos,
            BlockState appearanceState, BlockState state, BlockPos otherPos, Direction face,
            TextureAtlasSprite quadSprite) {
        boolean upstream = delegate.shouldConnect(level, pos, appearanceState, state, otherPos, face, quadSprite);
        if (!upstream) {
            return false;
        }

        // Continuity's innerSeams path performs a second auxiliary lookup displaced along the
        // face normal. That is not an in-plane connection candidate and must retain upstream
        // behavior rather than being interpreted as a failed Canary 4 surface contact.
        if (coordinate(otherPos, face.getAxis()) != coordinate(pos, face.getAxis())) {
            return true;
        }

        // Recover reality from the render view. One exact upstream provider swaps its nominal
        // appearance/state parameters, so those arguments are not a safe geometry authority.
        BlockState realSourceState = level.getBlockState(pos);
        BlockState realOtherState = level.getBlockState(otherPos);
        Decision stateDecision = stateContactPolicy.inspect(
                realSourceState, pos, realOtherState, otherPos, face);
        ContinuityQuadContext.Capture capture = ContinuityQuadContext.current();
        Decision geometryDecision = capture != null && capture.valid()
                ? SurfaceContactResolver.inspectWithSourceSurface(realSourceState, pos,
                        realOtherState, otherPos, face, capture.surface())
                : stateDecision;
        boolean fallback = (capture == null || !capture.valid())
                && stateDecision == Decision.CONNECT
                && SurfaceContactResolver.stateDerivedFallbackSafe(realSourceState, realOtherState);
        boolean result = retainRegularAfterUpstream(stateDecision, capture, fallback,
                () -> SurfaceContactResolver.inspectWithSourceSurface(
                        realSourceState, pos, realOtherState, otherPos, face, capture.surface()));
        String reason = !stateDecision.allowsOriginal() ? stateDecision.name()
                : fallback ? "STATE_FALLBACK_CONTACT_OK"
                : result ? "FINAL_CONNECT"
                : capture == null || !capture.valid() ? "QUAD_CAPTURE_INVALID"
                : geometryDecision.name();
        BgeCtmDiagnostics.regular(realSourceState, pos, appearanceState, realOtherState, otherPos,
                CanonicalAppearanceResolver.inspect(realOtherState).appearance(), delegate, upstream,
                capture == null ? null : capture.surface(), stateDecision, geometryDecision, result, reason);
        return result;
    }

    /** Testable core of the decorator after Continuity's own positive decision. */
    static boolean allowsByContactPolicy(Decision stateDecision,
            ContinuityQuadContext.Capture capture, Supplier<Decision> exactDecision) {
        Objects.requireNonNull(stateDecision, "stateDecision");
        Objects.requireNonNull(exactDecision, "exactDecision");
        if (stateDecision == Decision.BYPASS_UNRELATED) {
            return true;
        }
        if (!stateDecision.allowsOriginal() || capture == null || !capture.valid()) {
            return false;
        }
        return exactDecision.get().allowsOriginal();
    }

    /** Shared regular final-decision path, including the narrow C4 state-cuboid fallback. */
    public static boolean retainRegularAfterUpstream(Decision stateDecision,
            ContinuityQuadContext.Capture capture, boolean fallbackSafe,
            Supplier<Decision> exactDecision) {
        if (stateDecision == Decision.CONNECT && (capture == null || !capture.valid())
                && fallbackSafe) {
            return true;
        }
        return allowsByContactPolicy(stateDecision, capture, exactDecision);
    }

    private static int coordinate(BlockPos pos, Direction.Axis axis) {
        return switch (axis) {
            case X -> pos.getX();
            case Y -> pos.getY();
            case Z -> pos.getZ();
        };
    }

    @FunctionalInterface
    interface StateContactPolicy {
        Decision inspect(BlockState sourceState, BlockPos sourcePos,
                BlockState otherState, BlockPos otherPos, Direction face);
    }
}
