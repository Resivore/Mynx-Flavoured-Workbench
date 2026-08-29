package games.twinhead.moreslabsstairsandwalls.fabric;

import games.twinhead.moreslabsstairsandwalls.MoreSlabsStairsAndWalls;
import games.twinhead.moreslabsstairsandwalls.registry.fabric.ModRegistry;
import games.twinhead.moreslabsstairsandwalls.block.spreadable.SpreadableSemantics;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfiles;
import net.fabricmc.api.ModInitializer;

public class MoreSlabsStairsAndWallsFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        MoreSlabsStairsAndWalls.init();
        ModRegistry.registerBlocks();
        NibaruMaterialProfiles.refresh();
        SpreadableSemantics.registerNativePairs();
    }
}
