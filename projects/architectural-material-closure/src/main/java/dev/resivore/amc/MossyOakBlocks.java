package dev.resivore.amc;

import com.starfish_studios.bbb.block.*;
import com.starfish_studios.bbb.item.DescriptionBlockItem;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.List;
import java.util.function.Function;

public final class MossyOakBlocks {
    private static final String NS = "architectural_material_closure";

    private static boolean bbbReady;
    private static boolean ribbitsReady;
    private static boolean treesReady;

    public static void afterTrees() {
        if (!BuiltInRegistries.BLOCK.containsKey(Identifier.parse("mynx_trees:wisteria_planks"))) return;
        treesReady = true;
        if (bbbReady) registerTrees();
        DoorIntegration.woodReady("mynx_trees");
        TrapdoorIntegration.woodReady("mynx_trees");
    }

    private static void registerTrees() {
        var container = FabricLoader.getInstance().getModContainer(NS).orElseThrow();
        if (!ResourceLoader.registerBuiltinPack(Identifier.fromNamespaceAndPath(NS,"mynx_trees"),container,PackActivationType.ALWAYS_ENABLED))
            throw new IllegalStateException("Missing Mynx Trees pack");
        for (String wood : List.of("silver_birch", "wisteria")) {
            WoodAccessories.register(wood);
            details(wood);
        }
    }

    public static void afterBbb() {
        bbbReady = true;
        if (treesReady) registerTrees();
        if (ribbitsReady) register();
    }

    public static void afterRibbits() {
        ribbitsReady = true;
        if (bbbReady) register();
    }

    private static void register() {
        for (String name : List.of("mossy_oak_planks", "mossy_oak_planks_slab")) {
            var id = Identifier.fromNamespaceAndPath("ribbits", name);
            var block = BuiltInRegistries.BLOCK.getOptional(id).orElseThrow();
            if (BuiltInRegistries.ITEM.getOptional(id).orElseThrow() != block.asItem()) {
                throw new IllegalStateException("Missing Ribbits block item " + id);
            }
        }
        var container = FabricLoader.getInstance().getModContainer(NS).orElseThrow();
        if (!ResourceLoader.registerBuiltinPack(Identifier.fromNamespaceAndPath(NS, "ribbits"),
                container, PackActivationType.ALWAYS_ENABLED)) throw new IllegalStateException("Missing Ribbits pack");
        // Exactly one recipe per ID, independent of pack sorting or initializer order.
        boolean nativeTimber = FabricLoader.getInstance().getModContainer("mynx_trees")
                .flatMap(mod -> mod.findPath("assets/mynx_trees/blockstates/mossy_oak_log.json")).isPresent();
        String recipes = nativeTimber ? "mossy_oak_timber" : "mossy_oak_legacy_recipes";
        if (!ResourceLoader.registerBuiltinPack(Identifier.fromNamespaceAndPath(NS, recipes), container, PackActivationType.ALWAYS_ENABLED))
            throw new IllegalStateException("Missing recipe pack " + recipes);
        WoodAccessories.register("mossy_oak");
        MossyOakFixtures.register();
        WoodBoats.register("mossy_oak");
        details("mossy_oak");
    }

    private static void details(String wood) {
        var items = List.of(register(wood, "lantern", WoodenLanternBlock::new), register(wood, "frame", FrameBlock::new), register(wood, "lattice", LatticeBlock::new),
                register(wood, "trim", FacingConnectingBlock::new), register(wood, "balustrade", BalustradeBlock::new),
                register(wood, "support", SupportBlock::new), register(wood, "pallet", PalletBlock::new));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS).register(out -> items.forEach(out::accept));
    }

    private static Item register(String wood, String form, Function<BlockBehaviour.Properties, Block> factory) {
        var oak = BuiltInRegistries.BLOCK.getOptional(Identifier.fromNamespaceAndPath("bbb", "oak_" + form)).orElseThrow();
        var id = Identifier.fromNamespaceAndPath(NS, wood + "_" + form);
        var key = ResourceKey.create(Registries.BLOCK, id);
        var block = Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(BlockBehaviour.Properties.ofFullCopy(oak).setId(key)));
        var item = ResourceKey.create(Registries.ITEM, id);
        return Registry.register(BuiltInRegistries.ITEM, item, new DescriptionBlockItem(block,
                new Item.Properties().setId(item).useBlockDescriptionPrefix()));
    }
}
