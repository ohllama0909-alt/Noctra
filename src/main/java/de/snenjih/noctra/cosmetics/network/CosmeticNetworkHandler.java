package de.snenjih.noctra.cosmetics.network;

import de.snenjih.noctra.NoctraMod;
import de.snenjih.noctra.cosmetics.api.CosmeticType;
import de.snenjih.noctra.cosmetics.api.PlayerCosmeticData;
import de.snenjih.noctra.cosmetics.storage.CosmeticRegistry;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

public final class CosmeticNetworkHandler {

    private CosmeticNetworkHandler() {}

    /** Send own cosmetics state to the server (which may relay to other clients). */
    public static void sendSelf() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null) return;

        if (!ClientPlayNetworking.canSend(CosmeticPayload.ID)) {
            NoctraMod.LOGGER.info("[Noctra Cosmetics] Server does not support cosmetics packets — multiplayer cosmetics disabled");
            return;
        }

        CosmeticPayload payload = CosmeticPayload.fromSelf(player.getUuid());
        ClientPlayNetworking.send(payload);
        NoctraMod.LOGGER.debug("[Noctra Cosmetics] Sent own cosmetics state to server");
    }

    /** Handle an incoming cosmetics payload from another player (relayed by server). */
    public static void handleIncoming(CosmeticPayload payload, ClientPlayNetworking.Context context) {
        UUID uuid = payload.playerUuid();
        MinecraftClient mc = MinecraftClient.getInstance();

        // Ignore packets about ourselves
        if (mc.player != null && mc.player.getUuid().equals(uuid)) return;

        Map<CosmeticType, String> equipped = new EnumMap<>(CosmeticType.class);
        for (Map.Entry<String, String> entry : payload.equipped().entrySet()) {
            CosmeticType type = CosmeticType.fromId(entry.getKey());
            if (type != null) {
                equipped.put(type, entry.getValue());
            }
        }

        PlayerCosmeticData data = new PlayerCosmeticData(uuid, equipped);
        CosmeticRegistry.setOtherPlayer(uuid, data);
        NoctraMod.LOGGER.debug("[Noctra Cosmetics] Received cosmetics for player {}", uuid);
    }

    /** Register all network channels. Call once from NoctraMod.onInitializeClient(). */
    public static void register() {
        // Register payload type for both C2S and S2C channels
        PayloadTypeRegistry.playC2S().register(CosmeticPayload.ID, CosmeticPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(CosmeticPayload.ID, CosmeticPayload.CODEC);

        // Register S2C receiver (incoming from server relay)
        ClientPlayNetworking.registerGlobalReceiver(CosmeticPayload.ID, CosmeticNetworkHandler::handleIncoming);
    }
}
