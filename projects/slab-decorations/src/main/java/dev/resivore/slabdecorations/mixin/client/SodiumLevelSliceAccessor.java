package dev.resivore.slabdecorations.mixin.client;

import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Optional Sodium terrain-snapshot bridge. Sodium's LevelSlice is not a LevelReader, but it
 * retains immutable block data and its owning ClientLevel, exactly like the vanilla snapshot.
 */
@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.world.LevelSlice", remap = false)
public interface SodiumLevelSliceAccessor {
    @Accessor("level")
    ClientLevel slabDecorations$getLevel();
}
