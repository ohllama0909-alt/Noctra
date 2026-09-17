package de.snenjih.noctra.modules.impl.shulker_tooltip;

import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipData;

import java.util.List;

public record ShulkerTooltipData(
    List<ItemStack> items,
    int totalSlots,
    int maxRowSize,
    boolean shortCounts,
    boolean compactMode,
    int backgroundColor
) implements TooltipData {}
