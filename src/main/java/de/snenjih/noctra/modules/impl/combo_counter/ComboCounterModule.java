package de.snenjih.noctra.modules.impl.combo_counter;

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
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;

public class ComboCounterModule extends BaseHudModule {

    private final ModuleSetting<Boolean> resetOnDamage;
    private final ModuleSetting<Integer> resetTimeout;
    private final ModuleSetting<Boolean> showMaxCombo;
    private final ModuleSetting<Boolean> showOnlyWhenActive;
    private final ModuleSetting<Integer> minDisplayCombo;
    private final ModuleSetting<Boolean> comboColors;
    private final ModuleSetting<Integer> colorLow;
    private final ModuleSetting<Integer> colorMid;
    private final ModuleSetting<Integer> colorHigh;
    private final ModuleSetting<Integer> colorMax;
    private final ModuleSetting<Integer> playSoundAt;

    private int currentCombo = 0;
    private int maxCombo = 0;
    private long lastHitTime = 0L;
    private float lastPlayerHp = 20f;

    public ComboCounterModule() {
        super(
            "combo_counter",
            "Combo Counter",
            "Counts consecutive hits without taking damage.",
            ModuleCategory.COMBAT,
            Identifier.of("noctra", "modules/combo_counter")
        );
        resetOnDamage     = addSetting(new BooleanSetting("reset_on_damage",      "Reset on Damage",       true));
        resetTimeout      = addSetting(new IntSetting    ("reset_timeout",         "Reset Timeout (s)",     5,    0, 60));
        showMaxCombo      = addSetting(new BooleanSetting("show_max_combo",        "Show Max Combo",        true));
        showOnlyWhenActive= addSetting(new BooleanSetting("show_only_when_active", "Show Only When Active", false));
        minDisplayCombo   = addSetting(new IntSetting    ("min_display_combo",     "Min Display Combo",     2,    1, 10));
        playSoundAt       = addSetting(new IntSetting    ("play_sound_at",         "Sound at Combo",        10,   0, 100));
        beginSection("Colors");
        comboColors = addSetting(new BooleanSetting("combo_colors", "Combo Colors",  true));
        colorLow    = addSetting(new ColorSetting  ("color_low",    "Color (1-4)",   0xFFFFFFFF));
        colorMid    = addSetting(new ColorSetting  ("color_mid",    "Color (5-9)",   0xFFFFFF55));
        colorHigh   = addSetting(new ColorSetting  ("color_high",   "Color (10-19)", 0xFFFF8800));
        colorMax    = addSetting(new ColorSetting  ("color_max",    "Color (20+)",   0xFFFF5555));
    }

    @Override public String getHudId()      { return "combo_counter"; }
    @Override public String getHudName()    { return "Combo Counter"; }
    @Override public int getDefaultWidth()  { return 100; }
    @Override public int getDefaultHeight() { return 22; }

    @Override
    public void onEnable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        currentCombo = 0;
        maxCombo = 0;
        lastHitTime = 0L;
        lastPlayerHp = mc.player != null ? mc.player.getHealth() : 20f;
    }

    @Override
    public void onDisable() {
        currentCombo = 0;
        maxCombo = 0;
    }

    @Override
    public ActionResult onAttackEntity(ClientPlayerEntity player, Entity target) {
        if (target instanceof LivingEntity) {
            recordHit(player);
        }
        return ActionResult.PASS;
    }

    private void recordHit(ClientPlayerEntity player) {
        currentCombo++;
        lastHitTime = System.currentTimeMillis();
        if (currentCombo > maxCombo) maxCombo = currentCombo;

        int soundThreshold = playSoundAt.get();
        if (soundThreshold > 0 && currentCombo == soundThreshold) {
            player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.5f);
        }
    }

    private void resetCombo() {
        currentCombo = 0;
    }

    @Override
    public void onClientTick(MinecraftClient client) {
        if (client.player == null) return;

        float hp = client.player.getHealth();

        // Reset on damage
        if (resetOnDamage.get() && hp < lastPlayerHp - 0.01f) {
            resetCombo();
        }
        lastPlayerHp = hp;

        // Timeout reset
        int timeout = resetTimeout.get();
        if (timeout > 0 && currentCombo > 0) {
            long elapsed = System.currentTimeMillis() - lastHitTime;
            if (elapsed > timeout * 1000L) {
                resetCombo();
            }
        }
    }

    @Override
    protected void renderHudContent(DrawContext ctx, float tickDelta, int x, int y, int w, int h) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        if (mc.options.hudHidden) return;

        if (showOnlyWhenActive.get() && currentCombo < minDisplayCombo.get()) return;

        int color = textColor.get();
        if (comboColors.get()) {
            if (currentCombo >= 20)      color = colorMax.get();
            else if (currentCombo >= 10) color = colorHigh.get();
            else if (currentCombo >= 5)  color = colorMid.get();
            else                          color = colorLow.get();
        }

        drawBackground(ctx, x, y, w, h);

        ctx.drawTextWithShadow(mc.textRenderer,
            Text.literal("Combo: x" + currentCombo),
            x + 4, y + 4, color);

        if (showMaxCombo.get()) {
            ctx.drawTextWithShadow(mc.textRenderer,
                Text.literal("Best: x" + maxCombo),
                x + 4, y + 14, textColor.get());
        }
    }
}
