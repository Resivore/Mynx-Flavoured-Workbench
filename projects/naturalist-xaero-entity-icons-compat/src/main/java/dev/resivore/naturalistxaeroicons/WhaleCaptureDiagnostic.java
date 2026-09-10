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

/** C27's one-time, adult-Whale-only capture-path evidence. It never changes Xaero's outcome. */
public final class WhaleCaptureDiagnostic {
    private static final Logger LOGGER = LoggerFactory.getLogger("NaturalistXaero WhaleCapture");
    private static final Set<String> REPORTED = ConcurrentHashMap.newKeySet();
    private static final ThreadLocal<Request> REQUEST = new ThreadLocal<>();

    private WhaleCaptureDiagnostic() {}

    public static void requestStarted(Entity entity, boolean canPrerender) {
        if (!isAdultWhale(entity)) return;
        REQUEST.set(new Request());
        report("request", "request naturalist:whale adult; canPrerender=" + canPrerender);
    }

    public static void cacheLookup(boolean hit) {
        Request request = REQUEST.get();
        if (request == null) return;
        request.cacheHit = hit;
        report("cache-" + hit, "RadarIconEntityCache#get initial " + (hit ? "HIT" : "MISS"));
    }

    public static void creatorStarted(Entity entity, Object form, Identifier texture) {
        Request request = REQUEST.get();
        if (request == null || !isAdultWhale(entity) || request.cacheHit) return;
        request.creatorEntered = true;
        report("creator", "post-MISS RadarIconCreator#create form=" + className(form) + "; rendererTexture=" + texture);
    }

    public static void modelFormStarted(Entity entity, int traces, String textures) {
        Request request = REQUEST.get();
        if (request == null || !isAdultWhale(entity) || request.cacheHit) return;
        request.modelFormEntered = true;
        report("model-form", "post-MISS RadarIconModelFormPrerenderer#prerender traces=" + traces + "; traceTextures=" + textures);
    }

    public static void modelPartPath(String method) {
        Request request = REQUEST.get();
        if (request == null || request.cacheHit) return;
        request.modelPartEntered = true;
        report("model-part-" + method, "post-MISS " + method + " reached");
    }

    public static void nativePathObserved(Model model, int renderedParts) {
        Request request = REQUEST.get();
        if (request == null) return;
        request.nativeRenderedParts = renderedParts;
        report("native-" + renderedParts, "native model path exactModelClass=" + className(model)
                + "; rendered destination count=" + renderedParts);
    }

    public static void fallbackSkipped(String reason) {
        Request request = REQUEST.get();
        if (request == null) return;
        request.skipped = reason;
        report("skipped-" + reason, "fallback skipped: " + reason);
    }

    public static void contractResolved(NaturalistModelContracts.Contract contract, ModelPart source, ModelPart selected) {
        Request request = REQUEST.get();
        if (request == null) return;
        request.contractResolved = true;
        var p = contract.presentation();
        report("contract", "contract source=" + path(contract.path()) + "; trace=" + path(contract.tracePath())
                + "; selected geometry=" + selectedGeometry(contract, selected) + "; source direct-cube count=" + cubeCount(source)
                + "; selected direct-cube count=" + cubeCount(selected) + "; selected child names=" + childNames(selected)
                + "; scale=" + p.scale() + "; rotations=" + p.xRotation() + "," + p.yRotation() + "," + p.zRotation()
                + "; frame offset=" + p.frameYOffset());
    }

    public static void renderCenter(NaturalistModelContracts.ResolvedContract contract, ModelPart selected,
                                    ModelPart center, ModelRenderTrace mrt) {
        Request request = REQUEST.get();
        if (request == null) return;
        request.selectedSynthetic = selected != contract.source();
        request.centerIsLiveNaturalist = center == contract.trace() || center == contract.source();
        request.centerHasDirectCubes = cubeCount(center) > 0;
        request.centerHasDirectTrace = mrt.getModelPartRenderInfo(center) != null;
        request.centerIsTrace = center == contract.trace();
        report("center", "render center identity=" + (request.centerIsTrace ? "live Whale topJaw trace" : "selected assembly")
                + "; live Naturalist ModelPart=" + request.centerIsLiveNaturalist + "; direct cubes=" + request.centerHasDirectCubes
                + "; direct ModelRenderTrace entry=" + request.centerHasDirectTrace + "; center child names=" + childNames(center));
    }

    public static void adapterBuilt(boolean traceExists) {
        Request request = REQUEST.get();
        if (request == null) return;
        request.adapterBuilt = true;
        request.adapterTraceResolved = traceExists;
        report("adapter-" + traceExists, "adapter trace resolution succeeds=" + traceExists);
    }

    public static void fallbackRendered(int before, int after, ModelPart adapter, ModelPart selected, ModelPart center,
                                        List<ModelPart> renderedParts) {
        Request request = REQUEST.get();
        if (request == null) return;
        request.fallbackRenderedParts = after;
        request.adapterRecorded = renderedParts.contains(adapter);
        request.selectedRecorded = renderedParts.contains(selected);
        request.centerRecorded = renderedParts.contains(center);
        report("render-" + before + "-" + after, "fallback rendered destination before=" + before + "; after=" + after
                + "; adapter recording result=" + request.adapterRecorded + "; selected recording result=" + request.selectedRecorded
                + "; render-center recording result=" + request.centerRecorded);
    }

    public static void failed(RuntimeException error) {
        Request request = REQUEST.get();
        if (request == null) return;
        request.failure = error.getClass().getName();
        report("failure-" + request.failure, "fallback exception=" + request.failure + "; message=" + error.getMessage());
    }

    public static void creatorFinished(Entity entity, XaeroIcon icon) {
        Request request = REQUEST.get();
        if (request == null || !isAdultWhale(entity) || request.cacheHit) return;
        request.creatorProduced = icon != null;
        report("creator-result-" + request.creatorProduced, "RadarIconCreator#create result non-null=" + request.creatorProduced);
    }

    public static void cacheWritten(EntityType<?> type, XaeroIcon icon) {
        Request request = REQUEST.get();
        if (request == null || type == null || !"naturalist:whale".equals(EntityType.getKey(type).toString()) || request.cacheHit) return;
        request.cacheWritten = true;
        report("cache-write-" + (icon != null), "cache write result non-null=" + (icon != null));
    }

    public static void requestFinished(Entity entity, XaeroIcon icon) {
        Request request = REQUEST.get();
        if (request == null || !isAdultWhale(entity)) return;
        report("final", "final manager result non-null=" + (icon != null) + "; cacheHit=" + request.cacheHit
                + "; creator=" + request.creatorEntered + "; modelForm=" + request.modelFormEntered + "; modelPart=" + request.modelPartEntered
                + "; native rendered destination count=" + request.nativeRenderedParts + "; contractResolved=" + request.contractResolved
                + "; adapter trace resolution=" + request.adapterTraceResolved + "; selectedSynthetic=" + request.selectedSynthetic
                + "; renderCenterLive=" + request.centerIsLiveNaturalist + "; renderCenterDirectCubes=" + request.centerHasDirectCubes
                + "; renderCenterDirectTrace=" + request.centerHasDirectTrace + "; fallbackRenderedParts=" + request.fallbackRenderedParts
                + "; adapterRecorded=" + request.adapterRecorded + "; selectedRecorded=" + request.selectedRecorded
                + "; renderCenterRecorded=" + request.centerRecorded + "; creatorResult=" + request.creatorProduced
                + "; cacheWrite=" + request.cacheWritten + "; skipped=" + request.skipped + "; exception=" + request.failure);
        REQUEST.remove();
    }

    public static void resourceReloaded() { REQUEST.remove(); REPORTED.clear(); }
    public static boolean isAdultWhale(Entity entity) {
        return entity instanceof AgeableMob ageable && !ageable.isBaby()
                && "naturalist:whale".equals(String.valueOf(EntityType.getKey(entity.getType())));
    }

    private static int cubeCount(ModelPart part) {
        List<ModelPart.Cube> cubes = part == null ? null : ModelPartUtil.getCubes(part);
        return cubes == null ? -1 : cubes.size();
    }
    private static String childNames(ModelPart part) {
        Map<String, ModelPart> children = part == null ? null : ModelPartUtil.getChildren(part);
        return children == null ? "<unavailable>" : children.keySet().stream().sorted().toList().toString();
    }
    private static String selectedGeometry(NaturalistModelContracts.Contract contract, ModelPart selected) {
        return contract.drawableChildren().isEmpty() ? "complete source subtree" : String.join(",", contract.drawableChildren());
    }
    private static String path(List<String> segments) { return segments.isEmpty() ? "<model root>" : String.join("/", segments); }
    private static String className(Object value) { return value == null ? "null" : value.getClass().getName(); }
    private static void report(String outcome, String message) { if (REPORTED.add(outcome)) LOGGER.info("{}", message); }

    private static final class Request {
        private boolean cacheHit, creatorEntered, modelFormEntered, modelPartEntered, contractResolved, adapterBuilt,
                adapterTraceResolved, selectedSynthetic, centerIsLiveNaturalist, centerHasDirectCubes, centerHasDirectTrace,
                centerIsTrace, adapterRecorded, selectedRecorded, centerRecorded, creatorProduced, cacheWritten;
        private int nativeRenderedParts, fallbackRenderedParts;
        private String skipped, failure;
    }
}
