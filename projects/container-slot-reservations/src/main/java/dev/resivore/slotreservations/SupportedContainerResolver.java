package dev.resivore.slotreservations;

import dev.resivore.slotreservations.mixin.CompoundContainerAccessor;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTypes;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Resolves a menu-visible slot to its stable physical vanilla block-entity owner. */
public final class SupportedContainerResolver {
    private static final Set<Block> SHULKER_BLOCKS = createShulkerBlocks();
    private static final Set<Item> SHULKER_ITEMS = SHULKER_BLOCKS.stream()
            .map(Block::asItem)
            .collect(java.util.stream.Collectors.toUnmodifiableSet());

    private SupportedContainerResolver() {
    }

    private static Set<Block> createShulkerBlocks() {
        HashSet<Block> blocks = new HashSet<>(Blocks.DYED_SHULKER_BOX.asList());
        blocks.add(Blocks.SHULKER_BOX);
        return Set.copyOf(blocks);
    }

    public static Optional<ResolvedSlot> resolve(Container container, int slot) {
        Objects.requireNonNull(container, "container");
        if (slot < 0 || slot >= container.getContainerSize()) {
            return Optional.empty();
        }

        if (container instanceof CompoundContainer compound) {
            CompoundContainerAccessor accessor = (CompoundContainerAccessor) (Object) compound;
            Container first = accessor.containerSlotReservations$getFirst();
            Container second = accessor.containerSlotReservations$getSecond();
            int firstSize = first.getContainerSize();
            return slot < firstSize
                    ? resolvePhysical(first, slot)
                    : resolvePhysical(second, slot - firstSize);
        }

        return resolvePhysical(container, slot);
    }

    public static boolean isSupportedShulkerItem(ItemStack stack) {
        return !stack.isEmpty() && SHULKER_ITEMS.contains(stack.getItem());
    }

    public static boolean isSupportedShulker(BlockEntity blockEntity) {
        return blockEntity.getType() == BlockEntityTypes.SHULKER_BOX
                && SHULKER_BLOCKS.contains(blockEntity.getBlockState().getBlock());
    }

    private static Optional<ResolvedSlot> resolvePhysical(Container container, int localSlot) {
        if (!(container instanceof BlockEntity blockEntity)
                || localSlot < 0
                || localSlot >= ReservationData.SLOT_COUNT
                || localSlot >= container.getContainerSize()
                || !isSupported(blockEntity)) {
            return Optional.empty();
        }
        return Optional.of(new ResolvedSlot(container, blockEntity, localSlot));
    }

    private static boolean isSupported(BlockEntity blockEntity) {
        Block block = blockEntity.getBlockState().getBlock();
        if (blockEntity.getType() == BlockEntityTypes.CHEST) {
            return block == Blocks.CHEST;
        }
        if (blockEntity.getType() == BlockEntityTypes.TRAPPED_CHEST) {
            return block == Blocks.TRAPPED_CHEST;
        }
        if (blockEntity.getType() == BlockEntityTypes.BARREL) {
            return block == Blocks.BARREL;
        }
        return isSupportedShulker(blockEntity);
    }

    public record ResolvedSlot(Container owner, BlockEntity blockEntity, int localSlot) {
        public ResolvedSlot {
            Objects.requireNonNull(owner, "owner");
            Objects.requireNonNull(blockEntity, "blockEntity");
            if (owner != blockEntity) {
                throw new IllegalArgumentException("Resolved owner and block entity must be the same physical object");
            }
            if (localSlot < 0 || localSlot >= ReservationData.SLOT_COUNT
                    || localSlot >= owner.getContainerSize()) {
                throw new IndexOutOfBoundsException("Invalid physical reservation slot: " + localSlot);
            }
        }

        public ItemStack physicalStack() {
            return owner.getItem(localSlot);
        }
    }
}
