package com.yungnickyoung.minecraft.ribbits.client.chute;

import com.yungnickyoung.minecraft.ribbits.chute.ChuteAckState;
import com.yungnickyoung.minecraft.ribbits.chute.ChuteEquipment;
import com.yungnickyoung.minecraft.ribbits.chute.ChuteInputState;
import com.yungnickyoung.minecraft.ribbits.chute.ChutePhysics;
import com.yungnickyoung.minecraft.ribbits.chute.ChutePlayerAccess;
import com.yungnickyoung.minecraft.ribbits.client.render.ChuteLeafRenderer;
import com.yungnickyoung.minecraft.ribbits.network.payload.ChuteAckS2C;
import com.yungnickyoung.minecraft.ribbits.network.payload.ChutePressC2S;
import com.yungnickyoung.minecraft.ribbits.platform.PlatformHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Physical input capture, owner prediction, acknowledgements, and narrow movement mirroring. */
public final class ChuteClientController {
    private static final ChuteInputState INPUT = new ChuteInputState();

    private static ClientPacketListener connectionIdentity;
    private static Identifier dimensionIdentity;
    private static ChutePressC2S queuedPress;
    private static long latestRequestSequence = Long.MIN_VALUE;
    private static long latestAcknowledgedSequence = Long.MIN_VALUE;
    private static ChuteAckState prediction = ChuteAckState.REJECTED;

    private ChuteClientController() {
    }

    public static void initialize() {
        ChuteLeafRenderer.register();
    }

    /** Called at KeyboardInput.tick RETURN, before LocalPlayer can synthesize auto-jump. */
    public static void capturePhysicalJump(boolean physicalJump) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientPacketListener connection = minecraft.getConnection();
        if (connection != connectionIdentity) {
            resetConnectionState();
            connectionIdentity = connection;
        }

        LocalPlayer player = minecraft.player;
        Identifier dimension = player == null ? null : player.level().dimension().identifier();
        if (dimension != null && !dimension.equals(dimensionIdentity)) {
            dimensionIdentity = dimension;
            queuedPress = null;
            prediction = ChuteAckState.REJECTED;
        }

        boolean maySend = player != null
                && connection != null
                && minecraft.gui.screen() == null
                && minecraft.isWindowActive()
                && !player.onGround()
                && ChuteEquipment.hasActiveChute(player);

        INPUT.sample(physicalJump, maySend).ifPresent(sequence -> {
            Identifier currentDimension = player.level().dimension().identifier();
            queuedPress = new ChutePressC2S(sequence, currentDimension);
            latestRequestSequence = sequence;
            prediction = player.getDeltaMovement().y <= 0.0D
                    ? ChuteAckState.DEPLOYED
                    : ChuteAckState.PENDING;
        });
    }

    /** LocalPlayer.tick TAIL is after vanilla's normal player-input packet when it changed. */
    public static void flushAfterVanillaInput(LocalPlayer player) {
        maintainLocalState(player);
        ChutePressC2S press = queuedPress;
        queuedPress = null;
        Minecraft minecraft = Minecraft.getInstance();
        if (press == null
                || minecraft.getConnection() == null
                || minecraft.gui.screen() != null
                || !minecraft.isWindowActive()
                || !press.dimensionId().equals(player.level().dimension().identifier())) {
            return;
        }
        PlatformHelper.sendToServer(press);
    }

    public static void handleAcknowledgement(ChuteAckS2C payload) {
        if (payload.sequence() < latestRequestSequence
                || payload.sequence() < latestAcknowledgedSequence) {
            return;
        }
        latestAcknowledgedSequence = payload.sequence();
        prediction = payload.state();
    }

    public static boolean isRenderedDeployed(Player player) {
        boolean authoritative = ((ChutePlayerAccess) player).ribbits$isChuteDeployed();
        Minecraft minecraft = Minecraft.getInstance();
        boolean predictedOwner = player == minecraft.player && prediction == ChuteAckState.DEPLOYED;
        return (authoritative || predictedOwner) && ChuteEquipment.canRemainOpen(player);
    }

    /** Owner-only mirror of the exact server cap; remote players use normal entity movement. */
    public static void applyOwnerDescentCap(LocalPlayer player) {
        if (!isRenderedDeployed(player)) {
            return;
        }
        Vec3 current = player.getDeltaMovement();
        Vec3 capped = ChutePhysics.capDescent(current);
        if (capped != current) {
            player.setDeltaMovement(capped);
        }
        player.resetFallDistance();
    }

    public static void resetConnection() {
        resetConnectionState();
        connectionIdentity = null;
        dimensionIdentity = null;
    }

    private static void maintainLocalState(LocalPlayer player) {
        Identifier currentDimension = player.level().dimension().identifier();
        if (!currentDimension.equals(dimensionIdentity)) {
            dimensionIdentity = currentDimension;
            queuedPress = null;
            prediction = ChuteAckState.REJECTED;
        }
        if (!ChuteEquipment.canRemainOpen(player)) {
            prediction = ChuteAckState.REJECTED;
        }
    }

    private static void resetConnectionState() {
        INPUT.reset();
        queuedPress = null;
        latestRequestSequence = Long.MIN_VALUE;
        latestAcknowledgedSequence = Long.MIN_VALUE;
        prediction = ChuteAckState.REJECTED;
    }
}
