package com.crispytwig.naturalist.server.entity.base;

import com.crispytwig.naturalist.registry.NaturalistRegistry;
import com.crispytwig.naturalist.registry.NaturalistSoundEvents;
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
    static <T extends TamableAnimal & FollowingPet> InteractionResult tryWhistle(T mob, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.is(NaturalistRegistry.WHISTLE.get()) || !mob.isTame() || !mob.isOwnedBy(player)
                || player.isSecondaryUseActive() || player.getCooldowns().isOnCooldown(stack)) {
            return null;
        }
        if (!mob.level().isClientSide()) {
            boolean follow = !mob.isFollowingOwner();
            mob.setFollowingOwner(follow);
            mob.setOrderedToSit(false);
            if (!follow) {
                mob.setTarget(null);
            }
            mob.playSound(NaturalistSoundEvents.WHISTLE.get(), 0.8F, 1.0F);
            Component name = mob.getDisplayName().copy().withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW).withItalic(false));
            Component state = Component.translatable(follow ? "naturalist.whistle.following" : "naturalist.whistle.wandering")
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW).withBold(true).withItalic(false));
            player.sendOverlayMessage(Component.translatable("naturalist.whistle.message", name, state)
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY).withItalic(true)));
        }
        player.getCooldowns().addCooldown(stack, 20);
        return mob.level().isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
    }
}
