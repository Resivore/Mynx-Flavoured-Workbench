package dev.resivore.bgebushyleaves.geometry;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/** Deterministic target-footprint subtraction for partially covered leaf contacts. */
public final class RectSubtraction {
    private RectSubtraction() {}
    public static List<Rect16> subtract(Rect16 source, List<Rect16> hidden) {
        List<Rect16> clipped = hidden.stream().map(source::intersection).flatMap(Optional -> Optional.stream()).distinct().toList();
        if (clipped.isEmpty()) return List.of(source);
        TreeSet<Integer> us = new TreeSet<>(List.of(source.uMin(), source.uMax()));
        TreeSet<Integer> vs = new TreeSet<>(List.of(source.vMin(), source.vMax()));
        for (Rect16 rect : clipped) { us.add(rect.uMin()); us.add(rect.uMax()); vs.add(rect.vMin()); vs.add(rect.vMax()); }
        List<Integer> u = List.copyOf(us), v = List.copyOf(vs); List<Rect16> result = new ArrayList<>();
        for (int vi = 0; vi + 1 < v.size(); vi++) for (int ui = 0; ui + 1 < u.size(); ui++) {
            Rect16 cell = new Rect16(u.get(ui), u.get(ui + 1), v.get(vi), v.get(vi + 1));
            if (clipped.stream().noneMatch(rect -> rect.covers(cell))) result.add(cell);
        }
        return List.copyOf(result);
    }
}
