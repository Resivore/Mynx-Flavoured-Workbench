package tempeststudios.quickstacknearby;

import net.fabricmc.api.ClientModInitializer;

public final class QuickStackNearbyClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        QuickStackHotkey.register();
        QuickStackClientNetworkingCompat.registerNearbySearchReceivers();
        QuickStackInventoryControls.register();
        QuickStackNearbySmokeTest.registerIfEnabled();
    }
}
