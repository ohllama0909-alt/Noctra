package de.snenjih.noctra.modules.api;

import de.snenjih.noctra.modules.api.settings.ColorSetting;
import de.snenjih.noctra.modules.api.settings.FloatSetting;
import de.snenjih.noctra.modules.api.settings.ModuleSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

public abstract class BaseHudModule extends BaseModule implements HudElement {

    protected final ModuleSetting<Integer> bgColor;
    protected final ModuleSetting<Integer> textColor;
    protected final ModuleSetting<Float>   textScale;

    protected BaseHudModule(String id, String name, String desc,
                             ModuleCategory category, Identifier icon) {
        super(id, name, desc, category, icon);
        beginSection("Appearance");
        bgColor   = addSetting(new ColorSetting("bg_color",   "Background Color", 0xCC0D1B2A));
        textColor = addSetting(new ColorSetting("text_color", "Text Color",       0xFFFFFFFF));
        textScale = addSetting(new FloatSetting("text_scale", "Text Scale",       1.0f, 0.5f, 2.0f));
    }

    @Override
    public final void renderHud(DrawContext ctx, float tickDelta, int x, int y, int w, int h) {
        float scale = textScale.get();
        var ms = ctx.getMatrices();
        ms.pushMatrix();
        ms.scale(scale, scale);
        renderHudContent(ctx, tickDelta,
                (int)(x / scale), (int)(y / scale),
                (int)(w / scale), (int)(h / scale));
        ms.popMatrix();
    }

    protected abstract void renderHudContent(DrawContext ctx, float tickDelta,
                                              int x, int y, int w, int h);

    protected void drawBackground(DrawContext ctx, int x, int y, int w, int h) {
        ctx.fill(x, y, x + w, y + h, bgColor.get());
        ctx.drawStrokedRectangle(x, y, w, h, 0xFF1E3A5F);
    }
}
