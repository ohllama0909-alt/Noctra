package de.snenjih.noctra.menu.widget;

import de.snenjih.noctra.config.ModConfig;
import de.snenjih.noctra.modules.api.BaseModule;
import de.snenjih.noctra.modules.api.settings.EnumSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/** Renders an EnumSetting as a label + ◀ VALUE ▶ cycle button. */
public class EnumCycleWidget<E extends Enum<E>> implements SettingsWidget {

    private static final int HEIGHT   = 18;
    private static final int BTN_W    = 90;
    private static final int BTN_H    = 12;
    private static final int ARROW_W  = 10;
    private static final int COL_TEXT = 0xFFFFFFFF;
    private static final int COL_GRAY = 0xFF8899AA;
    private static final int COL_BG   = 0xFF1A2A40;
    private static final int COL_BORDER = 0xFF1E3A5F;
    private static final int COL_ARROW_HOVER = 0xFF4A7CF8;

    private final EnumSetting<E> setting;
    private final BaseModule     module;

    private int lastX, lastY, lastW;

    public EnumCycleWidget(EnumSetting<E> setting, BaseModule module) {
        this.setting = setting;
        this.module  = module;
    }

    @Override
    public int getHeight() { return HEIGHT; }

    @Override
    public void render(DrawContext ctx, int x, int y, int width, int mouseX, int mouseY) {
        lastX = x; lastY = y; lastW = width;
        var tr = MinecraftClient.getInstance().textRenderer;

        int btnX = x + width - BTN_W - 2;
        int btnY = y + (HEIGHT - BTN_H) / 2;

        // Label
        ctx.drawTextWithShadow(tr, setting.getLabel(), x + 2, y + (HEIGHT - 8) / 2, COL_TEXT);

        // Button background
        ctx.fill(btnX, btnY, btnX + BTN_W, btnY + BTN_H, COL_BG);
        ctx.drawStrokedRectangle(btnX, btnY, BTN_W, BTN_H, COL_BORDER);

        // Left arrow
        boolean hoverLeft = mouseX >= btnX && mouseX <= btnX + ARROW_W
                         && mouseY >= btnY && mouseY <= btnY + BTN_H;
        ctx.drawTextWithShadow(tr, "◀", btnX + 1, btnY + 2,
                hoverLeft ? COL_ARROW_HOVER : COL_GRAY);

        // Right arrow
        boolean hoverRight = mouseX >= btnX + BTN_W - ARROW_W && mouseX <= btnX + BTN_W
                          && mouseY >= btnY && mouseY <= btnY + BTN_H;
        ctx.drawTextWithShadow(tr, "▶", btnX + BTN_W - ARROW_W, btnY + 2,
                hoverRight ? COL_ARROW_HOVER : COL_GRAY);

        // Current value (centred)
        String valName = readableName(setting.get().name());
        int nameW = tr.getWidth(valName);
        ctx.drawTextWithShadow(tr, valName,
                btnX + (BTN_W - nameW) / 2, btnY + 2, COL_TEXT);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return false;
        int btnX = lastX + lastW - BTN_W - 2;
        int btnY = lastY + (HEIGHT - BTN_H) / 2;

        if (my < btnY || my > btnY + BTN_H) return false;

        E[] vals = setting.values();
        int idx  = setting.get().ordinal();

        if (mx >= btnX && mx <= btnX + ARROW_W) {
            // Left arrow — backwards
            setting.set(vals[(idx - 1 + vals.length) % vals.length]);
            ModConfig.getInstance().saveModuleSettings(module);
            return true;
        }
        if (mx >= btnX + BTN_W - ARROW_W && mx <= btnX + BTN_W) {
            // Right arrow — forwards
            setting.set(vals[(idx + 1) % vals.length]);
            ModConfig.getInstance().saveModuleSettings(module);
            return true;
        }
        // Click anywhere on button body cycles forward
        if (mx >= btnX && mx <= btnX + BTN_W) {
            setting.set(vals[(idx + 1) % vals.length]);
            ModConfig.getInstance().saveModuleSettings(module);
            return true;
        }
        return false;
    }

    /** Converts "SOME_ENUM_VALUE" → "Some Enum Value". */
    private static String readableName(String name) {
        String[] parts = name.split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (!p.isEmpty()) {
                if (!sb.isEmpty()) sb.append(' ');
                sb.append(Character.toUpperCase(p.charAt(0)));
                if (p.length() > 1) sb.append(p.substring(1).toLowerCase());
            }
        }
        return sb.toString();
    }
}
