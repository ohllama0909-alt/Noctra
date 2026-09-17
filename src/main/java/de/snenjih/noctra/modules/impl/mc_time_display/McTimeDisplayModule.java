package de.snenjih.noctra.modules.impl.mc_time_display;

import de.snenjih.noctra.modules.api.BaseHudModule;
import de.snenjih.noctra.modules.api.ModuleCategory;
import de.snenjih.noctra.modules.api.settings.BooleanSetting;
import de.snenjih.noctra.modules.api.settings.EnumSetting;
import de.snenjih.noctra.modules.api.settings.ModuleSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class McTimeDisplayModule extends BaseHudModule {

    public enum TimeFormat { HOURS_24, HOURS_12 }

    private final ModuleSetting<Boolean>    showDayNumber;
    private final ModuleSetting<Boolean>    showPhase;
    private final ModuleSetting<TimeFormat> timeFormat;

    public McTimeDisplayModule() {
        super(
            "mc_time_display",
            "MC Time Display",
            "Shows Minecraft world time and day phase on the HUD.",
            ModuleCategory.VISUAL,
            Identifier.of("noctra", "modules/mc_time_display")
        );
        showDayNumber = addSetting(new BooleanSetting("show_day_number", "Show Day Number", true));
        showPhase     = addSetting(new BooleanSetting("show_phase",      "Show Phase",      true));
        timeFormat    = addSetting(new EnumSetting<>("time_format", "Time Format", TimeFormat.HOURS_24, TimeFormat.class));
    }

    @Override public String getHudId()      { return "mc_time_display"; }
    @Override public String getHudName()    { return "MC Time Display"; }
    @Override public int getDefaultWidth()  { return 120; }
    @Override public int getDefaultHeight() { return 32; }

    @Override
    protected void renderHudContent(DrawContext ctx, float tickDelta, int x, int y, int w, int h) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;

        long timeOfDay  = mc.world.getTimeOfDay();
        long dayTime    = Math.floorMod(timeOfDay, 24000L);
        long dayNumber  = timeOfDay / 24000L + 1;

        int hour   = (int) ((dayTime / 1000L + 6L) % 24L);
        int minute = (int) ((dayTime % 1000L) * 60L / 1000L);

        String timeStr;
        if (timeFormat.get() == TimeFormat.HOURS_24) {
            timeStr = String.format("%02d:%02d", hour, minute);
        } else {
            int h12 = hour % 12;
            if (h12 == 0) h12 = 12;
            timeStr = String.format("%d:%02d %s", h12, minute, hour < 12 ? "AM" : "PM");
        }

        int phaseColor;
        String phase;
        if (dayTime < 12000)      { phase = "Day";   phaseColor = 0xFFFF55; }
        else if (dayTime < 13800) { phase = "Dusk";  phaseColor = 0xFF8800; }
        else if (dayTime < 22200) { phase = "Night"; phaseColor = 0x5588FF; }
        else                      { phase = "Dawn";  phaseColor = 0xFF88AA; }

        drawBackground(ctx, x, y, w, h);
        ctx.drawTextWithShadow(mc.textRenderer, Text.literal(timeStr), x + 4, y + 4, phaseColor);

        if (showPhase.get()) {
            String phaseLabel = "[" + phase + "]";
            int phaseX = x + 4 + mc.textRenderer.getWidth(timeStr) + 4;
            ctx.drawTextWithShadow(mc.textRenderer, Text.literal(phaseLabel), phaseX, y + 4, phaseColor);
        }

        if (showDayNumber.get()) {
            ctx.drawTextWithShadow(mc.textRenderer, Text.literal("Day " + dayNumber), x + 4, y + 14, 0xAAAAAA);
        }
    }
}
