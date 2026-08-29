package dev.aero.shulkertrowel.client;

import dev.aero.shulkertrowel.item.ModItems;
import dev.aero.shulkertrowel.network.ChangeTrowelGeometryPayload;
import dev.tazer.clutternomore.ClutterNoMoreClient;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.InvocationTargetException;

/** Trowel-only branch at CNM's existing shape-key entry point. */
public final class TrowelShapeInput {
    private static final int RELEASE = 0;
    private static final int PRESS = 1;

    private TrowelShapeInput() {}

    public static boolean tryHandle(int key, int action) {
        if (key != ClutterNoMoreClient.shapeKey() || controllerInputIsActive()) return false;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gui.screen() != null || minecraft.player == null) return false;

        ItemStack held = minecraft.player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!held.is(ModItems.TROWEL)
                || !ClientPlayNetworking.canSend(ChangeTrowelGeometryPayload.TYPE)) {
            return false;
        }

        if (action == PRESS) ClutterNoMoreClient.keyHeld = true;
        if (action == RELEASE) ClutterNoMoreClient.keyHeld = false;

        switch (CnmInputSettings.inputType()) {
            case "TOGGLE" -> toggle(minecraft, held, action);
            case "PRESS" -> press(minecraft, held, action);
            default -> hold(minecraft, held, action);
        }
        return true;
    }

    private static void hold(Minecraft minecraft, ItemStack held, int action) {
        if (action == PRESS && !(ClutterNoMoreClient.OVERLAY instanceof TrowelShapeSwitcherOverlay)) {
            ClutterNoMoreClient.OVERLAY = new TrowelShapeSwitcherOverlay(minecraft, held, true);
        } else if (action == RELEASE
                && ClutterNoMoreClient.OVERLAY instanceof TrowelShapeSwitcherOverlay) {
            ClutterNoMoreClient.OVERLAY = null;
        }
    }

    private static void toggle(Minecraft minecraft, ItemStack held, int action) {
        if (action != PRESS) return;
        ClutterNoMoreClient.OVERLAY = ClutterNoMoreClient.OVERLAY instanceof TrowelShapeSwitcherOverlay
                ? null
                : new TrowelShapeSwitcherOverlay(minecraft, held, true);
    }

    private static void press(Minecraft minecraft, ItemStack held, int action) {
        if (action != PRESS) return;
        TrowelShapeSwitcherOverlay overlay = new TrowelShapeSwitcherOverlay(minecraft, held, false);
        ClutterNoMoreClient.OVERLAY = overlay;
        overlay.onMouseScrolled(-1);
        ClutterNoMoreClient.OVERLAY = null;
    }

    private static boolean controllerInputIsActive() {
        if (!FabricLoader.getInstance().isModLoaded("controlify")) return false;
        try {
            Class<?> compat = Class.forName(
                    "dev.tazer.clutternomore.common.compat.ControlifyCompat",
                    false,
                    TrowelShapeInput.class.getClassLoader()
            );
            return (boolean) compat.getMethod("currentInputModeIsController").invoke(null);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException
                 | InvocationTargetException | LinkageError exception) {
            // If Controlify is present but its compatibility seam changed, do not steal input.
            return true;
        }
    }
}
