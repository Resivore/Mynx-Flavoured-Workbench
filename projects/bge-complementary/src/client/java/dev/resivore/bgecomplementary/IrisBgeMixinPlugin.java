package dev.resivore.bgecomplementary;

import java.util.List;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * Mixin-bootstrap gate for the version-coupled Iris descriptors only. BGE capability inspection
 * deliberately happens from the completed-map RETURN hooks, after Mixin preparation.
 */
public final class IrisBgeMixinPlugin implements IMixinConfigPlugin {
    static final String IRIS_VERSION = "1.11.2+mc26.2";

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return activationAllowed(hasExactVersion("iris", IRIS_VERSION));
    }

    static boolean activationAllowed(boolean exactIrisHook) {
        return exactIrisHook;
    }

    private static boolean hasExactVersion(String modId, String expected) {
        return FabricLoader.getInstance().getModContainer(modId)
                .map(container -> expected.equals(container.getMetadata().getVersion().getFriendlyString()))
                .orElse(false);
    }

    @Override public void onLoad(String mixinPackage) {}
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo info) {}
    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo info) {}
}
