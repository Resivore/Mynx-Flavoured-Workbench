package dev.resivore.bgeglassculling.geometry;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

/** Deterministic exact partition of one rectangle minus the union of any cull rectangles. */
public final class RectSubtraction {
    private RectSubtraction() {}

    public static List<Rect16> subtract(Rect16 source, List<Rect16> cullRegions) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(cullRegions, "cullRegions");

        List<Rect16> clipped = cullRegions.stream()
                .map(region -> source.intersection(Objects.requireNonNull(region, "cull region")))
                .flatMap(java.util.Optional::stream)
                .distinct()
                .toList();
        if (clipped.isEmpty()) return List.of(source);

        TreeSet<Integer> uEdges = new TreeSet<>();
        TreeSet<Integer> vEdges = new TreeSet<>();
        uEdges.add(source.uMin());
        uEdges.add(source.uMax());
        vEdges.add(source.vMin());
        vEdges.add(source.vMax());
        for (Rect16 region : clipped) {
            uEdges.add(region.uMin());
            uEdges.add(region.uMax());
            vEdges.add(region.vMin());
            vEdges.add(region.vMax());
        }

        List<Integer> u = List.copyOf(uEdges);
        List<Integer> v = List.copyOf(vEdges);
        List<Rect16> visible = new ArrayList<>();
        // Stable bottom-to-top, then left-to-right order.
        for (int vIndex = 0; vIndex + 1 < v.size(); vIndex++) {
            for (int uIndex = 0; uIndex + 1 < u.size(); uIndex++) {
                Rect16 cell = new Rect16(u.get(uIndex), u.get(uIndex + 1),
                        v.get(vIndex), v.get(vIndex + 1));
                if (clipped.stream().noneMatch(region -> region.covers(cell))) visible.add(cell);
            }
        }
        return List.copyOf(visible);
    }
}
