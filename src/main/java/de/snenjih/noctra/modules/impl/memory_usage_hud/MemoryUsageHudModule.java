package de.snenjih.noctra.modules.impl.memory_usage_hud;

import de.snenjih.noctra.modules.api.BaseHudModule;
import de.snenjih.noctra.modules.api.ModuleCategory;
import de.snenjih.noctra.modules.api.settings.BooleanSetting;
import de.snenjih.noctra.modules.api.settings.ColorSetting;
import de.snenjih.noctra.modules.api.settings.IntSetting;
import de.snenjih.noctra.modules.api.settings.ModuleSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

public class MemoryUsageHudModule extends BaseHudModule {

    private final ModuleSetting<Boolean> showBar;
    private final ModuleSetting<Boolean> showMax;
    private final ModuleSetting<Boolean> showPercentage;
    private final ModuleSetting<Integer> warnThreshold;
    private final ModuleSetting<Integer> critThreshold;
    private final ModuleSetting<Integer> unit;
    private final ModuleSetting<Integer> colorGood;
    private final ModuleSetting<Integer> colorWarn;
    private final ModuleSetting<Integer> colorCrit;

    public MemoryUsageHudModule() {
        super(
            "memory_usage_hud",
            "Memory Usage",
            "Shows JVM heap memory usage on the HUD.",
            ModuleCategory.VISUAL,
            Identifier.of("noctra", "modules/memory_usage_hud")
        );
        showBar        = addSetting(new BooleanSetting("show_bar",        "Show Bar",            true));
        showMax        = addSetting(new BooleanSetting("show_max",        "Show Max",            true));
        showPercentage = addSetting(new BooleanSetting("show_percentage", "Show Percent",        false));
        unit           = addSetting(new IntSetting    ("unit",            "Unit (0=MB 1=GB)",    0, 0, 1));
        beginSection("Thresholds");
        warnThreshold  = addSetting(new IntSetting    ("warn_threshold",  "Warn Threshold (%)",  80, 30, 99));
        critThreshold  = addSetting(new IntSetting    ("crit_threshold",  "Crit Threshold (%)",  95, 50, 100));
        beginSection("Colors");
        colorGood      = addSetting(new ColorSetting  ("color_good",      "Color Good",          0xFF55FF55));
        colorWarn      = addSetting(new ColorSetting  ("color_warn",      "Color Warn",          0xFFFFFF55));
        colorCrit      = addSetting(new ColorSetting  ("color_crit",      "Color Critical",      0xFFFF5555));
    }

    @Override public String getHudId()      { return "memory_usage_hud"; }
    @Override public String getHudName()    { return "Memory Usage"; }
    @Override public int getDefaultWidth()  { return 150; }
    @Override public int getDefaultHeight() { return 26; }

    @Override
    protected void renderHudContent(DrawContext ctx, float tickDelta, int x, int y, int w, int h) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        Runtime rt  = Runtime.getRuntime();
        long used   = (rt.totalMemory() - rt.freeMemory()) / (1024L * 1024L);
        long max    = rt.maxMemory() / (1024L * 1024L);

        // Guard: no -Xmx set
        if (max <= 0 || max == Long.MAX_VALUE / (1024L * 1024L)) {
            max = rt.totalMemory() / (1024L * 1024L);
        }
        if (max <= 0) return;

        double pct = (double) used / max * 100.0;

        int memColor;
        if (pct >= critThreshold.get()) memColor = colorCrit.get();
        else if (pct >= warnThreshold.get()) memColor = colorWarn.get();
        else memColor = colorGood.get();

        String usedStr = unit.get() == 1 ? String.format("%.1fGB", used / 1024.0) : used + "MB";
        String maxStr  = unit.get() == 1 ? String.format("%.1fGB", max  / 1024.0) : max  + "MB";

        String line;
        if (showPercentage.get()) {
            line = String.format("RAM: %.0f%%", pct);
        } else if (showMax.get()) {
            line = "RAM: " + usedStr + " / " + maxStr;
        } else {
            line = "RAM: " + usedStr;
        }

        int lineW = MinecraftClient.getInstance().textRenderer.getWidth(line);
        int totalW = Math.max(w, lineW + 8);
        boolean bar = showBar.get();
        int totalH = bar ? 26 : 18;

        drawBackground(ctx, x, y, totalW, totalH);
        ctx.drawTextWithShadow(mc.textRenderer, line, x + 4, y + (bar ? 5 : 5), memColor);

        if (bar) {
            int barX = x + 4;
            int barY = y + 16;
            int barW = totalW - 8;
            int fill = (int) (barW * Math.min(1.0, pct / 100.0));
            ctx.fill(barX, barY, barX + barW, barY + 4, 0xFF333333);
            if (fill > 0) ctx.fill(barX, barY, barX + fill, barY + 4, memColor);
        }
    }
}
