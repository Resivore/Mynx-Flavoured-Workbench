package dev.resivore.enderscapeintegration;

import net.minecraft.advancements.Advancement;
import net.minecraft.resources.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;

/** Removes only complete candidate branches, preserving all retained advancement parents. */
public final class IntegrationAdvancements {
    private IntegrationAdvancements() {
    }

    public static Map<Identifier, Advancement> removeSuppressed(Map<Identifier, Advancement> resolved) {
        LinkedHashMap<Identifier, Advancement> retained = new LinkedHashMap<>(resolved);
        IntegrationContract.BLOCKED_ADVANCEMENT_IDS.forEach(id -> retained.remove(Identifier.parse(id)));
        return Map.copyOf(retained);
    }
}
