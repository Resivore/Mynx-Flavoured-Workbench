package dev.resivore.carryonpatch;

import dev.resivore.carryonpatch.mixin.EntityIdAccessor;
import java.util.HashMap;
import net.minecraft.world.entity.Entity;

/**
 * GrabAndGo-compatible cache that assigns identity only at the synthetic-entity ownership seam.
 * It deliberately retains no entities beyond the entries already owned by GrabAndGo's map.
 */
public final class RenderIdAssigningEntityCache extends HashMap<String, Entity> {
    @Override
    public Entity put(String entityTypeId, Entity entity) {
        if (entity != null) {
            int currentId = ((EntityIdAccessor) entity).carryOnPatch$getRawId();
            int selectedId = RenderOnlyEntityIds.selectId(
                    currentId,
                    candidate -> entity.level().getEntity(candidate) != null
            );
            if (selectedId != currentId) {
                entity.setId(selectedId);
            }
        }
        return super.put(entityTypeId, entity);
    }
}
