package dev.aero.cnmterraincompat.mixin;

import dev.aero.cnmterraincompat.NibaruProviderAdapter;
import games.twinhead.moreslabsstairsandwalls.api.material.DerivedGeometrySupport;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Geometry-side movement lookup for provider-bound companion blocks. */
@Mixin(Entity.class)
public abstract class ProviderMovementMaterialMixin {
    @Inject(method = "getBlockPosBelowThatAffectsMyMovement", at = @At("HEAD"), cancellable = true)
    private void cnmNibaru$movementMaterialPosition(CallbackInfoReturnable<BlockPos> cir) {
        Entity self = (Entity) (Object) this;
        BlockPos current = self.blockPosition();
        var state = self.level().getBlockState(current);
        var binding = NibaruProviderAdapter.runtimeBinding(state.getBlock()).orElse(null);
        if (binding == null || !binding.profile().derivedBlockTags().contains(BlockTags.SOUL_SPEED_BLOCKS)) return;
        if (binding.geometry() == DerivedGeometrySupport.Geometry.VERTICAL_SLAB
                || binding.geometry() == DerivedGeometrySupport.Geometry.LAYER
                || binding.geometry() == DerivedGeometrySupport.Geometry.STEP
                && state.hasProperty(BlockStateProperties.SLAB_TYPE)
                && state.getValue(BlockStateProperties.SLAB_TYPE) == SlabType.BOTTOM) {
            cir.setReturnValue(current);
        }
    }
}
