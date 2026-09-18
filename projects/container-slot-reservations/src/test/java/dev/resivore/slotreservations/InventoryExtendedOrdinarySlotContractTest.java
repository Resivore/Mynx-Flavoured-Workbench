package dev.resivore.slotreservations;

import net.minecraft.SharedConstants;
import net.minecraft.core.NonNullList;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Binds CSR's ordinary-slot rule to the exact accepted Inventory Extended runtime artifact. */
final class InventoryExtendedOrdinarySlotContractTest {
    private static final Path INVENTORY_EXTENDED =
            Path.of(System.getProperty("inventoryExtendedReferenceJar"));
    private static final String MODIFY_CONSTANT =
            "Lorg/spongepowered/asm/mixin/injection/ModifyConstant;";
    private static final String CONSTANT =
            "Lorg/spongepowered/asm/mixin/injection/Constant;";

    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void acceptedArtifactDefinesSixtyThreeOrdinarySlotsBeforeEquipment() throws Exception {
        assertEquals(80_604L, Files.size(INVENTORY_EXTENDED));
        assertEquals("a0ced554cb687f7119aa19ac3466c0de3ffb0a888e994514223152335b043636",
                sha256(INVENTORY_EXTENDED));

        String metadata = jarText("fabric.mod.json");
        assertTrue(metadata.contains("\"id\": \"inventoryextended\""));
        assertTrue(metadata.contains("\"version\": \"1.1.2\""));
        String mixins = jarText("inventoryextended.mixins.json");
        assertTrue(mixins.contains("\"ExtendPlayerInventory\""));
        assertTrue(mixins.contains("\"RemapPlayerSlots\""));
        assertFalse(mixins.contains("\"FixCreativeSlotRangeCheck\""),
                "The accepted compatibility artifact must keep the unsafe creative range mixin disabled");

        Map<String, Shift> inventory = shifts("inventoryextended/mixin/ExtendPlayerInventory.class");
        assertShift(inventory, "modifyInitListSize", 36, -1, "<init>");
        assertEquals(63, inventory.get("modifyInitListSize").apply(36),
                "The live non-equipment list grows from 36 to 63 slots");

        List<Integer> equipment = List.of(
                shifted(inventory, "modifyClinitFeetIndex", 36, 0),
                shifted(inventory, "modifyClinitLegsIndex", 36, 1) + 1,
                shifted(inventory, "modifyClinitChestIndex", 36, 2) + 2,
                shifted(inventory, "modifyClinitHeadIndex", 36, 3) + 3,
                shifted(inventory, "modifyClinitOffhand", 40, -1),
                shifted(inventory, "modifyClinitBody", 41, -1),
                shifted(inventory, "modifyClinitSaddle", 42, -1));
        assertEquals(List.of(63, 64, 65, 66, 67, 68, 69), equipment,
                "Equipment begins exactly at the first coordinate outside the 63 ordinary slots");
        assertEquals(70, 63 + equipment.size(), "The extended Inventory container ends at slot 69");

        Map<String, Shift> menu = shifts("inventoryextended/mixin/RemapPlayerSlots.class");
        assertEquals(63, shifted(menu, "modify36", 36, -1));
        assertEquals(72, shifted(menu, "modify45", 45, -1));
        assertEquals(73, shifted(menu, "modify46", 46, -1));
        assertEquals(66, shifted(menu, "modifyArmorSlotIndex", 39, -1));
        assertEquals(67, shifted(menu, "modifyOffhandSlotIndex", 40, -1));
    }

    @Test
    void csrUsesTheLiveOrdinaryListBoundaryRatherThanVanillasCompiledConstant() {
        Player player = mock(Player.class);
        Inventory inventory = mock(Inventory.class);
        when(player.getInventory()).thenReturn(inventory);
        when(inventory.getNonEquipmentItems())
                .thenReturn(NonNullList.withSize(63, ItemStack.EMPTY));
        when(inventory.stillValid(player)).thenReturn(true);

        for (int physical : List.of(0, 35, 36, 62)) {
            assertTrue(OrdinaryPlayerInventorySlots.isEligible(player, slot(inventory, physical)),
                    "Expected ordinary physical slot " + physical + " to be eligible");
        }
        for (int physical : List.of(-1, 63, 64, 65, 66, 67, 68, 69)) {
            assertFalse(OrdinaryPlayerInventorySlots.isEligible(player, slot(inventory, physical)),
                    "Expected special/out-of-range physical slot " + physical + " to be rejected");
        }
    }

    private static int shifted(
            Map<String, Shift> shifts,
            String name,
            int original,
            int ordinal
    ) {
        assertShift(shifts, name, original, ordinal, null);
        return shifts.get(name).apply(original);
    }

    private static void assertShift(
            Map<String, Shift> shifts,
            String name,
            int original,
            int ordinal,
            String target
    ) {
        Shift shift = shifts.get(name);
        assertTrue(shift != null, "Missing exact Inventory Extended shift method " + name);
        assertEquals(List.of(original), shift.constants);
        assertEquals(ordinal, shift.ordinal);
        if (target != null) assertTrue(shift.targets.contains(target));
        assertEquals(27, shift.addend);
        assertTrue(shift.sawIntegerAdd && shift.sawReturn,
                name + " must remain the exact original-plus-27 transformation");
    }

    private static Map<String, Shift> shifts(String entryName) throws IOException {
        Map<String, Shift> result = new LinkedHashMap<>();
        new ClassReader(jarBytes(entryName)).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(
                    int access,
                    String name,
                    String descriptor,
                    String signature,
                    String[] exceptions
            ) {
                if (!"(I)I".equals(descriptor)) return null;
                Shift shift = new Shift();
                result.put(name, shift);
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                        if (!MODIFY_CONSTANT.equals(descriptor)) return null;
                        return modifyConstantAnnotation(shift);
                    }

                    @Override
                    public void visitIntInsn(int opcode, int operand) {
                        if ((opcode == Opcodes.BIPUSH || opcode == Opcodes.SIPUSH) && operand == 27) {
                            shift.addend = operand;
                        }
                    }

                    @Override
                    public void visitInsn(int opcode) {
                        if (opcode == Opcodes.IADD) shift.sawIntegerAdd = true;
                        if (opcode == Opcodes.IRETURN) shift.sawReturn = true;
                    }
                };
            }
        }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return result;
    }

    private static AnnotationVisitor modifyConstantAnnotation(Shift shift) {
        return new AnnotationVisitor(Opcodes.ASM9) {
            @Override
            public AnnotationVisitor visitArray(String name) {
                if ("method".equals(name)) {
                    return new AnnotationVisitor(Opcodes.ASM9) {
                        @Override
                        public void visit(String ignored, Object value) {
                            shift.targets.add((String) value);
                        }
                    };
                }
                if ("constant".equals(name)) {
                    return new AnnotationVisitor(Opcodes.ASM9) {
                        @Override
                        public AnnotationVisitor visitAnnotation(String ignored, String descriptor) {
                            if (!CONSTANT.equals(descriptor)) return null;
                            return new AnnotationVisitor(Opcodes.ASM9) {
                                @Override
                                public void visit(String name, Object value) {
                                    if ("intValue".equals(name)) shift.constants.add((Integer) value);
                                    if ("ordinal".equals(name)) shift.ordinal = (Integer) value;
                                }
                            };
                        }
                    };
                }
                return null;
            }
        };
    }

    private static Slot slot(Inventory owner, int physical) {
        return new Slot(owner, physical, 0, 0);
    }

    private static byte[] jarBytes(String entryName) throws IOException {
        try (JarFile jar = new JarFile(INVENTORY_EXTENDED.toFile())) {
            var entry = jar.getJarEntry(entryName);
            assertTrue(entry != null, "Missing accepted artifact entry " + entryName);
            try (InputStream input = jar.getInputStream(entry)) {
                return input.readAllBytes();
            }
        }
    }

    private static String jarText(String entryName) throws IOException {
        return new String(jarBytes(entryName), java.nio.charset.StandardCharsets.UTF_8);
    }

    private static String sha256(Path path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = Files.newInputStream(path)) {
            byte[] buffer = new byte[8192];
            for (int read; (read = input.read(buffer)) >= 0; ) {
                digest.update(buffer, 0, read);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static final class Shift {
        private final List<Integer> constants = new ArrayList<>();
        private final List<String> targets = new ArrayList<>();
        private int ordinal = -1;
        private int addend;
        private boolean sawIntegerAdd;
        private boolean sawReturn;

        private int apply(int original) {
            return original + addend;
        }
    }
}
