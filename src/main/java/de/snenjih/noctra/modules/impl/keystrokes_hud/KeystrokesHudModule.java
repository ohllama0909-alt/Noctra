package de.snenjih.noctra.modules.impl.keystrokes_hud;

import de.snenjih.noctra.modules.api.BaseHudModule;
import de.snenjih.noctra.modules.api.ModuleCategory;
import de.snenjih.noctra.modules.api.settings.BooleanSetting;
import de.snenjih.noctra.modules.api.settings.ModuleSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

public class KeystrokesHudModule extends BaseHudModule {

    private final ModuleSetting<Boolean> showMouseButtons;
    private final ModuleSetting<Boolean> showSpacebar;

    public KeystrokesHudModule() {
        super(
            "keystrokes_hud",
            "Keystrokes",
            "Shows WASD and mouse button keystrokes on the HUD.",
            ModuleCategory.VISUAL,
            Identifier.of("noctra", "modules/keystrokes_hud")
        );
        showMouseButtons = addSetting(new BooleanSetting("show_mouse_buttons", "Show Mouse Buttons", true));
        showSpacebar     = addSetting(new BooleanSetting("show_spacebar",      "Show Spacebar",      true));
    }

    // ── HudElement ────────────────────────────────────────────────────────────

    @Override public String getHudId()      { return "keystrokes_hud"; }
    @Override public String getHudName()    { return "Keystrokes"; }
    @Override public int getDefaultWidth()  { return 70; }
    @Override public int getDefaultHeight() { return 55; }

    @Override
    protected void renderHudContent(DrawContext ctx, float tickDelta, int x, int y, int w, int h) {
        MinecraftClient mc = MinecraftClient.getInstance();

        boolean inScreen = mc.player == null || mc.currentScreen != null;

        boolean pressW     = !inScreen && mc.options.forwardKey.isPressed();
        boolean pressA     = !inScreen && mc.options.leftKey.isPressed();
        boolean pressS     = !inScreen && mc.options.backKey.isPressed();
        boolean pressD     = !inScreen && mc.options.rightKey.isPressed();
        boolean pressSpace = !inScreen && mc.options.jumpKey.isPressed();
        boolean pressLmb   = !inScreen && mc.options.attackKey.isPressed();
        boolean pressRmb   = !inScreen && mc.options.useKey.isPressed();

        var tr = mc.textRenderer;

        int keyW = 18;
        int keyH = 18;
        int gap  = 2;

        int rows = 2;
        if (showMouseButtons.get()) rows++;
        if (showSpacebar.get())     rows++;
        int totalH = rows * (keyH + gap) + gap;
        int totalW = 3 * (keyW + gap) + gap;

        drawBackground(ctx, x, y, totalW, totalH);

        int rowY0 = y + gap;
        drawKey(ctx, tr, "W", x + gap + (keyW + gap), rowY0, keyW, keyH, pressW);

        int rowY1 = rowY0 + keyH + gap;
        drawKey(ctx, tr, "A", x + gap,                     rowY1, keyW, keyH, pressA);
        drawKey(ctx, tr, "S", x + gap + (keyW + gap),       rowY1, keyW, keyH, pressS);
        drawKey(ctx, tr, "D", x + gap + 2 * (keyW + gap),   rowY1, keyW, keyH, pressD);

        int nextRowY = rowY1 + keyH + gap;

        if (showMouseButtons.get()) {
            int halfW = (totalW - 3 * gap) / 2;
            drawKeyRect(ctx, tr, "LMB", x + gap,               nextRowY, halfW, keyH, pressLmb);
            drawKeyRect(ctx, tr, "RMB", x + gap + halfW + gap,  nextRowY, halfW, keyH, pressRmb);
            nextRowY += keyH + gap;
        }

        if (showSpacebar.get()) {
            int spaceW = totalW - 2 * gap;
            drawKeyRect(ctx, tr, "SPACE", x + gap, nextRowY, spaceW, keyH, pressSpace);
        }
    }

    private void drawKey(DrawContext ctx, net.minecraft.client.font.TextRenderer tr,
                          String label, int kx, int ky, int kw, int kh, boolean pressed) {
        drawKeyRect(ctx, tr, label, kx, ky, kw, kh, pressed);
    }

    private void drawKeyRect(DrawContext ctx, net.minecraft.client.font.TextRenderer tr,
                              String label, int kx, int ky, int kw, int kh, boolean pressed) {
        int keyBgColor   = pressed ? 0xFFFFFFFF : 0xFF1A2A40;
        int keyTextColor = pressed ? 0xFF000000 : 0xFF8899AA;

        ctx.fill(kx, ky, kx + kw, ky + kh, keyBgColor);
        ctx.drawStrokedRectangle(kx, ky, kw, kh, 0xFF1E3A5F);

        int textX = kx + (kw - tr.getWidth(label)) / 2;
        int textY = ky + (kh - 8) / 2;
        ctx.drawTextWithShadow(tr, label, textX, textY, keyTextColor);
    }
}
