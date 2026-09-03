package com.yungnickyoung.minecraft.ribbits.entity.trade;

import com.google.gson.JsonElement;
import com.yungnickyoung.minecraft.ribbits.world.loot.RibbitVillageExplorerMap;
import com.mojang.serialization.JsonOps;
import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentExactPredicate;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.saveddata.maps.MapId;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StrictMerchantOfferCodecTest {
    private static RegistryOps<JsonElement> registryOps;
    private static HolderLookup.Provider registryProvider;

    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
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
        registryProvider = registries;
        registryOps = RegistryOps.create(JsonOps.INSTANCE, registries);
    }

    @Test
    void codecRoundTripRestoresAbsenceAwareBenzeneMatching() {
        ItemStack benzene = new ItemStack(Items.ENDERMITE_SPAWN_EGG, 4);
        benzene.remove(DataComponents.ENTITY_DATA);
        StrictMerchantOffer template = new StrictMerchantOffer(
                exactCost(benzene, 4), Optional.empty(), new ItemStack(Items.EMERALD),
                16, 1, 0.0F, true, false);

        MerchantOffer decoded = roundTrip(template);
        assertTrue(decoded.getItemCostA().itemStack().has(DataComponents.ENTITY_DATA),
                "vanilla ItemCost decode reconstructs the spawn egg's default entity data");

        MerchantOffer restored = StrictMerchantOffer.restoreFromTemplate(decoded, template);
        assertTrue(restored.satisfiedBy(benzene.copy(), ItemStack.EMPTY),
                "the exact entity-data-absent Benzene stack remains valid after reload");
        assertFalse(restored.satisfiedBy(new ItemStack(Items.ENDERMITE_SPAWN_EGG, 4), ItemStack.EMPTY),
                "an ordinary Endermite Spawn Egg must remain invalid after reload");
    }

    @Test
    void codecRoundTripRestoresExactDivineFragmentSecondCost() {
        ItemStack divineFragment = new ItemStack(Items.TURTLE_SCUTE);
        divineFragment.set(DataComponents.RARITY, Rarity.RARE);
        divineFragment.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        StrictMerchantOffer template = new StrictMerchantOffer(
                new ItemCost(Items.EMERALD, 16), Optional.of(exactCost(divineFragment, 1)),
                new ItemStack(Items.HEART_OF_THE_SEA), 1, 0, 0.0F, false, true);

        MerchantOffer restored = StrictMerchantOffer.restoreFromTemplate(roundTrip(template), template);
        assertTrue(restored.satisfiedBy(new ItemStack(Items.EMERALD, 16), divineFragment.copy()),
                "the exact rare, glinting Divine Fragment remains valid after reload");
        assertFalse(restored.satisfiedBy(
                        new ItemStack(Items.EMERALD, 16), new ItemStack(Items.TURTLE_SCUTE)),
                "a plain Turtle Scute must remain invalid after reload");
    }

    @Test
    void codecRoundTripKeepsOpalNarrowerThanItsGenericBaseItem() {
        ItemStack opal = MatchaStackCatalog.opal();
        StrictMerchantOffer template = new StrictMerchantOffer(
                exactCost(opal, 1), Optional.empty(), new ItemStack(Items.EMERALD, 16),
                2, 4, 0.0F, true, false);

        MerchantOffer restored = StrictMerchantOffer.restoreFromTemplate(roundTrip(template), template);
        assertTrue(restored.satisfiedBy(opal.copy(), ItemStack.EMPTY));
        assertFalse(restored.satisfiedBy(
                        new ItemStack(Items.FERMENTED_SPIDER_EYE), ItemStack.EMPTY),
                "a plain Fermented Spider Eye must never satisfy the Opal gate");

        ItemStack renamedOpal = opal.copy();
        renamedOpal.set(DataComponents.CUSTOM_NAME, Component.literal("Almost Opal"));
        assertFalse(restored.satisfiedBy(renamedOpal, ItemStack.EMPTY),
                "exact component-bearing gates reject unrelated extra components");
    }

    @Test
    void codecRoundTripRestoresPrivateFailedMapMatchingWithoutDependingOnDisplayText() {
        ItemStack failedMap = RibbitVillageExplorerMap.createFailedMap(1);
        ItemCost failedMapCost = new ItemCost(failedMap.typeHolder(), 1,
                DataComponentExactPredicate.expect(
                        DataComponents.CUSTOM_DATA,
                        RibbitVillageExplorerMap.failedMarker()),
                failedMap);
        StrictMerchantOffer template = new StrictMerchantOffer(
                failedMapCost, Optional.empty(), new ItemStack(Items.HEART_OF_THE_SEA),
                16, 0, 0.0F, false, false, true, false);

        MerchantOffer restored = StrictMerchantOffer.restoreFromTemplate(roundTrip(template), template);
        assertEquals(16, restored.getMaxUses());
        assertEquals(0, restored.getXp());
        assertEquals(0.0F, restored.getPriceMultiplier());
        assertEquals(failedMap.get(DataComponents.CUSTOM_NAME),
                restored.getItemCostA().itemStack().get(DataComponents.CUSTOM_NAME),
                "the visible failed-map cost is restored after vanilla codec decoding");
        assertTrue(restored.satisfiedBy(failedMap.copy(), ItemStack.EMPTY));

        ItemStack renamedGenuine = failedMap.copy();
        renamedGenuine.set(DataComponents.CUSTOM_NAME, Component.literal("Still genuine"));
        assertTrue(restored.satisfiedBy(renamedGenuine, ItemStack.EMPTY),
                "authenticity is the private marker rather than display text");

        ItemStack generic = new ItemStack(Items.MAP);
        assertFalse(restored.satisfiedBy(generic, ItemStack.EMPTY));
        generic.set(DataComponents.CUSTOM_NAME,
                Component.translatable(RibbitVillageExplorerMap.FAILURE_NAME_KEY));
        assertFalse(restored.satisfiedBy(generic, ItemStack.EMPTY),
                "a renamed generic empty map is not redeemable");

        ItemStack successful = new ItemStack(Items.FILLED_MAP);
        successful.set(DataComponents.MAP_ID, new MapId(7));
        assertFalse(restored.satisfiedBy(successful, ItemStack.EMPTY));

        ItemStack staleMappedFailure = failedMap.copy();
        staleMappedFailure.set(DataComponents.MAP_ID, new MapId(7));
        assertFalse(restored.satisfiedBy(staleMappedFailure, ItemStack.EMPTY),
                "even a marked stack with map data is not an empty-search failure");
    }

    @Test
    void ordinaryEquipmentBuybackAcceptsDamageEnchantmentsCurseRepairCostAndCustomName() {
        MerchantOffer buyback = new MerchantOffer(
                new ItemCost(Items.IRON_PICKAXE, 1), new ItemStack(Items.EMERALD, 8),
                4, 1, 0.0F);

        ItemStack customizedPickaxe = new ItemStack(Items.IRON_PICKAXE);
        customizedPickaxe.setDamageValue(37);
        customizedPickaxe.set(DataComponents.REPAIR_COST, 9);
        customizedPickaxe.set(DataComponents.CUSTOM_NAME, Component.literal("Old Reliable"));
        var enchantments = registryProvider.lookupOrThrow(Registries.ENCHANTMENT);
        EnchantmentHelper.updateEnchantments(customizedPickaxe, mutable -> {
            mutable.set(enchantments.getOrThrow(Enchantments.EFFICIENCY), 3);
            mutable.set(enchantments.getOrThrow(Enchantments.VANISHING_CURSE), 1);
        });

        assertTrue(buyback.getItemCostA().test(customizedPickaxe));
        assertTrue(buyback.satisfiedBy(customizedPickaxe, ItemStack.EMPTY),
                "ordinary equipment costs deliberately ignore all additional components");

        ItemStack wrongEquipment = new ItemStack(Items.IRON_AXE);
        wrongEquipment.set(DataComponents.REPAIR_COST, 9);
        wrongEquipment.set(DataComponents.CUSTOM_NAME, Component.literal("Old Reliable"));
        assertFalse(buyback.getItemCostA().test(wrongEquipment));
        assertFalse(buyback.satisfiedBy(wrongEquipment, ItemStack.EMPTY),
                "component tolerance must never relax the underlying item identity");
    }

    @Test
    void neverOpenedRibbitInitializesOfferSchemaBeforeSavingTradeState() throws IOException {
        Path projectRoot = Path.of(System.getProperty("projectRoot"));
        String source = Files.readString(projectRoot.resolve(
                "common/src/main/java/com/yungnickyoung/minecraft/ribbits/entity/RibbitEntity.java"));
        int saveMethod = source.indexOf("protected void addAdditionalSaveData");
        int offerInitialization = source.indexOf("offersToSave = this.getOffers();", saveMethod);
        int stateWrite = source.indexOf("this.tradeState.write(valueOutput);", saveMethod);

        assertTrue(saveMethod >= 0 && offerInitialization > saveMethod,
                "save path must initialize a never-opened Ribbit's offers");
        assertTrue(stateWrite > offerInitialization,
                "offer construction must establish the schema before the state is persisted");
    }

    private static ItemCost exactCost(ItemStack stack, int count) {
        ItemStack counted = stack.copyWithCount(count);
        return new ItemCost(counted.typeHolder(), count,
                DataComponentExactPredicate.allOf(counted.getComponents()), counted);
    }

    private static MerchantOffer roundTrip(MerchantOffer offer) {
        JsonElement encoded = MerchantOffer.CODEC.encodeStart(registryOps, offer).getOrThrow();
        return MerchantOffer.CODEC.parse(registryOps, encoded).getOrThrow();
    }
}
