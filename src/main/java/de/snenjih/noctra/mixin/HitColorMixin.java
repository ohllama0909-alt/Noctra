package de.snenjih.noctra.mixin;

import de.snenjih.noctra.modules.impl.hit_color.HitColorModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class HitColorMixin {

    @Inject(method = "renderVignetteOverlay(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/entity/Entity;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void onRenderVignette(DrawContext ctx, Entity entity, CallbackInfo ci) {
        HitColorModule mod = HitColorModule.INSTANCE;
        if (mod == null) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        int hurtTime = mc.player.hurtTime;
        if (hurtTime <= 0) return;

        ci.cancel();

        if (mod.disableFlash.get()) return;

        float progress   = hurtTime / 10.0f;
        float opacity    = Math.min(progress * mod.opacityFactor.get(), 1.0f);
        int   baseColor  = mod.hitColor.get();
        int   baseAlpha  = (baseColor >> 24) & 0xFF;
        int   finalAlpha = (int) (baseAlpha * opacity);
        int   finalColor = (finalAlpha << 24) | (baseColor & 0x00FFFFFF);

        int w = mc.getWindow().getScaledWidth();
        int h = mc.getWindow().getScaledHeight();

        ctx.fill(0, 0, w, h, finalColor);
    }
}
