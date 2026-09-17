package de.snenjih.noctra.menu.widget;

import de.snenjih.noctra.config.ModConfig;
import de.snenjih.noctra.modules.api.BaseModule;
import de.snenjih.noctra.modules.api.settings.ColorSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/**
 * Renders a ColorSetting as: color preview + 4 sliders (H/S/V/A) + hex input.
 * Total height: 80px.
 */
public class ColorPickerWidget implements SettingsWidget {

    private static final int HEIGHT      = 80;
    private static final int PREVIEW_W   = 20;
    private static final int SLIDER_H    = 8;
    private static final int TRACK_H     = 4;
    private static final int KNOB_W      = 4;
    private static final int ROW_GAP     = 3;
    private static final int LABEL_W_PX  = 10; // single-char label

    private final ColorSetting setting;
    private final BaseModule   module;

    // HSV + Alpha state (parsed from setting on construction)
    private float hue;        // 0-360
    private float saturation; // 0-1
    private float value;      // 0-1
    private float alpha;      // 0-1

    // Dragging state: which slider is active (-1 = none, 0=H, 1=S, 2=V, 3=A)
    private int draggingSlider = -1;

    // Hex input
    private boolean hexActive = false;
    private String  hexBuffer = "";

    private int lastX, lastY, lastW;

    public ColorPickerWidget(ColorSetting setting, BaseModule module) {
        this.setting = setting;
        this.module  = module;
        parseFromSetting();
    }

    @Override
    public int getHeight() { return HEIGHT; }

    // -------------------------------------------------------------------------
    // Parse / compose ARGB
    // -------------------------------------------------------------------------

    private void parseFromSetting() {
        int argb = setting.get();
        int a = (argb >> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >>  8) & 0xFF;
        int b =  argb        & 0xFF;

        float[] hsv = rgbToHsv(r, g, b);
        hue        = hsv[0];
        saturation = hsv[1];
        value      = hsv[2];
        alpha      = a / 255f;
    }

    private void commitToSetting() {
        int argb = hsvToArgb(hue, saturation, value, alpha);
        setting.set(argb);
        ModConfig.getInstance().saveModuleSettings(module);
    }

    // -------------------------------------------------------------------------
    // Render
    // -------------------------------------------------------------------------

    @Override
    public void render(DrawContext ctx, int x, int y, int width, int mouseX, int mouseY) {
        lastX = x; lastY = y; lastW = width;
        var tr = MinecraftClient.getInstance().textRenderer;

        // Label row at the top
        ctx.drawTextWithShadow(tr, setting.getLabel(), x + 2, y + 2, 0xFFFFFFFF);

        // Color preview square (right side, 20x20)
        int previewX = x + width - PREVIEW_W - 2;
        int argbCurrent = hsvToArgb(hue, saturation, value, alpha);
        ctx.fill(previewX, y + 2, previewX + PREVIEW_W, y + 22, argbCurrent);
        ctx.drawStrokedRectangle(previewX, y + 2, PREVIEW_W, 20, 0xFF4A7CF8);

        // Sliders area
        int sliderX = x + 12;
        int sliderW = width - PREVIEW_W - 18;
        int sy      = y + 14;

        renderSlider(ctx, sliderX, sy, sliderW, 0, "H"); sy += SLIDER_H + ROW_GAP;
        renderSlider(ctx, sliderX, sy, sliderW, 1, "S"); sy += SLIDER_H + ROW_GAP;
        renderSlider(ctx, sliderX, sy, sliderW, 2, "V"); sy += SLIDER_H + ROW_GAP;
        renderSlider(ctx, sliderX, sy, sliderW, 3, "A"); sy += SLIDER_H + ROW_GAP;

        // Hex input row
        String hex = String.format("#%08X", argbCurrent);
        int hexBgX = sliderX;
        int hexBgW = sliderW;
        ctx.fill(hexBgX, sy, hexBgX + hexBgW, sy + SLIDER_H + 2, 0xFF0D1B2A);
        ctx.drawStrokedRectangle(hexBgX, sy, hexBgW, SLIDER_H + 2,
                hexActive ? 0xFF4A7CF8 : 0xFF1E3A5F);
        String hexDisplay = hexActive ? (hexBuffer + (System.currentTimeMillis() % 1000 < 500 ? "|" : "")) : hex;
        ctx.drawTextWithShadow(tr, hexDisplay, hexBgX + 2, sy + 1, hexActive ? 0xFFFFFFFF : 0xFF8899AA);
    }

    private void renderSlider(DrawContext ctx, int x, int y, int width, int sliderIdx, String label) {
        var tr = MinecraftClient.getInstance().textRenderer;
        // Single-char label
        ctx.drawTextWithShadow(tr, label, x - 8, y, 0xFF8899AA);

        int trackX = x;
        int trackY = y + (SLIDER_H - TRACK_H) / 2;

        // Draw rainbow for H slider, otherwise flat
        if (sliderIdx == 0) {
            renderHueTrack(ctx, trackX, trackY, width, TRACK_H);
        } else {
            ctx.fill(trackX, trackY, trackX + width, trackY + TRACK_H, 0xFF1A2A40);
            ctx.drawStrokedRectangle(trackX, trackY, width, TRACK_H, 0xFF1E3A5F);
            // Fill bar
            float norm = getNorm(sliderIdx);
            int filled = (int)(width * norm);
            int fillColor = sliderIdx == 3 ? 0xFFCCCCCC : 0xFF4A7CF8;
            if (filled > 0) ctx.fill(trackX, trackY, trackX + filled, trackY + TRACK_H, fillColor);
        }

        // Knob
        float norm  = getNorm(sliderIdx);
        int   knobX = trackX + (int)(width * norm) - KNOB_W / 2;
        ctx.fill(knobX, trackY - 2, knobX + KNOB_W, trackY + TRACK_H + 2, 0xFFCCDDFF);
    }

    /** Draw 6-segment hue rainbow */
    private void renderHueTrack(DrawContext ctx, int x, int y, int w, int h) {
        int segW = w / 6;
        int[][] stops = {
            {0xFF0000, 0xFFFF00}, // red→yellow
            {0xFFFF00, 0x00FF00}, // yellow→green
            {0x00FF00, 0x00FFFF}, // green→cyan
            {0x00FFFF, 0x0000FF}, // cyan→blue
            {0x0000FF, 0xFF00FF}, // blue→magenta
            {0xFF00FF, 0xFF0000}, // magenta→red
        };
        for (int i = 0; i < 6; i++) {
            int sx = x + i * segW;
            int ex = (i == 5) ? x + w : sx + segW;
            // Approximate gradient with two fills
            int c1 = 0xFF000000 | stops[i][0];
            int c2 = 0xFF000000 | stops[i][1];
            // Simple: fill with midpoint color
            int mid = blendColors(c1, c2, 0.5f);
            ctx.fill(sx, y, ex, y + h, mid);
        }
        ctx.drawStrokedRectangle(x, y, w, h, 0xFF1E3A5F);
    }

    private int blendColors(int c1, int c2, float t) {
        int r1 = (c1 >> 16) & 0xFF; int g1 = (c1 >> 8) & 0xFF; int b1 = c1 & 0xFF;
        int r2 = (c2 >> 16) & 0xFF; int g2 = (c2 >> 8) & 0xFF; int b2 = c2 & 0xFF;
        int r  = (int)(r1 + (r2 - r1) * t);
        int g  = (int)(g1 + (g2 - g1) * t);
        int b  = (int)(b1 + (b2 - b1) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private float getNorm(int slider) {
        return switch (slider) {
            case 0 -> hue / 360f;
            case 1 -> saturation;
            case 2 -> value;
            case 3 -> alpha;
            default -> 0f;
        };
    }

    private void setNorm(int slider, float n) {
        n = Math.clamp(n, 0f, 1f);
        switch (slider) {
            case 0 -> hue        = n * 360f;
            case 1 -> saturation = n;
            case 2 -> value      = n;
            case 3 -> alpha      = n;
        }
    }

    // -------------------------------------------------------------------------
    // Mouse
    // -------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return false;
        int sliderIdx = hitTestSlider(mx, my);
        if (sliderIdx >= 0) {
            draggingSlider = sliderIdx;
            applyDrag(sliderIdx, mx);
            hexActive = false;
            return true;
        }
        // Hex input
        int[] hexBounds = hexInputBounds();
        if (mx >= hexBounds[0] && mx <= hexBounds[0] + hexBounds[2]
         && my >= hexBounds[1] && my <= hexBounds[1] + hexBounds[3]) {
            hexActive = true;
            hexBuffer = String.format("%08X", hsvToArgb(hue, saturation, value, alpha));
            return true;
        }
        if (hexActive) { commitHex(); }
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (draggingSlider < 0) return false;
        applyDrag(draggingSlider, mx);
        return true;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        draggingSlider = -1;
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!hexActive) return false;
        if (keyCode == 259 /* BACKSPACE */) {
            if (!hexBuffer.isEmpty()) hexBuffer = hexBuffer.substring(0, hexBuffer.length() - 1);
            return true;
        }
        if (keyCode == 257 /* ENTER */ || keyCode == 335 /* KP_ENTER */ || keyCode == 256 /* ESCAPE */) {
            commitHex();
            return true;
        }
        return false;
    }

    @Override
    public void charTyped(char chr, int mods) {
        if (!hexActive) return;
        if ((hexBuffer.startsWith("#") ? hexBuffer.length() - 1 : hexBuffer.length()) < 8) {
            char c = Character.toUpperCase(chr);
            if ((c >= '0' && c <= '9') || (c >= 'A' && c <= 'F')) {
                hexBuffer += c;
            }
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private int hitTestSlider(double mx, double my) {
        int sliderX = lastX + 12;
        int sliderW = lastW - PREVIEW_W - 18;
        int sy      = lastY + 14;
        for (int i = 0; i < 4; i++) {
            if (mx >= sliderX && mx <= sliderX + sliderW
             && my >= sy && my <= sy + SLIDER_H) return i;
            sy += SLIDER_H + ROW_GAP;
        }
        return -1;
    }

    private int[] hexInputBounds() {
        int sliderX = lastX + 12;
        int sliderW = lastW - PREVIEW_W - 18;
        int sy      = lastY + 14 + 4 * (SLIDER_H + ROW_GAP);
        return new int[]{sliderX, sy, sliderW, SLIDER_H + 2};
    }

    private void applyDrag(int sliderIdx, double mx) {
        int sliderX = lastX + 12;
        int sliderW = lastW - PREVIEW_W - 18;
        float n = (float)(mx - sliderX) / sliderW;
        setNorm(sliderIdx, n);
        commitToSetting();
    }

    private void commitHex() {
        hexActive = false;
        try {
            String h = hexBuffer.startsWith("#") ? hexBuffer.substring(1) : hexBuffer;
            if (h.length() == 6)  h = "FF" + h;
            if (h.length() == 8) {
                int argb = (int) Long.parseLong(h, 16);
                int a = (argb >> 24) & 0xFF;
                int r = (argb >> 16) & 0xFF;
                int g = (argb >>  8) & 0xFF;
                int b =  argb        & 0xFF;
                float[] hsv = rgbToHsv(r, g, b);
                hue = hsv[0]; saturation = hsv[1]; value = hsv[2]; alpha = a / 255f;
                commitToSetting();
            }
        } catch (NumberFormatException ignored) {}
    }

    // -------------------------------------------------------------------------
    // Color math
    // -------------------------------------------------------------------------

    private static int hsvToArgb(float h, float s, float v, float a) {
        int ai = Math.clamp((int)(a * 255), 0, 255);
        if (s == 0f) {
            int grey = (int)(v * 255);
            return (ai << 24) | (grey << 16) | (grey << 8) | grey;
        }
        float hh = (h % 360f) / 60f;
        int   i  = (int) hh;
        float f  = hh - i;
        float p  = v * (1f - s);
        float q  = v * (1f - s * f);
        float t  = v * (1f - s * (1f - f));
        float r, g, b;
        switch (i) {
            case 0 -> { r = v; g = t; b = p; }
            case 1 -> { r = q; g = v; b = p; }
            case 2 -> { r = p; g = v; b = t; }
            case 3 -> { r = p; g = q; b = v; }
            case 4 -> { r = t; g = p; b = v; }
            default -> { r = v; g = p; b = q; }
        }
        int ri = Math.clamp((int)(r * 255), 0, 255);
        int gi = Math.clamp((int)(g * 255), 0, 255);
        int bi = Math.clamp((int)(b * 255), 0, 255);
        return (ai << 24) | (ri << 16) | (gi << 8) | bi;
    }

    private static float[] rgbToHsv(int r, int g, int b) {
        float rf = r / 255f, gf = g / 255f, bf = b / 255f;
        float max = Math.max(rf, Math.max(gf, bf));
        float min = Math.min(rf, Math.min(gf, bf));
        float delta = max - min;
        float h = 0f, s, v = max;
        s = (max == 0f) ? 0f : delta / max;
        if (delta != 0f) {
            if (max == rf)      h = 60f * (((gf - bf) / delta) % 6f);
            else if (max == gf) h = 60f * (((bf - rf) / delta) + 2f);
            else                h = 60f * (((rf - gf) / delta) + 4f);
        }
        if (h < 0f) h += 360f;
        return new float[]{h, s, v};
    }
}
