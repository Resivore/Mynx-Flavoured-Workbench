package dev.aero.shulkertrowel.item;

import dev.aero.shulkertrowel.ShulkerTrowel;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

public final class ModItems {
    private ModItems() {}

    private static final ResourceKey<Item> TROWEL_KEY = ResourceKey.create(
            Registries.ITEM,
            Identifier.fromNamespaceAndPath(ShulkerTrowel.MOD_ID, "trowel")
    );

    public static final ShulkerTrowelItem TROWEL = Registry.register(
            BuiltInRegistries.ITEM,
            TROWEL_KEY,
            new ShulkerTrowelItem(new Item.Properties().stacksTo(1).setId(TROWEL_KEY))
    );

    public static void initialize() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
                .register(output -> output.accept(TROWEL));
    }
}
