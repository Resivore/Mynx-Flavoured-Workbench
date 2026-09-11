package dev.resivore.slotreservations;

import com.mojang.serialization.JsonOps;
import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.crafting.TransmuteRecipe;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Focused format-2 coverage for CSR's physical portable-container reservation rule. */
final class PortableContainerIdentityTest {
    @BeforeAll
    static void bootstrap() throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        var registry = net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_TYPE;
        var frozen = net.minecraft.core.MappedRegistry.class.getDeclaredField("frozen");
        frozen.setAccessible(true);
        boolean wasFrozen = frozen.getBoolean(registry);
        frozen.setBoolean(registry, false);
        try { ModComponents.initialize(); } finally { frozen.setBoolean(registry, wasFrozen); }
        for (Item item : List.of(Blocks.SHULKER_BOX.asItem(), Blocks.DYED_SHULKER_BOX.blue().asItem(),
                Items.BUNDLE, Items.DYED_BUNDLE.blue(), Items.STONE)) {
            if (!item.builtInRegistryHolder().areComponentsBound()) item.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
        }
    }

    @Test
    void nonEmptyCountOneShulkerGetsSpecificIdentityAndSurvivesMutableChanges() {
        ItemStack shulker = filledShulker();
        ReservationTransition.Result set = ReservationTransition.fromOccupied(ReservationData.EMPTY, 2, shulker);
        assertEquals(ReservationTransition.Outcome.SET, set.outcome());
        assertTrue(set.data().getEntry(2).orElseThrow().specific());
        set.attachIdentity(shulker);
        UUID id = PortableContainerIdentity.get(shulker).orElseThrow();
        assertTrue(set.data().matches(2, shulker));

        shulker.set(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        assertTrue(set.data().matches(2, shulker), "emptying an identified shulker must not change its reservation");
        shulker.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("renamed"));
        assertTrue(set.data().matches(2, shulker), "names are mutable, identity is not");
        assertEquals(id, PortableContainerIdentity.get(shulker).orElseThrow());

        ItemStack lookalike = filledShulker();
        assertFalse(set.data().matches(2, lookalike));
        lookalike.set(ModComponents.PORTABLE_CONTAINER_ID, UUID.randomUUID());
        assertFalse(set.data().matches(2, lookalike));
    }

    @Test
    void nonEmptyCountOneBundleGetsSpecificIdentityButLookalikeDoesNotMatch() {
        ItemStack bundle = filledBundle();
        ReservationTransition.Result set = ReservationTransition.fromCursor(ReservationData.EMPTY, 4, bundle);
        assertEquals(ReservationTransition.Outcome.SET, set.outcome());
        set.attachIdentity(bundle);
        assertTrue(set.data().getEntry(4).orElseThrow().specific());
        assertTrue(set.data().matches(4, bundle));
        bundle.set(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
        assertTrue(set.data().matches(4, bundle));
        assertFalse(set.data().matches(4, filledBundle()));
    }

    @Test
    void anonymousAndDormantEmptyPortableContainersRemainGenericAndStackSafe() {
        ItemStack emptyShulker = new ItemStack(Blocks.SHULKER_BOX, 16);
        ReservationTransition.Result generic = ReservationTransition.fromOccupied(ReservationData.EMPTY, 0, emptyShulker);
        assertEquals(ReservationTransition.Outcome.SET, generic.outcome());
        assertFalse(generic.data().getEntry(0).orElseThrow().specific());
        assertTrue(PortableContainerIdentity.get(emptyShulker).isEmpty());

        ItemStack dormant = new ItemStack(Blocks.SHULKER_BOX);
        dormant.set(ModComponents.PORTABLE_CONTAINER_ID, UUID.randomUUID());
        ReservationTransition.Result dormantGeneric = ReservationTransition.fromOccupied(ReservationData.EMPTY, 1, dormant);
        assertFalse(dormantGeneric.data().getEntry(1).orElseThrow().specific());
        assertTrue(dormantGeneric.data().matches(1, new ItemStack(Blocks.SHULKER_BOX)),
                "the generic template must omit a dormant CSR identity");
        assertEquals(1, dormant.getCount());

        ItemStack emptyBundle = new ItemStack(Items.BUNDLE, 16);
        ReservationTransition.Result bundleGeneric = ReservationTransition.fromOccupied(ReservationData.EMPTY, 3, emptyBundle);
        assertFalse(bundleGeneric.data().getEntry(3).orElseThrow().specific());
        assertTrue(PortableContainerIdentity.get(emptyBundle).isEmpty());
    }

    @Test
    void multiCountNonEmptyPortableContainersAreRejectedWithoutIdentity() {
        ItemStack shulkers = filledShulker();
        shulkers.setCount(2);
        ReservationTransition.Result result = ReservationTransition.fromOccupied(ReservationData.EMPTY, 0, shulkers);
        assertEquals(ReservationTransition.Outcome.REJECTED, result.outcome());
        assertTrue(PortableContainerIdentity.get(shulkers).isEmpty());
    }

    @Test
    void formatOneLoadsAsTemplateAndFormatTwoRetainsSpecificIdentityWithoutCopyingItIntoTemplate() {
        var legacy = new com.google.gson.JsonObject();
        legacy.addProperty("version", 1);
        var slots = new com.google.gson.JsonArray();
        var entry = new com.google.gson.JsonObject();
        entry.addProperty("slot", 7);
        entry.add("item", ItemStackTemplate.CODEC.encodeStart(JsonOps.INSTANCE, ItemStackTemplate.fromNonEmptyStack(new ItemStack(Items.STONE))).getOrThrow());
        slots.add(entry); legacy.add("slots", slots);
        ReservationData old = ReservationData.CODEC.parse(JsonOps.INSTANCE, legacy).getOrThrow();
        assertTrue(old.matches(7, new ItemStack(Items.STONE)));

        ItemStack shulker = filledShulker();
        ReservationTransition.Result specific = ReservationTransition.fromOccupied(ReservationData.EMPTY, 8, shulker);
        specific.attachIdentity(shulker);
        ReservationData reloaded = ReservationData.CODEC.parse(JsonOps.INSTANCE,
                ReservationData.CODEC.encodeStart(JsonOps.INSTANCE, specific.data()).getOrThrow()).getOrThrow();
        assertTrue(reloaded.matches(8, shulker));
        assertTrue(reloaded.get(8).orElseThrow().get(ModComponents.PORTABLE_CONTAINER_ID) == null,
                "a rendered/template stack cannot become a second identity-bearing item");
    }

    @Test
    void vanillaTransmutePreservesTheIdentityAcrossDyeableShulkerAndBundleVariants() {
        ItemStack shulker = filledShulker();
        ReservationTransition.Result shulkerReservation = ReservationTransition.fromOccupied(ReservationData.EMPTY, 0, shulker);
        shulkerReservation.attachIdentity(shulker);
        ItemStack dyedShulker = TransmuteRecipe.createWithOriginalComponents(
                ItemStackTemplate.fromNonEmptyStack(new ItemStack(Blocks.DYED_SHULKER_BOX.blue())), shulker);
        assertTrue(shulkerReservation.data().matches(0, dyedShulker));

        ItemStack bundle = filledBundle();
        ReservationTransition.Result bundleReservation = ReservationTransition.fromOccupied(ReservationData.EMPTY, 1, bundle);
        bundleReservation.attachIdentity(bundle);
        ItemStack dyedBundle = TransmuteRecipe.createWithOriginalComponents(
                ItemStackTemplate.fromNonEmptyStack(new ItemStack(Items.DYED_BUNDLE.blue())), bundle);
        assertTrue(bundleReservation.data().matches(1, dyedBundle));
    }

    private static ItemStack filledShulker() {
        ItemStack result = new ItemStack(Blocks.SHULKER_BOX);
        result.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(new ItemStack(Items.STONE))));
        return result;
    }

    private static ItemStack filledBundle() {
        ItemStack result = new ItemStack(Items.BUNDLE);
        result.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(List.of(ItemStackTemplate.fromNonEmptyStack(new ItemStack(Items.STONE)))));
        return result;
    }
}
