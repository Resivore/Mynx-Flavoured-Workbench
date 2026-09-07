package games.twinhead.moreslabsstairsandwalls.api.material;

/** Client tint semantics are independent from material behavior capabilities. */
public enum TintProfile {
    NONE,
    GRASS_BIOME,
    FOLIAGE_BIOME,
    FOLIAGE_SPRUCE,
    FOLIAGE_BIRCH,
    /** Delegates each derived block to its optional provider's own block-color contract. */
    SOURCE_PROVIDER
}
