package de.snenjih.noctra.mixin;

import de.snenjih.noctra.modules.impl.scoreboard_hud.ScoreboardHudModule;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class ScoreboardHudVanillaMixin {

    @Inject(method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void onRenderScoreboardSidebar(DrawContext ctx, RenderTickCounter counter, CallbackInfo ci) {
        ScoreboardHudModule mod = ScoreboardHudModule.INSTANCE;
        if (mod != null && mod.isEnabled() && mod.hideVanilla.get()) ci.cancel();
    }
}
