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

/**
 * Renders a hat cosmetic on top of the player's head.
 * The hat is rendered as a flat textured quad (billboard plane) aligned with the top of the head.
 */
public final class HatRenderer {

    private HatRenderer() {}

    public static void render(MatrixStack matrices, OrderedRenderCommandQueue renderQueue,
                              int light, PlayerEntityRenderState state, PlayerCosmeticData data) {
        String hatId = data.getEquipped(CosmeticType.HAT);
        if (hatId == null) return;

        Identifier texture = CosmeticRegistry.getTextureIdentifier(hatId);
        if (texture == null) return;

        RenderLayer layer = RenderLayers.entityCutoutNoCull(texture);

        matrices.push();
        // Player model head center is at y = 1.5 from feet (24/16 units up)
        // Head top is at y = 1.5 + 0.5 = 2.0 (the head is 8/16 tall)
        // In feature-renderer space, y=0 is at the entity's feet.
        // Head top: 1.5 + 4/16 = ~1.75 from feet => translate up by 1.75
        // However the model space is: head base at y=-1.5 (feet=0), so
        // In the vanilla player model: body goes from -0.75 to 0.75 (y), head from 0 to 0.5 above body
        // The entity's feet are at y=0 in world. The model renders with y=-height..0
        // For FeatureRenderers: translations are in model space where feet = 0
        // Head top in model space = -(24/16) - (8/16) = -2.0 (negative Y is up in GL)
        // Actually in MC: y=0 = top of head, y = 1.5 = feet
        // Let's use: translate to top of head, then go a bit higher for the hat
        matrices.translate(0.0, -1.5 - 8.0 / 16.0, 0.0); // top of head

        // Hat: flat 8×8 plane on top of head (horizontal)
        float size = 8.0f / 16.0f;
        float half = size / 2.0f;

        int overlay = LivingEntityRenderer.getOverlay(state, 0.0f);

        renderQueue.submitCustom(matrices, layer, (entry, vc) -> {
            // Horizontal quad lying on top of the head (XZ plane, facing up = -Y normal)
            vc.vertex(entry, -half, 0.0f, -half).color(255, 255, 255, 255).texture(0.0f, 0.0f).overlay(overlay).light(light).normal(entry, 0, -1, 0);
            vc.vertex(entry,  half, 0.0f, -half).color(255, 255, 255, 255).texture(1.0f, 0.0f).overlay(overlay).light(light).normal(entry, 0, -1, 0);
            vc.vertex(entry,  half, 0.0f,  half).color(255, 255, 255, 255).texture(1.0f, 1.0f).overlay(overlay).light(light).normal(entry, 0, -1, 0);
            vc.vertex(entry, -half, 0.0f,  half).color(255, 255, 255, 255).texture(0.0f, 1.0f).overlay(overlay).light(light).normal(entry, 0, -1, 0);
        });

        matrices.pop();
    }
}
