package dev.aero.cnmterraincompat;

import dev.tazer.clutternomore.ClutterNoMore;

/** Identifies CNM's two views of its own generated language resources. */
public final class CnmGeneratedLanguage {
    private CnmGeneratedLanguage() {}

    public static boolean isSelfGeneratedPack(String sourcePackId) {
        if ((ClutterNoMore.MODID + "-runtime").equals(sourcePackId)) return true;
        var packName = ClutterNoMore.pack.getFileName();
        return packName != null && ("file/" + packName).equals(sourcePackId);
    }
}
