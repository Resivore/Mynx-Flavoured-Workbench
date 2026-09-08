package dev.resivore.naturalistxaeroicons;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xaero.common.icon.XaeroIcon;

/**
 * C12 records only the cache-miss creator chain that actually produces a Brown Bear icon.
 * It is evidence, not a rendering override: no pose, texture, icon, cache, or model is changed.
 */
public final class BrownBearPathDiagnostic {
    private static final Logger LOGGER = LoggerFactory.getLogger("NaturalistXaero BrownBearPath");
    private static final Set<String> REPORTED = ConcurrentHashMap.newKeySet();
    private static final ThreadLocal<Request> REQUEST = new ThreadLocal<>();

    private BrownBearPathDiagnostic() {}

    public static boolean isBrownBear(Entity entity) {
        if (entity == null) return false;
        Identifier id = EntityType.getKey(entity.getType());
        return id != null && id.getNamespace().equals("naturalist") && id.getPath().equals("bear");
    }

    public static void requestStarted(Entity entity, boolean canPrerender) {
        if (!isBrownBear(entity)) return;
        REQUEST.set(new Request());
        report("request", "request naturalist:bear; canPrerender=" + canPrerender);
    }

    public static void cacheLookup(boolean hit) {
        Request request = REQUEST.get();
        if (request == null) return;
        request.cacheHit = hit;
        report("cache-" + hit, "RadarIconEntityCache#get initial " + (hit ? "HIT" : "MISS"));
    }

    public static void creatorStarted(Entity entity, Object form, Identifier texture) {
        Request request = REQUEST.get();
        if (request == null || !isBrownBear(entity) || request.cacheHit) return;
        request.creatorEntered = true;
        report("creator", "post-MISS RadarIconCreator#create form=" + className(form)
                + "; rendererTexture=" + String.valueOf(texture));
    }

    public static void modelFormStarted(Entity entity, int traces, String textures) {
        Request request = REQUEST.get();
        if (request == null || !isBrownBear(entity) || request.cacheHit) return;
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
        if (request == null || !isBrownBear(entity) || request.cacheHit) return;
        request.creatorProduced = icon != null;
        report("creator-result-" + (icon != null), "RadarIconCreator#create first returned non-null=" + (icon != null));
    }

    public static void cacheWritten(EntityType<?> type, XaeroIcon icon) {
        Request request = REQUEST.get();
        if (request == null || type == null || !"naturalist:bear".equals(EntityType.getKey(type).toString()) || request.cacheHit) return;
        request.cacheWritten = true;
        report("cache-write-" + (icon != null), "RadarIconEntityCache#add wrote post-MISS XaeroIcon non-null=" + (icon != null));
    }

    public static void requestFinished(XaeroIcon icon) {
        Request request = REQUEST.get();
        if (request == null) return;
        report("final-" + request.cacheHit + "-" + request.creatorEntered + "-" + request.modelFormEntered
                        + "-" + request.modelPartPath + "-" + request.creatorProduced + "-" + request.cacheWritten,
                "manager result non-null=" + (icon != null) + "; cacheHit=" + request.cacheHit
                        + "; creator=" + request.creatorEntered + "; modelForm=" + request.modelFormEntered
                        + "; modelPart=" + request.modelPartPath + "; creatorProduced=" + request.creatorProduced
                        + "; cacheWritten=" + request.cacheWritten);
        REQUEST.remove();
    }

    public static void resourceReloaded() {
        REQUEST.remove();
        REPORTED.clear();
        report("reload", "Xaero resource reload observed; one-time Brown Bear path evidence reset");
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
    }
}
