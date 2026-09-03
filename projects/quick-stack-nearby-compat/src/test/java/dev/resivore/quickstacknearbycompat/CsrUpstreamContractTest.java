package dev.resivore.quickstacknearbycompat;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsrUpstreamContractTest {
    private static final long EXPECTED_SIZE = 63_388L;
    private static final String EXPECTED_SHA256 =
            "4E7F0A470A387BE76189D0A5E1AE8C614D17EC8B24A6774B400838CC234C4532";

    @Test
    void exactAcceptedCsrC1IsTheCompileAndBytecodeReference() throws Exception {
        Path jar = referenceJar();
        assertEquals(EXPECTED_SIZE, Files.size(jar));
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        assertEquals(EXPECTED_SHA256,
                HexFormat.of().withUpperCase().formatHex(digest.digest(Files.readAllBytes(jar))));

        try (JarFile zip = new JarFile(jar.toFile())) {
            String metadata = readText(zip, "fabric.mod.json");
            assertTrue(metadata.contains("\"id\": \"container_slot_reservations\""));
            assertTrue(metadata.contains("\"version\": \"0.1.0-canary1\""));
        }
    }

    @Test
    void exactReadOnlyClassificationSeamRemainsAvailable() throws Exception {
        try (JarFile jar = new JarFile(referenceJar().toFile())) {
            ClassNode api = readClass(
                    jar,
                    "dev/resivore/slotreservations/api/ContainerSlotReservationsApi.class"
            );
            var classify = api.methods.stream()
                    .filter(method -> method.name.equals("classify")
                            && method.desc.equals("(Lnet/minecraft/world/Container;ILnet/minecraft/world/item/ItemStack;)"
                            + "Ldev/resivore/slotreservations/api/ReservationSlotClass;"))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Missing CSR container classification API"));
            assertEquals(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                    classify.access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED | Opcodes.ACC_PRIVATE
                            | Opcodes.ACC_STATIC));

            ClassNode slotClass = readClass(
                    jar,
                    "dev/resivore/slotreservations/api/ReservationSlotClass.class"
            );
            Set<String> constants = slotClass.fields.stream()
                    .filter(field -> (field.access & Opcodes.ACC_ENUM) != 0)
                    .map(field -> field.name)
                    .collect(Collectors.toSet());
            assertEquals(Set.of(
                    "OCCUPIED_COMPATIBLE",
                    "RESERVED_MATCH",
                    "UNRESERVED_EMPTY",
                    "RESERVED_OTHER",
                    "NON_WRITABLE",
                    "INELIGIBLE"
            ), constants);
        }
    }

    private static Path referenceJar() {
        String configured = System.getProperty("csrReferenceJar");
        assertNotNull(configured, "Gradle must provide csrReferenceJar");
        return Path.of(configured);
    }

    private static ClassNode readClass(JarFile jar, String entryName) throws Exception {
        var entry = jar.getJarEntry(entryName);
        assertNotNull(entry, "Missing audited class " + entryName);
        try (InputStream input = jar.getInputStream(entry)) {
            ClassNode node = new ClassNode();
            new ClassReader(input).accept(node, 0);
            return node;
        }
    }

    private static String readText(JarFile jar, String entryName) throws Exception {
        var entry = jar.getJarEntry(entryName);
        assertNotNull(entry, "Missing audited entry " + entryName);
        try (InputStream input = jar.getInputStream(entry)) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
