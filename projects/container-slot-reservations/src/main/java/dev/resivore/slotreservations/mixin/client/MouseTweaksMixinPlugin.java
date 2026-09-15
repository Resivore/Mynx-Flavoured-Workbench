package dev.resivore.slotreservations.mixin.client;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/** Keeps the Mouse Tweaks API mixin completely absent when its provider is not installed. */
public final class MouseTweaksMixinPlugin implements IMixinConfigPlugin {
    private static final String OPTIONAL_MIXIN =
            "dev.resivore.slotreservations.mixin.client.MouseTweaksContainerScreenMixin";

    @Override public void onLoad(String mixinPackage) {}
    @Override public String getRefMapperConfig() { return null; }
    @Override public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return !OPTIONAL_MIXIN.equals(mixinClassName)
                || FabricLoader.getInstance().isModLoaded("mousetweaks");
    }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetClassName, ClassNode targetClass,
                                   String mixinClassName, IMixinInfo mixinInfo) {}
    @Override public void postApply(String targetClassName, ClassNode targetClass,
                                    String mixinClassName, IMixinInfo mixinInfo) {}
}
