package dev.resivore.pickupaudiodiag;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;

import java.util.List;

/** Client-only startup context for a deliberately temporary diagnostic utility. */
public final class PickupAudioDiagnosticClient implements ClientModInitializer {
    private static final List<String> CCAR_IDS = List.of("carried_container_auto_routing");
    private static final List<String> SOUND_PHYSICS_PERFECTED_IDS = List.of(
            "sound_physics_perfected", "sound_physics_remastered", "soundphysics");
    private static final List<String> SOUND_PHYSICS_SHUTDOWN_COMPAT_IDS = List.of(
            "sound_physics_shutdown_compat", "soundphysics_shutdown_compat", "soundphysicsshutdowncompat");
    private static final List<String> INVENTORY_EXTENDED_IDS = List.of("inventory_extended", "inventoryextended");

    @Override
    public void onInitializeClient() {
        PickupAudioDiag.logInitialization(
                SharedConstants.getCurrentVersion().name(),
                versionOf("Carried Container Auto-Routing", CCAR_IDS),
                versionOf("Sound Physics Perfected", SOUND_PHYSICS_PERFECTED_IDS),
                versionOf("Sound Physics Shutdown Compat", SOUND_PHYSICS_SHUTDOWN_COMPAT_IDS),
                versionOf("Inventory Extended", INVENTORY_EXTENDED_IDS));
    }

    private static String versionOf(String displayName, List<String> knownIds) {
        return FabricLoader.getInstance().getAllMods().stream()
                .filter(container -> knownIds.contains(container.getMetadata().getId())
                        || displayName.equalsIgnoreCase(container.getMetadata().getName()))
                .findFirst()
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("not-loaded");
    }
}
