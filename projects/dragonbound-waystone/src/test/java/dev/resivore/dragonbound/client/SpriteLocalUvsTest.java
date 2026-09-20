package dev.resivore.dragonbound.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SpriteLocalUvsTest {
    private static final float EPSILON = 0.000001F;

    @Test
    void warpedHyphaeStyleTargetMappingStaysWithinOneDonorSpriteInsteadOfSpanningAtlasTiles() {
        float sourceStart = 0.25F;
        float sourceEnd = 0.3125F;
        float donorStart = 0.6875F;
        float donorEnd = 0.75F;

        for (float sourceCoordinate : new float[]{sourceStart, 0.265625F, 0.296875F, sourceEnd}) {
            Float local = SpriteLocalUvs.toLocal(sourceCoordinate, sourceStart, sourceEnd);
            assertNotNull(local);
            float donorCoordinate = SpriteLocalUvs.toAtlas(local, donorStart, donorEnd);
            assertTrue(SpriteLocalUvs.isInside(donorCoordinate, donorStart, donorEnd));
        }

        assertEquals(donorStart, SpriteLocalUvs.toAtlas(0.0F, donorStart, donorEnd), EPSILON);
        assertEquals(donorEnd, SpriteLocalUvs.toAtlas(1.0F, donorStart, donorEnd), EPSILON);
    }

    @Test
    void localRegionPreservesTheWaystoneUvExtentForBothAxes() {
        SpriteLocalUvs.OptionalCoordinates local = SpriteLocalUvs.toLocal(
                0.28125F, 0.53125F,
                0.25F, 0.3125F,
                0.5F, 0.5625F);

        assertNotNull(local);
        assertEquals(0.5F, local.u(), EPSILON);
        assertEquals(0.5F, local.v(), EPSILON);
        assertEquals(0.65625F, SpriteLocalUvs.toAtlas(local.u(), 0.625F, 0.6875F), EPSILON);
        assertEquals(0.15625F, SpriteLocalUvs.toAtlas(local.v(), 0.125F, 0.1875F), EPSILON);
    }

    @Test
    void outOfSpriteOrInvalidCoordinatesCannotBeRebakedOntoAnotherAtlasRegion() {
        assertNull(SpriteLocalUvs.toLocal(0.24F, 0.25F, 0.3125F));
        assertNull(SpriteLocalUvs.toLocal(0.313F, 0.25F, 0.3125F));
        assertNull(SpriteLocalUvs.toLocal(Float.NaN, 0.25F, 0.3125F));
        assertNull(SpriteLocalUvs.toLocal(0.25F, 0.5F, 0.5F));
        assertFalse(SpriteLocalUvs.isInside(0.6249F, 0.625F, 0.6875F));
        assertFalse(SpriteLocalUvs.isInside(0.6876F, 0.625F, 0.6875F));
    }
}
