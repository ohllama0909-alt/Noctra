package de.snenjih.noctra.mixin;

import de.snenjih.noctra.menu.ModuleRegistry;
import de.snenjih.noctra.modules.api.Module;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public class ClientInteractionMixin {

    @Inject(method = "interactItem", at = @At("HEAD"), cancellable = true)
    private void onInteractItem(PlayerEntity player, Hand hand,
                                CallbackInfoReturnable<ActionResult> cir) {
        if (!(player instanceof ClientPlayerEntity clientPlayer)) return;

        for (Module module : ModuleRegistry.getInstance().getAll()) {
            if (!module.isEnabled()) continue;
            ActionResult result = module.onInteractItem(clientPlayer, hand);
            if (result != ActionResult.PASS) {
                cir.setReturnValue(result);
                cir.cancel();
                return;
            }
        }
    }
}
