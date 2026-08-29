package dev.resivore.mapmarkerextension.core;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;

public final class MapMarkerDecorationTypes {
    private static volatile Map<MapMarkerIdentity, Holder<MapDecorationType>> holders = Map.of();

    private MapMarkerDecorationTypes() {
    }

    public static synchronized void register() {
        if (!holders.isEmpty()) {
            return;
        }

        Map<MapMarkerIdentity, Holder<MapDecorationType>> registered =
            new EnumMap<>(MapMarkerIdentity.class);
        for (MapMarkerIdentity identity : MapMarkerIdentity.values()) {
            Identifier resultId = Identifier.parse(identity.decorationTypeId());
            Holder<MapDecorationType> holder = BuiltInRegistries.MAP_DECORATION_TYPE
                .get(resultId)
                .map(existing -> (Holder<MapDecorationType>) existing)
                .orElseGet(() -> registerCustom(identity, resultId));
            registered.put(identity, holder);
        }
        holders = Map.copyOf(registered);
    }

    public static Holder<MapDecorationType> holder(MapMarkerIdentity identity) {
        Holder<MapDecorationType> holder = holders.get(identity);
        if (holder == null) {
            throw new IllegalStateException("Map marker decoration types are not registered");
        }
        return holder;
    }

    private static Holder<MapDecorationType> registerCustom(
        MapMarkerIdentity identity,
        Identifier resultId
    ) {
        if (!identity.customDecoration()) {
            throw new IllegalStateException("Missing vanilla decoration type " + resultId);
        }

        MapDecorationType source = BuiltInRegistries.MAP_DECORATION_TYPE
            .get(Identifier.parse(identity.sourceDecorationTypeId()))
            .orElseThrow(() -> new IllegalStateException(
                "Missing source map-decoration type " + identity.sourceDecorationTypeId()
            ))
            .value();
        MapDecorationType custom = new MapDecorationType(
            Identifier.parse(identity.markerAssetId()),
            source.showOnItemFrame(),
            source.mapColor(),
            source.explorationMapElement(),
            source.trackCount()
        );
        return Registry.registerForHolder(
            BuiltInRegistries.MAP_DECORATION_TYPE,
            resultId,
            custom
        );
    }
}
