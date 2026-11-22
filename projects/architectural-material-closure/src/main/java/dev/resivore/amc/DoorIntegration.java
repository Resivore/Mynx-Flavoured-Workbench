package dev.resivore.amc;

import java.util.LinkedHashSet;
import java.util.Set;

/** Both providers may initialize in either order before the block registry freezes. */
public final class DoorIntegration {
    private static final Set<String> WOODS_READY = new LinkedHashSet<>();
    private static boolean macawReady;

    public static void doorsReady() {
        macawReady = true;
        WOODS_READY.forEach(MacawDoorBlocks::registerPack);
    }

    public static void woodReady(String provider) {
        if (WOODS_READY.add(provider) && macawReady) MacawDoorBlocks.registerPack(provider);
    }
}
