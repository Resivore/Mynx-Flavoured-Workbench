package tempeststudios.quickstacknearby;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

import java.io.IOException;
import java.nio.file.Path;

public final class QuickStackTrackingNamespace {
    private static final String UNKNOWN = "unknown";

    private QuickStackTrackingNamespace() {
    }

    public static String current(Minecraft client) {
        if (client == null) {
            return UNKNOWN;
        }

        if (ClientStateCompat.hasSingleplayerServer(client) && client.getSingleplayerServer() != null) {
            String worldId = UNKNOWN;
            try {
                Path serverDir = ServerDirectoryCompat.path(client.getSingleplayerServer());
                if (serverDir != null && serverDir.getFileName() != null) {
                    worldId = serverDir.getFileName().toString();
                }
            } catch (Exception ignored) {
                worldId = client.getSingleplayerServer().getWorldData().getLevelName();
            }
            return singleplayerNamespace(client, worldId);
        }

        ServerData server = client.getCurrentServer();
        if (server != null) {
            String serverId = server.ip != null && !server.ip.isBlank() ? server.ip : server.name;
            return accountScopedServerNamespace(client, "server:" + TempestStudiosData.sanitize(serverId));
        }

        return UNKNOWN;
    }

    private static String singleplayerNamespace(Minecraft client, String worldId) {
        return "singleplayer:" + instanceId(client) + ":" + TempestStudiosData.sanitize(worldId);
    }

    private static String accountScopedServerNamespace(Minecraft client, String namespace) {
        String normalized = TempestStudiosData.sanitize(namespace);
        if (!normalized.startsWith("server:") || normalized.contains(":account:")) {
            return normalized;
        }
        return normalized + ":account:" + accountId(client);
    }

    private static String instanceId(Minecraft client) {
        Path path = canonicalGameDirectory(client);
        return "instance_" + TempestStudiosData.shortHash(path.toString());
    }

    private static String accountId(Minecraft client) {
        try {
            if (client != null && client.getUser() != null && client.getUser().getProfileId() != null) {
                return "account_" + TempestStudiosData.shortHash(client.getUser().getProfileId().toString());
            }
            if (client != null && client.getUser() != null && client.getUser().getName() != null) {
                return "account_" + TempestStudiosData.shortHash(client.getUser().getName());
            }
        } catch (Exception ignored) {
        }
        return "account_unknown";
    }

    private static Path canonicalGameDirectory(Minecraft client) {
        Path path = gameDirectory(client).toAbsolutePath().normalize();
        try {
            return path.toRealPath();
        } catch (IOException ignored) {
            return path;
        }
    }

    private static Path gameDirectory(Minecraft client) {
        if (client != null && client.gameDirectory != null) {
            return client.gameDirectory.toPath();
        }
        return Path.of(System.getProperty("user.dir", "."));
    }
}
