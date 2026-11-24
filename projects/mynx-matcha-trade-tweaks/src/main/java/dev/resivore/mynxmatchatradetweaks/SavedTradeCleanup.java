package dev.resivore.mynxmatchatradetweaks;

import dev.resivore.mynxmatchatradetweaks.mixin.SavedOffersAccessor;
import java.util.Set;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

final class SavedTradeCleanup {
    // Model identifiers written by the 42 published food_N functions.
    private static final Set<String> FOOD_MODELS = Set.of(
            "apple_empanada", "bokguk", "brachio_chicken_nugget", "brownie", "bruschetta",
            "bubbling_mead", "bunuelo", "carrot_cupcake", "cheese_pizza", "chocolate_chip_cookie",
            "crimson_stroganoff", "fish_and_chips", "french_toast", "gimmari", "glow_berry_crumble",
            "gnocchi", "green_curry", "honey_ginger_tea", "honied_french_toast", "ice_cream",
            "japanese_curry", "kontomire_stew", "latke", "lumpia", "mead", "meat_pizza",
            "melon_sorbet", "mushroom_pizza", "oatmeal", "pad_thai", "paneer_makhani",
            "ptero_chicken_nugget", "puerquito", "pumpkin_curry", "pumpkin_empanada", "ramen",
            "red_mushroom_stroganoff", "stego_chicken_nugget", "stroganoff", "sweet_berry_danish",
            "tricero_chicken_nugget", "warped_pizza");

    private SavedTradeCleanup() {
    }

    static void onLoad(Villager villager) {
        // Calling getOffers() would roll trades on villagers that have never traded.
        MerchantOffers saved = ((SavedOffersAccessor) villager).mynxMatchaTradeTweaks$savedOffers();
        boolean checkedFood = villager.removeTag("foodChecked");
        if (saved != null) {
            saved.removeIf(offer -> (checkedFood && isFavoriteFood(offer)) || isRetiredHeartExchange(offer));
        }
    }

    static boolean isFavoriteFood(MerchantOffer offer) {
        ItemStack buy = offer.getItemCostA().itemStack();
        Identifier model = buy.get(DataComponents.ITEM_MODEL);
        return model != null && model.getNamespace().equals("matcha") && FOOD_MODELS.contains(model.getPath())
                && buy.is(model.getPath().equals("bubbling_mead") ? Items.SPLASH_POTION : Items.POISONOUS_POTATO)
                && offer.getItemCostA().count() == 1 && offer.getItemCostB().isEmpty()
                && offer.getResult().is(Items.EMERALD) && offer.getResult().getCount() == 1
                && offer.getResult().getComponentsPatch().isEmpty();
    }

    static boolean isRetiredHeartExchange(MerchantOffer offer) {
        ItemStack heart = offer.getItemCostA().itemStack();
        return heart.is(Items.POISONOUS_POTATO) && offer.getItemCostA().count() == 2
                && Identifier.parse("matcha:heart_container").equals(heart.get(DataComponents.ITEM_MODEL))
                && offer.getItemCostB().filter(cost -> cost.count() == 1 && cost.item().value() == Items.ECHO_SHARD).isPresent()
                && offer.getResult().is(Items.TURTLE_SCUTE) && offer.getResult().getCount() == 1
                && Boolean.TRUE.equals(offer.getResult().get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE));
    }
}
