package com.crispytwig.naturalist.startup;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RenderMixinIntegrationTest {
    private static final String CLIENT = "com/crispytwig/naturalist/client/";
    private static final String MIXIN = "com/crispytwig/naturalist/mixin/";

    private static ClassNode read(String name) throws Exception {
        try (var input = RenderMixinIntegrationTest.class.getResourceAsStream("/" + name + ".class")) {
            assertNotNull(input, name);
            var node = new ClassNode();
            new ClassReader(input).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return node;
        }
    }

    @Test
    void declaredMixinPackagesContainOnlyMixinClasses() throws Exception {
        Path staged = Path.of(System.getProperty("naturalist.stagedResources"));
        Path classes = Path.of(System.getProperty("naturalist.classes"));
        for (String config : List.of("naturalist.mixins.json", "naturalist.fieldguide.mixins.json")) {
            String prefix = JsonParser.parseString(Files.readString(staged.resolve(config)))
                    .getAsJsonObject().get("package").getAsString().replace('.', '/');
            try (var files = Files.walk(classes.resolve(prefix))) {
                for (var path : files.filter(path -> path.toString().endsWith(".class")).toList()) {
                    String name = classes.relativize(path).toString().replace('\\', '/').replace(".class", "");
                    var node = read(name);
                    var annotations = new ArrayList<AnnotationNode>();
                    if (node.visibleAnnotations != null) annotations.addAll(node.visibleAnnotations);
                    if (node.invisibleAnnotations != null) annotations.addAll(node.invisibleAnnotations);
                    assertTrue(annotations.stream().anyMatch(a -> a.desc.equals("Lorg/spongepowered/asm/mixin/Mixin;")),
                            "Ordinary helper in a declared mixin package: " + name);
                }
            }
        }
    }

    @Test
    void allRenderHelperCallsResolveToPublicClassesOutsideMixinPackages() throws Exception {
        Set<String> helpers = Set.of("NaturalistRenderEntityLookup", "NaturalistParrotRenderStateLookup");
        int calls = 0;
        for (String name : List.of("EntityRenderDispatcherMixin", "PlayerModelMixin", "WolfModelMixin",
                "ParrotOnShoulderLayerMixin", "ParrotModelMixin")) {
            for (var method : read(MIXIN + name).methods) {
                for (var instruction : method.instructions) {
                    if (instruction instanceof MethodInsnNode call && helpers.stream().anyMatch(call.owner::endsWith)) {
                        assertTrue(call.owner.startsWith(CLIENT), call.owner);
                        var owner = read(call.owner);
                        assertTrue((owner.access & Opcodes.ACC_PUBLIC) != 0, call.owner);
                        var target = owner.methods.stream().filter(m -> m.name.equals(call.name) && m.desc.equals(call.desc))
                                .findFirst().orElseThrow();
                        assertEquals(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                                target.access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC));
                        calls++;
                    }
                }
            }
        }
        assertTrue(calls >= 8, "Render integrations must still call the live helpers");
    }

    @Test
    @SuppressWarnings("unchecked")
    void dispatcherStillRemembersTheReturnedStateAndUsesItToSelectBakedRiders() throws Exception {
        String descriptor = "(Lnet/minecraft/world/entity/Entity;F)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;";
        assertTrue(read("net/minecraft/client/renderer/entity/EntityRenderDispatcher").methods.stream()
                .anyMatch(m -> m.name.equals("extractEntity") && m.desc.equals(descriptor)));
        var mixin = read(MIXIN + "EntityRenderDispatcherMixin");
        var remember = mixin.methods.stream().filter(m -> m.name.equals("naturalist$rememberSource")).findFirst().orElseThrow();
        var injection = remember.visibleAnnotations.stream().filter(a -> a.desc.endsWith("/Inject;")).findFirst().orElseThrow();
        assertEquals(List.of("extractEntity" + descriptor), value(injection, "method"));
        var point = ((List<AnnotationNode>) value(injection, "at")).getFirst();
        assertEquals("RETURN", value(point, "value"));
        List<String> calls = new ArrayList<>();
        for (var instruction : remember.instructions) {
            if (instruction instanceof MethodInsnNode call) calls.add(call.owner + "." + call.name + call.desc);
        }
        assertEquals(List.of(
                "org/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable.getReturnValue()Ljava/lang/Object;",
                CLIENT + "NaturalistRenderEntityLookup.remember(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lnet/minecraft/world/entity/Entity;F)V"), calls);
        var skip = mixin.methods.stream().filter(m -> m.name.equals("naturalist$skipBakedRider")).findFirst().orElseThrow();
        Set<String> requiredCalls = new java.util.HashSet<>(Set.of("source", "getVehicle", "cancel"));
        boolean checksMount = false;
        for (var instruction : skip.instructions) {
            if (instruction instanceof MethodInsnNode call) requiredCalls.remove(call.name);
            if (instruction instanceof TypeInsnNode type && type.getOpcode() == Opcodes.INSTANCEOF && type.desc.endsWith("/IKMount")) checksMount = true;
        }
        assertTrue(requiredCalls.isEmpty());
        assertTrue(checksMount);
    }

    private static Object value(AnnotationNode annotation, String key) {
        int index = annotation.values.indexOf(key);
        assertTrue(index >= 0, key);
        return annotation.values.get(index + 1);
    }
}
