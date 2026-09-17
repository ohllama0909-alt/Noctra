package de.snenjih.noctra.mixin;

import de.snenjih.noctra.modules.impl.inventory_lock.InventoryLockModule;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerInteractionManager.class)
public class InventoryLockClickMixin {

    @Inject(
        method = "clickSlot",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onClickSlot(int syncId, int slotId, int button,
                             SlotActionType actionType, PlayerEntity player,
                             CallbackInfo ci) {
        InventoryLockModule module = InventoryLockModule.INSTANCE;
        if (module == null) return;

        if (player != null
                && player.playerScreenHandler != null
                && syncId == player.playerScreenHandler.syncId
                && module.isSlotLocked(slotId)) {
            ci.cancel();
        }
    }
}
