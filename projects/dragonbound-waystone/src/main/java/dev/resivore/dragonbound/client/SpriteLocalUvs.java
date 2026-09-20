package dev.resivore.dragonbound.client;

import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;

/** Exact atlas-to-local conversion required before Fabric's sprite rebake API is called. */
final class SpriteLocalUvs {
    private SpriteLocalUvs() {
    }

    static boolean unbake(MutableQuadView quad, TextureAtlasSprite source) {
        for (int vertex = 0; vertex < BakedQuad.VERTEX_COUNT; vertex++) {
            OptionalCoordinates local = toLocal(
                    quad.u(vertex), quad.v(vertex),
                    source.getU0(), source.getU1(), source.getV0(), source.getV1());
            if (local == null) {
                return false;
            }
            quad.uv(vertex, local.u(), local.v());
        }
        return true;
    }

    static boolean isInside(BakedQuad quad, TextureAtlasSprite sprite) {
        for (int vertex = 0; vertex < BakedQuad.VERTEX_COUNT; vertex++) {
            long packed = quad.packedUV(vertex);
            if (!isInside(UVPair.unpackU(packed), sprite.getU0(), sprite.getU1())
                    || !isInside(UVPair.unpackV(packed), sprite.getV0(), sprite.getV1())) {
                return false;
            }
        }
        return true;
    }

    static OptionalCoordinates toLocal(
            float atlasU,
            float atlasV,
            float sourceU0,
            float sourceU1,
            float sourceV0,
            float sourceV1) {
        Float localU = toLocal(atlasU, sourceU0, sourceU1);
        Float localV = toLocal(atlasV, sourceV0, sourceV1);
        return localU == null || localV == null ? null : new OptionalCoordinates(localU, localV);
    }

    static Float toLocal(float atlasCoordinate, float spriteStart, float spriteEnd) {
        float span = spriteEnd - spriteStart;
        if (!Float.isFinite(atlasCoordinate) || !Float.isFinite(span) || span <= 0.0F) {
            return null;
        }
        float local = (atlasCoordinate - spriteStart) / span;
        return Float.isFinite(local) && local >= 0.0F && local <= 1.0F ? local : null;
    }

    static float toAtlas(float localCoordinate, float spriteStart, float spriteEnd) {
        return spriteStart + localCoordinate * (spriteEnd - spriteStart);
    }

    static boolean isInside(float atlasCoordinate, float spriteStart, float spriteEnd) {
        return Float.isFinite(atlasCoordinate)
                && Float.isFinite(spriteStart)
                && Float.isFinite(spriteEnd)
                && spriteStart <= atlasCoordinate
                && atlasCoordinate <= spriteEnd;
    }

    record OptionalCoordinates(float u, float v) {
    }
}
