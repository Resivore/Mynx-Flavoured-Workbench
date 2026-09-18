package dev.resivore.slotreservations.client;

import dev.resivore.slotreservations.mixin.client.CreativeSlotWrapperAccess;
import net.minecraft.SharedConstants;
import net.minecraft.core.NonNullList;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Native-Slot traversal and Inventory Extended-style live-boundary contracts. */
final class CarriedShulkerSourceSlotTest {
    private Player player;
    private Inventory inventory;

    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @BeforeEach
    void setUp() {
        player = mock(Player.class);
        inventory = mock(Inventory.class);
        when(player.getInventory()).thenReturn(inventory);
        when(inventory.getNonEquipmentItems())
                .thenReturn(NonNullList.withSize(63, ItemStack.EMPTY));
        when(inventory.stillValid(player)).thenReturn(true);
    }

    @Test
    void resolvesActualVisibleSlotIdentityAndExtendedPhysicalCoordinate() {
        TestMenu menu = new TestMenu();
        Slot hovered = menu.expose(new Slot(inventory, 62, 0, 0));

        CarriedShulkerSourceSlot.Resolution resolution = CarriedShulkerSourceSlot.resolve(
                player, menu, hovered, false, false);

        assertNotNull(resolution);
        assertSame(hovered, resolution.hovered());
        assertSame(hovered, resolution.backing());
        assertEquals(0, resolution.key().menuSlot());
        assertEquals(62, resolution.key().physicalPlayerSlot());
    }

    @Test
    void rejectsEquipmentForeignAndInvisibleSlotsUsingLiveStructure() {
        TestMenu menu = new TestMenu();
        assertNull(CarriedShulkerSourceSlot.resolve(player, menu,
                menu.expose(new Slot(inventory, 63, 0, 0)), false, false));

        Inventory foreign = mock(Inventory.class);
        assertNull(CarriedShulkerSourceSlot.resolve(player, menu,
                menu.expose(new Slot(foreign, 36, 0, 0)), false, false));

        assertNull(CarriedShulkerSourceSlot.resolve(player, menu,
                new Slot(inventory, 36, 0, 0), false, false));
    }

    @Test
    void creativeInventoryWrapperUsesItsBackingInventorySlotNotPresentationCoordinate() {
        TestMenu menu = new TestMenu();
        Slot backing = new Slot(inventory, 36, 0, 0);
        Slot wrapper = menu.expose(new TestCreativeWrapper(backing, inventory, 67));

        CarriedShulkerSourceSlot.Resolution resolution = CarriedShulkerSourceSlot.resolve(
                player, menu, wrapper, true, true);

        assertNotNull(resolution);
        assertSame(wrapper, resolution.hovered());
        assertSame(backing, resolution.backing());
        assertEquals(-1, resolution.key().menuSlot());
        assertEquals(36, resolution.key().physicalPlayerSlot());
    }

    @Test
    void creativeEquipmentBackingAndNonWrapperInventoryTabSlotsAreRejected() {
        TestMenu menu = new TestMenu();
        Slot equipment = menu.expose(new TestCreativeWrapper(
                new Slot(inventory, 67, 0, 0), inventory, 10));
        Slot direct = menu.expose(new Slot(inventory, 36, 0, 0));

        assertNull(CarriedShulkerSourceSlot.resolve(player, menu,
                equipment, true, true));
        assertNull(CarriedShulkerSourceSlot.resolve(player, menu,
                direct, true, true));
    }

    private static final class TestMenu extends AbstractContainerMenu {
        TestMenu() {
            super(null, 7);
        }

        Slot expose(Slot slot) {
            return addSlot(slot);
        }

        @Override
        public ItemStack quickMoveStack(Player player, int index) {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }
    }

    private static final class TestCreativeWrapper extends Slot
            implements CreativeSlotWrapperAccess {
        private final Slot target;

        TestCreativeWrapper(Slot target, Container presentationOwner, int presentationSlot) {
            super(presentationOwner, presentationSlot, 0, 0);
            this.target = target;
        }

        @Override
        public Slot containerSlotReservations$getTarget() {
            return target;
        }
    }
}
