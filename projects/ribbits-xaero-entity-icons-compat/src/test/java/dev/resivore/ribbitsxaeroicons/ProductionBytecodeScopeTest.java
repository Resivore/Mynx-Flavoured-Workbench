package dev.resivore.ribbitsxaeroicons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

class ProductionBytecodeScopeTest {
    private static final Path PATCH = Path.of(Objects.requireNonNull(
            System.getProperty("patchJar"), "patchJar"));
    private static final String MIXIN = "Lorg/spongepowered/asm/mixin/Mixin;";
    private static final String REDIRECT =
            "Lorg/spongepowered/asm/mixin/injection/Redirect;";
    private static final String INJECT =
            "Lorg/spongepowered/asm/mixin/injection/Inject;";
    private static final String ACCESSOR =
            "Lorg/spongepowered/asm/mixin/gen/Accessor;";

    @Test
    void creatorMixinWrapsOnlyTheExactRadarIconFormPrerendererInvocation() throws Exception {
        ClassNode mixin = readClass(
                "dev/resivore/ribbitsxaeroicons/mixin/RadarIconCreatorMixin.class");
        assertEquals(List.of("xaero.hud.minimap.radar.icon.creator.RadarIconCreator"),
                mixinTargets(mixin));

        List<MethodNode> redirects = methodsAnnotated(mixin, REDIRECT);
        assertEquals(1, redirects.size());
        AnnotationNode annotation = annotation(redirects.getFirst(), REDIRECT);
        List<String> targetMethods = stringValues(annotation, "method");
        assertEquals(1, targetMethods.size());
        assertTrue(targetMethods.getFirst().startsWith("create("), targetMethods::toString);
        AnnotationNode at = nestedAnnotation(annotation, "at");
        assertEquals("INVOKE", value(at, "value"));
        String target = Objects.toString(value(at, "target"));
        assertEquals(
                "Lxaero/hud/minimap/radar/icon/definition/form/RadarIconForm;"
                        + "getPrerenderer()"
                        + "Lxaero/hud/minimap/radar/icon/creator/render/form/"
                        + "IRadarIconFormPrerenderer;",
                target);
        assertEquals(1, value(annotation, "require"));

        List<MethodInsnNode> calls = methodCalls(redirects.getFirst());
        assertEquals(1L, calls.stream()
                .filter(call -> call.owner.equals(
                        "xaero/hud/minimap/radar/icon/definition/form/RadarIconForm")
                        && call.name.equals("getPrerenderer"))
                .count());
        assertTrue(calls.stream().anyMatch(call -> call.owner.equals(
                "dev/resivore/ribbitsxaeroicons/GeoAwareFormPrerenderer")));
        assertEquals(1L, calls.stream()
                .filter(call -> call.owner.equals(
                        "dev/resivore/ribbitsxaeroicons/GeoIconProviders")
                        && call.name.equals("find"))
                .count());
        assertFalse(calls.stream().anyMatch(call ->
                call.name.equals("requiresEntityModel") || call.name.equals("prerender")),
                "creator ownership detection must not execute the upstream prerenderer");
        assertFalse(calls.stream().anyMatch(call -> call.name.equals("getEntityRendererModel")));
    }

    @Test
    void cacheAccessorExposesOnlyXaerosExactOuterEntityCacheMap() throws Exception {
        ClassNode accessor = readClass(
                "dev/resivore/ribbitsxaeroicons/mixin/RadarIconCacheAccessor.class");
        assertEquals(List.of("xaero.hud.minimap.radar.icon.cache.RadarIconCache"),
                mixinTargets(accessor));
        assertTrue((accessor.access & Opcodes.ACC_INTERFACE) != 0);

        List<MethodNode> methods = methodsAnnotated(accessor, ACCESSOR);
        assertEquals(1, methods.size());
        MethodNode method = methods.getFirst();
        assertEquals("ribbitsXaeroIcons$getIconCacheMap", method.name);
        assertEquals("()Ljava/util/Map;", method.desc);
        assertEquals("iconCacheMap", value(annotation(method, ACCESSOR), "value"));
    }

    @Test
    void variantHandlerMixinWrapsOnlyTheExactEntityVariantInvocation() throws Exception {
        ClassNode mixin = readClass(
                "dev/resivore/ribbitsxaeroicons/mixin/RadarIconVariantHandlerMixin.class");
        assertEquals(List.of(
                        "xaero.hud.minimap.radar.icon.cache.id.variant.RadarIconVariantHandler"),
                mixinTargets(mixin));

        List<MethodNode> injectors = methodsAnnotated(mixin, INJECT);
        assertEquals(1, injectors.size());
        AnnotationNode inject = annotation(injectors.getFirst(), INJECT);
        List<String> targetMethods = stringValues(inject, "method");
        assertEquals(1, targetMethods.size());
        assertTrue(targetMethods.getFirst().startsWith("getEntityVariant("), targetMethods::toString);
        assertEquals("RETURN", value(nestedAnnotation(inject, "at"), "value"));
        assertEquals(true, value(inject, "cancellable"));
        assertEquals(1, value(inject, "require"));
        List<MethodInsnNode> calls = methodCalls(injectors.getFirst());
        assertTrue(calls.stream().anyMatch(call ->
                call.owner.equals("dev/resivore/ribbitsxaeroicons/RibbitCacheVariant")));
        assertTrue(calls.stream().anyMatch(call ->
                call.owner.equals("dev/resivore/ribbitsxaeroicons/RibbitGeoIconProvider")
                        && call.name.equals("owns")));
        assertTrue(calls.stream().anyMatch(call ->
                call.owner.equals("dev/resivore/ribbitsxaeroicons/GeoIconProviders")
                        && call.name.equals("cacheIdentityForOwned")));
        assertFalse(calls.stream().anyMatch(call -> call.name.equals("find")),
                "exact Ribbits variants must keep one record class even on provider failure");
    }

    @Test
    void managerMixinInvalidatesOnlyOwnedEntriesOnExactResourceReset() throws Exception {
        ClassNode mixin = readClass(
                "dev/resivore/ribbitsxaeroicons/mixin/RadarIconManagerMixin.class");
        assertEquals(List.of("xaero.hud.minimap.radar.icon.RadarIconManager"),
                mixinTargets(mixin));

        List<MethodNode> injectors = methodsAnnotated(mixin, INJECT);
        assertEquals(1, injectors.size());
        AnnotationNode inject = annotation(injectors.getFirst(), INJECT);
        assertEquals(List.of("resetResources()V"), stringValues(inject, "method"));
        assertEquals("TAIL", value(nestedAnnotation(inject, "at"), "value"));
        assertEquals(1, value(inject, "require"));
        List<MethodInsnNode> resetCalls = methodCalls(injectors.getFirst());
        assertEquals(1L, resetCalls.stream()
                .filter(call -> call.owner.equals("dev/resivore/ribbitsxaeroicons/ReloadGeneration")
                        && call.name.equals("invalidate"))
                .count());
        assertEquals(1L, resetCalls.stream()
                .filter(call -> call.owner.equals("dev/resivore/ribbitsxaeroicons/ReloadGeneration")
                        && call.name.equals("evictRibbitOuterCache"))
                .count());
        assertEquals(1L, resetCalls.stream()
                .filter(call -> call.owner.equals(
                        "dev/resivore/ribbitsxaeroicons/mixin/RadarIconCacheAccessor")
                        && call.name.equals("ribbitsXaeroIcons$getIconCacheMap"))
                .count());

        List<MethodInsnNode> allManagerCalls = methodCalls(mixin);
        assertEquals(1L, allManagerCalls.stream()
                .filter(call -> call.owner.equals("net/minecraft/world/entity/EntityType")
                        && call.name.equals("getKey"))
                .count());
        assertFalse(allManagerCalls.stream().anyMatch(call ->
                call.owner.startsWith("java/util/")
                        && (call.name.equals("clear") || call.name.equals("remove"))));
        assertFalse(classPoolText(mixin).contains("RibbitCacheVariant"));

        ClassNode reload = readClass(
                "dev/resivore/ribbitsxaeroicons/ReloadGeneration.class");
        MethodNode outerEviction = reload.methods.stream()
                .filter(method -> method.name.equals("evictRibbitOuterCache"))
                .findFirst().orElseThrow();
        List<MethodInsnNode> evictionCalls = methodCalls(outerEviction);
        assertEquals(1L, evictionCalls.stream()
                .filter(call -> call.name.equals("keySet"))
                .count());
        assertEquals(1L, evictionCalls.stream()
                .filter(call -> call.name.equals("removeIf"))
                .count());
        assertTrue(classPoolText(reload).contains("RibbitGeoIconProvider"));
    }

    @Test
    void productionBytecodeReferencesOnlyClientRenderAndNarrowIconSeams() throws Exception {
        try (ZipFile zip = new ZipFile(PATCH.toFile())) {
            for (String entry : productionClasses(zip)) {
                byte[] bytes;
                try (InputStream input = zip.getInputStream(
                        Objects.requireNonNull(zip.getEntry(entry), entry))) {
                    bytes = input.readAllBytes();
                }
                String pool = new String(bytes, StandardCharsets.ISO_8859_1);
                String folded = pool.toLowerCase(Locale.ROOT);
                assertFalse(pool.contains("traben/entity_model_features"), entry);
                String nonResourceManagerPool = pool.replace(
                        "net/minecraft/server/packs/resources/ResourceManager", "");
                assertFalse(nonResourceManagerPool.contains("net/minecraft/server"), entry);
                assertFalse(pool.contains("net/minecraft/network"), entry);
                assertFalse(pool.contains("fabric/api/networking"), entry);
                assertFalse(pool.contains("SubmitNodeCollector"), entry);
                assertFalse(pool.contains("com/geckolib/renderer/layer/"), entry);
                assertFalse(folded.contains("assets/ribbits/geckolib/models"), entry);
                assertFalse(folded.contains("textures/entity"), entry);

                ClassNode owner = classNode(bytes);
                for (MethodInsnNode call : methodCalls(owner)) {
                    if (call.owner.startsWith("xaero/")) {
                        assertTrue(isAllowedXaeroOwner(call.owner),
                                owner.name + " calls broad Xaero seam " + call.owner + "." + call.name);
                    }
                    if (call.owner.startsWith("com/geckolib/")) {
                        assertTrue(isAllowedGeckoOwner(call.owner),
                                owner.name + " calls broad GeckoLib seam "
                                        + call.owner + "." + call.name);
                    }
                    if (call.owner.startsWith("net/minecraft/server/")) {
                        assertEquals(
                                "net/minecraft/server/packs/resources/ResourceManager",
                                call.owner,
                                owner.name + " calls a server seam "
                                        + call.owner + "." + call.name);
                    }
                }
            }
        }
    }

    @Test
    void renderThreadGuardPrecedesEveryGeoModelAndResourceResolution() throws Exception {
        ClassNode provider = readClass(
                "dev/resivore/ribbitsxaeroicons/RibbitGeoIconProvider.class");
        MethodNode prerender = provider.methods.stream()
                .filter(method -> method.name.equals("prerender"))
                .findFirst().orElseThrow();
        List<MethodInsnNode> calls = methodCalls(prerender);
        int guard = callIndex(calls, "com/mojang/blaze3d/systems/RenderSystem",
                "assertOnRenderThread");
        int prepare = callIndex(calls, "dev/resivore/ribbitsxaeroicons/RibbitGeoIconProvider",
                "prepare");

        assertTrue(guard >= 0, "missing render-thread assertion");
        assertTrue(prepare >= 0, "missing guarded geometry preparation");
        assertTrue(guard < prepare,
                "Geo/model/resource/render-type access must begin only after the thread guard");
    }

    @Test
    void geoPathSelectsAndDrawsDirectCubesWithoutRecursiveOrMutableBoneRendering()
            throws Exception {
        List<MethodInsnNode> calls = new ArrayList<>();
        try (ZipFile zip = new ZipFile(PATCH.toFile())) {
            for (String entry : productionClasses(zip)) {
                try (InputStream input = zip.getInputStream(
                        Objects.requireNonNull(zip.getEntry(entry), entry))) {
                    calls.addAll(methodCalls(classNode(input.readAllBytes())));
                }
            }
        }

        assertTrue(calls.stream().anyMatch(call ->
                call.owner.equals("dev/resivore/ribbitsxaeroicons/RibbitHeadSelector")
                        && call.name.equals("select")));
        assertTrue(calls.stream().anyMatch(call ->
                call.owner.equals("dev/resivore/ribbitsxaeroicons/TransformSafety")
                        && call.name.equals("isFiniteAndInvertible")));
        assertTrue(calls.stream().anyMatch(call ->
                call.owner.equals("dev/resivore/ribbitsxaeroicons/RenderStateGuard")
                        && call.name.equals("run")));
        assertEquals(1L, calls.stream().filter(call ->
                call.owner.equals("com/geckolib/cache/model/cuboid/GeoCube")
                        && call.name.equals("render")).count());
        assertTrue(calls.stream().anyMatch(call -> call.name.equals("getModelResource")));
        assertTrue(calls.stream().anyMatch(call -> call.name.equals("getTextureResource")));
        assertTrue(calls.stream().anyMatch(call -> call.name.equals("getBakedModel")));

        assertFalse(calls.stream().anyMatch(ProductionBytecodeScopeTest::isForbiddenSubmission),
                () -> "descendant, whole-model, render-layer, or entity submission calls: "
                        + calls.stream()
                        .filter(ProductionBytecodeScopeTest::isForbiddenSubmission)
                        .map(call -> call.owner + "." + call.name + call.desc)
                        .toList());

        ClassNode provider = readClass(
                "dev/resivore/ribbitsxaeroicons/RibbitGeoIconProvider.class");
        List<MethodInsnNode> providerCalls = methodCalls(provider);
        assertEquals(1L, providerCalls.stream()
                .filter(call -> call.owner.equals("com/geckolib/renderer/GeoEntityRenderer")
                        && call.name.equals("getRenderType"))
                .count());

        MethodNode cubeLoop = provider.methods.stream()
                .filter(method -> methodCalls(method).stream().anyMatch(call ->
                        call.owner.equals("com/geckolib/cache/model/cuboid/GeoCube")
                                && call.name.equals("render")))
                .findFirst().orElseThrow();
        List<MethodInsnNode> loopCalls = methodCalls(cubeLoop);
        assertEquals(1L, loopCalls.stream()
                .filter(call -> call.owner.equals("com/mojang/blaze3d/vertex/PoseStack")
                        && call.name.equals("pushPose"))
                .count());
        assertEquals(2L, loopCalls.stream()
                .filter(call -> call.owner.equals("com/mojang/blaze3d/vertex/PoseStack")
                        && call.name.equals("popPose"))
                .count());
        assertFalse(cubeLoop.tryCatchBlocks.isEmpty(),
                "per-cube pose restoration must cover exceptional exits");
        assertTrue(hasLdc(cubeLoop, 15_728_880), "draw must use full-bright lighting");
        assertTrue(hasOpcode(cubeLoop, Opcodes.ICONST_M1), "draw must use full-white color");
        assertTrue(hasField(cubeLoop,
                "net/minecraft/client/renderer/texture/OverlayTexture", "NO_OVERLAY"));

        MethodNode restore = provider.methods.stream()
                .filter(method -> method.name.equals("restoreRenderState"))
                .findFirst().orElseThrow();
        List<MethodInsnNode> restoreCalls = methodCalls(restore);
        assertEquals(1L, restoreCalls.stream()
                .filter(call -> call.owner.equals("xaero/lib/client/graphics/XaeroBufferProvider")
                        && call.name.equals("endBatch"))
                .count());
        assertEquals(1L, restoreCalls.stream()
                .filter(call -> call.owner.equals("com/mojang/blaze3d/vertex/PoseStack")
                        && call.name.equals("popPose"))
                .count());
        assertEquals(1L, restoreCalls.stream()
                .filter(call -> call.owner.equals(
                        "xaero/common/minimap/render/MinimapRendererHelper")
                        && call.name.equals("restoreDefaultShaderBlendState"))
                .count());
    }

    private static List<String> productionClasses(ZipFile zip) {
        return zip.stream()
                .filter(entry -> !entry.isDirectory()
                        && entry.getName().startsWith("dev/resivore/ribbitsxaeroicons/")
                        && entry.getName().endsWith(".class"))
                .map(entry -> entry.getName())
                .toList();
    }

    private static boolean isForbiddenSubmission(MethodInsnNode call) {
        Set<String> boneMethods = Set.of(
                "render", "renderChildren", "positionAndRender", "updateBonePositionListeners");
        if ((call.owner.equals("com/geckolib/cache/model/GeoBone")
                || call.owner.equals("com/geckolib/cache/model/cuboid/CuboidGeoBone"))
                && boneMethods.contains(call.name)) {
            return true;
        }
        if (call.owner.equals("com/geckolib/cache/model/BakedGeoModel")
                && call.name.toLowerCase(Locale.ROOT).contains("render")) {
            return true;
        }

        Set<String> rendererMethods = Set.of(
                "performRenderPass", "defaultRender", "reRender", "actuallyRender",
                "submitRenderTasks", "preRenderPass", "postRenderPass", "submit",
                "extractRenderState", "createRenderState", "getRenderLayers",
                "withRenderLayer", "fireCompileRenderLayersEvent", "firePreRenderEvent");
        if ((call.owner.startsWith("com/geckolib/renderer/")
                || call.owner.equals("net/minecraft/client/renderer/entity/EntityRenderer"))
                && rendererMethods.contains(call.name)) {
            return true;
        }
        if (call.owner.startsWith("com/geckolib/animation/")) {
            return true;
        }
        if (call.owner.equals("com/geckolib/renderer/base/RenderPassInfo")
                && call.name.equals("create")) {
            return true;
        }
        return Set.of(
                        "renderRecursively", "setHidden", "setChildrenHidden",
                        "setCubes", "setChildBones")
                .contains(call.name);
    }

    private static int callIndex(List<MethodInsnNode> calls, String owner, String name) {
        for (int index = 0; index < calls.size(); index++) {
            MethodInsnNode call = calls.get(index);
            if (call.owner.equals(owner) && call.name.equals(name)) {
                return index;
            }
        }
        return -1;
    }

    private static boolean hasLdc(MethodNode method, Object expected) {
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof LdcInsnNode ldc && Objects.equals(expected, ldc.cst)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasOpcode(MethodNode method, int opcode) {
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction.getOpcode() == opcode) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasField(MethodNode method, String owner, String name) {
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof FieldInsnNode field
                    && field.owner.equals(owner)
                    && field.name.equals(name)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isAllowedXaeroOwner(String owner) {
        return owner.equals("xaero/common/icon/XaeroIcon")
                || owner.startsWith("xaero/hud/minimap/radar/icon/")
                || owner.equals("xaero/hud/minimap/element/render/MinimapElementGraphics")
                || owner.equals("xaero/common/minimap/render/MinimapRendererHelper")
                || owner.equals("xaero/lib/client/graphics/XaeroBufferProvider");
    }

    private static boolean isAllowedGeckoOwner(String owner) {
        return owner.startsWith("com/geckolib/cache/model/")
                || owner.equals("com/geckolib/model/GeoModel")
                || owner.equals("com/geckolib/renderer/GeoEntityRenderer")
                || owner.equals("com/geckolib/renderer/base/GeoRenderState");
    }

    private static String classPoolText(ClassNode owner) {
        org.objectweb.asm.ClassWriter writer = new org.objectweb.asm.ClassWriter(0);
        owner.accept(writer);
        return new String(writer.toByteArray(), StandardCharsets.ISO_8859_1);
    }

    private static ClassNode readClass(String entryName) throws IOException {
        try (ZipFile zip = new ZipFile(PATCH.toFile())) {
            var entry = Objects.requireNonNull(zip.getEntry(entryName), entryName);
            try (InputStream input = zip.getInputStream(entry)) {
                return classNode(input.readAllBytes());
            }
        }
    }

    private static ClassNode classNode(byte[] bytes) {
        ClassNode result = new ClassNode();
        new org.objectweb.asm.ClassReader(bytes).accept(result, 0);
        return result;
    }

    private static List<MethodInsnNode> methodCalls(ClassNode owner) {
        List<MethodInsnNode> result = new ArrayList<>();
        owner.methods.forEach(method -> result.addAll(methodCalls(method)));
        return result;
    }

    private static List<MethodInsnNode> methodCalls(MethodNode owner) {
        List<MethodInsnNode> result = new ArrayList<>();
        for (AbstractInsnNode instruction : owner.instructions) {
            if (instruction instanceof MethodInsnNode call) {
                result.add(call);
            }
        }
        return result;
    }

    private static List<MethodNode> methodsAnnotated(ClassNode owner, String descriptor) {
        return owner.methods.stream()
                .filter(method -> findAnnotation(method, descriptor) != null)
                .toList();
    }

    private static AnnotationNode annotation(MethodNode owner, String descriptor) {
        return Objects.requireNonNull(findAnnotation(owner, descriptor),
                owner.name + owner.desc + " lacks " + descriptor);
    }

    private static AnnotationNode findAnnotation(MethodNode owner, String descriptor) {
        List<AnnotationNode> annotations = new ArrayList<>();
        if (owner.visibleAnnotations != null) annotations.addAll(owner.visibleAnnotations);
        if (owner.invisibleAnnotations != null) annotations.addAll(owner.invisibleAnnotations);
        return annotations.stream()
                .filter(candidate -> candidate.desc.equals(descriptor))
                .findFirst().orElse(null);
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

    private static List<String> mixinTargets(ClassNode owner) {
        AnnotationNode mixin = annotation(owner, MIXIN);
        List<String> result = new ArrayList<>();
        Object values = optionalValue(mixin, "value");
        if (values instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Type type) result.add(type.getClassName());
            }
        } else if (values instanceof Type type) {
            result.add(type.getClassName());
        }
        Object targets = optionalValue(mixin, "targets");
        if (targets instanceof List<?> list) {
            list.forEach(item -> result.add(Objects.toString(item)));
        } else if (targets != null) {
            result.add(Objects.toString(targets));
        }
        return result;
    }

    private static List<String> stringValues(AnnotationNode annotation, String key) {
        Object raw = value(annotation, key);
        if (raw instanceof List<?> list) {
            return list.stream().map(Objects::toString).toList();
        }
        return List.of(Objects.toString(raw));
    }

    private static AnnotationNode nestedAnnotation(AnnotationNode annotation, String key) {
        Object raw = value(annotation, key);
        assertNotNull(raw);
        if (raw instanceof List<?> list) {
            assertEquals(1, list.size());
            raw = list.getFirst();
        }
        assertTrue(raw instanceof AnnotationNode, key + " is not an annotation: " + raw);
        return (AnnotationNode) raw;
    }

    private static Object value(AnnotationNode annotation, String key) {
        Object result = optionalValue(annotation, key);
        if (result == null) {
            throw new AssertionError(annotation.desc + " lacks " + key);
        }
        return result;
    }

    private static Object optionalValue(AnnotationNode annotation, String key) {
        if (annotation.values != null) {
            for (int index = 0; index < annotation.values.size(); index += 2) {
                if (annotation.values.get(index).equals(key)) {
                    return annotation.values.get(index + 1);
                }
            }
        }
        return null;
    }
}
