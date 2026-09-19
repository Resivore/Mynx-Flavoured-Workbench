package dev.resivore.bgecomplementary.mixin;

import dev.resivore.bgecomplementary.BgeLateRuntimeBridge;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.Map;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Hooks the exact Iris 1.11.2 completed maps, after pack parsing and precedence resolution. */
@Pseudo
@Mixin(targets = "net.irisshaders.iris.shaderpack.materialmap.BlockMaterialMapping", remap = false)
public abstract class IrisBgeMaterialMappingMixin {
    @Inject(method = "createBlockStateIdMap(Lit/unimi/dsi/fastutil/ints/Int2ObjectLinkedOpenHashMap;Lit/unimi/dsi/fastutil/ints/Int2ObjectLinkedOpenHashMap;)Lit/unimi/dsi/fastutil/objects/Object2IntMap;",
            at = @At("RETURN"), remap = false)
    private static void bgeComplementary$inheritMaterialIds(
            CallbackInfoReturnable<Object2IntMap<BlockState>> cir) {
        BgeLateRuntimeBridge.inheritMaterialIds(cir.getReturnValue());
    }

    @Inject(method = "createBlockTypeMap(Ljava/util/Map;)Ljava/util/Map;", at = @At("RETURN"), remap = false)
    private static void bgeComplementary$inheritLayerTypes(
            CallbackInfoReturnable<Map<Block, Object>> cir) {
        BgeLateRuntimeBridge.inheritLayerTypes(cir.getReturnValue());
    }
}
