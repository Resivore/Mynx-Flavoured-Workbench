package dev.resivore.stacksarestackscontainerfixes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Objects;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

class UpstreamArtifactContractTest {
    private static final Path ARTIFACT = Path.of(System.getProperty("stacksAreStacksJar"));
    private static final String MOD_CLASS =
            "net/a5ho9999/stacksarestacks/StacksAreStacksMod.class";

    @Test
    void exactAuditedArtifactIsResolvedWithoutRedistribution() throws Exception {
        assertEquals(358573L, Files.size(ARTIFACT));
        assertEquals(
                "8E318394EA52A6DB343A00987DD1B122C69BF48E42DEB7813CC5EF293E655917",
                sha256(ARTIFACT));

        try (ZipFile zip = new ZipFile(ARTIFACT.toFile())) {
            String metadata = new String(
                    zip.getInputStream(Objects.requireNonNull(zip.getEntry("fabric.mod.json"))).readAllBytes(),
                    StandardCharsets.UTF_8);
            assertTrue(metadata.contains("\"id\":\"stacksarestacks\""));
            assertTrue(metadata.contains("\"version\":\"2.1.2-1.26.2\""));
            assertFalse(metadata.contains("\"client\":"));
        }
    }

    @Test
    void auditedPrivateStackSizeRoutineRemainsAvailableToInvoker() throws Exception {
        ClassNode owner = readClass(MOD_CLASS);
        MethodNode routine = method(owner, "setStackSizes", "(Lnet/minecraft/server/MinecraftServer;)V");

        assertTrue((routine.access & Opcodes.ACC_PRIVATE) != 0);
        assertTrue((routine.access & Opcodes.ACC_STATIC) != 0);
        assertEquals(0, variableReads(routine, Opcodes.ALOAD, 0),
                "The compatibility hook passes null because this audited routine does not use its server argument");
        MethodNode holderPatcher = owner.methods.stream()
                .filter(candidate -> fieldReads(candidate,
                        "net/minecraft/core/component/DataComponents", "MAX_STACK_SIZE") > 0)
                .findFirst()
                .orElse(null);
        assertNotNull(holderPatcher, "Audited class must still construct a MAX_STACK_SIZE patch");
        assertEquals(1, calls(holderPatcher,
                "net/minecraft/world/item/Item", "builtInRegistryHolder",
                "()Lnet/minecraft/core/Holder$Reference;"));
        assertEquals(1, calls(holderPatcher,
                "java/lang/reflect/Field", "set", "(Ljava/lang/Object;Ljava/lang/Object;)V"));
    }

    @Test
    void auditedVersionStillRunsOnlyAtServerStarted() throws Exception {
        MethodNode initializer = method(readClass(MOD_CLASS), "onInitialize", "()V");
        assertEquals(1, fieldReads(initializer,
                "net/fabricmc/fabric/api/event/lifecycle/v1/ServerLifecycleEvents", "SERVER_STARTED"));
        assertEquals(0, fieldReads(initializer,
                "net/fabricmc/fabric/api/event/lifecycle/v1/ServerLifecycleEvents", "SERVER_STARTING"));
    }

    private static long fieldReads(MethodNode method, String owner, String name) {
        long count = 0;
        for (var instruction : method.instructions) {
            if (instruction instanceof FieldInsnNode field
                    && field.getOpcode() == Opcodes.GETSTATIC
                    && field.owner.equals(owner)
                    && field.name.equals(name)) {
                count++;
            }
        }
        return count;
    }

    private static long variableReads(MethodNode method, int opcode, int variable) {
        long count = 0;
        for (var instruction : method.instructions) {
            if (instruction instanceof VarInsnNode access
                    && access.getOpcode() == opcode
                    && access.var == variable) {
                count++;
            }
        }
        return count;
    }

    private static long calls(MethodNode method, String owner, String name, String descriptor) {
        long count = 0;
        for (var instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call
                    && call.owner.equals(owner)
                    && call.name.equals(name)
                    && call.desc.equals(descriptor)) {
                count++;
            }
        }
        return count;
    }

    private static ClassNode readClass(String entryName) throws Exception {
        try (ZipFile zip = new ZipFile(ARTIFACT.toFile())) {
            var entry = Objects.requireNonNull(zip.getEntry(entryName), entryName);
            try (InputStream stream = zip.getInputStream(entry)) {
                ClassNode node = new ClassNode();
                new org.objectweb.asm.ClassReader(stream).accept(node, 0);
                return node;
            }
        }
    }

    private static MethodNode method(ClassNode owner, String name, String descriptor) {
        MethodNode result = owner.methods.stream()
                .filter(candidate -> candidate.name.equals(name) && candidate.desc.equals(descriptor))
                .findFirst()
                .orElse(null);
        assertNotNull(result, owner.name + "." + name + descriptor);
        return result;
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
