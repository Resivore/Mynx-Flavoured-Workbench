package com.yungnickyoung.minecraft.ribbits.entity.trade;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Exercises the actual merchant payment/notification sequence for Naturalist's one-shot offers. */
class WanderingRibbitNaturalistFaunaTransactionTest {
    private static RegistryOps<JsonElement> registryOps;

    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        RegistryAccess builtIns = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        HolderLookup.Provider vanillaData = VanillaRegistries.createLookup();
        HolderLookup.Provider registries = HolderLookup.Provider.create(
                java.util.stream.Stream.concat(
                        builtIns.listRegistries(),
                        vanillaData.listRegistries().filter(lookup -> builtIns.lookup(lookup.key()).isEmpty())
                )
        );
        BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(registries)
                .forEach(pending -> pending.apply());
        registryOps = RegistryOps.create(JsonOps.INSTANCE, registries);
    }

    @Test
    void faunaMaterializationMakesTwoIndependentServerEnforcedOneShotOffers() {
        MerchantOffer faunaA = WanderingRibbitNaturalistFaunaTradeProvider.faunaOffer(
                Items.GLOW_BERRIES, 4, new ItemStack(Items.EGG));
        MerchantOffer faunaB = WanderingRibbitNaturalistFaunaTradeProvider.faunaOffer(
                Items.GLOW_BERRIES, 6, new ItemStack(Items.SLIME_BALL));

        assertEquals(1, faunaA.getMaxUses());
        assertEquals(1, faunaB.getMaxUses());
        assertInstanceOf(WanderingRibbitOneShotOffer.class, faunaA);
        assertInstanceOf(WanderingRibbitOneShotOffer.class, faunaB);

        ItemStack firstPayment = new ItemStack(Items.GLOW_BERRIES, 8);
        assertTrue(completeMerchantResultSlotTransaction(faunaA, firstPayment));
        assertEquals(1, faunaA.getUses());
        assertTrue(faunaA.isOutOfStock());
        assertEquals(4, firstPayment.getCount());
        assertFalse(faunaB.isOutOfStock(), "the second selected fauna remains independent");

        int paymentBeforeRejectedRepeat = firstPayment.getCount();
        assertFalse(completeMerchantResultSlotTransaction(faunaA, firstPayment),
                "the server transaction rejects an exhausted result even when payment remains");
        assertEquals(paymentBeforeRejectedRepeat, firstPayment.getCount(),
                "the rejected completion consumes no extra Glowcaps");
        assertEquals(1, faunaA.getUses());

        ItemStack secondPayment = new ItemStack(Items.GLOW_BERRIES, 6);
        assertTrue(completeMerchantResultSlotTransaction(faunaB, secondPayment));
        assertEquals(1, faunaB.getUses());
        assertTrue(faunaB.isOutOfStock());
    }

    @Test
    void reloadRestoresOneShotGuardAndOrdinaryRestockCannotRefreshFauna() {
        MerchantOffer ordinary = new MerchantOffer(
                new ItemCost(Items.EMERALD, 1), new ItemStack(Items.STICK), 2, 0, 0.0F);
        ordinary.increaseUses();
        MerchantOffer faunaA = WanderingRibbitNaturalistFaunaTradeProvider.faunaOffer(
                Items.GLOW_BERRIES, 4, new ItemStack(Items.EGG));
        MerchantOffer faunaB = WanderingRibbitNaturalistFaunaTradeProvider.faunaOffer(
                Items.GLOW_BERRIES, 6, new ItemStack(Items.SLIME_BALL));
        faunaA.increaseUses();
        faunaB.increaseUses();

        MerchantOffers offers = new MerchantOffers();
        offers.add(ordinary);
        offers.add(roundTrip(faunaA));
        offers.add(roundTrip(faunaB));
        WanderingRibbitTradeSnapshot snapshot = new WanderingRibbitTradeSnapshot(73L, List.of(
                new WanderingRibbitTradeSnapshot.ProviderRange(
                        WanderingRibbitTradeProviders.NATIVE_PROVIDER_ID, 4, 0, 1,
                        WanderingRibbitTradeSnapshot.RestockPolicy.ORDINARY),
                new WanderingRibbitTradeSnapshot.ProviderRange(
                        WanderingRibbitTradeProviders.OPTIONAL_NATURALIST_FAUNA_PROVIDER_ID, 1, 1, 2,
                        WanderingRibbitTradeSnapshot.RestockPolicy.NEVER_RESTOCK)
        ));

        WanderingRibbitTradeProviders.restoreNaturalistFaunaOneShotOffers(offers, snapshot);
        assertInstanceOf(WanderingRibbitOneShotOffer.class, offers.get(1));
        assertEquals(1, offers.get(1).getUses());
        assertTrue(offers.get(1).isOutOfStock(), "serialized exhaustion survives reload");
        ItemStack payment = new ItemStack(Items.GLOW_BERRIES, 4);
        assertFalse(completeMerchantResultSlotTransaction(offers.get(1), payment));
        assertEquals(4, payment.getCount(), "restored exhaustion rejects stale completion without payment loss");

        WanderingRibbitTradeProviders.resetOrdinaryProviderUses(offers, snapshot);
        assertEquals(0, offers.getFirst().getUses(), "ordinary provider range still restocks");
        assertEquals(1, offers.get(1).getUses(), "fauna A never restocks");
        assertEquals(1, offers.get(2).getUses(), "fauna B never restocks");
    }

    /** Mirrors MerchantResultSlot: consume matching payment, then notify the merchant of completion. */
    private static boolean completeMerchantResultSlotTransaction(MerchantOffer offer, ItemStack firstPayment) {
        if (!offer.take(firstPayment, ItemStack.EMPTY)) {
            return false;
        }
        offer.increaseUses();
        return true;
    }

    private static MerchantOffer roundTrip(MerchantOffer offer) {
        JsonElement encoded = MerchantOffer.CODEC.encodeStart(registryOps, offer).getOrThrow();
        return MerchantOffer.CODEC.parse(registryOps, encoded).getOrThrow();
    }
}
