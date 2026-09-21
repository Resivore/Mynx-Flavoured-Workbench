package dev.aero.cnmterraincompat.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.aero.cnmterraincompat.AxisModelContract;
import dev.aero.cnmterraincompat.AxisModelContract.HalfSpacePair;
import dev.aero.cnmterraincompat.PrivateBeamFamilies;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;

import java.util.Objects;
import java.util.Set;

/** Item-only selection of the real default-axis Beam models with C92's display contract. */
public final class BeamItemModelContract {
    public static final String MODEL_SUFFIX = "_beam_inventory";
    /** BBB's authored 16-wide Beam side frame carries its decorative center band at this U. */
    public static final int AUTHORITATIVE_SIDE_BAND_U = 7;

    private static final Set<Identifier> BBB_BEAMS = Set.of(
            bbb("oak"), bbb("spruce"), bbb("birch"), bbb("jungle"), bbb("acacia"),
            bbb("dark_oak"), bbb("crimson"), bbb("warped"), bbb("mangrove"),
            bbb("bamboo"), bbb("cherry"), bbb("pale_oak"));

    private BeamItemModelContract() {}

    public static boolean applies(Identifier canonicalParent) {
        Objects.requireNonNull(canonicalParent, "canonicalParent");
        return BBB_BEAMS.contains(canonicalParent) || PrivateBeamFamilies.isPrivateBeam(canonicalParent);
    }

    /** Selects the unchanged placed axis=Y, facing=north Vertical Slab model. */
    public static JsonObject verticalSlab(Identifier shape) {
        return itemModel(AxisModelContract.verticalDirectHalfModelId(
                shape, Direction.Axis.Y, Direction.NORTH));
    }

    /** Selects the unchanged placed axis=Y, facing=north, bottom Step model. */
    public static JsonObject step(Identifier shape) {
        return itemModel(AxisModelContract.stepDirectPairModelId(shape, Direction.Axis.Y,
                new HalfSpacePair(Direction.NORTH, Direction.DOWN)));
    }

    private static JsonObject itemModel(String parent) {
        JsonObject model = new JsonObject();
        model.addProperty("parent", parent);
        model.add("display", c92Display());
        return model;
    }

    /** Exact display members inherited by C92's CNM Vertical Slab and Step item models. */
    private static JsonObject c92Display() {
        JsonObject display = new JsonObject();
        display.add("firstperson_righthand", transform(
                new double[] {0, -45, 0}, null, new double[] {.4, .4, .4}));
        display.add("firstperson_lefthand", transform(
                new double[] {0, -45, 0}, null, new double[] {.4, .4, .4}));
        display.add("gui", transform(new double[] {30, -135, 0},
                new double[] {-1.75, 0, 0}, new double[] {.625, .625, .625}));
        return display;
    }

    private static JsonObject transform(double[] rotation, double[] translation, double[] scale) {
        JsonObject transform = new JsonObject();
        if (rotation != null) transform.add("rotation", numbers(rotation));
        if (translation != null) transform.add("translation", numbers(translation));
        if (scale != null) transform.add("scale", numbers(scale));
        return transform;
    }

    private static JsonArray numbers(double... values) {
        JsonArray result = new JsonArray();
        for (double value : values) {
            if (value == Math.rint(value) && value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE) {
                result.add((int) value);
            } else {
                result.add(value);
            }
        }
        return result;
    }

    private static Identifier bbb(String material) {
        return Identifier.fromNamespaceAndPath("bbb", material + "_beam");
    }
}
