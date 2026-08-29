package dev.resivore.mapmarkerextension.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.resivore.mapmarkerextension.MinecraftTestBootstrap;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.MapDecorations;
import net.minecraft.world.item.component.MapItemColor;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import net.minecraft.world.level.saveddata.maps.MapId;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

final class MapMarkerNormalizerTest {
    private static final Map<MapMarkerIdentity, Holder<MapDecorationType>> TEST_TYPES =
        new EnumMap<>(MapMarkerIdentity.class);

    @BeforeAll
    static void bootstrapMinecraft() {
        MinecraftTestBootstrap.initialize();
        for (MapMarkerIdentity identity : MapMarkerIdentity.values()) {
            if (identity.customDecoration()) {
                MapDecorationType source = holder(identity.sourceDecorationTypeId()).value();
                TEST_TYPES.put(identity, Holder.direct(new MapDecorationType(
                    Identifier.parse(identity.markerAssetId()),
                    source.showOnItemFrame(),
                    source.mapColor(),
                    source.explorationMapElement(),
                    source.trackCount()
                )));
            }
        }
    }

    @Test
    void allSeventeenKnownPoiMapsNormalizeToDistinctNativeTypesAndAssets() {
        Set<String> resultingTypes = Arrays.stream(MapMarkerIdentity.values())
            .filter(MapMarkerIdentity::customDecoration)
            .map(identity -> {
                ItemStack stack = mapWithTarget(identity, 12.25D, -91.75D, 33.5F);
                MapDecorations.Entry changed = normalize(stack).orElseThrow();
                assertSame(TEST_TYPES.get(identity), changed.type(), identity.id());
                assertEquals(identity.markerAssetId(), changed.type().value().assetId().toString());
                return identity.decorationTypeId();
            })
            .collect(Collectors.toSet());

        assertEquals(17, resultingTypes.size());
    }

    @Test
    void normalizationPreservesCoordinatesMapIdUnrelatedDecorationsAndOtherComponents() {
        MapMarkerIdentity identity = MapMarkerIdentity.ABBEY;
        ItemStack stack = mapWithTarget(identity, -1234.5D, 8765.25D, 271.75F);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("My Abbey"));
        MapId mapId = new MapId(731);
        MapItemColor mapColor = new MapItemColor(0x5A77CC);
        stack.set(DataComponents.MAP_ID, mapId);
        stack.set(DataComponents.MAP_COLOR, mapColor);

        MapDecorations before = stack.get(DataComponents.MAP_DECORATIONS);
        MapDecorations.Entry source = before.decorations().get("+");
        MapDecorations.Entry unrelated = before.decorations().get("banner-home");
        int count = stack.getCount();

        MapDecorations.Entry changed = normalize(stack).orElseThrow();
        MapDecorations after = stack.get(DataComponents.MAP_DECORATIONS);

        assertEquals(source.x(), changed.x());
        assertEquals(source.z(), changed.z());
        assertEquals(source.rotation(), changed.rotation());
        assertSame(TEST_TYPES.get(identity), changed.type());
        assertSame(mapId, stack.get(DataComponents.MAP_ID));
        assertSame(mapColor, stack.get(DataComponents.MAP_COLOR));
        assertEquals(Component.literal("My Abbey"), stack.get(DataComponents.CUSTOM_NAME));
        assertEquals(count, stack.getCount());
        assertEquals(before.decorations().keySet(), after.decorations().keySet());
        assertSame(unrelated, after.decorations().get("banner-home"));
    }

    @Test
    void normalizationIsIdempotent() {
        ItemStack stack = mapWithTarget(MapMarkerIdentity.TRAIL_RUINS, 9.0D, 25.0D, 0.0F);
        assertTrue(normalize(stack).isPresent());
        MapDecorations normalized = stack.get(DataComponents.MAP_DECORATIONS);

        assertTrue(normalize(stack).isEmpty());
        assertSame(normalized, stack.get(DataComponents.MAP_DECORATIONS));
    }

    @Test
    void buriedTreasureRemainsVanillaRedX() {
        ItemStack stack = mapWithTarget(
            MapMarkerIdentity.BURIED_TREASURE,
            9.0D,
            25.0D,
            0.0F
        );
        MapDecorations before = stack.get(DataComponents.MAP_DECORATIONS);

        assertTrue(normalize(stack).isEmpty());
        assertSame(before, stack.get(DataComponents.MAP_DECORATIONS));
        assertEquals("minecraft:red_x", typeId(before.decorations().get("+")));
    }

    @Test
    void ordinaryAbsentOrMismatchedTargetsAreUntouched() {
        ItemStack ordinary = new ItemStack(net.minecraft.world.item.Items.FILLED_MAP);
        ordinary.set(DataComponents.MAP_DECORATIONS, new MapDecorations(Map.of(
            "+",
            entry(holder("minecraft:red_x"), 9.0D, 25.0D, 0.0F)
        )));

        ItemStack mismatched = MapMarkerItemIdentityTest.namedMap(MapMarkerIdentity.ABBEY);
        MapDecorations mismatchDecorations = new MapDecorations(Map.of(
            "+",
            entry(holder("minecraft:swamp_hut"), 9.0D, 25.0D, 0.0F)
        ));
        mismatched.set(DataComponents.MAP_DECORATIONS, mismatchDecorations);

        ItemStack absent = MapMarkerItemIdentityTest.namedMap(MapMarkerIdentity.ABBEY);
        absent.set(DataComponents.MAP_DECORATIONS, new MapDecorations(Map.of(
            "other",
            entry(holder("minecraft:red_x"), 9.0D, 25.0D, 0.0F)
        )));

        assertTrue(normalize(ordinary).isEmpty());
        assertTrue(normalize(mismatched).isEmpty());
        assertSame(mismatchDecorations, mismatched.get(DataComponents.MAP_DECORATIONS));
        assertTrue(normalize(absent).isEmpty());
    }

    @Test
    void formerlySharedLegacyTypesNoLongerCollapse() {
        assertFalse(MapMarkerIdentity.ABBEY.decorationTypeId().equals(
            MapMarkerIdentity.ANCIENT_CITY.decorationTypeId()
        ));
        assertFalse(MapMarkerIdentity.DESERT_PYRAMID.decorationTypeId().equals(
            MapMarkerIdentity.DESERT_VILLAGE.decorationTypeId()
        ));
        assertFalse(MapMarkerIdentity.TRAIL_RUINS.decorationTypeId().equals(
            MapMarkerIdentity.PLAINS_VILLAGE.decorationTypeId()
        ));
    }

    private static ItemStack mapWithTarget(
        MapMarkerIdentity identity,
        double x,
        double z,
        float rotation
    ) {
        ItemStack stack = MapMarkerItemIdentityTest.namedMap(identity);
        stack.setCount(3);
        Map<String, MapDecorations.Entry> decorations = new LinkedHashMap<>();
        decorations.put("+", entry(holder(identity.sourceDecorationTypeId()), x, z, rotation));
        decorations.put(
            "banner-home",
            entry(holder("minecraft:banner_white"), x + 1.0D, z + 1.0D, 90.0F)
        );
        stack.set(DataComponents.MAP_DECORATIONS, new MapDecorations(decorations));
        return stack;
    }

    private static MapDecorations.Entry entry(
        Holder<MapDecorationType> type,
        double x,
        double z,
        float rotation
    ) {
        return new MapDecorations.Entry(type, x, z, rotation);
    }

    private static Holder<MapDecorationType> holder(String id) {
        return BuiltInRegistries.MAP_DECORATION_TYPE
            .get(Identifier.parse(id))
            .orElseThrow();
    }

    private static java.util.Optional<MapDecorations.Entry> normalize(ItemStack stack) {
        return MapMarkerNormalizer.normalize(stack, TEST_TYPES::get);
    }

    private static String typeId(MapDecorations.Entry entry) {
        return entry.type().unwrapKey().orElseThrow().identifier().toString();
    }
}
