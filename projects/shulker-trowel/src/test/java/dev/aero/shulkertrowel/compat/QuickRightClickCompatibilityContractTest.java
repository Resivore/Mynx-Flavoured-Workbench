package dev.aero.shulkertrowel.compat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.jar.JarFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuickRightClickCompatibilityContractTest {
    private static final String QUICK_RIGHT_CLICK_SHA256 =
            "87e77365919532bd38a004235c58e1220bbcbae680d8a3d27ed2ce2ddbd7831b";
    private static final Path PROJECT_ROOT = Path.of(System.getProperty("projectRoot"));
    private static final Path QUICK_RIGHT_CLICK = Path.of(System.getProperty("quickRightClickJar"));

    @Test
    void exactReferenceJarUsesTheAuditedShulkerDecisionPathAndNativeToggle() throws Exception {
        assertEquals(QUICK_RIGHT_CLICK_SHA256, sha256(QUICK_RIGHT_CLICK));
        try (JarFile jar = new JarFile(QUICK_RIGHT_CLICK.toFile())) {
            JsonObject metadata = JsonParser.parseString(read(jar, "fabric.mod.json")).getAsJsonObject();
            assertEquals("quickrightclick", metadata.get("id").getAsString());
            assertEquals("1.9", metadata.get("version").getAsString());

            MethodNode onItemClick = method(classNode(jar,
                    "com/natamus/quickrightclick_common_fabric/events/QuickEvent.class"),
                    "onItemClick",
                    "(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;");
            assertTrue(onItemClick.instructions.iterator().hasNext());
            assertTrue(hasType(onItemClick, "net/minecraft/world/level/block/ShulkerBoxBlock"));
            assertTrue(hasCall(onItemClick,
                    "com/natamus/quickrightclick_common_fabric/features/ShulkerBoxFeature", "init"));

            MethodNode shulkerFeature = method(classNode(jar,
                    "com/natamus/quickrightclick_common_fabric/features/ShulkerBoxFeature.class"),
                    "init",
                    "(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/level/block/Block;)Z");
            assertTrue(hasField(shulkerFeature,
                    "com/natamus/quickrightclick_common_fabric/config/ConfigHandler",
                    "enableQuickShulkerBoxes"));
        }
    }

    @Test
    void optionalMixinIsVersionGatedAndDoesNotTouchOtherQrcFeatures() throws IOException {
        String plugin = Files.readString(PROJECT_ROOT.resolve(
                "src/main/java/dev/aero/shulkertrowel/mixin/ShulkerTrowelMixinPlugin.java"));
        String mixin = Files.readString(PROJECT_ROOT.resolve(
                "src/main/java/dev/aero/shulkertrowel/mixin/QuickRightClickShulkerCompatibilityMixin.java"));

        assertTrue(plugin.contains("getModContainer(QuickRightClickCompatibility.MOD_ID)"));
        assertTrue(plugin.contains("QuickRightClickCompatibility.supports"));
        assertTrue(mixin.contains("QuickEvent"));
        assertTrue(mixin.contains("ShulkerBoxBlock"));
        assertTrue(mixin.contains("InteractionResult.PASS"));
        assertFalse(mixin.contains("BedBlockFeature"));
        assertFalse(mixin.contains("CraftingTableFeature"));
        assertFalse(mixin.contains("EnderChestFeature"));
    }

    @Test
    void compatibilityIsEnabledForOnlyTheExactAuditedModIdentity() {
        assertTrue(QuickRightClickCompatibility.supports("quickrightclick", "1.9"));
        assertFalse(QuickRightClickCompatibility.supports("quickrightclick", "1.9.1"));
        assertFalse(QuickRightClickCompatibility.supports("other", "1.9"));
    }

    private static ClassNode classNode(JarFile jar, String entryName) throws IOException {
        var entry = jar.getJarEntry(entryName);
        assertNotNull(entry, "Missing reference class " + entryName);
        ClassNode node = new ClassNode();
        try (InputStream input = jar.getInputStream(entry)) {
            new ClassReader(input).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        }
        return node;
    }

    private static MethodNode method(ClassNode owner, String name, String descriptor) {
        return owner.methods.stream()
                .filter(method -> method.name.equals(name) && method.desc.equals(descriptor))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing method " + owner.name + "." + name));
    }

    private static boolean hasType(MethodNode method, String descriptor) {
        for (var instruction : method.instructions) {
            if (instruction instanceof TypeInsnNode type && type.desc.equals(descriptor)) return true;
        }
        return false;
    }

    private static boolean hasCall(MethodNode method, String owner, String name) {
        for (var instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call
                    && call.owner.equals(owner) && call.name.equals(name)) return true;
        }
        return false;
    }

    private static boolean hasField(MethodNode method, String owner, String name) {
        for (var instruction : method.instructions) {
            if (instruction instanceof FieldInsnNode field
                    && field.getOpcode() == Opcodes.GETSTATIC
                    && field.owner.equals(owner) && field.name.equals(name)) return true;
        }
        return false;
    }

    private static String read(JarFile jar, String entryName) throws IOException {
        var entry = jar.getJarEntry(entryName);
        assertNotNull(entry, "Missing reference resource " + entryName);
        try (InputStream input = jar.getInputStream(entry)) {
            return new String(input.readAllBytes());
        }
    }

    private static String sha256(Path path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = Files.newInputStream(path)) {
            input.transferTo(new java.security.DigestOutputStream(java.io.OutputStream.nullOutputStream(), digest));
        }
        return HexFormat.of().formatHex(digest.digest());
    }
}
