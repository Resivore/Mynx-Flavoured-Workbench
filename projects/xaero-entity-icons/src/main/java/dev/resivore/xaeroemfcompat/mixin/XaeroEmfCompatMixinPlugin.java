package dev.resivore.xaeroemfcompat.mixin;

import dev.resivore.xaeroemfcompat.CompatibilityActivation;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class XaeroEmfCompatMixinPlugin implements IMixinConfigPlugin {
    private static final String XAERO_MOD_ID = "xaerominimap";
    private static final String EMF_MOD_ID = "entity_model_features";
    private static final String EMF_TARGET = "traben.entity_model_features.models.parts.EMFModelPart";
    private static final String XAERO_PRERENDER_TARGET =
            "xaero.hud.minimap.radar.icon.creator.render.form.model.RadarIconModelPrerenderer";
    private static final String XAERO_MODEL_FORM_PRERENDER_TARGET =
            "xaero.hud.minimap.radar.icon.creator.render.form.model.RadarIconModelFormPrerenderer";
    private static final String XAERO_PART_PRERENDER_TARGET =
            "xaero.hud.minimap.radar.icon.creator.render.form.model.part."
                    + "RadarIconModelPartPrerenderer";

    @Override
    public void onLoad(String mixinPackage) {
        dev.resivore.xaeroemfcompat.IconDiagnostics.activation("plugin loaded");
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        FabricLoader loader = FabricLoader.getInstance();
        Optional<String> xaeroVersion = loader.getModContainer(XAERO_MOD_ID).map(XaeroEmfCompatMixinPlugin::version);
        Optional<String> emfVersion = loader.getModContainer(EMF_MOD_ID).map(XaeroEmfCompatMixinPlugin::version);
        boolean active = CompatibilityActivation.shouldApply(xaeroVersion, emfVersion);
        dev.resivore.xaeroemfcompat.IconDiagnostics.event("DEPENDENCY_DECISION", "xaero=" + xaeroVersion + " emf=" + emfVersion + " active=" + active);
        if (!active) {
            return false;
        }

        if (!Set.of(
                EMF_TARGET,
                XAERO_PRERENDER_TARGET,
                XAERO_MODEL_FORM_PRERENDER_TARGET,
                XAERO_PART_PRERENDER_TARGET,
                "xaero.hud.minimap.radar.icon.cache.RadarIconCache",
                "xaero.hud.minimap.radar.icon.creator.RadarIconCreator",
                "xaero.hud.minimap.radar.icon.RadarIconManager",
                "xaero.hud.minimap.radar.icon.cache.RadarIconEntityCache"
        ).contains(targetClassName)) {
            throw new IllegalStateException("Unexpected compatibility mixin target: " + targetClassName);
        }
        return true;
    }

    private static String version(ModContainer container) {
        return container.getMetadata().getVersion().getFriendlyString();
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
            IMixinInfo mixinInfo
    ) {
    }

    @Override
    public void postApply(
            String targetClassName,
            ClassNode targetClass,
            String mixinClassName,
            IMixinInfo mixinInfo
    ) {
    }
}
