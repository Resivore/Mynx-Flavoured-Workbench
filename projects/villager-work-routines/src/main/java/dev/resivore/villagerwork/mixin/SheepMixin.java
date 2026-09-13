package dev.resivore.villagerwork.mixin;

import dev.resivore.villagerwork.ShearCapture;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.function.BiConsumer;

@Mixin(Sheep.class)
abstract class SheepMixin {
    @ModifyArg(method = "shear", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/sheep/Sheep;dropFromShearingLootTable(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/world/item/ItemInstance;Ljava/util/function/BiConsumer;)V"), index = 3, require = 1)
    private BiConsumer<ServerLevel, ItemStack> villagerWork$captureOnlyThisShear(BiConsumer<ServerLevel, ItemStack> vanilla) {
        return ShearCapture.wrap((Sheep)(Object)this, vanilla);
    }
}
