package dev.resivore.quickstacknearbycompat;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventorySearchUpstreamContractTest {
    private static final String EXPECTED_SHA256 =
            "3C5C40A16C93FAEFE0B19A78F81809D911884A141B80C022830A30A88C63D3D9";
    private static final long EXPECTED_SIZE = 238_435L;
    private static final String CORE_ENTRY = "META-INF/jars/inventory-sort-core-3.4.0.jar";
    private static final String EXPECTED_CORE_SHA256 =
            "0A7C3B6504BBA2900B20DBF2A4194ADFA34D8DC8F63C1D1A74BCB909E51AE7F2";
    private static final int EXPECTED_CORE_SIZE = 155_502;

    @Test
    void exactInstalledInventorySearchBuildIsTheAuditedReference() throws Exception {
        Path jar = referenceJar();

        assertEquals(EXPECTED_SIZE, Files.size(jar));
        assertEquals(EXPECTED_SHA256, sha256(Files.readAllBytes(jar)));

        try (JarFile zip = new JarFile(jar.toFile())) {
            String metadata = readText(zip, "fabric.mod.json");
            assertTrue(metadata.contains("\"id\": \"inventorysearch\""));
            assertTrue(metadata.contains("\"version\": \"3.4.0\""));
            assertTrue(metadata.contains("\"file\": \"" + CORE_ENTRY + "\""));

            byte[] core = readBytes(zip, CORE_ENTRY);
            assertEquals(EXPECTED_CORE_SIZE, core.length);
            assertEquals(EXPECTED_CORE_SHA256, sha256(core));
            String coreMetadata = readNestedText(core, "fabric.mod.json");
            assertTrue(coreMetadata.contains("\"id\": \"inventorysort_core\""));
            assertTrue(coreMetadata.contains("\"version\": \"3.4.0\""));
        }
    }

    @Test
    void inventorySearchOwnsTheIndependentTwelvePixelPlayerButton() throws Exception {
        try (JarFile jar = new JarFile(referenceJar().toFile())) {
            ClassNode searchMixin = readClass(
                    jar,
                    "tempeststudios/inventorysort/mixin/SearchButtonMixin.class"
            );
            MethodNode onInit = method(
                    searchMixin,
                    "inventorySearch$onInit",
                    "(Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V"
            );
            MethodNode isContainer = method(
                    searchMixin,
                    "inventorySearch$isContainer",
                    "(Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;)Z"
            );
            MethodNode position = method(
                    searchMixin,
                    "inventorySearch$positionFor",
                    "(Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;Ltempeststudios/inventorysort/mixin/AbstractContainerScreenAccessor;)[I"
            );
            MethodNode update = method(searchMixin, "inventorySearch$updateButtonPosition", "()V");

            assertTrue(hasTypeInstruction(
                    onInit,
                    org.objectweb.asm.Opcodes.NEW,
                    "tempeststudios/inventorysort/InventorySortIconButton"
            ));
            assertTrue(hasCall(
                    onInit,
                    "tempeststudios/inventorysort/mixin/SearchButtonMixin",
                    "inventorySearch$isContainer"
            ));
            assertTrue(hasCall(
                    onInit,
                    "tempeststudios/inventorysort/mixin/ScreenAccessor",
                    "invokeAddRenderableWidget"
            ));
            assertTrue(hasCall(
                    isContainer,
                    "tempeststudios/inventorysort/api/InventoryScreenButtonSlots",
                    "isInventoryModsContainer"
            ));
            assertTrue(hasStringConstant(position, "inventorysearch"));
            assertTrue(hasStringConstant(position, "inventory_search"));
            assertEquals(1, countIntegerConstant(position, 100));
            assertEquals(1, countIntegerConstant(position, 12));
            assertTrue(hasCall(
                    position,
                    "tempeststudios/inventorysort/api/InventoryScreenButtonSlots",
                    "reserveRightSlot"
            ));
            assertTrue(hasCall(update, "net/minecraft/client/gui/components/Button", "setX"));
            assertTrue(hasCall(update, "net/minecraft/client/gui/components/Button", "setY"));

            byte[] core = readBytes(jar, CORE_ENTRY);
            ClassNode iconButton = readNestedClass(
                    core,
                    "tempeststudios/inventorysort/InventorySortIconButton.class"
            );
            MethodNode constructor = method(
                    iconButton,
                    "<init>",
                    "(IIILnet/minecraft/network/chat/Component;Lnet/minecraft/client/gui/components/Button$OnPress;Lnet/minecraft/client/gui/components/Button$OnPress;)V"
            );
            assertEquals(2, countIntegerConstant(constructor, 12));
            assertTrue(hasCall(constructor, "net/minecraft/client/gui/components/Button", "<init>"));

            ClassNode slots = readNestedClass(
                    core,
                    "tempeststudios/inventorysort/api/InventoryScreenButtonSlots.class"
            );
            MethodNode baseY = method(
                    slots,
                    "playerGroupTop",
                    "(Ltempeststudios/inventorysort/mixin/AbstractContainerScreenAccessor;)I"
            );
            MethodNode classification = method(
                    slots,
                    "isInventoryModsContainer",
                    "(Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;)Z"
            );
            MethodNode coordinates = method(
                    slots,
                    "coordinatesFor",
                    "(Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;Ltempeststudios/inventorysort/api/InventoryScreenButtonSlots$RightSlotGroup;III)Ltempeststudios/inventorysort/api/InventoryScreenButtonSlots$Coordinates;"
            );
            assertIntegerConstantField(slots, "DEFAULT_BUTTON_SIZE", 12);
            assertIntegerConstantField(slots, "DEFAULT_BUTTON_GAP", 1);
            assertIntegerConstantField(slots, "FIRST_PARTY_SEARCH_PRIORITY", 100);
            assertIntegerConstantField(slots, "THIRD_PARTY_DEFAULT_PRIORITY", 1000);
            assertEquals(1, countIntegerConstant(classification, 46));
            assertTrue(hasTypeInstruction(
                    classification,
                    org.objectweb.asm.Opcodes.INSTANCEOF,
                    "net/minecraft/client/gui/screens/inventory/CreativeModeInventoryScreen"
            ));
            assertTrue(hasTypeInstruction(
                    classification,
                    org.objectweb.asm.Opcodes.INSTANCEOF,
                    "net/minecraft/world/inventory/DispenserMenu"
            ));
            assertTrue(hasCall(
                    classification,
                    "net/minecraft/core/NonNullList",
                    "size"
            ));
            assertTrue(countIntegerConstant(coordinates, 1) >= 1);
            assertEquals(1, countIntegerConstant(baseY, 83));
            assertTrue(hasCall(
                    baseY,
                    "tempeststudios/inventorysort/mixin/AbstractContainerScreenAccessor",
                    "getTopPos"
            ));
            assertTrue(hasCall(
                    baseY,
                    "tempeststudios/inventorysort/mixin/AbstractContainerScreenAccessor",
                    "getImageHeight"
            ));

            ClassNode reservations = readNestedClass(
                    core,
                    "tempeststudios/inventorysort/api/InventoryScreenButtonSlots$ScreenReservations.class"
            );
            MethodNode priorityComparator = method(
                    reservations,
                    "lambda$sortedReservations$0",
                    "(Ltempeststudios/inventorysort/api/InventoryScreenButtonSlots$Reservation;Ltempeststudios/inventorysort/api/InventoryScreenButtonSlots$Reservation;)I"
            );
            assertTrue(hasCall(priorityComparator, "java/lang/Integer", "compare"));
            assertTrue(hasCall(priorityComparator, "java/lang/Long", "compare"));
        }
    }

    private static Path referenceJar() {
        String configured = System.getProperty("inventorySearchReferenceJar");
        assertNotNull(configured, "Gradle must provide inventorySearchReferenceJar");
        return Path.of(configured);
    }

    private static String sha256(byte[] bytes) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().withUpperCase().formatHex(digest.digest(bytes));
    }

    private static ClassNode readClass(JarFile jar, String entryName) throws Exception {
        return readClass(readBytes(jar, entryName));
    }

    private static ClassNode readNestedClass(byte[] jarBytes, String entryName) throws Exception {
        return readClass(readNestedBytes(jarBytes, entryName));
    }

    private static ClassNode readClass(byte[] classBytes) {
        ClassNode node = new ClassNode();
        new ClassReader(classBytes).accept(node, 0);
        return node;
    }

    private static byte[] readBytes(JarFile jar, String entryName) throws Exception {
        JarEntry entry = jar.getJarEntry(entryName);
        assertNotNull(entry, "Missing archive entry " + entryName);
        try (InputStream input = jar.getInputStream(entry)) {
            return input.readAllBytes();
        }
    }

    private static String readText(JarFile jar, String entryName) throws Exception {
        return new String(readBytes(jar, entryName), StandardCharsets.UTF_8);
    }

    private static byte[] readNestedBytes(byte[] jarBytes, String entryName) throws Exception {
        try (JarInputStream input = new JarInputStream(new ByteArrayInputStream(jarBytes))) {
            JarEntry entry;
            while ((entry = input.getNextJarEntry()) != null) {
                if (!entry.getName().equals(entryName)) {
                    continue;
                }
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                input.transferTo(output);
                return output.toByteArray();
            }
        }
        throw new AssertionError("Missing nested archive entry " + entryName);
    }

    private static String readNestedText(byte[] jarBytes, String entryName) throws Exception {
        return new String(readNestedBytes(jarBytes, entryName), StandardCharsets.UTF_8);
    }

    private static MethodNode method(ClassNode owner, String name, String descriptor) {
        return owner.methods.stream()
                .filter(candidate -> candidate.name.equals(name) && candidate.desc.equals(descriptor))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing " + owner.name + "." + name + descriptor));
    }

    private static void assertIntegerConstantField(ClassNode owner, String name, int expected) {
        Object value = owner.fields.stream()
                .filter(field -> field.name.equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing " + owner.name + "." + name))
                .value;
        assertEquals(Integer.valueOf(expected), value);
    }

    private static int countIntegerConstant(MethodNode method, int expected) {
        int count = 0;
        for (AbstractInsnNode instruction : method.instructions) {
            Integer value = integerConstant(instruction);
            if (value != null && value == expected) {
                count++;
            }
        }
        return count;
    }

    private static Integer integerConstant(AbstractInsnNode instruction) {
        if (instruction instanceof IntInsnNode value) {
            return value.operand;
        }
        if (instruction instanceof LdcInsnNode value && value.cst instanceof Integer integer) {
            return integer;
        }
        int opcode = instruction.getOpcode();
        if (opcode >= org.objectweb.asm.Opcodes.ICONST_M1
                && opcode <= org.objectweb.asm.Opcodes.ICONST_5) {
            return opcode - org.objectweb.asm.Opcodes.ICONST_0;
        }
        return null;
    }

    private static boolean hasCall(MethodNode method, String owner, String name) {
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call
                    && call.owner.equals(owner)
                    && call.name.equals(name)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasStringConstant(MethodNode method, String expected) {
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof LdcInsnNode value && expected.equals(value.cst)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasTypeInstruction(MethodNode method, int opcode, String descriptor) {
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof TypeInsnNode type
                    && type.getOpcode() == opcode
                    && type.desc.equals(descriptor)) {
                return true;
            }
        }
        return false;
    }
}
