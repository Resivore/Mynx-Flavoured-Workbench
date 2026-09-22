package dev.resivore.enderscapeintegration.client;

import java.util.List;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/** Gates the version-coupled Iris callback without making Iris a normal runtime dependency. */
public final class IrisEnderscapeIntegrationMixinPlugin implements IMixinConfigPlugin {
    static final String IRIS_VERSION = "1.11.2+mc26.2";
    static final String IRIS_MIXIN_CLASS = "dev.resivore.enderscapeintegration.mixin."
            + "IrisVeiledLeavesMaterialMappingMixin";

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!IRIS_MIXIN_CLASS.equals(mixinClassName)) {
            return true;
        }
        return activationAllowed(mixinClassName, hasExactVersion("iris", IRIS_VERSION));
    }

    static boolean activationAllowed(String mixinClassName, boolean exactIrisHook) {
        return !IRIS_MIXIN_CLASS.equals(mixinClassName) || exactIrisHook;
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
