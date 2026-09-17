package de.snenjih.noctra.mixin;

import de.snenjih.noctra.modules.impl.boss_bar_customizer.BossBarCustomizerModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.text.Text;
import org.joml.Matrix3x2fStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BossBarHud.class)
public class BossBarCustomizerMixin {

    @Unique
    private boolean noctraMatrixPushed = false;

    @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;)V", at = @At("HEAD"), cancellable = true)
    private void onRenderHead(DrawContext ctx, CallbackInfo ci) {
        BossBarCustomizerModule m = BossBarCustomizerModule.INSTANCE;
        if (m == null || !m.isEnabled()) return;

        if (m.hideBar.get()) { ci.cancel(); return; }

        float s  = m.scale.get();
        int   xo = m.xOffset.get();
        int   yo = m.yOffset.get();
        if (s == 1.0f && xo == 0 && yo == 0) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        float cx = mc.getWindow().getScaledWidth() / 2f;
        float cy = 12f;

        Matrix3x2fStack ms = ctx.getMatrices();
        ms.pushMatrix();
        ms.translate(cx + xo, cy + yo);
        ms.scale(s, s);
        ms.translate(-cx, -cy);
        noctraMatrixPushed = true;
    }

    @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;)V", at = @At("RETURN"))
    private void onRenderReturn(DrawContext ctx, CallbackInfo ci) {
        if (noctraMatrixPushed) {
            ctx.getMatrices().popMatrix();
            noctraMatrixPushed = false;
        }
    }

    @Redirect(
        method = "renderBossBar(Lnet/minecraft/client/gui/DrawContext;IILnet/minecraft/entity/boss/BossBar;)V",
        at = @At(value = "INVOKE",
                 target = "Lnet/minecraft/client/gui/DrawContext;drawCenteredTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;III)V"),
        require = 0
    )
    private void redirectBossBarText(DrawContext ctx, TextRenderer tr, Text text, int x, int y, int color) {
        BossBarCustomizerModule m = BossBarCustomizerModule.INSTANCE;
        if (m != null && m.isEnabled() && m.hideText.get()) return;
        ctx.drawCenteredTextWithShadow(tr, text, x, y, color);
    }
}
