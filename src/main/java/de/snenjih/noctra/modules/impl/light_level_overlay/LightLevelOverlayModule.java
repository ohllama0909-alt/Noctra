package de.snenjih.noctra.modules.impl.light_level_overlay;

import de.snenjih.noctra.modules.api.BaseModule;
import de.snenjih.noctra.modules.api.ModuleCategory;
import de.snenjih.noctra.modules.api.settings.BooleanSetting;
import de.snenjih.noctra.modules.api.settings.IntSetting;
import de.snenjih.noctra.modules.api.settings.ModuleSetting;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LightType;

import java.util.ArrayList;
import java.util.List;

public class LightLevelOverlayModule extends BaseModule {

    private record LightEntry(BlockPos pos, int level) {}

    private final List<LightEntry> lightCache = new ArrayList<>();
    private int tickCounter = 0;

    private final ModuleSetting<Integer> radius;
    private final ModuleSetting<Boolean> onlyDangerous;
    private final ModuleSetting<Boolean> useBlockLight;
    private final ModuleSetting<Integer> scanInterval;

    public LightLevelOverlayModule() {
        super(
            "light_level_overlay",
            "Light Level Overlay",
            "Shows light levels on blocks to warn about mob spawn locations.",
            ModuleCategory.WORLD,
            Identifier.of("noctra", "modules/light_level_overlay")
        );
        radius        = addSetting(new IntSetting    ("radius",         "Radius (blocks)",      12, 1,  20));
        onlyDangerous = addSetting(new BooleanSetting("only_dangerous", "Only Dangerous Blocks", false));
        useBlockLight = addSetting(new BooleanSetting("use_block_light","Use Block Light Only",  false));
        scanInterval  = addSetting(new IntSetting    ("scan_interval",  "Scan Interval (ticks)", 10, 1,  40));
    }

    @Override
    public void onEnable()  { lightCache.clear(); tickCounter = 0; }
    @Override
    public void onDisable() { lightCache.clear(); tickCounter = 0; }

    @Override
    public void onJoinWorld(ClientWorld world) { lightCache.clear(); }
    @Override
    public void onLeaveWorld()                 { lightCache.clear(); }

    @Override
    public void onClientTick(MinecraftClient client) {
        if (tickCounter++ % scanInterval.get() != 0) return;
        if (client.world == null || client.player == null) return;

        ClientWorld world = client.world;
        BlockPos center = client.player.getBlockPos();
        int r = radius.get();
        int bottomY = world.getBottomY();
        int topYIncl = world.getTopYInclusive();

        List<LightEntry> newCache = new ArrayList<>();

        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                for (int dy = -4; dy <= 4; dy++) {
                    BlockPos pos = center.add(dx, dy, dz);
                    if (pos.getY() < bottomY || pos.getY() >= topYIncl) continue;

                    var floorState = world.getBlockState(pos);
                    if (!floorState.isSolidBlock(world, pos)) continue;

                    BlockPos abovePos = pos.up();
                    var aboveState = world.getBlockState(abovePos);
                    if (!aboveState.isAir()) continue;

                    int light;
                    if (useBlockLight.get()) {
                        light = world.getLightLevel(LightType.BLOCK, abovePos);
                    } else {
                        light = world.getLightLevel(abovePos);
                    }

                    if (onlyDangerous.get() && light >= 12) continue;

                    newCache.add(new LightEntry(pos, light));
                }
            }
        }

        lightCache.clear();
        lightCache.addAll(newCache);
    }

    @Override
    public void onRenderWorld(WorldRenderContext ctx) {
        if (lightCache.isEmpty()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;

        Camera camera = ctx.gameRenderer().getCamera();
        Vec3d camPos  = camera.getCameraPos();
        MatrixStack matrices = ctx.matrices();

        VertexConsumerProvider.Immediate immediate = mc.getBufferBuilders().getEntityVertexConsumers();

        for (LightEntry entry : lightCache) {
            double relX = entry.pos().getX() + 0.5 - camPos.x;
            double relY = entry.pos().getY() + 1.01 - camPos.y;
            double relZ = entry.pos().getZ() + 0.5 - camPos.z;

            double distSq = relX * relX + relY * relY + relZ * relZ;
            if (distSq > 400.0) continue; // > 20 blocks

            int color = colorForLevel(entry.level());
            String label = String.valueOf(entry.level());
            float textX = -mc.textRenderer.getWidth(label) / 2.0f;

            matrices.push();
            matrices.translate(relX, relY, relZ);
            matrices.multiply(camera.getRotation());
            matrices.scale(-0.025f, -0.025f, 0.025f);

            mc.textRenderer.draw(
                label, textX, 0, color, false,
                matrices.peek().getPositionMatrix(),
                immediate,
                TextRenderer.TextLayerType.SEE_THROUGH,
                0,
                LightmapTextureManager.MAX_LIGHT_COORDINATE
            );

            matrices.pop();
        }
        immediate.draw();
    }

    private static int colorForLevel(int level) {
        if (level <= 7)  return 0xFFFF4444;
        if (level <= 11) return 0xFFFFAA00;
        return 0xFF44FF44;
    }
}
