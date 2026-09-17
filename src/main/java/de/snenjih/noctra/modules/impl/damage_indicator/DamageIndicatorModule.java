package de.snenjih.noctra.modules.impl.damage_indicator;

import de.snenjih.noctra.modules.api.BaseModule;
import de.snenjih.noctra.modules.api.ModuleCategory;
import de.snenjih.noctra.modules.api.settings.BooleanSetting;
import de.snenjih.noctra.modules.api.settings.ColorSetting;
import de.snenjih.noctra.modules.api.settings.FloatSetting;
import de.snenjih.noctra.modules.api.settings.IntSetting;
import de.snenjih.noctra.modules.api.settings.ModuleSetting;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class DamageIndicatorModule extends BaseModule {

    public static DamageIndicatorModule INSTANCE = null;

    public final ModuleSetting<Boolean> showDamage;
    public final ModuleSetting<Boolean> showHealing;
    public final ModuleSetting<Integer> damageColor;
    public final ModuleSetting<Integer> healingColor;
    public final ModuleSetting<Float>   floatSpeed;
    public final ModuleSetting<Integer> durationTicks;
    public final ModuleSetting<Boolean> showPlayerDamage;

    private final List<DamageParticle> particles = new ArrayList<>();

    public DamageIndicatorModule() {
        super(
            "damage_indicator",
            "Damage Indicator",
            "Shows floating damage numbers above entities when they take damage.",
            ModuleCategory.VISUAL,
            Identifier.of("noctra", "modules/damage_indicator")
        );
        showDamage       = addSetting(new BooleanSetting("show_damage",        "Show Damage",      true));
        showHealing      = addSetting(new BooleanSetting("show_healing",       "Show Healing",     true));
        beginSection("Colors");
        damageColor      = addSetting(new ColorSetting  ("damage_color",       "Damage Color",     0xFFFF5555));
        healingColor     = addSetting(new ColorSetting  ("healing_color",      "Healing Color",    0xFF55FF55));
        beginSection("Behavior");
        floatSpeed       = addSetting(new FloatSetting  ("float_speed",        "Float Speed",      1.0f, 0.1f, 3.0f));
        durationTicks    = addSetting(new IntSetting    ("duration_ticks",     "Duration (ticks)", 30, 10, 80));
        showPlayerDamage = addSetting(new BooleanSetting("show_player_damage", "Show Own Damage",  true));
    }

    @Override
    public void onEnable()  { INSTANCE = this; particles.clear(); }
    @Override
    public void onDisable() { INSTANCE = null; particles.clear(); }

    public void addParticle(Vec3d entityPos, float delta, float entityHeight) {
        if (particles.size() >= 50) particles.remove(0);
        Vec3d pos    = entityPos.add(0, entityHeight + 0.3, 0);
        boolean heal = delta < 0;
        particles.add(new DamageParticle(pos, Math.abs(delta), heal, durationTicks.get()));
    }

    @Override
    public void onClientTick(MinecraftClient mc) {
        if (mc.world == null) return;
        float rise = floatSpeed.get() * 0.05f;
        for (DamageParticle p : particles) {
            p.yRise += rise;
            p.ticksLeft--;
        }
        particles.removeIf(p -> p.ticksLeft <= 0);
    }

    @Override
    public void onRenderWorld(WorldRenderContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null || particles.isEmpty()) return;

        MatrixStack matrices  = ctx.matrices();
        Camera      camera    = ctx.gameRenderer().getCamera();
        Vec3d       camPos    = camera.getCameraPos();
        VertexConsumerProvider.Immediate immediate = mc.getBufferBuilders().getEntityVertexConsumers();

        for (DamageParticle p : new ArrayList<>(particles)) {
            Vec3d rel      = p.basePos.add(0, p.yRise, 0).subtract(camPos);
            float alphaFrac = Math.min(1.0f, p.ticksLeft / 10.0f);
            int   base     = p.isHeal ? healingColor.get() : damageColor.get();
            int   alpha    = (int) (((base >> 24) & 0xFF) * alphaFrac);
            int   color    = (alpha << 24) | (base & 0x00FFFFFF);
            String text    = (p.isHeal ? "+" : "-") + String.format("%.1f", p.amount);

            matrices.push();
            matrices.translate(rel.x, rel.y, rel.z);
            matrices.multiply(camera.getRotation());
            matrices.scale(-0.025f, -0.025f, 0.025f);

            mc.textRenderer.draw(
                text,
                -mc.textRenderer.getWidth(text) / 2f,
                0,
                color,
                true,
                matrices.peek().getPositionMatrix(),
                immediate,
                TextRenderer.TextLayerType.NORMAL,
                0,
                LightmapTextureManager.MAX_LIGHT_COORDINATE
            );
            immediate.draw();
            matrices.pop();
        }
    }

    public static class DamageParticle {
        public final Vec3d   basePos;
        public final float   amount;
        public final boolean isHeal;
        public       int     ticksLeft;
        public       float   yRise;

        DamageParticle(Vec3d basePos, float amount, boolean isHeal, int maxTicks) {
            this.basePos   = basePos;
            this.amount    = amount;
            this.isHeal    = isHeal;
            this.ticksLeft = maxTicks;
            this.yRise     = 0f;
        }
    }
}
