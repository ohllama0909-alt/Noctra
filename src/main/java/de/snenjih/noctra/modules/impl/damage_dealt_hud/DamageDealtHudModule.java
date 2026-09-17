package de.snenjih.noctra.modules.impl.damage_dealt_hud;

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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DamageDealtHudModule extends BaseHudModule {

    private final ModuleSetting<Integer> displayTime;
    private final ModuleSetting<Boolean> showTotal;
    private final ModuleSetting<Boolean> showMaxHit;
    private final ModuleSetting<Boolean> showDps;
    private final ModuleSetting<Boolean> fadeOut;
    private final ModuleSetting<Integer> damageColor;
    private final ModuleSetting<Integer> critColor;
    private final ModuleSetting<Integer> decimalPlaces;
    private final ModuleSetting<Boolean> hideWhenZero;

    private final Map<UUID, Float> lastEntityHealth = new HashMap<>();
    private float lastDamageDealt = 0f;
    private boolean lastWasCrit = false;
    private long lastHitTime = 0L;
    private float sessionTotal = 0f;
    private float maxHit = 0f;
    private float currentDps = 0f;
    private final Deque<DpsEntry> dpsWindow = new ArrayDeque<>();

    private record DpsEntry(long timestamp, float damage) {}

    public DamageDealtHudModule() {
        super(
            "damage_dealt_hud",
            "Damage Dealt",
            "Shows the last damage dealt and session combat statistics.",
            ModuleCategory.COMBAT,
            Identifier.of("noctra", "modules/damage_dealt_hud")
        );
        displayTime   = addSetting(new IntSetting    ("display_time",   "Display Time (s)",  3,    1,  30));
        decimalPlaces = addSetting(new IntSetting    ("decimal_places", "Decimal Places",    1,    0,   2));
        hideWhenZero  = addSetting(new BooleanSetting("hide_when_zero", "Hide When Zero",    true));
        fadeOut       = addSetting(new BooleanSetting("fade_out",       "Fade Out",          true));
        beginSection("Stats");
        showTotal     = addSetting(new BooleanSetting("show_total",     "Show Session Total", false));
        showMaxHit    = addSetting(new BooleanSetting("show_max_hit",   "Show Max Hit",       false));
        showDps       = addSetting(new BooleanSetting("show_dps",       "Show DPS",           false));
        beginSection("Colors");
        damageColor   = addSetting(new ColorSetting  ("damage_color",   "Damage Color",      0xFFFF5555));
        critColor     = addSetting(new ColorSetting  ("crit_color",     "Crit Hit Color",    0xFFFFAA00));
    }

    @Override public String getHudId()      { return "damage_dealt_hud"; }
    @Override public String getHudName()    { return "Damage Dealt"; }
    @Override public int getDefaultWidth()  { return 120; }
    @Override public int getDefaultHeight() { return 50; }

    @Override
    public void onEnable() {
        lastEntityHealth.clear();
        lastDamageDealt = 0f;
        lastWasCrit = false;
        lastHitTime = 0L;
        sessionTotal = 0f;
        maxHit = 0f;
        currentDps = 0f;
        dpsWindow.clear();
    }

    @Override
    public void onDisable() {
        lastEntityHealth.clear();
        sessionTotal = 0f;
        maxHit = 0f;
        dpsWindow.clear();
    }

    @Override
    public void onClientTick(MinecraftClient client) {
        if (client.player == null || client.world == null) return;

        // Update DPS window: remove entries older than 5 seconds
        long now = System.currentTimeMillis();
        dpsWindow.removeIf(e -> now - e.timestamp() > 5000L);
        float dpsSum = 0f;
        for (DpsEntry e : dpsWindow) dpsSum += e.damage();
        currentDps = dpsSum / 5.0f;

        // Track targeted entity health
        if (client.targetedEntity instanceof LivingEntity target) {
            UUID id = target.getUuid();
            float currentHp = target.getHealth();
            float prevHp = lastEntityHealth.getOrDefault(id, currentHp);

            float delta = prevHp - currentHp;
            if (delta > 0.01f) {
                lastDamageDealt = delta;
                lastHitTime = now;
                sessionTotal += delta;
                maxHit = Math.max(maxHit, delta);
                dpsWindow.add(new DpsEntry(now, delta));
                lastWasCrit = detectCrit(client);
            }

            lastEntityHealth.put(id, currentHp);
        }

        // Cleanup dead/gone entities
        lastEntityHealth.entrySet().removeIf(e -> {
            // Simple cleanup: if map is too large, clear old entries
            return false; // We rely on the small size of the map naturally
        });
        if (lastEntityHealth.size() > 100) {
            lastEntityHealth.clear();
        }
    }

    private boolean detectCrit(MinecraftClient client) {
        if (client.player == null) return false;
        return client.player.fallDistance > 0
            && !client.player.isOnGround()
            && !client.player.isSprinting()
            && !client.player.isInFluid();
    }

    @Override
    protected void renderHudContent(DrawContext ctx, float tickDelta, int x, int y, int w, int h) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        if (mc.options.hudHidden) return;

        long elapsed = System.currentTimeMillis() - lastHitTime;
        long maxMs = displayTime.get() * 1000L;

        if (hideWhenZero.get() && (lastHitTime == 0L || elapsed > maxMs)) return;

        // Fade-out alpha
        float alpha = 1.0f;
        if (fadeOut.get() && lastHitTime > 0L && elapsed > maxMs - 1000L) {
            alpha = Math.max(0f, (maxMs - elapsed) / 1000.0f);
        }

        int baseColor = lastWasCrit ? critColor.get() : damageColor.get();
        int dmgColor = applyAlpha(baseColor, alpha);
        int txtColor = applyAlpha(textColor.get(), alpha);

        String fmt = "%." + decimalPlaces.get() + "f";

        drawBackground(ctx, x, y, w, h);

        int lineY = y + 4;
        ctx.drawTextWithShadow(mc.textRenderer,
            Text.literal("Hit: " + String.format(fmt, lastDamageDealt) + " ♥"),
            x + 4, lineY, dmgColor);
        lineY += 10;

        if (showTotal.get()) {
            ctx.drawTextWithShadow(mc.textRenderer,
                Text.literal("Total: " + String.format(fmt, sessionTotal)),
                x + 4, lineY, txtColor);
            lineY += 10;
        }
        if (showMaxHit.get()) {
            ctx.drawTextWithShadow(mc.textRenderer,
                Text.literal("Max: " + String.format(fmt, maxHit)),
                x + 4, lineY, applyAlpha(critColor.get(), alpha));
            lineY += 10;
        }
        if (showDps.get()) {
            ctx.drawTextWithShadow(mc.textRenderer,
                Text.literal("DPS: " + String.format(fmt, currentDps)),
                x + 4, lineY, txtColor);
        }
    }

    private static int applyAlpha(int argb, float alpha) {
        int a = (int) (((argb >> 24) & 0xFF) * alpha);
        return (argb & 0x00FFFFFF) | (a << 24);
    }
}
