package de.snenjih.noctra.modules.impl.scoreboard_hud;

import de.snenjih.noctra.modules.api.BaseHudModule;
import de.snenjih.noctra.modules.api.ModuleCategory;
import de.snenjih.noctra.modules.api.settings.BooleanSetting;
import de.snenjih.noctra.modules.api.settings.IntSetting;
import de.snenjih.noctra.modules.api.settings.ModuleSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public class ScoreboardHudModule extends BaseHudModule {

    public static ScoreboardHudModule INSTANCE = null;

    public final ModuleSetting<Integer> maxEntries;
    public final ModuleSetting<Boolean> hideVanilla;

    public ScoreboardHudModule() {
        super(
            "scoreboard_hud",
            "Scoreboard HUD",
            "Replaces the vanilla scoreboard sidebar with a customizable HUD element.",
            ModuleCategory.VISUAL,
            Identifier.of("noctra", "modules/scoreboard_hud")
        );
        maxEntries  = addSetting(new IntSetting("max_entries",   "Max Entries",  15, 1, 20));
        hideVanilla = addSetting(new BooleanSetting("hide_vanilla", "Hide Vanilla", true));
    }

    @Override public void onEnable()  { INSTANCE = this; }
    @Override public void onDisable() { INSTANCE = null; }

    @Override public String getHudId()      { return "scoreboard_hud"; }
    @Override public String getHudName()    { return "Scoreboard HUD"; }
    @Override public int getDefaultWidth()  { return 150; }
    @Override public int getDefaultHeight() { return 160; }

    @Override
    protected void renderHudContent(DrawContext ctx, float tickDelta, int x, int y, int w, int h) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;

        Scoreboard          scoreboard = mc.world.getScoreboard();
        ScoreboardObjective objective  = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        if (objective == null) return;

        Collection<ScoreboardEntry> entries = scoreboard.getScoreboardEntries(objective);
        if (entries == null || entries.isEmpty()) return;

        List<ScoreboardEntry> sorted = entries.stream()
            .sorted(Comparator.comparingInt(ScoreboardEntry::value).reversed())
            .limit(maxEntries.get())
            .toList();

        TextRenderer tr         = mc.textRenderer;
        int          lineHeight = 10;
        String       title      = objective.getDisplayName().getString();

        int maxW = tr.getWidth(title);
        for (ScoreboardEntry entry : sorted) {
            String line = entry.owner() + ": " + entry.value();
            maxW = Math.max(maxW, tr.getWidth(line));
        }

        drawBackground(ctx, x, y, Math.max(w, maxW + 8), (sorted.size() + 1) * lineHeight + 6);
        ctx.drawTextWithShadow(tr, Text.literal(title), x + 4, y + 4, 0xFFFF55);

        int i = 1;
        for (ScoreboardEntry entry : sorted) {
            int ey = y + 4 + i * lineHeight;
            String score = String.valueOf(entry.value());
            ctx.drawTextWithShadow(tr, Text.literal(entry.owner()), x + 4, ey, 0xFFFFFF);
            ctx.drawTextWithShadow(tr, Text.literal(score), x + maxW + 4 - tr.getWidth(score), ey, 0xFF5555);
            i++;
        }
    }
}
