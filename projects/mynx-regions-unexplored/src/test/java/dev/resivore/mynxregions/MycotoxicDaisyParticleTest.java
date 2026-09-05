package dev.resivore.mynxregions;

import static org.junit.jupiter.api.Assertions.assertFalse;

import net.minecraft.SharedConstants;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.Test;

class MycotoxicDaisyParticleTest {
    @Test void vanillaEndRodParticleUsesTheNormalClientLimiter() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        assertFalse(ParticleTypes.END_ROD.getOverrideLimiter());
    }
}
