package dev.aero.cnmterraincompat;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.tazer.clutternomore.common.blocks.StepBlock;
import dev.tazer.clutternomore.common.blocks.VerticalSlabBlock;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.properties.SlabType;

/** Pure ordinary-model composition of provider inset visuals over CNM-owned geometry bounds. */
public final class InsetModelContract {
    private static final Direction[] HORIZONTAL = {
            Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST
    };

    private InsetModelContract() {}

    public static Direction[] horizontalDirections() { return HORIZONTAL.clone(); }

    public static JsonObject verticalModel(NibaruMaterialProfile profile, Direction facing, boolean doubled) {
        JsonObject model = model(profile, !doubled || facing == Direction.NORTH);
        if (doubled) addMaterialSegment(model, profile, new int[]{0, 0, 0, 16, 16, 16}, true);
        else addMaterialSegment(model, profile, halfBounds(facing, 0, 16), false);
        return model;
    }

    public static JsonObject stepModel(NibaruMaterialProfile profile, Direction facing, SlabType type) {
        JsonObject model = model(profile, facing == Direction.NORTH && type == SlabType.BOTTOM);
        if (type == SlabType.BOTTOM) addMaterialSegment(model, profile, halfBounds(facing, 0, 8), false);
        else if (type == SlabType.TOP) addMaterialSegment(model, profile, halfBounds(facing, 8, 16), false);
        else {
            addMaterialSegment(model, profile, halfBounds(facing, 8, 16), false);
            addMaterialSegment(model, profile, halfBounds(facing.getOpposite(), 0, 8), false);
        }
        return model;
    }

    public static JsonObject verticalBlockState(Identifier shape) {
        JsonObject variants = new JsonObject();
        for (Direction facing : HORIZONTAL) for (boolean doubled : new boolean[]{false, true}) {
            JsonObject apply = new JsonObject();
            apply.addProperty("model", modelId(shape, doubled ? "_inset_double" : suffix(facing, SlabType.BOTTOM)));
            variants.add(VerticalSlabBlock.FACING.getName() + "=" + facing.getSerializedName() + ","
                    + VerticalSlabBlock.DOUBLE.getName() + "=" + doubled, apply);
        }
        JsonObject result = new JsonObject();
        result.add("variants", variants);
        return result;
    }

    public static JsonObject stepBlockState(Identifier shape) {
        JsonObject variants = new JsonObject();
        for (Direction facing : HORIZONTAL) for (SlabType type : SlabType.values()) {
            JsonObject apply = new JsonObject();
            apply.addProperty("model", modelId(shape, suffix(facing, type)));
            variants.add(StepBlock.FACING.getName() + "=" + facing.getSerializedName() + ","
                    + StepBlock.SLAB_TYPE.getName() + "=" + type.getSerializedName(), apply);
        }
        JsonObject result = new JsonObject();
        result.add("variants", variants);
        return result;
    }

    public static String suffix(Direction facing, SlabType type) {
        return "_inset_" + facing.getSerializedName() + switch (type) {
            case BOTTOM -> "";
            case TOP -> "_top";
            case DOUBLE -> "_double";
        };
    }

    private static JsonObject model(NibaruMaterialProfile profile, boolean itemDisplay) {
        if (profile.insetVisualContract().isEmpty())
            throw new IllegalArgumentException("Missing inset visual contract: " + profile.canonicalParentId());
        JsonObject result = new JsonObject();
        result.addProperty("parent", "minecraft:block/block");
        result.addProperty("render_type", "translucent");
        JsonObject textures = new JsonObject();
        textures.addProperty("side", texture(profile.textureRoles().side()));
        textures.addProperty("top", texture(profile.textureRoles().top()));
        textures.addProperty("bottom", texture(profile.textureRoles().bottom()));
        textures.addProperty("particle", texture(profile.textureRoles().particle()));
        result.add("textures", textures);
        result.add("elements", new JsonArray());
        if (itemDisplay) result.add("display", itemDisplay());
        return result;
    }

    private static void addMaterialSegment(JsonObject model, NibaruMaterialProfile profile, int[] outer,
            boolean fullCube) {
        var contract = profile.insetVisualContract().orElseThrow();
        boolean bottomShell = contract.shellTexture()
                == NibaruMaterialProfile.InsetVisualContract.ShellTexture.BOTTOM;
        addBox(model, outer, bottomShell, true);
        if (fullCube && !contract.includeInnerLayerOnFullCube()) return;
        int[] inner = outer.clone();
        inner[0] += contract.insetForSpan(outer[3] - outer[0]);
        inner[1] += contract.insetForSpan(outer[4] - outer[1]);
        inner[2] += contract.insetForSpan(outer[5] - outer[2]);
        inner[3] -= contract.insetForSpan(outer[3] - outer[0]);
        inner[4] -= contract.insetForSpan(outer[4] - outer[1]);
        inner[5] -= contract.insetForSpan(outer[5] - outer[2]);
        addBox(model, inner, false, false);
    }

    private static void addBox(JsonObject model, int[] bounds, boolean bottomOnly, boolean cullBoundary) {
        JsonObject element = new JsonObject();
        element.add("from", numbers(bounds[0], bounds[1], bounds[2]));
        element.add("to", numbers(bounds[3], bounds[4], bounds[5]));
        JsonObject faces = new JsonObject();
        addFace(faces, "north", bottomOnly ? "bottom" : "side", cullBoundary && bounds[2] == 0);
        addFace(faces, "east", bottomOnly ? "bottom" : "side", cullBoundary && bounds[3] == 16);
        addFace(faces, "south", bottomOnly ? "bottom" : "side", cullBoundary && bounds[5] == 16);
        addFace(faces, "west", bottomOnly ? "bottom" : "side", cullBoundary && bounds[0] == 0);
        addFace(faces, "up", bottomOnly ? "bottom" : "top", cullBoundary && bounds[4] == 16);
        addFace(faces, "down", "bottom", cullBoundary && bounds[1] == 0);
        element.add("faces", faces);
        model.getAsJsonArray("elements").add(element);
    }

    private static void addFace(JsonObject faces, String direction, String texture, boolean cull) {
        JsonObject face = new JsonObject();
        face.addProperty("texture", "#" + texture);
        if (cull) face.addProperty("cullface", direction);
        faces.add(direction, face);
    }

    private static int[] halfBounds(Direction direction, int minY, int maxY) {
        return switch (direction) {
            case NORTH -> new int[]{0, minY, 0, 16, maxY, 8};
            case EAST -> new int[]{8, minY, 0, 16, maxY, 16};
            case SOUTH -> new int[]{0, minY, 8, 16, maxY, 16};
            case WEST -> new int[]{0, minY, 0, 8, maxY, 16};
            default -> throw new IllegalArgumentException("Inset geometry must face horizontally");
        };
    }

    private static String modelId(Identifier shape, String suffix) {
        return "clutternomore:block/" + shape.getPath() + suffix;
    }

    private static String texture(String path) {
        return path.contains(":") ? path : "minecraft:block/" + path;
    }

    private static JsonObject itemDisplay() {
        JsonObject display = new JsonObject();
        JsonObject firstPerson = transform(new int[]{0, -45, 0}, null, new double[]{0.4, 0.4, 0.4});
        display.add("firstperson_righthand", firstPerson);
        display.add("firstperson_lefthand", firstPerson.deepCopy());
        display.add("gui", transform(new int[]{30, -135, 0}, new double[]{-1.75, 0, 0},
                new double[]{0.625, 0.625, 0.625}));
        display.add("fixed", transform(new int[]{0, 90, 0}, null, new double[]{0.5, 0.5, 0.5}));
        return display;
    }

    private static JsonObject transform(int[] rotation, double[] translation, double[] scale) {
        JsonObject result = new JsonObject();
        result.add("rotation", numbers(rotation[0], rotation[1], rotation[2]));
        if (translation != null) result.add("translation", numbers(translation[0], translation[1], translation[2]));
        result.add("scale", numbers(scale[0], scale[1], scale[2]));
        return result;
    }

    private static JsonArray numbers(Number... values) {
        JsonArray result = new JsonArray();
        for (Number value : values) result.add(value);
        return result;
    }
}
