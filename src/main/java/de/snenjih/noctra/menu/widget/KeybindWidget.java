package de.snenjih.noctra.menu.widget;

import de.snenjih.noctra.config.ModConfig;
import de.snenjih.noctra.modules.api.BaseModule;
import de.snenjih.noctra.modules.api.settings.KeybindSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/** Renders a KeybindSetting as a label + clickable key button. */
public class KeybindWidget implements SettingsWidget {

    private static final int HEIGHT   = 18;
    private static final int BTN_W    = 80;
    private static final int BTN_H    = 12;
    private static final int COL_TEXT = 0xFFFFFFFF;
    private static final int COL_GRAY = 0xFF8899AA;
    private static final int COL_BG   = 0xFF1A2A40;
    private static final int COL_LISTEN = 0xFF4A1A1A;
    private static final int COL_BORDER = 0xFF1E3A5F;
    private static final int COL_BORDER_ACTIVE = 0xFFDD4444;

    private final KeybindSetting setting;
    private final BaseModule     module;

    private boolean isListening = false;
    private int lastX, lastY, lastW;

    public KeybindWidget(KeybindSetting setting, BaseModule module) {
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
        ctx.fill(btnX, btnY, btnX + BTN_W, btnY + BTN_H,
                 isListening ? COL_LISTEN : COL_BG);
        ctx.drawStrokedRectangle(btnX, btnY, BTN_W, BTN_H,
                 isListening ? COL_BORDER_ACTIVE : COL_BORDER);

        // Button text
        String label = isListening ? "[Press key...]" : getKeyName();
        int labelW   = tr.getWidth(label);
        int labelX   = btnX + (BTN_W - labelW) / 2;
        ctx.drawTextWithShadow(tr, label, labelX, btnY + 2,
                 isListening ? 0xFFFFAAAA : COL_GRAY);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return false;
        int btnX = lastX + lastW - BTN_W - 2;
        int btnY = lastY + (HEIGHT - BTN_H) / 2;
        if (mx >= btnX && mx <= btnX + BTN_W && my >= btnY && my <= btnY + BTN_H) {
            isListening = !isListening;
            return true;
        }
        if (isListening) {
            // Click outside → cancel listening
            isListening = false;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!isListening) return false;
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            setting.set(-1); // unbound
        } else {
            setting.set(keyCode);
        }
        ModConfig.getInstance().saveModuleSettings(module);
        isListening = false;
        return true;
    }

    private String getKeyName() {
        int key = setting.get();
        if (key == -1) return "Not bound";
        try {
            return InputUtil.Type.KEYSYM.createFromCode(key).getLocalizedText().getString();
        } catch (Exception e) {
            return "Key " + key;
        }
    }
}
