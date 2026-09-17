package de.snenjih.noctra.modules.impl.cps_counter;

import de.snenjih.noctra.modules.api.BaseHudModule;
import de.snenjih.noctra.modules.api.ModuleCategory;
import de.snenjih.noctra.modules.api.settings.BooleanSetting;
import de.snenjih.noctra.modules.api.settings.ColorSetting;
import de.snenjih.noctra.modules.api.settings.ModuleSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayDeque;

public class CpsCounterModule extends BaseHudModule {

    public static CpsCounterModule INSTANCE;

    private final ModuleSetting<Boolean> trackLeft;
    private final ModuleSetting<Boolean> trackRight;
    private final ModuleSetting<Integer> cpsTextColor;

    public final ArrayDeque<Long> lClickTimestamps = new ArrayDeque<>();
    public final ArrayDeque<Long> rClickTimestamps = new ArrayDeque<>();

    public CpsCounterModule() {
        super(
            "cps_counter",
            "CPS Counter",
            "Shows your clicks per second using a 1-second sliding window.",
            ModuleCategory.COMBAT,
            Identifier.of("noctra", "modules/cps_counter")
        );
        INSTANCE    = this;
        trackLeft   = addSetting(new BooleanSetting("track_left",  "Track Left Click",  true));
        trackRight  = addSetting(new BooleanSetting("track_right", "Track Right Click", false));
        cpsTextColor = addSetting(new ColorSetting ("text_color",  "Text Color",        0xFFFFFFFF));
    }

    @Override public String getHudId()      { return "cps_counter"; }
    @Override public String getHudName()    { return "CPS Counter"; }
    @Override public int getDefaultWidth()  { return 70; }
    @Override public int getDefaultHeight() { return 24; }

    @Override
    public void onDisable() {
        lClickTimestamps.clear();
        rClickTimestamps.clear();
    }

    public boolean isTrackLeft()  { return trackLeft.get(); }
    public boolean isTrackRight() { return trackRight.get(); }

    public static void onMouseClick(int button) {
        CpsCounterModule m = INSTANCE;
        if (m == null || !m.isEnabled()) return;
        long now = System.currentTimeMillis();
        if (button == 0 && m.isTrackLeft()) {
            m.lClickTimestamps.addLast(now);
        } else if (button == 1 && m.isTrackRight()) {
            m.rClickTimestamps.addLast(now);
        }
    }

    @Override
    public void onClientTick(MinecraftClient client) {
        long cutoff = System.currentTimeMillis() - 1000L;
        while (!lClickTimestamps.isEmpty() && lClickTimestamps.peekFirst() < cutoff) {
            lClickTimestamps.pollFirst();
        }
        while (!rClickTimestamps.isEmpty() && rClickTimestamps.peekFirst() < cutoff) {
            rClickTimestamps.pollFirst();
        }
    }

    @Override
    protected void renderHudContent(DrawContext ctx, float tickDelta, int x, int y, int w, int h) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        if (mc.options.hudHidden) return;

        drawBackground(ctx, x, y, w, h);

        int lineY = y + 2;
        int color = cpsTextColor.get();

        if (trackLeft.get()) {
            int lcps = lClickTimestamps.size();
            ctx.drawTextWithShadow(mc.textRenderer, Text.literal("CPS: " + lcps), x + 4, lineY, color);
            lineY += 12;
        }

        if (trackRight.get()) {
            int rcps = rClickTimestamps.size();
            ctx.drawTextWithShadow(mc.textRenderer, Text.literal("RCPS: " + rcps), x + 4, lineY, color);
        }
    }
}
