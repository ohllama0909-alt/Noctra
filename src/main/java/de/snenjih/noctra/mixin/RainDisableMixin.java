package de.snenjih.noctra.mixin;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import de.snenjih.noctra.menu.ModuleRegistry;
import de.snenjih.noctra.modules.api.Module;
import net.minecraft.client.render.FrameGraphBuilder;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class RainDisableMixin {

    @Inject(method = "renderWeather(Lnet/minecraft/client/render/FrameGraphBuilder;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void onRenderWeather(FrameGraphBuilder builder, GpuBufferSlice slice, CallbackInfo ci) {
        Module m = ModuleRegistry.getInstance().getById("rain_disable").orElse(null);
        if (m != null && m.isEnabled()) ci.cancel();
    }
}
