package dev.resivore.mapmarkerextension.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.resivore.mapmarkerextension.MinecraftTestBootstrap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.saveddata.maps.MapId;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

final class ExternalNativeMapIdentityTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        MinecraftTestBootstrap.initialize();
    }

    @Test
    void ribbitIdentityPinsTheExternalOwnerMarkerAndNativeDecoration() {
        ExternalNativeMapIdentity identity = ExternalNativeMapIdentities.RIBBIT_VILLAGE;

        assertEquals("ribbits:ribbit_village_explorer_map", identity.id());
        assertEquals("ribbits:ribbit_village_explorer_map", identity.stackMarkerKey());
        assertEquals("ribbits:ribbit_village", identity.decorationTypeId());
        assertEquals(1, ExternalNativeMapIdentities.values().size());
        assertSame(identity, ExternalNativeMapIdentities.values().getFirst());
    }

    @Test
    void exactTrueMarkerOnARealFilledMapWithMapIdIsRequired() {
        ItemStack success = markedMap(
            ExternalNativeMapIdentities.RIBBIT_VILLAGE.stackMarkerKey(),
            true,
            true
        );

        assertSame(
            ExternalNativeMapIdentities.RIBBIT_VILLAGE,
            ExternalNativeMapIdentities.find(success).orElseThrow()
        );

        assertTrue(ExternalNativeMapIdentities.find(markedMap(
            ExternalNativeMapIdentities.RIBBIT_VILLAGE.stackMarkerKey(), false, true
        )).isEmpty());
        assertTrue(ExternalNativeMapIdentities.find(markedMap(
            "ribbits:failed_ribbit_village_map", true, true
        )).isEmpty());
        assertTrue(ExternalNativeMapIdentities.find(markedMap(
            ExternalNativeMapIdentities.RIBBIT_VILLAGE.stackMarkerKey(), true, false
        )).isEmpty());

        ItemStack emptyMapWithId = new ItemStack(Items.MAP);
        emptyMapWithId.set(DataComponents.MAP_ID, new MapId(42));
        CompoundTag emptyMapMarker = new CompoundTag();
        emptyMapMarker.putBoolean(
            ExternalNativeMapIdentities.RIBBIT_VILLAGE.stackMarkerKey(),
            true
        );
        emptyMapWithId.set(DataComponents.CUSTOM_DATA, CustomData.of(emptyMapMarker));
        assertTrue(ExternalNativeMapIdentities.find(emptyMapWithId).isEmpty());
    }

    @Test
    void displayNamesAndUnrelatedCustomDataDoNotReplaceTheStableMarker() {
        ItemStack stack = markedMap(
            ExternalNativeMapIdentities.RIBBIT_VILLAGE.stackMarkerKey(), true, true
        );
        CustomData before = stack.get(DataComponents.CUSTOM_DATA);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("My Ribbit Expedition"));

        assertSame(
            ExternalNativeMapIdentities.RIBBIT_VILLAGE,
            ExternalNativeMapIdentities.find(stack).orElseThrow()
        );
        assertEquals(before, stack.get(DataComponents.CUSTOM_DATA));
        assertTrue(
            MapMarkerItemIdentity.find(stack).isEmpty(),
            "the external Ribbits map must not become one of MME's 18 owned identities"
        );
    }

    private static ItemStack markedMap(String key, boolean value, boolean withMapId) {
        ItemStack stack = new ItemStack(Items.FILLED_MAP);
        if (withMapId) {
            stack.set(DataComponents.MAP_ID, new MapId(41));
        }
        CompoundTag marker = new CompoundTag();
        marker.putBoolean(key, value);
        marker.putString("example:unrelated", "preserved");
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(marker));
        return stack;
    }
}
