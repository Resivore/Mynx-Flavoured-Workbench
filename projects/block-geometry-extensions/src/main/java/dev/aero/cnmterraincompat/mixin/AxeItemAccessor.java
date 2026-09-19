package dev.aero.cnmterraincompat.mixin;

import net.minecraft.world.item.AxeItem;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

/** Reads the authoritative axe transition table after optional providers finish registration. */
@Mixin(AxeItem.class)
public interface AxeItemAccessor {
    @Accessor("STRIPPABLES")
    static Map<Block, Block> bge$getStrippables() {
        throw new AssertionError("Mixin accessor not transformed");
    }
}
