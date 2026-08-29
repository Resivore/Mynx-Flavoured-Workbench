package dev.resivore.radialslotcycler.core;

import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrdinaryInventorySnapshotTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        bindTestComponents(Items.DIAMOND_PICKAXE);
        bindTestComponents(Items.TORCH);
    }

    @Test
    void captureIsAValueSnapshotAndUnchangedContentsMatch() {
        List<ItemStack> live = new ArrayList<>(List.of(
                new ItemStack(Items.DIAMOND_PICKAXE),
                new ItemStack(Items.TORCH, 32)));
        List<ItemStack> snapshot = OrdinaryInventorySnapshot.capture(live);

        assertNotSame(live.get(0), snapshot.get(0));
        assertTrue(OrdinaryInventorySnapshot.matches(snapshot, live));
    }

    @Test
    void countsComponentsAndSlotOrderAllInvalidateTheSession() {
        ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
        ItemStack torches = new ItemStack(Items.TORCH, 32);
        List<ItemStack> live = new ArrayList<>(List.of(pickaxe, torches));
        List<ItemStack> snapshot = OrdinaryInventorySnapshot.capture(live);

        torches.setCount(31);
        assertFalse(OrdinaryInventorySnapshot.matches(snapshot, live));
        torches.setCount(32);
        pickaxe.set(DataComponents.CUSTOM_NAME, Component.literal("changed"));
        assertFalse(OrdinaryInventorySnapshot.matches(snapshot, live));
        pickaxe.remove(DataComponents.CUSTOM_NAME);
        live.set(0, torches);
        live.set(1, pickaxe);
        assertFalse(OrdinaryInventorySnapshot.matches(snapshot, live));
        assertFalse(OrdinaryInventorySnapshot.matches(snapshot, live.subList(0, 1)));
    }

    private static void bindTestComponents(Item item) {
        if (!item.builtInRegistryHolder().areComponentsBound()) {
            item.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
        }
    }
}
