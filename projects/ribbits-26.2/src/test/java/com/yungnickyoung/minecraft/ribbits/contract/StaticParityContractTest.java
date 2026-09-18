package com.yungnickyoung.minecraft.ribbits.contract;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StaticParityContractTest {
    private static final Path PROJECT_ROOT = Path.of(System.getProperty("projectRoot"));

    @Test
    void entitySyncedDefaultsAndPersistenceKeysRemainExact() throws IOException {
        String source = read("common/src/main/java/com/yungnickyoung/minecraft/ribbits/entity/RibbitEntity.java");
        String[] accessors = {
                "RIBBIT_DATA",
                "PLAYING_INSTRUMENT",
                "UMBRELLA_FALLING",
                "WATERING",
                "FISHING",
                "BUFFING"
        };

        int previousDeclaration = -1;
        for (String accessor : accessors) {
            String declaration = " " + accessor + " = SynchedEntityData.defineId";
            int declarationIndex = source.indexOf(declaration);
            assertTrue(declarationIndex > previousDeclaration, accessor + " declaration order");
            assertEquals(1, occurrences(source, declaration), accessor + " declaration count");
            assertEquals(1, occurrences(source, "builder.define(" + accessor + ","), accessor + " default count");
            previousDeclaration = declarationIndex;
        }

        assertTrue(source.contains("builder.define(RIBBIT_DATA, new RibbitData(RibbitProfessionModule.NITWIT, RibbitUmbrellaTypeModule.UMBRELLA_1, RibbitInstrumentModule.NONE))"));
        for (String accessor : new String[]{"PLAYING_INSTRUMENT", "UMBRELLA_FALLING", "WATERING", "FISHING", "BUFFING"}) {
            assertTrue(source.contains("builder.define(" + accessor + ", false)"), accessor);
        }
        for (String key : new String[]{"RibbitData", "Offers", "HomePosX", "HomePosY", "HomePosZ"}) {
            assertTrue(source.contains("\"" + key + "\""), key);
        }
    }

    @Test
    void professionGoalsAndTradeInventoryRemainMappedToExactRoles() throws IOException {
        String entity = read("common/src/main/java/com/yungnickyoung/minecraft/ribbits/entity/RibbitEntity.java");
        String trades = read("common/src/main/java/com/yungnickyoung/minecraft/ribbits/module/RibbitTradeModule.java");

        assertTrue(entity.contains("RibbitProfessionModule.NITWIT)) {\n            this.goalSelector.addGoal(6, this.musicGoal)"));
        assertTrue(entity.contains("RibbitProfessionModule.GARDENER)) {\n            this.goalSelector.addGoal(6, this.waterCropsGoal)"));
        assertTrue(entity.contains("RibbitProfessionModule.FISHERMAN)) {\n            this.goalSelector.addGoal(6, this.fishGoal)"));
        assertTrue(entity.contains("RibbitProfessionModule.SORCERER)) {\n            this.goalSelector.addGoal(6, this.applyBuffGoal)"));

        assertTrue(trades.contains("Central, declarative trade-profile layer"));
        for (String profession : List.of("nitwit", "gardener", "fisherman", "merchant",
                "chef", "farmer", "sorcerer", "prospector", "guard")) {
            assertTrue(trades.contains("\"" + profession + "\""), profession);
        }
        assertTrue(trades.contains("FIXED_PRICE_MULTIPLIER = 0.0F"));
        assertFalse(trades.contains("ItemsForAmethysts"));
        assertFalse(trades.contains("AmethystForItems"));
        assertFalse(trades.contains("minecraft:amethyst_shard"));
    }

    @Test
    void allNineProfessionsUseTheNormalMerchantScreenWhenTheyHaveOffers() throws IOException {
        String entity = read("common/src/main/java/com/yungnickyoung/minecraft/ribbits/entity/RibbitEntity.java");
        String trades = read("common/src/main/java/com/yungnickyoung/minecraft/ribbits/module/RibbitTradeModule.java");
        assertTrue(trades.contains("profiles.put(\"nitwit\""));
        assertTrue(trades.contains("private static final List<TradeOfferSpec> ALL_OFFERS"));

        int interactStart = entity.indexOf("public @NotNull InteractionResult mobInteract");
        int interactEnd = entity.indexOf("public void reassessGoals()", interactStart);
        String interaction = entity.substring(interactStart, interactEnd);
        int emptyOffers = interaction.indexOf("boolean bl = this.getOffers().isEmpty();");
        int emptyReturn = interaction.indexOf("if (bl) {\n                return InteractionResult.PASS;\n            }");
        int openScreen = interaction.indexOf("this.startTrading(player);");
        assertTrue(emptyOffers >= 0);
        assertTrue(emptyReturn > emptyOffers);
        assertTrue(openScreen > emptyReturn);
        assertEquals(1, occurrences(interaction, "this.startTrading(player);"));
    }

    @Test
    void savedMerchantAndFishermanOffersStillUseTheMinecraftCodec() throws IOException {
        String entity = read("common/src/main/java/com/yungnickyoung/minecraft/ribbits/entity/RibbitEntity.java");
        assertTrue(entity.contains("valueInput.read(\"Offers\", MerchantOffers.CODEC)\n"
                + "                .ifPresent(offers -> {"));
        assertTrue(entity.contains("RibbitTradeModule.restoreStrictComponentMatching(this, offers);"));
        assertTrue(entity.contains("valueOutput.store(\"Offers\", MerchantOffers.CODEC, offersToSave);"));
        assertEquals(2, occurrences(entity, "MerchantOffers.CODEC"));
    }

    @Test
    void restockImplementationIsPersistentDailyAndDemandFree() throws IOException {
        String entity = read("common/src/main/java/com/yungnickyoung/minecraft/ribbits/entity/RibbitEntity.java");
        String policy = read("common/src/main/java/com/yungnickyoung/minecraft/ribbits/entity/trade/RibbitRestockPolicy.java");
        assertTrue(entity.contains("MynxRestockDay") || read(
                "common/src/main/java/com/yungnickyoung/minecraft/ribbits/entity/trade/RibbitTradeState.java")
                .contains("MynxRestockDay"));
        assertTrue(policy.contains("MAX_ADDITIONAL_RESTOCKS = 2"));
        assertTrue(policy.contains("MIN_RESTOCK_SPACING = 2400L"));
        assertTrue(entity.contains("this.needsToRestock()"));
        assertTrue(entity.contains("pendingChefDayChange"));
        assertFalse(entity.contains("updateDemand();"));
    }

    @Test
    void allNineTypedEggsAreRegisteredDispensableAndMappedToTheirProfession() throws IOException {
        String items = read("common/src/main/java/com/yungnickyoung/minecraft/ribbits/module/ItemModule.java");
        String creative = read("common/src/main/java/com/yungnickyoung/minecraft/ribbits/module/CreativeTabModule.java");
        String entity = read("common/src/main/java/com/yungnickyoung/minecraft/ribbits/entity/RibbitEntity.java");
        String[] professions = {
                "NITWIT",
                "FISHERMAN",
                "GARDENER",
                "MERCHANT",
                "SORCERER",
                "CHEF",
                "FARMER",
                "PROSPECTOR",
                "GUARD"
        };

        for (String profession : professions) {
            String lower = profession.toLowerCase(java.util.Locale.ROOT);
            String field = "RIBBIT_" + profession + "_SPAWN_EGG";
            assertTrue(items.contains("@AutoRegister(\"ribbit_" + lower + "_spawn_egg\")"), profession);
            assertTrue(items.contains("new RibbitSpawnEggItem(RibbitProfessionModule." + profession), profession);
            assertTrue(items.contains("RegisterHelper.itemKey(\"ribbit_" + lower + "_spawn_egg\")"), profession);
            assertTrue(items.contains("DispenserBlock.registerBehavior(" + field
                    + "::get, ribbitSpawnEggDispenseItemBehavior);"), profession);
            assertTrue(creative.contains("CreativeEntry.of(\"ribbit_" + lower
                    + "_spawn_egg\", ItemModule." + field + "::get)"), profession);
            assertTrue(entity.contains("case " + profession + " -> new ItemStack(ItemModule."
                    + field + ".get());"), profession);
        }
        assertEquals(9, occurrences(items, "new RibbitSpawnEggItem("));
        assertEquals(9, occurrences(items, "DispenserBlock.registerBehavior(RIBBIT_"));
    }

    @Test
    void wanderingEggUsesTheStandardComponentBackedSpawnPathExactlyOnce() throws IOException {
        String items = read("common/src/main/java/com/yungnickyoung/minecraft/ribbits/module/ItemModule.java");
        String creative = read("common/src/main/java/com/yungnickyoung/minecraft/ribbits/module/CreativeTabModule.java");
        String fabric = read("fabric/src/main/java/com/yungnickyoung/minecraft/ribbits/fabric/RibbitsFabric.java");
        String entity = read("common/src/main/java/com/yungnickyoung/minecraft/ribbits/entity/WanderingRibbitEntity.java");

        assertEquals(1, occurrences(items, "@AutoRegister(\"wandering_ribbit_spawn_egg\")"));
        assertEquals(1, occurrences(items, "new SpawnEggItem("));
        assertTrue(items.contains(".spawnEgg(EntityTypeModule.WANDERING_RIBBIT.get())"));
        assertFalse(items.contains("DispenserBlock.registerBehavior(WANDERING_RIBBIT_SPAWN_EGG"));
        assertFalse(items.contains("new RibbitSpawnEggItem(RibbitProfessionModule.WANDERING"));
        assertEquals(1, occurrences(creative,
                "CreativeEntry.of(\"wandering_ribbit_spawn_egg\", ItemModule.WANDERING_RIBBIT_SPAWN_EGG::get)"));
        assertEquals(1, occurrences(fabric,
                "CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS)"));
        assertEquals(1, occurrences(fabric, "CreativeModeTab.TabVisibility.PARENT_TAB_ONLY"));
        assertFalse(fabric.contains("CreativeModeTabs.SEARCH"));
        assertFalse(entity.contains("getPickResult()"),
                "standard Mob pick-block must resolve the component-bound SpawnEggItem");
    }

    @Test
    void creativeTabHasOneValidatedOrderedEntryPerRegisteredItem() throws IOException {
        String creative = read("common/src/main/java/com/yungnickyoung/minecraft/ribbits/module/CreativeTabModule.java");
        List<String> expectedIds = List.of(
                "red_toadstool",
                "brown_toadstool",
                "toadstool_stem",
                "swamp_lantern",
                "giant_lilypad",
                "swamp_daisy",
                "toadstool",
                "small_brown_toadstool",
                "glowcap",
                "toadstool_heart",
                "chute_leaf",
                "umbrella_leaf",
                "mossy_oak_planks",
                "mossy_oak_planks_stairs",
                "mossy_oak_planks_slab",
                "mossy_oak_planks_fence",
                "mossy_oak_planks_fence_gate",
                "mossy_oak_door",
                "maraca",
                "ribbit_nitwit_spawn_egg",
                "ribbit_fisherman_spawn_egg",
                "ribbit_gardener_spawn_egg",
                "ribbit_merchant_spawn_egg",
                "ribbit_sorcerer_spawn_egg",
                "ribbit_chef_spawn_egg",
                "ribbit_farmer_spawn_egg",
                "ribbit_prospector_spawn_egg",
                "ribbit_guard_spawn_egg",
                "wandering_ribbit_spawn_egg"
        );

        Matcher matcher = Pattern.compile("CreativeEntry\\.of\\(\\\"([^\\\"]+)\\\"").matcher(creative);
        List<String> actualIds = new ArrayList<>();
        while (matcher.find()) {
            actualIds.add(matcher.group(1));
        }

        assertEquals(expectedIds, actualIds, "creative entry IDs and order");
        assertEquals(expectedIds.size(), new HashSet<>(actualIds).size(), "creative entry IDs must be unique");
        assertEquals(1, occurrences(creative, "CreativeEntry.of(\"mossy_oak_planks_slab\""));
        assertEquals(1, occurrences(creative, "output.accept("), "the generator must emit only through the validated table");
        assertTrue(creative.contains("BuiltInRegistries.ITEM.getKey(item)"));
        assertTrue(creative.contains("!seenItems.add(item) || !seenIds.add(actualId)"));
        assertTrue(creative.contains("Duplicate Ribbits creative entry"));
    }

    @Test
    void newVisualProfessionsHaveNoImportedBehaviorHooks() throws IOException {
        String entity = read("common/src/main/java/com/yungnickyoung/minecraft/ribbits/entity/RibbitEntity.java");
        String goals = entity.substring(
                entity.indexOf("public void reassessGoals()"),
                entity.indexOf("@Override\n    public float getSpeed()"));
        for (String profession : new String[]{"CHEF", "FARMER", "PROSPECTOR", "GUARD"}) {
            assertFalse(goals.contains("RibbitProfessionModule." + profession), profession);
        }
        assertFalse(entity.contains("GuardRibbit"));
        assertFalse(entity.contains("UsefulRibbit"));
    }

    @Test
    void villageRandomizationIsLimitedToScrubbedTemplateDataAndStructureFallback() throws IOException {
        String entity = read("common/src/main/java/com/yungnickyoung/minecraft/ribbits/entity/RibbitEntity.java");
        int constructorStart = entity.indexOf("public RibbitEntity(EntityType<RibbitEntity> entityType, Level level)");
        int constructorEnd = entity.indexOf("@Override\n    protected void registerGoals()", constructorStart);
        String constructor = entity.substring(constructorStart, constructorEnd);
        assertTrue(constructor.contains("this.reassessGoals();"));
        assertFalse(constructor.contains("initializeDefaultVillageProfession"));

        int loadStart = entity.indexOf("protected void readAdditionalSaveData(ValueInput valueInput)");
        int loadEnd = entity.indexOf("protected void addAdditionalSaveData", loadStart);
        String load = entity.substring(loadStart, loadEnd);
        assertTrue(load.contains("if (savedRibbitData.isPresent())"));
        assertTrue(load.contains("this.setRibbitData(savedRibbitData.get());"));
        assertTrue(load.contains("this.initializeDefaultVillageProfession(this.getRandom());"));

        int finalizeStart = entity.indexOf("public SpawnGroupData finalizeSpawn");
        int finalizeEnd = entity.indexOf("public boolean removeWhenFarAway", finalizeStart);
        String finalizeSpawn = entity.substring(finalizeStart, finalizeEnd);
        assertTrue(finalizeSpawn.contains("if (entitySpawnReason == EntitySpawnReason.STRUCTURE)"));
        assertTrue(finalizeSpawn.contains("this.initializeDefaultVillageProfession(level.getRandom());"));
    }

    @Test
    void tradeMenuValidityPreservesExactCustomerIdentityRule() throws IOException {
        String source = read("common/src/main/java/com/yungnickyoung/minecraft/ribbits/entity/RibbitEntity.java");
        Pattern exactMethod = Pattern.compile(
                "public boolean stillValid\\(Player player\\) \\{\\s*return this\\.getTradingPlayer\\(\\) == player;\\s*}");

        assertTrue(exactMethod.matcher(source).find());
        assertFalse(source.contains("player.isWithinEntityInteractionRange(this, 4.0D)"));
    }

    @Test
    void regionalPrideOptionRemainsOptInAndKeepsTheExactDefault() throws IOException {
        String config = read("common/src/main/java/com/yungnickyoung/minecraft/ribbits/config/RibbitsConfig.java");
        String screen = read("common/src/main/java/com/yungnickyoung/minecraft/ribbits/client/screen/RibbitsClothScreen.java");

        assertTrue(config.contains("public boolean disablePrideFlagCN = false"));
        assertTrue(screen.contains(".setDefaultValue(false)"));
    }

    @Test
    void clientLifecycleRestoresQueueAndSupporterPopulationStartsOnce() throws IOException {
        String client = read("fabric/src/main/java/com/yungnickyoung/minecraft/ribbits/fabric/client/RibbitsFabricClient.java");
        String commonClient = read("common/src/main/java/com/yungnickyoung/minecraft/ribbits/client/RibbitsCommonClient.java");
        String networkModule = read("common/src/main/java/com/yungnickyoung/minecraft/ribbits/module/NetworkModule.java");

        assertTrue(client.contains("ClientEntityEvents.ENTITY_LOAD.register"));
        assertTrue(client.contains("ClientNetworkHandler.onEntityLoad(entity)"));
        assertTrue(client.contains("ClientPlayConnectionEvents.DISCONNECT.register"));
        assertTrue(client.contains("ClientNetworkHandler.clearPendingActions()"));
        assertEquals(0, occurrences(commonClient, "SupportersJSON.populateSupportersList()"));
        assertEquals(1, occurrences(networkModule, "SupportersJSON.populateSupportersList()"));
    }

    @Test
    void payloadDirectionsAndClientReceiverBoundaryRemainExact() throws IOException {
        String registrations = read("fabric/src/main/java/com/yungnickyoung/minecraft/ribbits/fabric/module/NetworkModuleFabric.java");
        String clientReceivers = read("fabric/src/main/java/com/yungnickyoung/minecraft/ribbits/fabric/client/ClientNetworkModuleFabric.java");
        String[] clientbound = {
                "RibbitStartMusicSinglePayload",
                "RibbitStopMusicSinglePayload",
                "RibbitStartMusicAllPayload",
                "StartHearingMaracaPayload",
                "StopHearingMaracaPayload",
                "RequestSupporterHatStatePayload",
                "ToggleSupporterHatPayloadS2C",
                "ChuteAckS2C"
        };

        for (String payload : clientbound) {
            assertTrue(registrations.contains(
                    "PayloadTypeRegistry.clientboundPlay().register(" + payload + ".TYPE, " + payload + ".STREAM_CODEC)"), payload);
            assertTrue(clientReceivers.contains(
                    "ClientPlayNetworking.registerGlobalReceiver(" + payload + ".TYPE"), payload);
        }

        assertTrue(registrations.contains(
                "PayloadTypeRegistry.serverboundPlay().register(ToggleSupporterHatPayloadC2S.TYPE, ToggleSupporterHatPayloadC2S.STREAM_CODEC)"));
        assertTrue(registrations.contains(
                "PayloadTypeRegistry.serverboundPlay().register(ChutePressC2S.TYPE, ChutePressC2S.STREAM_CODEC)"));
        assertEquals(8, occurrences(registrations, "PayloadTypeRegistry.clientboundPlay().register("));
        assertEquals(2, occurrences(registrations, "PayloadTypeRegistry.serverboundPlay().register("));
        assertEquals(8, occurrences(clientReceivers, "ClientPlayNetworking.registerGlobalReceiver("));
        assertFalse(registrations.contains("ClientNetworkHandler"));
    }

    @Test
    void maracaStopRejectsLoadedNonPlayerTargetsBeforeStoppingSound() throws IOException {
        String handler = read("common/src/main/java/com/yungnickyoung/minecraft/ribbits/network/ClientNetworkHandler.java");
        Pattern stopHandler = Pattern.compile(
                "handleStopHearingMaracaS2C.*?if \\(!\\(performer instanceof Player\\)\\).*?return;.*?ribbits\\$stopMaraca",
                Pattern.DOTALL);

        assertTrue(stopHandler.matcher(handler).find());
    }

    @Test
    void startAllQueuesEachMemberWithoutEarlyExitOnMissingEntity() throws IOException {
        String handler = read("common/src/main/java/com/yungnickyoung/minecraft/ribbits/network/ClientNetworkHandler.java");
        int methodStart = handler.indexOf("handleStartMusicAllS2C");
        int methodEnd = handler.indexOf("handleStartHearingMaracaS2C", methodStart);
        String startAll = handler.substring(methodStart, methodEnd);

        assertTrue(startAll.contains("for (int i = 0; i < payload.ribbitUUIDs().size(); i++)"));
        assertEquals(1, occurrences(startAll, "queueOrExecute(entityId,"));
        assertFalse(startAll.contains("callGetEntities()"));
        assertFalse(startAll.contains("not found"));
    }

    @Test
    void allSixExactProcessorIdsRemainBoundToTheirCodecs() throws IOException {
        String source = read("common/src/main/java/com/yungnickyoung/minecraft/ribbits/module/StructureProcessorTypeModule.java");
        String[][] contracts = {
                {"pillar_processor", "PillarProcessor.CODEC"},
                {"podzol_processor", "PodzolProcessor.CODEC"},
                {"warped_nylium_processor", "WarpedNyliumProcessor.CODEC"},
                {"block_replace_processor", "BlockReplaceProcessor.CODEC"},
                {"lapis_block_processor", "LapisBlockProcessor.CODEC"},
                {"brewing_stand_processor", "BrewingStandProcessor.CODEC"}
        };

        for (String[] contract : contracts) {
            assertTrue(source.contains("@AutoRegister(\"" + contract[0] + "\")"), contract[0]);
            assertTrue(source.contains("AutoRegisterStructureProcessor.of(() -> " + contract[1] + ")"), contract[1]);
        }
        assertEquals(6, occurrences(source, "AutoRegisterStructureProcessor.of("));
    }

    private static String read(String relativePath) throws IOException {
        return Files.readString(PROJECT_ROOT.resolve(relativePath)).replace("\r\n", "\n");
    }

    private static int occurrences(String value, String needle) {
        int count = 0;
        int fromIndex = 0;
        while ((fromIndex = value.indexOf(needle, fromIndex)) >= 0) {
            count++;
            fromIndex += needle.length();
        }
        return count;
    }
}
