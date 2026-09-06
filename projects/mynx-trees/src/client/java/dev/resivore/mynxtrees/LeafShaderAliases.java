package dev.resivore.mynxtrees;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.Collection;
import java.util.function.Function;

/** Fill only missing shader assignments, inheriting the pack's own material choices. */
public final class LeafShaderAliases {
    private LeafShaderAliases() {}

    public static <S> void inheritUnmapped(Object2IntMap<S> ids, Collection<S> customStates,
                                           Function<S, S> vanillaState) {
        for (S custom : customStates) {
            if (ids.containsKey(custom)) continue;
            S vanilla = vanillaState.apply(custom);
            if (ids.containsKey(vanilla)) ids.put(custom, ids.getInt(vanilla));
        }
    }

    /** Fill missing entries from the first shader-pack-mapped representative. */
    public static <S> void inheritUnmappedFromFirstPresent(Object2IntMap<S> ids, Collection<S> customStates,
                                                            Collection<S> representatives) {
        S representative = null;
        for (S candidate : representatives) {
            if (ids.containsKey(candidate)) {
                representative = candidate;
                break;
            }
        }
        if (representative == null) return;
        int material = ids.getInt(representative);
        for (S custom : customStates) {
            if (!ids.containsKey(custom)) ids.put(custom, material);
        }
    }
}
