package de.snenjih.noctra.mixin;

import de.snenjih.noctra.menu.ModuleRegistry;
import de.snenjih.noctra.modules.api.Module;
import de.snenjih.noctra.modules.impl.sneak_toggle.SneakToggleModule;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.util.PlayerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public class SneakToggleMixin {

    @Inject(at = @At("TAIL"), method = "tick()V")
    private void noctraSneakOverride(CallbackInfo ci) {
        Module m = ModuleRegistry.getInstance().getById("sneak_toggle").orElse(null);
        if (!(m instanceof SneakToggleModule sneakModule)) return;
        if (!sneakModule.isEnabled() || !sneakModule.isSneakActive()) return;

        // playerInput is a public field on the parent class Input
        Input input = (Input) (Object) this;
        PlayerInput pi = input.playerInput;
        if (pi == null || pi.sneak()) return; // already sneaking, no-op
        input.playerInput = new PlayerInput(
            pi.forward(), pi.backward(), pi.left(), pi.right(),
            pi.jump(), true, pi.sprint()
        );
    }
}
