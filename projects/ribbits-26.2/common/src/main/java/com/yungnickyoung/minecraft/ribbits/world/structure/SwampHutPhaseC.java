package com.yungnickyoung.minecraft.ribbits.world.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.structures.SwampHutPiece;

/** Audited local geometry and exact-piece predicate for the Phase C swamp-hut changes. */
public final class SwampHutPhaseC {
    public static final BlockPos SORCERER_LOCAL = new BlockPos(3, 2, 5);
    public static final BlockPos CAT_LOCAL = new BlockPos(2, 2, 4);
    public static final BlockPos BARREL_LOCAL = new BlockPos(2, 2, 6);
    public static final BlockPos CRAFTING_TABLE_LOCAL = new BlockPos(3, 2, 6);
    public static final BlockPos CAULDRON_LOCAL = new BlockPos(4, 2, 6);

    public static final int PIECE_MIN_X = 0;
    public static final int PIECE_MIN_Y = 0;
    public static final int PIECE_MIN_Z = 0;
    public static final int PIECE_MAX_X = 6;
    public static final int PIECE_MAX_Y = 6;
    public static final int PIECE_MAX_Z = 8;

    private SwampHutPhaseC() {
    }

    /**
     * A valid start is insufficient on its own: only the inclusive box of an actual procedural
     * SwampHutPiece qualifies. This deliberately excludes support columns below the piece.
     */
    public static boolean containsExactPiece(StructureStart start, BlockPos pos) {
        if (!start.isValid()) {
            return false;
        }
        for (StructurePiece piece : start.getPieces()) {
            if (piece instanceof SwampHutPiece && piece.getBoundingBox().isInside(pos)) {
                return true;
            }
        }
        return false;
    }

    static boolean contains(BoundingBox box, BlockPos pos) {
        return box.isInside(pos);
    }
}
