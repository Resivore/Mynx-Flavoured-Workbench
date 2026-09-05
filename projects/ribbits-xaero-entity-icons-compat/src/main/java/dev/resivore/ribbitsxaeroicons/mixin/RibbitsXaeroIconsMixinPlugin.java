package dev.resivore.ribbitsxaeroicons.mixin;

import dev.resivore.ribbitsxaeroicons.CompatibilityActivation;
import dev.resivore.ribbitsxaeroicons.GeoIconLog;
import dev.resivore.ribbitsxaeroicons.RuntimeCompatibility;
import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/** Applies no target mixin unless all audited archive identities match exactly. */
public final class RibbitsXaeroIconsMixinPlugin implements IMixinConfigPlugin {
    private static final Set<String> EXACT_TARGETS = Set.of(
            "xaero.hud.minimap.radar.icon.cache.RadarIconCache",
            "xaero.hud.minimap.radar.icon.cache.RadarIconEntityCache",
            "xaero.hud.minimap.radar.icon.creator.RadarIconCreator",
            "xaero.hud.minimap.radar.icon.RadarIconManager",
            "xaero.hud.minimap.radar.icon.cache.id.variant.RadarIconVariantHandler");
    private boolean initialized;
    private boolean active;

    @Override
    public void onLoad(String mixinPackage) {
        GeoIconLog.activation("Ribbits Xaero mixin plugin loaded");
        CompatibilityActivation.Decision decision = RuntimeCompatibility.evaluateLoadedMods();
        active = decision.active();
        initialized = true;
        GeoIconLog.activation((active ? "Activated" : "Safely declined")
                + " Ribbits Xaero entity icons: " + decision.reason());
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!EXACT_TARGETS.contains(targetClassName)) {
            throw new IllegalStateException(
                    "Unexpected Ribbits Xaero compatibility target: " + targetClassName);
        }
        return initialized && active;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(
            String targetClassName,
            ClassNode targetClass,
            String mixinClassName,
            IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(
            String targetClassName,
            ClassNode targetClass,
            String mixinClassName,
            IMixinInfo mixinInfo) {
    }
}
