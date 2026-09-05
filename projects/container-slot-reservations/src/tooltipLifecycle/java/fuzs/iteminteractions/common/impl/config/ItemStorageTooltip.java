package fuzs.iteminteractions.common.impl.config;
/** Fixture-only input/config boundary; exercises the original collapsible wrapper's branches. */
public enum ItemStorageTooltip implements fuzs.iteminteractions.common.impl.client.core.KeyType { ALWAYS, NEVER;
    public boolean isUsed() { return this == ALWAYS; }
    public net.minecraft.network.chat.Component getDisplayName() { return null; }
    public net.minecraft.network.chat.Component getNameComponent() { return null; }
    public net.minecraft.network.chat.Component getComponent(String key) { return null; }
}
