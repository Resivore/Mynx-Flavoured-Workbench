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

    public static void afterBbb() {
        bbbReady = true;
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
        var items = List.of(register("frame", FrameBlock::new), register("lattice", LatticeBlock::new),
                register("trim", FacingConnectingBlock::new), register("balustrade", BalustradeBlock::new),
                register("support", SupportBlock::new), register("pallet", PalletBlock::new));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS).register(out -> items.forEach(out::accept));
    }

    private static Item register(String form, Function<BlockBehaviour.Properties, Block> factory) {
        var oak = BuiltInRegistries.BLOCK.getOptional(Identifier.fromNamespaceAndPath("bbb", "oak_" + form)).orElseThrow();
        var id = Identifier.fromNamespaceAndPath(NS, "mossy_oak_" + form);
        var key = ResourceKey.create(Registries.BLOCK, id);
        var block = Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(BlockBehaviour.Properties.ofFullCopy(oak).setId(key)));
        var item = ResourceKey.create(Registries.ITEM, id);
        return Registry.register(BuiltInRegistries.ITEM, item, new DescriptionBlockItem(block,
                new Item.Properties().setId(item).useBlockDescriptionPrefix()));
    }
}
