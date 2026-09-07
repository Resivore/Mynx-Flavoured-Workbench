package tempeststudios.quickstacknearby;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

final class PlayerFeedbackCompat {
    private PlayerFeedbackCompat() {
    }

    static void displayClientMessage(ServerPlayer player, Component message) {
        player.sendSystemMessage(message, true);
    }
}
