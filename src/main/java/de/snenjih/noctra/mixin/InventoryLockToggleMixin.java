package de.snenjih.noctra.mixin;

import de.snenjih.noctra.modules.impl.inventory_lock.InventoryLockModule;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.screen.slot.Slot;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HandledScreen.class)
public class InventoryLockToggleMixin {

    @Shadow @Nullable protected Slot focusedSlot;

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(Click click, boolean releaseOnly,
                                CallbackInfoReturnable<Boolean> cir) {
        InventoryLockModule module = InventoryLockModule.INSTANCE;
        if (module == null || !module.isEnabled()) return;
        if (click.button() != 1 || (click.modifiers() & InputUtil.GLFW_MOD_SHIFT) == 0) return;
        if (focusedSlot == null) return;

        module.toggleSlot(focusedSlot.id);
        cir.setReturnValue(true);
        cir.cancel();
    }
}
