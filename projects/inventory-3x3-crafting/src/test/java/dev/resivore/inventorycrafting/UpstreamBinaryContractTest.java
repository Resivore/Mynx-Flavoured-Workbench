package dev.resivore.inventorycrafting;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpstreamBinaryContractTest {
    private static final Path WORKBENCH = Path.of(System.getProperty("workbenchRoot"));
    private static final Path MODS = WORKBENCH.resolve("originals/mods");
    private static final String MIXIN_DESC = "Lorg/spongepowered/asm/mixin/Mixin;";

    @Test
    void auditedPristineDependencyHashesStillMatch() throws Exception {
        Map<String, String> expected = Map.of(
                "inventoryextended-1.1.2-mc26.2.jar", "7CDBE2079D5E8BE9C5FABA8B03DBCCC1DDCCB7F99EC48987079CC9BD8E235BC6",
                "simple_trash_slot-1.0.4+26.1.2-fabric.jar", "81790940DA606F732A565ADCA2B4EC9FA0E8E0015CB1CDC1C4E009506244739B",
                "trinkets-4.1.0-beta.3+26.2.jar.disabled", "958D064DA8DFA62C782D7CAA00D17F9BA5630301096A0E637788FAA2D982A564",
                "jei-26.2-fabric-30.18.0.144.jar", "20BC7F0EBE5F36F84C8C4D571469968BE54A6E1989B4E85A736136DC41EA8FE2",
                "Simple-Portable-Crafting-1.0.0+mc26.2.jar", "34C7B4AC4FF2EDC46EEFC21429416603E81B6D3944EA508E4D7CAFBE41C17DA4"
        );

        for (var entry : expected.entrySet()) {
            assertEquals(entry.getValue(), sha256(MODS.resolve(entry.getKey())), entry.getKey());
        }
    }

    @Test
    void constructorInjectionPrioritiesRemainAtAuditedBoundaries() throws IOException {
        ClassNode trash = readClass(
                MODS.resolve("simple_trash_slot-1.0.4+26.1.2-fabric.jar"),
                "me/pajic/simple_trash_slot/mixin/InventoryMenuMixin.class"
        );
        ClassNode trinkets = readClass(
                MODS.resolve("trinkets-4.1.0-beta.3+26.2.jar.disabled"),
                "eu/pb4/trinkets/mixin/InventoryMenuMixin.class"
        );

        assertEquals(1, annotationInt(trash, MIXIN_DESC, "priority"));
        assertEquals(500, annotationInt(trinkets, MIXIN_DESC, "priority"));
        assertTrue(trash.methods.stream().anyMatch(method -> method.name.toLowerCase().contains("trash")));
        assertTrue(trinkets.methods.stream().anyMatch(method -> method.name.contains("updateTrinketSlots")));
    }

    @Test
    void actualMixinPrioritiesSortCallbacksTrashThenCraftThenTrinkets() throws IOException {
        ClassNode trash = readClass(
                MODS.resolve("simple_trash_slot-1.0.4+26.1.2-fabric.jar"),
                "me/pajic/simple_trash_slot/mixin/InventoryMenuMixin.class"
        );
        ClassNode project = readRuntimeClass(
                "dev/resivore/inventorycrafting/mixin/InventoryMenuMixin.class"
        );
        ClassNode trinkets = readClass(
                MODS.resolve("trinkets-4.1.0-beta.3+26.2.jar.disabled"),
                "eu/pb4/trinkets/mixin/InventoryMenuMixin.class"
        );

        List<PriorityOwner> applicationOrder = new ArrayList<>(List.of(
                new PriorityOwner("Trash", annotationInt(trash, MIXIN_DESC, "priority")),
                new PriorityOwner("Craft", annotationInt(project, MIXIN_DESC, "priority")),
                new PriorityOwner("Trinkets", annotationInt(trinkets, MIXIN_DESC, "priority"))
        ));
        applicationOrder.sort(Comparator.comparingInt(PriorityOwner::priority));
        assertEquals(List.of("Trash", "Craft", "Trinkets"),
                applicationOrder.stream().map(PriorityOwner::owner).toList());

        ClassNode mixinInfo = readRuntimeClass("org/spongepowered/asm/mixin/transformer/MixinInfo.class");
        MethodNode compareTo = mixinInfo.methods.stream()
                .filter(method -> method.name.equals("compareTo")
                        && method.desc.equals("(Lorg/spongepowered/asm/mixin/transformer/MixinInfo;)I"))
                .findFirst()
                .orElseThrow();
        long priorityReads = 0;
        for (AbstractInsnNode insn : compareTo.instructions) {
            if (insn instanceof org.objectweb.asm.tree.FieldInsnNode field
                    && field.name.equals("priority")) {
                priorityReads++;
            }
        }
        assertTrue(priorityReads >= 2, "MixinInfo.compareTo must still order on numeric priority");
    }

    @Test
    void productionDimensionHookIsStaticAndRequiresBothPreSuperConstants() throws IOException {
        ClassNode project = readRuntimeClass(
                "dev/resivore/inventorycrafting/mixin/InventoryMenuMixin.class"
        );
        MethodNode dimensionHook = method(
                project,
                "inventory3x3$constructNineCellContainer",
                "(I)I"
        );
        assertTrue((dimensionHook.access & Opcodes.ACC_STATIC) != 0,
                "a constructor pre-super hook must not invoke against uninitialized this");
        String modifyConstant = "Lorg/spongepowered/asm/mixin/injection/ModifyConstant;";
        assertEquals(2, annotationInt(dimensionHook, modifyConstant, "require"));
        assertEquals(2, annotationInt(dimensionHook, modifyConstant, "expect"));
    }

    @Test
    void vanillaInventoryMenuStillHasTheNarrowConstructorAndQuickMoveSeams() throws IOException {
        ClassNode menu = readRuntimeClass("net/minecraft/world/inventory/InventoryMenu.class");
        MethodNode constructor = method(menu, "<init>", "(Lnet/minecraft/world/entity/player/Inventory;ZLnet/minecraft/world/entity/player/Player;)V");
        List<Integer> constantsBeforeSuper = new ArrayList<>();
        boolean sawSuper = false;
        int gridCalls = 0;
        int resultCalls = 0;
        for (AbstractInsnNode insn : constructor.instructions) {
            if (!sawSuper && insn.getOpcode() >= Opcodes.ICONST_M1 && insn.getOpcode() <= Opcodes.ICONST_5) {
                constantsBeforeSuper.add(insn.getOpcode() - Opcodes.ICONST_0);
            }
            if (insn instanceof MethodInsnNode call) {
                if (call.owner.equals("net/minecraft/world/inventory/AbstractCraftingMenu") && call.name.equals("<init>")) {
                    sawSuper = true;
                }
                if (call.name.equals("addCraftingGridSlots") && call.desc.equals("(II)V")) {
                    gridCalls++;
                }
                if (call.name.equals("addResultSlot") && call.desc.endsWith("II)Lnet/minecraft/world/inventory/Slot;")) {
                    resultCalls++;
                }
            }
        }
        assertTrue(constantsBeforeSuper.containsAll(List.of(0, 2, 2)));
        assertEquals(1, gridCalls);
        assertEquals(1, resultCalls);

        MethodNode quickMove = method(menu, "quickMoveStack", "(Lnet/minecraft/world/entity/player/Player;I)Lnet/minecraft/world/item/ItemStack;");
        assertTrue(intConstants(quickMove).containsAll(List.of(1, 5, 9, 45)));
    }

    @Test
    void vanillaEntityAndNamedSlotRangesStillExposeFourCellsBeforeTransformation() throws IOException {
        ClassNode player = readRuntimeClass("net/minecraft/world/entity/player/Player.class");
        MethodNode getSlot = method(player, "getSlot", "(I)Lnet/minecraft/world/entity/SlotAccess;");
        assertTrue(intConstants(getSlot).contains(4));
        assertTrue(intConstants(getSlot).contains(500));

        ClassNode ranges = readRuntimeClass("net/minecraft/world/inventory/SlotRanges.class");
        MethodNode lambda = ranges.methods.stream()
                .filter(method -> method.name.equals("lambda$static$0"))
                .findFirst()
                .orElseThrow();
        assertTrue(lambda.instructions.iterator().hasNext());
        assertTrue(ldcStrings(lambda).contains("player.crafting."));
        assertTrue(intConstants(lambda).containsAll(List.of(4, 500)));
    }

    @Test
    void jeiBuiltinStillCarriesTheFourCellAndThirtySixSourceAssumptions() throws IOException {
        ClassNode handler = readClass(
                MODS.resolve("jei-26.2-fabric-30.18.0.144.jar"),
                "mezz/jei/library/transfer/PlayerRecipeTransferHandler.class"
        );
        MethodNode constructor = method(handler, "<init>", "(Lmezz/jei/api/recipe/transfer/IRecipeTransferHandlerHelper;)V");
        List<Integer> constants = intConstants(constructor);
        assertTrue(constants.containsAll(List.of(1, 4, 9, 36)));
    }

    private static ClassNode readRuntimeClass(String resource) throws IOException {
        try (InputStream stream = Objects.requireNonNull(
                UpstreamBinaryContractTest.class.getClassLoader().getResourceAsStream(resource), resource
        )) {
            return readClass(stream);
        }
    }

    private static ClassNode readClass(Path jar, String entryName) throws IOException {
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            ZipEntry entry = Objects.requireNonNull(zip.getEntry(entryName), entryName);
            try (InputStream stream = zip.getInputStream(entry)) {
                return readClass(stream);
            }
        }
    }

    private static ClassNode readClass(InputStream stream) throws IOException {
        ClassNode node = new ClassNode();
        new org.objectweb.asm.ClassReader(stream).accept(node, 0);
        return node;
    }

    private static MethodNode method(ClassNode owner, String name, String desc) {
        return owner.methods.stream()
                .filter(method -> method.name.equals(name) && method.desc.equals(desc))
                .findFirst()
                .orElseThrow(() -> new AssertionError(owner.name + "." + name + desc));
    }

    private static int annotationInt(ClassNode owner, String desc, String key) {
        List<AnnotationNode> annotations = new ArrayList<>();
        if (owner.visibleAnnotations != null) annotations.addAll(owner.visibleAnnotations);
        if (owner.invisibleAnnotations != null) annotations.addAll(owner.invisibleAnnotations);
        AnnotationNode annotation = annotations.stream()
                .filter(value -> value.desc.equals(desc))
                .findFirst()
                .orElseThrow();
        for (int index = 0; index < annotation.values.size(); index += 2) {
            if (annotation.values.get(index).equals(key)) {
                return (Integer) annotation.values.get(index + 1);
            }
        }
        throw new AssertionError(owner.name + " annotation " + Type.getType(desc).getClassName() + " lacks " + key);
    }

    private static int annotationInt(MethodNode owner, String desc, String key) {
        List<AnnotationNode> annotations = new ArrayList<>();
        if (owner.visibleAnnotations != null) annotations.addAll(owner.visibleAnnotations);
        if (owner.invisibleAnnotations != null) annotations.addAll(owner.invisibleAnnotations);
        AnnotationNode annotation = annotations.stream()
                .filter(value -> value.desc.equals(desc))
                .findFirst()
                .orElseThrow();
        for (int index = 0; index < annotation.values.size(); index += 2) {
            if (annotation.values.get(index).equals(key)) {
                return (Integer) annotation.values.get(index + 1);
            }
        }
        throw new AssertionError(owner.name + " annotation " + Type.getType(desc).getClassName() + " lacks " + key);
    }

    private static List<Integer> intConstants(MethodNode method) {
        List<Integer> values = new ArrayList<>();
        for (AbstractInsnNode insn : method.instructions) {
            int opcode = insn.getOpcode();
            if (opcode >= Opcodes.ICONST_M1 && opcode <= Opcodes.ICONST_5) {
                values.add(opcode - Opcodes.ICONST_0);
            } else if (insn instanceof IntInsnNode value) {
                values.add(value.operand);
            } else if (insn instanceof LdcInsnNode value && value.cst instanceof Integer integer) {
                values.add(integer);
            }
        }
        return values;
    }

    private static List<String> ldcStrings(MethodNode method) {
        List<String> values = new ArrayList<>();
        for (AbstractInsnNode insn : method.instructions) {
            if (insn instanceof LdcInsnNode value && value.cst instanceof String string) {
                values.add(string);
            }
        }
        return values;
    }

    private static String sha256(Path path) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream stream = Files.newInputStream(path)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = stream.read(buffer)) >= 0) {
                digest.update(buffer, 0, read);
            }
        }
        return HexFormat.of().withUpperCase().formatHex(digest.digest());
    }

    private record PriorityOwner(String owner, int priority) {
    }
}
