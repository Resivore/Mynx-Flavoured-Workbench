package dev.resivore.mynxfloratrades;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.impl.launch.knot.Knot;
import org.spongepowered.asm.mixin.Mixins;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.util.Optional;
import java.util.jar.JarFile;

/** Applies the packaged POI mixins to the production server and exercises its two scan lookups. */
public final class PoiProductionMixinApplicationHarness {
    private PoiProductionMixinApplicationHarness() { }

    public static void main(String[] args) throws Exception {
        require(args.length == 1, "expected the packaged Flora Trades JAR path");
        Path expectedJar = Path.of(args[0]).toRealPath();
        try (JarFile archive = new JarFile(expectedJar.toFile())) {
            require("official".equals(archive.getManifest().getMainAttributes()
                            .getValue("Fabric-Mapping-Namespace")),
                    "packaged Flora Trades JAR is not in the official Minecraft namespace");
        }

        Knot knot = new Knot(EnvType.SERVER);
        ClassLoader loader = knot.init(new String[] {"--nogui"});
        URL config = loader.getResource("mynx_flora_trades.mixins.json");
        require(config != null && config.openConnection() instanceof JarURLConnection,
                "packaged Flora Trades mixin config is missing");
        Path actualJar = Path.of(((JarURLConnection) config.openConnection()).getJarFileURL().toURI()).toRealPath();
        require(expectedJar.equals(actualJar), "mixin config did not load from the packaged Flora Trades JAR");
        Mixins.addConfiguration("mynx_flora_trades.mixins.json");

        // The production server bootstrap is required before loading Blocks/PoiTypes outside a
        // running Minecraft server, just as it is for Minecraft's own registry construction.
        Class.forName("net.minecraft.SharedConstants", true, loader).getMethod("tryDetectVersion").invoke(null);
        Class.forName("net.minecraft.server.Bootstrap", true, loader).getMethod("bootStrap").invoke(null);
        Class<?> poiTypes = Class.forName("net.minecraft.world.entity.ai.village.poi.PoiTypes", true, loader);
        Class<?> blocks = Class.forName("net.minecraft.world.level.block.Blocks", true, loader);
        Class<?> block = Class.forName("net.minecraft.world.level.block.Block", true, loader);
        Class<?> state = Class.forName("net.minecraft.world.level.block.state.BlockState", true, loader);
        Method defaultState = block.getMethod("defaultBlockState");
        Method hasPoi = poiTypes.getMethod("hasPoi", state);
        Method forState = poiTypes.getMethod("forState", state);

        assertFloristLookup(blockState(blocks, "FLOWER_POT", defaultState), hasPoi, forState);
        assertFloristLookup(blockState(blocks, "POTTED_POPPY", defaultState), hasPoi, forState);
        assertFalse((Boolean) hasPoi.invoke(null, blockState(blocks, "STONE", defaultState)),
                "unrelated blocks must not become Florist POIs");

        System.out.println("Production Minecraft 26.2 Florist POI lookup application passed for "
                + expectedJar.getFileName());
    }

    private static Object blockState(Class<?> blocks, String fieldName, Method defaultState) throws Exception {
        Field field = blocks.getField(fieldName);
        return defaultState.invoke(field.get(null));
    }

    private static void assertFloristLookup(Object state, Method hasPoi, Method forState) throws Exception {
        require((Boolean) hasPoi.invoke(null, state), "PoiManager's hasPoi scan fast-path rejected a flower pot");
        Optional<?> type = (Optional<?>) forState.invoke(null, state);
        require(type.isPresent(), "PoiManager's forState lookup rejected a flower pot");
        Method name = type.orElseThrow().getClass().getMethod("getRegisteredName");
        require("mynx_flora_trades:florist".equals(name.invoke(type.orElseThrow())),
                "flower pot did not resolve to the Florist POI");
    }

    private static void assertFalse(boolean value, String message) {
        require(!value, message);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
