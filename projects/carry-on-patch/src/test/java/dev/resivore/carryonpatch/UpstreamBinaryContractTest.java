package dev.resivore.carryonpatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import java.util.function.Predicate;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

class UpstreamBinaryContractTest {
    private static final Path GRAB_AND_GO = propertyPath("grabAndGoReferenceJar");
    private static final String RENDERER =
            "org/chermew/grabandgo/client/render/CarriedObjectFeatureRenderer";
    private static final String FIRST_PERSON_MIXIN =
            "org/chermew/grabandgo/client/mixin/ItemInHandRendererMixin";
    private static final String RENDER_ENTITY_DESC =
            "(Lcom/mojang/blaze3d/vertex/PoseStack;"
                    + "Lnet/minecraft/client/renderer/SubmitNodeCollector;I"
                    + "Lnet/minecraft/nbt/CompoundTag;"
                    + "Lnet/minecraft/world/entity/Entity;)V";
    private static final String FIRST_PERSON_DESC =
            "(Lcom/mojang/blaze3d/vertex/PoseStack;"
                    + "Lnet/minecraft/client/renderer/SubmitNodeCollector;I"
                    + "Lnet/minecraft/nbt/CompoundTag;"
                    + "Lnet/minecraft/client/player/LocalPlayer;F)V";
    private static final String EXTRACT_DESC =
            "(Lnet/minecraft/world/entity/Entity;F)"
                    + "Lnet/minecraft/client/renderer/entity/state/EntityRenderState;";

    @Test
    void exactAuditedGrabAndGoArtifactAndMetadataRemainPinned() throws Exception {
        assertEquals("199VyzmC-STumnqJf.jar", GRAB_AND_GO.getFileName().toString(),
                "Gradle must resolve the exact pinned Modrinth Maven coordinate");
        assertEquals(381_703L, Files.size(GRAB_AND_GO));
        assertEquals("9360863791C9E2F898DE7ABF5C2A671343FA47B793FADD51CC41BF254EF5BB64",
                sha256(GRAB_AND_GO));

        try (ZipFile zip = new ZipFile(GRAB_AND_GO.toFile())) {
            String metadata = readUtf8(zip, "fabric.mod.json");
            assertJsonString(metadata, "id", "grapandgo");
            assertJsonString(metadata, "version", "1.0.1");
            assertJsonString(metadata, "environment", "*");
            assertJsonString(metadata, "fabricloader", ">=0.19.2");
            assertJsonString(metadata, "fabric-api", "*");
            assertJsonString(metadata, "minecraft", "26.2");
            assertFalse(metadata.matches("(?s).*\\\"jars\\\"\\s*:.*"),
                    "the audited upstream must not acquire nested-JAR metadata");
            assertFalse(zip.stream().anyMatch(entry -> entry.getName().endsWith(".jar")),
                    "the audited upstream must not contain nested JARs");
        }
    }

    @Test
    void featureRendererCreatesCachesLoadsThenExtractsTheSyntheticEntity() throws Exception {
        ClassNode owner = readClass(GRAB_AND_GO, RENDERER + ".class");
        MethodNode method = method(owner, "renderEntity", RENDER_ENTITY_DESC);

        assertReconstructionOrder(method,
                call -> call.owner.equals("net/minecraft/world/entity/EntityType")
                        && call.name.equals("create")
                        && call.desc.equals("(Lnet/minecraft/world/level/Level;"
                                + "Lnet/minecraft/world/entity/EntitySpawnReason;)"
                                + "Lnet/minecraft/world/entity/Entity;"));
        assertTrue(fieldReads(method, RENDERER, "dummyEntityCache") >= 2);
        assertNoWorldInsertionOrPersistence(method);
    }

    @Test
    void firstPersonMixinUsesTheSameCacheAndTheExactProductionDescriptor() throws Exception {
        ClassNode owner = readClass(GRAB_AND_GO, FIRST_PERSON_MIXIN + ".class");
        MethodNode method = method(owner, "renderEntityInFirstPerson", FIRST_PERSON_DESC);

        assertReconstructionOrder(method,
                call -> call.owner.equals("net/minecraft/world/entity/EntityType")
                        && call.name.equals("create")
                        && call.desc.equals("(Lnet/minecraft/world/level/Level;"
                                + "Lnet/minecraft/world/entity/EntitySpawnRequest;)"
                                + "Lnet/minecraft/world/entity/Entity;"));
        assertTrue(fieldReads(method, RENDERER, "dummyEntityCache") >= 2,
                "first-person reconstruction must share the feature renderer cache");
        assertNoWorldInsertionOrPersistence(method);
    }

    @Test
    void ordinalZeroHashMapInClinitFeedsOnlyTheSharedEntityCache() throws Exception {
        ClassNode owner = readClass(GRAB_AND_GO, RENDERER + ".class");
        MethodNode clinit = method(owner, "<clinit>", "()V");
        List<Integer> allocations = instructionIndexes(clinit,
                instruction -> instruction instanceof TypeInsnNode type
                        && type.getOpcode() == Opcodes.NEW
                        && type.desc.equals("java/util/HashMap"));

        assertEquals(2, allocations.size(), "redirect ordinal relies on exactly two HashMap NEWs");
        assertEquals("dummyEntityCache", nextStaticWrite(clinit, allocations.get(0)).name);
        assertEquals("blockStateCache", nextStaticWrite(clinit, allocations.get(1)).name);

        FieldNode cache = field(owner, "dummyEntityCache");
        assertEquals("Ljava/util/Map;", cache.desc);
        assertTrue((cache.access & Opcodes.ACC_PUBLIC) != 0);
        assertTrue((cache.access & Opcodes.ACC_STATIC) != 0);
        assertTrue((cache.access & Opcodes.ACC_FINAL) != 0);
    }

    private static void assertReconstructionOrder(
            MethodNode method, Predicate<MethodInsnNode> createCall) {
        int create = callIndex(method, createCall);
        int put = callIndex(method, call -> call.owner.equals("java/util/Map")
                && call.name.equals("put")
                && call.desc.equals("(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"));
        int load = callIndex(method, call -> call.owner.equals("net/minecraft/world/entity/Entity")
                && call.name.equals("load")
                && call.desc.equals("(Lnet/minecraft/world/level/storage/ValueInput;)V"));
        int extract = callIndex(method,
                call -> call.owner.equals(
                                "net/minecraft/client/renderer/entity/EntityRenderDispatcher")
                        && call.name.equals("extractEntity")
                        && call.desc.equals(EXTRACT_DESC));

        assertTrue(create < put, "synthetic entity must be cached after creation");
        assertTrue(put < load, "cache put is the earliest shared seam before NBT load");
        assertTrue(load < extract, "render-state extraction must follow entity load");
    }

    private static void assertNoWorldInsertionOrPersistence(MethodNode method) {
        for (MethodInsnNode call : methodCalls(method)) {
            assertFalse(call.name.equals("addFreshEntity"), call.owner + "." + call.name);
            assertFalse(call.name.equals("save") || call.name.equals("saveWithoutId"),
                    call.owner + "." + call.name);
            assertFalse(call.owner.startsWith("net/minecraft/network/"),
                    call.owner + "." + call.name);
        }
    }

    private static int callIndex(MethodNode method, Predicate<MethodInsnNode> predicate) {
        int index = 0;
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call && predicate.test(call)) {
                return index;
            }
            index++;
        }
        throw new AssertionError("missing expected call in " + method.name + method.desc);
    }

    private static List<Integer> instructionIndexes(
            MethodNode method, Predicate<AbstractInsnNode> predicate) {
        List<Integer> indexes = new ArrayList<>();
        int index = 0;
        for (AbstractInsnNode instruction : method.instructions) {
            if (predicate.test(instruction)) {
                indexes.add(index);
            }
            index++;
        }
        return indexes;
    }

    private static FieldInsnNode nextStaticWrite(MethodNode method, int startIndex) {
        int index = 0;
        for (AbstractInsnNode instruction : method.instructions) {
            if (index > startIndex && instruction instanceof FieldInsnNode field
                    && field.getOpcode() == Opcodes.PUTSTATIC) {
                return field;
            }
            index++;
        }
        throw new AssertionError("no PUTSTATIC after instruction " + startIndex);
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

    private static List<MethodInsnNode> methodCalls(MethodNode method) {
        List<MethodInsnNode> calls = new ArrayList<>();
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call) {
                calls.add(call);
            }
        }
        return calls;
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

    private static ClassNode readClass(Path jar, String entryName) throws IOException {
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            var entry = Objects.requireNonNull(zip.getEntry(entryName), entryName);
            try (InputStream input = zip.getInputStream(entry)) {
                ClassNode result = new ClassNode();
                new org.objectweb.asm.ClassReader(input).accept(result, 0);
                return result;
            }
        }
    }

    private static String readUtf8(ZipFile zip, String entryName) throws IOException {
        var entry = Objects.requireNonNull(zip.getEntry(entryName), entryName);
        try (InputStream input = zip.getInputStream(entry)) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void assertJsonString(String json, String key, String value) {
        String expression = "(?s).*\\\"" + java.util.regex.Pattern.quote(key)
                + "\\\"\\s*:\\s*\\\"" + java.util.regex.Pattern.quote(value)
                + "\\\".*";
        assertTrue(json.matches(expression), () -> key + " must be " + value);
    }

    private static Path propertyPath(String property) {
        return Path.of(Objects.requireNonNull(System.getProperty(property), property));
    }

    private static String sha256(Path path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = Files.newInputStream(path)) {
            byte[] buffer = new byte[8192];
            for (int read; (read = input.read(buffer)) >= 0; ) {
                digest.update(buffer, 0, read);
            }
        }
        return HexFormat.of().withUpperCase().formatHex(digest.digest());
    }
}
