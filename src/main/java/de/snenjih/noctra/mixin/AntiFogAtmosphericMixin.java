package de.snenjih.noctra.mixin;

import de.snenjih.noctra.modules.impl.anti_fog.AntiFogModule;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.fog.AtmosphericFogModifier;
import net.minecraft.client.render.fog.FogData;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AtmosphericFogModifier.class)
public class AntiFogAtmosphericMixin {

    @Inject(method = "applyStartEndModifier", at = @At("RETURN"))
    private void onApplyFog(FogData fogData, Camera camera, ClientWorld world,
                             float viewDistance, RenderTickCounter counter, CallbackInfo ci) {
        AntiFogModule m = AntiFogModule.INSTANCE;
        if (m == null || !m.isEnabled()) return;
        float start = m.fogStart.get();
        float end   = m.fogEnd.get();
        if (end <= start) end = start + 1f;
        fogData.environmentalStart    = start;
        fogData.environmentalEnd      = end;
        fogData.renderDistanceStart   = start;
        fogData.renderDistanceEnd     = end;
    }
}
