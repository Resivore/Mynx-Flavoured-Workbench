package dev.resivore.carryonpatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

class MinecraftEntityIdContractTest {
    private static final Path MINECRAFT = propertyPath("productionMinecraftJar");
    private static final String ENTITY = "net/minecraft/world/entity/Entity";
    private static final String LEVEL = "net/minecraft/world/level/Level";

    @Test
    void minecraft262UsesZeroAsThePrivateRawUnassignedId() throws Exception {
        ClassNode entity = readClass(ENTITY + ".class");
        FieldNode sentinel = field(entity, "INVALID_ENTITY_ID");
        FieldNode rawId = field(entity, "id");

        assertEquals("I", sentinel.desc);
        assertEquals(0, sentinel.value);
        assertTrue((sentinel.access & Opcodes.ACC_PUBLIC) != 0);
        assertTrue((sentinel.access & Opcodes.ACC_STATIC) != 0);
        assertTrue((sentinel.access & Opcodes.ACC_FINAL) != 0);
        assertEquals("I", rawId.desc);
        assertTrue((rawId.access & Opcodes.ACC_PRIVATE) != 0);
        assertFalse((rawId.access & Opcodes.ACC_STATIC) != 0);
    }

    @Test
    void getIdThrowsOnZeroWhileSetIdIsOnlyADirectFieldStore() throws Exception {
        ClassNode entity = readClass(ENTITY + ".class");
        MethodNode getId = method(entity, "getId", "()I");
        MethodNode setId = method(entity, "setId", "(I)V");

        assertTrue(stringConstants(getId).contains(
                "Tried to access entity ID before ID assignment"));
        assertEquals(2, fieldAccesses(getId, Opcodes.GETFIELD, ENTITY, "id"));
        assertTrue(opcodes(getId).contains(Opcodes.ATHROW));

        assertEquals(List.of(
                        Opcodes.ALOAD,
                        Opcodes.ILOAD,
                        Opcodes.PUTFIELD,
                        Opcodes.RETURN),
                opcodes(setId));
        assertEquals(1, fieldAccesses(setId, Opcodes.PUTFIELD, ENTITY, "id"));
        assertTrue(methodCalls(setId).isEmpty(), "setId must remain callback/tracker free");
    }

    @Test
    void clientConstructionGetsZeroFromBaseLevelButServerAllocatesTrackedNonzeroIds()
            throws Exception {
        ClassNode entity = readClass(ENTITY + ".class");
        MethodNode constructor = method(entity, "<init>",
                "(Lnet/minecraft/world/entity/EntityType;"
                        + "Lnet/minecraft/world/level/Level;)V");
        assertEquals(1, calls(constructor, LEVEL, "getNextEntityId", "()I"));
        assertTrue(callPrecedesFieldWrite(constructor, LEVEL, "getNextEntityId", ENTITY, "id"));

        ClassNode level = readClass(LEVEL + ".class");
        assertEquals(List.of(Opcodes.ICONST_0, Opcodes.IRETURN),
                opcodes(method(level, "getNextEntityId", "()I")));

        ClassNode clientLevel = readClass("net/minecraft/client/multiplayer/ClientLevel.class");
        assertFalse(clientLevel.methods.stream().anyMatch(candidate ->
                candidate.name.equals("getNextEntityId") && candidate.desc.equals("()I")),
                "ClientLevel must still inherit Level's unassigned sentinel");

        ClassNode serverLevel = readClass("net/minecraft/server/level/ServerLevel.class");
        MethodNode serverAllocator = method(serverLevel, "getNextEntityId", "()I");
        assertEquals(1, calls(serverAllocator,
                "java/util/concurrent/atomic/AtomicInteger", "incrementAndGet", "()I"));
        assertEquals(1, calls(serverAllocator,
                "net/minecraft/server/level/ServerChunkCache", "hasEntityWithId", "(I)Z"));
        assertEquals(1, opcodes(serverAllocator).stream()
                .filter(opcode -> opcode == Opcodes.IRETURN).count());
        assertTrue(opcodes(serverAllocator).contains(Opcodes.IFEQ),
                "server allocation must reject zero before returning");
        assertTrue(fieldInstructions(serverAllocator).stream().anyMatch(access ->
                        access.getOpcode() == Opcodes.GETSTATIC
                                && access.desc.equals(
                                        "Ljava/util/concurrent/atomic/AtomicInteger;")),
                "server IDs must still come from the process-wide atomic allocator");
    }

    private static boolean callPrecedesFieldWrite(
            MethodNode method, String callOwner, String callName, String fieldOwner, String fieldName) {
        boolean sawCall = false;
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call
                    && call.owner.equals(callOwner) && call.name.equals(callName)) {
                sawCall = true;
            }
            if (sawCall && instruction instanceof FieldInsnNode field
                    && field.getOpcode() == Opcodes.PUTFIELD
                    && field.owner.equals(fieldOwner) && field.name.equals(fieldName)) {
                return true;
            }
        }
        return false;
    }

    private static long calls(MethodNode method, String owner, String name, String descriptor) {
        return methodCalls(method).stream()
                .filter(call -> call.owner.equals(owner)
                        && call.name.equals(name)
                        && call.desc.equals(descriptor))
                .count();
    }

    private static long fieldAccesses(
            MethodNode method, int opcode, String owner, String name) {
        return fieldInstructions(method).stream()
                .filter(field -> field.getOpcode() == opcode
                        && field.owner.equals(owner)
                        && field.name.equals(name))
                .count();
    }

    private static List<FieldInsnNode> fieldInstructions(MethodNode method) {
        List<FieldInsnNode> result = new ArrayList<>();
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof FieldInsnNode field) {
                result.add(field);
            }
        }
        return result;
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

    private static List<Integer> opcodes(MethodNode method) {
        List<Integer> result = new ArrayList<>();
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction.getOpcode() >= 0) {
                result.add(instruction.getOpcode());
            }
        }
        return result;
    }

    private static List<String> stringConstants(MethodNode method) {
        List<String> result = new ArrayList<>();
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof LdcInsnNode constant
                    && constant.cst instanceof String string) {
                result.add(string);
            }
        }
        return result;
    }

    private static FieldNode field(ClassNode owner, String name) {
        return owner.fields.stream()
                .filter(candidate -> candidate.name.equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError(owner.name + "." + name));
    }

    private static MethodNode method(ClassNode owner, String name, String descriptor) {
        return owner.methods.stream()
                .filter(candidate -> candidate.name.equals(name)
                        && candidate.desc.equals(descriptor))
                .findFirst()
                .orElseThrow(() -> new AssertionError(owner.name + "." + name + descriptor));
    }

    private static ClassNode readClass(String entryName) throws IOException {
        try (ZipFile zip = new ZipFile(MINECRAFT.toFile())) {
            var entry = Objects.requireNonNull(zip.getEntry(entryName), entryName);
            try (InputStream input = zip.getInputStream(entry)) {
                ClassNode result = new ClassNode();
                new org.objectweb.asm.ClassReader(input).accept(result, 0);
                return result;
            }
        }
    }

    private static Path propertyPath(String property) {
        return Path.of(Objects.requireNonNull(System.getProperty(property), property));
    }
}
