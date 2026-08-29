package dev.resivore.carriedrouting;

import net.minecraft.world.item.ItemStack;

interface RoutingDestination {
    boolean qualifies(ItemStack incoming);
    int insert(ItemStack incoming);
}
