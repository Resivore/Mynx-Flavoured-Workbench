package com.yungnickyoung.minecraft.ribbits.entity.trade;

import com.yungnickyoung.minecraft.ribbits.RibbitsCommon;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;

import java.util.ArrayList;
import java.util.List;

/**
 * Optional, all-or-nothing Naturalist fauna catalogue.  It deliberately names only registry
 * identifiers: Ribbits never loads a Naturalist class and remains usable without the mod.
 */
final class WanderingRibbitNaturalistFaunaTradeProvider implements WanderingRibbitTradeProvider {
    static final int OFFER_COUNT = 2;
    private static final int SCHEMA_VERSION = 1;
    private static final Identifier GLOWCAP = RibbitsCommon.id("glowcap");
    private static final List<Fauna> FAUNA = List.of(
            bucket("bass_bucket", 4, "bass", "bass_medium", "bass_large"),
            bucket("catfish_bucket", 4),
            bucket("starfish_bucket", 4, "orange", "purple", "blue", "red"),
            bucket("blobfish_bucket", 6),
            bucket("jellyfish_bucket", 6, "white", "orange", "pink", "blue", "green"),
            bucket("giant_isopod_bucket", 6, "brown", "blue"),
            bucket("piranha_bucket", 8),
            bucket("ray_bucket", 8, "eagle_ray", "mobula_ray", "stingray"),
            bucket("anglerfish_bucket", 8, "red", "glow"),
            baby("deer", 8), baby("zebra", 8), baby("boar", 8), baby("lizard", 12),
            baby("giraffe", 12), baby("rhino", 12), baby("hippo", 12), baby("mole", 12),
            baby("turkey", 12), baby("bear", 16), baby("black_bear", 16), baby("capybara", 16),
            baby("hedgehog", 16), baby("duck", 16), baby("komodo_dragon", 16),
            baby("lion", 20), baby("tiger", 20), baby("elephant", 24), baby("mammoth", 24)
    );

    @Override
    public Identifier id() {
        return WanderingRibbitTradeProviders.OPTIONAL_NATURALIST_FAUNA_PROVIDER_ID;
    }

    @Override
    public int schemaVersion() {
        return SCHEMA_VERSION;
    }

    @Override
    public WanderingRibbitTradeSnapshot.RestockPolicy restockPolicy() {
        return WanderingRibbitTradeSnapshot.RestockPolicy.NEVER_RESTOCK;
    }

    @Override
    public void contributeOffers(WanderingRibbitTradeContext context, OfferCollector offers) {
        Item glowcap = BuiltInRegistries.ITEM.getOptional(GLOWCAP).orElseThrow(() ->
                new IllegalStateException("Missing Ribbits Glowcap"));
        // Resolve every Naturalist item before adding anything. A partial/changed registry is an
        // optional-provider failure, so the outer transaction records a zero-width range.
        List<Fauna> pool = new ArrayList<>(FAUNA);
        List<ItemStack> resolved = new ArrayList<>(pool.size());
        for (Fauna fauna : pool) {
            resolved.add(fauna.materialize(context));
        }
        for (int index = 0; index < OFFER_COUNT; index++) {
            int selected = index + context.random().nextInt(pool.size() - index);
            Fauna chosenFauna = pool.get(selected);
            ItemStack chosenStack = resolved.get(selected);
            pool.set(selected, pool.get(index));
            resolved.set(selected, resolved.get(index));
            pool.set(index, chosenFauna);
            resolved.set(index, chosenStack);
            offers.add(new MerchantOffer(new ItemCost(glowcap, chosenFauna.price()), chosenStack,
                    1, 0, 0.0F));
        }
    }

    static List<Fauna> fauna() {
        return FAUNA;
    }

    private static Fauna baby(String species, int price) {
        return new Fauna(species, price, true, List.of());
    }

    private static Fauna bucket(String item, int price, String... variants) {
        return new Fauna(item, price, false, List.of(variants));
    }

    record Fauna(String path, int price, boolean baby, List<String> variants) {
        private ItemStack materialize(WanderingRibbitTradeContext context) {
            Identifier itemId = Identifier.fromNamespaceAndPath("naturalist",
                    baby ? path + "_spawn_egg" : path);
            Item item = BuiltInRegistries.ITEM.getOptional(itemId).orElseThrow(() ->
                    new IllegalStateException("Missing optional Naturalist item " + itemId));
            ItemStack stack = new ItemStack(item);
            if (baby) {
                Identifier entityId = Identifier.fromNamespaceAndPath("naturalist", path);
                var entityType = BuiltInRegistries.ENTITY_TYPE.getOptional(entityId).orElseThrow(() ->
                        new IllegalStateException("Missing Naturalist entity " + entityId));
                CompoundTag data = new CompoundTag();
                data.putInt("Age", -24000);
                stack.set(DataComponents.ENTITY_DATA, TypedEntityData.of(entityType, data));
                stack.set(DataComponents.ITEM_NAME,
                        Component.translatable("trade.ribbits.naturalist_fauna.baby", speciesName(path)));
            } else if (!variants.isEmpty()) {
                String variant = variants.get(context.random().nextInt(variants.size()));
                CompoundTag data = new CompoundTag();
                data.putString("Variant", Identifier.fromNamespaceAndPath("naturalist", variant).toString());
                CustomData customData = CustomData.of(data);
                // Naturalist's buckets read the bucket entity payload; keeping CUSTOM_DATA in
                // sync preserves its legacy tooltip path too.
                stack.set(DataComponents.BUCKET_ENTITY_DATA, customData);
                stack.set(DataComponents.CUSTOM_DATA, customData);
            }
            return stack;
        }

        private static Component speciesName(String path) {
            return Component.translatable("entity.naturalist." + path);
        }
    }
}
