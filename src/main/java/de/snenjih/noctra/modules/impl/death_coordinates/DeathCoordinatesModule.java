package de.snenjih.noctra.modules.impl.death_coordinates;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import de.snenjih.noctra.modules.api.BaseHudModule;
import de.snenjih.noctra.modules.api.ModuleCategory;
import de.snenjih.noctra.modules.api.settings.BooleanSetting;
import de.snenjih.noctra.modules.api.settings.IntSetting;
import de.snenjih.noctra.modules.api.settings.ModuleSetting;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class DeathCoordinatesModule extends BaseHudModule {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final ModuleSetting<Integer> maxEntries;
    private final ModuleSetting<Boolean> showHud;

    private boolean wasDeadLastTick = false;
    private final List<DeathEntry> deaths = new ArrayList<>();

    public DeathCoordinatesModule() {
        super(
            "death_coordinates",
            "Death Coordinates",
            "Saves your last death positions and shows them on the HUD.",
            ModuleCategory.UTILITY,
            Identifier.of("noctra", "modules/death_coordinates")
        );
        maxEntries = addSetting(new IntSetting("max_entries", "Max Entries", 5, 1, 20));
        showHud    = addSetting(new BooleanSetting("show_hud", "Show HUD", true));
    }

    // ── HudElement ────────────────────────────────────────────────────────────

    @Override public String getHudId()      { return "death_coordinates"; }
    @Override public String getHudName()    { return "Death Coordinates"; }
    @Override public int getDefaultWidth()  { return 180; }
    @Override public int getDefaultHeight() { return 24; }

    @Override
    protected void renderHudContent(DrawContext ctx, float tickDelta, int x, int y, int w, int h) {
        if (!showHud.get() || deaths.isEmpty()) return;
        DeathEntry last = deaths.get(0);
        String text = "Last death: " + last.x + ", " + last.y + ", " + last.z
                + " [" + shortDim(last.dimension) + "]";

        drawBackground(ctx, x, y, w, h);
        ctx.drawTextWithShadow(
                MinecraftClient.getInstance().textRenderer,
                Text.literal(text),
                x + 4, y + (h - 8) / 2,
                textColor.get());
    }

    // ── Tick logic ────────────────────────────────────────────────────────────

    @Override
    public void onJoinWorld(ClientWorld world) {
        wasDeadLastTick = false;
        loadFromFile();
    }

    @Override
    public void onClientTick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null) return;

        boolean isDead = player.isDead();
        if (isDead && !wasDeadLastTick) {
            BlockPos pos     = player.getBlockPos();
            String dimension = client.world.getRegistryKey().getValue().toString();
            long timestamp   = System.currentTimeMillis();
            deaths.add(0, new DeathEntry(pos.getX(), pos.getY(), pos.getZ(), dimension, timestamp));
            while (deaths.size() > maxEntries.get()) deaths.remove(deaths.size() - 1);
            saveToFile();
            sendDeathMessage(client, pos, dimension);
        }
        wasDeadLastTick = isDead;
    }

    private void sendDeathMessage(MinecraftClient client, BlockPos pos, String dimension) {
        Text msg = Text.literal("[Noctra] Died at "
                + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()
                + " in " + shortDim(dimension))
                .copy().styled(s -> s.withColor(0xFF5555));
        client.inGameHud.getChatHud().addMessage(msg);
    }

    private String shortDim(String dim) {
        if ("minecraft:overworld".equals(dim))  return "OW";
        if ("minecraft:the_nether".equals(dim)) return "NT";
        if ("minecraft:the_end".equals(dim))    return "END";
        int colon = dim.lastIndexOf(':');
        return colon >= 0 ? dim.substring(colon + 1) : dim;
    }

    private Path savePath() {
        Path p = FabricLoader.getInstance().getConfigDir().resolve("noctra_deaths.json");
        if (!Files.exists(p)) {
            Path legacy = FabricLoader.getInstance().getConfigDir().resolve("mandatory_deaths.json");
            if (Files.exists(legacy)) return legacy;
        }
        return p;
    }

    private void saveToFile() {
        try {
            Files.writeString(savePath(), GSON.toJson(deaths));
        } catch (IOException e) {
            System.err.println("[Noctra] Failed to save death coordinates: " + e.getMessage());
        }
    }

    private void loadFromFile() {
        Path path = savePath();
        if (!Files.exists(path)) return;
        try {
            String json = Files.readString(path);
            Type listType = new TypeToken<List<DeathEntry>>() {}.getType();
            List<DeathEntry> loaded = GSON.fromJson(json, listType);
            deaths.clear();
            if (loaded != null) deaths.addAll(loaded);
        } catch (Exception e) {
            System.err.println("[Noctra] Failed to load death coordinates: " + e.getMessage());
        }
    }

    private static class DeathEntry {
        int x, y, z;
        String dimension;
        long timestamp;

        DeathEntry(int x, int y, int z, String dimension, long timestamp) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.dimension = dimension;
            this.timestamp = timestamp;
        }
    }
}
