package de.snenjih.noctra.mixin;

import de.snenjih.noctra.modules.impl.damage_indicator.DamageIndicatorModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class DamageIndicatorMixin {

    @Inject(method = "setHealth(F)V", at = @At("HEAD"))
    private void onSetHealth(float health, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;

        DamageIndicatorModule module = DamageIndicatorModule.INSTANCE;
        if (module == null || !module.isEnabled()) return;

        LivingEntity self = (LivingEntity)(Object)this;
        float delta = self.getHealth() - health;

        if (Math.abs(delta) < 0.5f) return;
        if (delta > 0 && !module.showDamage.get()) return;
        if (delta < 0 && !module.showHealing.get()) return;

        if (self instanceof PlayerEntity p && !module.showPlayerDamage.get()) {
            if (mc.player != null && p.getUuid().equals(mc.player.getUuid())) return;
        }

        Vec3d pos = new Vec3d(self.getX(), self.getY(), self.getZ());
        module.addParticle(pos, delta, self.getHeight());
    }
}
