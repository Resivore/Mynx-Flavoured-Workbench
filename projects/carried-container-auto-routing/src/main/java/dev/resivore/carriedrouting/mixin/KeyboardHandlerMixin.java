package dev.resivore.carriedrouting.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import dev.resivore.carriedrouting.client.CarriedContainerAutoRoutingClient;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
abstract class KeyboardHandlerMixin {
    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void carriedRouting$handleContainerLockKey(
            long window,
            int action,
            KeyEvent event,
            CallbackInfo ci
    ) {
        Minecraft client = Minecraft.getInstance();
        if (window != client.getWindow().handle()
                || action != InputConstants.PRESS
                || !(client.gui.screen() instanceof AbstractContainerScreen<?>)) return;
        if (CarriedContainerAutoRoutingClient.handleContainerKey(client, event)) ci.cancel();
    }
}
