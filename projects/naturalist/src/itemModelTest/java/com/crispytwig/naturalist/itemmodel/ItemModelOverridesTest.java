package com.crispytwig.naturalist.itemmodel;

import com.google.gson.JsonParser;
import com.crispytwig.naturalist.client.model.item.ItemModelOverrides;
import com.crispytwig.naturalist.client.model.item.LegacyItemModelResources;
import com.crispytwig.naturalist.client.model.item.NaturalistItemModelProperties;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemModelOverridesTest {
    private static final Identifier COLOR = Identifier.withDefaultNamespace("color");

    @Test
    void parsesProtectedLegacyShapeAndSelectsNonDefaultColor() {
        ItemModelOverrides overrides = LegacyItemModelResources.parseOverrides(JsonParser.parseString("""
                {
                  "overrides": [
                    {"predicate":{"minecraft:color":0.0},"model":"naturalist:item/snail/white"},
                    {"predicate":{"minecraft:color":0.8},"model":"naturalist:item/snail/brown"},
                    {"predicate":{"minecraft:color":1.0},"model":"naturalist:item/snail/black"}
                  ]
                }
                """).getAsJsonObject());

        assertEquals(Identifier.parse("naturalist:item/snail/brown"),
                overrides.resolve(id -> id.equals(COLOR) ? 12.0F / 15.0F : Float.NEGATIVE_INFINITY).orElseThrow());
    }

    @Test
    void lastMatchingEntryKeepsLegacyThresholdPrecedence() {
        ItemModelOverrides overrides = new ItemModelOverrides(java.util.List.of(
                new ItemModelOverrides.Entry(Identifier.parse("naturalist:item/snail/white"), Map.of(COLOR, 0.0F)),
                new ItemModelOverrides.Entry(Identifier.parse("naturalist:item/snail/black"), Map.of(COLOR, 1.0F))));

        assertEquals(Identifier.parse("naturalist:item/snail/black"),
                overrides.resolve(id -> 1.0F).orElseThrow());
        assertEquals(Identifier.parse("naturalist:item/snail/white"),
                overrides.resolve(id -> 0.0F).orElseThrow());
    }

    @Test
    void unavailablePropertyLeavesTheParentModelSelected() {
        ItemModelOverrides overrides = new ItemModelOverrides(java.util.List.of(
                new ItemModelOverrides.Entry(Identifier.parse("naturalist:item/knapsack_filled"),
                        Map.of(Identifier.parse("naturalist:filled"), 1.0F))));

        assertTrue(overrides.resolve(id -> Float.NEGATIVE_INFINITY).isEmpty());
    }

    @Test
    void numericPropertiesRetainLegacyClamping() {
        NaturalistItemModelProperties.Property belowRange = (stack, level, owner, seed) -> -1.0F;
        NaturalistItemModelProperties.Property aboveRange = (stack, level, owner, seed) -> 2.0F;

        assertEquals(0.0F, belowRange.call(null, null, null, 0));
        assertEquals(1.0F, aboveRange.call(null, null, null, 0));
    }

    @Test
    void eachPropertyIsEvaluatedOnlyOncePerResolution() {
        ItemModelOverrides overrides = new ItemModelOverrides(java.util.List.of(
                new ItemModelOverrides.Entry(Identifier.parse("naturalist:item/snail/white"), Map.of(COLOR, 0.0F)),
                new ItemModelOverrides.Entry(Identifier.parse("naturalist:item/snail/black"), Map.of(COLOR, 1.0F))));
        AtomicInteger calls = new AtomicInteger();

        assertEquals(Identifier.parse("naturalist:item/snail/white"),
                overrides.resolve(id -> {
                    calls.incrementAndGet();
                    return 0.0F;
                }).orElseThrow());
        assertEquals(1, calls.get());
    }
}
