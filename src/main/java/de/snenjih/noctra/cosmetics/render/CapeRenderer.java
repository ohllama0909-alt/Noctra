package de.snenjih.noctra.cosmetics.render;

import de.snenjih.noctra.cosmetics.api.CosmeticType;
import de.snenjih.noctra.cosmetics.api.PlayerCosmeticData;
import de.snenjih.noctra.cosmetics.storage.CosmeticRegistry;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

/**
 * Renders a custom cape texture behind the player.
 * Implements a simplified flat-quad cape (10x16 pixel plane) at the player's back.
 */
public final class CapeRenderer {

    private CapeRenderer() {}

    public static void render(MatrixStack matrices, OrderedRenderCommandQueue renderQueue,
                              int light, PlayerEntityRenderState state, PlayerCosmeticData data) {
        String capeId = data.getEquipped(CosmeticType.CAPE);
        if (capeId == null) return;

        Identifier texture = CosmeticRegistry.getTextureIdentifier(capeId);
        if (texture == null) return;

        RenderLayer layer = RenderLayers.entityCutoutNoCull(texture);

        matrices.push();
        // Move to back of the player's body, centered at shoulder height
        matrices.translate(0.0, 0.0, 0.125);

        renderQueue.submitCustom(matrices, layer, (entry, vc) -> {
            // Cape: 10 wide × 16 tall (in model units, 1 unit = 1/16 block)
            // Render as a flat quad on the Z plane of the player's back
            float w = 10.0f / 16.0f;
            float h = 16.0f / 16.0f;
            float x0 = -w / 2.0f;
            float x1 =  w / 2.0f;
            float y0 = 0.0f;          // top of cape (at shoulder)
            float y1 = -h;            // bottom of cape

            int packedLight = light;
            int overlay = net.minecraft.client.render.entity.LivingEntityRenderer.getOverlay(state, 0.0f);

            // Counter-clockwise quad (front face), full UV coverage
            vc.vertex(entry, x0, y0, 0).color(255, 255, 255, 255).texture(0.0f, 0.0f).overlay(overlay).light(packedLight).normal(entry, 0, 0, 1);
            vc.vertex(entry, x1, y0, 0).color(255, 255, 255, 255).texture(1.0f, 0.0f).overlay(overlay).light(packedLight).normal(entry, 0, 0, 1);
            vc.vertex(entry, x1, y1, 0).color(255, 255, 255, 255).texture(1.0f, 1.0f).overlay(overlay).light(packedLight).normal(entry, 0, 0, 1);
            vc.vertex(entry, x0, y1, 0).color(255, 255, 255, 255).texture(0.0f, 1.0f).overlay(overlay).light(packedLight).normal(entry, 0, 0, 1);
        });

        matrices.pop();
    }
}
