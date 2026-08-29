package dev.resivore.dragonbound.channel;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class DragonboundReturnItem extends Item {
    private final ReturnSource source;

    public DragonboundReturnItem(Properties properties, ReturnSource source) {
        super(properties);
        this.source = source;
    }

    public ReturnSource source() {
        return source;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.FAIL;
        }

        // A click is only an input edge. The server owns the pending channel after this response;
        // no local or server-side vanilla sustained-use state is entered.
        if (level.isClientSide()) {
            return InteractionResult.CONSUME;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            ItemStack stack = player.getItemInHand(hand);
            return DragonboundChannelManager.start(serverPlayer, stack, source)
                    ? InteractionResult.CONSUME
                    : InteractionResult.FAIL;
        }

        return InteractionResult.FAIL;
    }
}
