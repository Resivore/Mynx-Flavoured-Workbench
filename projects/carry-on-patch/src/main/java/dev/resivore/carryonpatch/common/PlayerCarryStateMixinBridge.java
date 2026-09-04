package dev.resivore.carryonpatch.common;

import net.minecraft.nbt.CompoundTag;

public interface PlayerCarryStateMixinBridge {
    void carryOnPatch$restore(CompoundTag data, boolean carrying);
}
