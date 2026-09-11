package dev.resivore.dragonbound.contract;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.resivore.dragonbound.config.DragonboundConfig;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ArchitectureContractTest {
    private static final Path JAVA_ROOT = Path.of("src/main/java");
    private static final Path DRAGONBOUND_ROOT = JAVA_ROOT.resolve("dev/resivore/dragonbound");

    @Test
    void defaultChannelAndStaffCooldownAreTwoAndSixtySeconds() {
        DragonboundConfig defaults = DragonboundConfig.defaults();

        assertEquals(40, defaults.channelTicks());
        assertEquals(1_200, defaults.staffCooldownTicks());
        assertEquals(0.25D, defaults.movementTolerance());
    }

    @Test
    void staffHasNoDurabilityContract() throws IOException {
        String content = readJava("DragonboundContent.java");
        String staffProperties = content.substring(
                content.indexOf("public static final Item DRAGONBOUND_STAFF"),
                content.indexOf("private DragonboundContent()"));

        assertTrue(staffProperties.contains(".stacksTo(1)"));
        assertFalse(staffProperties.contains("durability("));
        assertFalse(staffProperties.contains("damage("));
    }

    @Test
    void acceptedDamageMixinUsesReturnedBooleanAtMethodReturn() throws IOException {
        String mixin = readJava("mixin/ServerPlayerChannelMixin.java");

        assertTrue(mixin.contains("@Inject(method = \"hurtServer\", at = @At(\"RETURN\"))"));
        assertTrue(mixin.contains("CallbackInfoReturnable<Boolean> callback"));
        assertTrue(mixin.contains("if (callback.getReturnValueZ())"));
        assertTrue(mixin.contains("DragonboundChannelManager.onAcceptedDamage"));
    }

    @Test
    void commonInitializerHasNoClientOnlyCodeOrClientEntrypoint() throws IOException {
        String allJava = readAllJava();
        String initializer = readJava("DragonboundWaystone.java");
        JsonObject metadata = JsonParser.parseString(
                Files.readString(Path.of("src/main/resources/fabric.mod.json"))).getAsJsonObject();

        assertFalse(allJava.contains("net.minecraft.client"));
        assertFalse(allJava.contains("ClientModInitializer"));
        assertTrue(initializer.contains("implements ModInitializer"));
        assertFalse(metadata.getAsJsonObject("entrypoints").has("client"));
    }

    @Test
    void lifecycleIsServerTickOwnedAndNotWallClockScheduled() throws IOException {
        String manager = readJava("channel/DragonboundChannelManager.java");
        String allJava = readAllJava();
        String normalized = allJava.toLowerCase(Locale.ROOT);

        assertTrue(manager.contains("Map<MinecraftServer, DragonboundChannelManager>"));
        assertTrue(manager.contains("ServerTickEvents.END_SERVER_TICK.register"));
        assertTrue(manager.contains("ServerPlayConnectionEvents.DISCONNECT.register"));
        assertTrue(manager.contains("ServerLifecycleEvents.SERVER_STOPPED.register"));
        assertFalse(normalized.contains("scheduledexecutor"));
        assertFalse(normalized.contains("scheduleatfixedrate"));
        assertFalse(normalized.contains("java.util.timer"));
        assertFalse(normalized.contains("thread.sleep"));
    }

    @Test
    void exactLegacyDefaultMigrationIsAppliedAndPersistedDuringServerConfigLoad() throws IOException {
        String manager = readJava("config/DragonboundConfigManager.java");
        String migration = readJava("config/DragonboundConfigMigration.java");
        int apply = manager.indexOf("DragonboundConfigMigration.apply(");
        int currentAssignment = manager.indexOf("current = DragonboundConfig.fromJson(migration.json())", apply);
        int changed = manager.indexOf("if (migration.changed())", currentAssignment);
        int persist = manager.indexOf("persistCurrent()", changed);

        assertTrue(apply >= 0);
        assertTrue(currentAssignment > apply);
        assertTrue(changed > currentAssignment);
        assertTrue(persist > changed);
        assertTrue(migration.contains("source.keySet().equals(LEGACY_DEFAULT_KEYS)"));
        assertTrue(migration.contains("LEGACY_DEFAULT_CHANNEL_TICKS = 80"));
        assertTrue(migration.contains("DragonboundConfig.DEFAULT_CHANNEL_TICKS"));
    }

    @Test
    void oneClickStartsOnlyTheServerOwnedChannelAndReleaseHasNoLifecycleHook() throws IOException {
        String item = readJava("channel/DragonboundReturnItem.java");
        int clientBranch = item.indexOf("if (level.isClientSide())");
        int clientAcknowledgement = item.indexOf("return InteractionResult.CONSUME", clientBranch);
        int serverManagerStart = item.indexOf("DragonboundChannelManager.start", clientAcknowledgement);

        assertTrue(clientBranch >= 0);
        assertTrue(clientAcknowledgement > clientBranch);
        assertTrue(serverManagerStart > clientAcknowledgement);
        assertFalse(item.contains("startUsingItem"));
        assertFalse(item.contains("releaseUsing"));
        assertFalse(item.contains("getUseDuration"));
        assertFalse(item.contains("getUseAnimation"));
        assertFalse(item.contains("cancelReleased"));
    }

    @Test
    void pearlAndStaffShareOneSemanticClickOnceLifecycleWithoutReferenceIdentity() throws IOException {
        String content = readJava("DragonboundContent.java");
        String manager = readJava("channel/DragonboundChannelManager.java");
        String rules = readJava("channel/ChannelRules.java");

        assertEquals(2, countOccurrences(content, "new DragonboundReturnItem("));
        assertTrue(manager.contains("player.getInventory().getSelectedSlot()"));
        assertTrue(manager.contains("ChannelRules.sourceItemIsStillValid("));
        assertTrue(rules.contains("ItemStack.isSameItemSameComponents(liveStack, snapshot)"));
        assertFalse(manager.contains("player.isUsingItem()"));
        assertFalse(manager.contains("player.getUsedItemHand()"));
        assertFalse(manager.contains("player.getUseItem()"));
        assertFalse(manager.contains("startUsingItem"));
        assertFalse(manager.contains("stopUsingItem"));
        assertFalse(manager.contains("getMainHandItem() =="));
        assertFalse(manager.contains("getUseItem() =="));
        assertFalse(manager.contains("channel.heldStack()"));
    }

    @Test
    void repeatedClickWhilePendingIsRefusedBeforeCreatingAnotherChannel() throws IOException {
        String manager = readJava("channel/DragonboundChannelManager.java");
        int duplicateCheck = manager.indexOf("if (pending.get(player.getUUID()).isPresent())");
        int duplicateFeedback = manager.indexOf(
                "feedback(player, ChannelCancellation.ALREADY_CHANNELING)", duplicateCheck);
        int add = manager.indexOf("if (!pending.add(channel))", duplicateFeedback);

        assertTrue(duplicateCheck >= 0);
        assertTrue(duplicateFeedback > duplicateCheck);
        assertTrue(add > duplicateFeedback);
    }

    @Test
    void destinationContractContainsNoNearbySearchOrFallback() throws IOException {
        String destination = readJava("channel/DestinationRules.java");
        String manager = readJava("channel/DragonboundChannelManager.java");
        String combined = (destination + manager).toLowerCase(Locale.ROOT);

        assertTrue(manager.contains("DestinationRules.exactTopCenter(channel.anchor().pos())"));
        assertFalse(combined.contains("betweenclosed"));
        assertFalse(combined.contains("findnearby"));
        assertFalse(combined.contains("nearbyposition"));
        assertFalse(combined.contains("spiral"));
        assertFalse(combined.contains("fallbackdestination"));
        assertFalse(combined.contains("offset("));
    }

    @Test
    void successEffectsFollowConfirmedArrivalAndPendingEntryIsRemovedFirst() throws IOException {
        String manager = readJava("channel/DragonboundChannelManager.java");
        int completionClaim = manager.indexOf("terminalCleanup(channel, null, null, true)");
        int teleport = manager.indexOf("player.teleport(new TeleportTransition(");
        int successGate = manager.indexOf("ChannelRules.shouldApplySuccessEffect(confirmed)");
        int pearlConsumption = manager.indexOf("arrivedHeld.shrink(1)");
        int staffCooldown = manager.indexOf("addCooldown(arrivedHeld, channel.staffCooldownTicks())");

        int cleanupMethod = manager.indexOf("private boolean terminalCleanup(");
        String cleanup = manager.substring(cleanupMethod);
        int exactRemoval = cleanup.indexOf("pending.removeExact(channel.playerId(), channel.token())");

        assertTrue(completionClaim >= 0 && completionClaim < teleport);
        assertTrue(exactRemoval >= 0);
        assertFalse(cleanup.contains("stopUsingItem"));
        assertTrue(manager.indexOf(
                "terminalCleanup(channel, player, ChannelCancellation.TELEPORT_FAILED, false)") > teleport);
        assertTrue(manager.indexOf(
                "terminalCleanup(channel, arrived, ChannelCancellation.TELEPORT_FAILED, false)") > teleport);
        assertTrue(teleport < successGate);
        assertTrue(successGate < pearlConsumption);
        assertTrue(successGate < staffCooldown);
    }

    @Test
    void audiovisualFeedbackIsSharedAndBoundToSuccessfulLifecycleTransitions() throws IOException {
        String manager = readJava("channel/DragonboundChannelManager.java");
        String effects = readJava("channel/ChannelEffects.java");

        int pendingAdd = manager.indexOf("if (!pending.add(channel))");
        int completionTick = manager.indexOf("ChannelRules.completionTickReached(gameTime, channel.completionTick())");
        int channelStart = manager.indexOf("ChannelEffects.channelStarted(player, config.channelTicks())");
        int channelTick = manager.indexOf("ChannelEffects.channelTick(player, gameTime - channel.startTick(), config.channelTicks())");
        int confirmed = manager.indexOf("ChannelRules.shouldApplySuccessEffect(confirmed)");
        int heldItemConfirmed = manager.indexOf("if (!heldItemConfirmed)");
        int completionBurst = manager.indexOf("ChannelEffects.successfulTeleport(arrived)");
        int pearlConsumption = manager.indexOf("arrivedHeld.shrink(1)");
        int staffCooldown = manager.indexOf("addCooldown(arrivedHeld, channel.staffCooldownTicks())");

        assertTrue(pendingAdd >= 0);
        assertTrue(pendingAdd < channelStart);
        assertTrue(completionTick >= 0);
        assertTrue(channelTick > completionTick);
        assertFalse(manager.contains("ServerLevel departure ="));
        assertTrue(confirmed < heldItemConfirmed && heldItemConfirmed < completionBurst);
        assertTrue(completionBurst < pearlConsumption);
        assertTrue(completionBurst < staffCooldown);

        assertTrue(effects.contains("SoundEvents.ENDERMAN_TELEPORT"));
        assertTrue(effects.contains("ParticleTypes.PORTAL"));
        assertTrue(effects.contains("SoundSource.PLAYERS"));
        assertEquals(1, countOccurrences(effects, "playSound("));
        assertTrue(effects.contains("portalBurst(level, position)"));
        assertTrue(effects.contains("CHANNEL_PARTICLES_AT_START = 1"));
        assertTrue(effects.contains("CHANNEL_PARTICLES_AT_COMPLETION = 4"));
        assertTrue(effects.contains("channelParticleCount(elapsedTicks, channelTicks)"));
        assertTrue(effects.contains("FOREGROUND_CHANNEL_PARTICLES_AT_START = 3"));
        assertTrue(effects.contains("FOREGROUND_CHANNEL_PARTICLES_AT_COMPLETION = 8"));
        assertTrue(effects.contains("FOREGROUND_SUCCESS_PARTICLE_COUNT = 24"));
        assertTrue(effects.contains("FOREGROUND_FORWARD_OFFSET = 0.55D"));
        assertTrue(effects.contains("FOREGROUND_VERTICAL_OFFSET = -0.15D"));
        assertTrue(effects.contains("foregroundChannelParticleCount(elapsedTicks, channelTicks)"));
        assertTrue(effects.contains("level.sendParticles(\n                player,"));
        assertEquals(2, countOccurrences(effects, "level.sendParticles(\n                player,"));
        assertTrue(effects.contains("0.50D,\n                0.35D,\n                0.50D"));
        assertTrue(effects.contains("0.60D,\n                0.45D,\n                0.60D"));
        assertFalse(effects.contains("departure"));
    }

    @Test
    void staffSwingIsServerOnlyAndOccursOnlyAfterAcceptedStaffChannelCreation() throws IOException {
        String manager = readJava("channel/DragonboundChannelManager.java");
        String item = readJava("channel/DragonboundReturnItem.java");
        int pendingAdd = manager.indexOf("if (!pending.add(channel))");
        int staffCheck = manager.indexOf("if (source == ReturnSource.DRAGONBOUND_STAFF)");
        int swing = manager.indexOf("player.swing(InteractionHand.MAIN_HAND, true)");

        assertTrue(pendingAdd < staffCheck && staffCheck < swing);
        assertFalse(item.contains("swing("));
        assertFalse(item.contains("startUsingItem"));
    }

    @Test
    void everyPendingChannelIsRevalidatedForLifecycleAndUnsafeTransitions() throws IOException {
        String manager = readJava("channel/DragonboundChannelManager.java");
        String anchors = readJava("anchor/DragonboundAnchors.java");
        String anchorData = readJava("anchor/DragonboundAnchorData.java");

        assertTrue(manager.contains("player == null || player.hasDisconnected()"));
        assertTrue(manager.contains("player != channel.playerInstance()"));
        assertTrue(manager.contains("!player.isAlive()"));
        assertTrue(manager.contains("!player.level().dimension().equals(channel.startDimension())"));
        assertTrue(manager.contains("!sourceItemIsStillValid(player, channel)"));
        assertTrue(manager.contains("!DragonboundAnchors.matches(server, channel.anchor())"));
        assertTrue(manager.contains("Optional<ChannelCancellation> restriction = unsafeState(player, config)"));
        assertTrue(manager.contains("cancel(channel, restriction.get())"));

        assertTrue(anchors.contains("return get(server).matches(expected)"));
        assertTrue(anchorData.contains("return active.filter(expected::equals).isPresent()"));
        assertTrue(anchorData.contains("new AnchorBinding(dimension, pos, ++lastGeneration)"));
    }

    @Test
    void deathHasAnExplicitImmediateCancellationHook() throws IOException {
        String mixin = readJava("mixin/ServerPlayerChannelMixin.java");

        assertTrue(mixin.contains("@Inject(method = \"die\", at = @At(\"HEAD\"))"));
        assertTrue(mixin.contains("DragonboundChannelManager.onDeath"));
    }

    @Test
    void crossDimensionResolutionUsesBoundedFullChunkLoadWithoutPermanentTicket() throws IOException {
        String manager = readJava("channel/DragonboundChannelManager.java");
        String normalized = manager.toLowerCase(Locale.ROOT);

        assertTrue(manager.contains("server.getLevel(channel.anchor().dimension())"));
        assertTrue(manager.contains("ChunkStatus.FULL"));
        assertTrue(manager.contains("TeleportTransition.DO_NOTHING"));
        assertFalse(manager.contains("TeleportTransition.PLACE_PORTAL_TICKET"));
        assertFalse(normalized.contains("setchunkforced"));
        assertFalse(normalized.contains("addregionticket"));
        assertFalse(normalized.contains("addticket"));
        assertFalse(normalized.contains("forcedchunk"));
    }

    private static String readJava(String relativePath) throws IOException {
        return Files.readString(DRAGONBOUND_ROOT.resolve(relativePath));
    }

    private static String readAllJava() throws IOException {
        StringBuilder source = new StringBuilder();
        try (Stream<Path> paths = Files.walk(JAVA_ROOT)) {
            for (Path path : paths.filter(file -> file.toString().endsWith(".java")).sorted().toList()) {
                source.append(Files.readString(path)).append('\n');
            }
        }
        return source.toString();
    }

    private static int countOccurrences(String value, String needle) {
        int count = 0;
        int offset = 0;
        while ((offset = value.indexOf(needle, offset)) >= 0) {
            count++;
            offset += needle.length();
        }
        return count;
    }
}
