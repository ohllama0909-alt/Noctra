package de.snenjih.noctra.cosmetics.sync;

import de.snenjih.noctra.NoctraMod;
import de.snenjih.noctra.cosmetics.api.CosmeticEntry;
import de.snenjih.noctra.cosmetics.api.CosmeticType;
import de.snenjih.noctra.cosmetics.storage.CosmeticRegistry;
import de.snenjih.noctra.cosmetics.storage.CosmeticStorage;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public final class CosmeticSyncService {

    public static final String MANIFEST_URL =
            "https://snenjih.github.io/Mandatory-cosmetics/cosmetics/manifest.json";

    private static final CosmeticSyncService INSTANCE = new CosmeticSyncService();
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "NoctraMod-CosmeticSync");
        t.setDaemon(true);
        return t;
    });

    private final AtomicBoolean syncing = new AtomicBoolean(false);
    private volatile boolean syncComplete = false;

    private CosmeticSyncService() {}

    public static CosmeticSyncService getInstance() {
        return INSTANCE;
    }

    public boolean isSyncing() {
        return syncing.get();
    }

    public boolean isSyncComplete() {
        return syncComplete;
    }

    public void syncAsync() {
        if (syncing.compareAndSet(false, true)) {
            EXECUTOR.submit(this::sync);
        }
    }

    private void sync() {
        try {
            NoctraMod.LOGGER.info("[Noctra Cosmetics] Starting cosmetic sync from {}", MANIFEST_URL);

            // 1. Load local cache
            Map<String, String> localHashes = CosmeticStorage.loadManifestCache();

            // 2. Fetch manifest
            String manifestJson;
            try {
                manifestJson = fetchString(MANIFEST_URL, 10);
            } catch (Exception e) {
                NoctraMod.LOGGER.warn("[Noctra Cosmetics] Could not reach manifest server, using cache: {}", e.getMessage());
                loadCachedIntoRegistry(localHashes);
                return;
            }

            // 3. Parse manifest
            CosmeticManifest manifest;
            try {
                manifest = CosmeticManifest.parse(manifestJson);
            } catch (Exception e) {
                NoctraMod.LOGGER.error("[Noctra Cosmetics] Failed to parse manifest", e);
                loadCachedIntoRegistry(localHashes);
                return;
            }

            if (manifest.cosmetics().isEmpty()) {
                NoctraMod.LOGGER.warn("[Noctra Cosmetics] Server returned empty manifest, keeping cache");
                loadCachedIntoRegistry(localHashes);
                return;
            }

            // 4. Compute delta
            Map<String, String> serverHashes = new HashMap<>();
            List<CosmeticEntry> toDownload = new ArrayList<>();
            List<CosmeticEntry> unchanged = new ArrayList<>();

            for (CosmeticEntry entry : manifest.cosmetics()) {
                serverHashes.put(entry.id(), entry.hash());
                String localHash = localHashes.get(entry.id());
                boolean filePresent = entry.type() == CosmeticType.PARTICLES
                        ? Files.exists(CosmeticStorage.getDescriptorPath(entry.id()))
                        : CosmeticStorage.textureExists(entry.id());

                if (!filePresent || !entry.hash().equals(localHash)) {
                    toDownload.add(entry);
                } else {
                    unchanged.add(entry);
                }
            }

            // 5. Find removed
            List<String> toRemove = new ArrayList<>();
            for (String id : localHashes.keySet()) {
                if (!serverHashes.containsKey(id)) toRemove.add(id);
            }

            NoctraMod.LOGGER.info("[Noctra Cosmetics] Delta: {} download, {} unchanged, {} remove",
                    toDownload.size(), unchanged.size(), toRemove.size());

            // 6. Download new/changed
            Map<String, String> newCache = new HashMap<>(localHashes);
            List<CosmeticEntry> successfullyDownloaded = new ArrayList<>();

            for (CosmeticEntry entry : toDownload) {
                boolean ok = downloadAsset(entry);
                if (ok) {
                    newCache.put(entry.id(), entry.hash());
                    successfullyDownloaded.add(entry);
                }
            }

            // 7. Remove deleted
            for (String id : toRemove) {
                CosmeticStorage.deleteTexture(id);
                newCache.remove(id);
                NoctraMod.LOGGER.info("[Noctra Cosmetics] Removed cosmetic: {}", id);
            }

            // 8. Save updated cache
            CosmeticStorage.saveManifestCache(newCache);

            // 9. Update registry with all available cosmetics
            List<CosmeticEntry> allAvailable = new ArrayList<>();
            allAvailable.addAll(unchanged);
            allAvailable.addAll(successfullyDownloaded);
            // Add entries that were in cache but download failed (still in local cache)
            for (CosmeticEntry entry : toDownload) {
                if (!successfullyDownloaded.contains(entry) && newCache.containsKey(entry.id())) {
                    allAvailable.add(entry);
                }
            }
            CosmeticRegistry.setAvailable(allAvailable);

            // 10. Register textures on render thread
            MinecraftClient.getInstance().execute(() -> {
                for (CosmeticEntry entry : allAvailable) {
                    if (entry.type() == CosmeticType.PARTICLES) continue;
                    if (CosmeticRegistry.isTextureLoaded(entry.id())) continue;
                    if (!CosmeticStorage.textureExists(entry.id())) continue;
                    try (InputStream is = Files.newInputStream(CosmeticStorage.getTexturePath(entry.id()))) {
                        NativeImage img = NativeImage.read(is);
                        CosmeticRegistry.registerTexture(entry.id(), img);
                    } catch (IOException e) {
                        NoctraMod.LOGGER.warn("[Noctra Cosmetics] Could not load texture {}", entry.id(), e);
                    }
                }
                syncComplete = true;
            });

            NoctraMod.LOGGER.info("[Noctra Cosmetics] Sync complete. {} cosmetics available.", allAvailable.size());

        } catch (Exception e) {
            NoctraMod.LOGGER.error("[Noctra Cosmetics] Sync failed unexpectedly", e);
        } finally {
            syncing.set(false);
        }
    }

    private void loadCachedIntoRegistry(Map<String, String> localHashes) {
        // We don't have the server manifest, but we can load what we have cached
        MinecraftClient.getInstance().execute(() -> {
            CosmeticRegistry.loadCachedTextures();
            syncComplete = true;
        });
    }

    private boolean downloadAsset(CosmeticEntry entry) {
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                byte[] data = fetchBytes(entry.textureUrl(), 15);

                // Verify hash
                String expectedHash = entry.hash();
                if (expectedHash.startsWith("sha256:")) {
                    String expected = expectedHash.substring(7);
                    String actual = sha256Hex(data);
                    if (!actual.equals(expected)) {
                        NoctraMod.LOGGER.warn("[Noctra Cosmetics] Hash mismatch for {} (attempt {}): expected {} got {}",
                                entry.id(), attempt + 1, expected, actual);
                        continue;
                    }
                }

                // Write to disk
                if (entry.type() == CosmeticType.PARTICLES) {
                    CosmeticStorage.writeDescriptor(entry.id(), data);
                } else {
                    CosmeticStorage.writeTexture(entry.id(), data);
                }
                NoctraMod.LOGGER.info("[Noctra Cosmetics] Downloaded: {}", entry.id());
                return true;

            } catch (Exception e) {
                NoctraMod.LOGGER.warn("[Noctra Cosmetics] Download attempt {} failed for {}: {}",
                        attempt + 1, entry.id(), e.getMessage());
            }
        }
        NoctraMod.LOGGER.error("[Noctra Cosmetics] Failed to download {} after 3 attempts", entry.id());
        return false;
    }

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private static String fetchString(String url, int timeoutSeconds) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .header("User-Agent", "NoctraMod/1.0")
                .GET()
                .build();
        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("HTTP " + response.statusCode() + " for " + url);
        }
        return response.body();
    }

    private static byte[] fetchBytes(String url, int timeoutSeconds) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .header("User-Agent", "NoctraMod/1.0")
                .GET()
                .build();
        HttpResponse<byte[]> response = HTTP.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() != 200) {
            throw new IOException("HTTP " + response.statusCode() + " for " + url);
        }
        return response.body();
    }

    private static String sha256Hex(byte[] data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data);
        return HexFormat.of().formatHex(hash);
    }
}
