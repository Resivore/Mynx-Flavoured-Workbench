package dev.resivore.mapmarkerextension.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

final class MapMarkerTargetResolverTest {
    @Test
    void allEighteenKnownIdentitiesConsumeTheirLiveDecorationAssets() {
        for (MapMarkerIdentity identity : MapMarkerIdentity.values()) {
            MapMarkerTarget target = resolve(identifiedMap(
                identity.ordinal() + 1,
                identity,
                decoration(identity.decorationTypeId(), identity.markerAssetId(), 9.0D, 25.0D)
            )).getFirst();
            assertEquals(identity.decorationTypeId(), target.decorationTypeId(), identity.id());
            assertEquals(identity.markerAssetId(), target.decorationAssetId(), identity.id());
        }
    }

    @Test
    void formerlySharedTypesRemainDistinctEvenAtIdenticalCoordinates() {
        List<MapMarkerTarget> targets = resolve(
            identifiedMap(1, MapMarkerIdentity.ABBEY, decoration(
                MapMarkerIdentity.ABBEY.decorationTypeId(),
                MapMarkerIdentity.ABBEY.markerAssetId(),
                25.0D,
                -7.0D
            )),
            identifiedMap(2, MapMarkerIdentity.ANCIENT_CITY, decoration(
                MapMarkerIdentity.ANCIENT_CITY.decorationTypeId(),
                MapMarkerIdentity.ANCIENT_CITY.markerAssetId(),
                25.0D,
                -7.0D
            )),
            identifiedMap(3, MapMarkerIdentity.PAPAL_OUTPOST, decoration(
                MapMarkerIdentity.PAPAL_OUTPOST.decorationTypeId(),
                MapMarkerIdentity.PAPAL_OUTPOST.markerAssetId(),
                25.0D,
                -7.0D
            ))
        );

        assertEquals(3, targets.size());
        assertEquals(3, targets.stream()
            .map(MapMarkerTarget::decorationAssetId)
            .collect(Collectors.toSet()).size());
    }

    @Test
    void exactCoordinatesAndAcceptedHalfBlockRenderAnchorRemainFrozen() {
        MapMarkerTarget target = resolve(identifiedMap(
            12,
            MapMarkerIdentity.WITCH_HUT,
            decoration(
                MapMarkerIdentity.WITCH_HUT.decorationTypeId(),
                MapMarkerIdentity.WITCH_HUT.markerAssetId(),
                -7.0D,
                9.0D
            )
        )).getFirst();

        assertEquals(-7, target.blockX());
        assertEquals(9, target.blockZ());
        assertEquals(-6.5D, target.renderX());
        assertEquals(9.5D, target.renderZ());
        assertEquals(Set.of(12), target.sourceMapIds());
    }

    @Test
    void duplicateSupportingMapsDeduplicateAndPossessionRemovalIsImmediate() {
        MapObservation first = identifiedMap(1, MapMarkerIdentity.OCEAN_MONUMENT, decoration(
            MapMarkerIdentity.OCEAN_MONUMENT.decorationTypeId(),
            MapMarkerIdentity.OCEAN_MONUMENT.markerAssetId(),
            25.0D,
            -7.0D
        ));
        MapObservation second = identifiedMap(2, MapMarkerIdentity.OCEAN_MONUMENT, decoration(
            MapMarkerIdentity.OCEAN_MONUMENT.decorationTypeId(),
            MapMarkerIdentity.OCEAN_MONUMENT.markerAssetId(),
            25.0D,
            -7.0D
        ));

        assertEquals(Set.of(1, 2), resolve(first, second).getFirst().sourceMapIds());
        assertEquals(Set.of(2), resolve(second).getFirst().sourceMapIds());
        assertTrue(resolve().isEmpty());
    }

    @Test
    void dimensionsAndDistinctCoordinatesRemainDistinct() {
        List<MapMarkerTarget> targets = resolve(
            map(1, true, "minecraft:overworld", decoration(
                "minecraft:red_x", "minecraft:red_x", 9.0D, 9.0D
            )),
            map(2, true, "minecraft:overworld", decoration(
                "minecraft:red_x", "minecraft:red_x", 25.0D, 9.0D
            )),
            map(3, true, "example:other", decoration(
                "minecraft:red_x", "minecraft:red_x", 9.0D, 9.0D
            ))
        );

        assertEquals(3, targets.size());
    }

    @Test
    void ordinaryEmptyNonMapAndUnrelatedDecorationsStayInvisible() {
        assertTrue(resolve(map(1, true, "minecraft:overworld")).isEmpty());
        assertTrue(resolve(map(2, false, "minecraft:overworld", decoration(
            "minecraft:red_x", "minecraft:red_x", 9.0D, 9.0D
        ))).isEmpty());
        assertTrue(resolve(map(3, true, "minecraft:overworld", decoration(
            "minecraft:player", "minecraft:player", 9.0D, 9.0D
        ), decoration(
            "example:other", "example:other", 9.0D, 9.0D
        ))).isEmpty());
    }

    @Test
    void legacyAcceptedTargetsRemainVisibleDuringTheMigrationTick() {
        assertEquals(
            MapMarkerTargetResolver.LEGACY_ADMITTED_DECORATION_IDS,
            Set.of(
                "minecraft:red_x",
                "minecraft:swamp_hut",
                "minecraft:village_desert",
                "minecraft:jungle_temple",
                "minecraft:village_plains",
                "minecraft:monument",
                "minecraft:trial_chambers",
                "minecraft:mansion"
            )
        );
        for (String type : MapMarkerTargetResolver.LEGACY_ADMITTED_DECORATION_IDS) {
            assertEquals(1, resolve(map(
                type.hashCode(), true, "minecraft:overworld", decoration(type, type, 9.0D, 25.0D)
            )).size());
        }
    }

    @Test
    void nonFiniteCoordinatesAreRejected() {
        List<DecorationObservation> invalid = Arrays.asList(
            decoration("minecraft:red_x", "minecraft:red_x", Double.NaN, 9.0D),
            decoration("minecraft:red_x", "minecraft:red_x", 9.0D, Double.POSITIVE_INFINITY)
        );
        assertTrue(MapMarkerTargetResolver.resolve(List.of(new MapObservation(
            1, true, "minecraft:overworld", Optional.empty(), invalid
        ))).isEmpty());
    }

    private static MapObservation map(
        int id,
        boolean filled,
        String dimension,
        DecorationObservation... decorations
    ) {
        return new MapObservation(id, filled, dimension, Optional.empty(), List.of(decorations));
    }

    private static MapObservation identifiedMap(
        int id,
        MapMarkerIdentity identity,
        DecorationObservation... decorations
    ) {
        return new MapObservation(
            id,
            true,
            "minecraft:overworld",
            Optional.of(identity),
            List.of(decorations)
        );
    }

    private static DecorationObservation decoration(
        String type,
        String asset,
        double x,
        double z
    ) {
        return new DecorationObservation(type, asset, x, z);
    }

    private static List<MapMarkerTarget> resolve(MapObservation... maps) {
        return MapMarkerTargetResolver.resolve(List.of(maps));
    }
}
