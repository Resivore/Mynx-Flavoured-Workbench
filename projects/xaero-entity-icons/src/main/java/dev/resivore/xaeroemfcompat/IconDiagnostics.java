package dev.resivore.xaeroemfcompat;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
/** Bounded reason codes; never logs entity UUIDs or model geometry. */
public final class IconDiagnostics {
    private static final Logger LOG=LogManager.getLogger("xaero_emf_entity_icon_compat");
    private static final Set<String> SEEN=ConcurrentHashMap.newKeySet();
    private static final Set<String> RETRIED_FAILED=ConcurrentHashMap.newKeySet();
    private static final ThreadLocal<String> CONTEXT=ThreadLocal.withInitial(()->"structural-fixture");
    private static final ThreadLocal<String> LAST=new ThreadLocal<>();
    private static long generation;
    private static final Set<net.minecraft.world.entity.EntityType<?>> OWNED=ConcurrentHashMap.newKeySet();
    public static void observe(net.minecraft.world.entity.EntityType<?> type) {OWNED.add(type);}
    public static boolean owns(net.minecraft.world.entity.EntityType<?> type) {return OWNED.contains(type);}
    /**
     * Allows one cache retry per exact resource generation for a model root
     * already proven to belong to this narrow EMF bridge.  This replaces a
     * stale FAILED sentinel without turning persistent failures into a hot
     * render loop.
     */
    public static boolean retryFailedOnce(net.minecraft.world.entity.EntityType<?> type,Object variant) {
        return owns(type) && RETRIED_FAILED.add(generation+":"+net.minecraft.world.entity.EntityType.getKey(type)+":"+variant);
    }
    private IconDiagnostics() {}
    public static void context(String value) {CONTEXT.set(value);LAST.remove();}
    public static void clearContext() {CONTEXT.remove();}
    public static String lastReason() {return LAST.get();}
    public static void event(String reason,String path) {
        LAST.set(reason);
        String key=CONTEXT.get()+":"+reason+":"+path;
        if(SEEN.size()<2048 && SEEN.add(key))LOG.info("EMF icon identity={} generation={} stage={} path={}",CONTEXT.get(),generation,reason,path);
    }
    public static void reload() {generation++;SEEN.clear();RETRIED_FAILED.clear();LOG.info("EMF icon resource generation={} cache reset requested",generation);}
    public static void activation(String value) {LOG.info("EMF icon Canary 4 mixin {}",value);}
}
