package com.yungnickyoung.minecraft.ribbits.contract;

import com.yungnickyoung.minecraft.ribbits.entity.trade.RibbitTradeState;
import com.yungnickyoung.minecraft.ribbits.entity.trade.RibbitRestockPolicy;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Source-level persistence and entity integration contracts that do not require a game launch. */
class RibbitEconomyPersistenceContractTest {
    private static final Path PROJECT_ROOT = Path.of(System.getProperty("projectRoot"));

    @Test
    void everyEconomyChoiceGateMenuAndRestockFieldRoundTripsWithSafeDefaults() throws IOException {
        String state = read("common/src/main/java/com/yungnickyoung/minecraft/ribbits/entity/trade/RibbitTradeState.java");
        String[] ints = {
                "MynxTradeRank", "MynxTradeXp", "MynxGardenerPair",
                "MynxFarmerTier2Choice", "MynxFarmerTier3Choice",
                 "MynxFishermanAquaticChoice", "MynxFishermanCoralFamily",
                 "MynxChefMasterSpecialty", "MynxSorcererBlessingChoice",
                 "MynxProspectorBullionChoice", "MynxGuardBranch", "MynxRestocksUsedToday",
                 "MynxTradeSchema"
        };
        String[] longs = {"MynxChefMenuDay", "MynxRestockDay", "MynxLastRestockGameTime"};
        String[] booleans = {"MynxFishermanOpalGate", "MynxSorcererBenzeneGate"};
        for (String key : ints) {
            assertEquals(1, occurrences(state, "getIntOr(\"" + key + "\""), key + " safe read");
            assertEquals(1, occurrences(state, "putInt(\"" + key + "\""), key + " write");
        }
        for (String key : longs) {
            assertEquals(1, occurrences(state, "getLongOr(\"" + key + "\""), key + " safe read");
            assertEquals(1, occurrences(state, "putLong(\"" + key + "\""), key + " write");
        }
        for (String key : booleans) {
            assertEquals(1, occurrences(state, "getBooleanOr(\"" + key + "\""), key + " safe read");
            assertEquals(1, occurrences(state, "putBoolean(\"" + key + "\""), key + " write");
        }
        for (int tier = 1; tier <= 4; tier++) {
            assertTrue(state.contains("getIntOr(\"MynxChefTier\" + tier + \"Menu\", -1)"));
            assertTrue(state.contains("putInt(\"MynxChefTier\" + tier + \"Menu\""));
        }
        assertTrue(state.contains("private int rank = 1;"));
        assertTrue(state.contains("private int gardenerPair = -1;"));
        assertTrue(state.contains("private final int[] chefMenus = {-1, -1, -1, -1};"));
        assertTrue(state.contains("private long restockDay = UNSET_DAY;"));
        assertTrue(state.contains("private long lastRestockGameTime = Long.MIN_VALUE;"));
        assertTrue(state.contains("private int tradeSchema;"));
    }

    @Test
    void stateClampsXpAndDailyRestockAllowanceWithoutMutatingPersistentChoices() {
        RibbitTradeState state = new RibbitTradeState();
        assertEquals(1, state.rank());
        assertEquals(0, state.xp());
        assertEquals(-1, state.gardenerPair());
        assertEquals(-1, state.farmerTier2Choice());
        assertEquals(-1, state.farmerTier3Choice());
        assertEquals(-1, state.fishermanAquaticChoice());
        assertEquals(-1, state.fishermanCoralFamily());
        assertEquals(-1, state.chefMasterSpecialty());
        assertEquals(-1, state.sorcererBlessingChoice());
        assertEquals(-1, state.prospectorBullionChoice());
        assertEquals(-1, state.guardBranch());
        assertEquals(RibbitTradeState.UNSET_DAY, state.chefMenuDay());
        assertEquals(RibbitTradeState.UNSET_DAY, state.restockDay());

        state.xp(-50);
        assertEquals(0, state.xp());
        state.restocksUsedToday(-1);
        assertEquals(0, state.restocksUsedToday());
        state.restocksUsedToday(3);
        assertEquals(2, state.restocksUsedToday());

        state.gardenerPair(1);
        state.farmerTier2Choice(1);
        state.farmerTier3Choice(0);
        state.fishermanAquaticChoice(2);
        state.fishermanCoralFamily(4);
        state.chefMasterSpecialty(2);
        state.sorcererBlessingChoice(1);
        state.prospectorBullionChoice(2);
        state.guardBranch(1);
        state.restockDay(99);
        state.lastRestockGameTime(12345);
        assertEquals(List.of(1, 1, 0, 2, 4, 2, 1, 2, 1), List.of(
                state.gardenerPair(), state.farmerTier2Choice(), state.farmerTier3Choice(),
                state.fishermanAquaticChoice(), state.fishermanCoralFamily(),
                state.chefMasterSpecialty(), state.sorcererBlessingChoice(),
                state.prospectorBullionChoice(), state.guardBranch()));
        assertEquals(99, state.restockDay());
        assertEquals(12345, state.lastRestockGameTime());
    }

    @Test
    void homeSettingUsesOnlyThePermanentToadstoolHeartAndPreservesSavedCoordinates() throws IOException {
        String entity = read("common/src/main/java/com/yungnickyoung/minecraft/ribbits/entity/RibbitEntity.java");
        String interaction = between(entity,
                "public @NotNull InteractionResult mobInteract",
                "public void reassessGoals()");

        assertTrue(interaction.contains("player.isSecondaryUseActive()"));
        assertTrue(interaction.contains("itemStack.is(ItemModule.TOADSTOOL_HEART.get())"));
        assertTrue(interaction.contains("this.homePosition = this.blockPosition();"));
        assertTrue(interaction.contains("this.level().broadcastEntityEvent(this, (byte) 12);"));
        assertTrue(interaction.contains("if (!player.getAbilities().instabuild)"));
        assertTrue(interaction.contains("itemStack.shrink(1);"));
        assertFalse(interaction.contains("AMETHYST_SHARD"));
        assertTrue(interaction.indexOf("if (this.level().isClientSide())")
                        < interaction.indexOf("boolean bl = this.getOffers().isEmpty();"),
                "client interactions must not resolve server-owned fixed-stack offers");

        assertTrue(entity.contains("if (flag == 12)"));
        assertTrue(entity.contains("this.addParticlesAroundSelf(ParticleTypes.HEART);"));
        for (String coordinate : List.of("X", "Y", "Z")) {
            assertTrue(entity.contains("valueInput.getInt(\"HomePos" + coordinate + "\")"));
            assertTrue(entity.contains("valueOutput.putInt(\"HomePos" + coordinate + "\""));
        }
        assertFalse(entity.contains("Items.AMETHYST_SHARD"));
    }

    @Test
    void serializedOffersRanksAndCustomTitlesUseTheNormalMerchantFramework() throws IOException {
        String entity = read("common/src/main/java/com/yungnickyoung/minecraft/ribbits/entity/RibbitEntity.java");
        String trades = read("common/src/main/java/com/yungnickyoung/minecraft/ribbits/module/RibbitTradeModule.java");
        assertTrue(entity.contains("valueInput.read(\"Offers\", MerchantOffers.CODEC)"));
        assertTrue(entity.contains("valueOutput.store(\"Offers\", MerchantOffers.CODEC, offersToSave)"));
        assertTrue(entity.contains("RibbitTradeModule.restoreStrictComponentMatching(this, offers)"));
        assertTrue(trades.contains("CURRENT_TRADE_SCHEMA = 1"));
        assertTrue(trades.contains("tradeSchema() != CURRENT_TRADE_SCHEMA"));
        assertTrue(trades.contains("StrictMerchantOffer.restoreFromTemplate"));
        assertTrue(entity.contains("Component title = this.hasCustomName() ? this.getDisplayName() : RibbitTradeModule.title(this);"));
        assertTrue(entity.contains("this.openTradingScreen(player, title, displayLevel);"));
        assertTrue(entity.contains("return RibbitTradeModule.profile(this.getRibbitData().getProfession()).tiered();"));
        assertTrue(entity.contains("RibbitTradeModule.addUnlockedTier(this, tier);"));
        assertTrue(entity.contains("this.tradeState.write(valueOutput);"));
        assertTrue(entity.contains("this.tradeState.read(valueInput,"));
    }

    @Test
    void strictDailyRestockingIsPersistentExhaustionGatedAndBoundedToThreeBatches() throws IOException {
        String entity = read("common/src/main/java/com/yungnickyoung/minecraft/ribbits/entity/RibbitEntity.java");
        String policy = read("common/src/main/java/com/yungnickyoung/minecraft/ribbits/entity/trade/RibbitRestockPolicy.java");
        String restock = between(entity,
                "public long currentRestockDay()",
                "public boolean isTrading()");

        assertTrue(restock.contains("Math.floorDiv(this.level().getOverworldClockTime(), 24000L)"));
        assertTrue(restock.contains("Math.max(current, this.tradeState.restockDay())"),
                "backward time changes cannot manufacture new days");
        assertTrue(restock.contains("RibbitRestockPolicy.mayOrdinarilyRestock("));
        assertTrue(restock.contains("this.needsToRestock(),"));
        assertTrue(restock.contains("this.pendingChefDayChange,"));
        assertTrue(restock.contains("this.tradeState.restocksUsedToday(),"));
        assertTrue(policy.contains("MAX_ADDITIONAL_RESTOCKS = 2"));
        assertTrue(policy.contains("MIN_RESTOCK_SPACING = 2400L"));
        assertTrue(policy.contains("!exhausted || dayChangeDeferred"));
        assertTrue(policy.contains("restocksUsed >= MAX_ADDITIONAL_RESTOCKS"));
        assertTrue(policy.contains("currentGameTime - lastRestockGameTime >= MIN_RESTOCK_SPACING"));
        assertTrue(restock.contains("offer.resetUses();"));
        assertTrue(restock.contains("this.tradeState.restocksUsedToday(0);"));
        assertTrue(restock.contains("this.tradeState.restockDay(day);"));
        assertTrue(restock.contains("this.tradeState.lastRestockGameTime(this.level().getGameTime());"));
        assertFalse(restock.contains("updateDemand"), "fixed barter prices do not use demand");
        assertFalse(restock.toLowerCase(java.util.Locale.ROOT).contains("workstation"));
        assertFalse(restock.contains("getHomePosition"));
    }

    @Test
    void pureRestockPolicyEnforcesExhaustionSpacingDailyCapAndBackwardTimeSafety() {
        assertEquals(2, RibbitRestockPolicy.MAX_ADDITIONAL_RESTOCKS);
        assertEquals(2400L, RibbitRestockPolicy.MIN_RESTOCK_SPACING);

        assertFalse(RibbitRestockPolicy.mayOrdinarilyRestock(false, false, 0, 100, 100));
        assertFalse(RibbitRestockPolicy.mayOrdinarilyRestock(true, true, 0, 100, 100));
        assertTrue(RibbitRestockPolicy.mayOrdinarilyRestock(true, false, 0, 100, 100));
        assertFalse(RibbitRestockPolicy.mayOrdinarilyRestock(true, false, 1, 100, 2499));
        assertTrue(RibbitRestockPolicy.mayOrdinarilyRestock(true, false, 1, 100, 2500));
        assertFalse(RibbitRestockPolicy.mayOrdinarilyRestock(true, false, 2, 100, 10000));
        assertFalse(RibbitRestockPolicy.mayOrdinarilyRestock(true, false, -1, 100, 10000));
        assertFalse(RibbitRestockPolicy.mayOrdinarilyRestock(true, false, 1, 5000, 100));

        assertFalse(RibbitRestockPolicy.beginsFreshDay(RibbitTradeState.UNSET_DAY, 0));
        assertFalse(RibbitRestockPolicy.beginsFreshDay(10, 10));
        assertFalse(RibbitRestockPolicy.beginsFreshDay(10, 9));
        assertTrue(RibbitRestockPolicy.beginsFreshDay(10, 11));
    }

    @Test
    void chefDayBoundaryDefersMenuReplacementUntilTheTradingSessionCloses() throws IOException {
        String entity = read("common/src/main/java/com/yungnickyoung/minecraft/ribbits/entity/RibbitEntity.java");
        String tradeModule = read("common/src/main/java/com/yungnickyoung/minecraft/ribbits/module/RibbitTradeModule.java");

        String boundary = between(entity, "private void handleDailyStockBoundary()", "public boolean isTrading()");
        assertTrue(boundary.contains("if (chef && this.isTrading())"));
        assertTrue(boundary.contains("this.pendingChefDayChange = true;"));
        assertTrue(boundary.contains("return;"));
        assertTrue(entity.contains("if (this.pendingChefDayChange)"));
        assertTrue(entity.contains("this.beginNewStockDay(day);"));
        assertTrue(boundary.contains("RibbitTradeModule.updateChefMenuForDay(this, day);"));
        assertTrue(boundary.contains("RibbitTradeModule.rebuildTrades(this);"));

        String ordinaryRestock = between(entity, "public void restock()", "private void resendOffersToTradingPlayer()");
        assertFalse(ordinaryRestock.contains("updateChefMenuForDay"));
        assertFalse(ordinaryRestock.contains("rebuildTrades"));
        assertFalse(ordinaryRestock.contains("initializePersistentChoices"));

        assertTrue(tradeModule.contains("if (newDay || state.chefMenu(tier) < 0)"));
        assertTrue(tradeModule.contains("int maxDailyTier = Math.min(4, state.rank());"));
        assertTrue(tradeModule.contains("state.chefMenuDay(day);"));
    }

    @Test
    void specializationsAreInitializedOnceAndNeverRerolledByRestockOrTierAdvance() throws IOException {
        String trades = read("common/src/main/java/com/yungnickyoung/minecraft/ribbits/module/RibbitTradeModule.java");
        String entity = read("common/src/main/java/com/yungnickyoung/minecraft/ribbits/entity/RibbitEntity.java");
        assertTrue(trades.contains("return current >= 0 && current < options ? current : random.nextInt(options);"));
        for (String stateField : List.of(
                "gardenerPair", "farmerTier2Choice", "farmerTier3Choice",
                "fishermanAquaticChoice", "fishermanCoralFamily", "chefMasterSpecialty",
                "sorcererBlessingChoice", "prospectorBullionChoice", "guardBranch")) {
            assertTrue(trades.contains("state." + stateField + "("), stateField + " assignment");
            assertTrue(trades.contains("validOrRoll(state." + stateField + "()"), stateField + " stable choice");
        }
        String restock = between(entity, "public void restock()", "private void resendOffersToTradingPlayer()");
        assertFalse(restock.contains("initializePersistentChoices"));
        assertFalse(restock.contains("updateTrades"));
        assertFalse(restock.contains("rebuildTrades"));
        assertTrue(trades.contains("public static void addUnlockedTier"));
        assertTrue(trades.contains("if (spec.tier == tier)"));
    }

    @Test
    void placeholderModelsDoNotChangeEitherPermanentItemsMechanicalIdentity() throws IOException {
        String itemModule = read("common/src/main/java/com/yungnickyoung/minecraft/ribbits/module/ItemModule.java");
        String glowcap = read("common/src/main/resources/assets/ribbits/items/glowcap.json");
        String heart = read("common/src/main/resources/assets/ribbits/items/toadstool_heart.json");
        String recipe = read("common/src/main/java/com/yungnickyoung/minecraft/ribbits/recipe/ToadstoolHeartRecipe.java");

        assertTrue(glowcap.contains("\"model\": \"minecraft:item/warped_fungus\""));
        assertTrue(heart.contains("\"model\": \"minecraft:item/heart_container\""));
        assertEquals(1, occurrences(itemModule, "@AutoRegister(\"glowcap\")"));
        assertEquals(1, occurrences(itemModule, "@AutoRegister(\"toadstool_heart\")"));
        String glowcapRegistration = between(itemModule,
                "public static final AutoRegisterItem GLOWCAP",
                "@AutoRegister(\"toadstool_heart\")");
        String heartRegistration = between(itemModule,
                "public static final AutoRegisterItem TOADSTOOL_HEART",
                "@AutoRegister(\"giant_lilypad\")");
        for (String registration : List.of(glowcapRegistration, heartRegistration)) {
            assertTrue(registration.contains("AutoRegisterItem.of(() -> new Item("));
            assertTrue(registration.contains("new Item.Properties().stacksTo(64)"));
            assertFalse(registration.contains("BlockItem"));
            assertFalse(registration.contains("FoodProperties"));
            assertFalse(registration.contains("Consumable"));
        }
        assertTrue(recipe.contains("return new ItemStack(ItemModule.TOADSTOOL_HEART.get(), 1);"));
        assertFalse(recipe.contains("return originalCrystalHeartStack()"));
    }

    private static String read(String relativePath) throws IOException {
        return Files.readString(PROJECT_ROOT.resolve(relativePath)).replace("\r\n", "\n");
    }

    private static String between(String value, String startNeedle, String endNeedle) {
        int start = value.indexOf(startNeedle);
        int end = value.indexOf(endNeedle, start + startNeedle.length());
        assertTrue(start >= 0, "missing start marker " + startNeedle);
        assertTrue(end > start, "missing end marker " + endNeedle);
        return value.substring(start, end);
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
