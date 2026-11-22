package com.fizzware.dramaticdoors.fabric;

import com.fizzware.dramaticdoors.compat.DDCompatRecipe;
import com.fizzware.dramaticdoors.registry.DDNames;
import com.fizzware.dramaticdoors.registry.DDRegistry;
import net.fabricmc.fabric.api.event.registry.RegistryEntryAddedCallback;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.DoorBlock;

/** Optional pairs are published only once their exact provider block and matching item exist. */
final class NativeDoorCompat {
    static void initialize() {
        if (FabricUtils.INSTANCE.isModLoaded("enderscape")) {
            watch("enderscape:celestial_door", DDNames.SHORT_CELESTIAL, DDNames.TALL_CELESTIAL);
            watch("enderscape:murublight_door", DDNames.SHORT_MURUSHROOM, DDNames.TALL_MURUSHROOM);
            watch("enderscape:veiled_door", DDNames.SHORT_VEILED, DDNames.TALL_VEILED);
        }
        if (FabricUtils.INSTANCE.isModLoaded("ribbits")) {
            watch("ribbits:mossy_oak_door", DDNames.SHORT_MOSSY_OAK, DDNames.TALL_MOSSY_OAK);
        }
    }

    private static void watch(String sourceId, String shortName, String tallName) {
        Identifier id = Identifier.parse(sourceId);
        // Both providers register the block before its BlockItem. An earlier initializer
        // is already in the item registry; a later one reaches the same path through Fabric.
        var existing = BuiltInRegistries.ITEM.getOptional(id);
        if (existing.isPresent()) {
            register(id, existing.get(), shortName, tallName);
        } else {
            RegistryEntryAddedCallback.event(BuiltInRegistries.ITEM).register((rawId, added, item) -> {
                if (id.equals(added)) register(id, item, shortName, tallName);
            });
        }
    }

    private static void register(Identifier id, Item item, String shortName, String tallName) {
        if (!(item instanceof BlockItem sourceItem) || !(sourceItem.getBlock() instanceof DoorBlock source)
                || BuiltInRegistries.BLOCK.getOptional(id).orElse(null) != source) return;

        registerDoor(shortName, source, false, id.getNamespace().equals("ribbits"));
        registerDoor(tallName, source, true, id.getNamespace().equals("ribbits"));
        DDCompatRecipe.createNativeWoodRecipes(shortName, tallName, id);
        if (shortName.equals(DDNames.SHORT_VEILED) || shortName.equals(DDNames.SHORT_MOSSY_OAK)) {
            Identifier pack = Identifier.fromNamespaceAndPath("dramaticdoors", id.getPath());
            if (!ResourceLoader.registerBuiltinPack(pack, FabricLoader.getInstance().getModContainer("dramaticdoors").orElseThrow(),
                    Component.literal("Dramatic Doors / " + id), PackActivationType.ALWAYS_ENABLED)) {
                throw new IllegalStateException("Missing Dramatic Doors resources for " + id);
            }
        }
    }

    private static void registerDoor(String name, DoorBlock source, boolean tall, boolean mossyOak) {
        BlockItem item = DDRegistry.queueNativeDoor(name, source, source.type(), tall);
        Identifier id = Identifier.fromNamespaceAndPath("dramaticdoors", name);
        Registry.register(BuiltInRegistries.BLOCK, id, item.getBlock());
        Registry.register(BuiltInRegistries.ITEM, id, item);
        if (mossyOak) FlammableBlockRegistry.getDefaultInstance().add(item.getBlock(), 5, 20);
    }
}
