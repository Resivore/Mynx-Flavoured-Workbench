package dev.resivore.woolsoundproofchests.mixin;

import dev.resivore.woolsoundproofchests.ChestSoundproofing;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.gameevent.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ContainerOpenersCounter.class)
abstract class ContainerOpenersCounterMixin {
    @Redirect(
        method = {"incrementOpeners", "decrementOpeners", "recheckOpeners"},
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;gameEvent(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/Holder;Lnet/minecraft/core/BlockPos;)V"
        ),
        require = 3
    )
    private void woolSoundproofChests$suppressQualifiedChestEvent(
        Level level,
        Entity source,
        Holder<GameEvent> event,
        BlockPos eventPos
    ) {
        if (!ChestSoundproofing.shouldSuppressContainerEvent(level, eventPos)) {
            level.gameEvent(source, event, eventPos);
        }
    }
}
