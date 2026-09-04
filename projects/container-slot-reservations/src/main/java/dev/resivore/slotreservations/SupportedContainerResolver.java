package dev.resivore.slotreservations;

import dev.resivore.slotreservations.mixin.CompoundContainerAccessor;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTypes;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Resolves a logical container slot to one stable reservation owner and physical local slot. */
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
        if (slot < 0) {
            return Optional.empty();
        }

        // The optional wrapper does not bounds-check, and a connected BarrelBlockEntity's
        // getContainerSize() can mutate a stale Double Barrels link. Audit these views first.
        DoubleBarrelBridge.Resolution doubleBarrel = DoubleBarrelBridge.resolve(container, slot);
        if (doubleBarrel.handled()) {
            return doubleBarrel.slot().map(ResolvedSlot::doubleBarrel);
        }

        if (container instanceof CompoundContainer compound) {
            if (slot >= compound.getContainerSize()) {
                return Optional.empty();
            }
            CompoundContainerAccessor accessor = (CompoundContainerAccessor) (Object) compound;
            Container first = accessor.containerSlotReservations$getFirst();
            Container second = accessor.containerSlotReservations$getSecond();
            int firstSize = first.getContainerSize();
            return slot < firstSize
                    ? resolvePhysical(first, slot)
                    : resolvePhysical(second, slot - firstSize);
        }

        if (container instanceof PlayerEnderChestContainer enderChest) {
            return slot < enderChest.getContainerSize()
                    ? Optional.of(ResolvedSlot.standard(enderChest, slot))
                    : Optional.empty();
        }

        if (slot >= container.getContainerSize()) {
            return Optional.empty();
        }
        return resolvePhysical(container, slot);
    }

    public static boolean isSupportedShulkerItem(ItemStack stack) {
        return !stack.isEmpty() && SHULKER_ITEMS.contains(stack.getItem());
    }

    public static boolean isSupportedShulker(BlockEntity blockEntity) {
        return exactType(blockEntity, BlockEntityTypes.SHULKER_BOX)
                && SHULKER_BLOCKS.contains(blockEntity.getBlockState().getBlock());
    }

    public static boolean isShulkerOwner(ResolvedSlot slot) {
        return slot.blockEntity() != null && isSupportedShulker(slot.blockEntity());
    }

    private static Optional<ResolvedSlot> resolvePhysical(Container container, int localSlot) {
        if (!(container instanceof BlockEntity blockEntity)
                || localSlot < 0
                || localSlot >= ReservationData.SLOT_COUNT
                || localSlot >= container.getContainerSize()
                || !isSupported(blockEntity)) {
            return Optional.empty();
        }
        return Optional.of(ResolvedSlot.standard(container, localSlot));
    }

    private static boolean isSupported(BlockEntity blockEntity) {
        return exactType(blockEntity, BlockEntityTypes.CHEST)
                || exactType(blockEntity, BlockEntityTypes.TRAPPED_CHEST)
                || exactType(blockEntity, BlockEntityTypes.BARREL)
                || exactType(blockEntity, BlockEntityTypes.SHULKER_BOX)
                || exactType(blockEntity, BlockEntityTypes.DISPENSER)
                || exactType(blockEntity, BlockEntityTypes.DROPPER)
                || exactType(blockEntity, BlockEntityTypes.HOPPER)
                || exactType(blockEntity, BlockEntityTypes.FURNACE)
                || exactType(blockEntity, BlockEntityTypes.BLAST_FURNACE)
                || exactType(blockEntity, BlockEntityTypes.SMOKER)
                || exactType(blockEntity, BlockEntityTypes.BREWING_STAND)
                || exactType(blockEntity, BlockEntityTypes.CRAFTER);
    }

    private static boolean exactType(BlockEntity blockEntity, BlockEntityType<?> type) {
        return blockEntity.getType() == type && type.isValid(blockEntity.getBlockState());
    }

    public static final class ResolvedSlot {
        private final Container owner;
        private final BlockEntity blockEntity;
        private final int localSlot;
        private final int ownerSlotCount;
        private final List<ItemStack> physicalItems;

        private ResolvedSlot(
                Container owner,
                BlockEntity blockEntity,
                int localSlot,
                int ownerSlotCount,
                List<ItemStack> physicalItems
        ) {
            this.owner = Objects.requireNonNull(owner, "owner");
            this.blockEntity = blockEntity;
            this.localSlot = localSlot;
            this.ownerSlotCount = ownerSlotCount;
            this.physicalItems = physicalItems;
            if (blockEntity != null && owner != blockEntity) {
                throw new IllegalArgumentException("Block reservation owner must be its physical block entity");
            }
            if (ownerSlotCount < 1 || ownerSlotCount > ReservationData.SLOT_COUNT
                    || localSlot < 0 || localSlot >= ownerSlotCount) {
                throw new IndexOutOfBoundsException("Invalid physical reservation slot: " + localSlot);
            }
            if (physicalItems != null && physicalItems.size() != ownerSlotCount) {
                throw new IllegalArgumentException("Physical item view does not match owner slot count");
            }
        }

        private static ResolvedSlot standard(Container owner, int localSlot) {
            int slotCount = owner.getContainerSize();
            return new ResolvedSlot(
                    owner,
                    owner instanceof BlockEntity blockEntity ? blockEntity : null,
                    localSlot,
                    slotCount,
                    null
            );
        }

        private static ResolvedSlot doubleBarrel(DoubleBarrelBridge.PhysicalSlot slot) {
            return new ResolvedSlot(slot.owner(), slot.owner(), slot.localSlot(), 27, slot.physicalItems());
        }

        public Container owner() {
            return owner;
        }

        /** Null only for the player-owned Ender Chest inventory. */
        public BlockEntity blockEntity() {
            return blockEntity;
        }

        public int localSlot() {
            return localSlot;
        }

        public int ownerSlotCount() {
            return ownerSlotCount;
        }

        public ItemStack physicalStack() {
            return physicalItems == null ? owner.getItem(localSlot) : physicalItems.get(localSlot);
        }
    }
}
