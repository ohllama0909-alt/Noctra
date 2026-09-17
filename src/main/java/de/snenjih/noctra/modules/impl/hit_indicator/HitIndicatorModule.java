package de.snenjih.noctra.modules.impl.hit_indicator;

import de.snenjih.noctra.modules.api.BaseHudModule;
import de.snenjih.noctra.modules.api.ModuleCategory;
import de.snenjih.noctra.modules.api.settings.BooleanSetting;
import de.snenjih.noctra.modules.api.settings.ColorSetting;
import de.snenjih.noctra.modules.api.settings.IntSetting;
import de.snenjih.noctra.modules.api.settings.ModuleSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;

public class HitIndicatorModule extends BaseHudModule {

    private final ModuleSetting<Boolean> flashEnabled;
    private final ModuleSetting<Integer> flashColor;
    private final ModuleSetting<Integer> flashDuration;
    private final ModuleSetting<Boolean> showText;
    private final ModuleSetting<Boolean> showDamage;
    private final ModuleSetting<Integer> hitTextColor;

    private int   flashTicksRemaining    = 0;
    private float lastTargetHealth       = -1f;
    private Entity lastTarget            = null;
    private float lastDamageDealt        = 0f;
    private int   hitTextTicksRemaining  = 0;

    public HitIndicatorModule() {
        super(
            "hit_indicator",
            "Hit Indicator",
            "Shows a visual flash and text when you land a hit on an entity.",
            ModuleCategory.COMBAT,
            Identifier.of("noctra", "modules/hit_indicator")
        );
        flashEnabled  = addSetting(new BooleanSetting("flash_enabled",  "Screen Flash",    true));
        flashColor    = addSetting(new ColorSetting  ("flash_color",    "Flash Color",     0x33FF0000));
        flashDuration = addSetting(new IntSetting    ("flash_duration", "Flash Duration",  5, 1, 20));
        showText      = addSetting(new BooleanSetting("show_text",      "Show Hit Text",   false));
        showDamage    = addSetting(new BooleanSetting("show_damage",    "Show Damage",     false));
        hitTextColor  = addSetting(new ColorSetting  ("text_color",     "Text Color",      0xFFFF4444));
    }

    @Override public String getHudId()       { return "hit_indicator"; }
    @Override public String getHudName()     { return "Hit Indicator"; }
    @Override public int getDefaultWidth()   { return 60; }
    @Override public int getDefaultHeight()  { return 20; }

    @Override
    public void onDisable() {
        flashTicksRemaining   = 0;
        hitTextTicksRemaining = 0;
        lastTarget            = null;
    }

    @Override
    public ActionResult onAttackEntity(ClientPlayerEntity player, Entity target) {
        // Start flash
        if (flashEnabled.get()) {
            flashTicksRemaining = flashDuration.get();
        }

        // Record target health for damage calculation
        if (showDamage.get() && target instanceof LivingEntity living) {
            lastTargetHealth = living.getHealth();
            lastTarget       = target;
        }

        if (showText.get()) {
            hitTextTicksRemaining = 40; // 2 seconds
        }

        return ActionResult.PASS;
    }

    @Override
    public void onClientTick(MinecraftClient client) {
        if (flashTicksRemaining > 0) {
            flashTicksRemaining--;
        }

        if (lastTarget instanceof LivingEntity living) {
            float currentHp = living.getHealth();
            if (currentHp < lastTargetHealth) {
                lastDamageDealt  = lastTargetHealth - currentHp;
                lastTargetHealth = currentHp;
            }
            if (living.isRemoved() || living.getHealth() <= 0) {
                lastTarget = null;
            }
        }

        if (hitTextTicksRemaining > 0) {
            hitTextTicksRemaining--;
        }
    }

    @Override
    protected void renderHudContent(DrawContext ctx, float tickDelta, int x, int y, int w, int h) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        if (mc.options.hudHidden) return;

        // Screen flash
        if (flashEnabled.get() && flashTicksRemaining > 0) {
            float fadeProgress = flashTicksRemaining / (float) flashDuration.get();
            int   baseColor    = flashColor.get();
            int   alpha        = (int) ((baseColor >> 24 & 0xFF) * fadeProgress);
            int   fadedColor   = (alpha << 24) | (baseColor & 0x00FFFFFF);
            int   sw           = mc.getWindow().getScaledWidth();
            int   sh           = mc.getWindow().getScaledHeight();
            ctx.fill(0, 0, sw, sh, fadedColor);
        }

        // Hit text
        if (showText.get() && hitTextTicksRemaining > 0) {
            int    sw       = mc.getWindow().getScaledWidth();
            int    sh       = mc.getWindow().getScaledHeight();
            String hitLabel = "Hit!";
            if (showDamage.get() && lastDamageDealt > 0) {
                hitLabel = String.format("-%.1f", lastDamageDealt);
            }
            int textX = sw / 2 - mc.textRenderer.getWidth(hitLabel) / 2;
            int textY = sh / 2 - 30;
            ctx.drawTextWithShadow(mc.textRenderer, Text.literal(hitLabel), textX, textY, hitTextColor.get());
        }
    }
}
