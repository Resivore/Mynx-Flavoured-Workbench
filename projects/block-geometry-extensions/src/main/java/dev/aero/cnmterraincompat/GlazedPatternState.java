package dev.aero.cnmterraincompat;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/** Independent glazed-pattern orientation shared by the specialized CNM geometries. */
public final class GlazedPatternState {
    public static final EnumProperty<Direction> PATTERN_FACING = EnumProperty.create(
            "pattern_facing", Direction.class, direction -> direction.getAxis().isHorizontal());

    private GlazedPatternState() {}

    /** Matches Nibaru's native glazed slab blockstate rotations: east=0, south=90, west=180, north=270. */
    public static int patternYaw(Direction direction) {
        return switch (direction) {
            case EAST -> 0;
            case SOUTH -> 90;
            case WEST -> 180;
            case NORTH -> 270;
            default -> throw new IllegalArgumentException("Glazed pattern direction must be horizontal: " + direction);
        };
    }

    /**
     * Returns the physical occupancy direction expressed in the material-pattern coordinate frame.
     * Rotating that model by {@link #patternYaw(Direction)} produces the requested world occupancy.
     */
    public static Direction relativePhysical(Direction physicalFacing, Direction patternFacing) {
        int relativeYaw = Math.floorMod(geometryYaw(physicalFacing) - patternYaw(patternFacing), 360);
        return switch (relativeYaw) {
            case 0 -> Direction.NORTH;
            case 90 -> Direction.EAST;
            case 180 -> Direction.SOUTH;
            case 270 -> Direction.WEST;
            default -> throw new IllegalStateException("Unexpected horizontal yaw " + relativeYaw);
        };
    }

    private static int geometryYaw(Direction direction) {
        return switch (direction) {
            case NORTH -> 0;
            case EAST -> 90;
            case SOUTH -> 180;
            case WEST -> 270;
            default -> throw new IllegalArgumentException("CNM physical direction must be horizontal: " + direction);
        };
    }
}
