package dev.resivore.tools;

import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/** Applies the narrow, hash-guarded Trinkets compatibility corrections. */
public final class TrinketsCompatPatcher {
    private static final String EXPECTED_INPUT_SHA256 =
            "958D064DA8DFA62C782D7CAA00D17F9BA5630301096A0E637788FAA2D982A564";
    private static final String CREATIVE_CLASS =
            "/eu/pb4/trinkets/mixin/client/CreativeModeInventoryScreenMixin.class";
    private static final String MENU_CLASS =
            "/eu/pb4/trinkets/mixin/InventoryMenuMixin.class";
    private static final String MENU_OWNER = "eu/pb4/trinkets/mixin/InventoryMenuMixin";

    private TrinketsCompatPatcher() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("Expected input and output JAR paths");
        Path input = Path.of(args[0]).toAbsolutePath().normalize();
        Path output = Path.of(args[1]).toAbsolutePath().normalize();
        requireHash(input, EXPECTED_INPUT_SHA256);
        Files.createDirectories(output.getParent());
        Files.copy(input, output, StandardCopyOption.REPLACE_EXISTING);

        try (FileSystem zip = FileSystems.newFileSystem(URI.create("jar:" + output.toUri()), Map.of())) {
            patchClass(zip.getPath(CREATIVE_CLASS), TrinketsCompatPatcher::patchCreativeSize);
            patchClass(zip.getPath(MENU_CLASS), TrinketsCompatPatcher::patchQuickMoveRanges);
        }
        System.out.println("Created " + output);
        System.out.println("SHA-256 " + sha256(output));
    }

    private static void patchClass(Path path, ClassPatch patch) throws Exception {
        if (!Files.isRegularFile(path)) throw new IllegalStateException("Missing target class " + path);
        FileTime time = Files.getLastModifiedTime(path);
        Files.write(path, patch.apply(Files.readAllBytes(path)));
        Files.setLastModifiedTime(path, time);
    }

    private static byte[] patchCreativeSize(byte[] original) {
        ClassNode node = read(original);
        MethodNode target = findUnique(node, "size", "(Lnet/minecraft/core/NonNullList;)I");
        int meaningful = 0;
        boolean saw46 = false;
        boolean sawReturn = false;
        for (AbstractInsnNode insn : target.instructions) {
            if (insn.getOpcode() < 0) continue;
            meaningful++;
            saw46 |= insn instanceof IntInsnNode value
                    && value.getOpcode() == Opcodes.BIPUSH && value.operand == 46;
            sawReturn |= insn.getOpcode() == Opcodes.IRETURN;
        }
        if (meaningful != 2 || !saw46 || !sawReturn) {
            throw new IllegalStateException("Refusing unexpected creative size method body");
        }
        InsnList replacement = new InsnList();
        replacement.add(new VarInsnNode(Opcodes.ALOAD, 0));
        replacement.add(new MethodInsnNode(
                Opcodes.INVOKEVIRTUAL,
                "eu/pb4/trinkets/mixin/client/CreativeModeInventoryScreenMixin",
                "trinkets$getHandler",
                "()Leu/pb4/trinkets/impl/TrinketInventoryMenu;",
                false));
        replacement.add(new MethodInsnNode(
                Opcodes.INVOKEINTERFACE,
                "eu/pb4/trinkets/impl/TrinketInventoryMenu",
                "trinkets$getTrinketSlotStart",
                "()I",
                true));
        replacement.add(new InsnNode(Opcodes.IRETURN));
        target.instructions = replacement;
        target.tryCatchBlocks.clear();
        if (target.localVariables != null) target.localVariables.clear();
        return write(node);
    }

    private static byte[] patchQuickMoveRanges(byte[] original) {
        ClassNode node = read(original);
        MethodNode target = findUnique(
                node,
                "quickMove",
                "(Lnet/minecraft/world/entity/player/Player;ILorg/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable;)V");
        int replacements = 0;
        for (AbstractInsnNode insn = target.instructions.getFirst(); insn != null; ) {
            AbstractInsnNode next = insn.getNext();
            if (insn instanceof IntInsnNode value
                    && value.getOpcode() == Opcodes.BIPUSH
                    && value.operand == 45) {
                InsnList dynamicEnd = new InsnList();
                dynamicEnd.add(new VarInsnNode(Opcodes.ALOAD, 0));
                dynamicEnd.add(new FieldInsnNode(
                        Opcodes.GETFIELD,
                        MENU_OWNER,
                        "owner",
                        "Lnet/minecraft/world/entity/player/Player;"));
                dynamicEnd.add(new MethodInsnNode(
                        Opcodes.INVOKEVIRTUAL,
                        "net/minecraft/world/entity/player/Player",
                        "getInventory",
                        "()Lnet/minecraft/world/entity/player/Inventory;",
                        false));
                dynamicEnd.add(new MethodInsnNode(
                        Opcodes.INVOKEVIRTUAL,
                        "net/minecraft/world/entity/player/Inventory",
                        "getNonEquipmentItems",
                        "()Lnet/minecraft/core/NonNullList;",
                        false));
                dynamicEnd.add(new MethodInsnNode(
                        Opcodes.INVOKEVIRTUAL,
                        "net/minecraft/core/NonNullList",
                        "size",
                        "()I",
                        false));
                dynamicEnd.add(new IntInsnNode(Opcodes.BIPUSH, 9));
                dynamicEnd.add(new InsnNode(Opcodes.IADD));
                target.instructions.insertBefore(insn, dynamicEnd);
                target.instructions.remove(insn);
                replacements++;
            }
            insn = next;
        }
        if (replacements != 2) {
            throw new IllegalStateException("Expected two quick-move range ends, found " + replacements);
        }
        return write(node);
    }

    private static ClassNode read(byte[] bytes) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        return node;
    }

    private static MethodNode findUnique(ClassNode node, String name, String descriptor) {
        MethodNode found = null;
        for (MethodNode method : node.methods) {
            if (method.name.equals(name) && method.desc.equals(descriptor)) {
                if (found != null) throw new IllegalStateException("Multiple target methods: " + name);
                found = method;
            }
        }
        if (found == null) throw new IllegalStateException("Target method not found: " + name + descriptor);
        return found;
    }

    private static byte[] write(ClassNode node) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static void requireHash(Path path, String expected) throws Exception {
        String actual = sha256(path);
        if (!actual.equals(expected)) {
            throw new IllegalStateException("Refusing unexpected input: expected " + expected + ", found " + actual);
        }
    }

    private static String sha256(Path path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (var stream = Files.newInputStream(path)) {
            byte[] buffer = new byte[8192];
            for (int read; (read = stream.read(buffer)) >= 0; ) digest.update(buffer, 0, read);
        }
        return HexFormat.of().withUpperCase().formatHex(digest.digest());
    }

    @FunctionalInterface
    private interface ClassPatch {
        byte[] apply(byte[] input) throws Exception;
    }
}
