package dev.resivore.ribbitsxaeroicons;

import java.util.List;
import java.util.Optional;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;

/** Deliberately closed registration list for ordinary and Wandering Ribbits. */
public final class GeoIconProviders {
    private static final RibbitGeoIconProvider RIBBIT_PROVIDER = new RibbitGeoIconProvider();
    private static final RibbitGeoIconProvider WANDERING_PROVIDER = new RibbitGeoIconProvider(true);
    private static final List<GeoIconProvider> PROVIDERS = List.of(RIBBIT_PROVIDER, WANDERING_PROVIDER);

    private GeoIconProviders() {
    }

    public static Optional<GeoIconProvider> find(
            Entity entity,
            EntityRenderer<?, ?> renderer,
            EntityRenderState renderState,
            boolean upstreamHandled) {
        return PROVIDERS.stream()
                .filter(provider -> provider.supports(
                        entity, renderer, renderState, upstreamHandled))
                .findFirst();
    }

    /**
     * Resolves a key even for an unsupported or temporarily invalid exact Ribbit state. This keeps
     * Xaero's per-entity variant class invariant intact; the provider supplies a bounded failure
     * sentinel when full resolution is unsafe.
     */
    public static CacheIdentity cacheIdentityForOwned(
            Entity entity, EntityRenderer<?, ?> renderer, EntityRenderState renderState) {
        if (!RibbitGeoIconProvider.owns(entity)) {
            throw new IllegalArgumentException("entity type is not owned by the Ribbits provider");
        }
        return (net.minecraft.world.entity.EntityType.getKey(entity.getType()).toString()
                .equals(RibbitGeoIconProvider.WANDERING_ENTITY_TYPE) ? WANDERING_PROVIDER : RIBBIT_PROVIDER)
                .cacheIdentity(entity, renderer, renderState);
    }
}
