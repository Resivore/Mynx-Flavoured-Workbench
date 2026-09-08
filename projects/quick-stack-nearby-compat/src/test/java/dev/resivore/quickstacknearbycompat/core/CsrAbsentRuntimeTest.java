package dev.resivore.quickstacknearbycompat.core;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tempeststudios.quickstacknearby.QuickStackMoveEngine;

import java.util.List;
import java.util.OptionalInt;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsrAbsentRuntimeTest {
    private static final String CSR_API_RESOURCE =
            "dev/resivore/slotreservations/api/ContainerSlotReservationsApi.class";

    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        bindTestComponents(Items.COBBLESTONE);
        bindTestComponents(Items.DIRT);
    }

    @Test
    void publicIntegrationGateIsAnExactNoOpWithoutCsr() {
        assertFalse(FabricLoader.getInstance().isModLoaded("container_slot_reservations"));
        if (Boolean.getBoolean("quickStackNearbyCompat.excludeCsrRuntime")) {
            assertNull(CsrAbsentRuntimeTest.class.getClassLoader().getResource(CSR_API_RESOURCE),
                    "The focused absent-runtime test unexpectedly has CSR's API on its classpath");
        }

        ItemStack moving = new ItemStack(Items.COBBLESTONE, 17);
        SimpleContainer source = new SimpleContainer(2);
        source.setItem(0, moving);
        ItemStack existing = new ItemStack(Items.DIRT, 5);
        SimpleContainer target = new SimpleContainer(3);
        target.setItem(2, existing);
        QuickStackMoveEngine.Target targetView = new QuickStackMoveEngine.Target(target, Set.of());
        List<QuickStackMoveEngine.Target> originalTargets = List.of(targetView);

        List<QuickStackMoveEngine.Target> augmented = CsrQuickStackIntegration.augmentTargets(
                source,
                0,
                source.getContainerSize(),
                originalTargets,
                QuickStackMoveEngine.SourceRules.EMPTY
        );
        Set<QuickStackMoveEngine.StackKey> nativeTypes = targetView.acceptedTypes();
        Set<QuickStackMoveEngine.StackKey> discoveryTypes =
                CsrQuickStackIntegration.withDiscoverySource(
                        source,
                        0,
                        source.getContainerSize(),
                        QuickStackMoveEngine.SourceRules.EMPTY,
                        () -> CsrQuickStackIntegration.augmentDiscoveredAcceptedTypes(
                                target,
                                nativeTypes
                        )
                );
        OptionalInt inserted = CsrQuickStackIntegration.insertIntoEmptySlots(moving, target);

        assertSame(originalTargets, augmented);
        assertSame(targetView, augmented.getFirst());
        assertSame(nativeTypes, discoveryTypes);
        assertSame(moving, source.getItem(0));
        assertEquals(17, moving.getCount());
        assertTrue(inserted.isEmpty());
        assertTrue(target.getItem(0).isEmpty());
        assertTrue(target.getItem(1).isEmpty());
        assertSame(existing, target.getItem(2));
        assertEquals(5, existing.getCount());
    }

    private static void bindTestComponents(Item item) {
        if (!item.builtInRegistryHolder().areComponentsBound()) {
            item.builtInRegistryHolder().bindComponents(DataComponentMap.builder()
                    .set(DataComponents.MAX_STACK_SIZE, 64)
                    .build());
        }
    }
}
