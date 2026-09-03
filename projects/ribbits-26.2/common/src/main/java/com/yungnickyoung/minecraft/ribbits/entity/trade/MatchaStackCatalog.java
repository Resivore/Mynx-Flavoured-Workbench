package com.yungnickyoung.minecraft.ribbits.entity.trade;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.world.level.Level;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Resolves the exact component-bearing stacks supplied by the fixed Matcha 26.2 pack.
 *
 * <p>The recipes remain the canonical component contracts. Looking up their result displays keeps
 * food effects, duration, lore, names, models, consumable settings and enchantments byte-for-byte
 * aligned with the active data pack instead of duplicating those definitions in Java.</p>
 */
public final class MatchaStackCatalog {
    private static final Gson CANONICAL_GSON = new GsonBuilder().disableHtmlEscaping().create();
    static final Identifier OPAL_LOOT_RESOURCE =
            Identifier.parse("minecraft:loot_table/kleis_items/opal.json");
    static final String OPAL_ENTRY_CONTRACT_SHA256 =
            "81d6a8b292697f291b3025dbfe4b591fbe86a35b1231164cbdd05d8a8bd04c0e";

    private MatchaStackCatalog() {
    }

    /**
     * SHA-256 values cover canonicalized recipe {@code result} JSON, not protected resource bytes.
     * Matcha contracts were derived from immutable Matcha_Flavoured_1_12.zip
     * (SHA-256 6209783021C358044ABEDABACEE471FAFF5BD4080437D4E3B5E51963F1804248).
     * Crystal Heart is also verified against the coordinated matcha-heart-death-compat recipe.
     */
    public enum FixedStack {
        GLOW_BERRY_CRUMBLE("food:glow_berry_crumble", "minecraft:poisonous_potato",
                "c064c3201c4c44dedd6f8e9ec51107d6bf8db48cb9ca755b3dd69b4cb8fe54d8"),
        HONEY_GINGER_TEA("food:honey_ginger_tea", "minecraft:poisonous_potato",
                "0e3faaf4946150a2ced63bb76c7ade6763329ed6cf98959cefa125f69d536e02"),
        PICKLED_CARROTS("food:pickled_carrots", "minecraft:poisonous_potato",
                "b844bc604d3c9ac0ef6ccf6dc6eb0a07b4a0290b7ed7d4525b007b572640e8d0"),
        RIND_JAM("food:rind_jam", "minecraft:poisonous_potato",
                "0a0b06c482585a83e71ead3addb4ee7629352e091aec34b18fc8b7ec13149734"),
        GIMMARI("food:gimmari", "minecraft:poisonous_potato",
                "d7e097e932457245e04fd775191a89b77290b36967909ad94e1b539576d85356"),
        BOKGUK("food:bokguk", "minecraft:poisonous_potato",
                "a9c4b66d5822f96f5f6ff1dc130985ed19904f30d71ddc3358bbc50cf72807db"),
        GOLDEN_PICKLED_CARROTS("food:golden_pickled_carrots", "minecraft:poisonous_potato",
                "491828999ebf66e4c087b1147ffc02dcd52cce734c87db1895ccdcb09be8bd3b"),
        MELON_SORBET("food:melon_sorbet", "minecraft:poisonous_potato",
                "0a0a305672c275c80bc1542da1dafdbdbb9b3b1e6836311fb216f990f0d261be"),
        PUMPKIN_EMPANADA("food:pumpkin_empanada", "minecraft:poisonous_potato",
                "4c8f335cf0b68a6a7c0e081ece849a61bf4fc56780acfdca6af8ec0a61ee10e9"),
        WARPED_PIZZA("food:warped_pizza", "minecraft:poisonous_potato",
                "3348e8211f9785d168a5e961852a280cfe7467355c32b0637581d4f455d34ef5"),
        WARPED_STROGANOFF("food:warped_stroganoff", "minecraft:poisonous_potato",
                "160ef7524ae7b5b7a16c814d45228dbd68add234e5b76066ba6b1836fa80d6cf"),
        SWEET_BERRY_DANISH("food:sweet_berry_danish", "minecraft:poisonous_potato",
                "d4ac8d81e6051ec9443e32000e0873c7ffdf0a270291ee5aeed4c1f5c1608062"),
        GOLDEN_CARROT_CUPCAKE("food:golden_carrot_cupcake", "minecraft:poisonous_potato",
                "5f10d771f35a3a65fa0af46fd649b45ccc1d40a6568ebd3e34350ef460dafad7"),
        JAPANESE_CURRY("food:japanese_curry", "minecraft:poisonous_potato",
                "5f0cc28abe03a02a29adac81b2b51a214f9ffcb808cf550f98366be4de8fbc4d"),
        GREEN_CURRY("food:green_curry", "minecraft:poisonous_potato",
                "fb7f996ffe2bbb3b7bac03e8e34f25209436b35e6a3f0213a24e00310a55b10b"),
        TONKOTSU_RAMEN("food:ramen", "minecraft:poisonous_potato",
                "aacd5a5fd956eaca546d5494f7efca863909562291e00cf3552918f2341c0da8"),
        BENZENE("crafting:benzene", "minecraft:endermite_spawn_egg",
                "e487be207d1243e21ad3e22b2aae4f01f36991e01f0c1c8818bf2563da93474c"),
        ESTUS_FLASK("crafting:estus_flask", "minecraft:potion",
                "ec7b84853cad4b17a8edb5992e33604465c12f36b2cfcb1e485307b8aa4cc56a"),
        REACH_BLESSING("blessings:reach", "minecraft:enchanted_book",
                "96a224ce3ea7d251fbb1fad4de8273b1a5ef67045c0c418f1b7b656b35f1e62d"),
        SILK_TOUCH_BLESSING("blessings:silk_touch", "minecraft:enchanted_book",
                "07e7a800dad6478c06038ec63ed56784abd21cc0c88e1772594dfd058fbb1cac"),
        CARBON_RICH_IRON("crafting:carbon_rich_iron", "minecraft:piglin_brute_spawn_egg",
                "768c3c7d98cec838607b075310f164e044eeb7e6e60bd5fc54c5f2072cb11b06"),
        HEPATIZON_ALLOY("crafting:bronze_alloy", "minecraft:phantom_membrane",
                "eb76460c156a6bd20b103b8ed747e0c6e540ed2a1163fa5f768a5e78de02adf8"),
        SHAKUDO_ALLOY("crafting:shakudo_alloy", "minecraft:shulker_shell",
                "89b858dfdffadc865c29f9ca055869fbd70a913992c12f03a346e38a0493dc41"),
        ELECTRUM_ALLOY("crafting:electrum_alloy", "minecraft:heart_of_the_sea",
                "b9d815288f637d303bb5e28ea8ad5cf2d97089debfda218eceb8cb0ef0d41a3f"),
        DIVINE_FRAGMENT("crafting:divine_fragment", "minecraft:turtle_scute",
                "593a62e2da9c4c1e3cad1f173bf143b81243a3579e6b9a2a87b0b78582df4174"),
        CRYSTAL_HEART("crafting:crystal_heart", "minecraft:poisonous_potato",
                "34f33cb21c8433418e547ccc97d869097adfe19104331f0885d993d86c16ba6b");

        private final Identifier recipeId;
        private final Identifier expectedItemId;
        private final String resultContractSha256;

        FixedStack(String recipeId, String expectedItemId, String resultContractSha256) {
            this.recipeId = Identifier.parse(recipeId);
            this.expectedItemId = Identifier.parse(expectedItemId);
            this.resultContractSha256 = resultContractSha256;
        }

        public Identifier recipeId() {
            return this.recipeId;
        }

        public Identifier expectedItemId() {
            return this.expectedItemId;
        }

        public String resultContractSha256() {
            return this.resultContractSha256;
        }
    }

    public static ItemStack resolve(Level level, FixedStack fixedStack) {
        if (!(level instanceof ServerLevel serverLevel)) {
            throw new IllegalStateException("Exact Matcha stacks may only be resolved server-side");
        }

        validateRecipeResultContract(serverLevel, fixedStack);
        ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, fixedStack.recipeId());
        RecipeHolder<?> holder = serverLevel.recipeAccess().byKey(key).orElseThrow(() ->
                new IllegalStateException("Missing required Matcha recipe " + fixedStack.recipeId()));
        List<RecipeDisplay> displays = holder.value().display();
        for (RecipeDisplay display : displays) {
            ItemStack result = display.result().resolveForFirstStack(SlotDisplayContext.fromLevel(level));
            if (!result.isEmpty()) {
                Identifier actualId = BuiltInRegistries.ITEM.getKey(result.getItem());
                if (!fixedStack.expectedItemId().equals(actualId)) {
                    throw new IllegalStateException("Matcha recipe " + fixedStack.recipeId()
                            + " resolved to " + actualId + ", expected " + fixedStack.expectedItemId());
                }
                return result.copy();
            }
        }
        throw new IllegalStateException("Matcha recipe has no resolvable result display: "
                + fixedStack.recipeId());
    }

    /** Exact current Opal loot-stack contract from minecraft:kleis_items/opal. */
    public static ItemStack opal() {
        ItemStack stack = new ItemStack(Items.FERMENTED_SPIDER_EYE);
        stack.set(DataComponents.ITEM_MODEL, Identifier.parse("minecraft:opal"));
        stack.set(DataComponents.ITEM_NAME,
                Component.translatable("item.kleispack.opal").withColor(0xAB85AD));
        return stack;
    }

    public static Item requiredItem(String id) {
        Identifier identifier = Identifier.parse(id);
        return BuiltInRegistries.ITEM.getOptional(identifier).orElseThrow(() ->
                new IllegalStateException("Missing required economy item " + identifier));
    }

    /** Fail closed on all fixed-stack contracts once server recipes are available. */
    public static void validateAll(ServerLevel level) {
        for (FixedStack value : FixedStack.values()) {
            resolve(level, value);
        }
        validateOpalContract(level);
        ItemStack opal = opal();
        if (!Identifier.parse("minecraft:fermented_spider_eye")
                .equals(BuiltInRegistries.ITEM.getKey(opal.getItem()))) {
            throw new IllegalStateException("Opal base-item contract changed");
        }
    }

    static Identifier recipeResource(Identifier recipeId) {
        return Identifier.fromNamespaceAndPath(recipeId.getNamespace(),
                "recipe/" + recipeId.getPath() + ".json");
    }

    static String canonicalContractSha256(JsonElement value) {
        try {
            byte[] canonical = canonicalJson(value).getBytes(StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    static String canonicalJson(JsonElement value) {
        if (value.isJsonObject()) {
            TreeMap<String, JsonElement> sorted = new TreeMap<>();
            for (Map.Entry<String, JsonElement> entry : value.getAsJsonObject().entrySet()) {
                sorted.put(entry.getKey(), entry.getValue());
            }
            StringBuilder result = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<String, JsonElement> entry : sorted.entrySet()) {
                if (!first) {
                    result.append(',');
                }
                first = false;
                result.append(CANONICAL_GSON.toJson(entry.getKey()))
                        .append(':')
                        .append(canonicalJson(entry.getValue()));
            }
            return result.append('}').toString();
        }
        if (value.isJsonArray()) {
            StringBuilder result = new StringBuilder("[");
            boolean first = true;
            for (JsonElement element : value.getAsJsonArray()) {
                if (!first) {
                    result.append(',');
                }
                first = false;
                result.append(canonicalJson(element));
            }
            return result.append(']').toString();
        }
        return CANONICAL_GSON.toJson(value);
    }

    private static void validateRecipeResultContract(ServerLevel level, FixedStack fixedStack) {
        Identifier resourceId = recipeResource(fixedStack.recipeId());
        JsonElement root = readJson(level.getServer().getResourceManager(), resourceId);
        if (!root.isJsonObject() || !root.getAsJsonObject().has("result")) {
            throw new IllegalStateException("Required Matcha recipe resource has no result: " + resourceId);
        }
        validateFingerprint("Matcha recipe result " + fixedStack.recipeId(),
                root.getAsJsonObject().get("result"), fixedStack.resultContractSha256());
    }

    private static void validateOpalContract(ServerLevel level) {
        JsonElement root = readJson(level.getServer().getResourceManager(), OPAL_LOOT_RESOURCE);
        JsonElement entry;
        try {
            entry = root.getAsJsonObject().getAsJsonArray("pools").get(0)
                    .getAsJsonObject().getAsJsonArray("entries").get(0);
        } catch (RuntimeException exception) {
            throw new IllegalStateException("Malformed required Matcha Opal loot contract: "
                    + OPAL_LOOT_RESOURCE, exception);
        }
        validateFingerprint("Matcha Opal loot entry", entry, OPAL_ENTRY_CONTRACT_SHA256);
    }

    private static JsonElement readJson(ResourceManager resourceManager, Identifier resourceId) {
        try (BufferedReader reader = resourceManager.openAsReader(resourceId)) {
            return JsonParser.parseReader(reader);
        } catch (IOException | JsonParseException exception) {
            throw new IllegalStateException("Unable to read required Matcha resource " + resourceId,
                    exception);
        }
    }

    static void validateFingerprint(String description, JsonElement contract,
                                    String expectedSha256) {
        String actualSha256 = canonicalContractSha256(contract);
        if (!expectedSha256.equals(actualSha256)) {
            throw new IllegalStateException(description + " contract drift: expected SHA-256 "
                    + expectedSha256 + ", got " + actualSha256);
        }
    }
}
