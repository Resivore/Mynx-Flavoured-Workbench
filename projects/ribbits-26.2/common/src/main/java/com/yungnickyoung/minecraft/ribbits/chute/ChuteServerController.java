package com.yungnickyoung.minecraft.ribbits.chute;

import com.yungnickyoung.minecraft.ribbits.network.payload.ChuteAckS2C;
import com.yungnickyoung.minecraft.ribbits.network.payload.ChutePressC2S;
import com.yungnickyoung.minecraft.ribbits.platform.PlatformHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Server authority for transient request, pending, deployed, and movement state. */
public final class ChuteServerController {
    private static final Map<ServerPlayer, PlayerState> STATES = new IdentityHashMap<>();

    private ChuteServerController() {
    }

    /** Explicit hook for the Fabric integration owner; deliberately performs no persistent load. */
    public static void initialize() {
    }

    public static void handlePress(ServerPlayer player, ChutePressC2S payload) {
        ResourceKey<Level> dimension = player.level().dimension();
        PlayerState state = STATES.get(player);
        if (state != null && state.dimension() != dimension) {
            clearTracked(player);
            STATES.remove(player);
            state = null;
        }
        if (state == null) {
            state = new PlayerState(dimension, new ChuteStateMachine());
            STATES.put(player, state);
        }

        long serverTick = ((ServerLevel) player.level()).getServer().getTickCount();
        boolean dimensionMatches = dimension.identifier().equals(payload.dimensionId());
        boolean valid = ChuteEquipment.canRemainOpen(player);
        ChuteAckState acknowledgement = state.machine().press(
                payload.sequence(),
                serverTick,
                dimensionMatches,
                valid,
                player.getDeltaMovement().y
        );

        syncTracked(player, state.machine().isDeployed());
        PlatformHelper.sendToPlayer(player, new ChuteAckS2C(payload.sequence(), acknowledgement));
    }

    /** Called exactly once at the end of each logical server tick. */
    public static void tick(MinecraftServer server) {
        Set<ServerPlayer> online = Collections.newSetFromMap(new IdentityHashMap<>());
        online.addAll(server.getPlayerList().getPlayers());
        long serverTick = server.getTickCount();

        Iterator<Map.Entry<ServerPlayer, PlayerState>> iterator = STATES.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<ServerPlayer, PlayerState> entry = iterator.next();
            ServerPlayer player = entry.getKey();
            PlayerState state = entry.getValue();
            if (!online.contains(player) || player.isRemoved()) {
                clearTracked(player);
                iterator.remove();
                continue;
            }

            boolean sameDimension = state.dimension() == player.level().dimension();
            boolean valid = sameDimension && ChuteEquipment.canRemainOpen(player);
            Optional<ChuteStateMachine.Acknowledgement> acknowledgement = state.machine().tick(
                    serverTick,
                    valid,
                    player.getDeltaMovement().y
            );
            acknowledgement.ifPresent(ack -> PlatformHelper.sendToPlayer(
                    player,
                    new ChuteAckS2C(ack.sequence(), ack.state())
            ));
            syncTracked(player, state.machine().isDeployed());

            if (!sameDimension) {
                iterator.remove();
            }
        }
    }

    /** Narrow pre/post-travel hook. It changes only an overly-negative Y component. */
    public static void applyDescentCap(ServerPlayer player) {
        PlayerState state = STATES.get(player);
        if (state == null || !state.machine().isDeployed()) {
            return;
        }
        if (state.dimension() != player.level().dimension() || !ChuteEquipment.canRemainOpen(player)) {
            state.machine().clearMotionState();
            clearTracked(player);
            return;
        }

        Vec3 current = player.getDeltaMovement();
        Vec3 capped = ChutePhysics.capDescent(current);
        if (capped != current) {
            player.setDeltaMovement(capped);
        }
        player.resetFallDistance();
    }

    public static void clearAll() {
        STATES.keySet().forEach(ChuteServerController::clearTracked);
        STATES.clear();
    }

    private static void syncTracked(ServerPlayer player, boolean deployed) {
        ((ChutePlayerAccess) player).ribbits$setChuteDeployed(deployed);
    }

    private static void clearTracked(ServerPlayer player) {
        syncTracked(player, false);
    }

    private record PlayerState(ResourceKey<Level> dimension, ChuteStateMachine machine) {
    }
}
