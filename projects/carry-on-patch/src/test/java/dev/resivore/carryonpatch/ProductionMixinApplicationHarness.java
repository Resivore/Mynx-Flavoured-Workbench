package dev.resivore.carryonpatch;

import java.lang.reflect.Field;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.jar.JarFile;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.impl.launch.knot.Knot;
import org.spongepowered.asm.mixin.Mixins;

/**
 * Applies the packaged client mixins to production Minecraft and exact GrabAndGo without starting
 * Minecraft. Loading the renderer initializes the redirected cache and links its Minecraft types.
 */
public final class ProductionMixinApplicationHarness {
    private static final String RENDERER =
            "org.chermew.grabandgo.client.render.CarriedObjectFeatureRenderer";
    private static final String ENTITY = "net.minecraft.world.entity.Entity";
    private static final String ACCESSOR =
            "dev.resivore.carryonpatch.mixin.EntityIdAccessor";
    private static final String CACHE =
            "dev.resivore.carryonpatch.RenderIdAssigningEntityCache";

    private ProductionMixinApplicationHarness() {
    }

    public static void main(String[] args) throws Exception {
        require(args.length == 2, "expected packaged patch JAR and exact GrabAndGo JAR paths");
        Path expectedPatch = Path.of(args[0]).toRealPath();
        Path expectedUpstream = Path.of(args[1]).toRealPath();
        require(expectedPatch.getFileName().toString()
                        .equals("carry-on-patch-0.1.0-canary3.jar"),
                "unexpected patch JAR " + expectedPatch);
        requireOfficialNamespace(expectedUpstream);

        boolean server = Boolean.getBoolean("carryOnPatch.serverTest");
        Knot knot = new Knot(server ? EnvType.SERVER : EnvType.CLIENT);
        ClassLoader targetLoader = knot.init(new String[0]);

        if (!server) {
            Mixins.addConfiguration("grabandgo.client.mixins.json");
            Mixins.addConfiguration("carry_on_patch.client.mixins.json");
        }
        Mixins.addConfiguration("grabandgo.mixins.json");
        Mixins.addConfiguration("carry_on_patch.mixins.json");
        Class<?> player = Class.forName("net.minecraft.world.entity.player.Player", false, targetLoader);
        require(Arrays.stream(player.getInterfaces()).anyMatch(t -> t.getName().equals(
                "dev.resivore.carryonpatch.common.CarryStateAccess")), "common player mixin missing");
        Class<?> handler = Class.forName("org.chermew.grabandgo.event.GrabHandler", false, targetLoader);
        require(Arrays.stream(handler.getDeclaredMethods()).anyMatch(m -> m.getName().contains("carryOnPatch$place")),
                "server placement interception missing");
        Class.forName("dev.resivore.carryonpatch.PersistenceFixtures", true, targetLoader)
                .getMethod("run").invoke(null);
        if (server) {
            require(Arrays.stream(Class.forName(ENTITY, false, targetLoader).getInterfaces())
                    .noneMatch(t -> t.getName().equals(ACCESSOR)), "client accessor leaked into server");
            System.out.println("Production SERVER common persistence/application fixtures passed");
            return;
        }
        requireResourceFrom(targetLoader, "carry_on_patch.client.mixins.json", expectedPatch);
        requireResourceFrom(targetLoader, "grabandgo.client.mixins.json", expectedUpstream);
        Mixins.addConfiguration("grabandgo.client.mixins.json");
        Mixins.addConfiguration("carry_on_patch.client.mixins.json");

        Class<?> entity = Class.forName(ENTITY, false, targetLoader);
        require(Arrays.stream(entity.getInterfaces())
                        .anyMatch(type -> type.getName().equals(ACCESSOR)),
                "EntityIdAccessor mixin did not apply to production Entity");
        require(Arrays.stream(entity.getDeclaredMethods())
                        .filter(method -> method.getName().equals("carryOnPatch$getRawId"))
                        .count() == 1,
                "raw-ID accessor did not resolve exactly once");

        Class<?> renderer = Class.forName(RENDERER, true, targetLoader);
        require(codeSource(renderer).equals(expectedUpstream),
                "renderer did not load from exact GrabAndGo JAR: " + codeSource(renderer));
        require(Arrays.stream(renderer.getDeclaredMethods())
                        .filter(method -> method.getName()
                                .contains("carryOnPatch$installAssigningCache"))
                        .count() == 1,
                "cache-construction redirect handler did not apply exactly once");

        Class<?> hands = Class.forName("net.minecraft.client.renderer.ItemInHandRenderer", false, targetLoader);
        require(Arrays.stream(hands.getDeclaredMethods()).filter(m -> m.getName()
                        .contains("carryOnPatch$placeFirstPerson")).count() == 1,
                "first-person carried submit redirect did not apply exactly once");
        require(Arrays.stream(renderer.getDeclaredMethods()).filter(m -> m.getName()
                        .contains("carryOnPatch$placeThirdPerson")).count() == 1,
                "third-person carried submit redirect did not apply exactly once");

        Field cacheField = renderer.getDeclaredField("dummyEntityCache");
        Object cache = cacheField.get(null);
        require(cache instanceof Map<?, ?>, "GrabAndGo dummyEntityCache is not a map");
        require(cache.getClass().getName().equals(CACHE),
                "ordinal-zero cache redirect installed " + cache.getClass().getName());
        require(codeSource(cache.getClass()).equals(expectedPatch),
                "assigning cache did not load from packaged patch JAR: "
                        + codeSource(cache.getClass()));

        System.out.println("Production Minecraft 26.2 client Mixin application passed for "
                + expectedPatch.getFileName() + " with " + expectedUpstream.getFileName());
    }

    private static void requireOfficialNamespace(Path jar) throws Exception {
        try (JarFile archive = new JarFile(jar.toFile())) {
            String namespace = archive.getManifest().getMainAttributes()
                    .getValue("Fabric-Mapping-Namespace");
            require("official".equals(namespace),
                    "GrabAndGo reference is not in the official runtime namespace");
        }
    }

    private static void requireResourceFrom(
            ClassLoader loader, String resourceName, Path expectedJar) throws Exception {
        URL resource = loader.getResource(resourceName);
        require(resource != null, "missing resource " + resourceName);
        require(resource.openConnection() instanceof JarURLConnection,
                resourceName + " did not load from a packaged JAR: " + resource);
        JarURLConnection connection = (JarURLConnection) resource.openConnection();
        Path actualJar = Path.of(connection.getJarFileURL().toURI()).toRealPath();
        require(actualJar.equals(expectedJar),
                resourceName + " loaded from " + actualJar + " instead of " + expectedJar);
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
