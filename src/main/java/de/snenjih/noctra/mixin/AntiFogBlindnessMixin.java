package de.snenjih.noctra.mixin;

import de.snenjih.noctra.modules.impl.anti_fog.AntiFogModule;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.fog.BlindnessEffectFogModifier;
import net.minecraft.client.render.fog.FogData;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlindnessEffectFogModifier.class)
public class AntiFogBlindnessMixin {

    @Inject(method = "applyStartEndModifier", at = @At("RETURN"))
    private void onApplyFog(FogData fogData, Camera camera, ClientWorld world,
                             float viewDistance, RenderTickCounter counter, CallbackInfo ci) {
        AntiFogModule m = AntiFogModule.INSTANCE;
        if (m == null || !m.isEnabled() || !m.disableBlindnessFog.get()) return;
        fogData.environmentalStart  = 900f;
        fogData.environmentalEnd    = 1000f;
        fogData.renderDistanceStart = 900f;
        fogData.renderDistanceEnd   = 1000f;
    }
}
