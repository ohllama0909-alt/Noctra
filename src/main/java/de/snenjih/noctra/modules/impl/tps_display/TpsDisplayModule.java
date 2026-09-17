package de.snenjih.noctra.modules.impl.tps_display;

import de.snenjih.noctra.modules.api.BaseHudModule;
import de.snenjih.noctra.modules.api.ModuleCategory;
import de.snenjih.noctra.modules.api.settings.BooleanSetting;
import de.snenjih.noctra.modules.api.settings.ColorSetting;
import de.snenjih.noctra.modules.api.settings.IntSetting;
import de.snenjih.noctra.modules.api.settings.ModuleSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

public class TpsDisplayModule extends BaseHudModule {

    private static final int SAMPLE_INTERVAL = 20;

    private final ModuleSetting<Boolean> showBar;
    private final ModuleSetting<Integer> goodThreshold;
    private final ModuleSetting<Integer> badThreshold;
    private final ModuleSetting<Integer> colorGood;
    private final ModuleSetting<Integer> colorWarn;
    private final ModuleSetting<Integer> colorBad;
    private final ModuleSetting<Boolean> showMspt;

    private long lastRealTime  = 0L;
    private long lastWorldTime = 0L;
    private double estimatedTps = 20.0;
    private int ticksSinceUpdate = 0;

    public TpsDisplayModule() {
        super(
            "tps_display",
            "TPS Display",
            "Estimates and shows server ticks per second.",
            ModuleCategory.VISUAL,
            Identifier.of("noctra", "modules/tps_display")
        );
        showBar       = addSetting(new BooleanSetting("show_bar",        "Show Bar",      true));
        showMspt      = addSetting(new BooleanSetting("show_mspt",       "Show MSPT",     false));
        beginSection("Thresholds");
        goodThreshold = addSetting(new IntSetting    ("good_threshold",   "Good TPS (≥)", 18, 10, 20));
        badThreshold  = addSetting(new IntSetting    ("bad_threshold",    "Bad TPS (<)",  15,  5, 19));
        beginSection("Colors");
        colorGood     = addSetting(new ColorSetting  ("color_good",       "Color Good",   0xFF55FF55));
        colorWarn     = addSetting(new ColorSetting  ("color_warn",       "Color Warn",   0xFFFFFF55));
        colorBad      = addSetting(new ColorSetting  ("color_bad",        "Color Bad",    0xFFFF5555));
    }

    @Override public String getHudId()      { return "tps_display"; }
    @Override public String getHudName()    { return "TPS Display"; }
    @Override public int getDefaultWidth()  { return 110; }
    @Override public int getDefaultHeight() { return 26; }

    @Override
    public void onClientTick(MinecraftClient mc) {
        if (mc.world == null) return;

        ticksSinceUpdate++;
        if (ticksSinceUpdate < SAMPLE_INTERVAL) return;
        ticksSinceUpdate = 0;

        long nowReal  = System.currentTimeMillis();
        long nowWorld = mc.world.getTime();

        if (lastRealTime != 0 && nowWorld > lastWorldTime) {
            long realDelta  = nowReal  - lastRealTime;
            long worldDelta = nowWorld - lastWorldTime;

            if (realDelta <= 0 || worldDelta == 0) {
                lastRealTime  = nowReal;
                lastWorldTime = nowWorld;
                return;
            }

            double rawTps = worldDelta / (realDelta / 1000.0);
            estimatedTps  = Math.min(20.0, rawTps);
        }

        lastRealTime  = nowReal;
        lastWorldTime = nowWorld;
    }

    @Override
    protected void renderHudContent(DrawContext ctx, float tickDelta, int x, int y, int w, int h) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;

        var tr = mc.textRenderer;

        int tpsColor;
        if (estimatedTps >= goodThreshold.get()) tpsColor = colorGood.get();
        else if (estimatedTps < badThreshold.get()) tpsColor = colorBad.get();
        else tpsColor = colorWarn.get();

        int tpsRounded = (int) Math.round(estimatedTps);
        String tpsText = "TPS: " + tpsRounded;

        boolean bar = showBar.get();
        int totalH = bar ? 26 : 18;
        drawBackground(ctx, x, y, w, totalH);

        int ty = y + (bar ? 5 : (totalH - 8) / 2);
        ctx.drawTextWithShadow(tr, tpsText, x + 4, ty, tpsColor);

        if (showMspt.get()) {
            double mspt = 1000.0 / Math.max(1, estimatedTps);
            String msptText = String.format("  %.1fms", mspt);
            ctx.drawTextWithShadow(tr, msptText, x + 4 + tr.getWidth(tpsText), ty, textColor.get());
        }

        if (bar) {
            int barX = x + 4;
            int barY = ty + 12;
            int barW = w - 8;
            int fill = (int) (barW * Math.min(1.0, estimatedTps / 20.0));
            ctx.fill(barX, barY, barX + barW, barY + 4, 0xFF333333);
            if (fill > 0) ctx.fill(barX, barY, barX + fill, barY + 4, tpsColor);
        }
    }
}
