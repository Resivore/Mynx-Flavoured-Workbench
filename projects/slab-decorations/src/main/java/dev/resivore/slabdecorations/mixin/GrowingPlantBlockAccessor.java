package dev.resivore.slabdecorations.mixin;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.GrowingPlantBlock;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Exposes Minecraft's generic growing-column contract without naming individual plant IDs. */
@Mixin(GrowingPlantBlock.class)
public interface GrowingPlantBlockAccessor {
    @Accessor("growthDirection")
    Direction slabDecorations$getGrowthDirection();

    @Invoker("getHeadBlock")
    GrowingPlantHeadBlock slabDecorations$invokeGetHeadBlock();

    @Invoker("getBodyBlock")
    Block slabDecorations$invokeGetBodyBlock();
}
