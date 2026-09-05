package com.yungnickyoung.minecraft.ribbits.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/** The unlimited-use Drop Leaf; deployment state lives on the player, never on the stack. */
public final class ChuteLeafItem extends Item {
    public ChuteLeafItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> tooltip,
            TooltipFlag flag
    ) {
        tooltip.accept(Component.translatable("item.ribbits.chute_leaf.tooltip")
                .withStyle(ChatFormatting.GRAY));
    }
}
