package com.yungnickyoung.minecraft.ribbits.world.feature;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import net.minecraft.SharedConstants;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.CompositeFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

class ConfiguredFeatureMigrationCodecTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void minecraftTwentySixRegistryDecodesTheRandomPatchReplacementSchema() {
        JsonElement synthetic = JsonParser.parseString("""
                {
                  "type": "minecraft:sequence",
                  "config": {
                    "features": [
                      {
                        "feature": {
                          "type": "minecraft:no_op",
                          "config": {}
                        },
                        "placement": [
                          {
                            "type": "minecraft:count",
                            "count": 5
                          },
                          {
                            "type": "minecraft:random_offset",
                            "xz_spread": {
                              "type": "minecraft:trapezoid",
                              "min": -2,
                              "max": 2,
                              "plateau": 0
                            },
                            "y_spread": {
                              "type": "minecraft:trapezoid",
                              "min": -1,
                              "max": 1,
                              "plateau": 0
                            }
                          },
                          {
                            "type": "minecraft:block_predicate_filter",
                            "predicate": {
                              "type": "minecraft:matching_blocks",
                              "blocks": "minecraft:air"
                            }
                          }
                        ]
                      }
                    ]
                  }
                }
                """);

        List<Registry<?>> registries = new ArrayList<>(
                RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY)
                        .registries()
                        .map(RegistryAccess.RegistryEntry::value)
                        .toList());
        registries.add(new MappedRegistry<ConfiguredFeature<?, ?>>(
                Registries.CONFIGURED_FEATURE, Lifecycle.stable()).freeze());
        registries.add(new MappedRegistry<PlacedFeature>(
                Registries.PLACED_FEATURE, Lifecycle.stable()).freeze());
        RegistryOps<JsonElement> registryOps = RegistryOps.create(
                JsonOps.INSTANCE, new RegistryAccess.ImmutableRegistryAccess(registries));
        ConfiguredFeature<?, ?> decoded = ConfiguredFeature.DIRECT_CODEC
                .parse(registryOps, synthetic)
                .getOrThrow();

        assertSame(Feature.SEQUENCE, decoded.feature());
        CompositeFeatureConfiguration sequence =
                assertInstanceOf(CompositeFeatureConfiguration.class, decoded.config());
        assertEquals(1, sequence.features().size());
        PlacedFeature placed = sequence.features().iterator().next().value();
        assertSame(Feature.NO_OP, placed.feature().value().feature());
        assertEquals(3, placed.placement().size());
        assertSame(PlacementModifierType.COUNT, placed.placement().get(0).type());
        assertSame(PlacementModifierType.RANDOM_OFFSET, placed.placement().get(1).type());
        assertSame(PlacementModifierType.BLOCK_PREDICATE_FILTER,
                placed.placement().get(2).type());

        JsonObject roundTrip = ConfiguredFeature.DIRECT_CODEC
                .encodeStart(registryOps, decoded)
                .getOrThrow()
                .getAsJsonObject();
        JsonElement encodedFeatures = roundTrip.getAsJsonObject("config").get("features");
        JsonObject encodedPlacedFeature = encodedFeatures.isJsonArray()
                ? encodedFeatures.getAsJsonArray().get(0).getAsJsonObject()
                : encodedFeatures.getAsJsonObject();
        JsonObject randomOffset = encodedPlacedFeature
                .getAsJsonArray("placement")
                .get(1).getAsJsonObject();
        assertEquals("minecraft:random_offset", randomOffset.get("type").getAsString());
        assertEquals(-2, randomOffset.getAsJsonObject("xz_spread").get("min").getAsInt());
        assertEquals(2, randomOffset.getAsJsonObject("xz_spread").get("max").getAsInt());
        assertEquals(0, randomOffset.getAsJsonObject("xz_spread").get("plateau").getAsInt());
        assertEquals(-1, randomOffset.getAsJsonObject("y_spread").get("min").getAsInt());
        assertEquals(1, randomOffset.getAsJsonObject("y_spread").get("max").getAsInt());
        assertEquals(0, randomOffset.getAsJsonObject("y_spread").get("plateau").getAsInt());
    }
}
