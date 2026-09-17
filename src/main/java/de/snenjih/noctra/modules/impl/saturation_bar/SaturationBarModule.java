package de.snenjih.noctra.modules.impl.saturation_bar;

import de.snenjih.noctra.modules.api.BaseHudModule;
import de.snenjih.noctra.modules.api.ModuleCategory;
import de.snenjih.noctra.modules.api.settings.BooleanSetting;
import de.snenjih.noctra.modules.api.settings.ColorSetting;
import de.snenjih.noctra.modules.api.settings.IntSetting;
import de.snenjih.noctra.modules.api.settings.ModuleSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class SaturationBarModule extends BaseHudModule {

    private final ModuleSetting<Boolean> anchorToHunger;
    private final ModuleSetting<Integer> barWidth;
    private final ModuleSetting<Integer> barHeight;
    private final ModuleSetting<Integer> colorSat;
    private final ModuleSetting<Boolean> showLabel;

    public SaturationBarModule() {
        super(
            "saturation_bar",
            "Saturation Bar",
            "Makes the hidden food saturation value visible as a bar.",
            ModuleCategory.VISUAL,
            Identifier.of("noctra", "modules/saturation_bar")
        );
        anchorToHunger = addSetting(new BooleanSetting("anchor_to_hunger", "Anchor to Hunger Bar", true));
        barWidth       = addSetting(new IntSetting("bar_width",   "Bar Width",        81, 20, 200));
        barHeight      = addSetting(new IntSetting("bar_height",  "Bar Height",       4,  1,  12));
        colorSat       = addSetting(new ColorSetting("color_sat", "Saturation Color", 0xFFFFD700));
        showLabel      = addSetting(new BooleanSetting("show_label", "Show Label",    false));
    }

    @Override public String getHudId()      { return "saturation_bar"; }
    @Override public String getHudName()    { return "Saturation Bar"; }
    @Override public int getDefaultWidth()  { return 90; }
    @Override public int getDefaultHeight() { return 14; }

    @Override
    protected void renderHudContent(DrawContext ctx, float tickDelta, int x, int y, int w, int h) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        HungerManager hunger = mc.player.getHungerManager();
        float saturation = hunger.getSaturationLevel();
        int foodLevel    = hunger.getFoodLevel();

        float maxSat   = foodLevel;
        float fraction = maxSat > 0 ? Math.min(saturation / maxSat, 1.0f) : 0f;

        int bx, by;
        if (anchorToHunger.get()) {
            int scaledW = mc.getWindow().getScaledWidth();
            int scaledH = mc.getWindow().getScaledHeight();
            bx = scaledW / 2 + 10;
            by = scaledH - 49 + 9;
        } else {
            bx = x;
            by = y;
        }

        int bw = barWidth.get();
        int bh = barHeight.get();

        ctx.fill(bx - 1, by - 1, bx + bw + 1, by + bh + 1, bgColor.get());
        ctx.fill(bx, by, bx + bw, by + bh, 0xFF222222);

        int fillW = Math.round(fraction * bw);
        if (fillW > 0) ctx.fill(bx, by, bx + fillW, by + bh, colorSat.get());

        if (showLabel.get()) {
            String label = String.format("%.1f", saturation);
            ctx.drawTextWithShadow(mc.textRenderer, Text.literal(label), bx + bw + 3, by, 0xFFFFD700);
        }
    }
}
