package dev.resivore.mapmarkerextension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.resivore.mapmarkerextension.core.MapMarkerIdentity;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

final class NativeMapDecorationIdentityTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        MinecraftTestBootstrap.initialize();
    }

    @Test
    void allSeventeenCustomTypesHaveUniqueNativeIdsAndAssets() {
        Set<String> typeIds = Arrays.stream(MapMarkerIdentity.values())
            .filter(MapMarkerIdentity::customDecoration)
            .map(MapMarkerIdentity::decorationTypeId)
            .collect(Collectors.toSet());
        Set<String> assetIds = Arrays.stream(MapMarkerIdentity.values())
            .filter(MapMarkerIdentity::customDecoration)
            .map(MapMarkerIdentity::markerAssetId)
            .collect(Collectors.toSet());

        assertEquals(17, typeIds.size());
        assertEquals(17, assetIds.size());
        assertTrue(typeIds.stream().allMatch(id -> id.startsWith("map_marker_extension:")));
        assertTrue(assetIds.stream().allMatch(id -> id.startsWith(
            "map_marker_extension:poi_icons/"
        )));

    }

    @Test
    void customTypesCloneTheirLegacyBehavioralFlags() {
        for (MapMarkerIdentity identity : MapMarkerIdentity.values()) {
            if (!identity.customDecoration()) {
                continue;
            }
            MapDecorationType source = BuiltInRegistries.MAP_DECORATION_TYPE
                .get(Identifier.parse(identity.sourceDecorationTypeId()))
                .orElseThrow()
                .value();
            MapDecorationType custom = nativeType(identity, source);
            assertEquals(source.showOnItemFrame(), custom.showOnItemFrame(), identity.id());
            assertEquals(source.mapColor(), custom.mapColor(), identity.id());
            assertEquals(source.explorationMapElement(), custom.explorationMapElement(), identity.id());
            assertEquals(source.trackCount(), custom.trackCount(), identity.id());
        }
    }

    @Test
    void buriedTreasureKeepsTheVanillaRedXHolder() {
        MapMarkerIdentity identity = MapMarkerIdentity.BURIED_TREASURE;
        assertEquals("minecraft:red_x", identity.decorationTypeId());
        assertEquals("minecraft:red_x", identity.markerAssetId());
        assertEquals(
            Identifier.parse("minecraft:red_x"),
            BuiltInRegistries.MAP_DECORATION_TYPE
                .get(Identifier.parse("minecraft:red_x"))
                .orElseThrow()
                .value()
                .assetId()
        );
    }

    private static MapDecorationType nativeType(
        MapMarkerIdentity identity,
        MapDecorationType source
    ) {
        return new MapDecorationType(
            Identifier.parse(identity.markerAssetId()),
            source.showOnItemFrame(),
            source.mapColor(),
            source.explorationMapElement(),
            source.trackCount()
        );
    }
}
