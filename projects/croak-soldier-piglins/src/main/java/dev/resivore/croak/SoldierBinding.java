package dev.resivore.croak;

import com.geckolib.cache.GeckoLibResources;
import com.mojang.logging.LogUtils;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.server.packs.resources.ResourceManager;
import java.util.List;

/** Owned by the registered piglin renderer for the lifetime of one resource reload. */
public final class SoldierBinding {
    public interface Owner {
        SoldierBinding croak$binding();
    }

    private final EntityRendererProvider.Context context;
    private SoldierRenderer visual;
    private boolean attempted;
    private com.geckolib.cache.BakedModelCache checkedModels;
    private boolean reportedState;
    private boolean reportedSubmit;
    private boolean reportedFallback;

    public SoldierBinding(EntityRendererProvider.Context context) { this.context = context; }

    public SoldierRenderer visual() {
        // A first lookup during reload can precede GeckoLib's cache swap.
        var models = GeckoLibResources.getBakedModels();
        if (attempted && checkedModels == models) return visual;
        checkedModels = models;
        visual = null;
        attempted = true;
        try {
            if (!resourcesReady(context.getResourceManager())) return null;
            visual = new SoldierRenderer(context);
            LogUtils.getLogger().info("[Croak Soldier] Renderer initialized for adult piglin");
        } catch (RuntimeException | LinkageError e) {
            LogUtils.getLogger().error("[Croak Soldier] Renderer initialization failed", e);
        }
        return visual;
    }

    public static boolean resourcesReady(ResourceManager resources) {
        for (var id : List.of(SoldierPack.MODEL, SoldierPack.ANIMATION, SoldierPack.TEXTURE)) {
            if (resources.getResource(id).isEmpty()) {
                LogUtils.getLogger().warn("[Croak Soldier] Missing client resource {}", id);
                return false;
            }
        }
        var id = SoldierPack.id("soldier");
        var model = GeckoLibResources.getBakedModels().cache().get(id);
        var clips = GeckoLibResources.getBakedAnimations().cache().get(id);
        if (model == null || model.isMissingno() || clips == null ||
                !clips.animations().keySet().containsAll(List.of("idle", "walk", "agro", "attack", "death"))) {
            LogUtils.getLogger().warn("[Croak Soldier] GeckoLib bake missing model or required clips for {}", id);
            return false;
        }
        return true;
    }

    public void extracted() {
        if (!reportedState) {
            reportedState = true;
            LogUtils.getLogger().info("[Croak Soldier] Adult piglin dispatched to Soldier state");
        }
    }

    public void submitting() {
        if (!reportedSubmit) {
            reportedSubmit = true;
            LogUtils.getLogger().info("[Croak Soldier] Submitting first Soldier state");
        }
    }

    public void vanillaState() {
        if (!reportedFallback) {
            reportedFallback = true;
            LogUtils.getLogger().warn("[Croak Soldier] Adult reached submission with vanilla state; renderer active={}", visual != null);
        }
    }
}
