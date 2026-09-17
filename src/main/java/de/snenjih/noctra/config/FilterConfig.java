package de.snenjih.noctra.config;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public final class FilterConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = getPath();

    private static Path getPath() {
        Path p = FabricLoader.getInstance().getConfigDir().resolve("noctra_filters.json");
        if (!Files.exists(p)) {
            Path legacy = FabricLoader.getInstance().getConfigDir().resolve("mandatory_filters.json");
            if (Files.exists(legacy)) return legacy;
        }
        return p;
    }
    private static final int MAX_PATTERNS = 8;

    private final List<String> patterns = new ArrayList<>();

    public void load() {
        if (!Files.exists(PATH)) return;
        try (Reader r = Files.newBufferedReader(PATH)) {
            JsonObject obj = JsonParser.parseReader(r).getAsJsonObject();
            JsonArray arr = obj.getAsJsonArray("patterns");
            patterns.clear();
            for (JsonElement el : arr) patterns.add(el.getAsString());
        } catch (Exception e) {
            System.err.println("[Noctra] Failed to load filter config: " + e.getMessage());
        }
    }

    public void save() {
        JsonArray arr = new JsonArray();
        patterns.forEach(arr::add);
        JsonObject obj = new JsonObject();
        obj.add("patterns", arr);
        try (Writer w = Files.newBufferedWriter(PATH)) {
            GSON.toJson(obj, w);
        } catch (IOException e) {
            System.err.println("[Noctra] Failed to save filter config: " + e.getMessage());
        }
    }

    /** Returns false if the list is full (8 entries) or pattern is blank. */
    public boolean add(String pattern) {
        if (pattern == null || pattern.isBlank()) return false;
        if (patterns.size() >= MAX_PATTERNS) return false;
        if (!patterns.contains(pattern)) {
            patterns.add(pattern);
            save();
        }
        return true;
    }

    public boolean remove(int index) {
        if (index < 0 || index >= patterns.size()) return false;
        patterns.remove(index);
        save();
        return true;
    }

    public void clear() {
        patterns.clear();
        save();
    }

    public List<String> getPatterns() {
        return Collections.unmodifiableList(patterns);
    }

    public int maxPatterns() { return MAX_PATTERNS; }
}
