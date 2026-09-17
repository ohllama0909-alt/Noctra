package de.snenjih.noctra.mixin;

import de.snenjih.noctra.modules.impl.inventory_lock.InventoryLockModule;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerEntity.class)
public class InventoryLockDropMixin {

    @Inject(method = "dropSelectedItem", at = @At("HEAD"), cancellable = true)
    private void onDropSelectedItem(boolean entireStack, CallbackInfoReturnable<Boolean> cir) {
        InventoryLockModule module = InventoryLockModule.INSTANCE;
        if (module == null) return;

        ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;
        int activeSlot = 36 + player.getInventory().getSelectedSlot();
        if (module.isSlotLocked(activeSlot)) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }
}
