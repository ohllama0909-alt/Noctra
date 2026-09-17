package de.snenjih.noctra.menu.widget;

import de.snenjih.noctra.config.ModConfig;
import de.snenjih.noctra.modules.api.BaseModule;
import de.snenjih.noctra.modules.api.settings.TextSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

/** Renders a TextSetting as a label + editable text field. */
public class TextInputWidget implements SettingsWidget {

    private static final int HEIGHT    = 18;
    private static final int FIELD_H   = 12;
    private static final int COL_TEXT  = 0xFFFFFFFF;
    private static final int COL_GRAY  = 0xFF8899AA;
    private static final int COL_BG    = 0xFF0D1B2A;
    private static final int COL_FOCUS = 0xFF4A7CF8;
    private static final int COL_IDLE  = 0xFF1E3A5F;

    private final TextSetting setting;
    private final BaseModule  module;

    private boolean isActive  = false;
    private String  buffer    = "";
    private int lastX, lastY, lastW;

    public TextInputWidget(TextSetting setting, BaseModule module) {
        this.setting = setting;
        this.module  = module;
        this.buffer  = setting.get();
    }

    @Override
    public int getHeight() { return HEIGHT; }

    @Override
    public void render(DrawContext ctx, int x, int y, int width, int mouseX, int mouseY) {
        lastX = x; lastY = y; lastW = width;
        var tr = MinecraftClient.getInstance().textRenderer;

        int labelW  = (int) (width * 0.45f);
        int fieldW  = width - labelW - 6;
        int fieldX  = x + labelW + 4;
        int fieldY  = y + (HEIGHT - FIELD_H) / 2;

        // Label
        ctx.drawTextWithShadow(tr, setting.getLabel(), x + 2, y + (HEIGHT - 8) / 2, COL_TEXT);

        // Field background
        ctx.fill(fieldX, fieldY, fieldX + fieldW, fieldY + FIELD_H, COL_BG);
        ctx.drawStrokedRectangle(fieldX, fieldY, fieldW, FIELD_H,
                isActive ? COL_FOCUS : COL_IDLE);

        // Text content + cursor
        String display = buffer;
        boolean showCursor = isActive && System.currentTimeMillis() % 1000 < 500;

        String textWithCursor = showCursor ? display + "|" : display;
        // Clip text to field width
        String rendered = textWithCursor;
        while (tr.getWidth(rendered) > fieldW - 4 && !rendered.isEmpty()) {
            rendered = rendered.substring(1);
        }
        ctx.drawTextWithShadow(tr, rendered, fieldX + 2, fieldY + 2,
                isActive ? COL_TEXT : COL_GRAY);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return false;
        int labelW = (int) (lastW * 0.45f);
        int fieldX = lastX + labelW + 4;
        int fieldY = lastY + (HEIGHT - FIELD_H) / 2;
        int fieldW = lastW - labelW - 6;

        boolean inField = mx >= fieldX && mx <= fieldX + fieldW
                       && my >= fieldY && my <= fieldY + FIELD_H;

        if (inField) {
            isActive = true;
            buffer   = setting.get();
            return true;
        } else if (isActive) {
            // Click outside → commit and deactivate
            commit();
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!isActive) return false;
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (!buffer.isEmpty()) {
                buffer = buffer.substring(0, buffer.length() - 1);
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER
                || keyCode == GLFW.GLFW_KEY_ESCAPE) {
            commit();
            return true;
        }
        return false;
    }

    @Override
    public void charTyped(char chr, int mods) {
        if (!isActive) return;
        if (buffer.length() < setting.getMaxLength()) {
            buffer += chr;
        }
    }

    private void commit() {
        setting.set(buffer);
        ModConfig.getInstance().saveModuleSettings(module);
        isActive = false;
    }

    /** Deactivates the field without saving (e.g. when the screen closes). */
    public void deactivate() {
        isActive = false;
    }
}
