package de.snenjih.noctra.mixin;

import de.snenjih.noctra.modules.impl.cps_counter.CpsCounterModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.input.MouseInput;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MouseButtonMixin {

    @Inject(at = @At("HEAD"), method = "onMouseButton(JLnet/minecraft/client/input/MouseInput;I)V")
    private void onMouseButton(long window, MouseInput mouseInput, int action, CallbackInfo ci) {
        if (action != GLFW.GLFW_PRESS) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.currentScreen != null) return;
        CpsCounterModule.onMouseClick(mouseInput.button());
    }
}
