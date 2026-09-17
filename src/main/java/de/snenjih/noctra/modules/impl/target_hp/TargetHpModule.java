package de.snenjih.noctra.modules.impl.target_hp;

import de.snenjih.noctra.modules.api.BaseHudModule;
import de.snenjih.noctra.modules.api.ModuleCategory;
import de.snenjih.noctra.modules.api.settings.BooleanSetting;
import de.snenjih.noctra.modules.api.settings.ColorSetting;
import de.snenjih.noctra.modules.api.settings.ModuleSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class TargetHpModule extends BaseHudModule {

    private final ModuleSetting<Boolean> showBar;
    private final ModuleSetting<Boolean> showNumbers;
    private final ModuleSetting<Boolean> showName;
    private final ModuleSetting<Integer> colorHigh;
    private final ModuleSetting<Integer> colorMid;
    private final ModuleSetting<Integer> colorLow;

    public TargetHpModule() {
        super(
            "target_hp",
            "Target HP",
            "Displays the health of the entity you are currently targeting.",
            ModuleCategory.VISUAL,
            Identifier.of("noctra", "modules/target_hp")
        );
        showBar     = addSetting(new BooleanSetting("show_bar",     "Show Bar",     true));
        showNumbers = addSetting(new BooleanSetting("show_numbers", "Show Numbers", true));
        showName    = addSetting(new BooleanSetting("show_name",    "Show Name",    true));
        beginSection("Colors");
        colorHigh   = addSetting(new ColorSetting("color_high", "Color High", 0xFF55FF55));
        colorMid    = addSetting(new ColorSetting("color_mid",  "Color Mid",  0xFFFFAA00));
        colorLow    = addSetting(new ColorSetting("color_low",  "Color Low",  0xFFFF5555));
    }

    @Override public String getHudId()      { return "target_hp"; }
    @Override public String getHudName()    { return "Target HP"; }
    @Override public int getDefaultWidth()  { return 120; }
    @Override public int getDefaultHeight() { return 36; }

    @Override
    protected void renderHudContent(DrawContext ctx, float tickDelta, int x, int y, int w, int h) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        Entity targeted = mc.targetedEntity;
        if (!(targeted instanceof LivingEntity living)) return;

        float health    = living.getHealth();
        float maxHealth = living.getMaxHealth();
        float fraction  = maxHealth > 0 ? health / maxHealth : 0f;

        int color;
        if (fraction > 0.6f)      color = colorHigh.get();
        else if (fraction > 0.3f) color = colorMid.get();
        else                      color = colorLow.get();

        drawBackground(ctx, x, y, w, h);

        int currentY = y + 2;
        int barW = w - 4;

        if (showName.get()) {
            String name = living.getName().getString();
            ctx.drawTextWithShadow(mc.textRenderer, Text.literal(name), x + 2, currentY, 0xFFFFFFFF);
            currentY += 10;
        }

        if (showBar.get()) {
            ctx.fill(x + 2, currentY, x + 2 + barW, currentY + 6, 0xFF222222);
            int fill = Math.round(fraction * barW);
            ctx.fill(x + 2, currentY, x + 2 + fill, currentY + 6, color);
            currentY += 9;
        }

        if (showNumbers.get()) {
            String hpText = String.format("%.1f / %.1f", health, maxHealth);
            ctx.drawTextWithShadow(mc.textRenderer, Text.literal(hpText), x + 2, currentY, color);
        }
    }
}
