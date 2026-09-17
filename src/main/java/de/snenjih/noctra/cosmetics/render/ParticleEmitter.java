package de.snenjih.noctra.cosmetics.render;

import de.snenjih.noctra.cosmetics.api.CosmeticType;
import de.snenjih.noctra.cosmetics.api.PlayerCosmeticData;
import de.snenjih.noctra.cosmetics.storage.CosmeticRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;

/**
 * Emits cosmetic particles around players who have a PARTICLES cosmetic equipped.
 * Called from WorldRenderEvents.AFTER_ENTITIES (not a FeatureRenderer).
 * Throttled to fire every 4 frames to limit particle density.
 */
public final class ParticleEmitter {

    private static int tickCounter = 0;

    private ParticleEmitter() {}

    public static void onRenderWorld(WorldRenderContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientWorld world = mc.world;
        if (world == null) return;

        tickCounter++;
        if (tickCounter % 4 != 0) return; // throttle: emit every 4 frames

        for (PlayerEntity player : world.getPlayers()) {
            PlayerCosmeticData data = CosmeticRegistry.getEquipped(player.getUuid());
            if (data == null) continue;

            String particleId = data.getEquipped(CosmeticType.PARTICLES);
            if (particleId == null) continue;

            // Spawn END_ROD sparkle particles around the player
            // A future iteration can read the particle descriptor from the cache
            // and pick the particle type from the JSON.
            double x = player.getX() + (Math.random() - 0.5) * 0.8;
            double y = player.getY() + 0.5 + Math.random() * 1.5;
            double z = player.getZ() + (Math.random() - 0.5) * 0.8;
            world.addParticleClient(ParticleTypes.END_ROD, x, y, z, 0.0, 0.02, 0.0);
        }
    }
}
