package de.snenjih.noctra.mixin;

import de.snenjih.noctra.modules.impl.nametag_badge.NametageModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityRendererMixin {

    @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true)
    private void injectBadge(CallbackInfoReturnable<Text> cir) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        Entity self = (Entity) (Object) this;
        if (self != mc.player) return;
        if (!NametageModule.isBadgeActive()) return;
        cir.setReturnValue(
            Text.literal("✦ ").withColor(NametageModule.getBadgeColor()).append(cir.getReturnValue())
        );
    }
}
