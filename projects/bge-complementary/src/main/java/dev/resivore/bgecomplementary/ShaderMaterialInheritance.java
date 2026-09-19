package dev.resivore.bgecomplementary;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/** Pure fallback algorithm shared by the live BGE bridge and focused synthetic tests. */
public final class ShaderMaterialInheritance {
    private ShaderMaterialInheritance() {}

    public record Result(int inherited, int explicitPhysical, int missingCanonical,
                         int missingParent) {
        public Result plus(Result other) {
            return new Result(inherited + other.inherited,
                    explicitPhysical + other.explicitPhysical,
                    missingCanonical + other.missingCanonical,
                    missingParent + other.missingParent);
        }

        public static Result empty() {
            return new Result(0, 0, 0, 0);
        }
    }

    /**
     * Copies an existing parent classification only into physical entries absent from a completed
     * shader-pack map. {@code containsKey}, rather than an ID sentinel, preserves explicit zero
     * and any other pack-owned assignment. The caller supplies BGE's authoritative projection;
     * this algorithm deliberately has no material-category eligibility policy.
     */
    public static <S, V> Result inheritMissing(Map<S, V> classifications, Iterable<S> physicalStates,
                                               Function<S, Optional<S>> canonicalState) {
        Result result = Result.empty();
        for (S physical : physicalStates) {
            if (classifications.containsKey(physical)) {
                result = result.plus(new Result(0, 1, 0, 0));
                continue;
            }
            Optional<S> parent = canonicalState.apply(physical);
            if (parent.isEmpty()) {
                result = result.plus(new Result(0, 0, 1, 0));
                continue;
            }
            if (!classifications.containsKey(parent.get())) {
                result = result.plus(new Result(0, 0, 0, 1));
                continue;
            }
            classifications.put(physical, classifications.get(parent.get()));
            result = result.plus(new Result(1, 0, 0, 0));
        }
        return result;
    }
}
