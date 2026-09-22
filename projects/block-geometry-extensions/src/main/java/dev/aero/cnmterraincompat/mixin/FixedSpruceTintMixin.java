package dev.aero.cnmterraincompat.mixin;

import dev.aero.cnmterraincompat.FoliageTintContract;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.color.block.BlockTintSources;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Keeps the exact non-root BGE spruce family on Minecraft's fixed spruce multiplier at final
 * lookup.
 *
 * <p>CNM normally replaces a shape's directly registered tint sources with its current ShapeMap
 * parent's sources. That cancellable lookup can bypass BGE's per-block registrations, and the
 * owner's actual resource-pack stack demonstrated that canonical delegation still produced biome
 * color. Resolve only the eight known BGE spruce geometries to the same fixed ARGB source that
 * Minecraft 26.2 registers for {@code minecraft:spruce_leaves}. This does not guess a pack color
 * or affect the canonical block, birch, biome foliage, or source-provider leaves. Priority 900 is
 * intentionally lower than CNM's default 1000: Mixin applies this callback later and therefore
 * prepends it at the shared HEAD, an ordering exercised by the isolated client GameTest.</p>
 */
@Mixin(value = BlockColors.class, priority = 900)
abstract class FixedSpruceTintMixin {
    private static final List<BlockTintSource> BGE_FIXED_SPRUCE_SOURCES =
            List.of(BlockTintSources.constant(FoliageTintContract.SPRUCE_FIXED_ARGB));

    @Inject(method = "getTintSources", at = @At("HEAD"), cancellable = true, require = 1)
    private void bge$useFixedSpruceTintSource(BlockState state,
            CallbackInfoReturnable<List<BlockTintSource>> cir) {
        if (!FoliageTintContract.isSpruceGeometry(state.getBlock())) return;
        cir.setReturnValue(BGE_FIXED_SPRUCE_SOURCES);
    }
}
