package de.snenjih.noctra.modules.impl.item_age_timer;

import de.snenjih.noctra.modules.api.BaseModule;
import de.snenjih.noctra.modules.api.ModuleCategory;
import de.snenjih.noctra.modules.api.settings.BooleanSetting;
import de.snenjih.noctra.modules.api.settings.FloatSetting;
import de.snenjih.noctra.modules.api.settings.IntSetting;
import de.snenjih.noctra.modules.api.settings.ModuleSetting;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.ItemEntity;
import net.minecraft.util.Identifier;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.Comparator;
import java.util.List;

public class ItemAgeTimerModule extends BaseModule {

    private static final int MAX_ITEM_AGE = 6000;
    private static final int MAX_RENDER   = 50;

    private final ModuleSetting<Integer> renderRadius;
    private final ModuleSetting<Boolean> showOnlyLowTime;
    private final ModuleSetting<Integer> lowTimeThreshold;
    private final ModuleSetting<Float>   textScale;

    public ItemAgeTimerModule() {
        super(
            "item_age_timer",
            "Item Age Timer",
            "Shows remaining despawn time above dropped items in the world.",
            ModuleCategory.VISUAL,
            Identifier.of("noctra", "modules/item_age_timer")
        );
        renderRadius     = addSetting(new IntSetting("render_radius",      "Render Radius",        16, 4,  64));
        showOnlyLowTime  = addSetting(new BooleanSetting("show_only_low_time", "Only Show Expiring", false));
        lowTimeThreshold = addSetting(new IntSetting("low_time_threshold", "Expiry Threshold (s)", 60, 5, 120));
        textScale        = addSetting(new FloatSetting("text_scale",       "Text Scale",           0.5f, 0.2f, 1.0f));
    }

    @Override
    public void onRenderWorld(WorldRenderContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        Vec3d playerPos = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        double radius   = renderRadius.get();

        Box searchBox = new Box(
            playerPos.x - radius, playerPos.y - radius, playerPos.z - radius,
            playerPos.x + radius, playerPos.y + radius, playerPos.z + radius
        );
        List<ItemEntity> items = mc.world.getEntitiesByClass(
            ItemEntity.class, searchBox, e -> !e.isRemoved()
        );

        int threshold = lowTimeThreshold.get();
        List<ItemEntity> toRender = items.stream()
            .filter(item -> {
                int remaining = (MAX_ITEM_AGE - item.getItemAge()) / 20;
                return !showOnlyLowTime.get() || remaining <= threshold;
            })
            .sorted(Comparator.comparingInt(item -> (MAX_ITEM_AGE - item.getItemAge())))
            .limit(MAX_RENDER)
            .toList();

        MatrixStack matrices   = ctx.matrices();
        Camera      camera     = ctx.gameRenderer().getCamera();
        Vec3d       camPos     = camera.getCameraPos();

        VertexConsumerProvider.Immediate immediate = mc.getBufferBuilders().getEntityVertexConsumers();

        for (ItemEntity item : toRender) {
            int remainingTicks   = MAX_ITEM_AGE - item.getItemAge();
            if (remainingTicks <= 0) continue;
            int remainingSeconds = remainingTicks / 20;

            int color;
            if (remainingSeconds > threshold)         color = 0x55FF55;
            else if (remainingSeconds > threshold / 2) color = 0xFFFF55;
            else                                      color = 0xFF5555;

            String text = remainingSeconds >= 60
                ? (remainingSeconds / 60) + ":" + String.format("%02d", remainingSeconds % 60)
                : remainingSeconds + "s";

            Vec3d itemPos  = new Vec3d(item.getX(), item.getY() + 0.5, item.getZ());
            Vec3d relative = itemPos.subtract(camPos);

            float scale = textScale.get();
            matrices.push();
            matrices.translate((float) relative.x, (float) relative.y, (float) relative.z);
            matrices.multiply(camera.getRotation());
            matrices.scale(-scale, -scale, scale);

            mc.textRenderer.draw(
                text,
                -mc.textRenderer.getWidth(text) / 2f,
                0,
                color,
                true,
                matrices.peek().getPositionMatrix(),
                immediate,
                TextRenderer.TextLayerType.NORMAL,
                0x44000000,
                LightmapTextureManager.MAX_LIGHT_COORDINATE
            );
            immediate.draw();
            matrices.pop();
        }
    }
}
