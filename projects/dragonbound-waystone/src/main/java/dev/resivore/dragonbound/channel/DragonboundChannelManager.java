package dev.resivore.dragonbound.channel;

import dev.resivore.dragonbound.DragonboundContent;
import dev.resivore.dragonbound.anchor.AnchorBinding;
import dev.resivore.dragonbound.anchor.DragonboundAnchors;
import dev.resivore.dragonbound.config.DragonboundConfig;
import dev.resivore.dragonbound.config.DragonboundConfigManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;

public final class DragonboundChannelManager {
    private static final double ARRIVAL_EPSILON_SQUARED = 1.0E-8D;
    private static final Map<MinecraftServer, DragonboundChannelManager> MANAGERS =
            Collections.synchronizedMap(new IdentityHashMap<>());
    private static boolean installed;

    private final MinecraftServer server;
    private final PendingChannelStore pending = new PendingChannelStore();
    private long nextToken = 1L;

    private DragonboundChannelManager(MinecraftServer server) {
        this.server = server;
    }

    public static synchronized void install() {
        if (installed) {
            return;
        }
        installed = true;
        ServerTickEvents.END_SERVER_TICK.register(server -> forServer(server).tick());
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            DragonboundChannelManager manager = existing(server);
            if (manager != null) {
                manager.cancelPlayer(handler.getPlayer(), ChannelCancellation.DISCONNECT);
            }
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            DragonboundChannelManager manager = existing(server);
            if (manager != null) {
                manager.shutdown();
            }
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> MANAGERS.remove(server));
    }

    public static boolean start(ServerPlayer player, ItemStack stack, ReturnSource source) {
        return forServer(player.level().getServer()).startChannel(player, stack, source);
    }

    public static void onAcceptedDamage(ServerPlayer player) {
        DragonboundChannelManager manager = existing(player.level().getServer());
        if (manager != null && ChannelRules.acceptedDamageCancels(
                DragonboundConfigManager.get().cancelOnDamage(), true)) {
            manager.cancelPlayer(player, ChannelCancellation.DAMAGE);
        }
    }

    public static void onDeath(ServerPlayer player) {
        DragonboundChannelManager manager = existing(player.level().getServer());
        if (manager != null) {
            manager.cancelPlayer(player, ChannelCancellation.DEATH);
        }
    }

    static DragonboundChannelManager forServer(MinecraftServer server) {
        synchronized (MANAGERS) {
            return MANAGERS.computeIfAbsent(server, DragonboundChannelManager::new);
        }
    }

    private static DragonboundChannelManager existing(MinecraftServer server) {
        synchronized (MANAGERS) {
            return MANAGERS.get(server);
        }
    }

    private boolean startChannel(ServerPlayer player, ItemStack stack, ReturnSource source) {
        if (pending.get(player.getUUID()).isPresent()) {
            feedback(player, ChannelCancellation.ALREADY_CHANNELING);
            return false;
        }
        if (!isExpectedItem(stack, source)
                || !ChannelRules.stackSemanticallyMatches(player.getMainHandItem(), stack)) {
            return false;
        }

        DragonboundConfig config = DragonboundConfigManager.get();
        Optional<ChannelCancellation> restriction = unsafeState(player, config);
        if (restriction.isPresent()) {
            feedback(player, restriction.get());
            return false;
        }

        Optional<AnchorBinding> anchorOptional = DragonboundAnchors.active(server);
        if (anchorOptional.isEmpty()) {
            feedback(player, ChannelCancellation.NO_WAYSTONE);
            return false;
        }
        AnchorBinding anchor = anchorOptional.get();
        if (!config.allowCrossDimension() && !player.level().dimension().equals(anchor.dimension())) {
            feedback(player, ChannelCancellation.CROSS_DIMENSION_DISABLED);
            return false;
        }
        if (source == ReturnSource.DRAGONBOUND_STAFF && player.getCooldowns().isOnCooldown(stack)) {
            feedback(player, ChannelCancellation.STAFF_COOLDOWN);
            return false;
        }
        if (!liveAnchorBlockExists(anchor)) {
            feedback(player, ChannelCancellation.NO_WAYSTONE);
            return false;
        }

        long now = server.overworld().getGameTime();
        long token = nextToken++;
        if (nextToken <= 0L) {
            nextToken = 1L;
        }
        PendingChannel channel = new PendingChannel(
                player.getUUID(),
                token,
                player,
                player.level().dimension(),
                player.position(),
                now,
                now + config.channelTicks(),
                player.getInventory().getSelectedSlot(),
                stack.copy(),
                source,
                anchor,
                config.staffCooldownTicks()
        );
        if (!pending.add(channel)) {
            feedback(player, ChannelCancellation.ALREADY_CHANNELING);
            return false;
        }

        ChannelEffects.channelStarted(player, config.channelTicks());
        if (source == ReturnSource.DRAGONBOUND_STAFF) {
            player.swing(InteractionHand.MAIN_HAND, true);
        }
        player.sendOverlayMessage(Component.translatable("message.dragonbound_waystone.channel_started"));
        return true;
    }

    private void tick() {
        DragonboundConfig config = DragonboundConfigManager.get();
        for (PendingChannel channel : pending.snapshot()) {
            if (!pending.isCurrent(channel.playerId(), channel.token())) {
                continue;
            }
            ServerPlayer player = server.getPlayerList().getPlayer(channel.playerId());
            if (player == null || player.hasDisconnected()) {
                cancel(channel, ChannelCancellation.DISCONNECT);
                continue;
            }
            if (player != channel.playerInstance()) {
                cancel(channel, ChannelCancellation.DISCONNECT);
                continue;
            }
            if (!player.isAlive()) {
                cancel(channel, ChannelCancellation.DEATH);
                continue;
            }
            if (!player.level().dimension().equals(channel.startDimension())) {
                cancel(channel, ChannelCancellation.DIMENSION_CHANGED);
                continue;
            }
            if (!sourceItemIsStillValid(player, channel)) {
                cancel(channel, ChannelCancellation.HELD_ITEM);
                continue;
            }
            if (!DragonboundAnchors.matches(server, channel.anchor())) {
                cancel(channel, ChannelCancellation.ANCHOR_CHANGED);
                continue;
            }
            Optional<ChannelCancellation> restriction = unsafeState(player, config);
            if (restriction.isPresent()) {
                cancel(channel, restriction.get());
                continue;
            }
            Vec3 current = player.position();
            Vec3 start = channel.startPosition();
            if (config.cancelOnMovement() && ChannelRules.exceedsMovementTolerance(
                    start.x, start.y, start.z,
                    current.x, current.y, current.z,
                    config.movementTolerance())) {
                cancel(channel, ChannelCancellation.MOVEMENT);
                continue;
            }
            long gameTime = server.overworld().getGameTime();
            if (ChannelRules.completionTickReached(gameTime, channel.completionTick())) {
                complete(channel);
                continue;
            }
            ChannelEffects.channelTick(player, gameTime - channel.startTick(), config.channelTicks());
        }
    }

    private void complete(PendingChannel channel) {
        if (!pending.isCurrent(channel.playerId(), channel.token())) {
            return;
        }
        ServerPlayer player = channel.playerInstance();
        if (!DragonboundAnchors.matches(server, channel.anchor())) {
            cancel(channel, ChannelCancellation.ANCHOR_CHANGED);
            return;
        }

        ServerLevel destination = server.getLevel(channel.anchor().dimension());
        if (destination == null) {
            cancel(channel, ChannelCancellation.DESTINATION_UNAVAILABLE);
            return;
        }
        try {
            destination.getChunk(
                    channel.anchor().pos().getX() >> 4,
                    channel.anchor().pos().getZ() >> 4,
                    ChunkStatus.FULL,
                    true
            );
        } catch (RuntimeException exception) {
            cancel(channel, ChannelCancellation.DESTINATION_UNAVAILABLE);
            return;
        }

        boolean blockMatches = destination.getBlockState(channel.anchor().pos()).is(DragonboundContent.WAYSTONE);
        Vec3 target = DestinationRules.exactTopCenter(channel.anchor().pos());
        Vec3 boxOffset = target.subtract(player.position());
        AABB targetBox = player.getBoundingBox().move(boxOffset);
        boolean insideBuildHeight = destination.isInWorldBounds(channel.anchor().pos())
                && destination.isInsideBuildHeight(Mth.floor(targetBox.minY))
                && destination.isInsideBuildHeight(Mth.floor(targetBox.maxY - 1.0E-7D));
        boolean insideBorder = destination.getWorldBorder().isWithinBounds(targetBox);
        boolean collisionFree = destination.noCollision(player, targetBox)
                && !containsLava(destination, targetBox);
        boolean safe = DestinationRules.exactDestinationIsSafe(
                DragonboundAnchors.matches(server, channel.anchor()),
                blockMatches,
                insideBuildHeight,
                insideBorder,
                collisionFree
        );
        if (!safe) {
            cancel(channel, blockMatches
                    ? ChannelCancellation.DESTINATION_OBSTRUCTED
                    : ChannelCancellation.ANCHOR_CHANGED);
            return;
        }

        if (!sourceItemIsStillValid(player, channel)) {
            cancel(channel, ChannelCancellation.HELD_ITEM);
            return;
        }
        if (!terminalCleanup(channel, null, null, true)) {
            return;
        }

        ServerPlayer arrived;
        try {
            arrived = player.teleport(new TeleportTransition(
                    destination,
                    target,
                    Vec3.ZERO,
                    player.getYRot(),
                    player.getXRot(),
                    TeleportTransition.DO_NOTHING
            ));
        } catch (RuntimeException exception) {
            terminalCleanup(channel, player, ChannelCancellation.TELEPORT_FAILED, false);
            return;
        }

        boolean confirmed = arrived != null
                && arrived.getUUID().equals(channel.playerId())
                && arrived.level() == destination
                && arrived.position().distanceToSqr(target) <= ARRIVAL_EPSILON_SQUARED
                && destination.getWorldBorder().isWithinBounds(arrived.getBoundingBox())
                && destination.noCollision(arrived, arrived.getBoundingBox());
        if (!ChannelRules.shouldApplySuccessEffect(confirmed)) {
            terminalCleanup(
                    channel,
                    arrived == null ? player : arrived,
                    ChannelCancellation.TELEPORT_FAILED,
                    false);
            return;
        }

        ItemStack arrivedHeld = arrived.getMainHandItem();
        boolean heldItemConfirmed = arrived.getInventory().getSelectedSlot() == channel.selectedSlot()
                && isExpectedItem(arrivedHeld, channel.source())
                && ChannelRules.stackSemanticallyMatches(arrivedHeld, channel.heldSnapshot());
        if (!heldItemConfirmed) {
            terminalCleanup(channel, arrived, ChannelCancellation.TELEPORT_FAILED, false);
            return;
        }

        ChannelEffects.successfulTeleport(arrived,
                isCrossDimensionReturn(channel.startDimension(), channel.anchor().dimension()));
        if (channel.source() == ReturnSource.IMBUED_VOID_PEARL) {
            arrivedHeld.shrink(1);
        } else {
            arrived.getCooldowns().addCooldown(arrivedHeld, channel.staffCooldownTicks());
        }
        arrived.sendOverlayMessage(Component.translatable("message.dragonbound_waystone.teleport_success"));
    }

    private boolean liveAnchorBlockExists(AnchorBinding anchor) {
        ServerLevel level = server.getLevel(anchor.dimension());
        if (level == null) {
            return false;
        }
        try {
            level.getChunk(anchor.pos().getX() >> 4, anchor.pos().getZ() >> 4, ChunkStatus.FULL, true);
        } catch (RuntimeException exception) {
            return false;
        }
        if (level.getBlockState(anchor.pos()).is(DragonboundContent.WAYSTONE)) {
            return true;
        }
        DragonboundAnchors.clearIfMatching(level, anchor.pos());
        return false;
    }

    private static boolean sourceItemIsStillValid(ServerPlayer player, PendingChannel channel) {
        ItemStack liveMainHand = player.getMainHandItem();
        return isExpectedItem(liveMainHand, channel.source())
                && ChannelRules.sourceItemIsStillValid(
                        channel.selectedSlot(),
                        player.getInventory().getSelectedSlot(),
                        liveMainHand,
                        channel.heldSnapshot());
    }

    private static boolean isExpectedItem(ItemStack stack, ReturnSource source) {
        return source == ReturnSource.IMBUED_VOID_PEARL
                ? stack.is(DragonboundContent.IMBUED_VOID_PEARL)
                : stack.is(DragonboundContent.DRAGONBOUND_STAFF);
    }

    static boolean isCrossDimensionReturn(
            ResourceKey<Level> startDimension,
            ResourceKey<Level> destinationDimension) {
        return !startDimension.equals(destinationDimension);
    }

    private static boolean containsLava(ServerLevel level, AABB box) {
        int minX = Mth.floor(box.minX + 1.0E-7D);
        int minY = Mth.floor(box.minY + 1.0E-7D);
        int minZ = Mth.floor(box.minZ + 1.0E-7D);
        int maxX = Mth.floor(box.maxX - 1.0E-7D);
        int maxY = Mth.floor(box.maxY - 1.0E-7D);
        int maxZ = Mth.floor(box.maxZ - 1.0E-7D);
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (level.getFluidState(new net.minecraft.core.BlockPos(x, y, z)).is(FluidTags.LAVA)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static Optional<ChannelCancellation> unsafeState(ServerPlayer player, DragonboundConfig config) {
        if (config.restrictMounted() && player.isPassenger()) {
            return Optional.of(ChannelCancellation.MOUNTED);
        }
        if (config.restrictFalling() && !player.onGround() && player.fallDistance > 0.0F) {
            return Optional.of(ChannelCancellation.FALLING);
        }
        if (config.restrictLava() && player.isInLava()) {
            return Optional.of(ChannelCancellation.LAVA);
        }
        if (config.restrictElytra() && player.isFallFlying()) {
            return Optional.of(ChannelCancellation.ELYTRA);
        }
        return Optional.empty();
    }

    private void cancelPlayer(ServerPlayer player, ChannelCancellation reason) {
        pending.get(player.getUUID()).ifPresent(channel -> cancel(channel, reason));
    }

    private void cancel(PendingChannel channel, ChannelCancellation reason) {
        terminalCleanup(channel, channel.playerInstance(), reason, true);
    }

    /**
     * One idempotent terminal path for cancellation, shutdown, completion claims, and failures
     * discovered after the synchronous teleport call. A claimed completion may call this again
     * with {@code requirePending=false}; cleanup stays harmless while its single final message is
     * still delivered.
     */
    private boolean terminalCleanup(
            PendingChannel channel,
            ServerPlayer feedbackTarget,
            ChannelCancellation reason,
            boolean requirePending
    ) {
        Optional<PendingChannel> removed = pending.removeExact(channel.playerId(), channel.token());
        if (requirePending && removed.isEmpty()) {
            return false;
        }
        if (reason != null && feedbackTarget != null && !feedbackTarget.hasDisconnected()) {
            feedback(feedbackTarget, reason);
        }
        return true;
    }

    private void shutdown() {
        for (PendingChannel channel : pending.snapshot()) {
            cancel(channel, ChannelCancellation.SERVER_STOPPING);
        }
    }

    private static void feedback(ServerPlayer player, ChannelCancellation reason) {
        if (reason.translationKey() != null) {
            player.sendOverlayMessage(Component.translatable(reason.translationKey()));
        }
    }
}
