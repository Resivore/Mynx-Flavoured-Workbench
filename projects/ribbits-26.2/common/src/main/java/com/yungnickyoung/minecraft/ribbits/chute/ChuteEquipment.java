package com.yungnickyoung.minecraft.ribbits.chute;

import com.yungnickyoung.minecraft.ribbits.RibbitsCommon;
import com.yungnickyoung.minecraft.ribbits.module.ItemModule;
import dev.yumi.commons.TriState;
import eu.pb4.trinkets.api.TrinketAttachment;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.api.TrinketsApi;
import eu.pb4.trinkets.api.event.TrinketCanEquipCallback;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;

/** Exact chest/cape ownership, validity, and mutual-exclusion rules for the Chute. */
public final class ChuteEquipment {
    public static final String SLOT_ID = "chest/cape";
    public static final int SLOT_INDEX = 0;

    private static boolean initialized;

    private ChuteEquipment() {
    }

    public static synchronized void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        TrinketCanEquipCallback.EVENT.register(
                RibbitsCommon.id("chute_leaf_conflicts"),
                ChuteEquipment::canEquip
        );
    }

    private static TriState canEquip(
            ItemStack incoming,
            TrinketSlotAccess slot,
            LivingEntity entity,
            boolean defaultResult
    ) {
        boolean chute = isChute(incoming);
        boolean exactSlot = isExactSlot(slot);

        // No later data pack or permissive callback may make the Chute valid outside the
        // accepted one-entry chest/cape route.
        if (chute && !exactSlot) {
            return TriState.FALSE;
        }
        if (!(entity instanceof Player player) || !exactSlot) {
            return TriState.DEFAULT;
        }
        if (chute && hasAnyEquippedGlider(player)) {
            return TriState.FALSE;
        }
        if (isTrinketsGlider(incoming) && hasActiveChute(player)) {
            return TriState.FALSE;
        }
        return TriState.DEFAULT;
    }

    public static boolean isChute(ItemStack stack) {
        return stack.is(ItemModule.CHUTE_LEAF.get());
    }

    public static boolean isExactSlot(TrinketSlotAccess slot) {
        return slot != null
                && SLOT_ID.equals(slot.slotType().getId())
                && slot.index() == SLOT_INDEX;
    }

    public static boolean hasActiveChute(Player player) {
        TrinketSlotAccess slot = TrinketsApi.getAttachment(player).getSlotAccess(SLOT_ID, SLOT_INDEX);
        if (slot == null || !slot.isValid()) {
            return false;
        }

        ItemStack stack = slot.get();
        return isChute(stack)
                && slot.slotType().validatorCheck(stack, slot, player)
                && slot.canApplyEffects();
    }

    /** Rejecting insertion is intentionally broader than movement: even a broken glider conflicts. */
    public static boolean hasAnyEquippedGlider(Player player) {
        for (EquipmentSlot slot : EquipmentSlot.VALUES) {
            ItemStack stack = player.getItemBySlot(slot);
            Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
            if (stack.has(DataComponents.GLIDER) && equippable != null && equippable.slot() == slot) {
                return true;
            }
        }

        TrinketAttachment attachment = TrinketsApi.getAttachment(player);
        return attachment.isEquipped(ChuteEquipment::isTrinketsGlider, true);
    }

    /** Mirrors vanilla and accepted Trinkets Elytra eligibility for the inert-state guard. */
    public static boolean hasUsableGliderConflict(Player player) {
        for (EquipmentSlot slot : EquipmentSlot.VALUES) {
            if (LivingEntity.canGlideUsing(player.getItemBySlot(slot), slot)) {
                return true;
            }
        }

        return TrinketsApi.getAttachment(player).isEquipped(
                stack -> isTrinketsGlider(stack) && !stack.nextDamageWillBreak(),
                true
        );
    }

    public static boolean isVanillaChestGlider(ItemStack stack) {
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        return stack.has(DataComponents.GLIDER)
                && equippable != null
                && equippable.slot() == EquipmentSlot.CHEST;
    }

    public static boolean canRemainOpen(Player player) {
        return player.isAlive()
                && !player.isRemoved()
                && !player.isSpectator()
                && !player.getAbilities().flying
                && hasActiveChute(player)
                && !player.onGround()
                && !player.isPassenger()
                && !player.isSwimming()
                && !player.isVisuallyCrawling()
                && !player.isInWater()
                && !player.isInLava()
                && !player.onClimbable()
                && !player.isSleeping()
                && !player.hasEffect(MobEffects.LEVITATION)
                && !player.isFallFlying()
                && !hasUsableGliderConflict(player);
    }

    private static boolean isTrinketsGlider(ItemStack stack) {
        return !stack.isEmpty() && stack.has(DataComponents.GLIDER);
    }
}
