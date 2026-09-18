package dev.resivore.bgectm.continuity;

import dev.resivore.bgectm.SurfaceContactResolver.QuadSurface;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

/** Exact non-overlapping emission cells derived from unmerged overlay contributions. */
public final class OverlayFootprintPlan {
    private OverlayFootprintPlan() {}

    /**
     * Partitions contributor rectangles at every authoritative edge. This preserves distinct
     * contributor bounds, avoids alpha overdraw where contributions overlap, and never expands
     * their union to a coarse bounding rectangle.
     */
    public static List<QuadSurface> partition(List<QuadSurface> contributions) {
        Objects.requireNonNull(contributions, "contributions");
        if (contributions.isEmpty()) return List.of();
        QuadSurface reference = Objects.requireNonNull(contributions.getFirst(), "contribution");
        TreeSet<Integer> uEdges = new TreeSet<>();
        TreeSet<Integer> vEdges = new TreeSet<>();
        for (QuadSurface contribution : contributions) {
            Objects.requireNonNull(contribution, "contribution");
            if (contribution.normal() != reference.normal()
                    || contribution.plane16() != reference.plane16()
                    || contribution.uAxis() != reference.uAxis()
                    || contribution.vAxis() != reference.vAxis()) {
                throw new IllegalArgumentException("Overlay contributions do not share a surface");
            }
            uEdges.add(contribution.uMin16());
            uEdges.add(contribution.uMax16());
            vEdges.add(contribution.vMin16());
            vEdges.add(contribution.vMax16());
        }
        List<Integer> u = List.copyOf(uEdges);
        List<Integer> v = List.copyOf(vEdges);
        List<QuadSurface> regions = new ArrayList<>();
        for (int uIndex = 0; uIndex + 1 < u.size(); uIndex++) {
            int uMin = u.get(uIndex);
            int uMax = u.get(uIndex + 1);
            for (int vIndex = 0; vIndex + 1 < v.size(); vIndex++) {
                int vMin = v.get(vIndex);
                int vMax = v.get(vIndex + 1);
                if (!covered(contributions, uMin, uMax, vMin, vMax)) continue;
                regions.add(new QuadSurface(reference.normal(), reference.plane16(),
                        reference.uAxis(), uMin, uMax, reference.vAxis(), vMin, vMax));
            }
        }
        return List.copyOf(regions);
    }

    public static QuadSurface onPlane(QuadSurface footprint, int plane16) {
        Objects.requireNonNull(footprint, "footprint");
        return new QuadSurface(footprint.normal(), plane16, footprint.uAxis(),
                footprint.uMin16(), footprint.uMax16(), footprint.vAxis(),
                footprint.vMin16(), footprint.vMax16());
    }

    private static boolean covered(List<QuadSurface> contributions, int uMin, int uMax,
            int vMin, int vMax) {
        for (QuadSurface contribution : contributions) {
            if (contribution.uMin16() <= uMin && contribution.uMax16() >= uMax
                    && contribution.vMin16() <= vMin && contribution.vMax16() >= vMax) {
                return true;
            }
        }
        return false;
    }
}
