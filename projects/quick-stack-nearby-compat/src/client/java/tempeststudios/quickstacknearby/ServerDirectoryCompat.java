package tempeststudios.quickstacknearby;

import net.minecraft.server.MinecraftServer;

import java.nio.file.Path;

final class ServerDirectoryCompat {
    private ServerDirectoryCompat() {
    }

    static Path path(MinecraftServer server) {
        return server.getServerDirectory();
    }
}
