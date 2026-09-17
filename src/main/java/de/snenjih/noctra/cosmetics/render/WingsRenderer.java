package de.snenjih.noctra.cosmetics.render;

import de.snenjih.noctra.cosmetics.api.CosmeticType;
import de.snenjih.noctra.cosmetics.api.PlayerCosmeticData;
import de.snenjih.noctra.cosmetics.storage.CosmeticRegistry;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

/**
 * Renders cosmetic wings behind the player's back.
 * Two mirrored textured quads, spread slightly outward at shoulder height.
 * Wings are suppressed while the player is gliding (elytra in use).
 */
public final class WingsRenderer {

    private WingsRenderer() {}

    public static void render(MatrixStack matrices, OrderedRenderCommandQueue renderQueue,
                              int light, PlayerEntityRenderState state, PlayerCosmeticData data) {
        String wingsId = data.getEquipped(CosmeticType.WINGS);
        if (wingsId == null) return;

        Identifier texture = CosmeticRegistry.getTextureIdentifier(wingsId);
        if (texture == null) return;

        RenderLayer layer = RenderLayers.entityCutoutNoCull(texture);

        int overlay = LivingEntityRenderer.getOverlay(state, 0.0f);

        matrices.push();
        // Position at the player's back, shoulder height
        // In model space: shoulder is roughly at y = -1.0 (chest area)
        matrices.translate(0.0, -1.0, 0.1);

        // Wing panels: 8×12 (width × height) each, left and right
        float wingW = 8.0f / 16.0f;
        float wingH = 12.0f / 16.0f;

        // Right wing panel (on player's right, in +X direction)
        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(30.0f)); // slight outward angle
        renderQueue.submitCustom(matrices, layer, (entry, vc) -> {
            // Right half of texture (u: 0.5 to 1.0), facing forward (+Z normal)
            vc.vertex(entry, 0.0f,     0.0f,   0.0f).color(255, 255, 255, 255).texture(0.5f, 0.0f).overlay(overlay).light(light).normal(entry, 0, 0, 1);
            vc.vertex(entry, wingW,    0.0f,   0.0f).color(255, 255, 255, 255).texture(1.0f, 0.0f).overlay(overlay).light(light).normal(entry, 0, 0, 1);
            vc.vertex(entry, wingW,   -wingH,  0.0f).color(255, 255, 255, 255).texture(1.0f, 1.0f).overlay(overlay).light(light).normal(entry, 0, 0, 1);
            vc.vertex(entry, 0.0f,    -wingH,  0.0f).color(255, 255, 255, 255).texture(0.5f, 1.0f).overlay(overlay).light(light).normal(entry, 0, 0, 1);
        });
        matrices.pop();

        // Left wing panel (on player's left, mirrored into -X direction)
        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-30.0f)); // slight outward angle (mirror)
        renderQueue.submitCustom(matrices, layer, (entry, vc) -> {
            // Left half of texture (u: 0.0 to 0.5), mirrored (UV flipped horizontally)
            vc.vertex(entry, 0.0f,     0.0f,   0.0f).color(255, 255, 255, 255).texture(0.5f, 0.0f).overlay(overlay).light(light).normal(entry, 0, 0, 1);
            vc.vertex(entry, -wingW,   0.0f,   0.0f).color(255, 255, 255, 255).texture(0.0f, 0.0f).overlay(overlay).light(light).normal(entry, 0, 0, 1);
            vc.vertex(entry, -wingW,  -wingH,  0.0f).color(255, 255, 255, 255).texture(0.0f, 1.0f).overlay(overlay).light(light).normal(entry, 0, 0, 1);
            vc.vertex(entry, 0.0f,    -wingH,  0.0f).color(255, 255, 255, 255).texture(0.5f, 1.0f).overlay(overlay).light(light).normal(entry, 0, 0, 1);
        });
        matrices.pop();

        matrices.pop();
    }
}
