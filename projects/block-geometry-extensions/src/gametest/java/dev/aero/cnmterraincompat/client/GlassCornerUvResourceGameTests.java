package dev.aero.cnmterraincompat.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.aero.cnmterraincompat.BgeCornerBlock.Orientation;
import dev.aero.cnmterraincompat.client.AuthoredGlassCornerModel.AuthoredElement;
import dev.aero.cnmterraincompat.client.AuthoredGlassCornerModel.AuthoredFace;
import dev.aero.cnmterraincompat.client.CornerColumnModelProjection.ColumnOccupancy;
import dev.aero.cnmterraincompat.client.CuboidListModelProjection.Bounds;
import dev.aero.cnmterraincompat.client.CuboidListModelProjection.Cuboid;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfiles;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Focused C58 coverage for the user-authored glass-Corner UV contract. */
public final class GlassCornerUvResourceGameTests implements CustomTestMethodInvoker {
    private static final Identifier CORNER = Identifier.parse(
            "cnm_terrain_slabs_compat:minecraft/test_corner");
    private static final Identifier COLUMN = Identifier.parse(
            "cnm_terrain_slabs_compat:minecraft/test_quarter_column");

    private static final String CANONICAL_SIGNATURE = String.join("\n", List.of(
            "0,0,0,16,16,7|north:0,0,16,16@0|east:9,0,16,16@0|west:0,0,7,16@0|up:0,0,16,7@0|down:0,7,16,0@0",
            "0,0,7,7,16,8|south:0,0,7,16@0|west:15,0,16,16@0|up:0,0,7,1@0|down:0,0,7,1@0",
            "9,0,7,16,16,8|east:8,0,9,16@0|up:9,7,16,8@0|down:9,7,16,8@0",
            "7,0,7,8,16,8|south:0,0,1,16@0|up:0,8,1,9@0|down:0,0,1,1@0",
            "9,0,8,16,16,16|east:0,0,8,16@0|south:9,0,16,16@0|up:9,8,16,16@0|down:9,16,16,8@0",
            "8,0,7,9,16,8|up:8,0,9,1@0|down:8,0,9,1@0",
            "8,0,9,9,16,16|south:0,0,1,16@0|west:9,0,16,16@0|up:0,9,1,16@0|down:15,9,16,16@0",
            "8,0,8,9,16,9|west:0,0,1,16@0|up:8,0,9,1@0|down:8,15,9,16@0"));

    private static final String CLOCKWISE_BOUNDS_SIGNATURE = String.join("\n", List.of(
            "9,0,0,16,16,16",
            "8,0,0,9,16,7",
            "8,0,9,9,16,16",
            "8,0,7,9,16,8",
            "0,0,9,8,16,16",
            "8,0,8,9,16,9",
            "0,0,8,7,16,9",
            "7,0,8,8,16,9"));

    @GameTest(maxTicks = 40)
    public void authoredNorthEastContractAndRotationGroupAreExact(GameTestHelper helper) {
        List<AuthoredElement> canonical = AuthoredGlassCornerModel.canonicalElements();
        helper.assertTrue(contractSignature(canonical).equals(CANONICAL_SIGNATURE),
                "Production NORTH_EAST glass-Corner contract drifted from the sanitized fixture");
        helper.assertTrue(canonical.size() == 8 && faceCount(canonical) == 28,
                "Authored glass-Corner element/face count changed");

        List<AuthoredElement> rotated = canonical;
        for (int turn = 0; turn < 4; turn++) {
            rotated = AuthoredGlassCornerModel.rotateClockwise(rotated);
        }
        helper.assertTrue(rotated.equals(canonical),
                "Four clockwise turns did not return the exact authored model contract");

        for (Orientation orientation : Orientation.values()) {
            List<AuthoredElement> physical = AuthoredGlassCornerModel.elementsFor(orientation);
            Set<String> actual = occupied(physical, true);
            Set<String> expected = occupied(CornerColumnModelProjection.cornerBounds(orientation));
            helper.assertTrue(actual.size() == 3072 && actual.equals(expected),
                    "Authored glass-Corner footprint changed for " + orientation
                            + ": actual=" + actual.size() + ", expected=" + expected.size());
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void uvFramesAndCullfacesRotateWithoutMirroring(GameTestHelper helper) {
        List<AuthoredElement> canonical = AuthoredGlassCornerModel.canonicalElements();
        List<AuthoredElement> clockwise = AuthoredGlassCornerModel.rotateClockwise(canonical);
        helper.assertTrue(boundsSignature(clockwise).equals(CLOCKWISE_BOUNDS_SIGNATURE),
                "Clockwise projection changed exact per-element bounds or ordering");
        for (int elementIndex = 0; elementIndex < canonical.size(); elementIndex++) {
            AuthoredElement source = canonical.get(elementIndex);
            AuthoredElement target = clockwise.get(elementIndex);
            helper.assertTrue(source.faces().size() == target.faces().size(),
                    "Clockwise rotation changed visible-face count on element " + elementIndex);
            for (int faceIndex = 0; faceIndex < source.faces().size(); faceIndex++) {
                AuthoredFace before = source.faces().get(faceIndex);
                AuthoredFace after = target.faces().get(faceIndex);
                Direction expectedDirection = clockwise(before.direction());
                int expectedRotation = switch (before.direction()) {
                    case UP -> 90;
                    case DOWN -> 270;
                    default -> 0;
                };
                helper.assertTrue(after.direction() == expectedDirection
                                && after.uv().equals(before.uv())
                                && after.rotation() == expectedRotation,
                        "Clockwise face/UV transform changed or mirrored element " + elementIndex
                                + " face " + before.direction());
                for (int newVertex = 0; newVertex < 4; newVertex++) {
                    int oldVertex = switch (before.direction()) {
                        case UP -> (newVertex + 1) % 4;
                        case DOWN -> (newVertex + 3) % 4;
                        default -> newVertex;
                    };
                    helper.assertTrue(Arrays.equals(sample(before, oldVertex), sample(after, newVertex)),
                            "Asymmetric UV vertex identity was mirrored or world-locked for "
                                    + before.direction() + " vertex " + newVertex);
                }
            }
        }

        NibaruMaterialProfile glass = profile("minecraft:glass");
        for (Orientation orientation : Orientation.values()) {
            List<AuthoredElement> contract = AuthoredGlassCornerModel.elementsFor(orientation);
            JsonObject model = AuthoredGlassCornerModel.worldModel(glass, orientation);
            JsonArray encodedElements = model.getAsJsonArray("elements");
            helper.assertTrue(encodedElements.size() == contract.size(),
                    "Serialized authored element count changed for " + orientation);
            helper.assertTrue(serializedContract(model).equals(contractSignature(contract)),
                    "Serialized bounds, face directions, UVs, rotations, or face set changed for "
                            + orientation);
            for (int elementIndex = 0; elementIndex < contract.size(); elementIndex++) {
                AuthoredElement element = contract.get(elementIndex);
                JsonObject encodedFaces = encodedElements.get(elementIndex).getAsJsonObject()
                        .getAsJsonObject("faces");
                for (AuthoredFace face : element.faces()) {
                    JsonObject encoded = encodedFaces.getAsJsonObject(
                            face.direction().getSerializedName());
                    boolean boundary = element.bounds().onBoundary(face.direction());
                    helper.assertTrue(encoded != null
                                    && encoded.get("texture").getAsString().equals("#side")
                                    && rotation(encoded) == face.rotation(),
                            "Serialized face contract changed for " + orientation + " "
                                    + face.direction());
                    helper.assertTrue(boundary
                                    ? encoded.has("cullface") && encoded.get("cullface").getAsString()
                                            .equals(face.direction().getSerializedName())
                                    : !encoded.has("cullface"),
                            "Cullface is missing, stale, or attached to an internal notch face for "
                                    + orientation + " " + face.direction());
                }
            }
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void clearStainedAndItemModelsUseOneAuthoredContract(GameTestHelper helper) {
        List<NibaruMaterialProfile> profiles = List.of(
                profile("minecraft:glass"),
                profile("minecraft:red_stained_glass"),
                profile("minecraft:blue_stained_glass"));
        List<String> textures = List.of(
                "minecraft:block/glass",
                "minecraft:block/red_stained_glass",
                "minecraft:block/blue_stained_glass");
        JsonArray referenceElements = null;
        for (int index = 0; index < profiles.size(); index++) {
            CornerColumnModelProjection.Projection projection =
                    CornerColumnModelProjection.projectCorner(profiles.get(index), CORNER);
            JsonObject northEast = selected(projection, "facing=east");
            helper.assertTrue(northEast.getAsJsonObject("textures").get("side").getAsString()
                            .equals(textures.get(index))
                            && northEast.get("render_type").getAsString().equals("translucent"),
                    "Glass Corner lost profile-driven texture/render metadata for "
                            + profiles.get(index).canonicalParentId());
            if (referenceElements == null) referenceElements = northEast.getAsJsonArray("elements");
            else helper.assertTrue(referenceElements.equals(northEast.getAsJsonArray("elements")),
                    "Stained glass changed authored Corner geometry or UV topology");

            JsonObject item = projection.models().get(projection.itemModel());
            helper.assertTrue(item.getAsJsonObject("textures").get("side").getAsString()
                            .equals(textures.get(index))
                            && item.get("render_type").getAsString().equals("translucent")
                            && item.getAsJsonObject("display")
                            .equals(CuboidListModelProjection.itemDisplay())
                            && serializedContract(item).equals(contractSignature(
                                    AuthoredGlassCornerModel.elementsFor(Orientation.SOUTH_WEST)))
                            && !hasCullface(item),
                    "Glass Corner item lost authored SOUTH_WEST topology, display, or cullface policy");
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void nonGlassCornerAndGlassColumnsRemainC57Exact(GameTestHelper helper) {
        NibaruMaterialProfile stone = profile("minecraft:stone");
        CornerColumnModelProjection.Projection stoneCorner =
                CornerColumnModelProjection.projectCorner(stone, CORNER);
        for (Orientation orientation : Orientation.values()) {
            JsonObject expected = CuboidListModelProjection.worldModel(stone,
                    CornerColumnModelProjection.cornerBounds(orientation).stream()
                            .map(Cuboid::world).toList(), null, false);
            helper.assertTrue(selected(stoneCorner, "facing="
                            + orientation.stateFacing().getSerializedName()).equals(expected),
                    "Representative non-glass Corner changed from C57 for " + orientation);
        }
        JsonObject expectedStoneItem = CuboidListModelProjection.itemModel(stone,
                CornerColumnModelProjection.cornerBounds(Orientation.SOUTH_WEST).stream()
                        .map(Cuboid::world).toList(), null, false);
        helper.assertTrue(stoneCorner.models().get(stoneCorner.itemModel()).equals(expectedStoneItem),
                "Representative non-glass Corner item changed from C57");

        for (NibaruMaterialProfile glass : List.of(
                profile("minecraft:glass"), profile("minecraft:green_stained_glass"))) {
            CornerColumnModelProjection.Projection columns =
                    CornerColumnModelProjection.projectColumn(glass, COLUMN);
            for (ColumnOccupancy occupancy : ColumnOccupancy.values()) {
                JsonObject expected = CuboidListModelProjection.worldModel(glass,
                        occupancy.bounds().stream().map(Cuboid::world).toList(), null, false);
                helper.assertTrue(selected(columns, "occupancy=" + occupancy.serializedName())
                                .equals(expected),
                        "Glass Quarter Column changed from C57 for "
                                + glass.canonicalParentId() + " " + occupancy);
            }
            Cuboid itemCuboid = Cuboid.world(ColumnOccupancy.SE.bounds().getFirst());
            helper.assertTrue(columns.models().get(columns.itemModel()).equals(
                            CuboidListModelProjection.itemModel(glass, itemCuboid, null, false)),
                    "Glass Quarter Column item changed from C57 for "
                            + glass.canonicalParentId());
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void glassCornerSelectorsAndModelIdsRemainExact(GameTestHelper helper) {
        CornerColumnModelProjection.Projection projection =
                CornerColumnModelProjection.projectCorner(profile("minecraft:glass"), CORNER);
        List<String> selectors = new ArrayList<>(projection.blockState()
                .getAsJsonObject("variants").keySet());
        List<String> expectedSelectors = List.of(
                "facing=west", "facing=north", "facing=east", "facing=south");
        List<String> expectedModels = List.of(
                "cnm_terrain_slabs_compat:block/minecraft/test_corner_south_west",
                "cnm_terrain_slabs_compat:block/minecraft/test_corner_north_west",
                "cnm_terrain_slabs_compat:block/minecraft/test_corner_north_east",
                "cnm_terrain_slabs_compat:block/minecraft/test_corner_south_east",
                "cnm_terrain_slabs_compat:block/minecraft/test_corner_item");
        helper.assertTrue(selectors.equals(expectedSelectors)
                        && new ArrayList<>(projection.models().keySet()).equals(expectedModels)
                        && projection.itemModel().equals(expectedModels.getLast()),
                "Glass Corner selector keys, model IDs, order, or item identity changed");
        helper.succeed();
    }

    private static String contractSignature(List<AuthoredElement> elements) {
        List<String> lines = new ArrayList<>(elements.size());
        for (AuthoredElement element : elements) {
            Bounds bounds = element.bounds();
            StringBuilder line = new StringBuilder()
                    .append(number(bounds.x0())).append(',').append(number(bounds.y0())).append(',')
                    .append(number(bounds.z0())).append(',').append(number(bounds.x1())).append(',')
                    .append(number(bounds.y1())).append(',').append(number(bounds.z1()));
            for (AuthoredFace face : element.faces()) {
                line.append('|').append(face.direction().getSerializedName()).append(':')
                        .append(face.uv().u0()).append(',').append(face.uv().v0()).append(',')
                        .append(face.uv().u1()).append(',').append(face.uv().v1()).append('@')
                        .append(face.rotation());
            }
            lines.add(line.toString());
        }
        return String.join("\n", lines);
    }

    private static String boundsSignature(List<AuthoredElement> elements) {
        return elements.stream().map(element -> {
            Bounds bounds = element.bounds();
            return String.join(",", List.of(
                    number(bounds.x0()), number(bounds.y0()), number(bounds.z0()),
                    number(bounds.x1()), number(bounds.y1()), number(bounds.z1())));
        }).collect(java.util.stream.Collectors.joining("\n"));
    }

    private static String serializedContract(JsonObject model) {
        List<String> lines = new ArrayList<>();
        for (var entry : model.getAsJsonArray("elements")) {
            JsonObject element = entry.getAsJsonObject();
            JsonArray from = element.getAsJsonArray("from");
            JsonArray to = element.getAsJsonArray("to");
            StringBuilder line = new StringBuilder()
                    .append(number(from.get(0).getAsDouble())).append(',')
                    .append(number(from.get(1).getAsDouble())).append(',')
                    .append(number(from.get(2).getAsDouble())).append(',')
                    .append(number(to.get(0).getAsDouble())).append(',')
                    .append(number(to.get(1).getAsDouble())).append(',')
                    .append(number(to.get(2).getAsDouble()));
            for (var faceEntry : element.getAsJsonObject("faces").entrySet()) {
                JsonObject face = faceEntry.getValue().getAsJsonObject();
                JsonArray uv = face.getAsJsonArray("uv");
                line.append('|').append(faceEntry.getKey()).append(':')
                        .append(number(uv.get(0).getAsDouble())).append(',')
                        .append(number(uv.get(1).getAsDouble())).append(',')
                        .append(number(uv.get(2).getAsDouble())).append(',')
                        .append(number(uv.get(3).getAsDouble())).append('@')
                        .append(rotation(face));
            }
            lines.add(line.toString());
        }
        return String.join("\n", lines);
    }

    private static Set<String> occupied(List<AuthoredElement> elements, boolean requireDisjoint) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (AuthoredElement element : elements) addOccupied(result, element.bounds(), requireDisjoint);
        return result;
    }

    private static Set<String> occupied(List<Bounds> bounds) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (Bounds member : bounds) addOccupied(result, member, false);
        return result;
    }

    private static void addOccupied(Set<String> occupied, Bounds bounds, boolean requireDisjoint) {
        for (int x = (int) bounds.x0(); x < (int) bounds.x1(); x++) {
            for (int y = (int) bounds.y0(); y < (int) bounds.y1(); y++) {
                for (int z = (int) bounds.z0(); z < (int) bounds.z1(); z++) {
                    boolean added = occupied.add(x + "," + y + "," + z);
                    if (requireDisjoint && !added) {
                        throw new IllegalStateException("Authored elements overlap at "
                                + x + "," + y + "," + z);
                    }
                }
            }
        }
    }

    private static int faceCount(List<AuthoredElement> elements) {
        return elements.stream().mapToInt(element -> element.faces().size()).sum();
    }

    private static int[] sample(AuthoredFace face, int vertex) {
        int index = (vertex + face.rotation() / 90) % 4;
        return switch (index) {
            case 0 -> new int[]{face.uv().u0(), face.uv().v0()};
            case 1 -> new int[]{face.uv().u0(), face.uv().v1()};
            case 2 -> new int[]{face.uv().u1(), face.uv().v1()};
            case 3 -> new int[]{face.uv().u1(), face.uv().v0()};
            default -> throw new AssertionError(index);
        };
    }

    private static Direction clockwise(Direction direction) {
        return switch (direction) {
            case NORTH -> Direction.EAST;
            case EAST -> Direction.SOUTH;
            case SOUTH -> Direction.WEST;
            case WEST -> Direction.NORTH;
            case UP -> Direction.UP;
            case DOWN -> Direction.DOWN;
        };
    }

    private static JsonObject selected(
            CornerColumnModelProjection.Projection projection, String key) {
        String modelId = projection.blockState().getAsJsonObject("variants")
                .getAsJsonObject(key).get("model").getAsString();
        JsonObject result = projection.models().get(modelId);
        if (result == null) throw new IllegalStateException("Selector did not resolve: " + key);
        return result;
    }

    private static boolean hasCullface(JsonObject model) {
        for (var element : model.getAsJsonArray("elements")) {
            for (var face : element.getAsJsonObject().getAsJsonObject("faces").entrySet()) {
                if (face.getValue().getAsJsonObject().has("cullface")) return true;
            }
        }
        return false;
    }

    private static int rotation(JsonObject face) {
        return face.has("rotation") ? face.get("rotation").getAsInt() : 0;
    }

    private static String number(double value) {
        return value == Math.rint(value) ? Integer.toString((int) value) : Double.toString(value);
    }

    private static NibaruMaterialProfile profile(String id) {
        return NibaruMaterialProfiles.fromId(Identifier.parse(id)).orElseThrow();
    }

    @Override
    public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
        method.invoke(this, helper);
    }
}
