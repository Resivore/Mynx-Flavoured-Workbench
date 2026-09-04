package com.yungnickyoung.minecraft.ribbits.entity.trade;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentExactPredicate;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WanderingRibbitCompassTradesTest {
    private static RegistryOps<JsonElement> registryOps;

    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        RegistryAccess builtIns = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        HolderLookup.Provider vanillaData = VanillaRegistries.createLookup();
        HolderLookup.Provider registries = HolderLookup.Provider.create(
                java.util.stream.Stream.concat(
                        builtIns.listRegistries(),
                        vanillaData.listRegistries().filter(
                                lookup -> builtIns.lookup(lookup.key()).isEmpty())
                )
        );
        BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(registries)
                .forEach(pending -> pending.apply());
        registryOps = RegistryOps.create(JsonOps.INSTANCE, registries);
    }

    @Test
    void identityPredicateSeparatesEveryTierButAllowsMutableCompassData() {
        ItemStack ordinary = new ItemStack(Items.COMPASS);
        ItemStack copper = compass("minecraft:copper_compass",
                "item.kleispack.copper_compass");
        ItemStack golden = compass("minecraft:golden_compass",
                "item.kleispack.golden_compass");
        ItemStack titanium = compass("minecraft:titanium_compass",
                "item.kleispack.titanium_compass");
        List<ItemStack> tiers = List.of(ordinary, copper, golden, titanium);

        for (int costIndex = 0; costIndex < tiers.size(); costIndex++) {
            ItemStack template = tiers.get(costIndex);
            ItemCost cost = WanderingRibbitCompassTrades.identityCost(template);
            DataComponentExactPredicate expectedIdentity = DataComponentExactPredicate.builder()
                    .expect(DataComponents.ITEM_MODEL, template.get(DataComponents.ITEM_MODEL))
                    .expect(DataComponents.ITEM_NAME, template.get(DataComponents.ITEM_NAME))
                    .build();
            assertEquals(expectedIdentity, cost.components(),
                    "only item-model and item-name define a compass tier");

            for (int candidateIndex = 0; candidateIndex < tiers.size(); candidateIndex++) {
                assertEquals(costIndex == candidateIndex, cost.test(tiers.get(candidateIndex)),
                        "tier " + costIndex + " cost versus tier " + candidateIndex);
            }

            ItemStack customized = template.copy();
            customized.set(DataComponents.CUSTOM_NAME, Component.literal("Field Compass"));
            customized.set(DataComponents.LODESTONE_TRACKER,
                    new LodestoneTracker(Optional.empty(), false));
            assertTrue(cost.test(customized),
                    "custom names and lodestone tracking are unrelated mutable data");
        }

        ItemStack nameOnlyForgery = ordinary.copy();
        nameOnlyForgery.set(DataComponents.ITEM_NAME,
                Component.translatable("item.kleispack.titanium_compass"));
        assertFalse(WanderingRibbitCompassTrades.identityCost(titanium).test(nameOnlyForgery),
                "matching display text without the audited model is not a tier identity");
    }

    @Test
    void identityCostRejectsMissingIdentityComponentsAndNonCompassBases() {
        ItemStack missingModel = new ItemStack(Items.COMPASS);
        missingModel.remove(DataComponents.ITEM_MODEL);
        assertThrows(IllegalArgumentException.class,
                () -> WanderingRibbitCompassTrades.identityCost(missingModel));

        ItemStack missingName = new ItemStack(Items.COMPASS);
        missingName.remove(DataComponents.ITEM_NAME);
        assertThrows(IllegalArgumentException.class,
                () -> WanderingRibbitCompassTrades.identityCost(missingName));
        assertThrows(IllegalArgumentException.class,
                () -> WanderingRibbitCompassTrades.identityCost(new ItemStack(Items.CLOCK)));
    }

    @Test
    void buybackTermsAreTwoUsesZeroXpAndZeroPriceMovement() {
        MerchantOffer offer = WanderingRibbitCompassTrades.buyback(
                compass("minecraft:copper_compass", "item.kleispack.copper_compass"),
                Items.EMERALD, 2);

        assertFalse(offer instanceof StrictMerchantOffer,
                "compass identities intentionally allow unrelated extra components");
        assertEquals(2, offer.getResult().getCount());
        assertTrue(offer.getResult().is(Items.EMERALD));
        assertEquals(2, offer.getMaxUses());
        assertEquals(0, offer.getXp());
        assertEquals(0.0F, offer.getPriceMultiplier());
    }

    @Test
    void intendedFourBuybacksKeepTheirExactRewardOrderAndTierMatching() {
        ItemStack ordinary = new ItemStack(Items.COMPASS);
        ItemStack copper = compass("minecraft:copper_compass",
                "item.kleispack.copper_compass");
        ItemStack golden = compass("minecraft:golden_compass",
                "item.kleispack.golden_compass");
        ItemStack titanium = compass("minecraft:titanium_compass",
                "item.kleispack.titanium_compass");
        List<ItemStack> inputs = List.of(ordinary, copper, golden, titanium);

        MerchantOffer ordinaryOffer = WanderingRibbitCompassTrades.buyback(
                ordinary, Items.EMERALD, 4);
        List<MerchantOffer> optionalOffers =
                WanderingRibbitMatchaCompassTradeProvider.resolvedOffers(
                        List.of(copper, golden, titanium), Items.EMERALD);
        List<MerchantOffer> offers = List.of(
                ordinaryOffer, optionalOffers.get(0), optionalOffers.get(1),
                optionalOffers.get(2));

        assertEquals(List.of(4, 2, 4, 8),
                offers.stream().map(offer -> offer.getResult().getCount()).toList());
        for (int offerIndex = 0; offerIndex < offers.size(); offerIndex++) {
            MerchantOffer offer = offers.get(offerIndex);
            assertEquals(2, offer.getMaxUses());
            assertEquals(0, offer.getXp());
            assertEquals(0.0F, offer.getPriceMultiplier());
            for (int inputIndex = 0; inputIndex < inputs.size(); inputIndex++) {
                assertEquals(offerIndex == inputIndex,
                        offer.satisfiedBy(inputs.get(inputIndex), ItemStack.EMPTY));
            }
        }

        assertThrows(IllegalArgumentException.class, () ->
                WanderingRibbitMatchaCompassTradeProvider.resolvedOffers(
                        List.of(copper, golden), Items.EMERALD));
    }

    @Test
    void vanillaOfferCodecPreservesTheTwoComponentIdentityPredicate() {
        ItemStack titanium = compass("minecraft:titanium_compass",
                "item.kleispack.titanium_compass");
        MerchantOffer encoded = WanderingRibbitCompassTrades.buyback(
                titanium, Items.EMERALD, 8);
        JsonElement json = MerchantOffer.CODEC.encodeStart(registryOps, encoded).getOrThrow();
        MerchantOffer decoded = MerchantOffer.CODEC.parse(registryOps, json).getOrThrow();

        ItemStack customizedTitanium = titanium.copy();
        customizedTitanium.set(DataComponents.CUSTOM_NAME, Component.literal("Wayfinder"));
        customizedTitanium.set(DataComponents.LODESTONE_TRACKER,
                new LodestoneTracker(Optional.empty(), false));
        assertTrue(decoded.satisfiedBy(customizedTitanium, ItemStack.EMPTY));
        assertFalse(decoded.satisfiedBy(
                compass("minecraft:copper_compass", "item.kleispack.copper_compass"),
                ItemStack.EMPTY));
        assertInstanceOf(MerchantOffer.class, decoded);
    }

    @Test
    void builtInOrdinaryCompassHasTheAuditedVanillaIdentity() {
        ItemStack ordinary = new ItemStack(Items.COMPASS);
        assertEquals(Identifier.parse("minecraft:compass"),
                ordinary.get(DataComponents.ITEM_MODEL));
        assertEquals(Component.translatable("item.minecraft.compass"),
                ordinary.get(DataComponents.ITEM_NAME));
    }

    private static ItemStack compass(String itemModel, String itemNameKey) {
        ItemStack stack = new ItemStack(Items.COMPASS);
        stack.set(DataComponents.ITEM_MODEL, Identifier.parse(itemModel));
        stack.set(DataComponents.ITEM_NAME, Component.translatable(itemNameKey));
        return stack;
    }
}
