package dev.aero.cnmterraincompat;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile;
import dev.tazer.clutternomore.common.blocks.StepBlock;
import dev.tazer.clutternomore.common.blocks.VerticalSlabBlock;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.properties.SlabType;

/** Pure normal-model JSON composition for independent CNM geometry and glazed-pattern directions. */
public final class GlazedModelContract {
    private static final Direction[] HORIZONTAL = {
            Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST
    };

    private GlazedModelContract() {}

    public static Direction[] horizontalDirections() {
        return HORIZONTAL.clone();
    }

    public static JsonObject verticalModel(NibaruMaterialProfile profile, Direction relativePhysical) {
        JsonObject model = model(profile, true);
        int[] bounds = halfBounds(relativePhysical);
        addBox(model, bounds[0], 0, bounds[1], bounds[2], 16, bounds[3]);
        return model;
    }

    public static JsonObject verticalDoubleModel(NibaruMaterialProfile profile) {
        JsonObject model = model(profile, false);
        addBox(model, 0, 0, 0, 16, 16, 16);
        return model;
    }

    public static JsonObject stepModel(NibaruMaterialProfile profile, Direction relativePhysical, SlabType type) {
        JsonObject model = model(profile, type == SlabType.BOTTOM);
        int[] lower = halfBounds(relativePhysical);
        if (type == SlabType.BOTTOM) {
            addBox(model, lower[0], 0, lower[1], lower[2], 8, lower[3]);
        } else if (type == SlabType.TOP) {
            addBox(model, lower[0], 8, lower[1], lower[2], 16, lower[3]);
        } else {
            int[] upper = halfBounds(relativePhysical);
            int[] opposite = halfBounds(relativePhysical.getOpposite());
            addBox(model, upper[0], 8, upper[1], upper[2], 16, upper[3]);
            addBox(model, opposite[0], 0, opposite[1], opposite[2], 8, opposite[3]);
        }
        return model;
    }

    public static JsonObject verticalBlockState(Identifier shape) {
        JsonObject variants = new JsonObject();
        for (Direction physical : HORIZONTAL) for (Direction pattern : HORIZONTAL) for (boolean doubled : new boolean[]{false, true}) {
            JsonObject apply = apply(pattern);
            if (doubled) {
                apply.addProperty("model", modelId(shape, "_glazed_double"));
            } else {
                Direction relative = GlazedPatternState.relativePhysical(physical, pattern);
                apply.addProperty("model", modelId(shape, relativeSuffix(relative, SlabType.BOTTOM)));
            }
            variants.add(verticalVariantKey(physical, pattern, doubled), apply);
        }
        JsonObject result = new JsonObject();
        result.add("variants", variants);
        return result;
    }

    public static JsonObject stepBlockState(Identifier shape) {
        JsonObject variants = new JsonObject();
        for (Direction physical : HORIZONTAL) for (Direction pattern : HORIZONTAL) for (SlabType type : SlabType.values()) {
            Direction relative = GlazedPatternState.relativePhysical(physical, pattern);
            JsonObject apply = apply(pattern);
            apply.addProperty("model", modelId(shape, relativeSuffix(relative, type)));
            variants.add(stepVariantKey(physical, pattern, type), apply);
        }
        JsonObject result = new JsonObject();
        result.add("variants", variants);
        return result;
    }

    public static String relativeSuffix(Direction relative, SlabType type) {
        String form = switch (type) {
            case BOTTOM -> "";
            case TOP -> "_top";
            case DOUBLE -> "_double";
        };
        return "_glazed_relative_" + relative.getSerializedName() + form;
    }

    public static String verticalVariantKey(Direction physical, Direction pattern, boolean doubled) {
        return VerticalSlabBlock.FACING.getName() + "=" + physical.getSerializedName() + ","
                + GlazedPatternState.PATTERN_FACING.getName() + "=" + pattern.getSerializedName() + ","
                + VerticalSlabBlock.DOUBLE.getName() + "=" + doubled;
    }

    public static String stepVariantKey(Direction physical, Direction pattern, SlabType type) {
        return StepBlock.FACING.getName() + "=" + physical.getSerializedName() + ","
                + GlazedPatternState.PATTERN_FACING.getName() + "=" + pattern.getSerializedName() + ","
                + StepBlock.SLAB_TYPE.getName() + "=" + type.getSerializedName();
    }

    private static JsonObject apply(Direction pattern) {
        JsonObject result = new JsonObject();
        int yaw = GlazedPatternState.patternYaw(pattern);
        if (yaw != 0) result.addProperty("y", yaw);
        result.addProperty("uvlock", false);
        return result;
    }

    private static String modelId(Identifier shape, String suffix) {
        return "clutternomore:block/" + shape.getPath() + suffix;
    }

    private static JsonObject model(NibaruMaterialProfile profile, boolean itemDisplay) {
        String side = profile.textureRoles().side();
        if (!side.equals(profile.textureRoles().top()) || !side.equals(profile.textureRoles().bottom())) {
            throw new IllegalArgumentException("Glazed model requires one canonical texture: "
                    + profile.canonicalParentId());
        }
        JsonObject model = new JsonObject();
        model.addProperty("parent", "minecraft:block/block");
        JsonObject textures = new JsonObject();
        textures.addProperty("all", texture(side));
        textures.addProperty("particle", "#all");
        model.add("textures", textures);
        model.add("elements", new JsonArray());
        if (itemDisplay) model.add("display", itemDisplay());
        return model;
    }

    private static String texture(String path) {
        return path.contains(":") ? path : "minecraft:block/" + path;
    }

    private static int[] halfBounds(Direction direction) {
        return switch (direction) {
            case NORTH -> new int[]{0, 0, 16, 8};
            case EAST -> new int[]{8, 0, 16, 16};
            case SOUTH -> new int[]{0, 8, 16, 16};
            case WEST -> new int[]{0, 0, 8, 16};
            default -> throw new IllegalArgumentException("Glazed geometry must be horizontal: " + direction);
        };
    }

    private static void addBox(JsonObject model, int x0, int y0, int z0, int x1, int y1, int z1) {
        JsonObject element = new JsonObject();
        element.add("from", numbers(x0, y0, z0));
        element.add("to", numbers(x1, y1, z1));
        JsonObject faces = new JsonObject();
        addFace(faces, "north", z0 == 0);
        addFace(faces, "east", x1 == 16);
        addFace(faces, "south", z1 == 16);
        addFace(faces, "west", x0 == 0);
        addFace(faces, "up", y1 == 16);
        addFace(faces, "down", y0 == 0);
        element.add("faces", faces);
        model.getAsJsonArray("elements").add(element);
    }

    private static void addFace(JsonObject faces, String direction, boolean boundary) {
        JsonObject face = new JsonObject();
        face.addProperty("texture", "#all");
        if (boundary) face.addProperty("cullface", direction);
        faces.add(direction, face);
    }

    private static JsonArray numbers(Number... values) {
        JsonArray result = new JsonArray();
        for (Number value : values) result.add(value);
        return result;
    }

    private static JsonObject itemDisplay() {
        JsonObject display = new JsonObject();
        JsonObject firstPerson = transform(new int[]{0, -45, 0}, null, new double[]{0.4, 0.4, 0.4});
        display.add("firstperson_righthand", firstPerson);
        display.add("firstperson_lefthand", firstPerson.deepCopy());
        display.add("gui", transform(new int[]{30, -135, 0}, new double[]{-1.75, 0, 0},
                new double[]{0.625, 0.625, 0.625}));
        return display;
    }

    private static JsonObject transform(int[] rotation, double[] translation, double[] scale) {
        JsonObject result = new JsonObject();
        result.add("rotation", numbers(rotation[0], rotation[1], rotation[2]));
        if (translation != null) result.add("translation", numbers(
                translation[0], translation[1], translation[2]));
        result.add("scale", numbers(scale[0], scale[1], scale[2]));
        return result;
    }
}
