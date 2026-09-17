package de.snenjih.noctra.menu.widget;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

/**
 * A collapsible section header that groups other SettingsWidgets.
 * Also implements SettingsWidget so screens can treat it uniformly.
 */
public class CollapsibleSection implements SettingsWidget {

    private static final int HEADER_H   = 16;
    private static final int ROW_GAP    = 2;
    private static final int COL_HEADER = 0xFF1A2A40;
    private static final int COL_BORDER = 0xFF1E3A5F;
    private static final int COL_TEXT   = 0xFFCCDDFF;
    private static final int COL_ARROW  = 0xFF4A7CF8;

    private final String             label;
    private final List<SettingsWidget> children = new ArrayList<>();
    private       boolean            collapsed  = false;

    private int lastX, lastY, lastW;

    public CollapsibleSection(String label) {
        this.label = label;
    }

    public void addWidget(SettingsWidget widget) {
        children.add(widget);
    }

    public List<SettingsWidget> getChildren() {
        return children;
    }

    @Override
    public int getHeight() {
        int h = HEADER_H;
        if (!collapsed) {
            for (SettingsWidget w : children) {
                h += w.getHeight() + ROW_GAP;
            }
        }
        return h;
    }

    @Override
    public void render(DrawContext ctx, int x, int y, int width, int mouseX, int mouseY) {
        lastX = x; lastY = y; lastW = width;
        var tr = MinecraftClient.getInstance().textRenderer;

        // Header bar
        ctx.fill(x, y, x + width, y + HEADER_H, COL_HEADER);
        ctx.drawStrokedRectangle(x, y, width, HEADER_H, COL_BORDER);

        // Collapse arrow
        String arrow = collapsed ? "▶" : "▼";
        ctx.drawTextWithShadow(tr, arrow, x + 3, y + (HEADER_H - 8) / 2, COL_ARROW);

        // Section label
        ctx.drawTextWithShadow(tr, label, x + 14, y + (HEADER_H - 8) / 2, COL_TEXT);

        // Children
        if (!collapsed) {
            int cy = y + HEADER_H + ROW_GAP;
            for (SettingsWidget w : children) {
                w.render(ctx, x + 4, cy, width - 8, mouseX, mouseY);
                cy += w.getHeight() + ROW_GAP;
            }
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return false;
        // Click on header → toggle collapse
        if (mx >= lastX && mx <= lastX + lastW && my >= lastY && my <= lastY + HEADER_H) {
            collapsed = !collapsed;
            return true;
        }
        // Dispatch to children
        if (!collapsed) {
            int cy = lastY + HEADER_H + ROW_GAP;
            for (SettingsWidget w : children) {
                if (w.mouseClicked(mx, my, button)) return true;
                cy += w.getHeight() + ROW_GAP;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (collapsed) return false;
        for (SettingsWidget w : children) {
            if (w.mouseDragged(mx, my, button, dx, dy)) return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (collapsed) return false;
        for (SettingsWidget w : children) {
            if (w.mouseReleased(mx, my, button)) return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (collapsed) return false;
        for (SettingsWidget w : children) {
            if (w.keyPressed(keyCode, scanCode, modifiers)) return true;
        }
        return false;
    }

    @Override
    public void charTyped(char chr, int mods) {
        if (collapsed) return;
        for (SettingsWidget w : children) {
            w.charTyped(chr, mods);
        }
    }
}
