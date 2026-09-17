package de.snenjih.noctra.cosmetics.api;

import java.util.List;

public record CosmeticEntry(
    String id,
    CosmeticType type,
    String name,
    String description,
    int version,
    String hash,           // format: "sha256:hexstring"
    String textureUrl,
    String previewUrl,     // nullable
    List<String> tags
) {}
