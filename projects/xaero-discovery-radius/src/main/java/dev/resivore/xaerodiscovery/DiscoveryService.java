package dev.resivore.xaerodiscovery;

import java.nio.file.Path;
import java.util.function.IntBinaryOperator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;

public final class DiscoveryService {
    private static volatile DiscoveryConfig config = DiscoveryConfig.defaults();
    private static volatile DiscoveryHistoryStore historyStore;
    private static volatile LegacyCacheManifest legacyCacheManifest;
    private static volatile Session session;

    private DiscoveryService() {
    }

    static void initialize(Path configDirectory, Path gameDirectory, Logger logger) {
        config = DiscoveryConfig.load(configDirectory.resolve("xaero-discovery-radius.json"), logger);
        historyStore = new DiscoveryHistoryStore(logger);
        legacyCacheManifest = LegacyCacheManifest.captureOrLoad(
                gameDirectory,
                configDirectory.resolve("xaero-discovery-radius"),
                logger
        );
        logger.info(
                "Xaero Discovery Radius initialized with an inclusive square radius of {} chunks",
                config.discoveryRadiusChunks()
        );
    }

    public static int clampWorldMapWriteDistance(int xaeroDistance) {
        return Math.min(xaeroDistance, config.discoveryRadiusChunks());
    }

    public static boolean mayReadLiveChunk(ClientLevel world, int chunkX, int chunkZ) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null || player.level() == null) {
            return false;
        }
        if (!player.level().dimension().equals(world.dimension())) {
            return false;
        }
        ChunkPos playerChunk = player.chunkPosition();
        return ChunkRadius.contains(
                playerChunk.x(),
                playerChunk.z(),
                chunkX,
                chunkZ,
                config.discoveryRadiusChunks()
        );
    }

    public static boolean mayReadSingleplayerSaveChunk(ServerLevel world, int chunkX, int chunkZ) {
        DiscoveryHistoryStore store = historyStore;
        Session current = session;
        if (store == null || current == null || current.server != world.getServer()) {
            return true;
        }
        return store.allows(key(world), chunkX, chunkZ);
    }

    public static void importXaeroCache(
            ServerLevel world,
            Path cacheFile,
            int tileChunkX,
            int tileChunkZ,
            IntBinaryOperator heightAtPixel
    ) {
        DiscoveryHistoryStore store = historyStore;
        LegacyCacheManifest manifest = legacyCacheManifest;
        Session current = session;
        if (store != null
                && manifest != null
                && current != null
                && current.server == world.getServer()
                && manifest.permits(cacheFile)) {
            store.recordPacked(key(world), CachedChunkDiscovery.find(tileChunkX, tileChunkZ, heightAtPixel));
        }
    }

    static void onServerStarting(MinecraftServer server) {
        DiscoveryHistoryStore store = historyStore;
        if (store != null) {
            store.resetSession();
        }
        session = new Session(server);
    }

    static void onServerLevelLoad(MinecraftServer server, ServerLevel level) {
        DiscoveryHistoryStore store = historyStore;
        Session current = session;
        if (store != null && current != null && current.server == server && level.getServer() == server) {
            WorldDimensionKey key = key(level);
            store.prepare(key);
            LegacyCacheManifest manifest = legacyCacheManifest;
            if (manifest == null || !manifest.isAvailable()) {
                store.markFailOpen(key, "legacy Xaero cache manifest unavailable");
            }
        }
    }

    static void onServerStopped(MinecraftServer server) {
        DiscoveryHistoryStore store = historyStore;
        Session current = session;
        if (store != null && current != null && current.server == server) {
            store.resetSession();
            session = null;
        }
    }

    static void onEndClientTick(Minecraft client) {
        DiscoveryHistoryStore store = historyStore;
        MinecraftServer server = client.getSingleplayerServer();
        LocalPlayer player = client.player;
        Session current = session;
        if (store == null || server == null || player == null || current == null || current.server != server) {
            return;
        }

        ServerLevel currentLevel = server.getLevel(player.level().dimension());
        if (currentLevel == null) {
            return;
        }
        ChunkPos playerChunk = player.chunkPosition();
        WorldDimensionKey key = key(currentLevel);
        int radius = config.discoveryRadiusChunks();
        if (current.shouldRecord(key, playerChunk.x(), playerChunk.z(), radius)) {
            store.recordSquare(key, playerChunk.x(), playerChunk.z(), radius);
        }
    }

    static int configuredRadius() {
        return config.discoveryRadiusChunks();
    }

    private static WorldDimensionKey key(ServerLevel world) {
        Path worldRoot = world.getServer().getWorldPath(LevelResource.ROOT);
        Path dimensionDirectory = DimensionType.getStorageFolder(world.dimension(), worldRoot);
        return new WorldDimensionKey(dimensionDirectory, world.dimension().identifier().toString());
    }

    private static final class Session {
        private final MinecraftServer server;
        private WorldDimensionKey lastKey;
        private int lastChunkX;
        private int lastChunkZ;
        private int lastRadius;
        private boolean hasLastCenter;

        private Session(MinecraftServer server) {
            this.server = server;
        }

        private boolean shouldRecord(WorldDimensionKey key, int chunkX, int chunkZ, int radius) {
            if (hasLastCenter
                    && key.equals(lastKey)
                    && chunkX == lastChunkX
                    && chunkZ == lastChunkZ
                    && radius == lastRadius) {
                return false;
            }
            lastKey = key;
            lastChunkX = chunkX;
            lastChunkZ = chunkZ;
            lastRadius = radius;
            hasLastCenter = true;
            return true;
        }
    }
}
