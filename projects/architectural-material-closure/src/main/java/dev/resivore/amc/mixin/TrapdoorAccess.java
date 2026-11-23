package dev.resivore.amc.mixin;

import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TrapDoorBlock.class)
public interface TrapdoorAccess {
    @Accessor("type")
    BlockSetType amc$type();

    @Invoker("<init>")
    static TrapDoorBlock create(BlockSetType type, BlockBehaviour.Properties properties) {
        throw new AssertionError();
    }
}
