package games.twinhead.moreslabsstairsandwalls.fabric.mixin;

import games.twinhead.moreslabsstairsandwalls.block.ModBlocks;
import games.twinhead.moreslabsstairsandwalls.api.material.DerivedMaterialTraits;
import it.unimi.dsi.fastutil.objects.Object2IntSortedMap;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.FuelValues;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Copies each exact source item's final resolved fuel duration to its Nibaru
 * geometries immediately before Minecraft freezes the fuel table.
 */
@Mixin(FuelValues.Builder.class)
abstract class FuelValuesBuilderMixin {
    @Shadow
    @Final
    private Object2IntSortedMap<Item> values;

    @Inject(method = "build", at = @At("HEAD"))
    private void moreSlabsStairsAndWalls$inheritResolvedFuelValues(
            CallbackInfoReturnable<FuelValues> callbackInfo) {
        FuelValues.Builder builder = (FuelValues.Builder) (Object) this;

        for (ModBlocks family : ModBlocks.values()) {
            int parentDuration = values.getInt(family.parentBlock.asItem());
            if (parentDuration <= 0) {
                continue;
            }

            for (ModBlocks.BlockType type : ModBlocks.BlockType.values()) {
                if (!family.hasBlock(type)) {
                    continue;
                }

                Block generatedBlock = family.getBlock(type);
                int inheritedDuration = type == ModBlocks.BlockType.SLAB
                        ? parentDuration / 2
                        : parentDuration;
                if (inheritedDuration > 0) {
                    builder.add(generatedBlock, inheritedDuration);
                }
            }
        }
        for (DerivedMaterialTraits.Entry entry : DerivedMaterialTraits.entries()) {
            int parentDuration = values.getInt(entry.canonicalParent().asItem());
            int inheritedDuration = parentDuration / entry.fuelDivisor();
            if (inheritedDuration > 0) builder.add(entry.derived(), inheritedDuration);
        }
    }
}
