package de.snenjih.noctra.cosmetics.storage;

import de.snenjih.noctra.NoctraMod;
import de.snenjih.noctra.cosmetics.api.CosmeticEntry;
import de.snenjih.noctra.cosmetics.api.CosmeticType;
import de.snenjih.noctra.cosmetics.api.PlayerCosmeticData;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class CosmeticRegistry {

    private static final List<CosmeticEntry> AVAILABLE = new ArrayList<>();
    private static final Map<CosmeticType, String> SELF_EQUIPPED = new EnumMap<>(CosmeticType.class);
    private static final Map<UUID, PlayerCosmeticData> OTHER_PLAYERS = new ConcurrentHashMap<>();
    private static final Map<String, Identifier> LOADED_TEXTURES = new ConcurrentHashMap<>();

    private CosmeticRegistry() {}

    public static void init() {
        Map<CosmeticType, String> saved = CosmeticStorage.loadEquipped();
        SELF_EQUIPPED.putAll(saved);
    }

    // ---- Available Cosmetics -----------------------------------------------

    public static synchronized void setAvailable(List<CosmeticEntry> entries) {
        AVAILABLE.clear();
        AVAILABLE.addAll(entries);
    }

    public static synchronized List<CosmeticEntry> getAvailable() {
        return Collections.unmodifiableList(new ArrayList<>(AVAILABLE));
    }

    public static synchronized List<CosmeticEntry> getAvailableByType(CosmeticType type) {
        List<CosmeticEntry> result = new ArrayList<>();
        for (CosmeticEntry e : AVAILABLE) {
            if (e.type() == type) result.add(e);
        }
        return result;
    }

    public static synchronized CosmeticEntry getById(String id) {
        for (CosmeticEntry e : AVAILABLE) {
            if (e.id().equals(id)) return e;
        }
        return null;
    }

    // ---- Self-Equipped State -----------------------------------------------

    public static void equip(CosmeticType type, String id) {
        SELF_EQUIPPED.put(type, id);
        CosmeticStorage.saveEquipped(SELF_EQUIPPED);
    }

    public static void unequip(CosmeticType type) {
        SELF_EQUIPPED.put(type, null);
        CosmeticStorage.saveEquipped(SELF_EQUIPPED);
    }

    public static String getSelfEquipped(CosmeticType type) {
        return SELF_EQUIPPED.get(type);
    }

    public static Map<CosmeticType, String> getAllSelfEquipped() {
        return Collections.unmodifiableMap(SELF_EQUIPPED);
    }

    // ---- Other Player State (via Fabric Packets) ---------------------------

    public static void setOtherPlayer(UUID uuid, PlayerCosmeticData data) {
        OTHER_PLAYERS.put(uuid, data);
    }

    public static PlayerCosmeticData getEquipped(UUID uuid) {
        // Check self first
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null && mc.player.getUuid().equals(uuid)) {
            return new PlayerCosmeticData(uuid, SELF_EQUIPPED);
        }
        return OTHER_PLAYERS.get(uuid);
    }

    public static void removePlayer(UUID uuid) {
        OTHER_PLAYERS.remove(uuid);
    }

    public static void clearOtherPlayers() {
        OTHER_PLAYERS.clear();
    }

    // ---- Texture Management (call on render thread only) -------------------

    public static void registerTexture(String id, NativeImage image) {
        Identifier textureId = Identifier.of("noctra", "cosmetics/" + id);
        MinecraftClient.getInstance().getTextureManager()
            .registerTexture(textureId, new NativeImageBackedTexture(() -> id, image));
        LOADED_TEXTURES.put(id, textureId);
    }

    public static Identifier getTextureIdentifier(String id) {
        return LOADED_TEXTURES.get(id);
    }

    public static boolean isTextureLoaded(String id) {
        return LOADED_TEXTURES.containsKey(id);
    }

    /** Load textures from disk for all cached cosmetics. Call on render thread after init. */
    public static void loadCachedTextures() {
        for (CosmeticEntry entry : getAvailable()) {
            if (entry.type() == CosmeticType.PARTICLES) continue; // particles use JSON, not PNG
            if (!CosmeticStorage.textureExists(entry.id())) continue;
            if (isTextureLoaded(entry.id())) continue;
            try (InputStream is = Files.newInputStream(CosmeticStorage.getTexturePath(entry.id()))) {
                NativeImage img = NativeImage.read(is);
                registerTexture(entry.id(), img);
            } catch (IOException e) {
                NoctraMod.LOGGER.warn("[Noctra Cosmetics] Could not load texture {}: {}", entry.id(), e.getMessage());
            }
        }
    }
}
