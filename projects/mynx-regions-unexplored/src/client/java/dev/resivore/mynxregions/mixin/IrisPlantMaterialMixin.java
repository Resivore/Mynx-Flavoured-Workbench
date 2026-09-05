package dev.resivore.mynxregions.mixin;

import dev.resivore.mynxregions.MynxRegionsUnexplored;
import dev.resivore.mynxregions.ShaderMaterialAliases;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.List;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.irisshaders.iris.shaderpack.materialmap.BlockMaterialMapping", remap = false)
public abstract class IrisPlantMaterialMixin {
    @Inject(method = "createBlockStateIdMap(Lit/unimi/dsi/fastutil/ints/Int2ObjectLinkedOpenHashMap;Lit/unimi/dsi/fastutil/ints/Int2ObjectLinkedOpenHashMap;)Lit/unimi/dsi/fastutil/objects/Object2IntMap;",
            at = @At("RETURN"), remap = false)
    private static void mynxRegions$inheritPlantMaterials(CallbackInfoReturnable<Object2IntMap<BlockState>> cir) {
        Object2IntMap<BlockState> ids = cir.getReturnValue();
        inherit(ids, List.of(MynxRegionsUnexplored.STONE_BUD, MynxRegionsUnexplored.CLOVER,
                        MynxRegionsUnexplored.HYSSOP, MynxRegionsUnexplored.BLUE_LUPINE, MynxRegionsUnexplored.PINK_LUPINE,
                        MynxRegionsUnexplored.PURPLE_LUPINE, MynxRegionsUnexplored.RED_LUPINE, MynxRegionsUnexplored.YELLOW_LUPINE),
                Blocks.SHORT_GRASS.defaultBlockState());
        for (Block tall : List.of(MynxRegionsUnexplored.BARLEY, MynxRegionsUnexplored.WINDSWEPT_GRASS,
                MynxRegionsUnexplored.MYCOTOXIC_DAISY, MynxRegionsUnexplored.CATTAIL,
                MynxRegionsUnexplored.TASSEL, MynxRegionsUnexplored.MEADOW_SAGE)) {
            ShaderMaterialAliases.inheritUnmapped(ids, tall.getStateDefinition().getPossibleStates(), Blocks.TALL_GRASS::withPropertiesOf);
        }
        ShaderMaterialAliases.inheritUnmapped(ids, MynxRegionsUnexplored.DUCKWEED.getStateDefinition().getPossibleStates(),
                state -> Blocks.LILY_PAD.defaultBlockState());
        ShaderMaterialAliases.inheritUnmapped(ids, MynxRegionsUnexplored.DROPLEAF.getStateDefinition().getPossibleStates(),
                Blocks.WEEPING_VINES::withPropertiesOf);
        ShaderMaterialAliases.inheritUnmapped(ids, MynxRegionsUnexplored.DROPLEAF_PLANT.getStateDefinition().getPossibleStates(),
                state -> Blocks.WEEPING_VINES_PLANT.defaultBlockState());
    }
    private static void inherit(Object2IntMap<BlockState> ids, List<Block> blocks, BlockState counterpart) {
        for (Block block : blocks) ShaderMaterialAliases.inheritUnmapped(ids, block.getStateDefinition().getPossibleStates(), state -> counterpart);
    }
}
