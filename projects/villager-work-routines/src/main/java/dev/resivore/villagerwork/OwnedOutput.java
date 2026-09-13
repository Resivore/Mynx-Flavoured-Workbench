package dev.resivore.villagerwork;

import net.minecraft.world.SimpleContainer;

/** Separate persisted output prevents unrelated item entities or villager inventory contents acquiring ownership. */
public interface OwnedOutput {
    SimpleContainer villagerWork$ownedOutput();
}
