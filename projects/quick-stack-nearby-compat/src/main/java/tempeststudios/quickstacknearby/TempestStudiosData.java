package tempeststudios.quickstacknearby;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

public final class TempestStudiosData {
    private static final String UNKNOWN = "unknown";

    public enum ModDataFolder {
        QUICK_STACK_NEARBY("QuickStackNearby", "quick-stack-nearby");

        private final String desktopFolder;
        private final String linuxFolder;

        ModDataFolder(String desktopFolder, String linuxFolder) {
            this.desktopFolder = desktopFolder;
            this.linuxFolder = linuxFolder;
        }
    }

    private TempestStudiosData() {
    }

    public static Path modRoot(ModDataFolder folder) {
        return appDataRoot().resolve(isLinux() ? folder.linuxFolder : folder.desktopFolder);
    }

    public static String fileNameSafe(String namespace) {
        return sanitize(namespace).replace(':', '_');
    }

    public static String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }
        return value.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9._:-]+", "_");
    }

    public static String shortHash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < Math.min(6, digest.length); i++) {
                builder.append(String.format(Locale.ROOT, "%02x", digest[i] & 0xff));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(value.hashCode());
        }
    }

    public static Path canonicalPath(Path path) {
        Path normalized = path.toAbsolutePath().normalize();
        try {
            return normalized.toRealPath();
        } catch (IOException ignored) {
            return normalized;
        }
    }

    private static Path appDataRoot() {
        if (isWindows()) {
            String appData = System.getenv("APPDATA");
            if (appData != null && !appData.isBlank()) {
                return Path.of(appData, "TempestStudios");
            }
            return Path.of(userHome(), "AppData", "Roaming", "TempestStudios");
        }
        if (isMac()) {
            return Path.of(userHome(), "Library", "Application Support", "TempestStudios");
        }

        String xdgDataHome = System.getenv("XDG_DATA_HOME");
        Path base = xdgDataHome != null && !xdgDataHome.isBlank()
                ? Path.of(xdgDataHome)
                : Path.of(userHome(), ".local", "share");
        return base.resolve("tempest-studios");
    }

    private static String userHome() {
        return System.getProperty("user.home", ".");
    }

    private static boolean isWindows() {
        return osName().contains("win");
    }

    private static boolean isMac() {
        return osName().contains("mac");
    }

    private static boolean isLinux() {
        return !isWindows() && !isMac();
    }

    private static String osName() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
    }
}
