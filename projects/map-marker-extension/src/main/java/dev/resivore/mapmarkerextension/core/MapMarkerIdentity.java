package dev.resivore.mapmarkerextension.core;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/** The authoritative mapping between stable map products and native marker identities. */
public enum MapMarkerIdentity {
    ABBEY("abbey", "filled_map.abbey_overgrown", "minecraft:red_x", "minecraft:red_x"),
    ANCIENT_CITY("ancient_city", "filled_map.ancient_city", "minecraft:red_x", "minecraft:red_x"),
    BURIED_MINESHAFT("buried_mineshaft", "filled_map.mineshaft", "minecraft:red_x", "minecraft:red_x"),
    BURIED_TREASURE(
        "buried_treasure",
        "filled_map.buried_treasure",
        "minecraft:red_x",
        "minecraft:red_x",
        false
    ),
    DESERT_PYRAMID(
        "desert_pyramid",
        "filled_map.desert_pyramid",
        "minecraft:village_desert",
        "minecraft:desert_village"
    ),
    DESERT_VILLAGE(
        "desert_village",
        "filled_map.village_desert",
        "minecraft:village_desert",
        "minecraft:desert_village"
    ),
    JUNGLE_PYRAMID(
        "jungle_pyramid",
        "filled_map.explorer_jungle",
        "minecraft:jungle_temple",
        "minecraft:jungle_temple"
    ),
    OCEAN_MONUMENT(
        "ocean_monument",
        "filled_map.monument",
        "minecraft:monument",
        "minecraft:ocean_monument"
    ),
    PAPAL_OUTPOST("papal_outpost", "filled_map.papal_outpost", "minecraft:red_x", "minecraft:red_x"),
    PLAINS_VILLAGE(
        "plains_village",
        "filled_map.village_plains",
        "minecraft:village_plains",
        "minecraft:plains_village"
    ),
    SAVANNAH_VILLAGE(
        "savannah_village",
        "filled_map.village_savanna",
        "minecraft:village_savanna",
        "minecraft:savanna_village"
    ),
    SNOWY_VILLAGE(
        "snowy_village",
        "filled_map.village_snowy",
        "minecraft:village_snowy",
        "minecraft:snowy_village"
    ),
    TAIGA_VILLAGE(
        "taiga_village",
        "filled_map.village_taiga",
        "minecraft:village_taiga",
        "minecraft:taiga_village"
    ),
    TRAIL_RUINS(
        "trail_ruins",
        "filled_map.trail_ruin",
        "minecraft:village_plains",
        "minecraft:plains_village"
    ),
    TRIAL_CHAMBER(
        "trial_chamber",
        "filled_map.trial_chambers",
        "minecraft:trial_chambers",
        "minecraft:trial_chambers"
    ),
    WARM_OCEAN_RUINS(
        "warm_ocean_ruins",
        "filled_map.warm_ocean_ruin",
        "minecraft:red_x",
        "minecraft:red_x"
    ),
    WITCH_HUT(
        "witch_hut",
        "filled_map.explorer_swamp",
        "minecraft:swamp_hut",
        "minecraft:swamp_hut"
    ),
    WOODLAND_MANSION(
        "woodland_mansion",
        "filled_map.mansion",
        "minecraft:mansion",
        "minecraft:woodland_mansion"
    );

    public static final String RESOURCE_NAMESPACE = "map_marker_extension";

    private static final Map<String, MapMarkerIdentity> BY_ITEM_NAME_TRANSLATION_KEY =
        Arrays.stream(values()).collect(Collectors.toUnmodifiableMap(
            MapMarkerIdentity::itemNameTranslationKey,
            Function.identity()
        ));
    private static final Map<String, MapMarkerIdentity> BY_DECORATION_TYPE_ID =
        Arrays.stream(values()).collect(Collectors.toUnmodifiableMap(
            MapMarkerIdentity::decorationTypeId,
            Function.identity()
        ));

    private final String id;
    private final String itemNameTranslationKey;
    private final String sourceDecorationTypeId;
    private final String sourceMarkerAssetId;
    private final String decorationTypeId;
    private final String markerAssetId;
    private final String filledMapItemAssetId;
    private final boolean customDecoration;

    MapMarkerIdentity(
        String id,
        String itemNameTranslationKey,
        String sourceDecorationTypeId,
        String sourceMarkerAssetId
    ) {
        this(id, itemNameTranslationKey, sourceDecorationTypeId, sourceMarkerAssetId, true);
    }

    MapMarkerIdentity(
        String id,
        String itemNameTranslationKey,
        String sourceDecorationTypeId,
        String sourceMarkerAssetId,
        boolean customDecoration
    ) {
        this.id = id;
        this.itemNameTranslationKey = itemNameTranslationKey;
        this.sourceDecorationTypeId = sourceDecorationTypeId;
        this.sourceMarkerAssetId = sourceMarkerAssetId;
        this.customDecoration = customDecoration;
        this.decorationTypeId = customDecoration
            ? RESOURCE_NAMESPACE + ":" + id
            : sourceDecorationTypeId;
        this.markerAssetId = customDecoration
            ? RESOURCE_NAMESPACE + ":poi_icons/" + id
            : sourceMarkerAssetId;
        this.filledMapItemAssetId = RESOURCE_NAMESPACE + ":map_sprites/" + id;
    }

    public String id() {
        return id;
    }

    public String itemNameTranslationKey() {
        return itemNameTranslationKey;
    }

    public String sourceDecorationTypeId() {
        return sourceDecorationTypeId;
    }

    public String sourceMarkerAssetId() {
        return sourceMarkerAssetId;
    }

    public String decorationTypeId() {
        return decorationTypeId;
    }

    public String markerAssetId() {
        return markerAssetId;
    }

    public String filledMapItemAssetId() {
        return filledMapItemAssetId;
    }

    public boolean customDecoration() {
        return customDecoration;
    }

    public boolean matchesSourceOrResultType(String candidateTypeId) {
        return sourceDecorationTypeId.equals(candidateTypeId)
            || decorationTypeId.equals(candidateTypeId);
    }

    public static Optional<MapMarkerIdentity> fromItemNameTranslationKey(String translationKey) {
        return Optional.ofNullable(BY_ITEM_NAME_TRANSLATION_KEY.get(translationKey));
    }

    public static Optional<MapMarkerIdentity> fromDecorationTypeId(String decorationTypeId) {
        return Optional.ofNullable(BY_DECORATION_TYPE_ID.get(decorationTypeId));
    }
}
