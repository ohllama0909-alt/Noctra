package de.snenjih.noctra.mixin;

import de.snenjih.noctra.modules.impl.anti_vignette.AntiVignetteModule;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class AntiVignetteMixin {

    @Inject(
        method = "renderVignetteOverlay(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/entity/Entity;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onRenderVignette(DrawContext ctx, Entity entity, CallbackInfo ci) {
        AntiVignetteModule m = AntiVignetteModule.INSTANCE;
        if (m != null && m.isEnabled()) ci.cancel();
    }

    @Inject(
        method = "renderOverlay(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/util/Identifier;F)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onRenderOverlay(DrawContext ctx, Identifier texture, float alpha, CallbackInfo ci) {
        AntiVignetteModule m = AntiVignetteModule.INSTANCE;
        if (m == null || !m.isEnabled()) return;
        String path = texture.getPath();
        if (path.contains("underwater") && m.disableUnderwater.get()) {
            ci.cancel();
        } else if ((path.contains("pumpkin") || path.contains("pumpkinblur")) && m.disablePumpkin.get()) {
            ci.cancel();
        }
    }
}
