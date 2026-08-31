package com.starfish_studios.bbb.item;

import com.starfish_studios.bbb.block.BalustradeBlock;
import com.starfish_studios.bbb.block.ColumnBlock;
import com.starfish_studios.bbb.block.FrameBlock;
import com.starfish_studios.bbb.block.LatticeBlock;
import com.starfish_studios.bbb.block.MouldingBlock;
import com.starfish_studios.bbb.block.PalletBlock;
import com.starfish_studios.bbb.block.StoneFenceBlock;
import com.starfish_studios.bbb.block.SupportBlock;
import com.starfish_studios.bbb.block.UrnBlock;
import com.starfish_studios.bbb.block.WoodenLanternBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

import java.util.function.Consumer;
import java.util.function.BooleanSupplier;

/** Preserves BBB's default Ctrl-expanded usage hints without its removed config dependency. */
public final class DescriptionBlockItem extends BlockItem {
    private static BooleanSupplier controlDown = () -> false;

    public DescriptionBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> output, TooltipFlag flag) {
        TooltipKind kind = TooltipKind.forBlock(getBlock());
        if (kind == null) {
            super.appendHoverText(stack, context, display, output, flag);
            return;
        }
        if (!hasControlDown()) {
            output.accept(Component.literal("[")
                    .append(Component.translatable("key.keyboard.left.control"))
                    .append(Component.literal("]"))
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            return;
        }

        switch (kind) {
            case BALUSTRADE -> {
                pencil(output, "balustrade1");
                line(output, "balustrade2");
                line(output, "balustrade3");
                pencil(output, "balustrade4");
                line(output, "balustrade5");
            }
            case URN -> {
                pencil(output, "urn1");
                line(output, "urn2");
                pencil(output, "urn3");
                line(output, "urn4");
            }
            case LATTICE -> {
                pencil(output, "lattice1");
                line(output, "lattice2");
                pencil(output, "lattice3");
                line(output, "lattice4");
            }
            case LANTERN -> {
                pencil(output, "lantern1");
                line(output, "lantern2");
            }
            case STONE_FENCE -> {
                pencil(output, "stone_fence1");
                line(output, "stone_fence2");
                pencil(output, "stone_fence3");
                line(output, "stone_fence4");
            }
            case MOULDING -> {
                pencil(output, "moulding1");
                line(output, "moulding2");
            }
            case SUPPORT -> {
                pencil(output, "support1");
                line(output, "support2");
            }
            case PALLET -> {
                pencil(output, "pallet1");
                line(output, "pallet2");
                pencil(output, "pallet3");
                line(output, "pallet4");
                line(output, "pallet5");
            }
            case COLUMN -> {
                pencil(output, "column1");
                line(output, "column2");
            }
            case FRAME -> {
                pencil(output, "frame1");
                line(output, "frame2");
                pencil(output, "frame3");
                output.accept(Component.literal("- ")
                        .append(Component.translatable("description.bbb.frameConfig1")
                                .withStyle(ChatFormatting.GRAY)));
                line(output, "frameConfig2");
            }
        }
    }

    private static void pencil(Consumer<Component> output, String suffix) {
        output.accept(Component.translatable("description.bbb.pencil")
                .withStyle(ChatFormatting.BLUE)
                .append(Component.translatable("description.bbb." + suffix)
                        .withStyle(ChatFormatting.GRAY)));
    }

    private static void line(Consumer<Component> output, String suffix) {
        output.accept(Component.translatable("description.bbb." + suffix).withStyle(ChatFormatting.GRAY));
    }

    private static boolean hasControlDown() {
        return controlDown.getAsBoolean();
    }

    public static void installControlKeyCheck(BooleanSupplier check) {
        controlDown = check;
    }

    private enum TooltipKind {
        BALUSTRADE,
        URN,
        LATTICE,
        LANTERN,
        STONE_FENCE,
        MOULDING,
        SUPPORT,
        PALLET,
        COLUMN,
        FRAME;

        private static TooltipKind forBlock(Block block) {
            if (block instanceof BalustradeBlock) return BALUSTRADE;
            if (block instanceof UrnBlock) return URN;
            if (block instanceof LatticeBlock) return LATTICE;
            if (block instanceof WoodenLanternBlock) return LANTERN;
            if (block instanceof StoneFenceBlock) return STONE_FENCE;
            if (block instanceof MouldingBlock) return MOULDING;
            if (block instanceof SupportBlock) return SUPPORT;
            if (block instanceof PalletBlock) return PALLET;
            if (block instanceof ColumnBlock) return COLUMN;
            if (block instanceof FrameBlock) return FRAME;
            return null;
        }
    }
}
