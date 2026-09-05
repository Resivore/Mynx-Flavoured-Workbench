package fuzs.iteminteractions.common.impl;
import net.minecraft.resources.Identifier;
import fuzs.puzzleslib.common.api.config.v3.ConfigHolder;
/** Fixture-only config/service bootstrap. Does not replace a tooltip or storage implementation. */
public final class ItemInteractions {
    public static final fuzs.iteminteractions.common.impl.config.ClientConfig CLIENT = new fuzs.iteminteractions.common.impl.config.ClientConfig();
    public static final ConfigHolder CONFIG = type -> CLIENT;
    public static Identifier id(String path) { return Identifier.fromNamespaceAndPath("iteminteractions", path); }
}
