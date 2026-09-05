package dev.resivore.ribbitsxaeroicons;

import com.geckolib.cache.model.cuboid.GeoCube;
import java.util.ArrayList;
import java.util.List;

/** C9 Wandering body also owns clothes/backpack; only the core and paired eyes are face geometry. */
final class WanderingRibbitHeadSelector {
    private WanderingRibbitHeadSelector() {}

    static GeoCube[] select(GeoCube[] body) {
        if (body == null || body.length != 15) throw new IllegalArgumentException("unknown Wandering body layout");
        // Semantic baked bounding boxes, not array positions or UVs. Gecko mirrors authored X
        // and divides coordinates by 16. Require exactly one of each; drift fails closed.
        double[][] boxes = {{-4, 2, -4, 4, 10, 3}, {1, 8, -2, 5, 12, 1}, {-5, 8, -2, -1, 12, 1}};
        List<GeoCube> selected = new ArrayList<>();
        for (double[] box : boxes) {
            GeoCube match = null;
            for (GeoCube cube : body) {
                if (matches(cube, box)) {
                    if (match != null) throw new IllegalArgumentException("ambiguous Wandering face cube");
                    match = cube;
                }
            }
            if (match == null) throw new IllegalArgumentException("Wandering face geometry changed");
            selected.add(match);
        }
        return selected.toArray(GeoCube[]::new);
    }

    private static boolean matches(GeoCube cube, double[] box) {
        if (cube == null || cube.quads() == null || cube.rotation() == null
                || cube.rotation().lengthSqr() != 0) return false;
        double[] bounds = {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY};
        for (var quad : cube.quads()) {
            if (quad == null) continue;
            for (var vertex : quad.vertices()) {
                double[] xyz = {vertex.posX(), vertex.posY(), vertex.posZ()};
                for (int axis = 0; axis < 3; axis++) {
                    bounds[axis] = Math.min(bounds[axis], xyz[axis]);
                    bounds[axis + 3] = Math.max(bounds[axis + 3], xyz[axis]);
                }
            }
        }
        for (int i = 0; i < 6; i++)
            if (!Double.isFinite(bounds[i]) || Math.abs(bounds[i] - box[i] / 16.0) > 1e-6) return false;
        return true;
    }
}
