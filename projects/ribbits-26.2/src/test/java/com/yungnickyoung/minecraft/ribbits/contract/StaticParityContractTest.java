package com.yungnickyoung.minecraft.ribbits.contract;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
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
        String[] merchantListings = {
                "new ItemsForAmethysts(BlockModule.BROWN_TOADSTOOL.get().asItem(), 1, 1, 8, 16, 32)",
                "new ItemsForAmethysts(BlockModule.RED_TOADSTOOL.get().asItem(), 1, 1, 8, 16, 32)",
                "new ItemsForAmethysts(BlockModule.TOADSTOOL_STEM.get().asItem(), 1, 1, 8, 16, 32)",
                "new ItemsForAmethysts(BlockModule.MOSSY_OAK_PLANKS.get().asItem(), 1, 2, 16, 32, 32)",
                "new ItemsForAmethysts(BlockModule.SWAMP_LANTERN.get().asItem(), 2, 3, 4, 8, 32)",
                "new ItemsForAmethysts(ItemModule.MARACA.get(), 6, 8, 1, 1, 4)"
        };
        String[] fishermanListings = {
                "new ItemsForAmethysts(Items.AXOLOTL_BUCKET, 4, 8, 1, 1, 16)",
                "new ItemsForAmethysts(Items.TROPICAL_FISH_BUCKET, 4, 8, 1, 1, 16)",
                "new ItemsForAmethysts(Items.COOKED_COD, 4, 8, 8, 24, 16)",
                "new ItemsForAmethysts(Items.COOKED_SALMON, 4, 8, 8, 24, 16)",
                "new EnchantedItemForAmethyst(Items.FISHING_ROD, 12, 16, 4)",
                "new AmethystForItems(Items.COD, 16, 32, 4, 8, 16)",
                "new AmethystForItems(Items.SALMON, 16, 32, 4, 8, 16)"
        };

        assertTrue(entity.contains("RibbitProfessionModule.NITWIT)) {\n            this.goalSelector.addGoal(6, this.musicGoal)"));
        assertTrue(entity.contains("RibbitProfessionModule.GARDENER)) {\n            this.goalSelector.addGoal(6, this.waterCropsGoal)"));
        assertTrue(entity.contains("RibbitProfessionModule.FISHERMAN)) {\n            this.goalSelector.addGoal(6, this.fishGoal)"));
        assertTrue(entity.contains("RibbitProfessionModule.SORCERER)) {\n            this.goalSelector.addGoal(6, this.applyBuffGoal)"));

        int merchantStart = trades.indexOf("map.put(RibbitProfessionModule.MERCHANT");
        int fishermanStart = trades.indexOf("map.put(RibbitProfessionModule.FISHERMAN");
        assertTrue(merchantStart >= 0 && fishermanStart > merchantStart);
        for (String listing : merchantListings) {
            int index = trades.indexOf(listing);
            assertTrue(index > merchantStart && index < fishermanStart, listing);
            assertEquals(1, occurrences(trades, listing), listing);
        }
        for (String listing : fishermanListings) {
            int index = trades.indexOf(listing);
            assertTrue(index > fishermanStart, listing);
            assertEquals(1, occurrences(trades, listing), listing);
        }
        assertEquals(13, merchantListings.length + fishermanListings.length);
        assertEquals(2, occurrences(trades, "map.put(RibbitProfessionModule."));
        assertTrue(trades.contains(
                "int numOffers = ribbit.getRibbitData().getProfession() == RibbitProfessionModule.MERCHANT ? 10 : 4;"));
        assertTrue(trades.contains("if (itemListings.length > numOffers)"));
        assertTrue(trades.contains("while (chosenIndices.size() < numOffers)"));
        assertTrue(trades.contains("chosenIndices.add(ribbit.getRandom().nextInt(itemListings.length))"));
    }

    @Test
    void sevenTradeLessProfessionsCannotOpenAnEmptyMerchantScreen() throws IOException {
        String entity = read("common/src/main/java/com/yungnickyoung/minecraft/ribbits/entity/RibbitEntity.java");
        String trades = read("common/src/main/java/com/yungnickyoung/minecraft/ribbits/module/RibbitTradeModule.java");
        String[] tradeLess = {
                "NITWIT",
                "GARDENER",
                "SORCERER",
                "CHEF",
                "FARMER",
                "PROSPECTOR",
                "GUARD"
        };

        for (String profession : tradeLess) {
            assertFalse(trades.contains("map.put(RibbitProfessionModule." + profession), profession);
        }

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
                + "                .ifPresent(offers -> this.offers = offers);"));
        assertTrue(entity.contains("valueOutput.store(\"Offers\", MerchantOffers.CODEC, offers);"));
        assertEquals(2, occurrences(entity, "MerchantOffers.CODEC"));
    }

    @Test
    void restockImplementationRemainsByteExactToAuthoritativeMain() throws IOException, NoSuchAlgorithmException {
        String entity = read("common/src/main/java/com/yungnickyoung/minecraft/ribbits/entity/RibbitEntity.java");
        int start = entity.indexOf("    @Override\n    public boolean canRestock()");
        int end = entity.indexOf("    public boolean isTrading()", start);
        assertTrue(start >= 0 && end > start);
        String restock = entity.substring(start, end);
        String hash = HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(restock.getBytes(StandardCharsets.UTF_8)));
        assertEquals("b7993d37498fe79130bdf4d8f3be6ae0e3d18153cb35c05c3dae420b5961e80a", hash);
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
            assertTrue(creative.contains("output.accept(ItemModule." + field + ".get());"), profession);
            assertTrue(entity.contains("case " + profession + " -> new ItemStack(ItemModule."
                    + field + ".get());"), profession);
        }
        assertEquals(9, occurrences(items, "new RibbitSpawnEggItem("));
        assertEquals(9, occurrences(items, "DispenserBlock.registerBehavior(RIBBIT_"));
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
                "ToggleSupporterHatPayloadS2C"
        };

        for (String payload : clientbound) {
            assertTrue(registrations.contains(
                    "PayloadTypeRegistry.clientboundPlay().register(" + payload + ".TYPE, " + payload + ".STREAM_CODEC)"), payload);
            assertTrue(clientReceivers.contains(
                    "ClientPlayNetworking.registerGlobalReceiver(" + payload + ".TYPE"), payload);
        }

        assertTrue(registrations.contains(
                "PayloadTypeRegistry.serverboundPlay().register(ToggleSupporterHatPayloadC2S.TYPE, ToggleSupporterHatPayloadC2S.STREAM_CODEC)"));
        assertEquals(7, occurrences(registrations, "PayloadTypeRegistry.clientboundPlay().register("));
        assertEquals(1, occurrences(registrations, "PayloadTypeRegistry.serverboundPlay().register("));
        assertEquals(7, occurrences(clientReceivers, "ClientPlayNetworking.registerGlobalReceiver("));
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
