package games.twinhead.moreslabsstairsandwalls.block.redstone;
/** Geometry-neutral Redstone Block material power contract. */
public final class RedstoneSemantics {
    public static final int NOMINAL_SIGNAL = 15;
    private RedstoneSemantics() {}
    public static boolean isSignalSource() { return true; }
    public static int signal() { return NOMINAL_SIGNAL; }
}
