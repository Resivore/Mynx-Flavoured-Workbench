package dev.aero.cnmterraincompat.mixin;

import dev.aero.cnmterraincompat.NibaruProviderAdapter;
import it.unimi.dsi.fastutil.objects.Object2IntSortedMap;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.FuelValues;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Fuel inheritance for BGE-local roles absent from exact Nibaru C46's role enum. */
@Mixin(FuelValues.Builder.class)
abstract class BgeFuelValuesBuilderMixin {
    @Shadow @Final private Object2IntSortedMap<Item> values;

    @Inject(method = "build", at = @At("HEAD"))
    private void cnmTerrainCompat$inheritLocalGeometryFuel(
            CallbackInfoReturnable<FuelValues> callbackInfo) {
        FuelValues.Builder builder = (FuelValues.Builder) (Object) this;
        for (NibaruProviderAdapter.LocalMaterialTrait trait
                : NibaruProviderAdapter.localMaterialTraits()) {
            int inherited = values.getInt(trait.canonicalParent().asItem()) / trait.fuelDivisor();
            if (inherited > 0) builder.add(trait.derived(), inherited);
        }
    }
}
