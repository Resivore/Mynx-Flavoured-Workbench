package dev.resivore.inventorysortercsrcompat.core;

import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ReservationFillPlanTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        bind(Items.COBBLESTONE);
        bind(Items.DIRT);
    }

    @Test
    void fillsTargetsAndDonorsInPhysicalOrderWithExactQuantities() {
        List<TestSlot> slots = List.of(
                reserved(ItemStack.EMPTY, stack -> stack.is(Items.COBBLESTONE)),
                open(stack(Items.COBBLESTONE, 20)),
                reserved(ItemStack.EMPTY, stack -> stack.is(Items.COBBLESTONE)),
                open(stack(Items.COBBLESTONE, 64)),
                open(stack(Items.DIRT, 7))
        );

        ReservationFillPlan.Result result = ReservationFillPlan.plan(slots);

        assertEquals(64, result.stacks().get(0).getCount());
        assertEquals(20, result.stacks().get(2).getCount());
        assertTrue(result.stacks().get(1).isEmpty());
        assertTrue(result.stacks().get(3).isEmpty());
        assertEquals(7, result.stacks().get(4).getCount());
        assertEquals(List.of(
                new ReservationFillPlan.Transfer(1, 0, 20, false),
                new ReservationFillPlan.Transfer(3, 0, 44, true),
                new ReservationFillPlan.Transfer(3, 2, 20, false)
        ), result.transfers());
        assertEquals(84, count(result.stacks(), Items.COBBLESTONE));
    }

    @Test
    void partialFillLeavesExactRemainderAndInsufficientDonorsAreConserved() {
        ReservationFillPlan.Result excess = ReservationFillPlan.plan(List.of(
                reserved(stack(Items.COBBLESTONE, 32), stack -> stack.is(Items.COBBLESTONE)),
                open(stack(Items.COBBLESTONE, 64))
        ));
        assertEquals(64, excess.stacks().get(0).getCount());
        assertEquals(32, excess.stacks().get(1).getCount());
        assertTrue(excess.transfers().getFirst().returnsRemainder());
        assertEquals(96, count(excess.stacks(), Items.COBBLESTONE));

        ReservationFillPlan.Result insufficient = ReservationFillPlan.plan(List.of(
                reserved(stack(Items.COBBLESTONE, 48), stack -> stack.is(Items.COBBLESTONE)),
                open(stack(Items.COBBLESTONE, 9))
        ));
        assertEquals(57, insufficient.stacks().get(0).getCount());
        assertTrue(insufficient.stacks().get(1).isEmpty());
        assertEquals(57, count(insufficient.stacks(), Items.COBBLESTONE));
    }

    @Test
    void reservedSlotsAreNeverDonorsAndWrongOrFullOccupantsStayExact() {
        ItemStack wrong = named(Items.DIRT, 11, "wrong occupant");
        ItemStack full = named(Items.COBBLESTONE, 64, "full reservation");
        ReservationFillPlan.Result result = ReservationFillPlan.plan(List.of(
                reserved(ItemStack.EMPTY, stack -> stack.is(Items.COBBLESTONE)),
                reserved(wrong, stack -> false),
                reserved(full, stack -> stack.is(Items.COBBLESTONE)),
                open(stack(Items.DIRT, 5))
        ));

        assertTrue(result.stacks().get(0).isEmpty(), "the full reserved stack must not become a donor");
        assertTrue(ItemStack.matches(wrong, result.stacks().get(1)));
        assertTrue(ItemStack.matches(full, result.stacks().get(2)));
        assertEquals(5, result.stacks().get(3).getCount());
        assertTrue(result.transfers().isEmpty());
    }

    @Test
    void broadAdmissionStillCannotMergeComponentIncompatibleStacks() {
        ItemStack target = named(Items.COBBLESTONE, 32, "alpha");
        ItemStack incompatible = named(Items.COBBLESTONE, 20, "beta");
        ReservationFillPlan.Result result = ReservationFillPlan.plan(List.of(
                reserved(target, stack -> stack.is(Items.COBBLESTONE)),
                open(incompatible)
        ));

        assertTrue(ItemStack.matches(target, result.stacks().get(0)));
        assertTrue(ItemStack.matches(incompatible, result.stacks().get(1)));
        assertTrue(result.transfers().isEmpty());
        assertEquals(52, count(result.stacks(), Items.COBBLESTONE));
    }

    @Test
    void targetSpecificCapacityIsRespected() {
        ReservationFillPlan.Result result = ReservationFillPlan.plan(List.of(
                new TestSlot(ItemStack.EMPTY, true, stack -> stack.is(Items.COBBLESTONE), 16),
                open(stack(Items.COBBLESTONE, 40))
        ));

        assertEquals(16, result.stacks().get(0).getCount());
        assertEquals(24, result.stacks().get(1).getCount());
        assertEquals(40, count(result.stacks(), Items.COBBLESTONE));
    }

    private static TestSlot reserved(ItemStack stack, Predicate<ItemStack> admission) {
        return new TestSlot(stack, true, admission, 64);
    }

    private static TestSlot open(ItemStack stack) {
        return new TestSlot(stack, false, ignored -> true, 64);
    }

    private static ItemStack named(net.minecraft.world.item.Item item, int count, String name) {
        ItemStack stack = stack(item, count);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        return stack;
    }

    private static ItemStack stack(net.minecraft.world.item.Item item, int count) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.MAX_STACK_SIZE, 64);
        stack.setCount(count);
        return stack;
    }

    private static int count(List<ItemStack> stacks, net.minecraft.world.item.Item item) {
        return stacks.stream().filter(stack -> stack.is(item)).mapToInt(ItemStack::getCount).sum();
    }

    private static void bind(net.minecraft.world.item.Item item) {
        if (!item.builtInRegistryHolder().areComponentsBound()) {
            item.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
        }
    }

    private record TestSlot(
            ItemStack stack,
            boolean reserved,
            Predicate<ItemStack> admission,
            int capacity
    ) implements ReservationFillPlan.SlotAccess {
        @Override public boolean mayInsert(ItemStack incoming) { return admission.test(incoming); }
        @Override public int maxStackSize(ItemStack incoming) { return capacity; }
    }
}
