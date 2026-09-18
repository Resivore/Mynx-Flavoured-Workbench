package dev.resivore.slotreservations.mixin.client;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/** Applies narrow client compatibility guards only when their exact provider is installed. */
public final class OptionalClientMixinPlugin implements IMixinConfigPlugin {
    private static final String MOUSE_TWEAKS_GUARD =
            "dev.resivore.slotreservations.mixin.client.MouseTweaksInboundGuardMixin";

    @Override public void onLoad(String mixinPackage) {}
    @Override public String getRefMapperConfig() { return null; }
    @Override public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return !MOUSE_TWEAKS_GUARD.equals(mixinClassName)
                || FabricLoader.getInstance().isModLoaded("mousetweaks");
    }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetClassName, ClassNode targetClass,
                                   String mixinClassName, IMixinInfo mixinInfo) {}
    @Override public void postApply(String targetClassName, ClassNode targetClass,
                                    String mixinClassName, IMixinInfo mixinInfo) {}
}
