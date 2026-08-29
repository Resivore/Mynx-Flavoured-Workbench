package dev.resivore.mapmarkerextension.compat;

import dev.resivore.mapmarkerextension.core.ChunkRelativePosition;
import net.minecraft.network.chat.Component;
import xaero.hud.minimap.info.InfoDisplay;
import xaero.hud.minimap.info.widget.InfoDisplayCommonWidgetFactories;
import xaero.lib.common.config.option.value.io.serialization.BuiltInConfigValueIOCodecs;

final class XaeroChunkInfoDisplay {
    // Retained so the user's persisted OFF preference survives the project rename.
    static final String ID = "treasure_x_chunk_relative";

    private XaeroChunkInfoDisplay() {
    }

    static InfoDisplay<Boolean> create() {
        return InfoDisplay.Builder.<Boolean>begin()
            .setId(ID)
            .setName(Component.translatable("gui.map_marker_extension.chunk_relative"))
            .setDefaultState(true)
            .setCodec(BuiltInConfigValueIOCodecs.BOOLEAN)
            .setWidgetFactory(InfoDisplayCommonWidgetFactories.OFF_ON)
            .setCompiler((display, compiler, session, availableWidth, playerPos) -> {
                if (display.getEffectiveState()) {
                    compiler.addLine(ChunkRelativePosition.format(playerPos.getX(), playerPos.getZ()));
                }
            })
            .build();
    }
}
