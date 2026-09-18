package dev.resivore.bgectm.client;

import dev.resivore.bgectm.BgeCtmDiagnostics;
import net.fabricmc.api.ClientModInitializer;

/** Emits one session-level compatibility/diagnostic configuration line. */
public final class BgeCtmClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BgeCtmDiagnostics.startup();
    }
}
