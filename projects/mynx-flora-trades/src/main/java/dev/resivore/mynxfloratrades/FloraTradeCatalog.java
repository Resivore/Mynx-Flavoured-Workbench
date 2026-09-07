package dev.resivore.mynxfloratrades;

import com.yungnickyoung.minecraft.ribbits.entity.trade.RibbitExternalTradeOffer;
import com.yungnickyoung.minecraft.ribbits.module.RibbitTradeModule;

import java.util.List;

/** Immutable Flora-owned trade semantics, separated from registry lookup for direct regression tests. */
final class FloraTradeCatalog {
    private static final String GLOWCAP = "ribbits:glowcap";
    private static final List<Offer> FARMER = List.of(
            farmer("clover", 1, "tier_1", 0, 64),
            farmer("stone_bud", 1, "tier_1", 1, 32),
            farmer("barley", 2, "tier_2", 0, 32),
            farmer("windswept_grass", 2, "tier_2", 1, 32));
    private static final List<List<Offer>> WANDERING = List.of(
            List.of(wandering("dropleaf", 16), wandering("mycotoxic_daisy", 16)),
            List.of(wandering("cattail", 16), wandering("duckweed", 32)));

    private FloraTradeCatalog() { }

    static List<RibbitExternalTradeOffer> farmerOffers() {
        return FARMER.stream().map(FloraTradeCatalog::asFarmerOffer).toList();
    }

    static List<Offer> farmerDefinitions() { return FARMER; }

    static List<List<Offer>> wanderingGroups() { return WANDERING; }

    private static Offer farmer(String flora, int tier, String selectionKey, int selectionOption, int outputCount) {
        return new Offer("flora_" + selectionKey + "_" + flora, tier, selectionKey, selectionOption,
                GLOWCAP, 1, "mynx_regions_unexplored:" + flora, outputCount);
    }

    private static Offer wandering(String flora, int outputCount) {
        return new Offer("wandering_flora_" + flora, 0, null, 0,
                GLOWCAP, 1, "mynx_regions_unexplored:" + flora, outputCount);
    }

    private static RibbitExternalTradeOffer asFarmerOffer(Offer offer) {
        return new RibbitExternalTradeOffer(offer.id(), "farmer", offer.tier(),
                new RibbitTradeModule.CostSpec(RibbitTradeModule.StackRef.item(offer.inputId()),
                        offer.inputCount(), false),
                null, RibbitTradeModule.StackRef.item(offer.outputId()), offer.outputCount(), 16, offer.tier(),
                offer.selectionKey(), 2, offer.selectionOption());
    }

    record Offer(String id, int tier, String selectionKey, int selectionOption,
                 String inputId, int inputCount, String outputId, int outputCount) { }
}
