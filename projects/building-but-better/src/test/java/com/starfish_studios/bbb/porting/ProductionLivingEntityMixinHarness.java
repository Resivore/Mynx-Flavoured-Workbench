package com.starfish_studios.bbb.porting;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.impl.launch.knot.Knot;
import org.spongepowered.asm.mixin.Mixins;

import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.jar.JarFile;

/**
 * Transforms the real production 26.2 LivingEntity through Fabric Knot and Mixin without
 * starting Minecraft. This catches target-member and namespace/refmap errors that source tests
 * or a structurally valid JAR cannot observe.
 */
public final class ProductionLivingEntityMixinHarness {
    private ProductionLivingEntityMixinHarness() {
    }

    public static void main(String[] args) throws Exception {
        require(args.length == 1, "expected the packaged BBB JAR path");
        Path expectedJar = Path.of(args[0]).toRealPath();
        try (JarFile archive = new JarFile(expectedJar.toFile())) {
            require("official".equals(archive.getManifest().getMainAttributes()
                            .getValue("Fabric-Mapping-Namespace")),
                    "packaged BBB JAR is not in the official Minecraft runtime namespace");
        }

        Knot knot = new Knot(EnvType.SERVER);
        ClassLoader loader = knot.init(new String[] {"--nogui"});
        URL config = loader.getResource("bbb.mixins.json");
        require(config != null && config.openConnection() instanceof JarURLConnection,
                "packaged BBB mixin config is missing");
        Path actualJar = Path.of(((JarURLConnection) config.openConnection()).getJarFileURL().toURI())
                .toRealPath();
        require(expectedJar.equals(actualJar), "BBB mixin config did not load from the packaged C5 JAR");

        Mixins.addConfiguration("bbb.mixins.json");
        Class<?> livingEntity = Class.forName("net.minecraft.world.entity.LivingEntity", false, loader);
        require(livingEntity.getDeclaredMethod("onClimbable") != null,
                "production LivingEntity no longer exposes onClimbable");
        require(Arrays.stream(livingEntity.getDeclaredMethods())
                        .filter(method -> method.getName().contains("bbb$recognizeVerticalRope"))
                        .count() == 1,
                "BBB vertical-rope injection did not apply exactly once to production LivingEntity");

        System.out.println("Production Minecraft 26.2 LivingEntity rope mixin application passed for "
                + expectedJar.getFileName());
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
