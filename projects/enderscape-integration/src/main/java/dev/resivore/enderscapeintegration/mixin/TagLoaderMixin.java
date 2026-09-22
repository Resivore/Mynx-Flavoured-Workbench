package dev.resivore.enderscapeintegration.mixin;

import dev.resivore.enderscapeintegration.IntegrationContract;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagLoader;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Removes only the three future-only enchantments from the composed tag stream,
 * preserving vanilla and third-party entries instead of replacing whole tags.
 */
@Mixin(TagLoader.class)
abstract class TagLoaderMixin {
    @Shadow @Final private String directory;

    @Inject(method = "load", at = @At("RETURN"), require = 1)
    private void enderscapeIntegration$filterFutureAcquisitionTags(
            ResourceManager resourceManager,
            CallbackInfoReturnable<Map<Identifier, List<TagLoader.EntryWithSource>>> cir) {
        Map<Identifier, List<TagLoader.EntryWithSource>> tags = cir.getReturnValue();
        if ("tags/enchantment".equals(this.directory)) {
            removeEntries(tags, "in_enchanting_table", Set.of("enderscape:bundling", "enderscape:stun_burst"));
            removeEntries(tags, "non_treasure", Set.of("enderscape:bundling", "enderscape:stun_burst"));
            removeEntries(tags, "treasure", Set.of("enderscape:transdimensional"));
        } else if ("tags/item".equals(this.directory)) {
            // Resonance's supported-items tag now resolves only to the retained
            // Magnia Attractor; old stacks still decode and retain their data.
            removeEntries(tags, "enderscape:nebulite_tools", Set.of("enderscape:mirror", "enderscape:dagger"));
        }
    }

    private static void removeEntries(
            Map<Identifier, List<TagLoader.EntryWithSource>> tags,
            String tagId,
            Set<String> suppressedIds) {
        List<TagLoader.EntryWithSource> entries = tags.get(Identifier.parse(tagId));
        if (entries != null) {
            entries.removeIf(entry -> suppressedIds.contains(entry.entry().toString()));
        }
    }
}
