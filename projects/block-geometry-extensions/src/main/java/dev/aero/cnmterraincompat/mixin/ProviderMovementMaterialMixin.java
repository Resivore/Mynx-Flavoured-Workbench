package dev.aero.cnmterraincompat.mixin;

import dev.aero.cnmterraincompat.NibaruProviderAdapter;
import dev.aero.cnmterraincompat.BgeCornerBlock;
import dev.aero.cnmterraincompat.BgeGeometryRole;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.Half;
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
        if (binding.role() == BgeGeometryRole.VERTICAL_SLAB
                || binding.role() == BgeGeometryRole.LAYER
                || binding.role() == BgeGeometryRole.QUARTER_COLUMN
                || binding.role() == BgeGeometryRole.CORNER
                && state.hasProperty(BgeCornerBlock.HALF)
                && state.getValue(BgeCornerBlock.HALF) == Half.BOTTOM
                || binding.role() == BgeGeometryRole.STEP
                && state.hasProperty(BlockStateProperties.SLAB_TYPE)
                && state.getValue(BlockStateProperties.SLAB_TYPE) == SlabType.BOTTOM) {
            cir.setReturnValue(current);
        }
    }
}
