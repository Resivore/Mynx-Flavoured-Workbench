package com.crispytwig.naturalist.startup;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MonsterMixinDescriptorTest {
    private static ClassNode readClass(String path) throws Exception {
        try (var input = MonsterMixinDescriptorTest.class.getResourceAsStream("/" + path + ".class")) {
            assertNotNull(input, path);
            var node = new ClassNode();
            new ClassReader(input).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return node;
        }
    }

    private static Object value(AnnotationNode annotation, String key) {
        int index = annotation.values.indexOf(key);
        assertTrue(index >= 0, key);
        return annotation.values.get(index + 1);
    }

    private static MethodNode callback() throws Exception {
        return readClass("com/crispytwig/naturalist/mixin/MonsterMixin").methods.stream()
                .filter(method -> method.name.equals("onIsPreventingPlayerRest")).findFirst().orElseThrow();
    }

    @Test
    void callbackMatchesActualMinecraft26_2TargetArguments() throws Exception {
        var targets = readClass("net/minecraft/world/entity/monster/Monster").methods.stream()
                .filter(method -> method.name.equals("isPreventingPlayerRest")).toList();
        assertEquals(1, targets.size());
        var target = targets.getFirst();
        assertEquals("(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/player/Player;)Z",
                target.desc);
        var arguments = Type.getArgumentTypes(target.desc);
        var expected = Arrays.copyOf(arguments, arguments.length + 1);
        expected[arguments.length] = Type.getObjectType(
                "org/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable");
        assertEquals(Type.getMethodDescriptor(Type.VOID_TYPE, expected), callback().desc);
    }

    @Test
    @SuppressWarnings("unchecked")
    void restInjectionRemainsRequiredCancellableHead() throws Exception {
        var injection = callback().visibleAnnotations.stream()
                .filter(annotation -> annotation.desc.equals("Lorg/spongepowered/asm/mixin/injection/Inject;"))
                .findFirst().orElseThrow();
        assertEquals(List.of("isPreventingPlayerRest"), value(injection, "method"));
        assertEquals(true, value(injection, "cancellable"));
        var points = (List<AnnotationNode>) value(injection, "at");
        assertEquals(1, points.size());
        assertEquals("HEAD", value(points.getFirst(), "value"));
        assertFalse(injection.values.contains("require"));
        try (var input = getClass().getResourceAsStream("/naturalist.mixins.json")) {
            assertNotNull(input);
            var config = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
            assertTrue(config.get("required").getAsBoolean());
            assertEquals(1, config.getAsJsonObject("injectors").get("defaultRequire").getAsInt());
            assertTrue(config.getAsJsonArray("mixins").asList().stream()
                    .anyMatch(name -> name.getAsString().equals("MonsterMixin")));
        }
    }
}
