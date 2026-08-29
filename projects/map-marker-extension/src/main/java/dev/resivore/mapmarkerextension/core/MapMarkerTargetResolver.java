package dev.resivore.mapmarkerextension.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public final class MapMarkerTargetResolver {
    public static final Set<String> LEGACY_ADMITTED_DECORATION_IDS = Set.of(
        "minecraft:red_x",
        "minecraft:swamp_hut",
        "minecraft:village_desert",
        "minecraft:jungle_temple",
        "minecraft:village_plains",
        "minecraft:monument",
        "minecraft:trial_chambers",
        "minecraft:mansion"
    );
    public static final Set<String> CUSTOM_DECORATION_IDS = Arrays.stream(MapMarkerIdentity.values())
        .filter(MapMarkerIdentity::customDecoration)
        .map(MapMarkerIdentity::decorationTypeId)
        .collect(Collectors.toUnmodifiableSet());

    private static final Comparator<TargetKey> TARGET_ORDER = Comparator
        .comparing(TargetKey::dimensionId)
        .thenComparingInt(TargetKey::blockX)
        .thenComparingInt(TargetKey::blockZ)
        .thenComparing(TargetKey::decorationTypeId)
        .thenComparing(TargetKey::decorationAssetId);

    private MapMarkerTargetResolver() {
    }

    public static List<MapMarkerTarget> resolve(List<MapObservation> maps) {
        Map<TargetKey, Set<Integer>> sourcesByTarget = new LinkedHashMap<>();
        for (MapObservation map : maps) {
            if (!map.filledMap()) {
                continue;
            }
            for (DecorationObservation decoration : map.decorations()) {
                Optional<MapMarkerIdentity> matchedIdentity = map.identity()
                    .filter(identity -> identity.matchesSourceOrResultType(decoration.typeId()));
                boolean admitted = matchedIdentity.isPresent()
                    || LEGACY_ADMITTED_DECORATION_IDS.contains(decoration.typeId())
                    || CUSTOM_DECORATION_IDS.contains(decoration.typeId());
                if (!admitted
                    || !Double.isFinite(decoration.worldX())
                    || !Double.isFinite(decoration.worldZ())) {
                    continue;
                }

                TargetKey key = new TargetKey(
                    map.dimensionId(),
                    floorToBlock(decoration.worldX()),
                    floorToBlock(decoration.worldZ()),
                    decoration.typeId(),
                    decoration.assetId()
                );
                sourcesByTarget.computeIfAbsent(key, ignored -> new LinkedHashSet<>()).add(map.mapId());
            }
        }

        List<TargetKey> orderedKeys = new ArrayList<>(sourcesByTarget.keySet());
        orderedKeys.sort(TARGET_ORDER);
        List<MapMarkerTarget> result = new ArrayList<>(orderedKeys.size());
        for (TargetKey key : orderedKeys) {
            result.add(new MapMarkerTarget(
                key.dimensionId(),
                key.decorationTypeId(),
                key.decorationAssetId(),
                key.blockX(),
                key.blockZ(),
                sourcesByTarget.get(key)
            ));
        }
        return List.copyOf(result);
    }

    private static int floorToBlock(double coordinate) {
        double floored = Math.floor(coordinate);
        if (floored < Integer.MIN_VALUE || floored > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Map decoration coordinate is outside block-coordinate range");
        }
        return (int) floored;
    }

    private record TargetKey(
        String dimensionId,
        int blockX,
        int blockZ,
        String decorationTypeId,
        String decorationAssetId
    ) {
    }
}
