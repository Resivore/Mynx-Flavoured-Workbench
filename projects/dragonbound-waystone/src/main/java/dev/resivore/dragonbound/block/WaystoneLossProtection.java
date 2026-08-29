package dev.resivore.dragonbound.block;

import dev.resivore.dragonbound.DragonboundWaystone;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

/**
 * Direct-inventory survival return with a persistent, damage-proof entity fallback.
 */
public final class WaystoneLossProtection {
    private WaystoneLossProtection() {
    }

    public static boolean returnToPlayerOrSpawnProtected(
            ServerLevel level,
            Player player,
            BlockPos pos,
            ItemStack returnedStack) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(pos, "pos");
        Objects.requireNonNull(returnedStack, "returnedStack");

        if (returnedStack.isEmpty()) {
            return true;
        }

        ItemStack remainder = returnedStack.copyWithCount(1);
        if (player.addItem(remainder)) {
            return true;
        }

        ItemEntity fallback = new ItemEntity(
                level,
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D,
                remainder);
        fallback.setUnlimitedLifetime();
        fallback.setInvulnerable(true);
        fallback.setNoPickUpDelay();
        fallback.setThrower(player);

        if (level.addFreshEntity(fallback)) {
            return true;
        }

        DragonboundWaystone.LOGGER.error(
                "Could not spawn protected Dragonbound Waystone fallback at {} in {}",
                pos,
                level.dimension().identifier());
        return false;
    }
}
