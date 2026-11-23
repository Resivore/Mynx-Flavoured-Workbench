package dev.resivore.amc;

import com.mcwpaths.kikoz.objects.FacingPathBlock;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.kikoz.mcwwindows.init.BlockInit;
import net.kikoz.mcwwindows.objects.*;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import java.util.function.Function;

final class WoodAccessories {
    static void register(String wood) {
        if (wood.equals("mossy_oak")) {
            register(wood,"button", Blocks.OAK_BUTTON,p -> new ButtonBlock(BlockSetType.OAK,30,p));
            register(wood,"pressure_plate", Blocks.OAK_PRESSURE_PLATE,p -> new PressurePlateBlock(BlockSetType.OAK,p));
        }
        boolean stem = wood.equals("celestial") || wood.equals("murublight");
        register(wood,"planks_path", com.mcwpaths.kikoz.init.BlockInit.OAK_PLANKS_PATH,FacingPathBlock::new);
        register(wood,stem ? "stem_parapet" : "log_parapet",stem ? BlockInit.WARPED_STEM_PARAPET : BlockInit.OAK_LOG_PARAPET,Parapet::new);
        register(wood,"plank_parapet",BlockInit.OAK_PLANK_PARAPET,Parapet::new);
        register(wood,"blinds",BlockInit.OAK_BLINDS,Blinds::new);
        register(wood,"curtain_rod",BlockInit.OAK_CURTAIN_ROD,CurtainRod::new);
        register(wood,"shutter",BlockInit.OAK_SHUTTER,Shutter::new);
        register(wood,"louvered_shutter",BlockInit.OAK_LOUVERED_SHUTTER,Shutter::new);
    }

    private static void register(String wood,String form,Block reference,Function<BlockBehaviour.Properties,Block> factory) {
        var id = Identifier.fromNamespaceAndPath("architectural_material_closure",wood+"_"+form);
        var key = ResourceKey.create(Registries.BLOCK,id);
        var block = Registry.register(BuiltInRegistries.BLOCK,key,factory.apply(BlockBehaviour.Properties.ofFullCopy(reference).setId(key)));
        var itemKey = ResourceKey.create(Registries.ITEM,id);
        boolean control = reference instanceof ButtonBlock || reference instanceof PressurePlateBlock;
        var properties = new Item.Properties().setId(itemKey);
        if (control) properties.useBlockDescriptionPrefix();
        var item = Registry.register(BuiltInRegistries.ITEM,itemKey,new BlockItem(block,properties));
        CreativeModeTabEvents.modifyOutputEvent(control ? CreativeModeTabs.REDSTONE_BLOCKS : CreativeModeTabs.BUILDING_BLOCKS)
                .register(output -> output.accept(item));
    }
}
