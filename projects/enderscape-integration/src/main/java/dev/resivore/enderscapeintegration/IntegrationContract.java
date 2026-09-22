package dev.resivore.enderscapeintegration;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.core.Holder;

import java.util.LinkedHashSet;
import java.util.Set;

/** Exact C2 ownership boundary. Registry IDs remain present; only future paths are filtered. */
public final class IntegrationContract {
    public static final String ENDERSCAPE_SHA256 =
            "9fcc4f59ca88e91f90e7c7d18289f2f859f20c810eebcca924764aa15236c40b";

    public static final Set<String> SUPPRESSED_ITEM_IDS = Set.of(
            "enderscape:shadoline_helmet",
            "enderscape:shadoline_chestplate",
            "enderscape:shadoline_leggings",
            "enderscape:shadoline_boots",
            "enderscape:dagger",
            "enderscape:rubble_shield",
            "enderscape:mirror",
            "enderscape:rubble_chitin");
    public static final Set<String> SUPPRESSED_ENCHANTMENT_IDS = Set.of(
            "enderscape:bundling",
            "enderscape:stun_burst",
            "enderscape:transdimensional");
    public static final String RESONANCE_ID = "enderscape:resonance";
    public static final String MAGNIA_ATTRACTOR_ID = "enderscape:magnia_attractor";
    public static final Set<String> NATIVE_FOOD_IDS = Set.of(
            "enderscape:drift_jelly_bottle",
            "enderscape:puruberry",
            "enderscape:murublight_bracket");

    public static final Set<String> BLOCKED_RECIPE_IDS = Set.of(
            "enderscape:dagger",
            "enderscape:mirror_dying",
            "enderscape:rubble_shield_end_stone",
            "enderscape:rubble_shield_mirestone",
            "enderscape:rubble_shield_veradite",
            "enderscape:rubble_shield_kurodite",
            "enderscape:shadoline_helmet",
            "enderscape:shadoline_chestplate",
            "enderscape:shadoline_leggings",
            "enderscape:shadoline_boots",
            "enderscape:shadoline_nugget_from_smelting",
            "enderscape:shadoline_nugget_from_blasting");
    public static final Set<String> BLOCKED_ADVANCEMENT_IDS = Set.of(
            "enderscape:stun_attack",
            "enderscape:rubble_shield_dash",
            "enderscape:mirror_teleport",
            "enderscape:long_distance",
            "enderscape:transdimensional",
            "enderscape:recipes/combat/dagger",
            "enderscape:recipes/combat/rubble_shield_end_stone",
            "enderscape:recipes/combat/rubble_shield_mirestone",
            "enderscape:recipes/combat/rubble_shield_veradite",
            "enderscape:recipes/combat/rubble_shield_kurodite",
            "enderscape:recipes/combat/shadoline_helmet",
            "enderscape:recipes/combat/shadoline_chestplate",
            "enderscape:recipes/combat/shadoline_leggings",
            "enderscape:recipes/combat/shadoline_boots",
            "enderscape:recipes/misc/shadoline_nugget_from_smelting",
            "enderscape:recipes/misc/shadoline_nugget_from_blasting");

    private IntegrationContract() {
    }

    public static boolean isSuppressedItem(ItemStack stack) {
        return stack != null && !stack.isEmpty()
                && SUPPRESSED_ITEM_IDS.contains(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
    }

    public static boolean isNativeFood(ItemStack stack) {
        return stack != null && !stack.isEmpty()
                && NATIVE_FOOD_IDS.contains(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
    }

    public static boolean isMagniaAttractor(ItemStack stack) {
        return stack != null && !stack.isEmpty()
                && MAGNIA_ATTRACTOR_ID.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
    }

    public static boolean isSuppressedEnchantment(Holder<Enchantment> enchantment) {
        return enchantment.unwrapKey().map(key -> SUPPRESSED_ENCHANTMENT_IDS.contains(key.identifier().toString()))
                .orElse(false);
    }

    public static boolean isResonance(Holder<Enchantment> enchantment) {
        return enchantment.unwrapKey().map(key -> RESONANCE_ID.equals(key.identifier().toString())).orElse(false);
    }

    /** Filters exact enchanted-book variants without hiding unrelated enchanted books. */
    public static boolean hasSuppressedStoredEnchantment(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        return enchantmentIds(stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY))
                .stream().anyMatch(SUPPRESSED_ENCHANTMENT_IDS::contains)
                || enchantmentIds(stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY))
                .stream().anyMatch(SUPPRESSED_ENCHANTMENT_IDS::contains);
    }

    private static Set<String> enchantmentIds(ItemEnchantments enchantments) {
        Set<String> ids = new LinkedHashSet<>();
        enchantments.entrySet().forEach(entry -> entry.getKey().unwrapKey()
                .ifPresent(key -> ids.add(key.identifier().toString())));
        return ids;
    }
}
