package dev.resivore.mapmarkerextension.compat;

import dev.resivore.mapmarkerextension.MapMarkerExtension;
import dev.resivore.mapmarkerextension.client.MapMarkerTargetRepository;
import dev.resivore.mapmarkerextension.compat.minimap.XaeroMinimapMarkerRenderer;
import dev.resivore.mapmarkerextension.compat.worldmap.XaeroWorldMapMarkerRenderer;
import xaero.common.HudMod;
import xaero.hud.minimap.Minimap;
import xaero.hud.minimap.element.render.over.MinimapElementOverMapRendererHandler;
import xaero.hud.minimap.info.InfoDisplayManager;
import xaero.map.WorldMap;
import xaero.map.element.MapElementRenderHandler;

public final class XaeroIntegration {
    private final MapMarkerTargetRepository targets;
    private MinimapElementOverMapRendererHandler installedMinimapHandler;
    private MapElementRenderHandler installedWorldMapHandler;
    private InfoDisplayManager installedInfoManager;

    public XaeroIntegration(MapMarkerTargetRepository targets) {
        this.targets = targets;
    }

    public void installIfReady() {
        if (!HudMod.INSTANCE.isLoadedClient()) {
            return;
        }
        Minimap minimap = HudMod.INSTANCE.getMinimap();
        if (minimap != null) {
            installMinimapRenderer(minimap);
            installChunkInfoDisplay(minimap);
        }

        MapElementRenderHandler worldMapHandler = WorldMap.loaded
            ? WorldMap.mapElementRenderHandler
            : null;
        if (worldMapHandler != null && worldMapHandler != installedWorldMapHandler) {
            worldMapHandler.add(new XaeroWorldMapMarkerRenderer(targets));
            installedWorldMapHandler = worldMapHandler;
            MapMarkerExtension.LOGGER.info(
                "Installed carried map-marker renderer in Xaero's World Map"
            );
        }
    }

    private void installMinimapRenderer(Minimap minimap) {
        MinimapElementOverMapRendererHandler handler = minimap.getOverMapRendererHandler();
        if (handler != installedMinimapHandler) {
            handler.add(new XaeroMinimapMarkerRenderer(targets));
            installedMinimapHandler = handler;
            MapMarkerExtension.LOGGER.info(
                "Installed carried map-marker renderer in Xaero's Minimap"
            );
        }
    }

    private void installChunkInfoDisplay(Minimap minimap) {
        InfoDisplayManager manager = minimap.getInfoDisplays().getManager();
        if (manager == installedInfoManager) {
            return;
        }
        if (manager.get(XaeroChunkInfoDisplay.ID) == null) {
            manager.add(XaeroChunkInfoDisplay.create());
            manager.applyLocalConfig();
            manager.clearStateCache();
            MapMarkerExtension.LOGGER.info("Registered chunk-relative Xaero info display");
        }
        installedInfoManager = manager;
    }
}
