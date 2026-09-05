package dev.resivore.ribbitsxaeroicons;

import java.net.JarURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.jar.JarFile;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.impl.launch.knot.Knot;

/** Applies both packaged compatibility mixin sets without launching Minecraft. */
public final class ProductionMixinApplicationHarness {
    private static final String CREATOR =
            "xaero.hud.minimap.radar.icon.creator.RadarIconCreator";
    private static final String VARIANT_HANDLER =
            "xaero.hud.minimap.radar.icon.cache.id.variant.RadarIconVariantHandler";
    private static final String MANAGER =
            "xaero.hud.minimap.radar.icon.RadarIconManager";
    private static final String CACHE =
            "xaero.hud.minimap.radar.icon.cache.RadarIconCache";
    private static final String CACHE_ACCESSOR =
            "dev.resivore.ribbitsxaeroicons.mixin.RadarIconCacheAccessor";

    private ProductionMixinApplicationHarness() {
    }

    public static void main(String[] args) throws Exception {
        require(args.length == 7,
                "expected patch, Xaero, GeckoLib, Ribbits, historical C3, EMF, and Trinkets paths");
        Path expectedPatch = Path.of(args[0]).toRealPath();
        Path expectedXaero = Path.of(args[1]).toRealPath();
        Path expectedGecko = Path.of(args[2]).toRealPath();
        Path expectedRibbits = Path.of(args[3]).toRealPath();
        Path expectedC3 = Path.of(args[4]).toRealPath();
        Path expectedEmf = Path.of(args[5]).toRealPath();
        Path expectedTrinkets = Path.of(args[6]).toRealPath();

        require(expectedPatch.getFileName().toString()
                        .equals("ribbits-xaero-entity-icons-compat-0.1.0-canary3.jar"),
                "unexpected Canary 2 JAR " + expectedPatch);
        requireOfficialNamespace(expectedXaero);
        requireOfficialNamespace(expectedGecko);
        requireOfficialNamespace(expectedRibbits);
        requireOfficialNamespace(expectedTrinkets);

        Knot knot = new Knot(EnvType.CLIENT);
        ClassLoader targetLoader = knot.init(new String[0]);

        requireResourceFrom(
                targetLoader,
                "ribbits_xaero_entity_icons_compat.mixins.json",
                expectedPatch);
        requireResourceFrom(
                targetLoader,
                "xaero_emf_entity_icon_compat.mixins.json",
                expectedC3);
        requireResourceFrom(targetLoader, "xaerominimap.mixins.json", expectedXaero);
        requireResourceFrom(targetLoader, "fabric.mod.json", expectedPatch, expectedXaero,
                expectedGecko, expectedRibbits, expectedC3, expectedEmf, expectedTrinkets);

        Class<?> creator = loadFrom(targetLoader, CREATOR, expectedXaero);
        requireSingleHandler(creator, "ribbitsXaeroIcons$wrapGeoPrerenderer");
        requireSingleHandler(creator, "ribbitsXaeroIcons$result");
        Class<?> entityCache = loadFrom(targetLoader, "xaero.hud.minimap.radar.icon.cache.RadarIconEntityCache", expectedXaero);
        requireSingleHandler(entityCache, "ribbitsXaeroIcons$cacheResult");
        if (expectedC3.getFileName().toString().contains("canary4")) {
            requireSingleHandler(entityCache, "xaeroEmf$cache");
        }

        Class<?> variantHandler = loadFrom(targetLoader, VARIANT_HANDLER, expectedXaero);
        requireSingleHandler(variantHandler, "ribbitsXaeroIcons$extendRibbitVariant");

        Class<?> manager = loadFrom(targetLoader, MANAGER, expectedXaero);
        requireSingleHandler(manager, "ribbitsXaeroIcons$invalidateRibbitResources");
        if (expectedC3.getFileName().toString().contains("canary4")) requireSingleHandler(manager, "xaeroEmf$reload");

        Class<?> cache = loadFrom(targetLoader, CACHE, expectedXaero);
        require(Arrays.stream(cache.getInterfaces())
                        .anyMatch(type -> type.getName().equals(CACHE_ACCESSOR)),
                "RadarIconCache accessor mixin did not apply");
        requireSingleHandler(cache, "ribbitsXaeroIcons$getIconCacheMap");

        Class<?> provider = Class.forName(
                "dev.resivore.ribbitsxaeroicons.RibbitGeoIconProvider", false, targetLoader);
        require(codeSource(provider).equals(expectedPatch),
                "provider did not load from packaged Canary 3");

        Class<?> c3ModelMixinTarget = loadFrom(
                targetLoader,
                "xaero.hud.minimap.radar.icon.creator.render.form.model."
                        + "RadarIconModelPrerenderer",
                expectedXaero);
        require(Arrays.stream(c3ModelMixinTarget.getDeclaredMethods())
                        .anyMatch(method -> method.getName()
                                .contains("xaeroEmfEntityIconCompat$renderRelocatedHead")),
                "historical C3 model-prerender mixin did not coexist");

        System.out.println("Production Minecraft 26.2 Knot/Mixin application passed for "
                + expectedPatch.getFileName()
                + " with exact Xaero, GeckoLib, Ribbits C9, Trinkets, and the specified EMF companion");
    }

    private static Class<?> loadFrom(
            ClassLoader loader, String className, Path expectedJar) throws Exception {
        Class<?> type = Class.forName(className, false, loader);
        require(codeSource(type).equals(expectedJar),
                className + " loaded from " + codeSource(type) + " instead of " + expectedJar);
        return type;
    }

    private static void requireSingleHandler(Class<?> type, String exactName) {
        long count = Arrays.stream(type.getDeclaredMethods())
                .filter(method -> method.getName().contains(exactName))
                .filter(method -> !method.getName().contains("lambda$"))
                .count();
        require(count == 1, exactName + " applied " + count + " times to " + type.getName());
    }

    private static void requireOfficialNamespace(Path jar) throws Exception {
        try (JarFile archive = new JarFile(jar.toFile())) {
            String namespace = archive.getManifest().getMainAttributes()
                    .getValue("Fabric-Mapping-Namespace");
            require("official".equals(namespace),
                    jar.getFileName() + " is not in the official production namespace");
        }
    }

    private static void requireResourceFrom(
            ClassLoader loader, String resourceName, Path expectedJar) throws Exception {
        URL resource = loader.getResource(resourceName);
        require(resource != null, "missing resource " + resourceName);
        require(resource.openConnection() instanceof JarURLConnection,
                resourceName + " did not load from a packaged JAR: " + resource);
        Path actualJar = Path.of(
                ((JarURLConnection) resource.openConnection()).getJarFileURL().toURI())
                .toRealPath();
        require(actualJar.equals(expectedJar),
                resourceName + " loaded from " + actualJar + " instead of " + expectedJar);
    }

    private static void requireResourceFrom(
            ClassLoader loader, String resourceName, Path... expectedJars) throws Exception {
        var resources = loader.getResources(resourceName);
        java.util.Set<Path> actual = new java.util.HashSet<>();
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            if (resource.openConnection() instanceof JarURLConnection connection) {
                actual.add(Path.of(connection.getJarFileURL().toURI()).toRealPath());
            }
        }
        for (Path expected : expectedJars) {
            require(actual.contains(expected),
                    resourceName + " was not visible from expected JAR " + expected);
        }
    }

    private static Path codeSource(Class<?> type) throws Exception {
        return Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI())
                .toRealPath();
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
