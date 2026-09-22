package dev.resivore.enderscapeintegration.client;

import java.util.Map;

/**
 * Copies a resolved reference classification only where the shader pack did not supply a
 * physical Veiled Leaves classification. This is intentionally independent of any shader-pack
 * numeric material convention.
 */
public final class VeiledLeavesShaderMaterialFallback {
    private VeiledLeavesShaderMaterialFallback() {
    }

    public record Result(int inherited, int explicit, int missingReference) {
    }

    public static <S, V> Result inheritMissing(Map<S, V> classifications,
                                                Iterable<S> targetStates,
                                                S referenceLeafState) {
        int inherited = 0;
        int explicit = 0;
        int missingReference = 0;
        for (S target : targetStates) {
            if (classifications.containsKey(target)) {
                explicit++;
            } else if (!classifications.containsKey(referenceLeafState)) {
                missingReference++;
            } else {
                classifications.put(target, classifications.get(referenceLeafState));
                inherited++;
            }
        }
        return new Result(inherited, explicit, missingReference);
    }
}
