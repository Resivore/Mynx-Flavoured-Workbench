package com.yungnickyoung.minecraft.ribbits.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.impl.launch.knot.Knot;
import org.spongepowered.asm.mixin.Mixins;

import java.net.JarURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.jar.JarFile;

/**
 * Loads, transforms, and links the two Phase C targets through production Fabric Loader and
 * Mixin without invoking either Minecraft entry point.
 */
public final class ProductionMixinApplicationHarness {
    private static final String STRUCTURE_PIECE =
            "net.minecraft.world.level.levelgen.structure.StructurePiece";
    private static final String SWAMP_HUT_PIECE =
            "net.minecraft.world.level.levelgen.structure.structures.SwampHutPiece";
    private static final String NATURAL_SPAWNER = "net.minecraft.world.level.NaturalSpawner";
    private static final String STRUCTURE_PIECE_INVOKER =
            "com.yungnickyoung.minecraft.ribbits.mixin.mixins.accessor.StructurePieceInvoker";

    private ProductionMixinApplicationHarness() {
    }

    public static void main(String[] args) throws Exception {
        require(args.length == 1, "expected the packaged Ribbits JAR path");
        Path expectedModJar = Path.of(args[0]).toRealPath();
        try (JarFile archive = new JarFile(expectedModJar.toFile())) {
            require("official".equals(archive.getManifest().getMainAttributes()
                            .getValue("Fabric-Mapping-Namespace")),
                    "packaged JAR is not in the official Minecraft 26.2 runtime namespace");
        }

        Knot knot = new Knot(EnvType.SERVER);
        ClassLoader targetLoader = knot.init(new String[] {"--nogui"});

        URL config = targetLoader.getResource("ribbits.mixins.json");
        require(config != null, "packaged ribbits.mixins.json is missing");
        require(config.openConnection() instanceof JarURLConnection,
                "mixin config did not come from the packaged JAR: " + config);
        JarURLConnection connection = (JarURLConnection) config.openConnection();
        Path actualModJar = Path.of(connection.getJarFileURL().toURI()).toRealPath();
        require(expectedModJar.equals(actualModJar),
                "mixin config came from " + actualModJar + " instead of " + expectedModJar);

        Mixins.addConfiguration("ribbits.mixins.json");

        Class<?> structurePiece = Class.forName(STRUCTURE_PIECE, false, targetLoader);
        Class<?> swampHutPiece = Class.forName(SWAMP_HUT_PIECE, false, targetLoader);
        Class<?> naturalSpawner = Class.forName(NATURAL_SPAWNER, false, targetLoader);

        require(Arrays.stream(structurePiece.getInterfaces())
                        .anyMatch(type -> type.getName().equals(STRUCTURE_PIECE_INVOKER)),
                "StructurePiece invoker mixin did not apply");
        require(methodCount(structurePiece, "ribbits$invokeGetWorldPos") == 1,
                "getWorldPos invoker did not resolve exactly once");
        require(methodCount(structurePiece, "ribbits$invokePlaceBlock") == 1,
                "placeBlock invoker did not resolve exactly once");
        require(methodTokenCount(swampHutPiece, "ribbits$moveInitialResidentToSorcererPosition") == 1,
                "initial-Witch ModifyArgs did not apply exactly once");
        require(methodTokenCount(swampHutPiece, "ribbits$replaceInitialWitch") == 1,
                "initial-Witch Redirect did not apply exactly once");
        require(methodTokenCount(swampHutPiece, "ribbits$moveInitialCat") == 1,
                "initial-Cat ModifyArgs did not apply exactly once");
        require(methodTokenCount(swampHutPiece, "ribbits$placeMapBarrel") == 1,
                "map-barrel Inject did not apply exactly once");
        require(methodTokenCount(naturalSpawner,
                        "ribbits$suppressNaturalWitchInsideExactHutPiece") == 1,
                "natural-Witch Inject did not apply exactly once");

        System.out.println("Production Minecraft 26.2 mixin application passed for "
                + expectedModJar.getFileName());
    }

    private static long methodCount(Class<?> owner, String name) {
        return Arrays.stream(owner.getDeclaredMethods())
                .filter(method -> method.getName().equals(name))
                .count();
    }

    private static long methodTokenCount(Class<?> owner, String token) {
        return Arrays.stream(owner.getDeclaredMethods())
                .filter(method -> method.getName().contains(token))
                .count();
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
