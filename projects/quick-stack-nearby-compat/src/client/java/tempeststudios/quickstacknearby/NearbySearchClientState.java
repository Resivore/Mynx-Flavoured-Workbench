package tempeststudios.quickstacknearby;

import net.minecraft.client.Minecraft;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Ephemeral client state: no worlds, locations, timestamps, or historical snapshots are persisted. */
final class NearbySearchClientState {
    private static List<NearbySearchPayload.Entry> snapshot = List.of();
    private static NearbySearchScreen active;

    private NearbySearchClientState() {}
    static List<NearbySearchPayload.Entry> snapshot() { return snapshot; }
    static void setActive(NearbySearchScreen screen) { active = screen; }
    static void clearActive(NearbySearchScreen screen) { if (active == screen) active = null; }
    static void acceptSnapshot(List<NearbySearchPayload.Entry> entries) {
        snapshot = List.copyOf(entries);
        if (active != null) active.refreshFromServer();
    }
    static void acceptTargetResult(NearbySearchPayload.TargetResult result) {
        Minecraft client = Minecraft.getInstance();
        if (active == null) return;
        if (!result.accepted()) { active.targetRejected(); return; }
        if (client.player != null) client.player.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(result.position()));
        active.targetAccepted();
    }
}
