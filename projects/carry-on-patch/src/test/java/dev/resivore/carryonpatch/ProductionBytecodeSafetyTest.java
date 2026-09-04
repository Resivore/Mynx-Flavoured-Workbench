package dev.resivore.carryonpatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

class ProductionBytecodeSafetyTest {
    private static final Path PATCH = propertyPath("patchJar");
    private static final String MIXIN_DESC = "Lorg/spongepowered/asm/mixin/Mixin;";
    private static final String ACCESSOR_DESC = "Lorg/spongepowered/asm/mixin/gen/Accessor;";
    private static final String REDIRECT_DESC =
            "Lorg/spongepowered/asm/mixin/injection/Redirect;";
    private static final String AT_DESC = "Lorg/spongepowered/asm/mixin/injection/At;";

    @Test
    void productionBytecodeNeverReadsThrowingIdOrTouchesWorldNetworkOrNbt() throws Exception {
        try (ZipFile zip = new ZipFile(PATCH.toFile())) {
            for (String entry : productionClasses(zip)) {
                ClassNode owner = readClass(zip, entry);
                for (MethodNode method : owner.methods) {
                    for (MethodInsnNode call : methodCalls(method)) {
                        assertFalse(call.name.equals("getId"), location(owner, method, call));
                        assertFalse(call.name.equals("addFreshEntity"),
                                location(owner, method, call));
                        assertFalse(call.owner.startsWith("net/minecraft/network/")
                                        || call.owner.startsWith("net/fabricmc/fabric/api/networking/")
                                        || call.owner.startsWith("net/minecraft/nbt/"),
                                location(owner, method, call));
                    }
                }
            }
        }
    }

    @Test
    void patchAddsNoEntityHoldingStaticStateAndNoSecondRetentionLayer() throws Exception {
        try (ZipFile zip = new ZipFile(PATCH.toFile())) {
            for (String entry : productionClasses(zip)) {
                ClassNode owner = readClass(zip, entry);
                for (FieldNode field : owner.fields) {
                    if ((field.access & Opcodes.ACC_STATIC) != 0) {
                        String signature = field.signature == null ? "" : field.signature;
                        assertFalse(field.desc.contains("net/minecraft/world/entity/Entity")
                                        || signature.contains("net/minecraft/world/entity/Entity"),
                                owner.name + "." + field.name);
                    }
                }
            }

            ClassNode cache = readClass(zip,
                    "dev/resivore/carryonpatch/RenderIdAssigningEntityCache.class");
            assertEquals("java/util/HashMap", cache.superName);
            assertTrue(cache.fields.isEmpty(),
                    "the assigning cache must not retain entities outside inherited map entries");

            MethodNode put = method(cache, "put",
                    "(Ljava/lang/String;Lnet/minecraft/world/entity/Entity;)"
                            + "Lnet/minecraft/world/entity/Entity;");
            Set<String> calls = methodCalls(put).stream()
                    .map(call -> call.owner + "." + call.name + call.desc)
                    .collect(java.util.stream.Collectors.toSet());
            assertEquals(Set.of(
                    "dev/resivore/carryonpatch/mixin/EntityIdAccessor."
                            + "carryOnPatch$getRawId()I",
                    "dev/resivore/carryonpatch/RenderOnlyEntityIds."
                            + "selectId(ILjava/util/function/IntPredicate;)I",
                    "net/minecraft/world/entity/Entity.setId(I)V",
                    "java/util/HashMap.put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"),
                    calls);
        }
    }

    @Test
    void mixinsAreLimitedToRawIdAccessAndOrdinalZeroUpstreamCacheConstruction()
            throws Exception {
        try (ZipFile zip = new ZipFile(PATCH.toFile())) {
            ClassNode accessor = readClass(zip,
                    "dev/resivore/carryonpatch/mixin/EntityIdAccessor.class");
            AnnotationNode accessorMixin = annotation(accessor, MIXIN_DESC);
            List<?> mixinValues = annotationList(accessorMixin, "value");
            assertEquals(1, mixinValues.size());
            assertEquals("net.minecraft.world.entity.Entity",
                    ((Type) mixinValues.getFirst()).getClassName());
            MethodNode rawId = method(accessor, "carryOnPatch$getRawId", "()I");
            assertEquals("id", annotationValue(annotation(rawId, ACCESSOR_DESC), "value"));

            ClassNode external = readClass(zip,
                    "dev/resivore/carryonpatch/mixin/CarriedObjectFeatureRendererMixin.class");
            AnnotationNode externalMixin = annotation(external, MIXIN_DESC);
            assertEquals(List.of(
                            "org.chermew.grabandgo.client.render.CarriedObjectFeatureRenderer"),
                    annotationList(externalMixin, "targets"));
            assertEquals(Boolean.FALSE, annotationValue(externalMixin, "remap"));

            MethodNode handler = method(external, "carryOnPatch$installAssigningCache",
                    "()Ljava/util/HashMap;");
            assertTrue((handler.access & Opcodes.ACC_STATIC) != 0);
            AnnotationNode redirect = annotation(handler, REDIRECT_DESC);
            assertEquals(List.of("<clinit>()V"), annotationList(redirect, "method"));
            assertEquals(1, annotationValue(redirect, "require"));
            assertEquals(Boolean.FALSE, annotationValue(redirect, "remap"));

            AnnotationNode at = (AnnotationNode) annotationValue(redirect, "at");
            assertNotNull(at);
            assertEquals(AT_DESC, at.desc);
            assertEquals("NEW", annotationValue(at, "value"));
            assertEquals("Ljava/util/HashMap;", annotationValue(at, "target"));
            assertEquals(0, annotationValue(at, "ordinal"));
            assertEquals(Boolean.FALSE, annotationValue(at, "remap"));
        }
    }

    private static String location(ClassNode owner, MethodNode method, MethodInsnNode call) {
        return owner.name + "." + method.name + method.desc + " calls "
                + call.owner + "." + call.name + call.desc;
    }

    private static List<String> productionClasses(ZipFile zip) {
        return zip.stream()
                .filter(entry -> !entry.isDirectory()
                        && entry.getName().startsWith("dev/resivore/carryonpatch/")
                        && entry.getName().endsWith(".class"))
                .map(entry -> entry.getName())
                .toList();
    }

    private static List<MethodInsnNode> methodCalls(MethodNode method) {
        List<MethodInsnNode> result = new ArrayList<>();
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call) {
                result.add(call);
            }
        }
        return result;
    }

    private static AnnotationNode annotation(ClassNode owner, String descriptor) {
        List<AnnotationNode> annotations = new ArrayList<>();
        if (owner.visibleAnnotations != null) annotations.addAll(owner.visibleAnnotations);
        if (owner.invisibleAnnotations != null) annotations.addAll(owner.invisibleAnnotations);
        return annotations.stream()
                .filter(candidate -> candidate.desc.equals(descriptor))
                .findFirst()
                .orElseThrow(() -> new AssertionError(owner.name + " lacks " + descriptor));
    }

    private static AnnotationNode annotation(MethodNode owner, String descriptor) {
        List<AnnotationNode> annotations = new ArrayList<>();
        if (owner.visibleAnnotations != null) annotations.addAll(owner.visibleAnnotations);
        if (owner.invisibleAnnotations != null) annotations.addAll(owner.invisibleAnnotations);
        return annotations.stream()
                .filter(candidate -> candidate.desc.equals(descriptor))
                .findFirst()
                .orElseThrow(() -> new AssertionError(owner.name + owner.desc
                        + " lacks " + descriptor));
    }

    private static Object annotationValue(AnnotationNode annotation, String key) {
        if (annotation.values != null) {
            for (int index = 0; index < annotation.values.size(); index += 2) {
                if (annotation.values.get(index).equals(key)) {
                    return annotation.values.get(index + 1);
                }
            }
        }
        throw new AssertionError(annotation.desc + " lacks " + key);
    }

    private static List<?> annotationList(AnnotationNode annotation, String key) {
        Object value = annotationValue(annotation, key);
        return value instanceof List<?> list ? list : List.of(value);
    }

    private static MethodNode method(ClassNode owner, String name, String descriptor) {
        return owner.methods.stream()
                .filter(candidate -> candidate.name.equals(name)
                        && candidate.desc.equals(descriptor))
                .findFirst()
                .orElseThrow(() -> new AssertionError(owner.name + "." + name + descriptor));
    }

    private static ClassNode readClass(ZipFile zip, String entryName) throws IOException {
        var entry = Objects.requireNonNull(zip.getEntry(entryName), entryName);
        try (InputStream input = zip.getInputStream(entry)) {
            ClassNode result = new ClassNode();
            new org.objectweb.asm.ClassReader(input).accept(result, 0);
            return result;
        }
    }

    private static Path propertyPath(String property) {
        return Path.of(Objects.requireNonNull(System.getProperty(property), property));
    }
}
