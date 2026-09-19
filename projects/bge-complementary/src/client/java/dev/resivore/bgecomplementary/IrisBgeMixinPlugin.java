package dev.resivore.bgecomplementary;

import java.util.List;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/** Version-coupled Iris mappings fail closed before Mixin resolves any Iris target. */
public final class IrisBgeMixinPlugin implements IMixinConfigPlugin {
    static final String IRIS_VERSION = "1.11.2+mc26.2";
    static final String BGE_VERSION = "4.2.23-bge.canary79.cnm-family-bridge+26.2";

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return hasExactVersion("iris", IRIS_VERSION)
                && hasExactVersion("cnm_terrain_slabs_compat", BGE_VERSION);
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
