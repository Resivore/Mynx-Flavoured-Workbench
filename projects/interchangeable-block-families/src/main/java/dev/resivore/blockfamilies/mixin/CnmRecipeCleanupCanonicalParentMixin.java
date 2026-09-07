package dev.resivore.blockfamilies.mixin;

import dev.resivore.blockfamilies.cnm.runtime.AuditedShapeRuntime;
import dev.tazer.clutternomore.common.recipe.RecipeRemover;
import dev.tazer.clutternomore.common.shape_map.ShapeMap;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

/**
 * CNM 2.0.7 cleanup tests a recipe JSON against every item returned by
 * {@link ShapeMap#getShapes(Item)}. The list includes its canonical parent,
 * which incorrectly removes an IBF parent\'s own acquisition recipe. Supply
 * the same component minus that parent only at CNM\'s cleanup match point.
 * Alternate results still match, and every non-IBF family retains CNM\'s
 * unchanged behavior.
 */
@Mixin(value = RecipeRemover.class, remap = false)
abstract class CnmRecipeCleanupCanonicalParentMixin {
    @Redirect(
            method = "removeShapeRecipes",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/tazer/clutternomore/common/shape_map/ShapeMap;getShapes(Lnet/minecraft/world/item/Item;)Ljava/util/List;"
            ),
            require = 1
    )
    private static List<Item> interchangeableBlockFamilies$excludeCanonicalParentFromCleanupMatch(Item result) {
        List<Item> members = ShapeMap.getShapes(result);
        if (!AuditedShapeRuntime.isAuditedCanonicalParent(result)) {
            return members;
        }
        return members.stream().filter(member -> member != result).toList();
    }
}
