package dev.resivore.naturalistxaeroicons;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xaero.common.icon.XaeroIcon;
import xaero.hud.minimap.radar.icon.creator.render.form.model.part.ModelPartUtil;
import xaero.hud.minimap.radar.icon.creator.render.trace.ModelRenderTrace;

/** Bounded C30 evidence for the adult Whale's detached head-plus-body capture only. */
public final class WhaleCaptureDiagnostic {
    private static final Logger LOGGER = LoggerFactory.getLogger("NaturalistXaero WhaleCapture");
    private static final Set<String> REPORTED = ConcurrentHashMap.newKeySet();
    private static final ThreadLocal<Request> REQUEST = new ThreadLocal<>();
    private WhaleCaptureDiagnostic() {}

    public static boolean isAdultWhale(Entity entity) {
        return entity instanceof AgeableMob mob && !mob.isBaby()
                && "naturalist:whale".equals(String.valueOf(EntityType.getKey(entity.getType())));
    }
    public static void requestStarted(Entity entity, boolean canPrerender) {
        if (!isAdultWhale(entity)) return;
        REQUEST.set(new Request()); report("request", "adult Whale request; canPrerender=" + canPrerender);
    }
    public static void cacheLookup(boolean hit) { if (active()) { REQUEST.get().hit = hit; report("cache-" + hit, "adult Whale cache " + (hit ? "HIT" : "MISS")); } }
    public static void creatorStarted(Entity entity, Object form, Identifier texture) {
        if (miss(entity)) report("creator", "MISS creator form=" + type(form) + "; texture=" + texture);
    }
    public static void modelFormStarted(Entity entity, int traces, String textures) {
        if (miss(entity)) report("model-form", "MISS model form traces=" + traces + "; textures=" + textures);
    }
    public static void modelPartPath(String method) { if (active() && !REQUEST.get().hit) report("part-" + method, "MISS reached " + method); }
    public static void nativePathObserved(Model model, int destination) {
        if (active()) report("native", "model=" + type(model) + "; destination before fallback=" + destination);
    }
    public static void fallbackSkipped(String reason) { if (active()) report("skip-" + reason, "fallback skipped=" + reason); }
    public static void contractResolved(NaturalistModelContracts.Contract contract, ModelPart source, ModelPart selected) {
        if (!active()) return;
        var p = contract.presentation();
        report("contract", "source=" + path(contract.path()) + "; trace=" + path(contract.tracePath())
                + "; selected=detached body direct cube + children " + contract.drawableChildren()
                + "; omitted=tail,tail2,fluke,bone,rightFin,leftFin; sourceCubes=" + cubes(source)
                + "; selectedCubes=" + cubes(selected) + "; selectedChildren=" + children(selected)
                + "; scale=" + p.scale() + "; rotations=" + p.xRotation() + "," + p.yRotation() + "," + p.zRotation()
                + "; frameOffset=" + p.frameYOffset());
    }
    public static void adapterBuilt(boolean trace) { if (active()) report("adapter", "adapter trace success=" + trace); }
    public static void renderCenter(NaturalistModelContracts.ResolvedContract resolved, ModelPart selected, ModelPart center, ModelRenderTrace mrt) {
        if (!active()) return;
        report("center", "render center=live body trace; synthetic=" + (selected != resolved.source())
                + "; directCubes=" + cubes(center) + "; liveMrt=" + (mrt.getModelPartRenderInfo(center) != null)
                + "; centerChildren=" + children(center));
    }
    public static void fallbackRendered(int before, int after, ModelPart adapter, ModelPart selected, ModelPart center, List<ModelPart> destination) {
        if (active()) report("render", "destination before=" + before + "; after=" + after + "; adapterRecorded="
                + destination.contains(adapter) + "; selectedRecorded=" + destination.contains(selected)
                + "; centerRecorded=" + destination.contains(center));
    }
    public static void failed(RuntimeException error) { if (active()) report("error", "fallback exception=" + error.getClass().getName()); }
    public static void creatorFinished(Entity entity, XaeroIcon icon) { if (miss(entity)) report("creator-result", "MISS creator nonNull=" + (icon != null)); }
    public static void cacheWritten(EntityType<?> type, XaeroIcon icon) {
        if (active() && type != null && "naturalist:whale".equals(String.valueOf(EntityType.getKey(type)))) report("write", "MISS cache write nonNull=" + (icon != null));
    }
    public static void requestFinished(Entity entity, XaeroIcon icon) {
        if (!isAdultWhale(entity) || !active()) return;
        report("final", "adult Whale final nonNull=" + (icon != null) + "; cacheHit=" + REQUEST.get().hit); REQUEST.remove();
    }
    public static void resourceReloaded() { REQUEST.remove(); REPORTED.clear(); }
    private static boolean active() { return REQUEST.get() != null; }
    private static boolean miss(Entity entity) { return active() && isAdultWhale(entity) && !REQUEST.get().hit; }
    private static int cubes(ModelPart part) { var values = part == null ? null : ModelPartUtil.getCubes(part); return values == null ? -1 : values.size(); }
    private static String children(ModelPart part) { Map<String, ModelPart> values = part == null ? null : ModelPartUtil.getChildren(part); return values == null ? "<unavailable>" : values.keySet().stream().sorted().toList().toString(); }
    private static String path(List<String> values) { return values.isEmpty() ? "<model root>" : String.join("/", values); }
    private static String type(Object value) { return value == null ? "null" : value.getClass().getName(); }
    private static void report(String key, String message) { if (REPORTED.add(key)) LOGGER.info("{}", message); }
    private static final class Request { private boolean hit; }
}
