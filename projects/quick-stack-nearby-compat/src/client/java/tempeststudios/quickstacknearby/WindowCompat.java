package tempeststudios.quickstacknearby;

import com.mojang.blaze3d.platform.Window;

public final class WindowCompat {
    private WindowCompat() {
    }

    public static long handle(Window window) {
        return window.handle();
    }
}
