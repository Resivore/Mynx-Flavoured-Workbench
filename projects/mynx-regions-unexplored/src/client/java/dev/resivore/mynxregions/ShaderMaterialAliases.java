package dev.resivore.mynxregions;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.Collection;
import java.util.function.Function;

/** Adds a shader material only where the active pack supplied one for the chosen vanilla semantic counterpart. */
public final class ShaderMaterialAliases {
    private ShaderMaterialAliases() { }
    public static <S> void inheritUnmapped(Object2IntMap<S> ids, Collection<S> customStates, Function<S, S> counterpart) {
        for (S custom : customStates) {
            if (ids.containsKey(custom)) continue;
            S vanilla = counterpart.apply(custom);
            if (ids.containsKey(vanilla)) ids.put(custom, ids.getInt(vanilla));
        }
    }
}
