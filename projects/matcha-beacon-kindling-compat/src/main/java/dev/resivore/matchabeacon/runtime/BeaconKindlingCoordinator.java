package dev.resivore.matchabeacon.runtime;

import dev.resivore.matchabeacon.MatchaBeaconKindlingCompat;
import dev.resivore.matchabeacon.state.BeaconKindlingSavedData;
import dev.resivore.matchabeacon.state.LifecycleDecision;
import dev.resivore.matchabeacon.state.LifecycleObservation;
import dev.resivore.matchabeacon.state.LifecycleStep;
import dev.resivore.matchabeacon.state.StaleLockPolicy;
import dev.resivore.matchabeacon.state.SummonLifecycle;
import dev.resivore.matchabeacon.state.SummonPhase;
import dev.resivore.matchabeacon.state.SummonRecord;
import net.fabricmc.fabric.api.event.lifecycle.v1.EntityLoadData;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.functions.CommandFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerFunctionManager;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ScoreAccess;
import net.minecraft.world.scores.ScoreHolder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Exact-owner, server-global lifecycle coordinator for Matcha's Beacon Kindling marker.
 *
 * <p>No operation in this class discovers an owner, marker, beacon, or trader with a nearest
 * selector. Placement captures the exact entity created by the exact player's spawn-egg call;
 * every later operation resolves persisted UUIDs in the persisted dimension.</p>
 */
public final class BeaconKindlingCoordinator {
    private static final Map<MinecraftServer, ServerCoordinator> COORDINATORS =
            Collections.synchronizedMap(new IdentityHashMap<>());
    private static final ThreadLocal<PlacementCapture> ACTIVE_PLACEMENT = new ThreadLocal<>();
    private static final ThreadLocal<TraderCapture> ACTIVE_TRADER_SUMMON = new ThreadLocal<>();
    private static boolean installed;

    private BeaconKindlingCoordinator() {
    }

    public static synchronized void install() {
        if (installed) {
            return;
        }
        installed = true;

        ServerEntityEvents.ENTITY_LOAD.register(BeaconKindlingCoordinator::onEntityLoad);
        ServerTickEvents.END_SERVER_TICK.register(server -> forServer(server).tick());
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                forServer(server).onJoin(handler.getPlayer()));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            synchronized (COORDINATORS) {
                COORDINATORS.remove(server);
            }
        });
    }

    public static void beginSpawnEggUse(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        if (!(context.getPlayer() instanceof ServerPlayer player)
                || SpawnEggItem.getType(stack) != EntityTypes.MARKER
                || !MatchaContract.ITEM_MODEL.equals(stack.get(DataComponents.ITEM_MODEL))) {
            return;
        }

        if (ACTIVE_PLACEMENT.get() != null) {
            MatchaBeaconKindlingCompat.LOGGER.error(
                    "Discarding an unexpected nested Beacon Kindling placement capture");
            ACTIVE_PLACEMENT.remove();
        }
        ACTIVE_PLACEMENT.set(new PlacementCapture(player));
    }

    public static void endSpawnEggUse(UseOnContext context) {
        PlacementCapture capture = ACTIVE_PLACEMENT.get();
        if (capture == null) {
            return;
        }
        try {
            if (context.getPlayer() instanceof ServerPlayer player
                    && capture.playerId.equals(player.getUUID())
                    && capture.server == player.level().getServer()
                    && capture.marker != null) {
                forServer(capture.server).rememberCapturedMarker(capture.playerId, capture.marker);
            }
        } finally {
            ACTIVE_PLACEMENT.remove();
        }
    }

    public static void handlePlacementFunction(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            forServer(player.level().getServer()).handlePlacement(player);
        } else {
            MatchaBeaconKindlingCompat.LOGGER.warn(
                    "Suppressed Beacon Kindling placement function without a player source");
        }
    }

    private static void onEntityLoad(Entity entity, ServerLevel level) {
        PlacementCapture placement = ACTIVE_PLACEMENT.get();
        if (placement != null
                && placement.marker == null
                && placement.server == level.getServer()
                && placement.levelKey.equals(level.dimension())
                && entity instanceof Marker
                && entity.getType() == EntityTypes.MARKER
                && entity.entityTags().contains(MatchaContract.MARKER_TAG)) {
            EntityLoadData loadData = (EntityLoadData) entity;
            if (!loadData.isLoadedFromDisk()
                    && loadData.spawnReason() == EntitySpawnReason.SPAWN_ITEM_USE) {
                placement.marker = new CapturedMarker(
                        entity.getUUID(), level.dimension(), entity.blockPosition());
            }
        }

        TraderCapture traderCapture = ACTIVE_TRADER_SUMMON.get();
        if (traderCapture != null
                && traderCapture.traderId == null
                && traderCapture.server == level.getServer()
                && traderCapture.levelKey.equals(level.dimension())
                && entity instanceof WanderingTrader
                && entity.getType() == EntityTypes.WANDERING_TRADER
                && entity.entityTags().contains(MatchaContract.TRADER_TAG)) {
            EntityLoadData loadData = (EntityLoadData) entity;
            if (!loadData.isLoadedFromDisk()
                    && loadData.spawnReason() == EntitySpawnReason.COMMAND) {
                entity.addTag(MatchaContract.OWNED_TRADER_TAG);
                traderCapture.traderId = entity.getUUID();
            }
        }

        forServer(level.getServer()).queueRelevantEntity(entity, level);
    }

    private static ServerCoordinator forServer(MinecraftServer server) {
        synchronized (COORDINATORS) {
            return COORDINATORS.computeIfAbsent(server, ServerCoordinator::new);
        }
    }

    private static final class ServerCoordinator {
        private final MinecraftServer server;
        private final Map<UUID, CapturedMarker> capturedMarkers = new HashMap<>();
        private final Map<UUID, LoadedEntityRef> queuedEntities = new LinkedHashMap<>();

        private ServerCoordinator(MinecraftServer server) {
            this.server = server;
        }

        private BeaconKindlingSavedData data() {
            return server.getDataStorage().computeIfAbsent(BeaconKindlingSavedData.TYPE);
        }

        private void rememberCapturedMarker(UUID playerId, CapturedMarker marker) {
            capturedMarkers.put(playerId, marker);
        }

        private void queueRelevantEntity(Entity entity, ServerLevel level) {
            LoadedEntityKind kind;
            if (entity instanceof Marker
                    && entity.entityTags().contains(MatchaContract.MARKER_TAG)) {
                kind = LoadedEntityKind.MARKER;
            } else if (entity instanceof WanderingTrader
                    && entity.entityTags().contains(MatchaContract.TRADER_TAG)) {
                kind = LoadedEntityKind.TRADER;
            } else {
                return;
            }
            queuedEntities.put(entity.getUUID(), new LoadedEntityRef(entity.getUUID(), level.dimension(), kind));
        }

        private void onJoin(ServerPlayer player) {
            Optional<SummonRecord> tracked = data().get(player.getUUID());
            if (tracked.isPresent() && server.getLevel(tracked.orElseThrow().dimension()) == null) {
                data().remove(player.getUUID());
                clearPlayerCompatibility(player);
                return;
            }

            boolean hasMatchaLock = player.entityTags().contains(MatchaContract.PLAYER_LOCK_TAG);
            if (StaleLockPolicy.shouldClearMatchaLock(hasMatchaLock, tracked)) {
                player.removeTag(MatchaContract.PLAYER_LOCK_TAG);
                resetTimerScore(player);
                MatchaBeaconKindlingCompat.LOGGER.info(
                        "Cleared stale pre-patch Beacon Kindling lock for {} ({})",
                        player.getScoreboardName(), player.getUUID());
                return;
            }
            tracked.ifPresent(record -> reconcileCompatibility(record, player));
        }

        private void handlePlacement(ServerPlayer player) {
            CapturedMarker captured = capturedMarkers.remove(player.getUUID());
            revokePlacementAdvancement(player);
            if (captured == null) {
                MatchaBeaconKindlingCompat.LOGGER.error(
                        "Suppressed Beacon Kindling placement for {} because no exact marker was captured",
                        player.getScoreboardName());
                return;
            }

            ServerLevel level = server.getLevel(captured.dimension);
            Entity capturedEntity = level == null ? null : level.getEntity(captured.markerId);
            if (!(capturedEntity instanceof Marker marker)
                    || !marker.entityTags().contains(MatchaContract.MARKER_TAG)
                    || !marker.blockPosition().equals(captured.beaconPos)) {
                MatchaBeaconKindlingCompat.LOGGER.error(
                        "Suppressed Beacon Kindling placement for {} because captured marker {} no longer matches",
                        player.getScoreboardName(), captured.markerId);
                return;
            }

            createBeaconEffects(level, marker);
            BeaconKindlingSavedData saved = data();
            Optional<SummonRecord> existing = saved.get(player.getUUID());
            if (existing.isPresent()) {
                reconcileCompatibility(existing.orElseThrow(), player);
                player.sendSystemMessage(gray(MatchaContract.DUPLICATE_MESSAGE));
                resetTimerScore(marker);
                marker.discard();
                return;
            }

            if (player.entityTags().contains(MatchaContract.PLAYER_LOCK_TAG)) {
                player.removeTag(MatchaContract.PLAYER_LOCK_TAG);
                resetTimerScore(player);
            }

            SummonRecord record = SummonRecord.approach(
                    player.getUUID(), marker.getUUID(), level.dimension(), captured.beaconPos);
            try {
                saved.put(record);
            } catch (IllegalArgumentException exception) {
                MatchaBeaconKindlingCompat.LOGGER.error(
                        "Rejected conflicting exact Beacon Kindling ownership for {}",
                        player.getScoreboardName(), exception);
                resetTimerScore(marker);
                marker.discard();
                return;
            }

            marker.addTag(MatchaContract.OWNED_MARKER_TAG);
            reconcileCompatibility(record, player);
            server.getPlayerList().broadcastSystemMessage(gray(MatchaContract.APPROACH_MESSAGE), false);
        }

        private void tick() {
            neutralizeQueuedLegacyEntities();
            BeaconKindlingSavedData saved = data();
            for (SummonRecord record : new ArrayList<>(saved.snapshot().values())) {
                tickRecord(saved, record);
            }
            capturedMarkers.clear();
        }

        private void tickRecord(BeaconKindlingSavedData saved, SummonRecord record) {
            ServerPlayer player = server.getPlayerList().getPlayer(record.playerId());
            if (player == null || player.hasDisconnected()) {
                return;
            }
            reconcileCompatibility(record, player);

            ServerLevel level = server.getLevel(record.dimension());
            if (level == null) {
                terminalCleanup(saved, record, player, LifecycleDecision.CANCEL);
                return;
            }

            int chunkX = record.beaconPos().getX() >> 4;
            int chunkZ = record.beaconPos().getZ() >> 4;
            LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
            if (chunk == null || !level.areEntitiesLoaded(ChunkPos.pack(chunkX, chunkZ))) {
                return;
            }

            BlockState beaconState = chunk.getBlockState(record.beaconPos());
            boolean campfireValid = beaconState.is(Blocks.CAMPFIRE)
                    && beaconState.getValue(CampfireBlock.LIT);
            Entity markerEntity = level.getEntity(record.markerId());
            boolean markerPresent = markerEntity instanceof Marker
                    && markerEntity.blockPosition().equals(record.beaconPos())
                    && markerEntity.entityTags().contains(MatchaContract.MARKER_TAG);
            boolean traderPresent = record.traderId()
                    .map(level::getEntity)
                    .filter(WanderingTrader.class::isInstance)
                    .isPresent();

            LifecycleStep step = SummonLifecycle.advance(record, new LifecycleObservation(
                    true, true, campfireValid, markerPresent, traderPresent));
            switch (step.decision()) {
                case PAUSE -> { }
                case ADVANCE -> {
                    SummonRecord advanced = step.nextRecord().orElseThrow();
                    saved.put(advanced);
                    reconcileCompatibility(advanced, player);
                }
                case ARRIVE, WAITING_FOR_VISIT -> {
                    SummonRecord arrived = step.nextRecord().orElseThrow();
                    Optional<UUID> traderId = summonExactMatchaTrader(level, markerEntity, arrived);
                    if (traderId.isEmpty()) {
                        terminalCleanup(saved, arrived, player, LifecycleDecision.CANCEL);
                        return;
                    }
                    SummonRecord visiting = SummonLifecycle.beginVisit(arrived, traderId.orElseThrow());
                    saved.put(visiting);
                    reconcileCompatibility(visiting, player);
                }
                case CANCEL, DEPART -> terminalCleanup(saved, record, player, step.decision());
            }
        }

        private Optional<UUID> summonExactMatchaTrader(
                ServerLevel level,
                Entity markerEntity,
                SummonRecord record
        ) {
            if (!(markerEntity instanceof Marker marker)
                    || !marker.getUUID().equals(record.markerId())) {
                return Optional.empty();
            }
            ServerFunctionManager functions = server.getFunctions();
            Optional<CommandFunction<CommandSourceStack>> function = functions.get(MatchaContract.SUMMON_FUNCTION);
            if (function.isEmpty()) {
                MatchaBeaconKindlingCompat.LOGGER.error(
                        "Matcha summon function {} is unavailable", MatchaContract.SUMMON_FUNCTION);
                return Optional.empty();
            }

            TraderCapture capture = new TraderCapture(server, level.dimension(), marker.getUUID());
            if (ACTIVE_TRADER_SUMMON.get() != null) {
                throw new IllegalStateException("Nested Beacon Kindling trader capture is not supported");
            }
            ACTIVE_TRADER_SUMMON.set(capture);
            try {
                FunctionExecutionGuard.withPermittedSummon(marker.getUUID(), () -> {
                    CommandSourceStack source = functions.getGameLoopSender()
                            .withLevel(level)
                            .withPosition(marker.position())
                            .withEntity(marker);
                    functions.execute(function.orElseThrow(), source);
                    return null;
                });
            } catch (RuntimeException exception) {
                MatchaBeaconKindlingCompat.LOGGER.error(
                        "Matcha Beacon Kindling summon failed for player {} marker {}",
                        record.playerId(), record.markerId(), exception);
                return Optional.empty();
            } finally {
                ACTIVE_TRADER_SUMMON.remove();
            }

            if (capture.traderId == null) {
                MatchaBeaconKindlingCompat.LOGGER.error(
                        "Matcha summon function returned without an exact trader capture for marker {}",
                        marker.getUUID());
                return Optional.empty();
            }
            Entity captured = level.getEntity(capture.traderId);
            if (!(captured instanceof WanderingTrader)
                    || !captured.entityTags().contains(MatchaContract.TRADER_TAG)) {
                return Optional.empty();
            }
            captured.addTag(MatchaContract.OWNED_TRADER_TAG);
            return Optional.of(capture.traderId);
        }

        private void terminalCleanup(
                BeaconKindlingSavedData saved,
                SummonRecord record,
                ServerPlayer player,
                LifecycleDecision decision
        ) {
            saved.remove(record.playerId());
            ServerLevel level = server.getLevel(record.dimension());
            Entity trader = level == null
                    ? null
                    : record.traderId().map(level::getEntity).orElse(null);
            Entity marker = level == null ? null : level.getEntity(record.markerId());

            if (decision == LifecycleDecision.DEPART && level != null && trader instanceof WanderingTrader) {
                level.sendParticles(
                        ParticleTypes.POOF,
                        trader.getX(), trader.getY() + 0.5D, trader.getZ(),
                        50, 0.2D, 1.0D, 0.2D, 0.0D);
            }
            if (trader instanceof WanderingTrader) {
                trader.discard();
            }

            if (level != null) {
                extinguishExactCampfire(level, record.beaconPos());
                if (decision == LifecycleDecision.DEPART) {
                    level.sendParticles(
                            ParticleTypes.LARGE_SMOKE,
                            record.beaconPos().getX() + 0.5D,
                            record.beaconPos().getY() + 0.5D,
                            record.beaconPos().getZ() + 0.5D,
                            10, 0.1D, 0.1D, 0.1D, 0.1D);
                }
            }
            if (marker instanceof Marker
                    && marker.blockPosition().equals(record.beaconPos())) {
                resetTimerScore(marker);
                marker.discard();
            }

            clearPlayerCompatibility(player);
            if (decision == LifecycleDecision.DEPART) {
                server.getPlayerList().broadcastSystemMessage(gray(MatchaContract.LEFT_MESSAGE), false);
            } else {
                player.sendSystemMessage(gray(MatchaContract.LOST_MESSAGE));
            }
        }

        private void neutralizeQueuedLegacyEntities() {
            List<LoadedEntityRef> queued = new ArrayList<>(queuedEntities.values());
            queuedEntities.clear();
            BeaconKindlingSavedData saved = data();
            for (LoadedEntityRef ref : queued) {
                ServerLevel level = server.getLevel(ref.dimension);
                Entity entity = level == null ? null : level.getEntity(ref.entityId);
                if (entity == null) {
                    continue;
                }
                if (ref.kind == LoadedEntityKind.MARKER) {
                    Optional<SummonRecord> owner = saved.findByMarker(ref.entityId);
                    boolean exactOwned = owner
                            .filter(record -> record.dimension().equals(ref.dimension))
                            .filter(record -> record.beaconPos().equals(entity.blockPosition()))
                            .isPresent();
                    if (exactOwned) {
                        entity.addTag(MatchaContract.OWNED_MARKER_TAG);
                    } else if (entity instanceof Marker
                            && entity.entityTags().contains(MatchaContract.MARKER_TAG)) {
                        resetTimerScore(entity);
                        entity.discard();
                        MatchaBeaconKindlingCompat.LOGGER.info(
                                "Neutralized legacy unowned Beacon Kindling marker {} in {}",
                                ref.entityId, ref.dimension.identifier());
                    }
                } else {
                    Optional<SummonRecord> owner = saved.findByTrader(ref.entityId);
                    if (owner.filter(record -> record.dimension().equals(ref.dimension)).isPresent()) {
                        entity.addTag(MatchaContract.OWNED_TRADER_TAG);
                    } else if (entity instanceof WanderingTrader
                            && entity.entityTags().contains(MatchaContract.TRADER_TAG)) {
                        entity.discard();
                        MatchaBeaconKindlingCompat.LOGGER.info(
                                "Neutralized legacy unowned Beacon Kindling trader {} in {}",
                                ref.entityId, ref.dimension.identifier());
                    }
                }
            }
        }

        private void reconcileCompatibility(SummonRecord record, ServerPlayer player) {
            player.addTag(MatchaContract.PLAYER_LOCK_TAG);
            int score = timerScore(record);
            setTimerScore(player, score);
            ServerLevel level = server.getLevel(record.dimension());
            Entity marker = level == null ? null : level.getEntity(record.markerId());
            if (marker instanceof Marker) {
                marker.addTag(MatchaContract.MARKER_TAG);
                marker.addTag(MatchaContract.OWNED_MARKER_TAG);
                setTimerScore(marker, score);
            }
            if (record.phase() == SummonPhase.VISIT) {
                Entity trader = level == null
                        ? null
                        : record.traderId().map(level::getEntity).orElse(null);
                if (trader instanceof WanderingTrader) {
                    trader.addTag(MatchaContract.TRADER_TAG);
                    trader.addTag(MatchaContract.OWNED_TRADER_TAG);
                }
            }
        }

        private int timerScore(SummonRecord record) {
            if (record.phase() == SummonPhase.APPROACH) {
                return (SummonRecord.APPROACH_TICKS - record.approachTicksRemaining()) / 200;
            }
            return 60 + (SummonRecord.VISIT_TICKS - record.visitTicksRemaining()) / 200;
        }

        private void clearPlayerCompatibility(ServerPlayer player) {
            player.removeTag(MatchaContract.PLAYER_LOCK_TAG);
            resetTimerScore(player);
        }

        private void setTimerScore(ScoreHolder holder, int value) {
            ServerScoreboard scoreboard = server.getScoreboard();
            Objective objective = scoreboard.getObjective(MatchaContract.TIMER_OBJECTIVE);
            if (objective == null) {
                return;
            }
            ScoreAccess score = scoreboard.getOrCreatePlayerScore(holder, objective);
            if (score.get() != value) {
                score.set(value);
            }
        }

        private void resetTimerScore(ScoreHolder holder) {
            Objective objective = server.getScoreboard().getObjective(MatchaContract.TIMER_OBJECTIVE);
            if (objective != null) {
                server.getScoreboard().resetSinglePlayerScore(holder, objective);
            }
        }

        private void revokePlacementAdvancement(ServerPlayer player) {
            AdvancementHolder advancement = server.getAdvancements().get(MatchaContract.ADVANCEMENT);
            if (advancement != null) {
                player.getAdvancements().revoke(advancement, "use_item");
            }
        }

        private void createBeaconEffects(ServerLevel level, Marker marker) {
            BlockState campfire = Blocks.CAMPFIRE.defaultBlockState()
                    .setValue(CampfireBlock.SIGNAL_FIRE, true);
            level.setBlockAndUpdate(marker.blockPosition(), campfire);
            level.sendParticles(
                    ParticleTypes.FLAME,
                    marker.getX(), marker.getY() + 0.7D, marker.getZ(),
                    30, 0.1D, 0.1D, 0.1D, 0.07D);
            level.playSound(
                    null, marker.blockPosition(), SoundEvents.FIRECHARGE_USE,
                    SoundSource.BLOCKS, 1.0F, 1.0F);
            level.playSound(
                    null, marker.blockPosition(), SoundEvents.WITHER_SPAWN,
                    SoundSource.BLOCKS, 0.5F, 1.0F);
        }

        private void extinguishExactCampfire(ServerLevel level, BlockPos pos) {
            LevelChunk chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
            if (chunk == null) {
                return;
            }
            BlockState state = chunk.getBlockState(pos);
            if (state.is(Blocks.CAMPFIRE) && state.getValue(CampfireBlock.LIT)) {
                level.setBlockAndUpdate(pos, state.setValue(CampfireBlock.LIT, false));
            }
        }
    }

    private static Component gray(String message) {
        return Component.literal(message).withStyle(ChatFormatting.GRAY);
    }

    private static final class PlacementCapture {
        private final MinecraftServer server;
        private final UUID playerId;
        private final ResourceKey<Level> levelKey;
        private CapturedMarker marker;

        private PlacementCapture(ServerPlayer player) {
            this.server = Objects.requireNonNull(player.level().getServer());
            this.playerId = player.getUUID();
            this.levelKey = player.level().dimension();
        }
    }

    private static final class TraderCapture {
        private final MinecraftServer server;
        private final ResourceKey<Level> levelKey;
        @SuppressWarnings("unused")
        private final UUID markerId;
        private UUID traderId;

        private TraderCapture(MinecraftServer server, ResourceKey<Level> levelKey, UUID markerId) {
            this.server = server;
            this.levelKey = levelKey;
            this.markerId = markerId;
        }
    }

    private record CapturedMarker(UUID markerId, ResourceKey<Level> dimension, BlockPos beaconPos) {
        private CapturedMarker {
            Objects.requireNonNull(markerId, "markerId");
            Objects.requireNonNull(dimension, "dimension");
            beaconPos = Objects.requireNonNull(beaconPos, "beaconPos").immutable();
        }
    }

    private enum LoadedEntityKind {
        MARKER,
        TRADER
    }

    private record LoadedEntityRef(
            UUID entityId,
            ResourceKey<Level> dimension,
            LoadedEntityKind kind
    ) {
    }
}
