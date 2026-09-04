package com.yungnickyoung.minecraft.ribbits.chute;

/** Mixin bridge for the one nonpersistent, entity-tracked authoritative state bit. */
public interface ChutePlayerAccess {
    boolean ribbits$isChuteDeployed();

    void ribbits$setChuteDeployed(boolean deployed);
}
