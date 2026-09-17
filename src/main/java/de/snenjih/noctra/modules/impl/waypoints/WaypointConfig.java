package de.snenjih.noctra.modules.impl.waypoints;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class WaypointConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path getFile() {
        Path p = FabricLoader.getInstance().getConfigDir().resolve("noctra_waypoints.json");
        if (!Files.exists(p)) {
            Path legacy = FabricLoader.getInstance().getConfigDir().resolve("mandatory_waypoints.json");
            if (Files.exists(legacy)) return legacy;
        }
        return p;
    }

    private static final List<WaypointEntry> waypoints = new ArrayList<>();

    public static List<WaypointEntry> getAll() {
        return waypoints;
    }

    public static void load() {
        waypoints.clear();
        Path file = getFile();
        if (!Files.exists(file)) return;
        try (Reader r = Files.newBufferedReader(file)) {
            JsonArray arr = JsonParser.parseReader(r).getAsJsonArray();
            for (var el : arr) {
                JsonObject obj = el.getAsJsonObject();
                String name      = obj.get("name").getAsString();
                int    x         = obj.get("x").getAsInt();
                int    y         = obj.get("y").getAsInt();
                int    z         = obj.get("z").getAsInt();
                String colorHex  = obj.has("color") ? obj.get("color").getAsString() : "#55FF55";
                String dimension = obj.has("dimension") ? obj.get("dimension").getAsString() : "minecraft:overworld";
                int argb = parseColor(colorHex);
                waypoints.add(new WaypointEntry(name, x, y, z, argb, colorHex, dimension));
            }
        } catch (Exception e) {
            // corrupted file — start fresh
            waypoints.clear();
        }
    }

    public static void save() {
        JsonArray arr = new JsonArray();
        for (WaypointEntry w : waypoints) {
            JsonObject obj = new JsonObject();
            obj.addProperty("name",      w.name());
            obj.addProperty("x",         w.x());
            obj.addProperty("y",         w.y());
            obj.addProperty("z",         w.z());
            obj.addProperty("color",     w.colorHex());
            obj.addProperty("dimension", w.dimension());
            arr.add(obj);
        }
        try (Writer writer = Files.newBufferedWriter(FabricLoader.getInstance().getConfigDir().resolve("noctra_waypoints.json"))) {
            GSON.toJson(arr, writer);
        } catch (IOException ignored) {}
    }

    /** Parse #RRGGBB → 0xFFRRGGBB */
    public static int parseColor(String hex) {
        try {
            String clean = hex.startsWith("#") ? hex.substring(1) : hex;
            return 0xFF000000 | Integer.parseInt(clean, 16);
        } catch (NumberFormatException e) {
            return 0xFF55FF55;
        }
    }
}
