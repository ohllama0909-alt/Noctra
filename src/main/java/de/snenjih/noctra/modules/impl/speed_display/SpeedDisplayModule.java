package de.snenjih.noctra.modules.impl.speed_display;

import de.snenjih.noctra.modules.api.BaseHudModule;
import de.snenjih.noctra.modules.api.ModuleCategory;
import de.snenjih.noctra.modules.api.settings.BooleanSetting;
import de.snenjih.noctra.modules.api.settings.IntSetting;
import de.snenjih.noctra.modules.api.settings.ModuleSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class SpeedDisplayModule extends BaseHudModule {

    private final ModuleSetting<Boolean> showLabel;
    private final ModuleSetting<Integer> decimalPlaces;
    private final ModuleSetting<Boolean> showVertical;

    private double prevX, prevY, prevZ;
    private double cachedSpeedH, cachedSpeedV;
    private boolean hasPrev;

    public SpeedDisplayModule() {
        super(
            "speed_display",
            "Speed Display",
            "Shows your movement speed in blocks per second.",
            ModuleCategory.VISUAL,
            Identifier.of("noctra", "modules/speed_display")
        );
        showLabel     = addSetting(new BooleanSetting("show_label",     "Show Label",          true));
        decimalPlaces = addSetting(new IntSetting("decimal_places",     "Decimal Places",      2, 0, 3));
        showVertical  = addSetting(new BooleanSetting("show_vertical",  "Show Vertical Speed", false));
    }

    @Override public String getHudId()      { return "speed_display"; }
    @Override public String getHudName()    { return "Speed Display"; }
    @Override public int getDefaultWidth()  { return 140; }
    @Override public int getDefaultHeight() { return 22; }

    @Override
    public void onClientTick(MinecraftClient client) {
        if (client.player == null) return;
        double x = client.player.getX();
        double y = client.player.getY();
        double z = client.player.getZ();
        if (hasPrev) {
            double dx = x - prevX, dy = y - prevY, dz = z - prevZ;
            cachedSpeedH = Math.sqrt(dx * dx + dz * dz) * 20.0;
            cachedSpeedV = Math.abs(dy) * 20.0;
        }
        prevX = x; prevY = y; prevZ = z;
        hasPrev = true;
    }

    @Override
    public void onLeaveWorld() {
        hasPrev = false;
        cachedSpeedH = 0;
        cachedSpeedV = 0;
    }

    @Override
    protected void renderHudContent(DrawContext ctx, float tickDelta, int x, int y, int w, int h) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        String fmt = "%." + decimalPlaces.get() + "f";
        String line = (showLabel.get() ? "Speed: " : "") + String.format(fmt, cachedSpeedH) + " b/s";

        drawBackground(ctx, x, y, w, h);
        ctx.drawTextWithShadow(mc.textRenderer, Text.literal(line), x + 4, y + 5, textColor.get());

        if (showVertical.get()) {
            String vLine = (showLabel.get() ? "V-Speed: " : "") + String.format(fmt, cachedSpeedV) + " b/s";
            ctx.drawTextWithShadow(mc.textRenderer, Text.literal(vLine), x + 4, y + 15, textColor.get());
        }
    }
}
