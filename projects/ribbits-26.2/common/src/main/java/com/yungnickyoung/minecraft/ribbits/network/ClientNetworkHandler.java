package com.yungnickyoung.minecraft.ribbits.network;

import com.yungnickyoung.minecraft.ribbits.RibbitsCommon;
import com.yungnickyoung.minecraft.ribbits.client.sound.PlayerInstrumentSoundInstance;
import com.yungnickyoung.minecraft.ribbits.client.sound.RibbitInstrumentSoundInstance;
import com.yungnickyoung.minecraft.ribbits.client.supporters.RibbitOptionsJSON;
import com.yungnickyoung.minecraft.ribbits.client.supporters.SupportersListClient;
import com.yungnickyoung.minecraft.ribbits.data.RibbitInstrument;
import com.yungnickyoung.minecraft.ribbits.entity.RibbitEntity;
import com.yungnickyoung.minecraft.ribbits.mixin.interfaces.client.ISoundManagerDuck;
import com.yungnickyoung.minecraft.ribbits.mixin.mixins.client.accessor.ClientLevelAccessor;
import com.yungnickyoung.minecraft.ribbits.module.RibbitInstrumentModule;
import com.yungnickyoung.minecraft.ribbits.module.SoundModule;
import com.yungnickyoung.minecraft.ribbits.network.payload.*;
import com.yungnickyoung.minecraft.ribbits.platform.PlatformHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;
import java.util.function.Consumer;

public class ClientNetworkHandler {
    private static final PendingEntityActions<Entity> PENDING_ENTITY_ACTIONS = new PendingEntityActions<>();

    public static void onEntityLoad(Entity entity) {
        PENDING_ENTITY_ACTIONS.onEntityAvailable(entity.getUUID(), entity);
    }

    public static void clearPendingActions() {
        PENDING_ENTITY_ACTIONS.clear();
    }

    private static void queueOrExecute(UUID entityId, Consumer<Entity> action) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }

        Entity entity = ((ClientLevelAccessor) level).callGetEntities().get(entityId);
        PENDING_ENTITY_ACTIONS.executeOrQueue(entityId, entity, action);
    }

    public static void handleStartMusicSingleS2C(RibbitStartMusicSinglePayload payload) {
        RibbitInstrument instrument = RibbitInstrumentModule.getInstrument(payload.instrumentId());
        if (instrument == null || instrument == RibbitInstrumentModule.NONE) {
            RibbitsCommon.LOGGER.error("StartMusicSingle: invalid instrument {}", payload.instrumentId());
            return;
        }

        queueOrExecute(payload.ribbitUUID(), entity -> {
            if (!(entity instanceof RibbitEntity ribbit)) {
                RibbitsCommon.LOGGER.error("StartMusicSingle: entity {} is not a ribbit", payload.ribbitUUID());
                return;
            }

            SoundEvent evt = instrument.soundEvent();
            Minecraft.getInstance().getSoundManager().play(
                    new RibbitInstrumentSoundInstance(ribbit, payload.tickOffset(), evt));
        });
    }

    public static void handleStopMusicSingleS2C(RibbitStopMusicSinglePayload payload) {
        queueOrExecute(payload.ribbitUUID(), entity ->
                ((ISoundManagerDuck) Minecraft.getInstance().getSoundManager())
                        .ribbits$stopRibbitsMusic(payload.ribbitUUID()));
    }

    public static void handleStartMusicAllS2C(RibbitStartMusicAllPayload payload) {
        if (payload.ribbitUUIDs().size() != payload.instrumentIds().size()) {
            RibbitsCommon.LOGGER.error("StartMusicAll: {} ribbits != {} instruments",
                    payload.ribbitUUIDs().size(), payload.instrumentIds().size());
            return;
        }
        for (int i = 0; i < payload.ribbitUUIDs().size(); i++) {
            RibbitInstrument instrument = RibbitInstrumentModule.getInstrument(payload.instrumentIds().get(i));
            if (instrument == null || instrument == RibbitInstrumentModule.NONE) {
                RibbitsCommon.LOGGER.error("StartMusicAll: invalid instrument {}", payload.instrumentIds().get(i));
                return;
            }

            UUID entityId = payload.ribbitUUIDs().get(i);
            queueOrExecute(entityId, entity -> {
                if (!(entity instanceof RibbitEntity ribbit)) {
                    RibbitsCommon.LOGGER.error("StartMusicAll: entity {} is not a ribbit", entityId);
                    return;
                }

                SoundEvent evt = instrument.soundEvent();
                Minecraft.getInstance().getSoundManager().play(
                        new RibbitInstrumentSoundInstance(ribbit, payload.tickOffset(), evt));
            });
        }
    }

    public static void handleStartHearingMaracaS2C(StartHearingMaracaPayload payload) {
        queueOrExecute(payload.performerUUID(), performer -> {
            if (!(performer instanceof Player player)) {
                RibbitsCommon.LOGGER.error("StartMaraca: performer {} invalid", payload.performerUUID());
                return;
            }

            Minecraft.getInstance().getSoundManager().play(
                    new PlayerInstrumentSoundInstance(player, -1, SoundModule.MUSIC_MARACA.get()));
        });
    }

    public static void handleStopHearingMaracaS2C(StopHearingMaracaPayload payload) {
        queueOrExecute(payload.performerUUID(), performer -> {
            if (!(performer instanceof Player)) {
                RibbitsCommon.LOGGER.error("StopMaraca: performer {} invalid", payload.performerUUID());
                return;
            }

            ((ISoundManagerDuck) Minecraft.getInstance().getSoundManager())
                    .ribbits$stopMaraca(payload.performerUUID());
        });
    }

    public static void handleRequestSupporterHatStateS2C(RequestSupporterHatStatePayload payload) {
        SupportersListClient.clear();
        payload.enabledSupporterHatPlayers().forEach(uuid -> SupportersListClient.toggleSupporterHat(uuid, true));
        notifyServerOfSupporterHatState(RibbitOptionsJSON.get().isSupporterHatEnabled());
    }

    public static void handleToggleSupporterHatS2C(ToggleSupporterHatPayloadS2C payload) {
        SupportersListClient.toggleSupporterHat(payload.playerUUID(), payload.enabled());
    }

    public static void notifyServerOfSupporterHatState(boolean enabled) {
        UUID playerUUID = Minecraft.getInstance().getUser().getProfileId();
        if (Minecraft.getInstance().getConnection() == null) return;
        PlatformHelper.sendToServer(new ToggleSupporterHatPayloadC2S(playerUUID, enabled));
    }
}
