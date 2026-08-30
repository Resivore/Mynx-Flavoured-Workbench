package dev.resivore.matchaheart;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

/** Registered item content owned by the unified Matcha heart compatibility project. */
public final class HeartItems {
    public static final Identifier RESONANT_FAVOUR_ID =
            Identifier.fromNamespaceAndPath(MatchaHeartDeathCompat.MOD_ID, "resonant_favour");
    public static final ResourceKey<Item> RESONANT_FAVOUR_KEY =
            ResourceKey.create(Registries.ITEM, RESONANT_FAVOUR_ID);
    public static final Item RESONANT_FAVOUR =
            new Item(new Item.Properties().setId(RESONANT_FAVOUR_KEY));

    private HeartItems() {}

    public static void register() {
        Registry.register(BuiltInRegistries.ITEM, RESONANT_FAVOUR_KEY, RESONANT_FAVOUR);
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS)
                .register(output -> output.accept(RESONANT_FAVOUR));
    }
}
