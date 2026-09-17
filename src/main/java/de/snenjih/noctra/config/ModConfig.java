package de.snenjih.noctra.config;

import com.google.gson.*;
import de.snenjih.noctra.modules.api.BaseModule;
import de.snenjih.noctra.modules.api.settings.ModuleSetting;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class ModConfig {

    private static final Gson GSON        = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir().resolve("noctra.json");
    private static final int  VERSION     = 3;

    private static ModConfig INSTANCE;

    // moduleId → JsonObject with "enabled" + optional setting keys
    private final Map<String, JsonObject> data = new HashMap<>();

    // hudId → { "x", "y", "w", "h", "visible" }
    private final Map<String, JsonObject> hudData = new HashMap<>();

    public ModConfig() {
        INSTANCE = this;
    }

    public static ModConfig getInstance() {
        return INSTANCE;
    }

    // -------------------------------------------------------------------------
    // Load / Save
    // -------------------------------------------------------------------------

    public void load() {
        Path path = CONFIG_PATH;
        if (!Files.exists(path)) {
            Path legacy = FabricLoader.getInstance().getConfigDir().resolve("mandatory.json");
            if (Files.exists(legacy)) path = legacy;
            else return;
        }
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonObject()) return;
            JsonObject obj = root.getAsJsonObject();

            int version = obj.has("_version") ? obj.get("_version").getAsInt() : 1;

            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                String key = entry.getKey();
                if (key.startsWith("_")) continue;

                if (key.equals("hud_positions")) {
                    // Parse HUD positions block
                    if (entry.getValue().isJsonObject()) {
                        for (Map.Entry<String, JsonElement> hudEntry :
                                entry.getValue().getAsJsonObject().entrySet()) {
                            if (hudEntry.getValue().isJsonObject()) {
                                hudData.put(hudEntry.getKey(), hudEntry.getValue().getAsJsonObject());
                            }
                        }
                    }
                    continue;
                }

                if (version < 2) {
                    // v1: flat boolean → migrate to nested { "enabled": bool }
                    JsonObject migrated = new JsonObject();
                    if (entry.getValue().isJsonPrimitive()) {
                        migrated.addProperty("enabled", entry.getValue().getAsBoolean());
                    }
                    data.put(key, migrated);
                } else {
                    // v2/v3: nested objects
                    if (entry.getValue().isJsonObject()) {
                        data.put(key, entry.getValue().getAsJsonObject());
                    }
                }
            }
        } catch (IOException | JsonSyntaxException e) {
            System.err.println("[Noctra] Failed to load config: " + e.getMessage());
        }
    }

    public void save() {
        JsonObject root = new JsonObject();
        root.addProperty("_version", VERSION);
        for (Map.Entry<String, JsonObject> entry : data.entrySet()) {
            root.add(entry.getKey(), entry.getValue());
        }
        // Save HUD positions
        JsonObject hudBlock = new JsonObject();
        for (Map.Entry<String, JsonObject> entry : hudData.entrySet()) {
            hudBlock.add(entry.getKey(), entry.getValue());
        }
        root.add("hud_positions", hudBlock);
        try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
            GSON.toJson(root, writer);
        } catch (IOException e) {
            System.err.println("[Noctra] Failed to save config: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Enabled-state access
    // -------------------------------------------------------------------------

    public boolean isEnabled(String moduleId, boolean defaultValue) {
        JsonObject obj = data.get(moduleId);
        if (obj == null || !obj.has("enabled")) return defaultValue;
        return obj.get("enabled").getAsBoolean();
    }

    public void setEnabled(String moduleId, boolean enabled) {
        data.computeIfAbsent(moduleId, k -> new JsonObject()).addProperty("enabled", enabled);
        save();
    }

    // -------------------------------------------------------------------------
    // Per-module settings
    // -------------------------------------------------------------------------

    public void loadModuleSettings(BaseModule module) {
        JsonObject obj = data.get(module.getId());
        if (obj == null) return;
        for (ModuleSetting<?> setting : module.getSettings()) {
            if (!obj.has(setting.getId())) continue;
            try {
                loadSetting(setting, obj.get(setting.getId()));
            } catch (Exception e) {
                System.err.println("[Noctra] Bad value for "
                        + module.getId() + "." + setting.getId() + ": " + e.getMessage());
            }
        }
    }

    public void saveModuleSettings(BaseModule module) {
        JsonObject obj = data.computeIfAbsent(module.getId(), k -> new JsonObject());
        for (ModuleSetting<?> setting : module.getSettings()) {
            obj.add(setting.getId(), setting.toJson());
        }
        save();
    }

    @SuppressWarnings("unchecked")
    private static <T> void loadSetting(ModuleSetting<T> setting, JsonElement el) {
        setting.set(setting.fromJson(el));
    }

    // -------------------------------------------------------------------------
    // HUD positions
    // -------------------------------------------------------------------------

    public record HudElementState(int x, int y, int w, int h, boolean visible) {}

    /** Initialize a HUD state entry only if it doesn't already exist in config. */
    public void initHudState(String hudId, int defaultX, int defaultY, int defaultW, int defaultH) {
        if (hudData.containsKey(hudId)) return;
        JsonObject obj = new JsonObject();
        obj.addProperty("x", defaultX);
        obj.addProperty("y", defaultY);
        obj.addProperty("w", defaultW);
        obj.addProperty("h", defaultH);
        obj.addProperty("visible", true);
        hudData.put(hudId, obj);
    }

    public HudElementState getHudState(String hudId) {
        JsonObject obj = hudData.get(hudId);
        if (obj == null) return null;
        return new HudElementState(
                obj.has("x") ? obj.get("x").getAsInt() : 4,
                obj.has("y") ? obj.get("y").getAsInt() : 4,
                obj.has("w") ? obj.get("w").getAsInt() : 100,
                obj.has("h") ? obj.get("h").getAsInt() : 20,
                !obj.has("visible") || obj.get("visible").getAsBoolean()
        );
    }

    public void setHudState(String hudId, HudElementState state) {
        JsonObject obj = hudData.computeIfAbsent(hudId, k -> new JsonObject());
        obj.addProperty("x", state.x());
        obj.addProperty("y", state.y());
        obj.addProperty("w", state.w());
        obj.addProperty("h", state.h());
        obj.addProperty("visible", state.visible());
        save();
    }
}
