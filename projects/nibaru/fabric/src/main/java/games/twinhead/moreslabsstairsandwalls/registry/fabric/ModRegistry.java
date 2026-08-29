package games.twinhead.moreslabsstairsandwalls.registry.fabric;

import games.twinhead.moreslabsstairsandwalls.MoreSlabsStairsAndWalls;
import games.twinhead.moreslabsstairsandwalls.block.ModBlocks;
import games.twinhead.moreslabsstairsandwalls.block.entity.FallingSlabBlockEntity;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

public class ModRegistry {

    private static final Map<Identifier, Block> REGISTERED_BLOCKS = new HashMap<>();

    private static final ResourceKey<EntityType<?>> FALLING_SLAB_ENTITY_KEY = ResourceKey.create(
            Registries.ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(MoreSlabsStairsAndWalls.MOD_ID, "falling_slab"));

    public static final EntityType<FallingSlabBlockEntity> FALLING_SLAB_BLOCK_ENTITY = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            FALLING_SLAB_ENTITY_KEY,
            EntityType.Builder.of(FallingSlabBlockEntity::new, MobCategory.MISC)
                    .sized(0.98f, 0.98f)
                    .clientTrackingRange(10)
                    .updateInterval(20)
                    .build(FALLING_SLAB_ENTITY_KEY));

    public static Block getBlock(Identifier id) {
        Block owned = REGISTERED_BLOCKS.get(id);
        return owned != null ? owned : BuiltInRegistries.BLOCK.getValue(id);
    }

    public static CreativeModeTab modGroup = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath(MoreSlabsStairsAndWalls.MOD_ID, "creative_tab")),
                   FabricCreativeModeTab.builder()
                           .icon(() -> new ItemStack(ModBlocks.GRASS_BLOCK.getBlock(ModBlocks.BlockType.STAIRS)))
            .title(Component.translatable("itemGroup.more_slabs_stairs_and_walls.creative_tab"))
            .displayItems((displayContext, entries) -> {
        Set<Item> emittedItems = Collections.newSetFromMap(new IdentityHashMap<>());
        for (ModBlocks block: ModBlocks.values()) {
            for (ModBlocks.BlockType type : ModBlocks.BlockType.values()) {
                if (!block.hasBlock(type)) continue;
                Block geometry = block.getBlock(type);
                if (emittedItems.add(geometry.asItem())) entries.accept(geometry);
            }
        }})
                                   .build());


    public static void registerBlocks(){
        for (ModBlocks modBlock : ModBlocks.values())
        {
            for (ModBlocks.BlockType type : ModBlocks.BlockType.values())
            {
                if (modBlock.hasBlock(type))
                {
                    Block block = games.twinhead.moreslabsstairsandwalls.registry.ModRegistry.getBlock(modBlock, type);
                    REGISTERED_BLOCKS.put(modBlock.getId(type), block);
                    Registry.register(BuiltInRegistries.BLOCK, ResourceKey.create(Registries.BLOCK, modBlock.getId(type)), block);
                    registerItem(modBlock.getId(type), block);

                    if (modBlock.parentBlock.defaultBlockState().ignitedByLava()){
                        FlammableBlockRegistry.getDefaultInstance().add(block, games.twinhead.moreslabsstairsandwalls.registry.ModRegistry.getBurnChance(modBlock), games.twinhead.moreslabsstairsandwalls.registry.ModRegistry.getSpreadChance(modBlock));
                    }

                }
            }
        }
    }

    public static void registerItem(Identifier id, Block block){
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
        Item.Properties properties = new Item.Properties().setId(key).useBlockDescriptionPrefix();
        Registry.register(BuiltInRegistries.ITEM, key, new BlockItem(block, properties));
    }

}
