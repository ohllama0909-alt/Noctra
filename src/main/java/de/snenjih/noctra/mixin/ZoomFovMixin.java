package de.snenjih.noctra.mixin;

import de.snenjih.noctra.modules.impl.zoom.ZoomModule;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class ZoomFovMixin {

    @Inject(method = "getFov(Lnet/minecraft/client/render/Camera;FZ)F",
            at = @At("RETURN"),
            cancellable = true)
    private void onGetFov(Camera camera, float tickDelta, boolean changingFov,
                          CallbackInfoReturnable<Float> cir) {
        ZoomModule mod = ZoomModule.INSTANCE;
        if (mod == null || !mod.isEnabled()) return;
        cir.setReturnValue((float) mod.getCurrentFov());
    }
}
