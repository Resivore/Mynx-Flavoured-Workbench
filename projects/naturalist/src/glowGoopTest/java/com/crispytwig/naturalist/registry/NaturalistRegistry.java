package com.crispytwig.naturalist.registry;

import com.crispytwig.naturalist.platform.registry.DeferredHolder;
import com.crispytwig.naturalist.server.item.GlowGoopItem;
import net.minecraft.world.item.Item;

/**
 * Test-only holder fixture, ahead of main classes on this suite's classpath.
 * Production GlowGoopBlock, GlowGoopItem, and FabricRegistrationProvider are
 * exercised unchanged without bootstrapping unrelated Naturalist registrations.
 */
public final class NaturalistRegistry {
    public static DeferredHolder<Item, GlowGoopItem> GLOW_GOOP;
}
