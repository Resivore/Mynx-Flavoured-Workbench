package dev.resivore.naturalistxaeroicons;

import java.net.JarURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.impl.launch.knot.Knot;

/** Applies C11 and the generic C9 against production Xaero without launching Minecraft. */
public final class ProductionMixinApplicationHarness {
    private ProductionMixinApplicationHarness() {}

    public static void main(String[] args) throws Exception {
        require(args.length == 3, "expected Naturalist C11, generic C9, and Xaero paths");
        Path naturalistC11 = Path.of(args[0]).toRealPath();
        Path genericC9 = Path.of(args[1]).toRealPath();
        Path xaero = Path.of(args[2]).toRealPath();

        Knot knot = new Knot(EnvType.CLIENT);
        ClassLoader loader = knot.init(new String[0]);
        requireResourceFrom(loader, "naturalist_xaero_entity_icons_compat.mixins.json", naturalistC11);
        requireResourceFrom(loader, "xaero_emf_entity_icon_compat.mixins.json", genericC9);

        Class<?> manager = loadFrom(loader, "xaero.hud.minimap.radar.icon.RadarIconManager", xaero);
        requireSingleHandler(manager, "naturalistXaeroIcons$observeBrownBearCacheBeforeXaeroEmfRetry");
        requireSingleHandler(manager, "naturalistXaeroIcons$startBrownBearDiagnostic");
        requireSingleHandler(manager, "naturalistXaeroIcons$finishBrownBearDiagnostic");
        requireSingleHandler(manager, "xaeroEmf$retryFailedAtActualPrerender");

        Class<?> cache = loadFrom(loader,
                "xaero.hud.minimap.radar.icon.cache.RadarIconEntityCache", xaero);
        require(Arrays.stream(cache.getInterfaces()).anyMatch(type -> type.getName().equals(
                        "dev.resivore.naturalistxaeroicons.mixin.RadarIconEntityCacheStorageAccessor")),
                "C11 read-only cache observer accessor did not apply");

        System.out.println("Production Knot/Mixin coexistence passed for "
                + naturalistC11.getFileName() + " and " + genericC9.getFileName());
    }

    private static Class<?> loadFrom(ClassLoader loader, String className, Path expectedJar) throws Exception {
        Class<?> type = Class.forName(className, false, loader);
        require(Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI())
                        .toRealPath().equals(expectedJar),
                className + " did not load from the expected production JAR");
        return type;
    }

    private static void requireResourceFrom(ClassLoader loader, String resourceName, Path expectedJar)
            throws Exception {
        URL resource = loader.getResource(resourceName);
        require(resource != null && resource.openConnection() instanceof JarURLConnection,
                "missing packaged " + resourceName);
        Path actual = Path.of(((JarURLConnection) resource.openConnection()).getJarFileURL().toURI())
                .toRealPath();
        require(actual.equals(expectedJar), resourceName + " loaded from " + actual);
    }

    private static void requireSingleHandler(Class<?> type, String name) {
        long count = Arrays.stream(type.getDeclaredMethods())
                .filter(method -> method.getName().contains(name))
                .filter(method -> !method.getName().contains("lambda$"))
                .count();
        require(count == 1, name + " applied " + count + " times to " + type.getName());
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
