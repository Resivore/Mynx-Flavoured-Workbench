package com.crispytwig.naturalist.server.entity.ai.goal;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;

public class SearchForItemsGoal extends Goal {
    private final PathfinderMob mob;
    private final double speedModifier;
    private final double horizontalSearchRange;
    private final double verticalSearchRange;
    private final Predicate<ItemStack> items;

    public SearchForItemsGoal(PathfinderMob mob, double speedModifier, Predicate<ItemStack> items, double horizontalSearchRange, double verticalSearchRange) {
        this.setFlags(EnumSet.of(Flag.MOVE));
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.items = items;
        this.horizontalSearchRange = horizontalSearchRange;
        this.verticalSearchRange = verticalSearchRange;
    }

    @Override
    public boolean canUse() {
        if (mob.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty()) {
            List<ItemEntity> list = mob.level().getEntitiesOfClass(ItemEntity.class, mob.getBoundingBox().inflate(horizontalSearchRange, verticalSearchRange, horizontalSearchRange), itemEntity -> items.test(itemEntity.getItem()));
            return !list.isEmpty() && mob.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty();
        }
        return false;
    }

    @Override
    public void tick() {
        List<ItemEntity> list = mob.level().getEntitiesOfClass(ItemEntity.class, mob.getBoundingBox().inflate(horizontalSearchRange, verticalSearchRange, horizontalSearchRange), itemEntity -> items.test(itemEntity.getItem()));
        if (mob.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty() && !list.isEmpty()) {
            mob.getNavigation().moveTo(list.getFirst(), speedModifier);
        }

    }

    @Override
    public void start() {
        List<ItemEntity> list = mob.level().getEntitiesOfClass(ItemEntity.class, mob.getBoundingBox().inflate(horizontalSearchRange, verticalSearchRange, horizontalSearchRange), itemEntity -> items.test(itemEntity.getItem()));
        if (!list.isEmpty()) {
            mob.getNavigation().moveTo(list.getFirst(), speedModifier);
        }

    }
}
