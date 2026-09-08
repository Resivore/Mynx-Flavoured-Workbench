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
 * Runtime-only evidence for the one intentionally conspicuous Brown Bear capture probe.
 * Messages are once per distinct outcome after each Xaero resource reload, never per frame.
 */
public final class BrownBearDiagnostic {
    private static final Logger LOGGER = LoggerFactory.getLogger("NaturalistXaero BrownBearDiagnostic");
    private static final Set<String> REPORTED = ConcurrentHashMap.newKeySet();
    private static final ThreadLocal<Request> REQUEST = new ThreadLocal<>();

    private BrownBearDiagnostic() {}

    public static boolean isBrownBear(Entity entity) {
        if (entity == null) return false;
        Identifier id = EntityType.getKey(entity.getType());
        return id != null && id.getNamespace().equals("naturalist") && id.getPath().equals("bear");
    }

    public static void requestStarted(Entity entity, boolean canPrerender) {
        if (!isBrownBear(entity)) return;
        REQUEST.set(new Request(canPrerender));
        report("request", "RadarIconManager#get began Brown Bear request; canPrerender=" + canPrerender);
    }

    public static void cacheLookup(boolean hit) {
        Request request = REQUEST.get();
        if (request == null) return;
        request.cacheHit = hit;
        report("cache-" + hit, "RadarIconEntityCache#get Brown Bear initial " + (hit ? "HIT" : "MISS") + ".");
    }

    public static void nativePresentationEntered() {
        Request request = REQUEST.get();
        if (request != null) request.nativeHookEntered = true;
        report("native-hook", "entered RadarIconModelPrerenderer#renderModel native Brown Bear presentation hook; applying C11 90-degree Z capture rotation.");
    }

    public static void nativePrerenderReturned(boolean rendered) {
        Request request = REQUEST.get();
        if (request == null && !rendered) return;
        if (request != null) {
            request.nativePrerenderReturned = true;
            request.nativeRendered = rendered;
        }
        report("native-result-" + rendered, "RadarIconModelPrerenderer#renderModel returned for Brown Bear; renderedDest=" + (rendered ? "nonempty" : "empty") + ".");
    }

    public static void requestFinished(XaeroIcon icon) {
        Request request = REQUEST.get();
        if (request == null) return;
        report("final-" + request.cacheHit + "-" + request.nativeHookEntered + "-" + request.nativeRendered,
                "RadarIconManager#get returned final XaeroIcon=" + (icon != null)
                        + "; cacheHit=" + request.cacheHit
                        + "; nativeHook=" + request.nativeHookEntered
                        + "; nativeRendered=" + request.nativeRendered + ".");
        REQUEST.remove();
    }

    public static void resourceReloaded() {
        REQUEST.remove();
        REPORTED.clear();
        report("reload", "Xaero resource reload observed; Brown Bear diagnostic outcome logging reset.");
    }

    private static void report(String outcome, String message) {
        if (REPORTED.add(outcome)) LOGGER.info("{}", message);
    }

    private static final class Request {
        private final boolean canPrerender;
        private boolean cacheHit;
        private boolean nativeHookEntered;
        private boolean nativePrerenderReturned;
        private boolean nativeRendered;

        private Request(boolean canPrerender) {
            this.canPrerender = canPrerender;
        }
    }
}
