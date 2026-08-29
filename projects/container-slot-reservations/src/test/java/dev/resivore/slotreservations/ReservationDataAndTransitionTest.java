package dev.resivore.slotreservations;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ReservationDataAndTransitionTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        bindTestComponents(Items.POISONOUS_POTATO);
        bindTestComponents(Items.RABBIT_STEW);
    }

    @Test
    void identityUsesTheExactItemAndCompleteComponentsWhileIgnoringCount() {
        ItemStack stored = identity(Items.POISONOUS_POTATO, 37, "green_curry", "Green Curry");
        ReservationData data = ReservationData.EMPTY.with(4, stored);

        ItemStack exact = stored.copyWithCount(64);
        ItemStack otherModel = identity(Items.POISONOUS_POTATO, 1, "ramen", "Green Curry");
        ItemStack otherName = identity(Items.POISONOUS_POTATO, 1, "green_curry", "Ramen");

        assertTrue(data.matches(4, exact), "Physical counts must not be part of reservation identity");
        assertFalse(data.matches(4, otherModel), "ITEM_MODEL is part of the exact component identity");
        assertFalse(data.matches(4, otherName), "ITEM_NAME is part of the exact component identity");
        assertFalse(data.matches(4, ItemStack.EMPTY));
        assertEquals(1, data.get(4).orElseThrow().getCount(), "Stored templates must be count one");
        assertEquals(37, stored.getCount(), "Capturing a template must not rewrite the physical count");
    }

    @Test
    void sourceAndReturnedStacksAreDefensiveAndKeepAllComponents() {
        ItemStack source = identity(Items.RABBIT_STEW, 12, "spicy_ramen", "Spicy Ramen");
        ItemStack expected = source.copyWithCount(1);
        ReservationData data = ReservationData.EMPTY.with(8, source);

        source.setCount(1);
        source.set(DataComponents.ITEM_NAME, Component.literal("mutated source"));
        ItemStack first = data.get(8).orElseThrow();
        assertTrue(ItemStack.isSameItemSameComponents(expected, first));
        assertEquals(expected.getComponentsPatch(), first.getComponentsPatch());

        first.set(DataComponents.ITEM_MODEL, fixtureId("mutated_return"));
        ItemStack second = data.get(8).orElseThrow();
        assertNotSame(first, second);
        assertTrue(ItemStack.isSameItemSameComponents(expected, second),
                "Mutating a returned stack must not mutate reservation state");

        Map<Integer, ItemStack> view = data.reservations();
        assertThrows(UnsupportedOperationException.class, () -> view.put(1, expected));
        view.get(8).setCount(42);
        assertEquals(1, data.get(8).orElseThrow().getCount());
    }

    @Test
    void persistentCodecRoundTripsVersionSlotsAndExactComponentsAndRejectsUnknownVersions() {
        ItemStack first = identity(Items.POISONOUS_POTATO, 31, "green_curry", "Green Curry");
        ItemStack second = identity(Items.RABBIT_STEW, 6, "ramen", "Ramen");
        ReservationData original = ReservationData.EMPTY.with(2, first).with(25, second);

        JsonElement encoded = ReservationData.CODEC.encodeStart(JsonOps.INSTANCE, original).getOrThrow();
        JsonObject object = encoded.getAsJsonObject();
        JsonArray slots = object.getAsJsonArray("slots");
        assertEquals(ReservationData.FORMAT_VERSION, object.get("version").getAsInt());
        assertEquals(2, slots.size());
        assertEquals(2, slots.get(0).getAsJsonObject().get("slot").getAsInt());
        assertEquals(25, slots.get(1).getAsJsonObject().get("slot").getAsInt());

        ReservationData decoded = ReservationData.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();
        assertEquals(original, decoded);
        assertTrue(decoded.matches(2, first.copyWithCount(64)));
        assertTrue(decoded.matches(25, second.copyWithCount(1)));
        assertEquals(1, decoded.get(2).orElseThrow().getCount());
        assertEquals(first.getComponentsPatch(), decoded.get(2).orElseThrow().getComponentsPatch());
        assertEquals(second.getComponentsPatch(), decoded.get(25).orElseThrow().getComponentsPatch());

        JsonObject unknownVersion = object.deepCopy();
        unknownVersion.addProperty("version", ReservationData.FORMAT_VERSION + 1);
        assertTrue(ReservationData.CODEC.parse(JsonOps.INSTANCE, unknownVersion).error().isPresent(),
                "Unknown persistent reservation versions must fail closed");
    }

    @Test
    void occupiedTemplateSetsAndMatchingSecondRequestClearsWithoutMutatingTheStack() {
        ItemStack physical = identity(Items.POISONOUS_POTATO, 23, "green_curry", "Green Curry");
        ItemStack physicalBefore = physical.copy();

        ReservationTransition.Result set = ReservationTransition.fromOccupied(
                ReservationData.EMPTY, 3, physical);
        assertEquals(ReservationTransition.Outcome.SET, set.outcome());
        assertTrue(set.changed());
        assertTrue(set.data().matches(3, physical));
        assertEquals(1, set.data().get(3).orElseThrow().getCount());

        ReservationTransition.Result cleared = ReservationTransition.fromOccupied(
                set.data(), 3, physical);
        assertEquals(ReservationTransition.Outcome.CLEARED, cleared.outcome());
        assertTrue(cleared.data().isEmpty());

        assertEquals(physicalBefore.getCount(), physical.getCount());
        assertEquals(physicalBefore.getComponentsPatch(), physical.getComponentsPatch());
    }

    @Test
    void emptyPhysicalSlotUsesCursorAndExplicitClearClearsOrNoOps() {
        ItemStack cursor = identity(Items.RABBIT_STEW, 16, "ramen", "Ramen");
        ItemStack cursorBefore = cursor.copy();
        ReservationTransition.Result set = ReservationTransition.fromCursor(
                ReservationData.EMPTY, 12, cursor);
        assertEquals(ReservationTransition.Outcome.SET, set.outcome());
        assertTrue(set.data().matches(12, cursor));
        assertEquals(16, cursor.getCount(), "Cursor count must remain physical state only");
        assertEquals(cursorBefore.getComponentsPatch(), cursor.getComponentsPatch());

        ReservationTransition.Result repeated = ReservationTransition.fromCursor(
                set.data(), 12, cursor.copyWithCount(1));
        assertEquals(ReservationTransition.Outcome.UNCHANGED, repeated.outcome());
        assertFalse(repeated.changed());
        assertSame(set.data(), repeated.data(),
                "Pressing the same carried template again must retain, not toggle, an empty-slot reservation");

        ReservationTransition.Result cleared = ReservationTransition.clear(set.data(), 12);
        assertEquals(ReservationTransition.Outcome.CLEARED, cleared.outcome());
        assertTrue(cleared.data().isEmpty());

        ReservationTransition.Result unchanged = ReservationTransition.clear(ReservationData.EMPTY, 12);
        assertEquals(ReservationTransition.Outcome.UNCHANGED, unchanged.outcome());
        assertFalse(unchanged.changed());
        assertSame(ReservationData.EMPTY, unchanged.data());
    }

    @Test
    void removingThePhysicalStackDoesNotRemoveTheIndependentReservation() {
        ItemStack physical = identity(Items.POISONOUS_POTATO, 9, "green_curry", "Green Curry");
        ReservationData reservation = ReservationData.EMPTY.with(0, physical);
        SimpleContainer container = new SimpleContainer(1);
        container.setItem(0, physical.copyWithCount(1));

        ItemStack removed = container.removeItemNoUpdate(0);

        assertEquals(1, removed.getCount());
        assertTrue(container.getItem(0).isEmpty());
        assertTrue(reservation.matches(0, removed),
                "Ordinary physical removal must leave the logical reservation intact");
    }

    @Test
    void slotBoundsAndEmptyTemplatesAreRejectedWithoutChangingExistingData() {
        ItemStack stack = identity(Items.POISONOUS_POTATO, 1, "green_curry", "Green Curry");
        ReservationData data = ReservationData.EMPTY.with(0, stack);

        assertThrows(IndexOutOfBoundsException.class, () -> data.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> data.with(27, stack));
        assertThrows(IllegalArgumentException.class, () -> data.with(1, ItemStack.EMPTY));
        assertSame(data, data.without(26));
    }

    private static ItemStack identity(Item item, int count, String model, String name) {
        ItemStack stack = new ItemStack(item, count);
        stack.set(DataComponents.ITEM_MODEL, fixtureId(model));
        stack.set(DataComponents.ITEM_NAME, Component.literal(name));
        return stack;
    }

    private static Identifier fixtureId(String path) {
        return Identifier.fromNamespaceAndPath("container_slot_reservations_fixture", path);
    }

    private static void bindTestComponents(Item item) {
        if (!item.builtInRegistryHolder().areComponentsBound()) {
            item.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
        }
    }
}
