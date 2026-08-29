package com.mozko.doublebarrels;

import net.minecraft.util.StringRepresentable;

public enum DoubleBarrelType implements StringRepresentable {
    SINGLE("single"),
    LEFT("left"),
    RIGHT("right"),
    TOP("top"),
    BOTTOM("bottom"),
    FRONT("front"),
    BACK("back"),
    LONG_FRONT("long_front"),
    LONG_BACK("long_back"),
    LONG_TOP("long_top"),
    LONG_BOTTOM("long_bottom");

    private final String name;

    DoubleBarrelType(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
