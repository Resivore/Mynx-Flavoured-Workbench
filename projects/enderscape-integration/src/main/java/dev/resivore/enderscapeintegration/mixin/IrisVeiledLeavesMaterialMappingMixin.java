package dev.resivore.enderscapeintegration.mixin;

import dev.resivore.enderscapeintegration.client.VeiledLeavesShaderMaterialBridge;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Seeds the canonical classification in an earlier injector-order pass than the generic BGE
 * bridge, so BGE observes Veiled Leaves through its existing canonical-parent path.
 */
@Pseudo
@Mixin(targets = "net.irisshaders.iris.shaderpack.materialmap.BlockMaterialMapping", remap = false)
public abstract class IrisVeiledLeavesMaterialMappingMixin {
    @Inject(method = "createBlockStateIdMap(Lit/unimi/dsi/fastutil/ints/Int2ObjectLinkedOpenHashMap;Lit/unimi/dsi/fastutil/ints/Int2ObjectLinkedOpenHashMap;)Lit/unimi/dsi/fastutil/objects/Object2IntMap;",
            at = @At("RETURN"), remap = false, order = 900)
    private static void enderscapeIntegration$inheritVeiledLeavesMaterialId(
            CallbackInfoReturnable<Object2IntMap<BlockState>> cir) {
        VeiledLeavesShaderMaterialBridge.inheritMaterialIds(cir.getReturnValue());
    }
}
