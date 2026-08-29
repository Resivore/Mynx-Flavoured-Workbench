package com.yungnickyoung.minecraft.ribbits.platform;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;

import java.nio.file.Path;
import java.util.List;
import java.util.ServiceLoader;
import java.util.function.Consumer;

public class PlatformHelper {
    private static final IPlatformHelper SERVICE = ServiceLoader.load(IPlatformHelper.class).findFirst().orElseThrow();
    private static Consumer<CustomPacketPayload> clientPacketSender;

    public static IPlatformHelper getPlatformService() {
        return SERVICE;
    }

    public static MinecraftServer getCurrentServer() {
        return SERVICE.getCurrentServer();
    }

    public static void setBlockAsFlammable(Block block, int igniteChance, int burnChance) {
        SERVICE.setBlockAsFlammable(block, igniteChance, burnChance);
    }

    public static String getPlatformName() {
        return SERVICE.getPlatformName();
    }

    public static Path getConfigFolder() {
        return SERVICE.getConfigFolder();
    }

    public static boolean isDevelopmentEnvironment() {
        return SERVICE.isDevelopmentEnvironment();
    }

    public static boolean isClient() {
        return SERVICE.isClient();
    }

    public static boolean isServer() {
        return SERVICE.isServer();
    }

    public static <T extends CustomPacketPayload> void sendToPlayer(ServerPlayer player, T payload) {
        SERVICE.sendToPlayer(player, payload);
    }

    public static <T extends CustomPacketPayload> void sendToPlayers(List<ServerPlayer> players, T payload) {
        SERVICE.sendToPlayers(players, payload);
    }

    public static void setClientPacketSender(Consumer<CustomPacketPayload> sender) {
        clientPacketSender = sender;
    }

    public static <T extends CustomPacketPayload> void sendToServer(T payload) {
        if (clientPacketSender == null) {
            throw new IllegalStateException("Client packet sender has not been initialized");
        }
        clientPacketSender.accept(payload);
    }
}
