package com.crispytwig.naturalist.mixin;

import com.crispytwig.naturalist.server.entity.variant.LegacyVariantRemap;
import com.crispytwig.naturalist.registry.NaturalistEntityTypes;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(EntityType.class)
public class EntityTypeMixin {
    @Inject(
            method = "by(Lnet/minecraft/world/level/storage/ValueInput;)Ljava/util/Optional;",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void naturalist$remapLegacyMobs(ValueInput input, CallbackInfoReturnable<Optional<EntityType<?>>> cir) {
        input.getString("id").ifPresent(id -> {
            if (LegacyVariantRemap.isLegacyBirdEntityId(id)) {
                cir.setReturnValue(Optional.of(NaturalistEntityTypes.BIRD.get()));
            } else if (LegacyVariantRemap.isLegacySnakeEntityId(id)) {
                cir.setReturnValue(Optional.of(NaturalistEntityTypes.SNAKE.get()));
            }
        });
    }
}
