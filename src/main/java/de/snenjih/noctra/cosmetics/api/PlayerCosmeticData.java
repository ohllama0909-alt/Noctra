package de.snenjih.noctra.cosmetics.api;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public record PlayerCosmeticData(
    UUID uuid,
    Map<CosmeticType, String> equipped  // type → cosmeticId, null value = nothing equipped
) {
    public static PlayerCosmeticData empty(UUID uuid) {
        Map<CosmeticType, String> map = new HashMap<>();
        for (CosmeticType t : CosmeticType.values()) map.put(t, null);
        return new PlayerCosmeticData(uuid, map);
    }

    public String getEquipped(CosmeticType type) {
        return equipped.get(type);
    }
}
