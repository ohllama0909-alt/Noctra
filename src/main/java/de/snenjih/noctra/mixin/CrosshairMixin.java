package de.snenjih.noctra.mixin;

import de.snenjih.noctra.modules.impl.crosshair_customizer.CrosshairCustomizerModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.mob.Monster;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class CrosshairMixin {

    @Inject(method = "renderCrosshair(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void onRenderCrosshair(DrawContext ctx, RenderTickCounter counter, CallbackInfo ci) {
        CrosshairCustomizerModule mod = CrosshairCustomizerModule.INSTANCE;
        if (mod == null) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options.getPerspective() != Perspective.FIRST_PERSON) return;

        ci.cancel();

        int cx = mc.getWindow().getScaledWidth()  / 2;
        int cy = mc.getWindow().getScaledHeight() / 2;

        int drawColor = mod.color.get();
        if (mod.dynamicColor.get() && mc.targetedEntity instanceof Monster) {
            drawColor = mod.enemyColor.get();
        }

        int s    = mod.size.get();
        int half = mod.thickness.get() / 2;
        int g    = mod.gap.get();

        if (mod.outline.get()) {
            int oc = mod.outlineColor.get();
            int p  = 1;
            ctx.fill(cx - s - p, cy - half - p, cx - g + p,     cy + half + p, oc);
            ctx.fill(cx + g - p, cy - half - p, cx + s + p,     cy + half + p, oc);
            ctx.fill(cx - half - p, cy - s - p, cx + half + p,  cy - g + p,    oc);
            ctx.fill(cx - half - p, cy + g - p, cx + half + p,  cy + s + p,    oc);
        }

        // Horizontal arms
        ctx.fill(cx - s, cy - half, cx - g, cy + half, drawColor);
        ctx.fill(cx + g, cy - half, cx + s, cy + half, drawColor);
        // Vertical arms
        ctx.fill(cx - half, cy - s, cx + half, cy - g, drawColor);
        ctx.fill(cx - half, cy + g, cx + half, cy + s, drawColor);

        if (mod.dot.get()) {
            int ds = mod.dotSize.get() / 2;
            if (mod.outline.get()) {
                ctx.fill(cx - ds - 1, cy - ds - 1, cx + ds + 1, cy + ds + 1, mod.outlineColor.get());
            }
            ctx.fill(cx - ds, cy - ds, cx + ds, cy + ds, drawColor);
        }
    }
}
