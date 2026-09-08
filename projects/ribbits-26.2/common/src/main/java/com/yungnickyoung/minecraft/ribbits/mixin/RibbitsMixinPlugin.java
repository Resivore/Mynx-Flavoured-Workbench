package com.yungnickyoung.minecraft.ribbits.mixin;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/** Keeps the optional Naturalist visual patch out of Ribbits' required linkage graph. */
public final class RibbitsMixinPlugin implements IMixinConfigPlugin {
    private static final String NATURALIST_SNAIL_MIXIN =
            "com.yungnickyoung.minecraft.ribbits.mixin.mixins.client.compat.NaturalistSnailLeashMixin";
    private static final String NATURALIST_SNAIL =
            "com.crispytwig.naturalist.server.entity.mob.Snail";

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!NATURALIST_SNAIL_MIXIN.equals(mixinClassName)) return true;
        try {
            Class.forName(NATURALIST_SNAIL, false, RibbitsMixinPlugin.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    @Override public void onLoad(String mixinPackage) { }
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) { }
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) { }
    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) { }
}
