package dev.resivore.bgectm.continuity;

import dev.resivore.bgectm.SurfaceContactResolver.Decision;
import dev.resivore.bgectm.SurfaceContactResolver.QuadSurface;
import me.pepperbell.continuity.client.processor.ConnectionPredicate;
import net.minecraft.SharedConstants;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ContactFilteringConnectionPredicateTest {
    private static final BlockPos ORIGIN = BlockPos.ZERO;
    private static final BlockPos EAST = ORIGIN.east();

    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void unrelatedSevenArgumentConnectionsPreserveTheDelegateExactly() {
        BlockState unrelated = Blocks.BARRIER.defaultBlockState();
        AtomicInteger worldReads = new AtomicInteger();
        BlockAndTintGetter view = viewReturning(unrelated, worldReads);
        ContactFilteringConnectionPredicate.StateContactPolicy unrelatedPolicy =
                (sourceState, sourcePos, otherState, otherPos, face) ->
                        Decision.BYPASS_UNRELATED;

        TrackingPredicate positive = new TrackingPredicate(true);
        var positiveFilter = new ContactFilteringConnectionPredicate(positive, unrelatedPolicy);
        assertTrue(positiveFilter.shouldConnect(
                view, ORIGIN, unrelated, unrelated, EAST, Direction.UP, null));
        assertEquals(1, positive.sevenArgumentCalls.get());
        assertEquals(2, worldReads.get());

        TrackingPredicate negative = new TrackingPredicate(false);
        var negativeFilter = new ContactFilteringConnectionPredicate(negative, unrelatedPolicy);
        assertFalse(negativeFilter.shouldConnect(
                view, ORIGIN, unrelated, unrelated, EAST, Direction.UP, null));
        assertEquals(1, negative.sevenArgumentCalls.get());
        assertEquals(2, worldReads.get(), "A negative delegate must short-circuit before world reads");
    }

    @Test
    void overlayOverloadDelegatesWithoutApplyingTheContactPolicy() {
        TrackingPredicate positive = new TrackingPredicate(true);
        var filter = new ContactFilteringConnectionPredicate(positive);
        assertTrue(filter.shouldConnect(null, null, null, null, null,
                null, null, null, null));
        assertEquals(1, positive.nineArgumentCalls.get());
        assertEquals(0, positive.sevenArgumentCalls.get());
    }

    @Test
    void managedConnectionsFailClosedWithoutExactQuadContext() {
        AtomicInteger exactEvaluations = new AtomicInteger();
        assertFalse(ContactFilteringConnectionPredicate.allowsByContactPolicy(
                Decision.CONNECT, null, () -> {
                    exactEvaluations.incrementAndGet();
                    return Decision.CONNECT;
                }));
        assertEquals(0, exactEvaluations.get());

        var invalidCapture = new ContinuityQuadContext.Capture(null);
        assertFalse(ContactFilteringConnectionPredicate.allowsByContactPolicy(
                Decision.CONNECT, invalidCapture, () -> Decision.CONNECT));

        var surface = new QuadSurface(Direction.UP, 16,
                Direction.Axis.X, 0, 16, Direction.Axis.Z, 0, 16);
        var validCapture = new ContinuityQuadContext.Capture(surface);
        assertTrue(ContactFilteringConnectionPredicate.allowsByContactPolicy(
                Decision.CONNECT, validCapture, () -> Decision.CONNECT));
        assertFalse(ContactFilteringConnectionPredicate.allowsByContactPolicy(
                Decision.CONNECT, validCapture, () -> Decision.NO_BOUNDARY_CONTACT));
    }

    private static BlockAndTintGetter viewReturning(BlockState state, AtomicInteger reads) {
        return (BlockAndTintGetter) Proxy.newProxyInstance(
                ContactFilteringConnectionPredicateTest.class.getClassLoader(),
                new Class<?>[] {BlockAndTintGetter.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("getBlockState")) {
                        reads.incrementAndGet();
                        return state;
                    }
                    if (method.getName().equals("toString")) return "BGE CTM test view";
                    if (method.getReturnType() == boolean.class) return false;
                    if (method.getReturnType() == byte.class) return (byte) 0;
                    if (method.getReturnType() == short.class) return (short) 0;
                    if (method.getReturnType() == int.class) return 0;
                    if (method.getReturnType() == long.class) return 0L;
                    if (method.getReturnType() == float.class) return 0.0F;
                    if (method.getReturnType() == double.class) return 0.0D;
                    if (method.getReturnType() == char.class) return '\0';
                    return null;
                });
    }

    private static final class TrackingPredicate implements ConnectionPredicate {
        private final boolean result;
        private final AtomicInteger sevenArgumentCalls = new AtomicInteger();
        private final AtomicInteger nineArgumentCalls = new AtomicInteger();

        private TrackingPredicate(boolean result) {
            this.result = result;
        }

        @Override
        public boolean shouldConnect(BlockAndTintGetter level, BlockPos pos,
                BlockState appearanceState, BlockState state, BlockPos otherPos,
                BlockState otherAppearanceState, BlockState otherState, Direction face,
                TextureAtlasSprite quadSprite) {
            nineArgumentCalls.incrementAndGet();
            return result;
        }

        @Override
        public boolean shouldConnect(BlockAndTintGetter level, BlockPos pos,
                BlockState appearanceState, BlockState state, BlockPos otherPos, Direction face,
                TextureAtlasSprite quadSprite) {
            sevenArgumentCalls.incrementAndGet();
            return result;
        }
    }
}
