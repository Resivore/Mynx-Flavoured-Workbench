package dev.resivore.amc;

import com.starfish_studios.bbb.block.ColumnBlock;
import com.starfish_studios.bbb.block.FrameBlock;
import com.starfish_studios.bbb.block.MouldingBlock;
import com.starfish_studios.bbb.block.StoneFenceBlock;
import com.starfish_studios.bbb.block.UrnBlock;
import com.starfish_studios.bbb.item.DescriptionBlockItem;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;

import java.util.List;
import java.util.function.Function;

public final class MasonryBlocks implements ModInitializer {
    @Override
    public void onInitialize() {
        var items = List.of(
                register("column", ColumnBlock::new),
                register("urn", props -> new UrnBlock(props.pushReaction(PushReaction.DESTROY))),
                register("moulding", props -> new MouldingBlock(Blocks.ANDESITE.defaultBlockState(), props)),
                register("fence", StoneFenceBlock::new),
                register("frame", props -> new FrameBlock(props.noCollision().pushReaction(PushReaction.DESTROY))));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS)
                .register(output -> items.forEach(output::accept));
    }

    private static Item register(String form, Function<BlockBehaviour.Properties, Block> factory) {
        var id = Identifier.fromNamespaceAndPath("architectural_material_closure", "andesite_" + form);
        var key = ResourceKey.create(Registries.BLOCK, id);
        var props = BlockBehaviour.Properties.ofFullCopy(Blocks.ANDESITE).setId(key).noOcclusion();
        var block = Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(props));
        var itemKey = ResourceKey.create(Registries.ITEM, id);
        return Registry.register(BuiltInRegistries.ITEM, itemKey,
                new DescriptionBlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
    }
}
