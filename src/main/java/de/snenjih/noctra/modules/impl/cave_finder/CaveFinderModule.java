package de.snenjih.noctra.modules.impl.cave_finder;

import de.snenjih.noctra.modules.api.BaseModule;
import de.snenjih.noctra.modules.api.ModuleCategory;
import de.snenjih.noctra.modules.api.settings.BooleanSetting;
import de.snenjih.noctra.modules.api.settings.EnumSetting;
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
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

public class CaveFinderModule extends BaseModule {

    public enum SearchMode { DOWN_ONLY, RADIUS, BOTH }

    private record CaveResult(BlockPos cavePos, int depth, int caveSize, double angle) {}

    private final ModuleSetting<Integer>    scanInterval;
    private final ModuleSetting<Integer>    radius;
    private final ModuleSetting<Integer>    minCaveSize;
    private final ModuleSetting<Boolean>    show3dMarker;
    private final ModuleSetting<Integer>    hudX;
    private final ModuleSetting<Integer>    hudY;
    private final ModuleSetting<SearchMode> searchMode;

    private volatile CaveResult lastResult = null;
    private int tickCounter = 0;

    public CaveFinderModule() {
        super(
            "cave_finder",
            "Cave Finder",
            "Locates nearby caves and shows their depth and direction on the HUD.",
            ModuleCategory.WORLD,
            Identifier.of("noctra", "modules/cave_finder")
        );
        scanInterval = addSetting(new IntSetting    ("scan_interval",  "Scan Interval (ticks)", 20,  1,  60));
        radius       = addSetting(new IntSetting    ("radius",         "Search Radius (blocks)", 32, 16,  64));
        minCaveSize  = addSetting(new IntSetting    ("min_cave_size",  "Min Cave Size (blocks)", 4,   1,  20));
        show3dMarker = addSetting(new BooleanSetting("show_3d_marker", "Show 3D Marker",         true));
        beginSection("HUD Position");
        hudX         = addSetting(new IntSetting    ("hud_x",          "HUD X",                  8,   0, 480));
        hudY         = addSetting(new IntSetting    ("hud_y",          "HUD Y",                  8,   0, 240));
        searchMode   = addSetting(new EnumSetting<> ("search_mode",    "Search Mode",  SearchMode.BOTH, SearchMode.class));
    }

    @Override
    public void onEnable()  { lastResult = null; tickCounter = 0; }
    @Override
    public void onDisable() { lastResult = null; }

    @Override
    public void onJoinWorld(ClientWorld world) { lastResult = null; }
    @Override
    public void onLeaveWorld()                 { lastResult = null; }

    @Override
    public void onClientTick(MinecraftClient client) {
        if (tickCounter++ % scanInterval.get() != 0) return;
        if (client.world == null || client.player == null) return;

        ClientWorld world = client.world;
        BlockPos playerPos = client.player.getBlockPos();
        SearchMode mode = searchMode.get();

        CaveResult best = null;

        if (mode == SearchMode.DOWN_ONLY || mode == SearchMode.BOTH) {
            best = scanDown(world, playerPos);
        }

        if (mode == SearchMode.RADIUS || mode == SearchMode.BOTH) {
            CaveResult radiusResult = scanRadius(world, playerPos, client.player);
            if (radiusResult != null) {
                if (best == null || radiusResult.caveSize() > best.caveSize()) {
                    best = radiusResult;
                }
            }
        }

        lastResult = best;
    }

    private CaveResult scanDown(ClientWorld world, BlockPos playerPos) {
        int bottomY = world.getBottomY();
        for (int dy = 1; dy <= 200; dy++) {
            BlockPos checkPos = playerPos.down(dy);
            if (checkPos.getY() < bottomY) break;

            if (world.getBlockState(checkPos).isAir()) {
                int size = floodFillSize(world, checkPos, minCaveSize.get() * 3);
                if (size >= minCaveSize.get()) {
                    return new CaveResult(checkPos, dy, size, 0.0);
                }
            }
        }
        return null;
    }

    private CaveResult scanRadius(ClientWorld world, BlockPos playerPos,
                                  net.minecraft.entity.player.PlayerEntity player) {
        CaveResult best = null;
        int r = radius.get();
        int bottomY = world.getBottomY();
        int topYIncl = world.getTopYInclusive();

        for (int dx = -r; dx <= r; dx += 4) {
            for (int dz = -r; dz <= r; dz += 4) {
                for (int dy = -4; dy >= -100; dy -= 4) {
                    int checkY = playerPos.getY() + dy;
                    if (checkY < bottomY || checkY > topYIncl) break;

                    BlockPos pos = new BlockPos(playerPos.getX() + dx, checkY, playerPos.getZ() + dz);
                    if (!world.isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4)) continue;
                    if (!world.getBlockState(pos).isAir()) continue;

                    int size = floodFillSize(world, pos, minCaveSize.get() * 3);
                    if (size < minCaveSize.get()) continue;

                    int depth = playerPos.getY() - checkY;
                    double dxD = (double) (playerPos.getX() + dx) - player.getX();
                    double dzD = (double) (playerPos.getZ() + dz) - player.getZ();
                    double angle = Math.atan2(dzD, dxD);

                    CaveResult candidate = new CaveResult(pos, depth, size, angle);
                    if (best == null || size > best.caveSize()) {
                        best = candidate;
                    }
                    break;
                }
            }
        }
        return best;
    }

    private int floodFillSize(ClientWorld world, BlockPos start, int cap) {
        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        visited.add(start);
        int count = 0;
        int bottomY = world.getBottomY();
        int topYIncl = world.getTopYInclusive();

        while (!queue.isEmpty() && count < cap) {
            BlockPos current = queue.poll();
            count++;
            for (int i = 0; i < 6; i++) {
                int nx = current.getX() + (i == 0 ? 1 : i == 1 ? -1 : 0);
                int ny = current.getY() + (i == 2 ? 1 : i == 3 ? -1 : 0);
                int nz = current.getZ() + (i == 4 ? 1 : i == 5 ? -1 : 0);
                if (ny < bottomY || ny > topYIncl) continue;
                BlockPos neighbor = new BlockPos(nx, ny, nz);
                if (visited.contains(neighbor)) continue;
                if (world.getBlockState(neighbor).isAir()) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }
        return count;
    }

    @Override
    public void onRenderHud(DrawContext ctx, float tickDelta) {
        CaveResult result = lastResult;
        if (result == null) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        double relAngle = result.angle() - Math.toRadians(mc.player.getYaw() + 90);
        String arrow = (result.angle() == 0.0 && result.depth() > 0) ? "↓" : angleToArrow(relAngle);
        String line = arrow + " Cave: " + result.depth() + "m below (" + result.caveSize() + "+ blocks)";
        ctx.drawTextWithShadow(mc.textRenderer, Text.literal(line), hudX.get(), hudY.get(), 0xFFAA66FF);
    }

    @Override
    public void onRenderWorld(WorldRenderContext ctx) {
        CaveResult result = lastResult;
        if (!show3dMarker.get() || result == null) return;

        Camera camera = ctx.gameRenderer().getCamera();
        Vec3d camPos  = camera.getCameraPos();
        MatrixStack matrices = ctx.matrices();
        VertexConsumerProvider consumers = ctx.consumers();
        if (consumers == null) return;

        BlockPos cave = result.cavePos();
        double relX = cave.getX() + 0.5 - camPos.x;
        double relY = cave.getY() - camPos.y;
        double relZ = cave.getZ() + 0.5 - camPos.z;

        VertexConsumer lines = consumers.getBuffer(RenderLayers.LINES);
        RenderUtil.drawBox(matrices, lines,
            relX - 0.1, relY,     relZ - 0.1,
            relX + 0.1, relY + 8, relZ + 0.1,
            0.67f, 0.4f, 1.0f, 0.9f);
    }

    private static String angleToArrow(double radians) {
        double angle = radians % (2 * Math.PI);
        if (angle < 0) angle += 2 * Math.PI;
        int sector = (int) Math.round(angle / (Math.PI / 4)) % 8;
        return switch (sector) {
            case 0 -> "→";
            case 1 -> "↘";
            case 2 -> "↓";
            case 3 -> "↙";
            case 4 -> "←";
            case 5 -> "↖";
            case 6 -> "↑";
            case 7 -> "↗";
            default -> "→";
        };
    }
}
