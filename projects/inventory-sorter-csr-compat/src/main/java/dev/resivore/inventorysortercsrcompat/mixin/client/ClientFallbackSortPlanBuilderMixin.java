package dev.resivore.inventorysortercsrcompat.mixin.client;

import dev.resivore.inventorysortercsrcompat.core.MaskedClientFallbackSort;
import net.kyrptonaught.inventorysorter.client.sort.ClientSortScope;
import net.kyrptonaught.inventorysorter.client.sort.plan.ClientFallbackSortPlanBuilder;
import net.kyrptonaught.inventorysorter.client.sort.plan.ClientSortClickPlanner;
import net.kyrptonaught.inventorysorter.client.sort.plan.PlannedContainerClick;
import net.kyrptonaught.inventorysorter.network.SortPriorityRuleSetting;
import net.kyrptonaught.inventorysorter.sort.SortType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Optional;

@Mixin(value = ClientFallbackSortPlanBuilder.class, remap = false)
public abstract class ClientFallbackSortPlanBuilderMixin {
    @Shadow @Final private ClientSortClickPlanner clickPlanner;

    @Inject(
            method = "build(Lnet/kyrptonaught/inventorysorter/client/sort/ClientSortScope;Lnet/kyrptonaught/inventorysorter/sort/SortType;Ljava/lang/String;Ljava/util/List;ZZ)Ljava/util/Optional;",
            at = @At("HEAD"), cancellable = true, require = 1, remap = false
    )
    private void inventorySorterCsrCompat$maskFixedSlots(
            ClientSortScope scope, SortType sortType, String languageCode,
            List<SortPriorityRuleSetting> priorityRules, boolean sortIntoBundles,
            boolean sortIntoHotbarBundles,
            CallbackInfoReturnable<Optional<List<PlannedContainerClick>>> callback
    ) {
        Optional<List<PlannedContainerClick>> masked = MaskedClientFallbackSort.planIfNeeded(
                scope, clickPlanner, sortType, languageCode, priorityRules);
        if (masked != null) callback.setReturnValue(masked);
    }
}
