package com.yungnickyoung.minecraft.ribbits.fabric.module;

import com.yungnickyoung.minecraft.ribbits.chute.ChuteServerController;
import com.yungnickyoung.minecraft.ribbits.network.ServerNetworkHandler;
import com.yungnickyoung.minecraft.ribbits.network.payload.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public class NetworkModuleFabric {
    public static void register() {
        PayloadTypeRegistry.clientboundPlay().register(RibbitStartMusicSinglePayload.TYPE, RibbitStartMusicSinglePayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(RibbitStopMusicSinglePayload.TYPE, RibbitStopMusicSinglePayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(RibbitStartMusicAllPayload.TYPE, RibbitStartMusicAllPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(StartHearingMaracaPayload.TYPE, StartHearingMaracaPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(StopHearingMaracaPayload.TYPE, StopHearingMaracaPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(RequestSupporterHatStatePayload.TYPE, RequestSupporterHatStatePayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ToggleSupporterHatPayloadS2C.TYPE, ToggleSupporterHatPayloadS2C.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ChuteAckS2C.TYPE, ChuteAckS2C.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ToggleSupporterHatPayloadC2S.TYPE, ToggleSupporterHatPayloadC2S.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ChutePressC2S.TYPE, ChutePressC2S.STREAM_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(ToggleSupporterHatPayloadC2S.TYPE, (payload, context) ->
                ServerNetworkHandler.handleToggleSupporterHatC2S(payload));
        ServerPlayNetworking.registerGlobalReceiver(ChutePressC2S.TYPE, (payload, context) ->
                ServerNetworkHandler.handleChutePressC2S(context.player(), payload));

        ChuteServerController.initialize();
        ServerTickEvents.END_SERVER_TICK.register(ChuteServerController::tick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> ChuteServerController.clearAll());
    }
}
