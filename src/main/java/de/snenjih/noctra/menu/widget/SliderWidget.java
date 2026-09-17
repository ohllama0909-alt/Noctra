package de.snenjih.noctra.menu.widget;

import de.snenjih.noctra.config.ModConfig;
import de.snenjih.noctra.modules.api.BaseModule;
import de.snenjih.noctra.modules.api.settings.FloatSetting;
import de.snenjih.noctra.modules.api.settings.IntSetting;
import de.snenjih.noctra.modules.api.settings.ModuleSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/**
 * Renders an IntSetting or FloatSetting as a label + slider track + value display.
 */
public class SliderWidget implements SettingsWidget {

    private static final int HEIGHT  = 18;
    private static final int TRACK_H = 4;
    private static final int KNOB_W  = 4;

    private final ModuleSetting<?> setting;
    private final BaseModule       module;
    private final float            min;
    private final float            max;
    private final boolean          isFloat;

    private boolean isDragging = false;
    private int lastX, lastY, lastW;

    public SliderWidget(IntSetting setting, BaseModule module) {
        this.setting = setting;
        this.module  = module;
        this.min     = setting.getMin();
        this.max     = setting.getMax();
        this.isFloat = false;
    }

    public SliderWidget(FloatSetting setting, BaseModule module) {
        this.setting = setting;
        this.module  = module;
        this.min     = setting.getMin();
        this.max     = setting.getMax();
        this.isFloat = true;
    }

    @Override
    public int getHeight() { return HEIGHT; }

    private float currentNormalized() {
        float val = (setting instanceof IntSetting is)
                ? is.get().floatValue()
                : ((FloatSetting) setting).get();
        if (max <= min) return 0f;
        return (val - min) / (max - min);
    }

    @SuppressWarnings("unchecked")
    private void setFromNormalized(float n) {
        n = Math.clamp(n, 0f, 1f);
        float val = min + n * (max - min);
        if (isFloat) {
            ((ModuleSetting<Float>) setting).set(val);
        } else {
            ((ModuleSetting<Integer>) setting).set(Math.round(val));
        }
        ModConfig.getInstance().saveModuleSettings(module);
    }

    @Override
    public void render(DrawContext ctx, int x, int y, int width, int mouseX, int mouseY) {
        lastX = x; lastY = y; lastW = width;
        var tr = MinecraftClient.getInstance().textRenderer;

        int labelW = (int) (width * 0.45f);
        int trackW = (int) (width * 0.38f);

        int trackX = x + labelW + 3;
        int trackY = y + (HEIGHT - TRACK_H) / 2;

        // Label
        ctx.drawTextWithShadow(tr, setting.getLabel(), x + 2, y + (HEIGHT - 8) / 2, 0xFFFFFFFF);

        // Track background
        ctx.fill(trackX, trackY, trackX + trackW, trackY + TRACK_H, 0xFF1A2A40);
        ctx.drawStrokedRectangle(trackX, trackY, trackW, TRACK_H, 0xFF1E3A5F);

        // Filled portion
        float norm    = currentNormalized();
        int   filledW = (int) (trackW * norm);
        if (filledW > 0) {
            ctx.fill(trackX, trackY, trackX + filledW, trackY + TRACK_H, 0xFF4A7CF8);
        }

        // Knob
        int knobX = trackX + filledW - KNOB_W / 2;
        ctx.fill(knobX, trackY - 2, knobX + KNOB_W, trackY + TRACK_H + 2, 0xFFCCDDFF);

        // Value text
        String valStr = isFloat
                ? String.format("%.1f", ((FloatSetting) setting).get())
                : String.valueOf(((IntSetting) setting).get());
        ctx.drawTextWithShadow(tr, valStr, trackX + trackW + 4, y + (HEIGHT - 8) / 2, 0xFF8899AA);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return false;
        int trackX = lastX + (int) (lastW * 0.45f) + 3;
        int trackW = (int) (lastW * 0.38f);
        if (mx >= trackX && mx <= trackX + trackW && my >= lastY && my <= lastY + HEIGHT) {
            isDragging = true;
            setFromNormalized((float) (mx - trackX) / trackW);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (!isDragging) return false;
        int trackX = lastX + (int) (lastW * 0.45f) + 3;
        int trackW = (int) (lastW * 0.38f);
        setFromNormalized((float) (mx - trackX) / trackW);
        return true;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        isDragging = false;
        return false;
    }
}
