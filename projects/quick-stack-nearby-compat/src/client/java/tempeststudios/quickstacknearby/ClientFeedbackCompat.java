package tempeststudios.quickstacknearby;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

final class ClientFeedbackCompat {
    private ClientFeedbackCompat() {
    }

    static void displayClientMessage(LocalPlayer player, Component message) {
        player.sendSystemMessage(message);
    }
}
