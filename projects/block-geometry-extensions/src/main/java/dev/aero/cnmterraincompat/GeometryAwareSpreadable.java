package dev.aero.cnmterraincompat;

import games.twinhead.moreslabsstairsandwalls.block.spreadable.SpreadableGeometry;

/**
 * Spreadable geometry whose exposure result accounts for its exact owned top footprint,
 * including fluid and collision coverage in the blockspace above.
 */
public interface GeometryAwareSpreadable extends SpreadableGeometry {}
