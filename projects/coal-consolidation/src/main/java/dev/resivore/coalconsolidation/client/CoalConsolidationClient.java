package dev.resivore.coalconsolidation.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Items;

public final class CoalConsolidationClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(output -> {
            output.getDisplayStacks().removeIf(stack -> stack.is(Items.CHARCOAL));
            output.getSearchTabStacks().removeIf(stack -> stack.is(Items.CHARCOAL));
        });
    }
}
