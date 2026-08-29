package dev.resivore.slotreservations.api;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;

import java.util.Objects;

/** A defensive public view of one exact component-aware reservation identity. */
public final class Reservation {
    private final ItemStackTemplate template;

    public Reservation(ItemStack template) {
        Objects.requireNonNull(template, "template");
        if (template.isEmpty()) {
            throw new IllegalArgumentException("A reservation template must be non-empty");
        }
        this.template = ItemStackTemplate.fromNonEmptyStack(template).withCount(1);
    }

    /** Returns a fresh count-one stack. Mutating it cannot mutate reservation state. */
    public ItemStack template() {
        return template.create();
    }

    public boolean matches(ItemStack incoming) {
        Objects.requireNonNull(incoming, "incoming");
        return !incoming.isEmpty() && ItemStack.isSameItemSameComponents(template.create(), incoming);
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof Reservation reservation
                && ItemStack.isSameItemSameComponents(template.create(), reservation.template.create());
    }

    @Override
    public int hashCode() {
        return ItemStack.hashItemAndComponents(template.create());
    }

    @Override
    public String toString() {
        return "Reservation[template=" + template + ']';
    }
}
