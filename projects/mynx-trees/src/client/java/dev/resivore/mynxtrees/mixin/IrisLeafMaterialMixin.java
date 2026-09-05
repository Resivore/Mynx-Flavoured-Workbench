package dev.resivore.mynxtrees.mixin;

import dev.resivore.mynxtrees.LeafShaderAliases;
import dev.resivore.mynxtrees.MynxTrees;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
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
        LeafShaderAliases.inheritUnmapped(ids, MynxTrees.SILVER_LEAVES.getStateDefinition().getPossibleStates(),
                state -> Blocks.BIRCH_LEAVES.withPropertiesOf(state));
        LeafShaderAliases.inheritUnmapped(ids, MynxTrees.WISTERIA_LEAVES.getStateDefinition().getPossibleStates(),
                state -> Blocks.CHERRY_LEAVES.withPropertiesOf(state));
    }
}
