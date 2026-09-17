package de.snenjih.noctra.modules.impl.waypoints;

import de.snenjih.noctra.modules.api.BaseModule;
import de.snenjih.noctra.modules.api.ModuleCategory;
import de.snenjih.noctra.modules.api.HudElement;
import de.snenjih.noctra.modules.api.settings.BooleanSetting;
import de.snenjih.noctra.modules.api.settings.IntSetting;
import de.snenjih.noctra.modules.api.settings.ModuleSetting;
import de.snenjih.noctra.util.RenderUtil;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.render.Camera;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class WaypointsModule extends BaseModule implements HudElement {

    private static final String[] COLOR_PALETTE = {
        "#FF5555", "#55FF55", "#5555FF", "#FFFF55", "#FF55FF", "#55FFFF", "#FF8800", "#AA00FF"
    };
    private static int paletteIndex = 0;

    private final ModuleSetting<Boolean> showBeacons;
    private final ModuleSetting<Integer> maxDistance;

    private final List<WaypointEntry> activeWaypoints = new ArrayList<>();
    private String currentDimension = null;
    private int tickCounter = 0;

    public WaypointsModule() {
        super(
            "waypoints",
            "Waypoints",
            "Save locations and display them as HUD markers.",
            ModuleCategory.WORLD,
            Identifier.of("noctra", "modules/waypoints")
        );
        showBeacons = addSetting(new BooleanSetting("show_beacons", "Show 3D Beacons", true));
        maxDistance = addSetting(new IntSetting("max_distance", "Max Render Distance", 2000, 100, 10000));
    }

    @Override public String getHudId()       { return "waypoints"; }
    @Override public String getHudName()     { return "Waypoints"; }
    @Override public int    getDefaultWidth()  { return 200; }
    @Override public int    getDefaultHeight() { return 100; }

    @Override
    public void onEnable() {
        WaypointConfig.load();
        refreshActive();
    }

    @Override
    public void onDisable() {
        activeWaypoints.clear();
    }

    @Override
    public void onJoinWorld(ClientWorld world) {
        currentDimension = world.getRegistryKey().getValue().toString();
        WaypointConfig.load();
        refreshActive();
    }

    @Override
    public void onLeaveWorld() {
        currentDimension = null;
        activeWaypoints.clear();
    }

    private void refreshActive() {
        activeWaypoints.clear();
        if (currentDimension == null) return;
        for (WaypointEntry w : WaypointConfig.getAll()) {
            if (currentDimension.equals(w.dimension())) {
                activeWaypoints.add(w);
            }
        }
    }

    @Override
    public void onClientTick(MinecraftClient client) {
        if (tickCounter++ % 10 != 0) return;
        if (client.player == null || client.world == null) return;

        double px = client.player.getX();
        double py = client.player.getY();
        double pz = client.player.getZ();

        for (WaypointEntry w : activeWaypoints) {
            double dx = w.x() + 0.5 - px;
            double dy = w.y() - py;
            double dz = w.z() + 0.5 - pz;
            w.cachedDistance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            w.cachedAngle    = Math.atan2(dz, dx);
        }
    }

    @Override
    public ActionResult onSendChat(String message) {
        if (!message.startsWith(".wp ")) return ActionResult.PASS;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || currentDimension == null) return ActionResult.SUCCESS;

        String[] parts = message.split(" ", 3);
        if (parts.length < 2) return ActionResult.SUCCESS;

        String sub = parts[1];

        switch (sub) {
            case "add" -> {
                if (parts.length < 3) {
                    mc.player.sendMessage(Text.literal("[Waypoints] Usage: .wp add <name> [#color]"), false);
                    return ActionResult.SUCCESS;
                }
                long count = WaypointConfig.getAll().stream()
                        .filter(w -> currentDimension.equals(w.dimension())).count();
                if (count >= 20) {
                    mc.player.sendMessage(Text.literal("[Waypoints] Waypoint limit reached (max 20)."), false);
                    return ActionResult.SUCCESS;
                }
                String rest = parts[2].trim();
                String name;
                String colorHex;
                String[] tokens = rest.split(" ");
                if (tokens.length >= 2 && tokens[tokens.length - 1].startsWith("#")) {
                    colorHex = tokens[tokens.length - 1];
                    name = rest.substring(0, rest.lastIndexOf(' ')).trim();
                } else {
                    colorHex = COLOR_PALETTE[paletteIndex++ % COLOR_PALETTE.length];
                    name = rest;
                }
                int argb = WaypointConfig.parseColor(colorHex);
                int px = (int) mc.player.getX();
                int py = (int) mc.player.getY();
                int pz = (int) mc.player.getZ();
                WaypointConfig.getAll().add(new WaypointEntry(name, px, py, pz, argb, colorHex, currentDimension));
                WaypointConfig.save();
                refreshActive();
                mc.player.sendMessage(Text.literal("[Waypoints] Added: " + name + " (" + px + ", " + py + ", " + pz + ")"), false);
            }
            case "remove" -> {
                if (parts.length < 3) return ActionResult.SUCCESS;
                String name = parts[2].trim();
                boolean removed = WaypointConfig.getAll().removeIf(
                    w -> currentDimension.equals(w.dimension()) && w.name().equalsIgnoreCase(name)
                );
                if (removed) {
                    WaypointConfig.save();
                    refreshActive();
                    mc.player.sendMessage(Text.literal("[Waypoints] Removed: " + name), false);
                } else {
                    mc.player.sendMessage(Text.literal("[Waypoints] Not found: " + name), false);
                }
            }
            case "list" -> {
                mc.player.sendMessage(Text.literal("[Waypoints] (" + currentDimension + "):"), false);
                for (WaypointEntry w : activeWaypoints) {
                    mc.player.sendMessage(Text.literal("  " + w.name() + " @ " + w.x() + ", " + w.y() + ", " + w.z()
                        + " (" + (int) w.cachedDistance + "m)"), false);
                }
            }
            case "clear" -> {
                WaypointConfig.getAll().removeIf(w -> currentDimension.equals(w.dimension()));
                WaypointConfig.save();
                refreshActive();
                mc.player.sendMessage(Text.literal("[Waypoints] All waypoints cleared for this dimension."), false);
            }
            default -> mc.player.sendMessage(Text.literal("[Waypoints] Commands: .wp add/remove/list/clear"), false);
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public void renderHud(DrawContext ctx, float tickDelta, int x, int y, int w, int h) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;
        if (activeWaypoints.isEmpty()) return;

        float playerYaw = mc.player.getYaw();
        int maxDist = maxDistance.get();

        List<WaypointEntry> sorted = activeWaypoints.stream()
            .filter(wp -> wp.cachedDistance <= maxDist)
            .sorted(Comparator.comparingDouble(wp -> wp.cachedDistance))
            .toList();

        int drawY = y;
        for (WaypointEntry wp : sorted) {
            double relAngle = wp.cachedAngle - Math.toRadians(playerYaw + 90);
            String arrow = angleToArrow(relAngle);
            String dist  = formatDistance(wp.cachedDistance);
            String label = arrow + " " + wp.name() + " §7" + dist;
            ctx.drawTextWithShadow(mc.textRenderer, Text.literal(label), x, drawY, wp.colorArgb());
            drawY += 10;
        }
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

    private static String formatDistance(double blocks) {
        if (blocks >= 1000) return String.format("%.1fkm", blocks / 1000.0);
        return (int) blocks + "m";
    }

    @Override
    public void onRenderWorld(WorldRenderContext ctx) {
        if (!showBeacons.get() || activeWaypoints.isEmpty()) return;

        Camera camera = ctx.gameRenderer().getCamera();
        Vec3d camPos  = camera.getCameraPos();
        MatrixStack matrices = ctx.matrices();
        VertexConsumerProvider consumers = ctx.consumers();
        if (consumers == null) return;

        VertexConsumer lineBuffer = consumers.getBuffer(RenderLayers.LINES);
        int maxDist = maxDistance.get();

        for (WaypointEntry w : activeWaypoints) {
            if (w.cachedDistance > maxDist) continue;

            float r = ((w.colorArgb() >> 16) & 0xFF) / 255.0f;
            float g = ((w.colorArgb() >>  8) & 0xFF) / 255.0f;
            float b = ( w.colorArgb()        & 0xFF) / 255.0f;

            double relX = w.x() + 0.5 - camPos.x;
            double relZ = w.z() + 0.5 - camPos.z;
            double bottomY = w.y() - camPos.y;
            double topY    = bottomY + 64;

            RenderUtil.drawBox(matrices, lineBuffer,
                relX - 0.05, bottomY, relZ - 0.05,
                relX + 0.05, topY,    relZ + 0.05,
                r, g, b, 0.8f);
        }
    }
}
