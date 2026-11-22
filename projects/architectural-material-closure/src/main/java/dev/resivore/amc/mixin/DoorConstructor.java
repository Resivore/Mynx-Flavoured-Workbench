package dev.resivore.amc.mixin;

import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(DoorBlock.class)
public interface DoorConstructor {
    @Invoker("<init>")
    static DoorBlock create(BlockSetType type, BlockBehaviour.Properties properties) {
        throw new AssertionError();
    }
}
