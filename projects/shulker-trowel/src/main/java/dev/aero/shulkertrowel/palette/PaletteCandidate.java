package dev.aero.shulkertrowel.palette;

import net.minecraft.world.item.BlockItem;

public record PaletteCandidate(int slot, int weight, BlockItem sourceItem, BlockItem placementItem) {}
