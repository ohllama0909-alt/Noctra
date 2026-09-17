package de.snenjih.noctra.auth;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class SkinCache {

    private static SkinCache INSTANCE;

    private final Path      cacheDir;
    private final HttpClient http;
    private final Set<UUID> loaded  = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<UUID> loading = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public SkinCache() {
        this.cacheDir = FabricLoader.getInstance().getGameDir().resolve("noctra/skins");
        this.http     = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(15))
                .build();
        try { Files.createDirectories(cacheDir); } catch (Exception ignored) {}
        INSTANCE = this;
    }

    public static SkinCache getInstance() { return INSTANCE; }

    public Identifier getTextureId(UUID uuid) {
        return Identifier.of("noctra", "skin/" + uuid.toString().replace("-", ""));
    }

    public boolean isLoaded(UUID uuid) { return loaded.contains(uuid); }

    public void ensureLoaded(UUID uuid, String skinUrl) {
        if (loaded.contains(uuid) || loading.contains(uuid)) return;
        if (skinUrl == null || skinUrl.isBlank()) return;
        loading.add(uuid);

        Path file = cacheDir.resolve(uuid + ".png");
        CompletableFuture.runAsync(() -> {
            try {
                byte[] bytes;
                if (Files.exists(file)) {
                    bytes = Files.readAllBytes(file);
                } else {
                    HttpRequest req = HttpRequest.newBuilder()
                            .uri(URI.create(skinUrl))
                            .GET()
                            .timeout(Duration.ofSeconds(15))
                            .build();
                    HttpResponse<InputStream> res = http.send(req, HttpResponse.BodyHandlers.ofInputStream());
                    if (res.statusCode() != 200) { loading.remove(uuid); return; }
                    bytes = res.body().readAllBytes();
                    Files.write(file, bytes);
                }

                final byte[] finalBytes = bytes;
                MinecraftClient.getInstance().execute(() -> {
                    try {
                        NativeImage img = NativeImage.read(finalBytes);
                        NativeImageBackedTexture tex = new NativeImageBackedTexture(
                                () -> "noctra:skin:" + uuid, img);
                        MinecraftClient.getInstance().getTextureManager().registerTexture(getTextureId(uuid), tex);
                        loaded.add(uuid);
                    } catch (Exception e) {
                        System.err.println("[Noctra] Failed to register skin texture for " + uuid + ": " + e.getMessage());
                    } finally {
                        loading.remove(uuid);
                    }
                });
            } catch (Exception e) {
                System.err.println("[Noctra] Failed to fetch skin for " + uuid + ": " + e.getMessage());
                loading.remove(uuid);
            }
        });
    }

    public void unload(UUID uuid) {
        if (!loaded.contains(uuid)) return;
        MinecraftClient.getInstance().execute(() -> {
            MinecraftClient.getInstance().getTextureManager().destroyTexture(getTextureId(uuid));
            loaded.remove(uuid);
        });
    }
}
