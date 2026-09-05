package fuzs.iteminteractions.common.impl.config;
/** Fixture-only config values. Rendering, geometry, and highlights use the exact upstream classes. */
public final class ClientConfig extends fuzs.puzzleslib.common.api.config.v3.ConfigCore {
    public boolean itemStorageColors = false;
    public SlotHighlight itemStorageHighlightSprite = SlotHighlight.HIGHLIGHT;
    public ItemStorageTooltip itemStorageTooltip = ItemStorageTooltip.ALWAYS;
}
