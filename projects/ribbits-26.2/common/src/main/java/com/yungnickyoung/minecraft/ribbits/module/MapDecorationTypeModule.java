package com.yungnickyoung.minecraft.ribbits.module;

import com.yungnickyoung.minecraft.ribbits.RibbitsCommon;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;

/** Registers the one Ribbits-owned native marker used by every village explorer map. */
public final class MapDecorationTypeModule {
    public static final Identifier RIBBIT_VILLAGE_ID = RibbitsCommon.id("ribbit_village");

    private static volatile Holder<MapDecorationType> ribbitVillage;

    private MapDecorationTypeModule() {
    }

    public static synchronized void init() {
        if (ribbitVillage != null) {
            return;
        }

        Holder<MapDecorationType> existing = BuiltInRegistries.MAP_DECORATION_TYPE
                .get(RIBBIT_VILLAGE_ID)
                .map(holder -> (Holder<MapDecorationType>) holder)
                .orElse(null);
        if (existing != null) {
            ribbitVillage = existing;
            return;
        }

        // Built-in holders are still unbound during Fabric's registration phase, so reading
        // PLAINS_VILLAGE.value() here would fail before the registry is frozen. These are the
        // exact 26.2 plains-village flags; the contract test compares them after bootstrap.
        MapDecorationType type = new MapDecorationType(
                RIBBIT_VILLAGE_ID,
                true,
                MapColor.COLOR_LIGHT_GRAY.col,
                true,
                false);
        ribbitVillage = Registry.registerForHolder(
                BuiltInRegistries.MAP_DECORATION_TYPE,
                RIBBIT_VILLAGE_ID,
                type);
    }

    public static Holder<MapDecorationType> ribbitVillage() {
        Holder<MapDecorationType> holder = ribbitVillage;
        if (holder == null) {
            throw new IllegalStateException("Ribbit village map decoration type is not registered");
        }
        return holder;
    }
}
