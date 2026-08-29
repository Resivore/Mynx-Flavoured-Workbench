package dev.resivore.stacksarestackscontainerfixes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

class VanillaRenderingContractTest {
    @Test
    void clientSimpleContainerClampsButPlayerInventorySetDoesNot() throws IOException {
        MethodNode simpleSet = method(
                readClass("net/minecraft/world/SimpleContainer.class"),
                "setItem",
                "(ILnet/minecraft/world/item/ItemStack;)V");
        assertEquals(1, calls(simpleSet, "net/minecraft/world/item/ItemStack", "limitSize", "(I)V"));

        MethodNode inventorySet = method(
                readClass("net/minecraft/world/entity/player/Inventory.class"),
                "setItem",
                "(ILnet/minecraft/world/item/ItemStack;)V");
        assertEquals(0, calls(inventorySet, "net/minecraft/world/item/ItemStack", "limitSize", "(I)V"));
    }

    @Test
    void slotCountRendererUsesActualCountWithoutMaximumAssumption() throws IOException {
        MethodNode itemCount = method(
                readClass("net/minecraft/client/gui/GuiGraphicsExtractor.class"),
                "itemCount",
                "(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V");

        assertEquals(2, calls(itemCount, "net/minecraft/world/item/ItemStack", "getCount", "()I"));
        assertEquals(0, calls(itemCount, "net/minecraft/world/item/ItemStack", "getMaxStackSize", "()I"));
        assertEquals(0, calls(itemCount, "net/minecraft/world/item/ItemStack", "isStackable", "()Z"));
    }

    @Test
    void containerContentsMaterializationUsesStrictTemplateValidation() throws IOException {
        ClassNode template = readClass("net/minecraft/world/item/ItemStackTemplate.class");
        MethodNode create = method(template, "create", "()Lnet/minecraft/world/item/ItemStack;");
        assertEquals(1, calls(create, template.name, "validate",
                "(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;"));

        MethodNode validate = method(template, "validate",
                "(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;");
        assertEquals(1, calls(validate, "net/minecraft/world/item/ItemStack", "validateStrict",
                "(Lnet/minecraft/world/item/ItemStack;)Lcom/mojang/serialization/DataResult;"));
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

    private static ClassNode readClass(String resourceName) throws IOException {
        try (InputStream stream = VanillaRenderingContractTest.class.getClassLoader()
                .getResourceAsStream(resourceName)) {
            assertNotNull(stream, resourceName);
            ClassNode node = new ClassNode();
            new org.objectweb.asm.ClassReader(stream).accept(node, 0);
            return node;
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
}
