package dev.resivore.amc;

import dev.resivore.amc.mixin.DoorConstructor;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.kikoz.mcwdoors.objects.JapaneseDoors;
import net.kikoz.mcwdoors.objects.StableDoor;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.ArrayList;
import java.util.List;

final class MacawDoorBlocks {
    private static final String AMC = "architectural_material_closure";

    static void registerPack(String provider) {
        var container = FabricLoader.getInstance().getModContainer(AMC).orElseThrow();
        if (!ResourceLoader.registerBuiltinPack(Identifier.fromNamespaceAndPath(AMC, provider + "_doors"),
                container, PackActivationType.ALWAYS_ENABLED)) throw new IllegalStateException(provider + " door pack missing");
        var items = new ArrayList<Item>();
        var woods = switch (provider) {
            case "enderscape" -> List.of("veiled", "celestial", "murublight");
            case "ribbits" -> List.of("mossy_oak");
            case "mynx_trees" -> List.of("silver_birch", "wisteria");
            default -> throw new IllegalArgumentException(provider);
        };
        for (String wood : woods) {
            var glass = wood.equals("celestial") || wood.equals("murublight") ? "stem_glass" : "bark_glass";
            for (String design : List.of("bamboo", glass, "barn", "barn_glass", "beach", "classic",
                    "cottage", "four_panel", "glass", "japanese", "japanese2", "modern", "mystic",
                    "nether", "paper", "stable", "stable_head", "swamp", "tropical", "waffle", "western", "whispering")) {
                String reference = switch (design) {
                    case "classic" -> "spruce";
                    case "stem_glass" -> "warped";
                    default -> "oak";
                };
                var id = Identifier.fromNamespaceAndPath(AMC, wood + "_" + design + "_door");
                var sourceId = Identifier.fromNamespaceAndPath("mcwdoors", reference + "_" + design + "_door");
                var source = (DoorBlock) BuiltInRegistries.BLOCK.getOptional(sourceId).orElseThrow();
                var key = ResourceKey.create(Registries.BLOCK, id);
                var properties = BlockBehaviour.Properties.ofFullCopy(source).setId(key);
                DoorBlock block;
                if (source.getClass() == JapaneseDoors.class) block = new JapaneseDoors(properties, source.type());
                else if (source.getClass() == StableDoor.class) block = new StableDoor(properties, source.type());
                else if (source.getClass() == DoorBlock.class) block = DoorConstructor.create(source.type(), properties);
                else throw new IllegalStateException("Unsupported door class for " + sourceId);
                Registry.register(BuiltInRegistries.BLOCK, key, block);
                var item = ResourceKey.create(Registries.ITEM, id);
                items.add(Registry.register(BuiltInRegistries.ITEM, item, new BlockItem(block, new Item.Properties().setId(item))));
            }
        }
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS).register(out -> items.forEach(out::accept));
    }
}
