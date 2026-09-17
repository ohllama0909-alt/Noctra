package de.snenjih.noctra.modules.impl.fps_ping_hud;

import de.snenjih.noctra.modules.api.BaseHudModule;
import de.snenjih.noctra.modules.api.ModuleCategory;
import de.snenjih.noctra.modules.api.settings.BooleanSetting;
import de.snenjih.noctra.modules.api.settings.ModuleSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.util.Identifier;

public class FpsPingHudModule extends BaseHudModule {

    private final ModuleSetting<Boolean> showPing;
    private final ModuleSetting<Boolean> colorCode;

    public FpsPingHudModule() {
        super(
            "fps_ping_hud",
            "FPS / Ping",
            "Displays FPS and ping on the HUD.",
            ModuleCategory.VISUAL,
            Identifier.of("noctra", "modules/fps_ping_hud")
        );
        showPing  = addSetting(new BooleanSetting("show_ping",  "Show Ping",  true));
        colorCode = addSetting(new BooleanSetting("color_code", "Color Code", true));
    }

    // ── HudElement ────────────────────────────────────────────────────────────

    @Override public String getHudId()      { return "fps_ping_hud"; }
    @Override public String getHudName()    { return "FPS / Ping"; }
    @Override public int getDefaultWidth()  { return 100; }
    @Override public int getDefaultHeight() { return 18; }

    @Override
    protected void renderHudContent(DrawContext ctx, float tickDelta, int x, int y, int w, int h) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        var tr = mc.textRenderer;

        drawBackground(ctx, x, y, w, h);

        int fps = mc.getCurrentFps();

        int fpsColor = textColor.get();
        if (colorCode.get()) {
            if (fps > 40)      fpsColor = 0xFF55FF55;
            else if (fps > 20) fpsColor = 0xFFFFFF55;
            else               fpsColor = 0xFFFF5555;
        }

        int ping = -1;
        if (showPing.get() && mc.getNetworkHandler() != null) {
            PlayerListEntry entry = mc.getNetworkHandler()
                    .getPlayerListEntry(mc.player.getGameProfile().id());
            if (entry != null) ping = entry.getLatency();
        }

        if (showPing.get() && ping >= 0) {
            int pingColor = textColor.get();
            if (colorCode.get()) {
                if (ping < 50)       pingColor = 0xFF55FF55;
                else if (ping < 100) pingColor = 0xFFFFFF55;
                else                 pingColor = 0xFFFF5555;
            }
            int ty = y + (h - 8) / 2;
            String fpsPart  = fps + " FPS | ";
            String pingPart = ping + "ms";
            ctx.drawTextWithShadow(tr, fpsPart,  x + 4, ty, fpsColor);
            ctx.drawTextWithShadow(tr, pingPart, x + 4 + tr.getWidth(fpsPart), ty, pingColor);
        } else {
            ctx.drawTextWithShadow(tr, fps + " FPS", x + 4, y + (h - 8) / 2, fpsColor);
        }
    }
}
