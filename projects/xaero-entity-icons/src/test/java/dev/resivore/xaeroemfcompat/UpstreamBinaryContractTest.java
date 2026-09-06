package dev.resivore.xaeroemfcompat;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpstreamBinaryContractTest {
    private static final Path XAERO = propertyPath("xaeroJar");
    private static final Path WORLD_MAP = propertyPath("worldMapJar");
    private static final Path EMF = propertyPath("emfJar");
    private static final Path ETF = propertyPath("etfJar");
    private static final Path FRESH_ANIMATIONS = propertyPath("freshAnimationsPack");

    private static final String COMPILE_DESCRIPTOR =
            "(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;"
                    + "Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V";
    private static final String RENDER_DESCRIPTOR =
            "(Lcom/mojang/blaze3d/vertex/PoseStack;"
                    + "Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V";

    @Test
    void exactAuditedArtifactsArePresent() throws Exception {
        assertArtifact(XAERO, 2_221_925L,
                "69284892D2EB853C9AEFA85A4C9B74232C322DA00207994C67AB8AEED8A64048");
        assertArtifact(WORLD_MAP, 1_473_719L,
                "D55EF45C559AE0ADCF66D894C022F61D9D921629B0C885D04AA00424546A2389");
        assertArtifact(EMF, 587_342L,
                "876A3E4FFDA021A6266DF87208F2D9980322CF86223D4FE1E313CA996631F115");
        assertArtifact(ETF, 762_131L,
                "F469BC914302A13A5C767296623DF60FB0CC3D4E4A02A77C56541A733AD36E3A");
        assertArtifact(FRESH_ANIMATIONS, 645_816L,
                "CF9F17A2977E171B33CB0B598BC4357DD0383E09C10D5F768FF17C12D0A028EE");
    }

    @Test
    void xaeroStillInjectsVanillaCompileAndExposesTheNarrowCallback() throws IOException {
        ClassNode mixin = readClass(XAERO, "xaero/common/mixin/MixinModelPart.class");
        MethodNode handler = method(mixin, "onRender",
                COMPILE_DESCRIPTOR.substring(0, COMPILE_DESCRIPTOR.length() - 2)
                        + "Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V");

        assertEquals(1, calls(handler,
                "xaero/common/core/XaeroMinimapCore",
                "onEntityIconsModelPartRenderDetection",
                "(Lnet/minecraft/client/model/geom/ModelPart;I)V"));
        assertTrue(annotationStrings(handler).contains("compile" + COMPILE_DESCRIPTOR));

        ClassNode core = readClass(XAERO, "xaero/common/core/XaeroMinimapCore.class");
        MethodNode callback = method(core, "onEntityIconsModelPartRenderDetection",
                "(Lnet/minecraft/client/model/geom/ModelPart;I)V");
        assertTrue((callback.access & Opcodes.ACC_PUBLIC) != 0);
        assertTrue((callback.access & Opcodes.ACC_STATIC) != 0);
        assertEquals(1, calls(callback,
                "xaero/common/minimap/render/MinimapFBORenderer",
                "onEntityIconModelPartRenderTrace",
                "(Lnet/minecraft/client/model/geom/ModelPart;I)V"));
    }

    @Test
    void emfCompileIsTheConfirmedBypassAndRenderLikeVanillaEntersItOnce() throws IOException {
        ClassNode emfPart = readClass(EMF,
                "traben/entity_model_features/models/parts/EMFModelPart.class");
        assertEquals(69, emfPart.version);

        MethodNode compile = method(emfPart, "compile", COMPILE_DESCRIPTOR);
        assertTrue((compile.access & Opcodes.ACC_PUBLIC) != 0);
        assertEquals(0, calls(compile,
                "net/minecraft/client/model/geom/ModelPart", "compile", COMPILE_DESCRIPTOR));
        assertEquals(1, calls(compile,
                "net/minecraft/client/model/geom/ModelPart$Cube", "compile", COMPILE_DESCRIPTOR));

        MethodNode renderLikeVanilla = method(emfPart, "renderLikeVanilla", RENDER_DESCRIPTOR);
        assertEquals(1, calls(renderLikeVanilla,
                "traben/entity_model_features/models/parts/EMFModelPart",
                "compile", COMPILE_DESCRIPTOR));
    }

    @Test
    void xaeroPostCallbackSelectionRejectsTheEmptyCanonicalHeadIdentity() throws IOException {
        ClassNode partPrerenderer = readClass(XAERO,
                "xaero/hud/minimap/radar/icon/creator/render/form/model/part/"
                        + "RadarIconModelPartPrerenderer.class");
        MethodNode renderPart = method(partPrerenderer, "renderPart",
                "(Lcom/mojang/blaze3d/vertex/PoseStack;"
                        + "Lcom/mojang/blaze3d/vertex/VertexConsumer;"
                        + "Lnet/minecraft/client/model/geom/ModelPart;"
                        + "Lnet/minecraft/client/model/geom/ModelPart;"
                        + "Lxaero/hud/minimap/radar/icon/creator/render/form/model/part/"
                        + "RadarIconModelPartPrerenderer$Parameters;)V");
        assertEquals(1, calls(renderPart,
                "xaero/hud/minimap/radar/icon/creator/render/trace/ModelRenderTrace",
                "getModelPartRenderInfo",
                "(Lnet/minecraft/client/model/geom/ModelPart;)"
                        + "Lxaero/hud/minimap/radar/icon/creator/render/trace/ModelPartRenderTrace;"));
        assertEquals(1, calls(renderPart,
                "xaero/hud/minimap/radar/icon/creator/render/form/model/part/ModelPartUtil",
                "hasCubes", "(Lnet/minecraft/client/model/geom/ModelPart;)Z"));
        assertEquals(1, calls(renderPart,
                "net/minecraft/client/model/geom/ModelPart",
                "render", RENDER_DESCRIPTOR));
        assertEquals(1, calls(renderPart,
                "xaero/hud/minimap/radar/icon/creator/render/form/model/part/ModelPartUtil",
                "getBiggestCuboid",
                "(Lnet/minecraft/client/model/geom/ModelPart;)"
                        + "Lnet/minecraft/client/model/geom/ModelPart$Cube;"));

        ClassNode modelPrerenderer = readClass(XAERO,
                "xaero/hud/minimap/radar/icon/creator/render/form/model/"
                        + "RadarIconModelPrerenderer.class");
        MethodNode hierarchy = modelPrerenderer.methods.stream()
                .filter(candidate -> candidate.name.equals("renderHierarchicalModel"))
                .findFirst().orElseThrow();
        assertTrue(constants(hierarchy).contains("head"));
        assertTrue(constants(hierarchy).contains("head_parts"));
        assertTrue(callsNamed(hierarchy,
                "xaero/hud/minimap/radar/icon/creator/render/form/model/"
                        + "RadarIconModelPrerenderer", "renderPart") >= 1);

        MethodNode ageable = modelPrerenderer.methods.stream()
                .filter(candidate -> candidate.name.equals("renderAgeableListModel"))
                .findFirst().orElseThrow();
        assertEquals(1, callsNamed(ageable,
                "xaero/hud/minimap/radar/icon/creator/render/form/model/"
                        + "RadarIconModelPrerenderer", "renderPart"));
    }

    @Test
    void c7RetriesTheExactFailedCacheReadOnlyWhenXaeroCanRecreateIt() throws Exception {
        ClassNode manager = readClass(XAERO,
                "xaero/hud/minimap/radar/icon/RadarIconManager.class");
        MethodNode get = manager.methods.stream()
                .filter(candidate -> candidate.name.equals("get")
                        && candidate.desc.contains("RadarIconDefinition"))
                .findFirst().orElseThrow();
        int cacheGet = firstCallIndex(get,
                "xaero/hud/minimap/radar/icon/cache/RadarIconEntityCache", "get");
        int creator = firstCallIndex(get,
                "xaero/hud/minimap/radar/icon/creator/RadarIconCreator", "create");
        assertTrue(cacheGet >= 0 && creator > cacheGet,
                "Xaero reads the cached result before it can recreate an icon");

        Path source = Path.of(System.getProperty("projectRoot")).resolve(
                "src/main/java/dev/resivore/xaeroemfcompat/mixin/RadarIconManagerMixin.java");
        String c7 = Files.readString(source);
        assertTrue(c7.contains("@Redirect"));
        assertTrue(c7.contains("RadarIconEntityCache;get"));
        assertTrue(c7.contains("retryFailedOnceAtPrerender(type,key.getVariant(),canPrerender)"));
        assertTrue(c7.contains("FAILED_RETRY_DEFERRED_NO_PRERENDER"));
        assertTrue(c7.contains("MANAGER_RETURNED_NULL"));

        String cacheMixin = Files.readString(Path.of(System.getProperty("projectRoot")).resolve(
                "src/main/java/dev/resivore/xaeroemfcompat/mixin/RadarIconEntityCacheMixin.java"));
        assertFalse(cacheMixin.contains("method=\"get\""));
        assertFalse(cacheMixin.contains("setReturnValue"));
    }

    @Test
    void emfNonAttachedJemPartsClearTheMappedVanillaCubes() throws IOException {
        ClassNode root = readClass(EMF,
                "traben/entity_model_features/models/parts/EMFModelPartRoot.class");
        var vanillaRoot = root.fields.stream()
                .filter(field -> field.name.equals("vanillaRoot"))
                .findFirst().orElseThrow();
        assertEquals("Lnet/minecraft/client/model/geom/ModelPart;", vanillaRoot.desc);
        assertTrue((vanillaRoot.access & Opcodes.ACC_PUBLIC) != 0);
        assertTrue((vanillaRoot.access & Opcodes.ACC_FINAL) != 0);
        MethodNode addVariant = method(root, "addVariantOfJem",
                "(Ltraben/entity_model_features/models/jem_objects/EMFJemData;I)V");
        assertTrue(calls(addVariant, "java/util/List", "of", "()Ljava/util/List;") >= 1);
        assertTrue(fieldWrites(addVariant,
                "traben/entity_model_features/models/parts/EMFModelPartVanilla", "cubes") >= 1);

        ClassNode partData = readClass(EMF,
                "traben/entity_model_features/models/jem_objects/EMFPartData.class");
        assertTrue(partData.fields.stream().anyMatch(field -> field.name.equals("attach")
                && field.desc.equals("Z")));
    }

    @Test
    void emfVanillaRootFamilyIsAnEmfPartAndKeepsAPublicRouteToTheRetainedRoot() throws IOException {
        ClassNode vanilla = readClass(EMF,
                "traben/entity_model_features/models/parts/EMFModelPartVanilla.class");
        assertEquals("traben/entity_model_features/models/parts/EMFModelPartWithState", vanilla.superName);
        ClassNode withState = readClass(EMF,
                "traben/entity_model_features/models/parts/EMFModelPartWithState.class");
        assertEquals("traben/entity_model_features/models/parts/EMFModelPart", withState.superName);
        ClassNode emfPart = readClass(EMF,
                "traben/entity_model_features/models/parts/EMFModelPart.class");
        assertTrue(emfPart.methods.stream().anyMatch(method -> method.name.equals("getRoot")
                && method.desc.equals("()Ltraben/entity_model_features/models/parts/EMFModelPartRoot;")));

        String resolver = Files.readString(Path.of(System.getProperty("projectRoot"))
                .resolve("src/main/java/dev/resivore/xaeroemfcompat/EmfIconPartResolver.java"));
        assertTrue(resolver.contains("EMFModelPartVanilla"));
        assertTrue(resolver.contains("getMethod(\"getRoot\")"));
    }

    @Test
    void xaeroCentersFromOnlyTheCanonicalPartsDirectLargestCube() throws IOException {
        ClassNode util = readClass(XAERO,
                "xaero/hud/minimap/radar/icon/creator/render/form/model/part/"
                        + "ModelPartUtil.class");
        MethodNode biggest = method(util, "getBiggestCuboid",
                "(Lnet/minecraft/client/model/geom/ModelPart;)"
                        + "Lnet/minecraft/client/model/geom/ModelPart$Cube;");
        assertEquals(1, calls(biggest,
                "xaero/hud/minimap/radar/icon/creator/render/form/model/part/ModelPartUtil",
                "getCubes", "(Lnet/minecraft/client/model/geom/ModelPart;)Ljava/util/List;"));
        assertEquals(0, callsNamed(biggest,
                "xaero/hud/minimap/radar/icon/creator/render/form/model/part/ModelPartUtil",
                "getChildren"));
        assertEquals(0, callsNamed(biggest,
                "xaero/hud/minimap/radar/icon/creator/render/form/model/part/ModelPartUtil",
                "getBiggestCuboid"));
    }

    @Test
    void worldMapAndEtfDoNotOwnThisEntityIconCompileSeam() throws IOException {
        try (ZipFile worldMap = new ZipFile(WORLD_MAP.toFile())) {
            assertNull(worldMap.getEntry(
                    "xaero/hud/minimap/radar/icon/creator/render/trace/EntityRenderTracer.class"));
            assertNull(worldMap.getEntry("xaero/common/mixin/MixinModelPart.class"));
        }

        ClassNode etfMixin = readClass(ETF,
                "traben/entity_texture_features/mixin/mixins/MixinModelPart.class");
        assertFalse(annotationStrings(etfMixin).stream().anyMatch(value -> value.contains("compile")));
        assertFalse(etfMixin.methods.stream().flatMap(method -> methodCalls(method).stream())
                .anyMatch(call -> call.owner.startsWith("xaero/")));
    }

    @Test
    void freshAnimationsContainsRepresentativeEmfModels() throws IOException {
        try (ZipFile pack = new ZipFile(FRESH_ANIMATIONS.toFile())) {
            String metadata = readUtf8(pack, "pack.mcmeta");
            assertTrue(metadata.contains("1.10.5 BETA"));
            assertTrue(metadata.contains("\"min_format\": 84"));

            for (String entity : List.of("pig", "creeper", "sheep", "horse", "turtle")) {
                String model = readUtf8(pack,
                        "assets/minecraft/optifine/cem/" + entity + ".jem");
                assertTrue(model.contains("\"models\":["), entity);
                assertTrue(model.contains("\"part\":"), entity);
            }
        }
    }

    private static Path propertyPath(String property) {
        return Path.of(Objects.requireNonNull(System.getProperty(property), property));
    }

    private static void assertArtifact(Path path, long size, String hash) throws Exception {
        assertEquals(size, Files.size(path), path.toString());
        assertEquals(hash, sha256(path), path.toString());
    }

    private static ClassNode readClass(Path jar, String entryName) throws IOException {
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            var entry = Objects.requireNonNull(zip.getEntry(entryName), entryName);
            try (InputStream stream = zip.getInputStream(entry)) {
                ClassNode node = new ClassNode();
                new org.objectweb.asm.ClassReader(stream).accept(node, 0);
                return node;
            }
        }
    }

    private static MethodNode method(ClassNode owner, String name, String descriptor) {
        return owner.methods.stream()
                .filter(candidate -> candidate.name.equals(name) && candidate.desc.equals(descriptor))
                .findFirst()
                .orElseThrow(() -> new AssertionError(owner.name + "." + name + descriptor));
    }

    private static long calls(MethodNode method, String owner, String name, String descriptor) {
        return methodCalls(method).stream()
                .filter(call -> call.owner.equals(owner)
                        && call.name.equals(name)
                        && call.desc.equals(descriptor))
                .count();
    }

    private static long callsNamed(MethodNode method, String owner, String name) {
        return methodCalls(method).stream()
                .filter(call -> call.owner.equals(owner) && call.name.equals(name))
                .count();
    }

    private static int firstCallIndex(MethodNode method, String owner, String name) {
        int index = 0;
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call
                    && call.owner.equals(owner) && call.name.equals(name)) {
                return index;
            }
            index++;
        }
        return -1;
    }

    private static long fieldWrites(MethodNode method, String owner, String name) {
        long result = 0;
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof FieldInsnNode field
                    && field.getOpcode() == Opcodes.PUTFIELD
                    && field.owner.equals(owner)
                    && field.name.equals(name)) {
                result++;
            }
        }
        return result;
    }

    private static List<String> constants(MethodNode method) {
        List<String> result = new ArrayList<>();
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof LdcInsnNode constant
                    && constant.cst instanceof String string) {
                result.add(string);
            }
        }
        return result;
    }

    private static List<MethodInsnNode> methodCalls(MethodNode method) {
        List<MethodInsnNode> calls = new ArrayList<>();
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call) {
                calls.add(call);
            }
        }
        return calls;
    }

    private static List<String> annotationStrings(MethodNode method) {
        List<String> values = new ArrayList<>();
        if (method.visibleAnnotations != null) {
            method.visibleAnnotations.forEach(annotation -> collectAnnotationStrings(annotation, values));
        }
        if (method.invisibleAnnotations != null) {
            method.invisibleAnnotations.forEach(annotation -> collectAnnotationStrings(annotation, values));
        }
        return values;
    }

    private static List<String> annotationStrings(ClassNode owner) {
        List<String> values = new ArrayList<>();
        if (owner.visibleAnnotations != null) {
            owner.visibleAnnotations.forEach(annotation -> collectAnnotationStrings(annotation, values));
        }
        if (owner.invisibleAnnotations != null) {
            owner.invisibleAnnotations.forEach(annotation -> collectAnnotationStrings(annotation, values));
        }
        owner.methods.forEach(method -> values.addAll(annotationStrings(method)));
        return values;
    }

    private static void collectAnnotationStrings(AnnotationNode annotation, List<String> result) {
        if (annotation.values == null) {
            return;
        }
        for (Object value : annotation.values) {
            collectAnnotationValue(value, result);
        }
    }

    private static void collectAnnotationValue(Object value, List<String> result) {
        if (value instanceof String string) {
            result.add(string);
        } else if (value instanceof AnnotationNode annotation) {
            collectAnnotationStrings(annotation, result);
        } else if (value instanceof List<?> list) {
            list.forEach(item -> collectAnnotationValue(item, result));
        }
    }

    private static String readUtf8(ZipFile zip, String entryName) throws IOException {
        var entry = Objects.requireNonNull(zip.getEntry(entryName), entryName);
        try (InputStream stream = zip.getInputStream(entry)) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String sha256(Path path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream stream = Files.newInputStream(path)) {
            byte[] buffer = new byte[8192];
            for (int read; (read = stream.read(buffer)) >= 0; ) {
                digest.update(buffer, 0, read);
            }
        }
        return HexFormat.of().withUpperCase().formatHex(digest.digest());
    }
}
