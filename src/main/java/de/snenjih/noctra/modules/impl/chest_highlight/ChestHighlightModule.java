package de.snenjih.noctra.modules.impl.chest_highlight;

import de.snenjih.noctra.modules.api.BaseModule;
import de.snenjih.noctra.modules.api.ModuleCategory;
import de.snenjih.noctra.modules.api.settings.BooleanSetting;
import de.snenjih.noctra.modules.api.settings.ColorSetting;
import de.snenjih.noctra.modules.api.settings.FloatSetting;
import de.snenjih.noctra.modules.api.settings.IntSetting;
import de.snenjih.noctra.modules.api.settings.ModuleSetting;
import de.snenjih.noctra.util.RenderUtil;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.block.BarrelBlock;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.TrappedChestBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class ChestHighlightModule extends BaseModule {

    private enum ContainerType { CHEST, BARREL, SHULKER }
    private record ContainerEntry(BlockPos pos, ContainerType type) {}

    private final List<ContainerEntry> containerCache = new ArrayList<>();
    private int tickCounter = 0;

    private final ModuleSetting<Integer> radius;
    private final ModuleSetting<Integer> scanInterval;
    private final ModuleSetting<Boolean> xray;
    private final ModuleSetting<Integer> colorChest;
    private final ModuleSetting<Integer> colorBarrel;
    private final ModuleSetting<Integer> colorShulker;
    private final ModuleSetting<Float>   lineWidth;

    public ChestHighlightModule() {
        super(
            "chest_highlight",
            "Chest Highlight",
            "Outlines nearby chests, barrels, and shulker boxes.",
            ModuleCategory.WORLD,
            Identifier.of("noctra", "modules/chest_highlight")
        );
        radius       = addSetting(new IntSetting    ("radius",        "Search Radius (blocks)", 32,  8, 64));
        scanInterval = addSetting(new IntSetting    ("scan_interval", "Scan Interval (ticks)",  20,  5, 60));
        xray         = addSetting(new BooleanSetting("xray",          "Show Through Walls",     true));
        beginSection("Colors");
        colorChest   = addSetting(new ColorSetting  ("color_chest",   "Chest Color",   0xFFAA6600));
        colorBarrel  = addSetting(new ColorSetting  ("color_barrel",  "Barrel Color",  0xFF886644));
        colorShulker = addSetting(new ColorSetting  ("color_shulker", "Shulker Color", 0xFFCC55FF));
        beginSection("Appearance");
        lineWidth    = addSetting(new FloatSetting  ("line_width",    "Line Width",    2.0f, 0.5f, 5.0f));
    }

    @Override
    public void onEnable()  { containerCache.clear(); }
    @Override
    public void onDisable() { containerCache.clear(); }

    @Override
    public void onJoinWorld(ClientWorld world) { containerCache.clear(); }
    @Override
    public void onLeaveWorld()                 { containerCache.clear(); }

    @Override
    public void onClientTick(MinecraftClient client) {
        if (tickCounter++ % scanInterval.get() != 0) return;
        if (client.world == null || client.player == null) return;

        ClientWorld world = client.world;
        BlockPos center = client.player.getBlockPos();
        int r = radius.get();

        List<ContainerEntry> newCache = new ArrayList<>();
        int count = 0;

        outer:
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (count >= 500) break outer;

                    BlockPos pos = center.add(dx, dy, dz);
                    if (!world.isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4)) continue;

                    var block = world.getBlockState(pos).getBlock();
                    ContainerType type = null;

                    if (block instanceof ChestBlock || block instanceof TrappedChestBlock) {
                        type = ContainerType.CHEST;
                    } else if (block instanceof BarrelBlock) {
                        type = ContainerType.BARREL;
                    } else if (block instanceof ShulkerBoxBlock) {
                        type = ContainerType.SHULKER;
                    }

                    if (type != null) {
                        newCache.add(new ContainerEntry(pos.toImmutable(), type));
                        count++;
                    }
                }
            }
        }

        containerCache.clear();
        containerCache.addAll(newCache);
    }

    @Override
    public void onRenderWorld(WorldRenderContext ctx) {
        if (containerCache.isEmpty()) return;

        Camera camera = ctx.gameRenderer().getCamera();
        Vec3d  camPos = camera.getCameraPos();
        MatrixStack matrices  = ctx.matrices();
        VertexConsumerProvider consumers = ctx.consumers();
        if (consumers == null) return;

        VertexConsumer lineBuffer = consumers.getBuffer(RenderLayers.LINES);
        float lw = lineWidth.get();

        for (ContainerEntry entry : containerCache) {
            int color = switch (entry.type()) {
                case CHEST   -> colorChest.get();
                case BARREL  -> colorBarrel.get();
                case SHULKER -> colorShulker.get();
            };

            float r = ((color >> 16) & 0xFF) / 255.0f;
            float g = ((color >>  8) & 0xFF) / 255.0f;
            float b = ( color        & 0xFF) / 255.0f;
            float a = ((color >> 24) & 0xFF) / 255.0f;

            double relX = entry.pos().getX() - camPos.x;
            double relY = entry.pos().getY() - camPos.y;
            double relZ = entry.pos().getZ() - camPos.z;

            RenderUtil.drawBox(matrices, lineBuffer,
                relX - 0.01, relY - 0.01, relZ - 0.01,
                relX + 1.01, relY + 1.01, relZ + 1.01,
                r, g, b, a, lw);
        }
    }
}
