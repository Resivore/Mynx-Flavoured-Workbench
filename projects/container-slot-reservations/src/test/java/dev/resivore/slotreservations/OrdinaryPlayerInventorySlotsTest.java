package dev.resivore.slotreservations;

import net.minecraft.core.NonNullList;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class OrdinaryPlayerInventorySlotsTest {
    private static final int MENU_SLOT = 17;

    private ServerPlayer player;
    private Inventory inventory;

    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @BeforeEach
    void setUp() {
        player = mock(ServerPlayer.class);
        inventory = mock(Inventory.class);
        when(player.getInventory()).thenReturn(inventory);
        when(inventory.getNonEquipmentItems())
                .thenReturn(NonNullList.withSize(63, ItemStack.EMPTY));
        when(inventory.stillValid(player)).thenReturn(true);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 35, 36, 62})
    void acceptsEveryRepresentativeLiveNonEquipmentCoordinate(int physicalPlayerSlot) {
        assertTrue(OrdinaryPlayerInventorySlots.isExactLiveSlot(
                player,
                slot(inventory, physicalPlayerSlot, MENU_SLOT, true, false),
                MENU_SLOT,
                physicalPlayerSlot));
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 63, 64, 65, 66, 67, 68, 69})
    void rejectsCoordinatesOutsideTheLiveNonEquipmentInventory(int physicalPlayerSlot) {
        assertFalse(OrdinaryPlayerInventorySlots.isExactLiveSlot(
                player,
                slot(inventory, physicalPlayerSlot, MENU_SLOT, true, false),
                MENU_SLOT,
                physicalPlayerSlot));
    }

    @Test
    void rejectsForeignFakeInactiveStaleAndInexactSlots() {
        Inventory foreignInventory = mock(Inventory.class);

        assertFalse(valid(slot(foreignInventory, 36, MENU_SLOT, true, false)),
                "foreign inventory identity");
        assertFalse(valid(slot(inventory, 36, MENU_SLOT, true, true)), "fake slot");
        assertFalse(valid(slot(inventory, 36, MENU_SLOT, false, false)), "inactive slot");

        when(inventory.stillValid(player)).thenReturn(false);
        assertFalse(valid(slot(inventory, 36, MENU_SLOT, true, false)), "stale inventory");
        when(inventory.stillValid(player)).thenReturn(true);

        assertFalse(OrdinaryPlayerInventorySlots.isExactLiveSlot(
                player, slot(inventory, 36, MENU_SLOT + 1, true, false), MENU_SLOT, 36),
                "wrong exact menu coordinate");
        assertFalse(OrdinaryPlayerInventorySlots.isExactLiveSlot(
                player, slot(inventory, 35, MENU_SLOT, true, false), MENU_SLOT, 36),
                "wrong exact physical coordinate");
        assertFalse(OrdinaryPlayerInventorySlots.isExactLiveSlot(
                player, null, MENU_SLOT, 36), "missing slot");
    }

    private boolean valid(Slot slot) {
        return OrdinaryPlayerInventorySlots.isExactLiveSlot(player, slot, MENU_SLOT, 36);
    }

    private static Slot slot(
            Container owner,
            int physicalPlayerSlot,
            int menuSlot,
            boolean active,
            boolean fake
    ) {
        Slot slot = new Slot(owner, physicalPlayerSlot, 0, 0) {
            @Override
            public boolean isActive() {
                return active;
            }

            @Override
            public boolean isFake() {
                return fake;
            }
        };
        slot.index = menuSlot;
        return slot;
    }
}
