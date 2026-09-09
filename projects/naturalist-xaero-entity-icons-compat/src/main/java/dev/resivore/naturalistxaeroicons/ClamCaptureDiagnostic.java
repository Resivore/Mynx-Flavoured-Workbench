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

/** C20 observes the Clam top-shell-only bridge and its exact production presentation. */
public final class ClamCaptureDiagnostic {
    private static final Logger LOGGER = LoggerFactory.getLogger("NaturalistXaero ClamCapture");
    private static final Set<String> REPORTED = ConcurrentHashMap.newKeySet();
    private static final ThreadLocal<Request> REQUEST = new ThreadLocal<>();

    private ClamCaptureDiagnostic() {}

    public static void requestStarted(Entity entity, boolean canPrerender) {
        if (!isClam(entity)) return;
        REQUEST.set(new Request());
        report("request", "request naturalist:clam; canPrerender=" + canPrerender);
    }

    public static void cacheLookup(boolean hit) {
        Request request = REQUEST.get();
        if (request == null) return;
        request.cacheHit = hit;
        report("cache-" + hit, "RadarIconEntityCache#get initial " + (hit ? "HIT" : "MISS"));
    }

    public static void creatorStarted(Entity entity, Object form, Identifier texture) {
        Request request = REQUEST.get();
        if (request == null || !isClam(entity) || request.cacheHit) return;
        request.creatorEntered = true;
        report("creator", "post-MISS RadarIconCreator#create form=" + className(form)
                + "; rendererTexture=" + String.valueOf(texture));
    }

    public static void modelFormStarted(Entity entity, int traces, String textures) {
        Request request = REQUEST.get();
        if (request == null || !isClam(entity) || request.cacheHit) return;
        request.modelFormEntered = true;
        report("model-form", "post-MISS RadarIconModelFormPrerenderer#prerender traces=" + traces
                + "; traceTextures=" + textures);
    }

    public static void modelPartPath(String method) {
        Request request = REQUEST.get();
        if (request == null || request.cacheHit) return;
        request.modelPartPath = true;
        report("model-part-" + method, "post-MISS " + method + " reached");
    }

    public static void creatorFinished(Entity entity, XaeroIcon icon) {
        Request request = REQUEST.get();
        if (request == null || !isClam(entity) || request.cacheHit) return;
        request.creatorProduced = icon != null;
        report("creator-result-" + (icon != null), "RadarIconCreator#create first returned non-null=" + (icon != null));
    }

    public static void cacheWritten(EntityType<?> type, XaeroIcon icon) {
        Request request = REQUEST.get();
        if (request == null || type == null || !"naturalist:clam".equals(EntityType.getKey(type).toString()) || request.cacheHit) return;
        request.cacheWritten = true;
        report("cache-write-" + (icon != null), "RadarIconEntityCache#add wrote post-MISS XaeroIcon non-null=" + (icon != null));
    }

    public static void nativePathObserved(Model model, int renderedParts) {
        Request request = REQUEST.get();
        if (request == null) return;
        request.nativeObserved = true;
        request.nativeRenderedParts = renderedParts;
        report("native-" + renderedParts, "native model path returned renderedParts=" + renderedParts
                + "; model=" + className(model));
    }

    public static void fallbackSkipped(String reason) {
        Request request = REQUEST.get();
        if (request == null) return;
        request.skipped = reason;
        report("skipped-" + reason, "fallback skipped: " + reason);
    }

    public static void contractResolved(NaturalistModelContracts.Contract contract) {
        Request request = REQUEST.get();
        if (request == null) return;
        request.contractResolved = true;
        report("contract", "resolved Clam contract source=" + String.join("/", contract.path())
                + "; trace=" + String.join("/", contract.tracePath())
                + "; children=" + String.join(",", contract.drawableChildren())
                + "; scale=" + contract.presentation().scale()
                + "; frameYOffset=" + contract.presentation().frameYOffset());
    }

    public static void adapterBuilt(boolean traceExists) {
        Request request = REQUEST.get();
        if (request == null) return;
        request.adapterBuilt = true;
        request.traceBound = traceExists;
        report("adapter-" + traceExists, "Clam adapter built; explicit trace bound=" + traceExists);
    }

    public static void fallbackRendered(int before, int after, ModelPart adapter, ModelPart selected,
                                        List<ModelPart> renderedParts) {
        Request request = REQUEST.get();
        if (request == null) return;
        request.fallbackRendered = true;
        request.fallbackRenderedParts = after;
        report("render-" + before + "-" + after, "Clam fallback render destination before=" + before
                + "; after=" + after + "; adapterRecorded=" + renderedParts.contains(adapter)
                + "; selectedRecorded=" + renderedParts.contains(selected));
    }

    public static void failed(RuntimeException error) {
        Request request = REQUEST.get();
        if (request == null) return;
        request.failure = error.getClass().getName();
        report("failure-" + request.failure, "Clam fallback threw " + request.failure
                + "; message=" + String.valueOf(error.getMessage()));
    }

    public static void requestFinished(XaeroIcon icon) {
        Request request = REQUEST.get();
        if (request == null) return;
        report("final-" + request.cacheHit + "-" + request.creatorEntered + "-" + request.modelFormEntered
                        + "-" + request.modelPartPath + "-" + request.creatorProduced + "-" + request.cacheWritten
                        + "-" + request.nativeRenderedParts + "-" + request.contractResolved + "-"
                        + request.adapterBuilt + "-" + request.traceBound + "-" + request.fallbackRendered
                        + "-" + request.failure + "-" + (icon != null),
                "manager result non-null=" + (icon != null) + "; cacheHit=" + request.cacheHit
                        + "; creator=" + request.creatorEntered + "; modelForm=" + request.modelFormEntered
                        + "; modelPart=" + request.modelPartPath + "; creatorProduced=" + request.creatorProduced
                        + "; cacheWritten=" + request.cacheWritten + "; nativeObserved=" + request.nativeObserved
                        + "; nativeRenderedParts=" + request.nativeRenderedParts
                        + "; contractResolved=" + request.contractResolved + "; adapterBuilt=" + request.adapterBuilt
                        + "; traceBound=" + request.traceBound + "; fallbackRendered=" + request.fallbackRendered
                        + "; fallbackRenderedParts=" + request.fallbackRenderedParts
                        + "; skipped=" + request.skipped + "; failure=" + request.failure);
        REQUEST.remove();
    }

    public static void resourceReloaded() {
        REQUEST.remove();
        REPORTED.clear();
        report("reload", "Xaero resource reload observed; one-time Clam capture evidence reset");
    }

    public static boolean isClam(Entity entity) {
        if (entity == null) return false;
        Identifier id = EntityType.getKey(entity.getType());
        return id != null && "naturalist".equals(id.getNamespace()) && "clam".equals(id.getPath());
    }

    private static String className(Object value) { return value == null ? "null" : value.getClass().getName(); }

    private static void report(String outcome, String message) {
        if (REPORTED.add(outcome)) LOGGER.info("{}", message);
    }

    private static final class Request {
        private boolean cacheHit;
        private boolean creatorEntered;
        private boolean modelFormEntered;
        private boolean modelPartPath;
        private boolean creatorProduced;
        private boolean cacheWritten;
        private boolean nativeObserved;
        private int nativeRenderedParts;
        private boolean contractResolved;
        private boolean adapterBuilt;
        private boolean traceBound;
        private boolean fallbackRendered;
        private int fallbackRenderedParts;
        private String skipped;
        private String failure;
    }
}
