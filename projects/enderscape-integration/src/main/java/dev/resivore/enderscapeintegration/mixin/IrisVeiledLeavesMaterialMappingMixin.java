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
 * Runs before the lower-priority generic BGE bridge, so BGE's existing canonical-parent path
 * observes the completed canonical Veiled Leaves classification rather than an Enderscape rule.
 */
@Pseudo
@Mixin(targets = "net.irisshaders.iris.shaderpack.materialmap.BlockMaterialMapping", remap = false, priority = 1100)
public abstract class IrisVeiledLeavesMaterialMappingMixin {
    @Inject(method = "createBlockStateIdMap(Lit/unimi/dsi/fastutil/ints/Int2ObjectLinkedOpenHashMap;Lit/unimi/dsi/fastutil/ints/Int2ObjectLinkedOpenHashMap;)Lit/unimi/dsi/fastutil/objects/Object2IntMap;",
            at = @At("RETURN"), remap = false)
    private static void enderscapeIntegration$inheritVeiledLeavesMaterialId(
            CallbackInfoReturnable<Object2IntMap<BlockState>> cir) {
        VeiledLeavesShaderMaterialBridge.inheritMaterialIds(cir.getReturnValue());
    }
}
