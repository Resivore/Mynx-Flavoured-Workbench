package dev.resivore.villagerwork.mixin.client;

import java.util.Map;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Read-only diagnostic access for reporting exact runtime child names and hierarchy. */
@Mixin(ModelPart.class)
public interface ModelPartChildrenAccessor {
    @Accessor("children")
    Map<String, ModelPart> villagerWork$getChildren();
}
