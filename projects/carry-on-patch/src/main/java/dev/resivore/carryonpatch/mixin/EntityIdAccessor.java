package dev.resivore.carryonpatch.mixin;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Reads the raw Minecraft 26.2 ID without invoking Entity.getId's throwing sentinel guard. */
@Mixin(Entity.class)
public interface EntityIdAccessor {
    @Accessor("id")
    int carryOnPatch$getRawId();
}
