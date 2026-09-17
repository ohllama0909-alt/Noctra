package de.snenjih.noctra.cosmetics.render;

import de.snenjih.noctra.cosmetics.api.CosmeticType;
import de.snenjih.noctra.cosmetics.api.PlayerCosmeticData;
import de.snenjih.noctra.cosmetics.storage.CosmeticRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.UUID;

/**
 * Master cosmetic feature renderer. Dispatches to sub-renderers based on
 * what cosmetics the player has equipped.
 */
public class CosmeticFeatureRenderer extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {

    public CosmeticFeatureRenderer(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> context) {
        super(context);
    }

    @Override
    public void render(MatrixStack matrices, OrderedRenderCommandQueue renderQueue, int light,
                       PlayerEntityRenderState state, float limbAngle, float limbDistance) {
        UUID uuid = resolveUuid(state);
        if (uuid == null) return;

        PlayerCosmeticData data = CosmeticRegistry.getEquipped(uuid);
        if (data == null) return;

        // Cape
        if (data.getEquipped(CosmeticType.CAPE) != null) {
            CapeRenderer.render(matrices, renderQueue, light, state, data);
        }

        // Hat
        if (data.getEquipped(CosmeticType.HAT) != null) {
            HatRenderer.render(matrices, renderQueue, light, state, data);
        }

        // Wings (skip when gliding)
        if (data.getEquipped(CosmeticType.WINGS) != null && !state.isGliding) {
            WingsRenderer.render(matrices, renderQueue, light, state, data);
        }
    }

    /**
     * Resolves the UUID of the player being rendered by looking up the entity
     * via its network ID, which is available on the render state.
     */
    private static UUID resolveUuid(PlayerEntityRenderState state) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return null;

        Entity entity = mc.world.getEntityById(state.id);
        if (entity instanceof PlayerEntity player) {
            return player.getUuid();
        }
        return null;
    }
}
