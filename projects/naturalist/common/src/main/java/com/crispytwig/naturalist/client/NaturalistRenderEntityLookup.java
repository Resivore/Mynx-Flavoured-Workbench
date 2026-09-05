package com.crispytwig.naturalist.client;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/** Runtime bridge shared by transformed render classes; must stay outside mixin packages. */
public final class NaturalistRenderEntityLookup {
    private static final Map<EntityRenderState, Source> SOURCES =
            Collections.synchronizedMap(new WeakHashMap<>());

    private NaturalistRenderEntityLookup() {
    }

    public static void remember(EntityRenderState state, Entity entity, float partialTick) {
        if (state != null) {
            SOURCES.put(state, new Source(entity, partialTick));
        }
    }

    public static Entity source(EntityRenderState state) {
        Source source = SOURCES.get(state);
        return source == null ? null : source.entity();
    }

    public static float partialTick(EntityRenderState state) {
        Source source = SOURCES.get(state);
        return source == null ? 0.0F : source.partialTick();
    }

    private record Source(Entity entity, float partialTick) {
    }
}
