package com.mozko.doublebarrels;

import net.minecraft.world.level.block.state.properties.EnumProperty;

public final class DoubleBarrelProperties {
    public static final EnumProperty<DoubleBarrelType> DOUBLE =
            EnumProperty.create("double_barrel", DoubleBarrelType.class);

    private DoubleBarrelProperties() {
    }
}
