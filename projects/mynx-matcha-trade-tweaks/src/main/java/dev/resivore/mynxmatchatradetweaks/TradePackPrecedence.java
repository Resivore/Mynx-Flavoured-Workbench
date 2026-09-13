package dev.resivore.mynxmatchatradetweaks;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.packs.PackResources;

public final class TradePackPrecedence {
    private TradePackPrecedence() {
    }

    /** Places only this project's built-in pack last, preserving all other pack relationships. */
    public static List<PackResources> prioritize(List<PackResources> packs) {
        int tradePackIndex = -1;

        for (int index = 0; index < packs.size(); index++) {
            if (!MynxMatchaTradeTweaks.BUILTIN_PACK_RESOURCE_ID.equals(packs.get(index).packId())) {
                continue;
            }
            if (tradePackIndex >= 0) {
                throw new IllegalStateException(
                        "Duplicate built-in pack id: " + MynxMatchaTradeTweaks.BUILTIN_PACK_RESOURCE_ID);
            }
            tradePackIndex = index;
        }

        if (tradePackIndex < 0 || tradePackIndex == packs.size() - 1) {
            return packs;
        }

        ArrayList<PackResources> ordered = new ArrayList<>(packs);
        PackResources tradePack = ordered.remove(tradePackIndex);
        ordered.add(tradePack);
        return List.copyOf(ordered);
    }
}
