package de.snenjih.noctra.modules.impl.attack_cooldown_indicator;

import de.snenjih.noctra.modules.api.BaseHudModule;
import de.snenjih.noctra.modules.api.ModuleCategory;
import de.snenjih.noctra.modules.api.settings.BooleanSetting;
import de.snenjih.noctra.modules.api.settings.ColorSetting;
import de.snenjih.noctra.modules.api.settings.IntSetting;
import de.snenjih.noctra.modules.api.settings.ModuleSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class AttackCooldownIndicatorModule extends BaseHudModule {

    private final ModuleSetting<Integer> barWidth;
    private final ModuleSetting<Integer> barHeight;
    private final ModuleSetting<Boolean> showText;
    private final ModuleSetting<Integer> colorReady;
    private final ModuleSetting<Integer> colorCharging;
    private final ModuleSetting<Boolean> showBorder;
    private final ModuleSetting<Boolean> onlyInCombat;

    public AttackCooldownIndicatorModule() {
        super(
            "attack_cooldown_indicator",
            "Attack Cooldown",
            "Shows your weapon attack cooldown as a customizable HUD bar.",
            ModuleCategory.COMBAT,
            Identifier.of("noctra", "modules/attack_cooldown_indicator")
        );
        barWidth     = addSetting(new IntSetting    ("bar_width",     "Bar Width",      60,  20, 200));
        barHeight    = addSetting(new IntSetting    ("bar_height",    "Bar Height",     4,   2,  20));
        showText     = addSetting(new BooleanSetting("show_text",     "Show Percentage",false));
        showBorder   = addSetting(new BooleanSetting("show_border",   "Show Border",    true));
        onlyInCombat = addSetting(new BooleanSetting("only_in_combat","Only in Combat", false));
        beginSection("Colors");
        colorReady    = addSetting(new ColorSetting("color_ready",    "Ready Color",    0xFF00FF00));
        colorCharging = addSetting(new ColorSetting("color_charging", "Charging Color", 0xFFFF5500));
    }

    @Override public String getHudId()       { return "attack_cooldown_indicator"; }
    @Override public String getHudName()     { return "Attack Cooldown"; }
    @Override public int getDefaultWidth()   { return 60; }
    @Override public int getDefaultHeight()  { return 4; }

    @Override
    protected void renderHudContent(DrawContext ctx, float tickDelta, int x, int y, int w, int h) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        if (mc.player.isSpectator()) return;
        if (mc.options.hudHidden) return;

        if (onlyInCombat.get() && !(mc.targetedEntity instanceof LivingEntity)) return;

        float progress = mc.player.getAttackCooldownProgress(tickDelta);

        int bw = barWidth.get();
        int bh = barHeight.get();

        // Background
        ctx.fill(x, y, x + bw, y + bh, 0x88000000);

        // Filled bar
        int filledWidth = (int) (progress * bw);
        int barColor = (progress >= 1.0f) ? colorReady.get() : colorCharging.get();
        if (filledWidth > 0) {
            ctx.fill(x, y, x + filledWidth, y + bh, barColor);
        }

        // Border
        if (showBorder.get()) {
            ctx.drawStrokedRectangle(x, y, bw, bh, 0xFF000000);
        }

        // Percentage text
        if (showText.get()) {
            int percent  = (int) (progress * 100);
            String label = percent + "%";
            int textX = x + bw / 2 - mc.textRenderer.getWidth(label) / 2;
            ctx.drawTextWithShadow(mc.textRenderer, Text.literal(label), textX, y - 10, 0xFFFFFFFF);
        }
    }
}
