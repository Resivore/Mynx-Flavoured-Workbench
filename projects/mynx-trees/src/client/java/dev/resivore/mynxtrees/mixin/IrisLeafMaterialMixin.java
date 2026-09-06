package dev.resivore.mynxtrees.mixin;

import dev.resivore.mynxtrees.LeafShaderAliases;
import dev.resivore.mynxtrees.MynxTrees;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.irisshaders.iris.shaderpack.materialmap.BlockMaterialMapping", remap = false)
public abstract class IrisLeafMaterialMixin {
    @Inject(method = "createBlockStateIdMap(Lit/unimi/dsi/fastutil/ints/Int2ObjectLinkedOpenHashMap;Lit/unimi/dsi/fastutil/ints/Int2ObjectLinkedOpenHashMap;)Lit/unimi/dsi/fastutil/objects/Object2IntMap;",
            at = @At("RETURN"), remap = false)
    private static void mynxTrees$inheritLeafMaterials(CallbackInfoReturnable<Object2IntMap<BlockState>> cir) {
        Object2IntMap<BlockState> ids = cir.getReturnValue();
        LeafShaderAliases.inheritUnmappedFromFirstPresent(ids,
                MynxTrees.SILVER_LEAVES.getStateDefinition().getPossibleStates(),
                java.util.List.of(
                        upperHalf(Blocks.SUNFLOWER),
                        upperHalf(Blocks.LILAC),
                        upperHalf(Blocks.ROSE_BUSH),
                        upperHalf(Blocks.PEONY)));
        LeafShaderAliases.inheritUnmapped(ids, MynxTrees.WISTERIA_LEAVES.getStateDefinition().getPossibleStates(),
                state -> Blocks.CHERRY_LEAVES.withPropertiesOf(state));
    }

    private static BlockState upperHalf(net.minecraft.world.level.block.Block block) {
        return block.defaultBlockState().setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER);
    }
}
