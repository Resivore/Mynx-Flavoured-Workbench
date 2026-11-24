package dev.resivore.mynxmatchatradetweaks;

import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.util.Set;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.zip.ZipFile;
import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SavedTradeCleanupTest {
    @Test
    void everyReviewedStackUsesRegisteredItemsAndComponents() throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        Path project = Path.of(System.getProperty("projectRoot"));
        Path overrides = project.resolve("src/main/resources/resourcepacks/mynx_matcha_trade_tweaks");
        Set<String> providerItems = Set.of("simple_copper_pipes:waxed_copper_pipe", "simple_copper_pipes:waxed_copper_fitting");
        int count = 0;
        try (var zip = new ZipFile(project.resolve("../matcha-flavoured-data/upstream/MF_1_12_5_26_2.zip").toFile())) {
            for (String row : Files.readAllLines(project.resolve("tools/trade_reconciliation.tsv")).subList(1, 308)) {
                String id = row.split("\t", -1)[5];
                if (id.isEmpty()) continue;
                var key = Identifier.parse(id);
                String path = "data/" + key.getNamespace() + "/villager_trade/" + key.getPath() + ".json";
                Path override = overrides.resolve(path);
                String json;
                if (Files.exists(override)) {
                    json = Files.readString(override);
                } else {
                    try (var stream = zip.getInputStream(zip.getEntry(path))) {
                        json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                    }
                }
                var trade = JsonParser.parseString(json).getAsJsonObject();
                for (String field : Set.of("wants", "additional_wants", "gives")) {
                    if (!trade.has(field)) continue;
                    var stack = trade.getAsJsonObject(field);
                    String item = stack.get("id").getAsString();
                    assertTrue(providerItems.contains(item) || BuiltInRegistries.ITEM.containsKey(Identifier.parse(item)), id + " " + item);
                    if (stack.has("components")) {
                        for (String component : stack.getAsJsonObject("components").keySet()) {
                            String type = component.startsWith("!") ? component.substring(1) : component;
                            assertTrue(BuiltInRegistries.DATA_COMPONENT_TYPE.containsKey(Identifier.parse(type)), id + " " + type);
                        }
                    }
                }
                count++;
            }
        }
        assertEquals(280, count);
    }

    @Test
    void publishedFoodOffersAndStaticSalesRemainDistinguishableAfterSerialization() throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        var builtIns = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        var registries = HolderLookup.Provider.create(Stream.concat(builtIns.listRegistries(),
                VanillaRegistries.createLookup().listRegistries().filter(r -> builtIns.lookup(r.key()).isEmpty())));
        BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(registries).forEach(pending -> pending.apply());
        var ops = registries.createSerializationContext(NbtOps.INSTANCE);
        Path archive = Path.of(System.getProperty("projectRoot"),
                "../matcha-flavoured-data/upstream/MF_1_12_5_26_2.zip");
        try (var zip = new ZipFile(archive.toFile())) {
            for (int i = 0; i < 42; i++) {
                String path = "data/matcha/function/favorite_food/food_trades/food_" + i + ".mcfunction";
                String source = new String(zip.getInputStream(zip.getEntry(path)).readAllBytes(), StandardCharsets.UTF_8);
                String raw = source.substring(source.indexOf("append value ") + "append value ".length(),
                        source.indexOf("tag @s add foodChecked")).replaceAll("\\\\\\s*\\R", "").trim();
                var tag = TagParser.parseCompoundFully(raw);
                var buy = tag.getCompoundOrEmpty("buy");
                Identifier model = Identifier.parse(buy.getCompoundOrEmpty("components").getStringOr("minecraft:item_model", ""));
                var item = BuiltInRegistries.ITEM.getValue(Identifier.parse(buy.getStringOr("id", "")));
                ItemCost food = new ItemCost(item).withComponents(b -> b.expect(DataComponents.ITEM_MODEL, model));
                MerchantOffer offer = new MerchantOffer(food, Optional.empty(), new ItemStack(Items.EMERALD), 1, 0, 0);
                var saved = MerchantOffer.CODEC.encodeStart(ops, offer).getOrThrow();
                assertTrue(SavedTradeCleanup.isFavoriteFood(MerchantOffer.CODEC.parse(ops, saved).getOrThrow()), path);
                // If vanilla accepts the writer's entire historical SNBT, check that exact decoded shape too.
                MerchantOffer.CODEC.parse(ops, tag).result().ifPresent(actual ->
                        assertTrue(SavedTradeCleanup.isFavoriteFood(actual), path));
                assertFalse(SavedTradeCleanup.isFavoriteFood(new MerchantOffer(new ItemCost(Items.EMERALD),
                        Optional.empty(), food.itemStack(), 1, 0, 0)), path);
                assertFalse(SavedTradeCleanup.isFavoriteFood(new MerchantOffer(food, Optional.of(new ItemCost(Items.BOWL)),
                        new ItemStack(Items.EMERALD), 1, 0, 0)), path);
            }
        }
    }
}
