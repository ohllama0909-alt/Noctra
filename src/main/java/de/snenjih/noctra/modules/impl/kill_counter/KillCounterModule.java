package de.snenjih.noctra.modules.impl.kill_counter;

import de.snenjih.noctra.modules.api.BaseHudModule;
import de.snenjih.noctra.modules.api.ModuleCategory;
import de.snenjih.noctra.modules.api.settings.BooleanSetting;
import de.snenjih.noctra.modules.api.settings.ColorSetting;
import de.snenjih.noctra.modules.api.settings.ModuleSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class KillCounterModule extends BaseHudModule {

    private final ModuleSetting<Boolean> showSessionTime;
    private final ModuleSetting<Boolean> resetOnDeath;
    private final ModuleSetting<Integer> killTextColor;

    private int killCount = 0;
    private final Map<Integer, Float> trackedEntities = new HashMap<>();
    private long sessionStartTime = 0L;
    private boolean wasDeadLastTick = false;

    public KillCounterModule() {
        super(
            "kill_counter",
            "Kill Counter",
            "Counts your kills this session and displays them on the HUD.",
            ModuleCategory.COMBAT,
            Identifier.of("noctra", "modules/kill_counter")
        );
        showSessionTime = addSetting(new BooleanSetting("show_session_time", "Show Session Time", false));
        resetOnDeath    = addSetting(new BooleanSetting("reset_on_death",    "Reset on Death",    false));
        killTextColor   = addSetting(new ColorSetting  ("text_color",        "Text Color",        0xFFFFFFFF));
    }

    @Override public String getHudId()      { return "kill_counter"; }
    @Override public String getHudName()    { return "Kill Counter"; }
    @Override public int getDefaultWidth()  { return 100; }
    @Override public int getDefaultHeight() { return 12; }

    @Override
    public void onEnable() {
        killCount = 0;
        trackedEntities.clear();
        sessionStartTime = System.currentTimeMillis();
        wasDeadLastTick = false;
    }

    @Override
    public void onDisable() {
        killCount = 0;
        trackedEntities.clear();
    }

    @Override
    public ActionResult onAttackEntity(ClientPlayerEntity player, Entity target) {
        if (target instanceof LivingEntity living) {
            trackedEntities.put(target.getId(), living.getHealth());
            // Safety cap
            if (trackedEntities.size() > 200) {
                trackedEntities.clear();
            }
        }
        return ActionResult.PASS;
    }

    @Override
    public void onClientTick(MinecraftClient client) {
        if (client.player == null || client.world == null) return;

        Iterator<Map.Entry<Integer, Float>> iter = trackedEntities.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<Integer, Float> entry = iter.next();
            int entityId = entry.getKey();
            Entity entity = client.world.getEntityById(entityId);

            if (entity == null) {
                // Entity no longer in world — use last-known HP heuristic
                if (entry.getValue() <= 3.0f) {
                    killCount++;
                }
                iter.remove();
                continue;
            }

            if (entity instanceof LivingEntity living) {
                if (living.getHealth() <= 0f) {
                    killCount++;
                    iter.remove();
                } else if (entity.isRemoved()) {
                    iter.remove();
                } else {
                    // Update last known health
                    entry.setValue(living.getHealth());
                }
            } else {
                iter.remove();
            }
        }

        // Reset on death
        if (resetOnDeath.get()) {
            boolean isDead = client.player.isDead();
            if (isDead && !wasDeadLastTick) {
                killCount = 0;
                trackedEntities.clear();
            }
            wasDeadLastTick = isDead;
        }
    }

    @Override
    protected void renderHudContent(DrawContext ctx, float tickDelta, int x, int y, int w, int h) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        if (mc.options.hudHidden) return;

        String killText = "Kills: " + killCount;

        if (showSessionTime.get()) {
            long elapsedMs = System.currentTimeMillis() - sessionStartTime;
            long seconds = elapsedMs / 1000;
            long minutes = seconds / 60;
            seconds = seconds % 60;
            killText += String.format(" (%02d:%02d)", minutes, seconds);
        }

        drawBackground(ctx, x, y, w, h);
        ctx.drawTextWithShadow(mc.textRenderer, Text.literal(killText), x + 4, y + 2, killTextColor.get());
    }
}
