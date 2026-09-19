package dev.resivore.dragonbound;

import dev.resivore.dragonbound.block.DragonboundWaystoneBlock;
import dev.resivore.dragonbound.block.DragonboundWaystoneBlockEntity;
import dev.resivore.dragonbound.channel.DragonboundReturnItem;
import dev.resivore.dragonbound.channel.ReturnSource;
import dev.resivore.dragonbound.material.WaystoneMaterial;
import dev.resivore.dragonbound.recipe.WaystoneMaterialRecipe;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.Set;

public final class DragonboundContent {
    public static final Identifier WAYSTONE_ID = id("dragonbound_waystone");
    public static final Identifier IMBUED_VOID_PEARL_ID = id("imbued_void_pearl");
    public static final Identifier DRAGONBOUND_STAFF_ID = id("dragonbound_staff");

    private static final ResourceKey<Block> WAYSTONE_BLOCK_KEY = ResourceKey.create(Registries.BLOCK, WAYSTONE_ID);
    private static final ResourceKey<BlockEntityType<?>> WAYSTONE_BLOCK_ENTITY_KEY =
            ResourceKey.create(Registries.BLOCK_ENTITY_TYPE, WAYSTONE_ID);
    private static final ResourceKey<Item> WAYSTONE_ITEM_KEY = ResourceKey.create(Registries.ITEM, WAYSTONE_ID);
    private static final ResourceKey<Item> IMBUED_VOID_PEARL_KEY =
            ResourceKey.create(Registries.ITEM, IMBUED_VOID_PEARL_ID);
    private static final ResourceKey<Item> DRAGONBOUND_STAFF_KEY =
            ResourceKey.create(Registries.ITEM, DRAGONBOUND_STAFF_ID);
    private static final ResourceKey<CreativeModeTab> TOOLS_AND_UTILITIES_TAB = ResourceKey.create(
            Registries.CREATIVE_MODE_TAB,
            Identifier.withDefaultNamespace("tools_and_utilities"));

    public static final DragonboundWaystoneBlock WAYSTONE = new DragonboundWaystoneBlock(
            BlockBehaviour.Properties.of().setId(WAYSTONE_BLOCK_KEY), DragonboundContent::waystoneBlockEntityType);
    public static final BlockEntityType<DragonboundWaystoneBlockEntity> WAYSTONE_BLOCK_ENTITY =
            new BlockEntityType<>(DragonboundWaystoneBlockEntity::new, Set.of(WAYSTONE));
    public static final Item WAYSTONE_ITEM = new BlockItem(WAYSTONE, new Item.Properties()
            .setId(WAYSTONE_ITEM_KEY)
            .stacksTo(1)
            .fireResistant()
            .useBlockDescriptionPrefix());
    public static final Item IMBUED_VOID_PEARL = new DragonboundReturnItem(
            new Item.Properties().setId(IMBUED_VOID_PEARL_KEY),
            ReturnSource.IMBUED_VOID_PEARL);
    public static final Item DRAGONBOUND_STAFF = new DragonboundReturnItem(
            new Item.Properties().setId(DRAGONBOUND_STAFF_KEY).stacksTo(1),
            ReturnSource.DRAGONBOUND_STAFF);
    public static final RecipeSerializer<WaystoneMaterialRecipe> WAYSTONE_MATERIAL_RECIPE =
            new RecipeSerializer<>(WaystoneMaterialRecipe.CODEC, WaystoneMaterialRecipe.STREAM_CODEC);

    private DragonboundContent() {
    }

    public static void register() {
        WaystoneMaterial.register();
        Registry.register(BuiltInRegistries.BLOCK, WAYSTONE_BLOCK_KEY, WAYSTONE);
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, WAYSTONE_BLOCK_ENTITY_KEY, WAYSTONE_BLOCK_ENTITY);
        Registry.register(BuiltInRegistries.ITEM, WAYSTONE_ITEM_KEY, WAYSTONE_ITEM);
        Registry.register(BuiltInRegistries.ITEM, IMBUED_VOID_PEARL_KEY, IMBUED_VOID_PEARL);
        Registry.register(BuiltInRegistries.ITEM, DRAGONBOUND_STAFF_KEY, DRAGONBOUND_STAFF);
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, id("waystone_material"), WAYSTONE_MATERIAL_RECIPE);

        CreativeModeTabEvents.modifyOutputEvent(TOOLS_AND_UTILITIES_TAB).register(output -> {
            output.accept(WAYSTONE_ITEM);
            output.accept(IMBUED_VOID_PEARL);
            output.accept(DRAGONBOUND_STAFF);
        });
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(DragonboundWaystone.MOD_ID, path);
    }

    private static BlockEntityType<DragonboundWaystoneBlockEntity> waystoneBlockEntityType() {
        return WAYSTONE_BLOCK_ENTITY;
    }
}
