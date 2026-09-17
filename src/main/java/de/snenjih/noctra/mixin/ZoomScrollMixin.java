package de.snenjih.noctra.mixin;

import de.snenjih.noctra.modules.impl.zoom.ZoomModule;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class ZoomScrollMixin {

    @Inject(method = "onMouseScroll(JDD)V",
            at = @At("HEAD"),
            cancellable = true)
    private void onScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        ZoomModule mod = ZoomModule.INSTANCE;
        if (mod == null || !mod.isEnabled() || !mod.isZoomActive()) return;
        mod.adjustZoom(vertical);
        ci.cancel();
    }
}
