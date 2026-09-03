package com.yungnickyoung.minecraft.ribbits.module;

import com.yungnickyoung.minecraft.ribbits.entity.trade.RibbitTradeState;
import com.yungnickyoung.minecraft.ribbits.module.RibbitTradeModule.TradeOfferSpec;
import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhaseCTradeMigrationContractTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        RegistryAccess builtIns = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        HolderLookup.Provider vanillaData = VanillaRegistries.createLookup();
        HolderLookup.Provider registries = HolderLookup.Provider.create(
                java.util.stream.Stream.concat(
                        builtIns.listRegistries(),
                        vanillaData.listRegistries()
                                .filter(lookup -> builtIns.lookup(lookup.key()).isEmpty())
                )
        );
        BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(registries)
                .forEach(pending -> pending.apply());
    }

    @Test
    void everyExistingSorcererRankRecognizesItsExactCanaryThreeShape() throws Exception {
        int[] expectedPhaseBCounts = {1, 17, 18, 19};
        for (int rank = 1; rank <= 4; rank++) {
            List<TradeOfferSpec> phaseC = selectedSorcererSpecs(rank);
            assertEquals(expectedPhaseBCounts[rank - 1] + 1, phaseC.size(),
                    "Phase C Sorcerer rank " + rank);
            assertEquals("sorcerer_failed_map_redemption", phaseC.getFirst().id());

            List<MerchantOffer> templates = dummyOffers(expectedPhaseBCounts[rank - 1]);
            List<MerchantOffer> savedObjects = copiesWithState(templates);
            MerchantOffers saved = new MerchantOffers();
            saved.addAll(savedObjects);
            MerchantOffer redemption = dummyOffer(1000);

            assertTrue(RibbitTradeModule.prependPhaseCRedemptionOffer(
                    saved, redemption, templates), "rank " + rank + " migration");
            assertEquals(expectedPhaseBCounts[rank - 1] + 1, saved.size());
            assertSame(redemption, saved.getFirst());
            for (int index = 0; index < templates.size(); index++) {
                assertSame(savedObjects.get(index), saved.get(index + 1));
            }
        }
    }

    @Test
    void insertionPreservesEverySavedOfferAndItsStockDemandAndSpecialPriceState() {
        List<MerchantOffer> expected = dummyOffers(19);
        List<MerchantOffer> savedObjects = copiesWithState(expected);
        MerchantOffers saved = new MerchantOffers();
        List<OfferState> before = new ArrayList<>();
        for (MerchantOffer offer : savedObjects) {
            saved.add(offer);
            before.add(OfferState.capture(offer));
        }

        MerchantOffer redemption = dummyOffer(1000);
        assertTrue(RibbitTradeModule.prependPhaseCRedemptionOffer(saved, redemption, expected));
        for (int index = 0; index < expected.size(); index++) {
            MerchantOffer after = saved.get(index + 1);
            assertSame(savedObjects.get(index), after, "offer object " + index);
            assertEquals(before.get(index), OfferState.capture(after), "offer state " + index);
        }
    }

    @Test
    void phaseCAndDriftedListsFailClosedWithoutDuplicationOrReplacement() {
        List<MerchantOffer> expected = dummyOffers(19);
        MerchantOffer redemption = dummyOffer(1000);

        MerchantOffers alreadyPhaseC = new MerchantOffers();
        alreadyPhaseC.add(redemption);
        alreadyPhaseC.addAll(copiesWithState(expected));
        assertFalse(RibbitTradeModule.prependPhaseCRedemptionOffer(
                alreadyPhaseC, dummyOffer(1001), expected));
        assertEquals(20, alreadyPhaseC.size());
        assertSame(redemption, alreadyPhaseC.getFirst());

        MerchantOffers drifted = new MerchantOffers();
        List<MerchantOffer> savedObjects = copiesWithState(expected);
        drifted.addAll(savedObjects);
        MerchantOffer unexpected = dummyOffer(2000);
        drifted.set(7, unexpected);
        assertFalse(RibbitTradeModule.prependPhaseCRedemptionOffer(
                drifted, dummyOffer(1002), expected));
        assertEquals(19, drifted.size());
        assertSame(unexpected, drifted.get(7));
        assertSame(savedObjects.getFirst(), drifted.getFirst());
    }

    @Test
    void componentOnlyCostOrResultDriftFailsClosed() {
        List<MerchantOffer> expected = dummyOffers(19);

        List<MerchantOffer> costDriftObjects = copiesWithState(expected);
        MerchantOffer originalCostOffer = costDriftObjects.get(7);
        ItemCost driftedCost = originalCostOffer.getItemCostA().withComponents(builder ->
                builder.expect(DataComponents.CUSTOM_NAME, Component.literal("drifted cost")));
        MerchantOffer costDrift = copyShapeWith(
                originalCostOffer, driftedCost, originalCostOffer.getResult().copy());
        costDriftObjects.set(7, costDrift);
        MerchantOffers costDrifted = new MerchantOffers();
        costDrifted.addAll(costDriftObjects);
        assertFalse(RibbitTradeModule.prependPhaseCRedemptionOffer(
                costDrifted, dummyOffer(1003), expected));
        assertEquals(19, costDrifted.size());
        assertSame(costDrift, costDrifted.get(7));

        List<MerchantOffer> resultDriftObjects = copiesWithState(expected);
        MerchantOffer originalResultOffer = resultDriftObjects.get(11);
        ItemStack driftedResult = originalResultOffer.getResult().copy();
        driftedResult.set(DataComponents.CUSTOM_NAME, Component.literal("drifted result"));
        MerchantOffer resultDrift = copyShapeWith(
                originalResultOffer, originalResultOffer.getItemCostA(), driftedResult);
        resultDriftObjects.set(11, resultDrift);
        MerchantOffers resultDrifted = new MerchantOffers();
        resultDrifted.addAll(resultDriftObjects);
        assertFalse(RibbitTradeModule.prependPhaseCRedemptionOffer(
                resultDrifted, dummyOffer(1004), expected));
        assertEquals(19, resultDrifted.size());
        assertSame(resultDrift, resultDrifted.get(11));
    }

    private static List<TradeOfferSpec> selectedSorcererSpecs(int rank) throws Exception {
        Method selected = RibbitTradeModule.class.getDeclaredMethod(
                "isSelected", TradeOfferSpec.class, RibbitTradeState.class);
        selected.setAccessible(true);
        RibbitTradeState state = new RibbitTradeState();
        state.rank(rank);
        state.sorcererBlessingChoice(0);
        List<TradeOfferSpec> result = new ArrayList<>();
        for (TradeOfferSpec spec : RibbitTradeModule.allOffers()) {
            if (spec.profession().equals("sorcerer") && spec.tier() <= rank
                    && (boolean) selected.invoke(null, spec, state)) {
                result.add(spec);
            }
        }
        return result;
    }

    private static List<MerchantOffer> dummyOffers(int count) {
        List<MerchantOffer> result = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            result.add(dummyOffer(index));
        }
        return result;
    }

    private static MerchantOffer dummyOffer(int index) {
        return new MerchantOffer(
                new ItemCost((index & 1) == 0 ? Items.EMERALD : Items.DIAMOND,
                        Math.floorMod(index, 50) + 1),
                java.util.Optional.empty(),
                new ItemStack((index & 1) == 0 ? Items.PAPER : Items.BOOK, 1),
                0,
                Math.floorMod(index, 16) + 4,
                Math.floorMod(index, 5),
                0.0F,
                index - 3);
    }

    private static List<MerchantOffer> copiesWithState(List<MerchantOffer> templates) {
        List<MerchantOffer> result = new ArrayList<>();
        for (int index = 0; index < templates.size(); index++) {
            MerchantOffer template = templates.get(index);
            MerchantOffer saved = new MerchantOffer(
                    template.getItemCostA(), template.getItemCostB(), template.getResult().copy(),
                    1 + (index & 1), template.getMaxUses(), template.getXp(),
                    template.getPriceMultiplier(), index * 3 - 7);
            saved.setSpecialPriceDiff(index - 9);
            result.add(saved);
        }
        return result;
    }

    private static MerchantOffer copyShapeWith(MerchantOffer source, ItemCost first,
                                                ItemStack result) {
        MerchantOffer copy = new MerchantOffer(
                first, source.getItemCostB(), result,
                source.getUses(), source.getMaxUses(), source.getXp(),
                source.getPriceMultiplier(), source.getDemand());
        copy.setSpecialPriceDiff(source.getSpecialPriceDiff());
        return copy;
    }

    private record OfferState(int uses, int demand, int specialPrice,
                              int maxUses, int xp, float multiplier) {
        private static OfferState capture(MerchantOffer offer) {
            return new OfferState(offer.getUses(), offer.getDemand(), offer.getSpecialPriceDiff(),
                    offer.getMaxUses(), offer.getXp(), offer.getPriceMultiplier());
        }
    }
}
