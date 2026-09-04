package dev.resivore.carryonpatch.common;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.chermew.grabandgo.duck.GrabCarrier;
import org.slf4j.LoggerFactory;

public final class CarryPersistence {
    private CarryPersistence() {}

    public static void load(Player player, ValueInput input) {
        CarryState state = ((CarryStateAccess) player).carryOnPatch$state();
        CompoundTag data = state.load(input);
        // Use upstream setters: these mark its existing STRING/BOOLEAN tracked fields dirty,
        // preserving initial tracking, reconnect and ordinary metadata synchronization.
        ((PlayerCarryStateMixinBridge) player).carryOnPatch$restore(data,
                state.protectedState() || !data.isEmpty());
        if (state.needsWarning()) warn(player, "Saved carry data needs recovery; its original payload is preserved. Placement is blocked.");
    }

    public static void save(Player player, ValueOutput output) {
        GrabCarrier carrier = (GrabCarrier) player;
        ((CarryStateAccess) player).carryOnPatch$state().save(output,
                carrier.grabandgo$getCarriedData(), carrier.grabandgo$isCarrying());
    }

    public static void warn(Player player, String reason) {
        if (((CarryStateAccess) player).carryOnPatch$state().markWarned(reason)) {
            LoggerFactory.getLogger("CarryOnPatch").warn("Player {}: {}", player.getUUID(), reason);
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer && serverPlayer.connection != null)
                player.sendSystemMessage(Component.literal("Carry On Patch: " + reason));
        }
    }
}
