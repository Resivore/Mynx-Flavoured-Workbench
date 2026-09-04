package com.yungnickyoung.minecraft.ribbits.fabric.client;

import com.yungnickyoung.minecraft.ribbits.client.render.WanderingRibbitRenderer;
import com.yungnickyoung.minecraft.ribbits.module.EntityTypeModule;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

/** Explicit client-only hook for the root Fabric client initializer. */
public final class WanderingRibbitClientHooks {
    private static boolean registered;

    private WanderingRibbitClientHooks() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        EntityRendererRegistry.register(
                EntityTypeModule.WANDERING_RIBBIT.get(), WanderingRibbitRenderer::new);
    }
}
