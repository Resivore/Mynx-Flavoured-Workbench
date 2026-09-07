package tempeststudios.quickstacknearby;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

final class QuickStackHotkey {
    private static final String QUICK_STACK_KEY = "key.quick-stack-nearby.quick_stack";
    private static final int REPEAT_INTERVAL_TICKS = 5;

    private static KeyMapping quickStackKey;
    private static int repeatCooldownTicks;

    private QuickStackHotkey() {
    }

    static void register() {
        if (quickStackKey != null) {
            return;
        }

        quickStackKey = QuickStackKeyBindingCompat.register(QUICK_STACK_KEY, InputConstants.KEY_B);
        ClientTickEvents.END_CLIENT_TICK.register(QuickStackHotkey::tick);
    }

    private static void tick(Minecraft client) {
        if (!canQuickStack(client)) {
            clearQueuedKeypresses();
            repeatCooldownTicks = 0;
            return;
        }

        if (!quickStackKey.isDown()) {
            clearQueuedKeypresses();
            repeatCooldownTicks = 0;
            return;
        }

        clearQueuedKeypresses();
        if (repeatCooldownTicks > 0) {
            repeatCooldownTicks--;
        }
        if (repeatCooldownTicks == 0) {
            QuickStackClientNetworking.sendQuickStackRequest();
            repeatCooldownTicks = REPEAT_INTERVAL_TICKS;
        }
    }

    private static boolean canQuickStack(Minecraft client) {
        return quickStackKey != null
                && client != null
                && client.player != null
                && client.level != null
                && !ClientStateCompat.isScreenOpen(client);
    }

    private static void clearQueuedKeypresses() {
        while (quickStackKey != null && quickStackKey.consumeClick()) {
            // Drain queued presses so typing in screens cannot trigger after close.
        }
    }
}
