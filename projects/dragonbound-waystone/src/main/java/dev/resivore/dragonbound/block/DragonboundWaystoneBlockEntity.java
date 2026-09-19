package dev.resivore.dragonbound.block;

import dev.resivore.dragonbound.DragonboundContent;
import dev.resivore.dragonbound.anchor.DragonboundAnchors;
import dev.resivore.dragonbound.material.WaystoneMaterial;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.Objects;
import java.util.Optional;

/**
 * Retains the exact placed item stack so a survival relocation preserves components.
 * Destination authority remains exclusively in {@code DragonboundAnchorData}.
 */
public final class DragonboundWaystoneBlockEntity extends BlockEntity {
    private static final String PLACED_STACK_KEY = "placed_stack";
    private static final String VISUAL_MATERIAL_KEY = "visual_material";

    private ItemStack placedStack = ItemStack.EMPTY;
    private Identifier visualMaterialId;

    public DragonboundWaystoneBlockEntity(BlockPos pos, BlockState state) {
        super(DragonboundContent.WAYSTONE_BLOCK_ENTITY, pos, state);
    }

    public void setPlacedStack(ItemStack stack) {
        placedStack = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        visualMaterialId = WaystoneMaterial.selectedBlockId(placedStack).orElse(null);
        setChanged();
        syncVisualMaterial();
    }

    public ItemStack copyPlacedStack() {
        return placedStack.isEmpty() ? ItemStack.EMPTY : placedStack.copyWithCount(1);
    }

    /** The only placed Waystone datum needed by client-side material rendering. */
    public Optional<Identifier> visualMaterialId() {
        return Optional.ofNullable(visualMaterialId);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level instanceof ServerLevel serverLevel) {
            DragonboundAnchors.clearIfMatching(serverLevel, pos);
        }
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        Optional<ItemStack> loadedStack = input.read(PLACED_STACK_KEY, ItemStack.CODEC)
                .filter(stack -> !stack.isEmpty())
                .map(stack -> stack.copyWithCount(1));
        if (loadedStack.isPresent()) {
            placedStack = loadedStack.get();
            visualMaterialId = WaystoneMaterial.selectedBlockId(placedStack).orElse(null);
        }

        Identifier priorVisualMaterialId = visualMaterialId;
        Optional<Identifier> synchronizedMaterial = input.read(VISUAL_MATERIAL_KEY, Identifier.CODEC)
                .flatMap(WaystoneMaterial::eligibleBlockId);
        if (synchronizedMaterial.isPresent() || loadedStack.isEmpty()) {
            visualMaterialId = synchronizedMaterial.orElse(null);
        }

        if (level != null && level.isClientSide() && !Objects.equals(priorVisualMaterialId, visualMaterialId)) {
            // A block-entity packet does not itself rebuild a same-state block model. Force the
            // geometry key to be re-evaluated only when the client-visible material changes.
            level.setBlocksDirty(worldPosition, getBlockState(), getBlockState());
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!placedStack.isEmpty()) {
            output.store(PLACED_STACK_KEY, ItemStack.CODEC, placedStack.copyWithCount(1));
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        if (visualMaterialId != null) {
            tag.putString(VISUAL_MATERIAL_KEY, visualMaterialId.toString());
        }
        return tag;
    }

    private void syncVisualMaterial() {
        if (level instanceof ServerLevel serverLevel) {
            // This produces the block-entity update packet and the corresponding client block
            // notification. The client-side load path above invalidates the changed geometry key.
            serverLevel.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }
}
