package dev.resivore.dragonbound.channel;

import dev.resivore.dragonbound.anchor.AnchorBinding;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

record PendingChannel(
        UUID playerId,
        long token,
        ServerPlayer playerInstance,
        ResourceKey<Level> startDimension,
        Vec3 startPosition,
        long startTick,
        long completionTick,
        int selectedSlot,
        ItemStack heldSnapshot,
        ReturnSource source,
        AnchorBinding anchor,
        int staffCooldownTicks
) {
}
