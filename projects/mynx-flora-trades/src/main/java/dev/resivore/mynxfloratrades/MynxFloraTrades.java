package dev.resivore.mynxfloratrades;

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
        RibbitTradeModule.registerExternalOffers(id("farmer_flora_buybacks"), FloraTradeCatalog.farmerOffers());
        WanderingRibbitTradeProviders.register(new FloraWanderingProvider());
    }

    private static final class FloraWanderingProvider implements WanderingRibbitTradeProvider {
        @Override public Identifier id() { return FLORA_PROVIDER; }
        @Override public int schemaVersion() { return 1; }
        @Override public WanderingRibbitTradeSnapshot.RestockPolicy restockPolicy() {
            return WanderingRibbitTradeSnapshot.RestockPolicy.ORDINARY;
        }
        @Override public void contributeOffers(WanderingRibbitTradeContext context, OfferCollector offers) {
            for (List<FloraTradeCatalog.Offer> group : FloraTradeCatalog.wanderingGroups()) {
                List<FloraTradeCatalog.Offer> options = group;
                offers.add(offer(options.get(context.random().nextInt(options.size()))));
            }
        }
        private static MerchantOffer offer(FloraTradeCatalog.Offer definition) {
            return new MerchantOffer(new ItemCost(item(definition.inputId()), definition.inputCount()),
                    new ItemStack(item(definition.outputId()), definition.outputCount()), 16, 0, 0.0F);
        }
        private static Item item(String id) {
            return BuiltInRegistries.ITEM.getOptional(Identifier.parse(id))
                    .orElseThrow(() -> new IllegalStateException("Required flora integration item missing: " + id));
        }
    }
}
