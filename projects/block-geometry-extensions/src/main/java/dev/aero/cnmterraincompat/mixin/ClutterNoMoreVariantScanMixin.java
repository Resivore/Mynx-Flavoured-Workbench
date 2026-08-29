package dev.aero.cnmterraincompat.mixin;

import dev.aero.cnmterraincompat.CanonicalGeometryRegistry;
import dev.aero.cnmterraincompat.CnmTerrainCompat;
import dev.aero.cnmterraincompat.DirtVerticalSlab;
import dev.aero.cnmterraincompat.NibaruProviderAdapter;
import games.twinhead.moreslabsstairsandwalls.api.material.DerivedGeometrySupport;
import dev.tazer.clutternomore.ClutterNoMore;
import dev.tazer.clutternomore.common.blocks.StepBlock;
import dev.tazer.clutternomore.common.blocks.VerticalSlabBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WeatheringCopperSlabBlock;
import net.minecraft.world.level.block.WeatheringCopperStairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ClutterNoMore.class, remap = false)
abstract class ClutterNoMoreVariantScanMixin {
    @Inject(method = "registerVariants", at = @At("TAIL"), require = 1)
    private static void cnmTerrainCompat$registerBgeLayers(CallbackInfo ci) {
        CnmTerrainCompat.registerLayers();
    }

    @Redirect(
            method = "registerVariants",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/tazer/clutternomore/ClutterNoMore;size(Lnet/minecraft/world/level/block/state/BlockState;)I"),
            require = 2)
    private static int cnmTerrainCompat$admitNibaruProfiles(BlockState state) {
        return NibaruProviderAdapter.admissionSize(state);
    }

    @Redirect(
            method = "lambda$registerVariants$1",
            at = @At(value = "NEW", target = "Ldev/tazer/clutternomore/common/blocks/VerticalSlabBlock;"),
            require = 1)
    private static VerticalSlabBlock cnmTerrainCompat$createVerticalSlab(
            BlockBehaviour.Properties properties, SlabBlock source, String registryPath) {
        return NibaruProviderAdapter.createVertical(properties, source);
    }

    @Redirect(
            method = "lambda$registerVariants$3",
            at = @At(value = "NEW", target = "Ldev/tazer/clutternomore/common/blocks/StepBlock;"),
            require = 1)
    private static StepBlock cnmTerrainCompat$createStep(
            BlockBehaviour.Properties properties, StairBlock source, String registryPath) {
        return NibaruProviderAdapter.createStep(properties, source);
    }

    @Inject(method = "lambda$registerVariants$0", at = @At("RETURN"), require = 1)
    private static void cnmTerrainCompat$bindWeatheringVertical(SlabBlock source, String registryPath,
            WeatheringCopperSlabBlock weatheringSource, CallbackInfoReturnable<Block> cir) {
        NibaruProviderAdapter.bindGenerated(source, DerivedGeometrySupport.Geometry.VERTICAL_SLAB,
                cir.getReturnValue());
    }

    @Inject(method = "lambda$registerVariants$2", at = @At("RETURN"), require = 1)
    private static void cnmTerrainCompat$bindWeatheringStep(StairBlock source, String registryPath,
            WeatheringCopperStairBlock weatheringSource, CallbackInfoReturnable<Block> cir) {
        NibaruProviderAdapter.bindGenerated(source, DerivedGeometrySupport.Geometry.STEP, cir.getReturnValue());
    }

    /**
     * CNM's first BlockItem#getBlock call feeds its SlabBlock discovery branch.
     * Hide only project-declared canonical geometries from that branch; the
     * second call (the stair/step branch) and every ordinary source are intact.
     */
    @Redirect(
            method = "registerVariants",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/BlockItem;getBlock()Lnet/minecraft/world/level/block/Block;",
                    ordinal = 0),
            require = 1)
    private static Block cnmTerrainCompat$skipCanonicalSlabRoots(BlockItem item) {
        Block block = item.getBlock();
        return CanonicalGeometryRegistry.contains(block) ? Blocks.AIR : block;
    }
}
