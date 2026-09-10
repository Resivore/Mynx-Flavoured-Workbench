package dev.resivore.naturalistxaeroicons;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xaero.common.icon.XaeroIcon;
import xaero.hud.minimap.radar.icon.creator.render.trace.ModelRenderTrace;

/** Records one normal cache-miss sequence for each C23 Scorpion without changing Xaero's result. */
public final class ScorpionCaptureDiagnostic {
    private static final Logger LOGGER = LoggerFactory.getLogger("NaturalistXaero ScorpionCapture");
    private static final String DESERT_SCORPION = "naturalist:desert_scorpion";
    private static final String JUNGLE_SCORPION = "naturalist:jungle_scorpion";
    private static final Set<String> REPORTED = ConcurrentHashMap.newKeySet();
    private static final ThreadLocal<Request> REQUEST = new ThreadLocal<>();

    private ScorpionCaptureDiagnostic() {}

    public static void requestStarted(Entity entity, boolean canPrerender) {
        String id = id(entity);
        if (id == null) return;
        REQUEST.set(new Request(id));
        report(id, "request", "request " + id + "; canPrerender=" + canPrerender);
    }

    public static void cacheLookup(boolean hit) {
        Request request = REQUEST.get();
        if (request == null) return;
        request.cacheHit = hit;
        report(request.id, "cache-" + hit, "RadarIconEntityCache#get initial " + (hit ? "HIT" : "MISS"));
    }

    public static void creatorStarted(Entity entity, Object form, Identifier texture) {
        Request request = REQUEST.get();
        if (request == null || !request.id.equals(id(entity)) || request.cacheHit) return;
        request.creatorEntered = true;
        report(request.id, "creator", "post-MISS RadarIconCreator#create form=" + className(form)
                + "; rendererTexture=" + String.valueOf(texture));
    }

    public static void modelFormStarted(Entity entity, int traces, String textures) {
        Request request = REQUEST.get();
        if (request == null || !request.id.equals(id(entity)) || request.cacheHit) return;
        request.modelFormEntered = true;
        report(request.id, "model-form", "post-MISS RadarIconModelFormPrerenderer#prerender traces=" + traces
                + "; traceTextures=" + textures);
    }

    public static void modelPartPath(String method) {
        Request request = REQUEST.get();
        if (request == null || request.cacheHit) return;
        request.modelPartPath = true;
        report(request.id, "model-part-" + method, "post-MISS " + method + " reached");
    }

    public static void nativePathObserved(Model model, int renderedParts) {
        Request request = REQUEST.get();
        if (request == null) return;
        request.nativeRenderedParts = renderedParts;
        report(request.id, "native-" + renderedParts, "native model path returned renderedParts=" + renderedParts
                + "; exactModelClass=" + className(model));
    }

    public static void fallbackSkipped(String reason) {
        Request request = REQUEST.get();
        if (request == null) return;
        request.skipped = reason;
        report(request.id, "skipped-" + reason, "fallback skipped: " + reason);
    }

    public static void contractResolved(NaturalistModelContracts.Contract contract) {
        Request request = REQUEST.get();
        if (request == null) return;
        request.contractResolved = true;
        var p = contract.presentation();
        report(request.id, "contract", "resolved contract source=" + path(contract.path())
                + "; trace=" + path(contract.tracePath()) + "; selectedGeometry="
                + String.join(",", contract.drawableChildren()) + "; scale=" + p.scale()
                + "; rotations=" + p.xRotation() + "," + p.yRotation() + "," + p.zRotation()
                + "; frameYOffset=" + p.frameYOffset());
    }

    public static void renderCenter(NaturalistModelContracts.ResolvedContract contract, ModelPart selected,
                                    ModelPart center, ModelRenderTrace mrt) {
        Request request = REQUEST.get();
        if (request == null) return;
        request.selectedSynthetic = selected != contract.source();
        request.centerIsTrace = center == contract.trace();
        request.centerHasDirectMrt = mrt.getModelPartRenderInfo(center) != null;
        report(request.id, "center-" + request.selectedSynthetic + "-" + request.centerIsTrace + "-" + request.centerHasDirectMrt,
                "selectedGeometry=" + (request.selectedSynthetic ? "synthetic copied assembly" : "live")
                        + "; renderCenter=" + (request.centerIsTrace ? "live body trace" : "selected assembly")
                        + "; renderCenterIsTrace=" + request.centerIsTrace
                        + "; renderCenterHasDirectMrt=" + request.centerHasDirectMrt);
    }

    public static void adapterBuilt(boolean traceExists) {
        Request request = REQUEST.get();
        if (request == null) return;
        request.adapterBuilt = true;
        request.traceBound = traceExists;
        report(request.id, "adapter-" + traceExists, "adapter built; explicit trace resolution succeeds=" + traceExists);
    }

    public static void fallbackRendered(int before, int after, ModelPart adapter, ModelPart selected, ModelPart center,
                                        List<ModelPart> renderedParts) {
        Request request = REQUEST.get();
        if (request == null) return;
        request.fallbackRenderedParts = after;
        report(request.id, "render-" + before + "-" + after, "render destination before=" + before + "; after=" + after
                + "; adapterRecorded=" + renderedParts.contains(adapter) + "; selectedAssemblyRecorded="
                + renderedParts.contains(selected) + "; renderCenterRecorded=" + renderedParts.contains(center));
    }

    public static void failed(RuntimeException error) {
        Request request = REQUEST.get();
        if (request == null) return;
        request.failure = error.getClass().getName();
        report(request.id, "failure-" + request.failure, "fallback threw " + request.failure
                + "; message=" + String.valueOf(error.getMessage()));
    }

    public static void creatorFinished(Entity entity, XaeroIcon icon) {
        Request request = REQUEST.get();
        if (request == null || !request.id.equals(id(entity)) || request.cacheHit) return;
        request.creatorProduced = icon != null;
        report(request.id, "creator-result-" + (icon != null), "RadarIconCreator#create returned non-null=" + (icon != null));
    }

    public static void cacheWritten(EntityType<?> type, XaeroIcon icon) {
        Request request = REQUEST.get();
        if (request == null || type == null || !request.id.equals(EntityType.getKey(type).toString()) || request.cacheHit) return;
        request.cacheWritten = true;
        report(request.id, "cache-write-" + (icon != null), "cache write post-MISS non-null=" + (icon != null));
    }

    public static void requestFinished(Entity entity, XaeroIcon icon) {
        Request request = REQUEST.get();
        if (request == null || !request.id.equals(id(entity))) return;
        report(request.id, "final", "manager result non-null=" + (icon != null) + "; cacheHit=" + request.cacheHit
                + "; creator=" + request.creatorEntered + "; modelForm=" + request.modelFormEntered
                + "; modelPart=" + request.modelPartPath + "; nativeRenderedParts=" + request.nativeRenderedParts
                + "; contractResolved=" + request.contractResolved + "; adapterBuilt=" + request.adapterBuilt
                + "; traceResolution=" + request.traceBound + "; selectedSynthetic=" + request.selectedSynthetic
                + "; renderCenterIsTrace=" + request.centerIsTrace + "; renderCenterHasDirectMrt=" + request.centerHasDirectMrt
                + "; fallbackRenderedParts=" + request.fallbackRenderedParts + "; creatorProduced=" + request.creatorProduced
                + "; cacheWritten=" + request.cacheWritten + "; skipped=" + request.skipped + "; failure=" + request.failure);
        REQUEST.remove();
    }

    public static void resourceReloaded() { REQUEST.remove(); REPORTED.clear(); }
    public static boolean isScorpion(Entity entity) { return id(entity) != null; }
    private static String id(Entity entity) {
        if (entity == null) return null;
        Identifier id = EntityType.getKey(entity.getType());
        if (id == null || !"naturalist".equals(id.getNamespace())) return null;
        return switch (id.toString()) {
            case DESERT_SCORPION, JUNGLE_SCORPION -> id.toString();
            default -> null;
        };
    }
    private static String path(List<String> segments) { return segments.isEmpty() ? "<model root>" : String.join("/", segments); }
    private static String className(Object value) { return value == null ? "null" : value.getClass().getName(); }
    private static void report(String id, String outcome, String message) { if (REPORTED.add(id + ":" + outcome)) LOGGER.info("{} {}", id, message); }

    private static final class Request {
        private final String id;
        private boolean cacheHit, creatorEntered, modelFormEntered, modelPartPath, creatorProduced, cacheWritten, contractResolved, adapterBuilt, traceBound, selectedSynthetic, centerIsTrace, centerHasDirectMrt;
        private int nativeRenderedParts, fallbackRenderedParts;
        private String skipped, failure;
        private Request(String id) { this.id = id; }
    }
}
