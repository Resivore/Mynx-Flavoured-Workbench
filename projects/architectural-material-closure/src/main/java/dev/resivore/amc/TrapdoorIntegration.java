package dev.resivore.amc;

import dev.resivore.amc.mixin.TrapdoorAccess;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class TrapdoorIntegration {
    private static final String AMC = "architectural_material_closure";
    private static final Set<String> WOODS_READY = new LinkedHashSet<>();
    private static boolean macawReady;
    private static final List<String> DESIGNS = List.of("bamboo", "bark", "barn", "barred", "barrel",
            "beach", "blossom", "classic", "cottage", "four_panel", "glass", "mystic", "paper",
            "ranch", "swamp", "tropical", "whispering");

    public static void trapdoorsReady() {
        BambooBlocks.registerRanch();
        macawReady = true;
        WOODS_READY.forEach(TrapdoorIntegration::registerPack);
    }

    public static void woodReady(String provider) {
        if (WOODS_READY.add(provider) && macawReady) registerPack(provider);
    }

    private static void registerPack(String provider) {
        var container = FabricLoader.getInstance().getModContainer(AMC).orElseThrow();
        var pack = Identifier.fromNamespaceAndPath(AMC, provider + "_trapdoors");
        if (!ResourceLoader.registerBuiltinPack(pack, container, PackActivationType.ALWAYS_ENABLED))
            throw new IllegalStateException("Missing trapdoor pack " + pack);
        var items = new ArrayList<Item>();
        var woods = switch (provider) {
            case "enderscape" -> List.of("veiled", "celestial", "murublight");
            case "ribbits" -> List.of("mossy_oak");
            case "mynx_trees" -> List.of("silver_birch", "wisteria");
            default -> throw new IllegalArgumentException(provider);
        };
        for (String wood : woods) {
            boolean stem = wood.equals("celestial") || wood.equals("murublight");
            for (String design : DESIGNS) {
                String ref = switch (design) {
                    case "classic" -> "spruce";
                    case "bark", "ranch" -> stem ? "warped" : "oak";
                    default -> "oak";
                };
                String suffix = "_" + design + "_trapdoor";
                var original = Identifier.fromNamespaceAndPath("mcwtrpdoors", ref + suffix);
                var source = BuiltInRegistries.BLOCK.getOptional(original).orElseThrow();
                if (source.getClass() != TrapDoorBlock.class) throw new IllegalStateException("Unexpected class for " + original);
                var id = Identifier.fromNamespaceAndPath(AMC, wood + suffix);
                var key = ResourceKey.create(Registries.BLOCK, id);
                var block = TrapdoorAccess.create(((TrapdoorAccess) source).amc$type(), BlockBehaviour.Properties.ofFullCopy(source).setId(key));
                Registry.register(BuiltInRegistries.BLOCK, key, block);
                var item = ResourceKey.create(Registries.ITEM, id);
                items.add(Registry.register(BuiltInRegistries.ITEM, item, new BlockItem(block, new Item.Properties().setId(item))));
            }
        }
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS).register(out -> items.forEach(out::accept));
    }
}
