package de.snenjih.mandatory.mixin;

import de.snenjih.mandatory.auth.AccountEntry;
import de.snenjih.mandatory.auth.AccountManager;
import de.snenjih.mandatory.auth.SkinCache;
import de.snenjih.mandatory.menu.AccountScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin {

    private static final int WIDGET_W = 130;
    private static final int WIDGET_H = 22;
    private static final int WIDGET_X = 6;

    private static final int COL_BG_IDLE   = 0xAA1A2A40;
    private static final int COL_BG_HOVER  = 0xAA243350;
    private static final int COL_BORDER    = 0xFF1E3A5F;
    private static final int COL_BORDER_HV = 0xFF4A7CF8;
    private static final int COL_TEXT      = 0xFFFFFFFF;
    private static final int COL_SUBTEXT   = 0xFF8899AA;

    @Unique private float mandatory$hoverProgress = 0f;

    @Inject(method = "render", at = @At("TAIL"))
    private void mandatory$renderAccountWidget(DrawContext ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (AccountManager.getInstance() == null) return;

        int screenW = mandatory$getWidth();
        int screenH = mandatory$getHeight();
        int wy = screenH - WIDGET_H - 5;

        boolean hovered = mouseX >= WIDGET_X && mouseX <= WIDGET_X + WIDGET_W
                       && mouseY >= wy       && mouseY <= wy + WIDGET_H;

        // Animate hover
        float target = hovered ? 1f : 0f;
        mandatory$hoverProgress += (target - mandatory$hoverProgress) * 0.2f;

        int border = mandatory$lerpColor(COL_BORDER, COL_BORDER_HV, mandatory$hoverProgress);
        int bg     = mandatory$lerpColor(COL_BG_IDLE, COL_BG_HOVER, mandatory$hoverProgress);

        ctx.fill(WIDGET_X, wy, WIDGET_X + WIDGET_W, wy + WIDGET_H, bg);
        ctx.drawStrokedRectangle(WIDGET_X, wy, WIDGET_W, WIDGET_H, border);

        AccountEntry active = AccountManager.getInstance().getActive();
        MinecraftClient mc  = MinecraftClient.getInstance();

        int headX = WIDGET_X + 3;
        int headY = wy + 3;
        int headS = 16;

        if (active != null) {
            // Skin head
            SkinCache cache = SkinCache.getInstance();
            if (cache != null && cache.isLoaded(active.uuid())) {
                Identifier skinId = cache.getTextureId(active.uuid());
                // Face layer
                ctx.drawTexture(RenderPipelines.GUI_TEXTURED, skinId,
                        headX, headY, 8f, 8f, headS, headS, 8, 8, 64, 64);
                // Hat overlay
                ctx.drawTexture(RenderPipelines.GUI_TEXTURED, skinId,
                        headX, headY, 40f, 8f, headS, headS, 8, 8, 64, 64);
            } else {
                // Fallback: gray box
                ctx.fill(headX, headY, headX + headS, headY + headS, 0xFF334455);
                ctx.drawCenteredTextWithShadow(mc.textRenderer, "?",
                        headX + headS / 2, headY + 4, COL_SUBTEXT);
            }

            // Username + caret
            String name   = active.username();
            int    textX  = headX + headS + 4;
            int    textY  = wy + (WIDGET_H - 8) / 2;
            int    maxLen = WIDGET_W - headS - 24;  // leave space for caret
            if (mc.textRenderer.getWidth(name) > maxLen) {
                // Truncate with ellipsis
                while (name.length() > 0 && mc.textRenderer.getWidth(name + "…") > maxLen)
                    name = name.substring(0, name.length() - 1);
                name = name + "…";
            }
            ctx.drawTextWithShadow(mc.textRenderer, name, textX, textY, COL_TEXT);
        } else {
            ctx.drawCenteredTextWithShadow(mc.textRenderer, "Add Account",
                    WIDGET_X + WIDGET_W / 2, wy + (WIDGET_H - 8) / 2, COL_SUBTEXT);
        }

        // Caret ▾
        ctx.drawTextWithShadow(mc.textRenderer, "▾",
                WIDGET_X + WIDGET_W - 12, wy + (WIDGET_H - 8) / 2, COL_SUBTEXT);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void mandatory$onMouseClicked(Click click, boolean releaseOnly,
                                          CallbackInfoReturnable<Boolean> cir) {
        if (releaseOnly || click.button() != 0 || AccountManager.getInstance() == null) return;

        int screenH = mandatory$getHeight();
        int wy      = screenH - WIDGET_H - 5;

        if (click.x() >= WIDGET_X && click.x() <= WIDGET_X + WIDGET_W
         && click.y() >= wy       && click.y() <= wy + WIDGET_H) {
            MinecraftClient.getInstance().setScreen(
                    new AccountScreen((net.minecraft.client.gui.screen.Screen)(Object) this)
            );
            cir.setReturnValue(true);
        }
    }

    // Access screen width/height — available because TitleScreen extends Screen
    @Unique
    private int mandatory$getWidth() {
        return ((net.minecraft.client.gui.screen.Screen)(Object) this).width;
    }

    @Unique
    private int mandatory$getHeight() {
        return ((net.minecraft.client.gui.screen.Screen)(Object) this).height;
    }

    @Unique
    private static int mandatory$lerpColor(int a, int b, float t) {
        int ar = (a >> 16 & 0xFF), ag = (a >> 8 & 0xFF), ab = (a & 0xFF), aa = (a >> 24 & 0xFF);
        int br = (b >> 16 & 0xFF), bg = (b >> 8 & 0xFF), bb = (b & 0xFF), ba = (b >> 24 & 0xFF);
        int rr = (int)(ar + (br - ar) * t);
        int rg = (int)(ag + (bg - ag) * t);
        int rb = (int)(ab + (bb - ab) * t);
        int ra = (int)(aa + (ba - aa) * t);
        return (ra << 24) | (rr << 16) | (rg << 8) | rb;
    }
}
