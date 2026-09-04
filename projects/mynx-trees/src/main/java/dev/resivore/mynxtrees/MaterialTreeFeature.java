package dev.resivore.mynxtrees;

import com.google.gson.*;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.*;
import net.minecraft.core.Holder;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.*;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/** Copies the loaded target feature, so active data-pack shape/decorator changes survive. */
public final class MaterialTreeFeature extends Feature<MaterialTreeFeature.Config> {
    public record Config(Holder<ConfiguredFeature<?, ?>> source, String species) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
            ConfiguredFeature.CODEC.fieldOf("source").forGetter(Config::source),
            Codec.STRING.validate(s -> Set.of("silver_birch", "wisteria", "cherry").contains(s) ? DataResult.success(s) : DataResult.error(() -> "Unknown tree species: " + s)).fieldOf("species").forGetter(Config::species)
        ).apply(i, Config::new));
    }
    // Registry holders are reload-scoped; weak keys avoid retaining old worlds/data packs.
    private final Map<Config, ConfiguredFeature<?, ?>> cache = Collections.synchronizedMap(new WeakHashMap<>());
    public MaterialTreeFeature() { super(Config.CODEC); }
    @Override public boolean place(FeaturePlaceContext<Config> context) {
        ConfiguredFeature<?, ?> copy;
        synchronized (cache) { copy = cache.computeIfAbsent(context.config(), c -> copy(c, context.level())); }
        return copy.place(context.level(), context.chunkGenerator(), context.random(), context.origin());
    }
    private static ConfiguredFeature<?, ?> copy(Config config, WorldGenLevel level) {
        var ops = level.registryAccess().createSerializationContext(JsonOps.INSTANCE);
        JsonObject json = ConfiguredFeature.DIRECT_CODEC.encodeStart(ops, config.source().value()).getOrThrow().getAsJsonObject();
        TreeMaterials.transform(json, config.species());
        return ConfiguredFeature.DIRECT_CODEC.parse(ops, json).getOrThrow();
    }
}
