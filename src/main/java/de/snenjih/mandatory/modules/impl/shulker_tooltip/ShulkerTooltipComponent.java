package de.snenjih.mandatory.modules.impl.shulker_tooltip;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.item.ItemStack;

import java.util.List;

public class ShulkerTooltipComponent implements TooltipComponent {

    private static final int PADDING = 7;
    private static final int SLOT_SIZE = 18;

    private final ShulkerTooltipData data;
    private final List<ItemStack> displayItems;
    private final int numCols;
    private final int numRows;

    public ShulkerTooltipComponent(ShulkerTooltipData data) {
        this.data = data;
        // In compactMode, show only non-empty items; in full mode show all slots
        if (data.compactMode()) {
            this.displayItems = data.items().stream().filter(s -> !s.isEmpty()).toList();
        } else {
            this.displayItems = data.items();
        }
        int count = Math.max(1, data.compactMode() ? displayItems.size() : data.totalSlots());
        this.numCols = Math.min(data.maxRowSize(), count);
        this.numRows = (int) Math.ceil((double) count / numCols);
    }

    @Override
    public int getWidth(TextRenderer textRenderer) {
        return PADDING * 2 + numCols * SLOT_SIZE;
    }

    @Override
    public int getHeight(TextRenderer textRenderer) {
        return PADDING * 2 + numRows * SLOT_SIZE;
    }

    @Override
    public void drawItems(TextRenderer textRenderer, int x, int y, int width, int height, DrawContext context) {
        // Border
        context.fill(x - 1, y - 1, x + width + 1, y + height + 1, 0xFF555555);
        // Background
        context.fill(x, y, x + width, y + height, data.backgroundColor());

        for (int i = 0; i < displayItems.size(); i++) {
            ItemStack stack = displayItems.get(i);
            int col = i % numCols;
            int row = i / numCols;
            int sx = x + PADDING + col * SLOT_SIZE;
            int sy = y + PADDING + row * SLOT_SIZE;

            if (!stack.isEmpty()) {
                context.drawItem(stack, sx, sy);
                int count = stack.getCount();
                if (count > 1) {
                    String label = data.shortCounts() ? abbreviate(count) : String.valueOf(count);
                    context.drawText(textRenderer, label,
                        sx + 16 - textRenderer.getWidth(label),
                        sy + 9,
                        0xFFFFFF, true);
                }
            }
        }
    }

    private static String abbreviate(int n) {
        if (n >= 1_000_000) return (n / 1_000_000) + "M";
        if (n >= 1_000)     return (n / 1_000) + "k";
        return String.valueOf(n);
    }
}
