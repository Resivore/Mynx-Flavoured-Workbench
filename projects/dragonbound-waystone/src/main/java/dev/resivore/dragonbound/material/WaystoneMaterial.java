package dev.resivore.dragonbound.material;

import dev.resivore.dragonbound.DragonboundContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

import java.util.Optional;

/**
 * The persistent visual-material identity for a Dragonbound Waystone.
 *
 * <p>The value is intentionally only a block registry id. It never contains an anchor position,
 * destination, or any other authority belonging to the server-owned anchor record.</p>
 */
public final class WaystoneMaterial {
    public static final Identifier COMPONENT_ID = DragonboundContent.id("waystone_material");
    public static final Identifier DEFAULT_BLOCK_ID = Identifier.withDefaultNamespace("end_stone_bricks");
    public static final TagKey<Block> DENY_TAG = TagKey.create(
            net.minecraft.core.registries.Registries.BLOCK,
            DragonboundContent.id("waystone_material_deny"));

    public static final DataComponentType<Identifier> BLOCK_ID = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            COMPONENT_ID,
            DataComponentType.<Identifier>builder()
                    .persistent(Identifier.CODEC)
                    .networkSynchronized(Identifier.STREAM_CODEC)
                    .cacheEncoding()
                    .build());

    private WaystoneMaterial() {
    }

    /** Forces component registration during Dragonbound's ordinary bootstrap. */
    public static void register() {
    }

    public static Optional<Identifier> selectedBlockId(ItemStack stack) {
        if (!stack.is(DragonboundContent.WAYSTONE_ITEM)) {
            return Optional.empty();
        }
        Identifier id = stack.get(BLOCK_ID);
        return id == null ? Optional.empty() : eligibleBlockId(id);
    }

    public static Optional<Block> selectedBlock(ItemStack stack) {
        return selectedBlockId(stack).map(BuiltInRegistries.BLOCK::getValue);
    }

    /** The legacy component-less visual is, and remains, End Stone Bricks. */
    public static Identifier effectiveBlockId(ItemStack stack) {
        return selectedBlockId(stack).orElse(DEFAULT_BLOCK_ID);
    }

    public static Optional<Identifier> eligibleDonorId(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem blockItem) || stack.is(DragonboundContent.WAYSTONE_ITEM)) {
            return Optional.empty();
        }

        Block block = blockItem.getBlock();
        Identifier id = BuiltInRegistries.BLOCK.getKey(block);
        if (id == null || !isEligibleBlock(block)) {
            return Optional.empty();
        }
        return Optional.of(id);
    }

    public static boolean isEligibleBlock(Block block) {
        BlockState state = block.defaultBlockState();
        return !state.isAir()
                && state.getRenderShape() == RenderShape.MODEL
                && state.canOcclude()
                && state.isSolidRender()
                && state.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO)
                && state.getLightEmission() == 0
                && !state.emissiveRendering()
                && state.getMapColor(EmptyBlockGetter.INSTANCE, BlockPos.ZERO) != MapColor.GRASS
                && state.getMapColor(EmptyBlockGetter.INSTANCE, BlockPos.ZERO) != MapColor.PLANT
                && !isDeniedByTag(block);
    }

    /** Validates an untrusted persisted or synchronized visual-material identity. */
    public static Optional<Identifier> eligibleBlockId(Identifier id) {
        if (!BuiltInRegistries.BLOCK.containsKey(id)) {
            return Optional.empty();
        }
        Block block = BuiltInRegistries.BLOCK.getValue(id);
        return isEligibleBlock(block) ? Optional.of(id) : Optional.empty();
    }

    private static boolean isDeniedByTag(Block block) {
        // Tags are always bound for a loaded server/client registry. The small guard keeps
        // bootstrap-only codec tests useful before a datapack tag reload has happened.
        try {
            return block.builtInRegistryHolder().is(DENY_TAG);
        } catch (IllegalStateException unboundTags) {
            return false;
        }
    }
}
