package dev.resivore.bgebushyleaves.geometry;

import dev.aero.cnmterraincompat.BgeSurfaceGeometry.SurfacePatch;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/** Merges only coplanar equal-frame BGE tiles; it never invents geometry outside their union. */
public final class PatchMerger {
    private PatchMerger() {}
    public static List<PatchFrame> mergeSurfacePatches(List<SurfacePatch> patches) {
        Map<Key, List<Rect16>> groups = new LinkedHashMap<>();
        for (SurfacePatch patch : patches) {
            Key key = new Key(patch.normal(), patch.plane16(), patch.uAxis(), patch.vAxis(), patch.canonicalFace());
            groups.computeIfAbsent(key, ignored -> new ArrayList<>()).add(new Rect16(patch.uMin16(), patch.uMax16(), patch.vMin16(), patch.vMax16()));
        }
        List<PatchFrame> result = new ArrayList<>();
        groups.forEach((key, rectangles) -> mergeRectangles(rectangles).forEach(bounds -> result.add(
                new PatchFrame(key.normal, key.plane16, key.uAxis, bounds, key.vAxis, key.canonicalFace))));
        return result.stream().sorted(Comparator.comparing((PatchFrame frame) -> frame.normal().ordinal())
                .thenComparingInt(PatchFrame::plane16).thenComparingInt(frame -> frame.bounds().vMin())
                .thenComparingInt(frame -> frame.bounds().uMin())).toList();
    }
    private static List<Rect16> mergeRectangles(List<Rect16> source) {
        TreeSet<Integer> us = new TreeSet<>(), vs = new TreeSet<>();
        for (Rect16 rect : source) { us.add(rect.uMin()); us.add(rect.uMax()); vs.add(rect.vMin()); vs.add(rect.vMax()); }
        List<Integer> u = List.copyOf(us), v = List.copyOf(vs); List<Rect16> result = new ArrayList<>();
        for (int vi = 0; vi + 1 < v.size(); vi++) {
            int ui = 0;
            while (ui + 1 < u.size()) {
                Rect16 cell = new Rect16(u.get(ui), u.get(ui + 1), v.get(vi), v.get(vi + 1));
                if (!covered(source, cell)) { ui++; continue; }
                int start = ui++;
                while (ui + 1 < u.size()
                        && covered(source, new Rect16(u.get(ui), u.get(ui + 1),
                                v.get(vi), v.get(vi + 1)))) ui++;
                result.add(new Rect16(u.get(start), u.get(ui), v.get(vi), v.get(vi + 1)));
            }
        }
        return result;
    }
    private static boolean covered(List<Rect16> source, Rect16 cell) {
        return source.stream().anyMatch(rect -> rect.covers(cell));
    }
    private record Key(net.minecraft.core.Direction normal, int plane16, net.minecraft.core.Direction.Axis uAxis,
            net.minecraft.core.Direction.Axis vAxis, net.minecraft.core.Direction canonicalFace) {}
}
