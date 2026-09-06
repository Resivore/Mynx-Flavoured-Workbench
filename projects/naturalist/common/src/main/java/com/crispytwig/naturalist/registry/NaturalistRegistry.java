package com.crispytwig.naturalist.registry;

import com.crispytwig.naturalist.Naturalist;
import com.crispytwig.naturalist.server.block.*;
import com.crispytwig.naturalist.server.entity.mob.Bass;
import com.crispytwig.naturalist.server.entity.mob.Butterfly;
import com.crispytwig.naturalist.server.entity.mob.Crab;
import com.crispytwig.naturalist.server.entity.mob.Hedgehog;
import com.crispytwig.naturalist.server.entity.mob.Rat;
import com.crispytwig.naturalist.server.item.HedgehogItem;
import com.crispytwig.naturalist.server.item.BugNetItem;
import com.crispytwig.naturalist.server.item.DuckEggItem;
import com.crispytwig.naturalist.server.item.KnapsackItem;
import com.crispytwig.naturalist.server.item.GlowGoopItem;
import com.crispytwig.naturalist.server.item.CaughtMobItem;
import com.crispytwig.naturalist.server.item.CaughtMobWithVariantsItem;
import com.crispytwig.naturalist.server.item.NaturalistBucketItem;
import com.crispytwig.naturalist.server.item.SnailItem;
import com.crispytwig.naturalist.server.entity.mob.Starfish;
import com.crispytwig.naturalist.server.entity.mob.GiantIsopod;
import com.crispytwig.naturalist.server.entity.mob.Jellyfish;
import com.crispytwig.naturalist.server.entity.mob.Anglerfish;
import com.crispytwig.naturalist.server.entity.mob.Ray;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.PushReaction;
import com.crispytwig.naturalist.platform.Services;
import com.crispytwig.naturalist.platform.registry.DeferredHolder;
import com.crispytwig.naturalist.platform.registry.DeferredRegister;

import java.util.function.Function;

@SuppressWarnings("unused")
public class NaturalistRegistry {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(BuiltInRegistries.BLOCK, Naturalist.MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, Naturalist.MOD_ID);

    public static final DeferredHolder<Block, AlligatorEggBlock> ALLIGATOR_EGG = registerBlock("alligator_egg", key -> new AlligatorEggBlock(copyBlockProperties(key, Blocks.TURTLE_EGG)));
    public static final DeferredHolder<Item, DuckEggItem> DUCK_EGG = registerItem("duck_egg", properties -> new DuckEggItem(properties));
    public static final DeferredHolder<Block, TortoiseEggBlock> TORTOISE_EGG = registerBlock("tortoise_egg", key -> new TortoiseEggBlock(copyBlockProperties(key, Blocks.TURTLE_EGG)));
    public static final DeferredHolder<Block, OstrichEggBlock> OSTRICH_EGG = registerBlock("ostrich_egg", key -> new OstrichEggBlock(copyBlockProperties(key, Blocks.TURTLE_EGG)));
    public static final DeferredHolder<Block, SnailEggBlock> SNAIL_EGGS = registerBlock("snail_eggs", key -> new SnailEggBlock(copyBlockProperties(key, Blocks.FROGSPAWN)));

    public static final DeferredHolder<Block, GlowGoopBlock> GLOW_GOOP_BLOCK = registerBlockOnly("glow_goop", key -> new GlowGoopBlock(blockProperties(key).strength(0.5F).replaceable().noOcclusion().noCollision().lightLevel(GlowGoopBlock.LIGHT_EMISSION).sound(SoundType.HONEY_BLOCK)));
    public static final DeferredHolder<Item, GlowGoopItem> GLOW_GOOP = registerItem("glow_goop", properties -> new GlowGoopItem(GLOW_GOOP_BLOCK.get(), properties));
    public static final DeferredHolder<Item, NaturalistBucketItem> CATFISH_BUCKET = registerItem("catfish_bucket", properties -> new NaturalistBucketItem(NaturalistEntityTypes.CATFISH.get(), Fluids.WATER, SoundEvents.BUCKET_EMPTY_FISH, properties.stacksTo(1)));
    public static final DeferredHolder<Item, NaturalistBucketItem> BASS_BUCKET = registerItem("bass_bucket", properties -> new NaturalistBucketItem(NaturalistEntityTypes.BASS.get(), Fluids.WATER, SoundEvents.BUCKET_EMPTY_FISH, properties.stacksTo(1), true, null, Bass.VARIANT_NAMES));
    public static final DeferredHolder<Item, NaturalistBucketItem> DUCK_BUCKET = registerItem("duck_bucket", properties -> new NaturalistBucketItem(NaturalistEntityTypes.DUCK.get(), Fluids.EMPTY, SoundEvents.BUCKET_EMPTY, properties.stacksTo(1).component(DataComponents.BUCKET_ENTITY_DATA, ducklingBucketData())));
    public static final DeferredHolder<Item, CaughtMobWithVariantsItem> CRAB = registerItem("crab", properties -> new CaughtMobWithVariantsItem(NaturalistEntityTypes.CRAB, () -> Fluids.EMPTY, NaturalistSoundEvents.CRAB_AMBIENT, "tooltip.naturalist.crab_", Crab.VARIANT_NAMES, properties.stacksTo(1)));
    public static final DeferredHolder<Item, BugNetItem> CAPTURE_NET = registerItem("capture_net", properties -> new BugNetItem(properties.durability(64)));
    public static final DeferredHolder<Item, KnapsackItem> KNAPSACK = registerItem("knapsack", properties -> new KnapsackItem(properties.stacksTo(1)));
    public static final ResourceKey<JukeboxSong> WILD_ONES_SONG = ResourceKey.create(Registries.JUKEBOX_SONG, Naturalist.location("wild_ones"));
    public static final DeferredHolder<Item, Item> MUSIC_DISC_WILD_ONES = registerItem("music_disc_wild_ones", properties -> new Item(properties.stacksTo(1).rarity(Rarity.RARE).jukeboxPlayable(WILD_ONES_SONG)));
    public static final ResourceKey<JukeboxSong> DEATH_BY_HOGS_SONG = ResourceKey.create(Registries.JUKEBOX_SONG, Naturalist.location("death_by_hogs"));
    public static final DeferredHolder<Item, Item> MUSIC_DISC_DEATH_BY_HOGS = registerItem("music_disc_death_by_hogs", properties -> new Item(properties.stacksTo(1).rarity(Rarity.RARE).jukeboxPlayable(DEATH_BY_HOGS_SONG)));
    public static final DeferredHolder<Block, ChrysalisBlock> CHRYSALIS_BLOCK = registerBlockOnly("chrysalis", key -> new ChrysalisBlock(blockProperties(key).randomTicks().strength(0.2F, 3.0F).sound(SoundType.GRASS).noOcclusion().noCollision().pushReaction(PushReaction.DESTROY)));
    public static final DeferredHolder<Item, BlockItem> CHRYSALIS = registerItem("chrysalis", properties -> new BlockItem(CHRYSALIS_BLOCK.get(), properties.stacksTo(1).useBlockDescriptionPrefix()));
    public static final DeferredHolder<Item, CaughtMobItem> CATERPILLAR = registerItem("caterpillar", properties -> new CaughtMobItem(NaturalistEntityTypes.CATERPILLAR, () -> Fluids.EMPTY, NaturalistSoundEvents.SNAIL_FORWARD, properties.stacksTo(1)));
    public static final DeferredHolder<Item, CaughtMobWithVariantsItem> BUTTERFLY = registerItem("butterfly", properties -> new CaughtMobWithVariantsItem(NaturalistEntityTypes.BUTTERFLY, () -> Fluids.EMPTY, NaturalistSoundEvents.BIRD_FLY, "tooltip.naturalist.", Butterfly.VARIANT_NAMES, properties.stacksTo(1)));
    public static final DeferredHolder<Item, CaughtMobWithVariantsItem> RAT = registerItem("rat", properties -> new CaughtMobWithVariantsItem(NaturalistEntityTypes.RAT, () -> Fluids.EMPTY, NaturalistSoundEvents.RAT_AMBIENT, "tooltip.naturalist.rat_", Rat.VARIANT_NAMES, properties.stacksTo(1)));
    public static final DeferredHolder<Item, CaughtMobItem> SCORPION = registerItem("scorpion", properties -> new CaughtMobItem(NaturalistEntityTypes.DESERT_SCORPION, () -> Fluids.EMPTY, NaturalistSoundEvents.SCORPION_AMBIENT, properties.stacksTo(1)));
    public static final DeferredHolder<Item, HedgehogItem> HEDGEHOG = registerItem("hedgehog", properties -> new HedgehogItem(NaturalistEntityTypes.HEDGEHOG, () -> Fluids.EMPTY, NaturalistSoundEvents.HEDGEHOG_AMBIENT, "tooltip.naturalist.hedgehog_", Hedgehog.VARIANT_NAMES, properties.stacksTo(1)));
    public static final DeferredHolder<Item, SnailItem> SNAIL = registerItem("snail", properties -> new SnailItem(NaturalistEntityTypes.SNAIL, () -> Fluids.EMPTY, NaturalistSoundEvents.SNAIL_FORWARD, properties.stacksTo(1)));
    public static final DeferredHolder<Item, NaturalistBucketItem> STARFISH_BUCKET = registerItem("starfish_bucket", properties -> new NaturalistBucketItem(NaturalistEntityTypes.STARFISH.get(), Fluids.WATER, SoundEvents.BUCKET_EMPTY_FISH, properties.stacksTo(1), false, "color.minecraft.", Starfish.VARIANT_NAMES));
    public static final DeferredHolder<Item, NaturalistBucketItem> GIANT_ISOPOD_BUCKET = registerItem("giant_isopod_bucket", properties -> new NaturalistBucketItem(NaturalistEntityTypes.GIANT_ISOPOD.get(), Fluids.WATER, SoundEvents.BUCKET_EMPTY_FISH, properties.stacksTo(1), false, "tooltip.naturalist.giant_isopod_", GiantIsopod.VARIANT_NAMES));
    public static final DeferredHolder<Item, NaturalistBucketItem> ANGLERFISH_BUCKET = registerItem("anglerfish_bucket", properties -> new NaturalistBucketItem(NaturalistEntityTypes.ANGLERFISH.get(), Fluids.WATER, SoundEvents.BUCKET_EMPTY_FISH, properties.stacksTo(1), true, "tooltip.naturalist.anglerfish_", Anglerfish.VARIANT_NAMES));
    public static final DeferredHolder<Item, NaturalistBucketItem> JELLYFISH_BUCKET = registerItem("jellyfish_bucket", properties -> new NaturalistBucketItem(NaturalistEntityTypes.JELLYFISH.get(), Fluids.WATER, SoundEvents.BUCKET_EMPTY_FISH, properties.stacksTo(1), true, "color.minecraft.", Jellyfish.VARIANT_NAMES));
    public static final DeferredHolder<Item, NaturalistBucketItem> RAY_BUCKET = registerItem("ray_bucket", properties -> new NaturalistBucketItem(NaturalistEntityTypes.RAY.get(), Fluids.WATER, SoundEvents.BUCKET_EMPTY_FISH, properties.stacksTo(1), true, "tooltip.naturalist.ray_", Ray.VARIANT_NAMES));
    public static final DeferredHolder<Item, NaturalistBucketItem> BLOBFISH_BUCKET = registerItem("blobfish_bucket", properties -> new NaturalistBucketItem(NaturalistEntityTypes.BLOBFISH.get(), Fluids.WATER, SoundEvents.BUCKET_EMPTY_FISH, properties.stacksTo(1)));
    public static final DeferredHolder<Item, NaturalistBucketItem> PIRANHA_BUCKET = registerItem("piranha_bucket", properties -> new NaturalistBucketItem(NaturalistEntityTypes.PIRANHA.get(), Fluids.WATER, SoundEvents.BUCKET_EMPTY_FISH, properties.stacksTo(1)));
    public static final DeferredHolder<Block, StarfishBlock> RED_STARFISH = registerStarfishBlock("red_starfish");
    public static final DeferredHolder<Block, StarfishBlock> ORANGE_STARFISH = registerStarfishBlock("orange_starfish");
    public static final DeferredHolder<Block, StarfishBlock> BLUE_STARFISH = registerStarfishBlock("blue_starfish");
    public static final DeferredHolder<Block, StarfishBlock> PURPLE_STARFISH = registerStarfishBlock("purple_starfish");


    public static final DeferredHolder<Item, SpawnEggItem> ALLIGATOR_SPAWN_EGG = registerItem("alligator_spawn_egg", properties -> Services.REGISTRY.createSpawnEgg(NaturalistEntityTypes.ALLIGATOR, 6184228, 13810273, properties));
    public static final DeferredHolder<Item, SpawnEggItem> ANGLERFISH_SPAWN_EGG = registerItem("anglerfish_spawn_egg", properties -> Services.REGISTRY.createSpawnEgg(NaturalistEntityTypes.ANGLERFISH, 3029818, 15132298, properties));
    public static final DeferredHolder<Item, SpawnEggItem> RAY_SPAWN_EGG = registerItem("ray_spawn_egg", properties -> Services.REGISTRY.createSpawnEgg(NaturalistEntityTypes.RAY, 4873579, 12568015, properties));
    public static final DeferredHolder<Item, SpawnEggItem> BLOBFISH_SPAWN_EGG = registerItem("blobfish_spawn_egg", properties -> Services.REGISTRY.createSpawnEgg(NaturalistEntityTypes.BLOBFISH, 9142136, 14722214, properties));
    public static final DeferredHolder<Item, SpawnEggItem> PIRANHA_SPAWN_EGG = registerItem("piranha_spawn_egg", properties -> Services.REGISTRY.createSpawnEgg(NaturalistEntityTypes.PIRANHA, 7171697, 11549218, properties));
    public static final DeferredHolder<Item, SpawnEggItem> BASS_SPAWN_EGG = registerItem("bass_spawn_egg", properties -> Services.REGISTRY.createSpawnEgg(NaturalistEntityTypes.BASS, 8159273, 14729339, properties));
    public static final DeferredHolder<Item, SpawnEggItem> BEAR_SPAWN_EGG = registerItem("bear_spawn_egg", properties -> Services.REGISTRY.createSpawnEgg(NaturalistEntityTypes.BEAR, 6569255, 13150577, properties));
    public static final DeferredHolder<Item, SpawnEggItem> BIRD_SPAWN_EGG = registerItem("bird_spawn_egg", properties -> Services.REGISTRY.createSpawnEgg(NaturalistEntityTypes.BIRD, 4865860, 16620592, properties));
    public static final DeferredHolder<Item, SpawnEggItem> BOAR_SPAWN_EGG = registerItem("boar_spawn_egg", properties -> Services.REGISTRY.createSpawnEgg(NaturalistEntityTypes.BOAR, 6768433, 9854549, properties));
    public static final DeferredHolder<Item, SpawnEggItem> BUTTERFLY_SPAWN_EGG = registerItem("butterfly_spawn_egg", properties -> Services.REGISTRY.createSpawnEgg(NaturalistEntityTypes.BUTTERFLY, 15165706, 6828564, properties));
    public static final DeferredHolder<Item, SpawnEggItem> CATFISH_SPAWN_EGG = registerItem("catfish_spawn_egg", properties -> Services.REGISTRY.createSpawnEgg(NaturalistEntityTypes.CATFISH, 8416033, 12233092, properties));
    public static final DeferredHolder<Item, SpawnEggItem> CATERPILLAR_SPAWN_EGG = registerItem("caterpillar_spawn_egg", properties -> Services.REGISTRY.createSpawnEgg(NaturalistEntityTypes.CATERPILLAR, 3815473, 15647488, properties));
    public static final DeferredHolder<Item, SpawnEggItem> CLAM_SPAWN_EGG = registerItem("clam_spawn_egg", properties -> Services.REGISTRY.createSpawnEgg(NaturalistEntityTypes.CLAM, 9138517, 14272931, properties));
    public static final DeferredHolder<Item, SpawnEggItem> CRAB_SPAWN_EGG = registerItem("crab_spawn_egg", properties -> Services.REGISTRY.createSpawnEgg(NaturalistEntityTypes.CRAB, 14179386, 15909531, properties));
    public static final DeferredHolder<Item, SpawnEggItem> DEER_SPAWN_EGG = registerItem("deer_spawn_egg", properties -> Services.REGISTRY.createSpawnEgg(NaturalistEntityTypes.DEER, 10318165, 14531208, properties));
    public static final DeferredHolder<Item, SpawnEggItem> DRAGONFLY_SPAWN_EGG = registerItem("dragonfly_spawn_egg", properties -> Services.REGISTRY.createSpawnEgg(NaturalistEntityTypes.DRAGONFLY, 7507200, 16771840, properties));
    public static final DeferredHolder<Item, SpawnEggItem> DUCK_SPAWN_EGG = registerItem("duck_spawn_egg", properties -> Services.REGISTRY.createSpawnEgg(NaturalistEntityTypes.DUCK, 13286315, 2333491, properties));
    public static final DeferredHolder<Item, SpawnEggItem> CAPYBARA_SPAWN_EGG = registerItem("capybara_spawn_egg", properties -> Services.REGISTRY.createSpawnEgg(NaturalistEntityTypes.CAPYBARA, 11691058, 5650727, properties));
    public static final DeferredHolder<Item, SpawnEggItem> HEDGEHOG_SPAWN_EGG = registerItem("hedgehog_spawn_egg", properties -> Services.REGISTRY.createSpawnEgg(NaturalistEntityTypes.HEDGEHOG, 5783076, 14468251, properties));
    public static final DeferredHolder<Item, SpawnEggItem> ELEPHANT_SPAWN_EGG = registerItem("elephant_spawn_egg", properties -> Services.REGISTRY.createSpawnEgg(NaturalistEntityTypes.ELEPHANT, 9539213, 6643034, properties));
    public static final DeferredHolder<Item, SpawnEggItem> MAMMOTH_SPAWN_EGG = registerItem("mammoth_spawn_egg", properties -> Services.REGISTRY.createSpawnEgg(NaturalistEntityTypes.MAMMOTH, 5520936, 3153430, properties));
    public static final DeferredHolder<Item, SpawnEggItem> FIREFLY_SPAWN_EGG = registerItem("firefly_spawn_egg", properties -> Services.REGISTRY.createSpawnEgg(NaturalistEntityTypes.FIREFLY, 6764577, 16768800, properties));
    public static final DeferredHolder<Item, SpawnEggItem> GIANT_ISOPOD_SPAWN_EGG = registerItem("giant_isopod_spawn_egg", properties -> Services.REGISTRY.createSpawnEgg(NaturalistEntityTypes.GIANT_ISOPOD, 7362632, 10125426, properties));
    public static final DeferredHolder<Item, SpawnEggItem> JELLYFISH_SPAWN_EGG = registerItem("jellyfish_spawn_egg", properties -> Services.REGISTRY.createSpawnEgg(NaturalistEntityTypes.JELLYFISH, 8900331, 8388736, properties));
    public static final DeferredHolder<Item, SpawnEggItem> GIRAFFE_SPAWN_EGG = registerItem("giraffe_spawn_egg", properties -> Services.REGISTRY.createSpawnEgg(NaturalistEntityTypes.GIRAFFE, 14329967, 7619616, properties));
    public static final DeferredHolder<Item, SpawnEggItem> HIPPO_SPAWN_EGG = registerItem("hippo_spawn_egg", properties -> Services.REGISTRY.createSpawnEgg(NaturalistEntityTypes.HIPPO, 15702682, 9004386, properties));
    public static final DeferredHolder<Item, SpawnEggItem> LION_SPAWN_EGG = registerItem("lion_spawn_egg", properties -> Services.REGISTRY.createSpawnEgg(NaturalistEntityTypes.LION, 14990722, 6699537, properties));
    public static final DeferredHolder<Item, SpawnEggItem> LIZARD_SPAWN_EGG = registerItem("lizard_spawn_egg", properties -> Services.REGISTRY.createSpawnEgg(NaturalistEntityTypes.LIZARD, 10853166, 15724462, properties));
    public static final DeferredHolder<Item, SpawnEggItem> RHINO_SPAWN_EGG = registerItem("rhino_spawn_egg", properties -> Services.REGISTRY.createSpawnEgg(NaturalistEntityTypes.RHINO, 7626842, 10982025, properties));
    public static final DeferredHolder<Item, SpawnEggItem> SNAKE_SPAWN_EGG = registerItem("snake_spawn_egg", properties -> Services.REGISTRY.createSpawnEgg(NaturalistEntityTypes.SNAKE, 8813107, 15524255, properties));
    public static final DeferredHolder<Item, SpawnEggItem> SNAIL_SPAWN_EGG = registerItem("snail_spawn_egg", properties -> Services.REGISTRY.createSpawnEgg(NaturalistEntityTypes.SNAIL, 5457209, 8811878, properties));
    public static final DeferredHolder<Item, SpawnEggItem> STARFISH_SPAWN_EGG = registerItem("starfish_spawn_egg", properties -> Services.REGISTRY.createSpawnEgg(NaturalistEntityTypes.STARFISH, 14245934, 15909006, properties));
    public static final DeferredHolder<Item, SpawnEggItem> TORTOISE_SPAWN_EGG = registerItem("tortoise_spawn_egg", properties -> Services.REGISTRY.createSpawnEgg(NaturalistEntityTypes.TORTOISE, 15724462, 11765582, properties));
    public static final DeferredHolder<Item, SpawnEggItem> VULTURE_SPAWN_EGG = registerItem("vulture_spawn_egg", properties -> Services.REGISTRY.createSpawnEgg(NaturalistEntityTypes.VULTURE, 4010022, 15325376, properties));
    public static final DeferredHolder<Item, SpawnEggItem> ZEBRA_SPAWN_EGG = registerItem("zebra_spawn_egg", properties -> Services.REGISTRY.createSpawnEgg(NaturalistEntityTypes.ZEBRA, 15263457, 1710104, properties));
    public static final DeferredHolder<Item, SpawnEggItem> WHALE_SPAWN_EGG = registerItem("whale_spawn_egg", properties -> Services.REGISTRY.createSpawnEgg(NaturalistEntityTypes.WHALE, 3886172, 9413037, properties));
    public static final DeferredHolder<Item, SpawnEggItem> MOLE_SPAWN_EGG = registerItem("mole_spawn_egg", properties -> Services.REGISTRY.createSpawnEgg(NaturalistEntityTypes.MOLE, 5259603, 15443344, properties));
    public static final DeferredHolder<Item, SpawnEggItem> RAT_SPAWN_EGG = registerItem("rat_spawn_egg", properties -> Services.REGISTRY.createSpawnEgg(NaturalistEntityTypes.RAT, 4866377, 15968421, properties));
    public static final DeferredHolder<Item, SpawnEggItem> BLACK_BEAR_SPAWN_EGG = registerItem("black_bear_spawn_egg", properties -> Services.REGISTRY.createSpawnEgg(NaturalistEntityTypes.BLACK_BEAR, 2500134, 12550772, properties));
    public static final DeferredHolder<Item, SpawnEggItem> TIGER_SPAWN_EGG = registerItem("tiger_spawn_egg", properties -> Services.REGISTRY.createSpawnEgg(NaturalistEntityTypes.TIGER, 14257212, 3158062, properties));
    public static final DeferredHolder<Item, SpawnEggItem> KOMODO_DRAGON_SPAWN_EGG = registerItem("komodo_dragon_spawn_egg", properties -> Services.REGISTRY.createSpawnEgg(NaturalistEntityTypes.KOMODO_DRAGON, 5590330, 8746320, properties));
    public static final DeferredHolder<Item, SpawnEggItem> OSTRICH_SPAWN_EGG = registerItem("ostrich_spawn_egg", properties -> Services.REGISTRY.createSpawnEgg(NaturalistEntityTypes.OSTRICH, 9521716, 14451302, properties));
    public static final DeferredHolder<Item, SpawnEggItem> DESERT_SCORPION_SPAWN_EGG = registerItem("desert_scorpion_spawn_egg", properties -> Services.REGISTRY.createSpawnEgg(NaturalistEntityTypes.DESERT_SCORPION, 13544037, 9856322, properties));
    public static final DeferredHolder<Item, SpawnEggItem> JUNGLE_SCORPION_SPAWN_EGG = registerItem("jungle_scorpion_spawn_egg", properties -> Services.REGISTRY.createSpawnEgg(NaturalistEntityTypes.JUNGLE_SCORPION, 2960943, 6926420, properties));
    public static final DeferredHolder<Item, SpawnEggItem> GREAT_WHITE_SHARK_SPAWN_EGG = registerItem("great_white_shark_spawn_egg", properties -> Services.REGISTRY.createSpawnEgg(NaturalistEntityTypes.GREAT_WHITE_SHARK, 6060149, 15329245, properties));
    public static final DeferredHolder<Item, SpawnEggItem> TURKEY_SPAWN_EGG = registerItem("turkey_spawn_egg", properties -> Services.REGISTRY.createSpawnEgg(NaturalistEntityTypes.TURKEY, 4600099, 14122348, properties));

    public static void init() {
    }

    private static CustomData ducklingBucketData() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Age", -24000);
        return CustomData.of(tag);
    }

    private static <T extends Item> DeferredHolder<Item, T> registerItem(String name, Function<Item.Properties, T> factory) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Naturalist.location(name));
        return ITEMS.register(name, () -> factory.apply(new Item.Properties().setId(key)));
    }

    private static <T extends Block> DeferredHolder<Block, T> registerBlock(String name, Function<ResourceKey<Block>, T> factory) {
        DeferredHolder<Block, T> holder = registerBlockOnly(name, factory);
        registerItem(name, properties -> new BlockItem(holder.get(), properties.useBlockDescriptionPrefix()));
        return holder;
    }

    private static <T extends Block> DeferredHolder<Block, T> registerBlockOnly(String name, Function<ResourceKey<Block>, T> factory) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Naturalist.location(name));
        return BLOCKS.register(name, () -> factory.apply(key));
    }

    private static BlockBehaviour.Properties blockProperties(ResourceKey<Block> key) {
        return BlockBehaviour.Properties.of().setId(key);
    }

    private static BlockBehaviour.Properties copyBlockProperties(ResourceKey<Block> key, Block source) {
        return BlockBehaviour.Properties.ofFullCopy(source).setId(key);
    }

    private static DeferredHolder<Block, StarfishBlock> registerStarfishBlock(String name) {
        return registerBlock(name, key -> new StarfishBlock(blockProperties(key).noCollision().instabreak().sound(SoundType.WET_GRASS).noOcclusion().pushReaction(PushReaction.DESTROY)));
    }
}
