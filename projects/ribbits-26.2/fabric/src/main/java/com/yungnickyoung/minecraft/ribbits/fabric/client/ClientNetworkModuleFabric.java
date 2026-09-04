package com.yungnickyoung.minecraft.ribbits.fabric.client;

import com.yungnickyoung.minecraft.ribbits.client.chute.ChuteClientController;
import com.yungnickyoung.minecraft.ribbits.network.ClientNetworkHandler;
import com.yungnickyoung.minecraft.ribbits.network.payload.ChuteAckS2C;
import com.yungnickyoung.minecraft.ribbits.network.payload.RequestSupporterHatStatePayload;
import com.yungnickyoung.minecraft.ribbits.network.payload.RibbitStartMusicAllPayload;
import com.yungnickyoung.minecraft.ribbits.network.payload.RibbitStartMusicSinglePayload;
import com.yungnickyoung.minecraft.ribbits.network.payload.RibbitStopMusicSinglePayload;
import com.yungnickyoung.minecraft.ribbits.network.payload.StartHearingMaracaPayload;
import com.yungnickyoung.minecraft.ribbits.network.payload.StopHearingMaracaPayload;
import com.yungnickyoung.minecraft.ribbits.network.payload.ToggleSupporterHatPayloadS2C;
import com.yungnickyoung.minecraft.ribbits.platform.PlatformHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

final class ClientNetworkModuleFabric {
    private ClientNetworkModuleFabric() {
    }

    static void register() {
        PlatformHelper.setClientPacketSender(ClientPlayNetworking::send);
        ChuteClientController.initialize();
        ClientPlayNetworking.registerGlobalReceiver(RibbitStartMusicSinglePayload.TYPE, (payload, context) ->
                ClientNetworkHandler.handleStartMusicSingleS2C(payload));
        ClientPlayNetworking.registerGlobalReceiver(RibbitStopMusicSinglePayload.TYPE, (payload, context) ->
                ClientNetworkHandler.handleStopMusicSingleS2C(payload));
        ClientPlayNetworking.registerGlobalReceiver(RibbitStartMusicAllPayload.TYPE, (payload, context) ->
                ClientNetworkHandler.handleStartMusicAllS2C(payload));
        ClientPlayNetworking.registerGlobalReceiver(StartHearingMaracaPayload.TYPE, (payload, context) ->
                ClientNetworkHandler.handleStartHearingMaracaS2C(payload));
        ClientPlayNetworking.registerGlobalReceiver(StopHearingMaracaPayload.TYPE, (payload, context) ->
                ClientNetworkHandler.handleStopHearingMaracaS2C(payload));
        ClientPlayNetworking.registerGlobalReceiver(RequestSupporterHatStatePayload.TYPE, (payload, context) ->
                ClientNetworkHandler.handleRequestSupporterHatStateS2C(payload));
        ClientPlayNetworking.registerGlobalReceiver(ToggleSupporterHatPayloadS2C.TYPE, (payload, context) ->
                ClientNetworkHandler.handleToggleSupporterHatS2C(payload));
        ClientPlayNetworking.registerGlobalReceiver(ChuteAckS2C.TYPE, (payload, context) ->
                ClientNetworkHandler.handleChuteAckS2C(payload));
    }
}
