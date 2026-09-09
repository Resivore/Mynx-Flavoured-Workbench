package dev.resivore.wearablelanterns;

import java.util.List;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * Keeps the private Iris hook fail-closed: its target is audited only for Iris 1.11.2 on
 * Minecraft 26.2, and Wearable Lanterns has no Iris linkage when that exact optional version is
 * absent.
 */
public final class WearableLanternsIrisMixinPlugin implements IMixinConfigPlugin {
    static final String IRIS_ID = "iris";
    static final String AUDITED_IRIS_VERSION = "1.11.2+mc26.2";

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return FabricLoader.getInstance().getModContainer(IRIS_ID)
                .map(container -> AUDITED_IRIS_VERSION.equals(
                        container.getMetadata().getVersion().getFriendlyString()))
                .orElse(false);
    }

    @Override public void onLoad(String mixinPackage) {}
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
