package de.snenjih.noctra.modules.impl.xp_level_hud;

import de.snenjih.noctra.modules.api.BaseHudModule;
import de.snenjih.noctra.modules.api.ModuleCategory;
import de.snenjih.noctra.modules.api.settings.BooleanSetting;
import de.snenjih.noctra.modules.api.settings.ColorSetting;
import de.snenjih.noctra.modules.api.settings.IntSetting;
import de.snenjih.noctra.modules.api.settings.ModuleSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

public class XpLevelHudModule extends BaseHudModule {

    private final ModuleSetting<Boolean> showBar;
    private final ModuleSetting<Boolean> showRawXp;
    private final ModuleSetting<Boolean> showXpToNext;
    private final ModuleSetting<Integer> targetLevel;
    private final ModuleSetting<Boolean> showTargetXp;
    private final ModuleSetting<Boolean> compactMode;
    private final ModuleSetting<Integer> xpBarColor;

    public XpLevelHudModule() {
        super(
            "xp_level_hud",
            "XP & Level",
            "Shows your experience level and XP progress in detail.",
            ModuleCategory.VISUAL,
            Identifier.of("noctra", "modules/xp_level_hud")
        );
        showBar      = addSetting(new BooleanSetting("show_bar",       "Show Bar",          true));
        showXpToNext = addSetting(new BooleanSetting("show_xp_to_next","Show XP to Next",   true));
        showRawXp    = addSetting(new BooleanSetting("show_raw_xp",    "Show Raw XP",       false));
        showTargetXp = addSetting(new BooleanSetting("show_target_xp", "Show Target XP",    false));
        targetLevel  = addSetting(new IntSetting    ("target_level",   "Target Level",      30, 0, 100));
        compactMode  = addSetting(new BooleanSetting("compact_mode",   "Compact Mode",      false));
        beginSection("Colors");
        xpBarColor   = addSetting(new ColorSetting  ("xp_bar_color",   "XP Bar Color",      0xFF7FFF00));
    }

    @Override public String getHudId()      { return "xp_level_hud"; }
    @Override public String getHudName()    { return "XP & Level"; }
    @Override public int getDefaultWidth()  { return 150; }
    @Override public int getDefaultHeight() { return 40; }

    private static int xpToNextLevel(int level) {
        if (level <= 15) return 2 * level + 7;
        if (level <= 30) return 5 * level - 38;
        return 9 * level - 158;
    }

    private static int totalXpForLevel(int level) {
        if (level <= 16) return level * level + 6 * level;
        if (level <= 31) return (int) (2.5 * level * level - 40.5 * level + 360);
        return (int) (4.5 * level * level - 162.5 * level + 2220);
    }

    @Override
    protected void renderHudContent(DrawContext ctx, float tickDelta, int x, int y, int w, int h) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        int   level    = mc.player.experienceLevel;
        float progress = Math.max(0f, Math.min(1f, mc.player.experienceProgress));
        int   toNext   = xpToNextLevel(level);
        int   xpInLvl  = Math.round(progress * toNext);
        int   totalXp  = totalXpForLevel(level) + xpInLvl;

        int xpNeeded = 0;
        if (showTargetXp.get() && targetLevel.get() > level) {
            xpNeeded = Math.max(0, totalXpForLevel(targetLevel.get()) - totalXp);
        }

        var tr = mc.textRenderer;

        if (compactMode.get()) {
            String line = "Lv " + level + "  " + xpInLvl + "/" + toNext;
            if (showRawXp.get()) line += "  [" + totalXp + " XP]";
            int lineW = Math.max(w, tr.getWidth(line) + 8);
            int boxH  = showBar.get() ? 26 : 18;
            drawBackground(ctx, x, y, lineW, boxH);
            ctx.drawTextWithShadow(tr, line, x + 4, y + 5, 0xFF7FFF00);
            if (showBar.get()) drawXpBar(ctx, x, y + 18, lineW, progress);
            return;
        }

        java.util.List<String>  lines  = new java.util.ArrayList<>();
        java.util.List<Integer> colors = new java.util.ArrayList<>();
        lines.add("Level: " + level);                         colors.add(0xFF7FFF00);
        if (showXpToNext.get()) { lines.add("XP: " + xpInLvl + " / " + toNext); colors.add(textColor.get()); }
        if (showRawXp.get())    { lines.add("Total: " + totalXp);                colors.add(0xFF99CC55); }
        if (showTargetXp.get() && targetLevel.get() > level) {
            lines.add("To Lv " + targetLevel.get() + ": " + xpNeeded);
            colors.add(0xFFAA8800);
        }

        int totalH = 8 + lines.size() * 10 + (showBar.get() ? 8 : 0);
        int maxW = w;
        for (String l : lines) maxW = Math.max(maxW, tr.getWidth(l) + 8);
        drawBackground(ctx, x, y, maxW, totalH);

        int lineY = y + 4;
        for (int i = 0; i < lines.size(); i++) {
            ctx.drawTextWithShadow(tr, lines.get(i), x + 4, lineY, colors.get(i));
            lineY += 10;
        }
        if (showBar.get()) drawXpBar(ctx, x, lineY + 2, maxW, progress);
    }

    private void drawXpBar(DrawContext ctx, int x, int barY, int barW, float progress) {
        int bx = x + 4;
        int bw = barW - 8;
        int fill = (int) (bw * progress);
        ctx.fill(bx, barY, bx + bw, barY + 4, 0xFF333333);
        if (fill > 0) ctx.fill(bx, barY, bx + fill, barY + 4, xpBarColor.get());
    }
}
