package de.snenjih.noctra.menu.widget;

import de.snenjih.noctra.config.ModConfig;
import de.snenjih.noctra.modules.api.BaseModule;
import de.snenjih.noctra.modules.api.settings.BooleanSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/** Renders a boolean setting as a label + ON/OFF toggle switch. */
public class ToggleWidget implements SettingsWidget {

    private static final int HEIGHT   = 18;
    private static final int TOGGLE_W = 26;
    private static final int TOGGLE_H = 10;

    private static final int COL_ON   = 0xFF44DD88;
    private static final int COL_OFF  = 0xFFDD4444;
    private static final int COL_TEXT = 0xFFFFFFFF;
    private static final int COL_GRAY = 0xFF8899AA;

    private final BooleanSetting setting;
    private final BaseModule     module;

    private int lastX, lastY, lastW;

    public ToggleWidget(BooleanSetting setting, BaseModule module) {
        this.setting = setting;
        this.module  = module;
    }

    @Override
    public int getHeight() { return HEIGHT; }

    @Override
    public void render(DrawContext ctx, int x, int y, int width, int mouseX, int mouseY) {
        lastX = x; lastY = y; lastW = width;
        var tr = MinecraftClient.getInstance().textRenderer;

        // Label
        ctx.drawTextWithShadow(tr, setting.getLabel(), x + 2, y + (HEIGHT - 8) / 2, COL_TEXT);

        // Toggle background
        int toggleX = x + width - TOGGLE_W - 2;
        int toggleY = y + (HEIGHT - TOGGLE_H) / 2;
        boolean on  = setting.get();

        ctx.fill(toggleX, toggleY, toggleX + TOGGLE_W, toggleY + TOGGLE_H,
                 on ? 0xFF2A8855 : 0xFF882222);
        ctx.drawStrokedRectangle(toggleX, toggleY, TOGGLE_W, TOGGLE_H,
                 on ? 0xFF66FFAA : 0xFFFF7777);

        // Knob
        int knobX = on ? toggleX + TOGGLE_W - TOGGLE_H : toggleX;
        ctx.fill(knobX, toggleY, knobX + TOGGLE_H, toggleY + TOGGLE_H, COL_TEXT);

        // ON/OFF label inside the toggle
        String label  = on ? "ON" : "OFF";
        int    labelX = on ? toggleX + 3 : toggleX + TOGGLE_H + 1;
        ctx.drawTextWithShadow(tr, label, labelX, toggleY + 1,
                               on ? 0xFF001A0A : COL_GRAY);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return false;
        if (mx >= lastX && mx <= lastX + lastW && my >= lastY && my <= lastY + HEIGHT) {
            setting.set(!setting.get());
            ModConfig.getInstance().saveModuleSettings(module);
            return true;
        }
        return false;
    }
}
