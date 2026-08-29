package com.yungnickyoung.minecraft.ribbits.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class RibbitsConfigParityTest {
    @Test
    void laterRegionalPrideOptionDoesNotChangeExactDefaultBehavior() {
        RibbitsConfig config = new RibbitsConfig();

        assertFalse(config.general.prideFlagAllYear);
        assertFalse(config.general.disablePrideFlagCN);
    }
}
