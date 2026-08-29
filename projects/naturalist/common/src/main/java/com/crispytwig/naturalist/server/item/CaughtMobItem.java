package com.crispytwig.naturalist.server.item;

import com.crispytwig.naturalist.server.entity.base.Catchable;
import com.crispytwig.naturalist.server.entity.base.ContainerBoundWorker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class CaughtMobItem extends NaturalistBucketItem {
    private final Supplier<? extends EntityType<?>> typeSup;

    private EntityType<?> type() {
        return this.typeSup.get();
    }

    public CaughtMobItem(Supplier<? extends EntityType<?>> entitySupplier, Supplier<? extends Fluid> fluidSupplier, Supplier<? extends SoundEvent> soundSupplier, Properties properties) {
        this(entitySupplier, fluidSupplier, soundSupplier, null, null, properties);
    }

    public CaughtMobItem(Supplier<? extends EntityType<?>> entitySupplier, Supplier<? extends Fluid> fluidSupplier, Supplier<? extends SoundEvent> soundSupplier, @Nullable String tooltipPrefix, @Nullable String[] variantNames, Properties properties) {
        super(entitySupplier.get(), fluidSupplier.get(), soundSupplier.get(), properties, true, tooltipPrefix, variantNames);
        this.typeSup = entitySupplier;
    }

    private void spawn(ServerLevel serverLevel, ItemStack itemStack, BlockPos pos) {
        Entity entity = this.type().spawn(serverLevel, itemStack, null, pos, EntitySpawnReason.BUCKET, true, false);
        if (entity instanceof Catchable catchable) {
            catchable.loadFromHandTag(itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag());
            catchable.setFromHand(true);
        }

        if (entity instanceof ContainerBoundWorker worker) {
            worker.tryAssignWorkstation(pos);
        }

    }

    @Override
    public void checkExtraContent(@Nullable Player player, @NotNull Level level, @NotNull ItemStack containerStack, @NotNull BlockPos pos) {
        if (level instanceof ServerLevel) {
            this.spawn((ServerLevel)level, containerStack, pos);
            level.gameEvent(player, GameEvent.ENTITY_PLACE, pos);
        }

    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        BlockHitResult blockhitresult = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);
        if (blockhitresult.getType() == HitResult.Type.MISS) {
            return InteractionResultHolder.pass(itemstack);
        } else if (blockhitresult.getType() != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(itemstack);
        } else {
            BlockPos pos = blockhitresult.getBlockPos();
            Direction direction = blockhitresult.getDirection();
            if (level.mayInteract(player, pos) && player.mayUseItemAt(pos.relative(direction), direction, itemstack)) {
                this.checkExtraContent(player, level, itemstack, pos);
                this.playEmptySound(player, level, pos);
                player.awardStat(Stats.ITEM_USED.get(this));
                return InteractionResultHolder.sidedSuccess(getEmptySuccessItem(itemstack, player), level.isClientSide());
            } else {
                return InteractionResultHolder.fail(itemstack);
            }
        }
    }

    public static @NotNull ItemStack getEmptySuccessItem(@NotNull ItemStack bucketStack, Player player) {
        return !player.getAbilities().instabuild ? new ItemStack(Items.AIR) : bucketStack;
    }
}
