package com.crispytwig.naturalist.server.entity.base;

import net.minecraft.ChatFormatting;
import com.mojang.serialization.Codec;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

public interface FollowingPet {
    boolean isFollowingOwner();

    void setFollowingOwner(boolean following);

    static void savePet(FollowingPet pet, ValueOutput output) {
        output.putBoolean("FollowingOwner", pet.isFollowingOwner());
    }

    static void loadPet(FollowingPet pet, ValueInput input) {
        input.read("FollowingOwner", Codec.BOOL).ifPresent(pet::setFollowingOwner);
    }

    @Nullable
    static <T extends TamableAnimal & FollowingPet> InteractionResult tryCyclePetMode(T mob, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.isEmpty() || !mob.isTame() || !mob.isOwnedBy(player) || !player.isSecondaryUseActive()) {
            return null;
        }
        if (!mob.level().isClientSide()) {
            PetMode next = currentMode(mob).next();
            applyMode(mob, next);
            Component name = mob.getDisplayName().copy().withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW).withItalic(false));
            Component state = Component.translatable("naturalist.pet_mode." + next.translationKey)
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW).withBold(true).withItalic(false));
            player.sendOverlayMessage(Component.translatable("naturalist.pet_mode.message", name, state)
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY).withItalic(true)));
        }
        return mob.level().isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
    }

    static <T extends TamableAnimal & FollowingPet> PetMode currentMode(T mob) {
        return mob.isOrderedToSit() ? PetMode.STAY : mob.isFollowingOwner() ? PetMode.FOLLOW : PetMode.WANDER;
    }

    static <T extends TamableAnimal & FollowingPet> void applyMode(T mob, PetMode mode) {
        switch (mode) {
            case FOLLOW -> {
                mob.setOrderedToSit(false);
                mob.setFollowingOwner(true);
            }
            case WANDER -> {
                mob.setOrderedToSit(false);
                mob.setFollowingOwner(false);
                mob.setTarget(null);
                mob.getNavigation().stop();
            }
            case STAY -> {
                mob.setOrderedToSit(true);
                mob.getNavigation().stop();
            }
        }
    }

    enum PetMode {
        FOLLOW("following"), WANDER("wandering"), STAY("staying");

        private final String translationKey;

        PetMode(String translationKey) {
            this.translationKey = translationKey;
        }

        PetMode next() {
            return switch (this) {
                case FOLLOW -> WANDER;
                case WANDER -> STAY;
                case STAY -> FOLLOW;
            };
        }
    }
}
