package dev.resivore.slotreservations.mixin;

import dev.resivore.slotreservations.ReservationStore;
import dev.resivore.slotreservations.SupportedContainerResolver;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;

/** Adds the shared hopper/backing-container insertion gate to the exact supported leaves only. */
@Mixin(RandomizableContainerBlockEntity.class)
public abstract class RandomizableContainerBlockEntityMixin {
    public boolean canPlaceItem(int slot, ItemStack incoming) {
        Container self = (Container) (Object) this;
        return SupportedContainerResolver.resolve(self, slot)
                .map(resolved -> ReservationStore.reservationAllows(resolved, incoming))
                .orElse(true);
    }
}
