package dev.resivore.mynxfloratrades;

import com.yungnickyoung.minecraft.ribbits.entity.trade.RibbitExternalTradeOffer;
import com.yungnickyoung.minecraft.ribbits.entity.trade.WanderingRibbitTradeContext;
import com.yungnickyoung.minecraft.ribbits.entity.trade.WanderingRibbitTradeProvider;
import com.yungnickyoung.minecraft.ribbits.entity.trade.WanderingRibbitTradeProviders;
import com.yungnickyoung.minecraft.ribbits.entity.trade.WanderingRibbitTradeSnapshot;
import com.yungnickyoung.minecraft.ribbits.module.RibbitTradeModule;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;

import java.util.List;

public final class MynxFloraTrades implements ModInitializer {
    public static final String MOD_ID = "mynx_flora_trades";
    public static final Identifier FLORA_PROVIDER = id("regions_unexplored_flora");

    public static Identifier id(String path) { return Identifier.fromNamespaceAndPath(MOD_ID, path); }

    @Override
    public void onInitialize() {
        RibbitTradeModule.registerExternalOffers(id("farmer_flora_buybacks"), List.of(
                farmer("clover", 1, "tier_1", 0, 64), farmer("stone_bud", 1, "tier_1", 1, 32),
                farmer("barley", 2, "tier_2", 0, 32), farmer("windswept_grass", 2, "tier_2", 1, 32)
        ));
        WanderingRibbitTradeProviders.register(new FloraWanderingProvider());
    }

    private static RibbitExternalTradeOffer farmer(String flora, int tier, String group, int option, int count) {
        return new RibbitExternalTradeOffer("flora_" + group + "_" + flora, "farmer", tier,
                new RibbitTradeModule.CostSpec(RibbitTradeModule.StackRef.item("mynx_regions_unexplored:" + flora), count, false),
                null, RibbitTradeModule.StackRef.item("ribbits:glowcap"), 1, 16, 0, group, 2, option);
    }

    private static final class FloraWanderingProvider implements WanderingRibbitTradeProvider {
        @Override public Identifier id() { return FLORA_PROVIDER; }
        @Override public int schemaVersion() { return 1; }
        @Override public WanderingRibbitTradeSnapshot.RestockPolicy restockPolicy() {
            return WanderingRibbitTradeSnapshot.RestockPolicy.ORDINARY;
        }
        @Override public void contributeOffers(WanderingRibbitTradeContext context, OfferCollector offers) {
            Item glowcap = item("ribbits:glowcap");
            offers.add(buyback(context.random().nextBoolean() ? "dropleaf" : "mycotoxic_daisy", 16, glowcap));
            boolean cattail = context.random().nextBoolean();
            offers.add(buyback(cattail ? "cattail" : "duckweed", cattail ? 16 : 32, glowcap));
        }
        private static MerchantOffer buyback(String flora, int count, Item glowcap) {
            return new MerchantOffer(new ItemCost(item("mynx_regions_unexplored:" + flora), count),
                    new ItemStack(glowcap), 16, 0, 0.0F);
        }
        private static Item item(String id) {
            return BuiltInRegistries.ITEM.getOptional(Identifier.parse(id))
                    .orElseThrow(() -> new IllegalStateException("Required flora integration item missing: " + id));
        }
    }
}
