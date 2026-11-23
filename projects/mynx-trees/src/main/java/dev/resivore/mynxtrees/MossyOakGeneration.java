package dev.resivore.mynxtrees;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;

public final class MossyOakGeneration {
    public static final ScopedValue<Integer> CHANCE = ScopedValue.newInstance();

    public record OakSapling(ServerLevel level, BlockPos pos) {}
    public static final ScopedValue<OakSapling> SAPLING = ScopedValue.newInstance();

    public static int denominator(ConfiguredFeature<?, ?> feature, WorldGenLevel level, BlockPos origin) {
        if (feature.feature() != Feature.TREE && feature.feature() != Feature.FALLEN_TREE) return 0;
        if (!(level instanceof WorldGenRegion)) {
            OakSapling sapling = SAPLING.isBound() ? SAPLING.get() : null;
            if (sapling == null || sapling.level() != level || !sapling.pos().equals(origin)
                    || feature.feature() != Feature.TREE) return 0;
        }
        Identifier id = level.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE).getKey(feature);
        if (id == null || !id.getNamespace().equals("minecraft")) return 0;
        return switch (id.getPath()) {
            case "swamp_oak" -> feature.feature() == Feature.TREE ? 3 : 0;
            case "fallen_oak_tree" -> feature.feature() == Feature.FALLEN_TREE ? 3 : 0;
            case "fancy_oak", "fancy_oak_bees", "fancy_oak_bees_002", "fancy_oak_bees_005",
                 "fancy_oak_leaf_litter", "fancy_oak_bees_0002_leaf_litter" -> feature.feature() == Feature.TREE ? 10 : 0;
            default -> 0;
        };
    }

    public static BlockState timber(BlockState state, WorldGenLevel level, BlockPos pos) {
        int chance = CHANCE.orElse(0);
        if (chance == 0 || !state.is(Blocks.OAK_LOG)) return state;
        return selected(level.getSeed(), pos, chance) ? NativeWoods.MOSSY_LOG.withPropertiesOf(state) : state;
    }

    static boolean selected(long worldSeed, BlockPos pos, int denominator) {
        long seed = RandomSupport.mixStafford13(pos.asLong() ^ worldSeed ^ 0x54d8abd9207a3bf1L);
        return RandomSource.create(seed).nextInt(denominator) == 0;
    }
}
