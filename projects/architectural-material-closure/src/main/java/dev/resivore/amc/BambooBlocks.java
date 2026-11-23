package dev.resivore.amc;

import dev.resivore.amc.mixin.TrapdoorAccess;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.kikoz.mcwwindows.init.BlockInit;
import net.kikoz.mcwwindows.objects.Blinds;
import net.kikoz.mcwwindows.objects.CurtainRod;
import net.kikoz.mcwwindows.objects.Parapet;
import net.kikoz.mcwwindows.objects.Shutter;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.Function;

public final class BambooBlocks implements ModInitializer {
    private static final String AMC = "architectural_material_closure";

    @Override
    public void onInitialize() {
        register("log_parapet", BlockInit.OAK_LOG_PARAPET, Parapet::new);
        register("plank_parapet", BlockInit.OAK_PLANK_PARAPET, Parapet::new);
        register("blinds", BlockInit.OAK_BLINDS, Blinds::new);
        register("curtain_rod", BlockInit.OAK_CURTAIN_ROD, CurtainRod::new);
        register("louvered_shutter", BlockInit.OAK_LOUVERED_SHUTTER, Shutter::new);
    }

    static void registerRanch() {
        var container = FabricLoader.getInstance().getModContainer(AMC).orElseThrow();
        var pack = Identifier.fromNamespaceAndPath(AMC, "bamboo_trapdoors");
        if (!ResourceLoader.registerBuiltinPack(pack, container, PackActivationType.ALWAYS_ENABLED))
            throw new IllegalStateException("Missing Bamboo Ranch pack");
        var source = BuiltInRegistries.BLOCK.getOptional(Identifier.parse("mcwtrpdoors:oak_ranch_trapdoor")).orElseThrow();
        register("ranch_trapdoor", source, props -> TrapdoorAccess.create(((TrapdoorAccess) source).amc$type(), props));
    }

    private static void register(String form, Block source, Function<BlockBehaviour.Properties, Block> create) {
        var id = Identifier.fromNamespaceAndPath(AMC, "bamboo_" + form);
        var key = ResourceKey.create(Registries.BLOCK, id);
        var block = Registry.register(BuiltInRegistries.BLOCK, key,
                create.apply(BlockBehaviour.Properties.ofFullCopy(source).setId(key)));
        var itemKey = ResourceKey.create(Registries.ITEM, id);
        var item = Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(block, new Item.Properties().setId(itemKey)));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS).register(output -> output.accept(item));
    }
}
