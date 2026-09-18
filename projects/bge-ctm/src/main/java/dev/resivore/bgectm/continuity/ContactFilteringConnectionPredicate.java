package dev.resivore.bgectm.continuity;

import dev.resivore.bgectm.SurfaceContactResolver;
import dev.resivore.bgectm.SurfaceContactResolver.Decision;
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
        // StandardOverlayQuadProcessor calls and negates this full overload. Preserve it exactly;
        // overlay applicability has intentionally different material/contact semantics.
        return delegate.shouldConnect(level, pos, appearanceState, state, otherPos,
                otherAppearanceState, otherState, face, quadSprite);
    }

    @Override
    public boolean shouldConnect(BlockAndTintGetter level, BlockPos pos,
            BlockState appearanceState, BlockState state, BlockPos otherPos, Direction face,
            TextureAtlasSprite quadSprite) {
        if (!delegate.shouldConnect(level, pos, appearanceState, state, otherPos, face, quadSprite)) {
            return false;
        }

        // Continuity's innerSeams path performs a second auxiliary lookup displaced along the
        // face normal. That is not an in-plane connection candidate and must retain upstream
        // behavior rather than being interpreted as a failed Canary 2 surface contact.
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
        return allowsByContactPolicy(stateDecision, capture,
                () -> SurfaceContactResolver.inspectWithSourceSurface(
                        realSourceState, pos, realOtherState, otherPos, face, capture.surface()));
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
