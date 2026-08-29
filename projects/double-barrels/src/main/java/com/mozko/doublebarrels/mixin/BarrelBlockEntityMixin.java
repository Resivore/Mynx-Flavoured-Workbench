package com.mozko.doublebarrels.mixin;

import com.mozko.doublebarrels.DoubleBarrelAccess;
import com.mozko.doublebarrels.DoubleBarrelInventory;
import com.mozko.doublebarrels.DoubleBarrelProperties;
import com.mozko.doublebarrels.DoubleBarrelType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BarrelBlockEntity.class)
public abstract class BarrelBlockEntityMixin implements DoubleBarrelAccess {
    @Shadow private NonNullList<ItemStack> items;

    @Shadow private void updateBlockState(BlockState state, boolean open) {
        throw new AssertionError();
    }

    @Shadow private void playSound(BlockState state, SoundEvent sound) {
        throw new AssertionError();
    }

    @Unique private @Nullable BlockPos doublebarrels$connectionPos;
    @Unique private boolean doublebarrels$isMain;

    @Unique
    private BarrelBlockEntity doublebarrels$self() {
        return (BarrelBlockEntity) (Object) this;
    }

    @Unique
    private @Nullable BarrelBlockEntity doublebarrels$getPartner() {
        if (doublebarrels$connectionPos == null) return null;
        Level level = doublebarrels$self().getLevel();
        if (level == null) return null;
        BlockEntity blockEntity = level.getBlockEntity(doublebarrels$connectionPos);
        return blockEntity instanceof BarrelBlockEntity barrel ? barrel : null;
    }

    @Override
    @Unique
    public @Nullable Container getCombinedInventory() {
        BarrelBlockEntity partner = doublebarrels$getPartner();
        if (partner == null) return null;

        NonNullList<ItemStack> partnerItems = ((DoubleBarrelAccess) partner).doublebarrels$getItems();
        if (doublebarrels$isMain) {
            return new DoubleBarrelInventory(
                    items,
                    partnerItems,
                    () -> {
                        doublebarrels$self().setChanged();
                        partner.setChanged();
                    },
                    user -> {
                        openLid(user);
                        ((DoubleBarrelAccess) partner).openLid(user);
                    },
                    user -> {
                        closeLid(user);
                        ((DoubleBarrelAccess) partner).closeLid(user);
                    });
        }
        return ((DoubleBarrelAccess) partner).getCombinedInventory();
    }

    @Override
    @Unique
    public NonNullList<ItemStack> doublebarrels$getItems() {
        return items;
    }

    @Override
    @Unique
    public void connectTo(BarrelBlockEntity partner) {
        disconnect();
        BlockPos partnerPos = partner.getBlockPos();
        doublebarrels$connectionPos = partnerPos;
        doublebarrels$isMain = true;

        DoubleBarrelAccess partnerAccess = (DoubleBarrelAccess) partner;
        partnerAccess.setConnectionPos(doublebarrels$self().getBlockPos());
        partnerAccess.setMain(false);

        Level level = doublebarrels$self().getLevel();
        if (level != null && !level.isClientSide()) {
            BlockState mainState = level.getBlockState(doublebarrels$self().getBlockPos());
            Direction facing = mainState.getValue(BarrelBlock.FACING);
            BlockPos delta = partnerPos.subtract(doublebarrels$self().getBlockPos());
            Direction toPartner = Direction.getNearest(delta, Direction.NORTH);
            DoubleBarrelType mainType;
            DoubleBarrelType partnerType;

            if (facing.getAxis() == Direction.Axis.Y) {
                if (toPartner.getAxis() == Direction.Axis.Y) {
                    if (toPartner == Direction.UP) {
                        mainType = DoubleBarrelType.LONG_BOTTOM;
                        partnerType = DoubleBarrelType.LONG_TOP;
                    } else {
                        mainType = DoubleBarrelType.LONG_TOP;
                        partnerType = DoubleBarrelType.LONG_BOTTOM;
                    }
                } else if (toPartner == Direction.EAST) {
                    mainType = DoubleBarrelType.LEFT;
                    partnerType = DoubleBarrelType.RIGHT;
                } else if (toPartner == Direction.WEST) {
                    mainType = DoubleBarrelType.RIGHT;
                    partnerType = DoubleBarrelType.LEFT;
                } else if (toPartner == Direction.SOUTH) {
                    mainType = DoubleBarrelType.FRONT;
                    partnerType = DoubleBarrelType.BACK;
                } else {
                    mainType = DoubleBarrelType.BACK;
                    partnerType = DoubleBarrelType.FRONT;
                }
            } else if (toPartner.getAxis() == Direction.Axis.Y) {
                if (toPartner == Direction.UP) {
                    mainType = DoubleBarrelType.BOTTOM;
                    partnerType = DoubleBarrelType.TOP;
                } else {
                    mainType = DoubleBarrelType.TOP;
                    partnerType = DoubleBarrelType.BOTTOM;
                }
            } else if (toPartner.getAxis() == facing.getAxis()) {
                if (toPartner == facing) {
                    mainType = DoubleBarrelType.LONG_BACK;
                    partnerType = DoubleBarrelType.LONG_FRONT;
                } else {
                    mainType = DoubleBarrelType.LONG_FRONT;
                    partnerType = DoubleBarrelType.LONG_BACK;
                }
            } else if (toPartner == facing.getClockWise()) {
                mainType = DoubleBarrelType.RIGHT;
                partnerType = DoubleBarrelType.LEFT;
            } else {
                mainType = DoubleBarrelType.LEFT;
                partnerType = DoubleBarrelType.RIGHT;
            }

            level.setBlock(
                    doublebarrels$self().getBlockPos(),
                    mainState.setValue(DoubleBarrelProperties.DOUBLE, mainType),
                    3);
            BlockState partnerState = level.getBlockState(partnerPos);
            level.setBlock(
                    partnerPos,
                    partnerState.setValue(DoubleBarrelProperties.DOUBLE, partnerType),
                    3);
        }

        doublebarrels$self().setChanged();
        partner.setChanged();
    }

    @Override
    @Unique
    public void disconnect() {
        if (doublebarrels$connectionPos == null) return;

        BarrelBlockEntity partner = doublebarrels$getPartner();
        if (partner != null) {
            DoubleBarrelAccess partnerAccess = (DoubleBarrelAccess) partner;
            partnerAccess.setConnectionPos(null);
            partnerAccess.setMain(false);
            Level partnerLevel = partner.getLevel();
            if (partnerLevel != null && !partnerLevel.isClientSide()) {
                BlockState partnerState = partnerLevel.getBlockState(partner.getBlockPos());
                if (partnerState.hasProperty(DoubleBarrelProperties.DOUBLE)) {
                    partnerLevel.setBlock(
                            partner.getBlockPos(),
                            partnerState.setValue(DoubleBarrelProperties.DOUBLE, DoubleBarrelType.SINGLE),
                            3);
                }
            }
            partner.setChanged();
        }

        Level level = doublebarrels$self().getLevel();
        if (level != null && !level.isClientSide()) {
            BlockState state = level.getBlockState(doublebarrels$self().getBlockPos());
            if (state.hasProperty(DoubleBarrelProperties.DOUBLE)) {
                level.setBlock(
                        doublebarrels$self().getBlockPos(),
                        state.setValue(DoubleBarrelProperties.DOUBLE, DoubleBarrelType.SINGLE),
                        3);
            }
        }
        doublebarrels$connectionPos = null;
        doublebarrels$isMain = false;
        doublebarrels$self().setChanged();
    }

    @Override public boolean isConnected() { return doublebarrels$connectionPos != null; }
    @Override public boolean isMainBarrel() { return doublebarrels$isMain; }
    @Override public @Nullable BlockPos getConnectionPos() { return doublebarrels$connectionPos; }
    @Override public void setConnectionPos(@Nullable BlockPos pos) { doublebarrels$connectionPos = pos; }
    @Override public void setMain(boolean main) { doublebarrels$isMain = main; }

    @Override
    @Unique
    public void openLid(ContainerUser user) {
        if (doublebarrels$self().getLevel() == null) return;
        BlockState state = doublebarrels$self().getBlockState();
        if (doublebarrels$isMain) playSound(state, SoundEvents.BARREL_OPEN);
        updateBlockState(state, true);
    }

    @Override
    @Unique
    public void closeLid(ContainerUser user) {
        if (doublebarrels$self().getLevel() == null) return;
        BlockState state = doublebarrels$self().getBlockState();
        if (doublebarrels$isMain) playSound(state, SoundEvents.BARREL_CLOSE);
        updateBlockState(state, false);
    }

    @Inject(method = "getContainerSize", at = @At("HEAD"), cancellable = true)
    private void doublebarrels$getContainerSize(CallbackInfoReturnable<Integer> cir) {
        if (doublebarrels$connectionPos != null && doublebarrels$getPartner() != null) {
            cir.setReturnValue(54);
        } else if (doublebarrels$connectionPos != null) {
            disconnect();
        }
    }

    @Inject(method = "createMenu", at = @At("HEAD"), cancellable = true)
    private void doublebarrels$createMenu(
            int syncId,
            Inventory playerInventory,
            CallbackInfoReturnable<AbstractContainerMenu> cir) {
        if (doublebarrels$connectionPos == null || doublebarrels$getPartner() == null) {
            if (doublebarrels$connectionPos != null) disconnect();
            return;
        }
        Container combined = getCombinedInventory();
        if (combined != null) {
            cir.setReturnValue(ChestMenu.sixRows(syncId, playerInventory, combined));
        }
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void doublebarrels$loadConnectionData(ValueInput input, CallbackInfo ci) {
        input.getIntArray("DoubleBarrelPos").ifPresent(pos -> {
            if (pos.length == 3) {
                doublebarrels$connectionPos = new BlockPos(pos[0], pos[1], pos[2]);
                doublebarrels$isMain = input.getBooleanOr("DoubleBarrelMain", false);
            }
        });
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void doublebarrels$saveConnectionData(ValueOutput output, CallbackInfo ci) {
        if (doublebarrels$connectionPos != null) {
            output.putIntArray("DoubleBarrelPos", new int[]{
                    doublebarrels$connectionPos.getX(),
                    doublebarrels$connectionPos.getY(),
                    doublebarrels$connectionPos.getZ()});
            output.putBoolean("DoubleBarrelMain", doublebarrels$isMain);
        }
    }

}
