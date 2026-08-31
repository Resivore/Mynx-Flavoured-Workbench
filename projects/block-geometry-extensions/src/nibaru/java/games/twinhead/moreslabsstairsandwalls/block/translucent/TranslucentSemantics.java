package games.twinhead.moreslabsstairsandwalls.block.translucent;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfiles;
import net.minecraft.world.level.block.Block;
public final class TranslucentSemantics { private TranslucentSemantics() {}
 public static boolean sameMaterial(Block a,Block b){var x=NibaruMaterialProfiles.fromBlock(a);var y=NibaruMaterialProfiles.fromBlock(b);return x.isPresent()&&y.isPresent()&&x.get().canonicalParent()==y.get().canonicalParent();}
}
