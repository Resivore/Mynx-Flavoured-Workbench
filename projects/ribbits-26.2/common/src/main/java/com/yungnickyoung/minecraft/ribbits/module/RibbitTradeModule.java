package com.yungnickyoung.minecraft.ribbits.module;

import com.yungnickyoung.minecraft.ribbits.data.RibbitProfession;
import com.yungnickyoung.minecraft.ribbits.entity.RibbitEntity;
import com.yungnickyoung.minecraft.ribbits.entity.trade.MatchaStackCatalog;
import com.yungnickyoung.minecraft.ribbits.entity.trade.MatchaStackCatalog.FixedStack;
import com.yungnickyoung.minecraft.ribbits.entity.trade.RibbitTradeState;
import com.yungnickyoung.minecraft.ribbits.entity.trade.RibbitExternalTradeOffer;
import com.yungnickyoung.minecraft.ribbits.entity.trade.StrictMerchantOffer;
import com.yungnickyoung.minecraft.ribbits.world.loot.RibbitVillageExplorerMap;
import net.minecraft.core.component.DataComponentExactPredicate;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;
import java.util.UUID;
import java.util.TreeMap;

/** Central, declarative trade-profile layer for the Mynx Ribbit economy. */
public final class RibbitTradeModule {
    public static final int[] XP_THRESHOLDS = {0, 5, 15, 30, 50};
    public static final float FIXED_PRICE_MULTIPLIER = 0.0F;
    public static final int CURRENT_TRADE_SCHEMA = 2;

    public enum Gate {
        NONE,
        SORCERER_BENZENE,
        FISHERMAN_OPAL
    }

    public record TradeProfile(String profession, int maxTier, List<String> rankNames,
                               List<String> titleKeys) {
        public boolean tiered() {
            return this.maxTier > 0;
        }

        public String rankName(int rank) {
            if (!this.tiered()) {
                return this.rankNames.getFirst();
            }
            return this.rankNames.get(Math.max(1, Math.min(rank, this.maxTier)) - 1);
        }

        public String titleKey(int rank) {
            if (!this.tiered()) {
                return this.titleKeys.getFirst();
            }
            return this.titleKeys.get(Math.max(1, Math.min(rank, this.maxTier)) - 1);
        }
    }

    public record StackRef(String registryId, FixedStack fixedStack, boolean opal) {
        public static StackRef item(String id) {
            return new StackRef(id, null, false);
        }

        public static StackRef fixed(FixedStack fixedStack) {
            return new StackRef(null, fixedStack, false);
        }

        public static StackRef opalStack() {
            return new StackRef(null, null, true);
        }

        public ItemStack resolve(Level level) {
            if (this.fixedStack != null) {
                return MatchaStackCatalog.resolve(level, this.fixedStack);
            }
            if (this.opal) {
                return MatchaStackCatalog.opal();
            }
            return new ItemStack(MatchaStackCatalog.requiredItem(this.registryId));
        }

        public String auditIdentity() {
            if (this.fixedStack != null) {
                return "recipe:" + this.fixedStack.recipeId();
            }
            return this.opal ? "loot:minecraft:kleis_items/opal" : this.registryId;
        }
    }

    public record CostSpec(StackRef stack, int count, boolean exactComponents,
                           boolean failedMapMarkerOnly) {
        public CostSpec(StackRef stack, int count, boolean exactComponents) {
            this(stack, count, exactComponents, false);
        }

        public CostSpec {
            if (exactComponents && failedMapMarkerOnly) {
                throw new IllegalArgumentException("A cost cannot use both exact-stack and failed-map matching");
            }
        }

        public ItemCost resolve(Level level) {
            ItemStack resolved = this.failedMapMarkerOnly
                    ? RibbitVillageExplorerMap.createFailedMap(this.count)
                    : this.stack.resolve(level).copyWithCount(this.count);
            if (this.failedMapMarkerOnly) {
                return new ItemCost(resolved.typeHolder(), this.count,
                        DataComponentExactPredicate.expect(
                                DataComponents.CUSTOM_DATA,
                                RibbitVillageExplorerMap.failedMarker()),
                        resolved);
            }
            if (!this.exactComponents) {
                return new ItemCost(resolved.getItem(), this.count);
            }
            return new ItemCost(resolved.typeHolder(), this.count,
                    DataComponentExactPredicate.allOf(resolved.getComponents()), resolved);
        }
    }

    public record TradeOfferSpec(String id, String profession, int tier, CostSpec first,
                                 CostSpec second, StackRef result, int resultCount, int maxUses,
                                 int merchantXp, String selection, int option, Gate gate) {
        public MerchantOffer create(Level level) {
            ItemCost firstCost = this.first.resolve(level);
            Optional<ItemCost> secondCost = Optional.ofNullable(this.second).map(cost -> cost.resolve(level));
            return new StrictMerchantOffer(firstCost, secondCost,
                    this.result.resolve(level).copyWithCount(this.resultCount),
                    this.maxUses, this.merchantXp, FIXED_PRICE_MULTIPLIER,
                    this.first.exactComponents,
                    this.second != null && this.second.exactComponents,
                    this.first.failedMapMarkerOnly,
                    this.second != null && this.second.failedMapMarkerOnly);
        }
    }

    private static final StackRef GLOWCAP = StackRef.item("ribbits:glowcap");
    private static final Map<String, TradeProfile> PROFILES = createProfiles();
    private static final List<TradeOfferSpec> ALL_OFFERS = createOffers();
    private static final Map<Identifier, List<RibbitExternalTradeOffer>> EXTERNAL_OFFERS = new TreeMap<>();

    private RibbitTradeModule() {
    }

    private static Map<String, TradeProfile> createProfiles() {
        Map<String, TradeProfile> profiles = new LinkedHashMap<>();
        profile(profiles, "gardener", 2, "Sprout Tender", "Toadstool Keeper");
        profile(profiles, "farmer", 3, "Vine Puller", "Root Wrangler", "Mudfield Steward");
        profile(profiles, "fisherman", 5, "Pond Forager", "Coral Keeper",
                "Amphibian Attendant", "Opal Angler", "Monument Mariner");
        profile(profiles, "merchant", 3, "Moss Peddler", "Lantern Trader", "Glowgoods Baron");
        profile(profiles, "chef", 5, "Tadpole Cook", "Pond Cook", "Swamp Chef",
                "Grand Chef", "Master of the Feast");
        profile(profiles, "sorcerer", 4, "Wart Whisperer", "Gatecaller",
                "Flask Sage", "Deep-Pond Oracle");
        profile(profiles, "prospector", 3, "Pebble Picker", "Vein-Seeker", "Deep Delver");
        profile(profiles, "guard", 4, "Pond Sentry", "Lily Warden",
                "Marsh Marshal", "Bulwark of the Bog");
        profiles.put("nitwit", new TradeProfile("nitwit", 0,
                List.of("Musician"), List.of("entity.ribbits.merchant.nitwit.musician")));
        return Map.copyOf(profiles);
    }

    private static void profile(Map<String, TradeProfile> profiles, String profession,
                                int maxTier, String... names) {
        List<String> keys = new ArrayList<>();
        for (int i = 1; i <= names.length; i++) {
            keys.add("entity.ribbits.merchant." + profession + ".tier_" + i);
        }
        profiles.put(profession, new TradeProfile(profession, maxTier,
                List.of(names), List.copyOf(keys)));
    }

    private static List<TradeOfferSpec> createOffers() {
        List<TradeOfferSpec> offers = new ArrayList<>();

        addChoice(offers, "gardener_red_mushroom_toadstool", "gardener", 1,
                item("minecraft:red_mushroom", 1), null, item("ribbits:toadstool"), 4,
                16, "gardener_tier1", 0);
        addChoice(offers, "gardener_brown_mushroom_toadstool", "gardener", 1,
                item("minecraft:brown_mushroom", 1), null, item("ribbits:small_brown_toadstool"), 4,
                16, "gardener_tier1", 1);
        addChoice(offers, "gardener_oxeye_daisy", "gardener", 1,
                item("minecraft:oxeye_daisy", 1), null, item("ribbits:swamp_daisy"), 4,
                16, "gardener_tier1", 2);
        addChoice(offers, "gardener_lily_pad", "gardener", 1,
                item("minecraft:lily_pad", 1), null, item("ribbits:giant_lilypad"), 4,
                16, "gardener_tier1", 3);
        addChoice(offers, "gardener_small_dripleaf", "gardener", 1,
                item("minecraft:small_dripleaf", 1), null, item("ribbits:umbrella_leaf"), 4,
                16, "gardener_tier1", 4);
        add(offers, "gardener_red_blocks", "gardener", 2, glowcaps(1), null,
                item("ribbits:red_toadstool"), 16, 16);
        add(offers, "gardener_brown_blocks", "gardener", 2, glowcaps(1), null,
                item("ribbits:brown_toadstool"), 16, 16);
        add(offers, "gardener_stems", "gardener", 2, glowcaps(1), null,
                item("ribbits:toadstool_stem"), 16, 16);

        add(offers, "farmer_vines", "farmer", 1, glowcaps(1), null,
                item("minecraft:vine"), 32, 16);
        add(offers, "farmer_hanging_roots", "farmer", 1, glowcaps(1), null,
                item("minecraft:hanging_roots"), 32, 16);
        addChoice(offers, "farmer_muddy_roots", "farmer", 2, glowcaps(1), null,
                item("minecraft:muddy_mangrove_roots"), 16, 16, "farmer_tier2", 0);
        addChoice(offers, "farmer_rooted_dirt", "farmer", 2, glowcaps(1), null,
                item("minecraft:rooted_dirt"), 16, 16, "farmer_tier2", 1);
        addChoice(offers, "farmer_coarse_dirt", "farmer", 3, glowcaps(1), null,
                item("minecraft:coarse_dirt"), 16, 16, "farmer_tier3", 0);
        addChoice(offers, "farmer_mud", "farmer", 3, glowcaps(1), null,
                item("minecraft:mud"), 16, 16, "farmer_tier3", 1);
        add(offers, "farmer_packed_mud", "farmer", 3, glowcaps(1), null,
                item("minecraft:packed_mud"), 8, 16);

        String[] aquatic = {"sea_pickle", "kelp", "seagrass"};
        for (int i = 0; i < aquatic.length; i++) {
            addChoice(offers, "fisherman_" + aquatic[i], "fisherman", 1,
                    glowcaps(1), null, item("minecraft:" + aquatic[i]), 16, 16,
                    "fisherman_aquatic", i);
        }
        String[] corals = {"tube", "brain", "bubble", "fire", "horn"};
        for (int family = 0; family < corals.length; family++) {
            String color = corals[family];
            addChoice(offers, "fisherman_" + color + "_coral_block", "fisherman", 2,
                    glowcaps(1), null, item("minecraft:" + color + "_coral_block"), 8, 16,
                    "fisherman_coral", family);
            addChoice(offers, "fisherman_" + color + "_coral_fan", "fisherman", 2,
                    glowcaps(1), null, item("minecraft:" + color + "_coral_fan"), 16, 16,
                    "fisherman_coral", family);
            addChoice(offers, "fisherman_" + color + "_coral", "fisherman", 2,
                    glowcaps(1), null, item("minecraft:" + color + "_coral"), 16, 16,
                    "fisherman_coral", family);
        }
        add(offers, "fisherman_tadpole_bucket", "fisherman", 3, glowcaps(1), null,
                item("minecraft:tadpole_bucket"), 1, 4);
        add(offers, "fisherman_axolotl_bucket", "fisherman", 3, glowcaps(1), null,
                item("minecraft:axolotl_bucket"), 1, 4);
        add(offers, "fisherman_opal_gate", "fisherman", 4, exact(StackRef.opalStack(), 1), null,
                GLOWCAP, 16, 2, "always", 0, Gate.FISHERMAN_OPAL);
        add(offers, "fisherman_dry_sponge", "fisherman", 5, glowcaps(1), null,
                item("minecraft:sponge"), 4, 1);

        add(offers, "merchant_mossy_oak", "merchant", 1, glowcaps(1), null,
                item("ribbits:mossy_oak_planks"), 16, 16);
        add(offers, "merchant_swamp_lantern", "merchant", 2, glowcaps(1), null,
                item("ribbits:swamp_lantern"), 8, 16);
        for (String light : List.of("ochre", "verdant", "pearlescent")) {
            add(offers, "merchant_" + light + "_froglight", "merchant", 3,
                    glowcaps(1), null, item("minecraft:" + light + "_froglight"), 8, 16);
        }
        add(offers, "musician_maraca", "nitwit", 0, glowcaps(8), null,
                item("ribbits:maraca"), 1, 4, "always", 0, Gate.NONE);

        addFixed(offers, "chef_glow_berry_crumble", "chef", 1, 2,
                FixedStack.GLOW_BERRY_CRUMBLE, 8, "always", 0);
        addFixed(offers, "chef_honey_ginger_tea", "chef", 1, 1,
                FixedStack.HONEY_GINGER_TEA, 8, "always", 0);
        addFixed(offers, "chef_pickled_carrots", "chef", 1, 2,
                FixedStack.PICKLED_CARROTS, 8, "chef_daily_1", 0);
        addFixed(offers, "chef_rind_jam", "chef", 1, 2,
                FixedStack.RIND_JAM, 8, "chef_daily_1", 1);
        addFixed(offers, "chef_gimmari", "chef", 2, 4,
                FixedStack.GIMMARI, 6, "chef_daily_2", 0);
        addFixed(offers, "chef_bokguk", "chef", 2, 4,
                FixedStack.BOKGUK, 6, "chef_daily_2", 1);
        FixedStack[] tier3Foods = {FixedStack.GOLDEN_PICKLED_CARROTS, FixedStack.MELON_SORBET,
                FixedStack.PUMPKIN_EMPANADA, FixedStack.WARPED_PIZZA, FixedStack.WARPED_STROGANOFF};
        String[] tier3Ids = {"golden_pickled_carrots", "melon_sorbet", "pumpkin_empanada",
                "warped_pizza", "warped_stroganoff"};
        for (int i = 0; i < tier3Foods.length; i++) {
            addFixed(offers, "chef_" + tier3Ids[i], "chef", 3, 6,
                    tier3Foods[i], 4, "chef_daily_3", i);
        }
        addFixed(offers, "chef_sweet_berry_danish", "chef", 4, 12,
                FixedStack.SWEET_BERRY_DANISH, 2, "chef_daily_4", 0);
        addFixed(offers, "chef_golden_carrot_cupcake", "chef", 4, 12,
                FixedStack.GOLDEN_CARROT_CUPCAKE, 2, "chef_daily_4", 1);
        addFixed(offers, "chef_japanese_curry", "chef", 5, 20,
                FixedStack.JAPANESE_CURRY, 1, "chef_master", 0);
        addFixed(offers, "chef_green_curry", "chef", 5, 20,
                FixedStack.GREEN_CURRY, 1, "chef_master", 1);
        addFixed(offers, "chef_tonkotsu_ramen", "chef", 5, 20,
                FixedStack.TONKOTSU_RAMEN, 1, "chef_master", 2);

        add(offers, "sorcerer_failed_map_redemption", "sorcerer", 0,
                failedMap(1), null, item("ribbits:toadstool_heart"), 1, 16);
        add(offers, "sorcerer_benzene_gate", "sorcerer", 1,
                exact(StackRef.fixed(FixedStack.BENZENE), 4), null, GLOWCAP, 1, 16,
                "always", 0, Gate.SORCERER_BENZENE);
        String[] colors = {"white", "orange", "magenta", "light_blue", "yellow", "lime",
                "pink", "gray", "light_gray", "cyan", "purple", "blue", "brown", "green",
                "red", "black"};
        for (String color : colors) {
            add(offers, "sorcerer_" + color + "_portal_catalyst", "sorcerer", 2,
                    item("minecraft:ender_pearl", 4), item("minecraft:" + color + "_dye", 1),
                    item("customportals:" + color + "_portal_catalyst"), 2, 8);
        }
        add(offers, "sorcerer_estus_flask", "sorcerer", 3, glowcaps(2), null,
                StackRef.fixed(FixedStack.ESTUS_FLASK), 1, 1);
        addChoice(offers, "sorcerer_reach_blessing", "sorcerer", 4, glowcaps(16), null,
                StackRef.fixed(FixedStack.REACH_BLESSING), 1, 1, "sorcerer_blessing", 0);
        addChoice(offers, "sorcerer_silk_touch_blessing", "sorcerer", 4, glowcaps(16), null,
                StackRef.fixed(FixedStack.SILK_TOUCH_BLESSING), 1, 1,
                "sorcerer_blessing", 1);

        String[][] tier1Tools = {
                {"golden_shovel", "2"}, {"iron_shovel", "2"},
                {"golden_hoe", "4"}, {"iron_hoe", "4"},
                {"golden_axe", "8"}, {"iron_axe", "8"},
                {"golden_pickaxe", "8"}, {"iron_pickaxe", "8"}
        };
        for (String[] row : tier1Tools) {
            add(offers, "prospector_" + row[0], "prospector", 1,
                    item("minecraft:" + row[0], 1), null, GLOWCAP,
                    Integer.parseInt(row[1]), 4);
        }
        add(offers, "prospector_glow_lichen", "prospector", 1, glowcaps(1), null,
                item("minecraft:glow_lichen"), 16, 16);
        String[][] tier2Tools = {
                {"diamond_shovel", "10"}, {"diamond_hoe", "12"},
                {"diamond_pickaxe", "16"}, {"diamond_axe", "16"}
        };
        for (String[] row : tier2Tools) {
            add(offers, "prospector_" + row[0], "prospector", 2,
                    item("minecraft:" + row[0], 1), null, GLOWCAP,
                    Integer.parseInt(row[1]), 2);
        }
        add(offers, "prospector_pointed_dripstone", "prospector", 2, glowcaps(1), null,
                item("minecraft:pointed_dripstone"), 4, 16);
        add(offers, "prospector_carbon_rich_iron", "prospector", 3, glowcaps(2), null,
                StackRef.fixed(FixedStack.CARBON_RICH_IRON), 1, 8);
        addChoice(offers, "prospector_hepatizon", "prospector", 3, glowcaps(8), null,
                StackRef.fixed(FixedStack.HEPATIZON_ALLOY), 1, 1, "prospector_bullion", 0);
        addChoice(offers, "prospector_shakudo", "prospector", 3, glowcaps(16), null,
                StackRef.fixed(FixedStack.SHAKUDO_ALLOY), 1, 1, "prospector_bullion", 1);
        addChoice(offers, "prospector_electrum", "prospector", 3, glowcaps(16),
                exact(StackRef.fixed(FixedStack.DIVINE_FRAGMENT), 1),
                StackRef.fixed(FixedStack.ELECTRUM_ALLOY), 1, 1, "prospector_bullion", 2);

        String[][] weapons1 = {{"golden_spear", "2"}, {"iron_spear", "2"},
                {"golden_sword", "4"}, {"iron_sword", "4"}, {"bow", "1"}};
        String[][] weapons3 = {{"diamond_spear", "4"}, {"diamond_sword", "8"},
                {"crossbow", "2"}};
        addGuardRows(offers, weapons1, 1, 4, 0);
        addGuardRows(offers, weapons3, 3, 2, 0);
        String[][] mount1 = {{"saddle", "4"}, {"golden_nautilus_armor", "4"},
                {"iron_nautilus_armor", "4"}, {"copper_nautilus_armor", "4"},
                {"golden_horse_armor", "4"}, {"iron_horse_armor", "4"},
                {"copper_horse_armor", "4"}};
        String[][] mount3 = {{"diamond_nautilus_armor", "8"}, {"diamond_horse_armor", "8"}};
        addGuardRows(offers, mount1, 1, 4, 1);
        addGuardRows(offers, mount3, 3, 2, 1);
        String[][] armor1 = {{"golden_helmet", "6"}, {"iron_helmet", "6"},
                {"golden_chestplate", "10"}, {"iron_chestplate", "10"},
                {"golden_leggings", "8"}, {"iron_leggings", "8"},
                {"golden_boots", "4"}, {"iron_boots", "4"}};
        String[][] armor3 = {{"diamond_helmet", "12"}, {"diamond_chestplate", "20"},
                {"diamond_leggings", "16"}, {"diamond_boots", "8"}};
        addGuardRows(offers, armor1, 1, 4, 2);
        addGuardRows(offers, armor3, 3, 2, 2);
        add(offers, "guard_iron_chains", "guard", 2, glowcaps(1), null,
                item("minecraft:iron_chain"), 16, 16);
        add(offers, "guard_copper_chains", "guard", 2, glowcaps(1), null,
                item("minecraft:copper_chain"), 16, 16);
        add(offers, "guard_anvil", "guard", 4, glowcaps(16), null,
                item("minecraft:anvil"), 1, 1);

        return List.copyOf(offers);
    }

    private static void addGuardRows(List<TradeOfferSpec> offers, String[][] rows,
                                     int tier, int maxUses, int branch) {
        for (String[] row : rows) {
            addChoice(offers, "guard_" + row[0], "guard", tier,
                    item("minecraft:" + row[0], 1), null, GLOWCAP,
                    Integer.parseInt(row[1]), maxUses, "guard_branch", branch);
        }
    }

    private static StackRef item(String id) { return StackRef.item(id); }
    private static CostSpec item(String id, int count) { return new CostSpec(item(id), count, false); }
    private static CostSpec glowcaps(int count) { return new CostSpec(GLOWCAP, count, false); }
    private static CostSpec exact(StackRef stack, int count) { return new CostSpec(stack, count, true); }
    private static CostSpec failedMap(int count) {
        return new CostSpec(item("minecraft:map"), count, false, true);
    }

    private static void addFixed(List<TradeOfferSpec> offers, String id, String profession,
                                 int tier, int glowcapCount, FixedStack result, int maxUses,
                                 String selection, int option) {
        add(offers, id, profession, tier, glowcaps(glowcapCount), null,
                StackRef.fixed(result), 1, maxUses, selection, option, Gate.NONE);
    }

    private static void addChoice(List<TradeOfferSpec> offers, String id, String profession,
                                  int tier, CostSpec first, CostSpec second, StackRef result,
                                  int resultCount, int maxUses, String selection, int option) {
        add(offers, id, profession, tier, first, second, result, resultCount, maxUses,
                selection, option, Gate.NONE);
    }

    private static void add(List<TradeOfferSpec> offers, String id, String profession,
                            int tier, CostSpec first, CostSpec second, StackRef result,
                            int resultCount, int maxUses) {
        add(offers, id, profession, tier, first, second, result, resultCount, maxUses,
                "always", 0, Gate.NONE);
    }

    private static void add(List<TradeOfferSpec> offers, String id, String profession,
                            int tier, CostSpec first, CostSpec second, StackRef result,
                            int resultCount, int maxUses, String selection, int option, Gate gate) {
        TradeProfile profile = PROFILES.get(profession);
        int xp = tier <= 0 || tier == profile.maxTier ? 0 : tier;
        offers.add(new TradeOfferSpec(id, profession, tier, first, second, result,
                resultCount, maxUses, xp, selection, option, gate));
    }

    public static List<TradeOfferSpec> allOffers() { return ALL_OFFERS; }
    public static Map<String, TradeProfile> profiles() { return PROFILES; }

    /** Registers one additive, stable-ID contribution without coupling Ribbits to its owner. */
    public static synchronized void registerExternalOffers(Identifier owner,
                                                            List<RibbitExternalTradeOffer> offers) {
        Objects.requireNonNull(owner, "owner");
        List<RibbitExternalTradeOffer> copy = List.copyOf(Objects.requireNonNull(offers, "offers"));
        if (copy.isEmpty() || EXTERNAL_OFFERS.containsKey(owner)) {
            throw new IllegalArgumentException("Invalid or duplicate external Ribbit trade contribution " + owner);
        }
        java.util.HashSet<String> ids = new java.util.HashSet<>();
        for (RibbitExternalTradeOffer offer : copy) {
            if (!ids.add(offer.id())) {
                throw new IllegalArgumentException("Duplicate external Ribbit offer " + owner + "/" + offer.id());
            }
        }
        EXTERNAL_OFFERS.put(owner, copy);
    }

    public static synchronized Map<Identifier, List<RibbitExternalTradeOffer>> externalOffers() {
        return Map.copyOf(EXTERNAL_OFFERS);
    }

    public static TradeProfile profile(RibbitProfession profession) {
        return profile(profession.id().getPath());
    }

    public static TradeProfile profile(String profession) {
        TradeProfile profile = PROFILES.get(profession);
        if (profile == null) {
            throw new IllegalStateException("No Ribbit trade profile for " + profession);
        }
        return profile;
    }

    public static Component title(RibbitEntity ribbit) {
        TradeProfile profile = profile(ribbit.getRibbitData().getProfession());
        return Component.translatable(profile.tiered()
                ? profile.titleKey(ribbit.getTradeState().rank())
                : profile.titleKeys.getFirst());
    }

    public static void normalizePersistentState(RibbitEntity ribbit) {
        TradeProfile profile = profile(ribbit.getRibbitData().getProfession());
        normalizePersistentState(profile, ribbit.getTradeState());
    }

    /**
     * Reconciles persisted rank data against the current schedule without erasing earned XP.
     * Lowered thresholds may unlock a tier immediately; special-gate caps remain authoritative.
     */
    public static void normalizePersistentState(TradeProfile profile, RibbitTradeState state) {
        if (!profile.tiered()) {
            state.rank(0);
            state.xp(0);
            return;
        }
        int rank = Math.max(1, Math.min(state.rank(), profile.maxTier));
        rank = Math.max(rank, rankForXp(profile, state, state.xp()));
        if ("sorcerer".equals(profile.profession)) {
            rank = state.sorcererBenzeneGate() ? Math.max(rank, 2) : 1;
        }
        if ("fisherman".equals(profile.profession)) {
            rank = state.fishermanOpalGate() ? Math.max(rank, 5) : Math.min(rank, 4);
        }
        state.rank(rank);
        int xpFloor = XP_THRESHOLDS[Math.max(0, rank - 1)];
        state.xp(Math.max(xpFloor, state.xp()));
    }

    public static void initializePersistentChoices(RibbitEntity ribbit) {
        RibbitTradeState state = ribbit.getTradeState();
        RandomSource random = ribbit.getRandom();
        String profession = profile(ribbit.getRibbitData().getProfession()).profession;
        switch (profession) {
            case "gardener" -> initializeGardenerTier1Trades(state, random);
            case "farmer" -> {
                state.farmerTier2Choice(validOrRoll(state.farmerTier2Choice(), 2, random));
                state.farmerTier3Choice(validOrRoll(state.farmerTier3Choice(), 2, random));
            }
            case "fisherman" -> {
                state.fishermanAquaticChoice(validOrRoll(state.fishermanAquaticChoice(), 3, random));
                state.fishermanCoralFamily(validOrRoll(state.fishermanCoralFamily(), 5, random));
            }
            case "sorcerer" -> state.sorcererBlessingChoice(
                    validOrRoll(state.sorcererBlessingChoice(), 2, random));
            case "prospector" -> state.prospectorBullionChoice(
                    validOrRoll(state.prospectorBullionChoice(), 3, random));
            case "guard" -> state.guardBranch(validOrRoll(state.guardBranch(), 3, random));
            case "chef" -> {
                if (state.rank() >= 5) {
                    state.chefMasterSpecialty(validOrRoll(state.chefMasterSpecialty(), 3, random));
                }
            }
            default -> { }
        }
        initializeExternalChoices(ribbit, state, random, profession);
    }

    private static synchronized void initializeExternalChoices(RibbitEntity ribbit,
                                                                 RibbitTradeState state,
                                                                 RandomSource random,
                                                                 String profession) {
        Map<String, Integer> counts = new TreeMap<>();
        for (Map.Entry<Identifier, List<RibbitExternalTradeOffer>> contribution : EXTERNAL_OFFERS.entrySet()) {
            for (RibbitExternalTradeOffer offer : contribution.getValue()) {
                if (!profession.equals(offer.profession()) || offer.selectionKey() == null) continue;
                String key = contribution.getKey() + "/" + offer.selectionKey();
                Integer previous = counts.putIfAbsent(key, offer.selectionOptions());
                if (previous != null && previous != offer.selectionOptions()) {
                    throw new IllegalStateException("Inconsistent external Ribbit choice " + key);
                }
            }
        }
        for (Map.Entry<String, Integer> choice : counts.entrySet()) {
            state.externalChoice(choice.getKey(), validOrRoll(state.externalChoice(choice.getKey()),
                    choice.getValue(), random));
        }
    }

    private static int validOrRoll(int current, int options, RandomSource random) {
        return current >= 0 && current < options ? current : random.nextInt(options);
    }

    private static void initializeGardenerTier1Trades(RibbitTradeState state, RandomSource random) {
        int current = state.gardenerTier1Trades();
        if (Integer.bitCount(current) == 3 && (current & ~0b1_1111) == 0) return;

        int legacyPair = state.legacyGardenerPair();
        if (legacyPair == 0 || legacyPair == 1) {
            int preserved = legacyPair == 0 ? 0b00101 : 0b11000;
            int[] remaining = new int[3];
            int next = 0;
            for (int option = 0; option < 5; option++) {
                if ((preserved & (1 << option)) == 0) remaining[next++] = option;
            }
            state.gardenerTier1Trades(preserved | (1 << remaining[random.nextInt(remaining.length)]));
            state.clearLegacyGardenerPair();
            return;
        }

        int[] options = {0, 1, 2, 3, 4};
        for (int index = options.length - 1; index > 0; index--) {
            int selected = random.nextInt(index + 1);
            int swap = options[index];
            options[index] = options[selected];
            options[selected] = swap;
        }
        state.gardenerTier1Trades((1 << options[0]) | (1 << options[1]) | (1 << options[2]));
        state.clearLegacyGardenerPair();
    }

    public static int deterministicChefMenu(UUID uuid, int tier, long day, int poolSize) {
        long seed = uuid.getMostSignificantBits()
                ^ Long.rotateLeft(uuid.getLeastSignificantBits(), 19)
                ^ (0x9E3779B97F4A7C15L * tier);
        int offset = (int) Math.floorMod(seed, poolSize);
        return (int) Math.floorMod((long) offset + day, poolSize);
    }

    public static void updateChefMenuForDay(RibbitEntity ribbit, long day) {
        RibbitTradeState state = ribbit.getTradeState();
        int maxDailyTier = Math.min(4, state.rank());
        boolean newDay = state.chefMenuDay() != day;
        for (int tier = 1; tier <= maxDailyTier; tier++) {
            if (newDay || state.chefMenu(tier) < 0) {
                int poolSize = tier == 3 ? 5 : 2;
                state.chefMenu(tier, deterministicChefMenu(ribbit.getUUID(), tier, day, poolSize));
            }
        }
        state.chefMenuDay(day);
    }

    public static void updateTrades(RibbitEntity ribbit) { rebuildTrades(ribbit); }

    public static void rebuildTrades(RibbitEntity ribbit) {
        normalizePersistentState(ribbit);
        initializePersistentChoices(ribbit);
        TradeProfile profile = profile(ribbit.getRibbitData().getProfession());
        if ("chef".equals(profile.profession)) {
            updateChefMenuForDay(ribbit, ribbit.currentRestockDay());
        }
        MerchantOffers offers = ribbit.getMutableOffers();
        offers.clear();
        ribbit.getTradeState().tradeSchema(CURRENT_TRADE_SCHEMA);
        int rank = profile.tiered() ? ribbit.getTradeState().rank() : 0;
        for (TradeOfferSpec spec : selectedSpecs(ribbit, rank)) {
            offers.add(spec.create(ribbit.level()));
        }
    }

    public static void addUnlockedTier(RibbitEntity ribbit, int tier) {
        initializePersistentChoices(ribbit);
        ribbit.getTradeState().tradeSchema(CURRENT_TRADE_SCHEMA);
        TradeProfile profile = profile(ribbit.getRibbitData().getProfession());
        if ("chef".equals(profile.profession)) {
            updateChefMenuForDay(ribbit, ribbit.currentRestockDay());
        }
        for (TradeOfferSpec spec : selectedSpecs(ribbit, tier)) {
            if (spec.tier == tier) {
                ribbit.getMutableOffers().add(spec.create(ribbit.level()));
            }
        }
    }

    /**
     * Lazily appends only newly unlocked tiers for a saved current-schema Ribbit.  It never
     * rebuilds the existing inventory, so offer uses, demand, special prices, Chef menus, gates,
     * and persistent selections remain intact.
     */
    public static void appendMissingUnlockedTiers(RibbitEntity ribbit, MerchantOffers offers) {
        TradeProfile profile = profile(ribbit.getRibbitData().getProfession());
        if (!profile.tiered() || ribbit.getTradeState().tradeSchema() != CURRENT_TRADE_SCHEMA) return;

        initializePersistentChoices(ribbit);
        int targetRank = ribbit.getTradeState().rank();
        for (int materializedRank = 1; materializedRank < targetRank; materializedRank++) {
            if (!offersMatchSelectedSpecs(ribbit, offers, materializedRank)) continue;
            for (int tier = materializedRank + 1; tier <= targetRank; tier++) {
                addUnlockedTier(ribbit, tier);
            }
            return;
        }
    }

    private static boolean offersMatchSelectedSpecs(
            RibbitEntity ribbit, MerchantOffers offers, int rank
    ) {
        List<TradeOfferSpec> specs = selectedSpecs(ribbit, rank);
        if (offers.size() != specs.size()) return false;
        for (int index = 0; index < specs.size(); index++) {
            if (!sameOfferShape(offers.get(index), specs.get(index).create(ribbit.level()))) {
                return false;
            }
        }
        return true;
    }

    private static List<TradeOfferSpec> selectedSpecs(RibbitEntity ribbit, int rank) {
        String profession = profile(ribbit.getRibbitData().getProfession()).profession;
        RibbitTradeState state = ribbit.getTradeState();
        List<TradeOfferSpec> result = new ArrayList<>();
        for (TradeOfferSpec spec : ALL_OFFERS) {
            if (spec.profession.equals(profession) && spec.tier <= rank && isSelected(spec, state)) {
                result.add(spec);
            }
        }
        synchronized (RibbitTradeModule.class) {
            for (Map.Entry<Identifier, List<RibbitExternalTradeOffer>> contribution : EXTERNAL_OFFERS.entrySet()) {
                for (RibbitExternalTradeOffer offer : contribution.getValue()) {
                    if (!profession.equals(offer.profession()) || offer.tier() > rank) continue;
                    String key = offer.selectionKey() == null ? null
                            : contribution.getKey() + "/" + offer.selectionKey();
                    if (key == null || state.externalChoice(key) == offer.selectionOption()) {
                        result.add(offer.asTemplate(contribution.getKey()));
                    }
                }
            }
        }
        return result;
    }

    private static boolean isSelected(TradeOfferSpec spec, RibbitTradeState state) {
        return switch (spec.selection) {
            case "always" -> true;
            case "gardener_tier1" -> (state.gardenerTier1Trades() & (1 << spec.option)) != 0;
            case "farmer_tier2" -> spec.option == state.farmerTier2Choice();
            case "farmer_tier3" -> spec.option == state.farmerTier3Choice();
            case "fisherman_aquatic" -> spec.option == state.fishermanAquaticChoice();
            case "fisherman_coral" -> spec.option == state.fishermanCoralFamily();
            case "chef_daily_1" -> spec.option == state.chefMenu(1);
            case "chef_daily_2" -> spec.option == state.chefMenu(2);
            case "chef_daily_3" -> spec.option == state.chefMenu(3);
            case "chef_daily_4" -> spec.option == state.chefMenu(4);
            case "chef_master" -> spec.option == state.chefMasterSpecialty();
            case "sorcerer_blessing" -> spec.option == state.sorcererBlessingChoice();
            case "prospector_bullion" -> spec.option == state.prospectorBullionChoice();
            case "guard_branch" -> spec.option == state.guardBranch();
            default -> throw new IllegalStateException("Unknown trade selection " + spec.selection);
        };
    }

    /**
     * Reattaches the exact full-stack matcher lost by vanilla MerchantOffer serialization.
     * The explicit schema marker prevents this from becoming a migration of predecessor offers.
     */
    public static void restoreStrictComponentMatching(RibbitEntity ribbit, MerchantOffers offers) {
        if (!(ribbit.level() instanceof ServerLevel)
                || ribbit.getTradeState().tradeSchema() != CURRENT_TRADE_SCHEMA) {
            return;
        }

        TradeProfile profile = profile(ribbit.getRibbitData().getProfession());
        int rank = profile.tiered() ? ribbit.getTradeState().rank() : 0;
        List<TradeOfferSpec> specs = selectedSpecs(ribbit, rank);
        if (offers.size() != specs.size()) {
            return;
        }

        List<MerchantOffer> templates = new ArrayList<>(specs.size());
        for (int i = 0; i < specs.size(); i++) {
            MerchantOffer template = specs.get(i).create(ribbit.level());
            if (!sameOfferShape(offers.get(i), template)) {
                return;
            }
            templates.add(template);
        }
        for (int i = 0; i < offers.size(); i++) {
            offers.set(i, StrictMerchantOffer.restoreFromTemplate(offers.get(i), templates.get(i)));
        }
    }

    /**
     * Adds the sole Phase C economy change to a Canary 3 Sorcerer without migrating or rebuilding
     * any Phase B offer. Existing offers (including use, demand, and special-price state) remain
     * the same objects. Other professions and unexpected schemas/shapes are untouched.
     */
    public static void ensurePhaseCRedemptionOffer(RibbitEntity ribbit, MerchantOffers offers) {
        RibbitTradeState state = ribbit.getTradeState();
        if (state.tradeSchema() != CURRENT_TRADE_SCHEMA) {
            return;
        }

        TradeProfile profile = profile(ribbit.getRibbitData().getProfession());
        if (!"sorcerer".equals(profile.profession)) {
            return;
        }

        int rank = state.rank();
        List<TradeOfferSpec> currentSpecs = selectedSpecs(ribbit, rank);
        if (currentSpecs.isEmpty()
                || !"sorcerer_failed_map_redemption".equals(currentSpecs.getFirst().id)) {
            throw new IllegalStateException("Phase C Sorcerer service is not the first selected offer");
        }

        MerchantOffer redemptionTemplate = currentSpecs.getFirst().create(ribbit.level());
        List<MerchantOffer> expectedPhaseB = new ArrayList<>(currentSpecs.size() - 1);
        for (int index = 1; index < currentSpecs.size(); index++) {
            expectedPhaseB.add(currentSpecs.get(index).create(ribbit.level()));
        }
        prependPhaseCRedemptionOffer(offers, redemptionTemplate, expectedPhaseB);
    }

    /** Package-visible deterministic core, separated so state preservation is directly testable. */
    static boolean prependPhaseCRedemptionOffer(MerchantOffers offers,
                                                 MerchantOffer redemptionTemplate,
                                                 List<MerchantOffer> expectedPhaseB) {
        if (offers.size() == expectedPhaseB.size() + 1) {
            // Phase C list (or an unexpected same-sized list): restoration validates full shape.
            return false;
        }
        if (offers.size() != expectedPhaseB.size()) {
            return false;
        }
        for (int index = 0; index < expectedPhaseB.size(); index++) {
            if (!sameOfferShape(offers.get(index), expectedPhaseB.get(index))) {
                return false;
            }
        }

        offers.add(0, redemptionTemplate);
        return true;
    }

    private static boolean sameOfferShape(MerchantOffer saved, MerchantOffer template) {
        return sameCostShape(saved.getItemCostA(), template.getItemCostA())
                && saved.getItemCostB().isPresent() == template.getItemCostB().isPresent()
                && (saved.getItemCostB().isEmpty()
                    || sameCostShape(saved.getItemCostB().orElseThrow(),
                            template.getItemCostB().orElseThrow()))
                && ItemStack.isSameItemSameComponents(saved.getResult(), template.getResult())
                && saved.getResult().getCount() == template.getResult().getCount()
                && saved.getMaxUses() == template.getMaxUses()
                && saved.getXp() == template.getXp()
                && saved.shouldRewardExp() == template.shouldRewardExp()
                && Float.compare(saved.getPriceMultiplier(), template.getPriceMultiplier()) == 0;
    }

    private static boolean sameCostShape(ItemCost saved, ItemCost template) {
        // Compare the serialized predicate, not ItemCost#itemStack: a decoded spawn egg restores
        // its default entity_data even when the exact Benzene predicate deliberately omits it.
        return saved.item().equals(template.item())
                && saved.count() == template.count()
                && saved.components().equals(template.components());
    }

    public static Gate gateForCompletedOffer(RibbitEntity ribbit, MerchantOffer offer) {
        String profession = profile(ribbit.getRibbitData().getProfession()).profession;
        Identifier first = BuiltInRegistries.ITEM.getKey(offer.getItemCostA().item().value());
        Identifier result = BuiltInRegistries.ITEM.getKey(offer.getResult().getItem());
        if ("sorcerer".equals(profession)
                && first.equals(Identifier.parse("minecraft:endermite_spawn_egg"))
                && offer.getItemCostA().count() == 4
                && result.equals(Identifier.parse("ribbits:glowcap"))
                && offer.getResult().getCount() == 1) {
            return Gate.SORCERER_BENZENE;
        }
        if ("fisherman".equals(profession)
                && first.equals(Identifier.parse("minecraft:fermented_spider_eye"))
                && offer.getItemCostA().count() == 1
                && result.equals(Identifier.parse("ribbits:glowcap"))
                && offer.getResult().getCount() == 16) {
            return Gate.FISHERMAN_OPAL;
        }
        return Gate.NONE;
    }

    public static int rankForXp(TradeProfile profile, RibbitTradeState state, int xp) {
        if (!profile.tiered()) {
            return 0;
        }
        int rank = 1;
        for (int tier = 2; tier <= profile.maxTier; tier++) {
            if (xp >= XP_THRESHOLDS[tier - 1]) {
                rank = tier;
            }
        }
        if ("sorcerer".equals(profile.profession) && !state.sorcererBenzeneGate()) {
            rank = 1;
        }
        if ("fisherman".equals(profile.profession) && !state.fishermanOpalGate()) {
            rank = Math.min(rank, 4);
        }
        return rank;
    }

    public static int uiXpFor(RibbitTradeState state, TradeProfile profile) {
        if (!profile.tiered()) {
            return 0;
        }
        int rank = Math.max(1, Math.min(state.rank(), profile.maxTier));
        int[] vanilla = {0, 10, 70, 150, 250};
        if (rank >= profile.maxTier) {
            return vanilla[Math.min(rank, 4)];
        }
        int sourceMin = XP_THRESHOLDS[rank - 1];
        int sourceMax = XP_THRESHOLDS[Math.min(rank, 4)];
        int targetMin = vanilla[rank - 1];
        int targetMax = vanilla[rank];
        double progress = sourceMax <= sourceMin ? 0.0 : Math.max(0.0, Math.min(1.0,
                (state.xp() - sourceMin) / (double) (sourceMax - sourceMin)));
        return targetMin + (int) Math.round(progress * (targetMax - targetMin));
    }

    public static void validateRuntime(ServerLevel level) {
        MatchaStackCatalog.validateAll(level);
        for (String color : List.of("white", "orange", "magenta", "light_blue", "yellow",
                "lime", "pink", "gray", "light_gray", "cyan", "purple", "blue", "brown",
                "green", "red", "black")) {
            MatchaStackCatalog.requiredItem("customportals:" + color + "_portal_catalyst");
        }
        for (TradeOfferSpec spec : ALL_OFFERS) {
            if (spec.maxUses <= 0 || spec.resultCount <= 0 || spec.first.count <= 0
                    || spec.merchantXp < 0 || spec.merchantXp > 4) {
                throw new IllegalStateException("Invalid Ribbit offer " + spec.id);
            }
            validateRegistryRef(spec.first.stack);
            if (spec.second != null) {
                validateRegistryRef(spec.second.stack);
            }
            validateRegistryRef(spec.result);
        }
    }

    private static void validateRegistryRef(StackRef ref) {
        if (ref.registryId != null) {
            MatchaStackCatalog.requiredItem(ref.registryId);
        }
    }
}
