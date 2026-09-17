package de.snenjih.noctra.modules.impl.speedometer;

import de.snenjih.noctra.modules.api.BaseHudModule;
import de.snenjih.noctra.modules.api.ModuleCategory;
import de.snenjih.noctra.modules.api.settings.BooleanSetting;
import de.snenjih.noctra.modules.api.settings.ColorSetting;
import de.snenjih.noctra.modules.api.settings.FloatSetting;
import de.snenjih.noctra.modules.api.settings.IntSetting;
import de.snenjih.noctra.modules.api.settings.ModuleSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

public class SpeedometerModule extends BaseHudModule {

    private final ModuleSetting<Boolean> showVertical;
    private final ModuleSetting<Boolean> showMax;
    private final ModuleSetting<Boolean> showBar;
    private final ModuleSetting<Float>   barMaxSpeed;
    private final ModuleSetting<Integer> decimalPlaces;
    private final ModuleSetting<Float>   smoothing;
    private final ModuleSetting<Boolean> hideWhenStill;
    private final ModuleSetting<Boolean> showReference;
    private final ModuleSetting<Integer> colorSlow;
    private final ModuleSetting<Integer> colorWalk;
    private final ModuleSetting<Integer> colorSprint;
    private final ModuleSetting<Integer> colorFast;

    private double smoothedHorizSpeed = 0.0;
    private double smoothedVertSpeed  = 0.0;
    private double maxSpeedSession    = 0.0;

    public SpeedometerModule() {
        super(
            "speedometer",
            "Speedometer",
            "Shows your current movement speed in blocks per second.",
            ModuleCategory.VISUAL,
            Identifier.of("noctra", "modules/speedometer")
        );
        showVertical  = addSetting(new BooleanSetting("show_vertical",  "Show Vertical Speed", false));
        showMax       = addSetting(new BooleanSetting("show_max",        "Show Max Speed",      false));
        showBar       = addSetting(new BooleanSetting("show_bar",        "Show Bar",            false));
        barMaxSpeed   = addSetting(new FloatSetting  ("bar_max_speed",   "Bar Max (BPS)",       30.0f, 5.0f, 200.0f));
        decimalPlaces = addSetting(new IntSetting    ("decimal_places",  "Decimal Places",      1, 0, 2));
        smoothing     = addSetting(new FloatSetting  ("smoothing",       "Smoothing",           0.8f, 0.0f, 0.99f));
        hideWhenStill = addSetting(new BooleanSetting("hide_when_still", "Hide When Still",     false));
        showReference = addSetting(new BooleanSetting("show_reference",  "Show Reference",      false));
        beginSection("Colors");
        colorSlow   = addSetting(new ColorSetting("color_slow",   "Color Slow (<2 BPS)",   0xFF8899AA));
        colorWalk   = addSetting(new ColorSetting("color_walk",   "Color Walk (2-5 BPS)",  0xFFFFFFFF));
        colorSprint = addSetting(new ColorSetting("color_sprint", "Color Sprint (5-8 BPS)",0xFF55FF55));
        colorFast   = addSetting(new ColorSetting("color_fast",   "Color Fast (>8 BPS)",   0xFFFFAA00));
    }

    @Override public String getHudId()       { return "speedometer"; }
    @Override public String getHudName()     { return "Speedometer"; }
    @Override public int getDefaultWidth()   { return 140; }
    @Override public int getDefaultHeight()  { return 28; }

    @Override
    public void onLeaveWorld() {
        maxSpeedSession    = 0.0;
        smoothedHorizSpeed = 0.0;
        smoothedVertSpeed  = 0.0;
    }

    @Override
    protected void renderHudContent(DrawContext ctx, float tickDelta, int x, int y, int w, int h) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        Vec3d vel = mc.player.getVelocity();

        double rawHoriz = Math.min(Math.sqrt(vel.x * vel.x + vel.z * vel.z) * 20.0, 1000.0);
        double rawVert  = vel.y * 20.0;

        float s = smoothing.get();
        float alpha = 1.0f - s;
        smoothedHorizSpeed = smoothedHorizSpeed * s + rawHoriz * alpha;
        smoothedVertSpeed  = smoothedVertSpeed  * s + rawVert  * alpha;

        maxSpeedSession = Math.max(maxSpeedSession, rawHoriz);

        if (hideWhenStill.get() && smoothedHorizSpeed < 0.1) return;

        int color;
        if      (smoothedHorizSpeed < 2.0) color = colorSlow.get();
        else if (smoothedHorizSpeed < 5.0) color = colorWalk.get();
        else if (smoothedHorizSpeed < 8.0) color = colorSprint.get();
        else                               color = colorFast.get();

        String fmt = "%." + decimalPlaces.get() + "f";
        String speedStr = "Speed: " + String.format(fmt, smoothedHorizSpeed) + " BPS";

        var tr = mc.textRenderer;
        int neededW = Math.max(w, tr.getWidth(speedStr) + 8);
        drawBackground(ctx, x, y, neededW, h);

        int lineY = y + 4;
        ctx.drawTextWithShadow(tr, speedStr, x + 4, lineY, color);
        lineY += 10;

        if (showVertical.get()) {
            String sign   = smoothedVertSpeed > 0 ? "+" : "";
            String vertStr = "Vert: " + sign + String.format(fmt, smoothedVertSpeed) + " BPS";
            ctx.drawTextWithShadow(tr, vertStr, x + 4, lineY, 0xFF8899AA);
            lineY += 10;
        }

        if (showMax.get()) {
            String maxStr = "Max: " + String.format(fmt, maxSpeedSession);
            ctx.drawTextWithShadow(tr, maxStr, x + 4, lineY, colorFast.get());
            lineY += 10;
        }

        if (showBar.get()) {
            int bx   = x + 4;
            int bw   = neededW - 8;
            float ratio = (float) Math.min(1.0, smoothedHorizSpeed / barMaxSpeed.get());
            int fill = (int) (bw * ratio);
            ctx.fill(bx, lineY, bx + bw, lineY + 4, 0xFF333333);
            if (fill > 0) ctx.fill(bx, lineY, bx + fill, lineY + 4, color);
            lineY += 8;
        }

        if (showReference.get()) {
            ctx.drawTextWithShadow(tr, "Walk:4.3  Sprint:5.6", x + 4, lineY, 0xFF556677);
        }
    }
}
