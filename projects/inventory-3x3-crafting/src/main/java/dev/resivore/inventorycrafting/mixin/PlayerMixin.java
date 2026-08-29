package dev.resivore.inventorycrafting.mixin;

import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(Player.class)
abstract class PlayerMixin {
    @ModifyConstant(
            method = "getSlot",
            constant = @Constant(intValue = 4),
            require = 1,
            expect = 1
    )
    private int inventory3x3$exposeAllLogicalCraftingCells(int vanillaCount) {
        return 9;
    }
}
