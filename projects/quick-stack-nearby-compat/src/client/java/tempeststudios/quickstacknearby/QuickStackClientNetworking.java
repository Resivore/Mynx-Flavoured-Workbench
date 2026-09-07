package tempeststudios.quickstacknearby;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class QuickStackClientNetworking {
    private QuickStackClientNetworking() {
    }

    public static void sendQuickStackRequest() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }

        if (!QuickStackClientNetworkingCompat.canSendQuickStack()) {
            ClientFeedbackCompat.displayClientMessage(
                    client.player,
                    Component.literal("Quick Stack Nearby is not available on this server.")
            );
            return;
        }

        QuickStackClientNetworkingCompat.sendQuickStackRequest(new QuickStackRequestPayload(
                QuickStackRuleStore.getInstance().payloadRules()
        ));
    }
}
