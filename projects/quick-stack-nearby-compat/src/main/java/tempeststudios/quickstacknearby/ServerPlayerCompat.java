package tempeststudios.quickstacknearby;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

final class ServerPlayerCompat {
    private ServerPlayerCompat() {
    }

    static ServerLevel serverLevel(ServerPlayer player) {
        return player.level();
    }
}
