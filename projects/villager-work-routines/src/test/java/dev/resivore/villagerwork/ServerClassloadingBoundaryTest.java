package dev.resivore.villagerwork;

import com.google.gson.JsonParser;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import static org.junit.jupiter.api.Assertions.*;

/** Static linkage and isolated JVM load checks, never a Minecraft server/runtime trial. */
class ServerClassloadingBoundaryTest {
    private static final String OWN = "dev/resivore/villagerwork/";

    @Test void commonEntrypointAndMixinClosureLoadsWithoutClientOrRibbitsClasses() throws Exception {
        Path classes = Path.of("build/classes/java/main");
        var graph = new HashMap<String, Set<String>>();
        try (var files = Files.walk(classes)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".class")).toList()) {
                var references = new HashSet<String>();
                var reader = new ClassReader(Files.readAllBytes(file));
                reader.accept(new ClassRemapper(new ClassWriter(0), new Remapper() {
                    @Override public String map(String name) { references.add(name); return name; }
                }), 0);
                graph.put(reader.getClassName(), references);
                assertTrue(references.stream().noneMatch(n -> n.startsWith("com/yungnickyoung/minecraft/ribbits/")),
                        reader.getClassName() + " must not link Ribbits implementation classes");
            }
        }
        var metadata = JsonParser.parseString(Files.readString(Path.of("src/main/resources/fabric.mod.json"))).getAsJsonObject();
        var mixins = JsonParser.parseString(Files.readString(Path.of("src/main/resources/villager_work_routines.mixins.json"))).getAsJsonObject();
        var queue = new ArrayDeque<String>();
        metadata.getAsJsonObject("entrypoints").getAsJsonArray("main")
                .forEach(v -> queue.add(v.getAsString().replace('.', '/')));
        mixins.getAsJsonArray("mixins").forEach(v -> queue.add(
                (mixins.get("package").getAsString() + "." + v.getAsString()).replace('.', '/')));
        var reached = new HashSet<String>();
        while (!queue.isEmpty()) {
            String name = queue.remove();
            if (!reached.add(name)) continue;
            assertFalse(forbidden(name), "dedicated-server closure reaches " + name);
            if (graph.containsKey(name)) queue.addAll(graph.get(name));
        }
        assertTrue(reached.contains(OWN + "WorkCoordinator"));
        assertFalse(reached.contains(OWN + "FrogVillagerRodPose"));
        try (var loader = new URLClassLoader(new java.net.URL[]{classes.toUri().toURL()}, getClass().getClassLoader()) {
            @Override protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (forbidden(name.replace('.', '/'))) throw new ClassNotFoundException("server excludes " + name);
                if (!name.startsWith(OWN.replace('/', '.'))) return super.loadClass(name, resolve);
                Class<?> result = findLoadedClass(name);
                if (result == null) result = findClass(name);
                if (resolve) resolveClass(result);
                return result;
            }
        }) {
            for (String name : reached) {
                if (graph.containsKey(name)) assertNotNull(Class.forName(name.replace('/', '.'), false, loader));
            }
            assertThrows(ClassNotFoundException.class, () -> loader.loadClass("net.minecraft.client.Minecraft"));
        }
    }

    private static boolean forbidden(String name) {
        return name.startsWith("net/minecraft/client/") || name.startsWith("com/mojang/blaze3d/")
                || name.startsWith("com/geckolib/") || name.startsWith("com/yungnickyoung/minecraft/ribbits/")
                || name.startsWith(OWN + "client/") || name.startsWith(OWN + "mixin/client/");
    }
}
