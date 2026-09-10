package dev.resivore.xaeroemfcompat;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionBytecodeScopeTest {
    private static final Path PROJECT = Path.of(System.getProperty("projectRoot"));

    @Test
    void mixinHandlerOnlyForwardsPartAndColor() throws IOException {
        ClassNode mixin = readClass(
                "dev/resivore/xaeroemfcompat/mixin/EMFModelPartMixin.class");
        MethodNode handler = method(mixin, "xaeroEmfEntityIconCompat$detectModelPart",
                "(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;"
                        + "Lcom/mojang/blaze3d/vertex/VertexConsumer;III"
                        + "Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V");

        List<MethodInsnNode> calls = methodCalls(handler);
        assertEquals(1, calls.size());
        assertEquals("dev/resivore/xaeroemfcompat/ModelPartDetectionBridge", calls.getFirst().owner);
        assertEquals("forwardToXaero", calls.getFirst().name);
        assertEquals("(Lnet/minecraft/client/model/geom/ModelPart;I)V", calls.getFirst().desc);
        assertEquals(0, fieldWrites(handler));
        assertFalse(calls.stream().anyMatch(call -> call.owner.startsWith("com/mojang/blaze3d/vertex/")));
    }

    @Test
    void productionBridgeIsBoundToXaerosExactPublicCallback() throws IOException {
        ClassNode bridge = readClass(
                "dev/resivore/xaeroemfcompat/ModelPartDetectionBridge.class");
        MethodNode forward = method(bridge, "forwardToXaero",
                "(Lnet/minecraft/client/model/geom/ModelPart;I)V");

        List<Handle> callbackHandles = new ArrayList<>();
        for (AbstractInsnNode instruction : forward.instructions) {
            if (instruction instanceof InvokeDynamicInsnNode dynamic) {
                for (Object argument : dynamic.bsmArgs) {
                    if (argument instanceof Handle handle
                            && handle.getOwner().equals("xaero/common/core/XaeroMinimapCore")) {
                        callbackHandles.add(handle);
                    }
                }
            }
        }

        assertEquals(1, callbackHandles.size());
        Handle callback = callbackHandles.getFirst();
        assertEquals("onEntityIconsModelPartRenderDetection", callback.getName());
        assertEquals("(Lnet/minecraft/client/model/geom/ModelPart;I)V", callback.getDesc());
        assertTrue(callback.getTag() == Opcodes.H_INVOKESTATIC);
        assertEquals(0, fieldWrites(forward));
    }

    @Test
    void postSelectionMixinRunsOnlyAfterUpstreamFailureAndUsesXaerosRenderer() throws IOException {
        ClassNode mixin = readClass(
                "dev/resivore/xaeroemfcompat/mixin/RadarIconModelPrerendererMixin.class");
        MethodNode handler = mixin.methods.stream()
                .filter(candidate -> candidate.name.equals(
                        "xaeroEmfEntityIconCompat$renderRelocatedHead"))
                .findFirst().orElseThrow();
        List<MethodInsnNode> calls = methodCalls(handler);

        int emptyCheck = callIndex(calls, "java/util/List", "isEmpty");
        int resolve = callIndex(calls,
                "dev/resivore/xaeroemfcompat/EmfIconPartResolver", "resolve");
        assertTrue(emptyCheck >= 0);
        assertTrue(resolve > emptyCheck);
        assertEquals(1, calls.stream().filter(call -> call.owner.equals(
                        "dev/resivore/xaeroemfcompat/EmfIconPartResolver")
                && call.name.equals("resolve")).count());
        assertEquals(
                "(Lnet/minecraft/client/model/geom/ModelPart;"
                        + "Lnet/minecraft/client/model/geom/ModelPart;"
                        + "Lxaero/hud/minimap/radar/icon/creator/render/trace/"
                        + "ModelRenderTrace;ZLdev/resivore/xaeroemfcompat/"
                        + "IconTargetPolicy$Selection;)Ljava/util/Optional;",
                calls.stream().filter(call -> call.owner.equals(
                                "dev/resivore/xaeroemfcompat/EmfIconPartResolver")
                        && call.name.equals("resolve"))
                        .findFirst().orElseThrow().desc
        );

        assertEquals(1, calls.stream().filter(call -> call.owner.equals(
                        "xaero/hud/minimap/radar/icon/creator/render/form/model/"
                                + "RadarIconModelPrerenderer")
                && call.name.equals("getPartPrerenderer")).count());
        assertEquals(1, calls.stream().filter(call -> call.owner.equals(
                        "xaero/hud/minimap/radar/icon/creator/render/form/model/part/"
                                + "RadarIconModelPartPrerenderer")
                && call.name.equals("renderPart")).count());
        // One normal flush and one exception-cleanup flush; no additional render call.
        assertEquals(2, calls.stream().filter(call -> call.owner.equals(
                        "xaero/lib/client/graphics/XaeroBufferProvider")
                && call.name.equals("endBatch")).count());
        assertEquals(1, calls.stream().filter(call -> call.owner.equals(
                        "org/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable")
                && call.name.equals("setReturnValue")).count());
        assertEquals(0, fieldWrites(handler));
    }

    @Test
    void partPrerenderMixinBridgesOnlyAdapterTraceAndFinalRenderCall() throws IOException {
        ClassNode mixin = readClass(
                "dev/resivore/xaeroemfcompat/mixin/"
                        + "RadarIconModelPartPrerendererMixin.class");
        MethodNode traceHandler = mixin.methods.stream()
                .filter(candidate -> candidate.name.equals(
                        "xaeroEmfEntityIconCompat$resolveAdapterTrace"))
                .findFirst().orElseThrow();
        List<MethodInsnNode> traceCalls = methodCalls(traceHandler);
        assertEquals(2, traceCalls.stream().filter(call -> call.owner.equals(
                        "xaero/hud/minimap/radar/icon/creator/render/trace/ModelRenderTrace")
                && call.name.equals("getModelPartRenderInfo")).count());
        assertEquals(1, traceCalls.stream().filter(call -> call.owner.equals(
                        "dev/resivore/xaeroemfcompat/EmfIconPartResolver")
                && call.name.equals("adapterMetadata")).count());
        assertEquals(1, traceCalls.stream().filter(call -> call.owner.equals(
                        "dev/resivore/xaeroemfcompat/EmfIconPartResolver$AdapterMetadata")
                && call.name.equals("tracedHead")).count());
        assertEquals(0, fieldWrites(traceHandler));

        MethodNode renderHandler = mixin.methods.stream()
                .filter(candidate -> candidate.name.equals(
                        "xaeroEmfEntityIconCompat$renderCanonicalFrameAdapter"))
                .findFirst().orElseThrow();
        List<MethodInsnNode> renderCalls = methodCalls(renderHandler);
        assertEquals(1, renderCalls.stream().filter(call -> call.owner.equals(
                        "dev/resivore/xaeroemfcompat/EmfIconPartResolver")
                && call.name.equals("renderAdapter")).count());
        assertEquals(1, renderCalls.stream().filter(call -> call.owner.equals(
                        "net/minecraft/client/model/geom/ModelPart")
                && call.name.equals("render")).count());
        assertEquals(0, fieldWrites(renderHandler));
    }

    @Test
    void modelFormMixinAppliesOnlyTheScopedPresentationUpscale() throws IOException {
        ClassNode mixin = readClass(
                "dev/resivore/xaeroemfcompat/mixin/RadarIconModelFormPrerendererMixin.class");
        MethodNode handler = mixin.methods.stream()
                .filter(candidate -> candidate.name.equals("xaeroEmf$applyTargetedUpscale"))
                .findFirst().orElseThrow();
        List<MethodInsnNode> calls = methodCalls(handler);
        assertEquals(1, calls.stream().filter(call -> call.owner.equals(
                        "dev/resivore/xaeroemfcompat/IconPresentationPolicy")
                && call.name.equals("applyUpscaleForCurrentRequest")).count());
        assertEquals(1, calls.stream().filter(call -> call.owner.equals(
                        "xaero/hud/minimap/element/render/MinimapElementGraphics")
                && call.name.equals("pose")).count());
        assertEquals(0, fieldWrites(handler));
    }

    @Test
    void pluginDelegatesItsOptionalModDecisionToTheTestedActivationPolicy() throws IOException {
        ClassNode plugin = readClass(
                "dev/resivore/xaeroemfcompat/mixin/XaeroEmfCompatMixinPlugin.class");
        MethodNode decision = method(plugin, "shouldApplyMixin",
                "(Ljava/lang/String;Ljava/lang/String;)Z");

        assertEquals(1, methodCalls(decision).stream()
                .filter(call -> call.owner.equals("dev/resivore/xaeroemfcompat/CompatibilityActivation")
                        && call.name.equals("shouldApply")
                        && call.desc.equals("(Ljava/util/Optional;Ljava/util/Optional;)Z"))
                .count());
    }

    @Test
    void productionClassfilesContainNoServerOrNetworkReferences() throws IOException {
        Path classes = PROJECT.resolve("build/classes/java/main");
        try (var files = Files.walk(classes)) {
            for (Path classFile : files.filter(path -> path.toString().endsWith(".class")).toList()) {
                String constantPool = new String(Files.readAllBytes(classFile), StandardCharsets.ISO_8859_1);
                assertFalse(constantPool.contains("net/minecraft/server"), classFile.toString());
                assertFalse(constantPool.contains("net/minecraft/network"), classFile.toString());
                assertFalse(constantPool.contains("fabric/api/client/networking"), classFile.toString());
                assertFalse(constantPool.contains("fabric/api/networking"), classFile.toString());
            }
        }
    }

    private static ClassNode readClass(String resource) throws IOException {
        try (InputStream stream = Objects.requireNonNull(
                ProductionBytecodeScopeTest.class.getClassLoader().getResourceAsStream(resource), resource)) {
            ClassNode node = new ClassNode();
            new org.objectweb.asm.ClassReader(stream).accept(node, 0);
            return node;
        }
    }

    private static MethodNode method(ClassNode owner, String name, String descriptor) {
        return owner.methods.stream()
                .filter(candidate -> candidate.name.equals(name) && candidate.desc.equals(descriptor))
                .findFirst()
                .orElseThrow(() -> new AssertionError(owner.name + "." + name + descriptor));
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

    private static long fieldWrites(MethodNode method) {
        long result = 0;
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof FieldInsnNode field
                    && (field.getOpcode() == Opcodes.PUTFIELD || field.getOpcode() == Opcodes.PUTSTATIC)) {
                result++;
            }
        }
        return result;
    }

    private static int callIndex(List<MethodInsnNode> calls, String owner, String name) {
        for (int i = 0; i < calls.size(); i++) {
            MethodInsnNode call = calls.get(i);
            if (call.owner.equals(owner) && call.name.equals(name)) {
                return i;
            }
        }
        return -1;
    }
}
