package dev.resivore.bgebushyleaves.geometry;

import java.util.ArrayList;
import java.util.List;

/**
 * Matcha-style crossed foliage planes composed in block space, then clipped to BGE occupancy.
 * The templates never inspect an individual exposed face or derive an orientation from it.
 */
public final class BlockSpaceFoliagePlan {
    private static final float HORIZONTAL_OVERHANG_16 = 0.70F;
    private static final float VERTICAL_OVERHANG_16 = 0.45F;
    private static final float LEAN_16 = 1.10F;
    // The active Matcha reference uses the same two world-Y rotations; paired offsets give the
    // planes a leaf-volume read while their authored world axes remain stable across BGE forms.
    private static final List<Template> TEMPLATES = List.of(
            new Template(-12.0F, -1.20F), new Template(-12.0F, 1.20F),
            new Template(36.0F, -1.20F), new Template(36.0F, 1.20F));

    private BlockSpaceFoliagePlan() {}

    public static List<Card> plan(BlockSpaceOccupancy occupancy, long stableSeed) {
        BlockSpaceOccupancy.Bounds bounds = occupancy.bounds();
        float centerX = (bounds.minX() + bounds.maxX()) * 0.5F;
        float centerY = (bounds.minY() + bounds.maxY()) * 0.5F;
        float centerZ = (bounds.minZ() + bounds.maxZ()) * 0.5F;
        float radius = (float) Math.hypot(bounds.maxX() - bounds.minX(), bounds.maxZ() - bounds.minZ()) * 0.5F + 1.0F;
        int minS = (int) Math.floor(-radius), maxS = (int) Math.ceil(radius);
        List<Card> result = new ArrayList<>();
        int ordinal = 0;
        for (int templateIndex = 0; templateIndex < TEMPLATES.size(); templateIndex++) {
            Template template = TEMPLATES.get(templateIndex);
            float seedOffset = ((mix(stableSeed ^ templateIndex) & 3L) - 1.5F) * 0.10F;
            boolean[][] supported = supportGrid(occupancy, bounds, centerX, centerY, centerZ,
                    minS, maxS, template.withOffset(template.offset16() + seedOffset));
            List<Panel> panels = occupancy.fillsBounds()
                    ? List.of(enclosingPanel(supported, minS, bounds.minY()))
                    : panels(supported, minS, bounds.minY());
            for (Panel panel : panels) {
                Template applied = template.withOffset(template.offset16() + seedOffset);
                result.add(new Card(ordinal++, panel.cellCount(), vertices(panel, bounds, centerX, centerY, centerZ, applied)));
            }
        }
        return List.copyOf(result);
    }

    private static boolean[][] supportGrid(BlockSpaceOccupancy occupancy, BlockSpaceOccupancy.Bounds bounds,
            float centerX, float centerY, float centerZ, int minS, int maxS, Template template) {
        boolean[][] result = new boolean[maxS - minS][bounds.maxY() - bounds.minY()];
        for (int s = 0; s < result.length; s++) for (int y = 0; y < result[s].length; y++) {
            Vertex point = point(centerX, centerY, centerZ, bounds, template, minS + s + 0.5F,
                    bounds.minY() + y + 0.5F);
            result[s][y] = occupancy.contains(point.x16(), point.y16(), point.z16());
        }
        return result;
    }

    private static List<Panel> panels(boolean[][] cells, int minS, int minY) {
        List<Panel> result = new ArrayList<>();
        for (int y = 0; y < cells[0].length; y++) for (int s = 0; s < cells.length; s++) {
            if (!cells[s][y]) continue;
            int sEnd = s + 1;
            while (sEnd < cells.length && cells[sEnd][y]) sEnd++;
            int yEnd = y + 1;
            while (yEnd < cells[0].length && all(cells, s, sEnd, yEnd)) yEnd++;
            for (int clearY = y; clearY < yEnd; clearY++) for (int clearS = s; clearS < sEnd; clearS++) cells[clearS][clearY] = false;
            result.add(new Panel(minS + s, minS + sEnd, minY + y, minY + yEnd, (sEnd - s) * (yEnd - y)));
        }
        return result;
    }

    private static Panel enclosingPanel(boolean[][] cells, int minS, int minY) {
        int firstS = cells.length, lastS = 0, firstY = cells[0].length, lastY = 0, count = 0;
        for (int s = 0; s < cells.length; s++) for (int y = 0; y < cells[s].length; y++) if (cells[s][y]) {
            firstS = Math.min(firstS, s); lastS = Math.max(lastS, s + 1);
            firstY = Math.min(firstY, y); lastY = Math.max(lastY, y + 1); count++;
        }
        if (count == 0) throw new IllegalStateException("A nonempty BGE box must intersect each foliage plane");
        return new Panel(minS + firstS, minS + lastS, minY + firstY, minY + lastY, count);
    }

    private static boolean all(boolean[][] cells, int start, int end, int y) {
        for (int s = start; s < end; s++) if (!cells[s][y]) return false;
        return true;
    }

    private static Vertex[] vertices(Panel panel, BlockSpaceOccupancy.Bounds bounds, float centerX,
            float centerY, float centerZ, Template template) {
        float s0 = panel.sMin16() - HORIZONTAL_OVERHANG_16, s1 = panel.sMax16() + HORIZONTAL_OVERHANG_16;
        float y0 = panel.yMin16() - VERTICAL_OVERHANG_16, y1 = panel.yMax16() + VERTICAL_OVERHANG_16;
        return new Vertex[] {point(centerX, centerY, centerZ, bounds, template, s0, y0),
                point(centerX, centerY, centerZ, bounds, template, s1, y0),
                point(centerX, centerY, centerZ, bounds, template, s1, y1),
                point(centerX, centerY, centerZ, bounds, template, s0, y1)};
    }

    private static Vertex point(float centerX, float centerY, float centerZ, BlockSpaceOccupancy.Bounds bounds,
            Template template, float s, float y) {
        float angle = (float) Math.toRadians(template.angleDegrees());
        float alongX = (float) Math.cos(angle), alongZ = (float) Math.sin(angle);
        float normalX = -alongZ, normalZ = alongX;
        float heightFraction = (y - centerY) / Math.max(1.0F, bounds.maxY() - bounds.minY());
        float offset = template.offset16() + heightFraction * LEAN_16;
        return new Vertex(centerX + alongX * s + normalX * offset, y,
                centerZ + alongZ * s + normalZ * offset);
    }

    private static long mix(long value) {
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        return value;
    }

    private record Template(float angleDegrees, float offset16) {
        Template withOffset(float offset) { return new Template(angleDegrees, offset); }
    }
    private record Panel(int sMin16, int sMax16, int yMin16, int yMax16, int cellCount) {}
    public record Card(int ordinal, int supportedCells, Vertex[] vertices) {
        public Card { vertices = vertices.clone(); }
        @Override public Vertex[] vertices() { return vertices.clone(); }
    }
    public record Vertex(float x16, float y16, float z16) {}
}
