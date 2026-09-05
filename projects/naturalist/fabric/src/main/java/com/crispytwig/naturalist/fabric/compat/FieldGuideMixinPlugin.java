package com.crispytwig.naturalist.fabric.compat;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/** Filters optional client targets before Mixin resolves their classes. */
public final class FieldGuideMixinPlugin implements IMixinConfigPlugin {
    private final boolean enabled;

    public FieldGuideMixinPlugin() {
        this(FabricLoader.getInstance());
    }

    FieldGuideMixinPlugin(FabricLoader loader) {
        enabled = loader.getEnvironmentType() == EnvType.CLIENT && loader.isModLoaded("fieldguide");
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return enabled;
    }

    @Override
    public void onLoad(String mixinPackage) {}

    @Override
    public String getRefMapperConfig() { return null; }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() { return null; }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass,
                         String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public void postApply(String targetClassName, ClassNode targetClass,
                          String mixinClassName, IMixinInfo mixinInfo) {}
}
