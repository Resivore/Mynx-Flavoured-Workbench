package dev.resivore.matchafrost;

import java.util.List;
import java.util.Optional;
import java.util.function.ToIntFunction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

public final class FrostProtectionTraversal {
    static final ResourceKey<Enchantment> FROST_PROTECTION = ResourceKey.create(
            Registries.ENCHANTMENT, Identifier.parse("main:freezing_protection"));
    static final List<EquipmentSlot> ARMOR_SLOTS = List.of(
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET);

    private FrostProtectionTraversal() {}

    public static boolean allowsPowderSnowTraversal(Entity entity) {
        if (!(entity instanceof Player player)) {
            return false;
        }

        Optional<Holder.Reference<Enchantment>> frostProtection = player.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .get(FROST_PROTECTION);
        if (frostProtection.isEmpty()) {
            return false;
        }

        Holder<Enchantment> enchantment = frostProtection.get();
        return hasQualifyingArmor(slot -> levelOn(player, slot, enchantment));
    }

    static boolean hasQualifyingArmor(ToIntFunction<EquipmentSlot> levelBySlot) {
        return ARMOR_SLOTS.stream().anyMatch(slot -> levelBySlot.applyAsInt(slot) > 0);
    }

    private static int levelOn(
            Player player,
            EquipmentSlot slot,
            Holder<Enchantment> enchantment) {
        return EnchantmentHelper.getItemEnchantmentLevel(enchantment, player.getItemBySlot(slot));
    }
}
