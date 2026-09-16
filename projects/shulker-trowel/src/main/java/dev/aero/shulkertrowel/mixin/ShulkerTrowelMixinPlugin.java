package dev.aero.shulkertrowel.mixin;

import dev.aero.shulkertrowel.compat.QuickRightClickCompatibility;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/** Applies the optional QRC seam only to the exact independently audited binary. */
public final class ShulkerTrowelMixinPlugin implements IMixinConfigPlugin {
    private static final String QRC_SHULKER_MIXIN =
            "dev.aero.shulkertrowel.mixin.QuickRightClickShulkerCompatibilityMixin";

    @Override
    public void onLoad(String mixinPackage) {}

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!QRC_SHULKER_MIXIN.equals(mixinClassName)) return true;
        return FabricLoader.getInstance().getModContainer(QuickRightClickCompatibility.MOD_ID)
                .map(container -> QuickRightClickCompatibility.supports(
                        container.getMetadata().getId(),
                        container.getMetadata().getVersion().getFriendlyString()))
                .orElse(false);
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass,
            String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public void postApply(String targetClassName, ClassNode targetClass,
            String mixinClassName, IMixinInfo mixinInfo) {}
}
