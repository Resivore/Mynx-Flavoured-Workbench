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
import java.util.Optional;
import java.util.function.Supplier;

/** Evaluates Continuity semantics on canonical material state, then applies BGE contact geometry. */
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
        boolean nativeSemantic = delegate.shouldConnect(level, pos, appearanceState, state, otherPos,
                otherAppearanceState, otherState, face, quadSprite);
        if (level == null || pos == null || otherPos == null || face == null) return nativeSemantic;

        BlockState realSourceState = level.getBlockState(pos);
        BlockState realOtherState = level.getBlockState(otherPos);
        if (!managed(realSourceState) && !managed(realOtherState)) return nativeSemantic;

        Direction semanticFace = semanticFace(realSourceState, face, ContinuityQuadContext.current());
        BlockState canonicalSource = canonical(realSourceState, level, pos, semanticFace,
                realOtherState, otherPos);
        BlockState canonicalOther = canonical(realOtherState, level, otherPos, semanticFace,
                realSourceState, pos);
        boolean canonicalSemantic = delegate.shouldConnect(level, pos, canonicalSource,
                canonicalSource, otherPos, canonicalOther, canonicalOther, semanticFace, quadSprite);
        if (BgeCtmDiagnostics.enabled()) {
            OverlayAttemptContext.connectionSemantics(nativeSemantic, canonicalSemantic);
        }
        return canonicalSemantic;
    }

    @Override
    public boolean shouldConnect(BlockAndTintGetter level, BlockPos pos,
            BlockState appearanceState, BlockState state, BlockPos otherPos, Direction face,
            TextureAtlasSprite quadSprite) {
        BlockState realSourceState = level.getBlockState(pos);
        BlockState realOtherState = level.getBlockState(otherPos);
        BlockState nativeOtherAppearance = realOtherState.getAppearance(
                level, otherPos, face, state, pos);
        boolean nativeSemantic = delegate.shouldConnect(level, pos, appearanceState, state,
                otherPos, nativeOtherAppearance, realOtherState, face, quadSprite);
        if (!managed(realSourceState) && !managed(realOtherState)) return nativeSemantic;

        ContinuityQuadContext.Capture capture = ContinuityQuadContext.current();
        Direction semanticFace = semanticFace(realSourceState, face, capture);
        BlockState canonicalSource = canonical(realSourceState, level, pos, semanticFace,
                realOtherState, otherPos);
        BlockState canonicalOther = canonical(realOtherState, level, otherPos, semanticFace,
                realSourceState, pos);
        boolean canonicalSemantic = delegate.shouldConnect(level, pos, canonicalSource,
                canonicalSource, otherPos, canonicalOther, canonicalOther, semanticFace, quadSprite);

        // Continuity's innerSeams probe is displaced along the rendered face normal. It still
        // needs canonical material semantics, but it is not an in-plane BGE contact candidate.
        if (coordinate(otherPos, face.getAxis()) != coordinate(pos, face.getAxis())) {
            return canonicalSemantic;
        }

        if (!canonicalSemantic) {
            if (BgeCtmDiagnostics.enabled()) {
                BgeCtmDiagnostics.regular(realSourceState, pos, canonicalSource,
                        realOtherState, otherPos, canonicalOther, delegate, nativeSemantic,
                        false, capture == null ? null : capture.surface(),
                        Decision.BYPASS_UNRELATED, Decision.BYPASS_UNRELATED, false,
                        "CANONICAL_SEMANTIC_REJECT");
            }
            return false;
        }

        Decision stateDecision = stateContactPolicy.inspect(
                realSourceState, pos, realOtherState, otherPos, face);
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
                : result && !nativeSemantic ? "CARRIER_REJECTION_PROMOTED"
                : result ? "FINAL_CONNECT"
                : capture == null || !capture.valid() ? "QUAD_CAPTURE_INVALID"
                : geometryDecision.name();
        if (BgeCtmDiagnostics.enabled()) {
            BgeCtmDiagnostics.regular(realSourceState, pos, canonicalSource, realOtherState, otherPos,
                    canonicalOther, delegate, nativeSemantic, canonicalSemantic,
                    capture == null ? null : capture.surface(), stateDecision, geometryDecision,
                    result, reason);
        }
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

    private static boolean managed(BlockState state) {
        return CanonicalAppearanceResolver.inspect(state).inherited();
    }

    private static BlockState canonical(BlockState physical, BlockAndTintGetter level,
            BlockPos pos, Direction face, BlockState querySource, BlockPos sourcePos) {
        return CanonicalAppearanceResolver.resolve(
                physical, level, pos, face, querySource, sourcePos);
    }

    private static Direction semanticFace(BlockState source, Direction physicalFace,
            ContinuityQuadContext.Capture capture) {
        if (capture == null || !capture.valid() || capture.surface().normal() != physicalFace) {
            return physicalFace;
        }
        Optional<SurfaceContactResolver.SurfaceMatch> match =
                SurfaceContactResolver.matchRenderedSurface(source, capture.surface());
        return match.map(SurfaceContactResolver.SurfaceMatch::canonicalFace).orElse(physicalFace);
    }

    @FunctionalInterface
    interface StateContactPolicy {
        Decision inspect(BlockState sourceState, BlockPos sourcePos,
                BlockState otherState, BlockPos otherPos, Direction face);
    }
}
