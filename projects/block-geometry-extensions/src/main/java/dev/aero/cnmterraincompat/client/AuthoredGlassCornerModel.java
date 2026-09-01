package dev.aero.cnmterraincompat.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.aero.cnmterraincompat.BgeCornerBlock.Orientation;
import dev.aero.cnmterraincompat.ProviderVisualAdapter;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile;
import games.twinhead.moreslabsstairsandwalls.api.material.VisualProfile;
import net.minecraft.core.Direction;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

import static dev.aero.cnmterraincompat.client.CuboidListModelProjection.Bounds;

/**
 * Authored glass Corner geometry projected deterministically from one north-east template.
 *
 * <p>The authored UV rectangles are orientation-independent. Clockwise turns rotate only model
 * bounds, face directions, boundary cull directions, and the top/bottom face frames.</p>
 */
public final class AuthoredGlassCornerModel {
    private static final List<AuthoredElement> NORTH_EAST = List.of(
            element(bounds(0, 0, 0, 16, 16, 7),
                    face(Direction.NORTH, 0, 0, 16, 16),
                    face(Direction.EAST, 9, 0, 16, 16),
                    face(Direction.WEST, 0, 0, 7, 16),
                    face(Direction.UP, 0, 0, 16, 7),
                    face(Direction.DOWN, 0, 7, 16, 0)),
            element(bounds(0, 0, 7, 7, 16, 8),
                    face(Direction.SOUTH, 0, 0, 7, 16),
                    face(Direction.WEST, 15, 0, 16, 16),
                    face(Direction.UP, 0, 0, 7, 1),
                    face(Direction.DOWN, 0, 0, 7, 1)),
            element(bounds(9, 0, 7, 16, 16, 8),
                    face(Direction.EAST, 8, 0, 9, 16),
                    face(Direction.UP, 9, 7, 16, 8),
                    face(Direction.DOWN, 9, 7, 16, 8)),
            element(bounds(7, 0, 7, 8, 16, 8),
                    face(Direction.SOUTH, 0, 0, 1, 16),
                    face(Direction.UP, 0, 8, 1, 9),
                    face(Direction.DOWN, 0, 0, 1, 1)),
            element(bounds(9, 0, 8, 16, 16, 16),
                    face(Direction.EAST, 0, 0, 8, 16),
                    face(Direction.SOUTH, 9, 0, 16, 16),
                    face(Direction.UP, 9, 8, 16, 16),
                    face(Direction.DOWN, 9, 16, 16, 8)),
            element(bounds(8, 0, 7, 9, 16, 8),
                    face(Direction.UP, 8, 0, 9, 1),
                    face(Direction.DOWN, 8, 0, 9, 1)),
            element(bounds(8, 0, 9, 9, 16, 16),
                    face(Direction.SOUTH, 0, 0, 1, 16),
                    face(Direction.WEST, 9, 0, 16, 16),
                    face(Direction.UP, 0, 9, 1, 16),
                    face(Direction.DOWN, 15, 9, 16, 16)),
            element(bounds(8, 0, 8, 9, 16, 9),
                    face(Direction.WEST, 0, 0, 1, 16),
                    face(Direction.UP, 8, 0, 9, 1),
                    face(Direction.DOWN, 8, 15, 9, 16))
    );

    private AuthoredGlassCornerModel() {}

    /** Creates the placed-world model for one physical Corner orientation. */
    public static JsonObject worldModel(NibaruMaterialProfile profile, Orientation orientation) {
        return model(profile, elementsFor(orientation), true, false);
    }

    /** Creates the canonical south-west inventory model with the shared BGE display transforms. */
    public static JsonObject itemModel(NibaruMaterialProfile profile) {
        return model(profile, elementsFor(Orientation.SOUTH_WEST), false, true);
    }

    private static JsonObject model(NibaruMaterialProfile profile, List<AuthoredElement> elements,
            boolean cullBoundary, boolean itemDisplay) {
        requireGlassEdge(profile);
        JsonObject result = baseModel(profile);
        JsonArray encodedElements = result.getAsJsonArray("elements");
        for (AuthoredElement authored : elements) {
            JsonObject encoded = new JsonObject();
            encoded.add("from", numbers(authored.bounds().x0(), authored.bounds().y0(),
                    authored.bounds().z0()));
            encoded.add("to", numbers(authored.bounds().x1(), authored.bounds().y1(),
                    authored.bounds().z1()));

            JsonObject faces = new JsonObject();
            for (AuthoredFace face : authored.faces()) {
                JsonObject encodedFace = new JsonObject();
                encodedFace.addProperty("texture", "#side");
                encodedFace.add("uv", numbers(face.uv().u0(), face.uv().v0(),
                        face.uv().u1(), face.uv().v1()));
                if (face.rotation() != 0) encodedFace.addProperty("rotation", face.rotation());
                if (cullBoundary && authored.bounds().onBoundary(face.direction())) {
                    encodedFace.addProperty("cullface", face.direction().getSerializedName());
                }
                faces.add(face.direction().getSerializedName(), encodedFace);
            }
            encoded.add("faces", faces);
            encodedElements.add(encoded);
        }
        if (itemDisplay) result.add("display", CuboidListModelProjection.itemDisplay());
        ProviderVisualAdapter.decorateModel(profile, result);
        return result;
    }

    private static JsonObject baseModel(NibaruMaterialProfile profile) {
        NibaruMaterialProfile.TextureRoles roles = profile.textureRoles();
        JsonObject result = new JsonObject();
        result.addProperty("parent", "minecraft:block/block");
        JsonObject textures = new JsonObject();
        textures.addProperty("side", texture(roles.side()));
        textures.addProperty("top", texture(roles.top()));
        textures.addProperty("bottom", texture(roles.bottom()));
        textures.addProperty("particle", texture(roles.particle()));
        if (!roles.overlay().isEmpty()) textures.addProperty("overlay", texture(roles.overlay()));
        result.add("textures", textures);
        result.add("elements", new JsonArray());
        return result;
    }

    private static void requireGlassEdge(NibaruMaterialProfile profile) {
        Objects.requireNonNull(profile, "profile");
        if (profile.visualProfile() != VisualProfile.GLASS_EDGE) {
            throw new IllegalArgumentException("Authored glass Corner model requires GLASS_EDGE: "
                    + profile.canonicalParentId());
        }
    }

    /** Immutable canonical authored elements before any orientation transform. */
    static List<AuthoredElement> canonicalElements() {
        return NORTH_EAST;
    }

    /** Immutable authored elements projected into one physical Corner orientation. */
    static List<AuthoredElement> elementsFor(Orientation orientation) {
        Objects.requireNonNull(orientation, "orientation");
        int turns = switch (orientation) {
            case NORTH_EAST -> 0;
            case SOUTH_EAST -> 1;
            case SOUTH_WEST -> 2;
            case NORTH_WEST -> 3;
        };
        List<AuthoredElement> result = NORTH_EAST;
        for (int turn = 0; turn < turns; turn++) result = rotateClockwise(result);
        return result;
    }

    /** Pure one-turn projection used recursively by every non-canonical orientation. */
    static List<AuthoredElement> rotateClockwise(List<AuthoredElement> elements) {
        Objects.requireNonNull(elements, "elements");
        return elements.stream().map(AuthoredGlassCornerModel::rotateClockwise).toList();
    }

    private static AuthoredElement rotateClockwise(AuthoredElement element) {
        Bounds bounds = element.bounds();
        Bounds rotated = new Bounds(16 - bounds.z1(), bounds.y0(), bounds.x0(),
                16 - bounds.z0(), bounds.y1(), bounds.x1());
        return new AuthoredElement(rotated,
                element.faces().stream().map(AuthoredGlassCornerModel::rotateClockwise).toList());
    }

    private static AuthoredFace rotateClockwise(AuthoredFace face) {
        int rotation = switch (face.direction()) {
            case UP -> (face.rotation() + 90) % 360;
            case DOWN -> (face.rotation() + 270) % 360;
            default -> face.rotation();
        };
        return new AuthoredFace(rotateClockwise(face.direction()), face.uv(), rotation);
    }

    private static Direction rotateClockwise(Direction direction) {
        return switch (direction) {
            case NORTH -> Direction.EAST;
            case EAST -> Direction.SOUTH;
            case SOUTH -> Direction.WEST;
            case WEST -> Direction.NORTH;
            case UP -> Direction.UP;
            case DOWN -> Direction.DOWN;
        };
    }

    private static Bounds bounds(int x0, int y0, int z0, int x1, int y1, int z1) {
        return new Bounds(x0, y0, z0, x1, y1, z1);
    }

    private static AuthoredElement element(Bounds bounds, AuthoredFace... faces) {
        return new AuthoredElement(bounds, List.of(faces));
    }

    private static AuthoredFace face(Direction direction, int u0, int v0, int u1, int v1) {
        return new AuthoredFace(direction, new Uv(u0, v0, u1, v1), 0);
    }

    private static String texture(String path) {
        return path.contains(":") ? path : "minecraft:block/" + path;
    }

    private static JsonArray numbers(double... values) {
        JsonArray result = new JsonArray();
        for (double value : values) {
            if (value == Math.rint(value)) result.add((int) value);
            else result.add(value);
        }
        return result;
    }

    record AuthoredElement(Bounds bounds, List<AuthoredFace> faces) {
        AuthoredElement {
            Objects.requireNonNull(bounds, "bounds");
            if (!bounds.hasVolume()) throw new IllegalArgumentException("Authored element needs volume");
            faces = List.copyOf(faces);
            EnumSet<Direction> directions = EnumSet.noneOf(Direction.class);
            for (AuthoredFace face : faces) {
                if (!directions.add(face.direction())) {
                    throw new IllegalArgumentException("Duplicate authored face " + face.direction());
                }
            }
        }
    }

    record AuthoredFace(Direction direction, Uv uv, int rotation) {
        AuthoredFace {
            Objects.requireNonNull(direction, "direction");
            Objects.requireNonNull(uv, "uv");
            if (rotation != 0 && rotation != 90 && rotation != 180 && rotation != 270) {
                throw new IllegalArgumentException("Unsupported face rotation: " + rotation);
            }
        }
    }

    record Uv(int u0, int v0, int u1, int v1) {
        Uv {
            if (!inTexture(u0) || !inTexture(v0) || !inTexture(u1) || !inTexture(v1)) {
                throw new IllegalArgumentException("UV coordinate outside 0..16");
            }
        }

        private static boolean inTexture(int coordinate) {
            return coordinate >= 0 && coordinate <= 16;
        }
    }
}
