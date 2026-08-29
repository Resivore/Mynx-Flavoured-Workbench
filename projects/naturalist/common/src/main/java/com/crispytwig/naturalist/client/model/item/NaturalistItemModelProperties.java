package com.crispytwig.naturalist.client.model.item;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Small compatibility registry for Naturalist's three legacy numeric item properties.
 * Minecraft 26.2 removed the global ItemProperties API, so the values are consumed by
 * {@link PropertyAwareItemModel} while the original property identifiers stay intact.
 */
@Environment(EnvType.CLIENT)
public final class NaturalistItemModelProperties {
    private static final Map<Item, Map<Identifier, Property>> PROPERTIES = new IdentityHashMap<>();

    private NaturalistItemModelProperties() {
    }

    public static void register(Item item, Identifier id, Property property) {
        PROPERTIES.computeIfAbsent(item, unused -> new LinkedHashMap<>()).put(id, property);
    }

    public static Map<Item, Map<Identifier, Property>> snapshot() {
        Map<Item, Map<Identifier, Property>> copy = new IdentityHashMap<>();
        PROPERTIES.forEach((item, properties) -> copy.put(item, Map.copyOf(properties)));
        return Map.copyOf(copy);
    }

    public static float get(Item item, Identifier id, ItemStack stack, @Nullable ClientLevel level,
                            @Nullable ItemOwner owner, int seed) {
        Map<Identifier, Property> properties = PROPERTIES.get(item);
        if (properties == null) {
            return Float.NEGATIVE_INFINITY;
        }
        Property property = properties.get(id);
        return property != null ? property.call(stack, level, owner, seed) : Float.NEGATIVE_INFINITY;
    }

    @FunctionalInterface
    public interface Property {
        float unclampedCall(ItemStack stack, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed);

        default float call(ItemStack stack, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
            return Mth.clamp(this.unclampedCall(stack, level, owner, seed), 0.0F, 1.0F);
        }
    }
}
