package dev.resivore.slabdecorations;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Narrow optional contracts with no foreign classes in this mod's linkage graph. */
final class OptionalSurfaceAdapters {
    private OptionalSurfaceAdapters() {}

    static boolean isRibbitsSwampLantern(BlockState state) {
        Identifier id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return id != null && id.getNamespace().equals("ribbits")
                && state.hasProperty(BlockStateProperties.HANGING)
                && state.hasProperty(BlockStateProperties.WATERLOGGED);
    }
}
