package com.yungnickyoung.minecraft.ribbits.data;

import net.minecraft.resources.Identifier;

import java.util.Objects;

public record RibbitProfession(Identifier id, Identifier modelLocation, Identifier textureLocation) {
    public RibbitProfession {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(modelLocation, "modelLocation");
        Objects.requireNonNull(textureLocation, "textureLocation");
    }

    @Override
    public String toString() {
        return this.id.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof RibbitProfession other)) {
            return false;
        } else {
            return this.id.equals(other.id());
        }
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }
}
