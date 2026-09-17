package de.snenjih.noctra.cosmetics.storage;

import com.google.gson.*;
import de.snenjih.noctra.NoctraMod;
import de.snenjih.noctra.cosmetics.api.CosmeticType;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.*;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public final class CosmeticStorage {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path BASE_DIR;
    private static Path TEXTURES_DIR;
    private static Path MANIFEST_CACHE_PATH;
    private static Path EQUIPPED_PATH;

    private CosmeticStorage() {}

    public static void init() {
        BASE_DIR = FabricLoader.getInstance().getGameDir().resolve("noctra/cosmetics");
        TEXTURES_DIR = BASE_DIR.resolve("textures");
        MANIFEST_CACHE_PATH = BASE_DIR.resolve("manifest_cache.json");
        EQUIPPED_PATH = BASE_DIR.resolve("equipped.json");
        try {
            Files.createDirectories(TEXTURES_DIR);
        } catch (IOException e) {
            NoctraMod.LOGGER.error("[Noctra Cosmetics] Failed to create storage directories", e);
        }
    }

    // ---- Manifest Cache (id → hash) ----------------------------------------

    public static Map<String, String> loadManifestCache() {
        if (!Files.exists(MANIFEST_CACHE_PATH)) return new HashMap<>();
        try (Reader r = Files.newBufferedReader(MANIFEST_CACHE_PATH)) {
            JsonElement el = JsonParser.parseReader(r);
            if (!el.isJsonObject()) return new HashMap<>();
            Map<String, String> result = new HashMap<>();
            for (Map.Entry<String, JsonElement> entry : el.getAsJsonObject().entrySet()) {
                result.put(entry.getKey(), entry.getValue().getAsString());
            }
            return result;
        } catch (Exception e) {
            NoctraMod.LOGGER.warn("[Noctra Cosmetics] Corrupt manifest cache, resetting: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    public static void saveManifestCache(Map<String, String> hashes) {
        JsonObject obj = new JsonObject();
        hashes.forEach(obj::addProperty);
        try (Writer w = Files.newBufferedWriter(MANIFEST_CACHE_PATH)) {
            GSON.toJson(obj, w);
        } catch (IOException e) {
            NoctraMod.LOGGER.error("[Noctra Cosmetics] Failed to save manifest cache", e);
        }
    }

    // ---- Equipped State ----------------------------------------------------

    public static Map<CosmeticType, String> loadEquipped() {
        Map<CosmeticType, String> result = new EnumMap<>(CosmeticType.class);
        for (CosmeticType t : CosmeticType.values()) result.put(t, null);
        if (!Files.exists(EQUIPPED_PATH)) return result;
        try (Reader r = Files.newBufferedReader(EQUIPPED_PATH)) {
            JsonElement el = JsonParser.parseReader(r);
            if (!el.isJsonObject()) return result;
            for (Map.Entry<String, JsonElement> entry : el.getAsJsonObject().entrySet()) {
                CosmeticType type = CosmeticType.fromId(entry.getKey());
                if (type == null) continue;
                String val = entry.getValue().isJsonNull() ? null : entry.getValue().getAsString();
                result.put(type, val);
            }
        } catch (Exception e) {
            NoctraMod.LOGGER.warn("[Noctra Cosmetics] Corrupt equipped.json, resetting: {}", e.getMessage());
        }
        return result;
    }

    public static void saveEquipped(Map<CosmeticType, String> equipped) {
        JsonObject obj = new JsonObject();
        for (Map.Entry<CosmeticType, String> entry : equipped.entrySet()) {
            if (entry.getValue() == null) obj.add(entry.getKey().id(), JsonNull.INSTANCE);
            else obj.addProperty(entry.getKey().id(), entry.getValue());
        }
        try (Writer w = Files.newBufferedWriter(EQUIPPED_PATH)) {
            GSON.toJson(obj, w);
        } catch (IOException e) {
            NoctraMod.LOGGER.error("[Noctra Cosmetics] Failed to save equipped.json", e);
        }
    }

    // ---- Texture Files -----------------------------------------------------

    public static boolean textureExists(String id) {
        return Files.exists(getTexturePath(id));
    }

    public static Path getTexturePath(String id) {
        return TEXTURES_DIR.resolve(id + ".png");
    }

    public static Path getDescriptorPath(String id) {
        return BASE_DIR.resolve("descriptors/" + id + ".json");
    }

    public static void writeTexture(String id, byte[] data) {
        try {
            Files.write(getTexturePath(id), data);
        } catch (IOException e) {
            NoctraMod.LOGGER.error("[Noctra Cosmetics] Failed to write texture {}: {}", id, e.getMessage());
        }
    }

    public static void writeDescriptor(String id, byte[] data) {
        try {
            Path descriptorDir = BASE_DIR.resolve("descriptors");
            Files.createDirectories(descriptorDir);
            Files.write(descriptorDir.resolve(id + ".json"), data);
        } catch (IOException e) {
            NoctraMod.LOGGER.error("[Noctra Cosmetics] Failed to write descriptor {}: {}", id, e.getMessage());
        }
    }

    public static void deleteTexture(String id) {
        try {
            Files.deleteIfExists(getTexturePath(id));
            Files.deleteIfExists(getDescriptorPath(id));
        } catch (IOException e) {
            NoctraMod.LOGGER.warn("[Noctra Cosmetics] Could not delete cached file for {}: {}", id, e.getMessage());
        }
    }
}
