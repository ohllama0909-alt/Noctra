package de.snenjih.noctra.modules.impl.day_counter;

import de.snenjih.noctra.modules.api.BaseHudModule;
import de.snenjih.noctra.modules.api.ModuleCategory;
import de.snenjih.noctra.modules.api.settings.BooleanSetting;
import de.snenjih.noctra.modules.api.settings.ColorSetting;
import de.snenjih.noctra.modules.api.settings.ModuleSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class DayCounterModule extends BaseHudModule {

    private static final String[] PHASE_NAMES = {
        "Full Moon", "Waning Gibbous", "Last Quarter", "Waning Crescent",
        "New Moon", "Waxing Crescent", "First Quarter", "Waxing Gibbous"
    };

    private final ModuleSetting<Boolean> showTimeOfDay;
    private final ModuleSetting<Boolean> showMoonPhase;
    private final ModuleSetting<Boolean> showNightWarning;
    private final ModuleSetting<Boolean> compactMode;
    private final ModuleSetting<Boolean> timeFormat;
    private final ModuleSetting<Boolean> moonPhaseNames;
    private final ModuleSetting<Integer> colorDay;
    private final ModuleSetting<Integer> colorNight;

    public DayCounterModule() {
        super(
            "day_counter",
            "Day Counter",
            "Shows the current Minecraft day and time of day.",
            ModuleCategory.VISUAL,
            Identifier.of("noctra", "modules/day_counter")
        );
        showTimeOfDay    = addSetting(new BooleanSetting("show_time_of_day",   "Show Time of Day",   true));
        showMoonPhase    = addSetting(new BooleanSetting("show_moon_phase",    "Show Moon Phase",    false));
        showNightWarning = addSetting(new BooleanSetting("show_night_warning", "Show Night Warning", true));
        compactMode      = addSetting(new BooleanSetting("compact_mode",       "Compact Mode",       false));
        timeFormat       = addSetting(new BooleanSetting("time_format",        "24h Format",         true));
        moonPhaseNames   = addSetting(new BooleanSetting("moon_phase_names",   "Moon Phase Names",   true));
        beginSection("Colors");
        colorDay         = addSetting(new ColorSetting  ("color_day",          "Day Color",          0xFFFFFF55));
        colorNight       = addSetting(new ColorSetting  ("color_night",        "Night Color",        0xFF8899CC));
    }

    @Override public String getHudId()      { return "day_counter"; }
    @Override public String getHudName()    { return "Day Counter"; }
    @Override public int getDefaultWidth()  { return 150; }
    @Override public int getDefaultHeight() { return 48; }

    @Override
    protected void renderHudContent(DrawContext ctx, float tickDelta, int x, int y, int w, int h) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;

        // Only meaningful in overworld — other dimensions have no day cycle
        RegistryKey<World> dim = mc.world.getRegistryKey();
        if (!dim.equals(World.OVERWORLD)) {
            drawBackground(ctx, x, y, w, 18);
            ctx.drawTextWithShadow(mc.textRenderer, "No Day Cycle", x + 4, y + 5, textColor.get());
            return;
        }

        long totalTicks = mc.world.getTimeOfDay();
        long dayTicks   = totalTicks % 24000L;
        long dayNumber  = totalTicks / 24000L + 1;

        int moonPhase = (int) ((dayNumber - 1) % 8);

        int mcHour   = (int) ((dayTicks / 1000 + 6) % 24);
        int mcMinute = (int) ((dayTicks % 1000) / 1000.0 * 60);

        boolean isNight    = dayTicks >= 13000 && dayTicks < 23000;
        boolean isNightWarn = dayTicks >= 12000 && dayTicks < 13000;

        String timeStr;
        if (timeFormat.get()) {
            timeStr = String.format("%02d:%02d", mcHour, mcMinute);
        } else {
            int h12 = mcHour % 12;
            if (h12 == 0) h12 = 12;
            timeStr = String.format("%d:%02d %s", h12, mcMinute, mcHour < 12 ? "AM" : "PM");
        }

        int timeColor = isNight ? colorNight.get() : colorDay.get();
        if (isNightWarn && showNightWarning.get()) timeColor = 0xFFFF9933;

        var tr = mc.textRenderer;

        if (compactMode.get()) {
            StringBuilder sb = new StringBuilder("Day " + dayNumber);
            if (showTimeOfDay.get())  sb.append("  ").append(timeStr);
            if (showMoonPhase.get())  sb.append("  ").append(moonPhaseNames.get() ? PHASE_NAMES[moonPhase] : "Phase " + moonPhase);
            String line = sb.toString();
            int lineW = Math.max(w, tr.getWidth(line) + 8);
            drawBackground(ctx, x, y, lineW, 18);
            ctx.drawTextWithShadow(tr, line, x + 4, y + 5, timeColor);
            return;
        }

        List<String>  lines  = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        lines.add("Day: " + dayNumber);    colors.add(textColor.get());
        if (showTimeOfDay.get()) { lines.add("Time: " + timeStr);  colors.add(timeColor); }
        if (showMoonPhase.get()) {
            String phase = moonPhaseNames.get() ? PHASE_NAMES[moonPhase] : "Moon: " + moonPhase;
            lines.add(phase); colors.add(0xFF99AACC);
        }

        int totalH = 8 + lines.size() * 10;
        int maxW = w;
        for (String l : lines) maxW = Math.max(maxW, tr.getWidth(l) + 8);
        drawBackground(ctx, x, y, maxW, totalH);

        int lineY = y + 4;
        for (int i = 0; i < lines.size(); i++) {
            ctx.drawTextWithShadow(tr, lines.get(i), x + 4, lineY, colors.get(i));
            lineY += 10;
        }
    }
}
