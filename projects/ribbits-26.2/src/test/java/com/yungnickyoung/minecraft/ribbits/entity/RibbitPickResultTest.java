package com.yungnickyoung.minecraft.ribbits.entity;

import com.yungnickyoung.minecraft.ribbits.RibbitsCommon;
import com.yungnickyoung.minecraft.ribbits.data.RibbitProfession;
import com.yungnickyoung.minecraft.ribbits.module.RibbitProfessionModule;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RibbitPickResultTest {
    @Test
    void builtInProfessionsSelectTheirExactEgg() {
        assertEquals(RibbitPickResult.Egg.NITWIT,
                RibbitPickResult.eggForProfession(RibbitProfessionModule.NITWIT));
        assertEquals(RibbitPickResult.Egg.FISHERMAN,
                RibbitPickResult.eggForProfession(RibbitProfessionModule.FISHERMAN));
        assertEquals(RibbitPickResult.Egg.GARDENER,
                RibbitPickResult.eggForProfession(RibbitProfessionModule.GARDENER));
        assertEquals(RibbitPickResult.Egg.MERCHANT,
                RibbitPickResult.eggForProfession(RibbitProfessionModule.MERCHANT));
        assertEquals(RibbitPickResult.Egg.SORCERER,
                RibbitPickResult.eggForProfession(RibbitProfessionModule.SORCERER));
        assertEquals(RibbitPickResult.Egg.CHEF,
                RibbitPickResult.eggForProfession(RibbitProfessionModule.CHEF));
        assertEquals(RibbitPickResult.Egg.FARMER,
                RibbitPickResult.eggForProfession(RibbitProfessionModule.FARMER));
        assertEquals(RibbitPickResult.Egg.PROSPECTOR,
                RibbitPickResult.eggForProfession(RibbitProfessionModule.PROSPECTOR));
        assertEquals(RibbitPickResult.Egg.GUARD,
                RibbitPickResult.eggForProfession(RibbitProfessionModule.GUARD));
    }

    @Test
    void unknownProfessionFallsBackToNitwitInsteadOfAnyGenericEgg() {
        RibbitProfession custom = new RibbitProfession(
                RibbitsCommon.id("custom"), RibbitsCommon.id("custom_ribbit"),
                RibbitsCommon.id("textures/entity/custom.png"));

        assertEquals(RibbitPickResult.Egg.NITWIT, RibbitPickResult.eggForProfession(custom));
        assertEquals(RibbitPickResult.Egg.NITWIT, RibbitPickResult.eggForProfession(null));
    }

    @Test
    void equalIdButNonCanonicalProfessionStillUsesExactIdentityFallback() {
        RibbitProfession duplicateFisherman = new RibbitProfession(
                RibbitProfessionModule.FISHERMAN.id(), RibbitsCommon.id("different_model"),
                RibbitsCommon.id("textures/entity/different.png"));

        assertEquals(RibbitPickResult.Egg.NITWIT,
                RibbitPickResult.eggForProfession(duplicateFisherman));
    }
}
