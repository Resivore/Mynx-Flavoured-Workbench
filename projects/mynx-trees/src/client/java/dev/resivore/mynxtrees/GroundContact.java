package dev.resivore.mynxtrees;

import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;

/** Bounded render-context selection, with no retained world or tag-membership cache. */
final class GroundContact {
    private GroundContact() { }

    static <S> boolean matches(S state, BlockPos pos, Function<BlockPos, S> snapshot,
                              Predicate<S> uprightLog, Predicate<S> soil) {
        // A nearest-position entity render is not evidence of an actual placed log.
        return pos != null && snapshot != null && uprightLog.test(state)
                && state.equals(snapshot.apply(pos)) && soil.test(snapshot.apply(pos.below()));
    }

    record GeometryKey(Object owner, boolean grounded, Object delegate) { }

    static Object geometryKey(Object owner, boolean grounded, Object delegate) {
        return delegate == null ? null : new GeometryKey(owner, grounded, delegate);
    }
}
