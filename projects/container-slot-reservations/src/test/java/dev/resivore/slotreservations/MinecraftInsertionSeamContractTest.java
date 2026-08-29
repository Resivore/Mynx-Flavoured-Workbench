package dev.resivore.slotreservations;

import java.io.IOException;
import java.io.InputStream;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.MethodModel;
import java.lang.classfile.instruction.FieldInstruction;
import java.lang.classfile.instruction.InvokeInstruction;

import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pins the vanilla 26.2 seams used by the narrow reservation enforcement mixins. */
final class MinecraftInsertionSeamContractTest {
    private static final String ITEM_STACK = "Lnet/minecraft/world/item/ItemStack;";
    private static final String CONTAINER = "net/minecraft/world/Container";
    private static final String SLOT = "net/minecraft/world/inventory/Slot";

    @Test
    void safeInsertConsultsSlotMayPlace() throws IOException {
        MethodModel safeInsert = method(Slot.class, "safeInsert", "(" + ITEM_STACK + "I)" + ITEM_STACK);
        assertTrue(calls(safeInsert, SLOT, "mayPlace", "(" + ITEM_STACK + ")Z"),
                "Slot.safeInsert(ItemStack,int) no longer delegates admission to Slot.mayPlace");
    }

    @Test
    void ordinaryManualPickupAndPlaceConsultsSlotMayPlaceDirectly() throws IOException {
        MethodModel click = method(AbstractContainerMenu.class, "doClick",
                "(IILnet/minecraft/world/inventory/ContainerInput;"
                        + "Lnet/minecraft/world/entity/player/Player;)V");
        assertTrue(calls(click, SLOT, "mayPlace", "(" + ITEM_STACK + ")Z"),
                "AbstractContainerMenu.doClick no longer checks destination Slot.mayPlace");
    }

    @Test
    void quickMoveEmptySlotPhaseConsultsSlotMayPlace() throws IOException {
        MethodModel move = method(AbstractContainerMenu.class, "moveItemStackTo",
                "(" + ITEM_STACK + "IIZ)Z");
        assertTrue(calls(move, SLOT, "mayPlace", "(" + ITEM_STACK + ")Z"),
                "AbstractContainerMenu.moveItemStackTo no longer checks Slot.mayPlace");
    }

    @Test
    void bundleEjectionConsultsSlotSafeInsertAndThereforeMayPlace() throws IOException {
        MethodModel ejection = method(BundleItem.class, "overrideStackedOnOther",
                "(" + ITEM_STACK
                        + "Lnet/minecraft/world/inventory/Slot;"
                        + "Lnet/minecraft/world/inventory/ClickAction;"
                        + "Lnet/minecraft/world/entity/player/Player;)Z");
        assertTrue(calls(ejection, SLOT, "safeInsert", "(" + ITEM_STACK + ")" + ITEM_STACK),
                "Bundle ejection no longer routes through the reservation-aware Slot admission seam");
    }

    @Test
    void hopperAdmissionConsultsContainerCanPlaceItem() throws IOException {
        MethodModel admission = method(HopperBlockEntity.class, "canPlaceItemInContainer",
                "(Lnet/minecraft/world/Container;" + ITEM_STACK + "ILnet/minecraft/core/Direction;)Z");
        assertTrue(calls(admission, CONTAINER, "canPlaceItem", "(I" + ITEM_STACK + ")Z"),
                "HopperBlockEntity no longer delegates target admission to Container.canPlaceItem");
    }

    @Test
    void compoundContainerDelegatesEachLogicalHalfToItsPhysicalOwner() throws IOException {
        MethodModel admission = method(CompoundContainer.class, "canPlaceItem", "(I" + ITEM_STACK + ")Z");
        long delegates = admission.code().orElseThrow().elementStream()
                .filter(InvokeInstruction.class::isInstance)
                .map(InvokeInstruction.class::cast)
                .filter(instruction -> instruction.owner().asInternalName().equals(CONTAINER))
                .filter(instruction -> instruction.name().equalsString("canPlaceItem"))
                .filter(instruction -> instruction.type().equalsString("(I" + ITEM_STACK + ")Z"))
                .count();
        assertTrue(delegates >= 2,
                "CompoundContainer must continue delegating both index ranges to their backing containers");
    }

    @Test
    void screenRenderingUsesLiveSlotCoordinatesAndTooltipSeam() throws IOException {
        MethodModel extractSlot = method(AbstractContainerScreen.class, "extractSlot",
                "(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/world/inventory/Slot;II)V");
        assertTrue(readsField(extractSlot, SLOT, "x", "I"), "extractSlot no longer reads live Slot.x");
        assertTrue(readsField(extractSlot, SLOT, "y", "I"), "extractSlot no longer reads live Slot.y");
        assertNotNull(method(AbstractContainerScreen.class, "extractTooltip",
                "(Lnet/minecraft/client/gui/GuiGraphicsExtractor;II)V"));
    }

    @Test
    void keyboardInitialPressSeamRetainsTheMappedKeyEventSignature() throws IOException {
        assertNotNull(method(KeyboardHandler.class, "keyPress",
                "(JILnet/minecraft/client/input/KeyEvent;)V"));
    }

    private static MethodModel method(Class<?> owner, String name, String descriptor) throws IOException {
        return model(owner).methods().stream()
                .filter(method -> method.methodName().equalsString(name))
                .filter(method -> method.methodType().equalsString(descriptor))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing mapped method " + owner.getName() + "." + name
                        + descriptor));
    }

    private static ClassModel model(Class<?> type) throws IOException {
        String resource = "/" + type.getName().replace('.', '/') + ".class";
        try (InputStream input = type.getResourceAsStream(resource)) {
            if (input == null) throw new AssertionError("Missing class resource " + resource);
            return ClassFile.of().parse(input.readAllBytes());
        }
    }

    private static boolean calls(MethodModel method, String owner, String name, String descriptor) {
        return method.code().orElseThrow().elementStream()
                .filter(InvokeInstruction.class::isInstance)
                .map(InvokeInstruction.class::cast)
                .anyMatch(instruction -> instruction.owner().asInternalName().equals(owner)
                        && instruction.name().equalsString(name)
                        && instruction.type().equalsString(descriptor));
    }

    private static boolean readsField(MethodModel method, String owner, String name, String descriptor) {
        return method.code().orElseThrow().elementStream()
                .filter(FieldInstruction.class::isInstance)
                .map(FieldInstruction.class::cast)
                .anyMatch(instruction -> instruction.owner().asInternalName().equals(owner)
                        && instruction.name().equalsString(name)
                        && instruction.type().equalsString(descriptor));
    }
}
