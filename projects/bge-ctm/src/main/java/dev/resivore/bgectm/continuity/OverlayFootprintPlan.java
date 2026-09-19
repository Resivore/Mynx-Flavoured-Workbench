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

    /**
     * Splits one receiver quad at every overlay edge. The returned cells cover the receiver
     * exactly once, so the base face and every overlay can reuse identical triangles wherever
     * they overlap instead of relying on numerically coplanar but differently sized quads.
     */
    public static List<QuadSurface> tessellateReceiver(QuadSurface receiver,
            List<QuadSurface> overlayRegions) {
        Objects.requireNonNull(receiver, "receiver");
        Objects.requireNonNull(overlayRegions, "overlayRegions");
        TreeSet<Integer> uEdges = new TreeSet<>(List.of(receiver.uMin16(), receiver.uMax16()));
        TreeSet<Integer> vEdges = new TreeSet<>(List.of(receiver.vMin16(), receiver.vMax16()));
        for (QuadSurface overlay : overlayRegions) {
            requireSamePlane(receiver, Objects.requireNonNull(overlay, "overlay"));
            int uMin = Math.max(receiver.uMin16(), overlay.uMin16());
            int uMax = Math.min(receiver.uMax16(), overlay.uMax16());
            int vMin = Math.max(receiver.vMin16(), overlay.vMin16());
            int vMax = Math.min(receiver.vMax16(), overlay.vMax16());
            if (uMin >= uMax || vMin >= vMax) continue;
            uEdges.add(uMin);
            uEdges.add(uMax);
            vEdges.add(vMin);
            vEdges.add(vMax);
        }
        return cells(receiver, List.copyOf(uEdges), List.copyOf(vEdges), false, List.of());
    }

    /** Shared overlay-only cells for a nominal presentation plane with no physical base face. */
    public static List<QuadSurface> tessellateUnion(List<QuadSurface> overlayRegions) {
        Objects.requireNonNull(overlayRegions, "overlayRegions");
        if (overlayRegions.isEmpty()) return List.of();
        QuadSurface reference = Objects.requireNonNull(overlayRegions.getFirst(), "overlay");
        TreeSet<Integer> uEdges = new TreeSet<>();
        TreeSet<Integer> vEdges = new TreeSet<>();
        for (QuadSurface overlay : overlayRegions) {
            requireSamePlane(reference, Objects.requireNonNull(overlay, "overlay"));
            uEdges.add(overlay.uMin16());
            uEdges.add(overlay.uMax16());
            vEdges.add(overlay.vMin16());
            vEdges.add(overlay.vMax16());
        }
        return cells(reference, List.copyOf(uEdges), List.copyOf(vEdges), true, overlayRegions);
    }

    /** Selects shared cells covered by this one logical Continuity contribution. */
    public static List<QuadSurface> coveredCells(List<QuadSurface> cells,
            List<QuadSurface> contribution) {
        Objects.requireNonNull(cells, "cells");
        Objects.requireNonNull(contribution, "contribution");
        return cells.stream().filter(cell -> covered(contribution, cell.uMin16(), cell.uMax16(),
                cell.vMin16(), cell.vMax16())).toList();
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

    private static List<QuadSurface> cells(QuadSurface reference, List<Integer> u,
            List<Integer> v, boolean unionOnly, List<QuadSurface> coverage) {
        List<QuadSurface> result = new ArrayList<>();
        for (int uIndex = 0; uIndex + 1 < u.size(); uIndex++) {
            int uMin = u.get(uIndex);
            int uMax = u.get(uIndex + 1);
            for (int vIndex = 0; vIndex + 1 < v.size(); vIndex++) {
                int vMin = v.get(vIndex);
                int vMax = v.get(vIndex + 1);
                if (unionOnly && !covered(coverage, uMin, uMax, vMin, vMax)) continue;
                result.add(new QuadSurface(reference.normal(), reference.plane16(),
                        reference.uAxis(), uMin, uMax, reference.vAxis(), vMin, vMax));
            }
        }
        return List.copyOf(result);
    }

    private static void requireSamePlane(QuadSurface reference, QuadSurface surface) {
        if (surface.normal() != reference.normal()
                || surface.plane16() != reference.plane16()
                || surface.uAxis() != reference.uAxis()
                || surface.vAxis() != reference.vAxis()) {
            throw new IllegalArgumentException("Overlay surfaces do not share a physical plane");
        }
    }
}
