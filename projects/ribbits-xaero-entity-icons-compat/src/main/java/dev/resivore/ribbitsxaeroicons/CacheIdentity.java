package dev.resivore.ribbitsxaeroicons;

import java.util.Objects;

/** Bounded visual inputs which can change a generated Ribbit icon. */
public record CacheIdentity(
        String entityType,
        String providerId,
        String selectorVersion,
        String modelId,
        String textureId,
        String professionId,
        String babyPolicy,
        boolean pride,
        long reloadGeneration) {
    public CacheIdentity {
        Objects.requireNonNull(entityType, "entityType");
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(selectorVersion, "selectorVersion");
        Objects.requireNonNull(modelId, "modelId");
        Objects.requireNonNull(textureId, "textureId");
        Objects.requireNonNull(professionId, "professionId");
        Objects.requireNonNull(babyPolicy, "babyPolicy");
    }
}
