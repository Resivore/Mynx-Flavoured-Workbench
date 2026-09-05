package dev.resivore.mynxregions;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.registry.CompostableRegistry;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PlaceOnWaterBlockItem;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class MynxRegionsUnexplored implements ModInitializer {
    public static final String MOD_ID = "mynx_regions_unexplored";
    public static final TagKey<Block> STONE_BUD_SUPPORTS = TagKey.create(Registries.BLOCK, id("stone_bud_supports"));
    public static final TagKey<Block> CATTAIL_SUPPORTS = TagKey.create(Registries.BLOCK, id("cattail_supports"));

    public static final DropleafHeadBlock DROPLEAF = registerBlock("dropleaf", DropleafHeadBlock::new, dropleafProperties(true));
    public static final DropleafPlantBlock DROPLEAF_PLANT = registerBlockNoItem("dropleaf_plant", DropleafPlantBlock::new, dropleafProperties(false));
    public static final DoublePlantBlock BARLEY = registerBlock("barley", DoublePlantBlock::new, plantCopy(Blocks.TALL_GRASS));
    public static final DoublePlantBlock WINDSWEPT_GRASS = registerBlock("windswept_grass", DoublePlantBlock::new, plantCopy(Blocks.TALL_GRASS));
    public static final CloverBlock CLOVER = registerBlock("clover", CloverBlock::new, plantCopy(Blocks.PINK_PETALS));
    public static final StoneBudBlock STONE_BUD = registerBlock("stone_bud", StoneBudBlock::new, plantCopy(Blocks.SHORT_GRASS));
    public static final MycotoxicDaisyBlock MYCOTOXIC_DAISY = registerBlock("mycotoxic_daisy", MycotoxicDaisyBlock::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.WARPED_ROOTS).replaceable().noCollision().offsetType(BlockBehaviour.OffsetType.XZ)
                    .sound(SoundType.ROOTS).lightLevel(state -> 14));
    public static final Block HYSSOP = registerFlower("hyssop", MobEffects.LUCK, 10.0F);
    public static final Block BLUE_LUPINE = registerFlower("blue_lupine", MobEffects.SATURATION, 0.2F);
    public static final Block PINK_LUPINE = registerFlower("pink_lupine", MobEffects.SATURATION, 0.2F);
    public static final Block PURPLE_LUPINE = registerFlower("purple_lupine", MobEffects.SATURATION, 0.2F);
    public static final Block RED_LUPINE = registerFlower("red_lupine", MobEffects.SATURATION, 0.2F);
    public static final Block YELLOW_LUPINE = registerFlower("yellow_lupine", MobEffects.SATURATION, 0.2F);
    public static final CattailBlock CATTAIL = registerBlock("cattail", CattailBlock::new, plantCopy(Blocks.TALL_GRASS));
    public static final DuckweedBlock DUCKWEED = registerWaterBlock("duckweed", DuckweedBlock::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.LILY_PAD).replaceable().noCollision().instabreak().sound(SoundType.LILY_PAD));
    public static final TasselBlock TASSEL = registerBlock("tassel", TasselBlock::new, plantCopy(Blocks.TALL_GRASS));
    public static final DoublePlantBlock MEADOW_SAGE = registerBlockNoItem("meadow_sage", DoublePlantBlock::new, plantCopy(Blocks.LARGE_FERN));

    public static final Item MEADOW_SAGE_ITEM = registerMeadowSageItem();

    public static final Block POTTED_HYSSOP = pot("potted_hyssop", HYSSOP, 0);
    public static final Block POTTED_BLUE_LUPINE = pot("potted_blue_lupine", BLUE_LUPINE, 0);
    public static final Block POTTED_PINK_LUPINE = pot("potted_pink_lupine", PINK_LUPINE, 0);
    public static final Block POTTED_PURPLE_LUPINE = pot("potted_purple_lupine", PURPLE_LUPINE, 0);
    public static final Block POTTED_RED_LUPINE = pot("potted_red_lupine", RED_LUPINE, 0);
    public static final Block POTTED_YELLOW_LUPINE = pot("potted_yellow_lupine", YELLOW_LUPINE, 0);
    public static final Block POTTED_MYCOTOXIC_DAISY = pottedMycotoxicDaisy("potted_mycotoxic_daisy", MYCOTOXIC_DAISY, 14);

    public static final List<Block> OBTAINABLE = List.of(DROPLEAF, BARLEY, WINDSWEPT_GRASS, CLOVER, STONE_BUD,
            MYCOTOXIC_DAISY, HYSSOP, BLUE_LUPINE, PINK_LUPINE, PURPLE_LUPINE, RED_LUPINE, YELLOW_LUPINE,
            CATTAIL, DUCKWEED, TASSEL, MEADOW_SAGE);
    public static final Map<Block, Float> COMPOST_CHANCES = compostChances();

    public static Identifier id(String path) { return Identifier.fromNamespaceAndPath(MOD_ID, path); }

    @Override public void onInitialize() {
        COMPOST_CHANCES.forEach((block, chance) -> CompostableRegistry.INSTANCE.add(block.asItem(), chance));
        for (Block block : OBTAINABLE) {
            if (block != MYCOTOXIC_DAISY) FlammableBlockRegistry.getDefaultInstance().add(block, 60, 100);
        }
        FlammableBlockRegistry.getDefaultInstance().add(DROPLEAF_PLANT, 60, 100);
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.NATURAL_BLOCKS).register(entries -> OBTAINABLE.forEach(entries::accept));
    }

    private static Map<Block, Float> compostChances() {
        Map<Block, Float> values = new LinkedHashMap<>();
        values.put(DROPLEAF, .15F); values.put(BARLEY, .6F); values.put(WINDSWEPT_GRASS, .5F);
        values.put(CLOVER, .2F); values.put(STONE_BUD, .3F); values.put(MYCOTOXIC_DAISY, .3F);
        for (Block flower : List.of(HYSSOP, BLUE_LUPINE, PINK_LUPINE, PURPLE_LUPINE, RED_LUPINE, YELLOW_LUPINE)) values.put(flower, .4F);
        values.put(CATTAIL, .6F); values.put(DUCKWEED, .15F); values.put(TASSEL, .6F); values.put(MEADOW_SAGE, .6F);
        return Map.copyOf(values);
    }

    private static BlockBehaviour.Properties plantCopy(Block base) {
        return BlockBehaviour.Properties.ofFullCopy(base).replaceable().noCollision().offsetType(BlockBehaviour.OffsetType.XZ);
    }
    private static BlockBehaviour.Properties dropleafProperties(boolean head) {
        Block base = head ? Blocks.WEEPING_VINES : Blocks.WEEPING_VINES_PLANT;
        BlockBehaviour.Properties p = BlockBehaviour.Properties.ofFullCopy(base).replaceable().noCollision()
                .offsetType(BlockBehaviour.OffsetType.XZ).sound(SoundType.WEEPING_VINES);
        return head ? p.randomTicks().lightLevel(state -> 14) : p;
    }
    private static Block registerFlower(String name, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect, float seconds) {
        return registerBlock(name, p -> new RuFlowerBlock(effect, seconds, p), plantCopy(Blocks.POPPY));
    }
    private static Item registerMeadowSageItem() {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id("meadow_sage"));
        FoodProperties food = new FoodProperties.Builder().nutrition(2).saturationModifier(.15F).build();
        var consumable = Consumables.defaultFood()
                .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.INSTANT_HEALTH, 1), .5F)).build();
        return Registry.register(BuiltInRegistries.ITEM, key,
                new BlockItem(MEADOW_SAGE, new Item.Properties().setId(key).useBlockDescriptionPrefix().food(food, consumable)));
    }
    private static Block pot(String name, Block content, int light) {
        BlockBehaviour.Properties properties = BlockBehaviour.Properties.ofFullCopy(Blocks.FLOWER_POT).noOcclusion();
        if (light > 0) properties.lightLevel(state -> light);
        return registerBlockNoItem(name, p -> new FlowerPotBlock(content, p), properties);
    }
    private static Block pottedMycotoxicDaisy(String name, Block content, int light) {
        BlockBehaviour.Properties properties = BlockBehaviour.Properties.ofFullCopy(Blocks.FLOWER_POT).noOcclusion();
        if (light > 0) properties.lightLevel(state -> light);
        return registerBlockNoItem(name, p -> new MycotoxicDaisyPotBlock(content, p), properties);
    }
    private static <T extends Block> T registerBlock(String name, Function<BlockBehaviour.Properties, T> factory, BlockBehaviour.Properties properties) {
        T block = registerBlockNoItem(name, factory, properties);
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id(name));
        Registry.register(BuiltInRegistries.ITEM, itemKey,
                new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
        return block;
    }
    private static <T extends Block> T registerWaterBlock(String name, Function<BlockBehaviour.Properties, T> factory, BlockBehaviour.Properties properties) {
        T block = registerBlockNoItem(name, factory, properties);
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id(name));
        Registry.register(BuiltInRegistries.ITEM, itemKey,
                new PlaceOnWaterBlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
        return block;
    }
    private static <T extends Block> T registerBlockNoItem(String name, Function<BlockBehaviour.Properties, T> factory, BlockBehaviour.Properties properties) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id(name));
        return Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(properties.setId(key)));
    }
}
