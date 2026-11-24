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
            Identifier.parse("matcha:loot_table/treasure/opal.json");
    static final String OPAL_ENTRY_CONTRACT_SHA256 =
            "65878efb532070bf49eb65bcf779d108bca3b9a4396e40616ca7540957f01ce6";

    private MatchaStackCatalog() {
    }

    /**
     * SHA-256 values cover canonicalized recipe {@code result} JSON, not protected resource bytes.
     * Recipe contracts use MF_1_12_5_26_2.zip. The offer-only Estus Flask retains the
     * Matcha Flavoured 1.12 potion by Klei (CC BY-NC-SA 4.0).
     * Crystal Heart is also verified against the coordinated matcha-heart-death-compat recipe.
     */
    public enum FixedStack {
        GLOW_BERRY_CRUMBLE("matcha:food/crafting/glow_berry_crumble", "minecraft:poisonous_potato",
                "f26a16561d4f6ab37cd524546a6a4c03a65590d10ef2b6060d74bad0f69c26d0"),
        HONEY_GINGER_TEA("matcha:food/oven/honey_ginger_tea", "minecraft:poisonous_potato",
                "371e965c948c1e8a8df46a019b911069814342d432a2ae1628395de0e25f5944"),
        PICKLED_CARROTS("matcha:food/crafting/pickled_carrots", "minecraft:poisonous_potato",
                "2cad963ecad6876140ad3b545d222f34611f159b296069f3f608177d3cc797af"),
        RIND_JAM("matcha:food/crafting/rind_jam", "minecraft:poisonous_potato",
                "f054af52683bacfc9abd86b4fdc4cdb67dce08b8e514cf7fd09dc5da8543a4d0"),
        GIMMARI("matcha:food/crafting/gimmari", "minecraft:poisonous_potato",
                "6a6d10d5ecf1ca2ccc3a2914b20f6c816a859c371db3e72081bc710921fa95a1"),
        BOKGUK("matcha:food/crafting/bokguk", "minecraft:poisonous_potato",
                "1d424efa6b25858990d5259b2da6a03ff17ea3f20a762b3eba4394d8c9129507"),
        GOLDEN_PICKLED_CARROTS("matcha:food/crafting/golden_pickled_carrots", "minecraft:poisonous_potato",
                "488ac0a4abafd74f364acc6d79aac51696912ceb50fffbce3b5911b6f60926ba"),
        MELON_SORBET("matcha:food/crafting/melon_sorbet", "minecraft:poisonous_potato",
                "ac896ede1636afedb077c28a33d72a6bf42a0fd8e0576761497049e73556921a"),
        PUMPKIN_EMPANADA("matcha:food/crafting/pumpkin_empanada", "minecraft:poisonous_potato",
                "4aa72313400ae49ffb26c257b210df4127df2018ce111d15732fc971259368c2"),
        WARPED_PIZZA("matcha:food/crafting/warped_pizza", "minecraft:poisonous_potato",
                "e161f06665198d6241bec8d3be5a57627b4f87fcb125961529ce27aac9c6a589"),
        WARPED_STROGANOFF("matcha:food/crafting/warped_stroganoff", "minecraft:poisonous_potato",
                "075e537632f1bf7c0cc18b9ca5b2d8531ef9ea14dbeda093c2411daf849058c8"),
        SWEET_BERRY_DANISH("matcha:food/crafting/sweet_berry_danish", "minecraft:poisonous_potato",
                "8440acfc8fadf6e42a72cd05d48e2ced0bb7b7e00e1b0eb6920d3d1678fbf0ef"),
        GOLDEN_CARROT_CUPCAKE("matcha:food/crafting/golden_carrot_cupcake", "minecraft:poisonous_potato",
                "f2e87843ef3303c285057b6fe9b7695c92d67cacc7ae553b28175e268121039f"),
        JAPANESE_CURRY("matcha:food/oven/japanese_curry", "minecraft:poisonous_potato",
                "25f04d89698e3fb2d6aeae6872660c801631f38fa12852b291d7fa50d9567925"),
        GREEN_CURRY("matcha:food/oven/green_curry", "minecraft:poisonous_potato",
                "7b02e440ea5bd0aa9bb7f6d03adbf8389583c7731f39f2f09bf98ef2813d90ea"),
        TONKOTSU_RAMEN("matcha:food/oven/ramen", "minecraft:poisonous_potato",
                "40eee757ea734d1f720d38b222cbf6d7dae9d720be7c3655c7af319faf8c060c"),
        BENZENE("matcha:crafting/benzene", "minecraft:endermite_spawn_egg",
                "bb4fb30940e4151f5be4ae7f6fb23df0a30192e4a9dd258bd50a9933a343cf6e"),
        ESTUS_FLASK("ribbits:estus_flask", "minecraft:potion",
                "ec7b84853cad4b17a8edb5992e33604465c12f36b2cfcb1e485307b8aa4cc56a"),
        REACH_BLESSING("matcha:blessing/reach", "minecraft:enchanted_book",
                "c68cd1485734e645344bab124a553ca6afdbb950e6d9f3466038a63c13f3bc13"),
        SILK_TOUCH_BLESSING("matcha:blessing/silk_touch", "minecraft:enchanted_book",
                "872bb5b94ca61cbcf4d589957c19de839b9bc9907831355b4a27ea3eefa677eb"),
        CARBON_RICH_IRON("matcha:crafting/carbon_rich_iron", "minecraft:piglin_brute_spawn_egg",
                "2bb6ace178337ddf65f0af7e1de9354ac2751da3daef98da60a62903469531fa"),
        HEPATIZON_ALLOY("matcha:crafting/bronze_alloy", "minecraft:phantom_membrane",
                "eb76460c156a6bd20b103b8ed747e0c6e540ed2a1163fa5f768a5e78de02adf8"),
        SHAKUDO_ALLOY("matcha:crafting/shakudo_alloy", "minecraft:shulker_shell",
                "89b858dfdffadc865c29f9ca055869fbd70a913992c12f03a346e38a0493dc41"),
        ELECTRUM_ALLOY("matcha:crafting/electrum_alloy", "minecraft:heart_of_the_sea",
                "b9d815288f637d303bb5e28ea8ad5cf2d97089debfda218eceb8cb0ef0d41a3f"),
        DIVINE_FRAGMENT("matcha:crafting/divine_fragment", "minecraft:turtle_scute",
                "8fc469fcc5cf34b389bb3b463fe6ae20ee337267c6754e3b6619539a72e96a24"),
        CRYSTAL_HEART("matcha:crafting/crystal_heart", "minecraft:poisonous_potato",
                "7c706ab20f974dc6811eb5cb8efe10ad96354c522e9f95526038c360385423fa");

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

        if (fixedStack == FixedStack.ESTUS_FLASK) {
            // 1.12.5 removed this recipe. Keep the accepted offer-only potion and its exact components.
            var result = readJson(serverLevel.getServer().getResourceManager(),
                    Identifier.parse("ribbits:trade_item/estus_flask.json"));
            validateFingerprint("Ribbit Estus Flask offer", result, fixedStack.resultContractSha256());
            return ItemStack.CODEC.parse(serverLevel.registryAccess().createSerializationContext(
                    com.mojang.serialization.JsonOps.INSTANCE), result).getOrThrow();
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

    /** Exact current Opal loot-stack contract from matcha:treasure/opal. */
    public static ItemStack opal() {
        ItemStack stack = new ItemStack(Items.POISONOUS_POTATO);
        stack.set(DataComponents.ITEM_MODEL, Identifier.parse("matcha:opal"));
        stack.set(DataComponents.ITEM_NAME,
                Component.translatable("item.kleispack.opal").withColor(0xAB85AD));
        stack.remove(DataComponents.CONSUMABLE);
        var version = new net.minecraft.nbt.CompoundTag();
        version.putInt("version", 1);
        stack.set(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(version));
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
        if (!Identifier.parse("minecraft:poisonous_potato")
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

    static JsonElement readJson(ResourceManager resourceManager, Identifier resourceId) {
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
