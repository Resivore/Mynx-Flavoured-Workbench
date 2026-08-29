package dev.aero.shulkertrowel.client;

import dev.aero.shulkertrowel.geometry.TargetGeometry;
import dev.aero.shulkertrowel.item.ModItems;
import dev.aero.shulkertrowel.mixin.client.ShapeSwitcherOverlayAccessor;
import dev.aero.shulkertrowel.network.ChangeTrowelGeometryPayload;
import dev.tazer.clutternomore.ClutterNoMoreClient;
import dev.tazer.clutternomore.client.ShapeSwitcherOverlay;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

/** CNM-rendered selector that changes only persisted trowel mode state. */
public final class TrowelShapeSwitcherOverlay extends ShapeSwitcherOverlay {
    public TrowelShapeSwitcherOverlay(Minecraft minecraft, ItemStack trowel, boolean render) {
        this(
                minecraft,
                trowel,
                render,
                TrowelClientModeCache.displayed(trowel, selectedSlot(minecraft))
        );
    }

    private TrowelShapeSwitcherOverlay(
            Minecraft minecraft,
            ItemStack trowel,
            boolean render,
            TargetGeometry initialMode
    ) {
        super(minecraft, TrowelGeometryIcons.stack(initialMode), render);
        ((ShapeSwitcherOverlayAccessor) (Object) this)
                .shulkerTrowel$setShapes(TrowelGeometryIcons.items());
        this.count = trowel.getCount();
        this.selectedIndex = initialMode.networkId();
        this.currentIndex = this.selectedIndex;
        if (minecraft.player != null) this.lastYaw = minecraft.player.getYRot();
    }

    @Override
    public void changeSlot(int requestedIndex) {
        if (!ClientPlayNetworking.canSend(ChangeTrowelGeometryPayload.TYPE)) return;

        int previousIndex = selectedIndex;
        int lastIndex = shapes.size() - 1;
        boolean wraps = CnmInputSettings.wrapScrolling()
                || "PRESS".equals(CnmInputSettings.inputType());
        selectedIndex = wraps
                ? Math.floorMod(requestedIndex, shapes.size())
                : Mth.clamp(requestedIndex, 0, lastIndex);
        if (selectedIndex == previousIndex) return;

        Player player = Objects.requireNonNull(minecraft.player);
        player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.3F, 1.5F);
        ClientPlayNetworking.send(new ChangeTrowelGeometryPayload(selectedIndex));
        TrowelClientModeCache.record(
                player.getItemInHand(InteractionHand.MAIN_HAND),
                selected,
                TargetGeometry.byNetworkId(selectedIndex)
        );
    }

    @Override
    public boolean shouldStayOpenThisTick() {
        if (minecraft.player == null) return false;
        ItemStack held = minecraft.player.getItemInHand(InteractionHand.MAIN_HAND);
        count = held.getCount();
        return held.is(ModItems.TROWEL)
                && ClutterNoMoreClient.selectedSlot(minecraft.player) == selected;
    }

    public ItemStack iconStack(int geometryId) {
        return TrowelGeometryIcons.stack(geometryId);
    }

    private static int selectedSlot(Minecraft minecraft) {
        return minecraft.player == null ? -1 : ClutterNoMoreClient.selectedSlot(minecraft.player);
    }
}
