package com.crispytwig.naturalist.client;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.client.renderer.entity.state.ParrotRenderState;

/**
 * Tracks the ephemeral shoulder-parrot states that need Naturalist's flight pose. Model setup is
 * deferred in 26.2, so the decision must follow the queued state rather than a frame-global flag.
 */
public final class NaturalistParrotRenderStateLookup {
    private static final Set<ParrotRenderState> FLYING_SHOULDERS =
            Collections.newSetFromMap(Collections.synchronizedMap(new WeakHashMap<>()));

    private NaturalistParrotRenderStateLookup() {
    }

    public static void markFlyingShoulder(ParrotRenderState state) {
        FLYING_SHOULDERS.add(state);
    }

    public static boolean isFlyingShoulder(ParrotRenderState state) {
        return FLYING_SHOULDERS.contains(state);
    }
}
