package de.snenjih.noctra.cosmetics.sync;

import com.google.gson.*;
import de.snenjih.noctra.cosmetics.api.CosmeticEntry;
import de.snenjih.noctra.cosmetics.api.CosmeticType;

import java.util.ArrayList;
import java.util.List;

public record CosmeticManifest(int schema, List<CosmeticEntry> cosmetics) {

    public static CosmeticManifest parse(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        int schema = root.has("_schema") ? root.get("_schema").getAsInt() : 1;
        List<CosmeticEntry> entries = new ArrayList<>();

        if (!root.has("cosmetics")) return new CosmeticManifest(schema, entries);

        for (JsonElement el : root.getAsJsonArray("cosmetics")) {
            if (!el.isJsonObject()) continue;
            JsonObject obj = el.getAsJsonObject();
            try {
                String id = obj.get("id").getAsString();
                CosmeticType type = CosmeticType.fromId(obj.get("type").getAsString());
                if (type == null) continue;
                String name = obj.get("name").getAsString();
                String desc = obj.has("description") ? obj.get("description").getAsString() : "";
                int version = obj.has("version") ? obj.get("version").getAsInt() : 1;
                String hash = obj.get("hash").getAsString();
                String textureUrl = obj.get("texture_url").getAsString();
                String previewUrl = obj.has("preview_url") && !obj.get("preview_url").isJsonNull()
                        ? obj.get("preview_url").getAsString() : null;
                List<String> tags = new ArrayList<>();
                if (obj.has("tags")) {
                    for (JsonElement tag : obj.getAsJsonArray("tags")) {
                        tags.add(tag.getAsString());
                    }
                }
                entries.add(new CosmeticEntry(id, type, name, desc, version, hash, textureUrl, previewUrl, tags));
            } catch (Exception e) {
                // Skip malformed entries
            }
        }
        return new CosmeticManifest(schema, entries);
    }
}
