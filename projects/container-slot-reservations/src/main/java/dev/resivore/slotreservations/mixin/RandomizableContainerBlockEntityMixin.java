package dev.resivore.slotreservations.mixin;

import dev.resivore.slotreservations.NativeInsertionPolicy;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;

/** Materializes Container's inherited Minecraft 26.2 native-true admission, then applies CSR. */
@Mixin(RandomizableContainerBlockEntity.class)
public abstract class RandomizableContainerBlockEntityMixin {
    public boolean canPlaceItem(int slot, ItemStack incoming) {
        Container self = (Container) (Object) this;
        return NativeInsertionPolicy.applyReservation(self, slot, incoming, true);
    }
}
