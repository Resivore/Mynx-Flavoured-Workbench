package com.crispytwig.naturalist.server.entity.variant;

import com.crispytwig.naturalist.Naturalist;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;

import java.util.List;
import java.util.Optional;

public final class MobVariantUtil {
    private MobVariantUtil() {
    }

    public static Optional<Holder.Reference<MobVariant>> byId(HolderLookup.Provider provider, ResourceKey<Registry<MobVariant>> registryKey, Identifier id) {
        return provider.lookup(registryKey).flatMap(lookup -> lookup.get(ResourceKey.create(registryKey, id)));
    }

    public static Optional<Holder.Reference<MobVariant>> byKey(HolderLookup.Provider provider, ResourceKey<Registry<MobVariant>> registryKey, ResourceKey<MobVariant> key) {
        return provider.lookup(registryKey).flatMap(lookup -> lookup.get(key));
    }

    public static Optional<Identifier> readVariantId(CompoundTag tag, String[] legacyNames) {
        Optional<String> serialized = tag.getString(DataDrivenVariantAnimal.VARIANT_TAG);
        if (serialized.isPresent()) {
            return Optional.ofNullable(Identifier.tryParse(serialized.get()));
        }
        if (legacyNames != null && legacyNames.length > 0) {
            return tag.getInt(DataDrivenVariantAnimal.VARIANT_TAG)
                    .map(index -> Naturalist.location(legacyNames[Math.floorMod(index, legacyNames.length)]));
        }
        return Optional.empty();
    }

    public static Optional<Holder.Reference<MobVariant>> selectVariantForSpawn(ServerLevelAccessor level, BlockPos pos, ResourceKey<Registry<MobVariant>> registryKey) {
        Optional<Registry<MobVariant>> maybeRegistry = level.registryAccess().lookup(registryKey);
        if (maybeRegistry.isEmpty()) {
            return Optional.empty();
        }
        Registry<MobVariant> registry = maybeRegistry.get();
        Holder<Biome> biome = level.getBiome(pos);
        List<Holder.Reference<MobVariant>> pool = registry.listElements()
                .filter(holder -> holder.value().weight() > 0)
                .filter(holder -> holder.value().biomes().map(biomes -> biomes.contains(biome)).orElse(false))
                .toList();
        if (pool.isEmpty()) {
            pool = registry.listElements()
                    .filter(holder -> holder.value().weight() > 0 && holder.value().biomes().isEmpty())
                    .toList();
        }
        return weightedChoice(highestPriority(pool), level.getRandom());
    }

    private static List<Holder.Reference<MobVariant>> highestPriority(List<Holder.Reference<MobVariant>> pool) {
        int max = pool.stream().mapToInt(holder -> holder.value().priority()).max().orElse(0);
        return pool.stream().filter(holder -> holder.value().priority() == max).toList();
    }

    private static Optional<Holder.Reference<MobVariant>> weightedChoice(List<Holder.Reference<MobVariant>> pool, RandomSource random) {
        int total = pool.stream().mapToInt(holder -> holder.value().weight()).sum();
        if (total <= 0) {
            return Optional.empty();
        }
        int roll = random.nextInt(total);
        for (Holder.Reference<MobVariant> holder : pool) {
            roll -= holder.value().weight();
            if (roll < 0) {
                return Optional.of(holder);
            }
        }
        return Optional.empty();
    }
}
