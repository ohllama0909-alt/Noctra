package de.snenjih.noctra.mixin;

import de.snenjih.noctra.modules.impl.inventory_lock.InventoryLockModule;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public class InventoryLockOverlayMixin {

    @Inject(method = "drawSlot", at = @At("TAIL"))
    private void onDrawSlot(DrawContext ctx, Slot slot, int x, int y, CallbackInfo ci) {
        InventoryLockModule module = InventoryLockModule.INSTANCE;
        if (module == null || !module.isEnabled() || !module.showLockIcon.get()) return;
        if (!module.isSlotLocked(slot.id)) return;

        ctx.fill(x, y, x + 16, y + 16, 0x88FF0000);
    }
}
