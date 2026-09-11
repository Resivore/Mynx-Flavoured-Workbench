package dev.resivore.slotreservations;

import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

final class ShulkerReservationDrops {
    private ShulkerReservationDrops() {
    }

    static void register() {
        LootTableEvents.MODIFY_DROPS.register((lootTable, context, drops) -> {
            BlockEntity blockEntity = context.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
            if (!(blockEntity instanceof ShulkerBoxBlockEntity shulker)
                    || SupportedContainerResolver.resolve(shulker, 0).isEmpty()
                    || ReservationStore.getData(shulker).isEmpty()
                    && shulker.components().get(ModComponents.PORTABLE_CONTAINER_ID) == null) {
                return;
            }

            for (ItemStack drop : drops) {
                if (drop.getItem() instanceof BlockItem blockItem
                        && blockItem.getBlock() == shulker.getBlockState().getBlock()) {
                    // Vanilla 26.2 shulker loot whitelists four implicit components. Apply
                    // the surviving unconsumed component map to that same drop so the
                    // reservation and unrelated item-origin components remain intact.
                    drop.applyComponents(shulker.components());
                }
            }
        });
    }
}
