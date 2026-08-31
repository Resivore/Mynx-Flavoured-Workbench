package dev.resivore.xaerodiscovery;

import java.nio.file.Path;
import java.util.function.IntBinaryOperator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.entity.Entity;
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
                "Xaero Discovery Radius initialized with a chunk-quantized circular radius of {} chunks",
                config.discoveryRadiusChunks()
        );
    }

    public static int clampWorldMapWriteDistance(int xaeroDistance) {
        return Math.min(xaeroDistance, config.discoveryRadiusChunks());
    }

    public static boolean mayReadLiveChunk(ClientLevel world, int chunkX, int chunkZ) {
        Minecraft client = Minecraft.getInstance();
        Entity camera = client.getCameraEntity();
        if (camera == null || camera.level() == null) {
            return false;
        }
        if (!camera.level().dimension().equals(world.dimension())) {
            return false;
        }
        ChunkPos cameraChunk = camera.chunkPosition();
        return ChunkRadius.contains(
                cameraChunk.x(),
                cameraChunk.z(),
                chunkX,
                chunkZ,
                config.discoveryRadiusChunks()
        );
    }

    public static boolean mayWriteWorldMapChunk(
            int centerChunkX,
            int centerChunkZ,
            int chunkX,
            int chunkZ
    ) {
        return ChunkRadius.contains(
                centerChunkX,
                centerChunkZ,
                chunkX,
                chunkZ,
                config.discoveryRadiusChunks()
        );
    }

    public static void recordWorldMapLayer(
            Level world,
            int centerChunkX,
            int centerChunkZ,
            int xaeroLayer
    ) {
        DiscoveryHistoryStore store = historyStore;
        Session current = session;
        MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
        if (store != null && current != null && server != null && current.server == server) {
            ServerLevel serverLevel = server.getLevel(world.dimension());
            if (serverLevel != null) {
                store.recordCircle(
                        key(serverLevel),
                        centerChunkX,
                        centerChunkZ,
                        config.discoveryRadiusChunks(),
                        xaeroLayer
                );
            }
        }
    }

    public static boolean mayReadSingleplayerSaveChunk(
            ServerLevel world,
            int chunkX,
            int chunkZ,
            int xaeroLayer
    ) {
        DiscoveryHistoryStore store = historyStore;
        Session current = session;
        if (store == null || current == null || current.server != world.getServer()) {
            return true;
        }
        return store.allows(key(world), chunkX, chunkZ, xaeroLayer);
    }

    public static void importXaeroCache(
            ServerLevel world,
            Path cacheFile,
            int xaeroLayer,
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
            store.recordPacked(
                    key(world),
                    CachedChunkDiscovery.find(tileChunkX, tileChunkZ, heightAtPixel),
                    xaeroLayer
            );
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

        private Session(MinecraftServer server) {
            this.server = server;
        }
    }
}
