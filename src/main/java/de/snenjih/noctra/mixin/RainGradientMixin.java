package de.snenjih.noctra.mixin;

import de.snenjih.noctra.menu.ModuleRegistry;
import de.snenjih.noctra.modules.api.Module;
import de.snenjih.noctra.modules.impl.rain_disable.RainDisableModule;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(World.class)
public class RainGradientMixin {

    @Inject(method = "getRainGradient(F)F",
            at = @At("HEAD"),
            cancellable = true)
    private void onGetRainGradient(float delta, CallbackInfoReturnable<Float> cir) {
        Module m = ModuleRegistry.getInstance().getById("rain_disable").orElse(null);
        if (!(m instanceof RainDisableModule rain)) return;
        if (rain.isEnabled() && rain.alsoDisableThunder.get()) cir.setReturnValue(0f);
    }
}
