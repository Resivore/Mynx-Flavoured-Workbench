package dev.resivore.stacksarestackscontainerfixes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
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
import org.objectweb.asm.tree.TypeInsnNode;

class ClientConfigurationLifecycleContractTest {
    private static final String CLIENT_LISTENER =
            "net/minecraft/client/multiplayer/ClientConfigurationPacketListenerImpl";

    @Test
    void initializerRegistersConfigurationCompleteOnceAndNeverUsesClientStarted() throws IOException {
        ClassNode client = readClass(
                "dev/resivore/stacksarestackscontainerfixes/StacksAreStacksContainerFixesClient.class");
        MethodNode initializer = method(
                client,
                "onInitializeClient",
                "()V");

        assertEquals(1, fieldReads(initializer,
                "net/fabricmc/fabric/api/client/networking/v1/ClientConfigurationConnectionEvents",
                "COMPLETE"));
        assertEquals(0, fieldReads(initializer,
                "net/fabricmc/fabric/api/client/event/lifecycle/v1/ClientLifecycleEvents",
                "CLIENT_STARTED"));
        assertEquals(1, calls(initializer, "java/util/concurrent/atomic/AtomicBoolean", "compareAndSet"));
        assertEquals(0, calls(initializer,
                "dev/resivore/stacksarestackscontainerfixes/mixin/StacksAreStacksModInvoker",
                "stacksAreStacksContainerFixes$setStackSizes"));

        MethodNode readiness = method(
                client,
                "inspectItemHolderReadiness",
                "()Ldev/resivore/stacksarestackscontainerfixes/ClientAlignmentCoordinator$HolderReadiness;");
        assertEquals(1, calls(readiness,
                "net/minecraft/core/DefaultedRegistry", "wrapAsHolder"));
        assertEquals(1, calls(readiness,
                "net/minecraft/core/Holder$Reference", "areComponentsBound"));
        assertEquals(0, calls(readiness,
                "dev/resivore/stacksarestackscontainerfixes/mixin/StacksAreStacksModInvoker",
                "stacksAreStacksContainerFixes$setStackSizes"));

        long invocations = client.methods.stream()
                .mapToLong(candidate -> calls(candidate,
                        "dev/resivore/stacksarestackscontainerfixes/mixin/StacksAreStacksModInvoker",
                        "stacksAreStacksContainerFixes$setStackSizes"))
                .sum();
        assertEquals(1, invocations, "Only the post-readiness alignment callback may invoke upstream");
    }

    @Test
    void vanillaBindsHolderComponentsOnTheClientThreadBeforeFabricComplete() throws IOException {
        MethodNode finish = method(
                readClass(CLIENT_LISTENER + ".class"),
                "handleConfigurationFinished",
                "(Lnet/minecraft/network/protocol/configuration/ClientboundFinishConfigurationPacket;)V");

        int threadCheck = callIndex(finish,
                "net/minecraft/network/protocol/PacketUtils", "ensureRunningOnSameThread");
        int collectRegistries = callIndex(finish, CLIENT_LISTENER, "runWithResources");
        int playListenerConstruction = typeIndex(finish, Opcodes.NEW,
                "net/minecraft/client/multiplayer/ClientPacketListener");
        assertTrue(threadCheck < collectRegistries);
        assertTrue(collectRegistries < playListenerConstruction);

        MethodNode collectLambda = method(
                readClass(CLIENT_LISTENER + ".class"),
                "lambda$handleConfigurationFinished$0",
                "(Lnet/minecraft/server/packs/resources/ResourceProvider;)"
                        + "Lnet/minecraft/core/RegistryAccess$Frozen;");
        assertEquals(1, calls(collectLambda,
                "net/minecraft/client/multiplayer/RegistryDataCollector", "collectGameRegistries"));

        MethodNode collect = method(
                readClass("net/minecraft/client/multiplayer/RegistryDataCollector.class"),
                "collectGameRegistries",
                "(Lnet/minecraft/server/packs/resources/ResourceProvider;Lnet/minecraft/core/RegistryAccess$Frozen;Z)"
                        + "Lnet/minecraft/core/RegistryAccess$Frozen;");
        assertEquals(1, calls(collect,
                "net/minecraft/client/multiplayer/RegistryDataCollector", "updateComponents"));

        MethodNode applyPending = method(
                readClass("net/minecraft/client/multiplayer/RegistryDataCollector.class"),
                "lambda$updateComponents$0",
                "(ZLnet/minecraft/core/component/DataComponentInitializers$PendingComponents;)V");
        assertEquals(1, calls(applyPending,
                "net/minecraft/core/component/DataComponentInitializers$PendingComponents", "apply"));

        MethodNode bind = method(
                readClass("net/minecraft/core/component/DataComponentInitializers$BakedEntry.class"),
                "apply",
                "()V");
        assertEquals(1, calls(bind, "net/minecraft/core/Holder$Reference", "bindComponents"));
    }

    @Test
    void exactFabricCompleteInjectionRunsAtThePostBindingPlayListenerBoundary() throws IOException {
        ClassNode mixin = readClass(
                "net/fabricmc/fabric/mixin/networking/client/ClientConfigurationPacketListenerImplMixin.class");
        MethodNode hook = method(
                mixin,
                "handleComplete",
                "(Lnet/minecraft/network/protocol/configuration/ClientboundFinishConfigurationPacket;"
                        + "Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;"
                        + "Lnet/minecraft/core/RegistryAccess$Frozen;)V");
        AnnotationNode inject = annotation(hook.visibleAnnotations,
                "Lorg/spongepowered/asm/mixin/injection/Inject;");
        assertTrue(annotationStrings(inject, "method").contains("handleConfigurationFinished"));

        AnnotationNode at = annotationNodes(inject, "at").getFirst();
        assertEquals("NEW", annotationValue(at, "value"));
        assertTrue(((String) annotationValue(at, "target"))
                .endsWith("Lnet/minecraft/client/multiplayer/ClientPacketListener;"));
        assertEquals(1, calls(hook,
                "net/fabricmc/fabric/impl/networking/client/ClientConfigurationNetworkAddon",
                "handleComplete"));

        MethodNode dispatch = method(
                readClass("net/fabricmc/fabric/impl/networking/client/ClientConfigurationNetworkAddon.class"),
                "handleComplete",
                "()V");
        assertEquals(1, fieldReads(dispatch,
                "net/fabricmc/fabric/api/client/networking/v1/ClientConfigurationConnectionEvents",
                "COMPLETE"));
    }

    @Test
    void initialConnectAndPlayReconfigurationEachCreateAFreshEpochIdentity() throws IOException {
        MethodNode login = method(
                readClass("net/minecraft/client/multiplayer/ClientHandshakePacketListenerImpl.class"),
                "handleLoginFinished",
                "(Lnet/minecraft/network/protocol/login/ClientboundLoginFinishedPacket;)V");
        MethodNode reconfigure = method(
                readClass("net/minecraft/client/multiplayer/ClientPacketListener.class"),
                "handleConfigurationStart",
                "(Lnet/minecraft/network/protocol/game/ClientboundStartConfigurationPacket;)V");

        assertEquals(1, typeCount(login, Opcodes.NEW, CLIENT_LISTENER));
        assertEquals(1, typeCount(reconfigure, Opcodes.NEW, CLIENT_LISTENER));
    }

    @Test
    void itemStackTemplateSnapshotsOnlyTheExplicitComponentPatch() throws IOException {
        ClassNode template = readClass("net/minecraft/world/item/ItemStackTemplate.class");
        MethodNode fromNonEmptyStack = method(
                template,
                "fromNonEmptyStack",
                "(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStackTemplate;");
        MethodNode fromStack = method(
                template,
                "fromStack",
                "(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStackTemplate;");

        assertEquals(1, calls(fromNonEmptyStack,
                "net/minecraft/world/item/ItemStackTemplate", "fromStack"));
        assertEquals(1, calls(fromStack,
                "net/minecraft/world/item/ItemStack", "getComponentsPatch"));
        assertEquals(0, calls(fromStack,
                "net/minecraft/world/item/ItemStack", "getComponents"));
    }

    @Test
    void productionClassesHaveNoCsrLinkageWhenTheOptionalModIsAbsent() throws IOException {
        for (String resource : List.of(
                "dev/resivore/stacksarestackscontainerfixes/StacksAreStacksContainerFixesClient.class",
                "dev/resivore/stacksarestackscontainerfixes/ClientAlignmentCoordinator.class",
                "dev/resivore/stacksarestackscontainerfixes/mixin/StacksAreStacksModInvoker.class")) {
            ClassNode owner = readClass(resource);
            assertTrue(owner.interfaces.stream().noneMatch(ClientConfigurationLifecycleContractTest::isCsrClass));
            assertTrue(!isCsrClass(owner.superName));
            for (MethodNode candidate : owner.methods) {
                assertTrue(candidate.instructions == null || !referencesCsr(candidate),
                        owner.name + '.' + candidate.name + " must not link CSR");
            }
        }
    }

    @Test
    void clientEntrypointLoadsWhenCsrIsExplicitlyUnavailable() throws Exception {
        URL productionClasses = StacksAreStacksContainerFixesClient.class
                .getProtectionDomain()
                .getCodeSource()
                .getLocation();
        ClassLoader dependencies = StacksAreStacksContainerFixesClient.class.getClassLoader();

        try (URLClassLoader isolated = new URLClassLoader(new URL[] {productionClasses}, dependencies) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                synchronized (getClassLoadingLock(name)) {
                    if (name.startsWith("dev.resivore.slotreservations.")) {
                        throw new ClassNotFoundException("CSR intentionally unavailable: " + name);
                    }
                    if (name.startsWith("dev.resivore.stacksarestackscontainerfixes.")) {
                        Class<?> loaded = findLoadedClass(name);
                        if (loaded == null) {
                            loaded = findClass(name);
                        }
                        if (resolve) {
                            resolveClass(loaded);
                        }
                        return loaded;
                    }
                    return super.loadClass(name, resolve);
                }
            }
        }) {
            Class<?> entrypoint = assertDoesNotThrow(() -> Class.forName(
                    "dev.resivore.stacksarestackscontainerfixes.StacksAreStacksContainerFixesClient",
                    true,
                    isolated));
            assertSame(isolated, entrypoint.getClassLoader());
        }
    }

    private static boolean referencesCsr(MethodNode method) {
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call && isCsrClass(call.owner)) {
                return true;
            }
            if (instruction instanceof FieldInsnNode field && isCsrClass(field.owner)) {
                return true;
            }
            if (instruction instanceof TypeInsnNode type && isCsrClass(type.desc)) {
                return true;
            }
            if (instruction instanceof LdcInsnNode constant
                    && constant.cst instanceof Type type
                    && isCsrClass(type.getInternalName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCsrClass(String internalName) {
        return internalName != null && internalName.startsWith("dev/resivore/slotreservations/");
    }

    private static long fieldReads(MethodNode method, String owner, String name) {
        long count = 0;
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof FieldInsnNode field
                    && field.getOpcode() == Opcodes.GETSTATIC
                    && field.owner.equals(owner)
                    && field.name.equals(name)) {
                count++;
            }
        }
        return count;
    }

    private static long calls(MethodNode method, String owner, String name) {
        long count = 0;
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call
                    && call.owner.equals(owner)
                    && call.name.equals(name)) {
                count++;
            }
        }
        return count;
    }

    private static int callIndex(MethodNode method, String owner, String name) {
        int index = 0;
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call
                    && call.owner.equals(owner)
                    && call.name.equals(name)) {
                return index;
            }
            index++;
        }
        throw new AssertionError("Missing call " + owner + '.' + name);
    }

    private static int typeIndex(MethodNode method, int opcode, String type) {
        int index = 0;
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof TypeInsnNode typed
                    && typed.getOpcode() == opcode
                    && typed.desc.equals(type)) {
                return index;
            }
            index++;
        }
        throw new AssertionError("Missing type instruction " + type);
    }

    private static long typeCount(MethodNode method, int opcode, String type) {
        long count = 0;
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof TypeInsnNode typed
                    && typed.getOpcode() == opcode
                    && typed.desc.equals(type)) {
                count++;
            }
        }
        return count;
    }

    private static AnnotationNode annotation(List<AnnotationNode> annotations, String descriptor) {
        assertNotNull(annotations);
        return annotations.stream()
                .filter(candidate -> candidate.desc.equals(descriptor))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing annotation " + descriptor));
    }

    private static Object annotationValue(AnnotationNode annotation, String name) {
        for (int i = 0; i < annotation.values.size(); i += 2) {
            if (annotation.values.get(i).equals(name)) {
                return annotation.values.get(i + 1);
            }
        }
        throw new AssertionError("Missing annotation value " + name);
    }

    @SuppressWarnings("unchecked")
    private static List<String> annotationStrings(AnnotationNode annotation, String name) {
        return (List<String>) annotationValue(annotation, name);
    }

    @SuppressWarnings("unchecked")
    private static List<AnnotationNode> annotationNodes(AnnotationNode annotation, String name) {
        return (List<AnnotationNode>) annotationValue(annotation, name);
    }

    private static ClassNode readClass(String resourceName) throws IOException {
        try (InputStream stream = ClientConfigurationLifecycleContractTest.class.getClassLoader()
                .getResourceAsStream(resourceName)) {
            assertNotNull(stream, resourceName);
            ClassNode node = new ClassNode();
            new org.objectweb.asm.ClassReader(stream).accept(node, 0);
            return node;
        }
    }

    private static MethodNode method(ClassNode owner, String name, String descriptor) {
        return owner.methods.stream()
                .filter(candidate -> candidate.name.equals(name) && candidate.desc.equals(descriptor))
                .findFirst()
                .orElseThrow(() -> new AssertionError(owner.name + '.' + name + descriptor));
    }
}
