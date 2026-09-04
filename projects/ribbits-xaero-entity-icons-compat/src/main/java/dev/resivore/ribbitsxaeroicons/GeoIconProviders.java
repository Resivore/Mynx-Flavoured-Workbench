package dev.resivore.ribbitsxaeroicons;

import java.util.List;
import java.util.Optional;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;

/** Deliberately closed registration list for Canary 1. */
public final class GeoIconProviders {
    private static final RibbitGeoIconProvider RIBBIT_PROVIDER = new RibbitGeoIconProvider();
    private static final List<GeoIconProvider> PROVIDERS = List.of(RIBBIT_PROVIDER);

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
        return RIBBIT_PROVIDER.cacheIdentity(entity, renderer, renderState);
    }
}
