package de.snenjih.noctra.modules.impl.glide_stats;

import de.snenjih.noctra.modules.api.BaseHudModule;
import de.snenjih.noctra.modules.api.ModuleCategory;
import de.snenjih.noctra.modules.api.settings.BooleanSetting;
import de.snenjih.noctra.modules.api.settings.ModuleSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

public class GlideStatsModule extends BaseHudModule {

    private final ModuleSetting<Boolean> alwaysShow;
    private final ModuleSetting<Boolean> showBackground;

    private double smoothHSpeed = 0.0;
    private double smoothVSpeed = 0.0;

    public GlideStatsModule() {
        super(
            "glide_stats",
            "Glide Stats",
            "Shows real-time speed, altitude, and pitch while gliding.",
            ModuleCategory.ELYTRA,
            Identifier.of("noctra", "modules/glide_stats")
        );
        alwaysShow     = addSetting(new BooleanSetting("always_show",     "Always Show",     false));
        showBackground = addSetting(new BooleanSetting("show_background", "Show Background", true));
    }

    @Override public String getHudId()      { return "glide_stats"; }
    @Override public String getHudName()    { return "Glide Stats"; }
    @Override public int getDefaultWidth()  { return 100; }
    @Override public int getDefaultHeight() { return 44; }

    @Override
    public void onEnable() {
        smoothHSpeed = 0.0;
        smoothVSpeed = 0.0;
    }

    @Override
    public void onClientTick(MinecraftClient client) {
        if (client.player == null) return;

        Vec3d vel = client.player.getVelocity();
        double hSpeed = Math.sqrt(vel.x * vel.x + vel.z * vel.z) * 20.0;
        double vSpeed = vel.y * 20.0;

        // Exponential smoothing (alpha = 0.3)
        smoothHSpeed = smoothHSpeed * 0.7 + hSpeed * 0.3;
        smoothVSpeed = smoothVSpeed * 0.7 + vSpeed * 0.3;
    }

    @Override
    protected void renderHudContent(DrawContext ctx, float tickDelta, int x, int y, int w, int h) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        if (mc.options.hudHidden) return;
        if (!alwaysShow.get() && !mc.player.isGliding()) return;

        String[] lines = {
            String.format("H-Speed: %.1f b/s", smoothHSpeed),
            String.format("V-Speed: %+.1f b/s", smoothVSpeed),
            String.format("Y: %.1f", mc.player.getY()),
            String.format("Pitch: %.1f°", mc.player.getPitch())
        };

        if (showBackground.get()) {
            int bw = 98;
            int bh = lines.length * 10 + 4;
            ctx.fill(x - 2, y - 2, x + bw, y + bh, 0x88000000);
        }

        for (int i = 0; i < lines.length; i++) {
            ctx.drawTextWithShadow(mc.textRenderer, Text.literal(lines[i]), x, y + i * 10, textColor.get());
        }
    }
}
