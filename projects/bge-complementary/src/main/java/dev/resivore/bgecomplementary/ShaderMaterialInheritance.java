package dev.resivore.bgecomplementary;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/** Pure fallback algorithm shared by the live BGE bridge and focused synthetic tests. */
public final class ShaderMaterialInheritance {
    private ShaderMaterialInheritance() {}

    /**
     * Copies an existing parent classification only into physical entries absent from a completed
     * shader-pack map. {@code containsKey}, rather than an ID sentinel, preserves explicit zero
     * and any other pack-owned assignment. The caller supplies BGE's authoritative projection;
     * this algorithm deliberately has no material-category eligibility policy.
     */
    public static <S, V> void inheritMissing(Map<S, V> classifications, Iterable<S> physicalStates,
                                               Function<S, Optional<S>> canonicalState) {
        for (S physical : physicalStates) {
            if (classifications.containsKey(physical)) {
                continue;
            }
            Optional<S> parent = canonicalState.apply(physical);
            if (parent.isEmpty()) {
                continue;
            }
            if (!classifications.containsKey(parent.get())) {
                continue;
            }
            classifications.put(physical, classifications.get(parent.get()));
        }
    }
}
