package dev.resivore.wearablelanterns.gametest;

import java.util.List;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

/** Registers the five audited provider IDs only for the explicit provider-present GameTest run. */
public final class ProviderFixtureInitializer implements ModInitializer {
    public static final String FIXTURES_PROPERTY = "wearableLanterns.providerFixtures";
    public static final List<Identifier> APPROVED_PROVIDER_ITEMS = List.of(
            Identifier.parse("enderscape:void_lantern"),
            Identifier.parse("enderscape:bulb_lantern"),
            Identifier.parse("ribbits:swamp_lantern"),
            Identifier.parse("auroraslanterns:amethyst_lantern"),
            Identifier.parse("auroraslanterns:redstone_lantern"));

    @Override
    public void onInitialize() {
        if (!fixturesEnabled()) {
            return;
        }
        APPROVED_PROVIDER_ITEMS.forEach(ProviderFixtureInitializer::registerItem);
    }

    public static boolean fixturesEnabled() {
        return Boolean.getBoolean(FIXTURES_PROPERTY);
    }

    private static void registerItem(Identifier id) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
        Registry.register(
                BuiltInRegistries.ITEM,
                key,
                new Item(new Item.Properties().setId(key)));
    }
}
