package de.snenjih.noctra.modules.impl.real_time_clock;

import de.snenjih.noctra.modules.api.BaseHudModule;
import de.snenjih.noctra.modules.api.ModuleCategory;
import de.snenjih.noctra.modules.api.settings.BooleanSetting;
import de.snenjih.noctra.modules.api.settings.ModuleSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class RealTimeClockModule extends BaseHudModule {

    private final ModuleSetting<Boolean> format24h;
    private final ModuleSetting<Boolean> showSeconds;
    private final ModuleSetting<Boolean> showDate;

    private static final DateTimeFormatter TIME_24    = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter TIME_24_NS = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter TIME_12    = DateTimeFormatter.ofPattern("hh:mm:ss a");
    private static final DateTimeFormatter TIME_12_NS = DateTimeFormatter.ofPattern("hh:mm a");
    private static final DateTimeFormatter DATE_FMT   = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public RealTimeClockModule() {
        super(
            "real_time_clock",
            "Real Time Clock",
            "Shows your real system time as an on-screen overlay.",
            ModuleCategory.VISUAL,
            Identifier.of("noctra", "modules/real_time_clock")
        );
        format24h   = addSetting(new BooleanSetting("format_24h",  "24h Format",   true));
        showSeconds = addSetting(new BooleanSetting("show_seconds", "Show Seconds", true));
        showDate    = addSetting(new BooleanSetting("show_date",    "Show Date",    false));
    }

    @Override public String getHudId()      { return "real_time_clock"; }
    @Override public String getHudName()    { return "Real Time Clock"; }
    @Override public int getDefaultWidth()  { return 110; }
    @Override public int getDefaultHeight() { return 22; }

    @Override
    protected void renderHudContent(DrawContext ctx, float tickDelta, int x, int y, int w, int h) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;

        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter timeFmt = format24h.get()
            ? (showSeconds.get() ? TIME_24 : TIME_24_NS)
            : (showSeconds.get() ? TIME_12 : TIME_12_NS);

        String timeStr = now.format(timeFmt);
        String dateStr = showDate.get() ? now.format(DATE_FMT) : null;

        drawBackground(ctx, x, y, w, h);
        ctx.drawTextWithShadow(mc.textRenderer, Text.literal(timeStr), x + 4, y + 5, textColor.get());
        if (dateStr != null) {
            ctx.drawTextWithShadow(mc.textRenderer, Text.literal(dateStr), x + 4, y + 15, textColor.get());
        }
    }
}
