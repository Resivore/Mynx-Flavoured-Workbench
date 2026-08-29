package games.twinhead.moreslabsstairsandwalls;

import net.minecraft.resources.Identifier;

public class MoreSlabsStairsAndWalls
{
	public static final String MOD_ID = "more_slabs_stairs_and_walls";

	public static void init() {}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
