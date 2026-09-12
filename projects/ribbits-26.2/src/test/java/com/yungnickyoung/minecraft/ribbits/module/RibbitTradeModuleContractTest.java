package com.yungnickyoung.minecraft.ribbits.module;

import com.yungnickyoung.minecraft.ribbits.entity.trade.MatchaStackCatalog.FixedStack;
import com.yungnickyoung.minecraft.ribbits.entity.trade.RibbitTradeState;
import com.yungnickyoung.minecraft.ribbits.module.RibbitTradeModule.Gate;
import com.yungnickyoung.minecraft.ribbits.module.RibbitTradeModule.TradeOfferSpec;
import com.yungnickyoung.minecraft.ribbits.module.RibbitTradeModule.TradeProfile;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Source-safe exhaustive contract coverage for the declarative Mynx economy. */
class RibbitTradeModuleContractTest {
    private static final Map<String, ExpectedOffer> EXPECTED = expectedOffers();

    @Test
    void everyConcreteOfferHasTheApprovedExactDescriptor() {
        List<TradeOfferSpec> actual = RibbitTradeModule.allOffers();
        Map<String, TradeOfferSpec> byId = new LinkedHashMap<>();
        for (TradeOfferSpec spec : actual) {
            TradeOfferSpec duplicate = byId.put(spec.id(), spec);
            assertEquals(null, duplicate, "duplicate concrete offer ID " + spec.id());
        }

        assertEquals(129, actual.size(), "complete concrete descriptor count");
        assertEquals(EXPECTED.keySet(), byId.keySet(), "missing, extra, or reordered offer descriptors");
        for (Map.Entry<String, ExpectedOffer> entry : EXPECTED.entrySet()) {
            String id = entry.getKey();
            ExpectedOffer expected = entry.getValue();
            TradeOfferSpec spec = byId.get(id);
            assertNotNull(spec, id);
            assertEquals(expected.profession, spec.profession(), id + " profession");
            assertEquals(expected.tier, spec.tier(), id + " tier");
            assertCost(expected.first, spec.first(), id + " first input");
            if (expected.second == null) {
                assertEquals(null, spec.second(), id + " second input");
            } else {
                assertCost(expected.second, spec.second(), id + " second input");
            }
            assertEquals(expected.resultIdentity, spec.result().auditIdentity(), id + " result identity");
            assertEquals(expected.resultCount, spec.resultCount(), id + " result count");
            assertEquals(expected.maxUses, spec.maxUses(), id + " max uses");
            assertEquals(expectedXp(expected.profession, expected.tier), spec.merchantXp(), id + " XP");
            assertEquals(expected.selection, spec.selection(), id + " selection policy");
            assertEquals(expected.option, spec.option(), id + " selection option");
            assertEquals(expected.gate, spec.gate(), id + " gate");
        }
        assertEquals(0.0F, RibbitTradeModule.FIXED_PRICE_MULTIPLIER,
                "barter prices must never be demand-adjusted");
    }

    @Test
    void descriptorCountsMatchEveryProfessionAndApprovedGroupedExpansion() {
        assertEquals(Map.of(
                        "gardener", 7L,
                        "farmer", 7L,
                        "fisherman", 22L,
                        "merchant", 5L,
                        "nitwit", 1L,
                        "chef", 16L,
                        "sorcerer", 21L,
                        "prospector", 18L,
                        "guard", 32L
                ), countsBy(TradeOfferSpec::profession));

        assertEquals(16, selected("sorcerer", "portal_catalyst").size());
        assertEquals(Set.of(
                        "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink",
                        "gray", "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"),
                catalystColors());
        assertEquals(15, selected("fisherman", "_coral").size());
        for (String family : List.of("tube", "brain", "bubble", "fire", "horn")) {
            assertEquals(Set.of(
                            "fisherman_" + family + "_coral_block",
                            "fisherman_" + family + "_coral_fan",
                            "fisherman_" + family + "_coral"),
                    idsWithSelectionOption("fisherman_coral", List.of("tube", "brain", "bubble", "fire", "horn").indexOf(family)));
        }

        Set<String> prospectorTools = new HashSet<>(List.of(
                "prospector_golden_shovel", "prospector_iron_shovel",
                "prospector_golden_hoe", "prospector_iron_hoe",
                "prospector_golden_axe", "prospector_iron_axe",
                "prospector_golden_pickaxe", "prospector_iron_pickaxe",
                "prospector_diamond_shovel", "prospector_diamond_hoe",
                "prospector_diamond_pickaxe", "prospector_diamond_axe"));
        assertEquals(prospectorTools, idsMatching(spec -> spec.profession().equals("prospector")
                && spec.id().matches("prospector_(golden|iron|diamond)_(shovel|hoe|axe|pickaxe)")));

        assertEquals(29, RibbitTradeModule.allOffers().stream()
                .filter(spec -> spec.profession().equals("guard") && spec.selection().equals("guard_branch"))
                .count());
        assertTrue(EXPECTED.containsKey("guard_saddle"), "the approved Saddle row is present");
        assertEquals(3, RibbitTradeModule.allOffers().stream()
                .filter(spec -> spec.profession().equals("merchant") && spec.id().endsWith("_froglight"))
                .count());
    }

    @Test
    void maximumRankInventoriesMaterializeOneChoicePerPolicyWithoutBranchMixing() throws Exception {
        assertMaterializedCount("gardener", state -> state.gardenerPair(0), 5);
        assertMaterializedCount("gardener", state -> state.gardenerPair(1), 5);

        for (int tier2 = 0; tier2 < 2; tier2++) {
            for (int tier3 = 0; tier3 < 2; tier3++) {
                int a = tier2;
                int b = tier3;
                assertMaterializedCount("farmer", state -> {
                    state.farmerTier2Choice(a);
                    state.farmerTier3Choice(b);
                }, 5);
            }
        }

        for (int aquatic = 0; aquatic < 3; aquatic++) {
            for (int coral = 0; coral < 5; coral++) {
                int a = aquatic;
                int c = coral;
                List<TradeOfferSpec> selected = materialized("fisherman", state -> {
                    state.fishermanAquaticChoice(a);
                    state.fishermanCoralFamily(c);
                });
                assertEquals(8, selected.size());
                List<TradeOfferSpec> coralSet = selected.stream()
                        .filter(spec -> spec.selection().equals("fisherman_coral"))
                        .toList();
                assertEquals(3, coralSet.size());
                assertTrue(coralSet.stream().allMatch(spec -> spec.option() == c),
                        "one linked living coral family only");
            }
        }

        assertMaterializedCount("merchant", state -> { }, 5);
        assertMaterializedCount("nitwit", state -> { }, 1);
        for (int tier1 = 0; tier1 < 2; tier1++) {
            for (int tier2 = 0; tier2 < 2; tier2++) {
                for (int tier3 = 0; tier3 < 5; tier3++) {
                    for (int tier4 = 0; tier4 < 2; tier4++) {
                        for (int master = 0; master < 3; master++) {
                            int a = tier1;
                            int b = tier2;
                            int c = tier3;
                            int d = tier4;
                            int e = master;
                            assertMaterializedCount("chef", state -> {
                                state.chefMenu(1, a);
                                state.chefMenu(2, b);
                                state.chefMenu(3, c);
                                state.chefMenu(4, d);
                                state.chefMasterSpecialty(e);
                            }, 7);
                        }
                    }
                }
            }
        }
        for (int blessing = 0; blessing < 2; blessing++) {
            int choice = blessing;
            assertMaterializedCount("sorcerer", state -> state.sorcererBlessingChoice(choice), 20);
        }
        for (int bullion = 0; bullion < 3; bullion++) {
            int choice = bullion;
            assertMaterializedCount("prospector", state -> state.prospectorBullionChoice(choice), 16);
        }

        int[] expectedGuardCounts = {11, 12, 15};
        for (int branch = 0; branch < 3; branch++) {
            int choice = branch;
            List<TradeOfferSpec> selected = materialized("guard", state -> state.guardBranch(choice));
            assertEquals(expectedGuardCounts[branch], selected.size());
            List<TradeOfferSpec> branchSpecific = selected.stream()
                    .filter(spec -> spec.selection().equals("guard_branch"))
                    .toList();
            assertTrue(branchSpecific.stream().allMatch(spec -> spec.option() == choice),
                    "Guard branch " + branch + " must not mix equipment families");
            assertTrue(selected.stream().anyMatch(spec -> spec.id().equals("guard_iron_chains")));
            assertTrue(selected.stream().anyMatch(spec -> spec.id().equals("guard_copper_chains")));
            assertTrue(selected.stream().anyMatch(spec -> spec.id().equals("guard_anvil")));
        }
        assertTrue(materialized("guard", state -> state.guardBranch(1)).stream()
                .anyMatch(spec -> spec.id().equals("guard_saddle")));
    }

    @Test
    void allRankNamesThresholdsAndMaximumTiersAreExact() {
        assertArrayEquals(new int[]{0, 5, 15, 30, 50}, RibbitTradeModule.XP_THRESHOLDS);
        assertProfile("gardener", 2, "Sprout Tender", "Toadstool Keeper");
        assertProfile("farmer", 3, "Vine Puller", "Root Wrangler", "Mudfield Steward");
        assertProfile("fisherman", 5, "Pond Forager", "Coral Keeper", "Amphibian Attendant",
                "Opal Angler", "Monument Mariner");
        assertProfile("merchant", 3, "Moss Peddler", "Lantern Trader", "Glowgoods Baron");
        assertProfile("chef", 5, "Tadpole Cook", "Pond Cook", "Swamp Chef", "Grand Chef",
                "Master of the Feast");
        assertProfile("sorcerer", 4, "Wart Whisperer", "Gatecaller", "Flask Sage", "Deep-Pond Oracle");
        assertProfile("prospector", 3, "Pebble Picker", "Vein-Seeker", "Deep Delver");
        assertProfile("guard", 4, "Pond Sentry", "Lily Warden", "Marsh Marshal", "Bulwark of the Bog");
        assertProfile("nitwit", 0, "Musician");
        assertFalse(RibbitTradeModule.profile("nitwit").tiered(), "Musician remains unranked");
        assertTrue(RibbitTradeModule.profiles().entrySet().stream()
                .filter(entry -> !entry.getKey().equals("nitwit"))
                .allMatch(entry -> entry.getValue().tiered()));
    }

    @Test
    void rankProgressionUsesFiveTradesPerTierHonorsGatesAndPreservesEarnedXp() {
        for (String profession : List.of("gardener", "farmer", "merchant", "chef", "prospector", "guard")) {
            TradeProfile profile = RibbitTradeModule.profile(profession);
            RibbitTradeState state = new RibbitTradeState();
            for (int tier = 1; tier <= profile.maxTier(); tier++) {
                assertEquals(tier, RibbitTradeModule.rankForXp(profile, state,
                        RibbitTradeModule.XP_THRESHOLDS[tier - 1]), profession + " tier " + tier);
            }
        }

        TradeProfile chef = RibbitTradeModule.profile("chef");
        RibbitTradeState ordinary = new RibbitTradeState();
        int xp = 0;
        for (int trade = 0; trade < 5; trade++) xp += 1;
        assertEquals(2, RibbitTradeModule.rankForXp(chef, ordinary, xp),
                "five tier-one trades unlock rank two");
        for (int trade = 0; trade < 5; trade++) xp += 2;
        assertEquals(3, RibbitTradeModule.rankForXp(chef, ordinary, xp),
                "five tier-two trades unlock rank three");
        for (int trade = 0; trade < 5; trade++) xp += 3;
        assertEquals(4, RibbitTradeModule.rankForXp(chef, ordinary, xp),
                "five tier-three trades unlock rank four");
        for (int trade = 0; trade < 5; trade++) xp += 4;
        assertEquals(5, RibbitTradeModule.rankForXp(chef, ordinary, xp),
                "five tier-four trades unlock rank five");

        assertEquals(2, RibbitTradeModule.rankForXp(RibbitTradeModule.profile("gardener"),
                new RibbitTradeState(), 50), "lower-rank professions stop at their own maximum");
        assertEquals(3, RibbitTradeModule.rankForXp(RibbitTradeModule.profile("farmer"),
                new RibbitTradeState(), 50), "three-tier professions stop at rank three");

        RibbitTradeState sorcerer = new RibbitTradeState();
        assertEquals(1, RibbitTradeModule.rankForXp(RibbitTradeModule.profile("sorcerer"), sorcerer, 50));
        sorcerer.sorcererBenzeneGate(true);
        RibbitTradeModule.normalizePersistentState(RibbitTradeModule.profile("sorcerer"), sorcerer);
        assertEquals(2, sorcerer.rank(), "Benzene promotion reaches rank two");
        assertEquals(5, sorcerer.xp(), "Benzene promotion uses the new rank-two floor");
        assertEquals(4, RibbitTradeModule.rankForXp(RibbitTradeModule.profile("sorcerer"), sorcerer, 50));

        RibbitTradeState fisherman = new RibbitTradeState();
        assertEquals(4, RibbitTradeModule.rankForXp(RibbitTradeModule.profile("fisherman"), fisherman, 50));
        fisherman.fishermanOpalGate(true);
        RibbitTradeModule.normalizePersistentState(RibbitTradeModule.profile("fisherman"), fisherman);
        assertEquals(5, fisherman.rank(), "Opal promotion reaches rank five");
        assertEquals(50, fisherman.xp(), "Opal promotion uses the new rank-five floor");
        assertEquals(5, RibbitTradeModule.rankForXp(RibbitTradeModule.profile("fisherman"), fisherman, 50));
        assertEquals(0, RibbitTradeModule.rankForXp(RibbitTradeModule.profile("nitwit"), new RibbitTradeState(), 50));

        RibbitTradeState savedLowerThreshold = new RibbitTradeState();
        savedLowerThreshold.rank(1);
        savedLowerThreshold.xp(8);
        RibbitTradeModule.normalizePersistentState(RibbitTradeModule.profile("gardener"), savedLowerThreshold);
        assertEquals(2, savedLowerThreshold.rank(), "saved earned XP unlocks its newly eligible tier");
        assertEquals(8, savedLowerThreshold.xp(), "saved XP is never discarded by a lowered threshold");

        RibbitTradeState savedMaximum = new RibbitTradeState();
        savedMaximum.rank(5);
        savedMaximum.xp(100);
        RibbitTradeModule.normalizePersistentState(chef, savedMaximum);
        assertEquals(5, savedMaximum.rank());
        assertEquals(100, savedMaximum.xp(), "a former maximum XP value is preserved");

        RibbitTradeState ui = new RibbitTradeState();
        ui.rank(1);
        ui.xp(4);
        assertEquals(8, RibbitTradeModule.uiXpFor(ui, chef));
        ui.rank(2);
        ui.xp(5);
        assertEquals(10, RibbitTradeModule.uiXpFor(ui, chef));
        ui.rank(3);
        ui.xp(15);
        assertEquals(70, RibbitTradeModule.uiXpFor(ui, chef));
    }

    @Test
    void chefSelectionsAreStableAndCycleEveryPoolBeforeRepeating() {
        List<UUID> chefs = List.of(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                UUID.fromString("2a650718-ec62-568f-8dff-71258a4d6f3f"),
                UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff"));
        int[] pools = {2, 2, 5, 2};
        for (UUID chef : chefs) {
            for (int tier = 1; tier <= 4; tier++) {
                int pool = pools[tier - 1];
                for (long startingDay : List.of(-3L, 0L, 17777L)) {
                    Set<Integer> cycle = new HashSet<>();
                    for (int offset = 0; offset < pool; offset++) {
                        int selection = RibbitTradeModule.deterministicChefMenu(
                                chef, tier, startingDay + offset, pool);
                        assertEquals(selection, RibbitTradeModule.deterministicChefMenu(
                                chef, tier, startingDay + offset, pool), "same Chef/tier/day stability");
                        assertTrue(selection >= 0 && selection < pool);
                        cycle.add(selection);
                    }
                    assertEquals(pool, cycle.size(), "complete pool before repeat");
                    assertEquals(
                            RibbitTradeModule.deterministicChefMenu(chef, tier, startingDay, pool),
                            RibbitTradeModule.deterministicChefMenu(chef, tier, startingDay + pool, pool),
                            "cycle repeats only after every option");
                }
            }
        }
    }

    @Test
    void exactComponentInputsAreNarrowAndEquipmentInputsRemainComponentTolerant() {
        Set<String> exactInputs = new HashSet<>();
        for (TradeOfferSpec spec : RibbitTradeModule.allOffers()) {
            if (spec.first().exactComponents()) {
                exactInputs.add(spec.id() + ":first");
            }
            if (spec.second() != null && spec.second().exactComponents()) {
                exactInputs.add(spec.id() + ":second");
            }
        }
        assertEquals(Set.of(
                "fisherman_opal_gate:first",
                "sorcerer_benzene_gate:first",
                "prospector_electrum:second"), exactInputs);

        TradeOfferSpec electrum = offer("prospector_electrum");
        assertEquals("recipe:crafting:divine_fragment", electrum.second().stack().auditIdentity());
        assertEquals(1, electrum.second().count());
        assertTrue(electrum.second().exactComponents(), "plain Turtle Scutes must not match");
        assertEquals("recipe:crafting:electrum_alloy", electrum.result().auditIdentity());
        assertEquals("crafting:divine_fragment", FixedStack.DIVINE_FRAGMENT.recipeId().toString());
        assertEquals("minecraft:turtle_scute", FixedStack.DIVINE_FRAGMENT.expectedItemId().toString());

        assertTrue(RibbitTradeModule.allOffers().stream()
                .filter(spec -> spec.profession().equals("prospector") || spec.profession().equals("guard"))
                .filter(spec -> spec.result().auditIdentity().equals("ribbits:glowcap"))
                .filter(spec -> !spec.id().equals("prospector_electrum"))
                .allMatch(spec -> !spec.first().exactComponents()),
                "tool, weapon, mount, and armor buybacks accept damage and extra components");
    }

    @Test
    void failedMapRedemptionIsTheOnlyMarkerMatchedOfferAndIsRankIndependent() throws Exception {
        TradeOfferSpec redemption = offer("sorcerer_failed_map_redemption");
        assertEquals("sorcerer", redemption.profession());
        assertEquals(0, redemption.tier());
        assertEquals("minecraft:map", redemption.first().stack().auditIdentity());
        assertEquals(1, redemption.first().count());
        assertFalse(redemption.first().exactComponents());
        assertTrue(redemption.first().failedMapMarkerOnly());
        assertEquals(null, redemption.second());
        assertEquals("ribbits:toadstool_heart", redemption.result().auditIdentity());
        assertEquals(1, redemption.resultCount());
        assertEquals(16, redemption.maxUses());
        assertEquals(0, redemption.merchantXp());
        assertEquals(Gate.NONE, redemption.gate());

        assertEquals(List.of("sorcerer_failed_map_redemption"),
                RibbitTradeModule.allOffers().stream()
                        .filter(spec -> spec.first().failedMapMarkerOnly()
                                || (spec.second() != null && spec.second().failedMapMarkerOnly()))
                        .map(TradeOfferSpec::id)
                        .toList());

        Method isSelected = RibbitTradeModule.class.getDeclaredMethod(
                "isSelected", TradeOfferSpec.class, RibbitTradeState.class);
        isSelected.setAccessible(true);
        for (int rank = 1; rank <= 4; rank++) {
            RibbitTradeState state = new RibbitTradeState();
            state.rank(rank);
            assertTrue(redemption.tier() <= rank && (boolean) isSelected.invoke(null, redemption, state),
                    "redemption remains selected at Sorcerer rank " + rank);
        }
    }

    @Test
    void allCurrencyReferencesUseThePermanentGlowcapItemNotItsWarpedFungusVisual() {
        for (TradeOfferSpec spec : RibbitTradeModule.allOffers()) {
            List<String> identities = new ArrayList<>();
            identities.add(spec.first().stack().auditIdentity());
            if (spec.second() != null) {
                identities.add(spec.second().stack().auditIdentity());
            }
            identities.add(spec.result().auditIdentity());
            assertFalse(identities.contains("minecraft:amethyst_shard"), spec.id());
            assertFalse(identities.contains("minecraft:warped_fungus"), spec.id());
        }
        assertTrue(RibbitTradeModule.allOffers().stream().anyMatch(spec ->
                spec.first().stack().auditIdentity().equals("ribbits:glowcap")));
        assertTrue(RibbitTradeModule.allOffers().stream().anyMatch(spec ->
                spec.result().auditIdentity().equals("ribbits:glowcap")));
    }

    private static void assertCost(ExpectedCost expected, RibbitTradeModule.CostSpec actual, String label) {
        assertEquals(expected.identity, actual.stack().auditIdentity(), label + " identity");
        assertEquals(expected.count, actual.count(), label + " count");
        assertEquals(expected.exact, actual.exactComponents(), label + " component policy");
    }

    private static void assertProfile(String profession, int maximumTier, String... rankNames) {
        TradeProfile profile = RibbitTradeModule.profile(profession);
        assertEquals(maximumTier, profile.maxTier(), profession + " maximum tier");
        assertEquals(List.of(rankNames), profile.rankNames(), profession + " rank names");
        assertEquals(rankNames.length, profile.titleKeys().size(), profession + " title key count");
        for (int tier = 1; tier <= rankNames.length; tier++) {
            String expectedKey = profession.equals("nitwit")
                    ? "entity.ribbits.merchant.nitwit.musician"
                    : "entity.ribbits.merchant." + profession + ".tier_" + tier;
            assertEquals(expectedKey, profile.titleKeys().get(tier - 1));
        }
    }

    private static int expectedXp(String profession, int tier) {
        int maximumTier = RibbitTradeModule.profile(profession).maxTier();
        return tier <= 0 || tier == maximumTier ? 0 : tier;
    }

    private static TradeOfferSpec offer(String id) {
        return RibbitTradeModule.allOffers().stream()
                .filter(spec -> spec.id().equals(id))
                .findFirst()
                .orElseThrow();
    }

    private static Map<String, Long> countsBy(java.util.function.Function<TradeOfferSpec, String> classifier) {
        Map<String, Long> counts = new HashMap<>();
        for (TradeOfferSpec spec : RibbitTradeModule.allOffers()) {
            counts.merge(classifier.apply(spec), 1L, Long::sum);
        }
        return counts;
    }

    private static List<TradeOfferSpec> selected(String profession, String idFragment) {
        return RibbitTradeModule.allOffers().stream()
                .filter(spec -> spec.profession().equals(profession) && spec.id().contains(idFragment))
                .toList();
    }

    private static Set<String> catalystColors() {
        Set<String> colors = new HashSet<>();
        for (TradeOfferSpec spec : selected("sorcerer", "portal_catalyst")) {
            String prefix = "customportals:";
            String suffix = "_portal_catalyst";
            String identity = spec.result().auditIdentity();
            assertTrue(identity.startsWith(prefix) && identity.endsWith(suffix));
            colors.add(identity.substring(prefix.length(), identity.length() - suffix.length()));
        }
        return colors;
    }

    private static Set<String> idsWithSelectionOption(String selection, int option) {
        return idsMatching(spec -> spec.selection().equals(selection) && spec.option() == option);
    }

    private static Set<String> idsMatching(java.util.function.Predicate<TradeOfferSpec> predicate) {
        Set<String> ids = new HashSet<>();
        RibbitTradeModule.allOffers().stream().filter(predicate).forEach(spec -> ids.add(spec.id()));
        return ids;
    }

    private static void assertMaterializedCount(String profession,
                                                java.util.function.Consumer<RibbitTradeState> choices,
                                                int expected) throws Exception {
        assertEquals(expected, materialized(profession, choices).size(), profession);
    }

    private static List<TradeOfferSpec> materialized(String profession,
                                                      java.util.function.Consumer<RibbitTradeState> choices)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        RibbitTradeState state = new RibbitTradeState();
        state.rank(Math.max(0, RibbitTradeModule.profile(profession).maxTier()));
        choices.accept(state);
        Method isSelected = RibbitTradeModule.class.getDeclaredMethod(
                "isSelected", TradeOfferSpec.class, RibbitTradeState.class);
        isSelected.setAccessible(true);
        List<TradeOfferSpec> result = new ArrayList<>();
        for (TradeOfferSpec spec : RibbitTradeModule.allOffers()) {
            if (spec.profession().equals(profession)
                    && spec.tier() <= state.rank()
                    && (boolean) isSelected.invoke(null, spec, state)) {
                result.add(spec);
            }
        }
        return result;
    }

    private static Map<String, ExpectedOffer> expectedOffers() {
        LinkedHashMap<String, ExpectedOffer> offers = new LinkedHashMap<>();

        choice(offers, "gardener_pair_a_toadstool", "gardener", 1, item("minecraft:red_mushroom", 1), null,
                "ribbits:toadstool", 4, 16, "gardener_pair", 0);
        choice(offers, "gardener_pair_a_daisy", "gardener", 1, item("minecraft:oxeye_daisy", 1), null,
                "ribbits:swamp_daisy", 4, 16, "gardener_pair", 0);
        choice(offers, "gardener_pair_b_lily", "gardener", 1, item("minecraft:lily_pad", 1), null,
                "ribbits:giant_lilypad", 4, 16, "gardener_pair", 1);
        choice(offers, "gardener_pair_b_umbrella", "gardener", 1, item("minecraft:small_dripleaf", 1), null,
                "ribbits:umbrella_leaf", 4, 16, "gardener_pair", 1);
        add(offers, "gardener_red_blocks", "gardener", 2, glowcaps(1), null,
                "ribbits:red_toadstool", 16, 16);
        add(offers, "gardener_brown_blocks", "gardener", 2, glowcaps(1), null,
                "ribbits:brown_toadstool", 16, 16);
        add(offers, "gardener_stems", "gardener", 2, glowcaps(1), null,
                "ribbits:toadstool_stem", 16, 16);

        add(offers, "farmer_vines", "farmer", 1, glowcaps(1), null, "minecraft:vine", 32, 16);
        add(offers, "farmer_hanging_roots", "farmer", 1, glowcaps(1), null, "minecraft:hanging_roots", 32, 16);
        choice(offers, "farmer_muddy_roots", "farmer", 2, glowcaps(1), null,
                "minecraft:muddy_mangrove_roots", 16, 16, "farmer_tier2", 0);
        choice(offers, "farmer_rooted_dirt", "farmer", 2, glowcaps(1), null,
                "minecraft:rooted_dirt", 16, 16, "farmer_tier2", 1);
        choice(offers, "farmer_coarse_dirt", "farmer", 3, glowcaps(1), null,
                "minecraft:coarse_dirt", 16, 16, "farmer_tier3", 0);
        choice(offers, "farmer_mud", "farmer", 3, glowcaps(1), null,
                "minecraft:mud", 16, 16, "farmer_tier3", 1);
        add(offers, "farmer_packed_mud", "farmer", 3, glowcaps(1), null,
                "minecraft:packed_mud", 8, 16);

        String[] aquatic = {"sea_pickle", "kelp", "seagrass"};
        for (int i = 0; i < aquatic.length; i++) {
            choice(offers, "fisherman_" + aquatic[i], "fisherman", 1, glowcaps(1), null,
                    "minecraft:" + aquatic[i], 16, 16, "fisherman_aquatic", i);
        }
        String[] corals = {"tube", "brain", "bubble", "fire", "horn"};
        for (int i = 0; i < corals.length; i++) {
            String family = corals[i];
            choice(offers, "fisherman_" + family + "_coral_block", "fisherman", 2,
                    glowcaps(1), null, "minecraft:" + family + "_coral_block", 8, 16,
                    "fisherman_coral", i);
            choice(offers, "fisherman_" + family + "_coral_fan", "fisherman", 2,
                    glowcaps(1), null, "minecraft:" + family + "_coral_fan", 16, 16,
                    "fisherman_coral", i);
            choice(offers, "fisherman_" + family + "_coral", "fisherman", 2,
                    glowcaps(1), null, "minecraft:" + family + "_coral", 16, 16,
                    "fisherman_coral", i);
        }
        add(offers, "fisherman_tadpole_bucket", "fisherman", 3, glowcaps(1), null,
                "minecraft:tadpole_bucket", 1, 4);
        add(offers, "fisherman_axolotl_bucket", "fisherman", 3, glowcaps(1), null,
                "minecraft:axolotl_bucket", 1, 4);
        add(offers, "fisherman_opal_gate", "fisherman", 4,
                exact("loot:minecraft:kleis_items/opal", 1), null, "ribbits:glowcap", 16, 2,
                "always", 0, Gate.FISHERMAN_OPAL);
        add(offers, "fisherman_dry_sponge", "fisherman", 5, glowcaps(1), null,
                "minecraft:sponge", 4, 1);

        add(offers, "merchant_mossy_oak", "merchant", 1, glowcaps(1), null,
                "ribbits:mossy_oak_planks", 16, 16);
        add(offers, "merchant_swamp_lantern", "merchant", 2, glowcaps(1), null,
                "ribbits:swamp_lantern", 8, 16);
        for (String light : List.of("ochre", "verdant", "pearlescent")) {
            add(offers, "merchant_" + light + "_froglight", "merchant", 3, glowcaps(1), null,
                    "minecraft:" + light + "_froglight", 8, 16);
        }
        add(offers, "musician_maraca", "nitwit", 0, glowcaps(8), null,
                "ribbits:maraca", 1, 4);

        fixed(offers, "chef_glow_berry_crumble", 1, 2, "food:glow_berry_crumble", 8, "always", 0);
        fixed(offers, "chef_honey_ginger_tea", 1, 1, "food:honey_ginger_tea", 8, "always", 0);
        fixed(offers, "chef_pickled_carrots", 1, 2, "food:pickled_carrots", 8, "chef_daily_1", 0);
        fixed(offers, "chef_rind_jam", 1, 2, "food:rind_jam", 8, "chef_daily_1", 1);
        fixed(offers, "chef_gimmari", 2, 4, "food:gimmari", 6, "chef_daily_2", 0);
        fixed(offers, "chef_bokguk", 2, 4, "food:bokguk", 6, "chef_daily_2", 1);
        String[] tier3Foods = {"golden_pickled_carrots", "melon_sorbet", "pumpkin_empanada",
                "warped_pizza", "warped_stroganoff"};
        for (int i = 0; i < tier3Foods.length; i++) {
            fixed(offers, "chef_" + tier3Foods[i], 3, 6, "food:" + tier3Foods[i], 4,
                    "chef_daily_3", i);
        }
        fixed(offers, "chef_sweet_berry_danish", 4, 12, "food:sweet_berry_danish", 2,
                "chef_daily_4", 0);
        fixed(offers, "chef_golden_carrot_cupcake", 4, 12, "food:golden_carrot_cupcake", 2,
                "chef_daily_4", 1);
        fixed(offers, "chef_japanese_curry", 5, 20, "food:japanese_curry", 1,
                "chef_master", 0);
        fixed(offers, "chef_green_curry", 5, 20, "food:green_curry", 1,
                "chef_master", 1);
        fixed(offers, "chef_tonkotsu_ramen", 5, 20, "food:ramen", 1,
                "chef_master", 2);

        add(offers, "sorcerer_failed_map_redemption", "sorcerer", 0,
                item("minecraft:map", 1), null, "ribbits:toadstool_heart", 1, 16);
        add(offers, "sorcerer_benzene_gate", "sorcerer", 1,
                exact("recipe:crafting:benzene", 4), null, "ribbits:glowcap", 1, 16,
                "always", 0, Gate.SORCERER_BENZENE);
        String[] colors = {"white", "orange", "magenta", "light_blue", "yellow", "lime",
                "pink", "gray", "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"};
        for (String color : colors) {
            add(offers, "sorcerer_" + color + "_portal_catalyst", "sorcerer", 2,
                    item("minecraft:ender_pearl", 4), item("minecraft:" + color + "_dye", 1),
                    "customportals:" + color + "_portal_catalyst", 2, 8);
        }
        add(offers, "sorcerer_estus_flask", "sorcerer", 3, glowcaps(2), null,
                "recipe:crafting:estus_flask", 1, 1);
        choice(offers, "sorcerer_reach_blessing", "sorcerer", 4, glowcaps(16), null,
                "recipe:blessings:reach", 1, 1, "sorcerer_blessing", 0);
        choice(offers, "sorcerer_silk_touch_blessing", "sorcerer", 4, glowcaps(16), null,
                "recipe:blessings:silk_touch", 1, 1, "sorcerer_blessing", 1);

        String[][] tier1Tools = {
                {"golden_shovel", "2"}, {"iron_shovel", "2"}, {"golden_hoe", "4"},
                {"iron_hoe", "4"}, {"golden_axe", "8"}, {"iron_axe", "8"},
                {"golden_pickaxe", "8"}, {"iron_pickaxe", "8"}
        };
        for (String[] row : tier1Tools) {
            add(offers, "prospector_" + row[0], "prospector", 1,
                    item("minecraft:" + row[0], 1), null, "ribbits:glowcap",
                    Integer.parseInt(row[1]), 4);
        }
        add(offers, "prospector_glow_lichen", "prospector", 1, glowcaps(1), null,
                "minecraft:glow_lichen", 16, 16);
        String[][] tier2Tools = {
                {"diamond_shovel", "10"}, {"diamond_hoe", "12"},
                {"diamond_pickaxe", "16"}, {"diamond_axe", "16"}
        };
        for (String[] row : tier2Tools) {
            add(offers, "prospector_" + row[0], "prospector", 2,
                    item("minecraft:" + row[0], 1), null, "ribbits:glowcap",
                    Integer.parseInt(row[1]), 2);
        }
        add(offers, "prospector_pointed_dripstone", "prospector", 2, glowcaps(1), null,
                "minecraft:pointed_dripstone", 4, 16);
        add(offers, "prospector_carbon_rich_iron", "prospector", 3, glowcaps(2), null,
                "recipe:crafting:carbon_rich_iron", 1, 8);
        choice(offers, "prospector_hepatizon", "prospector", 3, glowcaps(8), null,
                "recipe:crafting:bronze_alloy", 1, 1, "prospector_bullion", 0);
        choice(offers, "prospector_shakudo", "prospector", 3, glowcaps(16), null,
                "recipe:crafting:shakudo_alloy", 1, 1, "prospector_bullion", 1);
        add(offers, "prospector_electrum", "prospector", 3, glowcaps(16),
                exact("recipe:crafting:divine_fragment", 1), "recipe:crafting:electrum_alloy",
                1, 1, "prospector_bullion", 2, Gate.NONE);

        String[][] weapons1 = {{"golden_spear", "2"}, {"iron_spear", "2"},
                {"golden_sword", "4"}, {"iron_sword", "4"}, {"bow", "1"}};
        String[][] weapons3 = {{"diamond_spear", "4"}, {"diamond_sword", "8"}, {"crossbow", "2"}};
        guard(offers, weapons1, 1, 4, 0);
        guard(offers, weapons3, 3, 2, 0);
        String[][] mount1 = {{"saddle", "4"}, {"golden_nautilus_armor", "4"},
                {"iron_nautilus_armor", "4"}, {"copper_nautilus_armor", "4"},
                {"golden_horse_armor", "4"}, {"iron_horse_armor", "4"},
                {"copper_horse_armor", "4"}};
        String[][] mount3 = {{"diamond_nautilus_armor", "8"}, {"diamond_horse_armor", "8"}};
        guard(offers, mount1, 1, 4, 1);
        guard(offers, mount3, 3, 2, 1);
        String[][] armor1 = {{"golden_helmet", "6"}, {"iron_helmet", "6"},
                {"golden_chestplate", "10"}, {"iron_chestplate", "10"},
                {"golden_leggings", "8"}, {"iron_leggings", "8"},
                {"golden_boots", "4"}, {"iron_boots", "4"}};
        String[][] armor3 = {{"diamond_helmet", "12"}, {"diamond_chestplate", "20"},
                {"diamond_leggings", "16"}, {"diamond_boots", "8"}};
        guard(offers, armor1, 1, 4, 2);
        guard(offers, armor3, 3, 2, 2);
        add(offers, "guard_iron_chains", "guard", 2, glowcaps(1), null,
                "minecraft:iron_chain", 16, 16);
        add(offers, "guard_copper_chains", "guard", 2, glowcaps(1), null,
                "minecraft:copper_chain", 16, 16);
        add(offers, "guard_anvil", "guard", 4, glowcaps(16), null,
                "minecraft:anvil", 1, 1);
        return offers;
    }

    private static void fixed(Map<String, ExpectedOffer> offers, String id, int tier, int glowcaps,
                              String recipe, int maxUses, String selection, int option) {
        add(offers, id, "chef", tier, glowcaps(glowcaps), null, "recipe:" + recipe,
                1, maxUses, selection, option, Gate.NONE);
    }

    private static void guard(Map<String, ExpectedOffer> offers, String[][] rows,
                              int tier, int maxUses, int branch) {
        for (String[] row : rows) {
            choice(offers, "guard_" + row[0], "guard", tier,
                    item("minecraft:" + row[0], 1), null, "ribbits:glowcap",
                    Integer.parseInt(row[1]), maxUses, "guard_branch", branch);
        }
    }

    private static void choice(Map<String, ExpectedOffer> offers, String id, String profession, int tier,
                               ExpectedCost first, ExpectedCost second, String result, int resultCount,
                               int maxUses, String selection, int option) {
        add(offers, id, profession, tier, first, second, result, resultCount, maxUses,
                selection, option, Gate.NONE);
    }

    private static void add(Map<String, ExpectedOffer> offers, String id, String profession, int tier,
                            ExpectedCost first, ExpectedCost second, String result, int resultCount,
                            int maxUses) {
        add(offers, id, profession, tier, first, second, result, resultCount, maxUses,
                "always", 0, Gate.NONE);
    }

    private static void add(Map<String, ExpectedOffer> offers, String id, String profession, int tier,
                            ExpectedCost first, ExpectedCost second, String result, int resultCount,
                            int maxUses, String selection, int option, Gate gate) {
        ExpectedOffer duplicate = offers.put(id, new ExpectedOffer(profession, tier, first, second,
                result, resultCount, maxUses, selection, option, gate));
        if (duplicate != null) {
            throw new IllegalStateException("duplicate expected offer " + id);
        }
    }

    private static ExpectedCost glowcaps(int count) {
        return item("ribbits:glowcap", count);
    }

    private static ExpectedCost item(String identity, int count) {
        return new ExpectedCost(identity, count, false);
    }

    private static ExpectedCost exact(String identity, int count) {
        return new ExpectedCost(identity, count, true);
    }

    private record ExpectedCost(String identity, int count, boolean exact) { }

    private record ExpectedOffer(String profession, int tier, ExpectedCost first, ExpectedCost second,
                                 String resultIdentity, int resultCount, int maxUses, String selection,
                                 int option, Gate gate) { }
}
