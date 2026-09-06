package com.yungnickyoung.minecraft.ribbits.world.spawn;

import com.yungnickyoung.minecraft.ribbits.RibbitsCommon;
import com.yungnickyoung.minecraft.ribbits.entity.WanderingRibbitEntity;
import com.yungnickyoung.minecraft.ribbits.entity.trade.WanderingRibbitTradeProviders;
import com.yungnickyoung.minecraft.ribbits.module.EntityTypeModule;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Ribbits-owned server scheduler. It never reads or mutates vanilla Wandering Trader state and
 * must be called once from the server tick, not once per dimension.
 */
public final class WanderingRibbitScheduler {
    public static final int FIRST_ATTEMPT_MIN_TICKS = 48_000;
    public static final int FIRST_ATTEMPT_MAX_TICKS = 72_000;
    public static final int SUCCESS_COOLDOWN_MIN_TICKS = 120_000;
    public static final int SUCCESS_COOLDOWN_MAX_TICKS = 168_000;
    public static final int FAILURE_RETRY_TICKS = 1_200;
    public static final int VISIT_LIFETIME_TICKS = 48_000;
    public static final int MIN_RADIUS = 24;
    public static final int MAX_RADIUS = 48;
    public static final int MIN_PLAYER_DISTANCE = 16;
    public static final int CANDIDATES_PER_PLAYER = 16;
    public static final int MAX_CANDIDATES = 64;

    public static final TagKey<Biome> ALLOW_BIOMES = TagKey.create(
            Registries.BIOME, RibbitsCommon.id("allows_wandering_ribbit_spawns"));
    public static final TagKey<Biome> DENY_BIOMES = TagKey.create(
            Registries.BIOME, RibbitsCommon.id("without_wandering_ribbit_spawns"));

    private static final Map<MinecraftServer, Long> LAST_EXECUTED_TICK = new WeakHashMap<>();

    private WanderingRibbitScheduler() {
    }

    /** Idempotent common initialization hook for the integration owner. */
    public static void init() {
        WanderingRibbitTradeProviders.bootstrap();
    }

    /** One call per server tick. A guard makes accidental per-level wiring harmless. */
    public static void tick(MinecraftServer server) {
        init();
        ServerLevel overworld = server.overworld();
        long now = overworld.getGameTime();
        synchronized (LAST_EXECUTED_TICK) {
            Long previous = LAST_EXECUTED_TICK.put(server, now);
            if (previous != null && previous == now) {
                return;
            }
        }

        WanderingRibbitSpawnerData data = data(server);
        cleanupLoadedNaturalistCompanions(server, data);
        RandomSource random = overworld.getRandom();
        if (data.initializeIfNeeded(now, randomInclusive(
                random, FIRST_ATTEMPT_MIN_TICKS, FIRST_ATTEMPT_MAX_TICKS))) {
            return;
        }
        data.observeClock(now);

        if (data.hasActiveLease()) {
            maintainActiveLease(server, data, now);
            if (data.hasActiveLease()) {
                return;
            }
        }
        if (now < data.nextAttemptTime()) {
            return;
        }

        List<ServerPlayer> players = eligiblePlayers(server);
        if (players.isEmpty()) {
            data.scheduleNextAttempt(now, FAILURE_RETRY_TICKS);
            return;
        }

        int start = data.takeFairPlayerStart(players.size());
        SpawnSite site = findSpawnSite(overworld, players, start, random);
        if (site == null || !spawn(overworld, data, site, now, random)) {
            data.scheduleNextAttempt(now, FAILURE_RETRY_TICKS);
            return;
        }
        data.scheduleNextAttempt(now, randomInclusive(
                random, SUCCESS_COOLDOWN_MIN_TICKS, SUCCESS_COOLDOWN_MAX_TICKS));
    }

    /** Entity-side reload check: a stale generation can never survive alongside its successor. */
    public static void validateManagedEntity(ServerLevel level, WanderingRibbitEntity entity) {
        ResourceKey<Level> leaseDimension = entity.getLeaseDimension();
        if (!entity.isSchedulerManaged()
                || leaseDimension == null
                || !leaseDimension.equals(level.dimension())) {
            entity.discard();
            return;
        }

        WanderingRibbitSpawnerData data = data(level.getServer());
        if (!data.ownsLease(entity.getUUID(), level.dimension(), entity.getLeaseGeneration())) {
            entity.discard();
            return;
        }
        entity.synchronizeLeaseExpiry(data.visitExpiry());
        if (level.getServer().overworld().getGameTime() < data.visitExpiry()) {
            return;
        }
        if (entity.hasValidTradingSession()) {
            return;
        }

        data.clearLeaseIfOwned(entity.getUUID(), level.dimension(), entity.getLeaseGeneration());
        entity.discardNaturalistCompanions(level);
        entity.discard();
    }

    /** Releases only the exact active generation; unload removals never call this path. */
    public static void releaseDestroyedLease(ServerLevel level, WanderingRibbitEntity entity) {
        if (!entity.isSchedulerManaged() || entity.getLeaseDimension() == null) {
            return;
        }
        entity.discardNaturalistCompanions(level);
        data(level.getServer()).clearLeaseIfOwned(
                entity.getUUID(), entity.getLeaseDimension(), entity.getLeaseGeneration());
    }

    public static WanderingRibbitSpawnerData data(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(WanderingRibbitSpawnerData.TYPE);
    }

    private static void maintainActiveLease(
            MinecraftServer server,
            WanderingRibbitSpawnerData data,
            long now
    ) {
        ResourceKey<Level> dimension = data.activeDimension().orElse(null);
        java.util.UUID uuid = data.activeEntityUuid().orElse(null);
        if (dimension == null || uuid == null) {
            data.clearLease();
            return;
        }
        ServerLevel level = server.getLevel(dimension);
        if (level == null) {
            data.clearLease();
            return;
        }

        Entity loaded = level.getEntity(uuid); // Null means unloaded; never force-load its chunk.
        if (loaded != null && !(loaded instanceof WanderingRibbitEntity ribbit)) {
            data.clearLease();
            return;
        }
        if (now < data.visitExpiry()) {
            if (loaded instanceof WanderingRibbitEntity ribbit) {
                ribbit.synchronizeLeaseExpiry(data.visitExpiry());
            }
            return;
        }

        if (loaded instanceof WanderingRibbitEntity ribbit && ribbit.hasValidTradingSession()) {
            return;
        }
        data.clearLease();
        if (loaded instanceof WanderingRibbitEntity ribbit) {
            ribbit.discardNaturalistCompanions(level);
            ribbit.discard();
        }
    }

    private static boolean spawn(
            ServerLevel level,
            WanderingRibbitSpawnerData data,
            SpawnSite site,
            long now,
            RandomSource random
    ) {
        WanderingRibbitEntity entity = EntityTypeModule.WANDERING_RIBBIT.get()
                .spawn(level, site.position(), EntitySpawnReason.EVENT);
        if (entity == null) {
            return false;
        }

        long generation = data.nextLeaseGeneration();
        long expiry = saturatedAdd(now, VISIT_LIFETIME_TICKS);
        long tradeSeed = random.nextLong();
        try {
            entity.initializeSchedulerLease(
                    generation, expiry, level.dimension(), site.wanderTarget(), tradeSeed);
            entity.materializeOffers(level);
            entity.initializeNaturalistCompanions(level);
            if (entity.isRemoved()
                    || !entity.isAlive()
                    || level.getEntity(entity.getUUID()) != entity) {
                entity.discard();
                return false;
            }
            data.commitLease(entity.getUUID(), level.dimension(), generation, expiry);
            return true;
        } catch (RuntimeException exception) {
            entity.discard();
            RibbitsCommon.LOGGER.error("Failed to initialize a scheduled Wandering Ribbit", exception);
            return false;
        }
    }

    private static SpawnSite findSpawnSite(
            ServerLevel level,
            List<ServerPlayer> players,
            int start,
            RandomSource random
    ) {
        int playersToTry = Math.min(players.size(), MAX_CANDIDATES / CANDIDATES_PER_PLAYER);
        int attempted = 0;
        for (int offset = 0; offset < playersToTry && attempted < MAX_CANDIDATES; offset++) {
            ServerPlayer selected = players.get((start + offset) % players.size());
            BlockPos target = selected.blockPosition();
            for (int i = 0; i < CANDIDATES_PER_PLAYER && attempted < MAX_CANDIDATES; i++, attempted++) {
                double angle = random.nextDouble() * Math.PI * 2.0D;
                int radius = randomInclusive(random, MIN_RADIUS, MAX_RADIUS);
                int x = target.getX() + (int) Math.round(Math.cos(angle) * radius);
                int z = target.getZ() + (int) Math.round(Math.sin(angle) * radius);
                long deltaX = (long) x - target.getX();
                long deltaZ = (long) z - target.getZ();
                long distanceSquared = deltaX * deltaX + deltaZ * deltaZ;
                if (distanceSquared < (long) MIN_RADIUS * MIN_RADIUS
                        || distanceSquared > (long) MAX_RADIUS * MAX_RADIUS) {
                    continue;
                }
                BlockPos safe = safeSurfacePosition(level, x, z, players);
                if (safe != null) {
                    return new SpawnSite(safe, target);
                }
            }
        }
        return null;
    }

    private static BlockPos safeSurfacePosition(
            ServerLevel level,
            int x,
            int z,
            List<ServerPlayer> eligiblePlayers
    ) {
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
        if (chunk == null || !level.areEntitiesLoaded(ChunkPos.pack(chunkX, chunkZ))) {
            return null;
        }

        int y = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x & 15, z & 15);
        BlockPos feet = new BlockPos(x, y, z);
        if (y <= level.getMinY() + 1 || y >= level.getMaxY() - 1) {
            return null;
        }
        BlockPos groundPos = feet.below();
        BlockState ground = chunk.getBlockState(groundPos);
        BlockState body = chunk.getBlockState(feet);
        BlockState head = chunk.getBlockState(feet.above());
        if (!ground.isFaceSturdy(level, groundPos, Direction.UP)
                || isHazardous(ground)
                || isHazardous(body)
                || isHazardous(head)
                || !ground.getFluidState().isEmpty()
                || !body.getFluidState().isEmpty()
                || !head.getFluidState().isEmpty()) {
            return null;
        }

        Holder<Biome> biome = level.getBiome(feet);
        if (!biomeAllowed(level, biome)) {
            return null;
        }

        AABB spawnBox = EntityTypeModule.WANDERING_RIBBIT.get().getSpawnAABB(
                x + 0.5D, y, z + 0.5D);
        if (!level.getWorldBorder().isWithinBounds(spawnBox) || !level.noCollision(spawnBox)) {
            return null;
        }
        Vec3 center = Vec3.atBottomCenterOf(feet);
        double minimumSquared = (double) MIN_PLAYER_DISTANCE * MIN_PLAYER_DISTANCE;
        for (ServerPlayer player : eligiblePlayers) {
            if (player.distanceToSqr(center) < minimumSquared) {
                return null;
            }
        }
        return feet;
    }

    private static boolean biomeAllowed(ServerLevel level, Holder<Biome> biome) {
        boolean denied = biome.is(DENY_BIOMES)
                || biome.is(BiomeTags.IS_OCEAN)
                || biome.is(BiomeTags.IS_BADLANDS)
                || biome.is(BiomeTags.IS_SAVANNA)
                || biome.is(BiomeTags.IS_HILL)
                || biome.is(BiomeTags.SPAWNS_SNOW_FOXES)
                || biome.unwrapKey().map(ResourceKey::identifier).map(identifier ->
                        identifier.equals(Identifier.withDefaultNamespace("desert"))
                                || identifier.equals(Identifier.withDefaultNamespace("stony_peaks")))
                .orElse(false);
        boolean allowTagExists = level.registryAccess()
                .lookupOrThrow(Registries.BIOME)
                .get(ALLOW_BIOMES)
                .isPresent();
        boolean auditedFamilyMember = biome.is(BiomeTags.IS_FOREST)
                || biome.is(BiomeTags.IS_JUNGLE)
                || biome.is(BiomeTags.IS_TAIGA)
                || biome.is(BiomeTags.IS_RIVER);
        return WanderingRibbitBiomePolicy.allows(
                biome.unwrapKey().map(ResourceKey::identifier).orElse(null),
                denied,
                allowTagExists,
                biome.is(ALLOW_BIOMES),
                auditedFamilyMember);
    }

    private static boolean isHazardous(BlockState state) {
        return state.is(BlockTags.FIRE)
                || state.is(Blocks.CACTUS)
                || state.is(Blocks.MAGMA_BLOCK)
                || state.is(Blocks.POWDER_SNOW)
                || CampfireBlock.isLitCampfire(state);
    }

    private static List<ServerPlayer> eligiblePlayers(MinecraftServer server) {
        ArrayList<ServerPlayer> eligible = new ArrayList<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            GameType mode = player.gameMode();
            if (!player.hasDisconnected()
                    && player.isAlive()
                    && !player.isSpectator()
                    && player.level().dimension().equals(Level.OVERWORLD)
                    && (mode == GameType.SURVIVAL || mode == GameType.ADVENTURE)) {
                eligible.add(player);
            }
        }
        eligible.sort(Comparator.comparing(player -> player.getUUID().toString()));
        return List.copyOf(eligible);
    }

    private static int randomInclusive(RandomSource random, int minimum, int maximum) {
        return minimum + random.nextInt(maximum - minimum + 1);
    }

    private static void cleanupLoadedNaturalistCompanions(MinecraftServer server,
                                                           WanderingRibbitSpawnerData data) {
        java.util.UUID activeMerchant = data.activeEntityUuid().orElse(null);
        for (ServerLevel level : server.getAllLevels()) {
            NaturalistSnailCompanions.discardLoadedStaleCompanions(level, activeMerchant);
        }
    }

    private static long saturatedAdd(long left, long right) {
        return left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
    }

    private record SpawnSite(BlockPos position, BlockPos wanderTarget) {
        private SpawnSite {
            position = position.immutable();
            wanderTarget = wanderTarget.immutable();
        }
    }
}
