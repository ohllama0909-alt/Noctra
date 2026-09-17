package de.snenjih.noctra.modules.impl.durability_hud;

import de.snenjih.noctra.modules.api.BaseHudModule;
import de.snenjih.noctra.modules.api.ModuleCategory;
import de.snenjih.noctra.modules.api.settings.BooleanSetting;
import de.snenjih.noctra.modules.api.settings.ColorSetting;
import de.snenjih.noctra.modules.api.settings.FloatSetting;
import de.snenjih.noctra.modules.api.settings.ModuleSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class DurabilityHudModule extends BaseHudModule {

    private final ModuleSetting<Boolean> showBar;
    private final ModuleSetting<Boolean> showNumbers;
    private final ModuleSetting<Boolean> showPercent;
    private final ModuleSetting<Integer> colorFull;
    private final ModuleSetting<Integer> colorWarn;
    private final ModuleSetting<Integer> colorCrit;
    private final ModuleSetting<Float>   warnThreshold;
    private final ModuleSetting<Float>   critThreshold;
    private final ModuleSetting<Boolean> hideWhenFull;

    public DurabilityHudModule() {
        super(
            "durability_hud",
            "Durability HUD",
            "Shows the held item's durability as a large bar and number.",
            ModuleCategory.VISUAL,
            Identifier.of("noctra", "modules/durability_hud")
        );
        showBar       = addSetting(new BooleanSetting("show_bar",       "Show Bar",       true));
        showNumbers   = addSetting(new BooleanSetting("show_numbers",   "Show Numbers",   true));
        showPercent   = addSetting(new BooleanSetting("show_percent",   "Show Percent",   false));
        hideWhenFull  = addSetting(new BooleanSetting("hide_when_full", "Hide When Full", false));
        beginSection("Colors");
        colorFull     = addSetting(new ColorSetting("color_full",       "Color Full",     0xFF55FF55));
        colorWarn     = addSetting(new ColorSetting("color_warn",       "Color Warn",     0xFFFFAA00));
        colorCrit     = addSetting(new ColorSetting("color_crit",       "Color Critical", 0xFFFF5555));
        warnThreshold = addSetting(new FloatSetting("warn_threshold",   "Warn Threshold", 0.5f, 0.1f, 0.9f));
        critThreshold = addSetting(new FloatSetting("crit_threshold",   "Crit Threshold", 0.2f, 0.05f, 0.5f));
    }

    @Override public String getHudId()      { return "durability_hud"; }
    @Override public String getHudName()    { return "Durability HUD"; }
    @Override public int getDefaultWidth()  { return 100; }
    @Override public int getDefaultHeight() { return 30; }

    @Override
    protected void renderHudContent(DrawContext ctx, float tickDelta, int x, int y, int w, int h) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        ItemStack stack = mc.player.getMainHandStack();
        if (stack.isEmpty() || !stack.isDamageable()) return;

        int maxDmg    = stack.getMaxDamage();
        int remaining = maxDmg - stack.getDamage();
        float fraction = (float) remaining / maxDmg;

        if (hideWhenFull.get() && fraction >= 1.0f) return;

        int color;
        if (fraction <= critThreshold.get())      color = colorCrit.get();
        else if (fraction <= warnThreshold.get()) color = colorWarn.get();
        else                                      color = colorFull.get();

        drawBackground(ctx, x, y, w, h);

        int currentY = y + 2;
        int barW = w - 4;

        if (showBar.get()) {
            ctx.fill(x + 2, currentY, x + 2 + barW, currentY + 6, 0xFF222222);
            int fill = Math.round(fraction * barW);
            ctx.fill(x + 2, currentY, x + 2 + fill, currentY + 6, color);
            currentY += 9;
        }

        if (showNumbers.get()) {
            String text = remaining + " / " + maxDmg;
            ctx.drawTextWithShadow(mc.textRenderer, Text.literal(text), x + 2, currentY, color);
            currentY += 10;
        }

        if (showPercent.get()) {
            String pct = String.format("%.1f%%", fraction * 100);
            ctx.drawTextWithShadow(mc.textRenderer, Text.literal(pct), x + 2, currentY, color);
        }
    }
}
