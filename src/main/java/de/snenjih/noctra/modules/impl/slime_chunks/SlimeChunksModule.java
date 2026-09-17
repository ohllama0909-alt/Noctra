package de.snenjih.noctra.modules.impl.slime_chunks;

import de.snenjih.noctra.modules.api.BaseModule;
import de.snenjih.noctra.modules.api.ModuleCategory;
import de.snenjih.noctra.modules.api.settings.BooleanSetting;
import de.snenjih.noctra.modules.api.settings.IntSetting;
import de.snenjih.noctra.modules.api.settings.ModuleSetting;
import de.snenjih.noctra.util.RenderUtil;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Random;

public class SlimeChunksModule extends BaseModule {

    private final ModuleSetting<Integer> radius;
    private final ModuleSetting<Boolean> showNonSlime;
    private final ModuleSetting<Integer> opacity;
    private final ModuleSetting<Integer> heightOffset;

    private long    worldSeed = 0L;
    private boolean hasSeed   = false;

    public SlimeChunksModule() {
        super(
            "slime_chunks",
            "Slime Chunks",
            "Highlights slime chunks in the world based on the world seed.",
            ModuleCategory.WORLD,
            Identifier.of("noctra", "modules/slime_chunks")
        );
        radius       = addSetting(new IntSetting    ("radius",         "Chunk Radius",     4,   1,  8));
        showNonSlime = addSetting(new BooleanSetting("show_non_slime", "Show Non-Slime",   false));
        opacity      = addSetting(new IntSetting    ("opacity",        "Overlay Opacity",  80,  10, 255));
        heightOffset = addSetting(new IntSetting    ("height_offset",  "Y Offset",         0,  -64, 320));
    }

    @Override
    public void onEnable() {
        tryLoadSeed();
    }

    @Override
    public void onDisable() {
        hasSeed   = false;
        worldSeed = 0L;
    }

    @Override
    public void onJoinWorld(ClientWorld world) {
        hasSeed   = false;
        worldSeed = 0L;
        tryLoadSeed();
    }

    @Override
    public void onLeaveWorld() {
        hasSeed   = false;
        worldSeed = 0L;
    }

    private void tryLoadSeed() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.isInSingleplayer()) {
            IntegratedServer server = mc.getServer();
            if (server != null) {
                worldSeed = server.getOverworld().getSeed();
                hasSeed   = true;
            }
        }
    }

    public void setSeed(long seed) {
        this.worldSeed = seed;
        this.hasSeed   = true;
    }

    public boolean hasSeed() { return hasSeed; }

    @Override
    public ActionResult onSendChat(String message) {
        if (!message.startsWith(".slimeseed ")) return ActionResult.PASS;
        String arg = message.substring(".slimeseed ".length()).trim();
        try {
            setSeed(Long.parseLong(arg));
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player != null) {
                mc.player.sendMessage(Text.literal("[Slime Chunks] Seed set to: " + worldSeed), false);
            }
        } catch (NumberFormatException e) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player != null) {
                mc.player.sendMessage(Text.literal("[Slime Chunks] Invalid seed (must be a number)."), false);
            }
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public void onRenderHud(DrawContext ctx, float tickDelta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;
        if (!hasSeed) {
            ctx.drawTextWithShadow(mc.textRenderer,
                Text.literal("Slime Chunks: No seed! Use .slimeseed <seed>"),
                8, 8, 0xFFFF5555);
        }
    }

    @Override
    public void onRenderWorld(WorldRenderContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;
        if (!hasSeed) return;

        if (!mc.world.getRegistryKey().equals(World.OVERWORLD)) return;

        Camera camera = ctx.gameRenderer().getCamera();
        Vec3d camPos  = camera.getCameraPos();
        MatrixStack matrices = ctx.matrices();
        VertexConsumerProvider consumers = ctx.consumers();
        if (consumers == null) return;

        VertexConsumer lines = consumers.getBuffer(RenderLayers.LINES);

        int playerChunkX = mc.player.getBlockPos().getX() >> 4;
        int playerChunkZ = mc.player.getBlockPos().getZ() >> 4;
        int r = radius.get();
        // Ignore opacity for line rendering; use full alpha for visibility
        float lineAlpha = Math.max(0.2f, opacity.get() / 255.0f);

        int playerY = mc.player.getBlockPos().getY() + heightOffset.get();
        double boxY1 = playerY - 0.05 - camPos.y;
        double boxY2 = playerY + 0.05 - camPos.y;

        for (int cx = playerChunkX - r; cx <= playerChunkX + r; cx++) {
            for (int cz = playerChunkZ - r; cz <= playerChunkZ + r; cz++) {
                boolean isSlime = isSlimeChunk(worldSeed, cx, cz);
                if (!isSlime && !showNonSlime.get()) continue;

                double x1 = cx * 16.0 - camPos.x;
                double z1 = cz * 16.0 - camPos.z;
                double x2 = x1 + 16.0;
                double z2 = z1 + 16.0;

                float lineR, lineG, lineB;
                if (isSlime) {
                    lineR = 0f; lineG = 1f; lineB = 0f;
                } else {
                    lineR = 1f; lineG = 0f; lineB = 0f;
                }

                RenderUtil.drawBox(matrices, lines,
                    x1, boxY1, z1, x2, boxY2, z2,
                    lineR, lineG, lineB, lineAlpha);
            }
        }
    }

    public static boolean isSlimeChunk(long seed, int chunkX, int chunkZ) {
        Random rng = new Random(
            seed
            + (long) (chunkX * chunkX * 0x4C1906)
            + (long) (chunkX * 0x5AC0DB)
            + (long) (chunkZ * chunkZ) * 0x4307A7L
            + (long) (chunkZ * 0x5F24F)
            ^ 0x3AD8025FL
        );
        return rng.nextInt(10) == 0;
    }
}
