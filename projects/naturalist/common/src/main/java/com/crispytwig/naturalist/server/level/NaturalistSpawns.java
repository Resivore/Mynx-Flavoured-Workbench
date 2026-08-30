package com.crispytwig.naturalist.server.level;

import com.crispytwig.naturalist.registry.NaturalistEntityTypes;
import com.crispytwig.naturalist.registry.NaturalistTags;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import org.jetbrains.annotations.Nullable;

public final class NaturalistSpawns {
    private NaturalistSpawns() {
    }

    @FunctionalInterface
    public interface SpawnConsumer {
        void accept(TagKey<Biome> hasTag, @Nullable TagKey<Biome> blacklistTag, MobCategory category, EntityType<?> type, int weight, int min, int max);
    }

    public static void forEachSpawn(SpawnConsumer c) {
        c.accept(NaturalistTags.Biomes.HAS_ALLIGATOR, NaturalistTags.Biomes.BLACKLIST_ALLIGATOR, MobCategory.CREATURE, NaturalistEntityTypes.ALLIGATOR.get(), 10, 2, 3);
        c.accept(NaturalistTags.Biomes.HAS_ANGLERFISH, NaturalistTags.Biomes.BLACKLIST_ANGLERFISH, MobCategory.WATER_AMBIENT, NaturalistEntityTypes.ANGLERFISH.get(), 10, 1, 1);
        c.accept(NaturalistTags.Biomes.HAS_RAY, NaturalistTags.Biomes.BLACKLIST_RAY, MobCategory.WATER_AMBIENT, NaturalistEntityTypes.RAY.get(), 8, 1, 2);
        c.accept(NaturalistTags.Biomes.HAS_BLOBFISH, NaturalistTags.Biomes.BLACKLIST_BLOBFISH, MobCategory.WATER_AMBIENT, NaturalistEntityTypes.BLOBFISH.get(), 6, 1, 1);
        c.accept(NaturalistTags.Biomes.HAS_WHALE, NaturalistTags.Biomes.BLACKLIST_WHALE, MobCategory.WATER_CREATURE, NaturalistEntityTypes.WHALE.get(), 5, 1, 2);
        c.accept(NaturalistTags.Biomes.HAS_GREAT_WHITE_SHARK, NaturalistTags.Biomes.BLACKLIST_GREAT_WHITE_SHARK, MobCategory.WATER_CREATURE, NaturalistEntityTypes.GREAT_WHITE_SHARK.get(), 4, 1, 2);
        c.accept(NaturalistTags.Biomes.HAS_PIRANHA, NaturalistTags.Biomes.BLACKLIST_PIRANHA, MobCategory.WATER_AMBIENT, NaturalistEntityTypes.PIRANHA.get(), 8, 2, 4);
        c.accept(NaturalistTags.Biomes.HAS_BASS, NaturalistTags.Biomes.BLACKLIST_BASS, MobCategory.WATER_AMBIENT, NaturalistEntityTypes.BASS.get(), 10, 3, 6);
        c.accept(NaturalistTags.Biomes.HAS_BEAR, NaturalistTags.Biomes.BLACKLIST_BEAR, MobCategory.CREATURE, NaturalistEntityTypes.BEAR.get(), 10, 1, 2);
        c.accept(NaturalistTags.Biomes.HAS_BLACK_BEAR, NaturalistTags.Biomes.BLACKLIST_BLACK_BEAR, MobCategory.CREATURE, NaturalistEntityTypes.BLACK_BEAR.get(), 10, 1, 2);
        c.accept(NaturalistTags.Biomes.HAS_BLUEJAY, NaturalistTags.Biomes.BLACKLIST_BLUEJAY, MobCategory.CREATURE, NaturalistEntityTypes.BIRD.get(), 10, 3, 4);
        c.accept(NaturalistTags.Biomes.HAS_BOAR, NaturalistTags.Biomes.BLACKLIST_BOAR, MobCategory.CREATURE, NaturalistEntityTypes.BOAR.get(), 10, 3, 4);
        c.accept(NaturalistTags.Biomes.HAS_BUTTERFLY, NaturalistTags.Biomes.BLACKLIST_BUTTERFLY, MobCategory.AMBIENT, NaturalistEntityTypes.BUTTERFLY.get(), 10, 3, 6);
        c.accept(NaturalistTags.Biomes.HAS_CANARY, NaturalistTags.Biomes.BLACKLIST_CANARY, MobCategory.CREATURE, NaturalistEntityTypes.BIRD.get(), 10, 3, 4);
        c.accept(NaturalistTags.Biomes.HAS_CARDINAL, NaturalistTags.Biomes.BLACKLIST_CARDINAL, MobCategory.CREATURE, NaturalistEntityTypes.BIRD.get(), 10, 3, 4);
        c.accept(NaturalistTags.Biomes.HAS_CAPYBARA, NaturalistTags.Biomes.BLACKLIST_CAPYBARA, MobCategory.CREATURE, NaturalistEntityTypes.CAPYBARA.get(), 10, 2, 4);
        c.accept(NaturalistTags.Biomes.HAS_CATFISH, NaturalistTags.Biomes.BLACKLIST_CATFISH, MobCategory.WATER_AMBIENT, NaturalistEntityTypes.CATFISH.get(), 10, 1, 2);
        c.accept(NaturalistTags.Biomes.HAS_CLAM, NaturalistTags.Biomes.BLACKLIST_CLAM, MobCategory.WATER_AMBIENT, NaturalistEntityTypes.CLAM.get(), 8, 1, 1);
        c.accept(NaturalistTags.Biomes.HAS_CORAL_SNAKE, NaturalistTags.Biomes.BLACKLIST_CORAL_SNAKE, MobCategory.CREATURE, NaturalistEntityTypes.SNAKE.get(), 10, 1, 1);
        c.accept(NaturalistTags.Biomes.HAS_CRAB, NaturalistTags.Biomes.BLACKLIST_CRAB, MobCategory.CREATURE, NaturalistEntityTypes.CRAB.get(), 10, 1, 2);
        c.accept(NaturalistTags.Biomes.HAS_DEER, NaturalistTags.Biomes.BLACKLIST_DEER, MobCategory.CREATURE, NaturalistEntityTypes.DEER.get(), 10, 3, 5);
        c.accept(NaturalistTags.Biomes.HAS_DESERT_SCORPION, NaturalistTags.Biomes.BLACKLIST_DESERT_SCORPION, MobCategory.CREATURE, NaturalistEntityTypes.DESERT_SCORPION.get(), 20, 1, 3);
        c.accept(NaturalistTags.Biomes.HAS_JUNGLE_SCORPION, NaturalistTags.Biomes.BLACKLIST_JUNGLE_SCORPION, MobCategory.CREATURE, NaturalistEntityTypes.JUNGLE_SCORPION.get(), 30, 1, 3);
        c.accept(NaturalistTags.Biomes.HAS_DRAGONFLY, NaturalistTags.Biomes.BLACKLIST_DRAGONFLY, MobCategory.AMBIENT, NaturalistEntityTypes.DRAGONFLY.get(), 10, 2, 4);
        c.accept(NaturalistTags.Biomes.HAS_DUCK, NaturalistTags.Biomes.BLACKLIST_DUCK, MobCategory.CREATURE, NaturalistEntityTypes.DUCK.get(), 10, 3, 4);
        c.accept(NaturalistTags.Biomes.HAS_ELEPHANT, NaturalistTags.Biomes.BLACKLIST_ELEPHANT, MobCategory.CREATURE, NaturalistEntityTypes.ELEPHANT.get(), 5, 1, 3);
        c.accept(NaturalistTags.Biomes.HAS_FINCH, NaturalistTags.Biomes.BLACKLIST_FINCH, MobCategory.CREATURE, NaturalistEntityTypes.BIRD.get(), 10, 3, 4);
        c.accept(NaturalistTags.Biomes.HAS_FIREFLY, NaturalistTags.Biomes.BLACKLIST_FIREFLY, MobCategory.AMBIENT, NaturalistEntityTypes.FIREFLY.get(), 10, 2, 4);
        c.accept(BiomeTags.IS_FOREST, NaturalistTags.Biomes.BLACKLIST_FOREST_FOXES, MobCategory.CREATURE, EntityTypes.FOX, 10, 1, 2);
        c.accept(BiomeTags.IS_FOREST, NaturalistTags.Biomes.BLACKLIST_FOREST_RABBITS, MobCategory.CREATURE, EntityTypes.RABBIT, 10, 2, 3);
        c.accept(NaturalistTags.Biomes.HAS_GIANT_ISOPOD, NaturalistTags.Biomes.BLACKLIST_GIANT_ISOPOD, MobCategory.WATER_AMBIENT, NaturalistEntityTypes.GIANT_ISOPOD.get(), 8, 1, 2);
        c.accept(NaturalistTags.Biomes.HAS_HEDGEHOG, NaturalistTags.Biomes.BLACKLIST_HEDGEHOG, MobCategory.CREATURE, NaturalistEntityTypes.HEDGEHOG.get(), 10, 1, 3);
        c.accept(NaturalistTags.Biomes.HAS_JELLYFISH, NaturalistTags.Biomes.BLACKLIST_JELLYFISH, MobCategory.WATER_AMBIENT, NaturalistEntityTypes.JELLYFISH.get(), 10, 1, 3);
        c.accept(NaturalistTags.Biomes.HAS_GIRAFFE, NaturalistTags.Biomes.BLACKLIST_GIRAFFE, MobCategory.CREATURE, NaturalistEntityTypes.GIRAFFE.get(), 5, 1, 3);
        c.accept(NaturalistTags.Biomes.HAS_HIPPO, NaturalistTags.Biomes.BLACKLIST_HIPPO, MobCategory.CREATURE, NaturalistEntityTypes.HIPPO.get(), 10, 1, 3);
        c.accept(NaturalistTags.Biomes.HAS_KOMODO_DRAGON, NaturalistTags.Biomes.BLACKLIST_KOMODO_DRAGON, MobCategory.CREATURE, NaturalistEntityTypes.KOMODO_DRAGON.get(), 20, 1, 2);
        c.accept(NaturalistTags.Biomes.HAS_LION, NaturalistTags.Biomes.BLACKLIST_LION, MobCategory.CREATURE, NaturalistEntityTypes.LION.get(), 5, 3, 5);
        c.accept(NaturalistTags.Biomes.HAS_LIZARD, NaturalistTags.Biomes.BLACKLIST_LIZARD, MobCategory.CREATURE, NaturalistEntityTypes.LIZARD.get(), 10, 1, 1);
        c.accept(NaturalistTags.Biomes.HAS_MAMMOTH, NaturalistTags.Biomes.BLACKLIST_MAMMOTH, MobCategory.CREATURE, NaturalistEntityTypes.MAMMOTH.get(), 5, 2, 3);
        c.accept(NaturalistTags.Biomes.HAS_MOLE, NaturalistTags.Biomes.BLACKLIST_MOLE, MobCategory.CREATURE, NaturalistEntityTypes.MOLE.get(), 10, 1, 2);
        c.accept(NaturalistTags.Biomes.HAS_OSTRICH, NaturalistTags.Biomes.BLACKLIST_OSTRICH, MobCategory.CREATURE, NaturalistEntityTypes.OSTRICH.get(), 28, 1, 2);
        c.accept(NaturalistTags.Biomes.HAS_RAT, NaturalistTags.Biomes.BLACKLIST_RAT, MobCategory.CREATURE, NaturalistEntityTypes.RAT.get(), 10, 1, 3);
        c.accept(NaturalistTags.Biomes.HAS_RHINO, NaturalistTags.Biomes.BLACKLIST_RHINO, MobCategory.CREATURE, NaturalistEntityTypes.RHINO.get(), 1, 1, 3);
        c.accept(NaturalistTags.Biomes.HAS_ROBIN, NaturalistTags.Biomes.BLACKLIST_ROBIN, MobCategory.CREATURE, NaturalistEntityTypes.BIRD.get(), 10, 3, 4);
        c.accept(NaturalistTags.Biomes.HAS_SNAIL, NaturalistTags.Biomes.BLACKLIST_SNAIL, MobCategory.CREATURE, NaturalistEntityTypes.SNAIL.get(), 10, 2, 3);
        c.accept(NaturalistTags.Biomes.HAS_SNAKE, NaturalistTags.Biomes.BLACKLIST_SNAKE, MobCategory.CREATURE, NaturalistEntityTypes.SNAKE.get(), 10, 1, 1);
        c.accept(NaturalistTags.Biomes.HAS_RATTLESNAKE, NaturalistTags.Biomes.BLACKLIST_RATTLESNAKE, MobCategory.CREATURE, NaturalistEntityTypes.SNAKE.get(), 10, 1, 1);
        c.accept(NaturalistTags.Biomes.HAS_SPARROW, NaturalistTags.Biomes.BLACKLIST_SPARROW, MobCategory.CREATURE, NaturalistEntityTypes.BIRD.get(), 10, 3, 4);
        c.accept(NaturalistTags.Biomes.HAS_STARFISH, NaturalistTags.Biomes.BLACKLIST_STARFISH, MobCategory.WATER_AMBIENT, NaturalistEntityTypes.STARFISH.get(), 8, 1, 2);
        c.accept(NaturalistTags.Biomes.HAS_TIGER, NaturalistTags.Biomes.BLACKLIST_TIGER, MobCategory.CREATURE, NaturalistEntityTypes.TIGER.get(), 8, 1, 1);
        c.accept(NaturalistTags.Biomes.HAS_TORTOISE, NaturalistTags.Biomes.BLACKLIST_TORTOISE, MobCategory.CREATURE, NaturalistEntityTypes.TORTOISE.get(), 10, 1, 1);
        c.accept(NaturalistTags.Biomes.HAS_TURKEY, NaturalistTags.Biomes.BLACKLIST_TURKEY, MobCategory.CREATURE, NaturalistEntityTypes.TURKEY.get(), 17, 1, 2);
        c.accept(NaturalistTags.Biomes.HAS_VULTURE, NaturalistTags.Biomes.BLACKLIST_VULTURE, MobCategory.CREATURE, NaturalistEntityTypes.VULTURE.get(), 3, 3, 5);
        c.accept(NaturalistTags.Biomes.HAS_ZEBRA, NaturalistTags.Biomes.BLACKLIST_ZEBRA, MobCategory.CREATURE, NaturalistEntityTypes.ZEBRA.get(), 1, 2, 6);
    }
}
