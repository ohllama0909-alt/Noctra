package de.snenjih.noctra.cosmetics.network;

import de.snenjih.noctra.cosmetics.api.CosmeticType;
import de.snenjih.noctra.cosmetics.storage.CosmeticRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public record CosmeticPayload(UUID playerUuid, Map<String, String> equipped) implements CustomPayload {

    public static final CustomPayload.Id<CosmeticPayload> ID =
            new CustomPayload.Id<>(Identifier.of("noctra", "cosmetics"));

    public static final PacketCodec<RegistryByteBuf, CosmeticPayload> CODEC =
            PacketCodec.of(CosmeticPayload::write, CosmeticPayload::read);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }

    private void write(RegistryByteBuf buf) {
        buf.writeUuid(playerUuid);
        buf.writeVarInt(equipped.size());
        for (Map.Entry<String, String> entry : equipped.entrySet()) {
            buf.writeString(entry.getKey());
            buf.writeBoolean(entry.getValue() != null);
            if (entry.getValue() != null) buf.writeString(entry.getValue());
        }
    }

    public static CosmeticPayload read(RegistryByteBuf buf) {
        UUID uuid = buf.readUuid();
        int size = buf.readVarInt();
        Map<String, String> equipped = new HashMap<>();
        for (int i = 0; i < size; i++) {
            String key = buf.readString(64);
            boolean hasValue = buf.readBoolean();
            String value = hasValue ? buf.readString(128) : null;
            equipped.put(key, value);
        }
        return new CosmeticPayload(uuid, equipped);
    }

    /** Build a payload from the registry's self-equipped map. */
    public static CosmeticPayload fromSelf(UUID uuid) {
        Map<String, String> map = new HashMap<>();
        for (CosmeticType type : CosmeticType.values()) {
            String id = CosmeticRegistry.getSelfEquipped(type);
            map.put(type.id(), id);
        }
        return new CosmeticPayload(uuid, map);
    }
}
