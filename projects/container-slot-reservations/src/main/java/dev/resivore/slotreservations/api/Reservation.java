package dev.resivore.slotreservations.api;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import dev.resivore.slotreservations.PortableContainerIdentity;
import dev.resivore.slotreservations.ReservationData;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** A defensive public view of one exact component-aware reservation identity. */
public final class Reservation {
    private final ItemStackTemplate template;
    private final Optional<PortableContainerIdentity.Family> family;
    private final Optional<UUID> identity;

    public Reservation(ItemStack template) {
        Objects.requireNonNull(template, "template");
        if (template.isEmpty()) {
            throw new IllegalArgumentException("A reservation template must be non-empty");
        }
        this.template = ItemStackTemplate.fromNonEmptyStack(template).withCount(1);
        this.family = Optional.empty();
        this.identity = Optional.empty();
    }

    public Reservation(ReservationData.Entry entry) {
        Objects.requireNonNull(entry, "entry");
        this.template = entry.template().withCount(1);
        this.family = entry.family();
        this.identity = entry.identity();
    }

    /** Returns a fresh count-one stack. Mutating it cannot mutate reservation state. */
    public ItemStack template() {
        return template.create();
    }

    public boolean matches(ItemStack incoming) {
        Objects.requireNonNull(incoming, "incoming");
        return !incoming.isEmpty() && (family.isPresent()
                ? PortableContainerIdentity.matches(family.orElseThrow(), identity.orElseThrow(), incoming)
                : PortableContainerIdentity.genericEmptyMatches(template.create(), incoming)
                || ItemStack.isSameItemSameComponents(template.create(), incoming));
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Reservation reservation)) return false;
        if (family.isPresent() || reservation.family.isPresent()) {
            return family.equals(reservation.family) && identity.equals(reservation.identity);
        }
        return ItemStack.isSameItemSameComponents(template.create(), reservation.template.create());
    }

    @Override
    public int hashCode() {
        return family.isPresent() ? Objects.hash(family, identity) : ItemStack.hashItemAndComponents(template.create());
    }

    @Override
    public String toString() {
        return "Reservation[template=" + template + ']';
    }
}
